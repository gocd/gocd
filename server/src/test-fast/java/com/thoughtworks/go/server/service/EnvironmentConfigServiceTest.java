/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.merge.MergeEnvironmentConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.config.update.ConfigUpdateCheckFailedException;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.presentation.environment.EnvironmentPipelineModel;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.ClonerFactory;
import com.thoughtworks.go.util.SystemEnvironment;
import org.hamcrest.Matchers;
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
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
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

        AgentInstances agentInstances = new AgentInstances(null);

        AgentInstance agentInstance1 = AgentInstance.createFromAgent(agentUat, new SystemEnvironment(), null);
        AgentInstance agentInstance2 = AgentInstance.createFromAgent(agentProd, new SystemEnvironment(), null);
        AgentInstance agentInstance3 = AgentInstance.createFromAgent(omnipresentAgent, new SystemEnvironment(), null);
        agentInstances.add(agentInstance1);
        agentInstances.add(agentInstance2);
        agentInstances.add(agentInstance3);
        when(agentService.agents()).thenReturn(new Agents(agentUat, agentProd, omnipresentAgent));
        when(agentService.getAgentInstances()).thenReturn(agentInstances);
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

        environmentConfigService.syncEnvironments(environments);

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        BasicEnvironmentConfig env = (BasicEnvironmentConfig) environmentConfigService.getEnvironmentConfig(uat).getLocal();
        cruiseConfig.addEnvironment(env);
        BasicEnvironmentConfig expectedToEdit = ClonerFactory.instance().deepClone(env);

        when(mockGoConfigService.getConfigForEditing()).thenReturn(cruiseConfig);

        assertThat(environmentConfigService.getEnvironmentForEdit(uat), is(expectedToEdit));
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

        when(agentService.getAgentInstances()).thenReturn(new AgentInstances(null));

        environmentConfigService.syncEnvironments(environments);

        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        BasicEnvironmentConfig env = (BasicEnvironmentConfig) environmentConfigService.getEnvironmentConfig(uat).getLocal();
        cruiseConfig.addEnvironment(env);
        List<BasicEnvironmentConfig> expectedToEdit = singletonList(ClonerFactory.instance().deepClone(env));

        when(mockGoConfigService.getConfigForEditing()).thenReturn(cruiseConfig);

        assertThat(environmentConfigService.getAllLocalEnvironments(), is(expectedToEdit));
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
        when(agentService.getAgentInstances()).thenReturn(new AgentInstances(null));
        environmentConfigService.syncEnvironments(environments);
        when(mockGoConfigService.getMergedConfigForEditing()).thenReturn(config);

        assertThat(environmentConfigService.getAllMergedEnvironments(), is(singletonList(env)));
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    void shouldFilterWhenAgentIsNotInAnEnvironment() {
        environmentConfigService.syncEnvironments(environments("uat", "prod"));

        List<JobPlan> filtered = environmentConfigService.filterJobsByAgent(jobs("no-env", "uat", "prod"), "no-env-uuid");

        assertThat(filtered.size(), is(1));
        assertThat(filtered.get(0).getPipelineName(), is("no-env-pipeline"));
    }

    @Test
    void shouldFilterWhenAgentIsInTheSameEnvironment() {
        environmentConfigService.syncEnvironments(environments("uat", "prod"));

        List<JobPlan> filtered = environmentConfigService.filterJobsByAgent(jobs("no-env", "uat", "prod"), "uat-agent");

        assertThat(filtered.size(), is(1));
        assertThat(filtered.get(0).getPipelineName(), is("uat-pipeline"));
    }

    @Test
    void shouldFilterWhenAgentIsInMultipleEnvironments() {
        environmentConfigService.syncEnvironments(environments("uat", "prod"));

        List<JobPlan> filtered = environmentConfigService.filterJobsByAgent(jobs("no-env", "uat", "prod"), OMNIPRESENT_AGENT);

        assertThat(filtered.size(), is(2));
        assertThat(filtered.get(0).getPipelineName(), is("uat-pipeline"));
        assertThat(filtered.get(1).getPipelineName(), is("prod-pipeline"));
    }

    @Test
    void shouldFilterWhenAgentIsInAnotherEnvironment() {
        environmentConfigService.syncEnvironments(environments("uat", "prod"));

        List<JobPlan> filtered = environmentConfigService.filterJobsByAgent(jobs("no-env", "prod"), "uat-agent");

        assertThat(filtered.size(), is(0));
    }

    @Test
    void shouldFindAgentsForPipelineUnderEnvironment() {
        environmentConfigService.syncEnvironments(environments("uat", "prod"));
        Agent agentUnderEnv = new Agent("uat-agent", "localhost", "127.0.0.1");
        Agent omnipresentAgent = new Agent(OMNIPRESENT_AGENT, "localhost", "127.0.0.2");

        Mockito.when(agentService.getAgentByUUID("uat-agent")).thenReturn(agentUnderEnv);
        Mockito.when(agentService.getAgentByUUID(OMNIPRESENT_AGENT)).thenReturn(omnipresentAgent);

        assertThat(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("uat-pipeline")).size(), is(2));
        assertTrue(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("uat-pipeline")).contains(agentUnderEnv));
        assertTrue(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("uat-pipeline")).contains(omnipresentAgent));
    }

    @Test
    void shouldFindAgentsForPipelineUnderNoEnvironment() {
        environmentConfigService.syncEnvironments(environments("uat", "prod"));
        Agent noEnvAgent = new Agent("no-env-agent", "localhost", "127.0.0.1");

        Agents agents = new Agents();
        agents.add(noEnvAgent);
        agents.add(new Agent(OMNIPRESENT_AGENT, "localhost", "127.0.0.2"));
        Mockito.when(agentService.agents()).thenReturn(agents);


        assertThat(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("no-env-pipeline")).size(), is(1));
        assertTrue(environmentConfigService.agentsForPipeline(new CaseInsensitiveString("no-env-pipeline")).contains(noEnvAgent));
    }

    @Test
    void shouldReturnEnvironmentNames() {
        environmentConfigService.syncEnvironments(environments("uat", "prod"));
        List<String> envNames = environmentConfigService.getEnvironmentNames();
        assertThat(envNames.size(), is(2));
        assertTrue(envNames.contains("uat"));
        assertTrue(envNames.contains("prod"));
    }

    @Test
    void shouldReturnEnvironmentsForAnAgent() {
        environmentConfigService.syncEnvironments(environments("uat", "prod"));
        Set<String> envForUat = environmentConfigService.getAgentEnvironmentNames("uat-agent");
        assertThat(envForUat.size(), is(1));
        assertTrue(envForUat.contains("uat"));
        Set<String> envForProd = environmentConfigService.getAgentEnvironmentNames("prod-agent");
        assertThat(envForProd.size(), is(1));
        assertTrue(envForProd.contains("prod"));
        Set<String> envForOmniPresent = environmentConfigService.getAgentEnvironmentNames(OMNIPRESENT_AGENT);
        assertThat(envForOmniPresent.size(), is(2));
        assertTrue(envForOmniPresent.contains("uat"));
        assertTrue(envForOmniPresent.contains("prod"));
    }

    @Test
    void shouldReturnEnvironmentConfigsForSpecifiedUUID() {
        EnvironmentsConfig envConfigs = environments("uat", "prod");
        environmentConfigService.syncEnvironments(envConfigs);

        Set<EnvironmentConfig> envConfigSet = environmentConfigService.getAgentEnvironments("uat-agent");
        assertThat(envConfigSet.size(), is(1));
        assertTrue(envConfigSet.contains(envConfigs.named(new CaseInsensitiveString("uat"))));

        envConfigSet = environmentConfigService.getAgentEnvironments("prod-agent");
        assertThat(envConfigSet.size(), is(1));
        assertTrue(envConfigSet.contains(envConfigs.named(new CaseInsensitiveString("prod"))));

        envConfigSet = environmentConfigService.getAgentEnvironments(OMNIPRESENT_AGENT);
        assertThat(envConfigSet.size(), is(2));
        assertTrue(envConfigSet.contains(envConfigs.named(new CaseInsensitiveString("uat"))));
        assertTrue(envConfigSet.contains(envConfigs.named(new CaseInsensitiveString("prod"))));
    }

    @Nested
    class CreateEnvironment {
        @Test
        void shouldCreateANewEnvironment() {
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
        void shouldCreateANewEnvironmentWithAgentsAndNoPipelines() {
            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            Username user = new Username(new CaseInsensitiveString("user"));
            when(securityService.isUserAdmin(user)).thenReturn(true);
            String environmentName = "foo-environment";
            when(mockGoConfigService.hasEnvironmentNamed(new CaseInsensitiveString(environmentName))).thenReturn(false);

            environmentConfigService.createEnvironment(env(environmentName, new ArrayList<>(), new ArrayList<>(), singletonList("agent-guid-1")), user, result);

            assertThat(result.isSuccessful(), is(true));
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

            assertThat(result.isSuccessful(), is(true));
            BasicEnvironmentConfig expectedConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
            expectedConfig.addEnvironmentVariable("SHELL", "/bin/zsh");
            expectedConfig.addEnvironmentVariable("HOME", "/home/cruise");
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

            assertThat(result.isSuccessful(), is(true));
            BasicEnvironmentConfig config = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
            config.addPipeline(new CaseInsensitiveString("first"));
            config.addPipeline(new CaseInsensitiveString("second"));
        }

        private Map<String, String> envVar(String name, String value) {
            HashMap<String, String> map = new HashMap<>();
            map.put("name", name);
            map.put("value", value);
            return map;
        }
    }

    @Test
    void getAllLocalPipelinesForUser_shouldReturnAllPipelinesToWhichAlongWithTheEnvironmentsToWhichTheyBelong() {
        Username user = new Username(new CaseInsensitiveString("user"));

        String fooPipeline = "foo";
        String barPipeline = "bar";
        String bazPipeline = "baz";

        PipelineConfig pipeline1 = pipelineConfig(fooPipeline);
        PipelineConfig pipeline2 = pipelineConfig(barPipeline);
        PipelineConfig pipeline3 = pipelineConfig(bazPipeline);
        when(mockGoConfigService.getAllLocalPipelineConfigs()).thenReturn(asList(pipeline1, pipeline2, pipeline3));

        when(securityService.hasViewPermissionForPipeline(user, fooPipeline)).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, barPipeline)).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, bazPipeline)).thenReturn(false);

        environmentConfigService.syncEnvironments(environmentsConfig("foo-env", fooPipeline));
        List<EnvironmentPipelineModel> pipelines = environmentConfigService.getAllLocalPipelinesForUser(user);


        assertThat(pipelines.size(), is(2));
        assertThat(pipelines, is(asList(new EnvironmentPipelineModel(barPipeline), new EnvironmentPipelineModel(fooPipeline, "foo-env"))));
    }

    @Test
    void getAllLocalPipelinesForUser_shouldReturnOnlyLocalPipelines() {
        Username user = new Username(new CaseInsensitiveString("user"));

        when(mockGoConfigService.getAllLocalPipelineConfigs()).thenReturn(asList(pipelineConfig("foo"), pipelineConfig("bar"), pipelineConfig("baz")));

        when(securityService.hasViewPermissionForPipeline(user, "foo")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "bar")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "baz")).thenReturn(false);

        environmentConfigService.syncEnvironments(environmentsConfig("foo-env", "foo"));
        List<EnvironmentPipelineModel> pipelines = environmentConfigService.getAllLocalPipelinesForUser(user);

        assertThat(pipelines.size(), is(2));
        assertThat(pipelines, is(asList(new EnvironmentPipelineModel("bar"), new EnvironmentPipelineModel("foo", "foo-env"))));
    }

    @Test
    void getAllRemotePipelinesForUserInEnvironment_shouldReturnOnlyRemotelyAssignedPipelinesWhichUserHasPermissionToView() {
        Username user = new Username(new CaseInsensitiveString("user"));

        when(mockGoConfigService.getAllPipelineConfigs()).thenReturn(asList(pipelineConfig("foo"), pipelineConfig("bar"), pipelineConfig("baz")));

        when(securityService.hasViewPermissionForPipeline(user, "foo")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "bar")).thenReturn(true);
        when(securityService.hasViewPermissionForPipeline(user, "baz")).thenReturn(false);

        EnvironmentsConfig environmentConfigs = environmentsConfig("foo-env", "foo");
        EnvironmentConfig fooEnv = environmentConfigs.named(new CaseInsensitiveString("foo-env"));
        fooEnv.setOrigins(new RepoConfigOrigin());
        environmentConfigService.syncEnvironments(environmentConfigs);
        List<EnvironmentPipelineModel> pipelines = environmentConfigService.getAllRemotePipelinesForUserInEnvironment(user, fooEnv);


        assertThat(pipelines.size(), is(1));
        assertThat(pipelines, is(singletonList(new EnvironmentPipelineModel("foo", "foo-env"))));
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
        environmentConfigService.syncEnvironments(environments);
        when(mockGoConfigService.getMergedConfigForEditing()).thenReturn(config);
        assertThat(environmentConfigService.getMergedEnvironmentforDisplay("foo", result).getConfigElement(), is(env));
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    void shouldReturnResultWithMessageThatConfigWasMerged_WhenMergingEnvironmentChanges_NewUpdateEnvironmentMethod() {
        String environmentName = "env_name";
        EnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        Username user = new Username(new CaseInsensitiveString("user"));
        environmentConfigService.syncEnvironments(environments("uat", "prod", "env_name"));

        when(securityService.isUserAdmin(user)).thenReturn(true);
        when(mockGoConfigService.updateConfig(any(UpdateConfigCommand.class))).thenReturn(ConfigSaveState.MERGED);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String md5 = "md5";
        environmentConfigService.updateEnvironment(environmentConfig.name().toString(), environmentConfig, user, md5, result);

        assertThat(result.isSuccessful(), is(true));
        assertThat(result.message(), is("Updated environment 'env_name'."));
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

        assertThat(result.isSuccessful(), is(true));
        assertThat(result.message(), is("Updated environment 'env_name'."));
    }

    @Test
    void shouldReturnEnvironmentConfig() {
        String environmentName = "foo-environment";
        String pipelineName = "up42";
        environmentConfigService.syncEnvironments(environmentsConfig(environmentName, pipelineName));
        EnvironmentConfig expectedEnvironmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        expectedEnvironmentConfig.addPipeline(new CaseInsensitiveString(pipelineName));
        assertThat(environmentConfigService.getEnvironmentConfig(environmentName), is(expectedEnvironmentConfig));
    }

    @Test
    void shouldThrowExceptionWhenEnvironmentIsAbsent() {
        String environmentName = "foo-environment";
        String pipelineName = "up42";
        environmentConfigService.syncEnvironments(environmentsConfig(environmentName, pipelineName));
        assertThatCode(() -> environmentConfigService.getEnvironmentConfig("invalid-environment-name"))
                .isInstanceOf(RecordNotFoundException.class);
    }

    @Test
    void shouldReturnNullWhenEnvIsNotPresent() {
        String envName = "foo-environment";
        String pipelineName = "up42";
        environmentConfigService.syncEnvironments(environmentsConfig(envName, pipelineName));

        EnvironmentConfig envConfig = environmentConfigService.find("invalid-environment-name");

        assertThat(envConfig, is(nullValue()));
    }

    @Test
    void shouldReturnEnvConfigWhenEnvIsPresent() {
        String envName = "foo-environment";
        String pipelineName = "up42";
        environmentConfigService.syncEnvironments(environmentsConfig(envName, pipelineName));

        EnvironmentConfig envConfig = environmentConfigService.find(envName);

        assertThat(envConfig, is(not(nullValue())));
        assertThat(envConfig.name(), is(new CaseInsensitiveString(envName)));
    }

    @Nested
    class EnvironmentForPipeline {
        @Test
        void shouldReturnEnvironmentForGivenPipeline() {
            String environmentName = "foo-environment";
            String pipelineName = "up42";
            EnvironmentsConfig environmentsConfig = environmentsConfig(environmentName, pipelineName);
            environmentConfigService.syncEnvironments(environmentsConfig);

            assertThat(environmentConfigService.environmentForPipeline("up42"), is(environmentsConfig.get(0)));
        }

        @Test
        void shouldReturnNullIfPipelineIsNotAssociatedWithEnvironment() {
            String environmentName = "foo-environment";
            String pipelineName = "up42";
            EnvironmentsConfig environmentConfig = environmentsConfig(environmentName, pipelineName);
            environmentConfigService.syncEnvironments(environmentConfig);

            assertThat(environmentConfigService.environmentForPipeline("foo"), is(nullValue()));
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
                    "test", "uuid1",
                    "stage", "uuid2",
                    "prod", "uuid2"
            ));
        }

        @Test
        void shouldSyncEnvironmentsFromAgentAssociationInDB2() {
            shouldSyncEnvironmentsFromAgentAssociationInDB(ImmutableMap.of(
                    "dev", "uuid1",
                    "stage", "uuid2"
            ));
        }

        @Test
        void shouldSyncEnvironmentsFromAgentAssociationInDB5() {
            shouldSyncEnvironmentsFromAgentAssociationInDB(ImmutableMap.of(
                    "dev", "uuid3",
                    "test", "uuid3",
                    "stage", "uuid3",
                    "prod", "uuid3"
            ));
        }

        void shouldSyncEnvironmentsFromAgentAssociationInDB(Map<String, String> envNameToAgentMap) {
            initializeAgents();
            String uuid1 = "uuid1";
            String uuid2 = "uuid2";

            environmentConfigService.syncEnvironments(createEnvironments(envNameToAgentMap));
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
            AgentInstances agentInstances = new AgentInstances(null);
            Agent agent1 = new Agent("uuid1");
            agent1.setEnvironments("dev,test");

            Agent agent2 = new Agent("uuid2");
            agent2.setEnvironments("stage,prod");

            AgentInstance agentInstance1 = AgentInstance.createFromAgent(agent1, new SystemEnvironment(), null);
            AgentInstance agentInstance2 = AgentInstance.createFromAgent(agent2, new SystemEnvironment(), null);
            agentInstances.add(agentInstance1);
            agentInstances.add(agentInstance2);
            when(agentService.getAgentInstances()).thenReturn(agentInstances);
        }

        private void assertThatAgentAndEnvsAssociationsAreInSync(String uuid1, String uuid2) {
            Set<EnvironmentConfig> envConfigs = environmentConfigService.getEnvironments();
            envConfigs.forEach(env -> {
                String envName = env.name().toString();
                if (envName.equalsIgnoreCase("dev") || envName.equalsIgnoreCase("test")) {
                    assertThat(env.hasAgent(uuid1), is(true));
                    assertThat(env.hasAgent(uuid2), is(false));
                } else if (envName.equalsIgnoreCase("stage") || envName.equalsIgnoreCase("prod")) {
                    assertThat(env.hasAgent(uuid2), is(true));
                    assertThat(env.hasAgent(uuid1), is(false));
                }
            });
        }
    }

    @Test
    void shouldThrowExceptionIfEnvironmentConfigDoesNotExist() {
        String environmentName = "foo-environment";
        String pipelineName = "up42";
        EnvironmentsConfig environmentConfig = environmentsConfig(environmentName, pipelineName);
        environmentConfigService.syncEnvironments(environmentConfig);

        assertThatCode(() -> environmentConfigService.getEnvironmentConfig("non-existent-env-name"))
                .isInstanceOf(RecordNotFoundException.class)
                .hasMessage("Environment with name 'non-existent-env-name' was not found!");
    }

    @Test
    void shouldReturnResultWithMessageOnSuccessInPatchEnv() {
        String environmentName = "env_name";
        EnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        Username user = new Username(new CaseInsensitiveString("user"));
        environmentConfigService.syncEnvironments(environments("uat", "prod", "env_name"));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        environmentConfigService.patchEnvironment(environmentConfig, singletonList("pipeline1"), emptyList(), emptyList(), emptyList(), user, result);

        assertThat(result.isSuccessful(), is(true));
        assertThat(result.message(), is("Updated environment 'env_name'."));
    }

    @Test
    void shouldReturnResultWithFailedMessageIfUserIsNotAuthorizedInPatchEnv() {
        String environmentName = "env_name";
        EnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        Username user = new Username(new CaseInsensitiveString("user"));
        environmentConfigService.syncEnvironments(environments("uat", "prod", "env_name"));

        doThrow(ConfigUpdateCheckFailedException.class)
                .when(mockGoConfigService).updateConfig(any(EntityConfigUpdateCommand.class), any(Username.class));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        environmentConfigService.patchEnvironment(environmentConfig, singletonList("pipeline1"), emptyList(), emptyList(), emptyList(), user, result);

        assertFalse(result.isSuccessful());
        assertEquals(result.message(), "Failed to update environment 'env_name'. ");
    }

    @Test
    void shouldReturnResultWithSuccessMessageOnDeleteEnv() {
        String environmentName = "env_name";
        EnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        Username user = new Username(new CaseInsensitiveString("user"));
        environmentConfigService.syncEnvironments(environments("uat", "prod", "env_name"));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        environmentConfigService.deleteEnvironment(environmentConfig, user, result);

        assertThat(result.isSuccessful(), is(true));
        assertThat(result.message(), is("Environment with name 'env_name' was deleted successfully!"));
    }

    @Test
    void shouldReturnResultWithFailedMessageIfUserIsNotAuthorizedToDeleteEnv() {
        String environmentName = "env_name";
        EnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        Username user = new Username(new CaseInsensitiveString("user"));
        environmentConfigService.syncEnvironments(environments("uat", "prod", "env_name"));

        doThrow(ConfigUpdateCheckFailedException.class)
                .when(mockGoConfigService).updateConfig(any(EntityConfigUpdateCommand.class), any(Username.class));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        environmentConfigService.deleteEnvironment(environmentConfig, user, result);

        assertFalse(result.isSuccessful());
        assertEquals(result.message(), "Failed to delete environment 'env_name'. ");
    }

    @Nested
    class AgentChanged {
        @Test
        void shouldUpdateEnvironmentCacheOnAgentChange() {
            String uuid = "uuid";
            String environmentName = "foo-environment";
            EnvironmentsConfig environments = new EnvironmentsConfig();
            BasicEnvironmentConfig config = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
            config.addAgent(uuid);
            environments.add(config);
            environmentConfigService.syncEnvironments(environments);

            EnvironmentConfig environmentConfig = environmentConfigService.getEnvironmentConfig(environmentName);
            EnvironmentAgentsConfig environmentConfigAgents = environmentConfig.getAgents();
            assertThat(environmentConfigAgents.size(), is(1));
            assertThat(environmentConfigAgents.get(0).getUuid(), is(uuid));

            String newEnvironmentName = "new-environment";
            Agent agentAfterUpdate = new Agent(uuid);
            agentAfterUpdate.addEnvironment(newEnvironmentName);

            environmentConfigService.agentChanged(agentAfterUpdate);

            EnvironmentConfig afterUpdateEnvConfig = environmentConfigService.getEnvironmentConfig(environmentName);
            assertThat(afterUpdateEnvConfig.getAgents().size(), is(0));
        }

        @Test
        void shouldNotAddEnvironmentToCacheIfNotAlreadyPresent() {
            String uuid = "uuid";
            String environmentName = "foo-environment";
            EnvironmentsConfig environments = new EnvironmentsConfig();
            environmentConfigService.syncEnvironments(environments);

            assertThatCode(() -> environmentConfigService.getEnvironmentConfig(environmentName))
                    .isInstanceOf(RecordNotFoundException.class);

            Agent agentAfterUpdate = new Agent(uuid);
            agentAfterUpdate.addEnvironment(environmentName);

            environmentConfigService.agentChanged(agentAfterUpdate);

            assertThatCode(() -> environmentConfigService.getEnvironmentConfig(environmentName))
                    .isInstanceOf(RecordNotFoundException.class);
        }

        @Test
        void shouldNotAddAgentIfAlreadyAssociatedWithIt() {
            String uuid = "uuid";
            String environmentName = "foo-environment";
            EnvironmentsConfig environments = new EnvironmentsConfig();
            BasicEnvironmentConfig config = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
            config.addAgent(uuid);
            environments.add(config);
            environmentConfigService.syncEnvironments(environments);

            EnvironmentConfig environmentConfig = environmentConfigService.getEnvironmentConfig(environmentName);
            EnvironmentAgentsConfig environmentConfigAgents = environmentConfig.getAgents();
            assertThat(environmentConfigAgents.size(), is(1));
            assertThat(environmentConfigAgents.get(0).getUuid(), is(uuid));

            Agent agentAfterUpdate = new Agent(uuid);
            agentAfterUpdate.addEnvironment(environmentName);

            environmentConfigService.agentChanged(agentAfterUpdate);

            EnvironmentConfig afterUpdateEnvConfig = environmentConfigService.getEnvironmentConfig(environmentName);
            assertThat(afterUpdateEnvConfig.getAgents().size(), is(1));
            assertThat(environmentConfigAgents.get(0).getUuid(), is(uuid));
        }
    }

    @Test
    void shouldRemoveAgentFromEnvCacheOnAgentDeletion() {
        String uuid = "uuid";
        String environmentName = "foo-environment";
        EnvironmentsConfig environments = new EnvironmentsConfig();
        BasicEnvironmentConfig config = new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName));
        config.addAgent(uuid);
        environments.add(config);
        environmentConfigService.syncEnvironments(environments);

        EnvironmentConfig environmentConfig = environmentConfigService.getEnvironmentConfig(environmentName);
        EnvironmentAgentsConfig environmentConfigAgents = environmentConfig.getAgents();
        assertThat(environmentConfigAgents.size(), is(1));
        assertThat(environmentConfigAgents.get(0).getUuid(), is(uuid));

        Agent agentDeleted = new Agent(uuid);
        agentDeleted.addEnvironment(environmentName);
        environmentConfigService.agentDeleted(agentDeleted);

        EnvironmentConfig afterUpdateEnvConfig = environmentConfigService.getEnvironmentConfig(environmentName);
        assertThat(afterUpdateEnvConfig.getAgents().size(), is(0));
    }

    @Test
    void shouldSyncAgentsFromDB() {
        String uuid = "uuid";
        String environmentName = "foo-environment";
        EnvironmentsConfig environments = new EnvironmentsConfig();
        environments.add(new BasicEnvironmentConfig(new CaseInsensitiveString(environmentName)));
        environmentConfigService.syncEnvironments(environments);

        EnvironmentConfig environmentConfig = environmentConfigService.getEnvironmentConfig(environmentName);
        assertThat(environmentConfig.getAgents().size(), is(0));

        Agent agent = new Agent(uuid);
        agent.addEnvironment(environmentName);

        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, new SystemEnvironment(), null);
        AgentInstances agentInstances = new AgentInstances(null);
        agentInstances.add(agentInstance);

        when(agentService.getAgentInstances()).thenReturn(agentInstances);
        environmentConfigService.syncEnvironments(environments);

        EnvironmentConfig afterUpdateEnvConfig = environmentConfigService.getEnvironmentConfig(environmentName);
        EnvironmentAgentsConfig afterUpdateEnvConfigAgents = afterUpdateEnvConfig.getAgents();
        assertThat(afterUpdateEnvConfigAgents.size(), is(1));
        assertThat(afterUpdateEnvConfigAgents.get(0).getUuid(), is(uuid));
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
        environmentConfigService.syncEnvironments(environments);

        EnvironmentAgentsConfig envConfigAgents1 = environmentConfigService.getEnvironmentConfig(envName1).getAgents();
        assertThat(envConfigAgents1.size(), is(1));
        assertThat(envConfigAgents1.get(0).getUuid(), is(uuid));

        Agent agent = new Agent(uuid);
        agent.addEnvironments(asList(envName, envName1));
        AgentInstances agentInstances = new AgentInstances(null);
        AgentInstance agentInstance = AgentInstance.createFromAgent(agent, new SystemEnvironment(), null);
        agentInstances.add(agentInstance);
        when(agentService.getAgentInstances()).thenReturn(agentInstances);

        environmentConfigService.syncEnvironments(environments);

        EnvironmentAgentsConfig afterUpdateEnvConfigAgents = environmentConfigService.getEnvironmentConfig(envName).getAgents();
        assertThat(afterUpdateEnvConfigAgents.size(), is(1));
        assertThat(afterUpdateEnvConfigAgents.get(0).getUuid(), is(uuid));

        EnvironmentAgentsConfig afterUpdateEnvConfigAgents1 = environmentConfigService.getEnvironmentConfig(envName1).getAgents();
        assertThat(afterUpdateEnvConfigAgents1.size(), is(1));

    }

    @Test
    void shouldNotUpdateTheCacheIfNullIsPassedForSync() {
        EnvironmentsConfig environments = new EnvironmentsConfig();
        environments.add(new BasicEnvironmentConfig(new CaseInsensitiveString("uat")));
        environments.add(new BasicEnvironmentConfig(new CaseInsensitiveString("prod")));

        environmentConfigService.syncEnvironments(environments);

        assertThat(environmentConfigService.getEnvironments().size(), is(2));
        assertThat(environmentConfigService.getEnvironmentNames(), Matchers.containsInAnyOrder("uat", "prod"));

        environmentConfigService.syncEnvironments(null);

        // only 1 time (for the first sync call)
        verify(agentService, times(1)).getAgentInstances();
        assertThat(environmentConfigService.getEnvironments().size(), is(2));
        assertThat(environmentConfigService.getEnvironmentNames(), Matchers.containsInAnyOrder("uat", "prod"));
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
        return new DefaultJobPlan(new Resources(), new ArrayList<>(), 1L, jobIdentifier, null, new EnvironmentVariables(), new EnvironmentVariables(), null, null);
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
