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
import com.thoughtworks.go.server.service.AgentService;
import com.thoughtworks.go.util.command.HgUrlArgument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.thoughtworks.go.config.PipelineConfig.LOCK_VALUE_LOCK_ON_FAILURE;
import static com.thoughtworks.go.helper.MaterialConfigsMother.*;
import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigConverterTest {

    private ConfigConverter configConverter;
    private PartialConfigLoadContext context;
    private List<String> filter = new ArrayList<>();
    private CachedGoConfig cachedGoConfig;

    private CRStage crStage;
    private CRGitMaterial git;
    private AgentService agentService;

    private CRJob buildJob() {
        CRJob crJob = new CRJob("name");
        crJob.setRunOnAllAgents(true);
        crJob.setRunInstanceCount(5);
        crJob.setTimeout(120);
        crJob.addEnvironmentVariable("key", "value");
        crJob.addTab(new CRTab("tabname", "tabpath"));
        crJob.addArtifact(new CRBuiltInArtifact("src", "dest", CRArtifactType.build));
        crJob.addTask(new CRFetchArtifactTask(CRRunIf.failed, null, "upstream", "stage", "job", "src", "dest", false));
        return crJob;
    }

    private CRPipeline buildPipeline() {
        CRPipeline crPipeline = new CRPipeline("pipeline", "group");
        git = new CRGitMaterial("name", "folder", true, false, null, filter, "url", "branch", true);

        crPipeline.addMaterial(git);
        crPipeline.addEnvironmentVariable("key", "value");
        crPipeline.setTrackingTool(new CRTrackingTool("link", "regex"));
        crPipeline.setTimer(new CRTimer("timer", true));
        crPipeline.addStage(crStage);
        crPipeline.setLabelTemplate("label-template");
        crPipeline.setLockBehavior(LOCK_VALUE_LOCK_ON_FAILURE);

        return crPipeline;
    }

    @BeforeEach
    void setUp() {
        cachedGoConfig = mock(CachedGoConfig.class);
        context = mock(PartialConfigLoadContext.class);
        agentService = mock(AgentService.class);
        configConverter = new ConfigConverter(new GoCipher(), cachedGoConfig, agentService);

        filter = new ArrayList<>();
        filter.add("filter");

        CRApproval approval = new CRApproval(CRApprovalCondition.manual);
        approval.addAuthorizedRole("authRole");
        approval.addAuthorizedUser("authUser");

        crStage = new CRStage("stageName");
        crStage.setFetchMaterials(true);
        crStage.setNeverCleanupArtifacts(true);
        crStage.setCleanWorkingDirectory(true);
        crStage.addEnvironmentVariable("key", "value");
        crStage.addJob(buildJob());

        git = new CRGitMaterial("name", "folder", true, false, null, filter, "url", "branch", true);
    }

    @Test
    void shouldConvertEnvironmentVariableToInsecureWhenValueIsNotBlank() {
        CREnvironmentVariable crEnvironmentVariable = new CREnvironmentVariable("key1", "value");
        EnvironmentVariableConfig result = configConverter.toEnvironmentVariableConfig(crEnvironmentVariable);
        assertThat(result.getValue()).isEqualTo("value");
        assertThat(result.getName()).isEqualTo("key1");
        assertThat(result.isSecure()).isFalse();
    }

    @Test
    void shouldConvertEnvironmentVariableToSecureWhenEncryptedValueIsNotBlank() throws CryptoException {
        String encryptedPassword = new GoCipher().encrypt("plain-text-password");
        CREnvironmentVariable crEnvironmentVariable = new CREnvironmentVariable("key1", null, encryptedPassword);
        EnvironmentVariableConfig result = configConverter.toEnvironmentVariableConfig(crEnvironmentVariable);
        assertThat(result.isSecure()).isTrue();
        assertThat(result.getValue()).isEqualTo("plain-text-password");
        assertThat(result.getName()).isEqualTo("key1");
    }

    @Test
    void shouldConvertEnvironmentVariableToSecureWhenEncryptedValueIsEmptyString() {
        CREnvironmentVariable crEnvironmentVariable = new CREnvironmentVariable("key1", null, "");
        EnvironmentVariableConfig result = configConverter.toEnvironmentVariableConfig(crEnvironmentVariable);
        assertThat(result.isSecure()).isTrue();
        assertThat(result.getValue()).isEqualTo("");
        assertThat(result.getName()).isEqualTo("key1");
    }

    @Test
    void shouldConvertEnvironmentVariableToInsecureWhenValueAndEncryptedValueIsNull() {
        CREnvironmentVariable crEnvironmentVariable = new CREnvironmentVariable("key1", null, null);
        EnvironmentVariableConfig result = configConverter.toEnvironmentVariableConfig(crEnvironmentVariable);
        assertThat(result.isSecure()).isFalse();
        assertThat(result.getValue()).isEqualTo("");
        assertThat(result.getName()).isEqualTo("key1");
    }

    @Test
    void shouldMigrateEnvironment() {
        CREnvironment crEnvironment = new CREnvironment("dev");
        crEnvironment.addEnvironmentVariable("key", "value");
        crEnvironment.addAgent("12");
        crEnvironment.addPipeline("pipe1");

        BasicEnvironmentConfig environmentConfig = configConverter.toEnvironmentConfig(crEnvironment);
        assertThat(environmentConfig.name().toLower()).isEqualTo("dev");
        assertThat(environmentConfig.contains("pipe1")).isTrue();
        assertThat(environmentConfig.hasVariable("key")).isTrue();
        assertThat(environmentConfig.hasAgent("12")).isTrue();
    }

    @Test
    void shouldMigratePluggableTask() {
        ArrayList<CRConfigurationProperty> configs = new ArrayList<>();
        configs.add(new CRConfigurationProperty("k", "m", null));
        CRPluggableTask pluggableTask = new CRPluggableTask(CRRunIf.any, null,
                new CRPluginConfiguration("myplugin", "1"), configs);
        PluggableTask result = (PluggableTask) configConverter.toAbstractTask(pluggableTask);

        assertThat(result.getPluginConfiguration().getId()).isEqualTo("myplugin");
        assertThat(result.getPluginConfiguration().getVersion()).isEqualTo("1");
        assertThat(result.getConfiguration().getProperty("k").getValue()).isEqualTo("m");
        assertThat(result.getConditions().first()).isEqualTo(RunIfConfig.ANY);
    }

    @Test
    void shouldMigrateRakeTask() {
        CRBuildTask crBuildTask = new CRBuildTask(CRBuildFramework.rake, CRRunIf.failed, null, "Rakefile.rb", "build", "src");
        RakeTask result = (RakeTask) configConverter.toAbstractTask(crBuildTask);

        assertRakeTask(result);
    }

    private void assertRakeTask(RakeTask result) {
        assertThat(result.getConditions().first()).isEqualTo(RunIfConfig.FAILED);
        assertThat(result.getBuildFile()).isEqualTo("Rakefile.rb");
        assertThat(result.getTarget()).isEqualTo("build");
        assertThat(result.workingDirectory()).isEqualTo("src");
    }

    @Test
    void shouldMigrateAntTask() {
        CRTask cancel = new CRBuildTask(CRBuildFramework.rake, CRRunIf.failed, null, "Rakefile.rb", "build", "src");
        CRBuildTask crBuildTask = new CRBuildTask(CRBuildFramework.ant, CRRunIf.failed, cancel, "ant", "build", "src");
        AntTask result = (AntTask) configConverter.toAbstractTask(crBuildTask);

        assertThat(result.getConditions().first()).isEqualTo(RunIfConfig.FAILED);
        assertThat(result.getBuildFile()).isEqualTo("ant");
        assertThat(result.getTarget()).isEqualTo("build");
        assertThat(result.workingDirectory()).isEqualTo("src");

        assertThat(result.cancelTask() instanceof RakeTask).isTrue();
        assertRakeTask((RakeTask) result.cancelTask());
    }

    @Test
    void shouldMigrateNantTask() {
        CRBuildTask crBuildTask = new CRNantTask(CRRunIf.passed, null, "nant", "build", "src", "path");
        NantTask result = (NantTask) configConverter.toAbstractTask(crBuildTask);

        assertThat(result.getConditions().first()).isEqualTo(RunIfConfig.PASSED);
        assertThat(result.getBuildFile()).isEqualTo("nant");
        assertThat(result.getTarget()).isEqualTo("build");
        assertThat(result.workingDirectory()).isEqualTo("src");
    }

    @Test
    void shouldConvertExecTaskWhenCancelIsNotSpecified() {
        CRExecTask crExecTask = new CRExecTask(CRRunIf.failed, null, "bash", "work", 120L);
        crExecTask.addArgument("1");
        crExecTask.addArgument("2");
        ExecTask result = (ExecTask) configConverter.toAbstractTask(crExecTask);

        assertThat(result.getConditions().first()).isEqualTo(RunIfConfig.FAILED);
        assertThat(result.command()).isEqualTo("bash");
        assertThat(result.getArgList()).contains(new Argument("1"));
        assertThat(result.getArgList()).contains(new Argument("2"));
        assertThat(result.workingDirectory()).isEqualTo("work");
        assertThat(result.getTimeout()).isEqualTo(120L);
        assertThat(result.getOnCancelConfig().getTask()).isInstanceOf(KillAllChildProcessTask.class);
    }

    @Test
    void shouldConvertExecTaskWhenCancelIsSpecified() {
        CRExecTask crExecTask = new CRExecTask(CRRunIf.failed, new CRExecTask(null, null, "kill", null, 0), "bash", "work", 120L);
        crExecTask.addArgument("1");
        crExecTask.addArgument("2");
        ExecTask result = (ExecTask) configConverter.toAbstractTask(crExecTask);

        assertThat(result.getConditions().first()).isEqualTo(RunIfConfig.FAILED);
        assertThat(result.command()).isEqualTo("bash");
        assertThat(result.getArgList()).contains(new Argument("1"));
        assertThat(result.getArgList()).contains(new Argument("2"));
        assertThat(result.workingDirectory()).isEqualTo("work");
        assertThat(result.getTimeout()).isEqualTo(120L);
        assertThat(result.getOnCancelConfig().getTask()).isInstanceOf(ExecTask.class);
        ExecTask cancel = (ExecTask) result.getOnCancelConfig().getTask();
        assertThat(cancel.command()).isEqualTo("kill");
    }

    @Test
    void shouldConvertFetchArtifactTaskAndSetEmptyStringWhenPipelineIsNotSpecified() {
        // if not then null causes errors in parameter expansion
        CRFetchArtifactTask crFetchArtifactTask = new CRFetchArtifactTask(CRRunIf.passed, null,
                null, "stage", "job", "src", null, false);

        FetchTask result = (FetchTask) configConverter.toAbstractTask(crFetchArtifactTask);

        assertThat(result.getConditions().first()).isEqualTo(RunIfConfig.PASSED);
        assertThat(result.getDest()).isEqualTo("");
        assertThat(result.getJob().toLower()).isEqualTo("job");
        assertThat(result.getPipelineName().toLower()).isEqualTo("");
        assertThat(result.getSrc()).isEqualTo("src");
        assertThat(result.isSourceAFile()).isTrue();
    }

    @Test
    void shouldConvertFetchArtifactTaskWhenDestinationIsNotSpecified() {
        CRFetchArtifactTask crFetchArtifactTask = new CRFetchArtifactTask(CRRunIf.passed, null,
                "upstream", "stage", "job", "src", null, false);

        FetchTask result = (FetchTask) configConverter.toAbstractTask(crFetchArtifactTask);

        assertThat(result.getConditions().first()).isEqualTo(RunIfConfig.PASSED);
        assertThat(result.getDest()).isEqualTo("");
        assertThat(result.getJob().toLower()).isEqualTo("job");
        assertThat(result.getPipelineName().toLower()).isEqualTo("upstream");
        assertThat(result.getSrc()).isEqualTo("src");
        assertThat(result.isSourceAFile()).isTrue();
    }

    @Test
    void shouldConvertFetchArtifactTaskWhenSourceIsDirectory() {
        CRFetchArtifactTask crFetchArtifactTask = new CRFetchArtifactTask(CRRunIf.failed, null,
                "upstream", "stage", "job", "src", "dest", true);

        FetchTask result = (FetchTask) configConverter.toAbstractTask(crFetchArtifactTask);

        assertThat(result.getConditions().first()).isEqualTo(RunIfConfig.FAILED);
        assertThat(result.getDest()).isEqualTo("dest");
        assertThat(result.getJob().toLower()).isEqualTo("job");
        assertThat(result.getPipelineName().toLower()).isEqualTo("upstream");
        assertThat(result.getSrc()).isEqualTo("src");
        assertThat(result.getSrcfile()).isNull();
        assertThat(result.isSourceAFile()).isFalse();
    }

    @Test
    void shouldConvertFetchArtifactTaskWhenSourceIsFile() {
        CRFetchArtifactTask crFetchArtifactTask = new CRFetchArtifactTask(CRRunIf.failed, null,
                "upstream", "stage", "job", "src", "dest", false);

        FetchTask result = (FetchTask) configConverter.toAbstractTask(crFetchArtifactTask);

        assertThat(result.getConditions().first()).isEqualTo(RunIfConfig.FAILED);
        assertThat(result.getDest()).isEqualTo("dest");
        assertThat(result.getJob().toLower()).isEqualTo("job");
        assertThat(result.getPipelineName().toLower()).isEqualTo("upstream");
        assertThat(result.getSrc()).isEqualTo("src");
        assertThat(result.getSrcdir()).isNull();
        assertThat(result.isSourceAFile()).isTrue();
    }

    @Test
    void shouldConvertFetchPluggableArtifactTaskAndSetEmptyStringWhenPipelineIsNotSpecified() {
        CRConfigurationProperty crConfigurationProperty = new CRConfigurationProperty("k1", "v1", null);
        final CRConfigurationProperty[] crConfigurationProperties = new CRConfigurationProperty[]{crConfigurationProperty};
        final List<CRConfigurationProperty> crConfigurationProperties1 = Arrays.asList(crConfigurationProperties);
        CRFetchPluggableArtifactTask crFetchPluggableArtifactTask = new CRFetchPluggableArtifactTask(CRRunIf.passed, null,
                null, "stage", "job", "artifactId", crConfigurationProperties1);

        FetchPluggableArtifactTask result = (FetchPluggableArtifactTask) configConverter.toAbstractTask(crFetchPluggableArtifactTask);

        assertThat(result.getConditions().first()).isEqualTo(RunIfConfig.PASSED);
        assertThat(result.getJob().toLower()).isEqualTo("job");
        assertThat(result.getPipelineName().toLower()).isEqualTo("");
        assertThat(result.getArtifactId()).isEqualTo("artifactId");
        assertThat(result.getConfiguration().getProperty("k1").getValue()).isEqualTo("v1");
    }

    @Test
    void shouldConvertFetchPluggableArtifactTaskWhenConfigurationIsNotSet() {
        final CRConfigurationProperty[] crConfigurationProperties = new CRConfigurationProperty[]{};
        final List<CRConfigurationProperty> crConfigurationProperties1 = Arrays.asList(crConfigurationProperties);
        CRFetchPluggableArtifactTask crFetchPluggableArtifactTask = new CRFetchPluggableArtifactTask(CRRunIf.passed, null,
                null, "stage", "job", "artifactId", crConfigurationProperties1);

        FetchPluggableArtifactTask result = (FetchPluggableArtifactTask) configConverter.toAbstractTask(crFetchPluggableArtifactTask);

        assertThat(result.getConditions().first()).isEqualTo(RunIfConfig.PASSED);
        assertThat(result.getJob().toLower()).isEqualTo("job");
        assertThat(result.getPipelineName().toLower()).isEqualTo("");
        assertThat(result.getArtifactId()).isEqualTo("artifactId");
        assertThat(result.getConfiguration().isEmpty()).isTrue();
    }

    @Test
    void shouldConvertDependencyMaterial() {
        CRDependencyMaterial crDependencyMaterial = new CRDependencyMaterial("name", "pipe", "stage");
        DependencyMaterialConfig dependencyMaterialConfig =
                (DependencyMaterialConfig) configConverter.toMaterialConfig(crDependencyMaterial, context);

        assertThat(dependencyMaterialConfig.getName().toLower()).isEqualTo("name");
        assertThat(dependencyMaterialConfig.getPipelineName().toLower()).isEqualTo("pipe");
        assertThat(dependencyMaterialConfig.getStageName().toLower()).isEqualTo("stage");
    }

    @Test
    void shouldConvertGitMaterial() {
        CRGitMaterial crGitMaterial = new CRGitMaterial("name", "folder", true, false, null, filter, "url", "branch", true);

        GitMaterialConfig gitMaterialConfig =
                (GitMaterialConfig) configConverter.toMaterialConfig(crGitMaterial, context);

        assertThat(gitMaterialConfig.getName().toLower()).isEqualTo("name");
        assertThat(gitMaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(gitMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(gitMaterialConfig.isInvertFilter()).isFalse();
        assertThat(gitMaterialConfig.getFilterAsString()).isEqualTo("filter");
        assertThat(gitMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(gitMaterialConfig.getBranch()).isEqualTo("branch");
    }

    @Test
    void shouldConvertGitMaterialWhenWhitelist() {
        CRGitMaterial crGitMaterial = new CRGitMaterial("name", "folder", true, true, null, filter, "url", "branch", true);

        GitMaterialConfig gitMaterialConfig =
                (GitMaterialConfig) configConverter.toMaterialConfig(crGitMaterial, context);

        assertThat(gitMaterialConfig.getName().toLower()).isEqualTo("name");
        assertThat(gitMaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(gitMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(gitMaterialConfig.isInvertFilter()).isTrue();
        assertThat(gitMaterialConfig.getFilterAsString()).isEqualTo("filter");
        assertThat(gitMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(gitMaterialConfig.getBranch()).isEqualTo("branch");
    }

    @Test
    void shouldConvertHgMaterialWhenWhitelist() {
        CRHgMaterial crHgMaterial = new CRHgMaterial("name", "folder", true, true, null, filter, "url", "feature");

        HgMaterialConfig hgMaterialConfig =
                (HgMaterialConfig) configConverter.toMaterialConfig(crHgMaterial, context);

        assertThat(hgMaterialConfig.getName().toLower()).isEqualTo("name");
        assertThat(hgMaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(hgMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(hgMaterialConfig.isInvertFilter()).isTrue();
        assertThat(hgMaterialConfig.getFilterAsString()).isEqualTo("filter");
        assertThat(hgMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(hgMaterialConfig.getBranch()).isEqualTo("feature");
    }

    @Test
    void shouldConvertGitMaterialWhenNulls() {
        CRGitMaterial crGitMaterial = new CRGitMaterial();
        crGitMaterial.setUrl("url");

        GitMaterialConfig gitMaterialConfig =
                (GitMaterialConfig) configConverter.toMaterialConfig(crGitMaterial, context);

        assertThat(crGitMaterial.getName()).isNull();
        assertThat(crGitMaterial.getDestination()).isNull();
        assertThat(gitMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(gitMaterialConfig.isShallowClone()).isFalse();
        assertThat(gitMaterialConfig.filter()).isEqualTo(new Filter());
        assertThat(gitMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(gitMaterialConfig.getBranch()).isEqualTo("master");
    }

    @Test
    void shouldConvertGitMaterialWhenPlainPassword() {
        CRGitMaterial crGitMaterial = new CRGitMaterial("name", "folder", true, false, null, filter, "url", "branch", true);
        crGitMaterial.setPassword("secret");

        GitMaterialConfig gitMaterialConfig =
                (GitMaterialConfig) configConverter.toMaterialConfig(crGitMaterial, context);

        assertThat(gitMaterialConfig.getName().toLower()).isEqualTo("name");
        assertThat(gitMaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(gitMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(gitMaterialConfig.isInvertFilter()).isFalse();
        assertThat(gitMaterialConfig.getFilterAsString()).isEqualTo("filter");
        assertThat(gitMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(gitMaterialConfig.getBranch()).isEqualTo("branch");
        assertThat(gitMaterialConfig.getPassword()).isEqualTo("secret");
    }

    @Test
    void shouldConvertGitMaterialWhenEncryptedPassword() throws CryptoException {
        CRGitMaterial crGitMaterial = new CRGitMaterial("name", "folder", true, false, null, filter, "url", "branch", true);
        crGitMaterial.setEncryptedPassword(new GoCipher().encrypt("secret"));

        GitMaterialConfig gitMaterialConfig =
                (GitMaterialConfig) configConverter.toMaterialConfig(crGitMaterial, context);

        assertThat(gitMaterialConfig.getName().toLower()).isEqualTo("name");
        assertThat(gitMaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(gitMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(gitMaterialConfig.isInvertFilter()).isFalse();
        assertThat(gitMaterialConfig.getFilterAsString()).isEqualTo("filter");
        assertThat(gitMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(gitMaterialConfig.getBranch()).isEqualTo("branch");
        assertThat(gitMaterialConfig.getPassword()).isEqualTo("secret");
    }

    @Test
    void shouldConvertConfigMaterialWhenConfigRepoIsGitWithUrlOnly() {
        // this url would be configured inside xml config-repo section
        GitMaterialConfig configRepoMaterial = git("url");
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial();

        MaterialConfig materialConfig = configConverter.toMaterialConfig(crConfigMaterial, context);
        assertThat(materialConfig.getName()).as("shouldSetEmptyMaterialNameAsInConfigRepoSourceCode").isNull();

        GitMaterialConfig gitMaterialConfig = (GitMaterialConfig) materialConfig;
        assertThat(gitMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(gitMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(gitMaterialConfig.getFolder()).isNull();
        assertThat(gitMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(gitMaterialConfig.isShallowClone()).isFalse();
        assertThat(gitMaterialConfig.filter()).isEqualTo(new Filter());
        assertThat(gitMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(gitMaterialConfig.getBranch()).isEqualTo("master");
    }


    @Test
    void shouldConvertConfigMaterialWhenConfigRepoIsGitWithBlacklist() {
        // this url would be configured inside xml config-repo section
        GitMaterialConfig configRepoMaterial = git("url");
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial();
        crConfigMaterial.setFilter(new CRFilter(filter, false));

        MaterialConfig materialConfig = configConverter.toMaterialConfig(crConfigMaterial, context);
        assertThat(materialConfig.getName()).as("shouldSetEmptyMaterialNameAsInConfigRepoSourceCode").isNull();

        GitMaterialConfig gitMaterialConfig = (GitMaterialConfig) materialConfig;
        assertThat(gitMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(gitMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(gitMaterialConfig.getFolder()).isNull();
        assertThat(gitMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(gitMaterialConfig.isShallowClone()).isFalse();
        Filter blacklistFilter = new Filter(new IgnoredFiles("filter"));
        assertThat(gitMaterialConfig.filter()).isEqualTo(blacklistFilter);
        assertThat(gitMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(gitMaterialConfig.getBranch()).isEqualTo("master");
    }

    @Test
    void shouldConvertConfigMaterialWhenConfigRepoIsHgWithWhitelist() {
        // this url would be configured inside xml config-repo section
        HgMaterialConfig configRepoMaterial = hg("url", "folder");
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial(null, null, new CRFilter(filter, true));

        MaterialConfig materialConfig = configConverter.toMaterialConfig(crConfigMaterial, context);
        assertThat(materialConfig.getName()).as("shouldSetEmptyMaterialNameAsInConfigRepoSourceCode").isNull();

        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig) materialConfig;
        assertThat(hgMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(hgMaterialConfig.getFilterAsString()).isEqualTo("filter");
        assertThat(hgMaterialConfig.getUrl()).isEqualTo("url");
        Filter whitelistFilter = new Filter(new IgnoredFiles("filter"));
        assertThat(hgMaterialConfig.filter()).isEqualTo(whitelistFilter);
        assertThat(hgMaterialConfig.isInvertFilter()).isTrue();
    }

    @Test
    void shouldConvertConfigMaterialWhenConfigRepoIsHgWithEmptyFilter() {
        // this url would be configured inside xml config-repo section
        HgMaterialConfig configRepoMaterial = hg("url", "folder");
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial(null, null, new CRFilter(new ArrayList<>(), true));

        MaterialConfig materialConfig = configConverter.toMaterialConfig(crConfigMaterial, context);
        assertThat(materialConfig.getName()).as("shouldSetEmptyMaterialNameAsInConfigRepoSourceCode").isNull();

        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig) materialConfig;
        assertThat(hgMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(hgMaterialConfig.getFilterAsString()).isEqualTo("");
        assertThat(hgMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(hgMaterialConfig.filter()).isEqualTo(new Filter());
        assertThat(hgMaterialConfig.isInvertFilter()).isFalse();
    }

    @Test
    void shouldConvertConfigMaterialWhenConfigRepoIsHg() {
        // these parameters would be configured inside xml config-repo section
        HgMaterialConfig configRepoMaterial = hg(new HgUrlArgument("url"), null, null, null, true, new Filter(new IgnoredFiles("ignore")), false, "folder", new CaseInsensitiveString("name"));
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial("example", null, null);

        MaterialConfig materialConfig = configConverter.toMaterialConfig(crConfigMaterial, context);
        assertThat(materialConfig.getName().toLower()).as("shouldSetMaterialNameAsInConfigRepoSourceCode").isEqualTo("example");
        assertThat(materialConfig.getFolder()).as("shouldUseFolderFromXMLWhenConfigRepoHasNone").isEqualTo("folder");

        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig) materialConfig;
        assertThat(hgMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(hgMaterialConfig.getFilterAsString()).isEqualTo("ignore");
        assertThat(hgMaterialConfig.getUrl()).isEqualTo("url");
    }

    @Test
    void shouldConvertConfigMaterialWhenConfigRepoIsHgWithDestination() {
        // these parameters would be configured inside xml config-repo section
        HgMaterialConfig configRepoMaterial = hg(new HgUrlArgument("url"), null, null, null, true, new Filter(new IgnoredFiles("ignore")), false, "folder", new CaseInsensitiveString("name"));
        when(context.configMaterial()).thenReturn(configRepoMaterial);
        CRConfigMaterial crConfigMaterial = new CRConfigMaterial("example", "dest1", null);

        MaterialConfig materialConfig = configConverter.toMaterialConfig(crConfigMaterial, context);
        assertThat(materialConfig.getName().toLower()).as("shouldSetMaterialNameAsInConfigRepoSourceCode").isEqualTo("example");
        assertThat(materialConfig.getFolder()).as("shouldUseFolderFromConfigRepoWhenSpecified").isEqualTo("dest1");

        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig) materialConfig;
        assertThat(hgMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(hgMaterialConfig.getFilterAsString()).isEqualTo("ignore");
        assertThat(hgMaterialConfig.getUrl()).isEqualTo("url");
    }

    @Test
    void shouldConvertConfigMaterialWhenPluggableScmMaterial() {
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

        assertThat(pluggableSCMMaterialConfig.getName().toLower()).isEqualTo("example");
        assertThat(pluggableSCMMaterialConfig.getSCMConfig()).isEqualTo(myscm);
        assertThat(pluggableSCMMaterialConfig.getScmId()).isEqualTo("scmid");
        assertThat(pluggableSCMMaterialConfig.getFolder()).isEqualTo("dest1");
        assertThat(pluggableSCMMaterialConfig.getFilterAsString()).isEqualTo("");
    }


    @Test
    void shouldFailToConvertConfigMaterialWhenPluggableScmMaterialWithWhitelist() {
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
            assertThat(ex.getMessage()).isEqualTo("Plugable SCMs do not support whitelisting");
        }
    }

    @Test
    void shouldConvertHgMaterial() {
        CRHgMaterial crHgMaterial = new CRHgMaterial("name", "folder", true, false, null, filter, "url", null);

        HgMaterialConfig hgMaterialConfig =
                (HgMaterialConfig) configConverter.toMaterialConfig(crHgMaterial, context);

        assertThat(hgMaterialConfig.getName().toLower()).isEqualTo("name");
        assertThat(hgMaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(hgMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(hgMaterialConfig.getFilterAsString()).isEqualTo("filter");
        assertThat(hgMaterialConfig.getUrl()).isEqualTo("url");
    }

    @Test
    void shouldConvertHgMaterialWhenNullName() {
        CRHgMaterial crHgMaterial = new CRHgMaterial(null, "folder", true, false, null, filter, "url", null);

        HgMaterialConfig hgMaterialConfig =
                (HgMaterialConfig) configConverter.toMaterialConfig(crHgMaterial, context);

        assertThat(hgMaterialConfig.getName()).isNull();
        assertThat(hgMaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(hgMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(hgMaterialConfig.getFilterAsString()).isEqualTo("filter");
        assertThat(hgMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(hgMaterialConfig.getInvertFilter()).isFalse();
        assertThat(hgMaterialConfig.getBranch()).isNull();
    }

    @Test
    void shouldConvertHgMaterialWhenPlainPassword() {
        CRHgMaterial crHgMaterial = new CRHgMaterial(null, "folder", true, false, null, filter, "url", null);
        crHgMaterial.setPassword("secret");

        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig) configConverter.toMaterialConfig(crHgMaterial, context);

        assertThat(hgMaterialConfig.getName()).isNull();
        assertThat(hgMaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(hgMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(hgMaterialConfig.getFilterAsString()).isEqualTo("filter");
        assertThat(hgMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(hgMaterialConfig.getPassword()).isEqualTo("secret");
        assertThat(hgMaterialConfig.getInvertFilter()).isFalse();
    }

    @Test
    void shouldConvertHgMaterialWhenEncryptedPassword() throws CryptoException {
        CRHgMaterial crHgMaterial = new CRHgMaterial(null, "folder", true, false, null, filter, "url", null);
        crHgMaterial.setEncryptedPassword(new GoCipher().encrypt("some password"));

        HgMaterialConfig hgMaterialConfig = (HgMaterialConfig) configConverter.toMaterialConfig(crHgMaterial, context);

        assertThat(hgMaterialConfig.getName()).isNull();
        assertThat(hgMaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(hgMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(hgMaterialConfig.getFilterAsString()).isEqualTo("filter");
        assertThat(hgMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(hgMaterialConfig.getPassword()).isEqualTo("some password");
        assertThat(hgMaterialConfig.getInvertFilter()).isFalse();
    }

    @Test
    void shouldConvertHgMaterialWhenEmptyName() {
        CRHgMaterial crHgMaterial = new CRHgMaterial("", "folder", true, false, null, filter, "url", null);

        HgMaterialConfig hgMaterialConfig =
                (HgMaterialConfig) configConverter.toMaterialConfig(crHgMaterial, context);

        assertThat(hgMaterialConfig.getName()).isNull();
        assertThat(hgMaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(hgMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(hgMaterialConfig.getFilterAsString()).isEqualTo("filter");
        assertThat(hgMaterialConfig.getUrl()).isEqualTo("url");
    }

    @Test
    void shouldConvertP4MaterialWhenEncryptedPassword() throws CryptoException {
        String encryptedPassword = new GoCipher().encrypt("plain-text-password");
        CRP4Material crp4Material1 = new CRP4Material("name", "folder", false, false, "user", filter, "server:port", "view", true);
        crp4Material1.setEncryptedPassword(encryptedPassword);
        CRP4Material crp4Material = crp4Material1;

        P4MaterialConfig p4MaterialConfig =
                (P4MaterialConfig) configConverter.toMaterialConfig(crp4Material, context);

        assertThat(p4MaterialConfig.getName().toLower()).isEqualTo("name");
        assertThat(p4MaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(p4MaterialConfig.getAutoUpdate()).isFalse();
        assertThat(p4MaterialConfig.getFilterAsString()).isEqualTo("filter");
        assertThat(p4MaterialConfig.getUrl()).isEqualTo("server:port");
        assertThat(p4MaterialConfig.getUserName()).isEqualTo("user");
        assertThat(p4MaterialConfig.getPassword()).isEqualTo("plain-text-password");
        assertThat(p4MaterialConfig.getUseTickets()).isTrue();
        assertThat(p4MaterialConfig.getView()).isEqualTo("view");

    }

    @Test
    void shouldConvertP4MaterialWhenPlainPassword() {
        CRP4Material crp4Material = new CRP4Material("name", "folder", false, false, "user", filter, "server:port", "view", true);
        crp4Material.setPassword("secret");

        P4MaterialConfig p4MaterialConfig = (P4MaterialConfig) configConverter.toMaterialConfig(crp4Material, context);

        assertThat(p4MaterialConfig.getName().toLower()).isEqualTo("name");
        assertThat(p4MaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(p4MaterialConfig.getAutoUpdate()).isFalse();
        assertThat(p4MaterialConfig.getFilterAsString()).isEqualTo("filter");
        assertThat(p4MaterialConfig.getUrl()).isEqualTo("server:port");
        assertThat(p4MaterialConfig.getUserName()).isEqualTo("user");
        assertThat(p4MaterialConfig.getPassword()).isEqualTo("secret");
        assertThat(p4MaterialConfig.getUseTickets()).isTrue();
        assertThat(p4MaterialConfig.getView()).isEqualTo("view");

    }

    @Test
    void shouldConvertSvmMaterialWhenEncryptedPassword() throws CryptoException {
        String encryptedPassword = new GoCipher().encrypt("plain-text-password");
        CRSvnMaterial crSvnMaterial = new CRSvnMaterial("name", "folder", true, false, "username", filter, "url", true);
        crSvnMaterial.setEncryptedPassword(encryptedPassword);

        SvnMaterialConfig svnMaterialConfig = (SvnMaterialConfig) configConverter.toMaterialConfig(crSvnMaterial, context);

        assertThat(svnMaterialConfig.getName().toLower()).isEqualTo("name");
        assertThat(svnMaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(svnMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(svnMaterialConfig.getFilterAsString()).isEqualTo("filter");
        assertThat(svnMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(svnMaterialConfig.getUserName()).isEqualTo("username");
        assertThat(svnMaterialConfig.getPassword()).isEqualTo("plain-text-password");
        assertThat(svnMaterialConfig.isCheckExternals()).isTrue();
    }

    @Test
    void shouldConvertSvmMaterialWhenPlainPassword() {
        CRSvnMaterial crSvnMaterial = new CRSvnMaterial("name", "folder", true, false, "username", filter, "url", true);
        crSvnMaterial.setPassword("password");

        SvnMaterialConfig svnMaterialConfig =
                (SvnMaterialConfig) configConverter.toMaterialConfig(crSvnMaterial, context);

        assertThat(svnMaterialConfig.getName().toLower()).isEqualTo("name");
        assertThat(svnMaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(svnMaterialConfig.getAutoUpdate()).isTrue();
        assertThat(svnMaterialConfig.getFilterAsString()).isEqualTo("filter");
        assertThat(svnMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(svnMaterialConfig.getUserName()).isEqualTo("username");
        assertThat(svnMaterialConfig.getPassword()).isEqualTo("password");
        assertThat(svnMaterialConfig.isCheckExternals()).isTrue();
    }

    @Test
    void shouldConvertTfsMaterialWhenPlainPassword() {
        CRTfsMaterial crTfsMaterial = new CRTfsMaterial("name", "folder", false, false, "user", filter, "url", "project", "domain");
        crTfsMaterial.setPassword("secret");

        TfsMaterialConfig tfsMaterialConfig = (TfsMaterialConfig) configConverter.toMaterialConfig(crTfsMaterial, context);

        assertThat(tfsMaterialConfig.getName().toLower()).isEqualTo("name");
        assertThat(tfsMaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(tfsMaterialConfig.getAutoUpdate()).isFalse();
        assertThat(tfsMaterialConfig.getFilterAsString()).isEqualTo("filter");
        assertThat(tfsMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(tfsMaterialConfig.getUserName()).isEqualTo("user");
        assertThat(tfsMaterialConfig.getPassword()).isEqualTo("secret");
        assertThat(tfsMaterialConfig.getDomain()).isEqualTo("domain");
        assertThat(tfsMaterialConfig.getProjectPath()).isEqualTo("project");

    }

    @Test
    void shouldConvertTfsMaterialWhenEncryptedPassword() throws CryptoException {
        String encryptedPassword = new GoCipher().encrypt("plain-text-password");
        CRTfsMaterial crTfsMaterial = new CRTfsMaterial("name", "folder", false, false, "user", filter, "url", "project", "domain");
        crTfsMaterial.setEncryptedPassword(encryptedPassword);

        TfsMaterialConfig tfsMaterialConfig = (TfsMaterialConfig) configConverter.toMaterialConfig(crTfsMaterial, context);

        assertThat(tfsMaterialConfig.getName().toLower()).isEqualTo("name");
        assertThat(tfsMaterialConfig.getFolder()).isEqualTo("folder");
        assertThat(tfsMaterialConfig.getAutoUpdate()).isFalse();
        assertThat(tfsMaterialConfig.getFilterAsString()).isEqualTo("filter");
        assertThat(tfsMaterialConfig.getUrl()).isEqualTo("url");
        assertThat(tfsMaterialConfig.getUserName()).isEqualTo("user");
        assertThat(tfsMaterialConfig.getPassword()).isEqualTo("plain-text-password");
        assertThat(tfsMaterialConfig.getDomain()).isEqualTo("domain");
        assertThat(tfsMaterialConfig.getProjectPath()).isEqualTo("project");

    }

    @Test
    void shouldConvertPluggableScmMaterial() {
        SCM myscm = new SCM("scmid", new PluginConfiguration(), new Configuration());
        SCMs scms = new SCMs(myscm);

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setSCMs(scms);
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);

        CRPluggableScmMaterial crPluggableScmMaterial = new CRPluggableScmMaterial("name", "scmid", "directory", filter);

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig =
                (PluggableSCMMaterialConfig) configConverter.toMaterialConfig(crPluggableScmMaterial, context);

        assertThat(pluggableSCMMaterialConfig.getName().toLower()).isEqualTo("name");
        assertThat(pluggableSCMMaterialConfig.getSCMConfig()).isEqualTo(myscm);
        assertThat(pluggableSCMMaterialConfig.getScmId()).isEqualTo("scmid");
        assertThat(pluggableSCMMaterialConfig.getFolder()).isEqualTo("directory");
        assertThat(pluggableSCMMaterialConfig.getFilterAsString()).isEqualTo("filter");
    }

    @Test
    void shouldConvertPluggableScmMaterialWithNewSCM() {
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

        assertThat(pluggableSCMMaterialConfig.getName().toLower()).isEqualTo("name");
        assertThat(pluggableSCMMaterialConfig.getSCMConfig().getName()).isEqualTo("name");
        assertThat(pluggableSCMMaterialConfig.getSCMConfig().getId()).isEqualTo("scmid");
        assertThat(pluggableSCMMaterialConfig.getSCMConfig().getFingerprint()).isEqualTo(myscm.getFingerprint());
        assertThat(pluggableSCMMaterialConfig.getScmId()).isEqualTo("scmid");
        assertThat(pluggableSCMMaterialConfig.getFolder()).isEqualTo("directory");
        assertThat(pluggableSCMMaterialConfig.getFilterAsString()).isEqualTo("filter");
    }

    @Test
    void shouldConvertPluggableScmMaterialWithADuplicateSCMFingerPrintShouldUseWhatAlreadyExists() {
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

        assertThat(pluggableSCMMaterialConfig.getSCMConfig()).isEqualTo(scm);
    }

    @Test
    void shouldConvertPluggableScmMaterialWithANewSCMDefinitionWithoutAnSCMID() {
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setSCMs(new SCMs());
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);

        CRPluggableScmMaterial crPluggableScmMaterial = new CRPluggableScmMaterial("name", null, "directory", filter);
        crPluggableScmMaterial.setPluginConfiguration(new CRPluginConfiguration("plugin_id", "1.0"));

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig =
                (PluggableSCMMaterialConfig) configConverter.toMaterialConfig(crPluggableScmMaterial, context);
        assertThat(pluggableSCMMaterialConfig.getScmId()).isNotNull();
    }

    @Test
    void shouldConvertPluggableScmMaterialWithADuplicateSCMIDShouldUseWhatAlreadyExists() {
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

        assertThat(pluggableSCMMaterialConfig.getSCMConfig()).isEqualTo(scm);
    }

    @Test
    void shouldConvertPluggableScmMaterialWithNewSCMPluginVersionShouldDefaultToEmptyString() {
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

        assertThat(pluggableSCMMaterialConfig.getName().toLower()).isEqualTo("name");
        assertThat(pluggableSCMMaterialConfig.getSCMConfig()).isEqualTo(myscm);
        assertThat(pluggableSCMMaterialConfig.getScmId()).isEqualTo("scmid");
        assertThat(pluggableSCMMaterialConfig.getFolder()).isEqualTo("directory");
        assertThat(pluggableSCMMaterialConfig.getFilterAsString()).isEqualTo("filter");
    }

    @Test
    void shouldConvertPackageMaterial() {
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

        assertThat(packageMaterialConfig.getName().toLower()).isEqualTo("name");
        assertThat(packageMaterialConfig.getPackageId()).isEqualTo("package-id");
        assertThat(packageMaterialConfig.getPackageDefinition()).isEqualTo(definition);
    }

    @Test
    void shouldConvertArtifactConfigWhenDestinationIsNull() {
        BuildArtifactConfig buildArtifactConfig = (BuildArtifactConfig) configConverter.toArtifactConfig(new CRBuiltInArtifact("src", null, CRArtifactType.build));
        assertThat(buildArtifactConfig.getDestination()).isEqualTo("");
    }

    @Test
    void shouldConvertTestArtifactConfigWhenDestinationIsNull() {
        TestArtifactConfig testArtifactConfig = (TestArtifactConfig) configConverter.toArtifactConfig(new CRBuiltInArtifact("src", null, CRArtifactType.test));
        assertThat(testArtifactConfig.getDestination()).isEqualTo("testoutput");
    }

    @Test
    void shouldConvertArtifactConfigWhenDestinationIsSet() {
        BuildArtifactConfig buildArtifactConfig = (BuildArtifactConfig) configConverter.toArtifactConfig(new CRBuiltInArtifact("src", "dest", CRArtifactType.build));
        assertThat(buildArtifactConfig.getDestination()).isEqualTo("dest");
    }

    @Test
    void shouldConvertToPluggableArtifactConfigWhenConfigrationIsNotPresent() {
        PluggableArtifactConfig pluggableArtifactConfig = (PluggableArtifactConfig) configConverter.toArtifactConfig(new CRPluggableArtifact("id", "storeId", null));

        assertThat(pluggableArtifactConfig.getId()).isEqualTo("id");
        assertThat(pluggableArtifactConfig.getStoreId()).isEqualTo("storeId");
        assertThat(pluggableArtifactConfig.getConfiguration().isEmpty()).isTrue();
    }

    @Test
    void shouldConvertToPluggableArtifactConfigWithRightConfiguration() {
        final CRConfigurationProperty[] filenames = new CRConfigurationProperty[]{new CRConfigurationProperty("filename", "who-cares")};
        PluggableArtifactConfig pluggableArtifactConfig = (PluggableArtifactConfig) configConverter.toArtifactConfig(new CRPluggableArtifact("id", "storeId", Arrays.asList(filenames)));

        assertThat(pluggableArtifactConfig.getId()).isEqualTo("id");
        assertThat(pluggableArtifactConfig.getStoreId()).isEqualTo("storeId");
        Configuration configuration = pluggableArtifactConfig.getConfiguration();
        assertThat(configuration.size()).isEqualTo(1);
        assertThat(configuration.get(0).getConfigKeyName()).isEqualTo("filename");
        assertThat(configuration.get(0).getConfigValue()).isEqualTo("who-cares");
    }

    @Test
    void shouldConvertJob() {
        CRJob crJob = buildJob();
        crJob.addResource("resource1");

        JobConfig jobConfig = configConverter.toJobConfig(crJob);

        assertThat(jobConfig.name().toLower()).isEqualTo("name");
        assertThat(jobConfig.hasVariable("key")).isTrue();
        assertThat(jobConfig.getTabs().first().getName()).isEqualTo("tabname");
        assertThat(jobConfig.resourceConfigs().get(0)).isEqualTo(new ResourceConfig("resource1"));
        assertThat(jobConfig.artifactConfigs()).contains(new BuildArtifactConfig("src", "dest"));
        assertThat(jobConfig.isRunOnAllAgents()).isFalse();
        assertThat(jobConfig.getRunInstanceCount()).isEqualTo("5");
        assertThat(jobConfig.getTimeout()).isEqualTo("120");
        assertThat(jobConfig.getTasks().size()).isEqualTo(1);
    }

    @Test
    void shouldConvertJobWhenHasElasticProfileId() {
        CRJob crJob = buildJob();
        crJob.setRunOnAllAgents(false);
        crJob.setElasticProfileId("myprofile");

        JobConfig jobConfig = configConverter.toJobConfig(crJob);

        assertThat(jobConfig.getElasticProfileId()).isEqualTo("myprofile");
        assertThat(jobConfig.resourceConfigs().size()).isEqualTo(0);
    }

    @Test
    void shouldConvertJobWhenRunInstanceCountIsNotSpecified() {
        CRJob crJob = buildJob();
        crJob.setRunOnAllAgents(false);

        JobConfig jobConfig = configConverter.toJobConfig(crJob);

        assertThat(jobConfig.isRunOnAllAgents()).isFalse();
        assertThat(jobConfig.getRunInstanceCount()).isNull();
        assertThat(jobConfig.getTimeout()).isEqualTo("120");
        assertThat(jobConfig.getTasks().size()).isEqualTo(1);
    }

    @Test
    void shouldConvertJobWhenRunInstanceCountIsAll() {
        CRJob crJob = buildJob();
        crJob.setRunOnAllAgents(true);

        JobConfig jobConfig = configConverter.toJobConfig(crJob);

        assertThat(jobConfig.getRunInstanceCount()).isNull();
        assertThat(jobConfig.isRunOnAllAgents()).isTrue();
        assertThat(jobConfig.getTimeout()).isEqualTo("120");
        assertThat(jobConfig.getTasks().size()).isEqualTo(1);
    }

    @Test
    void shouldConvertApprovalWhenManualAndAuth() {
        CRApproval crApproval = new CRApproval(CRApprovalCondition.manual);
        crApproval.addAuthorizedUser("authUser");
        crApproval.addAuthorizedRole("authRole");

        Approval approval = configConverter.toApproval(crApproval);
        assertThat(approval.isManual()).isTrue();
        assertThat(approval.isAuthorizationDefined()).isTrue();
        assertThat(approval.getAuthConfig().getRoles()).contains(new AdminRole(new CaseInsensitiveString("authRole")));
        assertThat(approval.getAuthConfig().getUsers()).contains(new AdminUser(new CaseInsensitiveString("authUser")));
    }

    @Test
    void shouldConvertApprovalWhenManualAndNoAuth() {
        CRApproval crApproval = new CRApproval(CRApprovalCondition.manual);

        Approval approval = configConverter.toApproval(crApproval);
        assertThat(approval.isManual()).isTrue();
        assertThat(approval.isAuthorizationDefined()).isFalse();
    }

    @Test
    void shouldConvertApprovalWhenSuccess() {
        CRApproval crApproval = new CRApproval(CRApprovalCondition.success);

        Approval approval = configConverter.toApproval(crApproval);
        assertThat(approval.isManual()).isFalse();
        assertThat(approval.isAuthorizationDefined()).isFalse();
    }

    @Test
    void shouldConvertApprovalWhenAllowOnlyOnSuccessIsSet() {
        CRApproval crApproval = new CRApproval(CRApprovalCondition.success);
        crApproval.setAllowOnlyOnSuccess(true);

        Approval approval = configConverter.toApproval(crApproval);
        assertThat(approval.isManual()).isFalse();
        assertThat(approval.isAuthorizationDefined()).isFalse();
        assertThat(approval.isAllowOnlyOnSuccess()).isTrue();
    }

    @Test
    void shouldConvertStage() {
        CRApproval approval = new CRApproval(CRApprovalCondition.manual);
        approval.addAuthorizedUser("authUser");
        approval.addAuthorizedRole("authRole");

        CRStage crStage = new CRStage("stageName");
        crStage.setFetchMaterials(true);
        crStage.setNeverCleanupArtifacts(true);
        crStage.setCleanWorkingDirectory(true);
        crStage.setApproval(approval);
        crStage.addEnvironmentVariable("key", "value");
        crStage.addJob(buildJob());

        StageConfig stageConfig = configConverter.toStage(crStage);

        assertThat(stageConfig.name().toLower()).isEqualTo("stagename");
        assertThat(stageConfig.isFetchMaterials()).isTrue();
        assertThat(stageConfig.isCleanWorkingDir()).isTrue();
        assertThat(stageConfig.isArtifactCleanupProhibited()).isTrue();
        assertThat(stageConfig.getVariables().hasVariable("key")).isTrue();
        assertThat(stageConfig.getJobs().size()).isEqualTo(1);
    }

    @Test
    void shouldConvertPipeline() {
        CRPipeline crPipeline = buildPipeline();
        crPipeline.setDisplayOrderWeight(10);

        PipelineConfig pipelineConfig = configConverter.toPipelineConfig(crPipeline, context);
        assertThat(pipelineConfig.name().toLower()).isEqualTo("pipeline");
        assertThat(pipelineConfig.materialConfigs().first() instanceof GitMaterialConfig).isTrue();
        assertThat(pipelineConfig.first().name().toLower()).isEqualTo("stagename");
        assertThat(pipelineConfig.getVariables().hasVariable("key")).isTrue();
        assertThat(pipelineConfig.trackingTool().getLink()).isEqualTo("link");
        assertThat(pipelineConfig.getTimer().getTimerSpec()).isEqualTo("timer");
        assertThat(pipelineConfig.getLabelTemplate()).isEqualTo("label-template");
        assertThat(pipelineConfig.isLockableOnFailure()).isTrue();
        assertThat(pipelineConfig.getDisplayOrderWeight()).isEqualTo(10);
    }

    @Test
    void shouldConvertMinimalPipeline() {
        CRPipeline crPipeline = new CRPipeline();
        crPipeline.setName("p1");
        List<CRStage> min_stages = new ArrayList<>();
        CRStage min_stage = new CRStage();
        min_stage.setName("build");
        Collection<CRJob> min_jobs = new ArrayList<>();
        CRJob job = new CRJob();
        job.setName("buildjob");
        List<CRTask> min_tasks = new ArrayList<>();
        min_tasks.add(new CRBuildTask(CRBuildFramework.rake));
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
        assertThat(pipelineConfig.name().toLower()).isEqualTo("p1");
        assertThat(pipelineConfig.materialConfigs().first() instanceof SvnMaterialConfig).isTrue();
        assertThat(pipelineConfig.first().name().toLower()).isEqualTo("build");
        assertThat(pipelineConfig.getLabelTemplate()).isEqualTo(PipelineLabel.COUNT_TEMPLATE);
    }

    @Test
    void shouldConvertPipelineGroup() {
        List<CRPipeline> pipelines = new ArrayList<>();
        pipelines.add(buildPipeline());
        Map<String, List<CRPipeline>> map = new HashMap<>();
        map.put("group", pipelines);
        Map.Entry<String, List<CRPipeline>> crPipelineGroup = map.entrySet().iterator().next();
        PipelineConfigs pipelineConfigs = configConverter.toBasicPipelineConfigs(crPipelineGroup, context);
        assertThat(pipelineConfigs.getGroup()).isEqualTo("group");
        assertThat(pipelineConfigs.getPipelines().size()).isEqualTo(1);
    }

    @Test
    void shouldConvertPipelineGroupWhenNoName() {
        List<CRPipeline> pipelines = new ArrayList<>();
        CRPipeline pipeline = buildPipeline();
        pipeline.setGroup(null);
        pipelines.add(pipeline);
        Map<String, List<CRPipeline>> map = new HashMap<>();
        map.put(null, pipelines);
        Map.Entry<String, List<CRPipeline>> crPipelineGroup = map.entrySet().iterator().next();
        PipelineConfigs pipelineConfigs = configConverter.toBasicPipelineConfigs(crPipelineGroup, context);
        assertThat(pipelineConfigs.getGroup()).isEqualTo(PipelineConfigs.DEFAULT_GROUP);
        assertThat(pipelineConfigs.getPipelines().size()).isEqualTo(1);
    }

    @Test
    void shouldConvertPipelineGroupWhenEmptyName() {
        List<CRPipeline> pipelines = new ArrayList<>();
        CRPipeline crPipeline = buildPipeline();
        crPipeline.setGroup("");
        pipelines.add(crPipeline);
        Map<String, List<CRPipeline>> map = new HashMap<>();
        map.put("", pipelines);
        Map.Entry<String, List<CRPipeline>> crPipelineGroup = map.entrySet().iterator().next();
        PipelineConfigs pipelineConfigs = configConverter.toBasicPipelineConfigs(crPipelineGroup, context);
        assertThat(pipelineConfigs.getGroup()).isEqualTo(PipelineConfigs.DEFAULT_GROUP);
        assertThat(pipelineConfigs.getPipelines().size()).isEqualTo(1);
    }

    @Test
    void shouldConvertPartialConfigWithGroupsAndEnvironments() {
        CRPipeline pipeline = buildPipeline();
        CREnvironment crEnvironment = new CREnvironment("dev");
        crEnvironment.addEnvironmentVariable("key", "value");
        crEnvironment.addAgent("12");
        crEnvironment.addPipeline("pipename");

        CRParseResult crPartialConfig = new CRParseResult();
        crPartialConfig.getEnvironments().add(crEnvironment);

        crPartialConfig.getPipelines().add(pipeline);

        PartialConfig partialConfig = configConverter.toPartialConfig(crPartialConfig, context);
        assertThat(partialConfig.getGroups().size()).isEqualTo(1);
        assertThat(partialConfig.getEnvironments().size()).isEqualTo(1);
    }

    @Test
    void shouldConvertCRTimerWhenAllAssigned() {
        CRTimer timer = new CRTimer("0 15 * * 6", true);
        TimerConfig result = configConverter.toTimerConfig(timer);
        assertThat(result.getTimerSpec()).isEqualTo("0 15 * * 6");
        assertThat(result.getOnlyOnChanges()).isTrue();
    }

    @Test
    void shouldConvertCRTimerWhenNullOnChanges() {
        CRTimer timer = new CRTimer("0 15 * * 6", false);
        TimerConfig result = configConverter.toTimerConfig(timer);
        assertThat(result.getTimerSpec()).isEqualTo("0 15 * * 6");
        assertThat(result.getOnlyOnChanges()).isFalse();
    }

    @Test
    void shouldFailConvertCRTimerWhenNullSpec() {
        CRTimer timer = new CRTimer(null, false);
        try {
            configConverter.toTimerConfig(timer);
            fail("should have thrown");
        } catch (Exception ex) {
            //ok
        }
    }

    @Test
    void shouldConvertParametersWhenPassed() throws Exception {
        CRPipeline crPipeline = buildPipeline();
        crPipeline.addParameter(new CRParameter("param", "value"));
        PipelineConfig pipeline = configConverter.toPipelineConfig(crPipeline, context);

        assertThat(pipeline.getParams()).isEqualTo(new ParamsConfig(new ParamConfig("param", "value")));
    }

    @Test
    void shouldConvertTemplateNameWhenGiven() throws Exception {
        CRPipeline crPipeline = buildPipeline();
        crPipeline.setTemplate("template");

        PipelineConfig pipeline = configConverter.toPipelineConfig(crPipeline, context);

        assertThat(pipeline.isEmpty()).isTrue();
        assertThat(pipeline.getTemplateName()).isEqualTo(new CaseInsensitiveString("template"));
    }

    @Test
    void shouldConvertParamConfigWhenPassed() throws Exception {
        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("p1");
        pipeline.addParam(new ParamConfig("param", "value"));

        Collection<CRParameter> parameters = new ArrayList<>();
        parameters.add(new CRParameter("param", "value"));
        CRPipeline crPipeline = configConverter.pipelineConfigToCRPipeline(pipeline, "group");
        assertThat(crPipeline.getParameters()).isEqualTo(parameters);
    }

    @Test
    void shouldConvertPipelineConfigToCRPipeline() {
        TrackingTool trackingTool = new TrackingTool();
        trackingTool.setLink("link");
        TimerConfig timerConfig = new TimerConfig("timer", true);
        PipelineConfig pipeline = new PipelineConfig();
        pipeline.setName("p1");
        pipeline.setTimer(timerConfig);
        pipeline.setTrackingTool(trackingTool);
        pipeline.addEnvironmentVariable("testing", "123");
        pipeline.setDisplayOrderWeight(10);

        StageConfig stage = new StageConfig();
        stage.setName(new CaseInsensitiveString("build"));

        JobConfig job = new JobConfig();
        job.setName("buildjob");
        job.setTasks(new Tasks(new RakeTask()));

        stage.setJobs(new JobConfigs(job));
        pipeline.addStageWithoutValidityAssertion(stage);

        SvnMaterialConfig mat = svn();
        mat.setName(new CaseInsensitiveString("mat"));
        mat.setUrl("url");
        pipeline.addMaterialConfig(mat);

        CRPipeline crPipeline = configConverter.pipelineConfigToCRPipeline(pipeline, "group1");
        assertThat(crPipeline.getName()).isEqualTo("p1");
        assertThat(crPipeline.getGroup()).isEqualTo("group1");
        assertThat(crPipeline.getMaterialByName("mat") instanceof CRSvnMaterial).isTrue();
        assertThat(crPipeline.getLabelTemplate()).isEqualTo(PipelineLabel.COUNT_TEMPLATE);
        assertThat(crPipeline.getMaterials().size()).isEqualTo(1);
        assertThat(crPipeline.hasEnvironmentVariable("testing")).isTrue();
        assertThat(crPipeline.getTrackingTool().getLink()).isEqualTo("link");
        assertThat(crPipeline.getTimer().getSpec()).isEqualTo("timer");
        assertThat(crPipeline.getStages().get(0).getName()).isEqualTo("build");
        assertThat(crPipeline.getStages().get(0).getJobs().size()).isEqualTo(1);
        assertThat(crPipeline.getDisplayOrderWeight()).isEqualTo(10);
    }

    @Test
    void shouldConvertStageConfigToCRStage() {
        EnvironmentVariablesConfig envVars = new EnvironmentVariablesConfig();
        envVars.add("testing", "123");

        JobConfig job = new JobConfig();
        job.setName("buildjob");
        job.setTasks(new Tasks(new RakeTask()));

        AdminRole role = new AdminRole(new CaseInsensitiveString("a_role"));
        AdminUser user = new AdminUser(new CaseInsensitiveString("a_user"));
        Approval approval = new Approval();
        approval.addAdmin(user, role);
        approval.setAllowOnlyOnSuccess(true);

        StageConfig stage = new StageConfig(new CaseInsensitiveString("stageName"), new JobConfigs(), approval);
        stage.setVariables(envVars);
        stage.setJobs(new JobConfigs(job));
        stage.setCleanWorkingDir(true);
        stage.setArtifactCleanupProhibited(true);

        CRStage crStage = configConverter.stageToCRStage(stage);
        CRApproval crStageApproval = crStage.getApproval();

        assertThat(crStage.getName()).isEqualTo("stageName");
        assertThat(crStageApproval.getRoles()).contains("a_role");
        assertThat(crStageApproval.getUsers()).contains("a_user");
        assertThat(crStageApproval.isAllowOnlyOnSuccess()).isTrue();
        assertThat(crStage.isFetchMaterials()).isTrue();
        assertThat(crStage.isCleanWorkingDirectory()).isTrue();
        assertThat(crStage.isNeverCleanupArtifacts()).isTrue();
        assertThat(crStage.hasEnvironmentVariable("testing")).isTrue();
        assertThat(crStage.getJobs().size()).isEqualTo(1);
    }

    @Test
    void shouldConvertJobConfigToCRJob() {
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("name"),
                new ResourceConfigs(new ResourceConfig("resource1")),
                new ArtifactTypeConfigs(new BuildArtifactConfig("src", "dest")));
        jobConfig.setRunOnAllAgents(false);
        jobConfig.setTimeout("120");
        jobConfig.addTask(new ExecTask());
        jobConfig.addVariable("key", "value");
        jobConfig.setRunInstanceCount("5");
        jobConfig.addTab("tabname", "path");

        CRJob job = configConverter.jobToCRJob(jobConfig);

        assertThat(job.getName()).isEqualTo("name");
        assertThat(job.hasEnvironmentVariable("key")).isTrue();
        assertThat(job.getTabs().contains(new CRTab("tabname", "path"))).isTrue();
        assertThat(job.getResources().contains("resource1")).isTrue();
        assertThat(job.getArtifacts().contains(new CRBuiltInArtifact("src", "dest", CRArtifactType.build))).isTrue();
        assertThat(job.isRunOnAllAgents()).isFalse();
        assertThat(job.getRunInstanceCount()).isEqualTo(5);
        assertThat(job.getTimeout()).isEqualTo(120);
        assertThat(job.getTasks().size()).isEqualTo(1);
    }

    @Test
    void shouldConvertJobConfigToCRJobWithNullTimeouBeingZerot() {
        JobConfig jobConfig = new JobConfig(new CaseInsensitiveString("name"),
                new ResourceConfigs(new ResourceConfig("resource1")),
                new ArtifactTypeConfigs(new BuildArtifactConfig("src", "dest")));
        jobConfig.setRunOnAllAgents(false);
        jobConfig.addTask(new ExecTask());

        CRJob job = configConverter.jobToCRJob(jobConfig);

        assertThat(job.getTimeout()).isEqualTo(0);
    }

    @Test
    void shouldConvertDependencyMaterialConfigToCRDependencyMaterial() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("name"), new CaseInsensitiveString("pipe"), new CaseInsensitiveString("stage"));

        CRDependencyMaterial crDependencyMaterial =
                (CRDependencyMaterial) configConverter.materialToCRMaterial(dependencyMaterialConfig);

        assertThat(crDependencyMaterial.getName()).isEqualTo("name");
        assertThat(crDependencyMaterial.getPipeline()).isEqualTo("pipe");
        assertThat(crDependencyMaterial.getStage()).isEqualTo("stage");
    }

    @Test
    void shouldConvertGitMaterialConfigToCRGitMaterial() {
        GitMaterialConfig gitMaterialConfig = git("url", "branch", true);
        gitMaterialConfig.setName(new CaseInsensitiveString("name"));
        gitMaterialConfig.setFolder("folder");
        gitMaterialConfig.setAutoUpdate(true);
        gitMaterialConfig.setInvertFilter(false);
        gitMaterialConfig.setFilter(Filter.create("filter"));

        CRGitMaterial crGitMaterial =
                (CRGitMaterial) configConverter.materialToCRMaterial(gitMaterialConfig);

        assertThat(crGitMaterial.getName()).isEqualTo("name");
        assertThat(crGitMaterial.getDestination()).isEqualTo("folder");
        assertThat(crGitMaterial.isAutoUpdate()).isTrue();
        assertThat(crGitMaterial.isWhitelist()).isFalse();
        assertThat(crGitMaterial.getFilterList()).contains("filter");
        assertThat(crGitMaterial.getUrl()).isEqualTo("url");
        assertThat(crGitMaterial.getBranch()).isEqualTo("branch");
        assertThat(crGitMaterial.isShallowClone()).isTrue();
    }

    @Test
    void shouldConvertGitMaterialConfigToCRGitMaterialWhenPlainPassword() throws CryptoException {
        GitMaterialConfig gitMaterialConfig = git("url", "branch", true);
        gitMaterialConfig.setName(new CaseInsensitiveString("name"));
        gitMaterialConfig.setFolder("folder");
        gitMaterialConfig.setAutoUpdate(true);
        gitMaterialConfig.setInvertFilter(false);
        gitMaterialConfig.setFilter(Filter.create("filter"));
        gitMaterialConfig.setPassword("secret");

        CRGitMaterial crGitMaterial = (CRGitMaterial) configConverter.materialToCRMaterial(gitMaterialConfig);

        assertThat(crGitMaterial.getName()).isEqualTo("name");
        assertThat(crGitMaterial.getDestination()).isEqualTo("folder");
        assertThat(crGitMaterial.isAutoUpdate()).isTrue();
        assertThat(crGitMaterial.isWhitelist()).isFalse();
        assertThat(crGitMaterial.getFilterList()).contains("filter");
        assertThat(crGitMaterial.getUrl()).isEqualTo("url");
        assertThat(crGitMaterial.getBranch()).isEqualTo("branch");
        assertThat(crGitMaterial.isShallowClone()).isTrue();
        assertThat(crGitMaterial.getPassword()).isNull();
        assertThat(crGitMaterial.getEncryptedPassword()).isEqualTo(new GoCipher().encrypt("secret"));
    }

    @Test
    void shouldConvertGitMaterialConfigToCRGitMaterialWhenEncryptedPassword() throws CryptoException {
        GitMaterialConfig gitMaterialConfig = git("url", "branch", true);
        gitMaterialConfig.setName(new CaseInsensitiveString("name"));
        gitMaterialConfig.setFolder("folder");
        gitMaterialConfig.setAutoUpdate(true);
        gitMaterialConfig.setInvertFilter(false);
        gitMaterialConfig.setFilter(Filter.create("filter"));
        gitMaterialConfig.setEncryptedPassword(new GoCipher().encrypt("secret"));

        CRGitMaterial crGitMaterial = (CRGitMaterial) configConverter.materialToCRMaterial(gitMaterialConfig);

        assertThat(crGitMaterial.getName()).isEqualTo("name");
        assertThat(crGitMaterial.getDestination()).isEqualTo("folder");
        assertThat(crGitMaterial.isAutoUpdate()).isTrue();
        assertThat(crGitMaterial.isWhitelist()).isFalse();
        assertThat(crGitMaterial.getFilterList()).contains("filter");
        assertThat(crGitMaterial.getUrl()).isEqualTo("url");
        assertThat(crGitMaterial.getBranch()).isEqualTo("branch");
        assertThat(crGitMaterial.isShallowClone()).isTrue();
        assertThat(crGitMaterial.getEncryptedPassword()).isEqualTo(new GoCipher().encrypt("secret"));
    }

    @Test
    void shouldConvertGitMaterialConfigWhenNulls() {
        GitMaterialConfig gitMaterialConfig = git();
        gitMaterialConfig.setUrl("url");

        CRGitMaterial crGitMaterial =
                (CRGitMaterial) configConverter.materialToCRMaterial(gitMaterialConfig);

        assertThat(crGitMaterial.getName()).isNull();
        assertThat(crGitMaterial.getDestination()).isNull();
        assertThat(crGitMaterial.isAutoUpdate()).isTrue();
        assertThat(crGitMaterial.isShallowClone()).isFalse();
        assertThat(crGitMaterial.getUrl()).isEqualTo("url");
        assertThat(crGitMaterial.getBranch()).isEqualTo("master");
    }

    @Test
    void shouldConvertHgMaterialConfigToCRHgMaterial() {
        HgMaterialConfig hgMaterialConfig = hg("url", "folder");
        hgMaterialConfig.setName(new CaseInsensitiveString("name"));
        hgMaterialConfig.setFilter(Filter.create("filter"));
        hgMaterialConfig.setAutoUpdate(true);

        CRHgMaterial crHgMaterial =
                (CRHgMaterial) configConverter.materialToCRMaterial(hgMaterialConfig);

        assertThat(crHgMaterial.getName()).isEqualTo("name");
        assertThat(crHgMaterial.getDestination()).isEqualTo("folder");
        assertThat(crHgMaterial.isAutoUpdate()).isTrue();
        assertThat(crHgMaterial.getFilterList()).contains("filter");
        assertThat(crHgMaterial.getUrl()).isEqualTo("url");
    }

    @Test
    void shouldConvertHgMaterialConfigToCRHgMaterialWhenPlainPassword() throws CryptoException {
        HgMaterialConfig hgMaterialConfig = hg("url", "folder");
        hgMaterialConfig.setName(new CaseInsensitiveString("name"));
        hgMaterialConfig.setFilter(Filter.create("filter"));
        hgMaterialConfig.setAutoUpdate(true);
        hgMaterialConfig.setPassword("secret");

        CRHgMaterial crHgMaterial = (CRHgMaterial) configConverter.materialToCRMaterial(hgMaterialConfig);

        assertThat(crHgMaterial.getName()).isEqualTo("name");
        assertThat(crHgMaterial.getDestination()).isEqualTo("folder");
        assertThat(crHgMaterial.isAutoUpdate()).isTrue();
        assertThat(crHgMaterial.getFilterList()).contains("filter");
        assertThat(crHgMaterial.getUrl()).isEqualTo("url");
        assertThat(crHgMaterial.getPassword()).isNull();
        assertThat(crHgMaterial.getEncryptedPassword()).isEqualTo(new GoCipher().encrypt("secret"));
    }

    @Test
    void shouldConvertHgMaterialConfigToCRHgMaterialWhenEncryptedPassword() throws CryptoException {
        HgMaterialConfig hgMaterialConfig = hg("url", "folder");
        hgMaterialConfig.setName(new CaseInsensitiveString("name"));
        hgMaterialConfig.setFilter(Filter.create("filter"));
        hgMaterialConfig.setAutoUpdate(true);
        hgMaterialConfig.setEncryptedPassword(new GoCipher().encrypt("secret"));

        CRHgMaterial crHgMaterial = (CRHgMaterial) configConverter.materialToCRMaterial(hgMaterialConfig);

        assertThat(crHgMaterial.getName()).isEqualTo("name");
        assertThat(crHgMaterial.getDestination()).isEqualTo("folder");
        assertThat(crHgMaterial.isAutoUpdate()).isTrue();
        assertThat(crHgMaterial.getFilterList()).contains("filter");
        assertThat(crHgMaterial.getUrl()).isEqualTo("url");
        assertThat(crHgMaterial.getEncryptedPassword()).isEqualTo(new GoCipher().encrypt("secret"));
    }

    @Test
    void shouldConvertHgMaterialConfigWhenNullName() {
        HgMaterialConfig hgMaterialConfig = hg("url", "folder");
        hgMaterialConfig.setFilter(Filter.create("filter"));
        hgMaterialConfig.setAutoUpdate(true);

        CRHgMaterial crHgMaterial =
                (CRHgMaterial) configConverter.materialToCRMaterial(hgMaterialConfig);

        assertThat(crHgMaterial.getName()).isNull();
        assertThat(crHgMaterial.getDestination()).isEqualTo("folder");
        assertThat(crHgMaterial.isAutoUpdate()).isTrue();
        assertThat(crHgMaterial.getFilterList()).contains("filter");
        assertThat(crHgMaterial.getUrl()).isEqualTo("url");
    }

    @Test
    void shouldConvertP4MaterialConfigWhenEncryptedPassword() {
        P4MaterialConfig p4MaterialConfig = p4("server:port", "view");
        p4MaterialConfig.setName(new CaseInsensitiveString("name"));
        p4MaterialConfig.setFolder("folder");
        p4MaterialConfig.setEncryptedPassword("plain-text-password");
        p4MaterialConfig.setFilter(Filter.create("filter"));
        p4MaterialConfig.setUserName("user");
        p4MaterialConfig.setUseTickets(true);
        p4MaterialConfig.setAutoUpdate(false);

        CRP4Material crp4Material = (CRP4Material) configConverter.materialToCRMaterial(p4MaterialConfig);

        assertThat(crp4Material.getName()).isEqualTo("name");
        assertThat(crp4Material.getDestination()).isEqualTo("folder");
        assertThat(crp4Material.isAutoUpdate()).isFalse();
        assertThat(crp4Material.getFilterList()).contains("filter");
        assertThat(crp4Material.getPort()).isEqualTo("server:port");
        assertThat(crp4Material.getUsername()).isEqualTo("user");
        assertThat(crp4Material.getEncryptedPassword()).isEqualTo("plain-text-password");
        assertThat(crp4Material.getPassword()).isNull();
        assertThat(crp4Material.isUseTickets()).isTrue();
        assertThat(crp4Material.getView()).isEqualTo("view");
    }

    @Test
    void shouldConvertP4MaterialConfigWhenPlainPassword() {
        P4MaterialConfig p4MaterialConfig = p4("server:port", "view");
        p4MaterialConfig.setName(new CaseInsensitiveString("name"));
        p4MaterialConfig.setFolder("folder");
        p4MaterialConfig.setPassword("password");
        p4MaterialConfig.setFilter(Filter.create("filter"));
        p4MaterialConfig.setUserName("user");
        p4MaterialConfig.setUseTickets(false);
        p4MaterialConfig.setAutoUpdate(false);

        CRP4Material crp4Material = (CRP4Material) configConverter.materialToCRMaterial(p4MaterialConfig);

        assertThat(crp4Material.getName()).isEqualTo("name");
        assertThat(crp4Material.getDestination()).isEqualTo("folder");
        assertThat(crp4Material.isAutoUpdate()).isFalse();
        assertThat(crp4Material.getFilterList()).contains("filter");
        assertThat(crp4Material.getPort()).isEqualTo("server:port");
        assertThat(crp4Material.getUsername()).isEqualTo("user");
        assertThat(crp4Material.isUseTickets()).isFalse();
        assertThat(crp4Material.getView()).isEqualTo("view");
    }

    @Test
    void shouldConvertSvmMaterialConfigWhenEncryptedPassword() throws CryptoException {
        String encryptedPassword = new GoCipher().encrypt("plain-text-password");
        SvnMaterialConfig svnMaterialConfig = svn("url", true);
        svnMaterialConfig.setName(new CaseInsensitiveString("name"));
        svnMaterialConfig.setEncryptedPassword(encryptedPassword);
        svnMaterialConfig.setFolder("folder");
        svnMaterialConfig.setFilter(Filter.create("filter"));
        svnMaterialConfig.setUserName("username");

        CRSvnMaterial crSvnMaterial = (CRSvnMaterial) configConverter.materialToCRMaterial(svnMaterialConfig);

        assertThat(crSvnMaterial.getName()).isEqualTo("name");
        assertThat(crSvnMaterial.getDestination()).isEqualTo("folder");
        assertThat(crSvnMaterial.isAutoUpdate()).isTrue();
        assertThat(crSvnMaterial.getFilterList()).contains("filter");
        assertThat(crSvnMaterial.getUrl()).isEqualTo("url");
        assertThat(crSvnMaterial.getUsername()).isEqualTo("username");
        assertThat(crSvnMaterial.getEncryptedPassword()).isEqualTo(encryptedPassword);
        assertThat(crSvnMaterial.getPassword()).isNull();
        assertThat(crSvnMaterial.isCheckExternals()).isTrue();
    }

    @Test
    void shouldConvertSvmMaterialConfigWhenPlainPassword() {
        SvnMaterialConfig svnMaterialConfig = svn("url", true);
        svnMaterialConfig.setName(new CaseInsensitiveString("name"));
        svnMaterialConfig.setPassword("pass");
        svnMaterialConfig.setFolder("folder");
        svnMaterialConfig.setFilter(Filter.create("filter"));
        svnMaterialConfig.setUserName("username");

        CRSvnMaterial crSvnMaterial =
                (CRSvnMaterial) configConverter.materialToCRMaterial(svnMaterialConfig);

        assertThat(crSvnMaterial.getName()).isEqualTo("name");
        assertThat(crSvnMaterial.getDestination()).isEqualTo("folder");
        assertThat(crSvnMaterial.isAutoUpdate()).isTrue();
        assertThat(crSvnMaterial.getFilterList()).contains("filter");
        assertThat(crSvnMaterial.getUrl()).isEqualTo("url");
        assertThat(crSvnMaterial.getUsername()).isEqualTo("username");
        assertThat(crSvnMaterial.getPassword()).isNull();
        assertThat(crSvnMaterial.isCheckExternals()).isTrue();
    }

    @Test
    void shouldConvertTfsMaterialConfigWhenPlainPassword() {
        TfsMaterialConfig tfsMaterialConfig = tfs();
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

        assertThat(crTfsMaterial.getName()).isEqualTo("name");
        assertThat(crTfsMaterial.getDestination()).isEqualTo("folder");
        assertThat(crTfsMaterial.isAutoUpdate()).isFalse();
        assertThat(crTfsMaterial.getFilterList()).contains("filter");
        assertThat(crTfsMaterial.getUrl()).isEqualTo("url");
        assertThat(crTfsMaterial.getUsername()).isEqualTo("user");
        assertThat(crTfsMaterial.getPassword()).isNull();
        assertThat(crTfsMaterial.getDomain()).isEqualTo("domain");
        assertThat(crTfsMaterial.getProject()).isEqualTo("project");
    }

    @Test
    void shouldConvertTfsMaterialConfigWhenEncryptedPassword() {
        TfsMaterialConfig tfsMaterialConfig = tfs();
        tfsMaterialConfig.setUrl("url");
        tfsMaterialConfig.setDomain("domain");
        tfsMaterialConfig.setProjectPath("project");
        tfsMaterialConfig.setName(new CaseInsensitiveString("name"));
        tfsMaterialConfig.setEncryptedPassword("plain-text-password");
        tfsMaterialConfig.setFolder("folder");
        tfsMaterialConfig.setAutoUpdate(false);
        tfsMaterialConfig.setFilter(Filter.create("filter"));
        tfsMaterialConfig.setUserName("user");

        CRTfsMaterial crTfsMaterial =
                (CRTfsMaterial) configConverter.materialToCRMaterial(tfsMaterialConfig);

        assertThat(crTfsMaterial.getName()).isEqualTo("name");
        assertThat(crTfsMaterial.getDestination()).isEqualTo("folder");
        assertThat(crTfsMaterial.isAutoUpdate()).isFalse();
        assertThat(crTfsMaterial.getFilterList()).contains("filter");
        assertThat(crTfsMaterial.getUrl()).isEqualTo("url");
        assertThat(crTfsMaterial.getUsername()).isEqualTo("user");
        assertThat(crTfsMaterial.getEncryptedPassword()).isEqualTo("plain-text-password");
        assertThat(crTfsMaterial.getPassword()).isNull();
        assertThat(crTfsMaterial.getDomain()).isEqualTo("domain");
        assertThat(crTfsMaterial.getProject()).isEqualTo("project");
    }

    @Test
    void shouldConvertPluggableScmMaterialConfig() {
        SCM myscm = new SCM("scmid", new PluginConfiguration(), new Configuration());
        SCMs scms = new SCMs(myscm);

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.setSCMs(scms);
        when(cachedGoConfig.currentConfig()).thenReturn(cruiseConfig);

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(new CaseInsensitiveString("name"), myscm, "directory", Filter.create("filter"));

        CRPluggableScmMaterial crPluggableScmMaterial =
                (CRPluggableScmMaterial) configConverter.materialToCRMaterial(pluggableSCMMaterialConfig);

        assertThat(crPluggableScmMaterial.getName()).isEqualTo("name");
        assertThat(crPluggableScmMaterial.getScmId()).isEqualTo("scmid");
        assertThat(crPluggableScmMaterial.getDestination()).isEqualTo("directory");
        assertThat(crPluggableScmMaterial.getFilterList()).contains("filter");
    }

    @Test
    void shouldConvertPackageMaterialConfig() {
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

        assertThat(crPackageMaterial.getName()).isEqualTo("name");
        assertThat(crPackageMaterial.getPackageId()).isEqualTo("package-id");
    }

    @Test
    void shouldConvertEnvironmentVariableConfigWhenNotSecure() {
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig("key1", "value");
        CREnvironmentVariable result = configConverter.environmentVariableConfigToCREnvironmentVariable(environmentVariableConfig);
        assertThat(result.getValue()).isEqualTo("value");
        assertThat(result.getName()).isEqualTo("key1");
        assertThat(result.hasEncryptedValue()).isFalse();
    }

    @Test
    void shouldConvertNullEnvironmentVariableConfigWhenNotSecure() {
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig("key1", null);
        CREnvironmentVariable result = configConverter.environmentVariableConfigToCREnvironmentVariable(environmentVariableConfig);
        assertThat(result.getValue()).isEqualTo("");
        assertThat(result.getName()).isEqualTo("key1");
        assertThat(result.hasEncryptedValue()).isFalse();
    }

    @Test
    void shouldConvertEnvironmentVariableConfigWhenSecure() throws CryptoException {
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig("key1", null);
        environmentVariableConfig.setIsSecure(true);
        String encryptedValue = new GoCipher().encrypt("plain-text-password");
        environmentVariableConfig.setEncryptedValue(encryptedValue);
        CREnvironmentVariable result = configConverter.environmentVariableConfigToCREnvironmentVariable(environmentVariableConfig);
        assertThat(result.hasEncryptedValue()).isTrue();
        assertThat(result.getEncryptedValue()).isEqualTo(encryptedValue);
        assertThat(result.getValue()).isNull();
        assertThat(result.getName()).isEqualTo("key1");
    }

    @Test
    void shouldMigratePluggableTasktoCR() {
        ArrayList<CRConfigurationProperty> configs = new ArrayList<>();
        configs.add(new CRConfigurationProperty("k", "m", null));

        ConfigurationProperty prop = new ConfigurationProperty(new ConfigurationKey("k"), new ConfigurationValue("m"));
        Configuration config = new Configuration(prop);

        PluggableTask pluggableTask = new PluggableTask(
                new PluginConfiguration("myplugin", "1"),
                config);
        pluggableTask.setConditions(new RunIfConfigs(RunIfConfig.ANY));

        CRPluggableTask result = (CRPluggableTask) configConverter.taskToCRTask(pluggableTask);

        assertThat(result.getPluginConfiguration().getId()).isEqualTo("myplugin");
        assertThat(result.getPluginConfiguration().getVersion()).isEqualTo("1");
        assertThat(result.getConfiguration()).contains(new CRConfigurationProperty("k", "m", null));
        assertThat(result.getRunIf()).isEqualTo(CRRunIf.any);
    }

    @Test
    void shouldMigrateRakeTaskToCR() {
        RakeTask rakeTask = new RakeTask();
        rakeTask.setBuildFile("Rakefile.rb");
        rakeTask.setWorkingDirectory("src");
        rakeTask.setTarget("build");
        rakeTask.setConditions(new RunIfConfigs(RunIfConfig.FAILED));

        CRBuildTask result = (CRBuildTask) configConverter.taskToCRTask(rakeTask);

        assertThat(result.getType()).isEqualTo(CRBuildFramework.rake);
        assertThat(result.getBuildFile()).isEqualTo("Rakefile.rb");
        assertThat(result.getTarget()).isEqualTo("build");
        assertThat(result.getWorkingDirectory()).isEqualTo("src");
        assertThat(result.getRunIf()).isEqualTo(CRRunIf.failed);
    }

    @Test
    void shouldMigrateAntTaskToCR() {
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

        assertThat(result.getRunIf()).isEqualTo(CRRunIf.failed);
        assertThat(result.getBuildFile()).isEqualTo("ant");
        assertThat(result.getTarget()).isEqualTo("build");
        assertThat(result.getWorkingDirectory()).isEqualTo("src");
        assertThat(onCancel.getType()).isEqualTo(CRBuildFramework.rake);
        assertThat(onCancel.getBuildFile()).isEqualTo("Rakefile.rb");
        assertThat(onCancel.getTarget()).isEqualTo("build");
        assertThat(onCancel.getWorkingDirectory()).isEqualTo("src");
        assertThat(onCancel.getRunIf()).isEqualTo(CRRunIf.failed);
    }

    @Test
    void shouldMigrateNantTaskToCR() {
        NantTask nantTask = new NantTask();
        nantTask.setBuildFile("nant");
        nantTask.setWorkingDirectory("src");
        nantTask.setTarget("build");
        nantTask.setNantPath("path");
        nantTask.setConditions(new RunIfConfigs(RunIfConfig.PASSED));

        CRNantTask result = (CRNantTask) configConverter.taskToCRTask(nantTask);

        assertThat(result.getRunIf()).isEqualTo(CRRunIf.passed);
        assertThat(result.getBuildFile()).isEqualTo("nant");
        assertThat(result.getTarget()).isEqualTo("build");
        assertThat(result.getWorkingDirectory()).isEqualTo("src");
        assertThat(result.getNantPath()).isEqualTo("path");
    }

    @Test
    void shouldConvertExecTaskWhenCancelIsNotSpecifiedToCR() {
        ExecTask execTask = new ExecTask("bash",
                new Arguments(new Argument("1"), new Argument("2")),
                "work");
        execTask.setConditions(new RunIfConfigs(RunIfConfig.FAILED));
        execTask.setTimeout(120L);
        CRExecTask result = (CRExecTask) configConverter.taskToCRTask(execTask);

        assertThat(result.getRunIf()).isEqualTo(CRRunIf.failed);
        assertThat(result.getCommand()).isEqualTo("bash");
        assertThat(result.getArguments()).contains("1");
        assertThat(result.getArguments()).contains("2");
        assertThat(result.getWorkingDirectory()).isEqualTo("work");
        assertThat(result.getTimeout()).isEqualTo(120L);
        assertThat(result.getOnCancel()).isNull();
    }

    @Test
    void shouldConvertExecTaskWhenOldArgsAreUsed() {
        ExecTask execTask = new ExecTask("bash",
                "1 2 \"file name\"",
                "work");
        execTask.setConditions(new RunIfConfigs(RunIfConfig.FAILED));
        execTask.setTimeout(120L);
        CRExecTask result = (CRExecTask) configConverter.taskToCRTask(execTask);

        assertThat(result.getRunIf()).isEqualTo(CRRunIf.failed);
        assertThat(result.getCommand()).isEqualTo("bash");
        assertThat(result.getArguments()).contains("1");
        assertThat(result.getArguments()).contains("2");
        assertThat(result.getArguments()).contains("file name");
        assertThat(result.getWorkingDirectory()).isEqualTo("work");
        assertThat(result.getTimeout()).isEqualTo(120L);
        assertThat(result.getOnCancel()).isNull();
    }

    @Test
    void shouldConvertExecTaskWhenCancelIsSpecifiedToCR() {
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

        assertThat(result.getRunIf()).isEqualTo(CRRunIf.failed);
        assertThat(result.getCommand()).isEqualTo("bash");
        assertThat(result.getArguments()).contains("1");
        assertThat(result.getArguments()).contains("2");
        assertThat(result.getWorkingDirectory()).isEqualTo("work");
        assertThat(result.getTimeout()).isEqualTo(120L);
        assertThat(crOnCancel.getCommand()).isEqualTo("kill");
    }

    @Test
    void shouldConvertFetchArtifactTaskWhenSourceIsDirectoryToCR() {
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

        assertThat(result.getRunIf()).isEqualTo(CRRunIf.failed);
        assertThat(result.getDestination()).isEqualTo("dest");
        assertThat(result.getJob()).isEqualTo("job");
        assertThat(result.getPipeline()).isEqualTo("upstream");
        assertThat(result.getSource()).isEqualTo("src");
        assertThat(result.sourceIsDirectory()).isTrue();
    }

    @Test
    void shouldConvertFetchArtifactTaskWhenSourceIsFileToCR() {
        FetchTask fetchTask = new FetchTask(
                new CaseInsensitiveString("upstream"),
                new CaseInsensitiveString("stage"),
                new CaseInsensitiveString("job"),
                "src",
                "dest"
        );
        fetchTask.setConditions(new RunIfConfigs(RunIfConfig.FAILED));

        CRFetchArtifactTask result = (CRFetchArtifactTask) configConverter.taskToCRTask(fetchTask);

        assertThat(result.getRunIf()).isEqualTo(CRRunIf.failed);
        assertThat(result.getDestination()).isEqualTo("dest");
        assertThat(result.getJob()).isEqualTo("job");
        assertThat(result.getPipeline()).isEqualTo("upstream");
        assertThat(result.getSource()).isEqualTo("src");
        assertThat(result.sourceIsDirectory()).isFalse();
    }

    @Test
    void shouldConvertFetchPluggableArtifactTaskToCRFetchPluggableArtifactTask() {
        FetchPluggableArtifactTask fetchPluggableArtifactTask = new FetchPluggableArtifactTask(
                new CaseInsensitiveString("upstream"),
                new CaseInsensitiveString("stage"),
                new CaseInsensitiveString("job"),
                "artifactId");
        fetchPluggableArtifactTask.setConditions(new RunIfConfigs(RunIfConfig.PASSED));

        CRFetchPluggableArtifactTask result = (CRFetchPluggableArtifactTask) configConverter.taskToCRTask(fetchPluggableArtifactTask);

        assertThat(result.getRunIf()).isEqualTo(CRRunIf.passed);
        assertThat(result.getJob()).isEqualTo("job");
        assertThat(result.getPipeline()).isEqualTo("upstream");
        assertThat(result.getStage()).isEqualTo("stage");
        assertThat(result.getArtifactId()).isEqualTo("artifactId");
        assertThat(result.getConfiguration().isEmpty()).isTrue();
    }
}
