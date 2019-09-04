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
package com.thoughtworks.go.server.service;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.merge.MergeEnvironmentConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.helper.EnvironmentConfigMother;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.presentation.environment.EnvironmentPipelineModel;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static com.thoughtworks.go.helper.EnvironmentConfigMother.environment;
import static com.thoughtworks.go.helper.EnvironmentConfigMother.environments;
import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.*;

class EnvironmentConfigServiceTest {
    private GoConfigService mockGoConfigService;
    private EnvironmentConfigService environmentConfigService;
    private SecurityService securityService;

    @BeforeEach
    void setUp() throws Exception {
        mockGoConfigService = mock(GoConfigService.class);
        securityService = mock(SecurityService.class);
        EntityHashingService entityHashingService = mock(EntityHashingService.class);
        environmentConfigService = new EnvironmentConfigService(mockGoConfigService, securityService, entityHashingService);
    }

    @Test
    void shouldRegisterAsACruiseConfigChangeListener() {
        environmentConfigService.initialize();
        Mockito.verify(mockGoConfigService).register(environmentConfigService);
        verify(mockGoConfigService, times(3)).register(any(EntityConfigChangedListener.class));
    }

    @Test
    void shouldGetEditablePartOfEnvironmentConfig() {
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

        assertThat(environmentConfigService.getEnvironmentForEdit(uat)).isEqualTo(expectedToEdit);
    }

    @Test
    void shouldReturnAllTheLocalEnvironments() {
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

        assertThat(environmentConfigService.getAllLocalEnvironments()).isEqualTo(expectedToEdit);
    }

    @Test
    void shouldReturnAllTheMergedEnvironments() {
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

        assertThat(environmentConfigService.getAllMergedEnvironments()).isEqualTo(asList(env));
        assertThat(result.isSuccessful()).isTrue();
    }

    @Test
    void shouldFilterWhenAgentIsNotInAnEnvironment() {
        environmentConfigService.sync(environments("uat", "prod"));

        List<JobPlan> filtered = environmentConfigService.filterJobsByAgent(jobs("no-env", "uat", "prod"), "no-env-uuid");

        assertThat(filtered.size()).isEqualTo(1);
        assertThat(filtered.get(0).getPipelineName()).isEqualTo("no-env-pipeline");
    }

    @Test
    void shouldFilterWhenAgentIsInTheSameEnvironment() {
        environmentConfigService.sync(environments("uat", "prod"));

        List<JobPlan> filtered = environmentConfigService.filterJobsByAgent(jobs("no-env", "uat", "prod"), "uat-agent");

        assertThat(filtered.size()).isEqualTo(1);
        assertThat(filtered.get(0).getPipelineName()).isEqualTo("uat-pipeline");
    }

    @Test
    void shouldFilterWhenAgentIsInMultipleEnvironments() {
        environmentConfigService.sync(environments("uat", "prod"));

        List<JobPlan> filtered = environmentConfigService.filterJobsByAgent(jobs("no-env", "uat", "prod"), EnvironmentConfigMother.OMNIPRESENT_AGENT);

        assertThat(filtered.size()).isEqualTo(2);
        assertThat(filtered.get(0).getPipelineName()).isEqualTo("uat-pipeline");
        assertThat(filtered.get(1).getPipelineName()).isEqualTo("prod-pipeline");
    }

    @Test
    void shouldFilterWhenAgentIsInAnotherEnvironment() {
        environmentConfigService.sync(environments("uat", "prod"));

        List<JobPlan> filtered = environmentConfigService.filterJobsByAgent(jobs("no-env", "prod"), "uat-agent");

        assertThat(filtered.size()).isEqualTo(0);
    }

    @Test
    void shouldFindPipelinesNamesForAGivenEnvironmentName() {
        environmentConfigService.sync(environments("uat", "prod"));
        assertThat(environmentConfigService.pipelinesFor(new CaseInsensitiveString("uat")).size()).isEqualTo(1);
        assertThat(environmentConfigService.pipelinesFor(new CaseInsensitiveString("uat"))).contains(new CaseInsensitiveString("uat-pipeline"));
        assertThat(environmentConfigService.pipelinesFor(new CaseInsensitiveString("prod"))).contains(new CaseInsensitiveString("prod-pipeline"));
    }

