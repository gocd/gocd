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

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.BasicEnvironmentConfig;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.EnvironmentsConfig;
import com.thoughtworks.go.config.exceptions.InvalidPendingAgentOperationException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.util.TriState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentUpdateValidatorTest {
    private HttpOperationResult result;
    private GoConfigService goConfigService;
    private AgentInstance agentInstance;
    private EnvironmentsConfig environmentsConfig;
    private String resources;
    private TriState state;

    @BeforeEach
    void setUp() {
        result = new HttpOperationResult();
        goConfigService = mock(GoConfigService.class);
        agentInstance = mock(AgentInstance.class);

        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        when(goConfigService.getEnvironments()).thenReturn(cruiseConfig.getEnvironments());
    }

    private AgentUpdateValidator newAgentUpdateValidator() {
        return new AgentUpdateValidator(agentInstance, environmentsConfig, resources, state, result);
    }

    @Nested
    class Validations {
        @Test
        void shouldNotThrowExceptionIfTheInputsAreValid() {
            agentInstance = AgentInstanceMother.idle();
            state = TriState.TRUE;
            environmentsConfig = createEnvironmentsConfigWith("uat");

            assertThatCode(() -> newAgentUpdateValidator().validate())
                    .doesNotThrowAnyException();

            assertTrue(result.isSuccess());
        }

        @Test
        void shouldThrowExceptionWhenOpsArePerformedOnPendingAgents() {
            state = TriState.UNSET;
            agentInstance = AgentInstanceMother.pending();

            assertThatCode(() -> newAgentUpdateValidator().validate())
                    .isInstanceOf(InvalidPendingAgentOperationException.class)
                    .hasMessage("Invalid operation performed on pending agents: [uuid4]");

            assertEquals(400, result.httpCode());
            assertEquals(format("Pending agent [%s] must be explicitly enabled or disabled when performing any operation on it.", agentInstance.getUuid()), result.message());
        }

        @Test
        void shouldPassValidationWhenEnvironmentsSpecifiedDoesNotExistsInConfigXML() {
            environmentsConfig = createEnvironmentsConfigWith("prod", "dev");

            agentInstance = AgentInstanceMother.disabled();

            assertThatCode(() -> newAgentUpdateValidator().validate())
                    .doesNotThrowAnyException();

            assertTrue(result.isSuccess());
        }

        @Test
        void shouldBombIfEnvsSpecifiedAsBlank() {
            environmentsConfig = new EnvironmentsConfig();
            AgentUpdateValidator agentUpdateValidator = newAgentUpdateValidator();

            assertThatCode(() -> newAgentUpdateValidator().validate())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Environments are specified but they are blank.");

            assertEquals(400, result.httpCode());
            assertEquals("Environments are specified but they are blank.", result.message());
        }

        @Test
        void shouldBombIfResourcesAreSpecifiedAsBlank() {
            environmentsConfig = createEnvironmentsConfigWith("env");
            resources = "";

            assertThatCode(() -> newAgentUpdateValidator().validate())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Resources are specified but they are blank.");
            assertEquals(400, result.httpCode());
            assertEquals("Resources are specified but they are blank.", result.message());
        }
    }

    private EnvironmentsConfig createEnvironmentsConfigWith(String... envs) {
        EnvironmentsConfig envsConfig = new EnvironmentsConfig();
        Arrays.stream(envs).forEach(env -> envsConfig.add(new BasicEnvironmentConfig(new CaseInsensitiveString(env))));
        return envsConfig;
    }
}