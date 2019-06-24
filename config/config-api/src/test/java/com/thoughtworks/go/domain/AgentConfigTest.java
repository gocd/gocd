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

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.ResourceConfig;
import com.thoughtworks.go.config.ResourceConfigs;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;

public class AgentConfigTest {

    @Nested
    class IPAddress{
        @Test
        public void agentWithNoIpAddressShouldBeValid() {
            AgentConfig agent = new AgentConfig("uuid", null, null);

            agent.validate(null);
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
            AgentConfig agentConfig = new AgentConfig("uuid", "host", "blahinvalid");
            agentConfig.validate(ConfigSaveValidationContext.forChain(agentConfig));
            assertThat(agentConfig.errors().on(AgentConfig.IP_ADDRESS), is("'blahinvalid' is an invalid IP address."));

            agentConfig = new AgentConfig("uuid", "host", "blah.invalid");
            agentConfig.validate(ConfigSaveValidationContext.forChain(agentConfig));
            assertThat(agentConfig.errors().on(AgentConfig.IP_ADDRESS), is("'blah.invalid' is an invalid IP address."));
        }

        @Test
        public void shouldFailValidationIfIPAddressIsInvalid2() {
            AgentConfig agentConfig = new AgentConfig("uuid", "host", "399.0.0.1");
            agentConfig.validate(ConfigSaveValidationContext.forChain(agentConfig));
            assertThat(agentConfig.errors().on(AgentConfig.IP_ADDRESS), is("'399.0.0.1' is an invalid IP address."));
        }

        @Test
        public void shouldInvalidateEmptyIpAddress() {
            AgentConfig agentConfig = new AgentConfig("uuid", "host", "");
            agentConfig.validate(ConfigSaveValidationContext.forChain(agentConfig));
            assertThat(agentConfig.errors().on(AgentConfig.IP_ADDRESS), is("IpAddress cannot be empty if it is present."));
        }

        private void shouldBeValid(String ipAddress) {
            AgentConfig agentConfig = new AgentConfig();
            agentConfig.setIpaddress(ipAddress);
            agentConfig.validate(ConfigSaveValidationContext.forChain(agentConfig));
            assertThat(agentConfig.errors().on(AgentConfig.IP_ADDRESS), is(nullValue()));
        }
    }

    @Nested
    class Resources{
        @Test
        void shouldAddResourcesToExistingResources() {
            AgentConfig agent = new AgentConfig("uuid", "cookie", "host", "127.0.0.1");
            agent.setResources(new ResourceConfigs("resource1"));

            agent.addResources(Arrays.asList("resource2", "resource3"));

            assertThat(agent.getResources().size(), is(3));
            assertThat(agent.getResources().resourceNames(), is(Arrays.asList("resource1", "resource2", "resource3")));
        }

        @Test
        void shouldAddResourcesToIfThereAreNoExistingResources() {
            AgentConfig agent = new AgentConfig("uuid", "cookie", "host", "127.0.0.1");

            agent.addResources(Arrays.asList("resource2", "resource3"));

            assertThat(agent.getResources().size(), is(2));
            assertThat(agent.getResources().resourceNames(), is(Arrays.asList("resource2", "resource3")));
        }

        @Test
        void shouldRemoveResourcesFromExistingResources() {
            AgentConfig agent = new AgentConfig("uuid", "cookie", "host", "127.0.0.1");
            agent.setResources(new ResourceConfigs("resource1,resource2,resource3"));

            agent.removeResources(Arrays.asList("resource2"));

            assertThat(agent.getResources().size(), is(2));
            assertThat(agent.getResources().resourceNames(), is(Arrays.asList("resource1", "resource3")));
        }

        @Test
        void shouldNotRemoveResourcesIfDoNotExist() {
            AgentConfig agent = new AgentConfig("uuid", "cookie", "host", "127.0.0.1");

            agent.removeResources(Arrays.asList("resource2"));

            assertTrue(agent.getResources().resourceNames().isEmpty());
        }

        @Test
        public void shouldAllowResourcesOnNonElasticAgents() {
            AgentConfig agentConfig = new AgentConfig("uuid", "hostname", "10.10.10.10");

            agentConfig.addResourceConfig(new ResourceConfig("foo"));
            agentConfig.validate(null);
            assertThat(agentConfig.errors().isEmpty(), is(true));
        }

        @Test
        public void shouldNotAllowResourcesElasticAgents() {
            AgentConfig agentConfig = new AgentConfig("uuid", "hostname", "10.10.10.10");
            agentConfig.setElasticPluginId("com.example.foo");
            agentConfig.setElasticAgentId("foobar");
            agentConfig.addResourceConfig(new ResourceConfig("foo"));
            agentConfig.validate(null);

            assertEquals(1, agentConfig.errors().size());
            assertThat(agentConfig.errors().on("elasticAgentId"), is("Elastic agents cannot have resources."));
        }
    }

    @Nested
    class UUID{
        @Test
        public void shouldPassValidationWhenUUidIsAvailable() {
            AgentConfig agentConfig = new AgentConfig("uuid");
            agentConfig.validate(ConfigSaveValidationContext.forChain(agentConfig));
            assertThat(agentConfig.errors().on(AgentConfig.UUID), is(nullValue()));
        }

        @Test
        public void shouldFailValidationWhenUUidIsBlank() {
            AgentConfig agentConfig = new AgentConfig("");
            agentConfig.validate(ConfigSaveValidationContext.forChain(agentConfig));
            assertThat(agentConfig.errors().on(AgentConfig.UUID), is("UUID cannot be empty"));
            agentConfig = new AgentConfig(null);
            agentConfig.validate(ConfigSaveValidationContext.forChain(agentConfig));
            assertThat(agentConfig.errors().on(AgentConfig.UUID), is("UUID cannot be empty"));
        }
    }

    @Nested
    class Environments {
        @Test
        void shouldAddEnvironmentsToExistingEnvironments() {
            AgentConfig agent = new AgentConfig("uuid", "cookie", "host", "127.0.0.1");
            agent.setEnvironments("env1,env2");

            agent.addEnvironments(Arrays.asList("env2", "env3"));

            assertThat(agent.getEnvironments(), is("env1,env2,env3"));
        }

        @Test
        void shouldAddEnvironmentsIfNoExistingEnvironments() {
            AgentConfig agent = new AgentConfig("uuid", "cookie", "host", "127.0.0.1");

            agent.addEnvironments(Arrays.asList("env2", "env3"));

            assertThat(agent.getEnvironments(), is("env2,env3"));
        }

        @Test
        void shouldRemoveEnvironmentsFromExistingEnvironments() {
            AgentConfig agent = new AgentConfig("uuid", "cookie", "host", "127.0.0.1");
            agent.setEnvironments("env1,env2");

            agent.removeEnvironments(Arrays.asList("env1", "env3"));

            assertThat(agent.getEnvironments(), is("env2"));
        }

        @Test
        void shouldNotRemoveEnvironmentsIfEnvironmentsDoNotExist() {
            AgentConfig agent = new AgentConfig("uuid", "cookie", "host", "127.0.0.1");

            agent.removeEnvironments(Arrays.asList("env1", "env3"));

            assertNull(agent.getEnvironments());
        }
    }
}
