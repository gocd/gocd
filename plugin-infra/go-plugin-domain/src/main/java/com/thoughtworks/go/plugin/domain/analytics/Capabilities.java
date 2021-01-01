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
package com.thoughtworks.go.plugin.domain.analytics;


import java.util.List;
import java.util.stream.Collectors;

public class Capabilities {
    private final List<SupportedAnalytics> supportedAnalytics;
    private static final String DASHBOARD_TYPE = "dashboard";
    private static final String PIPELINE_TYPE = "pipeline";
    private static final String VSM_TYPE = "vsm";

    public Capabilities(List<SupportedAnalytics> supportedAnalytics) {
        this.supportedAnalytics = supportedAnalytics;
    }

    public List<SupportedAnalytics> getSupportedAnalytics() {
        return supportedAnalytics;
    }

    public boolean supportsPipelineAnalytics() {
        return hasSupportFor(PIPELINE_TYPE);
    }

    public boolean supportsDashboardAnalytics() {
        return hasSupportFor(DASHBOARD_TYPE);
    }

    public boolean supportsVSMAnalytics() {
        return hasSupportFor(VSM_TYPE);
    }

    public List<String> supportedAnalyticsDashboardMetrics() {
        return this.supportedAnalytics.stream().filter(s -> DASHBOARD_TYPE.equalsIgnoreCase(s.getType())).map(SupportedAnalytics::getTitle).collect(Collectors.toList());
    }

    public List<SupportedAnalytics> supportedDashboardAnalytics() {
        return this.supportedAnalytics.stream().filter(s -> DASHBOARD_TYPE.equalsIgnoreCase(s.getType())).collect(Collectors.toList());
    }

    public List<SupportedAnalytics> supportedPipelineAnalytics() {
        return this.supportedAnalytics.stream().filter(s -> PIPELINE_TYPE.equalsIgnoreCase(s.getType())).collect(Collectors.toList());
    }

    public List<SupportedAnalytics> supportedVSMAnalytics() {
        return this.supportedAnalytics.stream().filter(s -> VSM_TYPE.equalsIgnoreCase(s.getType())).collect(Collectors.toList());
    }

    private boolean hasSupportFor(String analyticsType) {
        return this.supportedAnalytics.stream().anyMatch(s -> analyticsType.equalsIgnoreCase(s.getType()));
    }
}
