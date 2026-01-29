/*
 * Copyright Thoughtworks, Inc.
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
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultAgentLaunchDescriptorImplTest {

    @Test
    public void contextShouldContainEnvAndPropertiesAndHostAndPort() throws Exception {
        String hostname = "xx.xx.xx";
        int port = 20;
        AgentBootstrapperArgs bootstrapperArgs = new AgentBootstrapperArgs().setServerUrl(URI.create("https://" + hostname + ":" + port + "/go").toURL()).setRootCertFile(null).setSslVerificationMode(AgentBootstrapperArgs.SslMode.NONE);
        DefaultAgentLaunchDescriptorImpl launchDescriptor = new DefaultAgentLaunchDescriptorImpl(bootstrapperArgs, new AgentBootstrapper());
        Map<String, String> context = launchDescriptor.context();

        assertThat(context).containsAllEntriesOf(bootstrapperArgs.toProperties());
    }

    @Test
    public void contextShouldContainBootstrapperVersionInformation() throws Exception {
        AgentBootstrapper bootstrapper = mock(AgentBootstrapper.class);
        when(bootstrapper.version()).thenReturn("1.2.3-1234");

        DefaultAgentLaunchDescriptorImpl launchDescriptor = new DefaultAgentLaunchDescriptorImpl(new AgentBootstrapperArgs().setServerUrl(URI.create("https://www.example.com").toURL()), bootstrapper);
        Map<String, String> context = launchDescriptor.context();

        assertThat(context.get(SystemEnvironment.AGENT_BOOTSTRAPPER_VERSION)).isEqualTo("1.2.3-1234");
    }
}
