/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.UpdateConfigCommand;
import com.thoughtworks.go.config.update.AgentsUpdateCommand;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

public class AgentConfigServiceTest {

    private GoConfigService goConfigService;
    private AgentConfigService agentConfigService;

    @Before
    public void setUp() throws Exception {
        goConfigService = mock(GoConfigService.class);
        agentConfigService = new AgentConfigService(goConfigService);
    }

    @Test
    public void shouldEnableAgentWhenPending() {
        String agentId = DatabaseAccessHelper.AGENT_UUID;
        AgentConfig agentConfig = new AgentConfig(agentId, "remote-host", "50.40.30.20");
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("remote-host", "50.40.30.20", agentId), AgentRuntimeStatus.Unknown, "cookie", null, false);
        AgentInstance instance = AgentInstance.createFromLiveAgent(agentRuntimeInfo, new SystemEnvironment());
        agentConfigService.enableAgents(Username.ANONYMOUS, instance);
        shouldPerformCommand(new GoConfigDao.CompositeConfigCommand(new AgentConfigService.AddAgentCommand(agentConfig)));
    }

    private void shouldPerformCommand(UpdateConfigCommand command) {
        ArgumentCaptor<AgentsUpdateCommand> captor = ArgumentCaptor.forClass(AgentsUpdateCommand.class);
        verify(goConfigService).updateConfig(captor.capture(), eq(Username.ANONYMOUS));
        AgentsUpdateCommand updateCommand = captor.getValue();
        assertThat((UpdateConfigCommand)ReflectionUtil.getField(updateCommand, "command"), is(command));
    }

    @Test
    public void shouldEnableMultipleAgents() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("remote-host", "50.40.30.20", "abc"), AgentRuntimeStatus.Unknown, "cookie", null, false);
        AgentInstance pending = AgentInstance.createFromLiveAgent(agentRuntimeInfo, new SystemEnvironment());

        AgentConfig agentConfig = new AgentConfig("UUID2", "remote-host", "50.40.30.20");
        agentConfig.disable();
        AgentInstance fromConfigFile = AgentInstance.createFromConfig(agentConfig, new SystemEnvironment());
        when(goConfigService.hasAgent(fromConfigFile.getUuid())).thenReturn(true);
        when(goConfigService.hasAgent(pending.getUuid())).thenReturn(false);

        agentConfigService.enableAgents(Username.ANONYMOUS, pending, fromConfigFile);

        GoConfigDao.CompositeConfigCommand command = new GoConfigDao.CompositeConfigCommand(
                new AgentConfigService.AddAgentCommand(pending.agentConfig()),
                new AgentConfigService.UpdateAgentApprovalStatus("UUID2", false));
        ArgumentCaptor<AgentsUpdateCommand> captor = ArgumentCaptor.forClass(AgentsUpdateCommand.class);
        verify(goConfigService).updateConfig(captor.capture(), eq(Username.ANONYMOUS));
        AgentsUpdateCommand updateCommand = captor.getValue();
        assertThat((GoConfigDao.CompositeConfigCommand) ReflectionUtil.getField(updateCommand, "command"), is(command));
    }

    @Test
    public void shouldEnableAgentWhenAlreadyInTheConfig() {
        String agentId = DatabaseAccessHelper.AGENT_UUID;
        AgentConfig agentConfig = new AgentConfig(agentId, "remote-host", "50.40.30.20");
        agentConfig.disable();
        AgentInstance instance = AgentInstance.createFromConfig(agentConfig, new SystemEnvironment());
        when(goConfigService.currentCruiseConfig()).thenReturn(mock(CruiseConfig.class));
        when(goConfigService.hasAgent(agentConfig.getUuid())).thenReturn(true);
        agentConfigService.enableAgents(Username.ANONYMOUS, instance);
        shouldPerformCommand(new GoConfigDao.CompositeConfigCommand((UpdateConfigCommand) new AgentConfigService.UpdateAgentApprovalStatus(agentId, false)));
    }
}
