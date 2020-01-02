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
package com.thoughtworks.go.plugin.access.analytics;

import com.thoughtworks.go.plugin.access.analytics.V1.AnalyticsMessageConverterV1;
import com.thoughtworks.go.plugin.access.analytics.V2.AnalyticsMessageConverterV2;

import java.util.Arrays;
import java.util.List;

public interface AnalyticsPluginConstants {
    List<String> SUPPORTED_VERSIONS = Arrays.asList(AnalyticsMessageConverterV1.VERSION, AnalyticsMessageConverterV2.VERSION);

    String REQUEST_PREFIX = "go.cd.analytics";
    String REQUEST_GET_PLUGIN_ICON = REQUEST_PREFIX + ".get-icon";
    String REQUEST_GET_CAPABILITIES = REQUEST_PREFIX + ".get-capabilities";
    String REQUEST_GET_ANALYTICS = REQUEST_PREFIX + ".get-analytics";
    String REQUEST_GET_STATIC_ASSETS = REQUEST_PREFIX + ".get-static-assets";
}
