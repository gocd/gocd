/*
 * Copyright Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.preprocessor.ConcurrentFieldCache;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.XmlUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.jdom2.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIf;
import static java.text.MessageFormat.format;

public class MagicalGoConfigXmlWriter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MagicalGoConfigXmlWriter.class);
    public static final String XML_NS = "http://www.w3.org/2001/XMLSchema-instance";
    private final ConfigElementImplementationRegistry registry;

    public MagicalGoConfigXmlWriter(ConfigElementImplementationRegistry registry) {
        this.registry = registry;
    }

    private Document createEmptyCruiseConfigDocument() {
        Element root = new Element("cruise");
        Namespace xsiNamespace = Namespace.getNamespace("xsi", XML_NS);
        root.addNamespaceDeclaration(xsiNamespace);
        root.setAttribute("noNamespaceSchemaLocation", "cruise-config.xsd", xsiNamespace);
        root.setAttribute("schemaVersion", Integer.toString(GoConstants.CONFIG_SCHEMA_VERSION));
        return new Document(root);
    }

    public void write(CruiseConfig configForEdit, OutputStream output, boolean skipPreprocessingAndValidation) throws JDOMException, IOException {
        LOGGER.debug("[Serializing Config] Starting to write. Validation skipped? {}", skipPreprocessingAndValidation);
        MagicalGoConfigXmlLoader loader = new MagicalGoConfigXmlLoader(registry);
        if (!configForEdit.getOrigin().isLocal()) {
            throw new GoConfigInvalidException(configForEdit, "Attempted to save merged configuration with partials");
        }
        if (!skipPreprocessingAndValidation) {
            loader.preprocessAndValidate(configForEdit);
            LOGGER.debug("[Serializing Config] Done with cruise config validators.");
        }
        Document document = createEmptyCruiseConfigDocument();
        write(configForEdit, document.getRootElement(), registry);

        LOGGER.debug("[Serializing Config] XSD and DOM validation.");
        verifyXsdValid(document);
        MagicalGoConfigXmlLoader.validateDom(document.getRootElement(), registry);
        LOGGER.info("[Serializing Config] Generating config partial.");
        XmlUtils.writeXml(document, output);
        LOGGER.debug("[Serializing Config] Finished writing config partial.");
    }

    public Document documentFrom(CruiseConfig config) {
        Document document = createEmptyCruiseConfigDocument();
        write(config, document.getRootElement(), registry);
        return document;
    }

    public String toString(Document document) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream(32 * 1024)) {
            XmlUtils.writeXml(document, outputStream);
            return outputStream.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e); // Unlikely to happen due to use of ByteArrayOutputStream
        }
    }

    public void verifyXsdValid(Document document) throws JDOMException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream(32 * 1024)) {
            XmlUtils.writeXml(document, buffer);
            XmlUtils.buildValidatedXmlDocument(buffer.toInputStream(), GoConfigSchema.getCurrentSchema());
        } catch (IOException e) {
            throw new UncheckedIOException(e); // Unlikely to happen due to use of ByteArrayOutputStream
        }
    }

    public String toXmlPartial(Object domainObject) {
        bombIf(!domainObject.getClass().isAnnotationPresent(ConfigTag.class), () -> "Object " + domainObject + " does not have a ConfigTag");
        Element element = elementFor(domainObject.getClass());
        write(domainObject, element, registry);
        if (domainObject.getClass().isAnnotationPresent(ConfigCollection.class) && domainObject instanceof Collection) {
            for (Object item : (Collection<?>) domainObject) {
                if (item.getClass().isAnnotationPresent(ConfigCollection.class) && item instanceof Collection) {
                    new ExplicitCollectionXmlFieldWithValue(domainObject.getClass(), null, (Collection<?>) item, registry).populate(element);
                    continue;
                }
                Element childElement = elementFor(item.getClass());
                element.addContent(childElement);
                write(item, childElement, registry);
            }
        }

        try (ByteArrayOutputStream output = new ByteArrayOutputStream(32 * 1024)) {
            XmlUtils.writeXml(element, output);
            // FIXME the lack of charset here looks rather suspicious. But unclear how to fix without possible regressions.
            // Related to similar issue in GoConfigMigration?
            return output.toString(Charset.defaultCharset());
        } catch (IOException e) {
            throw new UncheckedIOException(e); // Unlikely to happen due to use of ByteArrayOutputStream
        }
    }

    private static Namespace namespaceFor(ConfigTag annotation) {
        return Namespace.getNamespace(annotation.namespacePrefix(), annotation.namespaceURI());
    }

    private static Namespace namespaceFor(AttributeAwareConfigTag annotation) {
        return Namespace.getNamespace(annotation.namespacePrefix(), annotation.namespaceURI());
    }

    private static void write(Object o, Element element, final ConfigElementImplementationRegistry registry) {
        for (XmlFieldWithValue<?> xmlFieldWithValue : allFields(o, registry)) {
            if (xmlFieldWithValue.isDefault() && !xmlFieldWithValue.alwaysWrite()) {
                continue;
            }
            xmlFieldWithValue.populate(element);
        }
    }

    private static List<XmlFieldWithValue<?>> allFields(Object o, final ConfigElementImplementationRegistry registry) {
        List<XmlFieldWithValue<?>> list = new ArrayList<>();
        Class<?> originalClass = o.getClass();
        ConcurrentFieldCache.nonStaticOrSyntheticFieldsFor(originalClass)
            .stream()
            .map(declaredField -> new GoConfigFieldWriter(declaredField, o))
            .forEach(field -> {
                Field configField = field.getConfigField();
                if (field.isImplicitCollection()) {
                    list.add(new ImplicitCollectionXmlFieldWithValue(originalClass, configField, (Collection<?>) field.getValue(), registry));
                } else if (field.isConfigCollection()) {
                    list.add(new ExplicitCollectionXmlFieldWithValue(originalClass, configField, (Collection<?>) field.getValue(), registry));
                } else if (field.isSubtag()) {
                    list.add(new SubTagXmlFieldWithValue(originalClass, configField, field.getValue(), registry));
                } else if (field.isAttribute()) {
                    final Object value = field.getValue();
                    list.add(new AttributeXmlFieldWithValue(originalClass, configField, value, registry));
                } else if (field.isConfigValue()) {
                    list.add(new ValueXmlFieldWithValue(configField, field.getValue(), originalClass, registry));
                }
            });
        return list;
    }

    private abstract static class XmlFieldWithValue<T> {
        protected final Field field;
        protected final Class<?> originalClass;
        protected final T value;
        protected final ConfigElementImplementationRegistry registry;

        private XmlFieldWithValue(Class<?> originalClass, Field field, T value, ConfigElementImplementationRegistry registry) {
            this.originalClass = originalClass;
            this.value = value;
            this.field = field;
            this.registry = registry;
        }

        public boolean isDefault() {
            try {
                Object defaultObject = ConfigElementInstantiator.instantiateConfigElement(new GoCipher(), originalClass);
                Object defaultValue = field.get(defaultObject);
                return Objects.equals(value, defaultValue);
            } catch (Exception e) {
                return false;
            }
        }

        public abstract void populate(Element parent);

        public abstract boolean alwaysWrite();

        protected String valueString() {
            String valueString = null;
            ConfigAttributeValue attributeValue = value.getClass().getAnnotation(ConfigAttributeValue.class);
            if (attributeValue != null) {
                try {
                    Field field = ConfigAttributeValue.Resolver.resolveAccessibleField(value.getClass(), attributeValue);
                    valueString = field.get(value).toString();
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    bomb(e);
                }
            } else {
                valueString = value.toString();
            }
            return valueString;
        }

    }

    private static Element elementFor(Class<?> aClass) {
        final AttributeAwareConfigTag attributeAwareConfigTag = aClass.getAnnotation(AttributeAwareConfigTag.class);

        if (attributeAwareConfigTag != null) {
            final Element element = new Element(attributeAwareConfigTag.value(), namespaceFor(attributeAwareConfigTag));
            element.setAttribute(attributeAwareConfigTag.attribute(), attributeAwareConfigTag.attributeValue());
            return element;
        }

        ConfigTag configTag = aClass.getAnnotation(ConfigTag.class);
        if (configTag == null) {
            throw bomb(format("Cannot get config tag for {0}", aClass));
        }
        return new Element(configTag.value(), namespaceFor(configTag));
    }

    private static class SubTagXmlFieldWithValue extends XmlFieldWithValue<Object> {

        public SubTagXmlFieldWithValue(Class<?> originalClass, Field field, Object value, final ConfigElementImplementationRegistry registry) {
            super(originalClass, field, value, registry);
        }

        @Override
        public void populate(Element parent) {
            Element child = elementFor(value.getClass());
            parent.addContent(child);
            write(value, child, registry);
        }

        @Override
        public boolean alwaysWrite() {
            return false;
        }
    }

    private static class AttributeXmlFieldWithValue extends XmlFieldWithValue<Object> {

        public AttributeXmlFieldWithValue(Class<?> originalClass, Field field, Object current, final ConfigElementImplementationRegistry registry) {
            super(originalClass, field, current, registry);
        }

        @Override
        public void populate(Element parent) {
            if (value == null && !isDefault()) {
                if (!isDefault()) {
                    throw bomb(
                            format("Try to write null value into configuration! [{0}.{1}]",
                                    field.getDeclaringClass().getName(), field.getName()));
                }
                throw bomb(format("A non default field {0}(on {1}) had null value",
                        field.getName(), field.getDeclaringClass().getName()));
            }
            String attributeName = field.getAnnotation(ConfigAttribute.class).value();
            parent.setAttribute(new Attribute(attributeName, valueString()));
        }

        @Override
        public boolean alwaysWrite() {
            return field.getAnnotation(ConfigAttribute.class).alwaysWrite();
        }
    }

    private static class ImplicitCollectionXmlFieldWithValue extends XmlFieldWithValue<Collection<?>> {
        public ImplicitCollectionXmlFieldWithValue(
                Class<?> originalClass, Field field, Collection<?> value, final ConfigElementImplementationRegistry registry) {
            super(originalClass, field, value, registry);
        }

        @Override
        public void populate(Element parent) {
            new CollectionXmlFieldWithValue(value, parent, originalClass, registry).populate();
        }

        @Override
        public boolean alwaysWrite() {
            return false;
        }
    }

    private static class CollectionXmlFieldWithValue {
        private final Collection<?> value;
        private final Element parent;
        private final Class<?> originalClass;
        private final ConfigElementImplementationRegistry registry;

        public CollectionXmlFieldWithValue(Collection<?> value, Element parent, Class<?> originalClass, final ConfigElementImplementationRegistry registry) {
            this.value = value;
            this.parent = parent;
            this.originalClass = originalClass;
            this.registry = registry;
        }

        public void populate() {
            Collection<?> defaultCollection = generateDefaultCollection();
            for (XmlFieldWithValue<?> xmlFieldWithValue : allFields(value, registry)) {
                if (!xmlFieldWithValue.isDefault()) {
                    xmlFieldWithValue.populate(parent);
                }
            }

            for (Object item : value) {
                if (defaultCollection.contains(item)) {
                    continue;
                }
                if (item.getClass().isAnnotationPresent(ConfigCollection.class) && item instanceof Collection) {
                    new ExplicitCollectionXmlFieldWithValue(originalClass, null, (Collection<?>) item, registry).populate(parent);
                    continue;
                }
                Element childElement = elementFor(item.getClass());
                parent.addContent(childElement);
                write(item, childElement, registry);
            }
        }

        protected Collection<?> generateDefaultCollection() {
            try {
                return value.getClass().getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw bomb("Error creating default instance of " + value.getClass().getName(), e);
            }
        }
    }

    private static class ExplicitCollectionXmlFieldWithValue extends XmlFieldWithValue<Collection<?>> {
        public ExplicitCollectionXmlFieldWithValue(Class<?> originalClass, Field field, Collection<?> value, final ConfigElementImplementationRegistry registry) {
            super(originalClass, field, value, registry);
        }

        @Override
        public void populate(Element parent) {
            Element containerElement = elementFor(value.getClass());
            new CollectionXmlFieldWithValue(value, containerElement, originalClass, registry).populate();
            parent.addContent(containerElement);
        }

        @Override
        public boolean alwaysWrite() {
            return false;
        }
    }

    private static class ValueXmlFieldWithValue extends XmlFieldWithValue<Object> {
        private final boolean requireCdata;

        public ValueXmlFieldWithValue(Field field, Object value, Class<?> originalClass, final ConfigElementImplementationRegistry registry) {
            super(originalClass, field, value, registry);
            ConfigValue configValue = field.getAnnotation(ConfigValue.class);
            requireCdata = configValue.requireCdata();
        }

        @Override
        public void populate(Element parent) {
            if (requireCdata) {
                parent.addContent(new CDATA(valueString()));
            } else {
                parent.setText(valueString());
            }
        }

        @Override
        public boolean alwaysWrite() {
            return false;
        }
    }

}
