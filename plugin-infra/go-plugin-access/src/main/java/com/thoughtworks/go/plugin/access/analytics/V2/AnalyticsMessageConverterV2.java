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
package com.thoughtworks.go.plugin.access.analytics.V2;

import com.google.gson.Gson;
import com.thoughtworks.go.plugin.access.analytics.AnalyticsMessageConverter;
import com.thoughtworks.go.plugin.access.analytics.V2.models.Capabilities;
import com.thoughtworks.go.plugin.access.common.models.ImageDeserializer;
import com.thoughtworks.go.plugin.domain.analytics.AnalyticsData;
import com.thoughtworks.go.plugin.domain.common.Image;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsMessageConverterV2 implements AnalyticsMessageConverter {
    public static final String VERSION = "2.0";
    private static final Gson GSON = new Gson();

    @Override
    public String getAnalyticsRequestBody(String type, String metricId, Map params) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("type", type);
        requestMap.put("id", metricId);
        requestMap.put("params", params);

        return GSON.toJson(requestMap);
    }

    @Override
    public com.thoughtworks.go.plugin.domain.analytics.Capabilities getCapabilitiesFromResponseBody(String responseBody) {
        return Capabilities.fromJSON(responseBody).toCapabilities();
    }

    @Override
    public AnalyticsData getAnalyticsFromResponseBody(String responseBody) {
        com.thoughtworks.go.plugin.access.analytics.V2.models.AnalyticsData analyticsData = com.thoughtworks.go.plugin.access.analytics.V2.models.AnalyticsData.fromJSON(responseBody);

        analyticsData.validate();

        return analyticsData.toAnalyticsData();
    }

    @Override
    public String getStaticAssetsFromResponseBody(String responseBody) {
        String assets = (String) new Gson().fromJson(responseBody, Map.class).get("assets");

        if (StringUtils.isBlank(assets)) {
            throw new RuntimeException("No assets defined!");
        }

        return assets;
    }

    @Override
    public Image getImageFromResponseBody(String responseBody) {
        return new ImageDeserializer().fromJSON(responseBody);
    }
}
