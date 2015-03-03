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

package com.thoughtworks.go.server;

import com.thoughtworks.go.helpers.FileSystemUtils;
import com.thoughtworks.go.server.util.GoCipherSuite;
import com.thoughtworks.go.util.ArrayUtil;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.MovedContextHandler;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import javax.net.ssl.SSLSocketFactory;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(Theories.class)
public class GoServerContextHandlersTest{
    private SSLSocketFactory sslSocketFactory;
    private File addonDir = new File("test-addons");
    @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Before
    public void setUp() throws Exception {
        sslSocketFactory = mock(SSLSocketFactory.class);
        when(sslSocketFactory.getSupportedCipherSuites()).thenReturn(new String[0]);
        addonDir.mkdirs();
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(addonDir);
    }

    @Test
    public void shouldRegisterAllHandlers() throws Exception {
        SystemEnvironment environment = spy(new SystemEnvironment());
        final Jetty9Server jettyServer = spy(new Jetty9Server(environment, "password", mock(SSLSocketFactory.class)));
//        when(jettyServer.getContainer()).thenReturn(new ServletContextHandler());
//        when(jettyServer.getServer()).thenReturn(new Server());
        final WebAppContext mainWebapp = mock(WebAppContext.class);
        final Jetty9Server.GoServerWelcomeFileHandler welcomeHandler = mock(Jetty9Server.GoServerWelcomeFileHandler.class);
        final Jetty9Server.LegacyUrlRequestHandler legacyRequestHandler = mock(Jetty9Server.LegacyUrlRequestHandler.class);

//        GoServer goServer = new GoServer(environment, new GoCipherSuite(sslSocketFactory), null) {
//            @Override
//            Jetty9Server createServer() {
//                return jettyServer;
//            }
//
//            @Override WebAppContext webApp() throws IOException, SAXException, ClassNotFoundException, UnavailableException {
//                return mainWebapp;
//            }
//
//            @Override ContextHandler welcomeFileHandler() {
//                return welcomeHandler;
//            }
//
//            @Override public Handler legacyRequestHandler() {
//                return legacyRequestHandler;
//            }
//
//            @Override
//            Validation validate() {
//                return Validation.SUCCESS;
//            }
//        };
        jettyServer.configure();

//        assertThat(goServer.configureServer(), sameInstance(jettyServer));
        ArgumentCaptor<HandlerCollection> captor = ArgumentCaptor.forClass(HandlerCollection.class);
        verify(jettyServer).setHandler(captor.capture());

        HandlerCollection handlerCollection = captor.getValue();
        List<Handler> handlers = ArrayUtil.asList(handlerCollection.getHandlers());
        assertThat(handlers.contains(welcomeHandler), is(true));
        assertThat(handlers.contains(legacyRequestHandler), is(true));
        assertThat(handlers.contains(mainWebapp), is(true));
    }


