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
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.helper.AgentInstanceMother;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.thoughtworks.go.CurrentGoCDVersion.docsUrl;
import static com.thoughtworks.go.serverhealth.HealthStateScope.forAgent;
import static com.thoughtworks.go.serverhealth.HealthStateType.duplicateAgent;
import static com.thoughtworks.go.serverhealth.ServerHealthState.warning;
import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static com.thoughtworks.go.utils.Timeout.THIRTY_SECONDS;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

public class AgentServiceTest {
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
    public void setUp() {
        agentInstances = mock(AgentInstances.class);
        securityService = mock(SecurityService.class);
        agent = new Agent("uuid", "host", "192.168.1.1");
        when(agentInstances.findAgentAndRefreshStatus("uuid")).thenReturn(AgentInstance.createFromAgent(agent, new SystemEnvironment(), null));
        agentDao = mock(AgentDao.class);
        goConfigService = mock(GoConfigService.class);
        uuidGenerator = mock(UuidGenerator.class);
        agentService = new AgentService(new SystemEnvironment(), agentInstances,
                securityService, agentDao, uuidGenerator, serverHealthService = mock(ServerHealthService.class), null, goConfigService);
        agentIdentifier = agent.getAgentIdentifier();
        when(agentDao.cookieFor(agentIdentifier)).thenReturn("cookie");
    }

