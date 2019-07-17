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
import com.thoughtworks.go.domain.*;
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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.InetAddress;
import java.util.*;

import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml",
        "classpath:WEB-INF/spring-all-servlet.xml"
})

//TODO: Vrushali and Viraj need to fix this
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
        return new AgentService(new SystemEnvironment(), agentInstances, securityService, agentDao,
                new UuidGenerator(), serverHealthService, agentStatusChangeNotifier(), goConfigService);
    }

    @Nested
    @ContextConfiguration(locations = {"classpath:WEB-INF/applicationContext-global.xml", "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
            "classpath:testPropertyConfigurer.xml", "classpath:WEB-INF/spring-all-servlet.xml"})
    class DeleteAgents {
        @Test
        public void onlyDisabledAgentsShouldBeAllowedToBeDeleted() {
            AgentConfig disabledAgent = createAnIdleAgentAndDisableIt(UUID);
            AgentConfig enabledAgent = createEnabledAgent(UUID2);

            assertThat(agentService.agentInstances().size(), is(2));

            HttpOperationResult disabledAgentOperationResult = new HttpOperationResult();
            HttpOperationResult enabledAgentOperationResult = new HttpOperationResult();

            agentService.deleteAgents(USERNAME, disabledAgentOperationResult, asList(disabledAgent.getUuid()));
            agentService.deleteAgents(USERNAME, enabledAgentOperationResult, asList(enabledAgent.getUuid()));

            assertThat(disabledAgentOperationResult.httpCode(), is(200));
            assertThat(disabledAgentOperationResult.message(), is("Deleted 1 agent(s)."));

            assertThat(enabledAgentOperationResult.httpCode(), is(406));
            assertThat(enabledAgentOperationResult.message(), is("Failed to delete 1 agent(s), as agent(s) might not be disabled or are still building."));

            assertThat(agentService.agentInstances().size(), is(1));
            assertTrue(agentService.agentInstances().hasAgent(UUID2));
        }

        @Test
        public void shouldDeleteAgentsOnlyWhenAllRequestedAgentsAreDisabled() {
            AgentConfig disabledAgent1 = createAnIdleAgentAndDisableIt(UUID);
            AgentConfig disabledAgent2 = createAnIdleAgentAndDisableIt(UUID2);

            assertThat(agentService.agentInstances().size(), is(2));

            HttpOperationResult operationResult = new HttpOperationResult();

            agentService.deleteAgents(USERNAME, operationResult, asList(disabledAgent1.getUuid(), disabledAgent2.getUuid()));

            assertThat(operationResult.httpCode(), is(200));
            assertThat(operationResult.message(), is("Deleted 2 agent(s)."));

            assertThat(agentService.agentInstances().size(), is(0));
        }

        @Test
        public void shouldNotBeAbleToDeleteADisabledAgentWhoseRuntimeStatusIsBuilding() {
            AgentConfig disabledButBuildingAgent = createDisabledAgentWithBuildingRuntimeStatus(UUID);

            assertThat(agentService.agentInstances().size(), is(1));

            HttpOperationResult operationResult = new HttpOperationResult();

            agentService.deleteAgents(USERNAME, operationResult, asList(disabledButBuildingAgent.getUuid()));

            assertThat(operationResult.httpCode(), is(406));
            assertThat(operationResult.message(), is("Failed to delete 1 agent(s), as agent(s) might not be disabled or are still building."));

            assertThat(agentService.agentInstances().size(), is(1));
            assertTrue(agentService.agentInstances().hasAgent(UUID));
        }

        @Test
        public void shouldFindAgentViewModelForAnExistingAgent() {
            AgentConfig agent = createEnabledAgent(UUID);
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
        public void shouldNotDeleteAnyAgentWhenOneOfTheRequestedAgentIsNotDisabled() {
            AgentConfig disabledAgent = createAnIdleAgentAndDisableIt(UUID);
            AgentConfig enabledAgent = createEnabledAgent(UUID2);

            assertThat(agentService.agentInstances().size(), is(2));

            HttpOperationResult operationResult = new HttpOperationResult();

            agentService.deleteAgents(USERNAME, operationResult, asList(disabledAgent.getUuid(), enabledAgent.getUuid()));

            assertThat(operationResult.httpCode(), is(406));
            assertThat(operationResult.message(), is("Failed to delete 2 agent(s), as agent(s) might not be disabled or are still building."));
            assertThat(agentService.agentInstances().size(), is(2));
        }

        @Test
        public void shouldDeleteAgentFromDBGivenUUID() throws Exception {
            createAnIdleAgentAndDisableIt(UUID);
            createEnabledAgent(UUID2);

            assertThat(agentService.agentInstances().size(), is(2));

            HttpOperationResult operationResult = new HttpOperationResult();
            agentService.deleteAgents(Username.ANONYMOUS, operationResult, singletonList(UUID));

            assertThat(operationResult.httpCode(), is(200));
            assertThat(operationResult.message(), is("Deleted 1 agent(s)."));

            assertThat(agentService.agentInstances().size(), is(1));
        }

        @Test
        public void shouldReturn404WhenAgentUUIDNotKnownForDeleteAgents() {
            HttpOperationResult operationResult = new HttpOperationResult();
            agentService.deleteAgents(USERNAME, operationResult, singletonList("unknown-agent-id"));
            assertThat(operationResult.httpCode(), is(404));
            assertThat(operationResult.message(), is("Agent not found."));
        }
    }

    @Nested
    @ContextConfiguration(locations = {"classpath:WEB-INF/applicationContext-global.xml", "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
            "classpath:testPropertyConfigurer.xml", "classpath:WEB-INF/spring-all-servlet.xml"})
    class PendingAgents {
        @Test
        public void listOfRegisteredAgentShouldNotIncludePendingAgents() {
            AgentInstance idle = AgentInstanceMother.updateUuid(AgentInstanceMother.idle(new Date(), "CCeDev01"), UUID);
            AgentInstance pending = AgentInstanceMother.pending();
            AgentInstance building = AgentInstanceMother.building();
            AgentInstance denied = AgentInstanceMother.disabled();

            AgentInstances instances = new AgentInstances(new SystemEnvironment(), agentStatusChangeListener(), idle, pending, building, denied);
            AgentService agentService = getAgentService(instances);

            AgentsViewModel agents = agentService.registeredAgents();
            assertThat(agents.size(), is(3));
            for (AgentViewModel agent : agents) {
                assertThat(agent.getStatus().getConfigStatus(), not(is(AgentConfigStatus.Pending)));
            }
        }

        @Test
        public void shouldBeAbleToDisableAPendingAgent() {
            String agentId = DatabaseAccessHelper.AGENT_UUID;
            AgentConfig agent = new AgentConfig(agentId, "agentName", "50.40.30.9");
            AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false);
            agentRuntimeInfo.busy(new AgentBuildingInfo("path", "buildLocator"));
            agentService.requestRegistration(new Username("bob"), agentRuntimeInfo);

            HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, asList(agentId), emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.FALSE);

            assertThat(operationResult.httpCode(), is(200));
            assertThat(operationResult.message(), is(String.format("Updated agent(s) with uuid(s): [%s].", agentId)));
        }

        @Test
        public void shouldBeAbleToEnableAPendingAgent() {
            String agentId = DatabaseAccessHelper.AGENT_UUID;
            AgentConfig agent = new AgentConfig(agentId, "agentName", "50.40.30.9");
            AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agent.getAgentIdentifier(), AgentRuntimeStatus.Idle, currentWorkingDirectory(), "cookie", false);
            agentRuntimeInfo.busy(new AgentBuildingInfo("path", "buildLocator"));
            agentService.requestRegistration(new Username("bob"), agentRuntimeInfo);

            HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, asList(agentId), emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.TRUE);

            assertThat(operationResult.httpCode(), is(200));
            assertThat(operationResult.message(), is(String.format("Updated agent(s) with uuid(s): [%s].", agentId)));
        }

        @Test
        public void shouldDenyAgentFromPendingList() {
            AgentInstance pending = AgentInstanceMother.pending();
            agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(pending.agentConfig(), false, "var/lib", 0L, "linux", false));

            String uuid = pending.getUuid();

            HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, asList(uuid), emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.FALSE);

            assertThat(operationResult.httpCode(), is(200));
            assertThat(operationResult.message(), is(String.format("Updated agent(s) with uuid(s): [%s].", uuid)));

            AgentInstances agents = agentService.agentInstances();

            assertThat(agents.size(), is(1));
            assertThat(agents.size(), is(1));
            assertThat(agents.findAgent(uuid).isDisabled(), is(true));
            assertThat(agentService.findAgentAndRefreshStatus(uuid).isDisabled(), is(true));
            assertThat(agentService.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Disabled));
        }

        @Test
        public void shouldNotAllowAnyUpdateOperationOnPendingAgentsIfConfigStateIsNotProvided() {
            AgentRuntimeInfo pendingAgent = AgentRuntimeInfo.fromServer(new AgentConfig(UUID, "CCeDev03", "10.18.5.3", new ResourceConfigs(new ResourceConfig("db"), new ResourceConfig("web"))), false, "/var/lib", 0L, "linux", false);
            agentService.requestRegistration(Username.ANONYMOUS, pendingAgent);
            assertThat(agentService.findAgent(UUID).isRegistered(), is(false));

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            ArrayList<String> uuids = new ArrayList<>();
            uuids.add(pendingAgent.getUUId());

            ArrayList<String> resourcesToAdd = new ArrayList<>();
            resourcesToAdd.add("Linux");

            ArrayList<String> resourcesToRemove = new ArrayList<>();
            resourcesToRemove.add("Gauge");

            agentService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, resourcesToAdd, resourcesToRemove, emptyEnvsConfig, emptyStrList, TriState.UNSET);

            HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
            expectedResult.badRequest("Pending agents [" + pendingAgent.getUUId() + "] must be explicitly enabled or disabled when performing any operations on them.");
            assertThat(result, is(expectedResult));
        }
    }

    @Nested
    @ContextConfiguration(locations = {"classpath:WEB-INF/applicationContext-global.xml", "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
            "classpath:testPropertyConfigurer.xml", "classpath:WEB-INF/spring-all-servlet.xml"})
    class EnableDisableAgents {
        @Test
        public void shouldBeAbleToDisableAgent() {
            AgentConfig agent = new AgentConfig(UUID, "agentName", "127.0.0.9", "cookie");
            agentDao.saveOrUpdate(agent);

            AgentConfig agentInDB = agentDao.agentByUuid(UUID);
            assertThat(agentInDB.isDisabled(), is(false));

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            List<String> emptyList = emptyStrList;
            agentService.bulkUpdateAgentAttributes(USERNAME, result, asList(UUID), emptyList, emptyList, emptyEnvsConfig, emptyList, TriState.FALSE);

            assertThat(agentService.agents().get(0).isDisabled(), is(true));
        }

        @Test
        public void shouldBeAbleToDisableAgentUsingUpdateAgentAttributesCall() {
            createEnabledAgent(UUID);

            assertThat(agentService.agentInstances().size(), is(1));
            assertThat(getFirstAgent().isDisabled(), is(false));

            HttpOperationResult result = new HttpOperationResult();
            agentService.updateAgentAttributes(USERNAME, result, UUID, null, null, null, TriState.FALSE);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent with uuid uuid."));

            assertThat(agentService.agentInstances().size(), is(1));
            assertThat(getFirstAgent().isDisabled(), is(true));
        }

        @Test
        public void shouldBeAbleToDisableBuildingAgent() {
            String agentName = "agentName";
            String agentId = DatabaseAccessHelper.AGENT_UUID;
            AgentConfig agent = new AgentConfig(agentId, agentName, "127.0.0.9");
            requestRegistrationAndApproveAgent(agent);

            AgentIdentifier agentIdentifier = agent.getAgentIdentifier();
            String cookie = agentService.assignCookie(agentIdentifier);
            AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), cookie, false);
            agentRuntimeInfo.busy(new AgentBuildingInfo("path", "buildLocator"));

            agentService.updateRuntimeInfo(agentRuntimeInfo);

            HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
            List<String> emptyList = emptyStrList;
            agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, asList(agentId), emptyList, emptyList, emptyEnvsConfig, emptyList, TriState.FALSE);

            assertThat(agentService.agents().getAgentByUuid(agentId).isDisabled(), is(true));
        }

        @Test
        public void shouldDisableCorrectAgentWhenTwoOnSameBox() {
            AgentConfig agent1 = new AgentConfig(UUID, "agentName", "127.0.0.9", "cookie");
            AgentConfig agent2 = new AgentConfig(UUID2, "agentName", "127.0.0.9", "cookie2");

            agentDao.saveOrUpdate(agent1);
            agentDao.saveOrUpdate(agent2);

            assertThat(agentDao.agentByUuid(UUID).isDisabled(), is(false));
            assertThat(agentDao.agentByUuid(UUID2).isDisabled(), is(false));

            HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
            List<String> emptyList = emptyStrList;
            agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, asList(UUID2), emptyList, emptyList, emptyEnvsConfig, emptyList, TriState.FALSE);

            assertThat(agentService.agents().getAgentByUuid(UUID).isDisabled(), is(false));
            assertThat(agentService.agents().getAgentByUuid(UUID2).isDisabled(), is(true));
        }

        @Test
        public void shouldBeAbleToEnableADisabledAgent() {
            String agentId = DatabaseAccessHelper.AGENT_UUID;
            AgentConfig agent = new AgentConfig(agentId, "agentName", "127.0.0.9");
            requestRegistrationAndApproveAgent(agent);
            disableAgent(agent);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, result, asList(agentId), emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.TRUE);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is(String.format("Updated agent(s) with uuid(s): [%s].", agentId)));
            assertThat(isDisabled(agent), is(false));
        }

        @Test
        public void shouldBeAbleToEnabledMultipleAgents() {
            AgentConfig agent1 = createDisabledAgentWithBuildingRuntimeStatus(UUID);
            AgentConfig agent2 = createDisabledAgentWithBuildingRuntimeStatus(UUID2);

            HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, asList(UUID, UUID2), emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.TRUE);

            assertThat(operationResult.httpCode(), is(200));
            assertThat(operationResult.message(), containsString("Updated agent(s) with uuid(s): [uuid, uuid2]."));
            assertThat(isDisabled(agent1), is(false));
            assertThat(isDisabled(agent2), is(false));
        }

        @Test
        public void shouldBeAbleToDisableMultipleAgents() {
            AgentConfig agentConfig1 = createEnabledAgent(UUID);
            AgentConfig agentConfig2 = createEnabledAgent(UUID2);

            HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, asList(UUID, UUID2), emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.FALSE);

            assertThat(operationResult.httpCode(), is(200));
            assertThat(operationResult.message(), containsString("Updated agent(s) with uuid(s): [uuid, uuid2]."));
            assertThat(isDisabled(agentConfig1), is(true));
            assertThat(isDisabled(agentConfig2), is(true));
        }

        @Test
        public void onlyAuthorizedUserShouldBeAbleToDisableAgents() {
            String agentId = "agent-id";
            CONFIG_HELPER.enableSecurity();
            CONFIG_HELPER.addAdmins("admin1");

            HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(new Username(new CaseInsensitiveString("not-admin")), operationResult, asList(agentId), emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.FALSE);

            assertThat(operationResult.httpCode(), is(403));
            assertThat(operationResult.message(), is("Unauthorized to edit."));
        }

        @Test
        public void shouldReturn200WhenAnAlreadyEnableAgentIsEnabled() {
            String agentName = "agentName";
            String agentId = DatabaseAccessHelper.AGENT_UUID;
            AgentConfig agentConfig = new AgentConfig(agentId, agentName, "127.0.0.9");
            requestRegistrationAndApproveAgent(agentConfig);

            HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, asList(agentId), emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.TRUE);

            assertThat(operationResult.httpCode(), is(200));
            assertThat(operationResult.message(), is(String.format("Updated agent(s) with uuid(s): [%s].", agentId)));
        }

        @Test
        public void shouldReturn200OnTryingToDisableADisabledAgent() {
            String agentName = "agentName";
            String agentId = DatabaseAccessHelper.AGENT_UUID;
            AgentConfig agentConfig = new AgentConfig(agentId, agentName, "127.0.0.9");
            requestRegistrationAndApproveAgent(agentConfig);
            disableAgent(agentConfig);

            HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, asList(agentId), emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.FALSE);

            assertThat(operationResult.httpCode(), is(200));
            assertThat(operationResult.message(), is(String.format("Updated agent(s) with uuid(s): [%s].", agentId)));
        }

        @Test
        public void shouldNotUpdateAgentWhenTriStateIsUnset() {
            createEnabledAgent("enabled");
            createDisabledAgentWithBuildingRuntimeStatus("disabled");

            assertThat(agentService.agentInstances().size(), is(2));
            assertThat(agentService.findAgentAndRefreshStatus("enabled").agentConfig().isDisabled(), is(false));
            assertThat(agentService.findAgentAndRefreshStatus("disabled").agentConfig().isDisabled(), is(true));

            HttpOperationResult operationResult = new HttpOperationResult();
            agentService.updateAgentAttributes(USERNAME, operationResult, "enabled", "new.enabled.hostname", "linux,java", null, TriState.UNSET);
            agentService.updateAgentAttributes(USERNAME, operationResult, "disabled", "new.disabled.hostname", "linux,java", null, TriState.UNSET);

            assertThat(operationResult.httpCode(), is(200));

            assertThat(agentService.agentInstances().size(), is(2));
            assertThat(agentService.findAgentAndRefreshStatus("enabled").agentConfig().isDisabled(), is(false));
            assertThat(agentService.findAgentAndRefreshStatus("disabled").agentConfig().isDisabled(), is(true));
        }

        @Test
        public void shouldAllowEnablingThePendingAndDisabledAgentsTogether() {
            AgentRuntimeInfo pendingAgent = AgentRuntimeInfo.fromServer(new AgentConfig(UUID, "CCeDev03", "10.18.5.3", new ResourceConfigs(new ResourceConfig("db"), new ResourceConfig("web"))), false, "/var/lib", 0L, "linux", false);
            AgentConfig agentConfig = new AgentConfig(UUID2, "remote-host1", "50.40.30.21");

            agentService.requestRegistration(Username.ANONYMOUS, pendingAgent);
            agentService.register(agentConfig, null, null, new HttpOperationResult());
            agentService.disableAgents(agentConfig.getUuid());

            assertThat(agentService.findAgent(UUID).isRegistered(), is(false));
            assertThat(agentService.findAgent(agentConfig.getUuid()).isDisabled(), is(true));

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            List<String> uuids = asList(UUID, agentConfig.getUuid());

            agentService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.TRUE);

            assertTrue(result.isSuccessful());
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [" + StringUtils.join(uuids, ", ") + "]."));
            assertThat(agentService.agents().getAgentByUuid(UUID).isEnabled(), is(true));
            assertThat(agentService.agents().getAgentByUuid(agentConfig.getUuid()).isEnabled(), is(true));
        }

        @Test
        public void shouldNotDisableAgentsWhenInvalidAgentUUIDIsProvided() {
            AgentConfig agentConfig1 = new AgentConfig(UUID, "remote-host1", "50.40.30.21");
            AgentConfig agentConfig2 = new AgentConfig(UUID2, "remote-host2", "50.40.30.22");

            agentService.register(agentConfig1, null, null, new HttpOperationResult());
            agentService.register(agentConfig2, null, null, new HttpOperationResult());

            assertFalse(agentService.findAgent(agentConfig1.getUuid()).isDisabled());
            assertFalse(agentService.findAgent(agentConfig2.getUuid()).isDisabled());

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            List<String> uuids = asList(agentConfig1.getUuid(), agentConfig2.getUuid(), "invalid-uuid");

            agentService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.FALSE);

            assertFalse(agentService.findAgent(agentConfig1.getUuid()).isDisabled());
            assertFalse(agentService.findAgent(agentConfig2.getUuid()).isDisabled());

            assertFalse(result.isSuccessful());
            assertThat(result.httpCode(), is(400));
            assertThat(result.message(), is(EntityType.Agent.notFoundMessage(Collections.singletonList("invalid-uuid"))));
        }

        @Test
        public void shouldNotEnableAgentsWhenInvalidAgentUUIDIsProvided() {
            AgentConfig agentConfig1 = new AgentConfig(UUID, "remote-host1", "50.40.30.21");
            AgentConfig agentConfig2 = new AgentConfig(UUID2, "remote-host2", "50.40.30.22");

            agentService.register(agentConfig1, null, null, new HttpOperationResult());
            agentService.register(agentConfig2, null, null, new HttpOperationResult());

            assertFalse(agentService.findAgent(agentConfig1.getUuid()).isDisabled());
            assertFalse(agentService.findAgent(agentConfig2.getUuid()).isDisabled());

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            List<String> uuids = asList(agentConfig1.getUuid(), agentConfig2.getUuid(), "invalid-uuid");

            agentService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.TRUE);

            assertFalse(agentService.findAgent(agentConfig1.getUuid()).isDisabled());
            assertFalse(agentService.findAgent(agentConfig2.getUuid()).isDisabled());

            assertFalse(result.isSuccessful());
            assertThat(result.httpCode(), is(400));
            assertThat(result.message(), is(EntityType.Agent.notFoundMessage(Collections.singletonList("invalid-uuid"))));
        }

    }

    @Nested
    @ContextConfiguration(locations = {"classpath:WEB-INF/applicationContext-global.xml", "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
            "classpath:testPropertyConfigurer.xml", "classpath:WEB-INF/spring-all-servlet.xml"})
    class ErrorConditions {
        @Test
        public void shouldReturn400WhenUpdatingAnUnknownAgent() {
            HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, singletonList("unknown-agent-id"), emptyStrList,
                    emptyStrList, emptyEnvsConfig, emptyStrList, TriState.TRUE);

            assertThat(operationResult.httpCode(), is(400));
            assertThat(operationResult.message(), is("Agents with uuids 'unknown-agent-id' were not found!"));
        }

        @Test
        public void shouldThrow400ErrorWhenNonExistingEnvironmentIsAddedToAgent() {
            createEnabledAgent(UUID);

            HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, asList(UUID), emptyStrList, emptyStrList,
                    createEnvironmentsConfigWith("unknown_env"), emptyStrList, TriState.TRUE);

            assertThat(operationResult.httpCode(), is(400));
            assertThat(operationResult.message(), containsString("Environment with name 'unknown_env' was not found!"));
        }

        @Test
        public void shouldReturn403WhenUnauthorizedUserTriesToDeleteAgent() {
            CONFIG_HELPER.enableSecurity();
            HttpOperationResult operationResult = new HttpOperationResult();
            CONFIG_HELPER.addAdmins("admin1");
            agentService.deleteAgents(new Username(new CaseInsensitiveString("not-admin")), operationResult, asList(UUID));
            assertThat(operationResult.httpCode(), is(403));
            assertThat(operationResult.message(), is("Unauthorized to operate on agent"));
        }

        @Test
        public void shouldReturn404WhenAgentToBeDeletedDoesNotExist() {
            String agentId = "unknown-agent-id";
            HttpOperationResult operationResult = new HttpOperationResult();
            agentService.deleteAgents(USERNAME, operationResult, asList(agentId));
            assertThat(operationResult.httpCode(), is(404));
            assertThat(operationResult.message(), is("Agent not found."));
        }

        @Test
        public void shouldReturn403WhenAUnauthorizedUserTriesToEnable() {
            String agentId = "agent-id";
            CONFIG_HELPER.enableSecurity();
            CONFIG_HELPER.addAdmins("admin1");

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(new Username(new CaseInsensitiveString("not-admin")), result,
                    asList(agentId), emptyStrList, emptyStrList, emptyEnvsConfig,
                    emptyStrList, TriState.TRUE);

            assertThat(result.httpCode(), is(403));
            assertThat(result.message(), is("Unauthorized to edit."));
        }

        @Test
        public void shouldReturn400WhenDisablingAnUnknownAgent() {
            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, result, singletonList("unknown-agent-id"), emptyStrList, emptyStrList,
                    emptyEnvsConfig, emptyStrList, TriState.FALSE);

            assertThat(result.httpCode(), is(400));
            assertThat(result.message(), is("Agents with uuids 'unknown-agent-id' were not found!"));
        }

        @Test
        public void shouldThrow422WhenUpdatingAgentWithInvalidInputs() {
            AgentConfig agent = createAnIdleAgentAndDisableIt(UUID);
            String originalHostname = agent.getHostname();
            List<String> originalResourceNames = agent.getResourceConfigs().resourceNames();

            assertThat(agentService.agentInstances().size(), is(1));
            assertThat(getFirstAgent().getHostname(), is(not("some-hostname")));

            HttpOperationResult operationResult = new HttpOperationResult();
            AgentInstance agentInstance = agentService.updateAgentAttributes(USERNAME, operationResult, UUID, "some-hostname", "lin!ux", null, TriState.UNSET);

            assertThat(operationResult.httpCode(), is(422));
            assertThat(operationResult.message(), is("Updating agent failed:"));
            assertThat(agentInstance.agentConfig().errors().on(JobConfig.RESOURCES), is("Resource name 'lin!ux' is not valid. Valid names much match '^[-\\w\\s|.]*$'"));

            assertThat(agentService.agentInstances().size(), is(1));
            assertThat(getFirstAgent().getHostname(), is(originalHostname));
            assertThat(getFirstAgent().getResourceConfigs().resourceNames(), is(originalResourceNames));
        }

        @Test
        public void shouldThrowExceptionWhenADuplicateAgentTriesToUpdateStatus() {
            AgentInstance instance = AgentInstanceMother.building();
            AgentService agentService = getAgentService(new AgentInstances(new SystemEnvironment(), agentStatusChangeListener(), instance));
            AgentInstances agents = agentService.findRegisteredAgents();

            String uuid = instance.agentConfig().getUuid();
            assertThat(agents.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Building));

            AgentIdentifier identifier = instance.agentConfig().getAgentIdentifier();
            agentDao.associateCookie(identifier, "new_cookie");
            AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(identifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "old_cookie", false);

            AgentWithDuplicateUUIDException e = assertThrows(AgentWithDuplicateUUIDException.class, () -> agentService.updateRuntimeInfo(runtimeInfo));
            assertEquals(e.getMessage(), format("Agent [%s] has invalid cookie", runtimeInfo.agentInfoDebugString()));

            agents = agentService.findRegisteredAgents();
            assertThat(agents.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Building));
            AgentIdentifier agentIdentifier = instance.agentConfig().getAgentIdentifier();
            String cookie = agentService.assignCookie(agentIdentifier);
            agentService.updateRuntimeInfo(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), cookie, false));
        }

    }

    @Nested
    @ContextConfiguration(locations = {"classpath:WEB-INF/applicationContext-global.xml", "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
            "classpath:testPropertyConfigurer.xml", "classpath:WEB-INF/spring-all-servlet.xml"})
    class Update {
        @Test
        public void shouldBeAbleToUpdateAgentsHostName() {
            createAnIdleAgentAndDisableIt(UUID);

            assertThat(agentService.agentInstances().size(), is(1));
            assertThat(getFirstAgent().getHostname(), is(not("some-hostname")));

            HttpOperationResult result = new HttpOperationResult();
            agentService.updateAgentAttributes(USERNAME, result, UUID, "some-hostname", null, null, TriState.UNSET);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent with uuid uuid."));

            assertThat(agentService.agentInstances().size(), is(1));
            assertThat(getFirstAgent().getHostname(), is("some-hostname"));
        }

        @Test
        public void shouldUpdateAgentAttributesForValidInputs() throws Exception {
            String headCommitBeforeUpdate = configRepository.getCurrentRevCommit().name();
            createAnIdleAgentAndDisableIt(UUID);
            createEnvironment("a", "b");

            assertThat(agentService.agentInstances().size(), is(1));
            assertThat(getFirstAgent().getHostname(), is(not("some-hostname")));
            assertThat(getFirstAgent().isDisabled(), is(true));

            HttpOperationResult operationResult = new HttpOperationResult();
            agentService.updateAgentAttributes(USERNAME, operationResult, UUID, "some-hostname", "linux,java", "a,b", TriState.UNSET);

            assertThat(operationResult.httpCode(), is(200));
            assertThat(operationResult.message(), is("Updated agent with uuid uuid."));

            assertThat(agentService.agentInstances().size(), is(1));
            assertThat(getFirstAgent().getHostname(), is("some-hostname"));
            assertThat(getFirstAgent().getResourceConfigs().resourceNames(), equalTo(asList("java", "linux")));
            assertThat(getFirstAgent().isDisabled(), is(true));
            assertEquals(getEnvironments(getFirstAgent().getUuid()), new HashSet<>(asList("a", "b")));
        }

        @Test
        public void shouldUpdateAgentStatus() {
            AgentInstance instance = AgentInstanceMother.building();
            AgentService agentService = getAgentService(new AgentInstances(new SystemEnvironment(), agentStatusChangeListener(), instance));
            AgentInstances agents = agentService.findRegisteredAgents();

            String uuid = instance.agentConfig().getUuid();
            assertThat(agents.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Building));
            AgentIdentifier agentIdentifier = instance.agentConfig().getAgentIdentifier();
            String cookie = agentService.assignCookie(agentIdentifier);
            agentService.updateRuntimeInfo(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), cookie, false));
            agents = agentService.findRegisteredAgents();
            assertThat(agents.findAgentAndRefreshStatus(uuid).getStatus(), is(AgentStatus.Idle));
        }

        @Test
        public void shouldUpdateOnlyThoseAgentsAttributeThatAreSpecified() {
            createEnvironment("a", "b");
            AgentConfig agent = createAnIdleAgentAndDisableIt(UUID);
            String originalHostname = agent.getHostname();

            HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, asList(UUID), emptyStrList, emptyStrList, createEnvironmentsConfigWith("a", "b"), emptyStrList, TriState.TRUE);

            assertThat(agentService.agentInstances().size(), is(1));

            HttpOperationResult operationResult1 = new HttpOperationResult();
            agentService.updateAgentAttributes(USERNAME, operationResult1, UUID, null, null, null, TriState.UNSET);

            assertThat(operationResult1.httpCode(), is(200));
            assertThat(operationResult1.message(), is("Updated agent with uuid uuid."));

            assertThat(agentService.agentInstances().size(), is(1));
            assertThat(getFirstAgent().getHostname(), is(originalHostname));
            assertEquals(getEnvironments(getFirstAgent().getUuid()), new HashSet<>(asList("a", "b")));
        }

        @Test
        public void shouldThrowBadRequestIfNoOperationsProvidedOnBulkUpdateAgents() {
            AgentInstance pendingAgent = AgentInstanceMother.pending();
            AgentInstance registeredAgent = AgentInstanceMother.disabled();

            AgentInstances instances = new AgentInstances(new SystemEnvironment(), agentStatusChangeListener(), pendingAgent);
            AgentService agentService = getAgentService(instances);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            ArrayList<String> uuids = new ArrayList<>();
            uuids.add(pendingAgent.getUuid());
            uuids.add(registeredAgent.getUuid());

            agentService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.UNSET);

            HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
            expectedResult.badRequest("No Operation performed on agents.");

            assertThat(result, is(expectedResult));
        }

        @Test
        public void shouldUpdateResourcesEnvironmentsAndAgentStateOfTheProvidedStatesAllTogether() throws Exception {
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

            agentService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, resources, emptyStrList, environmentsToAdd, emptyStrList, TriState.FALSE);

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
        public void shouldMarkAgentAsLostContactWhenAgentDoesNotPingWithinTimeoutPeriod() {
            new SystemEnvironment().setProperty("agent.connection.timeout", "-1");
            CONFIG_HELPER.addMailHost(new MailHost("ghost.name", 25, "loser", "boozer", true, false, "go@foo.mail.com", "admin@foo.mail.com"));

            Date date = new Date(70, 1, 1, 1, 1, 1);
            AgentInstance instance = AgentInstanceMother.idle(date, "CCeDev01");
            ((AgentRuntimeInfo) ReflectionUtil.getField(instance, "agentRuntimeInfo")).setOperatingSystem("Minix");
            EmailSender mailSender = mock(EmailSender.class);
            AgentService agentService = new AgentService(new SystemEnvironment(), securityService, agentDao, new UuidGenerator(), serverHealthService, agentStatusChangeNotifier(), goConfigService);
            AgentInstances agentInstances = (AgentInstances) ReflectionUtil.getField(agentService, "agentInstances");
            agentInstances.add(instance);

            AgentInstances agents = agentService.findRegisteredAgents();
            assertThat(agents.size(), is(1));
            AgentInstance agentInstance = agents.findAgentAndRefreshStatus(instance.agentConfig().getUuid());
            assertThat(agentInstance.getStatus(), is(AgentStatus.LostContact));
        }

        @Test
        public void shouldNotShootoutMailsWhenAgentHasLostContact() {
            CONFIG_HELPER.addMailHost(new MailHost("ghost.name", 25, "loser", "boozer", true, false, "go@foo.mail.com", "admin@foo.mail.com"));

            AgentInstance instance = AgentInstanceMother.idle(new Date(), "CCeDev01");
            EmailSender mailSender = mock(EmailSender.class);

            agentDao.associateCookie(instance.getAgentIdentifier(), "rotten-cookie");

            AgentService agentService = new AgentService(new SystemEnvironment(), securityService, agentDao, new UuidGenerator(), serverHealthService, agentStatusChangeNotifier(), goConfigService);
            AgentInstances agentInstances = (AgentInstances) ReflectionUtil.getField(agentService, "agentInstances");
            agentInstances.add(instance);

            ReflectionUtil.setField(instance, "lastHeardTime", null);

            AgentRuntimeInfo runtimeInfo = (AgentRuntimeInfo) ReflectionUtil.getField(instance, "agentRuntimeInfo");
            runtimeInfo.setCookie("rotten-cookie");
            runtimeInfo.setStatus(AgentStatus.Building);

            agentService.updateRuntimeInfo(runtimeInfo);

            verify(mailSender, never()).sendEmail(any(SendEmailMessage.class));
        }

        @Test
        public void shouldSendLostContactEmailWhenAgentStateIsLostContact_FEATURE_HIDDEN() {
            new SystemEnvironment().setProperty("agent.connection.timeout", "-1");
            CONFIG_HELPER.addMailHost(new MailHost("ghost.name", 25, "loser", "boozer", true, false, "go@foo.mail.com", "admin@foo.mail.com"));

            Date date = new Date(70, 1, 1, 1, 1, 1);
            AgentInstance instance = AgentInstanceMother.idle(date, "CCeDev01");
            ((AgentRuntimeInfo) ReflectionUtil.getField(instance, "agentRuntimeInfo")).setOperatingSystem("Minix");
            EmailSender mailSender = mock(EmailSender.class);
            AgentService agentService = new AgentService(new SystemEnvironment(), securityService, agentDao, new UuidGenerator(), serverHealthService, agentStatusChangeNotifier(), goConfigService);
            AgentInstances agentInstances = (AgentInstances) ReflectionUtil.getField(agentService, "agentInstances");
            agentInstances.add(instance);

            AgentInstances agents = agentService.findRegisteredAgents();
            assertThat(agents.size(), is(1));
            AgentInstance agentInstance = agents.findAgentAndRefreshStatus(instance.agentConfig().getUuid());
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
            verify(mailSender, never()).sendEmail(new SendEmailMessage("[Lost Contact] Go agent host: " + instance.getHostname(), body, "admin@foo.mail.com"));
        }

    }

    @Nested
    @ContextConfiguration(locations = {"classpath:WEB-INF/applicationContext-global.xml", "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
            "classpath:testPropertyConfigurer.xml", "classpath:WEB-INF/spring-all-servlet.xml"})
    class Resources {
        @Test
        public void shouldAddResourcesToDisabledAgent() {
            createAnIdleAgentAndDisableIt(UUID);

            assertThat(agentService.agentInstances().size(), is(1));
            assertThat(getFirstAgent().getResourceConfigs(), is(empty()));

            HttpOperationResult operationResult = new HttpOperationResult();
            agentService.updateAgentAttributes(USERNAME, operationResult, UUID, null, "linux,java", null, TriState.UNSET);

            assertThat(operationResult.httpCode(), is(200));
            assertThat(operationResult.message(), is("Updated agent with uuid uuid."));

            assertThat(agentService.agentInstances().size(), is(1));
            assertThat(getFirstAgent().getResourceConfigs().resourceNames(), is(asList("java", "linux")));
        }

        @Test
        public void shouldAddResourcesWhileBulkUpdatingAgents() {
            createEnabledAgent(UUID);
            createEnabledAgent(UUID2);

            List<String> uuids = asList(UUID, UUID2);
            List<String> resourcesToAdd = asList("resource1", "resource2");
            List<String> resourcesToRemove = emptyStrList;
            List<String> emptyEnvs = emptyStrList;

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, result, uuids, resourcesToAdd, resourcesToRemove,
                    emptyEnvsConfig, emptyEnvs, TriState.TRUE);
            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));

            ResourceConfigs uuidResources = agentService.findAgentAndRefreshStatus(UUID).agentConfig().getResourceConfigs();
            assertThat(uuidResources, hasItem(new ResourceConfig("resource1")));
            assertThat(uuidResources, hasItem(new ResourceConfig("resource2")));

            ResourceConfigs uuid2Resources = agentService.findAgentAndRefreshStatus(UUID2).agentConfig().getResourceConfigs();
            assertThat(uuid2Resources, hasItem(new ResourceConfig("resource1")));
            assertThat(uuid2Resources, hasItem(new ResourceConfig("resource2")));
        }

        @Test
        public void shouldNotAllowNonAdminUserToAddResourcesToAgents() {
            String agentId = "agent-id";
            CONFIG_HELPER.enableSecurity();
            CONFIG_HELPER.addAdmins("admin1");

            HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(new Username(new CaseInsensitiveString("not-admin")), operationResult,
                    asList(agentId), asList("dont-care"), emptyStrList,
                    emptyEnvsConfig, emptyStrList, TriState.UNSET);

            assertThat(operationResult.httpCode(), is(403));
            assertThat(operationResult.message(), is("Unauthorized to edit."));
        }

        @Test
        public void shouldNotUpdateResourcesOnElasticAgents() {
            AgentConfig elasticAgent = AgentMother.elasticAgent();

            agentService.register(elasticAgent, null, null, new HttpOperationResult());

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            List<String> uuids = singletonList(elasticAgent.getUuid());
            List<String> resourcesToAdd = singletonList("resource");

            assertTrue(agentService.findAgent(elasticAgent.getUuid()).getResourceConfigs().isEmpty());
            agentService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, resourcesToAdd, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.FALSE);

            HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
            expectedResult.badRequest("Resources on elastic agents with uuids [" + StringUtils.join(uuids, ", ") + "] can not be updated.");

            assertThat(result, is(expectedResult));
            assertTrue(agentService.findAgent(elasticAgent.getUuid()).getResourceConfigs().isEmpty());
        }

        @Test
        public void shouldRemoveResourcesFromTheSpecifiedAgents() {
            AgentConfig agentConfig1 = new AgentConfig(UUID, "remote-host1", "50.40.30.21");
            AgentConfig agentConfig2 = new AgentConfig(UUID2, "remote-host1", "50.40.30.22");

            HttpOperationResult operationResult = new HttpOperationResult();
            agentService.register(agentConfig1, "resource1,resource2", null, operationResult);
            agentService.register(agentConfig2, "resource2", null, operationResult);

            List<String> uuids = asList(UUID, UUID2);
            List<String> resourcesToRemove = singletonList("resource2");

            assertThat(agentService.findAgent(UUID).getResourceConfigs().size(), is(2));
            assertThat(agentService.findAgent(UUID2).getResourceConfigs().size(), is(1));

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, emptyStrList, resourcesToRemove, emptyEnvsConfig, emptyStrList, TriState.FALSE);

            assertTrue(result.isSuccessful());
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [" + StringUtils.join(uuids, ", ") + "]."));
            assertThat(agentService.findAgent(UUID).getResourceConfigs().size(), is(1));
            assertThat(agentService.findAgent(UUID).getResourceConfigs(), contains(new ResourceConfig("resource1")));
            assertThat(agentService.findAgent(UUID2).getResourceConfigs().size(), is(0));
        }

    }

    @Nested
    @ContextConfiguration(locations = {"classpath:WEB-INF/applicationContext-global.xml", "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
            "classpath:testPropertyConfigurer.xml", "classpath:WEB-INF/spring-all-servlet.xml"})
    class RegistrationAndApproval {
        @Test
        public void shouldRegisterLocalAgentWithNonLoopbackIpAddress() throws Exception {
            String nonLoopbackIp = SystemUtil.getFirstLocalNonLoopbackIpAddress();
            InetAddress inetAddress = InetAddress.getByName(nonLoopbackIp);
            assertThat(SystemUtil.isLocalIpAddress(nonLoopbackIp), is(true));
            AgentConfig agentConfig = new AgentConfig("uuid", inetAddress.getHostName(), nonLoopbackIp);
            AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromServer(agentConfig, false, "/var/lib", 0L, "linux", false);
            agentService.requestRegistration(new Username("bob"), agentRuntimeInfo);

            AgentInstance agentInstance = agentService.findRegisteredAgents().findAgentAndRefreshStatus("uuid");

            assertThat(agentInstance.agentConfig().getIpaddress(), is(nonLoopbackIp));
            assertThat(agentInstance.getStatus(), is(AgentStatus.Idle));
        }

        @Test
        public void shouldBeAbleToRegisterAgent() {
            AgentInstance pendingAgentInstance = AgentInstanceMother.pending();
            AgentConfig pendingAgent = pendingAgentInstance.agentConfig();

            agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(pendingAgent, false, "var/lib", 0L, "linux", false));
            String uuid = pendingAgentInstance.getUuid();
            agentService.approve(uuid);

            assertThat(agentService.findRegisteredAgents().size(), is(1));
            assertThat(agentService.findAgentAndRefreshStatus(uuid).agentConfig().isDisabled(), is(false));
            assertThat(agentDao.agentByUuid(uuid).isDisabled(), is(false));
        }

        @Test
        public void shouldRegisterAgentOnlyOnce() {
            AgentInstance pending = AgentInstanceMother.pending();
            agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(pending.agentConfig(), false, "var/lib", 0L, "linux", false));

            agentService.approve(pending.getUuid());

            agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(pending.agentConfig(), true, "var/lib", 0L, "linux", false));
            agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(pending.agentConfig(), true, "var/lib", 0L, "linux", false));
            assertThat(agentService.findRegisteredAgents().size(), is(1));
        }

        @Test
        public void shouldBeAbleToMakeReapprovedAgentIdle() {
            disableAgent();

            agentService.updateAgentApprovalStatus("uuid1", false, Username.ANONYMOUS);

            AgentInstance instance = agentService.findAgentAndRefreshStatus("uuid1");
            assertThat(instance.getStatus(), is(AgentStatus.Idle));
        }

        @Test
        public void shouldLoadAgentsByApprovalStatus() {
            AgentConfig disabledAgent1 = new AgentConfig("uuid1", "disabledAgent1", "127.0.0.1", "cookie1");
            disabledAgent1.disable();
            agentDao.saveOrUpdate(disabledAgent1);

            AgentConfig disabledAgent2 = new AgentConfig("uuid2", "disabledAgent2", "127.0.0.2", "cookie2");
            disabledAgent2.disable();
            agentDao.saveOrUpdate(disabledAgent2);

            agentDao.saveOrUpdate(new AgentConfig("uuid3", "approvedAgent1", "127.0.0.3", "cookie3"));

            AgentInstances approvedAgents = agentService.findEnabledAgents();
            assertThat(approvedAgents.size(), is(1));
            assertThat(approvedAgents.findAgentAndRefreshStatus("uuid3").agentConfig().getHostname(), is("approvedAgent1"));

            AgentInstances deniedAgents = agentService.findDisabledAgents();
            assertThat(deniedAgents.size(), is(2));
            assertThat(deniedAgents.findAgentAndRefreshStatus("uuid1").agentConfig().getHostname(), is("disabledAgent1"));
            assertThat(deniedAgents.findAgentAndRefreshStatus("uuid2").agentConfig().getHostname(), is("disabledAgent2"));
        }

        @Test
        public void shouldUpdateAgentApprovalStatusByUuid() {
            AgentConfig agentConfig = new AgentConfig(UUID, "test", "127.0.0.1", new ResourceConfigs("java"));
            agentService.register(agentConfig, null, null, new HttpOperationResult());

            agentService.updateAgentApprovalStatus(agentConfig.getUuid(), Boolean.TRUE, Username.ANONYMOUS);

            assertThat(agentService.findAgent(UUID).getStatus(), is(AgentStatus.Disabled));
        }

    }

    @Nested
    @ContextConfiguration(locations = {"classpath:WEB-INF/applicationContext-global.xml", "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
            "classpath:testPropertyConfigurer.xml", "classpath:WEB-INF/spring-all-servlet.xml"})
    class LoadingAgents {
        @Test
        public void shouldLoadAllAgents() {
            AgentInstance idle = AgentInstanceMother.idle(new Date(), "CCeDev01");
            AgentInstance pending = AgentInstanceMother.pending();
            AgentInstance building = AgentInstanceMother.building();
            AgentInstance denied = AgentInstanceMother.disabled();

            AgentInstances agentInstances = new AgentInstances(new SystemEnvironment(), agentStatusChangeListener(), idle, pending, building, denied);
            AgentService agentService = getAgentService(agentInstances);

            assertThat(agentService.agentInstances().size(), is(4));

            assertThat(agentService.findAgentAndRefreshStatus(idle.agentConfig().getUuid()), is(idle));
            assertThat(agentService.findAgentAndRefreshStatus(pending.agentConfig().getUuid()), is(pending));
            assertThat(agentService.findAgentAndRefreshStatus(building.agentConfig().getUuid()), is(building));
            assertThat(agentService.findAgentAndRefreshStatus(denied.agentConfig().getUuid()), is(denied));
        }
    }

    @Nested
    @ContextConfiguration(locations = {"classpath:WEB-INF/applicationContext-global.xml", "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
            "classpath:testPropertyConfigurer.xml", "classpath:WEB-INF/spring-all-servlet.xml"})
    class Environments {
        @Test
        public void shouldBeAbleToUpdateEnvironmentsUsingUpdateAgentAttrsCall() {
            createEnvironment("a", "b", "c", "d", "e");
            createEnabledAgent(UUID);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            List<String> emptyList = emptyStrList;
            agentService.bulkUpdateAgentAttributes(USERNAME, result, singletonList(UUID), emptyList, emptyList,
                    createEnvironmentsConfigWith("a", "b", "c"), emptyList,
                    TriState.UNSET);

            assertThat(result.httpCode(), is(200));

            assertThat(agentService.agentInstances().size(), is(1));
            assertThat(getFirstAgent().getResourceConfigs(), is(empty()));

            HttpOperationResult result1 = new HttpOperationResult();
            agentService.updateAgentAttributes(USERNAME, result1, UUID, null, null, "c,d,e", TriState.UNSET);

            assertThat(result1.httpCode(), is(200));
            assertThat(result1.message(), is("Updated agent with uuid uuid."));

            assertThat(agentService.agentInstances().size(), is(1));
            assertThat(getEnvironments(getFirstAgent().getUuid()).equals(new HashSet<>(asList("c", "d", "e"))), is(true));
        }

        @Test
        public void shouldAddEnvironmentsWhileBulkUpdatingAgents() {
            createEnvironment("uat", "prod");

            createEnabledAgent(UUID);
            createEnabledAgent(UUID2);

            HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, asList(UUID, UUID2), emptyStrList, emptyStrList, createEnvironmentsConfigWith("uat", "prod"), emptyStrList, TriState.TRUE);

            assertThat(operationResult.httpCode(), is(200));
            assertThat(operationResult.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));
            assertThat(environmentConfigService.environmentsFor(UUID), containsSet("uat", "prod"));
            assertThat(environmentConfigService.environmentsFor(UUID2), containsSet("uat", "prod"));
        }


        @Test
        public void shouldAddEnvToSpecifiedAgents() {
            createEnvironment("uat");

            createEnabledAgent(UUID);
            createEnabledAgent(UUID2);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            BasicEnvironmentConfig uat = new BasicEnvironmentConfig(new CaseInsensitiveString("uat"));
            agentService.updateAgentsAssociationWithSpecifiedEnv(USERNAME, uat, asList(UUID, UUID2), result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));
            assertThat(environmentConfigService.environmentsFor(UUID), containsSet("uat"));
            assertThat(environmentConfigService.environmentsFor(UUID2), containsSet("uat"));
        }

        @Test
        public void shouldRemoveEnvFromSpecifiedAgents() {
            createEnvironment("uat");

            createEnabledAgent(UUID);
            createEnabledAgent(UUID2);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, result, asList(UUID, UUID2), emptyStrList, emptyStrList, createEnvironmentsConfigWith("uat"), emptyStrList, TriState.TRUE);
            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));

            result = new HttpLocalizedOperationResult();
            BasicEnvironmentConfig uat = new BasicEnvironmentConfig(new CaseInsensitiveString("uat"));
            uat.addAgent(UUID);
            uat.addAgent(UUID2);
            agentService.updateAgentsAssociationWithSpecifiedEnv(USERNAME, uat, emptyList(), result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent(s) with uuid(s): []."));

            assertThat(environmentConfigService.environmentsFor(UUID), containsSet());
            assertThat(environmentConfigService.environmentsFor(UUID2), containsSet());
        }


        @Test
        public void shouldAddRemoveEnvFromSpecifiedAgents() {
            createEnvironment("uat");

            createEnabledAgent(UUID);
            createEnabledAgent(UUID2);
            String UUID3 = "uuid3";
            createEnabledAgent(UUID3);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, result, asList(UUID, UUID2), emptyStrList, emptyStrList, createEnvironmentsConfigWith("uat"), emptyStrList, TriState.TRUE);
            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));

            result = new HttpLocalizedOperationResult();
            BasicEnvironmentConfig uat = new BasicEnvironmentConfig(new CaseInsensitiveString("uat"));
            uat.addAgent(UUID);
            uat.addAgent(UUID2);
            agentService.updateAgentsAssociationWithSpecifiedEnv(USERNAME, uat, Arrays.asList(UUID, UUID3), result);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, uuid3]."));

            assertThat(environmentConfigService.environmentsFor(UUID), containsSet("uat"));
            assertThat(environmentConfigService.environmentsFor(UUID2), containsSet());
            assertThat(environmentConfigService.environmentsFor(UUID3), containsSet("uat"));
        }

        @Test
        public void shouldNotFailWhenAddingAgentsEnvironmentThatAlreadyExist() {
            createEnvironment("uat");

            createEnabledAgent(UUID);
            createEnabledAgent(UUID2);

            HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, asList(UUID, UUID2), emptyStrList, emptyStrList, createEnvironmentsConfigWith("uat"), emptyStrList, TriState.TRUE);
            agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, asList(UUID, UUID2), emptyStrList,
                    emptyStrList, createEnvironmentsConfigWith("uat"),
                    emptyStrList, TriState.TRUE);

            operationResult = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, asList(UUID, UUID2), emptyStrList, emptyStrList, createEnvironmentsConfigWith("uat"), emptyStrList, TriState.TRUE);
            agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, asList(UUID, UUID2), emptyStrList,
                    emptyStrList, createEnvironmentsConfigWith("uat"),
                    emptyStrList, TriState.TRUE);

            assertThat(operationResult.httpCode(), is(200));
            assertThat(environmentConfigService.environmentsFor(UUID), containsSet("uat"));
            assertThat(environmentConfigService.environmentsFor(UUID2), containsSet("uat"));
        }

        @Test
        public void shouldBeAbleToRemoveAgentsEnvironments() {
            createEnvironment("uat", "prod");

            AgentConfig enabledAgent1 = createEnabledAgent(UUID);
            enabledAgent1.setEnvironments("uat");
            agentDao.saveOrUpdate(enabledAgent1);

            AgentConfig enabledAgent2 = createEnabledAgent(UUID2);
            enabledAgent2.setEnvironments("uat,prod");
            agentDao.saveOrUpdate(enabledAgent2);

            HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, asList(UUID, UUID2), emptyStrList, emptyStrList, emptyEnvsConfig, asList("uat"), TriState.TRUE);

            assertThat(operationResult.httpCode(), is(200));
            assertThat(operationResult.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));

            assertThat(environmentConfigService.environmentsFor(UUID), not(containsSet("uat")));

            assertThat(environmentConfigService.environmentsFor(UUID2), not(containsSet("uat")));
            assertThat(environmentConfigService.environmentsFor(UUID2), containsSet("prod"));
        }

        @Test
        public void shouldOnlyAddRemoveAgentsEnvironmentThatAreRequested() {
            createEnvironment("uat", "prod", "perf", "test", "dev");

            AgentConfig agent1 = createEnabledAgent(UUID);
            agent1.setEnvironments("uat,prod");
            agentDao.saveOrUpdate(agent1);

            AgentConfig agent2 = createEnabledAgent(UUID2);
            agent2.setEnvironments("prod,uat,perf");
            agentDao.saveOrUpdate(agent2);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, result, asList(UUID, UUID2), emptyStrList,
                    emptyStrList, createEnvironmentsConfigWith("dev", "test", "perf"),
                    asList("uat", "prod"), TriState.TRUE);

            assertThat(result.httpCode(), is(200));
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));

            String[] expectedEnvs = new String[]{"perf", "dev", "test"};
            assertThat(environmentConfigService.environmentsFor(UUID).size(), is(3));
            assertThat(environmentConfigService.environmentsFor(UUID), containsSet(expectedEnvs));

            assertThat(environmentConfigService.environmentsFor(UUID2).size(), is(3));
            assertThat(environmentConfigService.environmentsFor(UUID2), containsSet(expectedEnvs));
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
        public void shouldNotAllowUnauthorizedUserToUpdateAgentsEnvironment() {
            CONFIG_HELPER.enableSecurity();
            CONFIG_HELPER.addAdmins("adminUser");

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.bulkUpdateAgentAttributes(USERNAME, result, asList(UUID), emptyStrList, emptyStrList,
                    emptyEnvsConfig, asList("uat"), TriState.TRUE);

            assertThat(result.httpCode(), is(403));
            assertThat(result.message(), is("Unauthorized to edit."));
        }

        @Test
        public void shouldNotAddAgentToNonExistingEnvironment() {
            createEnabledAgent(UUID);
            createEnabledAgent(UUID2);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            List<String> uuids = asList(UUID, UUID2);

            agentService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, emptyStrList, emptyStrList, createEnvironmentsConfigWith("non-existent-env"), emptyStrList, TriState.TRUE);

            assertFalse(result.isSuccessful());
            assertThat(result.httpCode(), is(400));
            assertThat(result.message(), is(EntityType.Environment.notFoundMessage("non-existent-env")));
        }

        @Test
        public void shouldNotRemoveAgentFromNonExistingEnvironment() {
            createEnabledAgent(UUID);
            createEnabledAgent(UUID2);

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            List<String> uuids = asList(UUID, UUID2);

            agentService.bulkUpdateAgentAttributes(Username.ANONYMOUS, result, uuids, emptyStrList, emptyStrList, emptyEnvsConfig, singletonList("non-existent-env"), TriState.TRUE);

            assertFalse(result.isSuccessful());
            assertThat(result.httpCode(), is(400));
            assertThat(result.message(), is(EntityType.Environment.notFoundMessage("non-existent-env")));
        }
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

    private AgentConfig createEnabledAgent(String uuid) {
        AgentConfig agentConfig = new AgentConfig(uuid, "agentName", "127.0.0.9", uuid);
        requestRegistrationAndApproveAgent(agentConfig);
        return agentConfig;
    }

    private void disableAgent(AgentConfig agentConfig) {
        AgentIdentifier agentIdentifier = agentConfig.getAgentIdentifier();
        String cookie = agentService.assignCookie(agentIdentifier);
        AgentRuntimeInfo agentRuntimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), cookie);
        agentRuntimeInfo.busy(new AgentBuildingInfo("path", "buildLocator"));
        agentService.updateRuntimeInfo(agentRuntimeInfo);

        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(USERNAME, operationResult, singletonList(agentConfig.getUuid()), emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.FALSE);

        assertThat(isDisabled(agentConfig), is(true));
    }

    private void disableAgent() {
        AgentConfig pending = new AgentConfig("uuid1", "agent1", "192.168.0.1");
        agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(pending, false, "/var/lib", 0L, "linux"));
        agentService.approve("uuid1");
        agentService.updateAgentApprovalStatus("uuid1", true, Username.ANONYMOUS);
    }

    private boolean isDisabled(AgentConfig agentConfig) {
        return agentService.findAgentAndRefreshStatus(agentConfig.getUuid()).isDisabled();
    }

    public void requestRegistrationAndApproveAgent(AgentConfig agentConfig) {
        agentService.requestRegistration(new Username("bob"), AgentRuntimeInfo.fromServer(agentConfig, false, "/var/lib", 0L, "linux"));
        agentService.approve(agentConfig.getUuid());
    }

    private AgentConfig createDisabledAgentWithBuildingRuntimeStatus(String uuid) {
        AgentConfig agentConfig = new AgentConfig(uuid, "agentName", "127.0.0.9");
        requestRegistrationAndApproveAgent(agentConfig);
        disableAgent(agentConfig);
        return agentConfig;
    }

    private AgentConfig createAnIdleAgentAndDisableIt(String uuid) {
        AgentConfig agent = new AgentConfig(uuid, "agentName", "127.0.0.9");
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
        agentService.bulkUpdateAgentAttributes(USERNAME, result, asList(agent.getUuid()), emptyStrList,
                emptyStrList, emptyEnvsConfig, emptyStrList, TriState.FALSE);
        AgentConfig updatedAgent = agentService.agentByUuid(agent.getUuid());
        assertThat(isDisabled(updatedAgent), is(true));

        return updatedAgent;
    }

    private AgentInstance getFirstAgent() {
        for (AgentInstance agentInstance : agentService.agentInstances()) {
            return agentInstance;
        }
        return null;
    }

    private Set<String> getEnvironments(String uuid) {
        return environmentConfigService.environmentsFor(uuid);
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
