/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.remote;

import org.junit.Test;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class PartialConfigTest {
    @Test
    public void ensureAllElementsUnderPartialConfigAreMarkedAsSerializable() {
        List<Class> walked = new ArrayList<>();
        assertAllFieldsInTheTreeAreSerializable(walked, PartialConfig.class);
    }

    private void assertAllFieldsInTheTreeAreSerializable(List<Class> walked, Class clazz) {
        if (clazz.isPrimitive()) return;
        assertThat(String.format("'%s' does not implement Serializable", clazz.getName()), Serializable.class.isAssignableFrom(clazz), is(true));
        if (walked.contains(clazz)) return;
        walked.add(clazz);
        System.out.println(String.format("Checking '%s'..", clazz.getName()));
        handleCollections(walked, clazz);

        for (Field declaredField : clazz.getDeclaredFields()) {
            if (declaredField.getType().isPrimitive()) continue;
            if (Modifier.isTransient(declaredField.getModifiers()) || Modifier.isStatic(declaredField.getModifiers()))
                continue;
            System.out.println(String.format("Checking '%s.%s'..", declaredField.getDeclaringClass().getName(), declaredField.getName()));
            assertThat(String.format("'%s.%s' does not implement Serializable", declaredField.getDeclaringClass().getName(), declaredField.getName()),
                    Serializable.class.isAssignableFrom(declaredField.getType()), is(true));

            assertAllFieldsInTheTreeAreSerializable(walked, declaredField.getType());
            handleCollections(walked, declaredField.getType());
        }
    }

    private void handleCollections(List<Class> walked, Class clazz) {
        if (clazz.isPrimitive()) return;
        if (ArrayList.class.isAssignableFrom(clazz) || Iterable.class.isAssignableFrom(clazz)) {
            if (clazz.getGenericSuperclass() != null && ParameterizedType.class.isAssignableFrom(clazz.getGenericSuperclass().getClass())) {
                Class genericClassUsedByCollection = (Class) ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments()[0];
                assertAllFieldsInTheTreeAreSerializable(walked, genericClassUsedByCollection);
            }
            Type[] genericInterfaces = clazz.getGenericInterfaces();
            for (Type genericInterface : genericInterfaces) {
                if (!ParameterizedType.class.isAssignableFrom(genericInterface.getClass())) continue;
                Class genericInterfaceUsedByCollection = (Class) ((ParameterizedType) genericInterface).getActualTypeArguments()[0];
                assertAllFieldsInTheTreeAreSerializable(walked, genericInterfaceUsedByCollection);
            }
        }
    }
}