/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.materials.MaterialUpdateService;
import com.thoughtworks.go.server.persistence.OauthTokenSweeper;
import com.thoughtworks.go.server.security.GoCasServiceProperties;
import com.thoughtworks.go.server.security.LdapContextFactory;
import com.thoughtworks.go.server.security.RemoveAdminPermissionFilter;
import com.thoughtworks.go.server.service.*;
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService;
import com.thoughtworks.go.server.service.support.toggle.Toggles;
import com.thoughtworks.go.server.util.ServletHelper;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.studios.shine.cruise.stage.details.StageResourceImporter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Component;

@Component
public class ApplicationInitializer implements ApplicationListener<ContextRefreshedEvent> {
    @Autowired private CommandRepositoryInitializer commandRepositoryInitializer;
    @Autowired private PluginsInitializer pluginsInitializer;
    @Autowired private PluginsZip pluginsZip;
    @Autowired private PipelineSqlMapDao pipelineSqlMapDao;
    @Autowired private PipelineTimeline pipelineTimeline;
    @Autowired private ConfigRepository configRepository;
    @Autowired private InvalidConfigMessageRemover invalidConfigMessageRemover;
    @Autowired private OauthTokenSweeper oauthTokenSweeper;
    @Autowired private LdapContextFactory ldapContextFactory;
    @Autowired private AgentService agentService;
    @Autowired private GoConfigService goConfigService;
    @Autowired private GoFileConfigDataSource goFileConfigDataSource;
    @Autowired private EnvironmentConfigService environmentConfigService;
    @Autowired private DefaultPluginJarLocationMonitor defaultPluginJarLocationMonitor;
    @Autowired private CachedGoConfig cachedGoConfig;
    @Autowired private ConsoleActivityMonitor consoleActivityMonitor;
    @Autowired private BuildAssignmentService buildAssignmentService;
    @Autowired private PipelineScheduler pipelineScheduler;
    @Autowired private TimerScheduler timerScheduler;
    @Autowired private ArtifactsDirHolder artifactsDirHolder;
    @Autowired private MaterialUpdateService materialUpdateService;
    @Autowired private RemoveAdminPermissionFilter removeAdminPermissionFilter;
    @Autowired private PipelineLockService pipelineLockService;
    @Autowired private StageResourceImporter stageResourceImporter;
    @Autowired private GoCasServiceProperties goCasServiceProperties;
    @Autowired private GoDiskSpaceMonitor goDiskSpaceMonitor;
    @Autowired private BackupService backupService;
    @Autowired private ArtifactsService artifactsService;
    @Autowired private ConsoleService consoleService;
    @Autowired private ConfigElementImplementationRegistrar configElementImplementationRegistrar;
    @Autowired private ConfigCipherUpdater configCipherUpdater;
    @Autowired private RailsAssetsService railsAssetsService;
    @Autowired private FeatureToggleService featureToggleService;
    @Autowired private CcTrayActivityListener ccTrayActivityListener;
    @Autowired private ServerVersionInfoManager serverVersionInfoManager;
    @Autowired private EntityHashingService entityHashingService;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent contextRefreshedEvent) {
        if (!isRootApplicationContext(contextRefreshedEvent.getApplicationContext())) {
            return;
        }
        try {
            //plugin
            defaultPluginJarLocationMonitor.initialize();
            pluginsInitializer.initialize();
            pluginsZip.create();
            //config

            configCipherUpdater.migrate(); // Should be done before configs get loaded
            configElementImplementationRegistrar.initialize();
            configRepository.initialize();
            goFileConfigDataSource.upgradeIfNecessary();
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
            oauthTokenSweeper.initialize();
            invalidConfigMessageRemover.initialize();
            ldapContextFactory.initialize();
            agentService.initialize();
            pipelineLockService.initialize();
            buildAssignmentService.initialize();
            materialUpdateService.initialize();
            pipelineScheduler.initialize();
            removeAdminPermissionFilter.initialize();

            pipelineTimeline.updateTimelineOnInit();
            pipelineSqlMapDao.initialize();
            commandRepositoryInitializer.initialize();
            consoleActivityMonitor.populateActivityMap();
            timerScheduler.initialize();
            stageResourceImporter.initialize();
            goCasServiceProperties.initialize();
            goDiskSpaceMonitor.initialize();
            backupService.initialize();
            railsAssetsService.initialize();
            ccTrayActivityListener.initialize();

            ServletHelper.init();
            // initialize static accessors
            Toggles.initializeWith(featureToggleService);
            serverVersionInfoManager.initialize();
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private boolean isRootApplicationContext(ApplicationContext applicationContext) {
        return applicationContext.getParent() == null;
    }
}
