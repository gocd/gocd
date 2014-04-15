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

package com.thoughtworks.go.agent.bootstrapper;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.thoughtworks.cruise.agent.common.launcher.AgentLaunchDescriptorKeys;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DefaultAgentLaunchDescriptorImplTest {

    @Test
    public void contextShouldContainEnvAndPropertiesAndHostAndPort() throws Exception {
        String hostname = "xx.xx.xx";
        int port = 20;
        DefaultAgentLaunchDescriptorImpl launchDescriptor = new DefaultAgentLaunchDescriptorImpl(hostname, port, new AgentBootstrapper());
        Map context = launchDescriptor.context();
        assertThat((String) context.get(AgentLaunchDescriptorKeys.HOSTNAME), is(hostname));
        assertThat((Integer) context.get(AgentLaunchDescriptorKeys.PORT), is(port));
        Set<String> envkeys = new HashSet<String>(System.getenv().keySet());
        envkeys.removeAll(context.keySet());
        assertThat(envkeys.isEmpty(), is(true));
        Set<Object> propkeys = new HashSet<Object>(System.getProperties().keySet());
        propkeys.removeAll(context.keySet());
        assertThat(propkeys.isEmpty(), is(true));
    }
}