    @Test
    public void shouldRedirectRootRequestsToWelcomePage() throws Exception {
        ContextHandler welcomeFileHandler = new Jetty9Server(new SystemEnvironment(), "", mock(SSLSocketFactory.class)).welcomeFileHandler();
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
        ContextHandler handler = new Jetty9Server(new SystemEnvironment(), "", mock(SSLSocketFactory.class)).welcomeFileHandler();
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


    @Test
    public void shouldLoadAllJarsInTheAddonsDirectoryIntoClassPath() throws Exception {
        File addonsDirectory = createInAddonDir("some-addon-dir");
        FileSystemUtils.createFile("addon-1.JAR", addonsDirectory);
        FileSystemUtils.createFile("addon-2.jar", addonsDirectory);
        FileSystemUtils.createFile("addon-3.jAR", addonsDirectory);
        FileSystemUtils.createFile("some-file-which-does-not-end-with-dot-jar.txt", addonsDirectory);

        File oneAddonDirectory = createInAddonDir("one-addon-dir");
        FileSystemUtils.createFile("addon-1.jar", oneAddonDirectory);

        File noAddonDirectory = createInAddonDir("no-addon-dir");

        SystemEnvironment systemEnvironment = setAddonsPathTo(addonsDirectory);
        when(systemEnvironment.getCruiseWar()).thenReturn("webapp");
        Jetty9Server serverWithMultipleAddons = new Jetty9Server(systemEnvironment, null, mock(SSLSocketFactory.class));
        serverWithMultipleAddons.configure();

        assertExtraClasspath(getWebAppContext(serverWithMultipleAddons), "test-addons/some-addon-dir/addon-1.JAR", "test-addons/some-addon-dir/addon-2.jar", "test-addons/some-addon-dir/addon-3.jAR");

        systemEnvironment = setJRubyJarsTo(setAddonsPathTo(oneAddonDirectory));
        when(systemEnvironment.getCruiseWar()).thenReturn("webapp");
        Jetty9Server serverWithOneAddon = new Jetty9Server(systemEnvironment, null, mock(SSLSocketFactory.class));
        assertExtraClasspath(getWebAppContext(serverWithOneAddon), "test-addons/one-addon-dir/addon-1.jar");

        systemEnvironment = setJRubyJarsTo(setAddonsPathTo(noAddonDirectory));
        when(systemEnvironment.getCruiseWar()).thenReturn("webapp");
        Jetty9Server serverWithNoAddon = new Jetty9Server(systemEnvironment, null, mock(SSLSocketFactory.class));
        assertThat(getWebAppContext(serverWithNoAddon).getExtraClasspath(), is(""));

        systemEnvironment = setJRubyJarsTo(setAddonsPathTo(new File("non-existent-directory")));
        when(systemEnvironment.getCruiseWar()).thenReturn("webapp");
        Jetty9Server serverWithInaccessibleAddonDir = new Jetty9Server(systemEnvironment, null, mock(SSLSocketFactory.class));
        assertThat(getWebAppContext(serverWithInaccessibleAddonDir).getExtraClasspath(), is(""));
    }

    private WebAppContext getWebAppContext(Jetty9Server server) {
        return (WebAppContext) ReflectionUtil.getField(server, "webAppContext");
    }

    @Test
    public void shouldToggleRailsToOldOneWhenTheNewOneIsSpecified() throws Exception {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
		when(systemEnvironment.getCruiseWar()).thenReturn("webapp");
        when(systemEnvironment.get(SystemEnvironment.ADDONS_PATH)).thenReturn("some-non-existent-directory");
        when(systemEnvironment.keystore()).thenReturn(temporaryFolder.newFolder());
        when(systemEnvironment.truststore()).thenReturn(temporaryFolder.newFolder());
        when(systemEnvironment.getJettyConfigFile()).thenReturn(new File("junk"));
        SSLSocketFactory socketFactory = mock(SSLSocketFactory.class);
        when(socketFactory.getSupportedCipherSuites()). thenReturn(new String[]{});
        Jetty9Server jetty9Server = new Jetty9Server(systemEnvironment, "password", socketFactory);
        jetty9Server.configure();
        jetty9Server.setInitParameter("name", "value");

        assertThat(getWebAppContext(jetty9Server).getInitParameter("name"), is("value"));
    }

    private void assertExtraClasspath(WebAppContext context, String... expectedClassPathJars) {
        List<String> actualExtraClassPath = Arrays.asList(context.getExtraClasspath().split(","));

        assertEquals("Number of jars wrong. Expected: " + Arrays.asList(expectedClassPathJars) + ". Actual: " + actualExtraClassPath, expectedClassPathJars.length, actualExtraClassPath.size());
        for (String expectedClassPathJar : expectedClassPathJars) {
            String platformIndependantNameOfExpectedJar = expectedClassPathJar.replace("/", File.separator);
            assertTrue("Expected " + context.getExtraClasspath() + " to contain: " + platformIndependantNameOfExpectedJar, actualExtraClassPath.contains(platformIndependantNameOfExpectedJar));
        }
    }

    private File createInAddonDir(String dirInsideAddonDir) {
        File dirWhichWillContainAddons = new File(addonDir, dirInsideAddonDir);
        dirWhichWillContainAddons.mkdirs();
        return dirWhichWillContainAddons;
    }

    private SystemEnvironment setAddonsPathTo(File path) {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        doReturn(path.getPath()).when(systemEnvironment).get(SystemEnvironment.ADDONS_PATH);
        return systemEnvironment;
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

        @Override public String toString() {
            return "HttpCall{" +
                    "url='" + url + '\'' +
                    ", method='" + method + '\'' +
                    ", expectedResponse=" + expectedResponse +
                    ", shouldRedirect=" + shouldRedirect +
                    '}';
        }
    }

    @DataPoint public static final HttpCall GET_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethod.GET.asString(), HttpStatus.MOVED_PERMANENTLY_301, true);
    @DataPoint public static final HttpCall GET_CALL_TO_CRUISE_WITH_NO_SUBPATH = new HttpCall("", HttpMethod.GET.asString(), HttpStatus.MOVED_PERMANENTLY_301, true);
    @DataPoint public static final HttpCall HEAD_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethod.HEAD.asString(), HttpStatus.MOVED_PERMANENTLY_301, true);
    @DataPoint public static final HttpCall HEAD_CALL_TO_CRUISE_WITH_NO_SUBPATH = new HttpCall("", HttpMethod.HEAD.asString(), HttpStatus.MOVED_PERMANENTLY_301, true);

    @DataPoint public static final HttpCall PUT_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethod.PUT.asString(), HttpStatus.NOT_FOUND_404, false);
    @DataPoint public static final HttpCall POST_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethod.POST.asString(), HttpStatus.NOT_FOUND_404, false);
    @DataPoint public static final HttpCall DELETE_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethod.DELETE.asString(), HttpStatus.NOT_FOUND_404, false);
    @DataPoint public static final HttpCall OPTIONS_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethod.OPTIONS.asString(), HttpStatus.NOT_FOUND_404, false);
    @DataPoint public static final HttpCall TRACE_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethod.TRACE.asString(), HttpStatus.NOT_FOUND_404, false);
    @DataPoint public static final HttpCall CONNECT_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethod.CONNECT.asString(), HttpStatus.NOT_FOUND_404, false);
    @DataPoint public static final HttpCall MOVE_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethod.MOVE.asString(), HttpStatus.NOT_FOUND_404, false);

