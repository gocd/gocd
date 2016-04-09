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
package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.util.TestingClock;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.TimeZone;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class BuildVariablesTest {

    private BuildVariables bvs;

    @Before
    public void setup() {
        AgentIdentifier agentIdentifier = new AgentIdentifier("duloc", "127.0.0.1", "uuid");
        AgentRuntimeInfo runtimeInfo = new AgentRuntimeInfo(agentIdentifier, AgentRuntimeStatus.Idle, "/home/lord-farquaad/builds", "cookie", null, false);

        bvs = new BuildVariables(runtimeInfo, new TestingClock(new Date(0)));
    }

    @Test
    public void lookupCurrentDate() {
        TimeZone oldDefault = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        try {
            assertThat(bvs.lookup("date"), is("1970-01-01 00:00:00 GMT"));
        } finally {
            TimeZone.setDefault(oldDefault);
        }
    }

    @Test
    public void lookupAgentLocation() {
        assertThat(bvs.lookup("agent.location"), is("/home/lord-farquaad/builds"));
    }

    @Test
    public void lookupAgentHostname() {
        assertThat(bvs.lookup("agent.hostname"), is("duloc"));
    }

    @Test
    public void lookupCrazyThingShouldReturnNull() {
        assertThat(bvs.lookup("questiontoanswer42"), is(nullValue()));
    }
}