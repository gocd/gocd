/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.config.ConfigRepository;
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
import com.thoughtworks.go.server.service.support.ResourceMonitoring;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.server.util.ServletHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextException;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class ApplicationInitializer implements ApplicationListener<ContextRefreshedEvent> {
    private final PluginsInitializer pluginsInitializer;
    private final PluginsZip pluginsZip;
    private final PipelineSqlMapDao pipelineSqlMapDao;
    private final PipelineTimeline pipelineTimeline;
    private final ConfigRepository configRepository;
    private final InvalidConfigMessageRemover invalidConfigMessageRemover;
    private final AgentService agentService;
    private final GoConfigService goConfigService;
    private final EnvironmentConfigService environmentConfigService;
    private final DefaultPluginJarLocationMonitor defaultPluginJarLocationMonitor;
    private final CachedGoConfig cachedGoConfig;
    private final ConsoleActivityMonitor consoleActivityMonitor;
    private final BuildAssignmentService buildAssignmentService;
    private final PipelineScheduler pipelineScheduler;
    private final TimerScheduler timerScheduler;
    private final BackupScheduler backupScheduler;
    private final ArtifactsDirHolder artifactsDirHolder;
    private final MaterialUpdateService materialUpdateService;
    private final InvalidateAuthenticationOnSecurityConfigChangeFilter invalidateAuthenticationOnSecurityConfigChangeFilter;
    private final PipelineLockService pipelineLockService;
    private final GoDiskSpaceMonitor goDiskSpaceMonitor;
    private final ArtifactsService artifactsService;
    private final ConsoleService consoleService;
    private final ConfigElementImplementationRegistrar configElementImplementationRegistrar;
    private final RailsAssetsService railsAssetsService;
    private final FeatureToggleService featureToggleService;
    private final CcTrayActivityListener ccTrayActivityListener;
    private final GoDashboardActivityListener dashboardActivityListener;
    private final EntityHashingService entityHashingService;
    private final DependencyMaterialUpdateNotifier dependencyMaterialUpdateNotifier;
    private final SCMMaterialSource scmMaterialSource;
    private final ResourceMonitoring resourceMonitoring;
    private final PipelineLabelCorrector pipelineLabelCorrector;
    private final BackupService backupService;
    private final DataSource dataSource;
    private final RevokeStaleAccessTokenService revokeStaleAccessTokenService;

    @Value("${cruise.daemons.enabled}")
    private boolean daemonsEnabled;

    @Autowired
    public ApplicationInitializer(PipelineTimeline pipelineTimeline, PluginsInitializer pluginsInitializer, PluginsZip pluginsZip, ArtifactsDirHolder artifactsDirHolder, GoDashboardActivityListener dashboardActivityListener, EntityHashingService entityHashingService, CcTrayActivityListener ccTrayActivityListener, PipelineScheduler pipelineScheduler, DataSource dataSource, PipelineSqlMapDao pipelineSqlMapDao, ConfigRepository configRepository, BackupScheduler backupScheduler, TimerScheduler timerScheduler, RevokeStaleAccessTokenService revokeStaleAccessTokenService, InvalidConfigMessageRemover invalidConfigMessageRemover, AgentService agentService, ConfigElementImplementationRegistrar configElementImplementationRegistrar, MaterialUpdateService materialUpdateService, SCMMaterialSource scmMaterialSource, GoDiskSpaceMonitor goDiskSpaceMonitor, ResourceMonitoring resourceMonitoring, ConsoleService consoleService, PipelineLockService pipelineLockService, GoConfigService goConfigService, EnvironmentConfigService environmentConfigService, InvalidateAuthenticationOnSecurityConfigChangeFilter invalidateAuthenticationOnSecurityConfigChangeFilter, FeatureToggleService featureToggleService, DependencyMaterialUpdateNotifier dependencyMaterialUpdateNotifier, PipelineLabelCorrector pipelineLabelCorrector, BackupService backupService, DefaultPluginJarLocationMonitor defaultPluginJarLocationMonitor, RailsAssetsService railsAssetsService, BuildAssignmentService buildAssignmentService, ConsoleActivityMonitor consoleActivityMonitor, CachedGoConfig cachedGoConfig, ArtifactsService artifactsService) {
        this.pipelineTimeline = pipelineTimeline;
        this.pluginsInitializer = pluginsInitializer;
        this.pluginsZip = pluginsZip;
        this.artifactsDirHolder = artifactsDirHolder;
        this.dashboardActivityListener = dashboardActivityListener;
        this.entityHashingService = entityHashingService;
        this.ccTrayActivityListener = ccTrayActivityListener;
        this.pipelineScheduler = pipelineScheduler;
        this.dataSource = dataSource;
        this.pipelineSqlMapDao = pipelineSqlMapDao;
        this.configRepository = configRepository;
        this.backupScheduler = backupScheduler;
        this.timerScheduler = timerScheduler;
        this.revokeStaleAccessTokenService = revokeStaleAccessTokenService;
        this.invalidConfigMessageRemover = invalidConfigMessageRemover;
        this.agentService = agentService;
        this.configElementImplementationRegistrar = configElementImplementationRegistrar;
        this.materialUpdateService = materialUpdateService;
        this.scmMaterialSource = scmMaterialSource;
        this.goDiskSpaceMonitor = goDiskSpaceMonitor;
        this.resourceMonitoring = resourceMonitoring;
        this.consoleService = consoleService;
        this.pipelineLockService = pipelineLockService;
        this.goConfigService = goConfigService;
        this.environmentConfigService = environmentConfigService;
        this.invalidateAuthenticationOnSecurityConfigChangeFilter = invalidateAuthenticationOnSecurityConfigChangeFilter;
        this.featureToggleService = featureToggleService;
        this.dependencyMaterialUpdateNotifier = dependencyMaterialUpdateNotifier;
        this.pipelineLabelCorrector = pipelineLabelCorrector;
        this.backupService = backupService;
        this.defaultPluginJarLocationMonitor = defaultPluginJarLocationMonitor;
        this.railsAssetsService = railsAssetsService;
        this.buildAssignmentService = buildAssignmentService;
        this.consoleActivityMonitor = consoleActivityMonitor;
        this.cachedGoConfig = cachedGoConfig;
        this.artifactsService = artifactsService;
    }

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

            pipelineTimeline.updateTimelineOnInit();
            pipelineSqlMapDao.initialize();
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

            dependencyMaterialUpdateNotifier.initialize();
            scmMaterialSource.initialize();
            backupService.initialize();

            revokeStaleAccessTokenService.initialize();

            if (this.daemonsEnabled) {
                dashboardActivityListener.start();
                ccTrayActivityListener.start();
            }
        } catch (Throwable throwable) {
            // Raise a Spring exception to ensure that existing beans are disposed of cleanly
            throw new ApplicationContextException("Unable to initialize Go Server after initial load", throwable);
        }
    }

    private boolean isRootApplicationContext(ApplicationContext applicationContext) {
        return applicationContext.getParent() == null;
    }
}
