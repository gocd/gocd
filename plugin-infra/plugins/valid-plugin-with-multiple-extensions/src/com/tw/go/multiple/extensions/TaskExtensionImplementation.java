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

package com.tw.go.multiple.extensions;

import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

import java.util.Collections;

@Extension
public class TaskExtensionImplementation implements GoPlugin {
    private static int numberOfCallsToPluginIdentifier = 0;
    private static int numberOfCallsToHandle = 0;
    private static int numberOfCallsToInitialize = 0;

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
        System.setProperty("valid-plugin-with-multiple-extensions.task_extension.initialize_accessor.count", String.valueOf(++numberOfCallsToInitialize));
        System.setProperty("valid-plugin-with-multiple-extensions.task_extension.initialize_accessor.value", String.valueOf(System.currentTimeMillis()));
    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest goPluginApiRequest) {
        System.setProperty("valid-plugin-with-multiple-extensions.task_extension.request.count", String.valueOf(++numberOfCallsToHandle));
        System.setProperty("valid-plugin-with-multiple-extensions.task_extension.request.name", goPluginApiRequest.requestName());
        System.setProperty("valid-plugin-with-multiple-extensions.task_extension.request.body", goPluginApiRequest.requestBody());

        return new DefaultGoPluginApiResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, "{}");
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        System.setProperty("valid-plugin-with-multiple-extensions.task_extension.plugin_identifier.count", String.valueOf(++numberOfCallsToPluginIdentifier));
        System.setProperty("valid-plugin-with-multiple-extensions.task_extension.plugin_identifier.value", String.valueOf(System.currentTimeMillis()));

        return new GoPluginIdentifier("task", Collections.singletonList("1.0"));
    }
}
