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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class UpdateResourceCommandTest {

    @Test
    public void shouldAssociateListOfResourcesForAnAgent() throws Exception {
        String uuid = "uuid";
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.agents().add(new AgentConfig(uuid, "localhost", "8153"));
        UpdateResourceCommand command = new UpdateResourceCommand(uuid, "foo, foo, bar, zoo,   moo   ,    blah");

        command.update(cruiseConfig);

        ResourceConfigs actualResourceConfigs = cruiseConfig.agents().getAgentByUuid(uuid).getResourceConfigs();
        assertThat(actualResourceConfigs.size(), is(5));
        assertThat(actualResourceConfigs, hasItem(new ResourceConfig("foo")));
        assertThat(actualResourceConfigs, hasItem(new ResourceConfig("bar")));
        assertThat(actualResourceConfigs, hasItem(new ResourceConfig("zoo")));
        assertThat(actualResourceConfigs, hasItem(new ResourceConfig("moo")));
        assertThat(actualResourceConfigs, hasItem(new ResourceConfig("blah")));
    }
}
