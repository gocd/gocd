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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.messaging.SendEmailMessage;
import com.thoughtworks.go.server.messaging.notifications.AgentStatusChangeNotifier;
import com.thoughtworks.go.server.persistence.AgentDao;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.ui.AgentViewModel;
import com.thoughtworks.go.server.ui.AgentsViewModel;
import com.thoughtworks.go.server.util.UuidGenerator;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.*;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static com.thoughtworks.go.domain.AgentConfigStatus.Pending;
import static com.thoughtworks.go.helper.AgentInstanceMother.*;
import static com.thoughtworks.go.server.service.AgentRuntimeInfo.fromServer;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static com.thoughtworks.go.util.TriState.TRUE;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml",
        "classpath:WEB-INF/spring-all-servlet.xml"
})

public class AgentServiceIntegrationTest {
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private EnvironmentConfigService environmentConfigService;
    @Autowired
    private AgentService agentService;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private CachedGoConfig cachedGoConfig;
    @Autowired
    private SecurityService securityService;
    @Autowired
    private ServerHealthService serverHealthService;
    @Autowired
    private AgentDao agentDao;
    @Autowired
    private ConfigRepository configRepository;
    @Autowired
    private DatabaseAccessHelper dbHelper;

    private static final GoConfigFileHelper CONFIG_HELPER = new GoConfigFileHelper();
    private static final String UUID = "uuid";
    private static final String UUID2 = "uuid2";
    private static final String UUID3 = "uuid3";

    private static final Username USERNAME = new Username(new CaseInsensitiveString("admin"));

    private static final List<String> emptyStrList = emptyList();
    private static final EnvironmentsConfig emptyEnvsConfig = new EnvironmentsConfig();

    @BeforeEach
    public void setUp() throws Exception {
        CONFIG_HELPER.usingCruiseConfigDao(goConfigDao);
        CONFIG_HELPER.onSetUp();
        dbHelper.onSetUp();
        cachedGoConfig.clearListeners();
        agentDao.clearListeners();
        agentService.clearAll();
        agentService.initialize();
        environmentConfigService.initialize();
    }

    @AfterEach
    public void tearDown() throws Exception {
        new SystemEnvironment().setProperty("agent.connection.timeout", "300");
        CONFIG_HELPER.usingCruiseConfigDao(goConfigDao);
        CONFIG_HELPER.onTearDown();
        cachedGoConfig.clearListeners();
        agentService.clearAll();
        dbHelper.onTearDown();
    }

    private AgentService getAgentService(AgentInstances agentInstances) {
        return new AgentService(new SystemEnvironment(), agentInstances, agentDao,
                new UuidGenerator(), serverHealthService, agentStatusChangeNotifier());
    }

    @Nested
    @ContextConfiguration(locations = {"classpath:WEB-INF/applicationContext-global.xml", "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
            "classpath:testPropertyConfigurer.xml", "classpath:WEB-INF/spring-all-servlet.xml"})
    class DeleteAgents {
        @Test
        void onlyDisabledAgentsShouldBeAllowedToBeDeleted() {
            createAnIdleAgentAndDisableIt(UUID);
            createEnabledAgent(UUID2);

            assertThat(agentService.getAgentInstances().size(), is(2));

            HttpOperationResult result = new HttpOperationResult();

            agentService.deleteAgents(singletonList(UUID), result);
            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Deleted 1 agent(s)."));

            result = new HttpOperationResult();
            agentService.deleteAgents(singletonList(UUID2), result);
            assertThat(result.httpCode(), is(406));
            assertThat(result.message(), is("Failed to delete an agent, as it is not in a disabled state or is still building."));

            assertThat(agentService.getAgentInstances().size(), is(1));
            assertTrue(agentService.getAgentInstances().hasAgent(UUID2));
        }

        @Test
        void shouldDeleteAgentsOnlyWhenAllRequestedAgentsAreDisabled() {
            createAnIdleAgentAndDisableIt(UUID);
            createAnIdleAgentAndDisableIt(UUID2);
            createEnabledAgent(UUID3);

            assertThat(agentService.getAgentInstances().size(), is(3));

            HttpOperationResult result = new HttpOperationResult();
            agentService.deleteAgents(asList(UUID, UUID2, UUID3), result);

            assertThat(result.httpCode(), is(406));
            assertThat(result.message(), is("Could not delete any agents, as one or more agents might not be disabled or are still building."));

            assertThat(agentService.getAgentInstances().size(), is(3));

            agentService.deleteAgents(asList(UUID, UUID2), result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Deleted 2 agent(s)."));

            assertThat(agentService.getAgentInstances().size(), is(1));
        }

        @Test
        void shouldNotBeAbleToDeleteDisabledAgentWhoseRuntimeStatusIsBuilding() {
            createDisabledAgentWithBuildingRuntimeStatus(UUID);

            assertThat(agentService.getAgentInstances().size(), is(1));

            HttpOperationResult result = new HttpOperationResult();
            agentService.deleteAgents(asList(UUID), result);

            assertThat(result.httpCode(), is(406));
            assertThat(result.message(), is("Failed to delete an agent, as it is not in a disabled state or is still building."));

            assertThat(agentService.getAgentInstances().size(), is(1));
            assertTrue(agentService.getAgentInstances().hasAgent(UUID));
        }

        @Test
        void shouldFindAgentViewModelForAnExistingAgent() {
            Agent agent = createEnabledAgent(UUID);
            String env = "prod";
            agent.setEnvironments(env);
            agentDao.saveOrUpdate(agent);

            AgentViewModel actual = agentService.findAgentViewModel(UUID);
            assertThat(actual.getUuid(), is(UUID));
            HashSet<String> envSet = new HashSet<>();
            envSet.add(env);
            assertThat(actual.getEnvironments(), is(envSet));
        }

        @Test
        void shouldBeAbleToDeleteDisabledAgentInIdleState() {
            createAnIdleAgentAndDisableIt(UUID);
            createEnabledAgent(UUID2);

            assertThat(agentService.getAgentInstances().size(), is(2));

            HttpOperationResult result = new HttpOperationResult();
            agentService.deleteAgents(singletonList(UUID), result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Deleted 1 agent(s)."));

            assertThat(agentService.getAgentInstances().size(), is(1));
        }

        @Test
        void shouldReturn404WhenDeleteAgentsIsCalledWithUnknownAgentUUID() {
            HttpOperationResult result = new HttpOperationResult();
            String unknownUUID = "unknown-agent-id";
            agentService.deleteAgents(singletonList(unknownUUID), result);
            assertThat(result.httpCode(), is(404));
            assertThat(result.message(), is("Not Found"));
            assertThat(result.getServerHealthState().getDescription(), is(format("Agent '%s' not found", unknownUUID)));
        }
    }

    @Test
    void getRegisteredAgentsViewModelShouldNotReturnNotRegisteredAgentsViewModel() {
        AgentInstance idle = AgentInstanceMother.updateUuid(idle(new Date(), "CCeDev01"), UUID);
        AgentInstance pending = pending();
        AgentInstance building = building();
        AgentInstance denied = disabled();

        AgentInstances instances = new AgentInstances(new SystemEnvironment(), agentStatusChangeListener(), idle, pending, building, denied);
        AgentService agentService = getAgentService(instances);

        AgentsViewModel agentViewModels = agentService.getRegisteredAgentsViewModel();
        assertThat(agentViewModels.size(), is(3));
        agentViewModels.forEach(agentViewModel -> assertThat(agentViewModel.getStatus().getConfigStatus(), not(is(Pending))));
    }

    @Test
    void getRegisteredAgentsViewModelShouldReturnEmptyRegisteredAgentsViewModelWhenThereAreNoRegisteredAgents() {
        AgentInstances agentInstances = new AgentInstances(new SystemEnvironment(), agentStatusChangeListener());
        AgentService agentService = getAgentService(agentInstances);

        AgentsViewModel agentViewModels = agentService.getRegisteredAgentsViewModel();
        assertThat(agentViewModels.size(), is(0));
    }


    @Nested
    @ContextConfiguration(locations = {"classpath:WEB-INF/applicationContext-global.xml", "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
            "classpath:testPropertyConfigurer.xml", "classpath:WEB-INF/spring-all-servlet.xml"})
    class PendingAgents {
        @Test
        void shouldBeAbleToDisableAPendingAgent() {
            String uuid = DatabaseAccessHelper.AGENT_UUID;
            Agent agent = new Agent(uuid, "agentName", "50.40.30.9");
            AgentRuntimeInfo agentRuntime = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle,
                    currentWorkingDirectory(), "cookie", false);
            agentRuntime.busy(new AgentBuildingInfo("path", "buildLocator"));
            agentService.requestRegistration(agentRuntime);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(asList(uuid), emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.FALSE, result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is(String.format("Updated agent(s) with uuid(s): [%s].", uuid)));

            AgentInstances agents = agentService.getAgentInstances();

            assertThat(agents.size(), is(1));
            assertThat(agents.findAgent(uuid).isDisabled(), is(true));
            assertThat(agentService.findAgentAndRefreshStatus(uuid).isDisabled(), is(true));
            assertThat(agentService.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Disabled));
        }

