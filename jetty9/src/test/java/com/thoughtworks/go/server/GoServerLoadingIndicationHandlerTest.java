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
package com.thoughtworks.go.server;

import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.http.HttpFields;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GoServerLoadingIndicationHandlerTest {
    @Mock
    private WebAppContext webAppContext;

    @Mock
    private SystemEnvironment systemEnvironment;

    private GoServerLoadingIndicationHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GoServerLoadingIndicationHandler(webAppContext, systemEnvironment);
    }

    @Nested
    @DisplayName("When web app has started")
    class WhenWebAppHasStarted {
        @BeforeEach
        void setUp() {
            webAppHasStarted();
        }

        @Test
        void shouldRedirectSlashOrRootToLandingPage() throws Exception {
            landingPageIsSetTo("/landingpage");

            MockResponse response = request("/", "*/*");

            assertTrue(response.wasRedirectedTo("/go/landingpage").done());
        }
    }


    @Nested
    @DisplayName("When web app is starting")
    class WhenWebAppIsStarting {
        @BeforeEach
        void setUp() {
            webAppIsStarting();
        }

        @ParameterizedTest
        @ValueSource(strings = {"/", "/go/pipelines", "/this/doesnt/exist"})
        void shouldRespondWithMessageInPlainTextWhenRequestAcceptHeaderIsNotJSONOrHTML(String target) throws Exception {
            webAppIsStarting();

            MockResponse response = request(target, "*/*");

            assertLoadingResponseInPlainText(response);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/", "/go/pipelines", "/this/doesnt/exist"})
        void shouldRespondWithMessageInJSONWhenRequestAcceptHeaderIsJSON(String target) throws Exception {
            webAppIsStarting();

            MockResponse response = request(target, "application/json");

            assertLoadingResponseInJSON(response);
        }

        @ParameterizedTest
        @ValueSource(strings = {"/", "/go/pipelines", "/this/doesnt/exist"})
        void shouldRespondWithMessageInHTMLWhenRequestAcceptHeaderIsHTML(String target) throws Exception {
            webAppIsStarting();
            loadingPageIsSetTo("/test.loading.page.html");

            MockResponse response = request(target, "text/html");

            assertLoadingResponseInHTML(response, "<div><b>GoCD server is starting. This comes from test.loading.page.html</b></div>");
        }

        @ParameterizedTest
        @DisplayName("should respond with application/json when Accept header value after sorting by quality factor contains the word json")
        @ValueSource(strings = {"application/json", "something/json", "text/html;q=0.5,application/json;q=0.8",
                "application/vnd.go.cd.v3+json", "something-with-json-in-it"})
        void shouldRespondWithMessageInJSONWhenAcceptHeaderContainsJSON(String acceptHeaderValue) throws Exception {
            webAppIsStarting();

            MockResponse response = request("/go/pipelines", acceptHeaderValue);

            assertLoadingResponseInJSON(response);
        }

        @ParameterizedTest
        @DisplayName("should respond with text/html when Accept header value after sorting by quality factor contains the word html or is empty")
        @ValueSource(strings = {"text/html", "something/html", "application/json;q=0.5,text/html;q=0.8", "something-with-html-in-it", ""})
        void shouldRespondWithMessageInHTMLWhenAcceptHeaderContainsHTML(String acceptHeaderValue) throws Exception {
            webAppIsStarting();
            loadingPageIsSetTo("/test.loading.page.html");

            MockResponse response = request("/go/pipelines", acceptHeaderValue);

            assertLoadingResponseInHTML(response, "<div><b>GoCD server is starting. This comes from test.loading.page.html</b></div>");
        }

        @ParameterizedTest
        @DisplayName("should respond with text/plain when Accept header value is unknown or not HTML/JSON after sorting by quality factor")
        @ValueSource(strings = {"some-random-value", "image/svg", "text/html;q=0.5,*/*;q=0.8", "text/html;q=0.5,text/plain;q=0.8", "text/html;q=0.5,image/svg;q=0.8", "*/*"})
        void shouldRespondWithMessageInPlainTextWhenAcceptHeaderIsNotHTMLOrJSON(String acceptHeaderValue) throws Exception {
            webAppIsStarting();

            MockResponse response = request("/go/pipelines", acceptHeaderValue);

            assertLoadingResponseInPlainText(response);
        }

        @Test
        void shouldRespondWithMessageInHTMLWhenAcceptHeaderIsMissing() throws Exception {
            webAppIsStarting();
            loadingPageIsSetTo("/test.loading.page.html");

            MockResponse response = request("/go/pipelines", null);

            assertLoadingResponseInHTML(response, "<div><b>GoCD server is starting. This comes from test.loading.page.html</b></div>");
        }

        @Test
        void shouldRespondWithSimpleMessageIfLoadingHTMLFileCannotBeLoaded() throws Exception {
            webAppIsStarting();
            loadingPageIsSetTo("/some-non-existent-file");

            MockResponse response = request("/go/pipelines", "text/html");

            assertLoadingResponseInHTML(response, "<h2>GoCD is starting up. Please wait ....</h2>");
        }
    }

    private void assertLoadingResponseInPlainText(MockResponse response) {
        assertTrue(response.
                hasStatus(503).
                withContentType("text/plain").
                withBody("GoCD server is starting").
                withNoCaching().
                done());
    }

    private void assertLoadingResponseInJSON(MockResponse response) {
        assertTrue(response.
                hasStatus(503).
                withContentType("application/json").
                withBody("{ \"message\": \"GoCD server is starting\" }").
                withNoCaching().
                done());
    }

    private void assertLoadingResponseInHTML(MockResponse response, String expectedBody) {
        assertTrue(response.
                hasStatus(503).
                withContentType("text/html").
                withBody(expectedBody).
                withNoCaching().
                done());
    }

    private MockResponse request(String target, String acceptHeaderValue) throws Exception {
        Request baseRequest = mock(Request.class);
        HttpFields httpFields = new HttpFields();
        if (acceptHeaderValue != null) {
            httpFields.add("Accept", acceptHeaderValue);
        }
        lenient().when(baseRequest.getHttpFields()).thenReturn(httpFields);

        HttpServletRequest servletRequest = mock(HttpServletRequest.class);
        HttpServletResponse servletResponse = mock(HttpServletResponse.class);
        PrintWriter printWriter = mock(PrintWriter.class);
        lenient().when(servletResponse.getWriter()).thenReturn(printWriter);

        handler.getHandler().handle(target, baseRequest, servletRequest, servletResponse);

        return new MockResponse(servletResponse, printWriter);
    }

    private void webAppIsStarting() {
        when(webAppContext.isAvailable()).thenReturn(false);
    }

    private void webAppHasStarted() {
        when(webAppContext.isAvailable()).thenReturn(true);
    }

    private void landingPageIsSetTo(String landingPage) {
        when(systemEnvironment.landingPage()).thenReturn(landingPage);
    }

    private void loadingPageIsSetTo(String loadingPageResourcePath) {
        when(systemEnvironment.get(SystemEnvironment.LOADING_PAGE)).thenReturn(loadingPageResourcePath);
    }

    private class MockResponse {
        private HttpServletResponse response;
        private PrintWriter printWriter;

        MockResponse(HttpServletResponse response, PrintWriter printWriter) {
            this.response = response;
            this.printWriter = printWriter;
        }

        MockResponse hasStatus(int expectedStatus) {
            verify(response).setStatus(expectedStatus);
            return this;
        }

        MockResponse withNoCaching() {
            verify(response).setHeader("Cache-Control", "no-cache, must-revalidate, no-store");
            return this;
        }

        MockResponse withContentType(String expectedContentType) {
            verify(response).setContentType(expectedContentType);
            return this;
        }

        MockResponse wasRedirectedTo(String redirectLocation) throws IOException {
            verify(response).sendRedirect(redirectLocation);
            return this;
        }

        MockResponse withBody(String expectedBody) {
            verify(printWriter).println(expectedBody);
            return this;
        }

        boolean done() {
            return true;
        }
    }
}
