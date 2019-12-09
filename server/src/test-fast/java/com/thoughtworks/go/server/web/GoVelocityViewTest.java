/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.web;

import com.thoughtworks.go.ClearSingleton;
import com.thoughtworks.go.plugin.domain.analytics.AnalyticsPluginInfo;
import com.thoughtworks.go.plugin.domain.analytics.Capabilities;
import com.thoughtworks.go.plugin.domain.analytics.SupportedAnalytics;
import com.thoughtworks.go.plugin.domain.common.CombinedPluginInfo;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.eclipse.jetty.server.Request;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Collections;
import java.util.List;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ANALYTICS_EXTENSION;
import static com.thoughtworks.go.server.newsecurity.SessionUtilsHelper.setAuthenticationToken;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class GoVelocityViewTest {
    @Rule
    public final ClearSingleton clearSingleton = new ClearSingleton();

    private GoVelocityView view;
    private HttpServletRequest request;
    private Context velocityContext;
    @Mock
    private RailsAssetsService railsAssetsService;
    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private VersionInfoService versionInfoService;
    @Mock
    private DefaultPluginInfoFinder pluginInfoFinder;
    @Mock
    private WebpackAssetsService webpackAssetsService;
    @Mock
    private SecurityService securityService;
    @Mock
    private MaintenanceModeService maintenanceModeService;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        when(featureToggleService.isToggleOn(anyString())).thenReturn(true);
        Toggles.initializeWith(featureToggleService);
        view = spy(new GoVelocityView());
        doReturn(railsAssetsService).when(view).getRailsAssetsService();
        doReturn(versionInfoService).when(view).getVersionInfoService();
        doReturn(pluginInfoFinder).when(view).getPluginInfoFinder();
        doReturn(webpackAssetsService).when(view).webpackAssetsService();
        doReturn(securityService).when(view).getSecurityService();
        doReturn(maintenanceModeService).when(view).getMaintenanceModeService();
        request = new MockHttpServletRequest();
        velocityContext = new VelocityContext();
    }

    @Test
    public void shouldNotShowAnalyticsDashboardIfPluginMissing() throws Exception {
        view.exposeHelpers(velocityContext, request);
        assertThat(velocityContext.get(GoVelocityView.SHOW_ANALYTICS_DASHBOARD), is(false));
    }

    @Test
    public void shouldShowAnalyticsDashboardForAdminIfPluginInstalled() throws Exception {
        List<SupportedAnalytics> supportedAnalytics = Collections.singletonList(new SupportedAnalytics("dashboard", "id", "foo"));
        AnalyticsPluginInfo info = new AnalyticsPluginInfo(null, null, new Capabilities(supportedAnalytics), null);

        when(securityService.isUserAdmin(any())).thenReturn(true);
        when(pluginInfoFinder.allPluginInfos(ANALYTICS_EXTENSION)).thenReturn(Collections.singletonList(new CombinedPluginInfo(info)));

        view.exposeHelpers(velocityContext, request);

        assertThat(velocityContext.get(GoVelocityView.SHOW_ANALYTICS_DASHBOARD), is(true));
    }

    @Test
    public void shouldNotShowAnalyticsDashboardForNonAdminEvenIfPluginInstalled() throws Exception {
        List<SupportedAnalytics> supportedAnalytics = Collections.singletonList(new SupportedAnalytics("dashboard", "id", "foo"));
        AnalyticsPluginInfo info = new AnalyticsPluginInfo(null, null, new Capabilities(supportedAnalytics), null);

        when(securityService.isUserAdmin(any())).thenReturn(false);
        when(pluginInfoFinder.allPluginInfos(ANALYTICS_EXTENSION)).thenReturn(Collections.singletonList(new CombinedPluginInfo(info)));

        view.exposeHelpers(velocityContext, request);

        assertThat(velocityContext.get(GoVelocityView.SHOW_ANALYTICS_DASHBOARD), is(false));
    }

    @Test
    public void shouldSetAdministratorIfUserIsAdministrator() throws Exception {
        when(securityService.isUserAdmin(any())).thenReturn(true);

        view.exposeHelpers(velocityContext, request);

        assertThat(velocityContext.get(GoVelocityView.ADMINISTRATOR), is(true));
    }

    @Test
    public void shouldSetTemplateAdministratorIfUserIsTemplateAdministrator() throws Exception {
        when(securityService.isAuthorizedToViewAndEditTemplates(any())).thenReturn(true);

        view.exposeHelpers(velocityContext, request);

        assertThat(velocityContext.get(GoVelocityView.TEMPLATE_ADMINISTRATOR), is(true));
    }

    @Test
    public void shouldSetTemplateViewUserRightsForTemplateViewUser() throws Exception {
        when(securityService.isAuthorizedToViewTemplates(any())).thenReturn(true);

        view.exposeHelpers(velocityContext, request);

        assertThat(velocityContext.get(GoVelocityView.TEMPLATE_VIEW_USER), is(true));
    }

    @Test
    public void shouldSetViewAdministratorRightsIfUserHasAnyLevelOfAdministratorRights() throws Exception {
        when(securityService.canViewAdminPage(any())).thenReturn(true);

        view.exposeHelpers(velocityContext, request);

        assertThat(velocityContext.get(GoVelocityView.VIEW_ADMINISTRATOR_RIGHTS), is(true));
    }

    @Test
    public void shouldSetGroupAdministratorIfUserIsAPipelineGroupAdministrator() throws Exception {
        when(securityService.isUserAdmin(any())).thenReturn(false);
        when(securityService.isUserGroupAdmin(any())).thenReturn(true);

        view.exposeHelpers(velocityContext, request);

        assertThat(velocityContext.get(GoVelocityView.ADMINISTRATOR), is(false));
        assertThat(velocityContext.get(GoVelocityView.GROUP_ADMINISTRATOR), is(true));
    }

    @Test
    public void shouldNotSetPrincipalIfNoSession() throws Exception {
        view.exposeHelpers(velocityContext, request);
        assertNull("Principal should be null", velocityContext.get(GoVelocityView.PRINCIPAL));
    }

    @Test
    public void shouldNotSetPrincipalIfAuthenticationInformationNotAvailable() throws Exception {
        view.exposeHelpers(velocityContext, request);
        assertNull("Principal should be null", velocityContext.get(GoVelocityView.PRINCIPAL));
    }

    @Test
    public void principalIsTheUsernameWhenNothingElseAvailable() throws Exception {
        setAuthenticationToken(request, "Test User");
        view.exposeHelpers(velocityContext, request);
        assertThat(velocityContext.get(GoVelocityView.PRINCIPAL), is("Test User"));
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
        doReturn(pluginInfoFinder).when(view).getPluginInfoFinder();
        doReturn(webpackAssetsService).when(view).webpackAssetsService();
        doReturn(maintenanceModeService).when(view).getMaintenanceModeService();
        doReturn(securityService).when(view).getSecurityService();
        Request servletRequest = mock(Request.class);
        when(servletRequest.getSession()).thenReturn(mock(HttpSession.class));

        view.exposeHelpers(velocityContext, servletRequest);

        assertThat(velocityContext.get(GoVelocityView.CONCATENATED_STAGE_BAR_CANCELLED_ICON_FILE_PATH), is("assets/g9/stage_bar_cancelled_icon.png"));
        assertThat(velocityContext.get(GoVelocityView.CONCATENATED_SPINNER_ICON_FILE_PATH), is("assets/spinner.gif"));
        assertThat(velocityContext.get(GoVelocityView.CONCATENATED_CRUISE_ICON_FILE_PATH), is("assets/cruise.ico"));
        assertThat(velocityContext.get(GoVelocityView.PATH_RESOLVER), is(railsAssetsService));
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
        doReturn(pluginInfoFinder).when(view).getPluginInfoFinder();
        doReturn(webpackAssetsService).when(view).webpackAssetsService();
        doReturn(maintenanceModeService).when(view).getMaintenanceModeService();
        doReturn(securityService).when(view).getSecurityService();
        Request servletRequest = mock(Request.class);
        when(servletRequest.getSession()).thenReturn(mock(HttpSession.class));

        view.exposeHelpers(velocityContext, servletRequest);

    }

    @Test
    public void shouldSetGoUpdateFeatureValues() throws Exception {
        Request servletRequest = mock(Request.class);

        when(versionInfoService.isGOUpdateCheckEnabled()).thenReturn(true);
        when(servletRequest.getSession()).thenReturn(mock(HttpSession.class));
        when(versionInfoService.getGoUpdate()).thenReturn("16.1.0-123");

        view.exposeHelpers(velocityContext, servletRequest);

        assertTrue((Boolean) velocityContext.get(GoVelocityView.GO_UPDATE_CHECK_ENABLED));
        assertThat(velocityContext.get(GoVelocityView.GO_UPDATE), is("16.1.0-123"));
    }
}
