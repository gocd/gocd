/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.access.analytics.V1.models;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class Capabilities {
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    @Expose
    @SerializedName("supported_analytics")
    private List<SupportedAnalytics> supportedAnalytics;

    public List<SupportedAnalytics> getSupportedAnalytics() {
        return supportedAnalytics;
    }

    public Capabilities(List<SupportedAnalytics> supportedAnalytics) {
        this.supportedAnalytics = supportedAnalytics;
    }

    public static Capabilities fromJSON(String json) {
        return GSON.fromJson(json, Capabilities.class);
    }

    public com.thoughtworks.go.plugin.domain.analytics.Capabilities toCapabilities() {
        return new com.thoughtworks.go.plugin.domain.analytics.Capabilities(supportedAnalytics());
    }

    private List<com.thoughtworks.go.plugin.domain.analytics.SupportedAnalytics> supportedAnalytics() {
        ArrayList<com.thoughtworks.go.plugin.domain.analytics.SupportedAnalytics> list = new ArrayList<>();

        if (this.supportedAnalytics != null) {
            for (SupportedAnalytics analytics : this.supportedAnalytics) {
                list.add(new com.thoughtworks.go.plugin.domain.analytics.SupportedAnalytics(analytics.getType(),
                        analytics.getId(), analytics.getTitle()));
            }
        }

        return list;
    }
}
