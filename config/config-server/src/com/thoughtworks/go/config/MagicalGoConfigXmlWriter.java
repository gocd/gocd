/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.XmlUtils;
import org.apache.log4j.Logger;
import org.jdom.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.thoughtworks.go.config.ConfigCache.annotationFor;
import static com.thoughtworks.go.config.ConfigCache.isAnnotationPresent;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIf;
import static com.thoughtworks.go.util.ObjectUtil.nullSafeEquals;
import static com.thoughtworks.go.util.XmlUtils.buildXmlDocument;
import static java.text.MessageFormat.format;
import static org.apache.commons.io.IOUtils.toInputStream;

public class MagicalGoConfigXmlWriter {
    private static final Logger LOGGER = Logger.getLogger(MagicalGoConfigXmlWriter.class);
    public static final String XML_NS = "http://www.w3.org/2001/XMLSchema-instance";
    private ConfigCache configCache;
    private final ConfigElementImplementationRegistry registry;

    public MagicalGoConfigXmlWriter(ConfigCache configCache, ConfigElementImplementationRegistry registry) {
        this.configCache = configCache;
        this.registry = registry;
    }

    private Document createEmptyCruiseConfigDocument() {
        Element root = new Element("cruise");
        Namespace xsiNamespace = Namespace.getNamespace("xsi", XML_NS);
        root.addNamespaceDeclaration(xsiNamespace);
        registry.registerNamespacesInto(root);
        root.setAttribute("noNamespaceSchemaLocation", "cruise-config.xsd", xsiNamespace);
        String xsds = registry.xsds();
        if (!xsds.isEmpty()) {
            root.setAttribute("schemaLocation", xsds, xsiNamespace);
        }
        root.setAttribute("schemaVersion", Integer.toString(GoConstants.CONFIG_SCHEMA_VERSION));
        return new Document(root);
    }

    public void write(CruiseConfig configForEdit, OutputStream output, boolean skipPreprocessingAndValidation) throws Exception {
        LOGGER.debug("[Serializing Config] Starting to write. Validation skipped? " + skipPreprocessingAndValidation);
        MagicalGoConfigXmlLoader loader = new MagicalGoConfigXmlLoader(configCache, registry);
        if (!configForEdit.getOrigin().isLocal()) {
            throw new GoConfigInvalidException(configForEdit,"Attempted to save merged configuration with patials");
        }
        if (!skipPreprocessingAndValidation) {
            loader.preprocessAndValidate(configForEdit);
            LOGGER.debug("[Serializing Config] Done with cruise config validators.");
        }
        Document document = createEmptyCruiseConfigDocument();
        write(configForEdit, document.getRootElement(), configCache, registry);

        LOGGER.debug("[Serializing Config] XSD and DOM validation.");
        verifyXsdValid(document);
        MagicalGoConfigXmlLoader.validateDom(document.getRootElement(), registry);
        LOGGER.info("[Serializing Config] Generating config partial.");
        XmlUtils.writeXml(document, output);
        LOGGER.debug("[Serializing Config] Finished writing config partial.");
    }

