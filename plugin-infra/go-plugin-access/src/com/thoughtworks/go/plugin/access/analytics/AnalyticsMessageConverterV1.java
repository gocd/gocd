/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.google.gson.Gson;
import com.thoughtworks.go.plugin.access.analytics.models.Capabilities;
import org.apache.commons.lang.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsMessageConverterV1 implements AnalyticsMessageConverter {
    public static final String VERSION = "1.0";
    private static final Gson GSON = new Gson();

    @Override
    public com.thoughtworks.go.plugin.domain.analytics.Capabilities getCapabilitiesFromResponseBody(String responseBody) {
        return Capabilities.fromJSON(responseBody).toCapabilites();
    }

    @Override
    public String getPipelineAnalyticsRequestBody(String pipelineName) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("pipeline_name", pipelineName );

        return GSON.toJson(requestMap);
    }

    @Override
    public String getPipelineAnalyticsFromResponseBody(String responseBody) {
        String analytics = (String) new Gson().fromJson(responseBody, Map.class).get("view");
        if (StringUtils.isBlank(analytics)) {
            throw new RuntimeException("Analytics is blank!");
        }
        return analytics;
    }
}