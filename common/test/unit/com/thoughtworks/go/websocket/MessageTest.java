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


package com.thoughtworks.go.websocket;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.*;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.remote.work.BuildWork;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;


public class MessageTest {

    @Test
    public void encodeAndDecodeMessageWithoutData() {
        byte[] msg = MessageEncoding.encodeMessage(new Message(Action.ping));
        Message decoded = MessageEncoding.decodeMessage(new ByteArrayInputStream(msg));
        assertThat(decoded.getAction(), is(Action.ping));
        assertNull(decoded.getData());

        assertEquals(decoded, MessageEncoding.decodeMessage(new ByteArrayInputStream(msg)));
    }

    @Test
    public void encodeAndDecodePingMessage() {
        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("hostName", "ipAddress", "uuid"), null, null, null, null, false);
        byte[] msg = MessageEncoding.encodeMessage(new Message(Action.ping, MessageEncoding.encodeData(info)));
        Message decoded = MessageEncoding.decodeMessage(new ByteArrayInputStream(msg));
        AgentRuntimeInfo decodedInfo = MessageEncoding.decodeData(decoded.getData(), AgentRuntimeInfo.class);
        assertThat(decodedInfo.getIdentifier(), is(info.getIdentifier()));
    }

    @Test
    public void encodeAndDecodeSetCookie() {
        byte[] msg = MessageEncoding.encodeMessage(new Message(Action.setCookie, MessageEncoding.encodeData("cookie")));
        Message decoded = MessageEncoding.decodeMessage(new ByteArrayInputStream(msg));
        assertThat(MessageEncoding.decodeData(decoded.getData(), String.class), is("cookie"));
    }

    @Test
    public void encodeAndDecodeAssignWorkWithDifferentBuilders() throws Exception {
        File workingDir = new File(CruiseConfig.WORKING_BASE_DIR + "pipelineName");
        Materials materials = MaterialsMother.defaultMaterials();
        MaterialRevisions revisions = ModificationsMother.modifyOneFile(materials, ModificationsMother.nextRevision());
        BuildCause buildCause = BuildCause.createWithModifications(revisions, "");

        List<Builder> builder = new ArrayList<Builder>();
        builder.add(new CommandBuilder("command", "args", workingDir, new RunIfConfigs(), new NullBuilder(), "desc"));
        builder.add(new BuilderForKillAllChildTask());
        builder.add(new CommandBuilderWithArgList("command", new String[]{"arg1", "arg2"}, workingDir, new RunIfConfigs(), new NullBuilder(), "desc"));
        builder.add(new FetchArtifactBuilder(new RunIfConfigs(), new NullBuilder(), "desc", jobPlan().getIdentifier(), "srcdir", "dest",
                new FileHandler(workingDir, "src"),
                new ChecksumFileHandler(workingDir)));
        BuildAssignment assignment = BuildAssignment.create(jobPlan(), buildCause, builder, workingDir);

        BuildWork work = new BuildWork(assignment);
        byte[] msg = MessageEncoding.encodeMessage(new Message(Action.assignWork, MessageEncoding.encodeWork(work)));
        Message decodedMsg = MessageEncoding.decodeMessage(new ByteArrayInputStream(msg));
        BuildWork decodedWork = (BuildWork) MessageEncoding.decodeWork(decodedMsg.getData());
        assertThat(decodedWork.getAssignment().getPlan().getPipelineName(), is("pipelineName"));
    }


    private DefaultJobPlan jobPlan() {
        JobIdentifier jobIdentifier = new JobIdentifier("pipelineName", 1, "1", "defaultStage", "1", "job1", 100L);
        return new DefaultJobPlan(new Resources(), new ArtifactPlans(), new ArtifactPropertiesGenerators(), 1L, jobIdentifier, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
    }
}
