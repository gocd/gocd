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

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.*;
import static org.junit.Assert.assertThat;

public class UpdateResourceCommandTest {

    @Test
    public void shouldAssociateListOfResourcesForAnAgent() throws Exception {
        String uuid = "uuid";
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.agents().add(new AgentConfig(uuid, "localhost", "8153"));
        UpdateResourceCommand command = new UpdateResourceCommand(uuid, "foo, foo, bar, zoo,   moo   ,    blah");

        command.update(cruiseConfig);

        Resources actualResources = cruiseConfig.agents().getAgentByUuid(uuid).getResources();
        assertThat(actualResources.size(), is(5));
        assertThat(actualResources, hasItem(new Resource("foo")));
        assertThat(actualResources, hasItem(new Resource("bar")));
        assertThat(actualResources, hasItem(new Resource("zoo")));
        assertThat(actualResources, hasItem(new Resource("moo")));
        assertThat(actualResources, hasItem(new Resource("blah")));
    }
}
