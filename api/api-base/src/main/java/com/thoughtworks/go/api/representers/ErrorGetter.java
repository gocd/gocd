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

package com.thoughtworks.go.api.representers;

import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.spark.RequestContext;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class ErrorGetter implements BiFunction<Validatable, RequestContext, Map> {

    private final Map<String, String> mapping;

    public ErrorGetter(Map<String, String> mapping) {
        this.mapping = mapping;
    }

    @Override
    public Map apply(Validatable entity, RequestContext requestContext) {
        LinkedHashMap<String, List<String>> transformedErrors = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : entity.errors().entrySet()) {
            String transformedKey = mapping.get(entry.getKey());
            if (transformedKey == null) {
                transformedKey = entry.getKey();
            }

            transformedErrors.put(transformedKey, entry.getValue());
        }

        return transformedErrors;
    }
}
