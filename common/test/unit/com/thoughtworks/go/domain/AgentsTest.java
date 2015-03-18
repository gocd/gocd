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

package com.thoughtworks.go.domain;

import java.util.Set;

import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import com.thoughtworks.go.config.AgentConfig;
import com.thoughtworks.go.config.Agents;
import org.junit.Test;

public class AgentsTest {

    @Test
    public void shouldFindAgentByUuid() {
        Agents agents = new Agents();
        agents.add(new AgentConfig("1", "localhost", "2"));
        assertThat(agents.getAgentByUuid("1").getHostname(), is("localhost"));
    }

    @Test public void shouldGiveAListOfUuids() throws Exception {
        Agents agents = new Agents();
        agents.add(new AgentConfig("1", "localhost", "2"));
        AgentConfig denied = new AgentConfig("2", "localhost", "2");
        denied.setDisabled(true);
        agents.add(denied);

        Set<String> uuids = agents.acceptedUuids();

        assertThat(uuids.size(), is(2));
        assertThat(uuids, hasItem("1"));
        assertThat(uuids, hasItem("2"));
    }
}
