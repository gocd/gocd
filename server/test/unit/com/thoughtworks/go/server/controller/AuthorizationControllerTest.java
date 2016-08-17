/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationPluginRegistry;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class AuthorizationControllerTest {

    Localizer localizer;
    AuthenticationPluginRegistry authenticationPluginRegistry;

    private AuthorizationController authorizationController;
    private MockHttpServletResponse response;

    @Before
    public void setUp() throws Exception {
        authenticationPluginRegistry = mock(AuthenticationPluginRegistry.class);
        localizer = mock(Localizer.class);

        authorizationController = new AuthorizationController(localizer, authenticationPluginRegistry);
        response = new MockHttpServletResponse();
    }

    @Test
    public void shouldAddCacheControlHeaderToTheResponse() throws Exception {
        authorizationController.login(false, response);

        assertThat(response.getHeader("Cache-Control"), is("no-cache, must-revalidate, no-store"));
    }

    @Test
    public void shouldAddPragmaHeaderToTheResponse() throws Exception {
        authorizationController.login(false, response);

        assertThat(response.getHeader("Pragma"), is("no-cache"));
    }

    @Test
    public void shouldSetModel() throws Exception {
        ModelAndView responseModel = authorizationController.login(false, response);

        Map<String, Object> modelMap = new ModelMap() {{
            put("login_error", false);
            put("l", localizer);
            put("authentication_plugin_registry", authenticationPluginRegistry);
        }};

        Map<String, Object> responseModelMap = responseModel.getModel();
        assertThat(responseModelMap, is(modelMap));
    }

    @Test
    public void shouldRedirectToGo() throws Exception {
        authorizationController.securityCheckHandlerWhenAuthenticationProcessingFilterIsOff(response);

        assertThat(response.getRedirectedUrl(), is("/go"));
    }
}
