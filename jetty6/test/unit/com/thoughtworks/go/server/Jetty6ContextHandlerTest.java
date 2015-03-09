package com.thoughtworks.go.server;

import com.sdicons.json.validator.impl.predicates.Str;
import com.thoughtworks.go.server.util.GoJetty6CipherSuite;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
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

public class Jetty6ContextHandlerTest {

    private Jetty6Server jetty6Server;

    @Before
    public void setUp() throws Exception {
        jetty6Server = new Jetty6Server(mock(SystemEnvironment.class), "pwd", mock(SSLSocketFactory.class), mock(Server.class), mock(GoJetty6CipherSuite.class), mock(Jetty6GoWebXmlConfiguration.class));
    }

    @Test
    public void shouldGetWelcomeFileHandlerToHandleRequestsToRootUrl() throws IOException, ServletException {
        Jetty6Server.GoServerWelcomeFileHandler welcomeFileHandler = (Jetty6Server.GoServerWelcomeFileHandler) jetty6Server.welcomeFileHandler();
        HttpServletResponse response = mock(HttpServletResponse.class);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        when(response.getWriter()).thenReturn(new PrintWriter(output));

        welcomeFileHandler.handle("/", mock(HttpServletRequest.class), response, 1);
        String responseBody = new String(output.toByteArray());
        assertThat(responseBody, is("redirecting.."));

        verify(response).setHeader("Location", "/go/home");
        verify(response).setStatus(HttpStatus.ORDINAL_301_Moved_Permanently);
        verify(response).setHeader(HttpHeaders.CONTENT_TYPE, "text/html");
        verify(response).getWriter();
        verifyNoMoreInteractions(response);
    }

    @Test
    public void shouldGetWelcomeFileHandlerToIgnoreRequestsToNonRootUrl() throws IOException, ServletException {
        Jetty6Server.GoServerWelcomeFileHandler welcomeFileHandler = (Jetty6Server.GoServerWelcomeFileHandler) jetty6Server.welcomeFileHandler();
        HttpServletResponse response = mock(HttpServletResponse.class);

        welcomeFileHandler.handle("/foo", mock(HttpServletRequest.class), response, 1);
        verifyNoMoreInteractions(response);
    }

    @Test
    public void shouldGetLegacyRequestHandlerToHandleAllRequestSentToCruiseUrl() throws IOException, ServletException {
        Jetty6Server.LegacyUrlRequestHandler legacyUrlRequestHandler = (Jetty6Server.LegacyUrlRequestHandler) jetty6Server.legacyRequestHandler();
        HttpServletResponse response = mock(HttpServletResponse.class);
        PrintWriter writer = mock(PrintWriter.class);
        when(response.getWriter()).thenReturn(writer);
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(HttpMethods.GET);

        legacyUrlRequestHandler.handle("/cruise/foo", request, response, 1);
        verify(response).setHeader("Location", "/go/foo");
        verify(response).setStatus(HttpStatus.ORDINAL_301_Moved_Permanently);
        verify(response).setHeader("Content-Type", "text/plain");
        verify(writer).write(String.format("Url(s) starting in '/cruise' have been permanently moved to '/go', please use the new path."));
        verify(writer).close();
    }

    @Test
    public void shouldNotRedirectNonCruiseRequestsToGoPage() throws IOException, ServletException {
        Handler legacyRequestHandler = jetty6Server.legacyRequestHandler();
        HttpServletResponse response = mock(HttpServletResponse.class);

        when(response.getWriter()).thenReturn(new PrintWriter(new ByteArrayOutputStream()));

        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn(HttpMethods.GET);
        legacyRequestHandler.handle("/cruise_but_not_quite", req, response, Handler.REQUEST);
        verifyNoMoreInteractions(response);
        legacyRequestHandler.handle("/something_totally_different", req, response, Handler.REQUEST);
        verifyNoMoreInteractions(response);
    }

}
