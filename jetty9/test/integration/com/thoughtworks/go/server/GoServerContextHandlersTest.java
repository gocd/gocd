/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server;

import com.thoughtworks.go.util.ArrayUtil;
import com.thoughtworks.go.util.ListUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.http.HttpHeaders;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.MovedContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;

import javax.net.ssl.SSLSocketFactory;
import javax.servlet.DispatcherType;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

@RunWith(Theories.class)
public class GoServerContextHandlersTest {
    private Jetty9Server jetty9Server;
    private CustomizedJettyServerWithoutWebAppHandler server;
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        SSLSocketFactory sslSocketFactory = mock(SSLSocketFactory.class);
        when(sslSocketFactory.getSupportedCipherSuites()).thenReturn(new String[0]);
        server = new CustomizedJettyServerWithoutWebAppHandler();

        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.getServerPort()).thenReturn(1234);
        when(systemEnvironment.keystore()).thenReturn(temporaryFolder.newFolder());
        when(systemEnvironment.truststore()).thenReturn(temporaryFolder.newFolder());
        when(systemEnvironment.getWebappContextPath()).thenReturn("context");
        when(systemEnvironment.getCruiseWar()).thenReturn("cruise.war");
        when(systemEnvironment.getParentLoaderPriority()).thenReturn(true);
        when(systemEnvironment.useCompressedJs()).thenReturn(false);
        sslSocketFactory = mock(SSLSocketFactory.class);
        when(sslSocketFactory.getSupportedCipherSuites()).thenReturn(new String[]{});

        jetty9Server = new Jetty9Server(systemEnvironment, "pwd", sslSocketFactory, server);
        jetty9Server.configure();
        jetty9Server.setInitParameter("rails.root", "/WEB-INF/rails.new");
        jetty9Server.addStopServlet();
    }

    @Test
    public void shouldRedirectRootRequestsToWelcomePage() throws Exception {
        ContextHandler welcomeFileHandler = jetty9Server.welcomeFileHandler();
        welcomeFileHandler.setServer(mock(Server.class));
        welcomeFileHandler.start();
        HttpServletResponse response = mock(HttpServletResponse.class);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        when(response.getWriter()).thenReturn(new PrintWriter(output));

        welcomeFileHandler.handle("/", mock(Request.class), mock(HttpServletRequest.class), response);

        String responseBody = new String(output.toByteArray());
        assertThat(responseBody, is("redirecting.."));

        verify(response).setHeader("Location", "/go/home");
        verify(response).setStatus(HttpStatus.MOVED_PERMANENTLY_301);
        verify(response).setHeader(HttpHeader.CONTENT_TYPE.asString(), "text/html");
        verify(response).getWriter();
        verifyNoMoreInteractions(response);
    }

    @Test
    public void shouldMatchRootUrlAsHandlerContextForWelcomePage() {
        ContextHandler handler = jetty9Server.welcomeFileHandler();
        assertThat(handler.getContextPath(), is("/"));
    }

    @Test
    public void shouldMatchCruiseUrlContextAsHandlerContextForLegacyRequestHandler() {
        Jetty9Server jetty9Server = new Jetty9Server(new SystemEnvironment(), null, mock(SSLSocketFactory.class));
        Jetty9Server.LegacyUrlRequestHandler handler = (Jetty9Server.LegacyUrlRequestHandler) jetty9Server.legacyRequestHandler();
        assertThat(handler.getHandler() instanceof MovedContextHandler, is(true));
        MovedContextHandler movedContextHandler = (MovedContextHandler) handler.getHandler();
        assertThat(movedContextHandler.getContextPath(), is("/cruise"));
        assertThat(movedContextHandler.getNewContextURL(), is("/go"));
        assertThat(movedContextHandler.isPermanent(), is(true));
    }

    @Test
    public void shouldNotRedirectNonRootRequestsToWelcomePage() throws IOException, ServletException {
        Jetty9Server jetty9Server = new Jetty9Server(new SystemEnvironment(), null, mock(SSLSocketFactory.class));
        Handler welcomeHandler = jetty9Server.welcomeFileHandler();
        HttpServletResponse response = mock(HttpServletResponse.class);

        welcomeHandler.handle("/foo", mock(Request.class), mock(HttpServletRequest.class), response);
        verifyNoMoreInteractions(response);
    }

    @Test
    public void shouldNotRedirectNonCruiseRequestsToGoPage() throws IOException, ServletException {
        Jetty9Server jetty9Server = new Jetty9Server(new SystemEnvironment(), null, mock(SSLSocketFactory.class));
        Handler legacyRequestHandler = jetty9Server.legacyRequestHandler();
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(response.getWriter()).thenReturn(new PrintWriter(new ByteArrayOutputStream()));

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn(HttpMethod.GET.asString());
        legacyRequestHandler.handle("/cruise_but_not_quite", mock(Request.class), req, response);
        verifyNoMoreInteractions(response);
        legacyRequestHandler.handle("/something_totally_different", mock(Request.class), req, response);
        verifyNoMoreInteractions(response);
    }

    static class HttpCall {
        final String url;
        final String method;
        final int expectedResponse;
        final boolean shouldRedirect;

        HttpCall(String url, String method, int expectedResponse, boolean shouldRedirect) {
            this.url = url;
            this.method = method;
            this.expectedResponse = expectedResponse;
            this.shouldRedirect = shouldRedirect;
        }

        @Override
        public String toString() {
            return "HttpCall{" +
                    "url='" + url + '\'' +
                    ", method='" + method + '\'' +
                    ", expectedResponse=" + expectedResponse +
                    ", shouldRedirect=" + shouldRedirect +
                    '}';
        }
    }

    @DataPoint
    public static final HttpCall GET_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethod.GET.asString(), HttpStatus.MOVED_PERMANENTLY_301, true);
    @DataPoint
    public static final HttpCall GET_CALL_TO_CRUISE_WITH_NO_SUBPATH = new HttpCall("", HttpMethod.GET.asString(), HttpStatus.MOVED_PERMANENTLY_301, true);
    @DataPoint
    public static final HttpCall HEAD_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethod.HEAD.asString(), HttpStatus.MOVED_PERMANENTLY_301, true);
    @DataPoint
    public static final HttpCall HEAD_CALL_TO_CRUISE_WITH_NO_SUBPATH = new HttpCall("", HttpMethod.HEAD.asString(), HttpStatus.MOVED_PERMANENTLY_301, true);

    @Theory
    public void shouldRedirectCruiseContextTargetedRequestsToCorrespondingGoUrl(HttpCall httpCall) throws Exception {
        HttpChannel<?> channel = mock(HttpChannel.class);
        Request request = new Request(channel, mock(HttpInput.class));
        request.setPathInfo("/cruise" + httpCall.url);
        request.setScheme("http");
        request.setServerName("go-server");
        request.setServerPort(1234);
        request.setDispatcherType(DispatcherType.REQUEST);
        HttpConfiguration httpConfiguration = mock(HttpConfiguration.class);
        when(httpConfiguration.getOutputBufferSize()).thenReturn(1000);
        when(channel.getHttpConfiguration()).thenReturn(httpConfiguration);
        HttpOutput httpOutput = mock(HttpOutput.class);
        ArgumentCaptor<byte[]> argumentCaptor = ArgumentCaptor.forClass(byte[].class);
        Response response = new Response(channel, httpOutput);
        when(channel.getRequest()).thenReturn(request);
        when(channel.getResponse()).thenReturn(response);
        server.start();

        jetty9Server.getServer().handle(channel);

        assertThat(response.getStatus(), is(httpCall.expectedResponse));
        if (httpCall.shouldRedirect) {
            assertThat(response.getHeader("Location"), is("http://go-server:1234" + "/go" + httpCall.url));
        }

        assertThat(response.getHeader(HttpHeaders.CONTENT_TYPE).contains("text/plain"), is(true));
        verify(httpOutput).write(argumentCaptor.capture(), any(Integer.class), any(Integer.class));
        assertThat(new String(argumentCaptor.getValue()).trim(), is("Url(s) starting in '/cruise' have been permanently moved to '/go', please use the new path."));
    }

    // WebAppContext requires a lot more setup to be done which is not required for these tests, hence this stub server
    private class CustomizedJettyServerWithoutWebAppHandler extends Server {
        private boolean isStarted = false;

        @Override
        protected void doStart() throws Exception {
            isStarted = true;
            HandlerCollection handlerCollection = (HandlerCollection) getHandler();
            WebAppContext webAppContext = (WebAppContext) ListUtil.find(ArrayUtil.asList(handlerCollection.getHandlers()), new ListUtil.Condition() {
                @Override
                public <T> boolean isMet(T item) {
                    return item instanceof WebAppContext;
                }
            });
            handlerCollection.removeHandler(webAppContext);
            handlerCollection.start();
        }

        @Override
        public boolean isStarted() {
            return isStarted;
        }
    }
}
