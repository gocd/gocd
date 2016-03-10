/*************************** GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.websocket;

import com.thoughtworks.go.domain.AgentRuntimeStatus;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ReportTest {

    @Test
    public void encodeAndDecodeAsMessageData() throws Exception {
        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("HostName", "ipAddress", "uuid"), AgentRuntimeStatus.Idle, null, null, null, true);
        JobIdentifier jobIdentifier = new JobIdentifier("pipeline", 1, "pipelinelabel", "stagename", "1", "job", 1L);
        Report report = new Report(info, jobIdentifier, JobResult.Passed);
        assertThat(MessageEncoding.decodeData(MessageEncoding.encodeData(report), Report.class), is(report));
    }
}