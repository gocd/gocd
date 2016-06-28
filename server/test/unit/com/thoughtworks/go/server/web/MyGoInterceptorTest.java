/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.controller.MyGoController;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PipelineConfigService;
import com.thoughtworks.go.server.service.UserService;
import com.thoughtworks.go.server.service.EnvironmentConfigService;

import static com.thoughtworks.go.server.web.MyGoInterceptor.SECURITY_IS_ENABLED;
import static com.thoughtworks.go.server.web.MyGoInterceptor.SMTP_IS_ENABLED;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;

public class MyGoInterceptorTest {
    private GoConfigService goConfigService;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private ModelAndView modelAndView;

    private MyGoInterceptor myGoInterceptor;
    private EnvironmentConfigService environmentConfigService;

    @Before
    public void setUp() throws Exception {
        goConfigService = mock(GoConfigService.class);
        environmentConfigService = mock(EnvironmentConfigService.class);

        myGoInterceptor = new MyGoInterceptor(goConfigService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        modelAndView = new ModelAndView();
    }

    @Test
    public void shouldReturnAccessIsDeniedWhenRoutingToMyCruiseWhileSecurityIsOff() throws Exception {
        securityIs(false);
        boolean result = myGoInterceptor.preHandle(request, response, new MyGoController(mock(UserService.class), mock(PipelineConfigService.class), mock(Localizer.class)));
        assertThat(response.getStatus(), is(HttpServletResponse.SC_NOT_FOUND));
        assertThat(result, is(false));
    }

    private void securityIs(final boolean result) {
        when(goConfigService.isSecurityEnabled()).thenReturn(result);
    }

    @Test
    public void shouldReturnAccessIsDeniedWhenRoutingToAnyOtherControllerWhileSecurityIsOff() throws Exception {
        securityIs(false);
        boolean result = myGoInterceptor.preHandle(request, response, new Object());
        assertThat(response.getStatus(), is(HttpServletResponse.SC_OK));
        assertThat(result, is(true));
    }

    @Test
    public void shouldReturnAccessGrantedWhenSecurityIsOn() throws Exception {
        securityIs(true);
        boolean result = myGoInterceptor.preHandle(request, response, null);
        assertThat(response.getStatus(), is(HttpServletResponse.SC_OK));
        assertThat(result, is(true));
    }

    @Test
    public void shouldPutSecurityIsEnabledIntoModelAndView() throws Exception {
        securityIs(true);
        smtpIs(true);
        myGoInterceptor.postHandle(request, response, null, modelAndView);
        ModelMap modelMap = modelAndView.getModelMap();
        assertThat((Boolean) modelMap.get(SECURITY_IS_ENABLED), is(true));
        assertThat((Boolean) modelMap.get(SMTP_IS_ENABLED), is(true));
    }

    private void smtpIs(final boolean result) {
        when(goConfigService.isSmtpEnabled()).thenReturn(result);
    }

    @Test
    public void shouldPutSecurityIsDisbaledIntoModelAndView() throws Exception {
        securityIs(false);
        smtpIs(false);
        myGoInterceptor.postHandle(request, response, null, modelAndView);
        ModelMap modelMap = modelAndView.getModelMap();
        assertThat((Boolean) modelMap.get(SECURITY_IS_ENABLED), is(false));
        assertThat((Boolean) modelMap.get(SMTP_IS_ENABLED), is(false));
    }
}
