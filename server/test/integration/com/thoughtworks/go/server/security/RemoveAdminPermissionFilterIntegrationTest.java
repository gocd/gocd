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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginRoleService;
import com.thoughtworks.go.server.service.RoleConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TimeProvider;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.Authentication;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.providers.TestingAuthenticationToken;
import org.springframework.security.ui.FilterChainOrder;
import org.springframework.security.userdetails.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import static com.thoughtworks.go.server.security.RemoveAdminPermissionFilter.SECURITY_CONFIG_LAST_CHANGE;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

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
    @Autowired private CachedGoConfig cachedGoConfig;
    @Autowired private PluginRoleService pluginRoleService;
    @Autowired private RoleConfigService roleService;

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
        RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider, pluginRoleService);
        assertThat(filter.getOrder(), is(FilterChainOrder.BASIC_PROCESSING_FILTER - 1));
    }

    @Test
    public void testShouldContinueWithChainReturnIfAuthenticationIsNull() throws IOException, ServletException {
        RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider, pluginRoleService);
        filter.doFilterHttp(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoMoreInteractions(chain);
    }

    @Test
    public void testShouldContinueWithChainReturnIfCruiseConfigIsNull() throws IOException, ServletException {
        RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider, pluginRoleService);
        filter.doFilterHttp(request, response, chain);
        verify(chain).doFilter(request, response);
        verifyNoMoreInteractions(chain);
    }

    @Test
    public void testShouldContinueWithTheChainIfTheSecurityConfigHasNotChanged() throws IOException, ServletException {
        Authentication authentication = setupAuthentication();

        RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider, pluginRoleService);

        filter.doFilterHttp(request, response, chain);
        modifyArtifactRoot();
        filter.doFilterHttp(request, response, chain);

        assertThat(authentication.isAuthenticated(), is(true));
    }

    private void modifyArtifactRoot() {
        configHelper.currentConfig().server().updateArtifactRoot("something/else");//Config changed but security config did not.
        cachedGoConfig.forceReload();
    }

    @Test
    public void testShouldReAuthenticateIffSecurityConfigChange() throws IOException, ServletException {
        Authentication authentication = setupAuthentication();
        when(session.getAttribute(RemoveAdminPermissionFilter.SECURITY_CONFIG_LAST_CHANGE)).thenReturn(0L).thenReturn(0L).thenReturn(100L);

        RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider, pluginRoleService);
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
    public void testShouldForceReAuthenticationOnRoleConfigChange() throws Exception {
        final ArgumentCaptor<Object> argumentCaptor = ArgumentCaptor.forClass(Object.class);
        final Username username = new Username("bob");
        final RoleConfig admin = new RoleConfig(new CaseInsensitiveString("admin"));
        final Authentication authentication = setupAuthentication();
        final RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider, pluginRoleService);
        filter.initialize();

        filter.doFilterHttp(request, response, chain);
        assertThat(authentication.isAuthenticated(), is(true));

        roleService.create(username, admin, new HttpLocalizedOperationResult());

        verify(session).setAttribute(eq(SECURITY_CONFIG_LAST_CHANGE), argumentCaptor.capture());
        when(session.getAttribute(SECURITY_CONFIG_LAST_CHANGE)).thenReturn(argumentCaptor.getValue());

        filter.doFilterHttp(request, response, chain);

        assertThat(authentication.isAuthenticated(), is(false));
    }

    @Test
    public void testShouldReAuthenticateOnlyOnceAfterConfigChange() throws IOException, ServletException {
        goConfigService.security().securityAuthConfigs().add(new SecurityAuthConfig("github", "cd.go.authorization.github"));
        goConfigService.security().addRole(new PluginRoleConfig("spacetiger", "github"));
        Authentication authentication = setupAuthentication();

        when(session.getAttribute(SECURITY_CONFIG_LAST_CHANGE)).thenReturn(0L).thenReturn(0L).thenReturn(100L);

        RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider, pluginRoleService);
        filter.initialize();

        assertThat(authentication.isAuthenticated(), is(true));//good initial state

        filter.doFilterHttp(request, response, chain);

        pluginRoleService.invalidateRolesFor("cd.go.authorization.github");

        assertThat(authentication.isAuthenticated(), is(true));

        filter.doFilterHttp(request, response, chain);

        assertThat(authentication.isAuthenticated(), is(false));

        authentication.setAuthenticated(true);

        filter.doFilterHttp(request, response, chain);

        assertThat(authentication.isAuthenticated(), is(true));
    }

    @Test
    public void testShouldReAuthenticateOnlyOnceAfterAuthorizationPluginUnloaded() throws IOException, ServletException {
        Authentication authentication = setupAuthentication();

        when(session.getAttribute(SECURITY_CONFIG_LAST_CHANGE)).thenReturn(0L).thenReturn(0L).thenReturn(100L);

        RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider, pluginRoleService);
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

        when(session.getAttribute(SECURITY_CONFIG_LAST_CHANGE)).thenReturn(null);
        RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider, pluginRoleService);
        filter.initialize();
        turnOnSecurity("pavan");
        filter.doFilterHttp(request, response, chain);
        verifyNoMoreInteractions(session);
    }

    @Test
    public void testShouldSetLastSecurityChangeAsASessionAttributeIfItsNotFound() throws IOException, ServletException {
        setupAuthentication();
        when(timeProvider.currentTimeMillis()).thenReturn(100L);

        when(session.getAttribute(SECURITY_CONFIG_LAST_CHANGE)).thenReturn(null);
        RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider, pluginRoleService);
        filter.initialize();
        turnOnSecurity("pavan");
        filter.doFilterHttp(request, response, chain);
        verify(session).setAttribute(SECURITY_CONFIG_LAST_CHANGE, 100L);
    }

    @Test
    public void testShouldNotSetLastSecurityChangeAsASessionAttributeIfItHasNotChanged() throws IOException, ServletException {
        setupAuthentication();
        when(timeProvider.currentTimeMillis()).thenReturn(100L);
        when(session.getAttribute(SECURITY_CONFIG_LAST_CHANGE)).thenReturn(100L);
        RemoveAdminPermissionFilter filter = new RemoveAdminPermissionFilter(goConfigService, timeProvider, pluginRoleService);
        filter.doFilterHttp(request, response, chain);

        verify(session).getAttribute(SECURITY_CONFIG_LAST_CHANGE);//Make sure the stub was indeed called.
        verifyNoMoreInteractions(session);//Make sure we did not set the config md5 again
    }

    private void turnOnSecurity(String username) throws IOException {
        configHelper.enableSecurity();
        configHelper.addAdmins(username);
        cachedGoConfig.forceReload();
    }

    private void stubSessionToReturn0() {
        when(session.getAttribute(SECURITY_CONFIG_LAST_CHANGE)).thenReturn(0L);
    }

    private Authentication setupAuthentication() {
        GrantedAuthority[] authorities = {};
        Authentication authentication = new TestingAuthenticationToken(new User("loser", "secret", true, true, true, true, authorities), null, authorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
        authentication.setAuthenticated(true);
        return authentication;
    }
}
