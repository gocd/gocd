/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.plugin.configrepo.contract.tasks;

import com.thoughtworks.go.plugin.configrepo.contract.AbstractCRTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CRFetchArtifactTest extends AbstractCRTest<CRFetchArtifactTask> {

    private final CRFetchArtifactTask fetch;
    private final CRFetchArtifactTask fetchFromPipe;
    private final CRFetchArtifactTask fetchToDest;

    private final CRFetchArtifactTask invalidFetchNoSource;
    private final CRFetchArtifactTask invalidFetchNoJob;
    private final CRFetchArtifactTask invalidFetchNoStage;

    public CRFetchArtifactTest() {
        fetch = new CRFetchArtifactTask(null, null, null, "build", "buildjob", "bin", null, false);
        fetchFromPipe = new CRFetchArtifactTask(null, null, null, "build", "buildjob", "bin", null, false);
        fetchFromPipe.setPipeline("pipeline1");

        fetchToDest = new CRFetchArtifactTask(null, null, null, "build", "buildjob", "bin", null, false);
        fetchToDest.setDestination("lib");

        invalidFetchNoSource = new CRFetchArtifactTask(null, null, null, "build", "buildjob", null, null, false);
        invalidFetchNoJob = new CRFetchArtifactTask(null, null, null, "build", null, "bin", null, false);
        invalidFetchNoStage = new CRFetchArtifactTask(null, null, null, null, "buildjob", "bin", null, false);
    }

    @Override
    public void addGoodExamples(Map<String, CRFetchArtifactTask> examples) {
        examples.put("fetch", fetch);
        examples.put("fetchFromPipe", fetchFromPipe);
        examples.put("fetchToDest", fetchToDest);
    }

    @Override
    public void addBadExamples(Map<String, CRFetchArtifactTask> examples) {
        examples.put("invalidFetchNoSource", invalidFetchNoSource);
        examples.put("invalidFetchNoJob", invalidFetchNoJob);
        examples.put("invalidFetchNoStage", invalidFetchNoStage);
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializingFetchTask() {
        CRTask value = fetch;
        String json = gson.toJson(value);

        CRFetchArtifactTask deserializedValue = (CRFetchArtifactTask) gson.fromJson(json, CRTask.class);
        assertThat(deserializedValue).isEqualTo(value);
    }

    @Test
    public void shouldDeserializeWhenDestinationIsNull() {
        String json = """
                {
                              "type" : "fetch",
                              "pipeline" : "pip",
                              "stage" : "build1",
                              "job" : "build",
                              "source" : "bin",
                              "run_if" : "passed",
                              "artifact_origin" : "gocd"
                            }""";
        CRFetchArtifactTask deserializedValue = (CRFetchArtifactTask) gson.fromJson(json, CRTask.class);

        assertThat(deserializedValue.getPipeline()).isEqualTo("pip");
        assertThat(deserializedValue.getJob()).isEqualTo("build");
        assertThat(deserializedValue.getStage()).isEqualTo("build1");
        assertThat(deserializedValue.getSource()).isEqualTo("bin");
        assertThat(deserializedValue.getRunIf()).isEqualTo(CRRunIf.passed);
        assertNull(deserializedValue.getDestination());
        assertThat(deserializedValue.sourceIsDirectory()).isTrue();
        assertThat(deserializedValue.getArtifactOrigin()).isEqualTo(CRAbstractFetchTask.ArtifactOrigin.gocd);
    }

    @Test
    public void shouldDeserializeWhenArtifactOriginIsNull() {
        String json = """
                {
                              "type" : "fetch",
                              "pipeline" : "pip",
                              "stage" : "build1",
                              "job" : "build",
                              "source" : "bin",
                              "run_if" : "passed"
                            }""";
        CRFetchArtifactTask deserializedValue = (CRFetchArtifactTask) gson.fromJson(json, CRTask.class);

        assertThat(deserializedValue.getPipeline()).isEqualTo("pip");
        assertThat(deserializedValue.getJob()).isEqualTo("build");
        assertThat(deserializedValue.getStage()).isEqualTo("build1");
        assertThat(deserializedValue.getSource()).isEqualTo("bin");
        assertThat(deserializedValue.getRunIf()).isEqualTo(CRRunIf.passed);
        assertNull(deserializedValue.getDestination());
        assertThat(deserializedValue.sourceIsDirectory()).isTrue();
        assertThat(deserializedValue.getArtifactOrigin()).isEqualTo(CRAbstractFetchTask.ArtifactOrigin.gocd);
    }
}
