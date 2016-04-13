/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import org.junit.Test;

import java.net.URL;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DefaultAgentLaunchDescriptorImplTest {

    @Test
    public void contextShouldContainEnvAndPropertiesAndHostAndPort() throws Exception {
        String hostname = "xx.xx.xx";
        int port = 20;
        AgentBootstrapperArgs bootstrapperArgs = new AgentBootstrapperArgs(new URL("https://" + hostname + ":" + port + "/go"), null, AgentBootstrapperArgs.SslMode.NONE);
        DefaultAgentLaunchDescriptorImpl launchDescriptor = new DefaultAgentLaunchDescriptorImpl(bootstrapperArgs, new AgentBootstrapper());
        Map context = launchDescriptor.context();

        assertEquals(context, bootstrapperArgs.toProperties());
    }
}
