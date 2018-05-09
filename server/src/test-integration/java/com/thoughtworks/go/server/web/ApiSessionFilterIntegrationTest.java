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

package com.thoughtworks.go.server.web;

import com.thoughtworks.go.server.service.BackupService;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.*;
import org.springframework.security.util.FilterChainProxy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.DelegatingFilterProxy;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml",
        "classpath:testPropertyConfigurer.xml"
})
public class ApiSessionFilterIntegrationTest {
    @Autowired
    FilterChainProxy filterChainProxy;
    @Autowired
    SystemEnvironment systemEnvironment;

    private DelegatingFilterProxy proxy;
    private MockServletContext servletContext;

    @Before
    public void setUp() throws Exception {
        WebApplicationContext wac = mock(WebApplicationContext.class);
        when(wac.getBean("filterChainProxy", javax.servlet.Filter.class)).thenReturn(filterChainProxy);
        when(wac.getBean("backupService")).thenReturn(mock(BackupService.class));

        filterChainProxy.init(new MockFilterConfig());

        servletContext = new MockServletContext();
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, wac);

        proxy = new DelegatingFilterProxy("filterChainProxy", wac);
        proxy.setServletContext(servletContext);
    }

    @Test
    public void apiRequestsShouldHaveShortLivedSessions() throws Exception {
        assertShortLivedSessionFor(requestFor("/api/support"));
        assertShortLivedSessionFor(requestFor("/api/pipelines/my-pipeline-1/stages.xml"));
    }

    @Test
    public void cctrayRequestsShouldHaveShortLivedSessions() throws Exception {
        assertShortLivedSessionFor(requestFor("/cctray.xml"));
    }

    @Test
    public void idleTimeOfARequestWhichAlreadyHasASessionShouldNotBeShortened() throws Exception {
        HttpServletRequest apiRequestWhichHasSession = requestFor("/api/pipelines/my-pipeline-1/stages.xml");
        apiRequestWhichHasSession.getSession(true);

        assertLongLivedSessionFor(apiRequestWhichHasSession);
    }

    @Test
    public void nonApiRequestsShouldHaveLongLivedSession() throws Exception {
        assertLongLivedSessionFor(requestFor("/"));
        assertLongLivedSessionFor(requestFor("/home"));
        assertLongLivedSessionFor(requestFor("/pipelines/my-pipeline-1/390/my-stage-1/1"));
        assertLongLivedSessionFor(requestFor("/something-else"));
        assertLongLivedSessionFor(requestFor("/server/messages.json"));
        assertLongLivedSessionFor(requestFor("/pipelines.json"));
        assertLongLivedSessionFor(requestFor("/assets/g9/stage_bar_cancelled_icon-d22d661b3cb1b4bfbb17a2072aff9cb4.png"));
    }

    private void assertShortLivedSessionFor(HttpServletRequest request) throws ServletException, IOException {
        /* Short-lived. Non-zero. */
        assertThat(getSessionIdleTimeFor(request), is(systemEnvironment.get(SystemEnvironment.API_REQUEST_IDLE_TIMEOUT_IN_SECONDS)));
    }

    private void assertLongLivedSessionFor(HttpServletRequest request) throws ServletException, IOException {
        /* Zero because there is no session level timeout. It is the default. At web service level. */
        assertThat(getSessionIdleTimeFor(request), is(0));
    }

    private int getSessionIdleTimeFor(HttpServletRequest request) throws ServletException, IOException {
        proxy.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return request.getSession(false).getMaxInactiveInterval();
    }

    private HttpServletRequest requestFor(String requestPath) {
        MockHttpServletRequest request = new MockHttpServletRequest(servletContext, "GET", "/go/");
        request.setPathInfo(requestPath);
        return request;
    }
}
