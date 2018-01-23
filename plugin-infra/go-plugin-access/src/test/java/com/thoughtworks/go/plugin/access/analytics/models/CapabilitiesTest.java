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

package com.thoughtworks.go.plugin.access.analytics.models;

import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class CapabilitiesTest {

    @Test
    public void shouldDeserializeFromJSON() throws Exception {
        String json = "" +
                "{\n" +
                "  \"supports_pipeline_analytics\": \"true\",\n" +
                "  \"supported_analytics_dashboard_metrics\": [\"foo\"]\n" +
                "}";

        Capabilities capabilities = Capabilities.fromJSON(json);

        assertTrue(capabilities.supportsPipelineAnalytics());
        assertEquals(Collections.singletonList("foo"), capabilities.supportedAnalyticsDashboardMetrics());
    }


    @Test
    public void shouldCreateEquivalentObjectsForSameJSON() throws Exception {
        String json = "" +
                "{\n" +
                "  \"supports_pipeline_analytics\": \"true\",\n" +
                "  \"supported_analytics_dashboard_metrics\": [\"foo\"]\n" +
                "}";

        com.thoughtworks.go.plugin.domain.analytics.Capabilities a = Capabilities.fromJSON(json).toCapabilities();
        com.thoughtworks.go.plugin.domain.analytics.Capabilities b = Capabilities.fromJSON(json).toCapabilities();
        com.thoughtworks.go.plugin.domain.analytics.Capabilities c = Capabilities.fromJSON("{\"supports_pipeline_analytics\": false, \"supported_analytics_dashboard_metrics\": [\"foo\"]}").toCapabilities();
        com.thoughtworks.go.plugin.domain.analytics.Capabilities d = Capabilities.fromJSON("{\"supports_pipeline_analytics\": true, \"supported_analytics_dashboard_metrics\": [\"bar\"]}").toCapabilities();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());

        assertNotEquals(a, c);
        assertNotEquals(a.hashCode(), c.hashCode());

        assertNotEquals(a, d);
        assertNotEquals(a.hashCode(), d.hashCode());

    }
}