//    @Test
//    public void shouldRedirectLegacyUrlsToNewContext() throws Exception {
//        MovedContextHandler movedContextHandler = mock(MovedContextHandler.class);
//        GoServer.LegacyUrlRequestHandler handler = new GoServer.LegacyUrlRequestHandler(movedContextHandler);
//
//
//        HttpServletResponse response = mock(HttpServletResponse.class);
//
//        ByteArrayOutputStream output = new ByteArrayOutputStream();
//        when(response.getWriter()).thenReturn(new PrintWriter(output));
//        HttpServletRequest request = mock(HttpServletRequest.class);
//        when(request.getMethod()).thenReturn(HttpMethod.GET.asString());
//        String target = "/cruise/foo/bar?baz=quux";
//        Request baseRequest = mock(Request.class);
//        handler.handle(target, baseRequest,request, response);
//
//        String responseBody = new String(output.toByteArray());
//        assertThat(responseBody, is("Url(s) starting in '/cruise' have been permanently moved to '/go', please use the new path."));
//        verify(movedContextHandler).handle(target, baseRequest, request, response);
//        verify(response).setHeader(HttpHeaders.CONTENT_TYPE, "text/plain");
//        verify(response).setHeader(HttpHeaders.CONTENT_LENGTH, "91");
//        verify(response).getWriter();
//        verifyNoMoreInteractions(response);
//    }
//
//    @Theory
//    @Ignore("Need to rewrite this test - Jyoti")
//    public void shouldRedirectCruiseContextTargetedRequestsToCorrespondingGoUrl(HttpCall httpCall) throws IOException, ServletException {
//        GoServer goServer = new GoServer(new SystemEnvironment(), new GoCipherSuite(sslSocketFactory), mock(GoWebXmlConfiguration.class));
//        Handler legacyHandler = goServer.legacyRequestHandler();
//        HttpServletResponse response = mock(HttpServletResponse.class);
//
//        ByteArrayOutputStream output = new ByteArrayOutputStream();
//        when(response.getWriter()).thenReturn(new PrintWriter(output));
//        HttpServletRequest request = mock(HttpServletRequest.class);
//        when(request.getMethod()).thenReturn(httpCall.method);
//        legacyHandler.handle("/cruise" + httpCall.url, mock(Request.class),request, response);
//
//        String responseBody = new String(output.toByteArray());
//        assertThat(responseBody, is("Url(s) starting in '/cruise' have been permanently moved to '/go', please use the new path."));
//
//        verify(response).setStatus(httpCall.expectedResponse);
//
//        if (httpCall.shouldRedirect) {
//            verify(response).setHeader("Location", "/go" + httpCall.url);
//        }
//
//        verify(response).setHeader(HttpHeaders.CONTENT_TYPE, "text/plain");
//        verify(response).getWriter();
//        verifyNoMoreInteractions(response);
//    }


    private SystemEnvironment setJRubyJarsTo(SystemEnvironment mockSystemEnvironment) {
        return mockSystemEnvironment;
    }
}
