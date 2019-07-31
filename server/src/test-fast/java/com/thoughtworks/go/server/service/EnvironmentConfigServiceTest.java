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

import com.google.common.collect.ImmutableMap;
import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.merge.MergeEnvironmentConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.config.update.ConfigUpdateCheckFailedException;
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

import static com.thoughtworks.go.helper.EnvironmentConfigMother.OMNIPRESENT_AGENT;
import static com.thoughtworks.go.helper.EnvironmentConfigMother.environments;
import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

class EnvironmentConfigServiceTest {
    private GoConfigService mockGoConfigService;
    private EnvironmentConfigService environmentConfigService;
    private SecurityService securityService;
    private AgentService agentService;

    @BeforeEach
    void setUp() throws Exception {
        mockGoConfigService = mock(GoConfigService.class);
        securityService = mock(SecurityService.class);
        agentService = mock(AgentService.class);

        EntityHashingService entityHashingService = mock(EntityHashingService.class);
        environmentConfigService = new EnvironmentConfigService(mockGoConfigService, securityService, entityHashingService, agentService);

        Agent agentUat = new Agent("uat-agent", "host1", "127.0.0.1", "cookie1");
        agentUat.setEnvironments("uat");
        Agent agentProd = new Agent("prod-agent", "host2", "127.0.0.1", "cookie2");
        agentProd.setEnvironments("prod");
        Agent omnipresentAgent = new Agent(OMNIPRESENT_AGENT, "host3", "127.0.0.1", "cookie3");
        omnipresentAgent.setEnvironments("uat,prod");
        when(agentService.agents()).thenReturn(new Agents(agentUat, agentProd, omnipresentAgent));
    }

    @Test
    void shouldRegisterAsACruiseConfigAndAgentChangeListener() {
        environmentConfigService.initialize();
        verify(mockGoConfigService).register(environmentConfigService);
        verify(mockGoConfigService, times(2)).register(any(EntityConfigChangedListener.class));
        verify(agentService).registerAgentChangeListeners(environmentConfigService);
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

        environmentConfigService.syncEnvironmentsFromConfig(environments);

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        BasicEnvironmentConfig env = (BasicEnvironmentConfig) environmentConfigService.getEnvironmentConfig(uat).getLocal();
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

        environmentConfigService.syncEnvironmentsFromConfig(environments);

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        BasicEnvironmentConfig env = (BasicEnvironmentConfig) environmentConfigService.getEnvironmentConfig(uat).getLocal();
        cruiseConfig.addEnvironment(env);
        List<BasicEnvironmentConfig> expectedToEdit = singletonList(new Cloner().deepClone(env));

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
        environmentConfigService.syncEnvironmentsFromConfig(environments);
        when(mockGoConfigService.getMergedConfigForEditing()).thenReturn(config);

        assertThat(environmentConfigService.getAllMergedEnvironments()).isEqualTo(singletonList(env));
        assertThat(result.isSuccessful()).isTrue();
    }

    @Test
    void shouldFilterWhenAgentIsNotInAnEnvironment() {
        environmentConfigService.syncEnvironmentsFromConfig(environments("uat", "prod"));
        environmentConfigService.syncAssociatedAgentsFromDB();

        List<JobPlan> filtered = environmentConfigService.filterJobsByAgent(jobs("no-env", "uat", "prod"), "no-env-uuid");

        assertThat(filtered.size()).isEqualTo(1);
        assertThat(filtered.get(0).getPipelineName()).isEqualTo("no-env-pipeline");
    }

    @Test
    void shouldFilterWhenAgentIsInTheSameEnvironment() {
        environmentConfigService.syncEnvironmentsFromConfig(environments("uat", "prod"));
        environmentConfigService.syncAssociatedAgentsFromDB();

        List<JobPlan> filtered = environmentConfigService.filterJobsByAgent(jobs("no-env", "uat", "prod"), "uat-agent");

        assertThat(filtered.size()).isEqualTo(1);
        assertThat(filtered.get(0).getPipelineName()).isEqualTo("uat-pipeline");
    }

