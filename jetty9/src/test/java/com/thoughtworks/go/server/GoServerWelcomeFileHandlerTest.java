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

import com.thoughtworks.go.http.mocks.MockHttpServletRequest;
import com.thoughtworks.go.http.mocks.MockHttpServletResponse;
import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.server.Handler;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.servlet.ServletException;
import java.io.IOException;

import static com.thoughtworks.go.http.mocks.MockHttpServletResponseAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoServerWelcomeFileHandlerTest {

    private MockHttpServletResponse response;
    private SystemEnvironment systemEnvironment;
    private MockHttpServletRequest request;
    private Handler welcomeFileHandler;

    @BeforeEach
    void setUp() throws Exception {
        response = new MockHttpServletResponse();
        systemEnvironment = mock(SystemEnvironment.class);
        welcomeFileHandler = new GoServerWelcomeFileHandler(systemEnvironment).getHandler();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/", "/go", "/go/"})
    void shouldRedirectToLandingPage(String pathInfo) throws Exception {
        request = new MockHttpServletRequest();
        request.setPathInfo(pathInfo);

        when(systemEnvironment.landingPage()).thenReturn("/foobar");

        welcomeFileHandler.handle("foo", null, request, response);

        assertThat(response).redirectsTo("/go/foobar");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/foo", "/go/foo", "/foo/bar"})
    void shouldNotRedirectToLandingPage(String pathInfo) throws Exception {
        request = new MockHttpServletRequest();
        request.setPathInfo(pathInfo);

        when(systemEnvironment.landingPage()).thenReturn("/foobar");

        welcomeFileHandler.handle("foo", null, request, response);

        assertThat(response).isOk();
    }

    @Test
    void shouldAddLocationHeaderWhenContextIsNotGo() throws IOException, ServletException {
        request = new MockHttpServletRequest();
        request.setPathInfo("/");

        when(systemEnvironment.landingPage()).thenReturn("/foobar");

        welcomeFileHandler.handle("foo", null, request, response);

        assertThat(response).redirectsTo("/go/foobar");
    }
}
