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

import com.thoughtworks.go.config.preprocessor.ClassAttributeCache;
import org.springframework.stereotype.Component;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

@Component
public class ConfigCache {

    private ClassAttributeCache.FieldCache fieldCache = new ClassAttributeCache.FieldCache();
    private ConfigCacheStore store;

    public static boolean isAnnotationPresent(AnnotatedElement element, Class<? extends Annotation> annotationClass) {
        return element.isAnnotationPresent(annotationClass);
    }

    public static <T extends Annotation> T annotationFor(AnnotatedElement klass, Class<T> annotationClass) {
         return klass.getAnnotation(annotationClass);
    }

    public void put(String md5, GoConfigHolder holder) {
        this.store.put(md5, holder);
    }

    public GoConfigHolder get(String md5) {
        if (md5 == null) {
            return null;
        }
        return (GoConfigHolder) this.store.get(md5);
    }

    public ClassAttributeCache.FieldCache getFieldCache() {
        return fieldCache;
    }

    public void setConfigCacheStore(ConfigCacheStore store) {
        this.store = store;
    }
}