    @Test
    void shouldFilterWhenAgentIsInMultipleEnvironments() {
        environmentConfigService.syncEnvironmentsFromConfig(environments("uat", "prod"));
        environmentConfigService.syncAssociatedAgentsFromDB();

        List<JobPlan> filtered = environmentConfigService.filterJobsByAgent(jobs("no-env", "uat", "prod"), OMNIPRESENT_AGENT);

        assertThat(filtered.size()).isEqualTo(2);
        assertThat(filtered.get(0).getPipelineName()).isEqualTo("uat-pipeline");
        assertThat(filtered.get(1).getPipelineName()).isEqualTo("prod-pipeline");
    }

    @Test
    void shouldFilterWhenAgentIsInAnotherEnvironment() {
        environmentConfigService.syncEnvironmentsFromConfig(environments("uat", "prod"));
        environmentConfigService.syncAssociatedAgentsFromDB();

        List<JobPlan> filtered = environmentConfigService.filterJobsByAgent(jobs("no-env", "prod"), "uat-agent");

        assertThat(filtered.size()).isEqualTo(0);
    }

    @Test
    void shouldFindPipelinesNamesForAGivenEnvironmentName() {
        environmentConfigService.syncEnvironmentsFromConfig(environments("uat", "prod"));
        assertThat(environmentConfigService.pipelinesFor(new CaseInsensitiveString("uat")).size()).isEqualTo(1);
        assertThat(environmentConfigService.pipelinesFor(new CaseInsensitiveString("uat"))).contains(new CaseInsensitiveString("uat-pipeline"));
        assertThat(environmentConfigService.pipelinesFor(new CaseInsensitiveString("prod"))).contains(new CaseInsensitiveString("prod-pipeline"));
    }

