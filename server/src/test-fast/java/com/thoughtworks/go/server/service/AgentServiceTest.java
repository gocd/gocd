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
import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.EnvironmentsConfig;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.AgentStatus;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.AgentDao;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.ui.AgentViewModel;
import com.thoughtworks.go.server.ui.AgentsViewModel;
import com.thoughtworks.go.server.util.UuidGenerator;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.LogFixture;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TriState;
import com.thoughtworks.go.utils.Timeout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static com.thoughtworks.go.util.SystemUtil.currentWorkingDirectory;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class AgentServiceTest {
    private AgentService agentService;
    private AgentInstances agentInstances;
    private AgentDao agentDao;
    private AgentIdentifier agentIdentifier;
    private UuidGenerator uuidGenerator;
    private ServerHealthService serverHealthService;
    AgentConfig agentConfig;
    private GoConfigService goConfigService;
    private SecurityService securityService;

    @BeforeEach
    public void setUp() {
        agentInstances = mock(AgentInstances.class);
        securityService = mock(SecurityService.class);
        agentConfig = new AgentConfig("uuid", "host", "192.168.1.1");
        when(agentInstances.findAgentAndRefreshStatus("uuid")).thenReturn(AgentInstance.createFromConfig(agentConfig, new SystemEnvironment(), null));
        agentDao = mock(AgentDao.class);
        goConfigService = mock(GoConfigService.class);
        uuidGenerator = mock(UuidGenerator.class);
        agentService = new AgentService(new SystemEnvironment(), agentInstances,
                securityService, agentDao, uuidGenerator, serverHealthService = mock(ServerHealthService.class), null, goConfigService);
        agentIdentifier = agentConfig.getAgentIdentifier();
        when(agentDao.cookieFor(agentIdentifier)).thenReturn("cookie");
    }

    @Test
    public void shouldUpdateStatus() throws Exception {
        AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, currentWorkingDirectory(), "pavanIsGreat");
        when(agentDao.cookieFor(runtimeInfo.getIdentifier())).thenReturn("pavanIsGreat");
        agentService.updateRuntimeInfo(runtimeInfo);
        verify(agentInstances).updateAgentRuntimeInfo(runtimeInfo);
    }

    @Test
    public void shouldThrowExceptionWhenAgentWithNoCookieTriesToUpdateStatus() throws Exception {
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
    public void shouldThrowExceptionWhenADuplicateAgentTriesToUpdateStatus() throws Exception {
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
                verify(serverHealthService).update(ServerHealthState.warning(format("[%s] has duplicate unique identifier which conflicts with [%s]", runtimeInfo.agentInfoForDisplay(), original.agentInfoForDisplay()),
                        "Please check the agent installation. Click <a href='" + CurrentGoCDVersion.docsUrl("/faq/agent_guid_issue.html") + "' target='_blank'>here</a> for more info.",
                        HealthStateType.duplicateAgent(HealthStateScope.forAgent(runtimeInfo.getCookie())), Timeout.THIRTY_SECONDS));
            }
        }

        verify(agentInstances).findAgentAndRefreshStatus(runtimeInfo.getUUId());
        verifyNoMoreInteractions(agentInstances);
    }

    @Test
    public void shouldAssociateCookieForAnAgent() throws Exception {
        when(uuidGenerator.randomUuid()).thenReturn("foo");
        assertThat(agentService.assignCookie(agentIdentifier), is("foo"));
        verify(agentDao).associateCookie(eq(agentIdentifier), any(String.class));
    }

    @Test
    public void shouldUnderstandFilteringAgentListBasedOnUuid() {
        AgentInstance instance1 = AgentInstance.createFromLiveAgent(AgentRuntimeInfo.fromServer(new AgentConfig("uuid-1", "host-1", "192.168.1.2"), true, "/foo/bar", 100l, "linux"), new SystemEnvironment(), null);
        AgentInstance instance3 = AgentInstance.createFromLiveAgent(AgentRuntimeInfo.fromServer(new AgentConfig("uuid-3", "host-3", "192.168.1.4"), true, "/baz/quux", 300l, "linux"), new SystemEnvironment(), null);
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

        AgentService agentService = new AgentService(new SystemEnvironment(), agentInstances,
                securityService, agentDao, uuidGenerator, serverHealthService = mock(ServerHealthService.class), null, goConfigService);

        agentService.deleteAgents(username, operationResult, asList(uuid));

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

        agentService.bulkUpdateAgentAttributes(username, operationResult, uuids, emptyList(), emptyList(), emptyList(), emptyList(), TriState.TRUE);

        verify(agentDao).bulkUpdateAttributes(uuids, emptyList(), emptyList(), emptyList(), emptyList(), TriState.TRUE, agentInstances);
        assertThat(operationResult.isSuccessful(), is(true));
        assertThat(operationResult.message(), is("Updated agent(s) with uuid(s): [uuid]."));
    }

    @Test
    public void shouldEnableMultipleAgents() {
        Username username = new Username(new CaseInsensitiveString("test"));
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromAgent(agentIdentifier, AgentRuntimeStatus.Unknown, "cookie", false);
        AgentInstance pending = AgentInstance.createFromLiveAgent(agentRuntimeInfo, new SystemEnvironment(), null);

        AgentConfig agentConfig = new AgentConfig("UUID2", "remote-host", "50.40.30.20");
        agentConfig.disable();
        AgentInstance fromConfigFile = AgentInstance.createFromConfig(agentConfig, new SystemEnvironment(), null);

        when(goConfigService.isAdministrator(username.getUsername())).thenReturn(true);
        when(goConfigService.getEnvironments()).thenReturn(new EnvironmentsConfig());
        when(agentInstances.findAgent("uuid")).thenReturn(pending);
        when(agentInstances.findAgent("UUID2")).thenReturn(fromConfigFile);

        List<String> uuids = asList(pending.getUuid(), fromConfigFile.getUuid());
        HttpLocalizedOperationResult operationResult = new HttpLocalizedOperationResult();
        agentService.bulkUpdateAgentAttributes(username, operationResult, uuids, emptyList(), emptyList(), emptyList(), emptyList(), TriState.TRUE);

        verify(agentDao).bulkUpdateAttributes(uuids, emptyList(), emptyList(), emptyList(), emptyList(), TriState.TRUE, agentInstances);
        assertThat(operationResult.isSuccessful(), is(true));
        assertThat(operationResult.message(), is("Updated agent(s) with uuid(s): [uuid, UUID2]."));

    }

}
