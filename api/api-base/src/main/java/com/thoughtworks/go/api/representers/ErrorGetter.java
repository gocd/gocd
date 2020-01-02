/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.config.Validatable;
import com.thoughtworks.go.domain.ConfigErrors;

import java.util.Map;

public class ErrorGetter {

    private final Map<String, String> mapping;

    public ErrorGetter(Map<String, String> mapping) {
        this.mapping = mapping;
    }

    public void toJSON(OutputWriter writer, Validatable entity) {
        entity.errors().forEach((key, value) -> {
            String transformedKey = mapping.get(key);
            if (transformedKey == null) {
                transformedKey = key;
            }
            writer.addChildList(transformedKey, value);
        });
    }

    public void toJSON(OutputWriter writer, ConfigErrors errors) {
        errors.forEach((key, value) -> {
            String transformedKey = mapping.get(key);
            if (transformedKey == null) {
                transformedKey = key;
            }
            writer.addChildList(transformedKey, value);
        });
    }
}
