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

package com.thoughtworks.go.server.service;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException;
import com.thoughtworks.go.config.merge.MergeEnvironmentConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.helper.EnvironmentConfigMother;
import com.thoughtworks.go.presentation.environment.EnvironmentPipelineModel;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.*;

import static com.thoughtworks.go.helper.EnvironmentConfigMother.*;
import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class EnvironmentConfigServiceTest {
    private GoConfigService mockGoConfigService;
    private EnvironmentConfigService environmentConfigService;
    private SecurityService securityService;

    @Before
    public void setUp() throws Exception {
        mockGoConfigService = mock(GoConfigService.class);
        securityService = mock(SecurityService.class);
        EntityHashingService entityHashingService = mock(EntityHashingService.class);
        environmentConfigService = new EnvironmentConfigService(mockGoConfigService, securityService, entityHashingService);
    }

    @Test
    public void shouldRegisterAsACruiseConfigChangeListener() throws Exception {
        environmentConfigService.initialize();
        Mockito.verify(mockGoConfigService).register(environmentConfigService);
    }

    @Test
    public void shouldGetEditablePartOfEnvironmentConfig() throws Exception {
        String uat = "uat";

        BasicEnvironmentConfig local = new BasicEnvironmentConfig(new CaseInsensitiveString("uat"));
        local.addEnvironmentVariable("user", "admin");
        BasicEnvironmentConfig remote = new BasicEnvironmentConfig(new CaseInsensitiveString("uat"));
        remote.addEnvironmentVariable("foo", "bar");
        MergeEnvironmentConfig merged = new MergeEnvironmentConfig(local, remote);

        EnvironmentsConfig environments = new EnvironmentsConfig();
        environments.add(merged);

        environmentConfigService.sync(environments);

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        BasicEnvironmentConfig env = (BasicEnvironmentConfig) environmentConfigService.named(uat).getLocal();
        cruiseConfig.addEnvironment(env);
        BasicEnvironmentConfig expectedToEdit = new Cloner().deepClone(env);

        when(mockGoConfigService.getConfigForEditing()).thenReturn(cruiseConfig);

        assertThat(environmentConfigService.getEnvironmentForEdit(uat), is(expectedToEdit));
    }

    @Test
    public void shouldReturnAllTheLocalEnvironments() throws Exception {
        String uat = "uat";

        BasicEnvironmentConfig local = new BasicEnvironmentConfig(new CaseInsensitiveString("uat"));
        local.addEnvironmentVariable("user", "admin");
        BasicEnvironmentConfig remote = new BasicEnvironmentConfig(new CaseInsensitiveString("uat"));
        remote.addEnvironmentVariable("foo", "bar");
        MergeEnvironmentConfig merged = new MergeEnvironmentConfig(local, remote);

        EnvironmentsConfig environments = new EnvironmentsConfig();
        environments.add(merged);

        environmentConfigService.sync(environments);

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        BasicEnvironmentConfig env = (BasicEnvironmentConfig) environmentConfigService.named(uat).getLocal();
        cruiseConfig.addEnvironment(env);
        List<BasicEnvironmentConfig> expectedToEdit = Arrays.asList(new Cloner().deepClone(env));

        when(mockGoConfigService.getConfigForEditing()).thenReturn(cruiseConfig);

        assertThat(environmentConfigService.getAllLocalEnvironments(), is(expectedToEdit));
    }

    @Test
    public void shouldReturnAllTheMergedEnvironments() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        CruiseConfig config = new BasicCruiseConfig();
        BasicEnvironmentConfig env = new BasicEnvironmentConfig(new CaseInsensitiveString("foo"));
        env.addPipeline(new CaseInsensitiveString("bar"));
        env.addAgent("baz");
        env.addEnvironmentVariable("quux", "bang");
        EnvironmentsConfig environments = config.getEnvironments();
        environments.add(env);
        environmentConfigService.sync(environments);
        when(mockGoConfigService.getMergedConfigForEditing()).thenReturn(config);

        assertThat(environmentConfigService.getAllMergedEnvironments(), Is.is(Arrays.asList(env)));
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldFilterWhenAgentIsNotInAnEnvironment() throws Exception {
        environmentConfigService.sync(environments("uat", "prod"));

        List<JobPlan> filtered = environmentConfigService.filterJobsByAgent(jobs("no-env", "uat", "prod"), "no-env-uuid");

        assertThat(filtered.size(), is(1));
        assertThat(filtered.get(0).getPipelineName(), is("no-env-pipeline"));
    }

    @Test
    public void shouldFilterWhenAgentIsInTheSameEnvironment() throws Exception {
        environmentConfigService.sync(environments("uat", "prod"));

        List<JobPlan> filtered = environmentConfigService.filterJobsByAgent(jobs("no-env", "uat", "prod"), "uat-agent");

        assertThat(filtered.size(), is(1));
        assertThat(filtered.get(0).getPipelineName(), is("uat-pipeline"));
    }

    @Test
    public void shouldFilterWhenAgentIsInMultipleEnvironments() throws Exception {
        environmentConfigService.sync(environments("uat", "prod"));

        List<JobPlan> filtered = environmentConfigService.filterJobsByAgent(jobs("no-env", "uat", "prod"), EnvironmentConfigMother.OMNIPRESENT_AGENT);

        assertThat(filtered.size(), is(2));
        assertThat(filtered.get(0).getPipelineName(), is("uat-pipeline"));
        assertThat(filtered.get(1).getPipelineName(), is("prod-pipeline"));
    }

    @Test
    public void shouldFilterWhenAgentIsInAnotherEnvironment() throws Exception {
        environmentConfigService.sync(environments("uat", "prod"));

        List<JobPlan> filtered = environmentConfigService.filterJobsByAgent(jobs("no-env", "prod"), "uat-agent");

        assertThat(filtered.size(), is(0));
    }

    @Test
    public void shouldFindPipelinesNamesForAGivenEnvironmentName() throws Exception {
        environmentConfigService.sync(environments("uat", "prod"));
        assertThat(environmentConfigService.pipelinesFor(new CaseInsensitiveString("uat")).size(), is(1));
        assertThat(environmentConfigService.pipelinesFor(new CaseInsensitiveString("uat")), hasItem(new CaseInsensitiveString("uat-pipeline")));
        assertThat(environmentConfigService.pipelinesFor(new CaseInsensitiveString("prod")), hasItem(new CaseInsensitiveString("prod-pipeline")));
    }

    @Test
    public void shouldFindAgentsForPipelineUnderEnvironment() throws Exception {
        environmentConfigService.sync(environments("uat", "prod"));
        AgentConfig agentUnderEnv = new AgentConfig("uat-agent", "localhost", "127.0.0.1");
        AgentConfig omnipresentAgent = new AgentConfig(EnvironmentConfigMother.OMNIPRESENT_AGENT, "localhost", "127.0.0.2");

        Mockito.when(mockGoConfigService.agentByUuid("uat-agent")).thenReturn(agentUnderEnv);
        Mockito.when(mockGoConfigService.agentByUuid(EnvironmentConfigMother.OMNIPRESENT_AGENT)).thenReturn(omnipresentAgent);

        assertThat(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("uat-pipeline")).size(), is(2));
        assertThat(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("uat-pipeline")), hasItem(agentUnderEnv));
        assertThat(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("uat-pipeline")), hasItem(omnipresentAgent));
    }

    @Test
    public void shouldFindAgentsForPipelineUnderNoEnvironment() throws Exception {
        environmentConfigService.sync(environments("uat", "prod"));
        AgentConfig noEnvAgent = new AgentConfig("no-env-agent", "localhost", "127.0.0.1");

        Agents agents = new Agents();
        agents.add(noEnvAgent);
        agents.add(new AgentConfig(EnvironmentConfigMother.OMNIPRESENT_AGENT, "localhost", "127.0.0.2"));
        Mockito.when(mockGoConfigService.agents()).thenReturn(agents);


        assertThat(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("no-env-pipeline")).size(), is(1));
        assertThat(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("no-env-pipeline")), hasItem(noEnvAgent));
    }

    @Test
    public void shouldListEnvironmentVariablesDefinedForAnEnvironment() throws Exception {
        EnvironmentsConfig environmentConfigs = new EnvironmentsConfig();
        BasicEnvironmentConfig environment = environment("uat");
        environment.addEnvironmentVariable("Var1", "Value1");
        environment.addEnvironmentVariable("Var2", "Value2");
        environmentConfigs.add(environment);
        environmentConfigService.sync(environmentConfigs);

        EnvironmentVariableContext environmentVariableContext = environmentConfigService.environmentVariableContextFor("uat-pipeline");

        assertThat(environmentVariableContext.getProperties().size(), is(3));
        assertThat(environmentVariableContext.getProperty("GO_ENVIRONMENT_NAME"), is("uat"));
        assertThat(environmentVariableContext.getProperty("Var1"), is("Value1"));
        assertThat(environmentVariableContext.getProperty("Var2"), is("Value2"));
        assertNull(environmentConfigService.environmentVariableContextFor("non-existent-pipeline"));
    }

    @Test
    public void shouldReturnNullEnvironmentVariablesIfThePipelineDoesNotBelongToAnEnvironment() throws Exception {
        environmentConfigService.sync(environments("uat", "prod"));
        assertNull(environmentConfigService.environmentVariableContextFor("pipeline-with-no-environment"));
    }

    @Test
    public void shouldListAllEnvironmentVariablesDefinedForAConfigRepoEnvironment() throws Exception {
        EnvironmentsConfig environmentConfigs = new EnvironmentsConfig();

        BasicEnvironmentConfig localPart = environment("uat");
        localPart.addEnvironmentVariable("Var1", "Value1");
        localPart.addEnvironmentVariable("Var2", "Value2");
        BasicEnvironmentConfig remotePart = remote("uat");
        remotePart.addEnvironmentVariable("remote-var1", "remote-value-1");
        environmentConfigs.add(new MergeEnvironmentConfig(localPart, remotePart));
        environmentConfigService.sync(environmentConfigs);

        EnvironmentVariableContext environmentVariableContext = environmentConfigService.environmentVariableContextFor("uat-pipeline");

        assertThat(environmentVariableContext.getProperties().size(), is(4));
        assertThat(environmentVariableContext.getProperty("GO_ENVIRONMENT_NAME"), is("uat"));
        assertThat(environmentVariableContext.getProperty("Var1"), is("Value1"));
        assertThat(environmentVariableContext.getProperty("Var2"), is("Value2"));
        assertThat(environmentVariableContext.getProperty("remote-var1"), is("remote-value-1"));
    }

    @Test
    public void shouldListAllEnvironmentVariablesDefinedForRemoteOnlyEnvironment() throws Exception {
        EnvironmentsConfig environmentConfigs = new EnvironmentsConfig();
        BasicEnvironmentConfig remotePart = remote("uat");
        remotePart.addEnvironmentVariable("remote-var1", "remote-value-1");
        environmentConfigs.add(new MergeEnvironmentConfig(remotePart));
        environmentConfigService.sync(environmentConfigs);

        EnvironmentVariableContext environmentVariableContext = environmentConfigService.environmentVariableContextFor("uat-pipeline");

        assertThat(environmentVariableContext.getProperties().size(), is(2));
        assertThat(environmentVariableContext.getProperty("GO_ENVIRONMENT_NAME"), is("uat"));
        assertThat(environmentVariableContext.getProperty("remote-var1"), is("remote-value-1"));
    }

    @Test
    public void shouldReturnEnvironmentNames() throws Exception {
        environmentConfigService.sync(environments("uat", "prod"));
        List<CaseInsensitiveString> environmentNames = environmentConfigService.environmentNames();
        assertThat(environmentNames.size(), is(2));
        assertThat(environmentNames, hasItem(new CaseInsensitiveString("uat")));
        assertThat(environmentNames, hasItem(new CaseInsensitiveString("prod")));
    }

    @Test
    public void shouldReturnEnvironmentsForAnAgent() {
        environmentConfigService.sync(environments("uat", "prod"));
        Set<String> envForUat = environmentConfigService.environmentsFor("uat-agent");
        assertThat(envForUat.size(), is(1));
        assertThat(envForUat, hasItem("uat"));
        Set<String> envForProd = environmentConfigService.environmentsFor("prod-agent");
        assertThat(envForProd.size(), is(1));
        assertThat(envForProd, hasItem("prod"));
        Set<String> envForOmniPresent = environmentConfigService.environmentsFor(EnvironmentConfigMother.OMNIPRESENT_AGENT);
        assertThat(envForOmniPresent.size(), is(2));
        assertThat(envForOmniPresent, hasItem("prod"));
        assertThat(envForOmniPresent, hasItem("prod"));
    }

    @Test
    public void shouldCreateANewEnvironment() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<String> selectedAgents = new ArrayList<>();
        Username user = new Username(new CaseInsensitiveString("user"));
        when(securityService.isUserAdmin(user)).thenReturn(true);
        String environmentName = "foo-environment";
        when(mockGoConfigService.hasEnvironmentNamed(new CaseInsensitiveString(environmentName))).thenReturn(false);
        environmentConfigService.createEnvironment(env(environmentName, new ArrayList<>(), new ArrayList<>(), selectedAgents), user, result);

        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldCreateANewEnvironmentWithAgentsAndNoPipelines() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Username user = new Username(new CaseInsensitiveString("user"));
        when(securityService.isUserAdmin(user)).thenReturn(true);
        String environmentName = "foo-environment";
        when(mockGoConfigService.hasEnvironmentNamed(new CaseInsensitiveString(environmentName))).thenReturn(false);

        environmentConfigService.createEnvironment(env(environmentName, new ArrayList<>(), new ArrayList<>(), Arrays.asList(new String[]{"agent-guid-1"})), user, result);

        assertThat(result.isSuccessful(), is(true));
        BasicEnvironmentConfig envConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        envConfig.addAgent("agent-guid-1");
    }

    @Test
    public void shouldCreateANewEnvironmentWithEnvironmentVariables() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<String> selectedAgents = new ArrayList<>();
        Username user = new Username(new CaseInsensitiveString("user"));
        when(securityService.isUserAdmin(user)).thenReturn(true);
        String environmentName = "foo-environment";
        when(mockGoConfigService.hasEnvironmentNamed(new CaseInsensitiveString(environmentName))).thenReturn(false);
        List<Map<String, String>> environmentVariables = new ArrayList<>();
        environmentVariables.addAll(Arrays.asList(envVar("SHELL", "/bin/zsh"), envVar("HOME", "/home/cruise")));
        environmentConfigService.createEnvironment(env(environmentName, new ArrayList<>(), environmentVariables, selectedAgents), user, result);

        assertThat(result.isSuccessful(), is(true));
        BasicEnvironmentConfig expectedConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        expectedConfig.addEnvironmentVariable("SHELL", "/bin/zsh");
        expectedConfig.addEnvironmentVariable("HOME", "/home/cruise");
    }

    private Map<String, String> envVar(String name, String value) {
        HashMap<String, String> map = new HashMap<>();
        map.put("name", name);
        map.put("value", value);
        return map;
    }

    @Test
    public void shouldCreateANewEnvironmentWithPipelineSelections() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Username user = new Username(new CaseInsensitiveString("user"));
        when(securityService.isUserAdmin(user)).thenReturn(true);
        String environmentName = "foo-environment";
        when(mockGoConfigService.hasEnvironmentNamed(new CaseInsensitiveString(environmentName))).thenReturn(false);
        List<String> selectedPipelines = asList("first", "second");
        environmentConfigService.createEnvironment(env(environmentName, selectedPipelines, new ArrayList<>(), new ArrayList<>()), user, result);

        assertThat(result.isSuccessful(), is(true));
        BasicEnvironmentConfig config = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        config.addPipeline(new CaseInsensitiveString("first"));
        config.addPipeline(new CaseInsensitiveString("second"));
    }

    @Test
    public void getAllLocalPipelinesForUser_shouldReturnAllPipelinesToWhichAlongWithTheEnvironmentsToWhichTheyBelong() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Username user = new Username(new CaseInsensitiveString("user"));

        when(mockGoConfigService.getAllLocalPipelineConfigs()).thenReturn(asList(pipelineConfig("foo"), pipelineConfig("bar"), pipelineConfig("baz")));

        when(securityService.hasViewPermissionForPipeline(user, "foo")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "bar")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "baz")).thenReturn(false);

        environmentConfigService.sync(environmentsConfig("foo-env", "foo"));
        List<EnvironmentPipelineModel> pipelines = environmentConfigService.getAllLocalPipelinesForUser(user);


        assertThat(pipelines.size(), is(2));
        assertThat(pipelines, is(asList(new EnvironmentPipelineModel("bar"), new EnvironmentPipelineModel("foo", "foo-env"))));
    }

    @Test
    public void getAllLocalPipelinesForUser_shouldReturnOnlyLocalPipelines() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Username user = new Username(new CaseInsensitiveString("user"));

        when(mockGoConfigService.getAllLocalPipelineConfigs()).thenReturn(asList(pipelineConfig("foo"), pipelineConfig("bar"), pipelineConfig("baz")));

        when(securityService.hasViewPermissionForPipeline(user, "foo")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "bar")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "baz")).thenReturn(false);

        environmentConfigService.sync(environmentsConfig("foo-env", "foo"));
        List<EnvironmentPipelineModel> pipelines = environmentConfigService.getAllLocalPipelinesForUser(user);


        assertThat(pipelines.size(), is(2));
        assertThat(pipelines, is(asList(new EnvironmentPipelineModel("bar"), new EnvironmentPipelineModel("foo", "foo-env"))));
    }

    @Test
    public void getAllRemotePipelinesForUserInEnvironment_shouldReturnOnlyRemotelyAssignedPipelinesWhichUserHasPermsToView() throws NoSuchEnvironmentException {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Username user = new Username(new CaseInsensitiveString("user"));

        when(mockGoConfigService.getAllPipelineConfigs()).thenReturn(asList(pipelineConfig("foo"), pipelineConfig("bar"), pipelineConfig("baz")));

        when(securityService.hasViewPermissionForPipeline(user, "foo")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "bar")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "baz")).thenReturn(false);

        EnvironmentsConfig environmentConfigs = environmentsConfig("foo-env", "foo");
        EnvironmentConfig fooEnv = environmentConfigs.named(new CaseInsensitiveString("foo-env"));
        fooEnv.setOrigins(new RepoConfigOrigin());
        environmentConfigService.sync(environmentConfigs);
        List<EnvironmentPipelineModel> pipelines = environmentConfigService.getAllRemotePipelinesForUserInEnvironment(user, fooEnv);


        assertThat(pipelines.size(), is(1));
        assertThat(pipelines, is(asList(new EnvironmentPipelineModel("foo", "foo-env"))));
    }

    @Test
    public void shouldReturnEnvironmentConfigForEdit() throws NoSuchEnvironmentException {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        CruiseConfig config = new BasicCruiseConfig();
        BasicEnvironmentConfig env = new BasicEnvironmentConfig(new CaseInsensitiveString("foo"));
        env.addPipeline(new CaseInsensitiveString("bar"));
        env.addAgent("baz");
        env.addEnvironmentVariable("quux", "bang");
        EnvironmentsConfig environments = config.getEnvironments();
        environments.add(env);
        environmentConfigService.sync(environments);
        when(mockGoConfigService.getMergedConfigForEditing()).thenReturn(config);
        assertThat(environmentConfigService.getMergedEnvironmentforDisplay("foo", result).getConfigElement(), Is.is(env));
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldReturnResultWithMessageThatConfigWasMerged_WhenMergingEnvironmentChanges_NewUpdateEnvironmentMethod() {
        String environmentName = "env_name";
        EnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        Username user = new Username(new CaseInsensitiveString("user"));

        when(securityService.isUserAdmin(user)).thenReturn(true);
        when(mockGoConfigService.updateConfig(any(UpdateConfigCommand.class))).thenReturn(ConfigSaveState.MERGED);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String md5 = "md5";
        environmentConfigService.updateEnvironment(environmentConfig.name().toString(), environmentConfig, user, md5, result);

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Updated environment 'env_name'."));
    }

    @Test
    public void shouldReturnResultWithMessageThatConfigisUpdated_WhenUpdatingLatestConfiguration_NewUpdateEnvironmentMethod() {
        String environmentName = "env_name";
        EnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        Username user = new Username(new CaseInsensitiveString("user"));

        when(securityService.isUserAdmin(user)).thenReturn(true);
        when(mockGoConfigService.updateConfig(any(UpdateConfigCommand.class))).thenReturn(ConfigSaveState.UPDATED);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String md5 = "md5";
        environmentConfigService.updateEnvironment(environmentConfig.name().toString(), environmentConfig, user, md5, result);

        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Updated environment 'env_name'."));
    }

    @Test
    public void shouldReturnEnvironmentConfig() throws Exception {
        String environmentName = "foo-environment";
        String pipelineName = "up42";
        environmentConfigService.sync(environmentsConfig(environmentName, pipelineName));
        EnvironmentConfig expectedEnvironmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        expectedEnvironmentConfig.addPipeline(new CaseInsensitiveString(pipelineName));
        assertThat(environmentConfigService.getEnvironmentConfig(environmentName), is(expectedEnvironmentConfig));
    }

    @Test(expected = NoSuchEnvironmentException.class)
    public void shouldThrowExceptionWhenEnvironmentIsAbsent() throws Exception {
        String environmentName = "foo-environment";
        String pipelineName = "up42";
        environmentConfigService.sync(environmentsConfig(environmentName, pipelineName));
        environmentConfigService.getEnvironmentConfig("invalid-environment-name");
    }

    private EnvironmentsConfig environmentsConfig(String envName, String pipelineName) {
        EnvironmentsConfig environments = new EnvironmentsConfig();
        BasicEnvironmentConfig config = new BasicEnvironmentConfig(new CaseInsensitiveString(envName));
        config.addPipeline(new CaseInsensitiveString(pipelineName));
        environments.add(config);
        return environments;
    }

    private List<JobPlan> jobs(String... envNames) {
        ArrayList<JobPlan> plans = new ArrayList<>();
        for (String envName : envNames) {
            plans.add(jobForPipeline(envName + "-pipeline"));
        }
        return plans;
    }

    private DefaultJobPlan jobForPipeline(String pipelineName) {
        JobIdentifier jobIdentifier = new JobIdentifier(pipelineName, 1, "1", "defaultStage", "1", "job1", 100L);
        return new DefaultJobPlan(new Resources(), new ArrayList<>(), new ArrayList<>(), 1L, jobIdentifier, null, new EnvironmentVariables(), new EnvironmentVariables(), null);
    }

    private static BasicEnvironmentConfig env(String name, List<String> selectedPipelines, List<Map<String, String>> environmentVariables, List<String> selectedAgents) {
        BasicEnvironmentConfig config = new BasicEnvironmentConfig(new CaseInsensitiveString(name));
        for (String selectedPipeline : selectedPipelines) {
            config.addPipeline(new CaseInsensitiveString(selectedPipeline));
        }
        for (String selectedAgent : selectedAgents) {
            config.addAgent(selectedAgent);
        }
        for (Map<String, String> environmentVariable : environmentVariables) {
            config.getVariables().add(environmentVariable.get("name"), environmentVariable.get("value"));
        }
        return config;
    }
}
