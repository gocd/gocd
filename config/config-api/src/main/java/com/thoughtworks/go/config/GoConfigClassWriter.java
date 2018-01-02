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
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static org.apache.commons.lang.StringUtils.capitalize;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class GoConfigClassWriter {
    private Class<?> aClass;
    private ConfigCache configCache;
    private final ConfigElementImplementationRegistry registry;

    public GoConfigClassWriter(Class aClass, ConfigCache configCache, final ConfigElementImplementationRegistry registry) {
        this.aClass = aClass;
        this.configCache = configCache;
        this.registry = registry;
    }

    public List<GoConfigFieldWriter> getAllFields(Object render) {
        ArrayList<GoConfigFieldWriter> fields = new ArrayList<>();
        for (Field declaredField : configCache.getFieldCache().valuesFor(aClass)) {
            try {
                declaredField.setAccessible(true);
                fields.add(new GoConfigFieldWriter(declaredField, declaredField.get(render), configCache, registry));
            } catch (IllegalAccessException e) {
                throw bomb(e);
            }
        }
        return fields;
    }

    public String label() {
        ConfigTag configTag = ConfigCache.annotationFor(aClass, ConfigTag.class);
        return isEmpty(configTag.label()) ? capitalize(configTag.value()) : configTag.label();
    }

    public Object defaultField(Field f) {
        try {
            Object o = aClass.newInstance();
            return f.get(o);
        } catch (Exception e) {
            return null;
        }
    }
}
