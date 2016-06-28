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

package com.thoughtworks.go.config.preprocessor;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigAttribute;
import com.thoughtworks.go.config.ConfigAttributeValue;
import com.thoughtworks.go.config.ConfigCollection;
import com.thoughtworks.go.config.ConfigSubtag;
import com.thoughtworks.go.config.ConfigValue;
import com.thoughtworks.go.config.ParamsConfig;
import com.thoughtworks.go.config.ValidationErrorKey;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class ParamResolver {
    private final ClassAttributeCache.FieldCache fieldCache;
    private final ParamHandlerFactory paramHandlerFactory;

    public ParamResolver(ParamHandlerFactory paramHandlerFactory, ClassAttributeCache.FieldCache fieldCache) {
        this.paramHandlerFactory = paramHandlerFactory;
        this.fieldCache = fieldCache;
    }

    public <T> void resolve(T resolvable) {
        ParamResolver resolver = this;
        if (ParamScope.class.isAssignableFrom(resolvable.getClass())) {
            ParamScope newScope = (ParamScope) resolvable;
            resolver = newScope.applyOver(resolver);
        }
        resolveStringLeaves(resolvable, resolver);
        resolveNonStringLeaves(resolvable, resolver);
        resolveNodes(resolvable, resolver);
    }

    public ParamResolver override(ParamsConfig params) {
         return new ParamResolver(paramHandlerFactory.override(params), fieldCache);
     }

    private <T> void resolveNodes(T resolvable, ParamResolver resolver) {
        resolveCollection(resolvable, resolver);
        for (Field node : filterResolvables(resolvable, nodeSelectorPredicate())) {
            try {
                Object subResolvable = node.get(resolvable);
                if (subResolvable != null) {
                    resolver.resolve(subResolvable);
                }
            } catch (IllegalAccessException e) {
                bomb(e);
            }
        }
    }

    private <T> void resolveCollection(Object resolvable, ParamResolver resolver) {
        if (hasAnnotation(resolvable.getClass(), ConfigCollection.class)) {
            for (Object subResolvable : (Collection) resolvable) {
                resolver.resolve(subResolvable);
            }
        }
    }

    private <T> void resolveNonStringLeaves(T resolvable, ParamResolver resolver) {
        for (Field leaf : filterResolvables(resolvable, leafAttributeSelectorPredicate())) {
            try {
                Object nonStringLeaf = leaf.get(resolvable);
                if (nonStringLeaf != null) {
                    Class type = leaf.getType();
                    Field field = getField(leaf, type);
                    field.setAccessible(true);
                    if (field.getType().equals(CaseInsensitiveString.class)) {
                        CaseInsensitiveString cis = (CaseInsensitiveString) field.get(nonStringLeaf);
                        String resolved = resolver.resolveString(resolvable, field, CaseInsensitiveString.str(cis));
                        CaseInsensitiveString value = new CaseInsensitiveString(resolved);
                        leaf.set(resolvable, type.getConstructor(CaseInsensitiveString.class).newInstance(value));
                    } else {//assume it is a string, what else can it be?
                        String resolved = resolver.resolveString(resolvable, field, (String) field.get(nonStringLeaf));
                        leaf.set(resolvable, type.getConstructor(String.class).newInstance(resolved));
                    }
                }
            } catch (Exception e) {
                bomb(e);
            }
        }
    }

    private Field getField(Field leaf, Class type) throws NoSuchFieldException {
        try {
            return type.getDeclaredField(configAttributeValue(leaf).fieldName());
        } catch (NoSuchFieldException e) {
            Class superclass = type.getSuperclass();
            if (superclass == null) {
                throw e;
            }
            return getField(leaf, superclass);
        }
    }

    private <T> void resolveStringLeaves(T resolvable, ParamResolver resolver) {
        for (Field leaf : filterResolvables(resolvable, leafStringSelectorPredicate())) {
            try {
                String preResolved = (String) leaf.get(resolvable);
                if (preResolved != null) {
                    String resolved = resolver.resolveString(resolvable, leaf, preResolved);
                    leaf.set(resolvable, resolved);
                }
            } catch (IllegalAccessException e) {
                bomb(e);
            }
        }
    }

    private String resolveString(Object resolvable, Field field, String preResolved) {
        String fieldName = field.getName();
        if (hasValidationErrorKey(field)) {
            fieldName = validationErrorKey(field).value();
        }
        return new ParamStateMachine().process(preResolved, paramHandlerFactory.createHandler(resolvable, fieldName, preResolved));
    }

    private <T> List<Field> filterResolvables(T resolvable, final NodeSelectorPredicate predicate) {
        List<Field> interpolatableFields = new ArrayList<>();
        for (Field declaredField : getFields(resolvable)) {
            if (predicate.shouldSelect(declaredField)) {
                interpolatableFields.add(declaredField);
                declaredField.setAccessible(true);
            }
        }
        return interpolatableFields;
    }

    private List<Field> getFields(Object resolvable) {
        return fieldCache.valuesFor(resolvable.getClass());
    }

    private NodeSelectorPredicate nodeSelectorPredicate() {
        return new NodeSelectorPredicate() {
            public boolean shouldSelect(Field declaredField) {
                return isConfigSubtag(declaredField) && notSkippable(declaredField);
            }
        };
    }

    private boolean isConfigSubtag(Field declaredField) {
        return hasAnnotation(declaredField, ConfigSubtag.class);
    }

    private NodeSelectorPredicate leafStringSelectorPredicate() {
        return new LeafStringSelectorPredicate();
    }

    private NodeSelectorPredicate leafAttributeSelectorPredicate() {
        return new LeafAttributeSelectorPredicate();
    }

    private static interface NodeSelectorPredicate {
        boolean shouldSelect(Field declaredField);
    }

    private class LeafStringSelectorPredicate implements NodeSelectorPredicate {
        public boolean shouldSelect(Field declaredField) {
            return (isConfigAttribute(declaredField) || isConfigValue(declaredField)) && isString(declaredField) && notSkippable(declaredField);
        }

        boolean isString(Field declaredField) {
            return declaredField.getType().isAssignableFrom(String.class);
        }
    }

    private class LeafAttributeSelectorPredicate implements NodeSelectorPredicate {
        public boolean shouldSelect(Field declaredField) {
            return (isConfigAttribute(declaredField) || isConfigValue(declaredField)) && notSkippable(declaredField) && isConfigAttributeValue(declaredField);
        }

        private boolean isConfigAttributeValue(Field declaredField) {
            return configAttributeValue(declaredField) != null;
        }
    }

    private boolean isConfigAttribute(Field declaredField) {
        return hasAnnotation(declaredField, ConfigAttribute.class);
    }

    private boolean notSkippable(Field declaredField) {
        return !hasAnnotation(declaredField, SkipParameterResolution.class);
    }

    private boolean hasValidationErrorKey(Field declaredField) {
        return hasAnnotation(declaredField, ValidationErrorKey.class);
    }

    private boolean isConfigValue(Field declaredField) {
        return hasAnnotation(declaredField, ConfigValue.class);
    }

    private static ConfigAttributeValue configAttributeValue(Field declaredField) {
        return declaredField.getType().getAnnotation(ConfigAttributeValue.class);
    }

    private static ValidationErrorKey validationErrorKey(Field declaredField) {
        return declaredField.getAnnotation(ValidationErrorKey.class);
    }

    private boolean hasAnnotation(AnnotatedElement configElement, Class annotation) {
        return configElement.isAnnotationPresent(annotation);
    }
}
