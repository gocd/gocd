/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.rackhack;

import com.thoughtworks.go.server.util.ServletHelper;
import com.thoughtworks.go.server.util.ServletRequest;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DelegatingServletTest {

    @Mock
    private ServletRequest servletRequestWrapper;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private ServletHelper servletHelper;

    @Captor
    private ArgumentCaptor<Function<String, String>> pathModifierCaptor;

    @BeforeEach
    public void setUp() {
        ReflectionUtil.setStaticField(ServletHelper.class, "instance", servletHelper);
        when(servletHelper.getRequest(httpServletRequest)).thenReturn(servletRequestWrapper);
    }

    @Test
    public void shouldDelegateToTheGivenServlet() throws IOException, ServletException {
        MockServletContext ctx = new MockServletContext();
        ctx.addInitParameter(DelegatingListener.DELEGATE_SERVLET, DummyServlet.class.getCanonicalName());
        ServletContextEvent evt = new ServletContextEvent(ctx);
        DelegatingListener listener = new DelegatingListener();
        listener.contextInitialized(evt);
        assertThat(ctx.getAttribute(DelegatingListener.DELEGATE_SERVLET)).isInstanceOf(DummyServlet.class);
        DelegatingServlet servlet = new DelegatingServlet();
        servlet.init(new MockServletConfig(ctx));

        servlet.service(httpServletRequest, new MockHttpServletResponse());
        verify(servletRequestWrapper).modifyPath(pathModifierCaptor.capture());

        Function<String, String> pathModifier = pathModifierCaptor.getValue();
        assertThat(pathModifier.apply("/go/rails/")).isEqualTo("/go/");
        assertThat(pathModifier.apply("/go/rails")).isEqualTo("/go/rails");
        assertThat(pathModifier.apply("rails")).isEqualTo("rails");
    }
}
