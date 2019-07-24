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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

public class AgentTest {
    @Test
    void shouldCopyAllFieldsFromAnotherAgentObjectUsingCopyConstructor() {
        Agent origAgent = new Agent("uuid", "host", "127.0.0.1", "cookie");
        origAgent.addResources(asList("Resource1", "Resource2"));
        origAgent.addEnvironments(asList("dev", "test"));

        Agent copiedAgent = new Agent(origAgent);
        assertEquals(origAgent, copiedAgent);
    }

    @Nested
    class IPAddress{
        @Test
        public void agentWithNoIpAddressShouldBeValid() {
            Agent agent = new Agent("uuid", null, null);

            agent.validate();
            assertFalse(agent.hasErrors());
        }

        @Test
        public void shouldValidateIpCorrectIPv4Address() {
            shouldBeValid("127.0.0.1");
            shouldBeValid("0.0.0.0");
            shouldBeValid("255.255.0.0");
            shouldBeValid("0:0:0:0:0:0:0:1");
        }

        @Test
        public void shouldValidateIpCorrectIPv6Address() {
            shouldBeValid("0:0:0:0:0:0:0:1");
        }

        @Test
        public void shouldFailValidationIfIPAddressIsInvalid1() {
            Agent agent = new Agent("uuid", "host", "blahinvalid");
            agent.validate();
            assertThat(agent.errors().on(Agent.IP_ADDRESS), is("'blahinvalid' is an invalid IP address."));

            agent = new Agent("uuid", "host", "blah.invalid");
            agent.validate();
            assertThat(agent.errors().on(Agent.IP_ADDRESS), is("'blah.invalid' is an invalid IP address."));
        }

        @Test
        public void shouldFailValidationIfIPAddressIsInvalid2() {
            Agent agent = new Agent("uuid", "host", "399.0.0.1");
            agent.validate();
            assertThat(agent.errors().on(Agent.IP_ADDRESS), is("'399.0.0.1' is an invalid IP address."));
        }

        @Test
        public void shouldInvalidateEmptyIpAddress() {
            Agent agent = new Agent("uuid", "host", "");
            agent.validate();
            assertThat(agent.errors().on(Agent.IP_ADDRESS), is("IpAddress cannot be empty if it is present."));
        }

        private void shouldBeValid(String ipAddress) {
            Agent agent = new Agent("some-dummy-uuid");
            agent.setIpaddress(ipAddress);
            agent.validate();
            assertThat(agent.errors().on(Agent.IP_ADDRESS), is(nullValue()));
        }
    }

    @Nested
    class Resources{
        @Test
        void shouldAddResourcesToExistingResources() {
            Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
            agent.setResources("resource1");

            agent.addResources(asList("resource2", "resource3"));

            assertThat(agent.getResources().size(), is(3));
            assertThat(agent.getResources().resourceNames(), is(asList("resource1", "resource2", "resource3")));
        }

        @Test
        void shouldAddResourcesToIfThereAreNoExistingResources() {
            Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");

            agent.addResources(asList("resource2", "resource3"));

            assertThat(agent.getResources().size(), is(2));
            assertThat(agent.getResources().resourceNames(), is(asList("resource2", "resource3")));
        }

        @Test
        void shouldRemoveResourcesFromExistingResources() {
            Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
            agent.setResources("resource1,resource2,resource3");

            agent.removeResources(asList("resource2"));

            assertThat(agent.getResources().size(), is(2));
            assertThat(agent.getResources().resourceNames(), is(asList("resource1", "resource3")));
        }

        @Test
        void shouldNotRemoveResourcesIfDoNotExist() {
            Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");

            agent.removeResources(asList("resource2"));

            assertTrue(agent.getResources().resourceNames().isEmpty());
        }

        @Test
        public void shouldAllowResourcesOnNonElasticAgents() {
            Agent agent = new Agent("uuid", "hostname", "10.10.10.10");

            agent.setResources("foo");
            agent.validate();
            assertThat(agent.errors().isEmpty(), is(true));
        }

        @Test
        public void shouldNotAllowResourcesElasticAgents() {
            Agent agent = new Agent("uuid", "hostname", "10.10.10.10");
            agent.setElasticPluginId("com.example.foo");
            agent.setElasticAgentId("foobar");
            agent.setResources("foo");
            agent.validate();

            assertEquals(1, agent.errors().size());
            assertThat(agent.errors().on("elasticAgentId"), is("Elastic agents cannot have resources."));
        }
    }

    @Nested
    class UUID{
        @Test
        public void shouldPassValidationWhenUUidIsAvailable() {
            Agent agent = new Agent("uuid");
            agent.validate();
            assertThat(agent.errors().on(Agent.UUID), is(nullValue()));
        }

        @Test
        public void shouldFailValidationWhenUUidIsBlank() {
            Agent agent = new Agent("");
            agent.validate();
            assertThat(agent.errors().on(Agent.UUID), is("UUID cannot be empty"));
            agent = new Agent("");
            agent.validate();
            assertThat(agent.errors().on(Agent.UUID), is("UUID cannot be empty"));
        }
    }

    @Nested
    class Environments {
        @Test
        void shouldAddEnvironmentsToExistingEnvironments() {
            Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
            agent.setEnvironments("env1,env2");

            agent.addEnvironments(asList("env2", "env3"));

            assertThat(agent.getEnvironments(), is("env1,env2,env3"));
        }

        @Test
        void shouldAddEnvironmentsIfNoExistingEnvironments() {
            Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");

            agent.addEnvironments(asList("env2", "env3"));

            assertThat(agent.getEnvironments(), is("env2,env3"));
        }

        @Test
        void shouldRemoveEnvironmentsFromExistingEnvironments() {
            Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");
            agent.setEnvironments("env1,env2");

            agent.removeEnvironments(asList("env1", "env3"));

            assertThat(agent.getEnvironments(), is("env2"));
        }

        @Test
        void shouldNotRemoveEnvironmentsIfEnvironmentsDoNotExist() {
            Agent agent = new Agent("uuid", "cookie", "host", "127.0.0.1");

            agent.removeEnvironments(asList("env1", "env3"));

            assertNull(agent.getEnvironments());
        }
    }
}
