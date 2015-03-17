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

import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.junit.Test;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
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
        when(systemEnvironment.getWebappContextPath()).thenReturn("/go");
        WebAppContext webAppContext = mock(WebAppContext.class);
        when(webAppContext.getInitParameter("rails.root")).thenReturn("/rails.root");
        when(webAppContext.getWebInf()).thenReturn(Resource.newResource("WEB-INF"));
        AssetsContextHandler handler = new AssetsContextHandler(systemEnvironment);

        handler.init(webAppContext);

        assertThat(handler.getContextPath(), is("/go/assets"));
        assertThat(handler.getHandler() instanceof AssetsContextHandler.AssetsHandler, is(true));
        AssetsContextHandler.AssetsHandler assetsHandler = (AssetsContextHandler.AssetsHandler) handler.getHandler();
        ResourceHandler resourceHandler = (ResourceHandler) ReflectionUtil.getField(assetsHandler, "resourceHandler");
        assertThat(resourceHandler.getCacheControl(), is("max-age=31536000,public"));
        assertThat(resourceHandler.getResourceBase(), is(new File("WEB-INF/rails.root/public/assets").toURI().toString()));
    }

    @Test
    public void shouldHandleIfTargetMatchesContextPath() throws IOException, ServletException {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
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
        verify(handler).superDotHandle(eq("/go/assets/junk"), any(Request.class), eq(request), eq(response));
    }

    @Test
    public void shouldNotHandleOnlyIfTargetDoesNotMatcheContextPath() throws IOException, ServletException {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
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
        verify(handler, never()).superDotHandle(any(String.class), any(Request.class), any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    @Test
    public void shouldNotHandleForRails4DevelopmentMode() throws IOException, ServletException {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
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
        verify(handler, never()).superDotHandle(any(String.class), any(Request.class), any(HttpServletRequest.class), any(HttpServletResponse.class));
    }
}
