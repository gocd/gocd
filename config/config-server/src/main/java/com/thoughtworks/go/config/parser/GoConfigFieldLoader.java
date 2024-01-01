/*
 * Copyright 2024 Thoughtworks, Inc.
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
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.springframework.beans.TypeMismatchException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.thoughtworks.go.config.ConfigCache.isAnnotationPresent;
import static com.thoughtworks.go.config.parser.GoConfigAttributeLoader.attributeParser;
import static com.thoughtworks.go.config.parser.GoConfigAttributeLoader.isAttribute;
import static com.thoughtworks.go.config.parser.GoConfigSubtagLoader.isSubtag;
import static com.thoughtworks.go.config.parser.GoConfigSubtagLoader.subtagParser;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.text.MessageFormat.format;

public class GoConfigFieldLoader<T> {
    private static final Map<Field, Boolean> implicits = new ConcurrentHashMap<>();

    private final Element e;
    private final T instance;
    private final Field field;
    private final ConfigCache configCache;
    private final ConfigReferenceElements configReferenceElements;
    private final ConfigElementImplementationRegistry registry;

    public static <T> GoConfigFieldLoader<T> fieldParser(Element e, T instance, Field field, ConfigCache configCache, final ConfigElementImplementationRegistry registry,
                                                         ConfigReferenceElements configReferenceElements) {
        return new GoConfigFieldLoader<>(e, instance, field, configCache, registry, configReferenceElements);
    }

    private GoConfigFieldLoader(Element e, T instance, Field field, ConfigCache configCache, final ConfigElementImplementationRegistry registry, ConfigReferenceElements configReferenceElements) {
        this.e = e;
        this.instance = instance;
        this.field = field;
        this.configCache = configCache;
        this.configReferenceElements = configReferenceElements;
        this.registry = registry;
    }

    public void parse() {
        if (isImplicitCollection()) {
            field.setAccessible(true);
            Object val = GoConfigClassLoader.classParser(e, field.getType(), configCache, new GoCipher(), registry, configReferenceElements).parseImplicitCollection();
            setValue(val);
        } else if (isSubtag(field)) {
            field.setAccessible(true);
            Object val = subtagParser(e, field, configCache, registry, configReferenceElements).parse();
            setValue(val);
        } else if (isAttribute(field)) {
            field.setAccessible(true);
            Object val = attributeParser(e, field).parse(defaultValue());
            setValue(val);
        } else if (isConfigValue()) {
            field.setAccessible(true);
            Object val = e.getText();
            setValue(val);
        } else if (isAnnotationPresent(field, ConfigReferenceElement.class)) {
            field.setAccessible(true);
            ConfigReferenceElement referenceField = field.getAnnotation(ConfigReferenceElement.class);
            Attribute attribute = e.getAttribute(referenceField.referenceAttribute());
            if (attribute == null) {
                bomb(String.format("Expected attribute `%s` to be present for %s.", referenceField.referenceAttribute(), e.getName()));
            }
            String refId = attribute.getValue();
            Object referredObject = configReferenceElements.get(referenceField.referenceCollection(), refId);
            setValue(referredObject);
        }
    }

    private boolean isImplicitCollection() {
        return implicits.computeIfAbsent(field,
                f -> ConfigCache.isAnnotationPresent(f, ConfigSubtag.class) && GoConfigClassLoader.isImplicitCollection(f.getType()));
    }

    private void setValue(Object val) {
        try {
            ConfigAttributeValue configAttributeValue = field.getType().getAnnotation(ConfigAttributeValue.class);
            if (configAttributeValue != null) {
                if (val != null || configAttributeValue.createForNull()) {
                    Constructor<?> constructor = field.getType().getConstructor(String.class);
                    field.set(instance, constructor.newInstance(val));
                }
            } else if (val != null) {
                field.set(instance, GoConfigFieldTypeConverter.forThread().convertIfNecessary(val, field.getType()));
            }
        } catch (IllegalAccessException e) {
            throw bomb("Error setting configField: " + field.getName(), e);
        } catch (TypeMismatchException e) {
            final String message = format("Could not set value [{0}] on field [{1}] of type [{2}] ",
                    val, field.getName(), field.getType());
            throw bomb(message, e);
        } catch (NoSuchMethodException e) {
            throw bomb("Error setting configField: " + field.getName() + " as " + field.getType(), e);
        } catch (InstantiationException | InvocationTargetException e) {
            throw bomb("Error creating configAttribute: " + field.getName() + " as " + field.getType(), e);
        }
    }

    private Object defaultValue() {
        try {
            return field.get(instance);
        } catch (IllegalAccessException e) {
            throw bomb("Error getting configField: " + field.getName(), e);
        }
    }

    public boolean isConfigValue() {
        return isAnnotationPresent(field, ConfigValue.class);
    }
}
