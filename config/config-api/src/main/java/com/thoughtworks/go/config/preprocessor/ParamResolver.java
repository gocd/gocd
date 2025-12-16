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
package com.thoughtworks.go.config.preprocessor;

import com.thoughtworks.go.config.*;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class ParamResolver {
    private final ParamHandlerFactory paramHandlerFactory;

    public ParamResolver(ParamHandlerFactory paramHandlerFactory) {
        this.paramHandlerFactory = paramHandlerFactory;
    }

    public <T> void resolve(T resolvable) {
        ParamResolver resolver = this;
        if (resolvable instanceof ParamScope newScope) {
            resolver = newScope.applyOver(resolver);
        }
        resolveStringLeaves(resolvable, resolver);
        resolveNonStringLeaves(resolvable, resolver);
        resolveNodes(resolvable, resolver);
    }

    public ParamResolver override(ParamsConfig params) {
         return new ParamResolver(paramHandlerFactory.override(params));
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

    private <T> void resolveCollection(T resolvable, ParamResolver resolver) {
        if (resolvable.getClass().isAnnotationPresent(ConfigCollection.class)) {
            for (Object subResolvable : (Collection<?>) resolvable) {
                resolver.resolve(subResolvable);
            }
        }
    }

    private <T> void resolveNonStringLeaves(T resolvable, ParamResolver resolver) {
        for (Field leaf : filterResolvables(resolvable, leafAttributes())) {
            try {
                Object nonStringLeaf = leaf.get(resolvable);
                if (nonStringLeaf != null) {
                    Class<?> type = leaf.getType();
                    Field field = ConfigAttributeValue.Resolver.resolveAccessibleField(type, configAttributeValueFor(leaf));
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

    private <T> void resolveStringLeaves(T resolvable, ParamResolver resolver) {
        for (Field leaf : filterResolvables(resolvable, leafStrings())) {
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

    private <T> List<Field> filterResolvables(T resolvable, final Predicate<Field> predicate) {
        List<Field> interpolatableFields = new ArrayList<>();
        for (Field field : ConcurrentFieldCache.nonStaticOrSyntheticFieldsFor(resolvable.getClass())) {
            if (predicate.test(field)) {
                interpolatableFields.add(field);
                field.setAccessible(true);
            }
        }
        return interpolatableFields;
    }

    private Predicate<Field> nodeSelectorPredicate() {
        return field -> isConfigSubtag(field) && notSkippable(field);
    }

    private Predicate<Field> leafStrings() {
        return field -> isLeafConfigValue(field) && isString(field);
    }

    private Predicate<Field> leafAttributes() {
        return field -> (isConfigAttribute(field) || isConfigValue(field)) && notSkippable(field) && isConfigAttributeValueType(field);
    }

    private boolean isLeafConfigValue(Field field) {
        return (isConfigAttribute(field) || isConfigValue(field)) && notSkippable(field);
    }

    private static boolean isString(Field field) {
        return field.getType().isAssignableFrom(String.class);
    }

    private boolean isConfigSubtag(Field field) {
        return field.isAnnotationPresent(ConfigSubtag.class);
    }

    private boolean isConfigAttribute(Field field) {
        return field.isAnnotationPresent(ConfigAttribute.class);
    }

    private boolean notSkippable(Field field) {
        return !field.isAnnotationPresent(SkipParameterResolution.class);
    }

    private boolean hasValidationErrorKey(Field field) {
        return field.isAnnotationPresent(ValidationErrorKey.class);
    }

    private boolean isConfigValue(Field field) {
        return field.isAnnotationPresent(ConfigValue.class);
    }

    private static boolean isConfigAttributeValueType(Field field) {
        return field.getType().isAnnotationPresent(ConfigAttributeValue.class);
    }

    private static ConfigAttributeValue configAttributeValueFor(Field field) {
        return field.getType().getAnnotation(ConfigAttributeValue.class);
    }

    private static ValidationErrorKey validationErrorKey(Field field) {
        return field.getAnnotation(ValidationErrorKey.class);
    }

}
