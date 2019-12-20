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
package com.thoughtworks.go.server.initializers;

import com.thoughtworks.go.config.CachedGoConfig;
import com.thoughtworks.go.config.ConfigCipherUpdater;
import com.thoughtworks.go.config.InvalidConfigMessageRemover;
import com.thoughtworks.go.config.migration.AgentXmlToDBMigration;
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
import com.thoughtworks.go.server.util.ServletHelper;
import com.thoughtworks.go.service.ConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class ApplicationInitializer implements ApplicationListener<ContextRefreshedEvent> {
    @Autowired private CommandRepositoryInitializer commandRepositoryInitializer;
    @Autowired private PluginsInitializer pluginsInitializer;
    @Autowired private PluginsZip pluginsZip;
    @Autowired private PipelineSqlMapDao pipelineSqlMapDao;
    @Autowired private PipelineTimeline pipelineTimeline;
    @Autowired private ConfigRepository configRepository;
    @Autowired private InvalidConfigMessageRemover invalidConfigMessageRemover;
    @Autowired private AgentService agentService;
    @Autowired private GoConfigService goConfigService;
    @Autowired private EnvironmentConfigService environmentConfigService;
    @Autowired private DefaultPluginJarLocationMonitor defaultPluginJarLocationMonitor;
    @Autowired private CachedGoConfig cachedGoConfig;
    @Autowired private ConsoleActivityMonitor consoleActivityMonitor;
    @Autowired private BuildAssignmentService buildAssignmentService;
    @Autowired private PipelineScheduler pipelineScheduler;
    @Autowired private TimerScheduler timerScheduler;
    @Autowired private BackupScheduler backupScheduler;
    @Autowired private ArtifactsDirHolder artifactsDirHolder;
    @Autowired private MaterialUpdateService materialUpdateService;
    @Autowired private InvalidateAuthenticationOnSecurityConfigChangeFilter invalidateAuthenticationOnSecurityConfigChangeFilter;
    @Autowired private PipelineLockService pipelineLockService;
    @Autowired private GoDiskSpaceMonitor goDiskSpaceMonitor;
    @Autowired private ArtifactsService artifactsService;
    @Autowired private ConsoleService consoleService;
    @Autowired private ConfigElementImplementationRegistrar configElementImplementationRegistrar;
    @Autowired private ConfigCipherUpdater configCipherUpdater;
    @Autowired private RailsAssetsService railsAssetsService;
    @Autowired private FeatureToggleService featureToggleService;
    @Autowired private CcTrayActivityListener ccTrayActivityListener;
    @Autowired private GoDashboardActivityListener dashboardActivityListener;
    @Autowired private ServerVersionInfoManager serverVersionInfoManager;
    @Autowired private EntityHashingService entityHashingService;
    @Autowired private DependencyMaterialUpdateNotifier dependencyMaterialUpdateNotifier;
    @Autowired private SCMMaterialSource scmMaterialSource;
    @Autowired private ResourceMonitoring resourceMonitoring;
    @Autowired private PipelineLabelCorrector pipelineLabelCorrector;
    @Autowired private DataSharingSettingsService dataSharingSettingsService;
    @Autowired private DataSharingUsageStatisticsReportingService dataSharingUsageStatisticsReportingService;
    @Autowired private BackupService backupService;
    @Autowired private DataSource dataSource;

    @Value("${cruise.daemons.enabled}")
    private boolean daemonsEnabled;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (!isRootApplicationContext(contextRefreshedEvent.getApplicationContext())) {
            return;
        }
        try {
            resourceMonitoring.enableIfDiagnosticsModeIsEnabled();
            //plugin
            defaultPluginJarLocationMonitor.initialize();
            pluginsInitializer.initialize();
            pluginsZip.create();
            //config

            configCipherUpdater.migrate(); // Should be done before configs get loaded
            configElementImplementationRegistrar.initialize();
            configRepository.initialize();

            AgentXmlToDBMigration.dataSource = dataSource;

            cachedGoConfig.upgradeConfig();
            cachedGoConfig.loadConfigIfNull();
            goConfigService.initialize();
            entityHashingService.initialize();

            //artifacts
            artifactsDirHolder.initialize();
            artifactsService.initialize();

            //console logs
            consoleService.initialize();

            //change listener
            environmentConfigService.initialize();
            invalidConfigMessageRemover.initialize();
            agentService.initialize();
            pipelineLockService.initialize();
            buildAssignmentService.initialize();
            materialUpdateService.initialize();
            pipelineLabelCorrector.correctPipelineLabelCountEntries();
            pipelineScheduler.initialize();
            invalidateAuthenticationOnSecurityConfigChangeFilter.initialize();

            pipelineSqlMapDao.initialize();
            commandRepositoryInitializer.initialize();
            consoleActivityMonitor.populateActivityMap();
            timerScheduler.initialize();
            backupScheduler.initialize();
            goDiskSpaceMonitor.initialize();
            railsAssetsService.initialize();
            ccTrayActivityListener.initialize();
            dashboardActivityListener.initialize();

            ServletHelper.init();
            // initialize static accessors
            Toggles.initializeWith(featureToggleService);
            serverVersionInfoManager.initialize();

            dependencyMaterialUpdateNotifier.initialize();
            scmMaterialSource.initialize();
            dataSharingSettingsService.initialize();
            dataSharingUsageStatisticsReportingService.initialize();
            backupService.initialize();
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }

        if (this.daemonsEnabled) {
            startDaemons();
        }
    }

    private void startDaemons() {
        try {
            dashboardActivityListener.startDaemon();
            ccTrayActivityListener.startDaemon();
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private boolean isRootApplicationContext(ApplicationContext applicationContext) {
        return applicationContext.getParent() == null;
    }
}