        @Test
        void shouldBeAbleToEnableAPendingAgent() {
            String uuid = DatabaseAccessHelper.AGENT_UUID;
            Agent agent = new Agent(uuid, "agentName", "50.40.30.9");
            AgentRuntimeInfo agentRuntime = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false);
            agentRuntime.busy(new AgentBuildingInfo("path", "buildLocator"));
            agentService.requestRegistration(agentRuntime);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(singletonList(uuid), emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TRUE, result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is(String.format("Updated agent(s) with uuid(s): [%s].", uuid)));
        }

        @Test
        void shouldNotAllowAnyUpdateOperationOnPendingAgent() {
            AgentRuntimeInfo pendingAgent = fromServer(new Agent(UUID, "CCeDev03", "10.18.5.3", asList("db", "web")),
                    false, "/var/lib", 0L, "linux", false);
            agentService.requestRegistration(pendingAgent);
            assertThat(agentService.findAgent(UUID).isRegistered(), is(false));

            HttpLocalizedOperationResult actualResult = new HttpLocalizedOperationResult();
            ArrayList<String> uuids = new ArrayList<>(singletonList(pendingAgent.getUUId()));
            ArrayList<String> resourcesToAdd = new ArrayList<>(singletonList("Linux"));
            ArrayList<String> resourcesToRemove = new ArrayList<>(singletonList("Gauge"));

            agentService.bulkUpdateAgentAttributes(uuids, resourcesToAdd, resourcesToRemove, emptyEnvsConfig, emptyStrList, TriState.UNSET, actualResult);

            HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
            expectedResult.badRequest("Pending agents [" + pendingAgent.getUUId() + "] must be explicitly enabled or disabled when performing any operations on them.");
            assertThat(actualResult, is(expectedResult));
        }
    }

    @Nested
    @ContextConfiguration(locations = {"classpath:WEB-INF/applicationContext-global.xml", "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
            "classpath:testPropertyConfigurer.xml", "classpath:WEB-INF/spring-all-servlet.xml"})
    class EnableDisableAgents {
        @Test
        void shouldBeAbleToDisableAgentUsingUpdateAgentAttributesCall() {
            createEnabledAgent(UUID2);

            assertThat(agentService.getAgentInstances().size(), is(1));
            assertThat(getFirstAgent().isDisabled(), is(false));

            HttpOperationResult result = new HttpOperationResult();
            agentService.updateAgentAttributes(UUID2, null, null, null, TriState.FALSE, result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent with uuid uuid2."));

            assertThat(agentService.getAgentInstances().size(), is(1));
            assertThat(getFirstAgent().isDisabled(), is(true));
        }

        @Test
        void shouldBeAbleToDisableBuildingAgent() {
            String uuid = DatabaseAccessHelper.AGENT_UUID;
            Agent agent = new Agent(uuid, "agentName", "127.0.0.9");
            requestRegistrationAndApproveAgent(agent);

            AgentIdentifier agentIdentifier = agent.getAgentIdentifier();
            String cookie = agentService.assignCookie(agentIdentifier);
            AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), cookie, false);
            agentRuntimeInfo.busy(new AgentBuildingInfo("path", "buildLocator"));

            agentService.updateRuntimeInfo(agentRuntimeInfo);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            List<String> emptyList = emptyStrList;
            agentService.bulkUpdateAgentAttributes(asList(uuid), emptyList, emptyList, emptyEnvsConfig, emptyList, TriState.FALSE, result);

            assertThat(agentService.agents().getAgentByUUID(uuid).isDisabled(), is(true));
        }

        @Test
        void shouldBeAbleToEnableADisabledAgent() {
            String agentId = DatabaseAccessHelper.AGENT_UUID;
            Agent agent = new Agent(agentId, "agentName", "127.0.0.9");
            requestRegistrationAndApproveAgent(agent);
            disableAgent(agent);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(asList(agentId), emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TRUE, result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is(String.format("Updated agent(s) with uuid(s): [%s].", agentId)));
            assertThat(isDisabled(agent), is(false));
        }

        @Test
        void shouldBeAbleToEnabledMultipleAgentsUsingBulkUpdateCall() {
            Agent agent1 = createDisabledAgentWithBuildingRuntimeStatus(UUID);
            Agent agent2 = createDisabledAgentWithBuildingRuntimeStatus(UUID2);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(asList(UUID, UUID2), emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TRUE, result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), containsString("Updated agent(s) with uuid(s): [uuid, uuid2]."));
            assertThat(isDisabled(agent1), is(false));
            assertThat(isDisabled(agent2), is(false));
        }

        @Test
        void shouldBeAbleToDisableMultipleAgentsUsingBulkUpdateCall() {
            Agent agent1 = createEnabledAgent(UUID);
            Agent agent2 = createEnabledAgent(UUID2);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(asList(UUID, UUID2), emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.FALSE, result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), containsString("Updated agent(s) with uuid(s): [uuid, uuid2]."));
            assertThat(isDisabled(agent1), is(true));
            assertThat(isDisabled(agent2), is(true));
        }

        @Test
        void shouldReturn200WhenAnAlreadyEnableAgentIsEnabled() {
            String uuid = DatabaseAccessHelper.AGENT_UUID;
            Agent agent = new Agent(uuid, "agentName", "127.0.0.9");
            requestRegistrationAndApproveAgent(agent);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(singletonList(uuid), emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TRUE, result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is(String.format("Updated agent(s) with uuid(s): [%s].", uuid)));
        }

        @Test
        void shouldReturn200WhenAnAlreadyDisabledAgentIsDisabled() {
            String uuid = DatabaseAccessHelper.AGENT_UUID;
            Agent agent = new Agent(uuid, "agentName", "127.0.0.9");
            requestRegistrationAndApproveAgent(agent);
            disableAgent(agent);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(asList(uuid), emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.FALSE, result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is(String.format("Updated agent(s) with uuid(s): [%s].", uuid)));
        }

        @Test
        void shouldNotBeAbleToEnableDisableWhenTriStateIsUnset() {
            createEnabledAgent("enabled");
            createDisabledAgentWithBuildingRuntimeStatus("disabled");

            assertThat(agentService.getAgentInstances().size(), is(2));
            assertThat(agentService.findAgentAndRefreshStatus("enabled").getAgent().isDisabled(), is(false));
            assertThat(agentService.findAgentAndRefreshStatus("disabled").getAgent().isDisabled(), is(true));

            HttpOperationResult result = new HttpOperationResult();
            agentService.updateAgentAttributes("enabled", "new.enabled.hostname", "linux,java", null, TriState.UNSET, result);
            assertThat(result.httpCode(), is(200));

            agentService.updateAgentAttributes("disabled", "new.disabled.hostname", "linux,java", null, TriState.UNSET, result);
            assertThat(result.httpCode(), is(200));

            assertThat(agentService.getAgentInstances().size(), is(2));
            assertThat(agentService.findAgentAndRefreshStatus("enabled").getAgent().isDisabled(), is(false));
            assertThat(agentService.findAgentAndRefreshStatus("disabled").getAgent().isDisabled(), is(true));
        }

        @Test
        void shouldAllowEnablingThePendingAndDisabledAgentsTogether() {
            AgentRuntimeInfo pendingAgent = fromServer(new Agent(UUID, "CCeDev03", "10.18.5.3", asList("db", "web")), false, "/var/lib", 0L, "linux", false);
            Agent agent = new Agent(UUID2, "remote-host1", "50.40.30.21");

            agentService.requestRegistration(pendingAgent);
            agentService.register(agent, null, null);
            agentService.disableAgents(agent.getUuid());

            assertThat(agentService.findAgent(UUID).isRegistered(), is(false));
            assertThat(agentService.findAgent(agent.getUuid()).isDisabled(), is(true));

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            List<String> uuids = asList(UUID, agent.getUuid());

            agentService.bulkUpdateAgentAttributes(uuids, emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TRUE, result);

            assertTrue(result.isSuccessful());
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [" + StringUtils.join(uuids, ", ") + "]."));
            assertThat(agentService.agents().getAgentByUUID(UUID).isEnabled(), is(true));
            assertThat(agentService.agents().getAgentByUUID(agent.getUuid()).isEnabled(), is(true));
        }

        @Test
        void shouldNotDisableAnyAgentsAndShouldThrow400WhenEvenOneOfTheUUIDIsInvalid() {
            Agent agent = new Agent(UUID, "remote-host1", "50.40.30.21");
            Agent agent1 = new Agent(UUID2, "remote-host2", "50.40.30.22");

            agentService.register(agent, null, null);
            agentService.register(agent1, null, null);

            assertFalse(agentService.findAgent(agent.getUuid()).isDisabled());
            assertFalse(agentService.findAgent(agent1.getUuid()).isDisabled());

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            List<String> uuids = asList(agent.getUuid(), agent1.getUuid(), "invalid-uuid");

            agentService.bulkUpdateAgentAttributes(uuids, emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.FALSE, result);

            assertFalse(agentService.findAgent(agent.getUuid()).isDisabled());
            assertFalse(agentService.findAgent(agent1.getUuid()).isDisabled());

            assertFalse(result.isSuccessful());
            assertThat(result.httpCode(), is(400));
            assertThat(result.message(), is(EntityType.Agent.notFoundMessage(singletonList("invalid-uuid"))));
        }

        @Test
        void shouldNotEnableAnyAgentsAndShouldThrow400WhenEvenOneOfTheUUIDIsInvalid() {
            Agent agent = new Agent(UUID, "remote-host1", "50.40.30.21");
            Agent agent1 = new Agent(UUID2, "remote-host2", "50.40.30.22");

            agentService.register(agent, null, null);
            agentService.register(agent1, null, null);

            assertFalse(agentService.findAgent(agent.getUuid()).isDisabled());
            assertFalse(agentService.findAgent(agent1.getUuid()).isDisabled());

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            List<String> uuids = asList(agent.getUuid(), agent1.getUuid(), "invalid-uuid");

            agentService.bulkUpdateAgentAttributes(uuids, emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TRUE, result);

            assertFalse(agentService.findAgent(agent.getUuid()).isDisabled());
            assertFalse(agentService.findAgent(agent1.getUuid()).isDisabled());

            assertFalse(result.isSuccessful());
            assertThat(result.httpCode(), is(400));
            assertThat(result.message(), is(EntityType.Agent.notFoundMessage(singletonList("invalid-uuid"))));
        }
    }

    @Nested
    @ContextConfiguration(locations = {"classpath:WEB-INF/applicationContext-global.xml", "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
            "classpath:testPropertyConfigurer.xml", "classpath:WEB-INF/spring-all-servlet.xml"})
    class ErrorConditions {
        @Test
        void shouldReturn400WhenUpdatingAnUnknownAgent() {
            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(singletonList("unknown-agent-id"), emptyStrList,
                    emptyStrList, emptyEnvsConfig, emptyStrList, TRUE, result);

            assertThat(result.httpCode(), is(400));
            assertThat(result.message(), is("Agents with uuids 'unknown-agent-id' were not found!"));
        }

        @Test
        void shouldReturn404WhenAgentToBeDeletedDoesNotExist() {
            String unknownUUID = "unknown-agent-id";
            HttpOperationResult result = new HttpOperationResult();

            agentService.deleteAgents(singletonList(unknownUUID), result);
            assertThat(result.httpCode(), is(404));
            assertThat(result.message(), is("Not Found"));
            assertThat(result.getServerHealthState().getDescription(), is(format("Agent '%s' not found", unknownUUID)));
        }

        @Test
        void shouldThrow422WhenUpdatingAgentWithInvalidInputs() {
            Agent agent = createAnIdleAgentAndDisableIt(UUID);
            String originalHostname = agent.getHostname();
            List<String> originalResourceNames = agent.getResourcesAsList();

            assertThat(agentService.getAgentInstances().size(), is(1));
            assertThat(getFirstAgent().getHostname(), is(not("some-hostname")));

            HttpOperationResult result = new HttpOperationResult();
            String invalidResourceName = "lin!ux";
            AgentInstance agentInstance = agentService.updateAgentAttributes(UUID, "some-hostname",
                    invalidResourceName, null, TriState.UNSET, result);

            assertThat(result.httpCode(), is(422));
            assertThat(result.message(), is("Updating agent failed."));
            assertThat(agentInstance.getAgent().errors().on(JobConfig.RESOURCES),
                    is("Resource name 'lin!ux' is not valid. Valid names much match '^[-\\w\\s|.]*$'"));

            assertThat(agentService.getAgentInstances().size(), is(1));
            assertThat(getFirstAgent().getHostname(), is(originalHostname));
            assertThat(getFirstAgent().getResourceConfigs().resourceNames(), is(originalResourceNames));
        }

        @Test
        void shouldThrowExceptionWhenADuplicateAgentTriesToUpdateStatus() {
            AgentInstance buildingAgentInstance = building();
            AgentService agentService = getAgentService(new AgentInstances(new SystemEnvironment(), agentStatusChangeListener(), buildingAgentInstance));
            AgentInstances agentInstances = agentService.findRegisteredAgents();

            String uuid = buildingAgentInstance.getAgent().getUuid();
            assertThat(agentInstances.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Building));

            AgentIdentifier identifier = buildingAgentInstance.getAgent().getAgentIdentifier();
            agentDao.associateCookie(identifier, "new_cookie");

            AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(identifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "old_cookie", false);
            AgentWithDuplicateUUIDException e = assertThrows(AgentWithDuplicateUUIDException.class, () -> agentService.updateRuntimeInfo(runtimeInfo));
            assertEquals(e.getMessage(), format("Agent [%s] has invalid cookie", runtimeInfo.agentInfoDebugString()));

            agentInstances = agentService.findRegisteredAgents();
            assertThat(agentInstances.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Building));
            AgentIdentifier agentIdentifier = buildingAgentInstance.getAgent().getAgentIdentifier();
            String cookie = agentService.assignCookie(agentIdentifier);
            agentService.updateRuntimeInfo(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), cookie, false));
        }
    }

    @Nested
    @ContextConfiguration(locations = {"classpath:WEB-INF/applicationContext-global.xml", "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
            "classpath:testPropertyConfigurer.xml", "classpath:WEB-INF/spring-all-servlet.xml"})
    class Update {
        @Test
        void shouldBeAbleToUpdateAgentsHostName() {
            createAnIdleAgentAndDisableIt(UUID);

            assertThat(agentService.getAgentInstances().size(), is(1));
            String someHostName = "some-hostname";
            assertThat(getFirstAgent().getHostname(), is(not(someHostName)));

            HttpOperationResult result = new HttpOperationResult();
            agentService.updateAgentAttributes(UUID, someHostName, null, null, TriState.UNSET, result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent with uuid uuid."));

            assertThat(agentService.getAgentInstances().size(), is(1));
            assertThat(getFirstAgent().getHostname(), is(someHostName));
        }

        @Test
        void shouldUpdateAgentAttributesForValidInputs() {
            createAnIdleAgentAndDisableIt(UUID);
            createEnvironment("a", "b");

            assertThat(agentService.getAgentInstances().size(), is(1));
            assertThat(getFirstAgent().getHostname(), is(not("some-hostname")));
            assertThat(getFirstAgent().isDisabled(), is(true));

            HttpOperationResult result = new HttpOperationResult();
            agentService.updateAgentAttributes(UUID, "some-hostname",
                    "linux,java", createEnvironmentsConfigWith("a", "b"), TRUE, result);

            AgentInstance firstAgent = getFirstAgent();
            List<String> resourceNames = firstAgent.getResourceConfigs().resourceNames();

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent with uuid uuid."));

            assertThat(agentService.getAgentInstances().size(), is(1));
            assertThat(firstAgent.getHostname(), is("some-hostname"));
            assertThat(resourceNames, equalTo(asList("java", "linux")));
            assertThat(firstAgent.isDisabled(), is(false));
            assertEquals(getEnvironments(getFirstAgent().getUuid()), new HashSet<>(asList("a", "b")));
        }

        @Test
        void shouldUpdateAgentStatus() {
            AgentInstance buildingAgentInstance = building();
            AgentService agentService = getAgentService(new AgentInstances(new SystemEnvironment(), agentStatusChangeListener(), buildingAgentInstance));
            AgentInstances registeredAgentInstances = agentService.findRegisteredAgents();

            String uuid = buildingAgentInstance.getAgent().getUuid();
            assertThat(registeredAgentInstances.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Building));

            AgentIdentifier agentIdentifier = buildingAgentInstance.getAgent().getAgentIdentifier();
            String cookie = agentService.assignCookie(agentIdentifier);
            agentService.updateRuntimeInfo(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), cookie, false));

            registeredAgentInstances = agentService.findRegisteredAgents();
            assertThat(registeredAgentInstances.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Idle));
        }

        @Test
        void shouldUpdateOnlyThoseAgentsAttributeThatAreSpecified() {
            createEnvironment("a", "b");
            Agent agent = createAnIdleAgentAndDisableIt(UUID);
            String originalHostname = agent.getHostname();

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(asList(UUID), emptyStrList, emptyStrList, createEnvironmentsConfigWith("a", "b"), emptyStrList, TRUE, result);

            assertTrue(result.isSuccessful());
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [" + StringUtils.join(asList(UUID), ", ") + "]."));
            assertThat(agentService.getAgentInstances().size(), is(1));

            HttpOperationResult result1 = new HttpOperationResult();
            String notSpecifying = null;
            agentService.updateAgentAttributes(UUID, notSpecifying, notSpecifying, null, TriState.UNSET, result1);

            assertThat(result1.httpCode(), is(400));
            assertThat(result1.message(), is("Bad Request. No operation is specified in the request to be performed on agent."));

            assertThat(agentService.getAgentInstances().size(), is(1));
            assertThat(getFirstAgent().getHostname(), is(originalHostname));
            assertEquals(getEnvironments(getFirstAgent().getUuid()), new HashSet<>(asList("a", "b")));
        }

        @Test
        void shouldThrowBadRequestIfNoOperationToPerformOnBulkUpdatingAgents() {
            AgentInstance pendingAgent = pending();
            AgentInstance registeredAgent = disabled();

            AgentInstances instances = new AgentInstances(new SystemEnvironment(), agentStatusChangeListener(), pendingAgent);
            AgentService agentService = getAgentService(instances);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            ArrayList<String> uuids = new ArrayList<>();
            uuids.add(pendingAgent.getUuid());
            uuids.add(registeredAgent.getUuid());

            agentService.bulkUpdateAgentAttributes(uuids, emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.UNSET, result);

            HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
            expectedResult.badRequest("Bad Request. No operation is specified in the request to be performed on agents.");

            assertThat(result, is(expectedResult));
        }

        @Test
        void shouldUpdateResourcesEnvironmentsAndAgentStateOfAllTheProvidedAgentsAllTogether() {
            createEnabledAgent(UUID);
            createEnabledAgent(UUID2);

            createEnvironment("dev");

            EnvironmentConfig dev = environmentConfigService.getEnvironmentConfig("dev");

            assertThat(dev.getAgents().getUuids(), not(containsInAnyOrder(UUID, UUID2)));
            assertFalse(agentService.findAgent(UUID).isDisabled());
            assertFalse(agentService.findAgent(UUID2).isDisabled());
            assertThat(agentService.findAgent(UUID).getResourceConfigs().size(), is(0));
            assertThat(agentService.findAgent(UUID2).getResourceConfigs().size(), is(0));

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            List<String> uuids = asList(UUID, UUID2);
            List<String> resources = singletonList("resource1");
            EnvironmentsConfig environmentsToAdd = createEnvironmentsConfigWith("dev");

            agentService.bulkUpdateAgentAttributes(uuids, resources, emptyStrList, environmentsToAdd, emptyStrList, TriState.FALSE, result);

            assertTrue(result.isSuccessful());
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [" + StringUtils.join(uuids, ", ") + "]."));
            assertTrue(agentService.findAgent(UUID).isDisabled());
            assertTrue(agentService.findAgent(UUID2).isDisabled());
            assertThat(agentService.findAgent(UUID).getResourceConfigs(), contains(new ResourceConfig("resource1")));
            assertThat(agentService.findAgent(UUID2).getResourceConfigs(), contains(new ResourceConfig("resource1")));

            assertThat(environmentConfigService.getEnvironmentConfig("dev").getAgents().getUuids(), containsInAnyOrder(UUID, UUID2));
        }
    }

    @Nested
    @ContextConfiguration(locations = {"classpath:WEB-INF/applicationContext-global.xml", "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
            "classpath:testPropertyConfigurer.xml", "classpath:WEB-INF/spring-all-servlet.xml"})
    class LostContact {
        @Test
        void shouldMarkAgentAsLostContactWhenAgentDoesNotPingWithinTimeoutPeriod() {
            new SystemEnvironment().setProperty("agent.connection.timeout", "-1");
            Date date = new Date(70, 1, 1, 1, 1, 1);
            AgentInstance instance = idle(date, "CCeDev01");
            ((AgentRuntimeInfo) ReflectionUtil.getField(instance, "agentRuntimeInfo")).setOperatingSystem("Minix");

            AgentService agentService = new AgentService(new SystemEnvironment(), agentDao, new UuidGenerator(),
                    serverHealthService, agentStatusChangeNotifier());
            AgentInstances agentInstances = (AgentInstances) ReflectionUtil.getField(agentService, "agentInstances");
            agentInstances.add(instance);

            AgentInstances agents = agentService.findRegisteredAgents();
            assertThat(agents.size(), is(1));
            AgentInstance agentInstance = agents.findAgentAndRefreshStatus(instance.getAgent().getUuid());
            assertThat(agentInstance.getStatus(), is(AgentStatus.LostContact));
        }

        @Test
        void shouldNotSendLostContactEmailWhenAgentStateIsLostContact() {
            new SystemEnvironment().setProperty("agent.connection.timeout", "-1");
            CONFIG_HELPER.addMailHost(new MailHost("ghost.name", 25, "loser", "boozer", true, false, "go@foo.mail.com", "admin@foo.mail.com"));

            Date date = new Date(70, 1, 1, 1, 1, 1);
            AgentInstance idleAgentInstance = idle(date, "CCeDev01");
            ((AgentRuntimeInfo) ReflectionUtil.getField(idleAgentInstance, "agentRuntimeInfo")).setOperatingSystem("Minix");

            EmailSender mailSender = mock(EmailSender.class);
            AgentService agentService = new AgentService(new SystemEnvironment(), agentDao, new UuidGenerator(), serverHealthService, agentStatusChangeNotifier());
            AgentInstances agentInstances = (AgentInstances) ReflectionUtil.getField(agentService, "agentInstances");
            agentInstances.add(idleAgentInstance);

            AgentInstances agents = agentService.findRegisteredAgents();
            assertThat(agents.size(), is(1));

            AgentInstance agentInstance = agents.findAgentAndRefreshStatus(idleAgentInstance.getAgent().getUuid());
            assertThat(agentInstance.getStatus(), is(AgentStatus.LostContact));
            String body = String.format("The email has been sent out automatically by the Go server at (%s) to Go administrators.\n"
                    + "\n"
                    + "The Go server has lost contact with agent:\n"
                    + "\n"
                    + "Agent name: CCeDev01\n"
                    + "Free Space: 10.0 KB\n"
                    + "Sandbox: /var/lib/foo\n"
                    + "IP Address: 10.18.5.1\n"
                    + "OS: Minix\n"
                    + "Resources: \n"
                    + "Environments: \n"
                    + "\n"
                    + "Lost contact at: %s", SystemUtil.getFirstLocalNonLoopbackIpAddress(), date);
            verify(mailSender, never()).sendEmail(new SendEmailMessage("[Lost Contact] Go agent host: " + idleAgentInstance.getHostname(), body, "admin@foo.mail.com"));
        }
    }

    @Nested
    @ContextConfiguration(locations = {"classpath:WEB-INF/applicationContext-global.xml", "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
            "classpath:testPropertyConfigurer.xml", "classpath:WEB-INF/spring-all-servlet.xml"})
    class Resources {
        @Test
        void shouldAddResourcesToDisabledAgent() {
            createAnIdleAgentAndDisableIt(UUID);

            assertThat(agentService.getAgentInstances().size(), is(1));
            assertThat(getFirstAgent().getResourceConfigs(), is(empty()));
            assertThat(agentService.findAgent(UUID).getStatus(), is(AgentStatus.Disabled));

            HttpOperationResult result = new HttpOperationResult();
            agentService.updateAgentAttributes(UUID, null, "linux,java", null, TriState.UNSET, result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent with uuid uuid."));

            assertThat(agentService.getAgentInstances().size(), is(1));
            assertThat(getFirstAgent().getResourceConfigs().resourceNames(), is(asList("java", "linux")));
        }

        @Test
        void shouldAddResourcesWhileBulkUpdatingAgents() {
            createEnabledAgent(UUID);
            createEnabledAgent(UUID2);

            List<String> uuids = asList(UUID, UUID2);
            List<String> resourcesToAdd = asList("resource1", "resource2");

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(uuids, resourcesToAdd, emptyStrList,
                    emptyEnvsConfig, emptyStrList, TRUE, result);
            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));

            List<String> uuidResources = agentService.findAgentAndRefreshStatus(UUID).getAgent().getResourcesAsList();
            assertThat(uuidResources, hasItem("resource1"));
            assertThat(uuidResources, hasItem("resource2"));

            List<String> uuid2Resources = agentService.findAgentAndRefreshStatus(UUID2).getAgent().getResourcesAsList();
            assertThat(uuid2Resources, hasItem("resource1"));
            assertThat(uuid2Resources, hasItem("resource2"));
        }

        @Test
        void shouldNotUpdateResourcesOnElasticAgents() {
            Agent elasticAgent = AgentMother.elasticAgent();

            agentService.register(elasticAgent, null, null);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            List<String> uuids = singletonList(elasticAgent.getUuid());
            List<String> resourcesToAdd = singletonList("resource");

            assertTrue(agentService.findAgent(elasticAgent.getUuid()).getResourceConfigs().isEmpty());
            agentService.bulkUpdateAgentAttributes(uuids, resourcesToAdd, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.UNSET, result);

            HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
            expectedResult.badRequest("Resources on elastic agents with uuids [" + StringUtils.join(uuids, ", ") + "] can not be updated.");

            assertThat(result, is(expectedResult));
            assertTrue(agentService.findAgent(elasticAgent.getUuid()).getResourceConfigs().isEmpty());
        }

        @Test
        void shouldRemoveResourcesFromTheSpecifiedAgents() {
            Agent agent = new Agent(UUID, "remote-host1", "50.40.30.21");
            Agent agent1 = new Agent(UUID2, "remote-host1", "50.40.30.22");

            agentService.register(agent, "resource1,resource2", null);
            agentService.register(agent1, "resource2", null);

            List<String> uuids = asList(UUID, UUID2);
            List<String> resourcesToRemove = singletonList("resource2");

            assertThat(agentService.findAgent(UUID).getResourceConfigs().size(), is(2));
            assertThat(agentService.findAgent(UUID2).getResourceConfigs().size(), is(1));

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(uuids, emptyStrList, resourcesToRemove, emptyEnvsConfig, emptyStrList, TriState.UNSET, result);

            assertTrue(result.isSuccessful());
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [" + StringUtils.join(uuids, ", ") + "]."));
            assertThat(agentService.findAgent(UUID).getResourceConfigs().size(), is(1));
            assertThat(agentService.findAgent(UUID).getResourceConfigs(), contains(new ResourceConfig("resource1")));
            assertThat(agentService.findAgent(UUID2).getResourceConfigs().size(), is(0));
        }

        @Test
        void shouldThrowValidationErrorOnResourceNameForUpdateAgent() {
            createAnIdleAgentAndDisableIt(UUID);

            assertThat(agentService.getAgentInstances().size(), is(1));
            assertThat(getFirstAgent().getResourceConfigs(), is(empty()));
            assertThat(agentService.findAgent(UUID).getStatus(), is(AgentStatus.Disabled));

            HttpOperationResult result = new HttpOperationResult();
            AgentInstance agentInstance = agentService.updateAgentAttributes(UUID, null, "foo%", null, TriState.UNSET, result);

            assertThat(result.httpCode(), is(422));
            assertThat(result.message(), is("Updating agent failed."));

            ConfigErrors configErrors = agentInstance.getAgent().errors();
            assertFalse(configErrors.isEmpty());
            assertEquals("Resource name 'foo%' is not valid. Valid names much match '^[-\\w\\s|.]*$'", configErrors.on("resources"));

            assertThat(agentService.getAgentInstances().size(), is(1));
            assertThat(getFirstAgent().getResourceConfigs().resourceNames(), is(emptyStrList));
        }

        @Test
        void shouldThrowValidationErrorOnResourceNameForUpdateBulkAgent() {
            createAnIdleAgentAndDisableIt(UUID);

            assertThat(agentService.getAgentInstances().size(), is(1));
            assertThat(getFirstAgent().getResourceConfigs(), is(empty()));
            assertThat(agentService.findAgent(UUID).getStatus(), is(AgentStatus.Disabled));

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(singletonList(UUID), singletonList("foo%"), emptyStrList, emptyEnvsConfig, emptyStrList, TriState.UNSET, result);

            assertThat(result.httpCode(), is(422));
            assertThat(result.message(), is("Validations failed for bulk update of agents. Error(s): {resources=[Resource name 'foo%' is not valid. Valid names much match '^[-\\w\\s|.]*$']}"));

            assertThat(agentService.getAgentInstances().size(), is(1));
            assertThat(getFirstAgent().getResourceConfigs(), is(empty()));
        }
    }

    @Nested
    @ContextConfiguration(locations = {"classpath:WEB-INF/applicationContext-global.xml", "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
            "classpath:testPropertyConfigurer.xml", "classpath:WEB-INF/spring-all-servlet.xml"})
    class RegistrationAndApproval {
        @Test
        void shouldRegisterLocalAgentWithNonLoopbackIpAddress() throws Exception {
            String nonLoopbackIp = SystemUtil.getFirstLocalNonLoopbackIpAddress();
            InetAddress nonLoopbackIpAddress = InetAddress.getByName(nonLoopbackIp);
            assertThat(SystemUtil.isLocalIpAddress(nonLoopbackIp), is(true));

            Agent agent = new Agent("uuid", nonLoopbackIpAddress.getHostName(), nonLoopbackIp);
            AgentRuntimeInfo agentRuntimeInfo = fromServer(agent, false, "/var/lib", 0L, "linux", false);
            agentService.requestRegistration(agentRuntimeInfo);

            AgentInstance agentInstance = agentService.findRegisteredAgents().findAgentAndRefreshStatus("uuid");

            assertThat(agentInstance.getAgent().getIpaddress(), is(nonLoopbackIp));
            assertThat(agentInstance.getStatus(), is(AgentStatus.Idle));
        }

        @Test
        void shouldBeAbleToRegisterPendingAgent() {
            AgentInstance pendingAgentInstance = pending();
            Agent pendingAgent = pendingAgentInstance.getAgent();

            agentService.requestRegistration(fromServer(pendingAgent, false, "var/lib",
                    0L, "linux", false));
            String uuid = pendingAgentInstance.getUuid();
            agentService.approve(uuid);

            assertThat(agentService.findRegisteredAgents().size(), is(1));
            assertThat(agentService.findAgentAndRefreshStatus(uuid).getAgent().isDisabled(), is(false));
            assertThat(agentDao.getAgentByUUIDFromCacheOrDB(uuid).isDisabled(), is(false));
        }

        @Test
        void shouldRegisterAgentOnlyOnce() {
            Agent pendingAgent = pending().getAgent();
            AgentRuntimeInfo agentRuntimeInfo1 = fromServer(pendingAgent, false, "var/lib", 0L,
                    "linux", false);
            agentService.requestRegistration(agentRuntimeInfo1);

            agentService.approve(pendingAgent.getUuid());

            AgentRuntimeInfo agentRuntimeInfo2 = fromServer(pendingAgent, true, "var/lib", 0L,
                    "linux", false);
            agentService.requestRegistration(agentRuntimeInfo2);
            agentService.requestRegistration(agentRuntimeInfo2);

            assertThat(agentService.findRegisteredAgents().size(), is(1));
        }

        @Test
        void shouldBeAbleToMakeReapprovedAgentIdle() {
            disableAgent();

            agentService.updateAgentApprovalStatus("uuid1", false);

            AgentInstance instance = agentService.findAgentAndRefreshStatus("uuid1");
            assertThat(instance.getStatus(), is(AgentStatus.Idle));
        }

        @Test
        void shouldUpdateAgentApprovalStatusByUuid() {
            Agent agent = new Agent(UUID, "test", "127.0.0.1", singletonList("java"));
            agentService.register(agent, null, null);

            agentService.updateAgentApprovalStatus(agent.getUuid(), Boolean.TRUE);

            assertThat(agentService.findAgent(UUID).getStatus(), is(AgentStatus.Disabled));
        }
    }

    @Nested
    @ContextConfiguration(locations = {"classpath:WEB-INF/applicationContext-global.xml", "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
            "classpath:testPropertyConfigurer.xml", "classpath:WEB-INF/spring-all-servlet.xml"})
    class LoadingAgents {
        @Test
        void shouldLoadAllAgents() {
            AgentInstance idleAgentInstance = idle(new Date(), "CCeDev01");
            AgentInstance pendingAgentInstance = pending();
            AgentInstance buildingAgentInstance = building();
            AgentInstance deniedAgentInstance = disabled();

            AgentInstances agentInstances = new AgentInstances(new SystemEnvironment(), agentStatusChangeListener(),
                    idleAgentInstance, pendingAgentInstance, buildingAgentInstance, deniedAgentInstance);
            AgentService agentService = getAgentService(agentInstances);

            assertThat(agentService.getAgentInstances().size(), is(4));

            assertThat(agentService.findAgentAndRefreshStatus(idleAgentInstance.getAgent().getUuid()), is(idleAgentInstance));
            assertThat(agentService.findAgentAndRefreshStatus(pendingAgentInstance.getAgent().getUuid()), is(pendingAgentInstance));
            assertThat(agentService.findAgentAndRefreshStatus(buildingAgentInstance.getAgent().getUuid()), is(buildingAgentInstance));
            assertThat(agentService.findAgentAndRefreshStatus(deniedAgentInstance.getAgent().getUuid()), is(deniedAgentInstance));
        }
    }

    @Nested
    @ContextConfiguration(locations = {"classpath:WEB-INF/applicationContext-global.xml", "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
            "classpath:testPropertyConfigurer.xml", "classpath:WEB-INF/spring-all-servlet.xml"})
    class Environments {
        @Test
        void shouldDoNothingWhenEnvironmentsToAddOrRemoveIsNullOrEmpty() {
            String prodEnv = "prod";

            createEnvironment(prodEnv);

            createEnabledAgent(UUID);

            List<String> emptyList = emptyStrList;
            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

            EnvironmentsConfig envsConfig = new EnvironmentsConfig();

            envsConfig.add(CONFIG_HELPER.getEnvironment(prodEnv));

            agentService.bulkUpdateAgentAttributes(singletonList(UUID), singletonList("R1"),
                    emptyList, envsConfig, emptyList, TriState.UNSET, result);

            Agent agent = agentService.findAgent(UUID).getAgent();
            assertTrue(agent.getEnvironmentsAsList().size() == 1);
            assertTrue(agent.getEnvironmentsAsList().contains(prodEnv));

            agentService.bulkUpdateAgentAttributes(singletonList(UUID), singletonList("R2"),
                    singletonList("R1"), null, emptyList, TriState.UNSET, result);

            agent = agentService.findAgent(UUID).getAgent();
            assertTrue(agent.getEnvironmentsAsList().size() == 1);
            assertTrue(agent.getEnvironmentsAsList().contains(prodEnv));

            assertTrue(agent.getResourcesAsList().contains("R2"));
            assertFalse(agent.getResourcesAsList().contains("R1"));

            agentService.bulkUpdateAgentAttributes(singletonList(UUID), asList("R3", "R4"),
                    singletonList("R2"), new EnvironmentsConfig(), null, TriState.UNSET, result);


            agent = agentService.findAgent(UUID).getAgent();
            assertTrue(agent.getEnvironmentsAsList().size() == 1);
            assertTrue(agent.getEnvironmentsAsList().contains(prodEnv));

            assertTrue(agent.getResourcesAsList().contains("R3"));
            assertTrue(agent.getResourcesAsList().contains("R4"));
            assertFalse(agent.getResourcesAsList().contains("R2"));
        }

        @Test
        void shouldDoNothingWhenResourcesToAddOrRemoveIsNULLOrEmpty() {
            createEnabledAgent(UUID);

            List<String> emptyList = emptyStrList;
            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            EnvironmentsConfig envsConfig = new EnvironmentsConfig();
            agentService.bulkUpdateAgentAttributes(singletonList(UUID), asList("r1", "r2"),
                    null, envsConfig, emptyList, TRUE, result);

            AgentInstance agentInstance = agentService.findAgent(UUID);
            Agent agent = agentInstance.getAgent();
            assertTrue(agent.getResourcesAsList().contains("r1"));
            assertTrue(agent.getResourcesAsList().contains("r2"));

            agentService.bulkUpdateAgentAttributes(singletonList(UUID), null,
                    emptyList, null, null, TRUE, result);

            assertTrue(agent.getResourcesAsList().contains("r1"));
            assertTrue(agent.getResourcesAsList().contains("r2"));
        }

        @Test
        void shouldNotAddEnvsWhichAreAssociatedWithTheAgentFromConfigRepoOnBulkAgentUpdate() {
            String prodEnv = "prod";
            createEnvironment(prodEnv);
            createEnabledAgent(UUID);

            EnvironmentConfig prodEnvConfig = CONFIG_HELPER.getEnvironment(prodEnv);
            prodEnvConfig.setOrigins(new RepoConfigOrigin());
            prodEnvConfig.addAgent(UUID);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            EnvironmentsConfig prodEnvsConfig = new EnvironmentsConfig();
            prodEnvsConfig.add(prodEnvConfig);
            agentService.bulkUpdateAgentAttributes(singletonList(UUID), emptyStrList, emptyStrList,
                    prodEnvsConfig, emptyStrList, TriState.UNSET, result);

            Agent agent = agentService.findAgent(UUID).getAgent();
            assertTrue(agent.getEnvironmentsAsList().isEmpty());
        }

        @Test
        void shouldNotAddEnvsWhichAreAssociatedWithTheAgentFromConfigRepoOnUpdateAgent() {
            String prodEnv = "prod";
            createEnvironment(prodEnv);
            createEnabledAgent(UUID);

            EnvironmentConfig prodEnvConfig = CONFIG_HELPER.getEnvironment(prodEnv);
            prodEnvConfig.setOrigins(new RepoConfigOrigin());
            prodEnvConfig.addAgent(UUID);

            HttpOperationResult result = new HttpOperationResult();
            EnvironmentsConfig prodEnvsConfig = new EnvironmentsConfig();
            prodEnvsConfig.add(prodEnvConfig);
            agentService.updateAgentAttributes(UUID, null, null, prodEnvsConfig, TriState.UNSET, result);

            Agent agent = agentService.findAgent(UUID).getAgent();
            assertTrue(agent.getEnvironmentsAsList().isEmpty());
        }

        @Test
        void shouldBeAbleToUpdateEnvironmentsUsingUpdateAgentAttrsCall() {
            createEnvironment("a", "b", "c", "d", "e");
            createEnabledAgent(UUID);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(singletonList(UUID), emptyStrList, emptyStrList,
                    createEnvironmentsConfigWith("a", "b", "c"), emptyStrList, TriState.UNSET, result);

            assertThat(result.httpCode(), is(200));

            assertThat(agentService.getAgentInstances().size(), is(1));
            assertThat(getFirstAgent().getResourceConfigs(), is(empty()));
            assertThat(getFirstAgent().getAgent().getEnvironmentsAsList(), is(Arrays.asList("a", "b", "c")));

            HttpOperationResult result1 = new HttpOperationResult();
            agentService.updateAgentAttributes(UUID, null, null, createEnvironmentsConfigWith("c", "d", "e"), TriState.UNSET, result1);

            assertThat(result1.httpCode(), is(200));
            assertThat(result1.message(), is("Updated agent with uuid uuid."));

            assertThat(agentService.getAgentInstances().size(), is(1));
            assertThat(getFirstAgent().getAgent().getEnvironmentsAsList(), is(Arrays.asList("c", "d", "e")));
        }

        @Test
        void shouldAddEnvironmentsWhileBulkUpdatingAgents() {
            createEnvironment("uat", "prod");

            createEnabledAgent(UUID);
            createEnabledAgent(UUID2);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(asList(UUID, UUID2), emptyStrList, emptyStrList,
                    createEnvironmentsConfigWith("uat", "prod"), emptyStrList, TRUE, result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));
            assertThat(environmentConfigService.getAgentEnvironmentNames(UUID), containsSet("uat", "prod"));
            assertThat(environmentConfigService.getAgentEnvironmentNames(UUID2), containsSet("uat", "prod"));
        }

        @Test
        void shouldAddEnvToSpecifiedAgents() {
            createEnvironment("uat");

            createEnabledAgent(UUID);
            createEnabledAgent(UUID2);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            BasicEnvironmentConfig uat = new BasicEnvironmentConfig(new CaseInsensitiveString("uat"));
            agentService.updateAgentsAssociationOfEnvironment(uat, asList(UUID, UUID2), result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));
            assertThat(environmentConfigService.getAgentEnvironmentNames(UUID), containsSet("uat"));
            assertThat(environmentConfigService.getAgentEnvironmentNames(UUID2), containsSet("uat"));
        }

        @Test
        void shouldRemoveEnvFromSpecifiedAgents() {
            createEnvironment("uat");

            createEnabledAgent(UUID);
            createEnabledAgent(UUID2);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(asList(UUID, UUID2), emptyStrList, emptyStrList,
                    createEnvironmentsConfigWith("uat"), emptyStrList, TRUE, result);
            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));

            result = new HttpLocalizedOperationResult();
            BasicEnvironmentConfig uat = new BasicEnvironmentConfig(new CaseInsensitiveString("uat"));
            uat.addAgent(UUID);
            uat.addAgent(UUID2);
            List<String> noAgents = emptyList();
            agentService.updateAgentsAssociationOfEnvironment(uat, noAgents, result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent(s) with uuid(s): []."));

            assertThat(environmentConfigService.getAgentEnvironmentNames(UUID), containsSet());
            assertThat(environmentConfigService.getAgentEnvironmentNames(UUID2), containsSet());
        }

        @Test
        void shouldAddRemoveEnvFromSpecifiedAgents() {
            createEnvironment("uat");

            createEnabledAgent(UUID);
            createEnabledAgent(UUID2);
            String UUID3 = "uuid3";
            createEnabledAgent(UUID3);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(asList(UUID, UUID2), emptyStrList, emptyStrList,
                    createEnvironmentsConfigWith("uat"), emptyStrList, TRUE, result);
            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));

            result = new HttpLocalizedOperationResult();
            BasicEnvironmentConfig uat = new BasicEnvironmentConfig(new CaseInsensitiveString("uat"));
            uat.addAgent(UUID);
            uat.addAgent(UUID2);
            agentService.updateAgentsAssociationOfEnvironment(uat, asList(UUID, UUID3), result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, uuid3]."));

            assertThat(environmentConfigService.getAgentEnvironmentNames(UUID), containsSet("uat"));
            assertThat(environmentConfigService.getAgentEnvironmentNames(UUID2), containsSet());
            assertThat(environmentConfigService.getAgentEnvironmentNames(UUID3), containsSet("uat"));
        }

        @Test
        void shouldNotFailWhenAddingAgentsEnvironmentThatAlreadyExist() {
            createEnvironment("uat");

            createEnabledAgent(UUID);
            createEnabledAgent(UUID2);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(asList(UUID, UUID2), emptyStrList, emptyStrList,
                    createEnvironmentsConfigWith("uat"), emptyStrList, TRUE, result);

            result = new HttpLocalizedOperationResult();
            assertTrue(result.isSuccessful());

            agentService.bulkUpdateAgentAttributes(asList(UUID, UUID2), emptyStrList, emptyStrList,
                    createEnvironmentsConfigWith("uat"), emptyStrList, TRUE, result);

            assertTrue(result.isSuccessful());
            assertThat(environmentConfigService.getAgentEnvironmentNames(UUID), containsSet("uat"));
            assertThat(environmentConfigService.getAgentEnvironmentNames(UUID2), containsSet("uat"));
        }

        @Test
        void shouldBeAbleToRemoveAgentsEnvironments() {
            createEnvironment("uat", "prod");

            Agent enabledAgent1 = createEnabledAgent(UUID);
            enabledAgent1.setEnvironments("uat");
            agentDao.saveOrUpdate(enabledAgent1);

            Agent enabledAgent2 = createEnabledAgent(UUID2);
            enabledAgent2.setEnvironments("uat,prod");
            agentDao.saveOrUpdate(enabledAgent2);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(asList(UUID, UUID2), emptyStrList, emptyStrList,
                    emptyEnvsConfig, singletonList("uat"), TRUE, result);

            assertTrue(result.isSuccessful());
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));

            assertTrue(environmentConfigService.getAgentEnvironmentNames(UUID).isEmpty());

            assertTrue(environmentConfigService.getAgentEnvironmentNames(UUID2).size() == 1);
            assertThat(environmentConfigService.getAgentEnvironmentNames(UUID2), containsSet("prod"));
        }

        @Test
        void shouldOnlyAddRemoveAgentsEnvironmentThatAreRequested() {
            createEnvironment("uat", "prod", "perf", "test", "dev");

            Agent agent1 = createEnabledAgent(UUID);
            agent1.setEnvironments("uat,prod");
            agentDao.saveOrUpdate(agent1);

            Agent agent2 = createEnabledAgent(UUID2);
            agent2.setEnvironments("prod,uat,perf");
            agentDao.saveOrUpdate(agent2);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(asList(UUID, UUID2), emptyStrList, emptyStrList,
                    createEnvironmentsConfigWith("dev", "test", "perf"), asList("uat", "prod"), TRUE, result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));

            String[] expectedEnvs = new String[]{"perf", "dev", "test"};
            assertThat(environmentConfigService.getAgentEnvironmentNames(UUID).size(), is(3));
            assertThat(environmentConfigService.getAgentEnvironmentNames(UUID), containsSet(expectedEnvs));

            assertThat(environmentConfigService.getAgentEnvironmentNames(UUID2).size(), is(3));
            assertThat(environmentConfigService.getAgentEnvironmentNames(UUID2), containsSet(expectedEnvs));
        }

        private TypeSafeMatcher<Set<String>> containsSet(final String... items) {
            return new TypeSafeMatcher<Set<String>>() {
                @Override
                public boolean matchesSafely(Set<String> item) {
                    return item.containsAll(asList(items));
                }

            @Override
            public void describeTo(Description description) {
                description.appendText("to contain ").appendValue(items);
            }
        };
    }

        @Test
        void shouldNotFailWhenAgentIsAssociatedWithNonExistingEnvironment() {
            createEnabledAgent(UUID);
            createEnabledAgent(UUID2);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

            agentService.bulkUpdateAgentAttributes(asList(UUID, UUID2), emptyStrList, emptyStrList,
                    createEnvironmentsConfigWith("non-existent-env"), emptyStrList, TRUE, result);

            assertTrue(result.isSuccessful());
            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));
        }

        @Test
        void shouldNotFailWhenAgentAssociatedIsAskedToBeRemovedFromNonExistingEnvironment() {
            createEnabledAgent(UUID);
            createEnabledAgent(UUID2);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

            agentService.bulkUpdateAgentAttributes(asList(UUID, UUID2), emptyStrList, emptyStrList, emptyEnvsConfig,
                    singletonList("non-existent-env"), TRUE, result);

            assertTrue(result.isSuccessful());
            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));
        }
    }

    private void bulkUpdateEnvironments(Agent... agents){
        List<String> uuids = Stream.of(agents).map(Agent::getUuid).collect(toList());
        EnvironmentsConfig envsConfig = createEnvironmentsConfigWith(randomName("env"), randomName("env"), randomName("env"));
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(uuids, null, null, envsConfig, null, TRUE, result);
    }

    private void bulkUpdateResources(Agent... agents){
        List<String> uuids = Stream.of(agents).map(Agent::getUuid).collect(toList());
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        String res1 = randomName("res");
        String res2 = randomName("res");
        agentService.bulkUpdateAgentAttributes(uuids, asList(res1, res2), null, new EnvironmentsConfig(), null, TRUE, result);
    }

    private void updateAgentHostnames(Agent... agents){
        List<String> uuids = Stream.of(agents).map(Agent::getUuid).collect(toList());
        HttpOperationResult result = new HttpOperationResult();
        uuids.forEach(uuid -> agentService.updateAgentAttributes(uuid, randomName("hostname"), null, new EnvironmentsConfig(), TRUE, result));
    }

    private String randomName(String namePrefix){
        return namePrefix + new Random().nextLong();
    }

    @Test
    void whenMultipleThreadsUpdateAgentDetailsInDBTheAgentsCacheShouldAlwaysBeInSyncWithAgentsInDB(){
        final int numOfThreads = 30;

        ExecutorService execService = Executors.newFixedThreadPool(numOfThreads);
        Collection<Future<?>> futures = new ArrayList<>(numOfThreads);

        final Agent agent1 = AgentMother.localAgent();
        final Agent agent2 = AgentMother.localAgent();
        final Agent agent3 = AgentMother.localAgent();

        agentDao.saveOrUpdate(agent1);
        agentDao.saveOrUpdate(agent2);
        agentDao.saveOrUpdate(agent3);

        for (int i = 0; i < (numOfThreads / 2); i++) {
            futures.add(execService.submit(() -> bulkUpdateEnvironments(agent1)));
            futures.add(execService.submit(() -> bulkUpdateResources(agent1, agent2, agent3)));
            futures.add(execService.submit(() -> updateAgentHostnames(agent1)));
            futures.add(execService.submit(() -> agentService.getAgentByUUID(agent1.getUuid())));
            futures.add(execService.submit(() -> agentService.register(AgentMother.localAgent(), "r1,r2,r3", "e1,e2")));
        }

        joinFutures(futures, numOfThreads);

        assertThat(agentDao.fetchAgentFromDBByUUID(agent1.getUuid()), is(agentService.findAgent(agent1.getUuid()).getAgent()));
        assertThat(agentDao.fetchAgentFromDBByUUID(agent2.getUuid()), is(agentService.findAgent(agent2.getUuid()).getAgent()));
        assertThat(agentDao.fetchAgentFromDBByUUID(agent3.getUuid()), is(agentService.findAgent(agent3.getUuid()).getAgent()));
    }

    private void joinFutures(Collection<Future<?>> futures, int numOfThreads) {
        int count = 0;
        for (Future<?> f : futures) {
            try {
                f.get();
                count++;
            } catch (InterruptedException | ExecutionException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
            }
        }
        assertThat(count, is(numOfThreads/2 * 5));
    }

    private EnvironmentsConfig createEnvironmentsConfigWith(String... envs) {
        EnvironmentsConfig envsConfig = new EnvironmentsConfig();
        Arrays.stream(envs).forEach(env -> envsConfig.add(new BasicEnvironmentConfig(new CaseInsensitiveString(env))));
        return envsConfig;
    }

    private AgentStatusChangeListener agentStatusChangeListener() {
        return agentInstance -> {
        };
    }

    private void createEnvironment(String... environmentNames) {
        CONFIG_HELPER.addEnvironments(environmentNames);
        goConfigService.forceNotifyListeners();
    }

    private Agent createEnabledAgent(String uuid) {
        Agent agent = new Agent(uuid, "agentName", "127.0.0.9", uuid);
        requestRegistrationAndApproveAgent(agent);
        return agent;
    }

    private void disableAgent(Agent agent) {
        AgentIdentifier agentIdentifier = agent.getAgentIdentifier();
        String cookie = agentService.assignCookie(agentIdentifier);
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), cookie);
        agentRuntimeInfo.busy(new AgentBuildingInfo("path", "buildLocator"));
        agentService.updateRuntimeInfo(agentRuntimeInfo);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(singletonList(agent.getUuid()), emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.FALSE, result);

        assertThat(isDisabled(agent), is(true));
    }

    private void disableAgent() {
        Agent pending = new Agent("uuid1", "agent1", "192.168.0.1");
        agentService.requestRegistration(fromServer(pending, false, "/var/lib", 0L, "linux"));
        agentService.approve("uuid1");
        agentService.updateAgentApprovalStatus("uuid1", true);
    }

    private boolean isDisabled(Agent agent) {
        return agentService.findAgentAndRefreshStatus(agent.getUuid()).isDisabled();
    }

    public void requestRegistrationAndApproveAgent(Agent agent) {
        agentService.requestRegistration(fromServer(agent, false, "/var/lib", 0L, "linux"));
        agentService.approve(agent.getUuid());
    }

    private Agent createDisabledAgentWithBuildingRuntimeStatus(String uuid) {
        Agent agent = new Agent(uuid, "agentName", "127.0.0.9");
        requestRegistrationAndApproveAgent(agent);
        disableAgent(agent);
        return agent;
    }

    private Agent createAnIdleAgentAndDisableIt(String uuid) {
        Agent agent = new Agent(uuid, "agentName", "127.0.0.9");
        requestRegistrationAndApproveAgent(agent);

        // Make an agent idle
        AgentIdentifier agentIdentifier = agent.getAgentIdentifier();
        String cookie = agentService.assignCookie(agentIdentifier);
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), cookie);
        agentRuntimeInfo.idle();
        agentService.updateRuntimeInfo(agentRuntimeInfo);
        assertTrue(agentService.findAgentAndRefreshStatus(uuid).isIdle());

        // Disable the agent
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(asList(agent.getUuid()), emptyStrList,
                emptyStrList, emptyEnvsConfig, emptyStrList, TriState.FALSE, result);
        Agent updatedAgent = agentService.getAgentByUUID(agent.getUuid());
        assertThat(isDisabled(updatedAgent), is(true));

        return updatedAgent;
    }

    private AgentInstance getFirstAgent() {
        for (AgentInstance agentInstance : agentService.getAgentInstances()) {
            return agentInstance;
        }
        return null;
    }

    private Set<String> getEnvironments(String uuid) {
        return environmentConfigService.getAgentEnvironmentNames(uuid);
    }

    private AgentStatusChangeNotifier agentStatusChangeNotifier() {
        return new MockAgentStatusChangeNotifier();
    }

    private class MockAgentStatusChangeNotifier extends AgentStatusChangeNotifier {
        public MockAgentStatusChangeNotifier() {
            super(null, null);
        }

        @Override
        public void onAgentStatusChange(AgentInstance agentInstance) {
        }
    }
}
