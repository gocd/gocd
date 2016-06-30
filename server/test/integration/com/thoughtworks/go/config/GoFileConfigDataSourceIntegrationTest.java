/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.parts.XmlPartialConfigProvider;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import static com.thoughtworks.go.helper.ConfigFileFixture.DEFAULT_XML_WITH_2_AGENTS;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class GoFileConfigDataSourceIntegrationTest {

    private final String DEFAULT_CHARSET = "defaultCharset";
    private final SystemEnvironment systemEnvironment = new SystemEnvironment();
    @Autowired
    private GoFileConfigDataSource dataSource;
    @Autowired
    private GoConfigService configService;
    @Autowired
    private GoPartialConfig goPartialConfig;
    @Autowired
    private GoConfigDao goConfigDao;
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private GoConfigFileHelper configHelper;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private GoConfigWatchList configWatchList;
    private ConfigRepoConfig configRepo;
    @Autowired
    private CachedGoPartials cachedGoPartials;
    @Autowired
    private ConfigCache configCache;
    @Autowired
    private ConfigElementImplementationRegistry configElementImplementationRegistry;
    private final String remoteDownstream = "remote_downstream";
    private PartialConfig partialConfig;
    private PipelineConfig upstreamPipeline;

    @Before
    public void setUp() throws Exception {
        File configDir = temporaryFolder.newFolder();
        String absolutePath = new File(configDir, "cruise-config.xml").getAbsolutePath();
        systemEnvironment.setProperty(SystemEnvironment.CONFIG_FILE_PROPERTY, absolutePath);
        configHelper = new GoConfigFileHelper(DEFAULT_XML_WITH_2_AGENTS);
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();
        configHelper.addConfigRepo(new ConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig("url"), XmlPartialConfigProvider.providerName));
        configHelper.addPipeline("upstream", "upstream_stage_original");
        goConfigService.forceNotifyListeners();
        cachedGoPartials.clear();
        configRepo = configWatchList.getCurrentConfigRepos().get(0);
        upstreamPipeline = goConfigService.pipelineConfigNamed(new CaseInsensitiveString("upstream"));
        partialConfig = PartialConfigMother.pipelineWithDependencyMaterial(remoteDownstream, upstreamPipeline, new RepoConfigOrigin(configRepo, "r1"));
        goPartialConfig.onSuccessPartialConfig(configRepo, partialConfig);
    }

    @After
    public void tearDown() throws Exception {
        cachedGoPartials.clear();
        dataSource.reloadIfModified();
        configHelper.onTearDown();
        ReflectionUtil.setStaticField(Charset.class, DEFAULT_CHARSET, null);
        systemEnvironment.clearProperty(SystemEnvironment.CONFIG_FILE_PROPERTY);
    }

    @Test
    public void shouldConvertToUTF8BeforeSavingConfigToFileSystem() throws IOException {
        ReflectionUtil.setStaticField(Charset.class, DEFAULT_CHARSET, Charset.forName("windows-1252"));
        GoFileConfigDataSource.GoConfigSaveResult result = dataSource.writeWithLock(new UpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString());
                JobConfig job = new JobConfig("job");
                ExecTask task = new ExecTask();
                task.setCommand("powershell");
                task.setArgs("Get-ChildItem -Path . â€“Recurse");
                job.addTask(task);
                pipelineConfig.first().getJobs().add(job);
                cruiseConfig.addPipeline(UUID.randomUUID().toString(), pipelineConfig);
                return cruiseConfig;
            }
        }, new GoConfigHolder(goConfigService.currentCruiseConfig(), goConfigService.getConfigForEditing()));
        assertThat(result.getConfigSaveState(), is(ConfigSaveState.UPDATED));
        FileInputStream inputStream = new FileInputStream(dataSource.fileLocation());
        String newMd5 = CachedDigestUtils.md5Hex(inputStream);
        assertThat(newMd5, is(result.getConfigHolder().config.getMd5()));
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldValidateMergedConfigForConfigChangesThroughFileSystem() throws Exception {
        assertThat(goConfigService.getCurrentConfig().getAllPipelineNames().contains(new CaseInsensitiveString(remoteDownstream)), is(true));
        updateConfigOnFileSystem(new UpdateConfig() {
            @Override
            public void update(CruiseConfig cruiseConfig) {
                PipelineConfig updatedUpstream = cruiseConfig.getPipelineConfigByName(upstreamPipeline.name());
                updatedUpstream.getFirstStageConfig().setName(new CaseInsensitiveString("upstream_stage_renamed"));
            }
        });

        thrown.expect(org.hamcrest.Matchers.any(GoConfigInvalidException.class));
        thrown.expectMessage("Stage with name 'upstream_stage_original' does not exist on pipeline 'upstream', it is being referred to from pipeline 'remote_downstream' (url at r1)");
        dataSource.forceLoad(new File(systemEnvironment.getCruiseConfigFile()));
    }

    @Test
    public void shouldFallbackToValidPartialsForConfigChangesThroughFileSystem() throws Exception {
        assertThat(goConfigService.getCurrentConfig().getAllPipelineNames().contains(new CaseInsensitiveString(remoteDownstream)), is(true));

        String remoteInvalidPipeline = "remote_invalid_pipeline";
        PartialConfig invalidPartial = PartialConfigMother.invalidPartial(remoteInvalidPipeline, new RepoConfigOrigin(configRepo, "r2"));
        goPartialConfig.onSuccessPartialConfig(configRepo, invalidPartial);
        assertThat(goConfigService.getCurrentConfig().getAllPipelineNames().contains(new CaseInsensitiveString(remoteInvalidPipeline)), is(false));

        final String newArtifactLocation = "some_random_change_to_config";
        updateConfigOnFileSystem(new UpdateConfig() {
            @Override
            public void update(CruiseConfig cruiseConfig) {
                cruiseConfig.server().setArtifactsDir(newArtifactLocation);
            }
        });

        GoConfigHolder goConfigHolder = dataSource.forceLoad(new File(systemEnvironment.getCruiseConfigFile()));
        assertThat(goConfigHolder.config.server().artifactsDir(), is(newArtifactLocation));
        assertThat(goConfigHolder.config.getAllPipelineNames().contains(new CaseInsensitiveString(remoteDownstream)), is(true));
        assertThat(goConfigHolder.config.getAllPipelineNames().contains(new CaseInsensitiveString(remoteInvalidPipeline)), is(false));
    }

    @Test
    public void shouldSaveWithKnownPartialsWhenValidationPassesForConfigChangesThroughFileSystem() throws Exception {
        assertThat(goConfigService.getCurrentConfig().getAllPipelineNames().contains(new CaseInsensitiveString(remoteDownstream)), is(true));

        //Introducing a change to make the latest version of remote pipeline invalid
        PipelineConfig remoteDownstreamPipeline = partialConfig.getGroups().first().getPipelines().get(0);
        DependencyMaterialConfig dependencyMaterial = remoteDownstreamPipeline.materialConfigs().findDependencyMaterial(upstreamPipeline.name());
        dependencyMaterial.setStageName(new CaseInsensitiveString("upstream_stage_renamed"));
        goPartialConfig.onSuccessPartialConfig(configRepo, partialConfig);
        DependencyMaterialConfig dependencyMaterialForRemotePipelineInConfigCache = goConfigService.getCurrentConfig().getPipelineConfigByName(new CaseInsensitiveString(remoteDownstream)).materialConfigs().findDependencyMaterial(upstreamPipeline.name());
        assertThat(dependencyMaterialForRemotePipelineInConfigCache.getStageName(), is(new CaseInsensitiveString("upstream_stage_original")));

        final CaseInsensitiveString upstreamStageRenamed = new CaseInsensitiveString("upstream_stage_renamed");
        updateConfigOnFileSystem(new UpdateConfig() {
            @Override
            public void update(CruiseConfig cruiseConfig) {
                cruiseConfig.getPipelineConfigByName(upstreamPipeline.name()).first().setName(upstreamStageRenamed);
            }
        });

        GoConfigHolder goConfigHolder = dataSource.forceLoad(new File(systemEnvironment.getCruiseConfigFile()));
        assertThat(goConfigHolder.config.getAllPipelineNames().contains(new CaseInsensitiveString(remoteDownstream)), is(true));
        assertThat(goConfigHolder.config.getPipelineConfigByName(remoteDownstreamPipeline.name()).materialConfigs().findDependencyMaterial(upstreamPipeline.name()).getStageName(), is(upstreamStageRenamed));
        assertThat(goConfigHolder.config.getPipelineConfigByName(upstreamPipeline.name()).getFirstStageConfig().name(), is(upstreamStageRenamed));
    }

    private interface UpdateConfig {
        void update(CruiseConfig cruiseConfig);
    }

    private void updateConfigOnFileSystem(UpdateConfig updateConfig) throws Exception {
        String cruiseConfigFile = systemEnvironment.getCruiseConfigFile();
        CruiseConfig updatedConfig = new Cloner().deepClone(goConfigService.getConfigForEditing());
        updateConfig.update(updatedConfig);
        File configFile = new File(cruiseConfigFile);
        FileOutputStream outputStream = new FileOutputStream(configFile);
        new MagicalGoConfigXmlWriter(configCache, configElementImplementationRegistry).write(updatedConfig, outputStream, true);
    }

}