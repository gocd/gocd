/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.helper.GoConfigMother;
import org.junit.Test;

import static com.thoughtworks.go.util.TestUtils.contains;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class AgentConfigTest {

    @Test
    public void agentWithNoIpAddressShouldBeValid() throws Exception {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        AgentConfig agent = new AgentConfig("uuid", null, null);
        cruiseConfig.agents().add(agent);

        assertThat(cruiseConfig.validateAfterPreprocess().isEmpty(), is(true));
    }

    @Test
    public void shouldValidateIpCorrectIPv4Address() throws Exception {
        shouldBeValid("127.0.0.1");
        shouldBeValid("0.0.0.0");
        shouldBeValid("255.255.0.0");
        shouldBeValid("0:0:0:0:0:0:0:1");
    }

    @Test
    public void shouldValidateIpCorrectIPv6Address() throws Exception {
        shouldBeValid("0:0:0:0:0:0:0:1");
    }

    @Test
    public void shouldFailValidationIfIPAddressIsAString() throws Exception {
        AgentConfig agentConfig = new AgentConfig("uuid", "host", "blahinvalid");
        agentConfig.validate(ConfigSaveValidationContext.forChain(agentConfig));
        assertThat(agentConfig.errors().on(AgentConfig.IP_ADDRESS), is("'blahinvalid' is an invalid IP address."));
        agentConfig = new AgentConfig("uuid", "host", "blah.invalid");
        agentConfig.validate(ConfigSaveValidationContext.forChain(agentConfig));
        assertThat(agentConfig.errors().on(AgentConfig.IP_ADDRESS), is("'blah.invalid' is an invalid IP address."));
    }

    @Test
    public void shouldFailValidationIfIPAddressHasAnIncorrectValue() throws Exception {
        AgentConfig agentConfig = new AgentConfig("uuid", "host", "399.0.0.1");
        agentConfig.validate(ConfigSaveValidationContext.forChain(agentConfig));
        assertThat(agentConfig.errors().on(AgentConfig.IP_ADDRESS), is("'399.0.0.1' is an invalid IP address."));
    }

    @Test
    public void shouldInvalidateEmptyAddress() {
        AgentConfig agentConfig = new AgentConfig("uuid", "host", "");
        agentConfig.validate(ConfigSaveValidationContext.forChain(agentConfig));
        assertThat(agentConfig.errors().on(AgentConfig.IP_ADDRESS), is("IpAddress cannot be empty if it is present."));
    }

    @Test
    public void shouldValidateTree() {
        Resource resource = new Resource("junk%");
        AgentConfig agentConfig = new AgentConfig("uuid", "junk", "junk", new Resources(resource));
        boolean isValid = agentConfig.validateTree(ConfigSaveValidationContext.forChain(agentConfig));
        assertThat(agentConfig.errors().on(AgentConfig.IP_ADDRESS), is("'junk' is an invalid IP address."));
        assertThat(resource.errors().on(JobConfig.RESOURCES), contains("Resource name 'junk%' is not valid."));
        assertThat(isValid, is(false));
    }

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

    @Test
    public void shouldAllowResourcesOnNonElasticAgents() throws Exception {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("dev", "qa");
        AgentConfig agentConfig = new AgentConfig("uuid", "hostname", "10.10.10.10");
        cruiseConfig.agents().add(agentConfig);

        agentConfig.addResource(new Resource("foo"));
        assertThat(cruiseConfig.validateAfterPreprocess().isEmpty(), is(true));
    }

    @Test
    public void shouldNotAllowResourcesElasticAgents() throws Exception {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("dev", "qa");
        AgentConfig agentConfig = new AgentConfig("uuid", "hostname", "10.10.10.10");
        cruiseConfig.agents().add(agentConfig);

        agentConfig.setElasticPluginId("com.example.foo");
        agentConfig.setElasticAgentId("foobar");
        agentConfig.addResource(new Resource("foo"));
        assertThat(cruiseConfig.validateAfterPreprocess().isEmpty(), is(false));
        assertEquals(1, agentConfig.errors().size());
        assertThat(agentConfig.errors().on("elasticAgentId"), is("Elastic agents cannot have resources."));
    }

    private void shouldBeValid(String ipAddress) throws Exception {
        AgentConfig agentConfig = new AgentConfig();
        agentConfig.setIpAddress(ipAddress);
        agentConfig.validate(ConfigSaveValidationContext.forChain(agentConfig));
        assertThat(agentConfig.errors().on(AgentConfig.IP_ADDRESS), is(nullValue()));
    }
}
