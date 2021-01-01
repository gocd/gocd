/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain.materials.dependency;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.MaterialAgent;
import com.thoughtworks.go.domain.materials.MaterialAgentFactory;
import com.thoughtworks.go.remote.AgentIdentifier;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Date;

import static com.thoughtworks.go.domain.materials.MaterialAgent.NO_OP;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DependencyMaterialAgentTest {

    @Before
    public void setUp() {
    }
    private MaterialRevision materialRevision(String pipelineName, Integer pipelineCounter, String pipelineLabel,
                                              String stageName, int stageCounter) {
        DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(stageName));
        DependencyMaterialRevision revision = DependencyMaterialRevision.create(pipelineName, pipelineCounter,
                pipelineLabel, stageName, stageCounter);
        MaterialRevision materialRevision = revision.convert(material, new Date());
        return materialRevision;
    }

    @Test
    public void shouldBeCreatedByAgentFactory() {
        MaterialAgentFactory factory = new MaterialAgentFactory(ProcessOutputStreamConsumer.inMemoryConsumer(), new File("blah"), new AgentIdentifier("", "", ""), null);
        MaterialAgent createdAgent = factory.createAgent(materialRevision("pipeline-name", 1, "pipeline-label", "stage-name", 1));

        assertThat(createdAgent, is(NO_OP));
    }
}
