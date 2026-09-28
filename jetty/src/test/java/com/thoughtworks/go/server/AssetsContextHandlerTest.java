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
package com.thoughtworks.go.server;

import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.eclipse.jetty.ee8.nested.HandlerWrapper;
import org.eclipse.jetty.ee8.webapp.WebAppContext;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.Callback;
import org.eclipse.jetty.util.resource.ResourceFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AssetsContextHandlerTest {

    private AssetsContextHandler handler;
    @Mock
    private SystemEnvironment systemEnvironment;
    @Mock
    private WebAppContext webAppContext;

    @BeforeEach
    public void setUp() throws Exception {
        when(webAppContext.getInitParameter("rails.root")).thenReturn("/rails.root");
        when(webAppContext.getWebInf()).thenReturn(ResourceFactory.root().newResource("WEB-INF"));
        handler = new AssetsContextHandler(systemEnvironment);

        handler.init(webAppContext);
    }

    @Test
    public void shouldSetHeadersAndBaseDirectory() {
        assertThat(handler.getContextPath()).isEqualTo("/go/assets");
        assertThat(((HandlerWrapper) handler.getHandler()).getHandler() instanceof AssetsContextHandler.AssetsHandler).isEqualTo(true);
        AssetsContextHandler.AssetsHandler assetsHandler = (AssetsContextHandler.AssetsHandler) ((HandlerWrapper) handler.getHandler()).getHandler();
        ResourceHandler resourceHandler = ReflectionUtil.getField(assetsHandler, "resourceHandler");
        assertThat(resourceHandler.getCacheControl()).isEqualTo("max-age=31536000,public");
        assertThat(resourceHandler.getBaseResource().getURI()).isEqualTo(new File("WEB-INF/rails.root/public/assets").toPath().toAbsolutePath().toUri());
    }

    @Test
    public void shouldPassOverHandlingToResourceHandler() throws Exception {
        Request request = mock(Request.class);
        Response response = mock(Response.class);
        Callback callback = mock(Callback.class);
        AssetsContextHandler.AssetsHandler resourceHandler = mock(AssetsContextHandler.AssetsHandler.class);
        handler.setHandler(resourceHandler);

        handler.getHandler().handle(request, response, callback);
        verify(resourceHandler).handle(request, response, callback);
    }

    @Test
    public void shouldNotHandleForDevelopmentMode() throws Exception {
        when(systemEnvironment.isDevMode()).thenReturn(true);

        Request request = mock(Request.class);
        Response response = mock(Response.class);
        Callback callback = mock(Callback.class);
        ResourceHandler resourceHandler = mock(ResourceHandler.class);
        ReflectionUtil.setField(((HandlerWrapper) handler.getHandler()).getHandler(), "resourceHandler", resourceHandler);

        handler.getHandler().handle(request, response, callback);
        verify(resourceHandler, never()).handle(any(), any(), any());
    }
}
