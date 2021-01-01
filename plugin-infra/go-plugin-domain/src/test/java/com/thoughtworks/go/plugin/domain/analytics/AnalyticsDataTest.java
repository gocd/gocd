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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

class AnalyticsDataTest {
    private String viewPath;
    private AnalyticsData analyticsData;

    @BeforeEach
    void setUp() {
    }

    @Test
    void shouldGetFullViewPathForLinuxOperatingSystem() {
        String assetRoot = "/assets/root";
        viewPath = "agents/agents.html";
        analyticsData = new AnalyticsData("{}", viewPath);
        analyticsData.setAssetRoot(assetRoot);

        assertThat(analyticsData.getFullViewPath(), is(assetRoot + '/' + viewPath));
    }

    @Test
    void shouldGetFullViewPathForWindowsOperatingSystem() {
        String assetRoot = "\\assets\\root";
        viewPath = "agents\\agents.html";
        analyticsData = new AnalyticsData("{}", viewPath);
        analyticsData.setAssetRoot(assetRoot);

        assertThat(analyticsData.getFullViewPath(), is("/assets/root/agents/agents.html"));
    }

    @Test
    void shouldAllowViewPathWithQueryParametersForNonWindowsOperatingSystem() {
        String assetRoot = "/assets/root";
        viewPath = "agents/agents.html?msg=Hello%20World&msg2=AnotherOne";
        analyticsData = new AnalyticsData("{}", viewPath);
        analyticsData.setAssetRoot(assetRoot);

        assertThat(analyticsData.getFullViewPath(), is(assetRoot + '/' + viewPath));
    }

    @Test
    void shouldAllowViewPathWithQueryParametersForWindowsOperatingSystem() {
        String assetRoot = "\\assets\\root";
        viewPath = "agents\\agents.html?msg=Hello%20World&msg2=AnotherOne";
        analyticsData = new AnalyticsData("{}", viewPath);
        analyticsData.setAssetRoot(assetRoot);

        assertThat(analyticsData.getFullViewPath(), is("/assets/root/agents/agents.html?msg=Hello%20World&msg2=AnotherOne"));
    }
}
