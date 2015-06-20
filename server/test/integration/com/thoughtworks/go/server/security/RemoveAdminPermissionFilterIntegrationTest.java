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

package com.thoughtworks.go.server.security;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.thoughtworks.go.config.MergedGoConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.TestingAuthenticationToken;
import org.springframework.security.ui.FilterChainOrder;
import org.springframework.security.userdetails.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class RemoveAdminPermissionFilterIntegrationTest {
    private FilterChain chain;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private HttpSession session;

    @Autowired private GoConfigService goConfigService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private MergedGoConfig mergedGoConfig;

    private static final GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private TimeProvider timeProvider;

    @Before public void setUp() throws Exception {
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();

        chain = mock(FilterChain.class);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        session = mock(HttpSession.class);
        timeProvider = mock(TimeProvider.class);

        when(request.getRequestedSessionId()).thenReturn("session_id");
        when(timeProvider.currentTimeMillis()).thenReturn(100L);
        when(request.getSession()).thenReturn(session);
    }

    @After public void tearDown() throws IOException, ServletException {
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onTearDown();

        SecurityContextHolder.getContext().setAuthentication(null);
        verifyNoMoreInteractions(response);
    }

    @Test
    public void testGetOrder() throws IOException, ServletException {
        RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider);
        assertThat(filter.getOrder(), is(FilterChainOrder.BASIC_PROCESSING_FILTER - 1));
    }

    @Test
    public void testShouldContinueWithChainReturnIfAuthenticationIsNull() throws IOException, ServletException {
        RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider);
        filter.doFilterHttp(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoMoreInteractions(chain);
    }

    @Test
    public void testShouldContinueWithChainReturnIfCruiseConfigIsNull() throws IOException, ServletException {
        RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider);
        filter.doFilterHttp(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoMoreInteractions(chain);
    }

    @Test
    public void testShouldContinueWithTheChainIfTheSecurityConfigHasNotChanged() throws IOException, ServletException {
        Authentication authentication = setupAuthentication();

        RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider);

        filter.doFilterHttp(request, response, chain);
        modifyArtifactRoot();
        filter.doFilterHttp(request, response, chain);
        
        assertThat(authentication.isAuthenticated(), is(true));
    }

    private void modifyArtifactRoot() {
        configHelper.currentConfig().server().updateArtifactRoot("something/else");//Config changed but security config did not.
        mergedGoConfig.forceReload();
    }

    @Test
    public void testShouldReAuthenticateIffSecurityConfigChange() throws IOException, ServletException {
        Authentication authentication = setupAuthentication();
        when(session.getAttribute(RemoveAdminPermissionFilter.SECURITY_CONFIG_LAST_CHANGE)).thenReturn(0L).thenReturn(0L).thenReturn(100L);

        RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider);
        filter.initialize();

        filter.doFilterHttp(request, response, chain);

        assertThat(authentication.isAuthenticated(), is(true));

        turnOnSecurity("pavan");//This changes the security config

        filter.doFilterHttp(request, response, chain);

        assertThat(authentication.isAuthenticated(), is(false));

        authentication.setAuthenticated(true);

        modifyArtifactRoot();//This changes something else

        filter.doFilterHttp(request, response, chain);

        assertThat(authentication.isAuthenticated(), is(true));
    }

    @Test
    public void testShouldReAuthenticateOnlyOnceAfterConfigChange() throws IOException, ServletException {
        Authentication authentication = setupAuthentication();

        when(session.getAttribute(RemoveAdminPermissionFilter.SECURITY_CONFIG_LAST_CHANGE)).thenReturn(0L).thenReturn(0L).thenReturn(100L);

        RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider);
        filter.initialize();

        assertThat(authentication.isAuthenticated(), is(true));//good initial state

        filter.doFilterHttp(request, response, chain);

        turnOnSecurity("pavan");

        assertThat(authentication.isAuthenticated(), is(true));

        filter.doFilterHttp(request, response, chain);

        assertThat(authentication.isAuthenticated(), is(false));

        authentication.setAuthenticated(true);

        filter.doFilterHttp(request, response, chain);

        assertThat(authentication.isAuthenticated(), is(true));
    }

    @Test
    public void testShouldNotSetLastSecurityChangeAsASessionAttributeIfNotAuthenticatedYet() throws IOException, ServletException {
        when(timeProvider.currentTimeMillis()).thenReturn(100L);

        when(session.getAttribute(RemoveAdminPermissionFilter.SECURITY_CONFIG_LAST_CHANGE)).thenReturn(null);
        RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider);
        filter.initialize();
        turnOnSecurity("pavan");
        filter.doFilterHttp(request, response, chain);
        verifyNoMoreInteractions(session);
    }

    @Test
    public void testShouldSetLastSecurityChangeAsASessionAttributeIfItsNotFound() throws IOException, ServletException {
        setupAuthentication();
        when(timeProvider.currentTimeMillis()).thenReturn(100L);

        when(session.getAttribute(RemoveAdminPermissionFilter.SECURITY_CONFIG_LAST_CHANGE)).thenReturn(null);
        RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider);
        filter.initialize();
        turnOnSecurity("pavan");
        filter.doFilterHttp(request, response, chain);
        verify(session).setAttribute(RemoveAdminPermissionFilter.SECURITY_CONFIG_LAST_CHANGE, 100L);
    }

    @Test
    public void testShouldNotSetLastSecurityChangeAsASessionAttributeIfItHasNotChanged() throws IOException, ServletException {
        setupAuthentication();
        when(timeProvider.currentTimeMillis()).thenReturn(100L);
        when(session.getAttribute(RemoveAdminPermissionFilter.SECURITY_CONFIG_LAST_CHANGE)).thenReturn(100L);
        RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider);
        filter.doFilterHttp(request, response, chain);

        verify(session).getAttribute(RemoveAdminPermissionFilter.SECURITY_CONFIG_LAST_CHANGE);//Make sure the stub was indeed called.
        verifyNoMoreInteractions(session);//Make sure we did not set the config md5 again
    }

    private void turnOnSecurity(String username) throws IOException {
        configHelper.turnOnSecurity();
        configHelper.addAdmins(username);
        mergedGoConfig.forceReload();
    }

    private void stubSessionToReturn0() {
        when(session.getAttribute(RemoveAdminPermissionFilter.SECURITY_CONFIG_LAST_CHANGE)).thenReturn(0L);
    }

    private Authentication setupAuthentication() {
        GrantedAuthority[] authorities = {};
        Authentication authentication = new TestingAuthenticationToken(new User("loser", "secret", true, true,true, true, authorities), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        authentication.setAuthenticated(true);
        return authentication;
    }
}
