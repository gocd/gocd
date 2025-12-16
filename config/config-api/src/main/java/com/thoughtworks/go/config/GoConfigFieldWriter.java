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

import java.lang.reflect.Field;
import java.util.Optional;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class GoConfigFieldWriter {
    private final Field configField;
    private final Object parent;

    public GoConfigFieldWriter(Field declaredField, Object parent) {
        this.configField = declaredField;
        this.parent = parent;
    }

    public Field getConfigField() {
        return configField;
    }

    public String description() {
        return Optional.ofNullable(configField.getAnnotation(ConfigAttribute.class)).map(ConfigAttribute::value)
            .orElseThrow(() -> bomb("Unknown type for field " + configField.getName()));
    }

    public boolean isSubtag() {
        return configField.isAnnotationPresent(ConfigSubtag.class);
    }

    boolean isImplicitCollection() {
        return isConfigCollection() && !configField.getType().isAnnotationPresent(ConfigTag.class);
    }

    boolean isConfigCollection() {
        return isSubtag() && isConfigCollection(configField.getType());
    }

    private boolean isConfigCollection(Class<?> type) {
        return type.isAnnotationPresent(ConfigCollection.class);
    }

    public boolean isAttribute() {
        return configField.isAnnotationPresent(ConfigAttribute.class);
    }

    public boolean isConfigValue() {
        return configField.isAnnotationPresent(ConfigValue.class);
    }

    @Override
    public String toString() {
        return "<" + this.getClass().getName() + ": " + this.configField.getName() + ">";
    }

    public Object getValue() {
        configField.setAccessible(true);
        try {
            return configField.get(parent);
        } catch (IllegalAccessException e) {
            return new RuntimeException(e);
        }
    }

}
