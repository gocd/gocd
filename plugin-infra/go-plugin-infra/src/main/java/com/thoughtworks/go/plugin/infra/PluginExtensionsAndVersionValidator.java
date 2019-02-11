/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

public interface PluginExtensionsAndVersionValidator {
    ValidationResult validate(GoPluginDescriptor descriptor);

    class ValidationResult {
        private final String pluginId;

        public ValidationResult(String pluginId) {
            this.pluginId = pluginId;
        }

        private final List<String> errors = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public List<String> allErrors() {
            return new ArrayList<>(this.errors);
        }

        public String toErrorMessage() {
            return format("Extension incompatibility detected between plugin(%s) and GoCD:\n  %s", pluginId, String.join("\n  ", errors));
        }

        public boolean hasError() {
            return !this.errors.isEmpty();
        }
    }
}


