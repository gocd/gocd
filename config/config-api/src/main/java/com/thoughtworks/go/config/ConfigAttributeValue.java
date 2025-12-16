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

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Understands mapping cruise-config attribute mapping to domain object
 */
@Retention(RUNTIME)
@Target(TYPE)
public @interface ConfigAttributeValue {
    String fieldName();
    boolean createForNull() default true;

    @UtilityClass
    class Resolver {
        public static @NotNull Field resolveAccessibleField(@NotNull Class<?> clazz, @NotNull ConfigAttributeValue attributeValue) throws NoSuchFieldException {
            try {
                Field field = clazz.getDeclaredField(attributeValue.fieldName());
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException e) {
                Class<?> superclass = clazz.getSuperclass();
                if (superclass == null) {
                    throw e;
                }
                return resolveAccessibleField(superclass, attributeValue);
            }
        }
    }
}
