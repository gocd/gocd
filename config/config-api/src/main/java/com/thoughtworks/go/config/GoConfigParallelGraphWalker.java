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

import com.thoughtworks.go.config.preprocessor.ConcurrentFieldCache;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Objects;

import static com.thoughtworks.go.config.GoConfigGraphWalker.shouldWalk;

public class GoConfigParallelGraphWalker {
    private final Object rawConfig;
    private final Object configWithErrors;

    public GoConfigParallelGraphWalker(Object from, Object to) {
        this.rawConfig = to;
        this.configWithErrors = from;
    }

    public void walk(Handler handler) {
        walkSubtree(this.rawConfig, this.configWithErrors, handler);
    }

    private void walkSubtree(Object raw, Object withErrors, Handler handler) {
        if (!shouldWalk(raw) || !shouldWalk(withErrors)) {
            return;
        }
        walkValidatable(raw, withErrors, handler);
        walkCollection(raw, withErrors, handler);
        walkFields(raw, withErrors, handler);
    }

    private void walkValidatable(Object raw, Object withErrors, Handler handler) {
        if (raw instanceof Validatable rawV && withErrors instanceof Validatable withErrorsV) {
            handler.handle(rawV, withErrorsV);
        }
    }

    private void walkFields(Object raw, Object withErrors, Handler handler) {
        for (Field field : ConcurrentFieldCache.nonStaticOrSyntheticFieldsFor(raw.getClass())) {
            if (isFinal(field) || field.isAnnotationPresent(IgnoreTraversal.class)) {
                continue;
            }
            try {
                field.setAccessible(true);
                walkSubtree(field.get(raw), field.get(withErrors), handler);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean isFinal(Field field) {
        // MCCXL cannot assign value to final fields as it always uses the default constructor. Hence this assumption is OK
        return Modifier.isFinal(field.getModifiers());
    }

    private void walkCollection(Object raw, Object withErrors, Handler handler) {
        if (raw instanceof Collection<?> rawCollection && withErrors instanceof Collection<?> withErrorsCollection) {
            rawCollection.stream()
                .filter(Objects::nonNull)
                .forEach(rawObject -> withErrorsCollection.stream()
                    .filter(rawObject::equals)
                    .findFirst()
                    .ifPresent(matchingObject -> walkSubtree(rawObject, matchingObject, handler))
                );
        }
    }

    public interface Handler {
        void handle(Validatable rawConfig, Validatable configWithErrors);
    }
}
