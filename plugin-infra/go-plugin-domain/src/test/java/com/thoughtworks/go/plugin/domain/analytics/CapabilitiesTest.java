/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import java.util.Collections;

import static org.junit.Assert.*;

public class CapabilitiesTest {
    @Test
    public void equivalenceBasedOnContent() throws Exception {
        Capabilities capabilities = new Capabilities(true, Collections.singletonList("foo"));
        Capabilities capabilities1 = new Capabilities(true, Collections.singletonList("foo"));

        assertEquals(capabilities, capabilities1);
        assertEquals(capabilities.hashCode(), capabilities1.hashCode());

        Capabilities capabilities2 = new Capabilities(true, Collections.singletonList("bar"));
        assertNotEquals(capabilities, capabilities2);
        assertNotEquals(capabilities.hashCode(), capabilities2.hashCode());

        Capabilities capabilities3 = new Capabilities(false, Collections.singletonList("foo"));
        assertNotEquals(capabilities, capabilities3);
        assertNotEquals(capabilities.hashCode(), capabilities3.hashCode());
    }

    @Test
    public void shouldSupportDashboardAnalyticsIfPluginListsSupportedAnalyticsDashboardMetrics() throws Exception {
        assertTrue(new Capabilities(true, Collections.singletonList("foo")).supportsDashboardAnalytics());

        assertFalse(new Capabilities(true, Collections.emptyList()).supportsDashboardAnalytics());

        assertFalse(new Capabilities(true, null).supportsDashboardAnalytics());
    }
}