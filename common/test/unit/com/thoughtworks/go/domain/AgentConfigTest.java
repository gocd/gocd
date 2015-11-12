/*
 * Copyright 2015 ThoughtWorks, Inc.
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
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AgentConfigTest {

    private CruiseConfig cruiseConfig;
    private AgentConfig agentConfig;

    @Before
    public void setUp() {
        cruiseConfig = GoConfigMother.configWithPipelines("dev", "qa");
        agentConfig = new AgentConfig("uuid", "hostname", "10.10.10.10");
        cruiseConfig.agents().add(agentConfig);
    }

    @Test
    public void agentWithNoIpAddressShouldBeValid() throws Exception {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        AgentConfig agent = new AgentConfig();
        cruiseConfig.agents().add(agent);

        assertThat(cruiseConfig.validateAfterPreprocess().isEmpty(), is(true));
    }

    @Test
    public void shouldValidateIpV4AndIpV6() throws Exception {
        shouldBeValid("127.0.0.1");
        shouldBeValid("0.0.0.0");
        shouldBeValid("255.255.0.0");
        shouldBeValid("0:0:0:0:0:0:0:1");
    }

    @Test
    public void shouldDetectInvalidIPAddress() throws Exception {
        shouldBeInvalid("blahinvalid", "IpAddress is invalid.");
        shouldBeInvalid("blah.invalid", "IpAddress is invalid.");
        shouldBeInvalid("399.0.0.1", "IpAddress is invalid.");
    }

    @Test
    public void shouldInvalidateEmptyAddress() {
        shouldBeInvalid("", "IpAddress cannot be empty if it is present.");
    }

    private void shouldBeInvalid(String address, String errorMsg) {
        AgentConfig agentConfig = new AgentConfig();
        agentConfig.setIpAddress(address);
        agentConfig.validate(ConfigSaveValidationContext.forChain(cruiseConfig));
        assertThat(agentConfig.errors().on("ipAddress"), is(errorMsg));
    }

    private void shouldBeValid(String ipAddress) throws Exception {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        AgentConfig agentConfig = new AgentConfig();
        agentConfig.setIpAddress(ipAddress);
        cruiseConfig.agents().add(agentConfig);
        agentConfig.validate(ConfigSaveValidationContext.forChain(cruiseConfig));
        assertThat(agentConfig.errors().isEmpty(), is(true));
    }

}
