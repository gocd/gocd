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
package com.thoughtworks.go.plugin.access;

import java.util.Map;

public class DefaultPluginInteractionCallback<T> implements PluginInteractionCallback<T> {
    @Override
    public String requestBody(String resolvedExtensionVersion) {
        return null;
    }

    @Override
    public Map<String, String> requestParams(String resolvedExtensionVersion) {
        return null;
    }

    @Override
    public Map<String, String> requestHeaders(String resolvedExtensionVersion) {
        return null;
    }

    @Override
    public T onSuccess(String responseBody, Map<String, String> responseHeaders, String resolvedExtensionVersion) {
        return null;
    }

    @Override
    public void onFailure(int responseCode, String responseBody, String resolvedExtensionVersion) {
    }
}
