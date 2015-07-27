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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.*;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.ConsoleResult;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.*;
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

import static com.thoughtworks.go.helper.ConfigFileFixture.DEFAULT_XML_WITH_2_AGENTS;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
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
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private String latestCommit;
    private ConfigRepoConfig configRepo;
    private File externalConfigRepo;

    @Before
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper(DEFAULT_XML_WITH_2_AGENTS);
        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();
        externalConfigRepo = temporaryFolder.newFolder();
        latestCommit = setupExternalConfigRepo(externalConfigRepo);
        configHelper.addConfigRepo(new ConfigRepoConfig(new GitMaterialConfig(externalConfigRepo.getAbsolutePath()), "plugin"));
        goConfigService.forceNotifyListeners();
        configRepo = configWatchList.getCurrentConfigRepos().get(0);
        cachedGoPartials.clear();
        configHelper.addAgent("hostname1", "uuid1");
    }

    @After
    public void tearDown() throws Exception {
        cachedGoPartials.clear();
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
        assertThat(findMessageFor(HealthStateType.invalidConfigMerge()).isEmpty(), is(true));
        assertThat(findMessageFor(HealthStateType.invalidConfig()).isEmpty(), is(true));
        assertThat(repoConfigDataSource.latestPartialConfigForMaterial(configRepo.getMaterialConfig()).getGroups().findGroup("first").findBy(new CaseInsensitiveString("pipe1")), is(not(nullValue())));
        assertThat(cachedGoConfig.currentConfig().hasPipelineNamed(new CaseInsensitiveString("pipe1")), is(true));
    }

    @Test
    public void shouldReturnRemotePipelinesAmongAllPipelinesInConfigForEdit() throws Exception
    {
        assertThat(configWatchList.getCurrentConfigRepos().size(), is(1));

        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), externalConfigRepo, latestCommit);
        assertThat(cachedGoConfig.loadForEditing().hasPipelineNamed(new CaseInsensitiveString("pipe1")), is(true));
    }

    private ArrayList<ServerHealthState> findMessageFor(final HealthStateType type) {
        return ListUtil.filterInto(new ArrayList<ServerHealthState>(), serverHealthService.getAllLogs(), new Filter<ServerHealthState>() {
            @Override
            public boolean matches(ServerHealthState element) {
                boolean b = element.getType().equals(type);
                return b;
            }
        });
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

        assertThat(serverHealthService.filterByScope(HealthStateScope.GLOBAL).isEmpty(), is(false));
        ArrayList<ServerHealthState> messageForInvalidMerge = findMessageFor(HealthStateType.invalidConfigMerge());
        assertThat(messageForInvalidMerge.isEmpty(), is(false));
        assertThat(messageForInvalidMerge.get(0).getDescription().contains("Pipeline 'pipeline_with_no_stage' does not have any stages configured"), is(true));
        assertThat(findMessageFor(HealthStateType.invalidConfig()).size(), is(0));
    }

    @Test
    public void shouldUnSetErrorHealthStateWhenMergePasses() throws IOException {
        ConfigRepoConfig configRepo = configWatchList.getCurrentConfigRepos().get(0);
        checkinPartial("config_repo_with_invalid_partial/bad_partial.gocd.xml");
        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), externalConfigRepo, latestCommit);
        assertThat(findMessageFor(HealthStateType.invalidConfigMerge()).isEmpty(), is(false));

        //fix partial
        deletePartial("bad_partial.gocd.xml");
        repoConfigDataSource.onCheckoutComplete(configRepo.getMaterialConfig(), externalConfigRepo, latestCommit);
        assertThat(findMessageFor(HealthStateType.invalidConfigMerge()).isEmpty(), is(true));
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
        assertThat(cachedGoConfig.currentConfig().agents().get(0).getResources().toString(), Matchers.is("osx"));

        cachedGoConfig.writeWithLock(updateFirstAgentResources("osx, firefox"));
        assertThat(cachedGoConfig.currentConfig().agents().get(0).getResources().toString(), Matchers.is("firefox | osx"));
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

        List<ServerHealthState> serverHealthStates = serverHealthService.getAllLogs();
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

    private void addPipelineWithParams(CruiseConfig cruiseConfig) {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("mingle", "dev", "ant");
        pipelineConfig.addParam(new ParamConfig("foo", "hg-server"));
        pipelineConfig.addParam(new ParamConfig("bar", "repo-name"));
        pipelineConfig.addMaterialConfig(MaterialConfigsMother.hgMaterialConfig("http://#{foo}/#{bar}", "folder"));
        cruiseConfig.addPipeline("another", pipelineConfig);
    }

    public MaterialConfig byFolder(MaterialConfigs materialConfigs, String folder) {
        for (MaterialConfig materialConfig : materialConfigs) {
            if (materialConfig instanceof ScmMaterialConfig && ObjectUtil.nullSafeEquals(folder, materialConfig.getFolder())) {
                return materialConfig;
            }
        }
        return null;
    }

    private UpdateConfigCommand updateFirstAgentResources(final String resources) {
        return new UpdateConfigCommand() {
            public CruiseConfig update(CruiseConfig cruiseConfig) {
                AgentConfig agentConfig = cruiseConfig.agents().get(0);
                agentConfig.setResources(new Resources(resources));
                return cruiseConfig;
            }
        };
    }

    private void deletePartial(String partial) {
        FileUtils.deleteQuietly(new File(externalConfigRepo, partial));
        gitAddDotAndCommit(externalConfigRepo);
    }

    private void checkinPartial(String partial) throws IOException {
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
        ClassPathResource resource = new ClassPathResource("external_git_config_repo");
        FileUtils.copyDirectory(resource.getFile(), configRepo);
        CommandLine.createCommandLine("git").withArg("init").withArg(configRepo.getAbsolutePath()).runOrBomb("");
        gitAddDotAndCommit(configRepo);
        ConsoleResult consoleResult = CommandLine.createCommandLine("git").withArg("log").withArg("-1").withArg("--pretty=format:%h").withWorkingDir(configRepo).runOrBomb("");

        return consoleResult.outputAsString();
    }

    private void gitAddDotAndCommit(File configRepo) {
        CommandLine.createCommandLine("git").withArg("add").withArg("-A").withArg(".").withWorkingDir(configRepo).runOrBomb("");
        CommandLine.createCommandLine("git").withArg("config").withArg("user.email").withArg("go_test@go_test.me").withWorkingDir(configRepo).runOrBomb("");
        CommandLine.createCommandLine("git").withArg("config").withArg("user.name").withArg("user").withWorkingDir(configRepo).runOrBomb("");
        CommandLine.createCommandLine("git").withArg("commit").withArg("-m").withArg("initial commit").withWorkingDir(configRepo).runOrBomb("");
    }
}