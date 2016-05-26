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

package com.thoughtworks.go.server.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.ConfigSaveState;
import com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.DefaultJobPlan;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.helper.EnvironmentConfigMother;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.presentation.environment.EnvironmentPipelineModel;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import static com.thoughtworks.go.helper.EnvironmentConfigMother.environments;
import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class EnvironmentConfigServiceTest {
    public GoConfigService mockGoConfigService;
    public EnvironmentConfigService environmentConfigService;
    private BuildAssignment mockBuildAssignment;
    private SecurityService securityService;
    private AgentService agentService;


    @Before
    public void setUp() throws Exception {
        mockGoConfigService = Mockito.mock(GoConfigService.class);
        mockBuildAssignment = Mockito.mock(BuildAssignment.class);
        securityService = mock(SecurityService.class);
        agentService = mock(AgentService.class);
        environmentConfigService = new EnvironmentConfigService(mockGoConfigService, securityService);
    }

    @Test
    public void shouldRegisterAsACruiseConfigChangeListener() throws Exception {
        environmentConfigService.initialize();
        Mockito.verify(mockGoConfigService).register(environmentConfigService);
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
    public void shouldAddEnvironmentNameAsEnvironmentVariableToBuildAssignment() throws Exception {
        environmentConfigService.sync(environments("uat", "prod"));

        Mockito.when(mockBuildAssignment.getPlan()).thenReturn(jobForPipeline("uat-pipeline"));
        environmentConfigService.enhanceEnvironmentVariables(mockBuildAssignment);

        EnvironmentVariableContext expectedContext = new EnvironmentVariableContext();
        expectedContext.setProperty(EnvironmentVariableContext.GO_ENVIRONMENT_NAME, "uat", false);
        Mockito.verify(mockBuildAssignment).enhanceEnvironmentVariables(expectedContext);
    }

    @Test
    public void shouldNotAddEnvironmentNameAsEnvironmentVariableWhenNotInEnvironment() throws Exception {
        environmentConfigService.sync(environments("uat", "prod"));

        Mockito.when(mockBuildAssignment.getPlan()).thenReturn(jobForPipeline("no-env-pipeline"));
        environmentConfigService.enhanceEnvironmentVariables(mockBuildAssignment);

        Mockito.verify(mockBuildAssignment, never()).enhanceEnvironmentVariables(any(EnvironmentVariableContext.class));
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
    public void shouldAddEnvironmentVariablesDefinedInAnEnvironment() throws Exception {
        environmentConfigService.sync(environments("uat", "prod"));

        Mockito.when(mockBuildAssignment.getPlan()).thenReturn(jobForPipeline("no-env-pipeline"));
        environmentConfigService.enhanceEnvironmentVariables(mockBuildAssignment);

        Mockito.verify(mockBuildAssignment, never()).enhanceEnvironmentVariables(any(EnvironmentVariableContext.class));
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
        List<String> selectedAgents = new ArrayList<String>();
        Username user = new Username(new CaseInsensitiveString("user"));
        when(securityService.isUserAdmin(user)).thenReturn(true);
        String environmentName = "foo-environment";
        when(mockGoConfigService.hasEnvironmentNamed(new CaseInsensitiveString(environmentName))).thenReturn(false);
        environmentConfigService.createEnvironment(env(environmentName, new ArrayList<String>(), new ArrayList<Map<String, String>>(), selectedAgents), user, result);

        assertThat(result.isSuccessful(), is(true));
        verify(mockGoConfigService).addEnvironment(new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName)));
    }

    @Test
    public void shouldCreateANewEnvironmentWithAgentsAndNoPipelines() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Username user = new Username(new CaseInsensitiveString("user"));
        when(securityService.isUserAdmin(user)).thenReturn(true);
        String environmentName = "foo-environment";
        when(mockGoConfigService.hasEnvironmentNamed(new CaseInsensitiveString(environmentName))).thenReturn(false);

        environmentConfigService.createEnvironment(env(environmentName, new ArrayList<String>(), new ArrayList<Map<String, String>>(), Arrays.asList(new String[]{"agent-guid-1"})), user, result);

        assertThat(result.isSuccessful(), is(true));
        BasicEnvironmentConfig envConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        envConfig.addAgent("agent-guid-1");
        verify(mockGoConfigService).addEnvironment(envConfig);
    }

    @Test
    public void shouldCreateANewEnvironmentWithEnvironmentVariables() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<String> selectedAgents = new ArrayList<String>();
        Username user = new Username(new CaseInsensitiveString("user"));
        when(securityService.isUserAdmin(user)).thenReturn(true);
        String environmentName = "foo-environment";
        when(mockGoConfigService.hasEnvironmentNamed(new CaseInsensitiveString(environmentName))).thenReturn(false);
        List<Map<String, String>> environmentVariables = new ArrayList<Map<String, String>>();
        environmentVariables.addAll(Arrays.asList(envVar("SHELL", "/bin/zsh"), envVar("HOME", "/home/cruise")));
        environmentConfigService.createEnvironment(env(environmentName, new ArrayList<String>(), environmentVariables, selectedAgents), user, result);

        assertThat(result.isSuccessful(), is(true));
        BasicEnvironmentConfig expectedConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        expectedConfig.addEnvironmentVariable("SHELL", "/bin/zsh");
        expectedConfig.addEnvironmentVariable("HOME", "/home/cruise");
        verify(mockGoConfigService).addEnvironment(expectedConfig);
    }

    private Map<String, String> envVar(String name, String value) {
        HashMap<String, String> map = new HashMap<String, String>();
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
        environmentConfigService.createEnvironment(env(environmentName, selectedPipelines, new ArrayList<Map<String, String>>(), new ArrayList<String>()), user, result);

        assertThat(result.isSuccessful(), is(true));
        BasicEnvironmentConfig config = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        config.addPipeline(new CaseInsensitiveString("first"));
        config.addPipeline(new CaseInsensitiveString("second"));
        verify(mockGoConfigService).addEnvironment(config);
    }

    @Test
    public void createEnvironment_shouldReturnConflictWhenEnvAlreadyExists() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Username user = new Username(new CaseInsensitiveString("user"));
        when(securityService.isUserAdmin(user)).thenReturn(true);
        String environmentName = "foo-environment";
        when(mockGoConfigService.hasEnvironmentNamed(new CaseInsensitiveString(environmentName))).thenReturn(true);
        environmentConfigService.createEnvironment(env(environmentName, new ArrayList<String>(), new ArrayList<Map<String, String>>(), new ArrayList<String>()),
                user, result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(HttpServletResponse.SC_CONFLICT));

        Localizer localizer = mock(Localizer.class);
        result.message(localizer);

        verify(localizer).localize("CANNOT_ADD_ENV_ALREADY_EXISTS", new CaseInsensitiveString(environmentName));
        verify(mockGoConfigService).hasEnvironmentNamed(new CaseInsensitiveString(environmentName));
        verifyNoMoreInteractions(mockGoConfigService);
    }

    @Test
    public void createEnvironment_shouldReturn401WhenUserIsNotAnSuperAdmin() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Username user = new Username(new CaseInsensitiveString("user"));
        when(securityService.isUserAdmin(user)).thenReturn(false);
        environmentConfigService.createEnvironment(env("foo-environment", new ArrayList<String>(), new ArrayList<Map<String, String>>(), new ArrayList<String>()), user, result);

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(HttpServletResponse.SC_UNAUTHORIZED));

        Localizer localizer = mock(Localizer.class);
        result.message(localizer);

        verify(localizer).localize("NO_PERMISSION_TO_ADD_ENVIRONMENT", "user");
        verifyNoMoreInteractions(mockGoConfigService);
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
    public void getAllRemotePipelinesForUserInEnvironment_shouldReturnOnlyRemotelyAssignedPipelinesWhichUserHasPermsToView() throws NoSuchEnvironmentException
    {
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
        List<EnvironmentPipelineModel> pipelines = environmentConfigService.getAllRemotePipelinesForUserInEnvironment(user,fooEnv);


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
        config.getEnvironments().add(env);
        when(mockGoConfigService.getConfigForEditing()).thenReturn(config);
        assertThat(environmentConfigService.forEdit("foo", result).getConfigElement(), Is.<EnvironmentConfig>is(env));
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldReturnResultWithMessageThatConfigWasMerged_WhenMergingEnvironmentChanges() {
        String environmentName = "env_name";
        EnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        String md5 = "md5";
        Username user = new Username(new CaseInsensitiveString("user"));

        when(securityService.isUserAdmin(user)).thenReturn(true);
        when(mockGoConfigService.updateEnvironment(environmentName, environmentConfig, md5)).thenReturn(ConfigSaveState.MERGED);

        HttpLocalizedOperationResult result = environmentConfigService.updateEnvironment(environmentName, environmentConfig, user, md5);

        assertThat(result.localizable(),
                is(LocalizedMessage.composite(LocalizedMessage.string("UPDATE_ENVIRONMENT_SUCCESS", environmentName), LocalizedMessage.string("CONFIG_MERGED"))));
    }

    @Test
    public void shouldReturnResultWithMessageThatConfigisUpdated_WhenUpdatingLatestConfiguration() {
        String environmentName = "env_name";
        EnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        String md5 = "md5";
        Username user = new Username(new CaseInsensitiveString("user"));

        when(securityService.isUserAdmin(user)).thenReturn(true);
        when(mockGoConfigService.updateEnvironment(environmentName, environmentConfig, md5)).thenReturn(ConfigSaveState.UPDATED);

        HttpLocalizedOperationResult result = environmentConfigService.updateEnvironment(environmentName, environmentConfig, user, md5);

        assertThat(result.localizable(), is((Localizable) LocalizedMessage.string("UPDATE_ENVIRONMENT_SUCCESS", environmentName)));
    }

    private EnvironmentsConfig environmentsConfig(String envName, String pipelineName) {
        EnvironmentsConfig environments = new EnvironmentsConfig();
        BasicEnvironmentConfig config = new BasicEnvironmentConfig(new CaseInsensitiveString(envName));
        config.addPipeline(new CaseInsensitiveString(pipelineName));
        environments.add(config);
        return environments;
    }

    private List<JobPlan> jobs(String... envNames) {
        ArrayList<JobPlan> plans = new ArrayList<JobPlan>();
        for (String envName : envNames) {
            plans.add(jobForPipeline(envName + "-pipeline"));
        }
        return plans;
    }

    private DefaultJobPlan jobForPipeline(String pipelineName) {
        JobIdentifier jobIdentifier = new JobIdentifier(pipelineName, 1, "1", "defaultStage", "1", "job1", 100L);
        return new DefaultJobPlan(new Resources(), new ArtifactPlans(), new ArtifactPropertiesGenerators(), 1L, jobIdentifier);
    }

    public static BasicEnvironmentConfig env(String name, List<String> selectedPipelines, List<Map<String, String>> environmentVariables, List<String> selectedAgents) {
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
