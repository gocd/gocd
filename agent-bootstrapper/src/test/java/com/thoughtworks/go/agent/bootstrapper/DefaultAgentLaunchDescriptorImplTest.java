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
package com.thoughtworks.go.agent.bootstrapper;

import com.thoughtworks.go.agent.common.AgentBootstrapperArgs;
import com.thoughtworks.go.util.GoConstants;
import org.junit.jupiter.api.Test;

import java.net.URL;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultAgentLaunchDescriptorImplTest {

    @Test
    public void contextShouldContainEnvAndPropertiesAndHostAndPort() throws Exception {
        String hostname = "xx.xx.xx";
        int port = 20;
        AgentBootstrapperArgs bootstrapperArgs = new AgentBootstrapperArgs().setServerUrl(new URL("https://" + hostname + ":" + port + "/go")).setRootCertFile(null).setSslVerificationMode(AgentBootstrapperArgs.SslMode.NONE);
        DefaultAgentLaunchDescriptorImpl launchDescriptor = new DefaultAgentLaunchDescriptorImpl(bootstrapperArgs, new AgentBootstrapper());
        Map context = launchDescriptor.context();

        assertContainsAll(bootstrapperArgs.toProperties(), context);
    }

    @Test
    public void contextShouldContainBootstrapperVersionInformation() throws Exception {
        AgentBootstrapper bootstrapper = mock(AgentBootstrapper.class);
        when(bootstrapper.version()).thenReturn("1.2.3-1234");

        DefaultAgentLaunchDescriptorImpl launchDescriptor = new DefaultAgentLaunchDescriptorImpl(new AgentBootstrapperArgs().setServerUrl(new URL("https://www.example.com")), bootstrapper);
        Map context = launchDescriptor.context();

        assertEquals("1.2.3-1234", context.get(GoConstants.AGENT_BOOTSTRAPPER_VERSION));
    }

    private void assertContainsAll(Map<String, String> expected, Map actual) {
        for (Map.Entry<String, String> keyValuePair : expected.entrySet()) {
            String key = keyValuePair.getKey();
            assertEquals(actual.get(key), expected.get(key));
        }
    }
}
