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

import com.thoughtworks.go.server.util.GoJetty6CipherSuite;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;
import org.mortbay.jetty.*;

import javax.net.ssl.SSLSocketFactory;
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
    private Handler legacyHandler;

    @Before
    public void setUp() throws Exception {
        Jetty6Server jettyServer = new Jetty6Server(mock(SystemEnvironment.class), "pwd", mock(SSLSocketFactory.class), new Server(), mock(GoJetty6CipherSuite.class), mock(Jetty6GoWebXmlConfiguration.class));
        legacyHandler = jettyServer.legacyRequestHandler();
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
