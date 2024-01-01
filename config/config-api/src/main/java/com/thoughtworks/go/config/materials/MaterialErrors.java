/*
 * Copyright 2024 Thoughtworks, Inc.
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

package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.config.CaseInsensitiveString;
import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.stream.Collectors;

@UtilityClass
public class MaterialErrors {
    public static String autoUpdateDisplayStatus(boolean autoUpdate) {
        return autoUpdate ? "auto update enabled" : "auto update disabled";
    }

    public static String autoUpdatePipelineErrorDisplay(Map<CaseInsensitiveString, Boolean> pipelineToAutoUpdateValue) {
        if (pipelineToAutoUpdateValue == null || pipelineToAutoUpdateValue.isEmpty()) {
            return "";
        }
        return pipelineToAutoUpdateValue
            .entrySet()
            .stream()
            .map(e -> String.format(" %s (%s)", e.getKey(), autoUpdateDisplayStatus(e.getValue())))
            .collect(Collectors.joining(",\n"));
    }
}
