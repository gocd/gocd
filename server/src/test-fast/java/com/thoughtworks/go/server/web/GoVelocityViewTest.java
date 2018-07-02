/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.security.GoAuthority;
import com.thoughtworks.go.server.service.RailsAssetsService;
import com.thoughtworks.go.server.service.VersionInfoService;
import com.thoughtworks.go.server.service.WebpackAssetsService;
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

import static com.thoughtworks.go.server.newsecurity.SessionUtilsHelper.setAuthenticationToken;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ANALYTICS_EXTENSION;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
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
        request = new MockHttpServletRequest();
        velocityContext = new VelocityContext();
    }

    @Test
    public void shouldNotSetSupportsAnalyticsDashboardIfPluginMissing() throws Exception {
        view.exposeHelpers(velocityContext, request);
        assertThat(velocityContext.get(GoVelocityView.SUPPORTS_ANALYTICS_DASHBOARD), is(false));
    }

    @Test
    public void shouldSetSupportsAnalyticsDashboardIfPluginInstalled() throws Exception {
        List<SupportedAnalytics> supportedAnalytics = Collections.singletonList(new SupportedAnalytics("dashboard", "id", "foo"));
        AnalyticsPluginInfo info = new AnalyticsPluginInfo(null, null, new Capabilities(supportedAnalytics), null);

        when(pluginInfoFinder.allPluginInfos(ANALYTICS_EXTENSION)).thenReturn(Collections.singletonList(new CombinedPluginInfo(info)));

        view.exposeHelpers(velocityContext, request);

        assertThat(velocityContext.get(GoVelocityView.SUPPORTS_ANALYTICS_DASHBOARD), is(true));
    }

    @Test
    public void shouldSetAdministratorIfUserIsAdministrator() throws Exception {
        setAuthenticationToken(request, "jez", GoAuthority.ROLE_SUPERVISOR.asAuthority());
        view.exposeHelpers(velocityContext, request);
        assertThat(velocityContext.get(GoVelocityView.ADMINISTRATOR), is(true));
    }

    @Test
    public void shouldSetTemplateAdministratorIfUserIsTemplateAdministrator() throws Exception {
        setAuthenticationToken(request, "jez", GoAuthority.ROLE_TEMPLATE_SUPERVISOR.asAuthority());
        view.exposeHelpers(velocityContext, request);
        assertThat(velocityContext.get(GoVelocityView.TEMPLATE_ADMINISTRATOR), is(true));
    }

    @Test
    public void shouldSetTemplateViewUserRightsForTemplateViewUser() throws Exception {
        setAuthenticationToken(request, "templateView", GoAuthority.ROLE_TEMPLATE_VIEW_USER.asAuthority());
        view.exposeHelpers(velocityContext, request);
        assertThat(velocityContext.get(GoVelocityView.TEMPLATE_VIEW_USER), is(true));
    }

    @Test
    public void shouldSetViewAdministratorRightsIfUserHasAnyLevelOfAdministratorRights() throws Exception {
        setAuthenticationToken(request, "jez", GoAuthority.ROLE_TEMPLATE_SUPERVISOR.asAuthority());
        view.exposeHelpers(velocityContext, request);
        assertThat(velocityContext.get(GoVelocityView.VIEW_ADMINISTRATOR_RIGHTS), is(true));

        setAuthenticationToken(request, "jez", GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
        view.exposeHelpers(velocityContext, request);
        assertThat(velocityContext.get(GoVelocityView.VIEW_ADMINISTRATOR_RIGHTS), is(true));

        setAuthenticationToken(request, "jez", GoAuthority.ROLE_SUPERVISOR.asAuthority());
        view.exposeHelpers(velocityContext, request);
        assertThat(velocityContext.get(GoVelocityView.VIEW_ADMINISTRATOR_RIGHTS), is(true));

        setAuthenticationToken(request, "jez", GoAuthority.ROLE_TEMPLATE_VIEW_USER.asAuthority());
        view.exposeHelpers(velocityContext, request);
        assertThat(velocityContext.get(GoVelocityView.VIEW_ADMINISTRATOR_RIGHTS), is(true));

        setAuthenticationToken(request, "jez", GoAuthority.ROLE_USER.asAuthority());
        view.exposeHelpers(velocityContext, request);
        assertThat(velocityContext.get(GoVelocityView.VIEW_ADMINISTRATOR_RIGHTS), is(nullValue()));
    }

    @Test
    public void shouldSetGroupAdministratorIfUserIsAPipelineGroupAdministrator() throws Exception {
        setAuthenticationToken(request, "jez", GoAuthority.ROLE_GROUP_SUPERVISOR.asAuthority());
        view.exposeHelpers(velocityContext, request);
        assertThat(velocityContext.get(GoVelocityView.ADMINISTRATOR), is(nullValue()));
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
        Request servletRequest = mock(Request.class);
        when(servletRequest.getSession()).thenReturn(mock(HttpSession.class));

        view.exposeHelpers(velocityContext, servletRequest);

        assertThat(velocityContext.get(GoVelocityView.CONCATENATED_JAVASCRIPT_FILE_PATH), is("assets/application-digest.js"));
        assertThat(velocityContext.get(GoVelocityView.CONCATENATED_APPLICATION_CSS_FILE_PATH), is("assets/application-digest.css"));
        assertThat(velocityContext.get(GoVelocityView.CONCATENATED_VM_APPLICATION_CSS_FILE_PATH), is("assets/vm/application-digest.css"));
        assertThat(velocityContext.get(GoVelocityView.CONCATENATED_CSS_APPLICATION_CSS_FILE_PATH), is("assets/css/application-digest.css"));
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
        Request servletRequest = mock(Request.class);
        when(servletRequest.getSession()).thenReturn(mock(HttpSession.class));

        view.exposeHelpers(velocityContext, servletRequest);

        assertThat(velocityContext.get(GoVelocityView.CONCATENATED_JAVASCRIPT_FILE_PATH), is("assets/application.js"));
        assertThat(velocityContext.get(GoVelocityView.CONCATENATED_APPLICATION_CSS_FILE_PATH), is("assets/application.css"));
        assertThat(velocityContext.get(GoVelocityView.CONCATENATED_VM_APPLICATION_CSS_FILE_PATH), is("assets/vm/application.css"));
        assertThat(velocityContext.get(GoVelocityView.CONCATENATED_CSS_APPLICATION_CSS_FILE_PATH), is("assets/css/application.css"));
    }

    @Test
    public void shouldSetFeatureToggleValues() throws Exception {
        Request servletRequest = mock(Request.class);
        when(servletRequest.getSession()).thenReturn(mock(HttpSession.class));

        view.exposeHelpers(velocityContext, servletRequest);

        assertThat(velocityContext.get(Toggles.PIPELINE_COMMENT_FEATURE_TOGGLE_KEY), is(true));
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

    @Test
    public void shouldSetArtifactStoresEnabledWhenToggledOn() throws Exception {
        Request servletRequest = mock(Request.class);
        when(servletRequest.getSession()).thenReturn(mock(HttpSession.class));
        when(featureToggleService.isToggleOn(Toggles.ARTIFACT_EXTENSION_KEY)).thenReturn(true);

        view.exposeHelpers(velocityContext, servletRequest);

        assertThat(velocityContext.get(GoVelocityView.ARTIFACT_STORES_ENABLED), is(true));
    }

    @Test
    public void shouldNotSetArtifactStoresEnabledWhenToggledOff() throws Exception {
        Request servletRequest = mock(Request.class);
        when(servletRequest.getSession()).thenReturn(mock(HttpSession.class));
        when(featureToggleService.isToggleOn(Toggles.ARTIFACT_EXTENSION_KEY)).thenReturn(false);

        view.exposeHelpers(velocityContext, servletRequest);

        assertThat(velocityContext.get(GoVelocityView.ARTIFACT_STORES_ENABLED), is(false));
    }
}
