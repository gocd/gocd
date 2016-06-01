/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config.parser;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.config.ConfigAttributeValue;
import com.thoughtworks.go.config.ConfigCache;
import com.thoughtworks.go.config.ConfigReferenceElement;
import com.thoughtworks.go.config.ConfigSubtag;
import com.thoughtworks.go.config.ConfigValue;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.security.GoCipher;
import org.jdom.Element;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeMismatchException;

import static com.thoughtworks.go.config.ConfigCache.isAnnotationPresent;
import static com.thoughtworks.go.config.parser.GoConfigAttributeLoader.attributeParser;
import static com.thoughtworks.go.config.parser.GoConfigAttributeLoader.isAttribute;
import static com.thoughtworks.go.config.parser.GoConfigSubtagLoader.isSubtag;
import static com.thoughtworks.go.config.parser.GoConfigSubtagLoader.subtagParser;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.text.MessageFormat.format;

public class GoConfigFieldLoader<T> {
    private static Map<Field, Boolean> implicts = new HashMap<>();
    private static final SimpleTypeConverter typeConverter = new GoConfigFieldTypeConverter();

    private final Element e;
    private final T instance;
    private final Field field;
    private ConfigCache configCache;
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
        field.setAccessible(true);
        this.registry = registry;
    }

    public void parse() {
        if (isImplicitCollection()) {
            Object val = GoConfigClassLoader.classParser(e, field.getType(), configCache, new GoCipher(), registry, configReferenceElements).parseImplicitCollection();
            setValue(val);
        } else if (isSubtag(field)) {
            Object val = subtagParser(e, field, configCache, registry, configReferenceElements).parse();
            setValue(val);
        } else if (isAttribute(field)) {
            Object val = attributeParser(e, field).parse(defaultValue());
            setValue(val);
        } else if (isConfigValue()) {
            Object val = e.getText();
            setValue(val);
        } else if (isAnnotationPresent(field, ConfigReferenceElement.class)) {
            ConfigReferenceElement referenceField = field.getAnnotation(ConfigReferenceElement.class);
            String refId = e.getAttribute(referenceField.referenceAttribute()).getValue();
            Object referredObject = configReferenceElements.get(referenceField.referenceCollection(), refId);
            setValue(referredObject);
        }
    }

    private boolean isImplicitCollection() {
        if (!implicts.containsKey(field)) {
            implicts.put(field, ConfigCache.isAnnotationPresent(field, ConfigSubtag.class)
                    && GoConfigClassLoader.isImplicitCollection(field.getType(), configCache));
        }
        return implicts.get(field);
    }

    private void setValue(Object val) {
        try {
            ConfigAttributeValue configAttributeValue = field.getType().getAnnotation(ConfigAttributeValue.class);
            if (configAttributeValue != null) {
                if (val != null || configAttributeValue.createForNull()) {
                    Constructor<?> constructor = field.getType().getConstructor(String.class);
                    field.set(instance, constructor.newInstance(new Object[]{val}));
                }
            } else if (val != null) {
                Object convertedValue = typeConverter.convertIfNecessary(val, field.getType());
                field.set(instance, convertedValue);
            }
        } catch (IllegalAccessException e) {
            throw bomb("Error setting configField: " + field.getName(), e);
        } catch (TypeMismatchException e) {
            final String message = format("Could not set value [{0}] on Field [{1}] of type [{2}] ",
                    val, field.getName(), field.getType());
            throw bomb(message, e);
        } catch (NoSuchMethodException e) {
            throw bomb("Error setting configField: " + field.getName() + " as " + field.getType(), e);
        } catch (InstantiationException e) {
            throw bomb("Error creating configAttribute: " + field.getName() + " as " + field.getType(), e);
        } catch (InvocationTargetException e) {
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
