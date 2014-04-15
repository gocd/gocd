/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.web;

import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.GoLicenseService;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.GoConstants;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class LicensedAgentInterceptorTest {
    private LicensedAgentInterceptor interceptor;
    private ModelAndView mov;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private GoConfigService goConfigService;
    private GoLicenseService goLicenseService;
    private ServerHealthService serverHealthService;

    @Before
    public void setup() throws Exception {
        goConfigService = mock(GoConfigService.class);
        goLicenseService = mock(GoLicenseService.class);
        serverHealthService = mock(ServerHealthService.class);
        interceptor = new LicensedAgentInterceptor(new LicensedAgentCountValidator(goConfigService, goLicenseService, serverHealthService));
        mov = new ModelAndView();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    public void shouldPutErrorMessageToMovWhenAgentGreaterThanAllowed() throws Exception {
        when(goLicenseService.getNumberOfLicensedRemoteAgents()).thenReturn(2);
        when(goConfigService.getNumberOfApprovedRemoteAgents()).thenReturn(3);
        when(goLicenseService.hasRemoteAgentsExceededLicenseLimit()).thenReturn(true);

        interceptor.postHandle(request, response, null, mov);

        assertThat(mov.getModelMap().containsKey(GoConstants.ERROR_FOR_GLOBAL_MESSAGE), is(true));
        String message = "Current Go license allows only 2 remote agents. Currently 3 remote agents are enabled. Go will continue to assign jobs only to 2 remote agents "
                + "and the remaining remote agents will not be used. Please disable additional remote agents to comply with your license, or "
                + "<a href='http://www.thoughtworks.com/products/go-continuous-delivery/compare'>contact our sales team</a> to buy more agents.";
        assertThat((String) mov.getModelMap().get(GoConstants.ERROR_FOR_GLOBAL_MESSAGE), is(message));
    }

    @Test
    public void shouldPutErrorMessageToMovWhenAgentLessEqualsThanAllowed() throws Exception {
        when(goLicenseService.getNumberOfLicensedRemoteAgents()).thenReturn(2);
        when(goLicenseService.hasRemoteAgentsExceededLicenseLimit()).thenReturn(false);
        interceptor.postHandle(request, response, null, mov);
        assertThat(mov.getModelMap().containsKey(GoConstants.ERROR_FOR_GLOBAL_MESSAGE), is(false));
    }
}
