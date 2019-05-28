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
package com.thoughtworks.go.plugin.infra.commons;

import java.util.HashMap;
import java.util.Map;

public class PluginUploadResponse {

    private Map<Integer, String> errors;
    private String successMessage;

    public PluginUploadResponse(String successMessage, Map<Integer, String> errors) {
        this.errors = errors;
        this.successMessage = successMessage;
    }

    public static PluginUploadResponse create(boolean isSuccess, String successMessage, Map<Integer, String> errors) {
        if (isSuccess) return new PluginUploadResponse(successMessage, new HashMap<>());
        return new PluginUploadResponse("", errors);
    }

    public String success() {
        return successMessage;
    }

    public Map<Integer, String> errors() {
        return errors;
    }

    public boolean isSuccess() {
        return errors.isEmpty();
    }
}