    @Test
    public void shouldUpdateStatus() {
        AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "pavanIsGreat");
        when(agentDao.cookieFor(runtimeInfo.getIdentifier())).thenReturn("pavanIsGreat");
        agentService.updateRuntimeInfo(runtimeInfo);
        verify(agentInstances).updateAgentRuntimeInfo(runtimeInfo);
    }

    @Test
    public void shouldThrowExceptionWhenAgentWithNoCookieTriesToUpdateStatus() {
        AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), null);

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
    public void shouldUpdateAgentsAssociationWithSpecifiedEnv() {
        AgentInstance agentInstance = mock(AgentInstance.class);
        Username username = new Username(new CaseInsensitiveString("test"));
        String uuid = "uuid";
        Agent agentConfigForUUID1 = mock(Agent.class);

        when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
        when(agentDao.agentByUuid(uuid)).thenReturn(agent);
        when(agentDao.agentByUuid("uuid1")).thenReturn(agentConfigForUUID1);
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
        agentService.updateAgentsAssociationWithSpecifiedEnv(username, testEnv, asList(uuid, "uuid2"), result);

        verify(agentDao).bulkUpdateAttributes(anyList(), anyMap(), eq(TriState.UNSET));
        assertTrue(result.isSuccessful());
        assertThat(result.message(), is("Updated agent(s) with uuid(s): [uuid, uuid2]."));
    }

    @Test
    public void shouldAddAgentsAssociationToTheSpecifiedEnv() {
        AgentInstance agentInstance = mock(AgentInstance.class);
        Username username = new Username(new CaseInsensitiveString("test"));
        String uuid = "uuid";
        Agent agentConfigForUUID1 = mock(Agent.class);

        when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
        when(agentDao.agentByUuid(uuid)).thenReturn(agent);
        when(agentDao.agentByUuid("uuid1")).thenReturn(agentConfigForUUID1);

        EnvironmentsConfig envConfigs = new EnvironmentsConfig();
        BasicEnvironmentConfig testEnv = new BasicEnvironmentConfig(new CaseInsensitiveString("test"));
        envConfigs.add(testEnv);

        when(goConfigService.getEnvironments()).thenReturn(envConfigs);
        when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
        when(agentInstances.findAgent("uuid1")).thenReturn(mock(AgentInstance.class));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        agentService.updateAgentsAssociationWithSpecifiedEnv(username, testEnv, asList(uuid, "uuid1"), result);

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
    public void shouldRemoveAgentsAssociationFromTheSpecifiedEnv() {
        AgentInstance agentInstance = mock(AgentInstance.class);
        Username username = new Username(new CaseInsensitiveString("test"));
        String uuid = "uuid";
        Agent agentConfigForUUID1 = mock(Agent.class);

        when(agentConfigForUUID1.getEnvironments()).thenReturn("test");
        agent.setEnvironments("test");

        when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
        when(agentDao.agentByUuid(uuid)).thenReturn(agent);
        when(agentDao.agentByUuid("uuid1")).thenReturn(agentConfigForUUID1);

        EnvironmentsConfig envConfigs = new EnvironmentsConfig();
        BasicEnvironmentConfig testEnv = new BasicEnvironmentConfig(new CaseInsensitiveString("test"));
        envConfigs.add(testEnv);
        testEnv.addAgent(uuid);
        testEnv.addAgent("uuid1");

        when(goConfigService.getEnvironments()).thenReturn(envConfigs);
        when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
        when(agentInstances.findAgent("uuid1")).thenReturn(mock(AgentInstance.class));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        agentService.updateAgentsAssociationWithSpecifiedEnv(username, testEnv, emptyList(), result);

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
    public void shouldThrowExceptionWhenADuplicateAgentTriesToUpdateStatus() {
        AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), null);
        runtimeInfo.setCookie("invalid_cookie");
        AgentInstance original = AgentInstance.createFromLiveAgent(new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), null), new SystemEnvironment(), null);

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

    @Test
    public void shouldAssociateCookieForAnAgent() {
        when(uuidGenerator.randomUuid()).thenReturn("foo");
        assertThat(agentService.assignCookie(agentIdentifier), is("foo"));
        verify(agentDao).associateCookie(eq(agentIdentifier), any(String.class));
    }

    @Test
    public void shouldUnderstandFilteringAgentListBasedOnUuid() {
        AgentInstance instance1 = AgentInstance.createFromLiveAgent(AgentRuntimeInfo.fromServer(new Agent("uuid-1", "host-1", "192.168.1.2"), true, "/foo/bar", 100l, "linux"), new SystemEnvironment(), null);
        AgentInstance instance3 = AgentInstance.createFromLiveAgent(AgentRuntimeInfo.fromServer(new Agent("uuid-3", "host-3", "192.168.1.4"), true, "/baz/quux", 300l, "linux"), new SystemEnvironment(), null);
        when(agentInstances.filter(asList("uuid-1", "uuid-3"))).thenReturn(asList(instance1, instance3));
        AgentsViewModel agents = agentService.filter(Arrays.asList("uuid-1", "uuid-3"));
        AgentViewModel view1 = new AgentViewModel(instance1);
        AgentViewModel view2 = new AgentViewModel(instance3);
        assertThat(agents, is(new AgentsViewModel(view1, view2)));
        verify(agentInstances).filter(asList("uuid-1", "uuid-3"));
    }

    @Test
    public void shouldFailWhenDeleteIsNotSuccessful() {
        AgentInstance agentInstance = mock(AgentInstance.class);
        String uuid = "1234";
        Username username = new Username(new CaseInsensitiveString("test"));
        HttpOperationResult operationResult = mock(HttpOperationResult.class);

        when(securityService.hasOperatePermissionForAgents(username)).thenReturn(true);

        when(agentInstance.canBeDeleted()).thenReturn(true);

        when(agentInstances.findAgentAndRefreshStatus(uuid)).thenReturn(agentInstance);
        when(agentInstance.getAgent()).thenReturn(mock(Agent.class));

        AgentService agentService = new AgentService(new SystemEnvironment(), agentInstances,
                securityService, agentDao, uuidGenerator, serverHealthService = mock(ServerHealthService.class), null, goConfigService);

        agentService.deleteAgents(username, operationResult, singletonList(uuid));

        verify(operationResult).internalServerError(any(String.class), any(HealthStateType.class));
    }

    @Test
    public void shouldUpdateAttributesForTheAgent() {
        AgentInstance agentInstance = mock(AgentInstance.class);
        List<String> uuids = singletonList("uuid");
        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        Username username = new Username(new CaseInsensitiveString("test"));

        when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
        when(goConfigService.getEnvironments()).thenReturn(new EnvironmentsConfig());
        when(agentInstances.findAgent("uuid")).thenReturn(agentInstance);

        agentService.bulkUpdateAgentAttributes(username, operationResult, uuids, emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.TRUE);

        verify(agentDao).bulkUpdateAttributes(anyList(), anyMap(), eq(TriState.TRUE));
        assertThat(operationResult.isSuccessful(), is(true));
        assertThat(operationResult.message(), is("Updated agent(s) with uuid(s): [uuid]."));
    }

    @Test
    public void shouldEnableMultipleAgents() {
        Username username = new Username(new CaseInsensitiveString("test"));
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromAgent(agentIdentifier, AgentRuntimeStatus.Unknown, "cookie", false);
        AgentInstance pending = AgentInstance.createFromLiveAgent(agentRuntimeInfo, new SystemEnvironment(), null);

        Agent agent = new Agent("UUID2", "remote-host", "50.40.30.20");
        agent.disable();
        AgentInstance fromConfigFile = AgentInstance.createFromAgent(agent, new SystemEnvironment(), null);

        when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
        when(goConfigService.getEnvironments()).thenReturn(new EnvironmentsConfig());
        when(agentInstances.findAgent("uuid")).thenReturn(pending);
        when(agentInstances.findAgent("UUID2")).thenReturn(fromConfigFile);

        List<String> uuids = asList(pending.getUuid(), fromConfigFile.getUuid());
        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(username, operationResult, uuids, emptyStrList, emptyStrList, emptyEnvsConfig, emptyStrList, TriState.TRUE);

        verify(agentDao).bulkUpdateAttributes(anyList(), anyMap(), eq(TriState.TRUE));
        assertThat(operationResult.isSuccessful(), is(true));
        assertThat(operationResult.message(), is("Updated agent(s) with uuid(s): [uuid, UUID2]."));
    }

    @Nested
    class AgentUpdateAttributes {
        @Test
        void shouldUpdateTheAgentAttributes() {
            String uuid = "uuid";
            HttpOperationResult result = new HttpOperationResult();
            Username username = new Username(new CaseInsensitiveString("test"));

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(agentDao.agentByUuid(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(mock(AgentInstance.class));

            AgentInstance agentInstance = agentService.updateAgentAttributes(username, result, uuid, "new-hostname", "resource1,resource2", createEnvironmentsConfigWith("env1", "env2"), TriState.TRUE);

            verify(agentDao).saveOrUpdate(any(Agent.class));
            assertTrue(result.isSuccess());
            assertThat(result.message(), is("Updated agent with uuid uuid."));

            Agent agentFromService = agentInstance.getAgent();

            assertThat(agentFromService.getHostname(), is("new-hostname"));
            assertThat(agentFromService.getResources().getCommaSeparatedResourceNames(), is("resource1,resource2"));
            assertThat(agentFromService.getEnvironments(), is("env1,env2"));
            assertFalse(agentFromService.isDisabled());
        }

        @Test
        void shouldThrow400IfNoOperationToPerform() {
            String uuid = "uuid";
            HttpOperationResult result = new HttpOperationResult();
            Username username = new Username(new CaseInsensitiveString("test"));

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(agentDao.agentByUuid(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(mock(AgentInstance.class));

            agentService.updateAgentAttributes(username, result, uuid, null, null, null, TriState.UNSET);

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
            when(agentDao.agentByUuid(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
            when(agentInstance.isNullAgent()).thenReturn(true);
            when(agentInstance.getUuid()).thenReturn(uuid);

            agentService.updateAgentAttributes(username, result, uuid, "new-hostname", "resource1,resource2", createEnvironmentsConfigWith("env1", "env2"), TriState.TRUE);

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
            when(agentDao.agentByUuid(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
            when(agentInstance.isNullAgent()).thenReturn(false);
            when(agentInstance.getUuid()).thenReturn(uuid);

            agentService.updateAgentAttributes(username, result, uuid, "new-hostname", "resource1,resource2", emptyEnvsConfig, TriState.TRUE);

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
            when(agentDao.agentByUuid(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
            when(agentInstance.isNullAgent()).thenReturn(false);
            when(agentInstance.getUuid()).thenReturn(uuid);

            agentService.updateAgentAttributes(username, result, uuid, "new-hostname", "", createEnvironmentsConfigWith("env1"), TriState.TRUE);

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
            when(agentDao.agentByUuid(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);
            when(agentInstance.isNullAgent()).thenReturn(false);
            when(agentInstance.isPending()).thenReturn(true);
            when(agentInstance.getUuid()).thenReturn(uuid);

            agentService.updateAgentAttributes(username, result, uuid, "new-hostname", "resource1", createEnvironmentsConfigWith("env1"), TriState.UNSET);

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
            when(agentDao.agentByUuid(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(mock(AgentInstance.class));

            AgentInstance agentInstance = agentService.updateAgentAttributes(username, result, uuid, "new-hostname", "res%^1", createEnvironmentsConfigWith("env1"), TriState.TRUE);

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
            when(agentDao.agentByUuid(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(mock(AgentInstance.class));

            AgentInstance agentInstance = agentService.updateAgentAttributes(username, result, uuid, null, null, createEnvironmentsConfigWith("env1", "env2"), TriState.TRUE);

            verify(agentDao).saveOrUpdate(any(Agent.class));
            assertTrue(result.isSuccess());
            assertThat(result.message(), is("Updated agent with uuid uuid."));

            Agent agentFromService = agentInstance.getAgent();

            assertThat(agentFromService.getHostname(), is("host"));
            assertThat(agentFromService.getResources().getCommaSeparatedResourceNames(), is(""));
            assertThat(agentFromService.getEnvironments(), is("env1,env2"));
            assertFalse(agentFromService.isDisabled());
        }

        @Test
        void shouldNotAddEnvsWhichAreAssociatedWithTheAgentFromConfigRepo() {
            String uuid = "uuid";
            HttpOperationResult result = new HttpOperationResult();
            Username username = new Username(new CaseInsensitiveString("test"));

            when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
            when(agentDao.agentByUuid(uuid)).thenReturn(agent);
            when(agentInstances.findAgent(uuid)).thenReturn(mock(AgentInstance.class));

            EnvironmentsConfig environmentConfigs = new EnvironmentsConfig();
            EnvironmentConfig repoEnvConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("config-repo-env"));
            repoEnvConfig.setOrigins(new RepoConfigOrigin());
            repoEnvConfig.addAgent(uuid);
            environmentConfigs.add(repoEnvConfig);
            environmentConfigs.add(new BasicEnvironmentConfig(new CaseInsensitiveString("non-config-repo-env")));
            AgentInstance agentInstance = agentService.updateAgentAttributes(username, result, uuid, null, null, environmentConfigs, TriState.TRUE);

            verify(agentDao).saveOrUpdate(any(Agent.class));
            assertTrue(result.isSuccess());
            assertThat(result.message(), is("Updated agent with uuid uuid."));

            assertThat(agentInstance.getAgent().getEnvironments(), is("non-config-repo-env"));
        }

        private EnvironmentsConfig createEnvironmentsConfigWith(String... envs) {
            EnvironmentsConfig envsConfig = new EnvironmentsConfig();
            Arrays.stream(envs).forEach(env -> envsConfig.add(new BasicEnvironmentConfig(new CaseInsensitiveString(env))));
            return envsConfig;
        }
    }

    @Test
    void shouldReturnTrueIsGivenUuidIsPresentAndTheAgentInstanceIsNotNullAndRegistered() {
        String uuid = "uuid";
        AgentInstance agentInstance = AgentInstanceMother.building();
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
        AgentInstance agentInstance = AgentInstanceMother.pendingInstance();
        when(agentInstances.findAgent(uuid)).thenReturn(agentInstance);

        assertFalse(agentService.hasAgent(uuid));
    }
}
