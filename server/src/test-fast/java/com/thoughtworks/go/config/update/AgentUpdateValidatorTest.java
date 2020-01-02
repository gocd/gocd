/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.exceptions.BadRequestException;
import com.thoughtworks.go.config.exceptions.InvalidPendingAgentOperationException;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.util.TriState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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
    private TriState state;

    @BeforeEach
    void setUp() {
        result = new HttpOperationResult();
        goConfigService = mock(GoConfigService.class);
        agentInstance = mock(AgentInstance.class);

        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        when(goConfigService.getEnvironments()).thenReturn(cruiseConfig.getEnvironments());
    }

    @Nested
    class Validations {
        @Test
        void shouldNotThrowValidationRelatedExceptionWhenAllSpecifiedAgentAttributesAreValid() {
            agentInstance = AgentInstanceMother.idle();
            state = TriState.TRUE;

            assertThatCode(() -> newAgentUpdateValidator().validate()).doesNotThrowAnyException();
            assertTrue(result.isSuccess());
        }
        @Test
        void shouldThrowExceptionWhenAnyOperationIsPerformedOnPendingAgent() {
            state = TriState.UNSET;
            agentInstance = AgentInstanceMother.pending();

            assertThatCode(() -> newAgentUpdateValidator().validate())
                    .isInstanceOf(BadRequestException.class)
                    .hasMessage("Pending agent [uuid4] must be explicitly enabled or disabled when performing any operation on it.");
        }

        @Test
        void shouldPassValidationWhenEnvironmentsSpecifiedDoesNotExistsInConfigXML() {
            agentInstance = AgentInstanceMother.disabled();
            assertThatCode(() -> newAgentUpdateValidator().validate()).doesNotThrowAnyException();
            assertTrue(result.isSuccess());
        }

        private AgentUpdateValidator newAgentUpdateValidator() {
            return new AgentUpdateValidator(agentInstance, state);
        }
    }
}