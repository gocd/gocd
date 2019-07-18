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
package com.thoughtworks.go.config;

import org.junit.Test;

import java.util.Set;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

public class AgentsTest {

    @Test
    public void shouldFindAgentByUuid() {
        Agents agents = new Agents();
        agents.add(new Agent("1", "localhost", "2"));
        assertThat(agents.getAgentByUuid("1").getHostname(), is("localhost"));
    }

    @Test
    public void shouldGiveAListOfUuids() throws Exception {
        Agents agents = new Agents();
        agents.add(new Agent("1", "localhost", "2"));
        Agent denied = new Agent("2", "localhost", "2");
        denied.setDisabled(true);
        agents.add(denied);

        Set<String> uuids = agents.acceptedUuids();

        assertThat(uuids.size(), is(2));
        assertThat(uuids, hasItem("1"));
        assertThat(uuids, hasItem("2"));
    }

    @Test
    public void shouldValidateDuplicateElasticAgentId() throws Exception {
        Agents agents = new Agents();

        Agent elasticAgent1 = new Agent("1", "localhost", "1");
        elasticAgent1.setElasticAgentId("elastic-agent-id");
        elasticAgent1.setElasticPluginId("awesome-elastic-agent");

        Agent elasticAgent2 = new Agent("2", "localhost", "2");
        elasticAgent2.setElasticAgentId("elastic-agent-id");
        elasticAgent2.setElasticPluginId("awesome-elastic-agent");

        agents.add(elasticAgent1);
        agents.add(elasticAgent2);

        agents.validate(new ConfigSaveValidationContext(agents));

        assertThat(elasticAgent1.errors().size(), is(1));
        assertThat(elasticAgent1.errors().getAllOn("elasticAgentId").get(0), is("Duplicate ElasticAgentId found for agents [1, 2]"));
        assertThat(elasticAgent2.errors().size(), is(1));
        assertThat(elasticAgent2.errors().getAllOn("elasticAgentId").get(0), is("Duplicate ElasticAgentId found for agents [1, 2]"));
    }
}
