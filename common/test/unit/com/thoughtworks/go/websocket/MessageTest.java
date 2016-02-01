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

import com.thoughtworks.go.config.ArtifactPlans;
import com.thoughtworks.go.config.ArtifactPropertiesGenerators;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.Resources;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.builder.*;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.remote.work.BuildWork;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;


public class MessageTest implements Socket {

    private List<ByteBuffer> messageBuffer = new ArrayList<>();
    private byte[] message;

    @Test
    public void encodeAndDecodeMessageWithoutData() {
        byte[] msg = Message.encode(new Message(Action.ping));
        Message decoded = Message.decode(msg);
        assertThat(decoded.getAction(), is(Action.ping));
        assertNull(decoded.getData());

        assertEquals(decoded, Message.decode(new ByteArrayInputStream(msg)));
    }

    @Test
    public void encodeAndDecodePingMessage() {
        AgentRuntimeInfo info = new AgentRuntimeInfo(new AgentIdentifier("hostName", "ipAddress", "uuid"), null, null, null, null);
        byte[] msg = Message.encode(new Message(Action.ping, info));
        Message decoded = Message.decode(msg);
        assertThat(((AgentRuntimeInfo) decoded.getData()).getIdentifier(), is(info.getIdentifier()));
    }

    @Test
    public void encodeAndDecodeSetCookie() {
        byte[] msg = Message.encode(new Message(Action.setCookie, "cookie"));
        Message decoded = Message.decode(msg);
        assertThat((String) decoded.getData(), is("cookie"));
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
        byte[] msg = Message.encode(new Message(Action.assignWork, work));
        Message decodedMsg = Message.decode(msg);
        assertThat(((BuildWork) decodedMsg.getData()).getAssignment().getPlan().getPipelineName(), is("pipelineName"));
    }

    @Test
    public void materialRevisions() {
        assertEncodeDecode(ModificationsMother.createHgMaterialRevisions());
        assertEncodeDecode(ModificationsMother.createSvnMaterialRevisions(ModificationsMother.withModifiedFileWhoseNameLengthIsOneK()));
        assertEncodeDecode(ModificationsMother.createP4MaterialRevisions(ModificationsMother.withModifiedFileWhoseNameLengthIsOneK()));

        assertEncodeDecode(ModificationsMother.createPackageMaterialRevision("revision"));
        assertEncodeDecode(ModificationsMother.createPipelineMaterialRevision("stageIdentifier"));
    }

    @Test
    public void materialInstance() {
        assertEncodeDecodeMaterialInstance(MaterialsMother.pluggableSCMMaterial());
        assertEncodeDecodeMaterialInstance(MaterialsMother.gitMaterial("url"));
        assertEncodeDecodeMaterialInstance(MaterialsMother.hgMaterial());
        assertEncodeDecodeMaterialInstance(MaterialsMother.tfsMaterial("url"));
        assertEncodeDecodeMaterialInstance(MaterialsMother.p4Material());
        assertEncodeDecodeMaterialInstance(MaterialsMother.packageMaterial());
    }

    @Test
    public void sendMessageWithPartialBytesThatMeetsMessageMaxBufferSize() throws IOException {
        Message.send(this, new Message(Action.ping));
        assertTrue(messageBuffer.size() > 1);
        assertEquals(getMaxMessageBufferSize(), messageBuffer.get(0).rewind().remaining());
        assertNotNull(message);
        Message decoded = Message.decode(message);
        assertEquals(Action.ping, decoded.getAction());
    }

    private void assertEncodeDecodeMaterialInstance(Material material) {
        MaterialRevisions materialRevisions = new MaterialRevisions();

        final ArrayList<Modification> modifications = new ArrayList<>();

        Modification m = new Modification("user2", "comment2", "email2", new Date(), "9fdcf27f16eadc362733328dd481d8a2c29915e1");
        m.setMaterialInstance(material.createMaterialInstance());
        modifications.add(m);
        materialRevisions.addRevision(MaterialsMother.hgMaterial(), modifications);

        assertEncodeDecode(materialRevisions);
    }

    private void assertEncodeDecode(Object obj) {
        byte[] msg = Message.encode(new Message(Action.assignWork, obj));
        Message decodedMsg = Message.decode(msg);
        assertEquals(decodedMsg.getData().getClass(), obj.getClass());
    }

    private DefaultJobPlan jobPlan() {
        JobIdentifier jobIdentifier = new JobIdentifier("pipelineName", 1, "1", "defaultStage", "1", "job1", 100L);
        return new DefaultJobPlan(new Resources(), new ArtifactPlans(), new ArtifactPropertiesGenerators(), 1L, jobIdentifier);
    }

    @Override
    public void sendPartialBytes(ByteBuffer byteBuffer, boolean last) throws IOException {
        messageBuffer.add(byteBuffer);
        if (last) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            for (ByteBuffer b : messageBuffer) {
                byte[] bytes = new byte[b.remaining()];
                b.get(bytes);
                out.write(bytes);
            }
            message = out.toByteArray();
        }
    }

    @Override
    public int getMaxMessageBufferSize() {
        return 10;
    }
}
