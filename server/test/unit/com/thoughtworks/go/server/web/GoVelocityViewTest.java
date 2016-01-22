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

package com.thoughtworks.go.server.web;

import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.service.RailsAssetsService;
import com.thoughtworks.go.server.service.VersionInfoService;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.eclipse.jetty.server.Request;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.security.context.SecurityContextImpl;
import org.springframework.security.providers.TestingAuthenticationToken;
import org.springframework.security.userdetails.User;
import org.springframework.security.userdetails.ldap.LdapUserDetailsImpl;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.security.context.HttpSessionContextIntegrationFilter.SPRING_SECURITY_CONTEXT_KEY;

public class GoVelocityViewTest {
    private GoVelocityView view;
    private HttpServletRequest request;
    private Context velocityContext;
    private SecurityContextImpl securityContext;
    @Mock private RailsAssetsService railsAssetsService;
    @Mock private FeatureToggleService featureToggleService;
    @Mock private VersionInfoService versionInfoService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(featureToggleService.isToggleOn(anyString())).thenReturn(true);
        Toggles.initializeWith(featureToggleService);
        view = spy(new GoVelocityView());
        doReturn(railsAssetsService).when(view).getRailsAssetsService();
        doReturn(versionInfoService).when(view).getVersionInfoService();
        request = new MockHttpServletRequest();
        velocityContext = new VelocityContext();
        securityContext = new SecurityContextImpl();
    }

    @Test
    public void shouldRetriveLdapCompleteNameFromSessionWhenAuthenticated() throws Exception {
        securityContext.setAuthentication(new TestingAuthenticationToken(new LdapUserDetailsImpl() {
            public String getUsername() {
                return "test1";
            }

            public String getDn() {
                return "cn=Test User, ou=Beijing, ou=Employees, ou=Enterprise, ou=Principal";
            }
        }, null, null));
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        view.exposeHelpers(velocityContext, request);
        assertThat((String) velocityContext.get(GoVelocityView.PRINCIPAL), is("Test User"));
    }

    @Test
    public void shouldSetAdministratorIfUserIsAdministrator() throws Exception {
        securityContext.setAuthentication(
                new TestingAuthenticationToken("jez", "badger",
                        new GrantedAuthority[]{new GrantedAuthorityImpl(GoAuthority.ROLE_SUPERVISOR.toString())}));
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        view.exposeHelpers(velocityContext, request);
        assertThat((Boolean) velocityContext.get(GoVelocityView.ADMINISTRATOR), is(true));
    }

    @Test
    public void shouldSetTemplateAdministratorIfUserIsTemplateAdministrator() throws Exception {
        securityContext.setAuthentication(
                new TestingAuthenticationToken("jez", "badger",
                        new GrantedAuthority[]{new GrantedAuthorityImpl(GoAuthority.ROLE_TEMPLATE_SUPERVISOR.toString())}));
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        view.exposeHelpers(velocityContext, request);
        assertThat((Boolean) velocityContext.get(GoVelocityView.TEMPLATE_ADMINISTRATOR), is(true));
    }

    @Test
    public void shouldSetViewAdministratorRightsIfUserHasAnyLevelOfAdministratorRights() throws Exception {
        securityContext.setAuthentication(new TestingAuthenticationToken("jez", "badger", new GrantedAuthority[]{new GrantedAuthorityImpl(GoAuthority.ROLE_TEMPLATE_SUPERVISOR.toString())}));
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        view.exposeHelpers(velocityContext, request);
        assertThat((Boolean) velocityContext.get(GoVelocityView.VIEW_ADMINISTRATOR_RIGHTS), is(true));

        securityContext.setAuthentication(new TestingAuthenticationToken("jez", "badger", new GrantedAuthority[]{new GrantedAuthorityImpl(GoAuthority.ROLE_GROUP_SUPERVISOR.toString())}));
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        view.exposeHelpers(velocityContext, request);
        assertThat((Boolean) velocityContext.get(GoVelocityView.VIEW_ADMINISTRATOR_RIGHTS), is(true));

        securityContext.setAuthentication(new TestingAuthenticationToken("jez", "badger", new GrantedAuthority[]{new GrantedAuthorityImpl(GoAuthority.ROLE_SUPERVISOR.toString())}));
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        view.exposeHelpers(velocityContext, request);
        assertThat((Boolean) velocityContext.get(GoVelocityView.VIEW_ADMINISTRATOR_RIGHTS), is(true));

        securityContext.setAuthentication(new TestingAuthenticationToken("jez", "badger", new GrantedAuthority[]{new GrantedAuthorityImpl(GoAuthority.ROLE_USER.toString())}));
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        view.exposeHelpers(velocityContext, request);
        assertThat(velocityContext.get(GoVelocityView.VIEW_ADMINISTRATOR_RIGHTS), is(nullValue()));
    }

    @Test
    public void shouldSetGroupAdministratorIfUserIsAPipelineGroupAdministrator() throws Exception {
        securityContext.setAuthentication(
                new TestingAuthenticationToken("jez", "badger",
                        new GrantedAuthority[]{new GrantedAuthorityImpl(GoAuthority.ROLE_GROUP_SUPERVISOR.toString())}));
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        view.exposeHelpers(velocityContext, request);
        assertThat((Boolean) velocityContext.get(GoVelocityView.ADMINISTRATOR), is(nullValue()));
        assertThat((Boolean) velocityContext.get(GoVelocityView.GROUP_ADMINISTRATOR), is(true));
    }

    @Test
    public void shouldNotSetPrincipalIfNoSession() throws Exception {
        view.exposeHelpers(velocityContext, request);
        assertNull("Principal should be null", velocityContext.get(GoVelocityView.PRINCIPAL));
    }

    @Test
    public void shouldNotSetPrincipalIfAuthenticationInformationNotAvailable() throws Exception {
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        view.exposeHelpers(velocityContext, request);
        assertNull("Principal should be null", velocityContext.get(GoVelocityView.PRINCIPAL));
    }

    @Test
    public void principalIsTheUsernameWhenNothingElseAvailable() throws Exception {
        request.getSession().setAttribute(SPRING_SECURITY_CONTEXT_KEY, securityContext);
        securityContext.setAuthentication(
                new TestingAuthenticationToken(
                        new User("Test User", "pwd", true, new GrantedAuthority[]{new GrantedAuthorityImpl("nowt")}),
                        null, null));
        view.exposeHelpers(velocityContext, request);
        assertThat((String) velocityContext.get(GoVelocityView.PRINCIPAL), is("Test User"));
    }

    @Test
    public void shouldSetAssetsPathVariableWhenRailsNewWithCompressedJavascriptsIsUsed() throws Exception {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.useCompressedJs()).thenReturn(true);
        when(railsAssetsService.getAssetPath("application.js")).thenReturn("assets/application-digest.js");
        when(railsAssetsService.getAssetPath("application.css")).thenReturn("assets/application-digest.css");
        when(railsAssetsService.getAssetPath("vm/application.css")).thenReturn("assets/vm/application-digest.css");
        when(railsAssetsService.getAssetPath("css/application.css")).thenReturn("assets/css/application-digest.css");
        when(railsAssetsService.getAssetPath("g9/stage_bar_cancelled_icon.png")).thenReturn("assets/g9/stage_bar_cancelled_icon.png");
        when(railsAssetsService.getAssetPath("spinner.gif")).thenReturn("assets/spinner.gif");
        when(railsAssetsService.getAssetPath("cruise.ico")).thenReturn("assets/cruise.ico");
        GoVelocityView view = spy(new GoVelocityView(systemEnvironment));
        doReturn(railsAssetsService).when(view).getRailsAssetsService();
        doReturn(versionInfoService).when(view).getVersionInfoService();
        Request servletRequest = mock(Request.class);
        when(servletRequest.getSession()).thenReturn(mock(HttpSession.class));

        view.exposeHelpers(velocityContext, servletRequest);

        assertThat((String) velocityContext.get(GoVelocityView.CONCATENATED_JAVASCRIPT_FILE_PATH), is("assets/application-digest.js"));
        assertThat((String) velocityContext.get(GoVelocityView.CONCATENATED_APPLICATION_CSS_FILE_PATH), is("assets/application-digest.css"));
        assertThat((String) velocityContext.get(GoVelocityView.CONCATENATED_VM_APPLICATION_CSS_FILE_PATH), is("assets/vm/application-digest.css"));
        assertThat((String) velocityContext.get(GoVelocityView.CONCATENATED_CSS_APPLICATION_CSS_FILE_PATH), is("assets/css/application-digest.css"));
        assertThat((String) velocityContext.get(GoVelocityView.CONCATENATED_STAGE_BAR_CANCELLED_ICON_FILE_PATH), is("assets/g9/stage_bar_cancelled_icon.png"));
        assertThat((String) velocityContext.get(GoVelocityView.CONCATENATED_SPINNER_ICON_FILE_PATH), is("assets/spinner.gif"));
        assertThat((String) velocityContext.get(GoVelocityView.CONCATENATED_CRUISE_ICON_FILE_PATH), is("assets/cruise.ico"));
        assertThat((RailsAssetsService) velocityContext.get(GoVelocityView.PATH_RESOLVER), is(railsAssetsService));
    }

    @Test
    public void shouldSetAssetsPathVariableWhenRailsNewIsUsedInDevelopmentEnvironment() throws Exception {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.useCompressedJs()).thenReturn(false);
        when(railsAssetsService.getAssetPath("application.js")).thenReturn("assets/application.js");
        when(railsAssetsService.getAssetPath("application.css")).thenReturn("assets/application.css");
        when(railsAssetsService.getAssetPath("vm/application.css")).thenReturn("assets/vm/application.css");
        when(railsAssetsService.getAssetPath("css/application.css")).thenReturn("assets/css/application.css");
        GoVelocityView view = spy(new GoVelocityView(systemEnvironment));
        doReturn(railsAssetsService).when(view).getRailsAssetsService();
        doReturn(versionInfoService).when(view).getVersionInfoService();
        Request servletRequest = mock(Request.class);
        when(servletRequest.getSession()).thenReturn(mock(HttpSession.class));

        view.exposeHelpers(velocityContext, servletRequest);

        assertThat((String) velocityContext.get(GoVelocityView.CONCATENATED_JAVASCRIPT_FILE_PATH), is("assets/application.js"));
        assertThat((String) velocityContext.get(GoVelocityView.CONCATENATED_APPLICATION_CSS_FILE_PATH), is("assets/application.css"));
        assertThat((String) velocityContext.get(GoVelocityView.CONCATENATED_VM_APPLICATION_CSS_FILE_PATH), is("assets/vm/application.css"));
        assertThat((String) velocityContext.get(GoVelocityView.CONCATENATED_CSS_APPLICATION_CSS_FILE_PATH), is("assets/css/application.css"));
    }

    @Test
    public void shouldSetFeatureToggleValues() throws Exception {
        Request servletRequest = mock(Request.class);
        when(servletRequest.getSession()).thenReturn(mock(HttpSession.class));

        view.exposeHelpers(velocityContext, servletRequest);

        assertThat((Boolean) velocityContext.get(Toggles.PIPELINE_COMMENT_FEATURE_TOGGLE_KEY), is(true));
    }

    @Test
    public void shouldSetGoUpdateFeatureValues() throws Exception {
        Request servletRequest = mock(Request.class);

        when(versionInfoService.isGOUpdateCheckEnabled()).thenReturn(true);
        when(servletRequest.getSession()).thenReturn(mock(HttpSession.class));
        when(versionInfoService.getGoUpdate()).thenReturn("16.1.0-123");

        view.exposeHelpers(velocityContext, servletRequest);

        assertTrue((Boolean) velocityContext.get(GoVelocityView.GO_UPDATE_CHECK_ENABLED));
        assertThat((String) velocityContext.get(GoVelocityView.GO_UPDATE), is("16.1.0-123"));
    }
}
