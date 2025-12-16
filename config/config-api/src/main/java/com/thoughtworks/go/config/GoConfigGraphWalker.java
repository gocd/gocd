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

/**
 * Understands visits all the nodes in the cruise config once using Java reflection
 */
public class GoConfigGraphWalker {
    private final Validatable rootValidatable;

    public GoConfigGraphWalker(Validatable rootValidatable) {
        this.rootValidatable = rootValidatable;
    }

    public static boolean shouldWalk(Object obj) {
        return obj != null && obj.getClass().getName().startsWith("com.thoughtworks");
    }

    public void walk(Validatable.Handler handler) {
        walkSubtree(this.rootValidatable, new ConfigSaveValidationContext(null), handler);
    }

    private void walkSubtree(Object current, ConfigSaveValidationContext context, Validatable.Handler handler) {
        if (!shouldWalk(current)) {
            return;
        }
        if (current instanceof Validatable validatable) {
            handler.handle(validatable, context);
            context = context.withParent(validatable);
        }
        tryWalkCollection(current, context, handler);
        tryWalkFields(current, context, handler);
    }

    private void tryWalkFields(Object current, ConfigSaveValidationContext ctx, Validatable.Handler handler) {
        for (Field field : ConcurrentFieldCache.nonStaticOrSyntheticFieldsFor(current.getClass())) {
            if (isFinal(field) || field.isAnnotationPresent(IgnoreTraversal.class)) {
                continue;
            }
            try {
                field.setAccessible(true);
                walkSubtree(field.get(current), ctx, handler);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private boolean isFinal(Field field) {
        //MCCXL cannot assign value to final fields as it always uses the default constructor. Hence this assumption is OK
        return Modifier.isFinal(field.getModifiers());
    }

    private void tryWalkCollection(Object current, ConfigSaveValidationContext ctx, Validatable.Handler handler) {
        // We can only expect java to honor the contract of data structure interfaces(read: List),
        // and not depend on how they choose to implement it, so we short-circuit at a level that we know will continue to work(bad, but safe)
        if (current instanceof Collection<?> collection) {
            for (Object collectionItem : collection) {
                walkSubtree(collectionItem, ctx, handler);
            }
        }
    }

}
