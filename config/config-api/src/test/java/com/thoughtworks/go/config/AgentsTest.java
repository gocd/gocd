/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.NullAgent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentsTest {
    @Nested
    class GetAgent {
        @Test
        void getAgentByUUIDShouldReturnValidAgentForExistingUUID() {
            Agents agents = new Agents();
            agents.add(new Agent("1", "localhost", "2"));

            assertThat(agents.getAgentByUUID("1").getHostname(), is("localhost"));
            assertThat(agents.getAgentByUUID("1").getIpaddress(), is("2"));
        }

        @Test
        void getAgentByUUIDShouldReturnNullForUUIDThatDoesNotExist() {
            Agents agents = new Agents();
            agents.add(new Agent("1", "localhost", "2"));
            assertTrue(agents.getAgentByUUID("uuid-that-does-not-exist") instanceof NullAgent);
            assertThat(agents.getAgentByUUID("uuid-that-does-not-exist").isNull(), is(true));
        }
    }

    @Nested
    class HasAgent {
        @Test
        void hasAgentShouldReturnTrueForExistingUUID() {
            Agents agents = new Agents();
            agents.add(new Agent("1", "localhost", "2"));
            assertThat(agents.hasAgent("1"), is(true));
        }

        @Test
        void hasAgentShouldReturnFalseForUUIDThatDoesNotExist() {
            Agents agents = new Agents();
            agents.add(new Agent("1", "localhost", "2"));
            assertThat(agents.hasAgent("uuid-that-does-not-exist"), is(false));
        }
    }

    @Nested
    class AddAgent {
        @Test
        void addShouldAddAgentToTheListOfAgents() {
            Agents agents = new Agents();

            Agent agent1 = new Agent("1", "localhost1", "1");
            Agent agent2 = new Agent("2", "localhost2", "2");
            agents.add(agent1);
            agents.add(agent2);

            assertThat(agents.getAgentByUUID("1"), is(equalTo(agent1)));
            assertThat(agents.getAgentByUUID("2"), is(equalTo(agent2)));
        }

        @Test
        void addShouldNotAddNullToTheListOfAgents() {
            Agents agents = new Agents();
            Agent agent = new Agent("1", "localhost2", "2");
            agents.add(null);
            agents.add(null);
            agents.add(null);
            agents.add(agent);

            assertThat(agents.size(), is(1));
            assertThat(agents.getAgentByUUID("1"), is(equalTo(agent)));
        }
    }
}
