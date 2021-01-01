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

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class CapabilitiesTest {
    @Test
    public void shouldSupportDashboardAnalyticsIfPluginListsDashboardMetricsAsCapability() {
        assertTrue(new Capabilities(Collections.singletonList(new SupportedAnalytics("dashboard", "id", "title"))).supportsDashboardAnalytics());
        assertTrue(new Capabilities(Collections.singletonList(new SupportedAnalytics("DashBoard", "id", "title"))).supportsDashboardAnalytics());

        assertFalse(new Capabilities(Collections.emptyList()).supportsDashboardAnalytics());
    }

    @Test
    public void shouldSupportPipelineAnalyticsIfPluginListsPipelineMetricsAsCapability() {
        assertTrue(new Capabilities(Collections.singletonList(new SupportedAnalytics("pipeline", "id", "title"))).supportsPipelineAnalytics());
        assertTrue(new Capabilities(Collections.singletonList(new SupportedAnalytics("PipeLine", "id", "title"))).supportsPipelineAnalytics());

        assertFalse(new Capabilities(Collections.emptyList()).supportsPipelineAnalytics());
    }

    @Test
    public void shouldSupportVSMAnalyticsIfPluginListsVSMMetricsAsCapability() {
        assertTrue(new Capabilities(Collections.singletonList(new SupportedAnalytics("vsm", "id", "title" ))).supportsVSMAnalytics());
        assertTrue(new Capabilities(Collections.singletonList(new SupportedAnalytics("VsM", "id", "title" ))).supportsVSMAnalytics());

        assertFalse(new Capabilities(Collections.emptyList()).supportsPipelineAnalytics());
    }

    @Test
    public void shouldListSupportedDashBoardAnalytics() {
        Capabilities capabilities = new Capabilities(Arrays.asList(new SupportedAnalytics("dashboard", "id1", "title1" ),
                new SupportedAnalytics("DashBoard", "id2", "title2" )));

        assertThat(capabilities.supportedAnalyticsDashboardMetrics(), is(Arrays.asList("title1", "title2")));
        assertTrue(new Capabilities(Collections.emptyList()).supportedAnalyticsDashboardMetrics().isEmpty());
    }

    @Test
    public void shouldListSupportedAnalyticsForDashboard() {
        Capabilities capabilities = new Capabilities(Arrays.asList(new SupportedAnalytics("dashboard", "id1", "title1" ),
                new SupportedAnalytics("DashBoard", "id2", "title2" )));

        assertThat(capabilities.supportedDashboardAnalytics(), is(Arrays.asList(new SupportedAnalytics("dashboard", "id1", "title1" ),
                new SupportedAnalytics("DashBoard", "id2", "title2" ))));
        assertTrue(new Capabilities(Collections.emptyList()).supportedDashboardAnalytics().isEmpty());
    }

    @Test
    public void shouldListSupportedAnalyticsForPipelines() {
        Capabilities capabilities = new Capabilities(Arrays.asList(new SupportedAnalytics("pipeline", "id1", "title1" ),
                new SupportedAnalytics("Pipeline", "id2", "title2" )));

        assertThat(capabilities.supportedPipelineAnalytics(), is(Arrays.asList(new SupportedAnalytics("pipeline", "id1", "title1" ),
                new SupportedAnalytics("Pipeline", "id2", "title2" ))));
        assertTrue(new Capabilities(Collections.emptyList()).supportedPipelineAnalytics().isEmpty());
    }

    @Test
    public void shouldListSupportedAnalyticsForVSM() {
        Capabilities capabilities = new Capabilities(Arrays.asList(new SupportedAnalytics("vsm", "id1", "title1" ),
                new SupportedAnalytics("VsM", "id2", "title2" )));

        assertThat(capabilities.supportedVSMAnalytics(), is(Arrays.asList(new SupportedAnalytics("vsm", "id1", "title1" ),
                new SupportedAnalytics("VsM", "id2", "title2" ))));
        assertTrue(new Capabilities(Collections.emptyList()).supportedVSMAnalytics().isEmpty());
    }
}
