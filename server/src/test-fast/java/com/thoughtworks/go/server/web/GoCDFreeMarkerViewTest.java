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
import org.eclipse.jetty.server.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ANALYTICS_EXTENSION;
import static com.thoughtworks.go.server.newsecurity.SessionUtilsHelper.setAuthenticationToken;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ExtendWith(ClearSingleton.class)
class GoCDFreeMarkerViewTest {
    private GoCDFreeMarkerView view;
    private HttpServletRequest request;
    private Map<String, Object> context;
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

    @BeforeEach
    public void setUp() throws Exception {
        lenient().when(featureToggleService.isToggleOn(anyString())).thenReturn(true);
        Toggles.initializeWith(featureToggleService);
        view = spy(new GoCDFreeMarkerView());
        lenient().doReturn(railsAssetsService).when(view).getRailsAssetsService();
        lenient().doReturn(versionInfoService).when(view).getVersionInfoService();
        lenient().doReturn(pluginInfoFinder).when(view).getPluginInfoFinder();
        lenient().doReturn(webpackAssetsService).when(view).webpackAssetsService();
        lenient().doReturn(securityService).when(view).getSecurityService();
        lenient().doReturn(maintenanceModeService).when(view).getMaintenanceModeService();
        request = new MockHttpServletRequest();
        context = new HashMap<>();
    }

    @Test
    public void shouldNotShowAnalyticsDashboardIfPluginMissing() throws Exception {
        view.exposeHelpers(context, request);
        assertThat(context.get(GoCDFreeMarkerView.SHOW_ANALYTICS_DASHBOARD), is(false));
    }

    @Test
    public void shouldShowAnalyticsDashboardForAdminIfPluginInstalled() throws Exception {
        List<SupportedAnalytics> supportedAnalytics = List.of(new SupportedAnalytics("dashboard", "id", "foo"));
        AnalyticsPluginInfo info = new AnalyticsPluginInfo(null, null, new Capabilities(supportedAnalytics), null);

        when(securityService.isUserAdmin(any())).thenReturn(true);
        when(pluginInfoFinder.allPluginInfos(ANALYTICS_EXTENSION)).thenReturn(List.of(new CombinedPluginInfo(info)));

        view.exposeHelpers(context, request);

        assertThat(context.get(GoCDFreeMarkerView.SHOW_ANALYTICS_DASHBOARD), is(true));
    }

    @Test
    public void shouldNotShowAnalyticsDashboardForNonAdminEvenIfPluginInstalled() throws Exception {
        List<SupportedAnalytics> supportedAnalytics = List.of(new SupportedAnalytics("dashboard", "id", "foo"));
        AnalyticsPluginInfo info = new AnalyticsPluginInfo(null, null, new Capabilities(supportedAnalytics), null);

        when(securityService.isUserAdmin(any())).thenReturn(false);
        lenient().when(pluginInfoFinder.allPluginInfos(ANALYTICS_EXTENSION)).thenReturn(List.of(new CombinedPluginInfo(info)));

        view.exposeHelpers(context, request);

        assertThat(context.get(GoCDFreeMarkerView.SHOW_ANALYTICS_DASHBOARD), is(false));
    }

    @Test
    public void shouldSetAdministratorIfUserIsAdministrator() throws Exception {
        when(securityService.isUserAdmin(any())).thenReturn(true);

        view.exposeHelpers(context, request);

        assertThat(context.get(GoCDFreeMarkerView.ADMINISTRATOR), is(true));
    }

    @Test
    public void shouldSetTemplateAdministratorIfUserIsTemplateAdministrator() throws Exception {
        when(securityService.isAuthorizedToViewAndEditTemplates(any())).thenReturn(true);

        view.exposeHelpers(context, request);

        assertThat(context.get(GoCDFreeMarkerView.TEMPLATE_ADMINISTRATOR), is(true));
    }

    @Test
    public void shouldSetTemplateViewUserRightsForTemplateViewUser() throws Exception {
        when(securityService.isAuthorizedToViewTemplates(any())).thenReturn(true);

        view.exposeHelpers(context, request);

        assertThat(context.get(GoCDFreeMarkerView.TEMPLATE_VIEW_USER), is(true));
    }

