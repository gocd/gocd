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

package com.thoughtworks.go.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class GoConfigParallelGraphWalker {
    private Object rawConfig;
    private Object configWithErrors;

    public GoConfigParallelGraphWalker(Object from, Object to) {
        this.rawConfig = to;
        this.configWithErrors = from;
    }

    public void walk(Handler handler) {
        walkSubtree(this.rawConfig, this.configWithErrors, handler);
    }

    private void walkSubtree(Object raw, Object withErrors, Handler handler) {
        GoConfigGraphWalker.WalkedObject walkedObject = new GoConfigGraphWalker.WalkedObject(raw);
        if (!walkedObject.shouldWalk()) {
            return;
        }
        if (Validatable.class.isAssignableFrom(raw.getClass())) {
            handler.handle((Validatable)raw, (Validatable)withErrors);
        }
        walkCollection(raw, withErrors, handler);
        walkFields(raw, withErrors, handler);
    }

    private void walkFields(Object raw, Object withErrors, Handler handler) {
        for (Field field : getAllFields(raw.getClass())) {
            field.setAccessible(true);
            try {
                Object rawObject = field.get(raw);
                Object withErrorsObject = field.get(withErrors);
                if (rawObject == null || withErrorsObject == null || isAConstantField(field) || field.isAnnotationPresent(IgnoreTraversal.class)) {
                    continue;
                }
                walkSubtree(rawObject, withErrorsObject, handler);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private List<Field> getAllFields(Class klass) {
        List<Field> declaredFields = new ArrayList<>(Arrays.asList(klass.getDeclaredFields()));
        Class<?> superKlass = klass.getSuperclass();
        if (superKlass != null) {
            declaredFields.addAll(getAllFields(superKlass));
        }
        return declaredFields;
    }

    private boolean isAConstantField(Field field) {
        int modifiers = field.getModifiers();
        //MCCXL cannot assign value to final fields as it always uses the default constructor. Hence this assumption is OK
        return Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers);
    }

    private void walkCollection(Object raw, Object withErrors, Handler handler) {
        if (Collection.class.isAssignableFrom(raw.getClass())) {
            Object[] rawCollection = ((Collection) raw).toArray();
            Object[] withErrorsCollection = ((Collection) withErrors).toArray();
            for (Object rawObject : rawCollection) {
                Object matchingObject = findMatchingObject(withErrorsCollection, rawObject);
                if (matchingObject != null) {
                    walkSubtree(rawObject, matchingObject, handler);
                }
            }
        }
    }

    private Object findMatchingObject(Object[] collectionToSearchIn, Object objectToFindEquivalentOf) {
        if (objectToFindEquivalentOf == null) {
            return null;
        }

        for (Object candidate : collectionToSearchIn) {
            if (objectToFindEquivalentOf.equals(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    public static interface Handler {
        void handle(Validatable rawConfig, Validatable configWithErrors);
    }
}
