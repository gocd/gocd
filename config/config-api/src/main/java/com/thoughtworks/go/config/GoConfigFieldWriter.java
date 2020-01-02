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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.parser.GoConfigFieldTypeConverter;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.util.ConfigUtil;
import org.jdom2.Element;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeMismatchException;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static com.thoughtworks.go.util.ExceptionUtils.*;
import static java.text.MessageFormat.format;

public class GoConfigFieldWriter {
    private final ConfigUtil configUtil = new ConfigUtil("magic");
    private Field configField;
    private final Object value;
    private SimpleTypeConverter typeConverter;
    private ConfigCache configCache;
    private final ConfigElementImplementationRegistry registry;

    public GoConfigFieldWriter(Field declaredField, Object value, SimpleTypeConverter converter, ConfigCache configCache, final ConfigElementImplementationRegistry registry) {
        this.configField = declaredField;
        this.value = value;
        this.configField.setAccessible(true);
        this.typeConverter = converter;
        this.configCache = configCache;
        this.registry = registry;
    }

    public GoConfigFieldWriter(Field declaredField, Object value, ConfigCache configCache, final ConfigElementImplementationRegistry registry) {
        this(declaredField, value, new GoConfigFieldTypeConverter(), configCache, registry);
    }

    public Field getConfigField() {
        return configField;
    }

    public String value() {
        if (isAttribute()) {
            return ConfigCache.annotationFor(configField, ConfigAttribute.class).value();
        }
        throw bomb("Unknown type for field " + configField.getName());
    }

    public void setValueIfNotNull(Element e, Object o) {
        configField.setAccessible(true);
        if (isSubtag()) {
            setFieldIfNotNull(configField, o, parseSubtag(e, configField));
        } else if (isAttribute()) {
            setValueFromElementIfAppropriate(e, o);
            bombIfNullAndNotAllowed(configField, o, configField.getAnnotation(ConfigAttribute.class));
        } else if (isConfigValue()) {
            setFieldIfNotNull(configField, o, e.getText());
        }
    }

    private void setValueFromElementIfAppropriate(Element e, Object o) {
        final String val = parseAttribute(e, configField);
        try {
            setFieldIfNotNull(configField, o, val);
        } catch (TypeMismatchException e1) {
            final String message = format("Could not set value [{0}] on Field [{1}] of type [{2}] ", val,
                    configField.getName(), configField.getType());
            throw new RuntimeException(message, e1);
        }
    }

    private void bombIfNullAndNotAllowed(Field field, Object instance, ConfigAttribute attribute) {
        try {
            if (!attribute.allowNull()) {
                bombIfNull(field.get(instance),
                        "Field '" + field.getName() + "' is still set to null. "
                                + "Must give a default value.");
            }
        } catch (IllegalAccessException e) {
            throw bomb("Error getting configField: " + field.getName(), e);
        }
    }

    private void setFieldIfNotNull(Field field, Object instance, Object val) {
        try {
            if (val != null) {
                Object convertedValue = typeConverter.convertIfNecessary(val, configField.getType());
                field.set(instance, convertedValue);
            }
        } catch (IllegalAccessException e) {
            throw bomb("Error setting configField: " + field.getName(), e);
        }
    }

private Collection parseCollection(Element e, Class<?> collectionType) {
        ConfigCollection collection = collectionType.getAnnotation(ConfigCollection.class);
        Class<?> type = collection.value();


        Object o = newInstance(collectionType);
        bombUnless(o instanceof Collection,
                "Must be some sort of list. Was: " + collectionType.getName());

        Collection baseCollection = (Collection) o;
        for (Element childElement : (List<Element>) e.getChildren()) {
            if (isInCollection(childElement, type)) {
                baseCollection.add(parseType(childElement, type));
            }
        }
        bombIf(baseCollection.size() < collection.minimum(),
                "Required at least " + collection.minimum() + " subelements to '" + e.getName() + "'. "
                        + "Found " + baseCollection.size() + ".");
        return baseCollection;
    }

    private boolean isInCollection(Element e, Class<?> type) {
        if (type.isInterface() && type.isAnnotationPresent(ConfigInterface.class)) {
            for (Class<?> implementation : registry.implementersOf(type)) {
                if (configTag(implementation).equals(e.getName())) {
                    return true;
                }
            }
        }
        return configTag(type).equals(e.getName());
    }

