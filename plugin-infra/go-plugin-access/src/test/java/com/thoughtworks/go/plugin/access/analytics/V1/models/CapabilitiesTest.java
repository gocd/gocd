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

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;

public class CapabilitiesTest {

    @Test
    public void shouldDeserializeFromJSON() throws Exception {
        String json = "{\n" +
                "\"supported_analytics\": [\n" +
                "  {\"type\": \"dashboard\", \"id\": \"abc\",  \"title\": \"Title 1\"},\n" +
                "  {\"type\": \"pipeline\", \"id\": \"abc\",  \"title\": \"Title 1\"}\n" +
                "]}";

        Capabilities capabilities = Capabilities.fromJSON(json);

        assertThat(capabilities.getSupportedAnalytics().size(), is(2));
        assertThat(capabilities.getSupportedAnalytics().get(0), is(new SupportedAnalytics("dashboard", "abc", "Title 1")) );
    }

    @Test
    public void shouldConvertToDomainCapabilities() throws Exception {
        String json = "{\n" +
                "\"supported_analytics\": [\n" +
                "  {\"type\": \"dashboard\", \"id\": \"abc\",  \"title\": \"Title 1\"},\n" +
                "  {\"type\": \"pipeline\", \"id\": \"abc\",  \"title\": \"Title 1\"}\n" +
                "]}";

        Capabilities capabilities = Capabilities.fromJSON(json);
        com.thoughtworks.go.plugin.domain.analytics.Capabilities domain = capabilities.toCapabilities();

        assertThat(domain.supportedDashboardAnalytics(), containsInAnyOrder(new com.thoughtworks.go.plugin.domain.analytics.SupportedAnalytics("dashboard", "abc", "Title 1")));
        assertThat(domain.supportedPipelineAnalytics(), containsInAnyOrder(new com.thoughtworks.go.plugin.domain.analytics.SupportedAnalytics("pipeline", "abc", "Title 1")));
    }
}
