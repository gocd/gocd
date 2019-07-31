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

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.exceptions.ElasticAgentsResourceUpdateException;
import com.thoughtworks.go.config.exceptions.InvalidPendingAgentOperationException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.helper.AgentMother;
import com.thoughtworks.go.server.domain.AgentInstances;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.TriState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.domain.AgentInstance.FilterBy.*;
import static java.lang.String.format;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgentsUpdateValidatorTest {

    private HttpLocalizedOperationResult result;
    private AgentInstances agentInstances;
    private List<String> uuids;
    private List<String> resourcesToAdd;
    private List<String> resourcesToRemove;
    private TriState triState;

    @BeforeEach
    public void setUp() throws Exception {
        result = new HttpLocalizedOperationResult();
        agentInstances = mock(AgentInstances.class);

        uuids = new ArrayList<>();
        resourcesToAdd = new ArrayList<>();
        resourcesToRemove = new ArrayList<>();
        triState = TriState.TRUE;
    }

    @Nested
    class Validations {
        @Test
        void shouldNotThrowExceptionIfTheInputsAreValid() {
            String uuid = "uuid";
            uuids.add(uuid);
            resourcesToAdd.add("resource1");
            resourcesToRemove.add("resource2");

            when(agentInstances.findAgent(uuid)).thenReturn(mock(AgentInstance.class));

            assertThatCode(() -> newAgentsUpdateValidator().validate())
                    .doesNotThrowAnyException();

            assertTrue(result.isSuccessful());
        }

        @Test
        public void shouldThrowExceptionWhenOpsArePerformedOnPendingAgents() {
            triState = TriState.UNSET;
            AgentInstance pendingAgent = AgentInstanceMother.pending();
            uuids.add(pendingAgent.getUuid());

            when(agentInstances.filterBy(uuids, Pending)).thenReturn(singletonList(pendingAgent.getUuid()));

            assertThatCode(() -> newAgentsUpdateValidator().validate())
                    .isInstanceOf(InvalidPendingAgentOperationException.class)
                    .hasMessage("Invalid operation performed on pending agents: [uuid4]");

            assertEquals(400, result.httpCode());
            assertEquals("Pending agents [uuid4] must be explicitly enabled or disabled when performing any operations on them.", result.message());
        }

        @Test
        public void shouldThrowExceptionWhenResourceNamesToAddAreInvalid() {
            resourcesToAdd.add("fire!fox");

            AgentInstance disabledAgentInstance = AgentInstanceMother.disabled();
            Agent disabledAgent = disabledAgentInstance.getAgent();
            disabledAgent.addResource("linux");

            when(agentInstances.findAgent(disabledAgent.getUuid())).thenReturn(disabledAgentInstance);
            uuids.add(disabledAgent.getUuid());

            assertThatCode(() -> newAgentsUpdateValidator().validate())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("{resources=[Resource name 'fire!fox' is not valid. Valid names much match '^[-\\w\\s|.]*$']}");
            assertTrue(result.message().contains("Resource name 'fire!fox' is not valid"));
        }

        @Test
        public void shouldThrowExceptionWhenAgentsToBeUpdatedDoesNotExist() {
            String nonExistingUuid = "non-existing-uuid";
            uuids.add(nonExistingUuid);

            when(agentInstances.filterBy(uuids, Null)).thenReturn(singletonList(nonExistingUuid));

            assertThatCode(() -> newAgentsUpdateValidator().validate())
                    .isInstanceOf(RecordNotFoundException.class)
                    .hasMessage("Agents with uuids 'non-existing-uuid' were not found!");

            assertEquals(400, result.httpCode());
            assertEquals(format("Agents with uuids '%s' were not found!", nonExistingUuid), result.message());
        }

        @Test
        public void shouldThrowExceptionWhenElasticAgentResourcesAreBeingUpdated() {
            resourcesToAdd.add("Linux");
            Agent elasticAgent = AgentMother.elasticAgent();
            uuids.add(elasticAgent.getUuid());

            when(agentInstances.filterBy(uuids, Elastic)).thenReturn(singletonList(elasticAgent.getUuid()));
            String errMsg = "Resources on elastic agents with uuids [" + elasticAgent.getUuid() + "] can not be updated.";

            assertThatCode(() -> newAgentsUpdateValidator().validate())
                    .isInstanceOf(ElasticAgentsResourceUpdateException.class)
                    .hasMessage(format("Can not update resources on Elastic Agents [%s]", elasticAgent.getUuid()));
            assertEquals(400, result.httpCode());
            assertEquals(errMsg, result.message());
        }
    }

    private AgentsUpdateValidator newAgentsUpdateValidator() {
        return new AgentsUpdateValidator(agentInstances, uuids, triState, resourcesToAdd, resourcesToRemove, result);
    }
}
