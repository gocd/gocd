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

package com.thoughtworks.go.config;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.parts.XmlPartialConfigProvider;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.config.update.CreatePipelineConfigCommand;
import com.thoughtworks.go.config.update.FullConfigUpdateCommand;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;
import com.thoughtworks.go.plugin.domain.artifact.Capabilities;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.security.ResetCipher;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.ArtifactStoreService;
import com.thoughtworks.go.server.service.ExternalArtifactsService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.DefaultLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.ConsoleResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.hamcrest.CoreMatchers;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.thoughtworks.go.util.*;

import static com.thoughtworks.go.helper.ConfigFileFixture.DEFAULT_XML_WITH_2_AGENTS;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml"
})
public class CachedGoConfigIntegrationTest {
    @Autowired
    private GoConfigWatchList configWatchList;
    @Autowired
    private GoRepoConfigDataSource repoConfigDataSource;
    @Autowired
    private CachedGoConfig cachedGoConfig;
    private GoConfigFileHelper configHelper;
    @Autowired
    private ServerHealthService serverHealthService;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private CachedGoPartials cachedGoPartials;
    @Autowired
    private GoPartialConfig goPartialConfig;
    @Autowired
    private GoFileConfigDataSource goFileConfigDataSource;
    @Autowired
    private ConfigRepository configRepository;
    @Autowired
    private ExternalArtifactsService externalArtifactsService;
    @Autowired
    private ArtifactStoreService artifactStoreService;
    @Autowired
    private GoConfigMigration goConfigMigration;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private String latestCommit;
    private ConfigRepoConfig configRepo;
    private File externalConfigRepo;
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Rule
    public final ResetCipher resetCipher = new ResetCipher();

