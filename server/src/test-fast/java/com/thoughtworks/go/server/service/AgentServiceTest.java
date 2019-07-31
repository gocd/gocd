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
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.listener.AgentStatusChangeListener;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.AgentDao;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.ui.AgentViewModel;
import com.thoughtworks.go.server.ui.AgentsViewModel;
import com.thoughtworks.go.server.util.UuidGenerator;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.LogFixture;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TriState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static com.thoughtworks.go.CurrentGoCDVersion.docsUrl;
import static com.thoughtworks.go.domain.AgentInstance.createFromLiveAgent;
import static com.thoughtworks.go.helper.AgentInstanceMother.building;
import static com.thoughtworks.go.helper.AgentInstanceMother.pendingInstance;
import static com.thoughtworks.go.server.service.AgentRuntimeInfo.fromServer;
import static com.thoughtworks.go.serverhealth.HealthStateScope.forAgent;
import static com.thoughtworks.go.serverhealth.HealthStateType.duplicateAgent;
import static com.thoughtworks.go.serverhealth.ServerHealthState.warning;
import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
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
    class AgentAssociationWithEnvironment{
        @Test
        void shouldAddAgentsAssociationToTheSpecifiedEnv() {
            AgentInstance agentInstance = mock(AgentInstance.class);
            Username username = new Username(new CaseInsensitiveString("test"));
            String uuid = "uuid";
            Agent agentConfigForUUID1 = mock(Agent.class);

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(agentDao.getAgentByUUID(uuid)).thenReturn(agent);
            when(agentDao.getAgentByUUID("uuid1")).thenReturn(agentConfigForUUID1);

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
            when(agentDao.getAgentByUUID(uuid)).thenReturn(agent);
            when(agentDao.getAgentByUUID("uuid1")).thenReturn(agentConfigForUUID1);
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
            when(agentDao.getAgentByUUID(uuid)).thenReturn(agent);
            when(agentDao.getAgentByUUID("uuid1")).thenReturn(agentConfigForUUID1);

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
        void shouldReturnMapOfAgentInstanceToSortedEnvironments(){
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
        void shouldReturnMapOfAgentInstanceToEmptyEnvironmentsWhenThereAreNoEnvironmentsAssociatedWithAnyAgent(){
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
            when(agentDao.getAgentByUUID(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(mock(AgentInstance.class));

            EnvironmentsConfig environmentConfigs = new EnvironmentsConfig();
            EnvironmentConfig repoEnvConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("config-repo-env"));
            repoEnvConfig.setOrigins(new RepoConfigOrigin());
            repoEnvConfig.addAgent(uuid);
            environmentConfigs.add(repoEnvConfig);
            environmentConfigs.add(new BasicEnvironmentConfig(new CaseInsensitiveString("non-config-repo-env")));
            AgentInstance agentInstance = agentService.updateAgentAttributes(uuid, null, null, environmentConfigs, TriState.TRUE, result);

            verify(agentDao).saveOrUpdate(any(Agent.class));
            assertTrue(result.isSuccess());
            assertThat(result.message(), is("Updated agent with uuid uuid."));

            assertThat(agentInstance.getAgent().getEnvironments(), is("non-config-repo-env"));
        }
    }

    @Nested
    class UpdateStatus{
        @Test
        void shouldUpdateStatus() {
            AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "pavanIsGreat", false);
            when(agentDao.cookieFor(runtimeInfo.getIdentifier())).thenReturn("pavanIsGreat");
            agentService.updateRuntimeInfo(runtimeInfo);
            verify(agentInstances).updateAgentRuntimeInfo(runtimeInfo);
        }

        @Test
        void shouldThrowExceptionWhenAgentWithNoCookieTriesToUpdateStatus() {
            AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), null, false);

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
        void shouldThrowExceptionWhenADuplicateAgentTriesToUpdateStatus() {
            AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), null, false);
            runtimeInfo.setCookie("invalid_cookie");
            AgentInstance original = createFromLiveAgent(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), null, false), new SystemEnvironment(), null);

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
        @Test
        void shouldUpdateTheAgentAttributes() {
            String uuid = "uuid";
            HttpOperationResult result = new HttpOperationResult();
            Username username = new Username(new CaseInsensitiveString("test"));

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(agentDao.getAgentByUUID(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(mock(AgentInstance.class));

            AgentInstance agentInstance = agentService.updateAgentAttributes(uuid, "new-hostname", "resource1,resource2", createEnvironmentsConfigWith("env1", "env2"), TriState.TRUE, result);

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

            when(agentDao.getAgentByUUID(uuid)).thenReturn(agent);
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
            when(agentDao.getAgentByUUID(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
            when(agentInstance.isNullAgent()).thenReturn(true);
            when(agentInstance.getUuid()).thenReturn(uuid);

            agentService.updateAgentAttributes(uuid, "new-hostname", "resource1,resource2", createEnvironmentsConfigWith("env1", "env2"), TriState.TRUE, result);

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
            when(agentDao.getAgentByUUID(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
            when(agentInstance.isNullAgent()).thenReturn(false);
            when(agentInstance.getUuid()).thenReturn(uuid);

            agentService.updateAgentAttributes(uuid, "new-hostname", "resource1,resource2", emptyEnvsConfig, TriState.TRUE, result);

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
            when(agentDao.getAgentByUUID(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
            when(agentInstance.isNullAgent()).thenReturn(false);
            when(agentInstance.getUuid()).thenReturn(uuid);

            agentService.updateAgentAttributes(uuid, "new-hostname", "", createEnvironmentsConfigWith("env1"), TriState.TRUE, result);

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
            when(agentDao.getAgentByUUID(uuid)).thenReturn(agent);
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
            when(agentDao.getAgentByUUID(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(mock(AgentInstance.class));

            AgentInstance agentInstance = agentService.updateAgentAttributes(uuid, "new-hostname", "res%^1", createEnvironmentsConfigWith("env1"), TriState.TRUE, result);

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
            when(agentDao.getAgentByUUID(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(mock(AgentInstance.class));

            AgentInstance agentInstance = agentService.updateAgentAttributes(uuid, null, null, createEnvironmentsConfigWith("env1", "env2"), TriState.TRUE, result);

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

    @Test
    void shouldAssociateCookieForAnAgent() {
        when(uuidGenerator.randomUuid()).thenReturn("foo");
        assertThat(agentService.assignCookie(agentIdentifier), is("foo"));
        verify(agentDao).associateCookie(eq(agentIdentifier), any(String.class));
    }

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

        AgentsViewModel agentsViewModel = agentService.filter(uuids);
        AgentViewModel view1 = new AgentViewModel(instance1);
        AgentViewModel view2 = new AgentViewModel(instance3);

        assertThat(agentsViewModel, is(new AgentsViewModel(view1, view2)));
        verify(agentInstances).filter(uuids);
    }

    @Test
    void filterShouldEmptyAgentsViewModelForNullOrEmptyListOfUUIDs() {
        AgentsViewModel agentsViewModel = agentService.filter(emptyList());
        assertThat(agentsViewModel, is(emptyList()));

        agentsViewModel = agentService.filter(null);
        assertThat(agentsViewModel, is(emptyList()));
    }

    @Test
    void shouldThrowInternalServerErrorWhenDeleteAgentsFailDueToAgentNotFound() {
        String uuid = "1234";
        Username username = new Username(new CaseInsensitiveString("test"));

        AgentInstance agentInstance = mock(AgentInstance.class);
        when(securityService.hasOperatePermissionForAgents(username)).thenReturn(true);
        when(agentInstance.canBeDeleted()).thenReturn(true);
        when(agentInstances.findAgentAndRefreshStatus(uuid)).thenReturn(agentInstance);
        when(agentInstance.getAgent()).thenReturn(mock(Agent.class));

        AgentService agentService = new AgentService(new SystemEnvironment(), agentInstances,
                agentDao, uuidGenerator, serverHealthService = mock(ServerHealthService.class), null);

        HttpOperationResult result = mock(HttpOperationResult.class);
        agentService.deleteAgents(result, singletonList(uuid));

        verify(result).internalServerError(any(String.class), any(HealthStateType.class));
    }

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

        agentService.bulkUpdateAgentAttributes(asList("uuid1", "uuid2"), asList("R1", "R2"), emptyStrList, createEnvironmentsConfigWith("test", "prod"), emptyStrList, TriState.TRUE, result);

        verify(agentDao).bulkUpdateAttributes(anyList(), anyMap(), eq(TriState.TRUE));
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
        agentService.bulkUpdateAgentAttributes(uuids, emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.TRUE, result);

        verify(agentDao).bulkUpdateAttributes(anyList(), anyMap(), eq(TriState.TRUE));
        assertThat(result.isSuccessful(), is(true));
        assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, UUID2]."));
    }

    @Test
    void shouldReturnTrueIsGivenUuidIsPresentAndTheAgentInstanceIsNotNullAndRegistered() {
        String uuid = "uuid";
        AgentInstance agentInstance = building();
        when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);

        assertTrue(agentService.hasAgent(uuid));
    }

    @Test
    void shouldReturnFalseIfUuidGivenDoesNotExist() {
        String uuid = "uuid";
        AgentInstance agentInstance = AgentInstanceMother.nullInstance();
        when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);

        assertFalse(agentService.hasAgent(uuid));
    }

    @Test
    void shouldReturnFalseIfAgentForTheUuidGivenExistButIsNotRegistered() {
        String uuid = "uuid";
        AgentInstance agentInstance = pendingInstance();
        when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);

        assertFalse(agentService.hasAgent(uuid));
    }

    @Test
    void shouldReturnDistinctListOfResourcesFromAllAgents() {
        AgentInstance agentInstance = building();
        agentInstance.getAgent().setResources("a,b,c");

        AgentInstance agentInstance1 = building();
        agentInstance1.getAgent().setResources("d,e,a");

        when(agentInstances.values()).thenReturn(asList(agentInstance, agentInstance1));

        assertEquals(asList("a","b","c","d","e"), agentService.getListOfResourcesAcrossAgents());
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

        assertEquals(asList("a","b","c"), agentService.getListOfResourcesAcrossAgents());
    }

    private EnvironmentsConfig createEnvironmentsConfigWith(String... envs) {
        EnvironmentsConfig envsConfig = new EnvironmentsConfig();
        Arrays.stream(envs).forEach(env -> envsConfig.add(new BasicEnvironmentConfig(new CaseInsensitiveString(env))));
        return envsConfig;
    }
}
