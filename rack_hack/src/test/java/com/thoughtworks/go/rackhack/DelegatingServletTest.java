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

package com.thoughtworks.go.rackhack;

import com.thoughtworks.go.server.util.ServletHelper;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.isA;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DelegatingServletTest {
    private com.thoughtworks.go.server.util.ServletRequest servletRequestWrapper;
    private HttpServletRequest httpServletRequest;

    @Before
    public void setUp() throws Exception {
        ServletHelper servletHelper = mock(ServletHelper.class);
        ReflectionUtil.setStaticField(ServletHelper.class, "instance", servletHelper);
        servletRequestWrapper = mock(com.thoughtworks.go.server.util.ServletRequest.class);
        httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getRequestURI()).thenReturn("/go/rails/stuff/action");
        when(servletHelper.getRequest(httpServletRequest)).thenReturn(servletRequestWrapper);
    }

    @Test
    public void shouldDelegateToTheGivenServlet() throws IOException, ServletException {
        MockServletContext ctx = new MockServletContext();
        ctx.addInitParameter(DelegatingListener.DELEGATE_SERVLET, DummyServlet.class.getCanonicalName());
        ServletContextEvent evt = new ServletContextEvent(ctx);
        DelegatingListener listener = new DelegatingListener();
        listener.contextInitialized(evt);
        assertThat((DummyServlet) ctx.getAttribute(DelegatingListener.DELEGATE_SERVLET), isA(DummyServlet.class));
        DelegatingServlet servlet = new DelegatingServlet();
        servlet.init(new MockServletConfig(ctx));

        servlet.service(httpServletRequest, new MockHttpServletResponse());
        verify(servletRequestWrapper).setRequestURI("/go/stuff/action");
    }
}
