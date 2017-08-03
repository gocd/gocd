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

package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.server.controller.actions.XmlAction;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoConfigAdministrationControllerTest {
    private GoConfigAdministrationController controller;
    private GoConfigService goConfigService;
    private SecurityService securityService;

    @Before
    public void setUp() {
        goConfigService = mock(GoConfigService.class);
        securityService = mock(SecurityService.class);
        controller = new GoConfigAdministrationController(goConfigService, securityService, new SystemEnvironment());
    }

    @Test
    public void shouldLoadSpecificConfigVersionWhenHistoricalVersionIsRequested() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        String configXml = "config-content";

        when(goConfigService.getConfigAtVersion("some-md5")).thenReturn(new GoConfigRevision(configXml, "some-md5", "loser", "100.9.3.1", new TimeProvider()));

        controller.getConfigRevision("some-md5", response);

        assertThat(response.getContentAsString(), is(configXml));
        assertThat(response.getHeader(XmlAction.X_CRUISE_CONFIG_MD5), is("some-md5"));
    }

    @Test
    public void shouldEnsurePresenceOfCustomHeaderWhileUpdatingTheConfig() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.postFileAsXml("content", "md5", new MockHttpServletRequest(), response);

        assertThat(response.getStatus(), is(400));
    }
}