    @Test
    public void shouldSetViewAdministratorRightsIfUserHasAnyLevelOfAdministratorRights() throws Exception {
        when(securityService.canViewAdminPage(any())).thenReturn(true);

        view.exposeHelpers(context, request);

        assertThat(context.get(GoCDFreeMarkerView.VIEW_ADMINISTRATOR_RIGHTS), is(true));
    }

    @Test
    public void shouldSetGroupAdministratorIfUserIsAPipelineGroupAdministrator() throws Exception {
        when(securityService.isUserAdmin(any())).thenReturn(false);
        when(securityService.isUserGroupAdmin(any())).thenReturn(true);

        view.exposeHelpers(context, request);

        assertThat(context.get(GoCDFreeMarkerView.ADMINISTRATOR), is(false));
        assertThat(context.get(GoCDFreeMarkerView.GROUP_ADMINISTRATOR), is(true));
    }

    @Test
    public void shouldNotSetPrincipalIfNoSession() throws Exception {
        view.exposeHelpers(context, request);
        assertNull(context.get(GoCDFreeMarkerView.PRINCIPAL), "Principal should be null");
    }

    @Test
    public void shouldNotSetPrincipalIfAuthenticationInformationNotAvailable() throws Exception {
        view.exposeHelpers(context, request);
        assertNull(context.get(GoCDFreeMarkerView.PRINCIPAL), "Principal should be null");
    }

    @Test
    public void principalIsTheUsernameWhenNothingElseAvailable() throws Exception {
        setAuthenticationToken(request, "Test User");
        view.exposeHelpers(context, request);
        assertThat(context.get(GoCDFreeMarkerView.PRINCIPAL), is("Test User"));
    }

    @Test
    public void shouldSetAssetsPathVariables() throws Exception {
        SystemEnvironment systemEnvironment = mock(SystemEnvironment.class);
        when(systemEnvironment.useCompressedJs()).thenReturn(true);
        when(railsAssetsService.getAssetPath("g9/stage_bar_cancelled_icon.png")).thenReturn("assets/g9/stage_bar_cancelled_icon.png");
        when(railsAssetsService.getAssetPath("cruise.ico")).thenReturn("assets/cruise.ico");
        GoCDFreeMarkerView view = spy(new GoCDFreeMarkerView(systemEnvironment));
        doReturn(railsAssetsService).when(view).getRailsAssetsService();
        doReturn(versionInfoService).when(view).getVersionInfoService();
        doReturn(webpackAssetsService).when(view).webpackAssetsService();
        doReturn(maintenanceModeService).when(view).getMaintenanceModeService();
        doReturn(securityService).when(view).getSecurityService();
        Request servletRequest = mock(Request.class);
        when(servletRequest.getSession()).thenReturn(mock(HttpSession.class));

        view.exposeHelpers(context, servletRequest);

        assertThat(context.get(GoCDFreeMarkerView.CONCATENATED_STAGE_BAR_CANCELLED_ICON_FILE_PATH), is("assets/g9/stage_bar_cancelled_icon.png"));
        assertThat(context.get(GoCDFreeMarkerView.CONCATENATED_CRUISE_ICON_FILE_PATH), is("assets/cruise.ico"));
        assertThat(context.get(GoCDFreeMarkerView.PATH_RESOLVER), is(railsAssetsService));
    }

    @Test
    public void shouldSetGoUpdateFeatureValues() throws Exception {
        Request servletRequest = mock(Request.class);

        when(versionInfoService.isGOUpdateCheckEnabled()).thenReturn(true);
        when(servletRequest.getSession()).thenReturn(mock(HttpSession.class));
        when(versionInfoService.getGoUpdate()).thenReturn("16.1.0-123");

        view.exposeHelpers(context, servletRequest);

        assertTrue((Boolean) context.get(GoCDFreeMarkerView.GO_UPDATE_CHECK_ENABLED));
        assertThat(context.get(GoCDFreeMarkerView.GO_UPDATE), is("16.1.0-123"));
    }

}