    @Test
    void shouldFindAgentsForPipelineUnderEnvironment() {
        environmentConfigService.sync(environments("uat", "prod"));
        AgentConfig agentUnderEnv = new AgentConfig("uat-agent", "localhost", "127.0.0.1");
        AgentConfig omnipresentAgent = new AgentConfig(EnvironmentConfigMother.OMNIPRESENT_AGENT, "localhost", "127.0.0.2");

        Mockito.when(mockGoConfigService.agentByUuid("uat-agent")).thenReturn(agentUnderEnv);
        Mockito.when(mockGoConfigService.agentByUuid(EnvironmentConfigMother.OMNIPRESENT_AGENT)).thenReturn(omnipresentAgent);

        assertThat(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("uat-pipeline")).size()).isEqualTo(2);
        assertThat(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("uat-pipeline"))).contains(agentUnderEnv);
        assertThat(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("uat-pipeline"))).contains(omnipresentAgent);
    }

    @Test
    void shouldFindAgentsForPipelineUnderNoEnvironment() {
        environmentConfigService.sync(environments("uat", "prod"));
        AgentConfig noEnvAgent = new AgentConfig("no-env-agent", "localhost", "127.0.0.1");

        Agents agents = new Agents();
        agents.add(noEnvAgent);
        agents.add(new AgentConfig(EnvironmentConfigMother.OMNIPRESENT_AGENT, "localhost", "127.0.0.2"));
        Mockito.when(mockGoConfigService.agents()).thenReturn(agents);


        assertThat(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("no-env-pipeline")).size()).isEqualTo(1);
        assertThat(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("no-env-pipeline"))).contains(noEnvAgent);
    }

    @Test
    void shouldReturnEnvironmentNames() {
        environmentConfigService.sync(environments("uat", "prod"));
        List<CaseInsensitiveString> environmentNames = environmentConfigService.environmentNames();
        assertThat(environmentNames.size()).isEqualTo(2);
        assertThat(environmentNames).contains(new CaseInsensitiveString("uat"));
        assertThat(environmentNames).contains(new CaseInsensitiveString("prod"));
    }

    @Test
    void shouldReturnEnvironmentsForAnAgent() {
        environmentConfigService.sync(environments("uat", "prod"));
        Set<String> envForUat = environmentConfigService.environmentsFor("uat-agent");
        assertThat(envForUat.size()).isEqualTo(1);
        assertThat(envForUat).contains("uat");
        Set<String> envForProd = environmentConfigService.environmentsFor("prod-agent");
        assertThat(envForProd.size()).isEqualTo(1);
        assertThat(envForProd).contains("prod");
        Set<String> envForOmniPresent = environmentConfigService.environmentsFor(EnvironmentConfigMother.OMNIPRESENT_AGENT);
        assertThat(envForOmniPresent.size()).isEqualTo(2);
        assertThat(envForOmniPresent).contains("uat");
        assertThat(envForOmniPresent).contains("prod");
    }

    @Test
    void shouldReturnEnvironmentConfigsForAnAgent() {
        environmentConfigService.sync(environments("uat", "prod"));
        Set<EnvironmentConfig> envForUat = environmentConfigService.environmentConfigsFor("uat-agent");
        assertThat(envForUat.size()).isEqualTo(1);
        assertThat(envForUat).contains(environment("uat"));
        Set<EnvironmentConfig> envForProd = environmentConfigService.environmentConfigsFor("prod-agent");
        assertThat(envForProd.size()).isEqualTo(1);
        assertThat(envForProd).contains(environment("prod"));
        Set<EnvironmentConfig> envForOmniPresent = environmentConfigService.environmentConfigsFor(EnvironmentConfigMother.OMNIPRESENT_AGENT);
        assertThat(envForOmniPresent.size()).isEqualTo(2);
        assertThat(envForOmniPresent).contains(environment("uat"));
        assertThat(envForOmniPresent).contains(environment("prod"));
    }

    @Test
    void shouldCreateANewEnvironment() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<String> selectedAgents = new ArrayList<>();
        Username user = new Username(new CaseInsensitiveString("user"));
        when(securityService.isUserAdmin(user)).thenReturn(true);
        String environmentName = "foo-environment";
        when(mockGoConfigService.hasEnvironmentNamed(new CaseInsensitiveString(environmentName))).thenReturn(false);
        environmentConfigService.createEnvironment(env(environmentName, new ArrayList<>(), new ArrayList<>(), selectedAgents), user, result);

        assertThat(result.isSuccessful()).isTrue();
    }

    @Test
    void shouldCreateANewEnvironmentWithAgentsAndNoPipelines() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Username user = new Username(new CaseInsensitiveString("user"));
        when(securityService.isUserAdmin(user)).thenReturn(true);
        String environmentName = "foo-environment";
        when(mockGoConfigService.hasEnvironmentNamed(new CaseInsensitiveString(environmentName))).thenReturn(false);

        environmentConfigService.createEnvironment(env(environmentName, new ArrayList<>(), new ArrayList<>(), Arrays.asList(new String[]{"agent-guid-1"})), user, result);

        assertThat(result.isSuccessful()).isTrue();
        BasicEnvironmentConfig envConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        envConfig.addAgent("agent-guid-1");
    }

    @Test
    void shouldCreateANewEnvironmentWithEnvironmentVariables() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<String> selectedAgents = new ArrayList<>();
        Username user = new Username(new CaseInsensitiveString("user"));
        when(securityService.isUserAdmin(user)).thenReturn(true);
        String environmentName = "foo-environment";
        when(mockGoConfigService.hasEnvironmentNamed(new CaseInsensitiveString(environmentName))).thenReturn(false);
        List<Map<String, String>> environmentVariables = new ArrayList<>();
        environmentVariables.addAll(Arrays.asList(envVar("SHELL", "/bin/zsh"), envVar("HOME", "/home/cruise")));
        environmentConfigService.createEnvironment(env(environmentName, new ArrayList<>(), environmentVariables, selectedAgents), user, result);

        assertThat(result.isSuccessful()).isTrue();
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
    void shouldCreateANewEnvironmentWithPipelineSelections() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Username user = new Username(new CaseInsensitiveString("user"));
        when(securityService.isUserAdmin(user)).thenReturn(true);
        String environmentName = "foo-environment";
        when(mockGoConfigService.hasEnvironmentNamed(new CaseInsensitiveString(environmentName))).thenReturn(false);
        List<String> selectedPipelines = asList("first", "second");
        environmentConfigService.createEnvironment(env(environmentName, selectedPipelines, new ArrayList<>(), new ArrayList<>()), user, result);

        assertThat(result.isSuccessful()).isTrue();
        BasicEnvironmentConfig config = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        config.addPipeline(new CaseInsensitiveString("first"));
        config.addPipeline(new CaseInsensitiveString("second"));
    }

    @Test
    void getAllLocalPipelinesForUser_shouldReturnAllPipelinesToWhichAlongWithTheEnvironmentsToWhichTheyBelong() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Username user = new Username(new CaseInsensitiveString("user"));

        when(mockGoConfigService.getAllLocalPipelineConfigs()).thenReturn(asList(pipelineConfig("foo"), pipelineConfig("bar"), pipelineConfig("baz")));

        when(securityService.hasViewPermissionForPipeline(user, "foo")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "bar")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "baz")).thenReturn(false);

        environmentConfigService.sync(environmentsConfig("foo-env", "foo"));
        List<EnvironmentPipelineModel> pipelines = environmentConfigService.getAllLocalPipelinesForUser(user);


        assertThat(pipelines.size()).isEqualTo(2);
        assertThat(pipelines).isEqualTo(asList(new EnvironmentPipelineModel("bar"), new EnvironmentPipelineModel("foo", "foo-env")));
    }

    @Test
    void getAllLocalPipelinesForUser_shouldReturnOnlyLocalPipelines() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Username user = new Username(new CaseInsensitiveString("user"));

        when(mockGoConfigService.getAllLocalPipelineConfigs()).thenReturn(asList(pipelineConfig("foo"), pipelineConfig("bar"), pipelineConfig("baz")));

        when(securityService.hasViewPermissionForPipeline(user, "foo")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "bar")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "baz")).thenReturn(false);

        environmentConfigService.sync(environmentsConfig("foo-env", "foo"));
        List<EnvironmentPipelineModel> pipelines = environmentConfigService.getAllLocalPipelinesForUser(user);


        assertThat(pipelines.size()).isEqualTo(2);
        assertThat(pipelines).isEqualTo(asList(new EnvironmentPipelineModel("bar"), new EnvironmentPipelineModel("foo", "foo-env")));
    }

    @Test
    void getAllRemotePipelinesForUserInEnvironment_shouldReturnOnlyRemotelyAssignedPipelinesWhichUserHasPermsToView() {
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


        assertThat(pipelines.size()).isEqualTo(1);
        assertThat(pipelines).isEqualTo(asList(new EnvironmentPipelineModel("foo", "foo-env")));
    }

    @Test
    void shouldReturnEnvironmentConfigForEdit() {
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
        assertThat(environmentConfigService.getMergedEnvironmentforDisplay("foo", result).getConfigElement()).isEqualTo(env);
        assertThat(result.isSuccessful()).isTrue();
    }

    @Test
    void shouldReturnResultWithMessageThatConfigWasMerged_WhenMergingEnvironmentChanges_NewUpdateEnvironmentMethod() {
        String environmentName = "env_name";
        EnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        Username user = new Username(new CaseInsensitiveString("user"));

        when(securityService.isUserAdmin(user)).thenReturn(true);
        when(mockGoConfigService.updateConfig(any(UpdateConfigCommand.class))).thenReturn(ConfigSaveState.MERGED);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String md5 = "md5";
        environmentConfigService.updateEnvironment(environmentConfig.name().toString(), environmentConfig, user, md5, result);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.message()).isEqualTo("Updated environment 'env_name'.");
    }

    @Test
    void shouldReturnResultWithMessageThatConfigisUpdated_WhenUpdatingLatestConfiguration_NewUpdateEnvironmentMethod() {
        String environmentName = "env_name";
        EnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        Username user = new Username(new CaseInsensitiveString("user"));

        when(securityService.isUserAdmin(user)).thenReturn(true);
        when(mockGoConfigService.updateConfig(any(UpdateConfigCommand.class))).thenReturn(ConfigSaveState.UPDATED);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String md5 = "md5";
        environmentConfigService.updateEnvironment(environmentConfig.name().toString(), environmentConfig, user, md5, result);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.message()).isEqualTo("Updated environment 'env_name'.");
    }

    @Test
    void shouldReturnEnvironmentConfig() {
        String environmentName = "foo-environment";
        String pipelineName = "up42";
        environmentConfigService.sync(environmentsConfig(environmentName, pipelineName));
        EnvironmentConfig expectedEnvironmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        expectedEnvironmentConfig.addPipeline(new CaseInsensitiveString(pipelineName));
        assertThat(environmentConfigService.getEnvironmentConfig(environmentName)).isEqualTo(expectedEnvironmentConfig);
    }

    @Test
    void shouldThrowExceptionWhenEnvironmentIsAbsent() {
        String environmentName = "foo-environment";
        String pipelineName = "up42";
        environmentConfigService.sync(environmentsConfig(environmentName, pipelineName));
        assertThatCode(() -> environmentConfigService.getEnvironmentConfig("invalid-environment-name"))
                .isInstanceOf(RecordNotFoundException.class);
    }

    @Nested
    class EnvironmentForPipeline {
        @Test
        void shouldReturnEnvironmentForGivenPipeline() {
            String environmentName = "foo-environment";
            String pipelineName = "up42";
            EnvironmentsConfig environmentsConfig = environmentsConfig(environmentName, pipelineName);
            environmentConfigService.sync(environmentsConfig);

            assertThat(environmentConfigService.environmentForPipeline("up42"))
                    .isEqualTo(environmentsConfig.get(0));
        }

        @Test
        void shouldReturnNullIfPipelineIsNotAssociatedWithEnvironment() {
            String environmentName = "foo-environment";
            String pipelineName = "up42";
            EnvironmentsConfig environmentConfig = environmentsConfig(environmentName, pipelineName);
            environmentConfigService.sync(environmentConfig);

            assertThat(environmentConfigService.environmentForPipeline("foo"))
                    .isNull();
        }
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
        return new DefaultJobPlan(new Resources(), new ArrayList<>(), new ArrayList<>(), 1L, jobIdentifier, null, new EnvironmentVariables(), new EnvironmentVariables(), null, null);
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
