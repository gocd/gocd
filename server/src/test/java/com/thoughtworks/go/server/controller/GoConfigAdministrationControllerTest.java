/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.remote.StandardHeaders;
import com.thoughtworks.go.server.controller.actions.JsonAction;
import com.thoughtworks.go.server.controller.actions.TextAction;
import com.thoughtworks.go.server.controller.actions.XmlAction;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.SessionUtilsHelper;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.util.json.JsonAware;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;
import java.util.stream.Stream;

import static java.net.HttpURLConnection.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GoConfigAdministrationControllerTest {
    private GoConfigAdministrationController controller;
    private GoConfigService goConfigService;
    private SecurityService securityService;

    @BeforeEach
    public void setUp() {
        goConfigService = mock(GoConfigService.class);
        securityService = mock(SecurityService.class);
        controller = new GoConfigAdministrationController(goConfigService, securityService);
    }

    @AfterEach
    public void tearDown() {
        SessionUtils.unsetCurrentUser();
    }

    @Test
    public void shouldLoadLatestConfig() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        userIsAdmin();

        String configXml = "config-content";
        GoConfigService.XmlPartialSaver<CruiseConfig> saver = mock();
        when(saver.asXml()).thenReturn(configXml);
        when(saver.getMd5()).thenReturn("some-md5");
        when(goConfigService.fileSaver(false)).thenReturn(saver);

        controller.getCurrentConfigXml(response);

        assertThat(response.getContentAsString()).isEqualTo(configXml);
        assertThat(response.getContentType()).isEqualTo(XmlAction.CONTENT_TYPE);
        assertThat(response.getHeader(XmlAction.HEADER_RESPONSE_CRUISE_CONFIG_MD5)).isEqualTo("some-md5");
    }

    @Test
    void currentConfigShouldBeNotFoundOnError() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        userIsAdmin();

        GoConfigService.XmlPartialSaver<CruiseConfig> saver = mock();
        when(saver.asXml()).thenThrow(new IllegalArgumentException("some error"));
        when(goConfigService.fileSaver(false)).thenReturn(saver);

        controller.getCurrentConfigXml(response);
        assertThat(response.getStatus()).isEqualTo(HTTP_NOT_FOUND);
        assertThat(response.getContentType()).isEqualTo(TextAction.CONTENT_TYPE);
        assertThat(response.getContentAsString()).isEqualTo("Unable to retrieve config XML for return");
    }

    @Test
    void currentConfigShouldBeForbiddenWhenNotAdmin() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.getCurrentConfigXml(response);
        assertThat(response.getStatus()).isEqualTo(HTTP_FORBIDDEN);
        assertThat(response.getContentType()).isEqualTo(TextAction.CONTENT_TYPE);
        assertThat(response.getContentAsString()).isEqualTo("User 'anonymous' does not have permissions to administer");
    }

    @Test
    void postAsXmlShouldUpdateConfig() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(StandardHeaders.REQUEST_CONFIRM_MODIFICATION, "true");
        MockHttpServletResponse response = new MockHttpServletResponse();
        userIsAdmin();

        GoConfigService.XmlPartialSaver<CruiseConfig> saver = mock();
        when(saver.getMd5()).thenReturn("some-md5");
        when(saver.saveXml("content", "some-md5")).thenReturn(GoConfigValidity.valid());
        when(goConfigService.fileSaver(false)).thenReturn(saver);

        ModelAndView modelAndView = controller.postFileAsXml("content", "some-md5", request, response);

        assertThat(response.getStatus()).isEqualTo(HTTP_OK);
        assertThat(response.getContentType()).isEqualTo(JsonAction.CONTENT_TYPE);
        assertThat(modelAndView.getModel()).containsExactlyInAnyOrderEntriesOf(Map.of(
            "json", Map.of("result", "File changed successfully.")
        ));
    }

    @ParameterizedTest
    @MethodSource("invalidConfigValidities")
    void postAsXmlShouldFailGracefullyOnConflict(GoConfigValidity.InvalidGoConfig validity, int expectedHttpCode) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(StandardHeaders.REQUEST_CONFIRM_MODIFICATION, "true");
        MockHttpServletResponse response = new MockHttpServletResponse();
        userIsAdmin();

        GoConfigService.XmlPartialSaver<CruiseConfig> saver = mock();
        when(saver.getMd5()).thenReturn("some-md5");
        when(saver.saveXml("content", "some-md5")).thenReturn(validity);
        when(goConfigService.fileSaver(false)).thenReturn(saver);

        ModelAndView modelAndView = controller.postFileAsXml("content", "some-md5", request, response);

        assertThat(response.getStatus()).isEqualTo(expectedHttpCode);
        assertThat(response.getContentType()).isEqualTo(JsonAction.CONTENT_TYPE);
        assertThat(modelAndView.getModel()).containsExactlyInAnyOrderEntriesOf(Map.of(
            "json", Map.of("originalContent", "content", "result", validity.errorMessage())
        ));
    }

    static Stream<Arguments> invalidConfigValidities() {
        return Stream.of(
            Arguments.of(GoConfigValidity.invalid("invalid config"), HTTP_NOT_FOUND),
            Arguments.of(GoConfigValidity.fromConflict("conflicted config"), HTTP_CONFLICT)
        );
    }

    @Test
    void postAsXmlShouldBeForbiddenWhenNotAdmin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(StandardHeaders.REQUEST_CONFIRM_MODIFICATION, "true");
        MockHttpServletResponse response = new MockHttpServletResponse();

        ModelAndView modelAndView = controller.postFileAsXml("content", "some-md5", request, response);

        assertThat(response.getStatus()).isEqualTo(HTTP_FORBIDDEN);
        assertThat(response.getContentType()).isEqualTo(JsonAction.CONTENT_TYPE);
        assertThat(modelAndView.getModel()).containsExactlyInAnyOrderEntriesOf(Map.of(
            "json", Map.of(JsonAware.ERROR_KEY, "User 'anonymous' does not have permissions to administer"))
        );
    }

    @Test
    public void shouldEnsurePresenceOfCustomHeaderWhileUpdatingTheConfig() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        ModelAndView modelAndView = controller.postFileAsXml("content", "some-md5", new MockHttpServletRequest(), response);

        assertThat(response.getStatus()).isEqualTo(HTTP_BAD_REQUEST);
        assertThat(response.getContentType()).isEqualTo(JsonAction.CONTENT_TYPE);
        assertThat(modelAndView.getModel()).containsExactlyInAnyOrderEntriesOf(Map.of(
            "json", Map.of(JsonAware.ERROR_KEY, "Missing required header `X-GoCD-Confirm`")
        ));
    }

    private void userIsAdmin() {
        SessionUtilsHelper.loginAs("admin");
        when(securityService.isUserAdmin(new Username("admin"))).thenReturn(true);
    }
}
