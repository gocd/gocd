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

import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.KillAllChildProcessTask;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.label.PipelineLabel;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.plugin.configrepo.contract.*;
import com.thoughtworks.go.plugin.configrepo.contract.material.*;
import com.thoughtworks.go.plugin.configrepo.contract.tasks.*;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.command.HgUrlArgument;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static com.thoughtworks.go.config.PipelineConfig.LOCK_VALUE_LOCK_ON_FAILURE;
import static junit.framework.TestCase.fail;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigConverterTest {

    private ConfigConverter configConverter;
    private GoCipher goCipher;
    private PartialConfigLoadContext context;
    private List<String> filter = new ArrayList<>();
    private CachedGoConfig cachedGoConfig;

    Collection<CREnvironmentVariable> environmentVariables = new ArrayList<>();
    Collection<CRParameter> parameters = new ArrayList<>();
    Collection<CRTab> tabs = new ArrayList<>();
    Collection<String> resources = new ArrayList<>();
    Collection<CRArtifact> artifacts = new ArrayList<>();
    Collection<CRPropertyGenerator> artifactPropertiesGenerators = new ArrayList<>();
    List<CRTask> tasks = new ArrayList<>();
    ArrayList<String> authorizedRoles;
    ArrayList<String> authorizedUsers;
    ArrayList<CRJob> jobs;

    private CRStage crStage;
    private CRTrackingTool trackingTool;
    private CRTimer timer;
    private ArrayList<CRMaterial> materials;
    private List<CRStage> stages;
    private CRGitMaterial git;

    @Before
    public void setUp() throws CryptoException {
        environmentVariables = new ArrayList<>();
        tabs = new ArrayList<>();
        resources = new ArrayList<>();
        artifacts = new ArrayList<>();
        artifactPropertiesGenerators = new ArrayList<>();
        tasks = new ArrayList<>();
        authorizedRoles = new ArrayList<>();
        authorizedUsers = new ArrayList<>();
        jobs = new ArrayList<>();
        stages = new ArrayList<>();
        materials = new ArrayList<>();

        cachedGoConfig = mock(CachedGoConfig.class);
        goCipher = mock(GoCipher.class);
        context = mock(PartialConfigLoadContext.class);
        configConverter = new ConfigConverter(goCipher, cachedGoConfig);
        String encryptedText = "secret";
        when(goCipher.decrypt("encryptedvalue")).thenReturn(encryptedText);
        when(goCipher.encrypt("secret")).thenReturn("encryptedvalue");

        filter = new ArrayList<>();
        filter.add("filter");

        environmentVariables.add(new CREnvironmentVariable("key", "value"));
        tabs.add(new CRTab("tabname", "tabpath"));
        resources.add("resource1");
        artifacts.add(new CRBuiltInArtifact("src", "dest"));
        artifactPropertiesGenerators.add(new CRPropertyGenerator("name", "src", "path"));

        tasks.add(new CRFetchArtifactTask(CRRunIf.failed, null,
                "upstream", "stage", "job", "src", "dest", false));

        jobs.add(new CRJob("name", environmentVariables, tabs,
                resources, null, artifacts, artifactPropertiesGenerators,
                true, 5, 120, tasks));

        authorizedUsers.add("authUser");
        authorizedRoles.add("authRole");

        CRApproval approval = new CRApproval(CRApprovalCondition.manual, authorizedRoles, authorizedUsers);

        crStage = new CRStage("stageName", true, true, true, approval, environmentVariables, jobs);
        stages.add(crStage);

        git = new CRGitMaterial("name", "folder", true, true, filter, "url", "branch", false);
        materials.add(git);

        trackingTool = new CRTrackingTool("link", "regex");
        timer = new CRTimer("timer", true);
    }

    @Test
    public void shouldConvertEnvironmentVariableWhenNotSecure() {
        CREnvironmentVariable crEnvironmentVariable = new CREnvironmentVariable("key1", "value");
        EnvironmentVariableConfig result = configConverter.toEnvironmentVariableConfig(crEnvironmentVariable);
        assertThat(result.getValue(), is("value"));
        assertThat(result.getName(), is("key1"));
        assertThat(result.isSecure(), is(false));
    }

    @Test
    public void shouldConvertNullEnvironmentVariableWhenNotSecure() {
        CREnvironmentVariable crEnvironmentVariable = new CREnvironmentVariable("key1", null);
        EnvironmentVariableConfig result = configConverter.toEnvironmentVariableConfig(crEnvironmentVariable);
        assertThat(result.getValue(), is(""));
        assertThat(result.getName(), is("key1"));
        assertThat(result.isSecure(), is(false));
    }

    @Test
    public void shouldConvertEnvironmentVariableWhenSecure() {
        CREnvironmentVariable crEnvironmentVariable = new CREnvironmentVariable("key1", null, "encryptedvalue");
        EnvironmentVariableConfig result = configConverter.toEnvironmentVariableConfig(crEnvironmentVariable);
        assertThat(result.isSecure(), is(true));
        assertThat(result.getValue(), is("secret"));
        assertThat(result.getName(), is("key1"));
    }

    @Test
    public void shouldMigrateEnvironment() {
        ArrayList<CREnvironmentVariable> environmentVariables = new ArrayList<>();
        environmentVariables.add(new CREnvironmentVariable("key", "value"));
        ArrayList<String> agents = new ArrayList<>();
        agents.add("12");
        ArrayList<String> pipelines = new ArrayList<>();
        pipelines.add("pipe1");
        CREnvironment crEnvironment = new CREnvironment("dev", environmentVariables, agents, pipelines);

        BasicEnvironmentConfig environmentConfig = configConverter.toEnvironmentConfig(crEnvironment);
        assertThat(environmentConfig.name().toLower(), is("dev"));
        assertThat(environmentConfig.contains("pipe1"), is(true));
        assertThat(environmentConfig.hasVariable("key"), is(true));
        assertThat(environmentConfig.hasAgent("12"), is(true));
    }

    @Test
    public void shouldMigratePluggableTask() {
        ArrayList<CRConfigurationProperty> configs = new ArrayList<>();
        configs.add(new CRConfigurationProperty("k", "m", null));
        CRPluggableTask pluggableTask = new CRPluggableTask(CRRunIf.any, null,
                new CRPluginConfiguration("myplugin", "1"), configs);
        PluggableTask result = (PluggableTask) configConverter.toAbstractTask(pluggableTask);

        assertThat(result.getPluginConfiguration().getId(), is("myplugin"));
        assertThat(result.getPluginConfiguration().getVersion(), is("1"));
        assertThat(result.getConfiguration().getProperty("k").getValue(), is("m"));
        assertThat(result.getConditions().first(), is(RunIfConfig.ANY));
    }

    @Test
    public void shouldMigrateRakeTask() {
        CRBuildTask crBuildTask = new CRBuildTask(CRRunIf.failed, null, "Rakefile.rb", "build", "src", CRBuildFramework.rake);
        RakeTask result = (RakeTask) configConverter.toAbstractTask(crBuildTask);

        assertRakeTask(result);
    }

    private void assertRakeTask(RakeTask result) {
        assertThat(result.getConditions().first(), is(RunIfConfig.FAILED));
        assertThat(result.getBuildFile(), is("Rakefile.rb"));
        assertThat(result.getTarget(), is("build"));
        assertThat(result.workingDirectory(), is("src"));
    }

    @Test
    public void shouldMigrateAntTask() {
        CRTask cancel = new CRBuildTask(CRRunIf.failed, null, "Rakefile.rb", "build", "src", CRBuildFramework.rake);
        CRBuildTask crBuildTask = new CRBuildTask(CRRunIf.failed, cancel, "ant", "build", "src", CRBuildFramework.ant);
        AntTask result = (AntTask) configConverter.toAbstractTask(crBuildTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.FAILED));
        assertThat(result.getBuildFile(), is("ant"));
        assertThat(result.getTarget(), is("build"));
        assertThat(result.workingDirectory(), is("src"));

        assertThat(result.cancelTask() instanceof RakeTask, is(true));
        assertRakeTask((RakeTask) result.cancelTask());
    }

    @Test
    public void shouldMigrateNantTask() {
        CRBuildTask crBuildTask = new CRNantTask(CRRunIf.passed, null, "nant", "build", "src", "path");
        NantTask result = (NantTask) configConverter.toAbstractTask(crBuildTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.PASSED));
        assertThat(result.getBuildFile(), is("nant"));
        assertThat(result.getTarget(), is("build"));
        assertThat(result.workingDirectory(), is("src"));
    }

    @Test
    public void shouldConvertExecTaskWhenCancelIsNotSpecified() {
        CRExecTask crExecTask = new CRExecTask(CRRunIf.failed, null, "bash", "work", 120L, Arrays.asList("1", "2"));
        ExecTask result = (ExecTask) configConverter.toAbstractTask(crExecTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.FAILED));
        assertThat(result.command(), is("bash"));
        assertThat(result.getArgList(), hasItem(new Argument("1")));
        assertThat(result.getArgList(), hasItem(new Argument("2")));
        assertThat(result.workingDirectory(), is("work"));
        assertThat(result.getTimeout(), is(120L));
        assertThat(result.getOnCancelConfig().getTask(), instanceOf(KillAllChildProcessTask.class));
    }

    @Test
    public void shouldConvertExecTaskWhenCancelIsSpecified() {
        CRExecTask crExecTask = new CRExecTask(CRRunIf.failed, new CRExecTask("kill"), "bash", "work", 120L, Arrays.asList("1", "2"));
        ExecTask result = (ExecTask) configConverter.toAbstractTask(crExecTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.FAILED));
        assertThat(result.command(), is("bash"));
        assertThat(result.getArgList(), hasItem(new Argument("1")));
        assertThat(result.getArgList(), hasItem(new Argument("2")));
        assertThat(result.workingDirectory(), is("work"));
        assertThat(result.getTimeout(), is(120L));
        assertThat(result.getOnCancelConfig().getTask(), instanceOf(ExecTask.class));
        ExecTask cancel = (ExecTask) result.getOnCancelConfig().getTask();
        assertThat(cancel.command(), is("kill"));
    }

    @Test
    public void shouldConvertFetchArtifactTaskAndSetEmptyStringWhenPipelineIsNotSpecified() {
        // if not then null causes errors in parameter expansion
        CRFetchArtifactTask crFetchArtifactTask = new CRFetchArtifactTask(CRRunIf.passed, null,
                null, "stage", "job", "src", null, false);

        FetchTask result = (FetchTask) configConverter.toAbstractTask(crFetchArtifactTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.PASSED));
        assertThat(result.getDest(), is(""));
        assertThat(result.getJob().toLower(), is("job"));
        assertThat(result.getPipelineName().toLower(), is(""));
        assertThat(result.getSrc(), is("src"));
        assertThat(result.isSourceAFile(), is(true));
    }

    @Test
    public void shouldConvertFetchArtifactTaskWhenDestinationIsNotSpecified() {
        CRFetchArtifactTask crFetchArtifactTask = new CRFetchArtifactTask(CRRunIf.passed, null,
                "upstream", "stage", "job", "src", null, false);

        FetchTask result = (FetchTask) configConverter.toAbstractTask(crFetchArtifactTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.PASSED));
        assertThat(result.getDest(), is(""));
        assertThat(result.getJob().toLower(), is("job"));
        assertThat(result.getPipelineName().toLower(), is("upstream"));
        assertThat(result.getSrc(), is("src"));
        assertThat(result.isSourceAFile(), is(true));
    }

    @Test
    public void shouldConvertFetchArtifactTaskWhenSourceIsDirectory() {
        CRFetchArtifactTask crFetchArtifactTask = new CRFetchArtifactTask(CRRunIf.failed, null,
                "upstream", "stage", "job", "src", "dest", true);

        FetchTask result = (FetchTask) configConverter.toAbstractTask(crFetchArtifactTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.FAILED));
        assertThat(result.getDest(), is("dest"));
        assertThat(result.getJob().toLower(), is("job"));
        assertThat(result.getPipelineName().toLower(), is("upstream"));
        assertThat(result.getSrc(), is("src"));
        assertNull(result.getSrcfile());
        assertThat(result.isSourceAFile(), is(false));
    }

    @Test
    public void shouldConvertFetchArtifactTaskWhenSourceIsFile() {
        CRFetchArtifactTask crFetchArtifactTask = new CRFetchArtifactTask(CRRunIf.failed, null,
                "upstream", "stage", "job", "src", "dest", false);

        FetchTask result = (FetchTask) configConverter.toAbstractTask(crFetchArtifactTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.FAILED));
        assertThat(result.getDest(), is("dest"));
        assertThat(result.getJob().toLower(), is("job"));
        assertThat(result.getPipelineName().toLower(), is("upstream"));
        assertThat(result.getSrc(), is("src"));
        assertNull(result.getSrcdir());
        assertThat(result.isSourceAFile(), is(true));
    }

    @Test
    public void shouldConvertFetchPluggableArtifactTaskAndSetEmptyStringWhenPipelineIsNotSpecified() {
        CRConfigurationProperty crConfigurationProperty = new CRConfigurationProperty("k1", "v1", null);
        CRFetchPluggableArtifactTask crFetchPluggableArtifactTask = new CRFetchPluggableArtifactTask(CRRunIf.passed, null,
                null, "stage", "job", "artifactId", crConfigurationProperty);

        FetchPluggableArtifactTask result = (FetchPluggableArtifactTask) configConverter.toAbstractTask(crFetchPluggableArtifactTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.PASSED));
        assertThat(result.getJob().toLower(), is("job"));
        assertThat(result.getPipelineName().toLower(), is(""));
        assertThat(result.getArtifactId(), is("artifactId"));
        assertThat(result.getConfiguration().getProperty("k1").getValue(), is("v1"));
    }

    @Test
    public void shouldConvertFetchPluggableArtifactTaskWhenConfigurationIsNotSet() {
        CRFetchPluggableArtifactTask crFetchPluggableArtifactTask = new CRFetchPluggableArtifactTask(CRRunIf.passed, null,
                null, "stage", "job", "artifactId");

        FetchPluggableArtifactTask result = (FetchPluggableArtifactTask) configConverter.toAbstractTask(crFetchPluggableArtifactTask);

        assertThat(result.getConditions().first(), is(RunIfConfig.PASSED));
        assertThat(result.getJob().toLower(), is("job"));
        assertThat(result.getPipelineName().toLower(), is(""));
        assertThat(result.getArtifactId(), is("artifactId"));
        assertThat(result.getConfiguration().isEmpty(), is(true));
    }

    @Test
    public void shouldConvertDependencyMaterial() {
        CRDependencyMaterial crDependencyMaterial = new CRDependencyMaterial("name", "pipe", "stage");
        DependencyMaterialConfig dependencyMaterialConfig =
                (DependencyMaterialConfig) configConverter.toMaterialConfig(crDependencyMaterial, context);

        assertThat(dependencyMaterialConfig.getName().toLower(), is("name"));
        assertThat(dependencyMaterialConfig.getPipelineName().toLower(), is("pipe"));
        assertThat(dependencyMaterialConfig.getStageName().toLower(), is("stage"));
    }

    @Test
    public void shouldConvertGitMaterial() {
        CRGitMaterial crGitMaterial = new CRGitMaterial("name", "folder", true, true, filter, "url", "branch", false);

        GitMaterialConfig gitMaterialConfig =
                (GitMaterialConfig) configConverter.toMaterialConfig(crGitMaterial, context);

        assertThat(gitMaterialConfig.getName().toLower(), is("name"));
        assertThat(gitMaterialConfig.getFolder(), is("folder"));
        assertThat(gitMaterialConfig.getAutoUpdate(), is(true));
        assertThat(gitMaterialConfig.isInvertFilter(), is(false));
        assertThat(gitMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(gitMaterialConfig.getUrl(), is("url"));
        assertThat(gitMaterialConfig.getBranch(), is("branch"));
    }

    @Test
    public void shouldConvertGitMaterialWhenWhitelist() {
        CRGitMaterial crGitMaterial = new CRGitMaterial("name", "folder", true, true, filter, "url", "branch", true);

        GitMaterialConfig gitMaterialConfig =
                (GitMaterialConfig) configConverter.toMaterialConfig(crGitMaterial, context);

        assertThat(gitMaterialConfig.getName().toLower(), is("name"));
        assertThat(gitMaterialConfig.getFolder(), is("folder"));
        assertThat(gitMaterialConfig.getAutoUpdate(), is(true));
        assertThat(gitMaterialConfig.isInvertFilter(), is(true));
        assertThat(gitMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(gitMaterialConfig.getUrl(), is("url"));
        assertThat(gitMaterialConfig.getBranch(), is("branch"));
    }

    @Test
    public void shouldConvertHgMaterialWhenWhitelist() {
        CRHgMaterial crHgMaterial = new CRHgMaterial("name", "folder", true, true, filter, "url");

        HgMaterialConfig hgMaterialConfig =
                (HgMaterialConfig) configConverter.toMaterialConfig(crHgMaterial, context);

        assertThat(hgMaterialConfig.getName().toLower(), is("name"));
        assertThat(hgMaterialConfig.getFolder(), is("folder"));
        assertThat(hgMaterialConfig.getAutoUpdate(), is(true));
        assertThat(hgMaterialConfig.isInvertFilter(), is(true));
        assertThat(hgMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(hgMaterialConfig.getUrl(), is("url"));
    }

    @Test
    public void shouldConvertGitMaterialWhenNulls() {
        CRGitMaterial crGitMaterial = new CRGitMaterial();
        crGitMaterial.setUrl("url");

        GitMaterialConfig gitMaterialConfig =
                (GitMaterialConfig) configConverter.toMaterialConfig(crGitMaterial, context);

        assertNull(crGitMaterial.getName());
        assertNull(crGitMaterial.getDirectory());
        assertThat(gitMaterialConfig.getAutoUpdate(), is(true));
        assertThat(gitMaterialConfig.isShallowClone(), is(false));
        assertThat(gitMaterialConfig.filter(), is(new Filter()));
        assertThat(gitMaterialConfig.getUrl(), is("url"));
        assertThat(gitMaterialConfig.getBranch(), is("master"));
    }

    @Test
    public void shouldConvertConfigMaterialWhenConfigRepoIsGitWithUrlOnly() {
        // this url would be configured inside xml config-repo section
        GitMaterialConfig configRepoMaterial = new GitMaterialConfig("url");
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial();

        MaterialConfig materialConfig = configConverter.toMaterialConfig(crConfigMaterial, context);
        assertNull("shouldSetEmptyMaterialNameAsInConfigRepoSourceCode", materialConfig.getName());

        GitMaterialConfig gitMaterialConfig = (GitMaterialConfig) materialConfig;
        assertThat(gitMaterialConfig.getAutoUpdate(), is(true));
        assertThat(gitMaterialConfig.getUrl(), is("url"));
        assertNull(gitMaterialConfig.getFolder());
        assertThat(gitMaterialConfig.getAutoUpdate(), is(true));
        assertThat(gitMaterialConfig.isShallowClone(), is(false));
        assertThat(gitMaterialConfig.filter(), is(new Filter()));
        assertThat(gitMaterialConfig.getUrl(), is("url"));
        assertThat(gitMaterialConfig.getBranch(), is("master"));
    }


    @Test
    public void shouldConvertConfigMaterialWhenConfigRepoIsGitWithBlacklist() {
        // this url would be configured inside xml config-repo section
        GitMaterialConfig configRepoMaterial = new GitMaterialConfig("url");
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial();
        crConfigMaterial.setFilter(new CRFilter(filter, false));

        MaterialConfig materialConfig = configConverter.toMaterialConfig(crConfigMaterial, context);
        assertNull("shouldSetEmptyMaterialNameAsInConfigRepoSourceCode", materialConfig.getName());

        GitMaterialConfig gitMaterialConfig = (GitMaterialConfig) materialConfig;
        assertThat(gitMaterialConfig.getAutoUpdate(), is(true));
        assertThat(gitMaterialConfig.getUrl(), is("url"));
        assertNull(gitMaterialConfig.getFolder());
        assertThat(gitMaterialConfig.getAutoUpdate(), is(true));
        assertThat(gitMaterialConfig.isShallowClone(), is(false));
        Filter blacklistFilter = new Filter(new IgnoredFiles("filter"));
        assertThat(gitMaterialConfig.filter(), is(blacklistFilter));
        assertThat(gitMaterialConfig.getUrl(), is("url"));
        assertThat(gitMaterialConfig.getBranch(), is("master"));
    }

    @Test
    public void shouldConvertConfigMaterialWhenConfigRepoIsHgWithWhitelist() {
        // this url would be configured inside xml config-repo section
        HgMaterialConfig configRepoMaterial = new HgMaterialConfig("url", "folder");
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial(null, null, new CRFilter(filter, true));

        MaterialConfig materialConfig = configConverter.toMaterialConfig(crConfigMaterial, context);
        assertNull("shouldSetEmptyMaterialNameAsInConfigRepoSourceCode", materialConfig.getName());

        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig) materialConfig;
        assertThat(hgMaterialConfig.getAutoUpdate(), is(true));
        assertThat(hgMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(hgMaterialConfig.getUrl(), is("url"));
        Filter whitelistFilter = new Filter(new IgnoredFiles("filter"));
        assertThat(hgMaterialConfig.filter(), is(whitelistFilter));
        assertThat(hgMaterialConfig.isInvertFilter(), is(true));
    }

    @Test
    public void shouldConvertConfigMaterialWhenConfigRepoIsHgWithEmptyFilter() {
        // this url would be configured inside xml config-repo section
        HgMaterialConfig configRepoMaterial = new HgMaterialConfig("url", "folder");
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial(null, null, new CRFilter(new ArrayList<>(), true));

        MaterialConfig materialConfig = configConverter.toMaterialConfig(crConfigMaterial, context);
        assertNull("shouldSetEmptyMaterialNameAsInConfigRepoSourceCode", materialConfig.getName());

        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig) materialConfig;
        assertThat(hgMaterialConfig.getAutoUpdate(), is(true));
        assertThat(hgMaterialConfig.getFilterAsString(), is(""));
        assertThat(hgMaterialConfig.getUrl(), is("url"));
        assertThat(hgMaterialConfig.filter(), is(new Filter()));
        assertThat(hgMaterialConfig.isInvertFilter(), is(false));
    }

    @Test
    public void shouldConvertConfigMaterialWhenConfigRepoIsHg() {
        // these parameters would be configured inside xml config-repo section
        HgMaterialConfig configRepoMaterial = new HgMaterialConfig(new HgUrlArgument("url"), true, new Filter(new IgnoredFiles("ignore")), false, "folder", new CaseInsensitiveString("name"));
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial("example", null, null);

        MaterialConfig materialConfig = configConverter.toMaterialConfig(crConfigMaterial, context);
        assertThat("shouldSetMaterialNameAsInConfigRepoSourceCode", materialConfig.getName().toLower(), is("example"));
        assertThat("shouldUseFolderFromXMLWhenConfigRepoHasNone", materialConfig.getFolder(), is("folder"));

        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig) materialConfig;
        assertThat(hgMaterialConfig.getAutoUpdate(), is(true));
        assertThat(hgMaterialConfig.getFilterAsString(), is("ignore"));
        assertThat(hgMaterialConfig.getUrl(), is("url"));
    }

    @Test
    public void shouldConvertConfigMaterialWhenConfigRepoIsHgWithDestination() {
        // these parameters would be configured inside xml config-repo section
        HgMaterialConfig configRepoMaterial = new HgMaterialConfig(new HgUrlArgument("url"), true, new Filter(new IgnoredFiles("ignore")), false, "folder", new CaseInsensitiveString("name"));
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial("example", "dest1", null);

        MaterialConfig materialConfig = configConverter.toMaterialConfig(crConfigMaterial, context);
        assertThat("shouldSetMaterialNameAsInConfigRepoSourceCode", materialConfig.getName().toLower(), is("example"));
        assertThat("shouldUseFolderFromConfigRepoWhenSpecified", materialConfig.getFolder(), is("dest1"));

        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig) materialConfig;
        assertThat(hgMaterialConfig.getAutoUpdate(), is(true));
        assertThat(hgMaterialConfig.getFilterAsString(), is("ignore"));
        assertThat(hgMaterialConfig.getUrl(), is("url"));
    }

    @Test
    public void shouldConvertConfigMaterialWhenPluggableScmMaterial() {
        SCM myscm = new SCM("scmid", new PluginConfiguration(), new Configuration());
        SCMs scms = new SCMs(myscm);
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setSCMs(scms);
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);
        PluggableSCMMaterialConfig configRepoMaterial = new PluggableSCMMaterialConfig(new CaseInsensitiveString("scmid"), myscm, null, null);
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial("example", "dest1", null);

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig =
                (PluggableSCMMaterialConfig) configConverter.toMaterialConfig(crConfigMaterial, context);

        assertThat(pluggableSCMMaterialConfig.getName().toLower(), is("example"));
        assertThat(pluggableSCMMaterialConfig.getSCMConfig(), is(myscm));
        assertThat(pluggableSCMMaterialConfig.getScmId(), is("scmid"));
        assertThat(pluggableSCMMaterialConfig.getFolder(), is("dest1"));
        assertThat(pluggableSCMMaterialConfig.getFilterAsString(), is(""));
    }


    @Test
    public void shouldFailToConvertConfigMaterialWhenPluggableScmMaterialWithWhitelist() {
        SCM myscm = new SCM("scmid", new PluginConfiguration(), new Configuration());
        SCMs scms = new SCMs(myscm);
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setSCMs(scms);
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);
        PluggableSCMMaterialConfig configRepoMaterial = new PluggableSCMMaterialConfig(new CaseInsensitiveString("scmid"), myscm, null, null);
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial("example", "dest1", new CRFilter(filter, true));

        try {
            configConverter.toMaterialConfig(crConfigMaterial, context);
            fail("should have thrown");
        } catch (ConfigConvertionException ex) {
            assertThat(ex.getMessage(), is("Plugable SCMs do not support whitelisting"));
        }
    }

    @Test
    public void shouldConvertHgMaterial() {
        CRHgMaterial crHgMaterial = new CRHgMaterial("name", "folder", true, false, filter, "url");

        HgMaterialConfig hgMaterialConfig =
                (HgMaterialConfig) configConverter.toMaterialConfig(crHgMaterial, context);

        assertThat(hgMaterialConfig.getName().toLower(), is("name"));
        assertThat(hgMaterialConfig.getFolder(), is("folder"));
        assertThat(hgMaterialConfig.getAutoUpdate(), is(true));
        assertThat(hgMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(hgMaterialConfig.getUrl(), is("url"));
    }

    @Test
    public void shouldConvertHgMaterialWhenNullName() {
        CRHgMaterial crHgMaterial = new CRHgMaterial(null, "folder", true, false, filter, "url");

        HgMaterialConfig hgMaterialConfig =
                (HgMaterialConfig) configConverter.toMaterialConfig(crHgMaterial, context);

        assertNull(hgMaterialConfig.getName());
        assertThat(hgMaterialConfig.getFolder(), is("folder"));
        assertThat(hgMaterialConfig.getAutoUpdate(), is(true));
        assertThat(hgMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(hgMaterialConfig.getUrl(), is("url"));
        assertThat(hgMaterialConfig.getInvertFilter(), is(false));
    }

    @Test
    public void shouldConvertHgMaterialWhenEmptyName() {
        CRHgMaterial crHgMaterial = new CRHgMaterial("", "folder", true, false, filter, "url");

        HgMaterialConfig hgMaterialConfig =
                (HgMaterialConfig) configConverter.toMaterialConfig(crHgMaterial, context);

        assertNull(hgMaterialConfig.getName());
        assertThat(hgMaterialConfig.getFolder(), is("folder"));
        assertThat(hgMaterialConfig.getAutoUpdate(), is(true));
        assertThat(hgMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(hgMaterialConfig.getUrl(), is("url"));
    }

    @Test
    public void shouldConvertP4MaterialWhenEncryptedPassword() {
        CRP4Material crp4Material = CRP4Material.withEncryptedPassword(
                "name", "folder", false, false, filter, "server:port", "user", "encryptedvalue", true, "view");

        P4MaterialConfig p4MaterialConfig =
                (P4MaterialConfig) configConverter.toMaterialConfig(crp4Material, context);

        assertThat(p4MaterialConfig.getName().toLower(), is("name"));
        assertThat(p4MaterialConfig.getFolder(), is("folder"));
        assertThat(p4MaterialConfig.getAutoUpdate(), is(false));
        assertThat(p4MaterialConfig.getFilterAsString(), is("filter"));
        assertThat(p4MaterialConfig.getUrl(), is("server:port"));
        assertThat(p4MaterialConfig.getUserName(), is("user"));
        assertThat(p4MaterialConfig.getPassword(), is("secret"));
        assertThat(p4MaterialConfig.getUseTickets(), is(true));
        assertThat(p4MaterialConfig.getView(), is("view"));

    }

    @Test
    public void shouldConvertP4MaterialWhenPlainPassword() {
        CRP4Material crp4Material = CRP4Material.withPlainPassword(
                "name", "folder", false, false, filter, "server:port", "user", "secret", true, "view");

        P4MaterialConfig p4MaterialConfig =
                (P4MaterialConfig) configConverter.toMaterialConfig(crp4Material, context);

        assertThat(p4MaterialConfig.getName().toLower(), is("name"));
        assertThat(p4MaterialConfig.getFolder(), is("folder"));
        assertThat(p4MaterialConfig.getAutoUpdate(), is(false));
        assertThat(p4MaterialConfig.getFilterAsString(), is("filter"));
        assertThat(p4MaterialConfig.getUrl(), is("server:port"));
        assertThat(p4MaterialConfig.getUserName(), is("user"));
        assertThat(p4MaterialConfig.getPassword(), is("secret"));
        assertThat(p4MaterialConfig.getUseTickets(), is(true));
        assertThat(p4MaterialConfig.getView(), is("view"));

    }

    @Test
    public void shouldConvertSvmMaterialWhenEncryptedPassword() {
        CRSvnMaterial crSvnMaterial = CRSvnMaterial.withEncryptedPassword("name", "folder", true, false, filter, "url", "username", "encryptedvalue", true);

        SvnMaterialConfig svnMaterialConfig =
                (SvnMaterialConfig) configConverter.toMaterialConfig(crSvnMaterial, context);

        assertThat(svnMaterialConfig.getName().toLower(), is("name"));
        assertThat(svnMaterialConfig.getFolder(), is("folder"));
        assertThat(svnMaterialConfig.getAutoUpdate(), is(true));
        assertThat(svnMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(svnMaterialConfig.getUrl(), is("url"));
        assertThat(svnMaterialConfig.getUserName(), is("username"));
        assertThat(svnMaterialConfig.getPassword(), is("secret"));
        assertThat(svnMaterialConfig.isCheckExternals(), is(true));
    }

    @Test
    public void shouldConvertSvmMaterialWhenPlainPassword() {
        CRSvnMaterial crSvnMaterial = new CRSvnMaterial("name", "folder", true, false, filter, "url", "username", "secret", true);

        SvnMaterialConfig svnMaterialConfig =
                (SvnMaterialConfig) configConverter.toMaterialConfig(crSvnMaterial, context);

        assertThat(svnMaterialConfig.getName().toLower(), is("name"));
        assertThat(svnMaterialConfig.getFolder(), is("folder"));
        assertThat(svnMaterialConfig.getAutoUpdate(), is(true));
        assertThat(svnMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(svnMaterialConfig.getUrl(), is("url"));
        assertThat(svnMaterialConfig.getUserName(), is("username"));
        assertThat(svnMaterialConfig.getPassword(), is("secret"));
        assertThat(svnMaterialConfig.isCheckExternals(), is(true));
    }

    @Test
    public void shouldConvertTfsMaterialWhenPlainPassword() {
        CRTfsMaterial crTfsMaterial = CRTfsMaterial.withPlainPassword(
                "name", "folder", false, false, filter, "url", "domain", "user", "secret", "project");

        TfsMaterialConfig tfsMaterialConfig =
                (TfsMaterialConfig) configConverter.toMaterialConfig(crTfsMaterial, context);

        assertThat(tfsMaterialConfig.getName().toLower(), is("name"));
        assertThat(tfsMaterialConfig.getFolder(), is("folder"));
        assertThat(tfsMaterialConfig.getAutoUpdate(), is(false));
        assertThat(tfsMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(tfsMaterialConfig.getUrl(), is("url"));
        assertThat(tfsMaterialConfig.getUserName(), is("user"));
        assertThat(tfsMaterialConfig.getPassword(), is("secret"));
        assertThat(tfsMaterialConfig.getDomain(), is("domain"));
        assertThat(tfsMaterialConfig.getProjectPath(), is("project"));

    }

    @Test
    public void shouldConvertTfsMaterialWhenEncryptedPassword() {
        CRTfsMaterial crTfsMaterial = CRTfsMaterial.withEncryptedPassword(
                "name", "folder", false, false, filter, "url", "domain", "user", "encryptedvalue", "project");

        TfsMaterialConfig tfsMaterialConfig =
                (TfsMaterialConfig) configConverter.toMaterialConfig(crTfsMaterial, context);

        assertThat(tfsMaterialConfig.getName().toLower(), is("name"));
        assertThat(tfsMaterialConfig.getFolder(), is("folder"));
        assertThat(tfsMaterialConfig.getAutoUpdate(), is(false));
        assertThat(tfsMaterialConfig.getFilterAsString(), is("filter"));
        assertThat(tfsMaterialConfig.getUrl(), is("url"));
        assertThat(tfsMaterialConfig.getUserName(), is("user"));
        assertThat(tfsMaterialConfig.getPassword(), is("secret"));
        assertThat(tfsMaterialConfig.getDomain(), is("domain"));
        assertThat(tfsMaterialConfig.getProjectPath(), is("project"));

    }

    @Test
    public void shouldConvertPluggableScmMaterial() {
        SCM myscm = new SCM("scmid", new PluginConfiguration(), new Configuration());
        SCMs scms = new SCMs(myscm);

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setSCMs(scms);
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);

        CRPluggableScmMaterial crPluggableScmMaterial = new CRPluggableScmMaterial("name", "scmid", "directory", filter);

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig =
                (PluggableSCMMaterialConfig) configConverter.toMaterialConfig(crPluggableScmMaterial, context);

        assertThat(pluggableSCMMaterialConfig.getName().toLower(), is("name"));
        assertThat(pluggableSCMMaterialConfig.getSCMConfig(), is(myscm));
        assertThat(pluggableSCMMaterialConfig.getScmId(), is("scmid"));
        assertThat(pluggableSCMMaterialConfig.getFolder(), is("directory"));
        assertThat(pluggableSCMMaterialConfig.getFilterAsString(), is("filter"));
    }

    @Test
    public void shouldConvertPluggableScmMaterialWithNewSCM() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setSCMs(new SCMs());
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);
        Configuration config = new Configuration();
        config.addNewConfigurationWithValue("url", "url", false);
        SCM myscm = new SCM("scmid", new PluginConfiguration("plugin_id", "1.0"), config);

        CRPluggableScmMaterial crPluggableScmMaterial = new CRPluggableScmMaterial("name", "scmid", "directory", filter);
        crPluggableScmMaterial.setPluginConfiguration(new CRPluginConfiguration("plugin_id", "1.0"));
        crPluggableScmMaterial.getConfiguration().add(new CRConfigurationProperty("url", "url"));

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig =
                (PluggableSCMMaterialConfig) configConverter.toMaterialConfig(crPluggableScmMaterial, context);

        assertThat(pluggableSCMMaterialConfig.getName().toLower(), is("name"));
        assertThat(pluggableSCMMaterialConfig.getSCMConfig().getName(), is("name"));
        assertThat(pluggableSCMMaterialConfig.getSCMConfig().getId(), is("scmid"));
        assertThat(pluggableSCMMaterialConfig.getSCMConfig().getFingerprint(), is(myscm.getFingerprint()));
        assertThat(pluggableSCMMaterialConfig.getScmId(), is("scmid"));
        assertThat(pluggableSCMMaterialConfig.getFolder(), is("directory"));
        assertThat(pluggableSCMMaterialConfig.getFilterAsString(), is("filter"));
    }

    @Test
    public void shouldConvertPluggableScmMaterialWithADuplicateSCMFingerPrintShouldUseWhatAlreadyExists() {
        Configuration config = new Configuration();
        config.addNewConfigurationWithValue("url", "url", false);
        SCM scm = new SCM("scmid", new PluginConfiguration("plugin_id", "1.0"), config);
        scm.setName("noName");
        SCMs scms = new SCMs(scm);
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setSCMs(scms);
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);

        CRPluggableScmMaterial crPluggableScmMaterial = new CRPluggableScmMaterial("name", "scmid", "directory", filter);
        crPluggableScmMaterial.setPluginConfiguration(new CRPluginConfiguration("plugin_id", "1.0"));
        crPluggableScmMaterial.getConfiguration().add(new CRConfigurationProperty("url", "url"));

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig =
                (PluggableSCMMaterialConfig) configConverter.toMaterialConfig(crPluggableScmMaterial, context);

        assertThat(pluggableSCMMaterialConfig.getSCMConfig(), is(scm));
    }

    @Test
    public void shouldConvertPluggableScmMaterialWithANewSCMDefinitionWithoutAnSCMID() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setSCMs(new SCMs());
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);

        CRPluggableScmMaterial crPluggableScmMaterial = new CRPluggableScmMaterial("name", null, "directory", filter);
        crPluggableScmMaterial.setPluginConfiguration(new CRPluginConfiguration("plugin_id", "1.0"));

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig =
                (PluggableSCMMaterialConfig) configConverter.toMaterialConfig(crPluggableScmMaterial, context);
        assertNotNull(pluggableSCMMaterialConfig.getScmId());
    }

    @Test
    public void shouldConvertPluggableScmMaterialWithADuplicateSCMIDShouldUseWhatAlreadyExists() {
        SCM scm = new SCM("scmid", new PluginConfiguration(), new Configuration());
        scm.setName("noName");
        SCMs scms = new SCMs(scm);
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setSCMs(scms);
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);

        CRPluggableScmMaterial crPluggableScmMaterial = new CRPluggableScmMaterial("name", "scmid", "directory", filter);
        crPluggableScmMaterial.setPluginConfiguration(new CRPluginConfiguration("plugin_id", "1.0"));
        crPluggableScmMaterial.getConfiguration().add(new CRConfigurationProperty("url", "url"));

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig =
                (PluggableSCMMaterialConfig) configConverter.toMaterialConfig(crPluggableScmMaterial, context);

        assertThat(pluggableSCMMaterialConfig.getSCMConfig(), is(scm));
    }

    @Test
    public void shouldConvertPluggableScmMaterialWithNewSCMPluginVersionShouldDefaultToEmptyString() {
        SCM s = new SCM("an_id", new PluginConfiguration(), new Configuration());
        s.setName("aname");
        SCMs scms = new SCMs(s);
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setSCMs(scms);
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);
        Configuration config = new Configuration();
        config.addNewConfigurationWithValue("url", "url", false);
        SCM myscm = new SCM("scmid", new PluginConfiguration("plugin_id", ""), config);
        myscm.setName("name");

        CRPluggableScmMaterial crPluggableScmMaterial = new CRPluggableScmMaterial("name", "scmid", "directory", filter);
        crPluggableScmMaterial.setPluginConfiguration(new CRPluginConfiguration("plugin_id", null));
        crPluggableScmMaterial.getConfiguration().add(new CRConfigurationProperty("url", "url"));

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig =
                (PluggableSCMMaterialConfig) configConverter.toMaterialConfig(crPluggableScmMaterial, context);

        assertThat(pluggableSCMMaterialConfig.getName().toLower(), is("name"));
        assertThat(pluggableSCMMaterialConfig.getSCMConfig(), is(myscm));
        assertThat(pluggableSCMMaterialConfig.getScmId(), is("scmid"));
        assertThat(pluggableSCMMaterialConfig.getFolder(), is("directory"));
        assertThat(pluggableSCMMaterialConfig.getFilterAsString(), is("filter"));
    }

    @Test
    public void shouldConvertPackageMaterial() {
        PackageRepositories repositories = new PackageRepositories();
        PackageRepository packageRepository = new PackageRepository();
        PackageDefinition definition = new PackageDefinition("package-id", "n", new Configuration());
        packageRepository.addPackage(definition);
        repositories.add(packageRepository);

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setPackageRepositories(repositories);
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);

        CRPackageMaterial crPackageMaterial = new CRPackageMaterial("name", "package-id");

        PackageMaterialConfig packageMaterialConfig =
                (PackageMaterialConfig) configConverter.toMaterialConfig(crPackageMaterial, context);

        assertThat(packageMaterialConfig.getName().toLower(), is("name"));
        assertThat(packageMaterialConfig.getPackageId(), is("package-id"));
        assertThat(packageMaterialConfig.getPackageDefinition(), is(definition));
    }

    @Test
    public void shouldConvertArtifactConfigWhenDestinationIsNull() {
        BuildArtifactConfig buildArtifactConfig = (BuildArtifactConfig)configConverter.toArtifactConfig(new CRBuiltInArtifact("src", null));
        assertThat(buildArtifactConfig.getDestination(), is(""));
    }

    @Test
    public void shouldConvertTestArtifactConfigWhenDestinationIsNull() {
        TestArtifactConfig testArtifactConfig = (TestArtifactConfig) configConverter.toArtifactConfig(new CRBuiltInArtifact("src", null, CRArtifactType.test));
        assertThat(testArtifactConfig.getDestination(), is("testoutput"));
    }

    @Test
    public void shouldConvertArtifactConfigWhenDestinationIsSet() {
        BuildArtifactConfig buildArtifactConfig = (BuildArtifactConfig) configConverter.toArtifactConfig(new CRBuiltInArtifact("src", "dest"));
        assertThat(buildArtifactConfig.getDestination(), is("dest"));
    }

    @Test
    public void shouldConvertToPluggableArtifactConfigWhenConfigrationIsNotPresent() {
        PluggableArtifactConfig pluggableArtifactConfig = (PluggableArtifactConfig) configConverter.toArtifactConfig(new CRPluggableArtifact("id", "storeId"));

        assertThat(pluggableArtifactConfig.getId(), is("id"));
        assertThat(pluggableArtifactConfig.getStoreId(), is("storeId"));
        assertThat(pluggableArtifactConfig.getConfiguration().isEmpty(), is(true));
    }

    @Test
    public void shouldConvertToPluggableArtifactConfigWithRightConfiguration() {
        PluggableArtifactConfig pluggableArtifactConfig = (PluggableArtifactConfig) configConverter.toArtifactConfig(new CRPluggableArtifact("id", "storeId", new CRConfigurationProperty("filename", "who-cares")));

        assertThat(pluggableArtifactConfig.getId(), is("id"));
        assertThat(pluggableArtifactConfig.getStoreId(), is("storeId"));
        Configuration configuration = pluggableArtifactConfig.getConfiguration();
        assertThat(configuration.size(), is(1));
        assertThat(configuration.get(0).getConfigKeyName(), is("filename"));
        assertThat(configuration.get(0).getConfigValue(), is("who-cares"));
    }

    @Test
    public void shouldConvertJob() {
        CRJob crJob = new CRJob("name", environmentVariables, tabs,
                resources, null, artifacts, artifactPropertiesGenerators,
                false, 5, 120, tasks);

        JobConfig jobConfig = configConverter.toJobConfig(crJob);

        assertThat(jobConfig.name().toLower(), is("name"));
        assertThat(jobConfig.hasVariable("key"), is(true));
        assertThat(jobConfig.getTabs().first().getName(), is("tabname"));
        assertThat(jobConfig.resourceConfigs(), hasItem(new ResourceConfig("resource1")));
        assertThat(jobConfig.artifactConfigs(), hasItem(new BuildArtifactConfig("src", "dest")));
        assertThat(jobConfig.getProperties(), hasItem(new ArtifactPropertyConfig("name", "src", "path")));
        assertThat(jobConfig.isRunOnAllAgents(), is(false));
        assertThat(jobConfig.getRunInstanceCount(), is("5"));
        assertThat(jobConfig.getTimeout(), is("120"));
        assertThat(jobConfig.getTasks().size(), is(1));
    }

    @Test
    public void shouldConvertJobWhenHasElasticProfileId() {
        CRJob crJob = new CRJob("name", environmentVariables, tabs,
                null, "myprofile", artifacts, artifactPropertiesGenerators,
                false, 5, 120, tasks);

        JobConfig jobConfig = configConverter.toJobConfig(crJob);

        assertThat(jobConfig.getElasticProfileId(), is("myprofile"));
        assertThat(jobConfig.resourceConfigs().size(), is(0));
    }

    @Test
    public void shouldConvertJobWhenRunInstanceCountIsNotSpecified() {
        CRJob crJob = new CRJob("name", environmentVariables, tabs,
                resources, null, artifacts, artifactPropertiesGenerators,
                null, 120, tasks);

        JobConfig jobConfig = configConverter.toJobConfig(crJob);

        assertThat(jobConfig.isRunOnAllAgents(), is(false));
        assertNull(jobConfig.getRunInstanceCount());
        assertThat(jobConfig.getTimeout(), is("120"));
        assertThat(jobConfig.getTasks().size(), is(1));
    }

    @Test
    public void shouldConvertJobWhenRunInstanceCountIsAll() {
        CRJob crJob = new CRJob("name", environmentVariables, tabs,
                resources, null, artifacts, artifactPropertiesGenerators,
                "all", 120, tasks);

        JobConfig jobConfig = configConverter.toJobConfig(crJob);

        assertNull(jobConfig.getRunInstanceCount());
        assertThat(jobConfig.isRunOnAllAgents(), is(true));
        assertThat(jobConfig.getTimeout(), is("120"));
        assertThat(jobConfig.getTasks().size(), is(1));
    }

    @Test
    public void shouldConvertApprovalWhenManualAndAuth() {
        CRApproval crApproval = new CRApproval(CRApprovalCondition.manual, authorizedRoles, authorizedUsers);

        Approval approval = configConverter.toApproval(crApproval);
        assertThat(approval.isManual(), is(true));
        assertThat(approval.isAuthorizationDefined(), is(true));
        assertThat(approval.getAuthConfig().getRoles(), hasItem(new AdminRole(new CaseInsensitiveString("authRole"))));
        assertThat(approval.getAuthConfig().getUsers(), hasItem(new AdminUser(new CaseInsensitiveString("authUser"))));
    }

    @Test
    public void shouldConvertApprovalWhenManualAndNoAuth() {
        CRApproval crApproval = new CRApproval(CRApprovalCondition.manual, new ArrayList<>(), new ArrayList<>());

        Approval approval = configConverter.toApproval(crApproval);
        assertThat(approval.isManual(), is(true));
        assertThat(approval.isAuthorizationDefined(), is(false));
    }

    @Test
    public void shouldConvertApprovalWhenSuccess() {
        CRApproval crApproval = new CRApproval(CRApprovalCondition.success, new ArrayList<>(), new ArrayList<>());

        Approval approval = configConverter.toApproval(crApproval);
        assertThat(approval.isManual(), is(false));
        assertThat(approval.isAuthorizationDefined(), is(false));
    }

    @Test
    public void shouldConvertStage() {
        CRApproval approval = new CRApproval(CRApprovalCondition.manual, authorizedRoles, authorizedUsers);

        CRStage crStage = new CRStage("stageName", true, true, true, approval, environmentVariables, jobs);

        StageConfig stageConfig = configConverter.toStage(crStage);

        assertThat(stageConfig.name().toLower(), is("stagename"));
        assertThat(stageConfig.isFetchMaterials(), is(true));
        assertThat(stageConfig.isCleanWorkingDir(), is(true));
        assertThat(stageConfig.isArtifactCleanupProhibited(), is(true));
        assertThat(stageConfig.getVariables().hasVariable("key"), is(true));
        assertThat(stageConfig.getJobs().size(), is(1));
    }

    @Test
    public void shouldConvertPipeline() {
        CRPipeline crPipeline = new CRPipeline("pipename", "group1", "label", LOCK_VALUE_LOCK_ON_FAILURE,
                trackingTool, null, timer, environmentVariables, materials, stages, null, parameters);

        PipelineConfig pipelineConfig = configConverter.toPipelineConfig(crPipeline, context);
        assertThat(pipelineConfig.name().toLower(), is("pipename"));
        assertThat(pipelineConfig.materialConfigs().first() instanceof GitMaterialConfig, is(true));
        assertThat(pipelineConfig.first().name().toLower(), is("stagename"));
        assertThat(pipelineConfig.getVariables().hasVariable("key"), is(true));
        assertThat(pipelineConfig.trackingTool().getLink(), is("link"));
        assertThat(pipelineConfig.getTimer().getTimerSpec(), is("timer"));
        assertThat(pipelineConfig.getLabelTemplate(), is("label"));
        assertThat(pipelineConfig.isLockableOnFailure(), is(true));
    }

    @Test
    public void shouldConvertMinimalPipeline() {
        CRPipeline crPipeline = new CRPipeline();
        crPipeline.setName("p1");
        List<CRStage> min_stages = new ArrayList<>();
        CRStage min_stage = new CRStage();
        min_stage.setName("build");
        Collection<CRJob> min_jobs = new ArrayList<>();
        CRJob job = new CRJob();
        job.setName("buildjob");
        List<CRTask> min_tasks = new ArrayList<>();
        min_tasks.add(new CRBuildTask("rake"));
        job.setTasks(min_tasks);
        min_jobs.add(job);
        min_stage.setJobs(min_jobs);
        min_stages.add(min_stage);
        crPipeline.setStages(min_stages);
        Collection<CRMaterial> min_materials = new ArrayList<>();
        CRSvnMaterial crSvnMaterial = new CRSvnMaterial();
        crSvnMaterial.setUrl("url");
        min_materials.add(crSvnMaterial);
        crPipeline.setMaterials(min_materials);

        PipelineConfig pipelineConfig = configConverter.toPipelineConfig(crPipeline, context);
        assertThat(pipelineConfig.name().toLower(), is("p1"));
        assertThat(pipelineConfig.materialConfigs().first() instanceof SvnMaterialConfig, is(true));
        assertThat(pipelineConfig.first().name().toLower(), is("build"));
        ;
        assertThat(pipelineConfig.getLabelTemplate(), is(PipelineLabel.COUNT_TEMPLATE));
    }

    @Test
    public void shouldConvertPipelineGroup() {
        List<CRPipeline> pipelines = new ArrayList<>();
        pipelines.add(new CRPipeline("pipename", "group", "label", LOCK_VALUE_LOCK_ON_FAILURE,
                trackingTool, null, timer, environmentVariables, materials, stages, null, parameters));
        Map<String, List<CRPipeline>> map = new HashMap<>();
        map.put("group", pipelines);
        Map.Entry<String, List<CRPipeline>> crPipelineGroup = map.entrySet().iterator().next();
        PipelineConfigs pipelineConfigs = configConverter.toBasicPipelineConfigs(crPipelineGroup, context);
        assertThat(pipelineConfigs.getGroup(), is("group"));
        assertThat(pipelineConfigs.getPipelines().size(), is(1));
    }

    @Test
    public void shouldConvertPipelineGroupWhenNoName() {
        List<CRPipeline> pipelines = new ArrayList<>();
        pipelines.add(new CRPipeline("pipename", null, "label", LOCK_VALUE_LOCK_ON_FAILURE,
                trackingTool, null, timer, environmentVariables, materials, stages, null, parameters));
        Map<String, List<CRPipeline>> map = new HashMap<>();
        map.put(null, pipelines);
        Map.Entry<String, List<CRPipeline>> crPipelineGroup = map.entrySet().iterator().next();
        PipelineConfigs pipelineConfigs = configConverter.toBasicPipelineConfigs(crPipelineGroup, context);
        assertThat(pipelineConfigs.getGroup(), is(PipelineConfigs.DEFAULT_GROUP));
        assertThat(pipelineConfigs.getPipelines().size(), is(1));
    }

    @Test
    public void shouldConvertPipelineGroupWhenEmptyName() {
        List<CRPipeline> pipelines = new ArrayList<>();
        pipelines.add(new CRPipeline("pipename", "", "label", LOCK_VALUE_LOCK_ON_FAILURE,
                trackingTool, null, timer, environmentVariables, materials, stages, null, parameters));
        Map<String, List<CRPipeline>> map = new HashMap<>();
        map.put("", pipelines);
        Map.Entry<String, List<CRPipeline>> crPipelineGroup = map.entrySet().iterator().next();
        PipelineConfigs pipelineConfigs = configConverter.toBasicPipelineConfigs(crPipelineGroup, context);
        assertThat(pipelineConfigs.getGroup(), is(PipelineConfigs.DEFAULT_GROUP));
        assertThat(pipelineConfigs.getPipelines().size(), is(1));
    }

    @Test
    public void shouldConvertPartialConfigWithGroupsAndEnvironments() {
        CRPipeline pipeline = new CRPipeline("pipename", "group", "label", LOCK_VALUE_LOCK_ON_FAILURE,
                trackingTool, null, timer, environmentVariables, materials, stages, null, parameters);
        ArrayList<String> agents = new ArrayList<>();
        agents.add("12");
        ArrayList<String> pipelineNames = new ArrayList<>();
        pipelineNames.add("pipename");
        CREnvironment crEnvironment = new CREnvironment("dev", environmentVariables, agents, pipelineNames);

        CRParseResult crPartialConfig = new CRParseResult();
        crPartialConfig.getEnvironments().add(crEnvironment);

        crPartialConfig.getPipelines().add(pipeline);

        PartialConfig partialConfig = configConverter.toPartialConfig(crPartialConfig, context);
        assertThat(partialConfig.getGroups().size(), is(1));
        assertThat(partialConfig.getEnvironments().size(), is(1));
    }

    @Test
    public void shouldConvertCRTimerWhenAllAssigned() {
        CRTimer timer = new CRTimer("0 15 * * 6", true);
        TimerConfig result = configConverter.toTimerConfig(timer);
        assertThat(result.getTimerSpec(), is("0 15 * * 6"));
        assertThat(result.getOnlyOnChanges(), is(true));
    }

    @Test
    public void shouldConvertCRTimerWhenNullOnChanges() {
        CRTimer timer = new CRTimer("0 15 * * 6", null);
        TimerConfig result = configConverter.toTimerConfig(timer);
        assertThat(result.getTimerSpec(), is("0 15 * * 6"));
        assertThat(result.getOnlyOnChanges(), is(false));
    }

    @Test
    public void shouldFailConvertCRTimerWhenNullSpec() {
        CRTimer timer = new CRTimer(null, false);
        try {
            configConverter.toTimerConfig(timer);
            fail("should have thrown");
        } catch (Exception ex) {
            //ok
        }
    }

    @Test
    public void shouldConvertParametersWhenPassed() throws Exception {
        Collection<CRParameter> parameters = new ArrayList<>();
        parameters.add(new CRParameter("param", "value"));
        CRPipeline crPipeline = new CRPipeline("p1", "g1", "template", LOCK_VALUE_LOCK_ON_FAILURE, null, null, null, environmentVariables, materials, null, "t1", parameters);
        PipelineConfig pipeline = configConverter.toPipelineConfig(crPipeline, context);
        assertThat(pipeline.getParams(), is(new ParamsConfig(new ParamConfig("param", "value"))));
    }

    @Test
    public void shouldConvertTemplateNameWhenGiven() throws Exception {
        CRPipeline crPipeline = new CRPipeline("p1", "g1", "template", LOCK_VALUE_LOCK_ON_FAILURE, null, null, null, environmentVariables, materials, null, "t1", parameters);
        PipelineConfig pipeline = configConverter.toPipelineConfig(crPipeline, context);
        assertThat(pipeline.isEmpty(), is(true));
        assertThat(pipeline.getTemplateName(), is(new CaseInsensitiveString("t1")));
    }

    @Test
    public void shouldConvertParamConfigWhenPassed() throws Exception {
        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("p1");
        pipeline.addParam(new ParamConfig("param", "value"));

        Collection<CRParameter> parameters = new ArrayList<>();
        parameters.add(new CRParameter("param", "value"));
        CRPipeline crPipeline = configConverter.pipelineConfigToCRPipeline(pipeline, "group");
        assertThat(crPipeline.getParameters(), is(parameters));
    }

    @Test
    public void shouldConvertPipelineConfigToCRPipeline() {
        TrackingTool trackingTool = new TrackingTool();
        trackingTool.setLink("link");
        TimerConfig timerConfig = new TimerConfig("timer", true);
        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("p1");
        pipeline.setTimer(timerConfig);
        pipeline.setTrackingTool(trackingTool);
        pipeline.addEnvironmentVariable("testing", "123");

        StageConfig stage = new StageConfig();
        stage.setName(new CaseInsensitiveString("build"));

        JobConfig job = new JobConfig();
        job.setName("buildjob");
        job.setTasks(new Tasks(new RakeTask()));

        stage.setJobs(new JobConfigs(job));
        pipeline.addStageWithoutValidityAssertion(stage);

        SvnMaterialConfig mat = new SvnMaterialConfig();
        mat.setName(new CaseInsensitiveString("mat"));
        mat.setUrl("url");
        pipeline.addMaterialConfig(mat);

        CRPipeline crPipeline = configConverter.pipelineConfigToCRPipeline(pipeline, "group1");
        assertThat(crPipeline.getName(), is("p1"));
        assertThat(crPipeline.getGroupName(), is("group1"));
        assertThat(crPipeline.getMaterialByName("mat") instanceof CRSvnMaterial, is(true));
        assertThat(crPipeline.getLabelTemplate(), is(PipelineLabel.COUNT_TEMPLATE));
        assertThat(crPipeline.getMaterials().size(), is(1));
        assertThat(crPipeline.hasEnvironmentVariable("testing"), is(true));
        assertThat(crPipeline.getTrackingTool().getLink(), is("link"));
        assertThat(crPipeline.getTimer().getTimerSpec(), is("timer"));
        assertThat(crPipeline.getStages().get(0).getName(), is("build"));
        assertThat(crPipeline.getStages().get(0).getJobs().size(), is(1));
        assertNull(crPipeline.getMingle());
    }

    @Test
    public void shouldConvertStageConfigToCRStage() {
        EnvironmentVariablesConfig envVars = new EnvironmentVariablesConfig();
        envVars.add("testing", "123");

        JobConfig job = new JobConfig();
        job.setName("buildjob");
        job.setTasks(new Tasks(new RakeTask()));

        AdminRole role = new AdminRole(new CaseInsensitiveString("a_role"));
        AdminUser user = new AdminUser(new CaseInsensitiveString("a_user"));
        Approval approval = new Approval();
        approval.addAdmin(user, role);

        StageConfig stage = new StageConfig(new CaseInsensitiveString("stageName"), new JobConfigs(), approval);
        stage.setVariables(envVars);
        stage.setJobs(new JobConfigs(job));
        stage.setCleanWorkingDir(true);
        stage.setArtifactCleanupProhibited(true);

        CRStage crStage = configConverter.stageToCRStage(stage);

        assertThat(crStage.getName(), is("stageName"));
        assertThat(crStage.getApproval().getAuthorizedRoles(), hasItem("a_role"));
        assertThat(crStage.getApproval().getAuthorizedUsers(), hasItem("a_user"));
        assertThat(crStage.isFetchMaterials(), is(true));
        assertThat(crStage.isCleanWorkingDir(), is(true));
        assertThat(crStage.isArtifactCleanupProhibited(), is(true));
        assertThat(crStage.hasEnvironmentVariable("testing"), is(true));
        assertThat(crStage.getJobs().size(), is(1));
    }

    @Test
    public void shouldConvertJobConfigToCRJob() {
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("name"),
        new ResourceConfigs(new ResourceConfig("resource1")),
        new ArtifactConfigs(new BuildArtifactConfig("src", "dest")));
        jobConfig.setRunOnAllAgents(false);
        jobConfig.setTimeout("120");
        jobConfig.addTask(new ExecTask());
        jobConfig.addVariable("key", "value");
        jobConfig.setRunInstanceCount("5");
        jobConfig.addTab("tabname", "path");
        jobConfig.setProperties(new ArtifactPropertiesConfig(new ArtifactPropertyConfig("name", "src", "path")));

        CRJob job = configConverter.jobToCRJob(jobConfig);

        assertThat(job.getName(), is("name"));
        assertThat(job.hasEnvironmentVariable("key"), is(true));
        assertThat(job.getTabs().contains(new CRTab("tabname", "path")), is(true));
        assertThat(job.getResources().contains("resource1"),  is(true));
        assertThat(job.getArtifacts().contains(new CRBuiltInArtifact("src", "dest")), is(true));
        assertThat(job.getArtifactPropertiesGenerators().contains(new CRPropertyGenerator("name", "src", "path")), is(true));
        assertThat(job.isRunOnAllAgents(), is(false));
        assertThat(job.getRunInstanceCount(), is(5));
        assertThat(job.getTimeout(), is(120));
        assertThat(job.getTasks().size(), is(1));
    }

    @Test
    public void shouldConvertJobConfigToCRJobWithNullTimeouBeingZerot() {
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("name"),
                new ResourceConfigs(new ResourceConfig("resource1")),
                new ArtifactConfigs(new BuildArtifactConfig("src", "dest")));
        jobConfig.setRunOnAllAgents(false);
        jobConfig.addTask(new ExecTask());

        CRJob job = configConverter.jobToCRJob(jobConfig);

        assertThat(job.getTimeout(), is(0));
    }

    @Test
    public void shouldConvertDependencyMaterialConfigToCRDependencyMaterial() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("name"), new CaseInsensitiveString("pipe"), new CaseInsensitiveString("stage"));

        CRDependencyMaterial crDependencyMaterial =
                (CRDependencyMaterial) configConverter.materialToCRMaterial(dependencyMaterialConfig);

        assertThat(crDependencyMaterial.getName(), is("name"));
        assertThat(crDependencyMaterial.getPipelineName(), is("pipe"));
        assertThat(crDependencyMaterial.getStageName(), is("stage"));
    }

    @Test
    public void shouldConvertGitMaterialConfigToCRGitMaterial() {
        GitMaterialConfig gitMaterialConfig = new GitMaterialConfig("url", "branch", true);
        gitMaterialConfig.setName(new CaseInsensitiveString("name"));
        gitMaterialConfig.setFolder("folder");
        gitMaterialConfig.setAutoUpdate(true);
        gitMaterialConfig.setInvertFilter(false);
        gitMaterialConfig.setFilter(Filter.create("filter"));

        CRGitMaterial crGitMaterial =
                (CRGitMaterial) configConverter.materialToCRMaterial(gitMaterialConfig);

        assertThat(crGitMaterial.getName(), is("name"));
        assertThat(crGitMaterial.getDirectory(), is("folder"));
        assertThat(crGitMaterial.isAutoUpdate(), is(true));
        assertThat(crGitMaterial.isWhitelist(), is(false));
        assertThat(crGitMaterial.getFilterList(), hasItem("filter"));
        assertThat(crGitMaterial.getUrl(), is("url"));
        assertThat(crGitMaterial.getBranch(), is("branch"));
        assertThat(crGitMaterial.shallowClone(), is(true));
    }

    @Test
    public void shouldConvertGitMaterialConfigWhenNulls() {
        GitMaterialConfig gitMaterialConfig = new GitMaterialConfig();
        gitMaterialConfig.setUrl("url");

        CRGitMaterial crGitMaterial =
                (CRGitMaterial) configConverter.materialToCRMaterial(gitMaterialConfig);

        assertNull(crGitMaterial.getName());
        assertNull(crGitMaterial.getDirectory());
        assertThat(crGitMaterial.isAutoUpdate(), is(true));
        assertThat(crGitMaterial.shallowClone(), is(false));
        assertThat(crGitMaterial.getUrl(), is("url"));
        assertThat(crGitMaterial.getBranch(), is("master"));
    }

    @Test
    public void shouldConvertHgMaterialConfigToCRHgMaterial() {
        HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("url", "folder");
        hgMaterialConfig.setName(new CaseInsensitiveString("name"));
        hgMaterialConfig.setFilter(Filter.create("filter"));
        hgMaterialConfig.setAutoUpdate(true);

        CRHgMaterial crHgMaterial =
                (CRHgMaterial) configConverter.materialToCRMaterial(hgMaterialConfig);

        assertThat(crHgMaterial.getName(), is("name"));
        assertThat(crHgMaterial.getDirectory(), is("folder"));
        assertThat(crHgMaterial.isAutoUpdate(), is(true));
        assertThat(crHgMaterial.getFilterList(), hasItem("filter"));
        assertThat(crHgMaterial.getUrl(), is("url"));
    }

    @Test
    public void shouldConvertHgMaterialConfigWhenNullName() {
        HgMaterialConfig hgMaterialConfig = new HgMaterialConfig("url", "folder");
        hgMaterialConfig.setFilter(Filter.create("filter"));
        hgMaterialConfig.setAutoUpdate(true);

        CRHgMaterial crHgMaterial =
                (CRHgMaterial) configConverter.materialToCRMaterial(hgMaterialConfig);

        assertNull(crHgMaterial.getName());
        assertThat(crHgMaterial.getDirectory(), is("folder"));
        assertThat(crHgMaterial.isAutoUpdate(), is(true));
        assertThat(crHgMaterial.getFilterList(), hasItem("filter"));
        assertThat(crHgMaterial.getUrl(), is("url"));
    }

    @Test
    public void shouldConvertP4MaterialConfigWhenEncryptedPassword() {
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig("server:port", "view");
        p4MaterialConfig.setName(new CaseInsensitiveString("name"));
        p4MaterialConfig.setFolder("folder");
        p4MaterialConfig.setEncryptedPassword("encryptedvalue");
        p4MaterialConfig.setFilter(Filter.create("filter"));
        p4MaterialConfig.setUserName("user");
        p4MaterialConfig.setUseTickets(true);
        p4MaterialConfig.setAutoUpdate(false);

        CRP4Material crp4Material =
                (CRP4Material) configConverter.materialToCRMaterial(p4MaterialConfig);

        assertThat(crp4Material.getName(), is("name"));
        assertThat(crp4Material.getDirectory(), is("folder"));
        assertThat(crp4Material.isAutoUpdate(), is(false));
        assertThat(crp4Material.getFilterList(), hasItem("filter"));
        assertThat(crp4Material.getServerAndPort(), is("server:port"));
        assertThat(crp4Material.getUserName(), is("user"));
        assertThat(crp4Material.getEncryptedPassword(), is("encryptedvalue"));
        assertNull(crp4Material.getPassword());
        assertThat(crp4Material.getUseTickets(), is(true));
        assertThat(crp4Material.getView(), is("view"));
    }

    @Test
    public void shouldConvertP4MaterialConfigWhenPlainPassword() {
        P4MaterialConfig p4MaterialConfig = new P4MaterialConfig("server:port", "view");
        p4MaterialConfig.setName(new CaseInsensitiveString("name"));
        p4MaterialConfig.setFolder("folder");
        p4MaterialConfig.setPassword("password");
        p4MaterialConfig.setFilter(Filter.create("filter"));
        p4MaterialConfig.setUserName("user");
        p4MaterialConfig.setUseTickets(false);
        p4MaterialConfig.setAutoUpdate(false);

        CRP4Material crp4Material =
                (CRP4Material) configConverter.materialToCRMaterial(p4MaterialConfig);

        assertThat(crp4Material.getName(), is("name"));
        assertThat(crp4Material.getDirectory(), is("folder"));
        assertThat(crp4Material.isAutoUpdate(), is(false));
        assertThat(crp4Material.getFilterList(), hasItem("filter"));
        assertThat(crp4Material.getServerAndPort(), is("server:port"));
        assertThat(crp4Material.getUserName(), is("user"));
        assertThat(crp4Material.getUseTickets(), is(false));
        assertThat(crp4Material.getView(), is("view"));
    }

    @Test
    public void shouldConvertSvmMaterialConfigWhenEncryptedPassword() {
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig("url", true);
        svnMaterialConfig.setName(new CaseInsensitiveString("name"));
        svnMaterialConfig.setEncryptedPassword("encryptedvalue");
        svnMaterialConfig.setFolder("folder");
        svnMaterialConfig.setFilter(Filter.create("filter"));
        svnMaterialConfig.setUserName("username");

        CRSvnMaterial crSvnMaterial =
                (CRSvnMaterial) configConverter.materialToCRMaterial(svnMaterialConfig);

        assertThat(crSvnMaterial.getName(), is("name"));
        assertThat(crSvnMaterial.getDirectory(), is("folder"));
        assertThat(crSvnMaterial.isAutoUpdate(), is(true));
        assertThat(crSvnMaterial.getFilterList(), hasItem("filter"));
        assertThat(crSvnMaterial.getUrl(), is("url"));
        assertThat(crSvnMaterial.getUserName(), is("username"));
        assertThat(crSvnMaterial.getEncryptedPassword(), is("encryptedvalue"));
        assertNull(crSvnMaterial.getPassword());
        assertThat(crSvnMaterial.isCheckExternals(), is(true));
    }

    @Test
    public void shouldConvertSvmMaterialConfigWhenPlainPassword() {
        SvnMaterialConfig svnMaterialConfig = new SvnMaterialConfig("url", true);
        svnMaterialConfig.setName(new CaseInsensitiveString("name"));
        svnMaterialConfig.setPassword("pass");
        svnMaterialConfig.setFolder("folder");
        svnMaterialConfig.setFilter(Filter.create("filter"));
        svnMaterialConfig.setUserName("username");

        CRSvnMaterial crSvnMaterial =
                (CRSvnMaterial) configConverter.materialToCRMaterial(svnMaterialConfig);

        assertThat(crSvnMaterial.getName(), is("name"));
        assertThat(crSvnMaterial.getDirectory(), is("folder"));
        assertThat(crSvnMaterial.isAutoUpdate(), is(true));
        assertThat(crSvnMaterial.getFilterList(), hasItem("filter"));
        assertThat(crSvnMaterial.getUrl(), is("url"));
        assertThat(crSvnMaterial.getUserName(), is("username"));
        assertNull(crSvnMaterial.getPassword());
        assertThat(crSvnMaterial.isCheckExternals(), is(true));
    }

    @Test
    public void shouldConvertTfsMaterialConfigWhenPlainPassword() {
        TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig();
        tfsMaterialConfig.setUrl("url");
        tfsMaterialConfig.setDomain("domain");
        tfsMaterialConfig.setProjectPath("project");
        tfsMaterialConfig.setName(new CaseInsensitiveString("name"));
        tfsMaterialConfig.setPassword("pass");
        tfsMaterialConfig.setFolder("folder");
        tfsMaterialConfig.setAutoUpdate(false);
        tfsMaterialConfig.setFilter(Filter.create("filter"));
        tfsMaterialConfig.setUserName("user");

        CRTfsMaterial crTfsMaterial =
                (CRTfsMaterial) configConverter.materialToCRMaterial(tfsMaterialConfig);

        assertThat(crTfsMaterial.getName(), is("name"));
        assertThat(crTfsMaterial.getDirectory(), is("folder"));
        assertThat(crTfsMaterial.isAutoUpdate(), is(false));
        assertThat(crTfsMaterial.getFilterList(), hasItem("filter"));
        assertThat(crTfsMaterial.getUrl(), is("url"));
        assertThat(crTfsMaterial.getUserName(), is("user"));
        assertNull(crTfsMaterial.getPassword());
        assertThat(crTfsMaterial.getDomain(), is("domain"));
        assertThat(crTfsMaterial.getProjectPath(), is("project"));
    }

    @Test
    public void shouldConvertTfsMaterialConfigWhenEncryptedPassword() {
        TfsMaterialConfig tfsMaterialConfig = new TfsMaterialConfig();
        tfsMaterialConfig.setUrl("url");
        tfsMaterialConfig.setDomain("domain");
        tfsMaterialConfig.setProjectPath("project");
        tfsMaterialConfig.setName(new CaseInsensitiveString("name"));
        tfsMaterialConfig.setEncryptedPassword("encryptedvalue");
        tfsMaterialConfig.setFolder("folder");
        tfsMaterialConfig.setAutoUpdate(false);
        tfsMaterialConfig.setFilter(Filter.create("filter"));
        tfsMaterialConfig.setUserName("user");

        CRTfsMaterial crTfsMaterial =
                (CRTfsMaterial) configConverter.materialToCRMaterial(tfsMaterialConfig);

        assertThat(crTfsMaterial.getName(), is("name"));
        assertThat(crTfsMaterial.getDirectory(), is("folder"));
        assertThat(crTfsMaterial.isAutoUpdate(), is(false));
        assertThat(crTfsMaterial.getFilterList(), hasItem("filter"));
        assertThat(crTfsMaterial.getUrl(), is("url"));
        assertThat(crTfsMaterial.getUserName(), is("user"));
        assertThat(crTfsMaterial.getEncryptedPassword(), is("encryptedvalue"));
        assertNull(crTfsMaterial.getPassword());
        assertThat(crTfsMaterial.getDomain(), is("domain"));
        assertThat(crTfsMaterial.getProjectPath(), is("project"));
    }

    @Test
    public void shouldConvertPluggableScmMaterialConfig() {
        SCM myscm = new SCM("scmid", new PluginConfiguration(), new Configuration());
        SCMs scms = new SCMs(myscm);

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setSCMs(scms);
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(new CaseInsensitiveString("name"), myscm, "directory",  Filter.create("filter"));

        CRPluggableScmMaterial crPluggableScmMaterial =
                (CRPluggableScmMaterial) configConverter.materialToCRMaterial(pluggableSCMMaterialConfig);

        assertThat(crPluggableScmMaterial.getName(), is("name"));
        assertThat(crPluggableScmMaterial.getScmId(), is("scmid"));
        assertThat(crPluggableScmMaterial.getDirectory(), is("directory"));
        assertThat(crPluggableScmMaterial.getFilterList(), hasItem("filter"));
    }

    @Test
    public void shouldConvertPackageMaterialConfig() {
        PackageRepositories repositories = new PackageRepositories();
        PackageRepository packageRepository = new PackageRepository();
        PackageDefinition definition = new PackageDefinition("package-id", "n", new Configuration());
        packageRepository.addPackage(definition);
        repositories.add(packageRepository);

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setPackageRepositories(repositories);
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);

        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig(new CaseInsensitiveString("name"), "package-id", definition);

        CRPackageMaterial crPackageMaterial =
                (CRPackageMaterial) configConverter.materialToCRMaterial(packageMaterialConfig);

        assertThat(crPackageMaterial.getName(), is("name"));
        assertThat(crPackageMaterial.getPackageId(), is("package-id"));
    }

    @Test
    public void shouldConvertEnvironmentVariableConfigWhenNotSecure() {
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig("key1", "value");
        CREnvironmentVariable result = configConverter.environmentVariableConfigToCREnvironmentVariable(environmentVariableConfig);
        assertThat(result.getValue(), is("value"));
        assertThat(result.getName(), is("key1"));
        assertThat(result.hasEncryptedValue(), is(false));
    }

    @Test
    public void shouldConvertNullEnvironmentVariableConfigWhenNotSecure() {
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig("key1", null);
        CREnvironmentVariable result = configConverter.environmentVariableConfigToCREnvironmentVariable(environmentVariableConfig);
        assertThat(result.getValue(), is(""));
        assertThat(result.getName(), is("key1"));
        assertThat(result.hasEncryptedValue(), is(false));
    }

    @Test
    public void shouldConvertEnvironmentVariableConfigWhenSecure() {
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig("key1", null);
        environmentVariableConfig.setIsSecure(true);
        environmentVariableConfig.setEncryptedValue("encryptedvalue");
        CREnvironmentVariable result = configConverter.environmentVariableConfigToCREnvironmentVariable(environmentVariableConfig);
        assertThat(result.hasEncryptedValue(), is(true));
        assertThat(result.getEncryptedValue(), is("encryptedvalue"));
        assertNull(result.getValue());
        assertThat(result.getName(), is("key1"));
    }

    @Test
    public void shouldMigratePluggableTasktoCR() {
        ArrayList<CRConfigurationProperty> configs = new ArrayList<>();
        configs.add(new CRConfigurationProperty("k", "m", null));

        ConfigurationProperty prop = new ConfigurationProperty(new ConfigurationKey("k"), new ConfigurationValue("m"));
        Configuration config = new Configuration(prop);

        PluggableTask pluggableTask = new PluggableTask(
                new PluginConfiguration("myplugin", "1"),
                config);
        pluggableTask.setConditions(new RunIfConfigs(RunIfConfig.ANY));

        CRPluggableTask result = (CRPluggableTask) configConverter.taskToCRTask(pluggableTask);

        assertThat(result.getPluginConfiguration().getId(), is("myplugin"));
        assertThat(result.getPluginConfiguration().getVersion(), is("1"));
        assertThat(result.getConfiguration(), hasItem(new CRConfigurationProperty("k", "m", null)));
        assertThat(result.getRunIf(), is(CRRunIf.any));
    }

    @Test
    public void shouldMigrateRakeTaskToCR() {
        RakeTask rakeTask = new RakeTask();
        rakeTask.setBuildFile("Rakefile.rb");
        rakeTask.setWorkingDirectory("src");
        rakeTask.setTarget("build");
        rakeTask.setConditions(new RunIfConfigs(RunIfConfig.FAILED));

        CRBuildTask result = (CRBuildTask) configConverter.taskToCRTask(rakeTask);

        assertThat(result.getType(), is(CRBuildFramework.rake));
        assertThat(result.getBuildFile(), is("Rakefile.rb"));
        assertThat(result.getTarget(), is("build"));
        assertThat(result.getWorkingDirectory(), is("src"));
        assertThat(result.getRunIf(), is(CRRunIf.failed));
    }

    @Test
    public void shouldMigrateAntTaskToCR() {
        RakeTask rakeTask = new RakeTask();
        rakeTask.setBuildFile("Rakefile.rb");
        rakeTask.setWorkingDirectory("src");
        rakeTask.setTarget("build");
        rakeTask.setConditions(new RunIfConfigs(RunIfConfig.FAILED));

        AntTask antTask = new AntTask();
        antTask.setBuildFile("ant");
        antTask.setWorkingDirectory("src");
        antTask.setTarget("build");
        antTask.setOnCancelConfig(new OnCancelConfig(rakeTask));
        antTask.setConditions(new RunIfConfigs(RunIfConfig.FAILED));

        CRBuildTask result = (CRBuildTask) configConverter.taskToCRTask(antTask);
        CRBuildTask onCancel = (CRBuildTask) result.getOnCancel();

        assertThat(result.getRunIf(), is(CRRunIf.failed));
        assertThat(result.getBuildFile(), is("ant"));
        assertThat(result.getTarget(), is("build"));
        assertThat(result.getWorkingDirectory(), is("src"));
        assertThat(onCancel.getType(), is(CRBuildFramework.rake));
        assertThat(onCancel.getBuildFile(), is("Rakefile.rb"));
        assertThat(onCancel.getTarget(), is("build"));
        assertThat(onCancel.getWorkingDirectory(), is("src"));
        assertThat(onCancel.getRunIf(), is(CRRunIf.failed));
    }

    @Test
    public void shouldMigrateNantTaskToCR() {
        NantTask nantTask = new NantTask();
        nantTask.setBuildFile("nant");
        nantTask.setWorkingDirectory("src");
        nantTask.setTarget("build");
        nantTask.setNantPath("path");
        nantTask.setConditions(new RunIfConfigs(RunIfConfig.PASSED));

        CRNantTask result = (CRNantTask) configConverter.taskToCRTask(nantTask);

        assertThat(result.getRunIf(), is(CRRunIf.passed));
        assertThat(result.getBuildFile(), is("nant"));
        assertThat(result.getTarget(), is("build"));
        assertThat(result.getWorkingDirectory(), is("src"));
        assertThat(result.getNantPath(), is("path"));
    }

    @Test
    public void shouldConvertExecTaskWhenCancelIsNotSpecifiedToCR() {
        ExecTask execTask = new ExecTask("bash",
                new Arguments(new Argument("1"), new Argument("2")),
                "work");
        execTask.setConditions(new RunIfConfigs(RunIfConfig.FAILED));
        execTask.setTimeout(120L);
        CRExecTask result = (CRExecTask) configConverter.taskToCRTask(execTask);

        assertThat(result.getRunIf(), is(CRRunIf.failed));
        assertThat(result.getCommand(), is("bash"));
        assertThat(result.getArgs(), hasItem("1"));
        assertThat(result.getArgs(), hasItem("2"));
        assertThat(result.getWorkingDirectory(), is("work"));
        assertThat(result.getTimeout(), is(120L));
        assertNull(result.getOnCancel());
    }

    @Test
    public void shouldConvertExecTaskWhenCancelIsSpecifiedToCR() {
        ExecTask onCancel = new ExecTask();
        onCancel.setCommand("kill");
        ExecTask execTask = new ExecTask("bash",
                new Arguments(new Argument("1"), new Argument("2")),
                "work");
        execTask.setOnCancelConfig(new OnCancelConfig(onCancel));
        execTask.setConditions(new RunIfConfigs(RunIfConfig.FAILED));
        execTask.setTimeout(120L);

        CRExecTask result = (CRExecTask) configConverter.taskToCRTask(execTask);
        CRExecTask crOnCancel = (CRExecTask) result.getOnCancel();

        assertThat(result.getRunIf(), is(CRRunIf.failed));
        assertThat(result.getCommand(), is("bash"));
        assertThat(result.getArgs(), hasItem("1"));
        assertThat(result.getArgs(), hasItem("2"));
        assertThat(result.getWorkingDirectory(), is("work"));
        assertThat(result.getTimeout(), is(120L));
        assertThat(crOnCancel.getCommand(), is("kill"));
    }

    @Test
    public void shouldConvertFetchArtifactTaskWhenSourceIsDirectoryToCR() {
        FetchTask fetchTask = new FetchTask(
               new CaseInsensitiveString("upstream"),
               new CaseInsensitiveString("stage"),
               new CaseInsensitiveString("job"),
               "",
               "dest"
        );
        fetchTask.setSrcdir("src");
        fetchTask.setConditions(new RunIfConfigs(RunIfConfig.FAILED));

        CRFetchArtifactTask result = (CRFetchArtifactTask) configConverter.taskToCRTask(fetchTask);

        assertThat(result.getRunIf(), is(CRRunIf.failed));
        assertThat(result.getDestination(), is("dest"));
        assertThat(result.getJob(), is("job"));
        assertThat(result.getPipelineName(), is("upstream"));
        assertThat(result.getSource(), is("src"));
        assertThat(result.sourceIsDirectory(), is(true));
    }

    @Test
    public void shouldConvertFetchArtifactTaskWhenSourceIsFileToCR() {
        FetchTask fetchTask = new FetchTask(
                new CaseInsensitiveString("upstream"),
                new CaseInsensitiveString("stage"),
                new CaseInsensitiveString("job"),
                "src",
                "dest"
        );
        fetchTask.setConditions(new RunIfConfigs(RunIfConfig.FAILED));

        CRFetchArtifactTask result = (CRFetchArtifactTask) configConverter.taskToCRTask(fetchTask);

        assertThat(result.getRunIf(), is(CRRunIf.failed));
        assertThat(result.getDestination(), is("dest"));
        assertThat(result.getJob(), is("job"));
        assertThat(result.getPipelineName(), is("upstream"));
        assertThat(result.getSource(), is("src"));
        assertThat(result.sourceIsDirectory(), is(false));
    }

    @Test
    public void shouldConvertFetchPluggableArtifactTaskToCRFetchPluggableArtifactTask() {
        FetchPluggableArtifactTask fetchPluggableArtifactTask = new FetchPluggableArtifactTask(
        new CaseInsensitiveString("upstream"),
                new CaseInsensitiveString("stage"),
                new CaseInsensitiveString("job"),
                "artifactId");
        fetchPluggableArtifactTask.setConditions(new RunIfConfigs(RunIfConfig.PASSED));

        CRFetchPluggableArtifactTask result = (CRFetchPluggableArtifactTask) configConverter.taskToCRTask(fetchPluggableArtifactTask);

        assertThat(result.getRunIf(), is(CRRunIf.passed));
        assertThat(result.getJob(), is("job"));
        assertThat(result.getPipelineName(), is("upstream"));
        assertThat(result.getStage(), is("stage"));
        assertThat(result.getArtifactId(), is("artifactId")); assertThat(result.getConfiguration().isEmpty(), is(true));
    }
}
