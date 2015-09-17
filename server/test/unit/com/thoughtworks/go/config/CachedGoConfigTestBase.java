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

package com.thoughtworks.go.config;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.*;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.*;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public abstract class CachedGoConfigTestBase {
    protected CachedGoConfig cachedGoConfig;
    protected GoConfigFileHelper configHelper;
    protected GoFileConfigDataSource dataSource;
    protected ServerHealthService serverHealthService;
    protected MetricsProbeService metricsProbeService = new NoOpMetricsProbeService();

    @Test public void shouldUpdateCachedConfigOnSave() throws Exception {
        assertThat(cachedGoConfig.currentConfig().agents().size(), is(1));
        configHelper.addAgent("hostname", "uuid2");
        assertThat(cachedGoConfig.currentConfig().agents().size(), is(2));
    }

    @Test public void shouldReloadCachedConfigWhenWriting() throws Exception {
        cachedGoConfig.writeWithLock(updateFirstAgentResources("osx"));
        assertThat(cachedGoConfig.currentConfig().agents().get(0).getResources().toString(), is("osx"));

        cachedGoConfig.writeWithLock(updateFirstAgentResources("osx, firefox"));
        assertThat(cachedGoConfig.currentConfig().agents().get(0).getResources().toString(), is("firefox | osx"));
    }

    @Test public void shouldReloadCachedConfigFromDisk() throws Exception {
        assertThat(cachedGoConfig.currentConfig().agents().size(), is(1));
        configHelper.writeXmlToConfigFile(ConfigFileFixture.TASKS_WITH_CONDITION);
        cachedGoConfig.forceReload();
        assertThat(cachedGoConfig.currentConfig().agents().size(), is(0));
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

        assertThat(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("dev")).size(), is(0));
        assertThat(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("acceptance")).size(), is(0));
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
            assertThat(configValidity.isValid(), is(false));
            assertThat(configValidity.errorMessage(), containsString("Environment Variable name 'GO_WORKING_DIR' is not unique for pipeline 'dev'"));
    }

    @Test public void shouldReturnCachedConfigIfConfigFileIsInvalid() throws Exception {
        CruiseConfig inTheBefore = cachedGoConfig.currentConfig();
        assertThat(inTheBefore.agents().size(), is(1));

        configHelper.writeXmlToConfigFile("invalid-xml");
        cachedGoConfig.forceReload();

        assertTrue(cachedGoConfig.currentConfig() == inTheBefore);
        assertThat(cachedGoConfig.checkConfigFileValid().isValid(), is(false));
    }

    @Test public void shouldClearInvalidExceptionWhenConfigErrorsAreFixed() throws Exception {
        configHelper.writeXmlToConfigFile("invalid-xml");
        cachedGoConfig.forceReload();

        cachedGoConfig.currentConfig();
        assertThat(cachedGoConfig.checkConfigFileValid().isValid(), is(false));

        configHelper.onSetUp();

        CruiseConfig cruiseConfig = cachedGoConfig.currentConfig();

        assertThat(cruiseConfig.agents().size(), is(1));
        assertThat(cachedGoConfig.checkConfigFileValid().isValid(), is(true));
    }

    @Test
    public void shouldSetServerHealthMessageWhenConfigFileIsInvalid() throws IOException {
        configHelper.writeXmlToConfigFile("invalid-xml");
        cachedGoConfig.forceReload();

        assertThat(cachedGoConfig.checkConfigFileValid().isValid(), is(false));

        List<ServerHealthState> serverHealthStates = serverHealthService.getAllLogs();
        assertThat(serverHealthStates.size(), is(1));
        assertThat(serverHealthStates.get(0), is(ServerHealthState.error(GoConfigService.INVALID_CRUISE_CONFIG_XML, "Error on line 1: Content is not allowed in prolog.", HealthStateType.invalidConfig())));
    }

    @Test
    public void shouldClearServerHealthMessageWhenConfigFileIsValid() throws IOException {
        ServerHealthState error = ServerHealthState.error(GoConfigService.INVALID_CRUISE_CONFIG_XML, "Error on line 1: Content is not allowed in prolog.", HealthStateType.invalidConfig());
        serverHealthService.update(error);

        assertThat(serverHealthService.getAllLogs().size(), is(1));

        configHelper.writeXmlToConfigFile(ConfigFileFixture.TASKS_WITH_CONDITION);
        cachedGoConfig.forceReload();

        assertThat(cachedGoConfig.checkConfigFileValid().isValid(), is(true));

        assertThat(serverHealthService.getAllLogs().size(), is(0));
    }

    @Test
    public void shouldReturnDefaultCruiseConfigIfLoadingTheConfigFailsForTheFirstTime() throws Exception {
        configHelper.writeXmlToConfigFile("invalid-xml");
        cachedGoConfig = new CachedFileGoConfig(dataSource, new ServerHealthService());
        assertThat(cachedGoConfig.currentConfig(), Matchers.<CruiseConfig>is(new BasicCruiseConfig()));
    }

    @Test
    public void shouldLoadConfigHolderIfNotAvailable() throws Exception {
        configHelper.addPipeline("foo", "bar");
        cachedGoConfig = new CachedFileGoConfig(dataSource, new ServerHealthService());
        dataSource.reloadIfModified();
        cachedGoConfig.forceReload();
        GoConfigHolder loaded = cachedGoConfig.loadConfigHolder();
        assertThat(loaded.config.hasPipelineNamed(new CaseInsensitiveString("foo")), is(true));
        assertThat(loaded.configForEdit.hasPipelineNamed(new CaseInsensitiveString("foo")), is(true));
    }

    @Test
    public void shouldGetConfigForEditAndRead() throws Exception {
        CruiseConfig cruiseConfig = configHelper.load();
        addPipelineWithParams(cruiseConfig);
        configHelper.writeConfigFile(cruiseConfig);
        cachedGoConfig = new CachedFileGoConfig(dataSource, new ServerHealthService());
        dataSource.reloadIfModified();

        cachedGoConfig.forceReload();

        PipelineConfig config = cachedGoConfig.currentConfig().pipelineConfigByName(new CaseInsensitiveString("mingle"));
        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig) byFolder(config.materialConfigs(), "folder");
        assertThat(hgMaterialConfig.getUrl(), is("http://hg-server/repo-name"));

        config = cachedGoConfig.loadForEditing().pipelineConfigByName(new CaseInsensitiveString("mingle"));
        hgMaterialConfig = (HgMaterialConfig) byFolder(config.materialConfigs(), "folder");
        assertThat(hgMaterialConfig.getUrl(), is("http://#{foo}/#{bar}"));

        cachedGoConfig.loadConfigHolder();
    }

    private void addPipelineWithParams(CruiseConfig cruiseConfig) {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("mingle", "dev", "ant");
        pipelineConfig.addParam(new ParamConfig("foo", "hg-server"));
        pipelineConfig.addParam(new ParamConfig("bar", "repo-name"));
        pipelineConfig.addMaterialConfig(MaterialConfigsMother.hgMaterialConfig("http://#{foo}/#{bar}", "folder"));
        cruiseConfig.addPipeline("another", pipelineConfig);
    }

    @Test
    public void shouldLoadConfigForReadAndEditWhenNewXMLIsWritten() throws Exception {
        cachedGoConfig.forceReload();
        GoConfigValidity configValidity = cachedGoConfig.checkConfigFileValid();
        assertThat(configValidity.isValid(), is(true));

        CruiseConfig cruiseConfig = cachedGoConfig.loadForEditing();

        addPipelineWithParams(cruiseConfig);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        new MagicalGoConfigXmlWriter(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService).write(cruiseConfig, buffer, false);

        cachedGoConfig.save(new String(buffer.toByteArray()), true);

        PipelineConfig reloadedPipelineConfig = cachedGoConfig.currentConfig().pipelineConfigByName(new CaseInsensitiveString("mingle"));
        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig) byFolder(reloadedPipelineConfig.materialConfigs(), "folder");
        assertThat(hgMaterialConfig.getUrl(), is("http://hg-server/repo-name"));

        reloadedPipelineConfig = cachedGoConfig.loadForEditing().pipelineConfigByName(new CaseInsensitiveString("mingle"));
        hgMaterialConfig = (HgMaterialConfig)  byFolder(reloadedPipelineConfig.materialConfigs(), "folder");
        assertThat(hgMaterialConfig.getUrl(), is("http://#{foo}/#{bar}"));
        
        GoConfigHolder configHolder = cachedGoConfig.loadConfigHolder();
        reloadedPipelineConfig = configHolder.config.pipelineConfigByName(new CaseInsensitiveString("mingle"));
        hgMaterialConfig = (HgMaterialConfig) byFolder(reloadedPipelineConfig.materialConfigs(), "folder");
        assertThat(hgMaterialConfig.getUrl(), is("http://hg-server/repo-name"));

        reloadedPipelineConfig = configHolder.configForEdit.pipelineConfigByName(new CaseInsensitiveString("mingle"));
        hgMaterialConfig = (HgMaterialConfig) byFolder(reloadedPipelineConfig.materialConfigs(), "folder");
        assertThat(hgMaterialConfig.getUrl(), is("http://#{foo}/#{bar}"));
    }

    @Test
    public void shouldLoadConfigForReadAndEditWhenConfigIsUpdatedThoughACommand() throws Exception {
        cachedGoConfig.forceReload();
        GoConfigValidity configValidity = cachedGoConfig.checkConfigFileValid();
        assertThat(configValidity.isValid(), is(true));

        cachedGoConfig.writeWithLock(new UpdateConfigCommand() {
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                addPipelineWithParams(cruiseConfig);
                return cruiseConfig;
            }
        });
        PipelineConfig reloadedPipelineConfig = cachedGoConfig.currentConfig().pipelineConfigByName(new CaseInsensitiveString("mingle"));
        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig) byFolder(reloadedPipelineConfig.materialConfigs(), "folder");
        assertThat(hgMaterialConfig.getUrl(), is("http://hg-server/repo-name"));

        reloadedPipelineConfig = cachedGoConfig.loadForEditing().pipelineConfigByName(new CaseInsensitiveString("mingle"));
        hgMaterialConfig = (HgMaterialConfig) byFolder(reloadedPipelineConfig.materialConfigs(), "folder");
        assertThat(hgMaterialConfig.getUrl(), is("http://#{foo}/#{bar}"));
    }

    @Test public void shouldNotifyConfigListenersWhenConfigChanges() throws Exception {
        final ConfigChangedListener listener = mock(ConfigChangedListener.class);
        cachedGoConfig.forceReload();
        
        cachedGoConfig.registerListener(listener);
        cachedGoConfig.writeWithLock(updateFirstAgentResources("osx"));

        verify(listener,times(2)).onConfigChange(any(BasicCruiseConfig.class));
    }

    @Test public void shouldNotNotifyWhenConfigIsNullDuringRegistration() throws Exception {
        configHelper.deleteConfigFile();
        cachedGoConfig = new CachedFileGoConfig(dataSource, new ServerHealthService());
        final ConfigChangedListener listener = mock(ConfigChangedListener.class);
        cachedGoConfig.registerListener(listener);
        verifyNoMoreInteractions(listener);
    }

    @Test
    public void shouldReturnMergedStatusWhenConfigIsMergedWithStaleCopy(){
        GoFileConfigDataSource goFileConfigDataSource = mock(GoFileConfigDataSource.class);
        UpdateConfigCommand updateConfigCommand = mock(UpdateConfigCommand.class);
        CruiseConfig currentConfig = GoConfigMother.configWithPipelines("p1");
        GoFileConfigDataSource.GoConfigSaveResult goConfigSaveResult = new GoFileConfigDataSource.GoConfigSaveResult(new GoConfigHolder(currentConfig, currentConfig), ConfigSaveState.MERGED);
        when(goFileConfigDataSource.writeWithLock(argThat(is(updateConfigCommand)), any(GoConfigHolder.class))).thenReturn(goConfigSaveResult);
        cachedGoConfig = new CachedFileGoConfig(goFileConfigDataSource, serverHealthService);

        ConfigSaveState configSaveState = cachedGoConfig.writeWithLock(updateConfigCommand);
        assertThat(configSaveState, is(ConfigSaveState.MERGED));
    }

    @Test
    public void shouldReturnUpdatedStatusWhenConfigIsUpdatedWithLatestCopy(){
        GoFileConfigDataSource goFileConfigDataSource = mock(GoFileConfigDataSource.class);
        UpdateConfigCommand updateConfigCommand = mock(UpdateConfigCommand.class);
        CruiseConfig currentConfig = GoConfigMother.configWithPipelines("p1");
        GoFileConfigDataSource.GoConfigSaveResult goConfigSaveResult = new GoFileConfigDataSource.GoConfigSaveResult(new GoConfigHolder(currentConfig, currentConfig), ConfigSaveState.UPDATED);
        when(goFileConfigDataSource.writeWithLock(argThat(is(updateConfigCommand)), any(GoConfigHolder.class))).thenReturn(goConfigSaveResult);
        cachedGoConfig = new CachedFileGoConfig(goFileConfigDataSource, serverHealthService);

        ConfigSaveState configSaveState = cachedGoConfig.writeWithLock(updateConfigCommand);
        assertThat(configSaveState, is(ConfigSaveState.UPDATED));
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


    public MaterialConfig byFolder(MaterialConfigs materialConfigs, String folder) {
        for (MaterialConfig materialConfig : materialConfigs) {
            if (materialConfig instanceof ScmMaterialConfig && ObjectUtil.nullSafeEquals(folder, materialConfig.getFolder())) {
                return materialConfig;
            }
        }
        return null;
    }
}
