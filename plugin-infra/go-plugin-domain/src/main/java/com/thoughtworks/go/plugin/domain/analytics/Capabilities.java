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

package com.thoughtworks.go.plugin.domain.analytics;


import java.util.List;
import java.util.stream.Collectors;

public class Capabilities {
    private final List<SupportedAnalytics> supportedAnalytics;
    private static final String TYPE_DASHBOARD = "dashboard";
    private static final String TYPE_PIPELINE = "pipeline";
    private static final String TYPE_STAGE = "stage";

    public Capabilities(List<SupportedAnalytics> supportedAnalytics) {
        this.supportedAnalytics = supportedAnalytics;
    }

    public List<SupportedAnalytics> getSupportedAnalytics() {
        return supportedAnalytics;
    }

    public boolean supportsDashboardAnalytics() {
        return hasSupportFor(TYPE_DASHBOARD);
    }

    public boolean supportsPipelineAnalytics() {
        return hasSupportFor(TYPE_PIPELINE);
    }

    public boolean supportsStageAnalytics() {
        return hasSupportFor(TYPE_STAGE);
    }

    public List<SupportedAnalytics> supportedDashboardAnalytics() {
        return this.supportedAnalytics.stream().filter(s -> TYPE_DASHBOARD.equalsIgnoreCase(s.getType())).collect(Collectors.toList());
    }

    public List<SupportedAnalytics> supportedPipelineAnalytics() {
        return this.supportedAnalytics.stream().filter(s -> TYPE_PIPELINE.equalsIgnoreCase(s.getType())).collect(Collectors.toList());
    }

    public List<SupportedAnalytics> supportedStageAnalytics() {
        return this.supportedAnalytics.stream().filter(s -> TYPE_STAGE.equalsIgnoreCase(s.getType())).collect(Collectors.toList());
    }

    private boolean hasSupportFor(String analyticsType) {
        return this.supportedAnalytics.stream().anyMatch(s -> analyticsType.equalsIgnoreCase(s.getType()));
    }
}
