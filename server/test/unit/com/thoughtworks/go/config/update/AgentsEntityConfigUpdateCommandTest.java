/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ResourceConfig;
import com.thoughtworks.go.config.exceptions.ElasticAgentsResourceUpdateException;
import com.thoughtworks.go.config.exceptions.InvalidPendingAgentOperationException;
import com.thoughtworks.go.config.exceptions.NoSuchAgentException;
import com.thoughtworks.go.config.exceptions.NoSuchEnvironmentException;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.util.TriState;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentsEntityConfigUpdateCommandTest {

    private HttpLocalizedOperationResult result;
    private Username currentUser;
    private BasicCruiseConfig cruiseConfig;
    private GoConfigService goConfigService;
    private AgentInstances agentInstances;
    private List<String> uuids;
    private List<String> environmentsToAdd;
    private List<String> environmentsToRemove;
    private List<String> resourcesToAdd;
    private List<String> resourcesToRemove;
    private TriState triState;

    @Before
    public void setUp() throws Exception {
        result = new HttpLocalizedOperationResult();
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = new GoConfigMother().defaultCruiseConfig();
        goConfigService = mock(GoConfigService.class);
        agentInstances = mock(AgentInstances.class);

        uuids = new ArrayList<>();
        environmentsToAdd = new ArrayList<>();
        environmentsToRemove = new ArrayList<>();
        resourcesToAdd = new ArrayList<>();
        resourcesToRemove = new ArrayList<>();
        triState = TriState.TRUE;
        when(goConfigService.getEnvironments()).thenReturn(cruiseConfig.getEnvironments());
    }


    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void shouldReturnTrueIfAnyOperationPerformedOnAgent() throws Exception {
        AgentsEntityConfigUpdateCommand command = new AgentsEntityConfigUpdateCommand(agentInstances, currentUser, result, uuids, environmentsToAdd,
                environmentsToRemove, triState, resourcesToAdd, resourcesToRemove, goConfigService);

        when(goConfigService.isAdministrator(currentUser.getUsername())).thenReturn(true);
        assertTrue(command.canContinue(cruiseConfig));
    }

    @Test
    public void shouldReturnFalseIfNoOperationPerformedOnAgents() throws Exception {
        triState = TriState.UNSET;
        AgentsEntityConfigUpdateCommand command = new AgentsEntityConfigUpdateCommand(agentInstances, currentUser, result, uuids, environmentsToAdd,
                environmentsToRemove, triState, resourcesToAdd, resourcesToRemove, goConfigService);

        when(goConfigService.isAdministrator(currentUser.getUsername())).thenReturn(true);
        assertFalse(command.canContinue(cruiseConfig));
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.badRequest(LocalizedMessage.string("NO_OPERATION_PERFORMED_ON_AGENTS"));
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldReturnFalseIfUserUnauthorizedToUpdateTheAgents() throws Exception {
        AgentsEntityConfigUpdateCommand command = new AgentsEntityConfigUpdateCommand(agentInstances, currentUser, result, uuids, environmentsToAdd,
                environmentsToRemove, triState, resourcesToAdd, resourcesToRemove, goConfigService);
        when(goConfigService.isAdministrator(currentUser.getUsername())).thenReturn(false);
        assertFalse(command.canContinue(cruiseConfig));
        HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
        expectedResult.unauthorized(LocalizedMessage.string("UNAUTHORIZED_TO_EDIT"), HealthStateType.unauthorised());
        assertThat(result, is(expectedResult));
    }

    @Test
    public void shouldThrowExceptionIfInvalidEnvironmentIsUpdatedOnAgent() throws Exception {
        environmentsToAdd.add("Dev");
        AgentsEntityConfigUpdateCommand command = new AgentsEntityConfigUpdateCommand(agentInstances, currentUser, result, uuids, environmentsToAdd,
                environmentsToRemove, triState, resourcesToAdd, resourcesToRemove, goConfigService);

        exception.expect(NoSuchEnvironmentException.class);
        exception.expectMessage("Environment [Dev] does not exist.");

        command.update(cruiseConfig);
    }

    @Test
    public void shouldThrowExceptionIfAgentWithUUIDNotFound() throws Exception {
        uuids.add("uuid-1");
        AgentInstance agentInstance = mock(AgentInstance.class);
        when(agentInstance.isNullAgent()).thenReturn(true);
        when(agentInstances.findAgent("uuid-1")).thenReturn(agentInstance);
        AgentsEntityConfigUpdateCommand command = new AgentsEntityConfigUpdateCommand(agentInstances, currentUser, result, uuids, environmentsToAdd,
                environmentsToRemove, triState, resourcesToAdd, resourcesToRemove, goConfigService);

        exception.expect(NoSuchAgentException.class);
        exception.expectMessage("Agents [uuid-1] could not be found");

        command.update(cruiseConfig);
    }

    @Test
    public void shouldThrowExceptionIfElasticAgentResourceIsUpdated() throws Exception {
        resourcesToAdd.add("firefox");
        uuids.add("uuid-1");
        AgentInstance agentInstance = mock(AgentInstance.class);
        when(agentInstance.isNullAgent()).thenReturn(false);
        when(agentInstance.isElastic()).thenReturn(true);
        when(agentInstances.findAgent("uuid-1")).thenReturn(agentInstance);
        AgentsEntityConfigUpdateCommand command = new AgentsEntityConfigUpdateCommand(agentInstances, currentUser, result, uuids, environmentsToAdd,
                environmentsToRemove, triState, resourcesToAdd, resourcesToRemove, goConfigService);

        exception.expect(ElasticAgentsResourceUpdateException.class);
        exception.expectMessage("Can not update resources on Elastic Agents [uuid-1]");

        command.update(cruiseConfig);
    }

    @Test
    public void shouldThrowExceptionIfPendingAgentIsUpdatedWithoutChangingItsState() throws Exception {
        resourcesToAdd.add("firefox");
        triState = TriState.UNSET;

        AgentInstance agentInstance = AgentInstanceMother.pendingInstance();
        AgentConfig agentConfig = agentInstance.agentConfig();

        uuids.add(agentConfig.getUuid());

        when(agentInstances.findAgent(agentConfig.getUuid())).thenReturn(agentInstance);
        AgentsEntityConfigUpdateCommand command = new AgentsEntityConfigUpdateCommand(agentInstances, currentUser, result, uuids, environmentsToAdd,
                environmentsToRemove, triState, resourcesToAdd, resourcesToRemove, goConfigService);

        exception.expect(InvalidPendingAgentOperationException.class);
        exception.expectMessage(String.format("Invalid operation performed on pending agents: [%s]", agentConfig.getUuid()));

        command.update(cruiseConfig);
    }

    @Test
    public void shouldUpdateResourcesOfAgents() throws Exception {
        resourcesToAdd.add("firefox");
        resourcesToRemove.add("linux");

        AgentInstance agentInstance = AgentInstanceMother.disabled();
        AgentConfig agentConfig = agentInstance.agentConfig();
        agentConfig.addResourceConfig(new ResourceConfig("linux"));

        cruiseConfig.agents().add(agentConfig);
        assertThat(agentConfig.getResourceConfigs().resourceNames(), is(Arrays.asList("linux")));

        when(agentInstances.findAgent(agentConfig.getUuid())).thenReturn(agentInstance);

        uuids.add(agentConfig.getUuid());
        AgentsEntityConfigUpdateCommand command = new AgentsEntityConfigUpdateCommand(agentInstances, currentUser, result, uuids, environmentsToAdd,
                environmentsToRemove, triState, resourcesToAdd, resourcesToRemove, goConfigService);
        command.update(cruiseConfig);

        assertThat(agentConfig.getResourceConfigs().resourceNames(), is(Arrays.asList("firefox")));

    }

    @Test
    public void shouldUpdateEnvironmentOfAgents() throws Exception {
        environmentsToAdd.add("prod");
        environmentsToRemove.add("dev");

        AgentInstance agentInstance = AgentInstanceMother.disabled();
        AgentConfig agentConfig = agentInstance.agentConfig();

        cruiseConfig.addEnvironment("dev");
        cruiseConfig.addEnvironment("prod");
        cruiseConfig.agents().add(agentConfig);
        cruiseConfig.getEnvironments().addAgentsToEnvironment("dev", agentConfig.getUuid());

        uuids.add(agentConfig.getUuid());

        List<String> emptyList = new ArrayList<>();

        assertThat(cruiseConfig.getEnvironments().find(new CaseInsensitiveString("dev")).getAgents().getUuids(), is(uuids));
        assertThat(cruiseConfig.getEnvironments().find(new CaseInsensitiveString("prod")).getAgents().getUuids(), is(emptyList));

        when(agentInstances.findAgent(agentConfig.getUuid())).thenReturn(agentInstance);

        AgentsEntityConfigUpdateCommand command = new AgentsEntityConfigUpdateCommand(agentInstances, currentUser, result, uuids, environmentsToAdd,
                environmentsToRemove, triState, resourcesToAdd, resourcesToRemove, goConfigService);
        command.update(cruiseConfig);

        assertThat(cruiseConfig.getEnvironments().find(new CaseInsensitiveString("dev")).getAgents().getUuids(), is(emptyList));
        assertThat(cruiseConfig.getEnvironments().find(new CaseInsensitiveString("prod")).getAgents().getUuids(), is(uuids));

    }

    @Test
    public void shouldUpdateConfigStateOfAgents() throws Exception {

        AgentInstance agentInstance = AgentInstanceMother.disabled();
        AgentConfig agentConfig = agentInstance.agentConfig();
        agentConfig.disable();

        cruiseConfig.agents().add(agentConfig);

        assertThat(agentConfig.isEnabled(), is(false));

        triState = TriState.TRUE;
        uuids.add(agentConfig.getUuid());

        when(agentInstances.findAgent(agentConfig.getUuid())).thenReturn(agentInstance);

        AgentsEntityConfigUpdateCommand command = new AgentsEntityConfigUpdateCommand(agentInstances, currentUser, result, uuids, environmentsToAdd,
                environmentsToRemove, triState, resourcesToAdd, resourcesToRemove, goConfigService);
        command.update(cruiseConfig);

        assertThat(agentConfig.isEnabled(), is(true));
    }
}
