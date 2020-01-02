/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config.parser;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ConfigUtil;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.thoughtworks.go.config.ConfigCache.annotationFor;
import static com.thoughtworks.go.config.ConfigCache.isAnnotationPresent;
import static com.thoughtworks.go.config.parser.GoConfigFieldLoader.fieldParser;
import static com.thoughtworks.go.util.ExceptionUtils.*;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class GoConfigClassLoader<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoConfigClassLoader.class);
    private final ConfigUtil configUtil = new ConfigUtil("magic");
    private final Element e;
    private final Class<T> aClass;
    private ConfigCache configCache;
    private final GoCipher goCipher;
    private ConfigElementImplementationRegistry registry;
    private final ConfigReferenceElements configReferenceElements;

public static <T> GoConfigClassLoader<T> classParser(Element e, Class<T> aClass, ConfigCache configCache, GoCipher goCipher, final ConfigElementImplementationRegistry registry, ConfigReferenceElements configReferenceElements) {
        return new GoConfigClassLoader<>(e, aClass, configCache, goCipher, registry, configReferenceElements);
    }

    private GoConfigClassLoader(Element e, Class<T> aClass, ConfigCache configCache, GoCipher goCipher, final ConfigElementImplementationRegistry registry, ConfigReferenceElements configReferenceElements) {
        this.e = e;
        this.aClass = aClass;
        this.configCache = configCache;
        this.goCipher = goCipher;
        this.registry = registry;
        this.configReferenceElements = configReferenceElements;
    }

    public T parse() {
        bombUnless(atElement(),
                "Unable to parse element <" + e.getName() + "> for class " + aClass.getSimpleName());
        T o = createInstance();
        if (isAnnotationPresent(aClass, ConfigReferenceCollection.class)) {
            ConfigReferenceCollection referenceCollection = aClass.getAnnotation(ConfigReferenceCollection.class);
            String collectionName = referenceCollection.collectionName();
            String idFieldName = referenceCollection.idFieldName();
            if (e.getAttribute(idFieldName) != null) {
                String id = e.getAttribute(idFieldName).getValue();
                configReferenceElements.add(collectionName, id, o);
            }
        }
        for (GoConfigFieldLoader field : allFields(o)) {
            field.parse();
        }
        if (isConfigCollection()) {
            parseCollection((Collection) o);
        }
        //check whether there are public PostConstruct methods and call them
        postConstruct(o);
        return o;
    }

    private void postConstruct(T o) {
        Method[] methods = o.getClass().getMethods();
        for (Method method : methods) {
            if (isAnnotationPresent(method, PostConstruct.class)) {
                try {
                    method.invoke(o);
                } catch (Exception e) {
                    LOGGER.error("Failed to save config: ", e);
                    throw bomb(e);
                }
            }
        }
    }

    public Collection parseImplicitCollection() {
        Collection collection = (Collection) createInstance();
        parseCollection(collection);
        return collection;
    }

private void parseCollection(Collection collection) {
        ConfigCollection collectionAnnotation = annotationFor(aClass, ConfigCollection.class);
        Class<?> elementType = collectionAnnotation.value();

        for (Element childElement : (List<Element>) e.getChildren()) {
            if (isInCollection(childElement, elementType)) {
                Class<?> collectionType = findConcreteType(childElement, elementType);
                collection.add(classParser(childElement, collectionType, configCache, new GoCipher(), registry, configReferenceElements).parse());
            }
        }
        int minimumSize = collectionAnnotation.minimum();
        bombIf(collection.size() < minimumSize,
                "Required at least " + minimumSize + " subelements to '" + e.getName() + "'. "
                        + "Found " + collection.size() + ".");
    }

    private <I> List<GoConfigFieldLoader> allFields(I o) {
        List<GoConfigFieldLoader> fields = new ArrayList<>();
        List<Field> allFields = configCache.getFieldCache().valuesFor(o.getClass());
        for (Field field : allFields) {
            fields.add(fieldParser(e, o, field, configCache, registry, configReferenceElements));
        }
        return fields;
    }

    private boolean atElement() {
        AttributeAwareConfigTag attributeAwareConfigTag = annotationFor(aClass, AttributeAwareConfigTag.class);
        if (attributeAwareConfigTag != null) {
            final String attribute = attributeAwareConfigTag.attribute();
            bombIf(isBlank(attribute), format("Type '%s' has invalid configuration for @AttributeAwareConfigTag. It must have `attribute` with non blank value.", aClass.getName()));
            bombIf(!hasAttribute(attribute), format("Expected attribute `%s` to be present for %s.", attribute, configUtil.elementOutput(e)));
            return configUtil.atTag(e, attributeAwareConfigTag.value());
        }

        ConfigTag configTag = annotationFor(aClass, ConfigTag.class);

        if (configTag == null) {
            return false;
        }

        String tag = configTag.value();
        return configUtil.atTag(e, tag);
    }

    private boolean hasAttribute(String attribute) {
        return e.getAttribute(attribute) != null;
    }

    private T createInstance() {
        return ConfigElementInstantiator.instantiateConfigElement(this.goCipher, typeToGenerate(e));
    }

private Class<T> typeToGenerate(Element e) {
        if (isImplicitCollection(aClass, configCache)) {
            return aClass;
        }
        Class<T> type = (Class<T>) findConcreteType(e, aClass);
        if (type == null) {
            bombIfNull(type, format("Unable to determine type to generate. Type: %s Element: %s", aClass.getName(), configUtil.elementOutput(e)));
        }
        return type;
    }

    public static boolean compare(Element e, Class<?> implementation, ConfigCache configCache) {
        final AttributeAwareConfigTag attributeAwareConfigTag = annotationFor(implementation, AttributeAwareConfigTag.class);

        if (attributeAwareConfigTag != null) {
            return compareAttributeAwareConfigTag(e, attributeAwareConfigTag);
        }

        ConfigTag configTag = configTag(implementation, configCache);
        return configTag.value().equals(e.getName()) && e.getNamespace().getURI().equals(configTag.namespaceURI());
    }

    private static boolean compareAttributeAwareConfigTag(Element e, AttributeAwareConfigTag attributeAwareConfigTag) {
        return attributeAwareConfigTag.value().equals(e.getName()) &&
                attributeAwareConfigTag.attributeValue().equals(e.getAttributeValue(attributeAwareConfigTag.attribute())) &&
                e.getNamespace().getURI().equals(attributeAwareConfigTag.namespaceURI());
    }

    static boolean isImplicitCollection(Class type, ConfigCache configCache) {
        return isAnnotationPresent(type, ConfigCollection.class) && !(isAnnotationPresent(type, ConfigTag.class) || isAnnotationPresent(type, AttributeAwareConfigTag.class));
    }

    public static ConfigTag configTag(Class<?> type, ConfigCache configCache) {
        ConfigTag tag = annotationFor(type, ConfigTag.class);
        bombIfNull(tag, "Invalid type '" + type + "' to autoload. Must have ConfigTag annotation.");
        return tag;
    }

    private boolean isConfigCollection() {
        return isAnnotationPresent(aClass, ConfigCollection.class);
    }

    private boolean isInCollection(Element e, Class<?> type) {
        return findConcreteType(e, type) != null;
    }

    private Class<?> findConcreteType(Element e, Class<?> type) {
        if (type.isInterface() && isAnnotationPresent(type, ConfigInterface.class)) {
            for (Class<?> implementation : registry.implementersOf(type)) {
                if (compare(e, implementation, configCache)) {
                    return implementation;
                }
            }
        } else if (compare(e, type, configCache)) {
            return type;
        }
        return null;
    }
}
