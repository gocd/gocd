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

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.update.AgentsEntityConfigUpdateCommand;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.AgentDao;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TriState;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

public class AgentConfigServiceTest {

    private GoConfigService goConfigService;
    private AgentConfigService agentConfigService;
    private EnvironmentConfigService environmentConfigService;
    private AgentDao agentDao;

    @Before
    public void setUp() throws Exception {
        goConfigService = mock(GoConfigService.class);
        environmentConfigService = mock(EnvironmentConfigService.class);
        agentDao = mock(AgentDao.class);
        agentConfigService = new AgentConfigService(goConfigService, agentDao);
    }

    @Test
    public void shouldEnableAgentWhenPending() {
        String agentId = DatabaseAccessHelper.AGENT_UUID;
        AgentConfig agentConfig = new AgentConfig(agentId, "remote-host", "50.40.30.20");
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("remote-host", "50.40.30.20", agentId), AgentRuntimeStatus.Unknown, "cookie");
        AgentInstance instance = AgentInstance.createFromLiveAgent(agentRuntimeInfo, new SystemEnvironment(), null);

        AgentInstances agentInstances = new AgentInstances(null, null, instance);
        List<String> uuids = Arrays.asList(agentId);

        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, new HttpLocalizedOperationResult(), uuids, environmentConfigService, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.TRUE);

        shouldPerformCommand();
    }

    private void shouldPerformCommand() {
        ArgumentCaptor<AgentsEntityConfigUpdateCommand> captor = ArgumentCaptor.forClass(AgentsEntityConfigUpdateCommand.class);
        verify(goConfigService).updateConfig(captor.capture(), eq(Username.ANONYMOUS));
    }

    @Test
    public void shouldEnableMultipleAgents() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("remote-host", "50.40.30.20", "abc"), AgentRuntimeStatus.Unknown, "cookie");
        AgentInstance pending = AgentInstance.createFromLiveAgent(agentRuntimeInfo, new SystemEnvironment(), null);

        AgentConfig agentConfig = new AgentConfig("UUID2", "remote-host", "50.40.30.20");
        agentConfig.disable();
        AgentInstance fromConfigFile = AgentInstance.createFromConfig(agentConfig, new SystemEnvironment(), null);

        AgentInstances agentInstances = new AgentInstances(null, null, fromConfigFile, pending);

        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, new HttpLocalizedOperationResult(), Arrays.asList(pending.getUuid(), fromConfigFile.getUuid()), environmentConfigService, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.TRUE);

        verify(agentDao).bulkUpdateAttributes(eq(Arrays.asList(pending.getUuid(),fromConfigFile.getUuid())), eq(Collections.emptyList()),eq(Collections.emptyList()),eq(Collections.emptyList()),eq(Collections.emptyList()), eq(TriState.TRUE), eq(agentInstances));
    }

    @Test
    public void shouldEnableAgentWhenAlreadyInTheDatabase() {
        String agentId = DatabaseAccessHelper.AGENT_UUID;
        AgentConfig agentConfig = new AgentConfig(agentId, "remote-host", "50.40.30.20");
        agentConfig.disable();
        AgentInstance instance = AgentInstance.createFromConfig(agentConfig, new SystemEnvironment(), null);
        when(goConfigService.currentCruiseConfig()).thenReturn(mock(CruiseConfig.class));
        when(agentConfigService.hasAgent(agentConfig.getUuid())).thenReturn(true);

        AgentInstances agentInstances = new AgentInstances(null, null, instance);
        agentConfigService.bulkUpdateAgentAttributes(agentInstances, Username.ANONYMOUS, new HttpLocalizedOperationResult(), Arrays.asList(instance.getUuid()), environmentConfigService, Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), TriState.TRUE);

        verify(agentDao).bulkUpdateAttributes(eq(Arrays.asList(agentConfig.getUuid())), eq(Collections.emptyList()),eq(Collections.emptyList()),eq(Collections.emptyList()),eq(Collections.emptyList()), eq(TriState.TRUE), eq(agentInstances));
    }
}
