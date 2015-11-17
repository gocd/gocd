package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.update.AgentsUpdateCommand;
import com.thoughtworks.go.domain.AgentInstance;
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
import static org.mockito.Matchers.any;
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
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("remote-host", "50.40.30.20", agentId), "cookie", null);
        AgentInstance instance = AgentInstance.createFromLiveAgent(agentRuntimeInfo, new SystemEnvironment());
        agentConfigService.enableAgents(Username.ANONYMOUS, instance);
        shouldPerformCommand(new GoConfigDao.CompositeConfigCommand(AgentConfigService.createAddAgentCommand(agentConfig)));
    }

    private void shouldPerformCommand(UpdateConfigCommand command) {
        ArgumentCaptor<AgentsUpdateCommand> captor = ArgumentCaptor.forClass(AgentsUpdateCommand.class);
        verify(goConfigService).updateConfig(captor.capture(), eq(Username.ANONYMOUS));
        AgentsUpdateCommand updateCommand = captor.getValue();
        assertThat((UpdateConfigCommand)ReflectionUtil.getField(updateCommand, "command"), is(command));
    }

    @Test
    public void shouldEnableMultipleAgents() {
        AgentRuntimeInfo agentRuntimeInfo = AgentRuntimeInfo.fromAgent(new AgentIdentifier("remote-host", "50.40.30.20", "abc"), "cookie", null);
        AgentInstance pending = AgentInstance.createFromLiveAgent(agentRuntimeInfo, new SystemEnvironment());

        AgentConfig agentConfig = new AgentConfig("UUID2", "remote-host", "50.40.30.20");
        agentConfig.disable();
        AgentInstance fromConfigFile = AgentInstance.createFromConfig(agentConfig, new SystemEnvironment());
        when(goConfigService.hasAgent(fromConfigFile.getUuid())).thenReturn(true);
        when(goConfigService.hasAgent(pending.getUuid())).thenReturn(false);

        agentConfigService.enableAgents(Username.ANONYMOUS, pending, fromConfigFile);

        GoConfigDao.CompositeConfigCommand command = new GoConfigDao.CompositeConfigCommand(
                AgentConfigService.createAddAgentCommand(pending.agentConfig()),
                AgentConfigService.updateApprovalStatus("UUID2", false));
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
        shouldPerformCommand(new GoConfigDao.CompositeConfigCommand(AgentConfigService.updateApprovalStatus(agentId, false)));
    }
}