    @Before
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper(DEFAULT_XML_WITH_2_AGENTS);
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();
        externalConfigRepo = temporaryFolder.newFolder();
        latestCommit = setupExternalConfigRepo(externalConfigRepo);
        configHelper.addConfigRepo(new ConfigRepoConfig(new GitMaterialConfig(externalConfigRepo.getAbsolutePath()), XmlPartialConfigProvider.providerName));
        goConfigService.forceNotifyListeners();
        configRepo = configWatchList.getCurrentConfigRepos().get(0);
        cachedGoPartials.clear();
        configHelper.addAgent("hostname1", "uuid1");
    }

    @After
    public void tearDown() throws Exception {
        cachedGoPartials.clear();
        for (PartialConfig partial : cachedGoPartials.lastValidPartials()) {
            assertThat(ErrorCollector.getAllErrors(partial).isEmpty(), is(true));
        }
        for (PartialConfig partial : cachedGoPartials.lastKnownPartials()) {
            assertThat(ErrorCollector.getAllErrors(partial).isEmpty(), is(true));
        }
        configHelper.onTearDown();
    }

    @Test
    public void shouldRecoverFromDeepConfigRepoReferencesBug1901When2Repos() throws Exception {
        // pipeline references are like this: pipe1 -> downstream
        File downstreamExternalConfigRepo = temporaryFolder.newFolder();
         /*here is a pipeline 'downstream' with material dependency on 'pipe1' in other repository*/
        String downstreamLatestCommit = setupExternalConfigRepo(downstreamExternalConfigRepo, "external_git_config_repo_referencing_first");
        configHelper.addConfigRepo(new ConfigRepoConfig(new GitMaterialConfig(downstreamExternalConfigRepo.getAbsolutePath()), "gocd-xml"));
        goConfigService.forceNotifyListeners();//TODO what if this is not called?
        ConfigRepoConfig downstreamConfigRepo = configWatchList.getCurrentConfigRepos().get(1);
        assertThat(configWatchList.getCurrentConfigRepos().size(), is(2));

        // And unluckily downstream gets parsed first
        repoConfigDataSource.onCheckoutComplete(downstreamConfigRepo.getMaterialConfig(), downstreamExternalConfigRepo, downstreamLatestCommit);
        // So parsing fails and proper message is shown:
        List<ServerHealthState> messageForInvalidMerge = serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(downstreamConfigRepo));
        assertThat(messageForInvalidMerge.isEmpty(), is(false));
        assertThat(messageForInvalidMerge.get(0).getDescription(), containsString("tries to fetch artifact from pipeline &quot;pipe1&quot;"));
        // and current config is still old
        assertThat(goConfigService.hasPipelineNamed(new CaseInsensitiveString("downstream")), is(false));
        assertThat(cachedGoPartials.lastKnownPartials().size(), is(1));
        assertThat(cachedGoPartials.lastValidPartials().size(), is(0));
        //here downstream partial is waiting to be merged
        assertThat(cachedGoPartials.lastKnownPartials().get(0).getGroups().get(0).hasPipeline(new CaseInsensitiveString("downstream")), is(true));

        // Finally upstream config repository is parsed
        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), externalConfigRepo, latestCommit);

        // now server should be healthy and contain all pipelines
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(configRepo)).isEmpty(), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(downstreamConfigRepo)).isEmpty(), is(true));
        assertThat(cachedGoConfig.currentConfig().hasPipelineNamed(new CaseInsensitiveString("pipe1")), is(true));
        assertThat(cachedGoConfig.currentConfig().hasPipelineNamed(new CaseInsensitiveString("downstream")), is(true));
    }

    @Test
    public void shouldRecoverFromDeepConfigRepoReferencesBug1901When3Repos() throws Exception {
        // pipeline references are like this: pipe1 -> downstream -> downstream2
        File secondDownstreamExternalConfigRepo = temporaryFolder.newFolder();
         /*here is a pipeline 'downstream2' with material dependency on 'downstream' in other repository*/
        String secondDownstreamLatestCommit = setupExternalConfigRepo(secondDownstreamExternalConfigRepo, "external_git_config_repo_referencing_second");
        configHelper.addConfigRepo(new ConfigRepoConfig(new GitMaterialConfig(secondDownstreamExternalConfigRepo.getAbsolutePath()), "gocd-xml"));
        File firstDownstreamExternalConfigRepo = temporaryFolder.newFolder();
         /*here is a pipeline 'downstream' with material dependency on 'pipe1' in other repository*/
        String firstDownstreamLatestCommit = setupExternalConfigRepo(firstDownstreamExternalConfigRepo, "external_git_config_repo_referencing_first");
        configHelper.addConfigRepo(new ConfigRepoConfig(new GitMaterialConfig(firstDownstreamExternalConfigRepo.getAbsolutePath()), "gocd-xml"));
        goConfigService.forceNotifyListeners();
        ConfigRepoConfig firstDownstreamConfigRepo = configWatchList.getCurrentConfigRepos().get(1);
        ConfigRepoConfig secondDownstreamConfigRepo = configWatchList.getCurrentConfigRepos().get(2);
        assertThat(configWatchList.getCurrentConfigRepos().size(), is(3));

        // And unluckily downstream2 gets parsed first
        repoConfigDataSource.onCheckoutComplete(secondDownstreamConfigRepo.getMaterialConfig(), secondDownstreamExternalConfigRepo, secondDownstreamLatestCommit);

        // So parsing fails and proper message is shown:
        List<ServerHealthState> messageForInvalidMerge = serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(secondDownstreamConfigRepo));

        assertThat(messageForInvalidMerge.isEmpty(), is(false));
        assertThat(messageForInvalidMerge.get(0).getDescription(), containsString("tries to fetch artifact from pipeline &quot;downstream&quot;"));
        // and current config is still old
        assertThat(goConfigService.hasPipelineNamed(new CaseInsensitiveString("downstream2")), is(false));
        assertThat(cachedGoPartials.lastKnownPartials().size(), is(1));
        assertThat(cachedGoPartials.lastValidPartials().size(), is(0));
        //here downstream2 partial is waiting to be merged
        assertThat(cachedGoPartials.lastKnownPartials().get(0).getGroups().get(0).hasPipeline(new CaseInsensitiveString("downstream2")), is(true));

        // Then middle upstream config repository is parsed
        repoConfigDataSource.onCheckoutComplete(firstDownstreamConfigRepo.getMaterialConfig(), firstDownstreamExternalConfigRepo, firstDownstreamLatestCommit);

        // and errors are still shown
        messageForInvalidMerge = serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(firstDownstreamConfigRepo));
        assertThat(messageForInvalidMerge.isEmpty(), is(false));
        assertThat(messageForInvalidMerge.get(0).getDescription(), containsString("Pipeline &quot;pipe1&quot; does not exist. It is used from pipeline &quot;downstream&quot"));
        // and current config is still old
        assertThat(goConfigService.hasPipelineNamed(new CaseInsensitiveString("downstream")), is(false));
        assertThat(goConfigService.hasPipelineNamed(new CaseInsensitiveString("downstream2")), is(false));
        assertThat(cachedGoPartials.lastKnownPartials().size(), is(2));
        assertThat(cachedGoPartials.lastValidPartials().size(), is(0));

        // Finally upstream config repository is parsed
        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), externalConfigRepo, latestCommit);

        // now server should be healthy and contain all pipelines

        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(firstDownstreamConfigRepo)).isEmpty(), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(secondDownstreamConfigRepo)).isEmpty(), is(true));
        assertThat(cachedGoConfig.currentConfig().hasPipelineNamed(new CaseInsensitiveString("pipe1")), is(true));
        assertThat(cachedGoConfig.currentConfig().hasPipelineNamed(new CaseInsensitiveString("downstream")), is(true));
        assertThat(cachedGoConfig.currentConfig().hasPipelineNamed(new CaseInsensitiveString("downstream2")), is(true));
    }

    @Test
    public void shouldFailWhenTryingToAddPipelineDefinedRemotely() throws Exception {
        assertThat(configWatchList.getCurrentConfigRepos().size(), is(1));
        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), externalConfigRepo, latestCommit);
        assertThat(cachedGoConfig.loadMergedForEditing().hasPipelineNamed(new CaseInsensitiveString("pipe1")), is(true));

        PipelineConfig dupPipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("pipe1", "ut",
                "www.spring.com");
        try {
            goConfigDao.addPipeline(dupPipelineConfig, PipelineConfigs.DEFAULT_GROUP);
        } catch (RuntimeException ex) {
            assertThat(ex.getMessage(), containsString("You have defined multiple pipelines named 'pipe1'. Pipeline names must be unique. Source(s):"));
            return;
        }
        fail("Should have thrown");
    }

    @Test
    public void shouldNotifyListenersWhenConfigChanged() {
        ConfigChangeListenerStub listener = new ConfigChangeListenerStub();
        cachedGoConfig.registerListener(listener);
        assertThat(listener.invocationCount, is(1));

        cachedGoConfig.writeWithLock(new UpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                return cruiseConfig;
            }
        });
        assertThat(listener.invocationCount, is(2));
    }

    @Test
    public void shouldReturnMergedConfig_WhenThereIsValidPartialConfig() throws Exception {
        assertThat(configWatchList.getCurrentConfigRepos().size(), is(1));

        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), externalConfigRepo, latestCommit);
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(configRepo)).isEmpty(), is(true));
        assertThat(repoConfigDataSource.latestPartialConfigForMaterial(configRepo.getMaterialConfig()).getGroups().findGroup("first").findBy(new CaseInsensitiveString("pipe1")), is(not(nullValue())));
        assertThat(cachedGoConfig.currentConfig().hasPipelineNamed(new CaseInsensitiveString("pipe1")), is(true));
    }

    @Test
    public void shouldFailWhenTryingToAddPipelineWithTheSameNameAsAnotherPipelineDefinedRemotely_EntitySave() throws Exception {
        assertThat(configWatchList.getCurrentConfigRepos().size(), is(1));
        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), externalConfigRepo, latestCommit);
        assertThat(cachedGoConfig.currentConfig().hasPipelineNamed(new CaseInsensitiveString("pipe1")), is(true));

        PipelineConfig dupPipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("pipe1", "ut",
                "www.spring.com");
        try {
            goConfigDao.updateConfig(new CreatePipelineConfigCommand(goConfigService, dupPipelineConfig, Username.ANONYMOUS, new DefaultLocalizedOperationResult(), "default", externalArtifactsService), Username.ANONYMOUS);
            fail("Should have thrown");
        } catch (RuntimeException ex) {
            PipelineConfig pipe1 = goConfigService.pipelineConfigNamed(new CaseInsensitiveString("pipe1"));
            String errorMessage = dupPipelineConfig.errors().on(PipelineConfig.NAME);
            assertThat(errorMessage, containsString("You have defined multiple pipelines named 'pipe1'. Pipeline names must be unique. Source(s):"));
            Matcher matcher = Pattern.compile("^.*\\[(.*),\\s(.*)\\].*$").matcher(errorMessage);
            assertThat(matcher.matches(), is(true));
            assertThat(matcher.groupCount(), is(2));
            List<String> expectedSources = asList(dupPipelineConfig.getOriginDisplayName(), pipe1.getOriginDisplayName());
            List<String> actualSources = new ArrayList<>();
            for (int i = 1; i <= matcher.groupCount(); i++) {
                actualSources.add(matcher.group(i));
            }
            assertThat(actualSources.size(), is(expectedSources.size()));
            assertThat(actualSources.containsAll(expectedSources), is(true));
        }
    }

    @Test
    public void shouldFailWhenTryingToAddPipelineWithTheSameNameAsAnotherPipelineDefinedRemotely_FullConfigSave() throws Exception {
        assertThat(configWatchList.getCurrentConfigRepos().size(), is(1));
        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), externalConfigRepo, latestCommit);
        assertThat(cachedGoConfig.currentConfig().hasPipelineNamed(new CaseInsensitiveString("pipe1")), is(true));

        final PipelineConfig dupPipelineConfig = PipelineMother.twoBuildPlansWithResourcesAndSvnMaterialsAtUrl("pipe1", "ut",
                "www.spring.com");
        try {
            goConfigDao.updateConfig(new UpdateConfigCommand() {
                @Override
                public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                    cruiseConfig.getGroups().first().add(dupPipelineConfig);
                    return cruiseConfig;
                }
            });
            fail("Should have thrown");
        } catch (RuntimeException ex) {
            String errorMessage = ex.getMessage();
            assertThat(errorMessage, containsString("You have defined multiple pipelines named 'pipe1'. Pipeline names must be unique. Source(s):"));
            Matcher matcher = Pattern.compile("^.*\\[(.*),\\s(.*)\\].*$").matcher(errorMessage);
            assertThat(matcher.matches(), is(true));
            assertThat(matcher.groupCount(), is(2));
            PipelineConfig pipe1 = goConfigService.pipelineConfigNamed(new CaseInsensitiveString("pipe1"));
            List<String> expectedSources = asList(dupPipelineConfig.getOriginDisplayName(), pipe1.getOriginDisplayName());
            List<String> actualSources = new ArrayList<>();
            for (int i = 1; i <= matcher.groupCount(); i++) {
                actualSources.add(matcher.group(i));
            }
            assertThat(actualSources.size(), is(expectedSources.size()));
            assertThat(actualSources.containsAll(expectedSources), is(true));
        }
    }

    @Test
    public void shouldReturnRemotePipelinesAmongAllPipelinesInMergedConfigForEdit() throws Exception {
        assertThat(configWatchList.getCurrentConfigRepos().size(), is(1));

        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), externalConfigRepo, latestCommit);
        assertThat(cachedGoConfig.loadMergedForEditing().hasPipelineNamed(new CaseInsensitiveString("pipe1")), is(true));
    }

    private List<ServerHealthState> findMessageFor(final HealthStateType type) {
        return serverHealthService.logs().stream().filter(new Predicate<ServerHealthState>() {
            @Override
            public boolean test(ServerHealthState element) {
                return element.getType().equals(type);
            }
        }).collect(Collectors.toList());
    }

    @Test
    public void shouldNotifyWithMergedConfig_WhenPartUpdated() throws Exception {
        ConfigChangeListenerStub listener = new ConfigChangeListenerStub();
        cachedGoConfig.registerListener(listener);
        // at registration
        assertThat(listener.invocationCount, is(1));

        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), externalConfigRepo, latestCommit);

        assertThat("currentConfigShouldBeMerged", cachedGoConfig.currentConfig().hasPipelineNamed(new CaseInsensitiveString("pipe1")), is(true));
        assertThat(listener.invocationCount, is(2));
    }

    @Test
    public void shouldNotNotifyListenersWhenMergeFails() throws IOException {
        checkinPartial("config_repo_with_invalid_partial");
        ConfigRepoConfig configRepo = configWatchList.getCurrentConfigRepos().get(0);


        ConfigChangeListenerStub listener = new ConfigChangeListenerStub();
        cachedGoConfig.registerListener(listener);
        // at registration
        assertThat(listener.invocationCount, is(1));
        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), externalConfigRepo, latestCommit);

        assertThat("currentConfigShouldBeMainXmlOnly", cachedGoConfig.currentConfig().hasPipelineNamed(new CaseInsensitiveString("pipeline_with_no_stage")), is(false));
        assertThat(listener.invocationCount, is(1));
    }

    @Test
    public void shouldSetErrorHealthStateWhenMergeFails() throws IOException {
        checkinPartial("config_repo_with_invalid_partial");
        ConfigRepoConfig configRepo = configWatchList.getCurrentConfigRepos().get(0);

        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), externalConfigRepo, latestCommit);

        List<ServerHealthState> messageForInvalidMerge = serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(configRepo));

        assertThat(messageForInvalidMerge.isEmpty(), is(false));
        assertThat(messageForInvalidMerge.get(0).getDescription().contains("Pipeline 'pipeline_with_no_stage' does not have any stages configured"), is(true));
    }

    @Test
    public void shouldUnSetErrorHealthStateWhenMergePasses() throws IOException {
        ConfigRepoConfig configRepo = configWatchList.getCurrentConfigRepos().get(0);
        checkinPartial("config_repo_with_invalid_partial/bad_partial.gocd.xml");
        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), externalConfigRepo, latestCommit);
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(configRepo)).isEmpty(), is(false));

        //fix partial
        deletePartial("bad_partial.gocd.xml");
        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), externalConfigRepo, latestCommit);
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(configRepo)).isEmpty(), is(true));
    }

    @Test
    public void shouldUpdateCachedConfigOnSave() throws Exception {
        assertThat(cachedGoConfig.currentConfig().agents().size(), Matchers.is(1));
        configHelper.addAgent("hostname", "uuid2");
        assertThat(cachedGoConfig.currentConfig().agents().size(), Matchers.is(2));
    }

    @Test
    public void shouldReloadCachedConfigWhenWriting() throws Exception {
        cachedGoConfig.writeWithLock(updateFirstAgentResources("osx"));
        assertThat(cachedGoConfig.currentConfig().agents().get(0).getResourceConfigs().toString(), Matchers.is("osx"));

        cachedGoConfig.writeWithLock(updateFirstAgentResources("osx, firefox"));
        assertThat(cachedGoConfig.currentConfig().agents().get(0).getResourceConfigs().toString(), Matchers.is("firefox | osx"));
    }

    @Test
    public void shouldReloadCachedConfigFromDisk() throws Exception {
        assertThat(cachedGoConfig.currentConfig().agents().size(), Matchers.is(1));
        configHelper.writeXmlToConfigFile(ConfigFileFixture.TASKS_WITH_CONDITION);
        cachedGoConfig.forceReload();
        assertThat(cachedGoConfig.currentConfig().agents().size(), Matchers.is(0));
    }

    @Test
    public void shouldInterpolateParamsInTemplate() throws Exception {
        String content = "<cruise schemaVersion='" + GoConstants.CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' >"
                + "</server>"
                + "<pipelines>\n"
                + "<pipeline name='dev' template='abc'>\n"
                + "    <params>"
                + "        <param name='command'>ls</param>"
                + "        <param name='dir'>/tmp</param>"
                + "    </params>"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "</pipeline>\n"
                + "<pipeline name='acceptance' template='abc'>\n"
                + "    <params>"
                + "        <param name='command'>twist</param>"
                + "        <param name='dir'>./acceptance</param>"
                + "    </params>"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "<templates>\n"
                + "  <pipeline name='abc'>\n"
                + "    <stage name='stage1'>"
                + "      <jobs>"
                + "        <job name='job1'>"
                + "            <tasks>"
                + "                <exec command='/bin/#{command}' args='#{dir}'/>"
                + "            </tasks>"
                + "        </job>"
                + "      </jobs>"
                + "    </stage>"
                + "  </pipeline>\n"
                + "</templates>\n"
                + "</cruise>";

        configHelper.writeXmlToConfigFile(content);

        cachedGoConfig.forceReload();

        CruiseConfig cruiseConfig = cachedGoConfig.currentConfig();
        ExecTask devExec = (ExecTask) cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("dev")).getFirstStageConfig().jobConfigByConfigName(new CaseInsensitiveString("job1")).getTasks().first();
        assertThat(devExec, Is.is(new ExecTask("/bin/ls", "/tmp", (String) null)));

        ExecTask acceptanceExec = (ExecTask) cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("acceptance")).getFirstStageConfig().jobConfigByConfigName(new CaseInsensitiveString("job1")).getTasks().first();
        assertThat(acceptanceExec, Is.is(new ExecTask("/bin/twist", "./acceptance", (String) null)));

        cruiseConfig = cachedGoConfig.loadForEditing();
        devExec = (ExecTask) cruiseConfig.getTemplateByName(new CaseInsensitiveString("abc")).get(0).jobConfigByConfigName(new CaseInsensitiveString("job1")).getTasks().first();
        assertThat(devExec, Is.is(new ExecTask("/bin/#{command}", "#{dir}", (String) null)));

        assertThat(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("dev")).size(), Matchers.is(0));
        assertThat(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("acceptance")).size(), Matchers.is(0));
    }

    @Test
    public void shouldHandleParamQuotingCorrectly() throws Exception {
        String content = "<cruise schemaVersion='" + GoConstants.CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' />"
                + "<pipelines>\n"
                + "<pipeline name='dev'>\n"
                + "    <params>"
                + "        <param name='command'>ls#{a}</param>"
                + "        <param name='dir'>/tmp</param>"
                + "    </params>"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "    <stage name='stage1'>"
                + "      <jobs>"
                + "        <job name='job1'>"
                + "            <tasks>"
                + "                <exec command='/bin/#{command}##{b}' args='#{dir}'/>"
                + "            </tasks>"
                + "        </job>"
                + "      </jobs>"
                + "    </stage>"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "</cruise>";

        configHelper.writeXmlToConfigFile(content);

        cachedGoConfig.forceReload();

        CruiseConfig cruiseConfig = cachedGoConfig.currentConfig();
        ExecTask devExec = (ExecTask) cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("dev")).getFirstStageConfig().jobConfigByConfigName(new CaseInsensitiveString("job1")).getTasks().first();
        assertThat(devExec, Is.is(new ExecTask("/bin/ls#{a}#{b}", "/tmp", (String) null)));
    }

    @Test
    public void shouldAllowParamsInLabelTemplates() throws Exception {
        String content = "<cruise schemaVersion='" + GoConstants.CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' />"
                + "<pipelines>\n"
                + "<pipeline name='dev' labeltemplate='cruise-#{VERSION}-${COUNT}'>\n"
                + "    <params>"
                + "        <param name='VERSION'>1.2</param>"
                + "    </params>"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "    <stage name='stage1'>"
                + "      <jobs>"
                + "        <job name='job1'>"
                + "            <tasks>"
                + "                <exec command='/bin/ls' args='some'/>"
                + "            </tasks>"
                + "        </job>"
                + "      </jobs>"
                + "    </stage>"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "</cruise>";

        configHelper.writeXmlToConfigFile(content);

        cachedGoConfig.forceReload();

        CruiseConfig cruiseConfig = cachedGoConfig.currentConfig();
        assertThat(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("dev")).getLabelTemplate(), Is.is("cruise-1.2-${COUNT}"));
    }

    @Test
    public void shouldThrowErrorWhenEnvironmentVariablesAreDuplicate() throws Exception {
        String content = "<cruise schemaVersion='" + GoConstants.CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' />"
                + "<pipelines>\n"
                + "<pipeline name='dev'>\n"
                + "    <params>"
                + "        <param name='product'>GO</param>"
                + "    </params>"
                + "    <environmentvariables>"
                + "        <variable name='#{product}_WORKING_DIR'><value>go_dir</value></variable>"
                + "        <variable name='GO_WORKING_DIR'><value>dir</value></variable>"
                + "    </environmentvariables>"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "    <stage name='stage1'>"
                + "      <jobs>"
                + "        <job name='job1'>"
                + "            <tasks>"
                + "                <exec command='/bin/ls' args='some'/>"
                + "            </tasks>"
                + "        </job>"
                + "      </jobs>"
                + "    </stage>"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "</cruise>";

        configHelper.writeXmlToConfigFile(content);

        GoConfigValidity configValidity = cachedGoConfig.checkConfigFileValid();
        assertThat(configValidity.isValid(), Matchers.is(false));
        assertThat(configValidity.errorMessage(), containsString("Environment Variable name 'GO_WORKING_DIR' is not unique for pipeline 'dev'"));
    }

    @Test
    public void shouldReturnCachedConfigIfConfigFileIsInvalid() throws Exception {
        CruiseConfig before = cachedGoConfig.currentConfig();
        assertThat(before.agents().size(), Matchers.is(1));

        configHelper.writeXmlToConfigFile("invalid-xml");
        cachedGoConfig.forceReload();

        assertTrue(cachedGoConfig.currentConfig() == before);
        assertThat(cachedGoConfig.checkConfigFileValid().isValid(), Matchers.is(false));
    }

    @Test
    public void shouldClearInvalidExceptionWhenConfigErrorsAreFixed() throws Exception {
        configHelper.writeXmlToConfigFile("invalid-xml");
        cachedGoConfig.forceReload();
        cachedGoConfig.currentConfig();
        assertThat(cachedGoConfig.checkConfigFileValid().isValid(), Matchers.is(false));

        configHelper.addAgent("hostname", "uuid2");//some valid change

        CruiseConfig cruiseConfig = cachedGoConfig.currentConfig();
        assertThat(cruiseConfig.agents().size(), Matchers.is(2));
        assertThat(cachedGoConfig.checkConfigFileValid().isValid(), Matchers.is(true));
    }

    @Test
    public void shouldSetServerHealthMessageWhenConfigFileIsInvalid() throws IOException {
        configHelper.writeXmlToConfigFile("invalid-xml");
        cachedGoConfig.forceReload();

        assertThat(cachedGoConfig.checkConfigFileValid().isValid(), Matchers.is(false));

        List<ServerHealthState> serverHealthStates = serverHealthService.logs();
        assertThat(serverHealthStates.isEmpty(), is(false));
        assertThat(serverHealthStates.contains(ServerHealthState.error(GoConfigService.INVALID_CRUISE_CONFIG_XML, "Error on line 1: Content is not allowed in prolog.", HealthStateType.invalidConfig())), is(true));
    }

    @Test
    public void shouldClearServerHealthMessageWhenConfigFileIsValid() throws IOException {
        serverHealthService.update(ServerHealthState.error(GoConfigService.INVALID_CRUISE_CONFIG_XML, "Error on line 1: Content is not allowed in prolog.", HealthStateType.invalidConfig()));

        Assert.assertThat(findMessageFor(HealthStateType.invalidConfig()).isEmpty(), is(false));

        configHelper.writeXmlToConfigFile(ConfigFileFixture.TASKS_WITH_CONDITION);
        cachedGoConfig.forceReload();

        Assert.assertThat(cachedGoConfig.checkConfigFileValid().isValid(), Matchers.is(true));
        Assert.assertThat(findMessageFor(HealthStateType.invalidConfig()).isEmpty(), is(true));
    }

    @Test
    public void shouldReturnDefaultCruiseConfigIfLoadingTheConfigFailsForTheFirstTime() throws Exception {
        ReflectionUtil.setField(cachedGoConfig, "currentConfig", null);
        configHelper.writeXmlToConfigFile("invalid-xml");
        Assert.assertThat(cachedGoConfig.currentConfig(), Matchers.<CruiseConfig>is(new BasicCruiseConfig()));
    }

    @Test
    public void shouldGetConfigForEditAndRead() throws Exception {
        CruiseConfig cruiseConfig = configHelper.load();
        addPipelineWithParams(cruiseConfig);
        configHelper.writeConfigFile(cruiseConfig);

        PipelineConfig config = cachedGoConfig.currentConfig().pipelineConfigByName(new CaseInsensitiveString("mingle"));
        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig) byFolder(config.materialConfigs(), "folder");
        Assert.assertThat(hgMaterialConfig.getUrl(), Matchers.is("http://hg-server/repo-name"));

        config = cachedGoConfig.loadForEditing().pipelineConfigByName(new CaseInsensitiveString("mingle"));
        hgMaterialConfig = (HgMaterialConfig) byFolder(config.materialConfigs(), "folder");
        Assert.assertThat(hgMaterialConfig.getUrl(), Matchers.is("http://#{foo}/#{bar}"));
    }

    @Test
    public void shouldLoadConfigForReadAndEditWhenNewXMLIsWritten() throws Exception {
        String pipelineName = "mingle";
        cachedGoConfig.save(configXmlWithPipeline(pipelineName), false);

        PipelineConfig reloadedPipelineConfig = cachedGoConfig.currentConfig().pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig) byFolder(reloadedPipelineConfig.materialConfigs(), "folder");
        Assert.assertThat(hgMaterialConfig.getUrl(), Matchers.is("http://hg-server/repo-name"));

        reloadedPipelineConfig = cachedGoConfig.loadForEditing().pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        hgMaterialConfig = (HgMaterialConfig) byFolder(reloadedPipelineConfig.materialConfigs(), "folder");
        Assert.assertThat(hgMaterialConfig.getUrl(), Matchers.is("http://#{foo}/#{bar}"));

        GoConfigHolder configHolder = cachedGoConfig.loadConfigHolder();
        reloadedPipelineConfig = configHolder.config.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        hgMaterialConfig = (HgMaterialConfig) byFolder(reloadedPipelineConfig.materialConfigs(), "folder");
        Assert.assertThat(hgMaterialConfig.getUrl(), Matchers.is("http://hg-server/repo-name"));

        reloadedPipelineConfig = configHolder.configForEdit.pipelineConfigByName(new CaseInsensitiveString(pipelineName));
        hgMaterialConfig = (HgMaterialConfig) byFolder(reloadedPipelineConfig.materialConfigs(), "folder");
        Assert.assertThat(hgMaterialConfig.getUrl(), Matchers.is("http://#{foo}/#{bar}"));
    }

    @Test
    public void shouldLoadConfigForReadAndEditWhenConfigIsUpdatedThoughACommand() throws Exception {
        cachedGoConfig.writeWithLock(new UpdateConfigCommand() {
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                addPipelineWithParams(cruiseConfig);
                return cruiseConfig;
            }
        });
        PipelineConfig reloadedPipelineConfig = cachedGoConfig.currentConfig().pipelineConfigByName(new CaseInsensitiveString("mingle"));
        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig) byFolder(reloadedPipelineConfig.materialConfigs(), "folder");
        Assert.assertThat(hgMaterialConfig.getUrl(), Matchers.is("http://hg-server/repo-name"));

        reloadedPipelineConfig = cachedGoConfig.loadForEditing().pipelineConfigByName(new CaseInsensitiveString("mingle"));
        hgMaterialConfig = (HgMaterialConfig) byFolder(reloadedPipelineConfig.materialConfigs(), "folder");
        Assert.assertThat(hgMaterialConfig.getUrl(), Matchers.is("http://#{foo}/#{bar}"));
    }


    private String configXmlWithPipeline(String pipelineName) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion=\"" + GoConstants.CONFIG_SCHEMA_VERSION + "\">\n" +
                "  <server artifactsdir=\"artifactsDir\" serverId=\"dd8d0f5a-7e8d-4948-a1c7-ddcedbac15d0\" />\n" +
                "  <pipelines group=\"another\">\n" +
                "    <pipeline name=\"" + pipelineName + "\">\n" +
                "      <params>\n" +
                "        <param name=\"foo\">hg-server</param>\n" +
                "        <param name=\"bar\">repo-name</param>\n" +
                "      </params>\n" +
                "      <materials>\n" +
                "        <svn url=\"http://some/svn/url\" dest=\"svnDir\" materialName=\"url\" />\n" +
                "        <hg url=\"http://#{foo}/#{bar}\" dest=\"folder\" />\n" +
                "      </materials>\n" +
                "      <stage name=\"dev\">\n" +
                "        <jobs>\n" +
                "          <job name=\"ant\" />\n" +
                "        </jobs>\n" +
                "      </stage>\n" +
                "    </pipeline>\n" +
                "  </pipelines>\n" +
                "</cruise>\n" +
                "\n";
    }

    @Test
    public void shouldReturnUpdatedStatusWhenConfigIsUpdatedWithLatestCopy() {
        final String md5 = cachedGoConfig.currentConfig().getMd5();
        ConfigSaveState firstSaveState = cachedGoConfig.writeWithLock(new NoOverwriteUpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.addPipeline("g1", PipelineConfigMother.createPipelineConfig("p1", "s1", "j1"));
                return cruiseConfig;
            }

            @Override
            public String unmodifiedMd5() {
                return md5;
            }
        });
        assertThat(firstSaveState, is(ConfigSaveState.UPDATED));
    }

    @Test
    public void shouldReturnMergedStatusWhenConfigIsMergedWithStaleCopy() {
        final String md5 = cachedGoConfig.currentConfig().getMd5();
        ConfigSaveState firstSaveState = cachedGoConfig.writeWithLock(new NoOverwriteUpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.addPipeline("g1", PipelineConfigMother.createPipelineConfig("p1", "s1", "j1"));
                return cruiseConfig;
            }

            @Override
            public String unmodifiedMd5() {
                return md5;
            }
        });
        assertThat(firstSaveState, is(ConfigSaveState.UPDATED));

        ConfigSaveState secondSaveState = cachedGoConfig.writeWithLock(new NoOverwriteUpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.server().setArtifactsDir("something");
                return cruiseConfig;
            }

            @Override
            public String unmodifiedMd5() {
                return md5;
            }
        });
        assertThat(secondSaveState, is(ConfigSaveState.MERGED));
    }

    @Test
    public void shouldNotAllowAGitMergeOfConcurrentChangesIfTheChangeCausesMergedPartialsToBecomeInvalid() {
        final String upstream = UUID.randomUUID().toString();
        String remoteDownstream = "remote-downstream";
        setupExternalConfigRepoWithDependencyMaterialOnPipelineInMainXml(upstream, remoteDownstream);
        final String md5 = cachedGoConfig.currentConfig().getMd5();

        // some random unrelated change to force a git merge workflow
        cachedGoConfig.writeWithLock(new NoOverwriteUpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.server().setCommandRepositoryLocation("new_location");
                return cruiseConfig;
            }

            @Override
            public String unmodifiedMd5() {
                return md5;
            }
        });

        thrown.expectMessage(String.format("Stage with name 'stage' does not exist on pipeline '%s', it is being referred to from pipeline 'remote-downstream' (%s at r1)", upstream, configRepo.getMaterialConfig().getDisplayName()));
        cachedGoConfig.writeWithLock(new NoOverwriteUpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString(upstream)).getFirstStageConfig().setName(new CaseInsensitiveString("new_name"));
                return cruiseConfig;
            }

            @Override
            public String unmodifiedMd5() {
                return md5;
            }
        });
    }

    @Test
    public void shouldMarkAPartialAsValidIfItBecomesValidBecauseOfNewerChangesInMainXml_GitMergeWorkflow() {
        final String upstream = UUID.randomUUID().toString();
        String remoteDownstream = "remote-downstream";
        setupExternalConfigRepoWithDependencyMaterialOnPipelineInMainXml(upstream, remoteDownstream);

        PartialConfig partialWithStageRenamed = new Cloner().deepClone(goPartialConfig.lastPartials().get(0));
        PipelineConfig pipelineInRemoteConfigRepo = partialWithStageRenamed.getGroups().get(0).getPipelines().get(0);
        pipelineInRemoteConfigRepo.materialConfigs().getDependencyMaterial().setStageName(new CaseInsensitiveString("new_name"));
        partialWithStageRenamed.setOrigin(new RepoConfigOrigin(configRepo, "r2"));

        goPartialConfig.onSuccessPartialConfig(configRepo, partialWithStageRenamed);
        final String md5 = cachedGoConfig.currentConfig().getMd5();

        // some random unrelated change to force a git merge workflow
        cachedGoConfig.writeWithLock(new NoOverwriteUpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.server().setCommandRepositoryLocation("new_location");
                return cruiseConfig;
            }

            @Override
            public String unmodifiedMd5() {
                return md5;
            }
        });

        ConfigSaveState saveState = cachedGoConfig.writeWithLock(new NoOverwriteUpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString(upstream)).getFirstStageConfig().setName(new CaseInsensitiveString("new_name"));
                return cruiseConfig;
            }

            @Override
            public String unmodifiedMd5() {
                return md5;
            }
        });
        assertThat(saveState, is(ConfigSaveState.MERGED));
        assertThat(cachedGoPartials.lastValidPartials().get(0).getGroups().first().get(0).materialConfigs().getDependencyMaterial().getStageName(), is(new CaseInsensitiveString("new_name")));
        assertThat(goConfigService.getConfigForEditing().getPipelineConfigByName(new CaseInsensitiveString(upstream)).getFirstStageConfig().name(), is(new CaseInsensitiveString("new_name")));
        assertThat(goConfigService.getCurrentConfig().getPipelineConfigByName(new CaseInsensitiveString(upstream)).getFirstStageConfig().name(), is(new CaseInsensitiveString("new_name")));
    }

    private void setupExternalConfigRepoWithDependencyMaterialOnPipelineInMainXml(String upstream, String remoteDownstreamPipelineName) {
        PipelineConfig upstreamPipelineConfig = GoConfigMother.createPipelineConfigWithMaterialConfig(upstream, new GitMaterialConfig("FOO"));
        goConfigService.addPipeline(upstreamPipelineConfig, "default");
        PartialConfig partialConfig = PartialConfigMother.pipelineWithDependencyMaterial(remoteDownstreamPipelineName, upstreamPipelineConfig, new RepoConfigOrigin(configRepo, "r1"));
        goPartialConfig.onSuccessPartialConfig(configRepo, partialConfig);
    }

    @Test
    public void shouldSaveConfigChangesWhenFullConfigIsBeingSavedFromConfigXmlTabAndAllKnownConfigRepoPartialsAreInvalid() throws Exception {
        cachedGoPartials.clear();
        PartialConfig invalidPartial = PartialConfigMother.invalidPartial("invalid", new RepoConfigOrigin(configRepo, "revision1"));
        goPartialConfig.onSuccessPartialConfig(configRepo, invalidPartial);
        CruiseConfig updatedConfig = new Cloner().deepClone(goConfigService.getConfigForEditing());
        updatedConfig.server().setCommandRepositoryLocation("foo");
        String updatedXml = goFileConfigDataSource.configAsXml(updatedConfig, false);
        FileUtils.writeStringToFile(new File(goConfigDao.fileLocation()), updatedXml, UTF_8);
        GoConfigValidity validity = goConfigService.fileSaver(false).saveXml(updatedXml, goConfigDao.md5OfConfigFile());
        assertThat(validity.isValid(), is(true));
        assertThat(cachedGoPartials.lastValidPartials().isEmpty(), is(true));
        assertThat(cachedGoPartials.lastKnownPartials().contains(invalidPartial), is(true));
    }

    @Test
    public void shouldAllowFallbackMergeAndSaveWhenKnownPartialHasAnInvalidEnvironmentThatRefersToAnUnknownPipeline() throws Exception {
        cachedGoPartials.clear();
        PartialConfig partialConfigWithInvalidEnvironment = PartialConfigMother.withEnvironment("env", new RepoConfigOrigin(configRepo, "revision1"));

        goPartialConfig.onSuccessPartialConfig(configRepo, partialConfigWithInvalidEnvironment);
        ConfigSaveState state = cachedGoConfig.writeWithLock(new UpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.server().setCommandRepositoryLocation("newlocation");
                return cruiseConfig;
            }
        });
        assertThat(state, is(ConfigSaveState.UPDATED));
        assertThat(goConfigService.getCurrentConfig().server().getCommandRepositoryLocation(), is("newlocation"));
    }

    @Test
    public void shouldRemoveCorrespondingRemotePipelinesFromCachedGoConfigIfTheConfigRepoIsDeleted() {
        final ConfigRepoConfig repoConfig1 = new ConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig("url1"), XmlPartialConfigProvider.providerName);
        final ConfigRepoConfig repoConfig2 = new ConfigRepoConfig(MaterialConfigsMother.gitMaterialConfig("url2"), XmlPartialConfigProvider.providerName);
        goConfigService.updateConfig(new UpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.getConfigRepos().add(repoConfig1);
                cruiseConfig.getConfigRepos().add(repoConfig2);
                return cruiseConfig;
            }
        });
        PartialConfig partialConfigInRepo1 = PartialConfigMother.withPipeline("pipeline_in_repo1", new RepoConfigOrigin(repoConfig1, "repo1_r1"));
        PartialConfig partialConfigInRepo2 = PartialConfigMother.withPipeline("pipeline_in_repo2", new RepoConfigOrigin(repoConfig2, "repo2_r1"));
        goPartialConfig.onSuccessPartialConfig(repoConfig1, partialConfigInRepo1);
        goPartialConfig.onSuccessPartialConfig(repoConfig2, partialConfigInRepo2);

        // introduce an invalid change in repo1 so that there is a server health message corresponding to it
        PartialConfig invalidPartialInRepo1Revision2 = PartialConfigMother.invalidPartial("pipeline_in_repo1", new RepoConfigOrigin(repoConfig1, "repo1_r2"));
        goPartialConfig.onSuccessPartialConfig(repoConfig1, invalidPartialInRepo1Revision2);
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).size(), is(1));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0).getMessage(), is("Invalid Merged Configuration"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).get(0).getDescription(), is("1+ errors :: Invalid stage name ''. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.;;  -  Config-Repo: url1 at repo1_r2"));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty(), is(true));

        int countBeforeDeletion = cachedGoConfig.currentConfig().getConfigRepos().size();
        ConfigSaveState configSaveState = cachedGoConfig.writeWithLock(new UpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                cruiseConfig.getConfigRepos().remove(repoConfig1);
                return cruiseConfig;
            }
        });
        assertThat(configSaveState, is(ConfigSaveState.UPDATED));
        assertThat(cachedGoConfig.currentConfig().getConfigRepos().size(), is(countBeforeDeletion - 1));
        assertThat(cachedGoConfig.currentConfig().getConfigRepos().contains(repoConfig2), is(true));
        assertThat(cachedGoConfig.currentConfig().getAllPipelineNames().contains(new CaseInsensitiveString("pipeline_in_repo1")), is(false));
        assertThat(cachedGoConfig.currentConfig().getAllPipelineNames().contains(new CaseInsensitiveString("pipeline_in_repo2")), is(true));
        assertThat(cachedGoPartials.lastKnownPartials().size(), is(1));
        assertThat(((RepoConfigOrigin) cachedGoPartials.lastKnownPartials().get(0).getOrigin()).getMaterial().getFingerprint().equals(repoConfig2.getMaterialConfig().getFingerprint()), is(true));
        assertThat(cachedGoPartials.lastKnownPartials().stream().filter(new Predicate<PartialConfig>() {
            @Override
            public boolean test(PartialConfig item) {
                return ((RepoConfigOrigin) item.getOrigin()).getMaterial().getFingerprint().equals(repoConfig1.getMaterialConfig().getFingerprint());
            }
        }).findFirst().orElse(null), is(nullValue()));
        assertThat(cachedGoPartials.lastValidPartials().size(), is(1));
        assertThat(((RepoConfigOrigin) cachedGoPartials.lastValidPartials().get(0).getOrigin()).getMaterial().getFingerprint().equals(repoConfig2.getMaterialConfig().getFingerprint()), is(true));
        assertThat(cachedGoPartials.lastValidPartials().stream().filter(new Predicate<PartialConfig>() {
            @Override
            public boolean test(PartialConfig item) {
                return ((RepoConfigOrigin) item.getOrigin()).getMaterial().getFingerprint().equals(repoConfig1.getMaterialConfig().getFingerprint());
            }
        }).findFirst().orElse(null), is(nullValue()));

        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig1)).isEmpty(), is(true));
        assertThat(serverHealthService.filterByScope(HealthStateScope.forPartialConfigRepo(repoConfig2)).isEmpty(), is(true));
    }

    @Test
    public void shouldUpdateConfigWhenPartialsAreNotConfigured() throws GitAPIException, IOException {
        String gitShaBeforeSave = configRepository.getCurrentRevCommit().getName();
        BasicCruiseConfig config = GoConfigMother.configWithPipelines("pipeline1");

        ConfigSaveState state = cachedGoConfig.writeFullConfigWithLock(new FullConfigUpdateCommand(config, goConfigService.configFileMd5()));

        String gitShaAfterSave = configRepository.getCurrentRevCommit().getName();
        String configXmlFromConfigFolder = FileUtils.readFileToString(new File(goConfigDao.fileLocation()), UTF_8);

        assertThat(state, is(ConfigSaveState.UPDATED));
        assertThat(cachedGoConfig.loadForEditing(), is(config));
        assertNotEquals(gitShaBeforeSave, gitShaAfterSave);
        assertThat(cachedGoConfig.loadForEditing().getMd5(), is(configRepository.getCurrentRevision().getMd5()));
        assertThat(cachedGoConfig.currentConfig().getMd5(), is(configRepository.getCurrentRevision().getMd5()));
        assertThat(configXmlFromConfigFolder, is(configRepository.getCurrentRevision().getContent()));
    }

    @Test
    public void writeFullConfigWithLockShouldUpdateReloadStrategyToEnsureReloadIsSkippedInAbsenceOfConfigFileChanges() throws GitAPIException, IOException {
        BasicCruiseConfig config = GoConfigMother.configWithPipelines("pipeline1");

        ConfigSaveState state = cachedGoConfig.writeFullConfigWithLock(new FullConfigUpdateCommand(config, goConfigService.configFileMd5()));

        String gitShaAfterSave = configRepository.getCurrentRevCommit().getName();
        assertThat(state, is(ConfigSaveState.UPDATED));

        cachedGoConfig.forceReload();
        String gitShaAfterReload = configRepository.getCurrentRevCommit().getName();
        assertThat(gitShaAfterReload, is(gitShaAfterSave));
    }

    @Test
    public void shouldUpdateConfigWhenPartialsAreConfigured() throws GitAPIException, IOException {
        String gitShaBeforeSave = configRepository.getCurrentRevCommit().getName();
        PartialConfig validPartial = PartialConfigMother.withPipeline("remote_pipeline", new RepoConfigOrigin(configRepo, "revision1"));
        goPartialConfig.onSuccessPartialConfig(configRepo, validPartial);

        assertThat(cachedGoPartials.lastValidPartials().contains(validPartial), is(true));
        assertThat(cachedGoPartials.lastKnownPartials().contains(validPartial), is(true));

        CruiseConfig config = new Cloner().deepClone(cachedGoConfig.loadForEditing());

        config.addEnvironment(UUID.randomUUID().toString());

        ConfigSaveState state = cachedGoConfig.writeFullConfigWithLock(new FullConfigUpdateCommand(config, goConfigService.configFileMd5()));

        String gitShaAfterSave = configRepository.getCurrentRevCommit().getName();
        String configXmlFromConfigFolder = FileUtils.readFileToString(new File(goConfigDao.fileLocation()), UTF_8);

        assertThat(state, is(ConfigSaveState.UPDATED));
        assertThat(cachedGoConfig.loadForEditing(), is(config));
        assertNotEquals(gitShaBeforeSave, gitShaAfterSave);
        assertThat(cachedGoConfig.loadForEditing().getMd5(), is(configRepository.getCurrentRevision().getMd5()));
        assertThat(cachedGoConfig.currentConfig().getMd5(), is(configRepository.getCurrentRevision().getMd5()));
        assertThat(configXmlFromConfigFolder, is(configRepository.getCurrentRevision().getContent()));
        assertThat(cachedGoPartials.lastValidPartials().contains(validPartial), is(true));
        assertThat(cachedGoPartials.lastKnownPartials().contains(validPartial), is(true));
    }

    @Test
    public void shouldUpdateConfigWithNoValidPartialsAndInvalidKnownPartials() throws GitAPIException, IOException {
        String gitShaBeforeSave = configRepository.getCurrentRevCommit().getName();
        PartialConfig invalidPartial = PartialConfigMother.invalidPartial("invalid", new RepoConfigOrigin(configRepo, "revision1"));
        goPartialConfig.onSuccessPartialConfig(configRepo, invalidPartial);

        assertTrue(cachedGoPartials.lastValidPartials().isEmpty());
        assertTrue(cachedGoPartials.lastKnownPartials().contains(invalidPartial));

        CruiseConfig config = new Cloner().deepClone(cachedGoConfig.loadForEditing());

        config.addEnvironment(UUID.randomUUID().toString());

        ConfigSaveState state = cachedGoConfig.writeFullConfigWithLock(new FullConfigUpdateCommand(config, goConfigService.configFileMd5()));

        String gitShaAfterSave = configRepository.getCurrentRevCommit().getName();
        String configXmlFromConfigFolder = FileUtils.readFileToString(new File(goConfigDao.fileLocation()), UTF_8);

        assertThat(state, is(ConfigSaveState.UPDATED));
        assertThat(cachedGoConfig.loadForEditing(), is(config));
        assertNotEquals(gitShaBeforeSave, gitShaAfterSave);
        assertThat(cachedGoConfig.loadForEditing().getMd5(), is(configRepository.getCurrentRevision().getMd5()));
        assertThat(cachedGoConfig.currentConfig().getMd5(), is(configRepository.getCurrentRevision().getMd5()));
        assertThat(configXmlFromConfigFolder, is(configRepository.getCurrentRevision().getContent()));
        assertTrue(cachedGoPartials.lastValidPartials().isEmpty());
        assertTrue(cachedGoPartials.lastKnownPartials().contains(invalidPartial));
    }

    @Test
    public void shouldUpdateConfigWithValidPartialsAndInvalidKnownPartials() throws GitAPIException, IOException {
        String gitShaBeforeSave = configRepository.getCurrentRevCommit().getName();
        PartialConfig validPartial = PartialConfigMother.withPipeline("remote_pipeline", new RepoConfigOrigin(configRepo, "revision1"));
        PartialConfig invalidPartial = PartialConfigMother.invalidPartial("invalid", new RepoConfigOrigin(configRepo, "revision2"));
        goPartialConfig.onSuccessPartialConfig(configRepo, validPartial);
        goPartialConfig.onSuccessPartialConfig(configRepo, invalidPartial);

        assertTrue(cachedGoPartials.lastValidPartials().contains(validPartial));
        assertTrue(cachedGoPartials.lastKnownPartials().contains(invalidPartial));

        CruiseConfig config = new Cloner().deepClone(cachedGoConfig.loadForEditing());

        config.addEnvironment(UUID.randomUUID().toString());

        ConfigSaveState state = cachedGoConfig.writeFullConfigWithLock(new FullConfigUpdateCommand(config, goConfigService.configFileMd5()));

        String gitShaAfterSave = configRepository.getCurrentRevCommit().getName();
        String configXmlFromConfigFolder = FileUtils.readFileToString(new File(goConfigDao.fileLocation()), UTF_8);

        assertThat(state, is(ConfigSaveState.UPDATED));
        assertThat(cachedGoConfig.loadForEditing(), is(config));
        assertNotEquals(gitShaBeforeSave, gitShaAfterSave);
        assertThat(cachedGoConfig.loadForEditing().getMd5(), is(configRepository.getCurrentRevision().getMd5()));
        assertThat(cachedGoConfig.currentConfig().getMd5(), is(configRepository.getCurrentRevision().getMd5()));
        assertThat(configXmlFromConfigFolder, is(configRepository.getCurrentRevision().getContent()));
        assertTrue(cachedGoPartials.lastValidPartials().contains(validPartial));
        assertTrue(cachedGoPartials.lastKnownPartials().contains(invalidPartial));
    }

    @Test
    public void shouldErrorOutOnUpdateConfigWithValidPartials_WithMainConfigBreakingPartials() throws GitAPIException, IOException {
        setupExternalConfigRepoWithDependencyMaterialOnPipelineInMainXml("upstream", "downstream");
        String gitShaBeforeSave = configRepository.getCurrentRevCommit().getName();
        CruiseConfig originalConfig = cachedGoConfig.loadForEditing();
        CruiseConfig editedConfig = new Cloner().deepClone(originalConfig);

        editedConfig.getGroups().remove(editedConfig.findGroup("default"));

        try {
            cachedGoConfig.writeFullConfigWithLock(new FullConfigUpdateCommand(editedConfig, goConfigService.configFileMd5()));
            fail("Expected the test to fail");
        } catch (Exception e) {
            String gitShaAfterSave = configRepository.getCurrentRevCommit().getName();
            String configXmlFromConfigFolder = FileUtils.readFileToString(new File(goConfigDao.fileLocation()), UTF_8);
            assertThat(cachedGoConfig.loadForEditing(), is(originalConfig));
            assertEquals(gitShaBeforeSave, gitShaAfterSave);
            assertThat(cachedGoConfig.loadForEditing().getMd5(), is(configRepository.getCurrentRevision().getMd5()));
            assertThat(cachedGoConfig.currentConfig().getMd5(), is(configRepository.getCurrentRevision().getMd5()));
            assertThat(configXmlFromConfigFolder, is(configRepository.getCurrentRevision().getContent()));
            RepoConfigOrigin origin = (RepoConfigOrigin) cachedGoPartials.lastValidPartials().get(0).getOrigin();
            assertThat(origin.getRevision(), is("r1"));
        }
    }

    @Test
    public void shouldMarkAPreviousInvalidPartialAsValid_IfMainXMLSatisfiesTheDependency() throws GitAPIException, IOException {
        String gitShaBeforeSave = configRepository.getCurrentRevCommit().getName();
        PipelineConfig upstream = PipelineConfigMother.createPipelineConfig("upstream", "S", "J");
        PartialConfig partialConfig = PartialConfigMother.pipelineWithDependencyMaterial("downstream", upstream, new RepoConfigOrigin(configRepo, "r2"));
        goPartialConfig.onSuccessPartialConfig(configRepo, partialConfig);

        assertTrue(cachedGoPartials.lastKnownPartials().contains(partialConfig));
        assertTrue(cachedGoPartials.lastValidPartials().isEmpty());

        CruiseConfig originalConfig = cachedGoConfig.loadForEditing();
        CruiseConfig editedConfig = new Cloner().deepClone(originalConfig);

        editedConfig.addPipeline("default", upstream);
        ConfigSaveState state = cachedGoConfig.writeFullConfigWithLock(new FullConfigUpdateCommand(editedConfig, goConfigService.configFileMd5()));

        String gitShaAfterSave = configRepository.getCurrentRevCommit().getName();
        String configXmlFromConfigFolder = FileUtils.readFileToString(new File(goConfigDao.fileLocation()), UTF_8);

        assertThat(state, is(ConfigSaveState.UPDATED));
        assertThat(cachedGoConfig.loadForEditing(), is(editedConfig));
        assertNotEquals(gitShaBeforeSave, gitShaAfterSave);
        assertThat(cachedGoConfig.loadForEditing().getMd5(), is(configRepository.getCurrentRevision().getMd5()));
        assertThat(cachedGoConfig.currentConfig().getMd5(), is(configRepository.getCurrentRevision().getMd5()));
        assertThat(configXmlFromConfigFolder, is(configRepository.getCurrentRevision().getContent()));
        RepoConfigOrigin origin = (RepoConfigOrigin) cachedGoPartials.lastValidPartials().get(0).getOrigin();
        assertThat(origin.getRevision(), is("r2"));
        assertTrue(cachedGoPartials.lastKnownPartials().contains(partialConfig));
        assertTrue(cachedGoPartials.lastValidPartials().contains(partialConfig));
    }

    @Test
    public void shouldEncryptPluggablePublishArtifactPropertiesDuringSave() throws Exception {
        resetCipher.setupAESCipherFile();
        resetCipher.setupDESCipherFile();
        setupMetadataForPlugin();
        ArtifactStore artifactStore = new ArtifactStore("dockerhub", "cd.go.artifact.docker.registry");
        artifactStoreService.create(Username.ANONYMOUS, artifactStore, new HttpLocalizedOperationResult());
        File configFile = new File(new SystemEnvironment().getCruiseConfigFile());
        String config = goConfigMigration.upgradeIfNecessary(IOUtils.toString(getClass().getResourceAsStream("/data/pluggable_artifacts_with_params.xml"), UTF_8));
        FileUtils.writeStringToFile(configFile, config, UTF_8);

        cachedGoConfig.forceReload();

        Configuration ancestorPluggablePublishAftifactConfigAfterEncryption = goConfigDao.loadConfigHolder()
                .configForEdit.pipelineConfigByName(new CaseInsensitiveString("ancestor"))
                .getExternalArtifactConfigs().get(0).getConfiguration();
        assertThat(ancestorPluggablePublishAftifactConfigAfterEncryption.getProperty("Image").getValue(), is("SECRET"));
        assertThat(ancestorPluggablePublishAftifactConfigAfterEncryption.getProperty("Image").getEncryptedValue(), is(new GoCipher().encrypt("SECRET")));
        assertThat(ancestorPluggablePublishAftifactConfigAfterEncryption.getProperty("Image").getConfigValue(), is(CoreMatchers.nullValue()));
    }

    @Test
    public void shouldEncryptPluggableFetchArtifactPropertiesDuringSave() throws IOException, CryptoException {
        resetCipher.setupAESCipherFile();
        resetCipher.setupDESCipherFile();
        setupMetadataForPlugin();
        ArtifactStore artifactStore = new ArtifactStore("dockerhub", "cd.go.artifact.docker.registry");
        artifactStoreService.create(Username.ANONYMOUS, artifactStore, new HttpLocalizedOperationResult());
        File configFile = new File(new SystemEnvironment().getCruiseConfigFile());
        String config = goConfigMigration.upgradeIfNecessary(IOUtils.toString(getClass().getResourceAsStream("/data/pluggable_artifacts_with_params.xml"), UTF_8));
        FileUtils.writeStringToFile(configFile, config, UTF_8);

        cachedGoConfig.forceReload();

        PipelineConfig child = goConfigDao.loadConfigHolder().configForEdit.pipelineConfigByName(new CaseInsensitiveString("child"));
        Configuration childFetchConfigAfterEncryption = ((FetchPluggableArtifactTask) child
                .get(0).getJobs().get(0).tasks().get(0)).getConfiguration();

        assertThat(childFetchConfigAfterEncryption.getProperty("FetchProperty").getValue(), is("SECRET"));
        assertThat(childFetchConfigAfterEncryption.getProperty("FetchProperty").getEncryptedValue(), is(new GoCipher().encrypt("SECRET")));
        assertThat(childFetchConfigAfterEncryption.getProperty("FetchProperty").getConfigValue(), is(CoreMatchers.nullValue()));
    }

    private void setupMetadataForPlugin() {
        PluginDescriptor pluginDescriptor = new GoPluginDescriptor("cd.go.artifact.docker.registry", "1.0", null, null, null, false);
        PluginConfiguration buildFile = new PluginConfiguration("BuildFile", new Metadata(false, false));
        PluginConfiguration image = new PluginConfiguration("Image", new Metadata(false, true));
        PluginConfiguration tag = new PluginConfiguration("Tag", new Metadata(false, false));
        PluginConfiguration fetchProperty = new PluginConfiguration("FetchProperty", new Metadata(false, true));
        PluginConfiguration fetchTag = new PluginConfiguration("Tag", new Metadata(false, false));
        PluginConfiguration registryUrl = new PluginConfiguration("RegistryURL", new Metadata(true, false));
        PluginConfiguration username = new PluginConfiguration("Username", new Metadata(false, false));
        PluginConfiguration password = new PluginConfiguration("Password", new Metadata(false, true));
        PluggableInstanceSettings storeConfigSettings = new PluggableInstanceSettings(asList(registryUrl, username, password));
        PluggableInstanceSettings publishArtifactSettings = new PluggableInstanceSettings(asList(buildFile, image, tag));
        PluggableInstanceSettings fetchArtifactSettings = new PluggableInstanceSettings(asList(fetchProperty, fetchTag));
        ArtifactPluginInfo artifactPluginInfo = new ArtifactPluginInfo(pluginDescriptor, storeConfigSettings, publishArtifactSettings, fetchArtifactSettings, null, new Capabilities());
        ArtifactMetadataStore.instance().setPluginInfo(artifactPluginInfo);
    }

    private void addPipelineWithParams(CruiseConfig cruiseConfig) {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("mingle", "dev", "ant");
        pipelineConfig.addParam(new ParamConfig("foo", "hg-server"));
        pipelineConfig.addParam(new ParamConfig("bar", "repo-name"));
        pipelineConfig.addMaterialConfig(MaterialConfigsMother.hgMaterialConfig("http://#{foo}/#{bar}", "folder"));
        cruiseConfig.addPipeline("another", pipelineConfig);
    }

    public MaterialConfig byFolder(MaterialConfigs materialConfigs, String folder) {
        for (MaterialConfig materialConfig : materialConfigs) {
            if (materialConfig instanceof ScmMaterialConfig && Objects.equals(folder, materialConfig.getFolder())) {
                return materialConfig;
            }
        }
        return null;
    }

    private UpdateConfigCommand updateFirstAgentResources(final String resources) {
        return new UpdateConfigCommand() {
            public CruiseConfig update(CruiseConfig cruiseConfig) {
                AgentConfig agentConfig = cruiseConfig.agents().get(0);
                agentConfig.setResourceConfigs(new ResourceConfigs(resources));
                return cruiseConfig;
            }
        };
    }

    private void deletePartial(String partial) {
        FileUtils.deleteQuietly(new File(externalConfigRepo, partial));
        gitAddDotAndCommit(externalConfigRepo);
    }

    private void checkinPartial(String partial) throws IOException {
        File externalConfigRepo = this.externalConfigRepo;
        checkInPartial(partial, externalConfigRepo);
    }

    private void checkInPartial(String partial, File externalConfigRepo) throws IOException {
        ClassPathResource resource = new ClassPathResource(partial);
        if (resource.getFile().isDirectory()) {
            FileUtils.copyDirectory(resource.getFile(), externalConfigRepo);
        } else {
            FileUtils.copyFileToDirectory(resource.getFile(), externalConfigRepo);
        }
        gitAddDotAndCommit(externalConfigRepo);
    }


    private class ConfigChangeListenerStub implements ConfigChangedListener {
        private int invocationCount = 0;

        @Override
        public void onConfigChange(CruiseConfig newCruiseConfig) {
            invocationCount++;
        }
    }

    private String setupExternalConfigRepo(File configRepo) throws IOException {
        String configRepoTestResource = "external_git_config_repo";
        return setupExternalConfigRepo(configRepo, configRepoTestResource);
    }

    private String setupExternalConfigRepo(File configRepo, String configRepoTestResource) throws IOException {
        ClassPathResource resource = new ClassPathResource(configRepoTestResource);
        FileUtils.copyDirectory(resource.getFile(), configRepo);
        CommandLine.createCommandLine("git").withEncoding("utf-8").withArg("init").withArg(configRepo.getAbsolutePath()).runOrBomb("");
        CommandLine.createCommandLine("git").withEncoding("utf-8").withArgs("config", "commit.gpgSign", "false").withWorkingDir(configRepo.getAbsoluteFile()).runOrBomb("");
        gitAddDotAndCommit(configRepo);
        ConsoleResult consoleResult = CommandLine.createCommandLine("git").withEncoding("utf-8").withArg("log").withArg("-1").withArg("--pretty=format:%h").withWorkingDir(configRepo).runOrBomb("");

        return consoleResult.outputAsString();
    }

    private void gitAddDotAndCommit(File configRepo) {
        CommandLine.createCommandLine("git").withEncoding("utf-8").withArg("add").withArg("-A").withArg(".").withWorkingDir(configRepo).runOrBomb("");
        CommandLine.createCommandLine("git").withEncoding("utf-8").withArg("config").withArg("user.email").withArg("go_test@go_test.me").withWorkingDir(configRepo).runOrBomb("");
        CommandLine.createCommandLine("git").withEncoding("utf-8").withArg("config").withArg("user.name").withArg("user").withWorkingDir(configRepo).runOrBomb("");
        CommandLine.createCommandLine("git").withEncoding("utf-8").withArg("commit").withArg("-m").withArg("initial commit").withWorkingDir(configRepo).runOrBomb("");
    }
}