    //TODO this is duplicated from magical cruiseconfig loader
    private void parseFields(Element e, Object o) {
        for (GoConfigFieldWriter field : new GoConfigClassWriter(o.getClass(), configCache, registry).getAllFields(o)) {
            field.setValueIfNotNull(e, o);
        }
    }

    private Class<?> typeToGenerate(Element e, Class<?> type) {
        if (type.isInterface() && type.isAnnotationPresent(ConfigInterface.class)) {
            for (Class<?> implementation : registry.implementersOf(type)) {
                if (configTag(implementation).equals(e.getName())) {
                    return implementation;
                }
            }
        } else {
            if (configTag(type).equals(e.getName())) {
                return type;
            }
        }
        throw bomb("Unable to determine type to generate.\n"
                + "Type: " + type.getName() + "\n"
                + "Element: " + configUtil.elementOutput(e));
    }

    public boolean isSubtag() {
        return ConfigCache.isAnnotationPresent(configField, ConfigSubtag.class);
    }

    boolean isImplicitCollection() {
        return isSubtag() && isConfigCollection(configField.getType()) && !ConfigCache.isAnnotationPresent(configField.getType(), ConfigTag.class);
    }

    boolean isConfigCollection() {
        return isSubtag() && isConfigCollection(configField.getType());
    }

    private boolean isConfigCollection(Class<?> type) {
        return ConfigCache.isAnnotationPresent(type, ConfigCollection.class);
    }

    public boolean isAttribute() {
        return ConfigCache.isAnnotationPresent(configField, ConfigAttribute.class);
    }

    public boolean isConfigValue() {
        return ConfigCache.isAnnotationPresent(configField, ConfigValue.class);
    }

    private boolean optionalAndMissingTag(Element e, Field field) {
        ConfigTag tag = configTag(field.getType());
        boolean optional = field.getAnnotation(ConfigSubtag.class).optional();
        boolean isMissingElement = !configUtil.hasChild(e, tag);
        if(!optional && isMissingElement) {
            throw bomb("Non optional tag '" + tag + "' is not in config file. Found: " + configUtil.elementOutput(e));
        }
        return optional && isMissingElement;
    }

    private boolean optionalAndMissingAttribute(Element e, ConfigAttribute attribute) {
        boolean optional = attribute.optional();
        boolean isMissingAttribute = !configUtil.hasAttribute(e, attribute.value());
        if(!optional && isMissingAttribute) {
            throw bomb("Non optional attribute '" + attribute.value() + "' is not in element: " + configUtil.elementOutput(e));
        }
        return optional && isMissingAttribute;
    }

    private Object newInstance(Class<?> type) {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw bomb("Error creating new instance of class " + type.getName(), e);
        }
    }

    private ConfigTag configTag(Class<?> type) {
        bombIf(!ConfigCache.isAnnotationPresent(type, ConfigTag.class), "Invalid type '" + type + "' to autoload. Must have ConfigTag annotation.");
        return ConfigCache.annotationFor(type, ConfigTag.class);
    }

    private String parseAttribute(Element e, Field field) {
        ConfigAttribute attribute = ConfigCache.annotationFor(field, ConfigAttribute.class);
        if (optionalAndMissingAttribute(e, attribute)) {
            return null;
        }
        return configUtil.getAttribute(e, attribute.value());
    }

    private Object parseSubtag(Element e, Field field) {
        Class<?> type = field.getType();
        ConfigTag tag = configTag(type);

        if (optionalAndMissingTag(e, field)) {
            return null;
        }

        Element subElement = configUtil.getChild(e, tag);

        return parseType(subElement, type);
    }

    private Object parseType(Element e, Class<?> type) {
        Object o = isConfigCollection(type) ? parseCollection(e, type) : newInstance(typeToGenerate(e, type));
        parseFields(e, o);
        return o;
    }

    @Override
    public String toString() {
        return "<" + this.getClass().getName() + ": " + this.configField.getName() + ">";
    }

    public boolean isDefault(GoConfigClassWriter configClass) {
        Object defaultObject = configClass.defaultField(configField);
        return Objects.equals(getValue(), defaultObject);
    }

    public Object getValue() {
        return value;
    }

}
