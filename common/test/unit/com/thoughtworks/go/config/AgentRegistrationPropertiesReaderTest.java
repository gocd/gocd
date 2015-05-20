/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AgentRegistrationPropertiesReaderTest {

    private Properties properties;

    @Before
    public void setUp() {
        properties = new Properties();
        properties.put(AgentRegistrationPropertiesReader.AGENT_AUTO_REGISTER_KEY, "foo");
        properties.put(AgentRegistrationPropertiesReader.AGENT_AUTO_REGISTER_RESOURCES, "foo, zoo");
        properties.put(AgentRegistrationPropertiesReader.AGENT_AUTO_REGISTER_ENVIRONMENTS, "foo, bar");
        properties.put(AgentRegistrationPropertiesReader.AGENT_AUTO_REGISTER_HOSTNAME, "agent01.example.com");
    }

    @Test
    public void shouldReturnAgentAutoRegisterPropertiesIfPresent() {
        AgentRegistrationPropertiesReader reader = new AgentRegistrationPropertiesReader(properties);
        assertThat(reader.getAgentAutoRegisterKey(), is("foo"));
        assertThat(reader.getAgentAutoRegisterResources(), is("foo, zoo"));
        assertThat(reader.getAgentAutoRegisterEnvironments(), is("foo, bar"));
        assertThat(reader.getAgentAutoRegisterHostname(), is("agent01.example.com"));
    }

    @Test
    public void shouldReturnEmptyStringIfPropertiesNotPresent() {
        Properties localProperties = new Properties();
        AgentRegistrationPropertiesReader reader = new AgentRegistrationPropertiesReader(localProperties);
        assertThat(reader.getAgentAutoRegisterKey().isEmpty(), is(true));
        assertThat(reader.getAgentAutoRegisterResources().isEmpty(), is(true));
        assertThat(reader.getAgentAutoRegisterEnvironments().isEmpty(), is(true));
        assertThat(reader.getAgentAutoRegisterHostname().isEmpty(), is(true));
    }
}
