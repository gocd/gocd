/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.websocket;

import com.thoughtworks.go.config.ArtifactPlans;
import com.thoughtworks.go.config.ArtifactPropertiesGenerators;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.Resources;
import com.thoughtworks.go.domain.DefaultJobPlan;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.builder.CommandBuilder;
import com.thoughtworks.go.domain.builder.NullBuilder;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.remote.work.BuildWork;
import com.thoughtworks.go.remote.work.Work;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import com.thoughtworks.go.work.FakeWork;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class MessageTest {

    @Test
    public void encodeAndDecode() {
        AgentRuntimeInfo info = AgentRuntimeInfo.fromAgent(new AgentIdentifier("hostName", "ipAddress", "uuid"));
        String msg = Message.encode(new Message(Action.ping, info));
        Message decoded = Message.decode(msg);

        assertThat(((AgentRuntimeInfo) decoded.getData()).getIdentifier(), is(info.getIdentifier()));
    }

    @Test
    public void encodeAndDecodeAssignWork() throws Exception {
        File workingDir = new File(CruiseConfig.WORKING_BASE_DIR + "pipelineName");
        MaterialRevisions revisions = ModificationsMother.modifyOneFile(MaterialsMother.defaultMaterials(), ModificationsMother.nextRevision());
        BuildCause buildCause = BuildCause.createWithModifications(revisions, "");

        List<Builder> builder = new ArrayList<Builder>();
        builder.add(new CommandBuilder("command", "args", workingDir, new RunIfConfigs(), new NullBuilder(), "desc"));
        BuildAssignment assignment = BuildAssignment.create(jobPlan(), buildCause, builder, workingDir);

        BuildWork work = new BuildWork(assignment);
        String msg = Message.encode(new Message(Action.assignWork, work));
        Message decodedMsg = Message.decode(msg);
        assertThat(((BuildWork) decodedMsg.getData()).getAssignment().getPlan().getPipelineName(), is("pipelineName"));
    }

    private DefaultJobPlan jobPlan() {
        JobIdentifier jobIdentifier = new JobIdentifier("pipelineName", 1, "1", "defaultStage", "1", "job1", 100L);
        return new DefaultJobPlan(new Resources(), new ArtifactPlans(), new ArtifactPropertiesGenerators(), 1L, jobIdentifier);
    }
}
