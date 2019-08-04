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

import ch.qos.logback.classic.Level;
import com.google.common.collect.Ordering;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.listener.AgentChangeListener;
import com.thoughtworks.go.listener.AgentChangedEvent;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.security.Registration;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.AgentDao;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.ui.AgentViewModel;
import com.thoughtworks.go.server.ui.AgentsViewModel;
import com.thoughtworks.go.server.util.UuidGenerator;
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
import static com.thoughtworks.go.domain.AgentInstance.FilterBy.*;
import static com.thoughtworks.go.domain.AgentInstance.createFromLiveAgent;
import static com.thoughtworks.go.domain.AgentRuntimeStatus.Idle;
import static com.thoughtworks.go.domain.AgentStatus.fromConfig;
import static com.thoughtworks.go.helper.AgentInstanceMother.*;
import static com.thoughtworks.go.security.Registration.createNullPrivateKeyEntry;
import static com.thoughtworks.go.server.service.AgentRuntimeInfo.fromServer;
import static com.thoughtworks.go.serverhealth.HealthStateScope.forAgent;
import static com.thoughtworks.go.serverhealth.HealthStateType.duplicateAgent;
import static com.thoughtworks.go.serverhealth.ServerHealthState.warning;
import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static com.thoughtworks.go.util.TriState.TRUE;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class AgentServiceTest {
    private AgentService agentService;
    private AgentInstances agentInstances;
    private AgentDao agentDao;
    private AgentIdentifier agentIdentifier;
    private UuidGenerator uuidGenerator;
    private ServerHealthService serverHealthService;
    private Agent agent;
    private GoConfigService goConfigService;
    private SecurityService securityService;

    private List<String> emptyStrList = Collections.emptyList();
    private EnvironmentsConfig emptyEnvsConfig = new EnvironmentsConfig();

    @BeforeEach
    void setUp() {
        agentInstances = mock(AgentInstances.class);
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

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.updateAgentsAssociationWithSpecifiedEnv(testEnv, asList(uuid, "uuid1"), result);

            ArgumentCaptor<List<Agent>> argument = ArgumentCaptor.forClass(List.class);

            List<Agent> agents = asList(agentConfigForUUID1, agent);

            verify(agentDao).bulkUpdateAttributes(argument.capture(), anyMap(), eq(TriState.UNSET));
            assertEquals(agents.size(), argument.getValue().size());
            assertTrue(argument.getValue().contains(agents.get(0)));
            assertTrue(argument.getValue().contains(agents.get(1)));

            assertTrue(result.isSuccessful());
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, uuid1]."));
        }

        @Test
        void shouldUpdateAgentsAssociationWithSpecifiedEnv() {
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

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.updateAgentsAssociationWithSpecifiedEnv(testEnv, asList(uuid, "uuid2"), result);

            verify(agentDao).bulkUpdateAttributes(anyList(), anyMap(), eq(TriState.UNSET));
            assertTrue(result.isSuccessful());
            assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));
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

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.updateAgentsAssociationWithSpecifiedEnv(testEnv, emptyList(), result);

            List<Agent> agents = asList(agentConfigForUUID1, agent);

            ArgumentCaptor<List<Agent>> argument = ArgumentCaptor.forClass(List.class);

            verify(agentDao).bulkUpdateAttributes(argument.capture(), anyMap(), eq(TriState.UNSET));
            assertEquals(agents.size(), argument.getValue().size());
            assertTrue(argument.getValue().contains(agents.get(0)));
            assertTrue(argument.getValue().contains(agents.get(1)));

            assertTrue(result.isSuccessful());
            assertThat(result.message(), is("Updated agent(s) with uuid(s): []."));
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

            HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
            agentService.updateAgentsAssociationWithSpecifiedEnv(testEnv, emptyStrList, result);

            verifyNoMoreInteractions(agentDao);
            assertTrue(result.isSuccessful());
            assertThat(result.message(), is(nullValue()));
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

        @Test
        void shouldNotAddEnvsWhichAreAssociatedWithTheAgentFromConfigRepo() {
            String uuid = "uuid";
            HttpOperationResult result = new HttpOperationResult();
            Username username = new Username(new CaseInsensitiveString("test"));

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(agentDao.getAgentByUUIDFromCacheOrDB(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(mock(AgentInstance.class));

            EnvironmentsConfig environmentConfigs = new EnvironmentsConfig();
            EnvironmentConfig repoEnvConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("config-repo-env"));
            repoEnvConfig.setOrigins(new RepoConfigOrigin());
            repoEnvConfig.addAgent(uuid);
            environmentConfigs.add(repoEnvConfig);
            environmentConfigs.add(new BasicEnvironmentConfig(new CaseInsensitiveString("non-config-repo-env")));
            AgentInstance agentInstance = agentService.updateAgentAttributes(uuid, null, null, environmentConfigs, TRUE, result);

            verify(agentDao).saveOrUpdate(any(Agent.class));
            assertTrue(result.isSuccess());
            assertThat(result.message(), is("Updated agent with uuid uuid."));

            assertThat(agentInstance.getAgent().getEnvironments(), is("non-config-repo-env"));
        }
    }

    @Nested
    class UpdateRuntimeInfo {
        @Test
        void shouldUpdateRuntimeInfo() {
            String cookie = "cookie";
            AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(agentIdentifier, Idle, currentWorkingDirectory(), cookie, false);
            when(agentDao.cookieFor(runtimeInfo.getIdentifier())).thenReturn(cookie);
            agentService.updateRuntimeInfo(runtimeInfo);
            verify(agentInstances).updateAgentRuntimeInfo(runtimeInfo);
        }

        @Test
        void shouldThrowExceptionWhenAgentWithNoCookieTriesToUpdateRuntimeInfo() {
            AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(agentIdentifier, Idle, currentWorkingDirectory(), null, false);

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
            AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(agentIdentifier, Idle, currentWorkingDirectory(), null, false);
            runtimeInfo.setCookie("invalid_cookie");
            AgentInstance original = createFromLiveAgent(new AgentRuntimeInfo(agentIdentifier, Idle, currentWorkingDirectory(), null, false), new SystemEnvironment(), null);

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
    class AgentUpdateAttributes {
        @Nested
        class BulkAgentUpdate {
            @Test
            void shouldBulkUpdateAgentsAttributes() {
                AgentInstance agentInstance1 = mock(AgentInstance.class);
                AgentInstance agentInstance2 = mock(AgentInstance.class);
                HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
                Username username = new Username(new CaseInsensitiveString("test"));

                when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
                when(goConfigService.getEnvironments()).thenReturn(new EnvironmentsConfig());
                when(agentInstances.findAgent("uuid1")).thenReturn(agentInstance1);
                when(agentInstances.findAgent("uuid2")).thenReturn(agentInstance2);

                agentService.bulkUpdateAgentAttributes(asList("uuid1", "uuid2"), asList("R1", "R2"), emptyStrList, createEnvironmentsConfigWith("test", "prod"), emptyStrList, TRUE, result);

                verify(agentDao).bulkUpdateAttributes(anyList(), anyMap(), eq(TRUE));
                assertThat(result.isSuccessful(), is(true));
                assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid1, uuid2]."));
            }

            @Test
            void shouldBulkEnableAgents() {
                Username username = new Username(new CaseInsensitiveString("test"));
                AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromAgent(agentIdentifier, AgentRuntimeStatus.Unknown, "cookie", false);
                AgentInstance pending = createFromLiveAgent(agentRuntimeInfo, new SystemEnvironment(), null);

                Agent agent = new Agent("UUID2", "remote-host", "50.40.30.20");
                agent.disable();
                AgentInstance fromConfigFile = AgentInstance.createFromAgent(agent, new SystemEnvironment(), null);

                when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
                when(goConfigService.getEnvironments()).thenReturn(new EnvironmentsConfig());
                when(agentInstances.findAgent("uuid")).thenReturn(pending);
                when(agentInstances.findAgent("UUID2")).thenReturn(fromConfigFile);

                List<String> uuids = asList(pending.getUuid(), fromConfigFile.getUuid());
                HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
                agentService.bulkUpdateAgentAttributes(uuids, emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TRUE, result);

                verify(agentDao).bulkUpdateAttributes(anyList(), anyMap(), eq(TRUE));
                assertThat(result.isSuccessful(), is(true));
                assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, UUID2]."));
            }

            @Test
            void shouldNotDoAnythingIfNoOperationsArePerformed() {
                HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
                agentService.bulkUpdateAgentAttributes(singletonList("uuid"), emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.UNSET, result);

                verifyZeroInteractions(agentDao);
                assertEquals(400, result.httpCode());
                assertEquals("No Operation performed on agents.", result.message());
            }

            @Test
            void shouldThrow400IfAgentDoesNotExist() {
                HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
                List<String> uuids = singletonList("uuid1");

                when(agentInstances.filterBy(uuids, Null)).thenReturn(uuids);

                agentService.bulkUpdateAgentAttributes(uuids, singletonList("resource"), emptyStrList, emptyEnvsConfig, emptyStrList, TriState.UNSET, result);

                verifyZeroInteractions(agentDao);
                assertEquals(400, result.httpCode());
                assertEquals("Agents with uuids 'uuid1' were not found!", result.message());
            }

            @Test
            void shouldThrow400IfResourcesForAnElasticAgentAreUpdated() {
                HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
                List<String> uuids = singletonList("uuid1");

                when(agentInstances.filterBy(uuids, Elastic)).thenReturn(uuids);

                agentService.bulkUpdateAgentAttributes(uuids, singletonList("resource"), emptyStrList, emptyEnvsConfig, emptyStrList, TriState.UNSET, result);

                verifyZeroInteractions(agentDao);
                assertEquals(400, result.httpCode());
                assertEquals("Resources on elastic agents with uuids [uuid1] can not be updated.", result.message());
            }

            @Test
            void shouldThrow422IfResourceNamesAreInvalid() {
                HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
                List<String> uuids = singletonList("uuid1");

                agentService.bulkUpdateAgentAttributes(uuids, singletonList("foo%"), emptyStrList, emptyEnvsConfig, emptyStrList, TriState.UNSET, result);

                verifyZeroInteractions(agentDao);
                assertEquals(422, result.httpCode());
                assertEquals("Validations failed for bulk update of agents. Error(s): {resources=[Resource name 'foo%' is not valid. Valid names much match '^[-\\w\\s|.]*$']}", result.message());
            }

            @Test
            void shouldThrow400IfOperationsArePerformedOnAPendingAgentWithoutUpdatingTheState() {
                HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
                List<String> uuids = singletonList("uuid1");

                when(agentInstances.filterBy(uuids, Pending)).thenReturn(uuids);

                agentService.bulkUpdateAgentAttributes(uuids, singletonList("resource"), emptyStrList, emptyEnvsConfig, emptyStrList, TriState.UNSET, result);

                verifyZeroInteractions(agentDao);
                assertEquals(400, result.httpCode());
                assertEquals("Pending agents [uuid1] must be explicitly enabled or disabled when performing any operations on them.", result.message());
            }

            @Test
            void shouldNotAddEnvsWhichAreAssociatedWithTheAgentFromConfigRepo() {
                HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
                String uuid = "uuid";
                List<String> uuids = singletonList(uuid);
                List<Agent> agents = singletonList(agent);
                AgentInstance agentInstance = mock(AgentInstance.class);

                when(agentDao.getAgentsByUUIDs(uuids)).thenReturn(agents);
                when(agentInstances.filterPendingAgents(uuids)).thenReturn(emptyList());
                when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
                when(agentInstance.getStatus()).thenReturn(fromConfig(AgentConfigStatus.Pending));

                EnvironmentsConfig environmentConfigs = new EnvironmentsConfig();
                BasicEnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("config-repo-env"));
                environmentConfig.setOrigins(new RepoConfigOrigin());
                environmentConfigs.add(environmentConfig);

                agentService.bulkUpdateAgentAttributes(uuids, emptyStrList, emptyStrList, environmentConfigs, emptyStrList, TRUE, result);

                verify(agentDao).getAgentsByUUIDs(uuids);
                verify(agentDao).bulkUpdateAttributes(eq(agents), anyMap(), eq(TRUE));
                assertTrue(result.isSuccessful());
                assertEquals("Updated agent(s) with uuid(s): [uuid].", result.message());
            }
        }

        @Test
        void shouldUpdateTheAgentAttributes() {
            String uuid = "uuid";
            HttpOperationResult result = new HttpOperationResult();
            Username username = new Username(new CaseInsensitiveString("test"));

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(agentDao.getAgentByUUIDFromCacheOrDB(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(mock(AgentInstance.class));

            AgentInstance agentInstance = agentService.updateAgentAttributes(uuid, "new-hostname", "resource1,resource2", createEnvironmentsConfigWith("env1", "env2"), TRUE, result);

            verify(agentDao).saveOrUpdate(any(Agent.class));
            assertTrue(result.isSuccess());
            assertThat(result.message(), is("Updated agent with uuid uuid."));

            Agent agentFromService = agentInstance.getAgent();

            assertThat(agentFromService.getHostname(), is("new-hostname"));
            assertThat(agentFromService.getResources(), is("resource1,resource2"));
            assertThat(agentFromService.getEnvironments(), is("env1,env2"));
            assertFalse(agentFromService.isDisabled());
        }

        @Test
        void shouldThrow400IfNoOperationToPerform() {
            String uuid = "uuid";
            HttpOperationResult result = new HttpOperationResult();

            when(agentDao.getAgentByUUIDFromCacheOrDB(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(mock(AgentInstance.class));

            agentService.updateAgentAttributes(uuid, null, null, null, TriState.UNSET, result);

            verify(agentDao, times(0)).saveOrUpdate(any(Agent.class));
            assertThat(result.httpCode(), is(400));
            assertThat(result.message(), is("No Operation performed on agent."));
        }

        @Test
        void shouldThrow404IfAgentDoesNotExist() {
            String uuid = "non-existent-uuid";
            HttpOperationResult result = new HttpOperationResult();
            Username username = new Username(new CaseInsensitiveString("test"));
            AgentInstance agentInstance = mock(AgentInstance.class);

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(agentDao.getAgentByUUIDFromCacheOrDB(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
            when(agentInstance.isNullAgent()).thenReturn(true);
            when(agentInstance.getUuid()).thenReturn(uuid);

            agentService.updateAgentAttributes(uuid, "new-hostname", "resource1,resource2", createEnvironmentsConfigWith("env1", "env2"), TRUE, result);

            verify(agentDao, times(0)).saveOrUpdate(any(Agent.class));
            assertThat(result.httpCode(), is(404));
            assertThat(result.message(), is("Agent 'non-existent-uuid' not found."));
        }

        @Test
        void shouldThrow400IfEnvironmentsSpecifiedAsBlank() {
            String uuid = "uuid";
            HttpOperationResult result = new HttpOperationResult();
            Username username = new Username(new CaseInsensitiveString("test"));
            AgentInstance agentInstance = mock(AgentInstance.class);

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(agentDao.getAgentByUUIDFromCacheOrDB(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
            when(agentInstance.isNullAgent()).thenReturn(false);
            when(agentInstance.getUuid()).thenReturn(uuid);

            agentService.updateAgentAttributes(uuid, "new-hostname", "resource1,resource2", emptyEnvsConfig, TRUE, result);

            verify(agentDao, times(0)).saveOrUpdate(any(Agent.class));
            assertThat(result.httpCode(), is(400));
            assertThat(result.message(), is("Environments are specified but they are blank."));
        }

        @Test
        void shouldThrow400IfResourcesSpecifiedAsBlank() {
            String uuid = "uuid";
            HttpOperationResult result = new HttpOperationResult();
            Username username = new Username(new CaseInsensitiveString("test"));
            AgentInstance agentInstance = mock(AgentInstance.class);

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(agentDao.getAgentByUUIDFromCacheOrDB(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
            when(agentInstance.isNullAgent()).thenReturn(false);
            when(agentInstance.getUuid()).thenReturn(uuid);

            agentService.updateAgentAttributes(uuid, "new-hostname", "", createEnvironmentsConfigWith("env1"), TRUE, result);

            verify(agentDao, times(0)).saveOrUpdate(any(Agent.class));
            assertThat(result.httpCode(), is(400));
            assertThat(result.message(), is("Resources are specified but they are blank."));
        }

        @Test
        void shouldThrow400IfOperationsPerformedOnPendingAgentWithoutUpdatingTheState() {
            String uuid = "uuid";
            HttpOperationResult result = new HttpOperationResult();
            Username username = new Username(new CaseInsensitiveString("test"));
            AgentInstance agentInstance = mock(AgentInstance.class);

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(agentDao.getAgentByUUIDFromCacheOrDB(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
            when(agentInstance.isNullAgent()).thenReturn(false);
            when(agentInstance.isPending()).thenReturn(true);
            when(agentInstance.getUuid()).thenReturn(uuid);

            agentService.updateAgentAttributes(uuid, "new-hostname", "resource1", createEnvironmentsConfigWith("env1"), TriState.UNSET, result);

            verify(agentDao, times(0)).saveOrUpdate(any(Agent.class));
            assertThat(result.httpCode(), is(400));
            assertThat(result.message(), is("Pending agent [uuid] must be explicitly enabled or disabled when performing any operation on it."));
        }

        @Test
        void shouldThrow422IfResourceNamesAreInvalid() {
            String uuid = "uuid";
            HttpOperationResult result = new HttpOperationResult();
            Username username = new Username(new CaseInsensitiveString("test"));

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(agentDao.getAgentByUUIDFromCacheOrDB(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(mock(AgentInstance.class));

            AgentInstance agentInstance = agentService.updateAgentAttributes(uuid, "new-hostname", "res%^1", createEnvironmentsConfigWith("env1"), TRUE, result);

            verify(agentDao, times(0)).saveOrUpdate(any(Agent.class));
            assertThat(result.httpCode(), is(422));
            assertThat(result.message(), is("Updating agent failed."));

            Agent agent = agentInstance.getAgent();
            assertTrue(agent.hasErrors());
            assertThat(agent.errors().on(JobConfig.RESOURCES), is("Resource name 'res%^1' is not valid. Valid names much match '^[-\\w\\s|.]*$'"));
        }

        @Test
        void shouldOnlyUpdateAttributesThatAreSpecified() {
            String uuid = "uuid";
            HttpOperationResult result = new HttpOperationResult();
            Username username = new Username(new CaseInsensitiveString("test"));

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(agentDao.getAgentByUUIDFromCacheOrDB(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(mock(AgentInstance.class));

            AgentInstance agentInstance = agentService.updateAgentAttributes(uuid, null, null, createEnvironmentsConfigWith("env1", "env2"), TRUE, result);

            verify(agentDao).saveOrUpdate(any(Agent.class));
            assertTrue(result.isSuccess());
            assertThat(result.message(), is("Updated agent with uuid uuid."));

            Agent agentFromService = agentInstance.getAgent();

            assertThat(agentFromService.getHostname(), is("host"));
            assertThat(agentFromService.getResources(), is(""));
            assertThat(agentFromService.getEnvironments(), is("env1,env2"));
            assertFalse(agentFromService.isDisabled());
        }
    }

    @Nested
    class FindRegistedAgentByUUID {
        @Test
        void shouldFindRegisteredAgentByUUID() {
            AgentInstance agentInstance = AgentInstanceMother.building();
            String uuidToUse = agentInstance.getUuid();

            when(agentInstances.findAgent(uuidToUse)).thenReturn(agentInstance);
            Agent agent = agentService.findRegisteredAgentByUUID(uuidToUse);

            assertThat(agent, is(agentInstance.getAgent()));
        }

        @Test
        void findRegisterdAgentByUUIDShouldReturnNullIfThereIsNoRegisteredAgentMatchingUUID() {
            AgentInstance agentInstance = AgentInstanceMother.pending();
            String uuidToUse = agentInstance.getUuid();

            when(agentInstances.findAgent(uuidToUse)).thenReturn(agentInstance);
            Agent agent = agentService.findRegisteredAgentByUUID(uuidToUse);

            assertThat(agent, is(nullValue()));
        }
    }

    @Nested
    class GetAgent{
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
        void shouldGetAllRegisteredAgentUUIDs(){
            AgentInstance agentInstance1 = AgentInstanceMother.idle();
            AgentInstance agentInstance2 = AgentInstanceMother.building();
            AgentInstance agentInstance3 = AgentInstanceMother.pending();

            List<AgentInstance> agentInstanceList = asList(agentInstance1, agentInstance2, agentInstance3);
            when(agentInstances.values()).thenReturn(agentInstanceList);

            List<String> allAgentUUIDs = agentService.getAllRegisteredAgentUUIDs();
            assertThat(allAgentUUIDs.size(), is(2));
        }

        @Test
        void getAllRegisteredAgentUUIDsShouldReturnEmptyListOfUUIDsIfThereAreNoAgentInstancesInCache(){
            when(agentInstances.values()).thenReturn(emptyList());
            List<String> allAgentUUIDs = agentService.getAllRegisteredAgentUUIDs();
            assertTrue(allAgentUUIDs.isEmpty());
        }

        @Test
        void getAllRegisteredAgentUUIDsShouldReturnEmptyListOfUUIDsIfThereAreNoRegisteredAgentInstancesInCache(){
            AgentInstance notRegisteredAgentInstance = AgentInstanceMother.pending();
            when(agentInstances.values()).thenReturn(singletonList(notRegisteredAgentInstance));
            List<String> allAgentUUIDs = agentService.getAllRegisteredAgentUUIDs();
            assertTrue(allAgentUUIDs.isEmpty());
        }
    }

    @Nested
    class DisableAgents {
        @Test
        void shouldDoNothingIfDisableAgentsIsCalledWithEmptyListOfUUIDs() {
            agentService.disableAgents(emptyList());
            verify(agentDao, times(0)).enableOrDisableAgents(anyList(), anyBoolean());
        }

        @Test
        void shouldDisableAgentsWhenCalledWithNonEmptyListOfUUIDs() {
            List<String> uuids = asList("uuid1", "uuid2");
            agentService.disableAgents(uuids);
            verify(agentDao).enableOrDisableAgents(uuids, true);
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
                when(mockAgent.isNull()).thenReturn(true);
                when(mockAgentInstance.getAgent().getUuid()).thenReturn(uuid);

                AgentService agentService = new AgentService(new SystemEnvironment(), agentInstances,
                        agentDao, uuidGenerator, serverHealthService = mock(ServerHealthService.class), null);

                HttpOperationResult result = new HttpOperationResult();
                agentService.deleteAgents(singletonList(uuid), result);

                assertThat(result.httpCode(), is(404));
                assertThat(result.message(), is("Not Found"));
                assertThat(result.getServerHealthState().getDescription(), is(format("Agent '%s' not found", uuid)));
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

                HttpOperationResult result = new HttpOperationResult();
                agentService.deleteAgents(singletonList(uuid), result);

                assertThat(result.httpCode(), is(406));
                assertThat(result.message(), is("Failed to delete an agent, as it is not in a disabled state or is still building."));
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

                HttpOperationResult result = new HttpOperationResult();
                agentService.deleteAgents(asList(uuid1, uuid2), result);

                assertThat(result.httpCode(), is(406));
                assertThat(result.message(), is("Could not delete any agents, as one or more agents might not be disabled or are still building."));
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

                HttpOperationResult result = new HttpOperationResult();
                agentService.deleteAgents(singletonList(uuid), result);

                assertThat(result.httpCode(), is(200));
                assertThat(result.message(), is("Deleted 1 agent(s)."));
                verify(agentDao).bulkSoftDelete(singletonList(uuid));
            }

            @Test
            void shouldReturn200WhenDeleteAgentsIsCalledWithNullAsListOfUUIDs() {
                Username username = new Username(new CaseInsensitiveString("test"));

                when(securityService.hasOperatePermissionForAgents(username)).thenReturn(true);

                AgentService agentService = new AgentService(new SystemEnvironment(), agentInstances,
                        agentDao, uuidGenerator, serverHealthService = mock(ServerHealthService.class), null);

                HttpOperationResult result = new HttpOperationResult();
                agentService.deleteAgents(null, result);

                assertThat(result.httpCode(), is(200));
                assertThat(result.message(), is("Deleted 0 agent(s)."));
            }

            @Test
            void shouldReturn200WhenDeleteAgentsIsCalledMultipleDisabledAgents() {
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

                AgentService agentService = new AgentService(new SystemEnvironment(), agentInstances,
                        agentDao, uuidGenerator, serverHealthService = mock(ServerHealthService.class), null);

                HttpOperationResult result = new HttpOperationResult();
                agentService.deleteAgents(asList(uuid1, uuid2), result);

                assertThat(result.httpCode(), is(200));
                assertThat(result.message(), is("Deleted 2 agent(s)."));
            }
        }
    }

    @Nested
    class IsRegistered {
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
    }

    @Nested
    class ListOfResourcesAcrossAgents {
        @Test
        void shouldReturnDistinctListOfResourcesFromAllAgents() {
            AgentInstance agentInstance = building();
            agentInstance.getAgent().setResources("a,b,c");

            AgentInstance agentInstance1 = building();
            agentInstance1.getAgent().setResources("d,e,a");

            when(agentInstances.values()).thenReturn(asList(agentInstance, agentInstance1));

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

            when(agentInstances.values()).thenReturn(asList(agentInstance, agentInstance1, agentInstance2));

            assertEquals(asList("a", "b", "c"), agentService.getListOfResourcesAcrossAgents());
        }
    }

    @Nested
    class FilterAgentsViewModel {
        @Test
        void filterShouldReturnAgentsViewModelForSpecifiedUUIDs() {
            SystemEnvironment sysEnv = new SystemEnvironment();

            Agent agent1 = new Agent("uuid-1", "host-1", "192.168.1.2");
            AgentRuntimeInfo runtimeInfo1 = fromServer(agent1, true, "/foo/bar", 100l, "linux", false);
            AgentInstance instance1 = createFromLiveAgent(runtimeInfo1, sysEnv, null);

            Agent agent3 = new Agent("uuid-3", "host-3", "192.168.1.4");
            AgentRuntimeInfo runtimeInfo3 = fromServer(agent3, true, "/baz/quux", 300l, "linux", false);
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
    class RequestRegistration{
        @Test
        void requestRegistrationShouldReturnNullPrivateKeyRegistrationWhenCalledWithPendingAgent(){
            AgentRuntimeInfo runtimeInfo = fromServer(pending().getAgent(), false, "sandbox",0l, "linux", false);
            AgentInstance agentInstance = mock(AgentInstance.class);
            Agent agent = mock(Agent.class);

            when(agentInstances.register(runtimeInfo)).thenReturn(agentInstance);
            Registration registrationWithNullPrivateKey = createNullPrivateKeyEntry();
            when(agentInstance.assignCertification()).thenReturn(registrationWithNullPrivateKey);
            when(agentInstance.getAgent()).thenReturn(agent);

            Registration registration = agentService.requestRegistration(runtimeInfo);
            assertThat(registration, is(registrationWithNullPrivateKey));
            verifyZeroInteractions(agentDao);
            verify(agent, never()).getCookie();
            verify(agent, never()).setCookie(anyString());
            verify(agent, never()).validate();
            verify(agent, never()).hasErrors();
        }

        @Test
        void requestRegistrationShouldReturnValidRegistrationWhenCalledWithRegisteredAgent(){
            AgentRuntimeInfo runtimeInfo = fromServer(building().getAgent(), false, "sandbox",0l, "linux", false);
            AgentInstance agentInstance = mock(AgentInstance.class);
            Agent agent = mock(Agent.class);

            when(agentInstances.register(runtimeInfo)).thenReturn(agentInstance);
            Registration mockRegistration = mock(Registration.class);
            when(agentInstance.assignCertification()).thenReturn(mockRegistration);
            when(agentInstance.getAgent()).thenReturn(agent);
            when(agentInstance.isRegistered()).thenReturn(true);

            String cookie = "cookie";
            when(uuidGenerator.randomUuid()).thenReturn(cookie);

            Registration requestedRegistration = agentService.requestRegistration(runtimeInfo);
            assertThat(requestedRegistration, is(mockRegistration));

            verify(agentDao, only()).saveOrUpdate(agent);
            verify(agent, times(1)).getCookie();
            verify(agent, times(1)).setCookie(cookie);
            verify(agent, times(1)).validate();
            verify(agent, times(2)).hasErrors();
        }

        @Test
        void requestRegistrationShouldBombIfAgentToBeRegisteredHasValidationErrors(){
            AgentInstance mockAgentInstance = mock(AgentInstance.class);
            Agent mockAgent = mock(Agent.class);
            Registration mockRegistration = mock(Registration.class);

            AgentRuntimeInfo runtimeInfo = fromServer(building().getAgent(), false, "sandbox",0l, "linux", false);

            when(agentInstances.register(runtimeInfo)).thenReturn(mockAgentInstance);
            when(mockAgentInstance.assignCertification()).thenReturn(mockRegistration);
            when(mockAgentInstance.getAgent()).thenReturn(mockAgent);
            when(mockAgentInstance.isRegistered()).thenReturn(true);

            when(mockAgent.hasErrors()).thenReturn(true);

            String cookie = "cookie";
            when(uuidGenerator.randomUuid()).thenReturn(cookie);

            assertThrows(GoConfigInvalidException.class, () -> agentService.requestRegistration(runtimeInfo));
        }
    }

    @Nested
    class AgentChangeListenerMethods{
        @Test
        void whenAgentIsUpdatedInDBEntityChangedMethodShouldRefreshAgentInstanceCacheWithUpdatedAgent(){
            AgentInstance agentInstanceBeforeUpdate = AgentInstanceMother.pending();
            Agent agentBeforeUpdate = agentInstanceBeforeUpdate.getAgent();
            Agent agentAfterUpdate = AgentMother.approvedAgent();
            agentBeforeUpdate.setUuid(agentAfterUpdate.getUuid());

            AgentChangeListener listener = mock(AgentChangeListener.class);
            agentService.registerAgentChangeListeners(listener);

            when(agentInstances.findAgent(agentBeforeUpdate.getUuid())).thenReturn(agentInstanceBeforeUpdate);
            agentService.entityChanged(agentAfterUpdate);
            assertThat(agentInstanceBeforeUpdate.getAgent(), is(agentAfterUpdate));
            AgentChangedEvent agentChangedEvent = new AgentChangedEvent(agentBeforeUpdate, agentAfterUpdate);
            verify(listener).agentChanged(agentChangedEvent);
        }

        @Test
        void whenAgentIsCreatedInDBEntityChangedMethodShouldAddNewlyCreatedAgentToCache(){
            Agent agentAfterUpdate = AgentMother.approvedAgent();
            String uuid = agentAfterUpdate.getUuid();
            when(agentInstances.findAgent(uuid)).thenReturn(new NullAgentInstance(uuid));

            agentService.entityChanged(agentAfterUpdate);

            verify(agentInstances).add(any(AgentInstance.class));
        }

        @Test
        void WhenMultipleAgentsAreUpdatedInDBBulkEntitiesChangedMethodShouldCallEntityChangedMethodForEachUpdatedAgent(){
            Agents listOf2UpdatedAgents = createTwoAgentsAndAddItToListOfAgents();

            AgentService agentServiceSpy = Mockito.spy(agentService);
            doNothing().when(agentServiceSpy).entityChanged(nullable(Agent.class));

            agentServiceSpy.bulkEntitiesChanged(listOf2UpdatedAgents);

            listOf2UpdatedAgents.forEach(agent -> verify(agentServiceSpy).entityChanged(agent));
        }

        @Test
        void WhenMultipleAgentsAreDeletedInDBBulkEntitiesDeletedMethodShouldCallEntityDeletedMethodForEachDeletedAgent(){
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
    class UpdateAgentApprovalStatus {
        @Test
        void shouldBombIfUpdateAgentApprovalStatusIsCalledWithNonExistingAgentUUID() {
            String nonExistingUUID = "some-non-existing-uuid";
            when(agentInstances.findAgent(nonExistingUUID)).thenReturn(new NullAgentInstance(nonExistingUUID));

            RuntimeException e = assertThrows(RuntimeException.class, () -> agentService.updateAgentApprovalStatus(nonExistingUUID, true));
            assertThat(e.getMessage(), is("Unable to update agent approval status; Agent [" + nonExistingUUID + "] not found."));
        }

        @Test
        void shouldBombIfUpdateAgentApprovalStatusIsCalledWithUnregisteredUUID() {
            AgentInstance mockInstance = mock(AgentInstance.class);
            String pendingUUID = "uuid1";
            when(agentInstances.findAgent(pendingUUID)).thenReturn(mockInstance);
            when(mockInstance.isRegistered()).thenReturn(false);

            RuntimeException e = assertThrows(RuntimeException.class, () -> agentService.updateAgentApprovalStatus(pendingUUID, true));
            assertThat(e.getMessage(), is("Unable to update agent approval status; Agent [" + pendingUUID + "] not found."));
        }

        @Test
        void shouldUpdateAgentApprovalStatusWhenCalledWithRegisteredUUID() {
            String registeredUUID = "registeredUUID";
            AgentInstance mockInstance = mock(AgentInstance.class);
            Agent mockAgent = mock(Agent.class);

            when(agentInstances.findAgent(registeredUUID)).thenReturn(mockInstance);
            when(mockInstance.isRegistered()).thenReturn(true);
            when(mockInstance.getAgent()).thenReturn(mockAgent);

            agentService.updateAgentApprovalStatus(registeredUUID, true);

            verify(mockAgent).setDisabled(true);
            verify(mockAgent).validate();
            verify(mockAgent).hasErrors();

            verify(agentDao).saveOrUpdate(mockAgent);
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

    @Test
    void shouldCreateAgentUsernameUsingSpecifiedInput(){
        String uuid = "uuid1";
        String ip = "127.0.0.1";
        String hostNameForDisplay = "localhost";

        Username username = agentService.createAgentUsername(uuid, ip, hostNameForDisplay);
        assertThat(username.getDisplayName(), is("agent_uuid1_127.0.0.1_localhost"));
    }

    @Test
    void notifyJobCancelledEventShouldCallUpdateAgentAboutCancelledBuildOnItsAgentInstances(){
        String uuid = "uuid";
        doNothing().when(agentInstances).updateAgentAboutCancelledBuild(uuid, true);

        agentService.notifyJobCancelledEvent(uuid);
        verify(agentInstances).updateAgentAboutCancelledBuild(uuid, true);
    }

    @Test
    void buildingMethodShouldDelegateToBuildingMethodOfAgentInstances(){
        String uuid = "uuid";
        AgentBuildingInfo mockAgentBuildingInfo = mock(AgentBuildingInfo.class);
        doNothing().when(agentInstances).building(uuid, mockAgentBuildingInfo);

        agentService.building(uuid, mockAgentBuildingInfo);
        verify(agentInstances).building(uuid, mockAgentBuildingInfo);
    }

    @Test
    void shouldDoNothingWhenRegisterAgentChangeListenerIsCalledWithNullListener(){
        Set<AgentChangeListener> setOfListeners = new HashSet<>();

        agentService.setAgentChangeListeners(setOfListeners);
        agentService.registerAgentChangeListeners(null);

        assertThat(setOfListeners.size(), is(0));
    }

    @Test
    void registerShouldSetResourcesEnvironmentsAndSaveAgentToDBWhenAgentWithCookieIsPassed(){
        String agentAutoRegisterResources = "r1,r2";
        String agentAutoRegisterEnvs = "e1,e2";

        Agent mockAgent = mock(Agent.class);
        when(mockAgent.getCookie()).thenReturn("cookie");
        when(mockAgent.hasErrors()).thenReturn(false);
        doNothing().when(mockAgent).setResources(agentAutoRegisterResources);
        doNothing().when(mockAgent).setEnvironments(agentAutoRegisterEnvs);
        doNothing().when(mockAgent).validate();

        agentService.register(mockAgent, agentAutoRegisterResources, agentAutoRegisterEnvs);

        String cookie = verify(uuidGenerator, never()).randomUuid();
        verify(mockAgent).getCookie();
        verify(mockAgent, never()).setCookie(cookie);

        verify(mockAgent).setResources(agentAutoRegisterResources);
        verify(mockAgent).setEnvironments(agentAutoRegisterEnvs);
        verify(mockAgent).validate();
        verify(mockAgent).hasErrors();

        verify(agentDao).saveOrUpdate(mockAgent);
    }

    @Test
    void registerShouldGenerateCookieSetResourcesEnvironmentsAndSaveAgentToDBWhenAgentWithoutCookieIsPassed(){
        String agentAutoRegisterResources = "r1,r2";
        String agentAutoRegisterEnvs = "e1,e2";

        Agent mockAgent = mock(Agent.class);
        when(mockAgent.getCookie()).thenReturn(null);
        when(mockAgent.hasErrors()).thenReturn(false);
        doNothing().when(mockAgent).setResources(agentAutoRegisterResources);
        doNothing().when(mockAgent).setEnvironments(agentAutoRegisterEnvs);
        doNothing().when(mockAgent).validate();

        agentService.register(mockAgent, agentAutoRegisterResources, agentAutoRegisterEnvs);

        String cookie = verify(uuidGenerator).randomUuid();
        verify(mockAgent).getCookie();
        verify(mockAgent).setCookie(cookie);

        verify(mockAgent).setResources(agentAutoRegisterResources);
        verify(mockAgent).setEnvironments(agentAutoRegisterEnvs);
        verify(mockAgent).validate();
        verify(mockAgent).hasErrors();

        verify(agentDao).saveOrUpdate(mockAgent);
    }

    @Test
    void shouldRegisterAgentChangeListener(){
        Set<AgentChangeListener> setOfListeners = new HashSet<>();
        agentService.setAgentChangeListeners(setOfListeners);

        AgentChangeListener mockListener = mock(AgentChangeListener.class);
        agentService.registerAgentChangeListeners(mockListener);

        assertThat(setOfListeners.size(), is(1));
        setOfListeners.forEach(listener -> assertThat(listener, is(mockListener)));
    }

    private EnvironmentsConfig createEnvironmentsConfigWith(String... envs) {
        EnvironmentsConfig envsConfig = new EnvironmentsConfig();
        Arrays.stream(envs).forEach(env -> envsConfig.add(new BasicEnvironmentConfig(new CaseInsensitiveString(env))));
        return envsConfig;
    }
}
