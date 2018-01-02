/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.HandlerWrapper;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
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


    private AssetsContextHandler handler;
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.getWebappContextPath()).thenReturn("/go");
        WebAppContext webAppContext = mock(WebAppContext.class);
        when(webAppContext.getInitParameter("rails.root")).thenReturn("/rails.root");
        when(webAppContext.getWebInf()).thenReturn(Resource.newResource("WEB-INF"));
        handler = new AssetsContextHandler(systemEnvironment);

        handler.init(webAppContext);
    }

    @Test
    public void shouldSetHeadersAndBaseDirectory() throws IOException {
        assertThat(handler.getContextPath(), is("/go/assets"));
        assertThat(((HandlerWrapper) handler.getHandler()).getHandler() instanceof AssetsContextHandler.AssetsHandler, is(true));
        AssetsContextHandler.AssetsHandler assetsHandler = (AssetsContextHandler.AssetsHandler) ((HandlerWrapper) handler.getHandler()).getHandler();
        ResourceHandler resourceHandler = (ResourceHandler) ReflectionUtil.getField(assetsHandler, "resourceHandler");
        assertThat(resourceHandler.getCacheControl(), is("max-age=31536000,public"));
        assertThat(resourceHandler.getResourceBase(), isSameFileAs(new File("WEB-INF/rails.root/public/assets").toURI().toString()));
    }

    @Test
    public void shouldPassOverHandlingToResourceHandler() throws IOException, ServletException {
        when(systemEnvironment.useCompressedJs()).thenReturn(true);
        String target = "/go/assets/junk";
        Request request = mock(Request.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Request baseRequest = mock(Request.class);
        ResourceHandler resourceHandler = mock(ResourceHandler.class);
        handler.setHandler(resourceHandler);

        handler.getHandler().handle(target, baseRequest, request, response);
        verify(resourceHandler).handle(target, baseRequest, request, response);
    }

    @Test
    public void shouldNotHandleForRails4DevelopmentMode() throws IOException, ServletException {
        when(systemEnvironment.useCompressedJs()).thenReturn(false);

        String target = "/go/assets/junk";
        Request request = mock(Request.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        Request baseRequest = mock(Request.class);
        ResourceHandler resourceHandler = mock(ResourceHandler.class);
        ReflectionUtil.setField(((HandlerWrapper) handler.getHandler()).getHandler(), "resourceHandler", resourceHandler);

        handler.getHandler().handle(target, baseRequest, request, response);
        verify(resourceHandler, never()).handle(any(String.class), any(Request.class), any(HttpServletRequest.class), any(HttpServletResponse.class));
    }

    private Matcher<? super String> isSameFileAs(final String expected) {
        return new BaseMatcher<String>() {
            @Override
            public boolean matches(Object o) {
                String actualFile = (String) o;

                if ("Windows".equals(new SystemEnvironment().getOperatingSystemFamilyName())) {
                    return expected.equalsIgnoreCase(actualFile);
                }
                return expected.equals(actualFile);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("     " + expected);
            }
        };
    }
}
