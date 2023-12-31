/*
 * Copyright 2024 Thoughtworks, Inc.
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

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CapabilitiesTest {
    @Test
    public void shouldSupportDashboardAnalyticsIfPluginListsDashboardMetricsAsCapability() {
        assertTrue(new Capabilities(List.of(new SupportedAnalytics("dashboard", "id", "title"))).supportsDashboardAnalytics());
        assertTrue(new Capabilities(List.of(new SupportedAnalytics("DashBoard", "id", "title"))).supportsDashboardAnalytics());

        assertFalse(new Capabilities(Collections.emptyList()).supportsDashboardAnalytics());
    }

    @Test
    public void shouldSupportPipelineAnalyticsIfPluginListsPipelineMetricsAsCapability() {
        assertTrue(new Capabilities(List.of(new SupportedAnalytics("pipeline", "id", "title"))).supportsPipelineAnalytics());
        assertTrue(new Capabilities(List.of(new SupportedAnalytics("PipeLine", "id", "title"))).supportsPipelineAnalytics());

        assertFalse(new Capabilities(Collections.emptyList()).supportsPipelineAnalytics());
    }

    @Test
    public void shouldSupportVSMAnalyticsIfPluginListsVSMMetricsAsCapability() {
        assertTrue(new Capabilities(List.of(new SupportedAnalytics("vsm", "id", "title" ))).supportsVSMAnalytics());
        assertTrue(new Capabilities(List.of(new SupportedAnalytics("VsM", "id", "title" ))).supportsVSMAnalytics());

        assertFalse(new Capabilities(Collections.emptyList()).supportsPipelineAnalytics());
    }

    @Test
    public void shouldListSupportedDashBoardAnalytics() {
        Capabilities capabilities = new Capabilities(List.of(new SupportedAnalytics("dashboard", "id1", "title1" ),
                new SupportedAnalytics("DashBoard", "id2", "title2" )));

        assertThat(capabilities.supportedAnalyticsDashboardMetrics(), is(List.of("title1", "title2")));
        assertTrue(new Capabilities(Collections.emptyList()).supportedAnalyticsDashboardMetrics().isEmpty());
    }

    @Test
    public void shouldListSupportedAnalyticsForDashboard() {
        Capabilities capabilities = new Capabilities(List.of(new SupportedAnalytics("dashboard", "id1", "title1" ),
                new SupportedAnalytics("DashBoard", "id2", "title2" )));

        assertThat(capabilities.supportedDashboardAnalytics(), is(List.of(new SupportedAnalytics("dashboard", "id1", "title1" ),
                new SupportedAnalytics("DashBoard", "id2", "title2" ))));
        assertTrue(new Capabilities(Collections.emptyList()).supportedDashboardAnalytics().isEmpty());
    }

    @Test
    public void shouldListSupportedAnalyticsForPipelines() {
        Capabilities capabilities = new Capabilities(List.of(new SupportedAnalytics("pipeline", "id1", "title1" ),
                new SupportedAnalytics("Pipeline", "id2", "title2" )));

        assertThat(capabilities.supportedPipelineAnalytics(), is(List.of(new SupportedAnalytics("pipeline", "id1", "title1" ),
                new SupportedAnalytics("Pipeline", "id2", "title2" ))));
        assertTrue(new Capabilities(Collections.emptyList()).supportedPipelineAnalytics().isEmpty());
    }

    @Test
    public void shouldListSupportedAnalyticsForVSM() {
        Capabilities capabilities = new Capabilities(List.of(new SupportedAnalytics("vsm", "id1", "title1" ),
                new SupportedAnalytics("VsM", "id2", "title2" )));

        assertThat(capabilities.supportedVSMAnalytics(), is(List.of(new SupportedAnalytics("vsm", "id1", "title1" ),
                new SupportedAnalytics("VsM", "id2", "title2" ))));
        assertTrue(new Capabilities(Collections.emptyList()).supportedVSMAnalytics().isEmpty());
    }
}
