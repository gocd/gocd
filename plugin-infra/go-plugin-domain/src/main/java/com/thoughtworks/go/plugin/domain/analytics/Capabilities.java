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

public class Capabilities {
    private final boolean supportsPipelineAnalytics;
    private final List<String> supportedAnalyticsDashboardMetrics;

    public Capabilities(boolean supportsPipelineAnalytics, List<String> supportedAnalyticsDashboardMetrics) {
        this.supportsPipelineAnalytics = supportsPipelineAnalytics;
        this.supportedAnalyticsDashboardMetrics = supportedAnalyticsDashboardMetrics;
    }

    public boolean supportsPipelineAnalytics() {
        return supportsPipelineAnalytics;
    }

    public boolean supportsDashboardAnalytics() {
        return this.supportedAnalyticsDashboardMetrics != null && this.supportedAnalyticsDashboardMetrics.size() > 0;
    }

    public List<String> supportedAnalyticsDashboardMetrics() {
        return supportedAnalyticsDashboardMetrics;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Capabilities that = (Capabilities) o;

        return supportsPipelineAnalytics == that.supportsPipelineAnalytics &&
                supportedAnalyticsDashboardMetrics.equals(that.supportedAnalyticsDashboardMetrics);
    }

    @Override
    public int hashCode() {
        return (supportedAnalyticsDashboardMetrics.hashCode() << 1) + (supportsPipelineAnalytics ? 1 : 0);
    }
}
