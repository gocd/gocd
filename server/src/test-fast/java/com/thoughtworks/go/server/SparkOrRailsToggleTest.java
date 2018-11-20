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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.servlet.http.HttpServletRequest;

import static org.mockito.Mockito.*;

class SparkOrRailsToggleTest {

    private FeatureToggleService featureToggleService;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() throws Exception {
        featureToggleService = mock(FeatureToggleService.class);
        Toggles.initializeWith(featureToggleService);
        request = mock(HttpServletRequest.class);
    }

    @Nested
    class AgentApisOverRailsToggle {
        @Test
        void shouldForwardToRails() {
            SparkOrRailsToggle sparkOrRailsToggle = new SparkOrRailsToggle();
            when(featureToggleService.isToggleOn(Toggles.AGENT_APIS_OVER_RAILS)).thenReturn(false);

            sparkOrRailsToggle.agentsApi(request, null);

            verify(request).setAttribute("sparkOrRails", "spark");
        }

        @Test
        void shouldForwardToSpark() {
            SparkOrRailsToggle sparkOrRailsToggle = new SparkOrRailsToggle();
            when(featureToggleService.isToggleOn(Toggles.AGENT_APIS_OVER_RAILS)).thenReturn(true);

            sparkOrRailsToggle.agentsApi(request, null);

            verify(request).setAttribute("sparkOrRails", "rails");
        }
    }
}
