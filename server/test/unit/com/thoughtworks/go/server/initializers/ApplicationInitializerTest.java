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

package com.thoughtworks.go.server.initializers;

import com.thoughtworks.go.config.CachedGoConfig;
import com.thoughtworks.go.config.GoConfigDataSource;
import com.thoughtworks.go.config.InvalidConfigMessageRemover;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistrar;
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
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.studios.shine.cruise.stage.details.StageResourceImporter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationInitializerTest {
    @Mock private CommandRepositoryInitializer commandRepositoryInitializer;
    @Mock private ConfigElementImplementationRegistrar configElementImplementationRegistrar;
    @Mock private PluginsInitializer pluginsInitializer;
    @Mock private PluginsZipInitializer pluginsZipInitializer;
    @Mock private PipelineSqlMapDao pipelineSqlMapDao;
    @Mock private PipelineTimeline pipelineTimeline;
    @Mock private ConfigRepository configRepository;
    @Mock private InvalidConfigMessageRemover invalidConfigMessageRemover;
    @Mock private OauthTokenSweeper oauthTokenSweeper;
    @Mock private LdapContextFactory ldapContextFactory;
    @Mock private AgentService agentService;
    @Mock private GoConfigService goConfigService;
    @Mock private GoConfigDataSource goConfigDataSource;
    @Mock private GoLicenseService goLicenseService;
    @Mock private EnvironmentConfigService environmentConfigService;
    @Mock private DefaultPluginJarLocationMonitor defaultPluginJarLocationMonitor;
    @Mock private CachedGoConfig cachedGoConfig;
    @Mock private ConsoleActivityMonitor consoleActivityMonitor;
    @Mock private BuildAssignmentService buildAssignmentService;
    @Mock private PipelineScheduler pipelineScheduler;
    @Mock private TimerScheduler timerScheduler;
    @Mock private ArtifactsDirHolder artifactsDirHolder;
    @Mock private MaterialUpdateService materialUpdateService;
    @Mock private RemoveAdminPermissionFilter removeAdminPermissionFilter;
    @Mock private LicenseViolationChecker licenseViolationChecker;
    @Mock private PipelineLockService pipelineLockService;
    @Mock private StageResourceImporter stageResourceImporter;
    @Mock private GoCasServiceProperties goCasServiceProperties;
    @Mock private GoDiskSpaceMonitor goDiskSpaceMonitor;
    @Mock private BackupService backupService;
    @Mock private ArtifactsService artifactsService;
    @Mock private RailsAssetsService railsAssetsService;

    @Mock private ContextRefreshedEvent contextRefreshedEvent;

    @InjectMocks
    ApplicationInitializer initializer = new ApplicationInitializer();

    @Test
    public void shouldCallInitializeOfPluginZipInitializerOnlyAfterInitializeOfPluginInitializer() throws Exception {
        verifyOrder(pluginsInitializer, pluginsZipInitializer);
    }

    private void verifyOrder(Initializer... initializers) {
        ApplicationContext context = mock(ApplicationContext.class);
        when(contextRefreshedEvent.getApplicationContext()).thenReturn(context);
        when(context.getParent()).thenReturn(null);

        initializer.onApplicationEvent(contextRefreshedEvent);

        InOrder inOrder = inOrder(initializers);
        for (Initializer initializer : initializers) {
            inOrder.verify(initializer).initialize();
        }
    }
}