    @Test
    void shouldFindAgentsForPipelineUnderEnvironment() {
        environmentConfigService.syncEnvironmentsFromConfig(environments("uat", "prod"));
        environmentConfigService.syncAssociatedAgentsFromDB();
        Agent agentUnderEnv = new Agent("uat-agent", "localhost", "127.0.0.1");
        Agent omnipresentAgent = new Agent(OMNIPRESENT_AGENT, "localhost", "127.0.0.2");

        Mockito.when(agentService.agentByUuid("uat-agent")).thenReturn(agentUnderEnv);
        Mockito.when(agentService.agentByUuid(OMNIPRESENT_AGENT)).thenReturn(omnipresentAgent);

        assertThat(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("uat-pipeline")).size()).isEqualTo(2);
        assertThat(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("uat-pipeline"))).contains(agentUnderEnv);
        assertThat(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("uat-pipeline"))).contains(omnipresentAgent);
    }

    @Test
    void shouldFindAgentsForPipelineUnderNoEnvironment() {
        environmentConfigService.syncEnvironmentsFromConfig(environments("uat", "prod"));
        environmentConfigService.syncAssociatedAgentsFromDB();
        Agent noEnvAgent = new Agent("no-env-agent", "localhost", "127.0.0.1");

        Agents agents = new Agents();
        agents.add(noEnvAgent);
        agents.add(new Agent(OMNIPRESENT_AGENT, "localhost", "127.0.0.2"));
        Mockito.when(agentService.agents()).thenReturn(agents);


        assertThat(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("no-env-pipeline")).size()).isEqualTo(1);
        assertThat(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("no-env-pipeline"))).contains(noEnvAgent);
    }

    @Test
    void shouldReturnEnvironmentNames() {
        environmentConfigService.syncEnvironmentsFromConfig(environments("uat", "prod"));
        List<CaseInsensitiveString> environmentNames = environmentConfigService.environmentNames();
        assertThat(environmentNames.size()).isEqualTo(2);
        assertThat(environmentNames).contains(new CaseInsensitiveString("uat"));
        assertThat(environmentNames).contains(new CaseInsensitiveString("prod"));
    }

    @Test
    void shouldReturnEnvironmentsForAnAgent() {
        environmentConfigService.syncEnvironmentsFromConfig(environments("uat", "prod"));
        environmentConfigService.syncAssociatedAgentsFromDB();
        Set<String> envForUat = environmentConfigService.environmentsFor("uat-agent");
        assertThat(envForUat.size()).isEqualTo(1);
        assertThat(envForUat).contains("uat");
        Set<String> envForProd = environmentConfigService.environmentsFor("prod-agent");
        assertThat(envForProd.size()).isEqualTo(1);
        assertThat(envForProd).contains("prod");
        Set<String> envForOmniPresent = environmentConfigService.environmentsFor(OMNIPRESENT_AGENT);
        assertThat(envForOmniPresent.size()).isEqualTo(2);
        assertThat(envForOmniPresent).contains("uat");
        assertThat(envForOmniPresent).contains("prod");
    }

    @Test
    void shouldReturnEnvironmentConfigsForAnAgent() {
        EnvironmentsConfig environmentConfigs = environments("uat", "prod");
        environmentConfigService.syncEnvironmentsFromConfig(environmentConfigs);
        environmentConfigService.syncAssociatedAgentsFromDB();
        Set<EnvironmentConfig> envForUat = environmentConfigService.environmentConfigsFor("uat-agent");
        assertThat(envForUat.size()).isEqualTo(1);
        assertThat(envForUat).contains(environmentConfigs.named(new CaseInsensitiveString("uat")));
        Set<EnvironmentConfig> envForProd = environmentConfigService.environmentConfigsFor("prod-agent");
        assertThat(envForProd.size()).isEqualTo(1);
        assertThat(envForProd).contains(environmentConfigs.named(new CaseInsensitiveString("prod")));
        Set<EnvironmentConfig> envForOmniPresent = environmentConfigService.environmentConfigsFor(OMNIPRESENT_AGENT);
        assertThat(envForOmniPresent.size()).isEqualTo(2);
        assertThat(envForOmniPresent).contains(environmentConfigs.named(new CaseInsensitiveString("uat")));
        assertThat(envForOmniPresent).contains(environmentConfigs.named(new CaseInsensitiveString("prod")));
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

        environmentConfigService.createEnvironment(env(environmentName, new ArrayList<>(), new ArrayList<>(), singletonList("agent-guid-1")), user, result);

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
        List<Map<String, String>> envVariables = new ArrayList<>(Arrays.asList(envVar("SHELL", "/bin/zsh"), envVar("HOME", "/home/cruise")));
        environmentConfigService.createEnvironment(env(environmentName, new ArrayList<>(), envVariables, selectedAgents), user, result);

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
        Username user = new Username(new CaseInsensitiveString("user"));

        when(mockGoConfigService.getAllLocalPipelineConfigs()).thenReturn(asList(pipelineConfig("foo"), pipelineConfig("bar"), pipelineConfig("baz")));

        when(securityService.hasViewPermissionForPipeline(user, "foo")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "bar")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "baz")).thenReturn(false);

        environmentConfigService.syncEnvironmentsFromConfig(environmentsConfig("foo-env", "foo"));
        List<EnvironmentPipelineModel> pipelines = environmentConfigService.getAllLocalPipelinesForUser(user);


        assertThat(pipelines.size()).isEqualTo(2);
        assertThat(pipelines).isEqualTo(asList(new EnvironmentPipelineModel("bar"), new EnvironmentPipelineModel("foo", "foo-env")));
    }

    @Test
    void getAllLocalPipelinesForUser_shouldReturnOnlyLocalPipelines() {
        Username user = new Username(new CaseInsensitiveString("user"));

        when(mockGoConfigService.getAllLocalPipelineConfigs()).thenReturn(asList(pipelineConfig("foo"), pipelineConfig("bar"), pipelineConfig("baz")));

        when(securityService.hasViewPermissionForPipeline(user, "foo")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "bar")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "baz")).thenReturn(false);

        environmentConfigService.syncEnvironmentsFromConfig(environmentsConfig("foo-env", "foo"));
        List<EnvironmentPipelineModel> pipelines = environmentConfigService.getAllLocalPipelinesForUser(user);


        assertThat(pipelines.size()).isEqualTo(2);
        assertThat(pipelines).isEqualTo(asList(new EnvironmentPipelineModel("bar"), new EnvironmentPipelineModel("foo", "foo-env")));
    }

    @Test
    void getAllRemotePipelinesForUserInEnvironment_shouldReturnOnlyRemotelyAssignedPipelinesWhichUserHasPermsToView() {
        Username user = new Username(new CaseInsensitiveString("user"));

        when(mockGoConfigService.getAllPipelineConfigs()).thenReturn(asList(pipelineConfig("foo"), pipelineConfig("bar"), pipelineConfig("baz")));

        when(securityService.hasViewPermissionForPipeline(user, "foo")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "bar")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "baz")).thenReturn(false);

        EnvironmentsConfig environmentConfigs = environmentsConfig("foo-env", "foo");
        EnvironmentConfig fooEnv = environmentConfigs.named(new CaseInsensitiveString("foo-env"));
        fooEnv.setOrigins(new RepoConfigOrigin());
        environmentConfigService.syncEnvironmentsFromConfig(environmentConfigs);
        List<EnvironmentPipelineModel> pipelines = environmentConfigService.getAllRemotePipelinesForUserInEnvironment(user, fooEnv);


        assertThat(pipelines.size()).isEqualTo(1);
        assertThat(pipelines).isEqualTo(singletonList(new EnvironmentPipelineModel("foo", "foo-env")));
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
        environmentConfigService.syncEnvironmentsFromConfig(environments);
        when(mockGoConfigService.getMergedConfigForEditing()).thenReturn(config);
        assertThat(environmentConfigService.getMergedEnvironmentforDisplay("foo", result).getConfigElement()).isEqualTo(env);
        assertThat(result.isSuccessful()).isTrue();
    }

    @Test
    void shouldReturnResultWithMessageThatConfigWasMerged_WhenMergingEnvironmentChanges_NewUpdateEnvironmentMethod() {
        String environmentName = "env_name";
        EnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        Username user = new Username(new CaseInsensitiveString("user"));
        environmentConfigService.syncEnvironmentsFromConfig(environments("uat", "prod", "env_name"));

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
        environmentConfigService.syncEnvironmentsFromConfig(environmentsConfig(environmentName, pipelineName));
        EnvironmentConfig expectedEnvironmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        expectedEnvironmentConfig.addPipeline(new CaseInsensitiveString(pipelineName));
        assertThat(environmentConfigService.getEnvironmentConfig(environmentName)).isEqualTo(expectedEnvironmentConfig);
    }

    @Test
    void shouldThrowExceptionWhenEnvironmentIsAbsent() {
        String environmentName = "foo-environment";
        String pipelineName = "up42";
        environmentConfigService.syncEnvironmentsFromConfig(environmentsConfig(environmentName, pipelineName));
        assertThatCode(() -> environmentConfigService.getEnvironmentConfig("invalid-environment-name"))
                .isInstanceOf(RecordNotFoundException.class);
    }

    @Test
    void shouldReturnDefaultBasicEnvConfigWhenEnvironmentIsAbsent() {
        String environmentName = "foo-environment";
        String pipelineName = "up42";
        environmentConfigService.syncEnvironmentsFromConfig(environmentsConfig(environmentName, pipelineName));

        assertThat(environmentConfigService.findOrDefault("invalid-environment-name")).isEqualTo(new BasicEnvironmentConfig(new CaseInsensitiveString("invalid-environment-name")));
    }

    @Nested
    class EnvironmentForPipeline {
        @Test
        void shouldReturnEnvironmentForGivenPipeline() {
            String environmentName = "foo-environment";
            String pipelineName = "up42";
            EnvironmentsConfig environmentsConfig = environmentsConfig(environmentName, pipelineName);
            environmentConfigService.syncEnvironmentsFromConfig(environmentsConfig);

            assertThat(environmentConfigService.environmentForPipeline("up42"))
                    .isEqualTo(environmentsConfig.get(0));
        }

        @Test
        void shouldReturnNullIfPipelineIsNotAssociatedWithEnvironment() {
            String environmentName = "foo-environment";
            String pipelineName = "up42";
            EnvironmentsConfig environmentConfig = environmentsConfig(environmentName, pipelineName);
            environmentConfigService.syncEnvironmentsFromConfig(environmentConfig);

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

    @Nested
    class EnvironmentConfigSyncTest {

        @Test
        void shouldSyncEnvironmentsFromAgentAssociationInDB1() {
            shouldSyncEnvironmentsFromAgentAssociationInDB(ImmutableMap.of(
                    "dev", "uuid1",
                    "test", "uuid2",
                    "stage", "uuid1",
                    "prod", "uuid2"
            ));
        }

        @Test
        void shouldSyncEnvironmentsFromAgentAssociationInDB2() {
            shouldSyncEnvironmentsFromAgentAssociationInDB(ImmutableMap.of(
                    "dev", "uuid2",
                    "test", "uuid2",
                    "stage", "uuid1",
                    "prod", "uuid1"
            ));
        }

        @Test
        void shouldSyncEnvironmentsFromAgentAssociationInDB3() {
            shouldSyncEnvironmentsFromAgentAssociationInDB(ImmutableMap.of(
                    "dev", "uuid1",
                    "test", "uuid1",
                    "stage", "uuid1",
                    "prod", "uuid2"
            ));
        }

        @Test
        void shouldSyncEnvironmentsFromAgentAssociationInDB4() {
            shouldSyncEnvironmentsFromAgentAssociationInDB(ImmutableMap.of(
                    "dev", "uuid2",
                    "test", "uuid2",
                    "stage", "uuid2",
                    "prod", "uuid2"
            ));
        }

        @Test
        void shouldSyncEnvironmentsFromAgentAssociationInDB5() {
            shouldSyncEnvironmentsFromAgentAssociationInDB(ImmutableMap.of(
                    "dev", "uuid1",
                    "test", "uuid1",
                    "stage", "uuid1",
                    "prod", "uuid1"
            ));
        }

        void shouldSyncEnvironmentsFromAgentAssociationInDB(Map<String, String> envNameToAgentMap) {
            initializeAgents();
            String uuid1 = "uuid1";
            String uuid2 = "uuid2";

            environmentConfigService.syncEnvironmentsFromConfig(createEnvironments(envNameToAgentMap));
            environmentConfigService.syncAssociatedAgentsFromDB();
            assertThatAgentAndEnvsAssociationsAreInSync(uuid1, uuid2);
        }

        private EnvironmentsConfig createEnvironments(Map<String, String> envNameToAgentMap) {
            EnvironmentsConfig environments = new EnvironmentsConfig();
            envNameToAgentMap.keySet().forEach(envName -> {
                BasicEnvironmentConfig env = env(envName, null, null, singletonList(envNameToAgentMap.get(envName)));
                environments.add(env);
            });

            return environments;
        }

        private void initializeAgents() {
            Agent agentConf1 = new Agent("uuid1");
            agentConf1.setEnvironments("dev,test");
            Agent agentConf2 = new Agent("uuid2");
            agentConf2.setEnvironments("stage,prod");

            Agents agents = new Agents(agentConf1, agentConf2);
            when(agentService.agents()).thenReturn(agents);
        }

        private void assertThatAgentAndEnvsAssociationsAreInSync(String uuid1, String uuid2) {
            Set<EnvironmentConfig> envConfigs = environmentConfigService.getEnvironments();
            envConfigs.forEach(env -> {
                String envName = env.name().toString();
                if (envName.equalsIgnoreCase("dev") || envName.equalsIgnoreCase("test")) {
                    assertThat(env.hasAgent(uuid1)).isTrue();
                    assertThat(env.hasAgent(uuid2)).isFalse();
                } else if (envName.equalsIgnoreCase("stage") || envName.equalsIgnoreCase("prod")) {
                    assertThat(env.hasAgent(uuid2)).isTrue();
                    assertThat(env.hasAgent(uuid1)).isFalse();
                }
            });
        }
    }

    @Test
    void shouldThrowExceptionIfEnvironmentConfigDoesNotExist() {
        String environmentName = "foo-environment";
        String pipelineName = "up42";
        EnvironmentsConfig environmentConfig = environmentsConfig(environmentName, pipelineName);
        environmentConfigService.syncEnvironmentsFromConfig(environmentConfig);

        assertThatCode(() -> environmentConfigService.getEnvironmentConfig("non-existent-env-name"))
                .isInstanceOf(RecordNotFoundException.class)
                .hasMessage("Environment with name 'non-existent-env-name' was not found!");
    }

    @Test
    void shouldReturnResultWithMessageOnSuccessInPatchEnv() {
        String environmentName = "env_name";
        EnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        Username user = new Username(new CaseInsensitiveString("user"));
        environmentConfigService.syncEnvironmentsFromConfig(environments("uat", "prod", "env_name"));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        environmentConfigService.patchEnvironment(environmentConfig, singletonList("pipeline1"), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), user, result);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.message()).isEqualTo("Updated environment 'env_name'.");
    }

    @Test
    void shouldReturnResultWithFailedMessageIfUserIsNotAuthorizedInPatchEnv() {
        String environmentName = "env_name";
        EnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        Username user = new Username(new CaseInsensitiveString("user"));
        environmentConfigService.syncEnvironmentsFromConfig(environments("uat", "prod", "env_name"));

        doThrow(ConfigUpdateCheckFailedException.class)
                .when(mockGoConfigService).updateConfig(any(EntityConfigUpdateCommand.class), any(Username.class));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        environmentConfigService.patchEnvironment(environmentConfig, singletonList("pipeline1"), emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), user, result);

        assertFalse(result.isSuccessful());
        assertEquals(result.message(), "Failed to update environment 'env_name'. ");
    }

    @Test
    void shouldReturnResultWithSuccessMessageOnDeleteEnv() {
        String environmentName = "env_name";
        EnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        Username user = new Username(new CaseInsensitiveString("user"));
        environmentConfigService.syncEnvironmentsFromConfig(environments("uat", "prod", "env_name"));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        environmentConfigService.deleteEnvironment(environmentConfig, user, result);

        assertThat(result.isSuccessful()).isTrue();
        assertThat(result.message()).isEqualTo("Environment with name 'env_name' was deleted successfully!");
    }

    @Test
    void shouldReturnResultWithFailedMessageIfUserIsNotAuthorizedToDeleteEnv() {
        String environmentName = "env_name";
        EnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        Username user = new Username(new CaseInsensitiveString("user"));
        environmentConfigService.syncEnvironmentsFromConfig(environments("uat", "prod", "env_name"));

        doThrow(ConfigUpdateCheckFailedException.class)
                .when(mockGoConfigService).updateConfig(any(EntityConfigUpdateCommand.class), any(Username.class));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        environmentConfigService.deleteEnvironment(environmentConfig, user, result);

        assertFalse(result.isSuccessful());
        assertEquals(result.message(), "Failed to delete environment 'env_name'. ");
    }

    @Test
    void shouldUpdateEnvironmentCacheOnAgentChange() {
        String uuid = "uuid";
        String environmentName = "foo-environment";
        EnvironmentsConfig environments = new EnvironmentsConfig();
        BasicEnvironmentConfig config = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        config.addAgent(uuid);
        environments.add(config);
        environmentConfigService.syncEnvironmentsFromConfig(environments);

        EnvironmentConfig environmentConfig = environmentConfigService.getEnvironmentConfig(environmentName);
        EnvironmentAgentsConfig environmentConfigAgents = environmentConfig.getAgents();
        assertThat(environmentConfigAgents.size()).isEqualTo(1);
        assertThat(environmentConfigAgents.get(0).getUuid()).isEqualTo(uuid);

        String newEnvironmentName = "new-environment";
        Agent agentBeforeUpdate = new Agent(uuid);
        agentBeforeUpdate.addEnvironment(environmentName);
        Agent agentAfterUpdate = new Agent(uuid);
        agentAfterUpdate.addEnvironment(newEnvironmentName);
        AgentChangedEvent agentChangedEvent = new AgentChangedEvent(agentBeforeUpdate, agentAfterUpdate);

        environmentConfigService.agentChanged(agentChangedEvent);

        EnvironmentConfig afterUpdateEnvConfig = environmentConfigService.getEnvironmentConfig(environmentName);
        assertThat(afterUpdateEnvConfig.getAgents().size()).isEqualTo(0);
    }

    @Test
    void shouldRemoveAgentFromEnvCacheOnAgentDeletion() {
        String uuid = "uuid";
        String environmentName = "foo-environment";
        EnvironmentsConfig environments = new EnvironmentsConfig();
        BasicEnvironmentConfig config = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        config.addAgent(uuid);
        environments.add(config);
        environmentConfigService.syncEnvironmentsFromConfig(environments);

        EnvironmentConfig environmentConfig = environmentConfigService.getEnvironmentConfig(environmentName);
        EnvironmentAgentsConfig environmentConfigAgents = environmentConfig.getAgents();
        assertThat(environmentConfigAgents.size()).isEqualTo(1);
        assertThat(environmentConfigAgents.get(0).getUuid()).isEqualTo(uuid);

        Agent agentDeleted = new Agent(uuid);
        agentDeleted.addEnvironment(environmentName);
        environmentConfigService.agentDeleted(agentDeleted);

        EnvironmentConfig afterUpdateEnvConfig = environmentConfigService.getEnvironmentConfig(environmentName);
        assertThat(afterUpdateEnvConfig.getAgents().size()).isEqualTo(0);
    }

    @Test
    void shouldSyncAgentsFromDB() {
        String uuid = "uuid";
        String environmentName = "foo-environment";
        EnvironmentsConfig environments = new EnvironmentsConfig();
        environments.add(new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName)));
        environmentConfigService.syncEnvironmentsFromConfig(environments);

        EnvironmentConfig environmentConfig = environmentConfigService.getEnvironmentConfig(environmentName);
        assertThat(environmentConfig.getAgents().size()).isEqualTo(0);

        Agents agents = new Agents();
        Agent agent = new Agent(uuid);
        agent.addEnvironment(environmentName);
        agents.add(agent);
        when(agentService.agents()).thenReturn(agents);

        environmentConfigService.syncAgentsFromDB();

        EnvironmentConfig afterUpdateEnvConfig = environmentConfigService.getEnvironmentConfig(environmentName);
        EnvironmentAgentsConfig afterUpdateEnvConfigAgents = afterUpdateEnvConfig.getAgents();
        assertThat(afterUpdateEnvConfigAgents.size()).isEqualTo(1);
        assertThat(afterUpdateEnvConfigAgents.get(0).getUuid()).isEqualTo(uuid);
    }

    @Test
    void shouldSyncAssociatedAgentsFromDB() {
        String uuid = "uuid";
        String envName = "foo-environment";
        String envName1 = "bar-environment";
        EnvironmentsConfig environments = new EnvironmentsConfig();
        environments.add(new BasicEnvironmentConfig(new CaseInsensitiveString(envName)));
        BasicEnvironmentConfig envConfig1 = new BasicEnvironmentConfig(new CaseInsensitiveString(envName1));
        envConfig1.addAgent(uuid);
        environments.add(envConfig1);
        environmentConfigService.syncEnvironmentsFromConfig(environments);

        EnvironmentAgentsConfig envConfigAgents1 = environmentConfigService.getEnvironmentConfig(envName1).getAgents();
        assertThat(envConfigAgents1.size()).isEqualTo(1);
        assertThat(envConfigAgents1.get(0).getUuid()).isEqualTo(uuid);

        Agent agent = new Agent(uuid);
        agent.addEnvironment(envName);
        when(agentService.agents()).thenReturn(new Agents(agent));

        environmentConfigService.syncAssociatedAgentsFromDB();

        EnvironmentAgentsConfig afterUpdateEnvConfigAgents = environmentConfigService.getEnvironmentConfig(envName).getAgents();
        assertThat(afterUpdateEnvConfigAgents.size()).isEqualTo(1);
        assertThat(afterUpdateEnvConfigAgents.get(0).getUuid()).isEqualTo(uuid);

        EnvironmentAgentsConfig afterUpdateEnvConfigAgents1 = environmentConfigService.getEnvironmentConfig(envName1).getAgents();
        assertThat(afterUpdateEnvConfigAgents1.size()).isEqualTo(0);

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

        if (selectedPipelines != null) {
            for (String selectedPipeline : selectedPipelines) {
                config.addPipeline(new CaseInsensitiveString(selectedPipeline));
            }
        }

        if (selectedAgents != null) {
            for (String selectedAgent : selectedAgents) {
                config.addAgent(selectedAgent);
            }
        }

        if (environmentVariables != null) {
            for (Map<String, String> environmentVariable : environmentVariables) {
                config.getVariables().add(environmentVariable.get("name"), environmentVariable.get("value"));
            }
        }

        return config;
    }
}