    private void verifyXsdValid(Document document) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        XmlUtils.writeXml(document, buffer);
        InputStream content = toInputStream(buffer.toString());
        buildXmlDocument(content, GoConfigSchema.getCurrentSchema(), registry.xsds());
    }

    public String toXmlPartial(Object domainObject) {
        bombIf(!isAnnotationPresent(domainObject.getClass(), ConfigTag.class), "Object " + domainObject + " does not have a ConfigTag");
        Element element = elementFor(domainObject.getClass(), configCache);
        write(domainObject, element, configCache, registry);
        if (isAnnotationPresent(domainObject.getClass(), ConfigCollection.class) && domainObject instanceof Collection) {
            for (Object item : (Collection) domainObject) {
                if (isAnnotationPresent(item.getClass(), ConfigCollection.class) && item instanceof Collection) {
                    new ExplicitCollectionXmlFieldWithValue(domainObject.getClass(), null, (Collection) item, configCache, registry).populate(element);
                    continue;
                }
                Element childElement = elementFor(item.getClass(), configCache);
                element.addContent(childElement);
                write(item, childElement, configCache, registry);
            }
        }

        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            XmlUtils.writeXml(element, output);
            return output.toString();
        } catch (IOException e) {
            throw bomb("Unable to write xml to String");
        }
    }

    private static Namespace namespaceFor(ConfigTag annotation) {
        return Namespace.getNamespace(annotation.namespacePrefix(), annotation.namespaceURI());
    }

    private static void write(Object o, Element element, ConfigCache configCache, final ConfigElementImplementationRegistry registry) {
        for (XmlFieldWithValue xmlFieldWithValue : allFields(o, configCache, registry)) {
            if (xmlFieldWithValue.isDefault() && !xmlFieldWithValue.alwaysWrite()) {
                continue;
            }
            xmlFieldWithValue.populate(element);
        }
    }

    private static List<XmlFieldWithValue> allFields(Object o, ConfigCache configCache, final ConfigElementImplementationRegistry registry) {
        List<XmlFieldWithValue> list = new ArrayList<>();
        Class originalClass = o.getClass();
        for (GoConfigFieldWriter field : allFieldsWithInherited(originalClass, o, configCache, registry)) {
            Field configField = field.getConfigField();
            if (field.isImplicitCollection()) {
                list.add(new ImplicitCollectionXmlFieldWithValue(originalClass, configField,
                        (Collection) field.getValue(), configCache, registry));
            } else if (field.isConfigCollection()) {
                list.add(new ExplicitCollectionXmlFieldWithValue(originalClass, configField,
                        (Collection) field.getValue(), configCache, registry));
            } else if (field.isSubtag()) {
                list.add(new SubTagXmlFieldWithValue(originalClass, configField, field.getValue(), configCache, registry));
            } else if (field.isAttribute()) {
                final Object value = field.getValue();
                list.add(new AttributeXmlFieldWithValue(originalClass, configField, value, configCache, registry));
            } else if (field.isConfigValue()) {
                list.add(new ValueXmlFieldWithValue(configField, field.getValue(), originalClass, configCache, registry));
            }
        }
        return list;
    }

    private static List<GoConfigFieldWriter> allFieldsWithInherited(Class aClass, Object o, ConfigCache configCache, final ConfigElementImplementationRegistry registry) {
        return new GoConfigClassWriter(aClass, configCache, registry).getAllFields(o);
    }

    private abstract static class XmlFieldWithValue<T> {
        protected final Field field;
        protected final Class originalClass;
        protected final T value;
        protected final ConfigCache configCache;
        protected final ConfigElementImplementationRegistry registry;

        private XmlFieldWithValue(Class originalClass, Field field, T value, ConfigCache configCache, ConfigElementImplementationRegistry registry) {
            this.originalClass = originalClass;
            this.value = value;
            this.field = field;
            this.configCache = configCache;
            this.registry = registry;
        }

        public boolean isDefault() {
            try {
                Object defaultObject = ConfigElementInstantiator.instantiateConfigElement(new GoCipher(), originalClass);
                Object defaultValue = field.get(defaultObject);
                return nullSafeEquals(value, defaultValue);
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
                    Field field = getField(value.getClass(), attributeValue);
                    field.setAccessible(true);
                    valueString = field.get(value).toString();
                } catch (NoSuchFieldException e) {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    bomb(e);
                } catch (IllegalAccessException e) {
                    //noinspection ThrowableResultOfMethodCallIgnored
                    bomb(e);
                }
            } else {
                valueString = value.toString();
            }
            return valueString;
        }

        private Field getField(Class clazz, ConfigAttributeValue attributeValue) throws NoSuchFieldException {
            try {
                return clazz.getDeclaredField(attributeValue.fieldName());
            } catch (NoSuchFieldException e) {
                Class klass = clazz.getSuperclass();
                if (klass == null) {
                    throw e;
                }
                return getField(klass, attributeValue);
            }
        }
    }

    private static Element elementFor(Class<?> aClass, ConfigCache configCache) {
        ConfigTag configTag = annotationFor(aClass, ConfigTag.class);
        if(configTag == null)
            throw bomb(format("Cannot get config tag for {0}",aClass));
        return new Element(configTag.value(), namespaceFor(configTag));
    }

    private static class SubTagXmlFieldWithValue extends XmlFieldWithValue<Object> {

        public SubTagXmlFieldWithValue(Class oringinalClass, Field field, Object value, ConfigCache configCache, final ConfigElementImplementationRegistry registry) {
            super(oringinalClass, field, value, configCache, registry);
        }

        public void populate(Element parent) {
            Element child = elementFor(value.getClass(), configCache);
            parent.addContent(child);
            write(value, child, configCache, registry);
        }

        public boolean alwaysWrite() {
            return false;
        }
    }

    private static class AttributeXmlFieldWithValue extends XmlFieldWithValue<Object> {

        public AttributeXmlFieldWithValue(Class oringinalClass, Field field, Object current, ConfigCache configCache, final ConfigElementImplementationRegistry registry) {
            super(oringinalClass, field, current, configCache, registry);
        }

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

        public boolean alwaysWrite() {
            return field.getAnnotation(ConfigAttribute.class).alwaysWrite();
        }
    }

    private static class ImplicitCollectionXmlFieldWithValue extends XmlFieldWithValue<Collection> {
        public ImplicitCollectionXmlFieldWithValue(
                Class oringinalClass, Field field, Collection value, ConfigCache configCache, final ConfigElementImplementationRegistry registry) {
            super(oringinalClass, field, value, configCache, registry);
        }

        public void populate(Element parent) {
            new CollectionXmlFieldWithValue(value, parent, originalClass, configCache, registry).populate();
        }

        public boolean alwaysWrite() {
            return false;
        }
    }

    private static class CollectionXmlFieldWithValue {
        private final Collection value;
        private final Element parent;
        private final Class originalClass;
        private final ConfigCache configCache;
        private final ConfigElementImplementationRegistry registry;

        public CollectionXmlFieldWithValue(Collection value, Element parent, Class originalClass, ConfigCache configCache, final ConfigElementImplementationRegistry registry) {
            this.value = value;
            this.parent = parent;
            this.originalClass = originalClass;
            this.configCache = configCache;
            this.registry = registry;
        }

        public void populate() {
            Collection defaultCollection = generateDefaultCollection();
            for (XmlFieldWithValue xmlFieldWithValue : allFields(value, configCache, registry)) {
                if (!xmlFieldWithValue.isDefault()) {
                    xmlFieldWithValue.populate(parent);
                }
            }

            for (Object item : value) {
                if (defaultCollection.contains(item)) {
                    continue;
                }
                if (item.getClass().isAnnotationPresent(ConfigCollection.class) && item instanceof Collection) {
                    new ExplicitCollectionXmlFieldWithValue(originalClass, null, (Collection) item, configCache, registry).populate(parent);
                    continue;
                }
                Element childElement = elementFor(item.getClass(), configCache);
                parent.addContent(childElement);
                write(item, childElement, configCache, registry);
            }
        }

        protected Collection generateDefaultCollection() {
            Class<? extends Collection> clazz = value.getClass();
            try {
                return clazz.newInstance();
            } catch (Exception e) {
                throw bomb("Error creating default instance of " + clazz.getName(), e);
            }
        }
    }

    private static class ExplicitCollectionXmlFieldWithValue extends XmlFieldWithValue<Collection> {
        public ExplicitCollectionXmlFieldWithValue(Class oringinalClass, Field field, Collection value, ConfigCache configCache, final ConfigElementImplementationRegistry registry) {
            super(oringinalClass, field, value, configCache, registry);
        }

        public void populate(Element parent) {
            Element containerElement = elementFor(value.getClass(), configCache);
            new CollectionXmlFieldWithValue(value, containerElement, originalClass, configCache, registry).populate();
            parent.addContent(containerElement);
        }

        public boolean alwaysWrite() {
            return false;
        }
    }

    private static class ValueXmlFieldWithValue extends XmlFieldWithValue<Object> {
        private boolean requireCdata;

        public ValueXmlFieldWithValue(Field field, Object value, Class oringinalClass, ConfigCache configCache, final ConfigElementImplementationRegistry registry) {
            super(oringinalClass, field, value, configCache, registry);
            ConfigValue configValue = field.getAnnotation(ConfigValue.class);
            requireCdata = configValue.requireCdata();
        }

        public void populate(Element parent) {
            if (requireCdata) {
                parent.addContent(new CDATA(valueString()));
            } else {
                parent.setText(valueString());
            }
        }

        public boolean alwaysWrite() {
            return false;
        }
    }

}
