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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import javax.net.ssl.SSLSocketFactory;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.helpers.FileSystemUtils;
import com.thoughtworks.go.server.util.GoCipherSuite;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.validators.Validation;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mortbay.component.Container;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpHeaders;
import org.mortbay.jetty.HttpMethods;
import org.mortbay.jetty.HttpStatus;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.webapp.WebAppContext;
import org.xml.sax.SAXException;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(Theories.class)
public class GoServerContextHandlersTest {
    private SSLSocketFactory sslSocketFactory;
    private File addonDir = new File("test-addons");

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
        final JettyServer server = mock(JettyServer.class);
        when(server.getContainer()).thenReturn(new Container());
        when(server.getServer()).thenReturn(mock(Server.class));
        SystemEnvironment environment = spy(new SystemEnvironment());
        final WebAppContext mainWebapp = mock(WebAppContext.class);
        final GoServer.GoServerWelcomeFileHandler welcomeHandler = mock(GoServer.GoServerWelcomeFileHandler.class);
        final GoServer.LegacyUrlRequestHandler legacyRequestHandler = mock(GoServer.LegacyUrlRequestHandler.class);

        GoServer goServer = new GoServer(environment, new GoCipherSuite(sslSocketFactory), null) {
            @Override JettyServer createServer() {
                return server;
            }

            @Override WebAppContext webApp() throws IOException, SAXException, ClassNotFoundException, UnavailableException {
                return mainWebapp;
            }

            @Override ContextHandler welcomeFileHandler() {
                return welcomeHandler;
            }

            @Override public ContextHandler legacyRequestHandler() {
                return legacyRequestHandler;
            }

            @Override
            Validation validate() {
                return Validation.SUCCESS;
            }
        };
        assertThat(goServer.configureServer(), sameInstance(server));
        verify(server).addHandler(welcomeHandler);
        verify(server).addHandler(legacyRequestHandler);
        verify(server).addWebAppHandler(mainWebapp);
    }

    @Test
    public void shouldStopServerAndThrowExceptionWhenServerFailsToStartWithAnUnhandledException() throws Exception {
        final JettyServer server = mock(JettyServer.class);
        final WebAppContext webAppContext = mock(WebAppContext.class);

        when(server.getContainer()).thenReturn(new Container());
        when(server.getServer()).thenReturn(mock(Server.class));

        GoServer goServer = new GoServer() {
            @Override
            WebAppContext webApp() throws IOException, SAXException, ClassNotFoundException, UnavailableException {
                return webAppContext;
            }

            @Override
            JettyServer createServer() {
                return server;
            }
        };

        doNothing().when(server).start();
        doNothing().when(server).stop();
        doReturn(webAppContext).when(server).webAppContext();
        when(webAppContext.getUnavailableException()).thenReturn(new RuntimeException("Some unhandled server startup exception"));

        try {
            goServer.startServer();
            fail("Should have thrown an exception");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Failed to start Go server."));
            assertThat(e.getCause().getMessage(), is("Some unhandled server startup exception"));
        }

        InOrder inOrder = inOrder(server, webAppContext);
        inOrder.verify(server).start();
        inOrder.verify(webAppContext).getUnavailableException();
        inOrder.verify(server).stop();
    }

    @Test
    public void shouldRedirectRootRequestsToWelcomePage() throws IOException, ServletException {
        GoServer goServer = new GoServer(new SystemEnvironment(), new GoCipherSuite(sslSocketFactory), mock(GoWebXmlConfiguration.class));
        Handler welcomeHandler = goServer.welcomeFileHandler();
        HttpServletResponse response = mock(HttpServletResponse.class);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        when(response.getWriter()).thenReturn(new PrintWriter(output));
        welcomeHandler.handle("/", mock(HttpServletRequest.class), response, Handler.REQUEST);

        String responseBody = new String(output.toByteArray());
        assertThat(responseBody, is("redirecting.."));

        verify(response).setHeader("Location", "/go/home");
        verify(response).setStatus(HttpStatus.ORDINAL_301_Moved_Permanently);
        verify(response).setHeader(HttpHeaders.CONTENT_TYPE, "text/html");
        verify(response).getWriter();
        verifyNoMoreInteractions(response);
    }

    @Test
    public void shouldMatchRootUrlAsHandlerContextForWelcomePage() {
        GoServer goServer = new GoServer(new SystemEnvironment(), new GoCipherSuite(sslSocketFactory), mock(GoWebXmlConfiguration.class));
        ContextHandler handler = goServer.welcomeFileHandler();
        assertThat(handler.getContextPath(), is("/"));
    }
    
    @Test
    public void shouldMatchCruiseUrlContextAsHandlerContextForLegacyRequestHandler() {
        GoServer goServer = new GoServer(new SystemEnvironment(), new GoCipherSuite(sslSocketFactory), mock(GoWebXmlConfiguration.class));
        ContextHandler handler = goServer.legacyRequestHandler();
        assertThat(handler.getContextPath(), is("/cruise"));
    }

    @Test
    public void shouldNotRedirectNonRootRequestsToWelcomePage() throws IOException, ServletException {
        GoServer goServer = new GoServer(new SystemEnvironment(), new GoCipherSuite(sslSocketFactory), mock(GoWebXmlConfiguration.class));
        Handler welcomeHandler = goServer.welcomeFileHandler();
        HttpServletResponse response = mock(HttpServletResponse.class);

        welcomeHandler.handle("/foo", mock(HttpServletRequest.class), response, Handler.REQUEST);
        verifyNoMoreInteractions(response);
    }

    @Test
    public void shouldNotRedirectNonCruiseRequestsToGoPage() throws IOException, ServletException {
        GoServer goServer = new GoServer(new SystemEnvironment(), new GoCipherSuite(sslSocketFactory), mock(GoWebXmlConfiguration.class));
        Handler legacyRequestHandler = goServer.legacyRequestHandler();
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(response.getWriter()).thenReturn(new PrintWriter(new ByteArrayOutputStream()));

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn(HttpMethods.GET);
        legacyRequestHandler.handle("/cruise_but_not_quite", req, response, Handler.REQUEST);
        verifyNoMoreInteractions(response);
        legacyRequestHandler.handle("/something_totally_different", req, response, Handler.REQUEST);
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

        GoServer goServerWithMultipleAddons = new GoServer(setAddonsPathTo(addonsDirectory), new GoCipherSuite(sslSocketFactory), mock(GoWebXmlConfiguration.class));
        assertExtraClasspath(goServerWithMultipleAddons.webApp(), "test-addons/some-addon-dir/addon-1.JAR", "test-addons/some-addon-dir/addon-2.jar", "test-addons/some-addon-dir/addon-3.jAR");

        GoServer goServerWithOneAddon = new GoServer(setAddonsPathTo(oneAddonDirectory), new GoCipherSuite(sslSocketFactory), mock(GoWebXmlConfiguration.class));
        assertExtraClasspath(goServerWithOneAddon.webApp(), "test-addons/one-addon-dir/addon-1.jar");

        GoServer goServerWithNoAddon = new GoServer(setAddonsPathTo(noAddonDirectory), new GoCipherSuite(sslSocketFactory), mock(GoWebXmlConfiguration.class));
        assertThat(goServerWithNoAddon.webApp().getExtraClasspath(), is(""));

        GoServer goServerWithInaccessibleAddonDir = new GoServer(setAddonsPathTo(new File("non-existent-directory")), new GoCipherSuite(sslSocketFactory), mock(GoWebXmlConfiguration.class));
        assertThat(goServerWithInaccessibleAddonDir.webApp().getExtraClasspath(), is(""));
    }

    private void assertExtraClasspath(WebAppContext context, String... expectedClassPathJars) {
        List<String> actualExtraClassPath = Arrays.asList(context.getExtraClasspath().split(","));

        assertEquals("Number of addons wrong. Expected: " + Arrays.asList(expectedClassPathJars) + ". Actual: " + actualExtraClassPath, expectedClassPathJars.length, actualExtraClassPath.size());
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

    @DataPoint public static final HttpCall GET_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethods.GET, HttpStatus.ORDINAL_301_Moved_Permanently, true);
    @DataPoint public static final HttpCall GET_CALL_TO_CRUISE_WITH_NO_SUBPATH = new HttpCall("", HttpMethods.GET, HttpStatus.ORDINAL_301_Moved_Permanently, true);
    @DataPoint public static final HttpCall HEAD_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethods.HEAD, HttpStatus.ORDINAL_301_Moved_Permanently, true);
    @DataPoint public static final HttpCall HEAD_CALL_TO_CRUISE_WITH_NO_SUBPATH = new HttpCall("", HttpMethods.HEAD, HttpStatus.ORDINAL_301_Moved_Permanently, true);

    @DataPoint public static final HttpCall PUT_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethods.PUT, HttpStatus.ORDINAL_404_Not_Found, false);
    @DataPoint public static final HttpCall POST_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethods.POST, HttpStatus.ORDINAL_404_Not_Found, false);
    @DataPoint public static final HttpCall DELETE_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethods.DELETE, HttpStatus.ORDINAL_404_Not_Found, false);
    @DataPoint public static final HttpCall OPTIONS_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethods.OPTIONS, HttpStatus.ORDINAL_404_Not_Found, false);
    @DataPoint public static final HttpCall TRACE_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethods.TRACE, HttpStatus.ORDINAL_404_Not_Found, false);
    @DataPoint public static final HttpCall CONNECT_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethods.CONNECT, HttpStatus.ORDINAL_404_Not_Found, false);
    @DataPoint public static final HttpCall MOVE_CALL_TO_CRUISE = new HttpCall("/foo/bar?baz=quux", HttpMethods.MOVE, HttpStatus.ORDINAL_404_Not_Found, false);

    @Theory
    public void shouldRedirectCruiseContextTargetedRequestsToCorrespondingGoUrl(HttpCall httpCall) throws IOException, ServletException {
        GoServer goServer = new GoServer(new SystemEnvironment(), new GoCipherSuite(sslSocketFactory), mock(GoWebXmlConfiguration.class));
        Handler legacyHandler = goServer.legacyRequestHandler();
        HttpServletResponse response = mock(HttpServletResponse.class);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        when(response.getWriter()).thenReturn(new PrintWriter(output));
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(httpCall.method);
        legacyHandler.handle("/cruise" + httpCall.url, request, response, Handler.REQUEST);

        String responseBody = new String(output.toByteArray());
        assertThat(responseBody, is("Url(s) starting in '/cruise' have been permanently moved to '/go', please use the new path."));

        verify(response).setStatus(httpCall.expectedResponse);

        if (httpCall.shouldRedirect) {
            verify(response).setHeader("Location", "/go" + httpCall.url);
        }

        verify(response).setHeader(HttpHeaders.CONTENT_TYPE, "text/plain");
        verify(response).getWriter();
        verifyNoMoreInteractions(response);
    }
}
