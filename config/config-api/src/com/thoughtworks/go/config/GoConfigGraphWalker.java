/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.thoughtworks.go.config.preprocessor.ClassAttributeCache;

/**
 * @understands visits all the nodes in the cruise config once using Java reflection
 */
public class GoConfigGraphWalker {
    private final Validatable rootValidatable;
    private final ClassAttributeCache.FieldCache fieldCache = new ClassAttributeCache.FieldCache();
    private final ClassAttributeCache.AssignableCache canAssignToValidatableCache = new ClassAttributeCache.AssignableCache();
    private final ClassAttributeCache.AssignableCache canAssignToCollectionCache = new ClassAttributeCache.AssignableCache();

    public static class WalkedObject {
        private final Object obj;

        public WalkedObject(Object obj) {
            this.obj = obj;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            WalkedObject that = (WalkedObject) o;

            return !(obj != null ? obj != that.obj : that.obj != null);

        }

        @Override
        public int hashCode() {
            return obj != null ? obj.hashCode() : 0;
        }

        public boolean shouldWalk() {
            return obj != null && obj.getClass().getName().startsWith("com.thoughtworks");
        }
    }

    public GoConfigGraphWalker(Validatable rootValidatable) {
        this.rootValidatable = rootValidatable;
    }

    public void walk(Handler handler) {
        walkSubtree(this.rootValidatable, new ConfigSaveValidationContext(null), handler);
    }

    private void walkSubtree(Object current, ConfigSaveValidationContext context, Handler handler) {
        WalkedObject walkedObject = new WalkedObject(current);
        if (!walkedObject.shouldWalk()) {
            return;
        }
        if (canAssignToValidatableCache.valuesFor(new AbstractMap.SimpleEntry<Class, Class>(Validatable.class, current.getClass()))) {
            Validatable validatable = (Validatable) current;
            handler.handle(validatable, context);
            context = context.withParent(validatable);
        }
        walkCollection(current, context, handler);
        walkFields(current, context, handler);
    }

    private void walkFields(Object current, ConfigSaveValidationContext ctx, Handler handler) {
        for (Field field : getAllFields(current.getClass())) {
            field.setAccessible(true);
            try {
                Object o = field.get(current);
                if (o == null || isAConstantField(field) || field.isAnnotationPresent(IgnoreTraversal.class)) {
                    continue;
                }
                walkSubtree(o, ctx, handler);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private List<Field> getAllFields(Class klass) {//TODO: if not com.thoughtworks.go don't bother
        return new ArrayList<>(fieldCache.valuesFor(klass));
    }

    private boolean isAConstantField(Field field) {
        int modifiers = field.getModifiers();
        //MCCXL cannot assign value to final fields as it always uses the default constructor. Hence this assumption is OK
        return Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers);
    }

    private void walkCollection(Object current, ConfigSaveValidationContext ctx, Handler handler) {
        // We can only expect java to honor the contract of datastructure interfaces(read: List),
        // and not depend on how they choose to implement it, so we short-circuit at a level that we know will continue to work(bad, but safe)
        // TODO: do java.util.Map when needed, not handled yet, but its a simple EntrySet walk
        if (canAssignToCollectionCache.valuesFor(new AbstractMap.SimpleEntry<Class, Class>(Collection.class, current.getClass()))) {
            Collection collection = (Collection) current;
            for (Object collectionItem : collection) {
                walkSubtree(collectionItem, ctx, handler);
            }
        }
    }

    public static interface Handler {
        void handle(Validatable validatable, ValidationContext ctx);
    }
}
