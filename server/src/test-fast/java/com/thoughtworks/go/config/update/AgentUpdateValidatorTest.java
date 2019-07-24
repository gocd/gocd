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
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.util.TriState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentUpdateValidatorTest {
    private HttpOperationResult result;
    private Username currentUser;
    private GoConfigService goConfigService;
    private AgentInstance agentInstance;
    private String hostName;
    private EnvironmentsConfig environmentsConfig;
    private String resources;
    private TriState state;

    @BeforeEach
    void setUp() {
        result = new HttpOperationResult();
        currentUser = new Username(new CaseInsensitiveString("user"));
        goConfigService = mock(GoConfigService.class);
        agentInstance = mock(AgentInstance.class);

        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        when(goConfigService.getEnvironments()).thenReturn(cruiseConfig.getEnvironments());
    }

    private AgentUpdateValidator newAgentUpdateValidator() {
        EnvironmentsConfig envsConfig = new EnvironmentsConfig();
        return new AgentUpdateValidator(currentUser, agentInstance, hostName, environmentsConfig, resources, state, result, goConfigService);
    }

    @Nested
    class CanContinue {
        @Test
        public void shouldAllowAdministratorToPerformAnyOperationOnAgents() {
            when(goConfigService.isAdministrator(currentUser.getUsername())).thenReturn(true);
            state = TriState.TRUE;
            assertTrue(newAgentUpdateValidator().canContinue());
        }

        @Test
        public void shouldThrow400WhenNoOperationIsPerformedOnAgents() {
            state = TriState.UNSET;
            AgentUpdateValidator validator = newAgentUpdateValidator();

            when(goConfigService.isAdministrator(currentUser.getUsername())).thenReturn(true);

            assertFalse(validator.canContinue());
            assertThat(result.httpCode()).isEqualTo(400);
            assertThat(result.message()).isEqualTo("No Operation performed on agent.");
        }

        @Test
        public void shouldThrow403WhenNonAdminUserIsUpdatingAgents() {
            AgentUpdateValidator validator = newAgentUpdateValidator();
            when(goConfigService.isAdministrator(currentUser.getUsername())).thenReturn(false);
            assertFalse(validator.canContinue());

            assertThat(result.httpCode()).isEqualTo(403);
            assertThat(result.message()).isEqualTo("Unauthorized to edit.");
        }
    }

    @Nested
    class Validations {
        @Test
        public void shouldThrowExceptionWhenAgentsToBeUpdatedDoesNotExist() {
            agentInstance = AgentInstanceMother.nullInstance();

            assertThrows(RecordNotFoundException.class, () -> newAgentUpdateValidator().validate());
            assertEquals(result.message(), format("Agent '%s' not found.", agentInstance.getUuid()));
        }

        @Test
        public void shouldThrowExceptionWhenOpsArePerformedOnPendingAgents() {
            state = TriState.UNSET;
            agentInstance = AgentInstanceMother.pending();

            assertThrows(InvalidPendingAgentOperationException.class, () -> newAgentUpdateValidator().validate());
        }

        @Test
        public void shouldPassValidationWhenEnvironmentsSpecifiedDoesNotExistsInConfigXML() {
            environmentsConfig = createEnvironmentsConfigWith("prod", "dev");

            agentInstance = AgentInstanceMother.disabled();

            assertDoesNotThrow(() -> newAgentUpdateValidator().validate());
        }

        @Test
        public void shouldThrowExceptionWhenElasticAgentResourcesAreBeingUpdated() {
            resources = "Linux";
            agentInstance = AgentInstanceMother.building();
            Agent agent = agentInstance.getAgent();
            agent.setElasticAgentId("elastic-agent-id");
            agent.setElasticPluginId("elastic-plugin-id");
            agentInstance.syncConfig(agent);

            assertThrows(ElasticAgentsResourceUpdateException.class, () -> newAgentUpdateValidator().validate());
            String errMsg = "Resources on elastic agent with uuid [" + agentInstance.getUuid() + "] can not be updated.";
            assertEquals(result.message(), errMsg);
        }
    }

    private EnvironmentsConfig createEnvironmentsConfigWith(String... envs) {
        EnvironmentsConfig envsConfig = new EnvironmentsConfig();
        Arrays.stream(envs).forEach(env -> envsConfig.add(new BasicEnvironmentConfig(new CaseInsensitiveString(env))));
        return envsConfig;
    }
}