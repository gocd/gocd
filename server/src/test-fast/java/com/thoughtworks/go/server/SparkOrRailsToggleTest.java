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

package com.thoughtworks.go.server;

import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SparkOrRailsToggleTest {

    private FeatureToggleService featureToggleService;
    private HttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        featureToggleService = mock(FeatureToggleService.class);
        Toggles.initializeWith(featureToggleService);
        request = mock(HttpServletRequest.class);
    }

    @Test
    public void shouldForwardToRailsIfQuickerDashboardToggleIsDisabled() {
        SparkOrRailsToggle sparkOrRailsToggle = new SparkOrRailsToggle();
        when(featureToggleService.isToggleOn(Toggles.QUICKER_DASHBOARD_KEY)).thenReturn(false);

        sparkOrRailsToggle.oldOrNewDashboard(request, null);

        verify(request).setAttribute("newUrl", "/rails/pipelines");
        verify(request).setAttribute("rails_bound", true);
    }

    @Test
    public void shouldForwardToRailsIfNewDashboardPageDefaultToggleIsDisabled() {
        SparkOrRailsToggle sparkOrRailsToggle = new SparkOrRailsToggle();
        when(featureToggleService.isToggleOn(Toggles.QUICKER_DASHBOARD_KEY)).thenReturn(true);
        when(featureToggleService.isToggleOn(Toggles.NEW_DASHBOARD_PAGE_DEFAULT)).thenReturn(false);

        sparkOrRailsToggle.oldOrNewDashboard(request, null);

        verify(request).setAttribute("newUrl", "/rails/pipelines");
        verify(request).setAttribute("rails_bound", true);
    }

    @Test
    public void shouldForwardToSparkIfBothDashboardTogglesAreEnabled() {
        SparkOrRailsToggle sparkOrRailsToggle = new SparkOrRailsToggle();
        when(featureToggleService.isToggleOn(Toggles.QUICKER_DASHBOARD_KEY)).thenReturn(true);
        when(featureToggleService.isToggleOn(Toggles.NEW_DASHBOARD_PAGE_DEFAULT)).thenReturn(true);

        sparkOrRailsToggle.oldOrNewDashboard(request, null);

        verify(request).setAttribute("newUrl", "/spark/dashboard");
    }
}
