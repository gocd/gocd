/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config.update;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.config.IgnoreTraversal;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.domain.ConfigErrors;
import org.apache.commons.collections4.CollectionUtils;

public class ErrorCollector {

    public static void collectGlobalErrors(List<String> errorBucket, List<ConfigErrors> listOfConfigErrors) {
        for (ConfigErrors configErrors : listOfConfigErrors) {
            errorBucket.addAll(configErrors.getAll());
        }
    }

    public static void collectFieldErrors(Map<String, List<String>> errorBucket, String prefix, Object subject) {
        Field[] fields = subject.getClass().getDeclaredFields();
        for (Field field : fields) {
            String fieldName = field.getName();
            Object fieldValue= null;
            try {
                field.setAccessible(true);
                fieldValue = field.get(subject);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
//            Object fieldValue = ReflectionUtil.getField(subject, fieldName);
            if (isAConstantField(field) || field.isAnnotationPresent(IgnoreTraversal.class)) {
                continue;
            }
            if (fieldValue != null && fieldValue instanceof Collection && isConfigObject(fieldValue)) { // collection to be walked
                Collection collection = (Collection) fieldValue;
                int index = 0;
                for (Object collectionItem : collection) {
                    collectFieldErrors(errorBucket, prefix + "[" + fieldName + "][" + (index++) + "]", collectionItem);
                }
            } else if (isConfigObject(fieldValue)) { // object to be walked
                collectFieldErrors(errorBucket, prefix + "[" + fieldName + "]", fieldValue);
            } else { // basic field
                if (subject instanceof Validatable) {
                    ConfigErrors configErrors = ((Validatable) subject).errors();
                    if (configErrors != null && CollectionUtils.isNotEmpty( configErrors.getAllOn(fieldName))) {
                        errorBucket.put(prefix + "[" + fieldName + "]", configErrors.getAllOn(fieldName));
                    }
                }
            }
        }
    }

    private static boolean isAConstantField(Field field) {
        int modifiers = field.getModifiers();
        return Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers);
    }

    private static boolean isConfigObject(Object obj) {
        return obj != null && obj.getClass().getName().startsWith("com.thoughtworks");
    }
}
