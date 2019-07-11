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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.ElasticAgentsResourceUpdateException;
import com.thoughtworks.go.config.exceptions.InvalidPendingAgentOperationException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.domain.NullAgentInstance;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.TriState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.domain.config.CaseInsensitiveStringMother.str;
import static com.thoughtworks.go.i18n.LocalizedMessage.forbiddenToEdit;
import static com.thoughtworks.go.serverhealth.HealthStateType.forbidden;
import static java.lang.String.format;
import static junit.framework.TestCase.assertFalse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentsUpdateValidatorTest {

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

    @BeforeEach
    public void setUp() throws Exception {
        result = new HttpLocalizedOperationResult();
        currentUser = new Username(new CaseInsensitiveString("user"));
        cruiseConfig = GoConfigMother.defaultCruiseConfig();
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

    @Nested
    class CanContinue {
        @Test
        public void shouldAllowAdministratorToPerformAnyOperationOnAgents() {
            when(goConfigService.isAdministrator(currentUser.getUsername())).thenReturn(true);
            assertTrue(newAgentsUpdateValidator().canContinue());
        }

        @Test
        public void shouldSignalBadRequestWhenNoOpIsPerformedOnAgents() {
            triState = TriState.UNSET;
            AgentsUpdateValidator command = newAgentsUpdateValidator();

            when(goConfigService.isAdministrator(currentUser.getUsername())).thenReturn(true);
            assertFalse(command.canContinue());
            HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
            expectedResult.badRequest("No Operation performed on agents.");
            assertThat(result).isEqualTo(expectedResult);
        }

        @Test
        public void shouldThrow403WhenNonAdminUserIsUpdatingAgents() {
            AgentsUpdateValidator command = newAgentsUpdateValidator();
            when(goConfigService.isAdministrator(currentUser.getUsername())).thenReturn(false);
            assertFalse(command.canContinue());
            HttpLocalizedOperationResult expectedResult = new HttpLocalizedOperationResult();
            expectedResult.forbidden(forbiddenToEdit(), forbidden());
            assertThat(result).isEqualTo(expectedResult);
        }
    }

    @Nested
    class Validations {
        @Test
        public void shouldThrowExceptionWhenOpsArePerformedOnPendingAgents() {
            triState = TriState.UNSET;
            AgentInstance pendingAgent = AgentInstanceMother.pending();
            uuids.add(pendingAgent.getUuid());
            when(agentInstances.findPendingAgents(uuids)).thenReturn(Arrays.asList(pendingAgent.agentConfig()));
            when(agentInstances.findAgent(pendingAgent.getUuid())).thenReturn(pendingAgent);

            assertThrows(InvalidPendingAgentOperationException.class, () -> newAgentsUpdateValidator().validate());
        }


        @Test
        public void shouldPassValidationWhenEnvironmentsToBeAddedRemovedExistsInConfigXML() throws Exception {
            environmentsToAdd.add("prod");
            environmentsToRemove.add("dev");

            AgentInstance agentInstance = AgentInstanceMother.disabled();
            AgentConfig agentConfig = agentInstance.agentConfig();

            uuids.add(agentConfig.getUuid());

            when(agentInstances.findAgent(agentConfig.getUuid())).thenReturn(agentInstance);

            EnvironmentsConfig envsConfig = new EnvironmentsConfig();
            envsConfig.add(new BasicEnvironmentConfig(str("dev")));
            envsConfig.add(new BasicEnvironmentConfig(str("prod")));

            when(goConfigService.getEnvironments()).thenReturn(envsConfig);
            newAgentsUpdateValidator().validate();
        }


        @Test
        public void shouldThrowExceptionWhenEnvironemntsToBeAddedRemovedDoesNotExistInConfigXML() {
            environmentsToAdd.add("prod");
            environmentsToRemove.add("dev");

            AgentInstance agentInstance = AgentInstanceMother.disabled();
            AgentConfig agentConfig = agentInstance.agentConfig();

            uuids.add(agentConfig.getUuid());

            when(agentInstances.findAgent(agentConfig.getUuid())).thenReturn(agentInstance);

            EnvironmentsConfig envsConfig = new EnvironmentsConfig();
            envsConfig.add(new BasicEnvironmentConfig(str("dev")));

            when(goConfigService.getEnvironments()).thenReturn(envsConfig);
            RecordNotFoundException rnfe = assertThrows(RecordNotFoundException.class, () -> newAgentsUpdateValidator().validate());
            assertEquals(rnfe.getMessage(), "Environment with name \'prod\' was not found!");
        }

        @Test
        public void shouldThrowExceptionWhenResourceNamesToAddAreInvalid() {
            resourcesToAdd.add("fire!fox");

            AgentInstance disabledAgentInstance = AgentInstanceMother.disabled();
            AgentConfig disabledAgent = disabledAgentInstance.agentConfig();
            disabledAgent.addResourceConfig(new ResourceConfig("linux"));

            when(agentInstances.findAgent(disabledAgent.getUuid())).thenReturn(disabledAgentInstance);

            uuids.add(disabledAgent.getUuid());
            assertThrows(IllegalArgumentException.class, () -> newAgentsUpdateValidator().validate());
            assertTrue(result.message().contains("Resource name 'fire!fox' is not valid"));
        }

        @Test
        public void shouldThrowExceptionWhenAgentsToBeUpdatedDoesNotExist() {
            String nonExistingUuid = "non-existing-uuid";
            uuids.add(nonExistingUuid);
            when(agentInstances.findAgent(nonExistingUuid)).thenReturn(new NullAgentInstance(nonExistingUuid));

            assertThrows(RecordNotFoundException.class, () -> newAgentsUpdateValidator().validate());
            assertTrue(result.message().equals(format("Agents with uuids '%s' were not found!", nonExistingUuid)));
        }

        @Test
        public void shouldThrowExceptionWhenElasticAgentResourcesAreBeingUpdated() {
            resourcesToAdd.add("Linux");
            AgentConfig elasticAgent = AgentMother.elasticAgent();
            uuids.add(elasticAgent.getUuid());
            when(agentInstances.findAgent(elasticAgent.getUuid()))
                               .thenReturn(AgentInstance.createFromConfig(elasticAgent, null, null));
            assertThrows(ElasticAgentsResourceUpdateException.class, () -> newAgentsUpdateValidator().validate());
            String errMsg = "Resources on elastic agents with uuids [" + elasticAgent.getUuid() + "] can not be updated.";
            assertTrue(result.message().contains(errMsg));
        }
    }

    //TODO Vrushali/Saurabh: Incorporate this test in Agent DB related tests.
//    @Test
//    public void shouldDoNothingWhenTryingToAddAgentToEnvironmentAlreadyAssociatedInConfigRepo() throws Exception {
//        environmentsToAdd.add("prod");
//        BasicEnvironmentConfig environmentConfig = new BasicEnvironmentConfig(str("prod"));
//        environmentConfig.setOrigins(new RepoConfigOrigin());
//
//        AgentInstance agentInstance = AgentInstanceMother.disabled();
//        AgentConfig agentConfig = agentInstance.agentConfig();
//
//        cruiseConfig.addEnvironment("prod");
//        cruiseConfig.agents().add(agentConfig);
//
//        uuids.add(agentConfig.getUuid());
//        environmentConfig.addAgent(agentConfig.getUuid());
//
//        assertThat(cruiseConfig.getEnvironments().find(new CaseInsensitiveString("prod")).getAgents().getUuids(), is(emptyList()));
//
//        when(agentInstances.findAgent(agentConfig.getUuid())).thenReturn(agentInstance);
//        when(environmentConfigService.getEnvironmentConfig("prod"))
//                .thenReturn(environmentConfig);
//
//        AgentsUpdateValidator command = newAgentsEntityConfigUpdateCommand();
//        command.update(cruiseConfig);
//
//        assertThat(cruiseConfig.getEnvironments().find(new CaseInsensitiveString("prod")).getAgents().getUuids(), is(emptyList()));
//    }

//    @Test
//    public void shouldUpdateConfigStateOfAgents() throws Exception {
//
//        AgentInstance agentInstance = AgentInstanceMother.disabled();
//        AgentConfig agentConfig = agentInstance.agentConfig();
//        agentConfig.disable();
//
//        cruiseConfig.agents().add(agentConfig);
//
//        assertThat(agentConfig.isEnabled(), is(false));
//
//        triState = TriState.TRUE;
//        uuids.add(agentConfig.getUuid());
//
//        when(agentInstances.findAgent(agentConfig.getUuid())).thenReturn(agentInstance);
//
//        AgentsUpdateValidator command = newAgentsEntityConfigUpdateCommand();
//        command.update(cruiseConfig);
//
//        assertThat(agentConfig.isEnabled(), is(true));
//    }

    private AgentsUpdateValidator newAgentsUpdateValidator() {
        EnvironmentsConfig envsConfig = new EnvironmentsConfig();
        environmentsToAdd.forEach(env -> envsConfig.add(new BasicEnvironmentConfig(new CaseInsensitiveString(env))));
        return new AgentsUpdateValidator(agentInstances, currentUser, result, uuids, envsConfig, environmentsToRemove,
                                         triState, resourcesToAdd, resourcesToRemove, goConfigService);
    }
}
