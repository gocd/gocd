/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import ch.qos.logback.classic.Level;
import com.google.common.collect.Ordering;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.NullAgent;
import com.thoughtworks.go.domain.NullAgentInstance;
import com.thoughtworks.go.domain.exception.InvalidAgentInstructionException;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.listener.AgentChangeListener;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.AgentDao;
import com.thoughtworks.go.server.ui.AgentViewModel;
import com.thoughtworks.go.server.ui.AgentsViewModel;
import com.thoughtworks.go.server.util.UuidGenerator;
import com.thoughtworks.go.serverhealth.HealthStateLevel;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.LogFixture;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TriState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.*;

import static com.thoughtworks.go.CurrentGoCDVersion.docsUrl;
import static com.thoughtworks.go.domain.AgentConfigStatus.Pending;
import static com.thoughtworks.go.domain.AgentInstance.FilterBy.Elastic;
import static com.thoughtworks.go.domain.AgentInstance.FilterBy.Null;
import static com.thoughtworks.go.domain.AgentInstance.createFromLiveAgent;
import static com.thoughtworks.go.domain.AgentRuntimeStatus.Idle;
import static com.thoughtworks.go.domain.AgentStatus.fromConfig;
import static com.thoughtworks.go.helper.AgentInstanceMother.*;
import static com.thoughtworks.go.server.service.AgentRuntimeInfo.fromServer;
import static com.thoughtworks.go.serverhealth.HealthStateScope.forAgent;
import static com.thoughtworks.go.serverhealth.HealthStateType.duplicateAgent;
import static com.thoughtworks.go.serverhealth.ServerHealthState.warning;
import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static com.thoughtworks.go.util.TriState.*;
import static com.thoughtworks.go.utils.Timeout.THIRTY_SECONDS;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Arrays.sort;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Fail.fail;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class AgentServiceTest {
    private AgentService agentService;
    private EnvironmentConfigService environmentConfigService;
    private AgentInstances agentInstances;
    private AgentDao agentDao;
    private AgentIdentifier agentIdentifier;
    private UuidGenerator uuidGenerator;
    private ServerHealthService serverHealthService;
    private Agent agent;
    private GoConfigService goConfigService;
    private SecurityService securityService;

    private List<String> emptyStrList = Collections.emptyList();

    @BeforeEach
    void setUp() {
        agentInstances = mock(AgentInstances.class);
        environmentConfigService = mock(EnvironmentConfigService.class);
        securityService = mock(SecurityService.class);
        agent = new Agent("uuid", "host", "192.168.1.1");
        when(agentInstances.findAgentAndRefreshStatus("uuid")).thenReturn(AgentInstance.createFromAgent(agent, new SystemEnvironment(), null));
        agentDao = mock(AgentDao.class);
        goConfigService = mock(GoConfigService.class);
        uuidGenerator = mock(UuidGenerator.class);
        agentService = new AgentService(new SystemEnvironment(), agentInstances,
                agentDao, uuidGenerator, serverHealthService = mock(ServerHealthService.class), null);
        agentIdentifier = agent.getAgentIdentifier();
        when(agentDao.cookieFor(agentIdentifier)).thenReturn("cookie");
    }

    @Nested
    class AnyOperationPerformedOnAgents {
        final String NOT_NULL_VALUE = "any.not.null.value-even.empty.string.would.have.worked";

        @Nested
        class SingleAgent {
            @Test
            void shouldReturnTrueIfOnlyHostNameValueIsSpecifiedAsNotNull() {
                TriState unsetState = UNSET;
                boolean anyOpsPerformed = agentService.validateAnyOperationPerformedOnAgent(NOT_NULL_VALUE, null, null, unsetState);
                assertThat(anyOpsPerformed, is(true));
            }

            @Test
            void shouldReturnTrueIfOnlyEnvironmentsValueIsSpecifiedAsNotNull() {
                TriState unsetState = UNSET;
                boolean anyOpsPerformed = agentService.validateAnyOperationPerformedOnAgent(null, "", null, unsetState);
                assertThat(anyOpsPerformed, is(true));
            }

            @Test
            void shouldReturnTrueIfOnlyResourcesValueIsSpecifiedAsNotNull() {
                TriState unsetState = UNSET;
                boolean anyOpsPerformed = agentService.validateAnyOperationPerformedOnAgent(null, null, NOT_NULL_VALUE, unsetState);
                assertThat(anyOpsPerformed, is(true));
            }

            @Test
            void shouldReturnTrueIfOnlyTriStateValueIsSpecifiedAsTrueOrFalse() {
                boolean anyOpsPerformed = agentService.validateAnyOperationPerformedOnAgent(null, null, null, TRUE);
                assertThat(anyOpsPerformed, is(true));
            }

            @Test
            void shouldReturnFalseOnlyIfNoneOfTheAgentAttributesAreSpecified() {
                TriState unsetState = UNSET;
                BadRequestException e = assertThrows(BadRequestException.class, () -> agentService.validateAnyOperationPerformedOnAgent(null, null, null, unsetState));
                assertThat(e.getMessage(), is("Bad Request. No operation is specified in the request to be performed on agent."));
            }
        }

        @Nested
        class BulkAgents {
            @Test
            void shouldReturnTrueIfOnlyResourcesToAddListIsNotEmpty() {
                TriState unsetState = UNSET;
                boolean anyOpsPerformed = agentService.isAnyOperationPerformedOnBulkAgents(singletonList("r1"), emptyList(), null, emptyList(), unsetState);
                assertThat(anyOpsPerformed, is(true));
            }

            @Test
            void shouldReturnTrueIfOnlyResourcesToRemoveListIsNotEmpty() {
                TriState unsetState = UNSET;
                boolean anyOpsPerformed = agentService.isAnyOperationPerformedOnBulkAgents(emptyList(), singletonList("r1"), null, emptyList(), unsetState);
                assertThat(anyOpsPerformed, is(true));
            }

            @Test
            void shouldReturnTrueIfOnlyEnvsToAddListIsNotEmpty() {
                TriState unsetState = UNSET;
                boolean anyOpsPerformed = agentService.isAnyOperationPerformedOnBulkAgents(emptyList(), null, singletonList("env1"), emptyList(), unsetState);
                assertThat(anyOpsPerformed, is(true));
            }

            @Test
            void shouldReturnTrueIfOnlyEnvsToRemoveListIsNotEmpty() {
                TriState unsetState = UNSET;
                boolean anyOpsPerformed = agentService.isAnyOperationPerformedOnBulkAgents(emptyList(), null, emptyStrList, singletonList("env1"), unsetState);
                assertThat(anyOpsPerformed, is(true));
            }

            @Test
            void shouldReturnTrueIfOnlyTriStateIsSetToTrueOrFalse() {
                boolean anyOpsPerformed = agentService.isAnyOperationPerformedOnBulkAgents(emptyList(), null, emptyStrList, emptyList(), TRUE);
                assertThat(anyOpsPerformed, is(true));
            }

            @Test
            void shouldThrow400IfNoOperationIsPerformedOnAgent() {
                assertThrows(BadRequestException.class, () -> agentService.isAnyOperationPerformedOnBulkAgents(emptyList(), null, emptyStrList, emptyList(), UNSET));
            }
        }
    }

    @Nested
    class UpdateSingleOrBulkAgentAttributes {
        private String uuid;
        private AgentInstance agentInstance;

        @BeforeEach
        void setUp() {
            uuid = "uuid";
            agentInstance = mock(AgentInstance.class);
            when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
            when(agentInstance.isNullAgent()).thenReturn(false);
            when(agentInstance.getUuid()).thenReturn(uuid);
        }

        @Nested
        class BulkAgentUpdate {

            @Test
            void shouldBulkUpdateAgentsAttributes() {
                AgentInstance agentInstance1 = mock(AgentInstance.class);
                AgentInstance agentInstance2 = mock(AgentInstance.class);
                Username username = new Username(new CaseInsensitiveString("test"));

                when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
                when(goConfigService.getEnvironments()).thenReturn(new EnvironmentsConfig());
                when(agentInstances.findAgent("uuid1")).thenReturn(agentInstance1);
                when(agentInstances.findAgent("uuid2")).thenReturn(agentInstance2);

                AgentService agentServiceSpy = Mockito.spy(agentService);
                assertDoesNotThrow(() -> agentServiceSpy.bulkUpdateAgentAttributes(asList("uuid1", "uuid2"), asList("R1", "R2"), emptyStrList, asList("test", "prod"), emptyStrList, TRUE, environmentConfigService));

                verify(agentDao).bulkUpdateAgents(anyList());
                verify(agentServiceSpy).updateIdsAndGenerateCookiesForPendingAgents(anyList(), eq(TRUE));
            }

            @Test
            void shouldBulkEnableAgents() {
                Username username = new Username(new CaseInsensitiveString("test"));
                AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromAgent(agentIdentifier, AgentRuntimeStatus.Unknown, "cookie");
                AgentInstance pending = createFromLiveAgent(agentRuntimeInfo, new SystemEnvironment(), null);

                Agent agent = new Agent("UUID2", "remote-host", "50.40.30.20");
                agent.disable();
                AgentInstance fromConfigFile = AgentInstance.createFromAgent(agent, new SystemEnvironment(), null);

                when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
                when(goConfigService.getEnvironments()).thenReturn(new EnvironmentsConfig());
                when(agentInstances.findAgent("uuid")).thenReturn(pending);
                when(agentInstances.findAgent("UUID2")).thenReturn(fromConfigFile);

                List<String> uuids = asList(pending.getUuid(), fromConfigFile.getUuid());
                AgentService agentServiceSpy = Mockito.spy(agentService);
                assertDoesNotThrow(() -> agentServiceSpy.bulkUpdateAgentAttributes(uuids, emptyStrList, emptyStrList, emptyStrList, emptyStrList, TRUE, environmentConfigService));

                verify(agentDao).bulkUpdateAgents(anyList());
                verify(agentServiceSpy).updateIdsAndGenerateCookiesForPendingAgents(anyList(), eq(TRUE));
            }

            @Test
            void shouldThrow400BadRequestWhenNoOperationIsSpecifiedToBePerformedOnAgents() {
                BadRequestException e = assertThrows(BadRequestException.class, () -> agentService.bulkUpdateAgentAttributes(singletonList("uuid"), emptyStrList, emptyStrList, emptyStrList, emptyStrList, UNSET, environmentConfigService));
                verifyZeroInteractions(agentDao);
                assertThat(e.getMessage(), is("Bad Request. No operation is specified in the request to be performed on agents."));
            }

            @Test
            void shouldThrow400IfAgentDoesNotExist() {
                List<String> uuids = singletonList("uuid1");

                when(agentInstances.filterBy(uuids, Null)).thenReturn(uuids);

                RecordNotFoundException e = assertThrows(RecordNotFoundException.class, () -> agentService.bulkUpdateAgentAttributes(uuids, singletonList("resource"), emptyStrList, emptyStrList, emptyStrList, UNSET, environmentConfigService));
                verifyZeroInteractions(agentDao);
                assertThat(e.getMessage(), is("Agents with uuids [uuid1] were not found!"));
            }

            @Test
            void shouldThrow400IfResourcesForAnElasticAgentAreUpdated() {
                List<String> uuids = singletonList("uuid1");

                when(agentInstances.filterBy(uuids, Elastic)).thenReturn(uuids);

                BadRequestException e = assertThrows(BadRequestException.class, () -> agentService.bulkUpdateAgentAttributes(uuids, singletonList("resource"), emptyStrList, emptyStrList, emptyStrList, UNSET, environmentConfigService));

                verifyZeroInteractions(agentDao);
                assertThat(e.getMessage(), is("Resources on elastic agents with uuids [uuid1] can not be updated."));
            }

            @Test
            void shouldThrow422IfResourceNamesAreInvalid() {
                List<String> uuids = singletonList("uuid1");

                UnprocessableEntityException e = assertThrows(UnprocessableEntityException.class, () -> agentService.bulkUpdateAgentAttributes(uuids, singletonList("foo%"), emptyStrList, emptyStrList, emptyStrList, UNSET, environmentConfigService));

                verifyZeroInteractions(agentDao);
                assertThat(e.getMessage(), is("Validations failed for bulk update of agents. Error(s): {resources=[Resource name 'foo%' is not valid. Valid names much match '^[-\\w\\s|.]*$']}"));
            }

            @Test
            void shouldThrow400IfOperationsArePerformedOnAPendingAgentWithoutUpdatingTheState() {
                List<String> uuids = singletonList("uuid1");

                when(agentInstances.filterBy(uuids, AgentInstance.FilterBy.Pending)).thenReturn(uuids);

                BadRequestException e = assertThrows(BadRequestException.class, () -> agentService.bulkUpdateAgentAttributes(uuids, singletonList("resource"), emptyStrList, emptyStrList, emptyStrList, UNSET, environmentConfigService));

                verifyZeroInteractions(agentDao);
                assertThat(e.getMessage(), is("Pending agents [uuid1] must be explicitly enabled or disabled when performing any operations on them."));
            }

            @Test
            void shouldNotAddEnvsWhichAreAssociatedWithTheAgentFromConfigRepo() {
                String uuid = "uuid";
                List<String> uuids = singletonList(uuid);
                List<Agent> agents = singletonList(agent);
                AgentInstance agentInstance = mock(AgentInstance.class);

                when(agentDao.getAgentsByUUIDs(uuids)).thenReturn(agents);
                when(agentInstances.filterPendingAgents(uuids)).thenReturn(emptyList());
                when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
                when(agentInstance.getStatus()).thenReturn(fromConfig(Pending));

                String configEnvName = "config-repo-env";
                BasicEnvironmentConfig remoteEnvConfig = new BasicEnvironmentConfig(new CaseInsensitiveString(configEnvName));
                remoteEnvConfig.setOrigins(new RepoConfigOrigin());
                remoteEnvConfig.addAgent(uuid);

                when(environmentConfigService.find(configEnvName)).thenReturn(remoteEnvConfig);

                AgentService agentServiceSpy = Mockito.spy(agentService);

                assertDoesNotThrow(() -> agentServiceSpy.bulkUpdateAgentAttributes(uuids, emptyStrList, emptyStrList, asList("env1", configEnvName), emptyStrList, TRUE, environmentConfigService));
                verify(agentDao).getAgentsByUUIDs(uuids);
                ArgumentCaptor<List<Agent>> argumentCaptor = ArgumentCaptor.forClass(List.class);
                verify(agentDao).bulkUpdateAgents(argumentCaptor.capture());
                assertEquals("env1", argumentCaptor.getValue().get(0).getEnvironments());
                verify(agentServiceSpy).updateIdsAndGenerateCookiesForPendingAgents(eq(agents), eq(TRUE));
            }

            @Test
            void shouldUpdateIdsAndGenerateCookiesOnlyForPendingAgentsAndOnlyWhenTriStateIsSet() {
                Agent buildingAgent = setupBuildingAgent();
                Agent pendingAgent = setupPendingAgent();
                Agent disabledAgent = setupDisabledAgent();

                String cookie = "generated-cookie";
                when(uuidGenerator.randomUuid()).thenReturn(cookie);

                assertAllAgentsCookieIs(null, pendingAgent, disabledAgent, buildingAgent);
                agentService.updateIdsAndGenerateCookiesForPendingAgents(asList(buildingAgent, pendingAgent, disabledAgent), UNSET);
                assertAllAgentsCookieIs(null, pendingAgent, disabledAgent, buildingAgent);

                assertAllAgentsCookieIs(null, pendingAgent, disabledAgent, buildingAgent);
                agentService.updateIdsAndGenerateCookiesForPendingAgents(asList(buildingAgent, pendingAgent, disabledAgent), TRUE);
                assertAllAgentsCookieIs(cookie, pendingAgent);
                assertAllAgentsCookieIs(null, disabledAgent, buildingAgent);

                verify(agentDao).updateAgentIdFromDBIfAgentDoesNotHaveAnIdAndAgentExistInDB(pendingAgent);
            }

            private Agent setupBuildingAgent() {
                AgentInstance buildingInstance = AgentInstanceMother.building();
                Agent buildingAgent = buildingInstance.getAgent();
                when(agentInstances.findAgent(buildingInstance.getUuid())).thenReturn(buildingInstance);
                return buildingAgent;
            }

            private Agent setupPendingAgent() {
                AgentInstance pendingInstance = AgentInstanceMother.pending();
                Agent pendingAgent = pendingInstance.getAgent();
                when(agentInstances.findAgent(pendingInstance.getUuid())).thenReturn(pendingInstance);
                return pendingAgent;
            }

            private Agent setupDisabledAgent() {
                AgentInstance disabledInstance = AgentInstanceMother.disabled();
                Agent disabledAgent = disabledInstance.getAgent();
                when(agentInstances.findAgent(disabledInstance.getUuid())).thenReturn(disabledInstance);
                return disabledAgent;
            }

            private void assertAllAgentsCookieIs(String value, Agent... agents) {
                Arrays.stream(agents).forEach(agent -> assertThat(agent.getCookie(), is(value)));
            }
        }

        @Nested
        class SingleAgentUpdate {

            @Nested
            class PositiveTests {
                private void assertThatUpdateIsSuccessfulWithSpecifiedValues(Agent updatedAgent, String hostname, String envs,
                                                                             String resources, boolean isDisabled) {
                    verify(agentDao).saveOrUpdate(any(Agent.class));

                    assertThat(updatedAgent.getHostname(), is(hostname));
                    assertThat(updatedAgent.getResources(), is(resources));
                    assertThat(updatedAgent.isDisabled(), is(isDisabled));
                    assertThat(updatedAgent.getEnvironments(), is(envs));
                }

                @Test
                void shouldUpdateAgentAttributes() {
                    Username username = new Username(new CaseInsensitiveString("test"));

                    when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
                    when(agentDao.fetchAgentFromDBByUUID(uuid)).thenReturn(agent);

                    String hostname = "new-hostname";
                    String resources = "resource1,resource2";
                    TriState state = TRUE;

                    AgentInstance agentInstance = agentService.updateAgentAttributes(uuid, hostname, resources, "env1,env2", state);
                    assertThatUpdateIsSuccessfulWithSpecifiedValues(agentInstance.getAgent(), hostname, "env1,env2", resources, false);
                }

                @Test
                void shouldOnlyUpdateAttributesThatAreSpecified() {
                    Username username = new Username(new CaseInsensitiveString("test"));

                    when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
                    when(agentDao.fetchAgentFromDBByUUID(uuid)).thenReturn(agent);

                    AgentInstance agentInstance = agentService.updateAgentAttributes(uuid, null, null, "env1,env2", TRUE);
                    assertThatUpdateIsSuccessfulWithSpecifiedValues(agentInstance.getAgent(), "host", "env1,env2", "", false);
                }

                @Test
                void shouldResetEnvironmentsWhenEmptyEnvironmentsConfigIsSpecified() {
                    Username username = new Username(new CaseInsensitiveString("test"));

                    when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
                    when(agentDao.fetchAgentFromDBByUUID(uuid)).thenReturn(agent);

                    String hostname = "new-hostname";
                    String resources = "resource1,resource2";
                    AgentInstance agentInstance = agentService.updateAgentAttributes(uuid, hostname, resources, "", TRUE);

                    assertThatUpdateIsSuccessfulWithSpecifiedValues(agentInstance.getAgent(), hostname, null, resources, false);
                }

                @Test
                void shouldResetResourcesWhenEmptyCommaSeparatedStringOfResourcesIsSpecified() {
                    Username username = new Username(new CaseInsensitiveString("test"));

                    when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
                    when(agentDao.fetchAgentFromDBByUUID(uuid)).thenReturn(agent);

                    String hostname = "new-hostname";
                    String resources = "  ";
                    AgentInstance agentInstance = agentService.updateAgentAttributes(uuid, hostname, resources, "e1,e2", TRUE);

                    assertThatUpdateIsSuccessfulWithSpecifiedValues(agentInstance.getAgent(), hostname, "e1,e2", null, false);
                }

                @Test
                void shouldUpdatePendingAgentAttributesProvidedItIsEnableOrDisabled() {
                    Username username = new Username(new CaseInsensitiveString("test"));

                    when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
                    when(agentInstance.isPending()).thenReturn(true);
                    when(agentInstance.getAgent()).thenReturn(agent);

                    String hostname = "new-hostname";
                    String resources = "resource1,resource2";
                    TriState state = FALSE;

                    AgentInstance agentInstance = agentService.updateAgentAttributes(uuid, hostname, resources, "env1,env2", state);
                    assertThatUpdateIsSuccessfulWithSpecifiedValues(agentInstance.getAgent(), hostname, "env1,env2", resources, true);
                }

                @Test
                void shouldNotUpdateTheCacheForPendingAgentIfTheDBSaveFails() {
                    Username username = new Username(new CaseInsensitiveString("test"));

                    AgentInstance pending = pending();
                    String uuid = pending.getUuid();

                    when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
                    when(agentInstances.findAgentAndRefreshStatus(uuid)).thenReturn(pending);
                    doThrow(RuntimeException.class).when(agentDao).saveOrUpdate(any(Agent.class));

                    String hostname = "new-hostname";
                    String resources = "resource1,resource2";
                    TriState state = FALSE;

                    assertThrows(RuntimeException.class, () -> agentService.updateAgentAttributes(uuid, hostname, resources, "env1,env2", state));
                    assertThat(pending.getAgentConfigStatus(), is(Pending));
                    assertThat(pending.getAgent().getResources(), is(""));
                    assertThat(pending.getAgent().getEnvironments(), is(""));
                    assertThat(pending.getHostname(), is("CCeDev03"));
                }
            }

            @Nested
            class NegativeTests {
                @Test
                void shouldThrow400BadRequestWhenNoOperationIsSpecifiedToBePerformedOnAgent() {
                    when(agentDao.fetchAgentFromDBByUUID(uuid)).thenReturn(agent);
                    BadRequestException e = assertThrows(BadRequestException.class, () -> agentService.updateAgentAttributes(uuid, null, null, null, UNSET));

                    verify(agentDao, times(0)).saveOrUpdate(any(Agent.class));
                    assertThat(e.getMessage(), is("Bad Request. No operation is specified in the request to be performed on agent."));
                }

                @Test
                void shouldThrow404IfAgentDoesNotExist() {
                    String uuid = "non-existent-uuid";
                    Username username = new Username(new CaseInsensitiveString("test"));
                    AgentInstance mockAgentInstance = mock(AgentInstance.class);

                    when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
                    when(agentDao.fetchAgentFromDBByUUID(uuid)).thenReturn(agent);
                    when(agentInstances.findAgent(uuid)).thenReturn(mockAgentInstance);
                    when(mockAgentInstance.isNullAgent()).thenReturn(true);
                    when(mockAgentInstance.getUuid()).thenReturn(uuid);

                    RecordNotFoundException e = assertThrows(RecordNotFoundException.class, () -> agentService.updateAgentAttributes(uuid, "new-hostname", "resource1,resource2", "env1,env2", TRUE));

                    verify(agentDao, times(0)).saveOrUpdate(any(Agent.class));
                    assertThat(e.getMessage(), is(format("Agent with uuid '%s' was not found!", uuid)));
                }

                @Test
                void shouldThrow400IfOperationsPerformedOnPendingAgentWithoutUpdatingTheState() {
                    Username username = new Username(new CaseInsensitiveString("test"));

                    when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
                    when(agentInstance.isPending()).thenReturn(true);
                    when(agentInstance.getAgent()).thenReturn(agent);

                    BadRequestException e = assertThrows(BadRequestException.class, () -> agentService.updateAgentAttributes(uuid, "new-hostname", "resource1", "env1", UNSET));

                    verify(agentDao, times(0)).saveOrUpdate(any(Agent.class));
                    assertThat(e.getMessage(), is("Pending agent [uuid] must be explicitly enabled or disabled when performing any operation on it."));
                }

                @Test
                void shouldThrow422IfSpecifiedResourceNamesAreInvalid() {
                    Username username = new Username(new CaseInsensitiveString("test"));

                    when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
                    when(agentDao.fetchAgentFromDBByUUID(uuid)).thenReturn(agent);

                    AgentInstance agentInstance = agentService.updateAgentAttributes(uuid, "new-hostname", "res%^1", "env1", TRUE);

                    verify(agentDao, times(0)).saveOrUpdate(any(Agent.class));

                    Agent agent = agentInstance.getAgent();
                    assertTrue(agent.hasErrors());
                    assertThat(agent.errors().on(JobConfig.RESOURCES), is("Resource name 'res%^1' is not valid. Valid names much match '^[-\\w\\s|.]*$'"));
                }
            }
        }
    }

    @Nested
    class UpdateRuntimeInfo {
        @Test
        void shouldUpdateRuntimeInfo() {
            String cookie = "cookie";
            AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(agentIdentifier, Idle, currentWorkingDirectory(), cookie);
            when(agentDao.cookieFor(runtimeInfo.getIdentifier())).thenReturn(cookie);
            agentService.updateRuntimeInfo(runtimeInfo);
            verify(agentInstances).updateAgentRuntimeInfo(runtimeInfo);
        }

        @Test
        void shouldThrowExceptionWhenAgentWithNoCookieTriesToUpdateRuntimeInfo() {
            AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(agentIdentifier, Idle, currentWorkingDirectory(), null);

            try (LogFixture logFixture = logFixtureFor(AgentService.class, Level.DEBUG)) {
                try {
                    agentService.updateRuntimeInfo(runtimeInfo);
                    fail("should throw exception when no cookie is set");
                } catch (Exception e) {
                    assertThat(e, instanceOf(AgentNoCookieSetException.class));
                    assertThat(e.getMessage(), is(format("Agent [%s] has no cookie set", runtimeInfo.agentInfoDebugString())));
                    assertThat(logFixture.getRawMessages(), hasItem(format("Agent [%s] has no cookie set", runtimeInfo.agentInfoDebugString())));
                }
            }

        }

        @Test
        void shouldThrowExceptionWhenADuplicateAgentTriesToUpdateRuntimeInfo() {
            AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(agentIdentifier, Idle, currentWorkingDirectory(), null);
            runtimeInfo.setCookie("invalid_cookie");
            AgentInstance original = createFromLiveAgent(new AgentRuntimeInfo(agentIdentifier, Idle, currentWorkingDirectory(), null), new SystemEnvironment(), null);

            try (LogFixture logFixture = logFixtureFor(AgentService.class, Level.DEBUG)) {
                try {
                    when(agentService.findAgentAndRefreshStatus(runtimeInfo.getUUId())).thenReturn(original);
                    agentService.updateRuntimeInfo(runtimeInfo);
                    fail("should throw exception when cookie mismatched");
                } catch (Exception e) {
                    assertThat(e.getMessage(), is(format("Agent [%s] has invalid cookie", runtimeInfo.agentInfoDebugString())));
                    assertThat(logFixture.getRawMessages(), hasItem(format("Found agent [%s] with duplicate uuid. Please check the agent installation.", runtimeInfo.agentInfoDebugString())));

                    String msg = format("[%s] has duplicate unique identifier which conflicts with [%s]",
                            runtimeInfo.agentInfoForDisplay(), original.agentInfoForDisplay());

                    String desc = "Please check the agent installation. Click <a href='" + docsUrl("/faq/agent_guid_issue.html") + "' target='_blank'>here</a> for more info.";
                    ServerHealthState serverHealthState = warning(msg, desc, duplicateAgent(forAgent(runtimeInfo.getCookie())), THIRTY_SECONDS);

                    verify(serverHealthService).update(serverHealthState);
                }
            }

            verify(agentInstances).findAgentAndRefreshStatus(runtimeInfo.getUUId());
            verifyNoMoreInteractions(agentInstances);
        }
    }

    @Nested
    class SaveOrUpdateAgent {
        @Test
        void shouldSaveOrUpdateAgent() {
            Agent mockAgent = mock(Agent.class);
            doNothing().when(mockAgent).validate();
            when(mockAgent.hasErrors()).thenReturn(false);

            agentService.saveOrUpdate(mockAgent);

            verify(mockAgent).validate();
            verify(mockAgent).hasErrors();
            verify(agentDao).saveOrUpdate(mockAgent);
            verify(agentDao).saveOrUpdate(mockAgent);
        }

        @Test
        void shouldNotSaveOrUpdateAgentIfAgentHasValidationErrors() {
            Agent mockAgent = mock(Agent.class);
            doNothing().when(mockAgent).validate();
            when(mockAgent.hasErrors()).thenReturn(true);

            agentService.saveOrUpdate(mockAgent);

            verify(mockAgent).validate();
            verify(mockAgent).hasErrors();
            verify(agentDao, never()).saveOrUpdate(mockAgent);
        }
    }

    @Nested
    class DisableAgents {
        @Test
        void shouldDoNothingIfDisableAgentsIsCalledWithEmptyListOfUUIDs() {
            agentService.disableAgents(emptyList());
            verify(agentDao, times(0)).disableAgents(anyList());
        }

        @Test
        void shouldDisableAgentsWhenCalledWithNonEmptyListOfUUIDs() {
            List<String> uuids = asList("uuid1", "uuid2");
            agentService.disableAgents(uuids);
            verify(agentDao).disableAgents(uuids);
        }
    }

    @Nested
    class DeleteAgents {
        @Nested
        class NegativeTests {
            @Test
            void shouldThrow404WhenDeleteAgentsIsCalledWithSingleAgentThatDoesNotExist() {
                String uuid = "1234";
                Username username = new Username(new CaseInsensitiveString("test"));

                AgentInstance mockAgentInstance = mock(AgentInstance.class);
                Agent mockAgent = mock(Agent.class);

                when(securityService.hasOperatePermissionForAgents(username)).thenReturn(true);
                when(mockAgentInstance.canBeDeleted()).thenReturn(true);

                when(agentInstances.findAgentAndRefreshStatus(uuid)).thenReturn(mockAgentInstance);
                when(mockAgentInstance.getAgent()).thenReturn(mockAgent);

                when(mockAgentInstance.isNullAgent()).thenReturn(true);
                when(mockAgentInstance.getUuid()).thenReturn(uuid);
                when(mockAgentInstance.getAgent().getUuid()).thenReturn(uuid);

                AgentService agentService = new AgentService(new SystemEnvironment(), agentInstances,
                        agentDao, uuidGenerator, serverHealthService = mock(ServerHealthService.class), null);

                RecordNotFoundException e = assertThrows(RecordNotFoundException.class, () -> agentService.deleteAgents(singletonList(uuid)));
                assertThat(e.getMessage(), is("Agent with uuid '1234' was not found!"));
            }

            @Test
            void shouldThrow406WhenDeleteAgentsIsCalledWithSingleNotDisabledAgent() {
                String uuid = "1234";
                Username username = new Username(new CaseInsensitiveString("test"));

                AgentInstance mockAgentInstance = mock(AgentInstance.class);
                Agent mockAgent = mock(Agent.class);

                when(securityService.hasOperatePermissionForAgents(username)).thenReturn(true);
                when(mockAgentInstance.canBeDeleted()).thenReturn(false);
                when(agentInstances.findAgentAndRefreshStatus(uuid)).thenReturn(mockAgentInstance);
                when(mockAgentInstance.getAgent()).thenReturn(mockAgent);
                when(mockAgent.isNull()).thenReturn(false);

                AgentService agentService = new AgentService(new SystemEnvironment(), agentInstances,
                        agentDao, uuidGenerator, serverHealthService = mock(ServerHealthService.class), null);

                UnprocessableEntityException e = assertThrows(UnprocessableEntityException.class, () -> agentService.deleteAgents(singletonList(uuid)));
                assertThat(e.getMessage(), is("Failed to delete an agent, as it is not in a disabled state or is still building."));
            }

            @Test
            void shouldThrow406WhenDeleteAgentsIsCalledWithMultipleAgentsWithOneBeingNonDisabledAgent() {
                String uuid1 = "1234";
                String uuid2 = "4321";
                Username username = new Username(new CaseInsensitiveString("test"));

                AgentInstance mockAgentInstance1 = mock(AgentInstance.class);
                AgentInstance mockAgentInstance2 = mock(AgentInstance.class);

                Agent mockAgent = mock(Agent.class);

                when(securityService.hasOperatePermissionForAgents(username)).thenReturn(true);
                when(mockAgentInstance1.canBeDeleted()).thenReturn(true);
                when(mockAgentInstance2.canBeDeleted()).thenReturn(false);

                when(agentInstances.findAgentAndRefreshStatus(uuid1)).thenReturn(mockAgentInstance1);
                when(agentInstances.findAgentAndRefreshStatus(uuid2)).thenReturn(mockAgentInstance2);

                when(mockAgentInstance1.getAgent()).thenReturn(mockAgent);
                when(mockAgentInstance2.getAgent()).thenReturn(mockAgent);

                when(mockAgent.isNull()).thenReturn(false);

                AgentService agentService = new AgentService(new SystemEnvironment(), agentInstances,
                        agentDao, uuidGenerator, serverHealthService = mock(ServerHealthService.class), null);

                UnprocessableEntityException e = assertThrows(UnprocessableEntityException.class, () -> agentService.deleteAgents(asList(uuid1, uuid2)));
                assertThat(e.getMessage(), is("Could not delete any agents, as one or more agents might not be disabled or are still building."));
            }
        }

        @Nested
        class PositiveTests {
            @Test
            void shouldReturn200WhenDeleteAgentsIsCalledWithSingleDisabledAgent() {
                String uuid = "1234";
                Username username = new Username(new CaseInsensitiveString("test"));

                AgentInstance mockAgentInstance = mock(AgentInstance.class);
                Agent mockAgent = mock(Agent.class);

                when(securityService.hasOperatePermissionForAgents(username)).thenReturn(true);
                when(mockAgentInstance.canBeDeleted()).thenReturn(true);
                when(agentInstances.findAgentAndRefreshStatus(uuid)).thenReturn(mockAgentInstance);
                when(mockAgentInstance.getAgent()).thenReturn(mockAgent);
                when(mockAgent.isNull()).thenReturn(false);
                when(mockAgentInstance.getAgent().getUuid()).thenReturn(uuid);
                doNothing().when(agentDao).bulkSoftDelete(singletonList(uuid));

                AgentService agentService = new AgentService(new SystemEnvironment(), agentInstances,
                        agentDao, uuidGenerator, serverHealthService = mock(ServerHealthService.class), null);

                assertDoesNotThrow(() -> agentService.deleteAgents(singletonList(uuid)));
                verify(agentDao).bulkSoftDelete(singletonList(uuid));
            }

            @Test
            void shouldReturn200WhenDeleteAgentsIsCalledWithNullAsListOfUUIDs() {
                Username username = new Username(new CaseInsensitiveString("test"));

                when(securityService.hasOperatePermissionForAgents(username)).thenReturn(true);

                serverHealthService = mock(ServerHealthService.class);
                AgentService agentService = new AgentService(new SystemEnvironment(), agentInstances, agentDao, uuidGenerator, serverHealthService, null);

                assertDoesNotThrow(() -> agentService.deleteAgents(null));
            }

            @Test
            void shouldReturn200WhenDeleteAgentsIsCalledWithMultipleDisabledAgents() {
                String uuid1 = "1234";
                String uuid2 = "4321";
                Username username = new Username(new CaseInsensitiveString("test"));

                AgentInstance mockAgentInstance1 = mock(AgentInstance.class);
                AgentInstance mockAgentInstance2 = mock(AgentInstance.class);

                Agent mockAgent = mock(Agent.class);

                when(securityService.hasOperatePermissionForAgents(username)).thenReturn(true);
                when(mockAgentInstance1.canBeDeleted()).thenReturn(true);
                when(mockAgentInstance2.canBeDeleted()).thenReturn(true);

                when(agentInstances.findAgentAndRefreshStatus(uuid1)).thenReturn(mockAgentInstance1);
                when(agentInstances.findAgentAndRefreshStatus(uuid2)).thenReturn(mockAgentInstance2);

                when(mockAgentInstance1.getAgent()).thenReturn(mockAgent);
                when(mockAgentInstance2.getAgent()).thenReturn(mockAgent);

                when(mockAgent.isNull()).thenReturn(false);

                serverHealthService = mock(ServerHealthService.class);
                AgentService agentService = new AgentService(new SystemEnvironment(), agentInstances, agentDao, uuidGenerator, serverHealthService, null);

                assertDoesNotThrow(() -> agentService.deleteAgents(asList(uuid1, uuid2)));
            }
        }
    }

    @Nested
    class AgentRegistration {
        @Test
        void isRegisteredShouldReturnTrueIfSpecifiedUUIDIsRegistered() {
            AgentInstance building = building();
            when(agentInstances.findAgent("uuid")).thenReturn(building);
            assertTrue(agentService.isRegistered("uuid"));
        }

        @Test
        void isRegisteredShouldReturnFalseIfSpecifiedUUIDDoesNotExist() {
            when(agentInstances.findAgent("uuid")).thenReturn(nullInstance());
            assertFalse(agentService.isRegistered("uuid"));
        }

        @Test
        void isRegisteredShouldReturnFalseIfSpecifiedUUIDIsNotRegistered() {
            when(agentInstances.findAgent("uuid")).thenReturn(pendingInstance());
            assertFalse(agentService.isRegistered("uuid"));
        }

        @Test
        void shouldDoNothingWhenRegisterAgentChangeListenerIsCalledWithNullListener() {
            Set<AgentChangeListener> setOfListeners = new HashSet<>();

            agentService.setAgentChangeListeners(setOfListeners);
            agentService.registerAgentChangeListeners(null);

            assertThat(setOfListeners.size(), is(0));
        }

        @Test
        void registerShouldGenerateCookieAndSaveAgentToDB() {
            Agent mockAgent = mock(Agent.class);
            when(mockAgent.getCookie()).thenReturn(null);
            when(mockAgent.hasErrors()).thenReturn(false);
            doNothing().when(mockAgent).validate();

            agentService.register(mockAgent);

            String cookie = verify(uuidGenerator).randomUuid();
            verify(mockAgent).setCookie(cookie);

            verify(mockAgent).validate();
            verify(mockAgent).hasErrors();

            verify(agentDao).saveOrUpdate(mockAgent);
        }
    }

    @Nested
    class AssociateCookie {
        @Test
        void shouldAssociateCookieForAnAgent() {
            when(uuidGenerator.randomUuid()).thenReturn("foo");
            assertThat(agentService.assignCookie(agentIdentifier), is("foo"));
            verify(agentDao).associateCookie(eq(agentIdentifier), any(String.class));
        }
    }

    @Nested
    class RequestRegistration {
        @Test
        void requestRegistrationShouldReturnNullPrivateKeyRegistrationWhenCalledWithPendingAgent() {
            AgentRuntimeInfo runtimeInfo = fromServer(pending().getAgent(), false, "sandbox", 0L, "linux");
            AgentInstance agentInstance = mock(AgentInstance.class);
            Agent agent = mock(Agent.class);

            when(agentInstances.register(runtimeInfo)).thenReturn(agentInstance);
            when(agentInstance.assignCertification()).thenReturn(false);
            when(agentInstance.getAgent()).thenReturn(agent);

            boolean registration = agentService.requestRegistration(runtimeInfo);
            assertThat(registration, is(false));
            verifyZeroInteractions(agentDao);
            verify(agent, never()).getCookie();
            verify(agent, never()).setCookie(anyString());
            verify(agent, never()).validate();
            verify(agent, never()).hasErrors();
        }

        @Test
        void requestRegistrationShouldReturnValidRegistrationWhenCalledWithRegisteredAgent() {
            AgentRuntimeInfo runtimeInfo = fromServer(building().getAgent(), false, "sandbox", 0l, "linux");
            AgentInstance agentInstance = mock(AgentInstance.class);
            Agent agent = mock(Agent.class);

            when(agentInstances.register(runtimeInfo)).thenReturn(agentInstance);
            when(agentInstance.assignCertification()).thenReturn(true);
            when(agentInstance.getAgent()).thenReturn(agent);
            when(agentInstance.isRegistered()).thenReturn(true);

            String cookie = "cookie";
            when(uuidGenerator.randomUuid()).thenReturn(cookie);

            boolean requestedRegistration = agentService.requestRegistration(runtimeInfo);
            assertThat(requestedRegistration, is(true));

            verify(agentDao, only()).saveOrUpdate(agent);
            verify(agent, times(1)).cookieAssigned();
            verify(agent, times(1)).setCookie(cookie);
            verify(agent, times(1)).validate();
            verify(agent, times(2)).hasErrors();
        }

        @Test
        void requestRegistrationShouldBombIfAgentToBeRegisteredHasValidationErrors() {
            AgentInstance mockAgentInstance = mock(AgentInstance.class);
            Agent mockAgent = mock(Agent.class);

            AgentRuntimeInfo runtimeInfo = fromServer(building().getAgent(), false, "sandbox", 0l, "linux");

            when(agentInstances.register(runtimeInfo)).thenReturn(mockAgentInstance);
            when(mockAgentInstance.assignCertification()).thenReturn(false);
            when(mockAgentInstance.getAgent()).thenReturn(mockAgent);
            when(mockAgentInstance.isRegistered()).thenReturn(true);

            when(mockAgent.hasErrors()).thenReturn(true);

            String cookie = "cookie";
            when(uuidGenerator.randomUuid()).thenReturn(cookie);

            assertThrows(GoConfigInvalidException.class, () -> agentService.requestRegistration(runtimeInfo));
        }
    }

    @Nested
    class AgentAssociationWithEnvironment {
        @Test
        void shouldAddAgentsAssociationToTheSpecifiedEnv() {
            AgentInstance agentInstance = mock(AgentInstance.class);
            Username username = new Username(new CaseInsensitiveString("test"));
            String uuid = "uuid";
            Agent agentConfigForUUID1 = mock(Agent.class);

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(agentDao.getAgentByUUIDFromCacheOrDB(uuid)).thenReturn(agent);
            when(agentDao.getAgentByUUIDFromCacheOrDB("uuid1")).thenReturn(agentConfigForUUID1);

            EnvironmentsConfig envConfigs = new EnvironmentsConfig();
            BasicEnvironmentConfig testEnv = new BasicEnvironmentConfig(new CaseInsensitiveString("test"));
            envConfigs.add(testEnv);

            when(goConfigService.getEnvironments()).thenReturn(envConfigs);
            when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
            when(agentInstances.findAgent("uuid1")).thenReturn(mock(AgentInstance.class));

            assertDoesNotThrow(() -> agentService.updateAgentsAssociationOfEnvironment(testEnv, asList(uuid, "uuid1")));

            ArgumentCaptor<List<Agent>> argument = ArgumentCaptor.forClass(List.class);

            List<Agent> agents = asList(agentConfigForUUID1, agent);

            verify(agentDao).bulkUpdateAgents(argument.capture());
            assertEquals(agents.size(), argument.getValue().size());
            assertTrue(argument.getValue().contains(agents.get(0)));
            assertTrue(argument.getValue().contains(agents.get(1)));
        }

        @Test
        void shouldUpdateAgentsAssociationOfEnvironment() {
            AgentInstance agentInstance = mock(AgentInstance.class);
            Username username = new Username(new CaseInsensitiveString("test"));
            String uuid = "uuid";
            Agent agentConfigForUUID1 = mock(Agent.class);

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(agentDao.getAgentByUUIDFromCacheOrDB(uuid)).thenReturn(agent);
            when(agentDao.getAgentByUUIDFromCacheOrDB("uuid1")).thenReturn(agentConfigForUUID1);
            when(agentConfigForUUID1.getEnvironments()).thenReturn("test");

            agent.setEnvironments("test");
            EnvironmentsConfig envConfigs = new EnvironmentsConfig();
            BasicEnvironmentConfig testEnv = new BasicEnvironmentConfig(new CaseInsensitiveString("test"));
            testEnv.addAgent("uuid1");
            testEnv.addAgent("uuid2");
            envConfigs.add(testEnv);

            when(goConfigService.getEnvironments()).thenReturn(envConfigs);
            when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
            when(agentInstances.findAgent("uuid2")).thenReturn(mock(AgentInstance.class));

            assertDoesNotThrow(() -> agentService.updateAgentsAssociationOfEnvironment(testEnv, asList(uuid, "uuid2")));
            verify(agentDao).bulkUpdateAgents(anyList());
        }

        @Test
        void shouldRemoveAgentsAssociationFromTheSpecifiedEnv() {
            AgentInstance agentInstance = mock(AgentInstance.class);
            Username username = new Username(new CaseInsensitiveString("test"));
            String uuid = "uuid";
            Agent agentConfigForUUID1 = mock(Agent.class);

            when(agentConfigForUUID1.getEnvironments()).thenReturn("test");
            agent.setEnvironments("test");

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(agentDao.getAgentByUUIDFromCacheOrDB(uuid)).thenReturn(agent);
            when(agentDao.getAgentByUUIDFromCacheOrDB("uuid1")).thenReturn(agentConfigForUUID1);

            EnvironmentsConfig envConfigs = new EnvironmentsConfig();
            BasicEnvironmentConfig testEnv = new BasicEnvironmentConfig(new CaseInsensitiveString("test"));
            envConfigs.add(testEnv);
            testEnv.addAgent(uuid);
            testEnv.addAgent("uuid1");

            when(goConfigService.getEnvironments()).thenReturn(envConfigs);
            when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
            when(agentInstances.findAgent("uuid1")).thenReturn(mock(AgentInstance.class));

            assertDoesNotThrow(() -> agentService.updateAgentsAssociationOfEnvironment(testEnv, emptyList()));

            List<Agent> agents = asList(agentConfigForUUID1, agent);

            ArgumentCaptor<List<Agent>> argument = ArgumentCaptor.forClass(List.class);

            verify(agentDao).bulkUpdateAgents(argument.capture());
            assertEquals(agents.size(), argument.getValue().size());
            assertTrue(argument.getValue().contains(agents.get(0)));
            assertTrue(argument.getValue().contains(agents.get(1)));
        }

        @Test
        void shouldNotDoAnythingIfEmptyAgentsUuidListAndNoAgentAssociationConfiguredWithEnvConfig() {
            Username username = new Username(new CaseInsensitiveString("test"));
            agent.setEnvironments("test");

            EnvironmentsConfig envConfigs = new EnvironmentsConfig();
            BasicEnvironmentConfig testEnv = new BasicEnvironmentConfig(new CaseInsensitiveString("test"));
            envConfigs.add(testEnv);

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(goConfigService.getEnvironments()).thenReturn(envConfigs);

            assertDoesNotThrow(() -> agentService.updateAgentsAssociationOfEnvironment(testEnv, emptyStrList));

            verifyNoMoreInteractions(agentDao);
        }

        @Test
        void shouldReturnMapOfAgentInstanceToSortedEnvironments() {
            String envs1 = "qa1,dev,test,prod,qa2,stage,perf";
            String[] splitEnvs = envs1.split("\\s*,\\s*");
            sort(splitEnvs);
            String envs2 = "stage,qa1,test,qa2,prod,perf,dev";

            AgentInstance pendingInstance = pendingInstance();
            pendingInstance.getAgent().setEnvironments(envs1);

            AgentInstance buildingInstance = building();
            buildingInstance.getAgent().setEnvironments(envs2);


            AgentInstances mockedAgentInstances = new AgentInstances(mock(AgentStatusChangeListener.class));
            mockedAgentInstances.add(pendingInstance);
            mockedAgentInstances.add(buildingInstance);

            when(agentInstances.getAllAgents()).thenReturn(mockedAgentInstances);

            Map<AgentInstance, Collection<String>> map = agentService.getAgentInstanceToSortedEnvMap();
            assertThat(map.size(), is(2));

            map.keySet().forEach(key -> {
                Collection<String> envs = map.get(key);
                assertThat(envs.size(), is(equalTo(7)));
                assertThat(Ordering.natural().isOrdered(envs), is(true));
                assertThat(envs.toArray(), is(splitEnvs));
            });
        }

        @Test
        void shouldReturnMapOfAgentInstanceToEmptyEnvironmentsWhenThereAreNoEnvironmentsAssociatedWithAnyAgent() {
            AgentInstance pendingInstance = pendingInstance();
            AgentInstance buildingInstance = building();

            AgentInstances mockedInstances = new AgentInstances(mock(AgentStatusChangeListener.class));
            mockedInstances.add(pendingInstance);
            mockedInstances.add(buildingInstance);

            when(agentInstances.getAllAgents()).thenReturn(mockedInstances);

            Map<AgentInstance, Collection<String>> map = agentService.getAgentInstanceToSortedEnvMap();
            assertThat(map.size(), is(2));

            map.keySet().forEach(key -> {
                Collection<String> envs = map.get(key);
                assertThat(envs.size(), is(equalTo(0)));
            });
        }
    }

    @Nested
    class AgentAssociationWithEnvironmentForPatchRequest {
        @Test
        void shouldAddAgentsAssociationToTheSpecifiedEnv() {
            AgentInstance agentInstance = mock(AgentInstance.class);
            Username username = new Username(new CaseInsensitiveString("test"));
            String uuid = "uuid";
            Agent agentConfigForUUID1 = mock(Agent.class);

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(agentDao.getAgentByUUIDFromCacheOrDB(uuid)).thenReturn(agent);
            when(agentDao.getAgentByUUIDFromCacheOrDB("uuid1")).thenReturn(agentConfigForUUID1);

            EnvironmentsConfig envConfigs = new EnvironmentsConfig();
            BasicEnvironmentConfig testEnv = new BasicEnvironmentConfig(new CaseInsensitiveString("test"));
            envConfigs.add(testEnv);

            when(goConfigService.getEnvironments()).thenReturn(envConfigs);
            when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
            when(agentInstances.findAgent("uuid1")).thenReturn(mock(AgentInstance.class));

            assertDoesNotThrow(() -> agentService.updateAgentsAssociationOfEnvironment(testEnv, asList(uuid, "uuid1"), Collections.emptyList()));

            ArgumentCaptor<List<Agent>> argument = ArgumentCaptor.forClass(List.class);

            List<Agent> agents = asList(agentConfigForUUID1, agent);

            verify(agentDao).bulkUpdateAgents(argument.capture());
            assertEquals(agents.size(), argument.getValue().size());
            assertTrue(argument.getValue().contains(agents.get(0)));
            assertTrue(argument.getValue().contains(agents.get(1)));
        }

        @Test
        void shouldUpdateAgentsAssociationOfEnvironment() {
            AgentInstance agentInstance = mock(AgentInstance.class);
            Username username = new Username(new CaseInsensitiveString("test"));
            String uuid = "uuid";
            Agent agentConfigForUUID1 = mock(Agent.class);

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(agentDao.getAgentByUUIDFromCacheOrDB(uuid)).thenReturn(agent);
            when(agentDao.getAgentByUUIDFromCacheOrDB("uuid1")).thenReturn(agentConfigForUUID1);
            when(agentConfigForUUID1.getEnvironments()).thenReturn("test");

            agent.setEnvironments("test");
            EnvironmentsConfig envConfigs = new EnvironmentsConfig();
            BasicEnvironmentConfig testEnv = new BasicEnvironmentConfig(new CaseInsensitiveString("test"));
            testEnv.addAgent("uuid1");
            testEnv.addAgent("uuid2");
            envConfigs.add(testEnv);

            when(goConfigService.getEnvironments()).thenReturn(envConfigs);
            when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
            when(agentInstances.findAgent("uuid2")).thenReturn(mock(AgentInstance.class));

            assertDoesNotThrow(() -> agentService.updateAgentsAssociationOfEnvironment(testEnv, asList(uuid, "uuid2")));
            verify(agentDao).bulkUpdateAgents(anyList());
        }

        @Test
        void shouldRemoveAgentsAssociationFromTheSpecifiedEnv() {
            AgentInstance agentInstance = mock(AgentInstance.class);
            Username username = new Username(new CaseInsensitiveString("test"));
            String uuid = "uuid";
            Agent agentConfigForUUID1 = mock(Agent.class);

            when(agentConfigForUUID1.getEnvironments()).thenReturn("test");
            agent.setEnvironments("test");

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(agentDao.getAgentByUUIDFromCacheOrDB(uuid)).thenReturn(agent);
            when(agentDao.getAgentByUUIDFromCacheOrDB("uuid1")).thenReturn(agentConfigForUUID1);

            EnvironmentsConfig envConfigs = new EnvironmentsConfig();
            BasicEnvironmentConfig testEnv = new BasicEnvironmentConfig(new CaseInsensitiveString("test"));
            envConfigs.add(testEnv);
            testEnv.addAgent(uuid);
            testEnv.addAgent("uuid1");

            when(goConfigService.getEnvironments()).thenReturn(envConfigs);
            when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
            when(agentInstances.findAgent("uuid1")).thenReturn(mock(AgentInstance.class));

            assertDoesNotThrow(() -> agentService.updateAgentsAssociationOfEnvironment(testEnv, emptyList(), Arrays.asList(agent.getUuid(), "uuid1")));

            List<Agent> agents = asList(agentConfigForUUID1, agent);

            ArgumentCaptor<List<Agent>> argument = ArgumentCaptor.forClass(List.class);

            verify(agentDao).bulkUpdateAgents(argument.capture());
            assertEquals(agents.size(), argument.getValue().size());
            assertTrue(argument.getValue().contains(agents.get(0)));
            assertTrue(argument.getValue().contains(agents.get(1)));
        }

        @Test
        void shouldNotDoAnythingIfEmptyAgentsUuidListAndNoAgentAssociationConfiguredWithEnvConfig() {
            Username username = new Username(new CaseInsensitiveString("test"));
            agent.setEnvironments("test");

            EnvironmentsConfig envConfigs = new EnvironmentsConfig();
            BasicEnvironmentConfig testEnv = new BasicEnvironmentConfig(new CaseInsensitiveString("test"));
            envConfigs.add(testEnv);

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(goConfigService.getEnvironments()).thenReturn(envConfigs);

            assertDoesNotThrow(() -> agentService.updateAgentsAssociationOfEnvironment(testEnv, emptyStrList, emptyStrList));

            verifyNoMoreInteractions(agentDao);
        }
    }

    @Nested
    class AgentChangeListenerMethods {
        @Test
        void shouldRegisterAgentChangeListener() {
            Set<AgentChangeListener> setOfListeners = new HashSet<>();
            agentService.setAgentChangeListeners(setOfListeners);

            AgentChangeListener mockListener = mock(AgentChangeListener.class);
            agentService.registerAgentChangeListeners(mockListener);

            assertThat(setOfListeners.size(), is(1));
            setOfListeners.forEach(listener -> assertThat(listener, is(mockListener)));
        }

        @Test
        void whenAgentIsUpdatedInDBEntityChangedMethodShouldRefreshAgentInstanceCacheWithUpdatedAgent() {
            AgentInstance agentInstanceBeforeUpdate = AgentInstanceMother.pending();
            Agent agentBeforeUpdate = agentInstanceBeforeUpdate.getAgent();
            Agent agentAfterUpdate = AgentMother.approvedAgent();
            agentBeforeUpdate.setUuid(agentAfterUpdate.getUuid());

            AgentChangeListener listener = mock(AgentChangeListener.class);
            agentService.registerAgentChangeListeners(listener);

            when(agentInstances.findAgent(agentBeforeUpdate.getUuid())).thenReturn(agentInstanceBeforeUpdate);
            agentService.entityChanged(agentAfterUpdate);
            assertThat(agentInstanceBeforeUpdate.getAgent(), is(agentAfterUpdate));
            verify(listener).agentChanged(agentAfterUpdate);
        }

        @Test
        void whenAgentIsCreatedInDBEntityChangedMethodShouldAddNewlyCreatedAgentToCache() {
            Agent agentAfterUpdate = AgentMother.approvedAgent();
            String uuid = agentAfterUpdate.getUuid();
            when(agentInstances.findAgent(uuid)).thenReturn(new NullAgentInstance(uuid));

            agentService.entityChanged(agentAfterUpdate);

            verify(agentInstances).add(any(AgentInstance.class));
        }

        @Test
        void WhenMultipleAgentsAreUpdatedInDBBulkEntitiesChangedMethodShouldCallEntityChangedMethodForEachUpdatedAgent() {
            Agents listOf2UpdatedAgents = createTwoAgentsAndAddItToListOfAgents();

            AgentService agentServiceSpy = Mockito.spy(agentService);
            doNothing().when(agentServiceSpy).entityChanged(nullable(Agent.class));

            agentServiceSpy.bulkEntitiesChanged(listOf2UpdatedAgents);

            listOf2UpdatedAgents.forEach(agent -> verify(agentServiceSpy).entityChanged(agent));
        }

        @Test
        void WhenMultipleAgentsAreDeletedInDBBulkEntitiesDeletedMethodShouldCallEntityDeletedMethodForEachDeletedAgent() {
            AgentService agentServiceSpy = Mockito.spy(agentService);
            doNothing().when(agentServiceSpy).entityDeleted(anyString());

            List<String> deletedUUIDs = asList("uuid1", "uuid2");
            agentServiceSpy.bulkEntitiesDeleted(deletedUUIDs);

            deletedUUIDs.forEach(uuid -> verify(agentServiceSpy).entityDeleted(uuid));
        }

        private Agents createTwoAgentsAndAddItToListOfAgents() {
            Agent updatedAgent1 = AgentMother.approvedAgent();
            Agent updatedAgent2 = AgentMother.elasticAgent();

            Agents updatedAgents = new Agents();
            updatedAgents.add(updatedAgent1);
            updatedAgents.add(updatedAgent2);

            return updatedAgents;
        }
    }

    @Nested
    class FindAgentByUUID {
        @Test
        void shouldFindAgentByUUID() {
            AgentInstance agentInstance = AgentInstanceMother.building();
            String uuidToUse = agentInstance.getUuid();

            when(agentInstances.findAgent(uuidToUse)).thenReturn(agentInstance);
            Agent agent = agentService.findAgentByUUID(uuidToUse);

            assertThat(agent, is(agentInstance.getAgent()));
        }

        @Test
        void findAgentByUUIDShouldNotReturnNullIfThereIsNoAgentMatchingUUIDInTheCache() {
            Agent fromDB = AgentMother.remoteAgent();
            AgentInstance agentInstance = AgentInstanceMother.nullInstance();
            String uuidToUse = agentInstance.getUuid();

            when(agentInstances.findAgent(uuidToUse)).thenReturn(agentInstance);
            when(agentDao.fetchAgentFromDBByUUIDIncludingDeleted(uuidToUse)).thenReturn(fromDB);
            Agent agent = agentService.findAgentByUUID(uuidToUse);

            assertThat(agent, is(fromDB));
        }

        @Test
        void shouldReturnNullIfTheGivenUuidDoesNotExistInCacheNorInDB() {
            String uuidToUse = "uuid";
            when(agentInstances.findAgent(uuidToUse)).thenReturn(nullInstance());
            when(agentDao.getAgentByUUIDFromCacheOrDB(uuidToUse)).thenReturn(null);
            Agent agent = agentService.findAgentByUUID(uuidToUse);

            assertThat(agent, is(nullValue()));
        }

        @Test
        void shouldReturnNullIfUUIDIsNotProvided() {
            assertThat(agentService.findAgentByUUID(null), is(nullValue()));
            assertThat(agentService.findAgentByUUID(" "), is(nullValue()));
        }
    }

    @Nested
    class GetAgent {
        @Test
        void shouldGetAgentByUUID() {
            String uuid = "uuid";

            AgentInstance mockAgentInstance = mock(AgentInstance.class);
            Agent mockAgent = mock(Agent.class);

            when(agentInstances.findAgent(uuid)).thenReturn(mockAgentInstance);
            when(mockAgentInstance.getAgent()).thenReturn(mockAgent);

            Agent foundAgent = agentService.getAgentByUUID(uuid);
            assertThat(foundAgent, is(mockAgent));
        }

        @Test
        void getAgentByUUIDShouldReturnNullAgentIfAgentIsNotFound() {
            String nonExistingUUID = "non-existing-nonExistingUUID";

            NullAgentInstance nullAgentInstance = new NullAgentInstance(nonExistingUUID);
            when(agentInstances.findAgent(nonExistingUUID)).thenReturn(nullAgentInstance);

            Agent foundAgent = agentService.getAgentByUUID(nonExistingUUID);
            assertThat(foundAgent instanceof NullAgent, is(true));
        }

        @Test
        void shouldGetAllRegisteredAgentUUIDs() {
            AgentInstance agentInstance1 = AgentInstanceMother.idle();
            AgentInstance agentInstance2 = AgentInstanceMother.building();
            AgentInstance agentInstance3 = AgentInstanceMother.pending();

            List<AgentInstance> agentInstanceList = asList(agentInstance1, agentInstance2, agentInstance3);
            when(agentInstances.spliterator()).thenReturn(agentInstanceList.spliterator());

            List<String> allAgentUUIDs = agentService.getAllRegisteredAgentUUIDs();
            assertThat(allAgentUUIDs.size(), is(2));
        }

        @Test
        void getAllRegisteredAgentUUIDsShouldReturnEmptyListOfUUIDsIfThereAreNoAgentInstancesInCache() {
            when(agentInstances.spliterator()).thenReturn(new ArrayList<AgentInstance>().spliterator());
            List<String> allAgentUUIDs = agentService.getAllRegisteredAgentUUIDs();
            assertTrue(allAgentUUIDs.isEmpty());
        }

        @Test
        void getAllRegisteredAgentUUIDsShouldReturnEmptyListOfUUIDsIfThereAreNoRegisteredAgentInstancesInCache() {
            AgentInstance notRegisteredAgentInstance = AgentInstanceMother.pending();
            when(agentInstances.spliterator()).thenReturn(singletonList(notRegisteredAgentInstance).spliterator());
            List<String> allAgentUUIDs = agentService.getAllRegisteredAgentUUIDs();
            assertTrue(allAgentUUIDs.isEmpty());
        }
    }

    @Nested
    class ListOfResourcesAcrossAgents {
        @Test
        void shouldReturnDistinctListOfResourcesFromAllAgents() {
            AgentInstance agentInstance = building();
            agentInstance.getAgent().setResources("a,b,c");

            AgentInstance agentInstance1 = building();
            agentInstance1.getAgent().setResources("d,e,a");

            when(agentInstances.spliterator()).thenReturn(asList(agentInstance, agentInstance1).spliterator());

            assertEquals(asList("a", "b", "c", "d", "e"), agentService.getListOfResourcesAcrossAgents());
        }

        @Test
        void shouldNotContainEmptyStringInReturnedListOfResourcesFromAllAgentsWhenSomeAgentsHaveNoResources() {
            AgentInstance agentInstance = building();
            agentInstance.getAgent().setResources("a,b,c");

            AgentInstance agentInstance1 = AgentInstanceMother.idle();
            agentInstance1.getAgent().setResources(" ");

            AgentInstance agentInstance2 = AgentInstanceMother.pending();
            agentInstance2.getAgent().setResources(null);

            when(agentInstances.spliterator()).thenReturn(asList(agentInstance, agentInstance1, agentInstance2).spliterator());

            assertEquals(asList("a", "b", "c"), agentService.getListOfResourcesAcrossAgents());
        }
    }

    @Nested
    class FilterAgentsViewModel {
        @Test
        void filterShouldReturnAgentsViewModelForSpecifiedUUIDs() {
            SystemEnvironment sysEnv = new SystemEnvironment();

            Agent agent1 = new Agent("uuid-1", "host-1", "192.168.1.2");
            AgentRuntimeInfo runtimeInfo1 = fromServer(agent1, true, "/foo/bar", 100l, "linux");
            AgentInstance instance1 = createFromLiveAgent(runtimeInfo1, sysEnv, null);

            Agent agent3 = new Agent("uuid-3", "host-3", "192.168.1.4");
            AgentRuntimeInfo runtimeInfo3 = fromServer(agent3, true, "/baz/quux", 300l, "linux");
            AgentInstance instance3 = createFromLiveAgent(runtimeInfo3, sysEnv, null);

            List<String> uuids = asList("uuid-1", "uuid-3");
            when(agentInstances.filter(uuids)).thenReturn(asList(instance1, instance3));

            AgentsViewModel agentsViewModel = agentService.filterAgentsViewModel(uuids);
            AgentViewModel view1 = new AgentViewModel(instance1);
            AgentViewModel view2 = new AgentViewModel(instance3);

            assertThat(agentsViewModel, is(new AgentsViewModel(view1, view2)));
            verify(agentInstances).filter(uuids);
        }

        @Test
        void filterShouldEmptyAgentsViewModelForNullOrEmptyListOfUUIDs() {
            AgentsViewModel agentsViewModel = agentService.filterAgentsViewModel(emptyList());
            assertThat(agentsViewModel, is(emptyList()));

            agentsViewModel = agentService.filterAgentsViewModel(null);
            assertThat(agentsViewModel, is(emptyList()));
        }
    }

    @Test
    void shouldCreateAgentUsernameUsingSpecifiedInput() {
        String uuid = "uuid1";
        String ip = "127.0.0.1";
        String hostNameForDisplay = "localhost";

        Username username = agentService.createAgentUsername(uuid, ip, hostNameForDisplay);
        assertThat(username.getDisplayName(), is("agent_uuid1_127.0.0.1_localhost"));
    }

    @Test
    void notifyJobCancelledEventShouldCallUpdateAgentAboutCancelledBuildOnItsAgentInstances() {
        String uuid = "uuid";
        doNothing().when(agentInstances).updateAgentAboutCancelledBuild(uuid, true);

        agentService.notifyJobCancelledEvent(uuid);
        verify(agentInstances).updateAgentAboutCancelledBuild(uuid, true);
    }

    @Test
    void buildingMethodShouldDelegateToBuildingMethodOfAgentInstances() {
        String uuid = "uuid";
        AgentBuildingInfo mockAgentBuildingInfo = mock(AgentBuildingInfo.class);
        doNothing().when(agentInstances).building(uuid, mockAgentBuildingInfo);

        agentService.building(uuid, mockAgentBuildingInfo);
        verify(agentInstances).building(uuid, mockAgentBuildingInfo);
    }

    @Nested
    class DeleteAgentsWithoutValidations {
        @Test
        void shouldDeleteAgentsWithoutAnyValidation() {
            List<String> uuids = asList("a1", "a2");

            agentService.deleteAgentsWithoutValidations(uuids);

            verify(agentDao).bulkSoftDelete(uuids);
        }

        @Test
        void shouldDoNothingIfEmptyListIsPassed() {
            agentService.deleteAgentsWithoutValidations(emptyStrList);

            verifyZeroInteractions(agentDao);
        }
    }

    @Nested
    class refresh {
        @Test
        void shouldAddWarningIfAgentIsStuckInCancel() {
            AgentInstance agentInstance = mock(AgentInstance.class);

            when(agentInstances.agentsStuckInCancel()).thenReturn(singletonList(agentInstance));
            when(agentInstance.cancelledAt()).thenReturn(new Date());
            when(agentInstance.getHostname()).thenReturn("test_agent");

            agentService.refresh();

            ArgumentCaptor<ServerHealthState> argument = ArgumentCaptor.forClass(ServerHealthState.class);

            verify(serverHealthService).update(argument.capture());
            ServerHealthState serverHealthState = argument.getValue();
            assertThat(serverHealthState.getMessage(), is("Agent `test_agent` is stuck in cancel."));
            assertThat(serverHealthState.getLogLevel(), is(HealthStateLevel.WARNING));
        }
    }

    @Nested
    class killAllRunningTasksOnAgent {
        @Test
        void shouldInstructAgentToKillAllRunningTasks() throws InvalidAgentInstructionException {
            AgentInstance agentInstance = mock(AgentInstance.class);

            when(agentInstances.findAgent("agent_uuid")).thenReturn(agentInstance);

            agentService.killAllRunningTasksOnAgent("agent_uuid");

            verify(agentInstance).killRunningTasks();
        }

        @Test
        void shouldErrorOutIfAgentForAGivenUUIDDoesNotExist() {
            when(agentInstances.findAgent("agent_uuid")).thenReturn(new NullAgentInstance("agent_uuid"));

            assertThrows(RecordNotFoundException.class, () -> agentService.killAllRunningTasksOnAgent("agent_uuid"));
        }
    }
}
