/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.ui.helper;

import java.util.HashSet;

import com.thoughtworks.go.config.ResourceConfig;
import com.thoughtworks.go.domain.AgentInstance;
import com.thoughtworks.go.helper.AgentInstanceMother;
import com.thoughtworks.go.server.ui.AgentViewModel;
import com.thoughtworks.go.server.ui.AgentsViewModel;

public class AgentsViewModelMother {
    public static AgentsViewModel getTwoAgents() {
        AgentInstance building = AgentInstanceMother.building();
        building.getResourceConfigs().add(new ResourceConfig("ruby"));

        AgentInstance idle = AgentInstanceMother.idle();

        HashSet<String> environments = new HashSet<>();
        environments.add("hello");
        environments.add("yellow");
        return new AgentsViewModel(new AgentViewModel(idle, environments), new AgentViewModel(building));
    }

    public static AgentsViewModel getZeroAgents() {
        return new AgentsViewModel();
    }
}
