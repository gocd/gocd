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

package com.thoughtworks.go.server.service;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class AgentBuildingInfoTest {
    @Test
    public void shouldReturnThePipelineName() {
        AgentBuildingInfo agentBuildingInfo = new AgentBuildingInfo("buildInfo", "foo");
        assertThat(agentBuildingInfo.getPipelineName(), is("foo"));
    }

    @Test
    public void shouldReturnTheStageName() {
        AgentBuildingInfo agentBuildingInfo = new AgentBuildingInfo("buildInfo", "foo/1/bar");
        assertThat(agentBuildingInfo.getStageName(), is("bar"));
    }

    @Test
    public void shouldReturnTheJobName() {
        AgentBuildingInfo agentBuildingInfo = new AgentBuildingInfo("buildInfo", "foo/1/bar/3/job");
        assertThat(agentBuildingInfo.getJobName(), is("job"));
    }

    @Test
    public void shouldReturnNullTheJobName() {
        AgentBuildingInfo agentBuildingInfo = new AgentBuildingInfo("buildInfo", "foo");
        assertNull(agentBuildingInfo.getJobName());
    }

    @Test
    public void shouldReturnNullForStageName() {
        AgentBuildingInfo agentBuildingInfo = new AgentBuildingInfo("buildInfo", "foo");
        assertNull(agentBuildingInfo.getStageName());
    }

}