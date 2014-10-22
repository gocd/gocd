package com.thoughtworks.go.server;

import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Test;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.resource.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class AssetsContextHandlerTest {
    @Test
    public void shouldSetHeadersAndBaseDirectory() throws IOException {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        WebAppContext webAppContext = mock(WebAppContext.class);
        when(webAppContext.getInitParameter("rails.root")).thenReturn("rails.root");
        when(webAppContext.getWebInf()).thenReturn(Resource.newResource("WEB-INF"));
        AssetsContextHandler handler = new AssetsContextHandler(systemEnvironment);
        handler.init(webAppContext);
        assertThat(handler.getHandler() instanceof ResourceHandler, is(true));
        ResourceHandler resourceHandler = (ResourceHandler) handler.getHandler();
        assertThat(resourceHandler.getCacheControl(), is("max-age=31536000,public"));
        assertThat(resourceHandler.getResourceBase(), is(new File("WEB-INF/rails.root/public/assets").toURI().toString()));
    }

    @Test
    public void shouldHandleIfTargetMatchesContextPath() throws IOException, ServletException {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.get(SystemEnvironment.USE_NEW_RAILS)).thenReturn(true);
        when(systemEnvironment.useCompressedJs()).thenReturn(true);
        when(systemEnvironment.getWebappContextPath()).thenReturn("/go");
        WebAppContext webAppContext = mock(WebAppContext.class);
        when(webAppContext.getInitParameter("rails.root")).thenReturn("rails.root");
        when(webAppContext.getWebInf()).thenReturn(Resource.newResource("WEB-INF"));
        AssetsContextHandler handler = spy(new AssetsContextHandler(systemEnvironment));
        handler.init(webAppContext);
        Request request = mock(Request.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.isHandled()).thenReturn(false);
        handler.handle("/go/assets/junk", request, response, 1);
        verify(handler).superDotHandle("/go/assets/junk", request, response, 1);
    }

    @Test
    public void shouldNotHandleOnlyIfTargetDoesNotMatcheContextPath() throws IOException, ServletException {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.get(SystemEnvironment.USE_NEW_RAILS)).thenReturn(true);
        when(systemEnvironment.useCompressedJs()).thenReturn(true);
        when(systemEnvironment.getWebappContextPath()).thenReturn("/go");
        WebAppContext webAppContext = mock(WebAppContext.class);
        when(webAppContext.getInitParameter("rails.root")).thenReturn("rails.root");
        when(webAppContext.getWebInf()).thenReturn(Resource.newResource("WEB-INF"));
        AssetsContextHandler handler = spy(new AssetsContextHandler(systemEnvironment));
        handler.init(webAppContext);
        Request request = mock(Request.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.isHandled()).thenReturn(false);
        handler.handle("/junk", request, response, 1);
        verify(handler, never()).superDotHandle("/junk", request, response, 1);
    }

    @Test
    public void shouldNotHandleForRails2() throws IOException, ServletException {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.get(SystemEnvironment.USE_NEW_RAILS)).thenReturn(false);
        when(systemEnvironment.useCompressedJs()).thenReturn(true);
        when(systemEnvironment.getWebappContextPath()).thenReturn("/go");
        WebAppContext webAppContext = mock(WebAppContext.class);
        when(webAppContext.getInitParameter("rails.root")).thenReturn("rails.root");
        when(webAppContext.getWebInf()).thenReturn(Resource.newResource("WEB-INF"));
        AssetsContextHandler handler = spy(new AssetsContextHandler(systemEnvironment));
        handler.init(webAppContext);
        Request request = mock(Request.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.isHandled()).thenReturn(false);
        handler.handle("/go/assets/junk", request, response, 1);
        verify(handler, never()).superDotHandle("/go/assets/junk", request, response, 1);
    }

    @Test
    public void shouldNotHandleForRails4DevelopmentMode() throws IOException, ServletException {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.get(SystemEnvironment.USE_NEW_RAILS)).thenReturn(true);
        when(systemEnvironment.useCompressedJs()).thenReturn(false);
        when(systemEnvironment.getWebappContextPath()).thenReturn("/go");
        WebAppContext webAppContext = mock(WebAppContext.class);
        when(webAppContext.getInitParameter("rails.root")).thenReturn("rails.root");
        when(webAppContext.getWebInf()).thenReturn(Resource.newResource("WEB-INF"));
        AssetsContextHandler handler = spy(new AssetsContextHandler(systemEnvironment));
        handler.init(webAppContext);
        Request request = mock(Request.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.isHandled()).thenReturn(false);
        handler.handle("/go/assets/junk", request, response, 1);
        verify(handler, never()).superDotHandle("/go/assets/junk", request, response, 1);
    }
}
