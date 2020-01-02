/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.initializers;

import com.thoughtworks.go.config.CachedGoConfig;
import com.thoughtworks.go.config.ConfigCipherUpdater;
import com.thoughtworks.go.config.GoFileConfigDataSource;
import com.thoughtworks.go.config.InvalidConfigMessageRemover;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistrar;
import com.thoughtworks.go.domain.cctray.CcTrayActivityListener;
import com.thoughtworks.go.plugin.infra.commons.PluginsZip;
import com.thoughtworks.go.plugin.infra.monitor.DefaultPluginJarLocationMonitor;
import com.thoughtworks.go.server.cronjob.GoDiskSpaceMonitor;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.dashboard.GoDashboardActivityListener;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.materials.DependencyMaterialUpdateNotifier;
import com.thoughtworks.go.server.materials.MaterialUpdateService;
import com.thoughtworks.go.server.materials.SCMMaterialSource;
import com.thoughtworks.go.server.newsecurity.filters.InvalidateAuthenticationOnSecurityConfigChangeFilter;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.datasharing.DataSharingSettingsService;
import com.thoughtworks.go.server.service.datasharing.DataSharingUsageStatisticsReportingService;
import com.thoughtworks.go.server.service.support.ResourceMonitoring;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.ReflectionUtil;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

@SuppressWarnings({"unused", "PMD.UnusedPrivateField"})
public class ApplicationInitializerTest {
    @Mock
    private CommandRepositoryInitializer commandRepositoryInitializer;
    @Mock
    private ConfigElementImplementationRegistrar configElementImplementationRegistrar;
    @Mock
    private PluginsInitializer pluginsInitializer;
    @Mock
    private PluginsZip pluginsZip;
    @Mock
    private PipelineSqlMapDao pipelineSqlMapDao;
    @Mock
    private PipelineTimeline pipelineTimeline;
    @Mock
    private ConfigRepository configRepository;
    @Mock
    private InvalidConfigMessageRemover invalidConfigMessageRemover;
    @Mock
    private AgentService agentService;
    @Mock
    private GoConfigService goConfigService;
    @Mock
    private GoFileConfigDataSource goFileConfigDataSource;
    @Mock
    private EnvironmentConfigService environmentConfigService;
    @Mock
    private DefaultPluginJarLocationMonitor defaultPluginJarLocationMonitor;
    @Mock
    private CachedGoConfig cachedGoConfig;
    @Mock
    private ConsoleActivityMonitor consoleActivityMonitor;
    @Mock
    private BuildAssignmentService buildAssignmentService;
    @Mock
    private PipelineScheduler pipelineScheduler;
    @Mock
    private TimerScheduler timerScheduler;
    @Mock
    private BackupScheduler backupScheduler;
    @Mock
    private ArtifactsDirHolder artifactsDirHolder;
    @Mock
    private MaterialUpdateService materialUpdateService;
    @Mock
    private InvalidateAuthenticationOnSecurityConfigChangeFilter invalidateAuthenticationOnSecurityConfigChangeFilter;
    @Mock
    private PipelineLockService pipelineLockService;
    @Mock
    private GoDiskSpaceMonitor goDiskSpaceMonitor;
    @Mock
    private BackupService backupService;
    @Mock
    private ArtifactsService artifactsService;
    @Mock
    private RailsAssetsService railsAssetsService;
    @Mock
    private FeatureToggleService featureToggleService;
    @Mock
    private CcTrayActivityListener ccTrayActivityListener;
    @Mock
    private GoDashboardActivityListener dashboardActivityListener;
    @Mock
    private ConsoleService consoleService;
    @Mock
    private ContextRefreshedEvent contextRefreshedEvent;
    @Mock
    private PipelineConfigService pipelineConfigService;
    @Mock
    private ServerVersionInfoManager serverVersionInfoManager;
    @Mock
    private ConfigCipherUpdater configCipherUpdater;
    @Mock
    private EntityHashingService entityHashingService;
    @Mock
    private DependencyMaterialUpdateNotifier dependencyMaterialUpdateNotifier;
    @Mock
    private SCMMaterialSource scmMaterialSource;
    @Mock
    private ResourceMonitoring resourceMonitoring;
    @Mock
    private PipelineLabelCorrector pipelineLabelCorrector;
    @Mock
    private DataSharingSettingsService dataSharingSettingsService;
    @Mock
    private DataSharingUsageStatisticsReportingService dataSharingUsageStatisticsReportingService;
    @InjectMocks
    ApplicationInitializer initializer = new ApplicationInitializer();

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        ApplicationContext context = mock(ApplicationContext.class);
        when(contextRefreshedEvent.getApplicationContext()).thenReturn(context);
        when(context.getParent()).thenReturn(null);
        initializer.onApplicationEvent(contextRefreshedEvent);
    }

    @Test
    public void shouldCallInitializeOfPluginZipInitializerOnlyAfterInitializeOfPluginInitializer() throws Exception {
        assertThat(ReflectionUtil.getField(new Toggles(), "service"), is(featureToggleService));
        verifyOrder(pluginsInitializer, pluginsZip);
    }

    @Test
    public void shouldInitializeCcTrayAndDashboardActivityListenersAfterGoConfigServiceAndPipelineSqlMapDaoAreInitialized() throws Exception {
        verifyOrder(goConfigService, pipelineSqlMapDao, ccTrayActivityListener, dashboardActivityListener);
    }
    @Test
    public void shouldInitializeBackupService() {
        verify(backupService).initialize();
    }

    @Test
    public void shouldRunConfigCipherUpdaterBeforeInitializationOfOtherConfigRelatedServicesAndDatastores() throws Exception {
        InOrder inOrder = inOrder(configCipherUpdater, configElementImplementationRegistrar, configRepository, goFileConfigDataSource, cachedGoConfig, goConfigService);
        inOrder.verify(configCipherUpdater).migrate();
        inOrder.verify(configElementImplementationRegistrar).initialize();
        inOrder.verify(configRepository).initialize();
        inOrder.verify(cachedGoConfig).upgradeConfig();
        inOrder.verify(cachedGoConfig).loadConfigIfNull();
        inOrder.verify(goConfigService).initialize();
    }

    @Test
    public void shouldInitializeDataSharingConsentSettings() throws Exception {
        verify(dataSharingSettingsService, times(1)).initialize();
    }

    @Test
    public void shouldInitializeServerStatsToBeCollected() throws Exception {
        verify(dataSharingUsageStatisticsReportingService, times(1)).initialize();
    }

    private void verifyOrder(Initializer... initializers) {
        InOrder inOrder = inOrder(initializers);
        for (Initializer initializer : initializers) {
            inOrder.verify(initializer).initialize();
        }
    }

    private void verifyOrder(PluginsInitializer pluginsInitializer, PluginsZip pluginsZip) {
        InOrder inOrder = inOrder(pluginsInitializer, pluginsZip);
        inOrder.verify(pluginsInitializer).initialize();
        inOrder.verify(pluginsZip).create();
    }
}
