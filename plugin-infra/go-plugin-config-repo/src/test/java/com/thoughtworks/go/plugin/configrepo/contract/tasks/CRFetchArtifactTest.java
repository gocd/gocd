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
package com.thoughtworks.go.plugin.configrepo.contract.tasks;

import com.thoughtworks.go.plugin.configrepo.contract.AbstractCRTest;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;

public class CRFetchArtifactTest extends AbstractCRTest<CRFetchArtifactTask> {

    private final CRFetchArtifactTask fetch;
    private final CRFetchArtifactTask fetchFromPipe;
    private final CRFetchArtifactTask fetchToDest;

    private final CRFetchArtifactTask invalidFetchNoSource;
    private final CRFetchArtifactTask invalidFetchNoJob;
    private final CRFetchArtifactTask invalidFetchNoStage;

    public CRFetchArtifactTest()
    {
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
        examples.put("fetch",fetch);
        examples.put("fetchFromPipe",fetchFromPipe);
        examples.put("fetchToDest",fetchToDest);
    }

    @Override
    public void addBadExamples(Map<String, CRFetchArtifactTask> examples) {
        examples.put("invalidFetchNoSource",invalidFetchNoSource);
        examples.put("invalidFetchNoJob",invalidFetchNoJob);
        examples.put("invalidFetchNoStage",invalidFetchNoStage);
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializingFetchTask()
    {
        CRTask value = fetch;
        String json = gson.toJson(value);

        CRFetchArtifactTask deserializedValue = (CRFetchArtifactTask)gson.fromJson(json,CRTask.class);
        assertThat("Deserialized value should equal to value before serialization",
                deserializedValue,is(value));
    }

    @Test
    public void shouldDeserializeWhenDestinationIsNull()
    {
        String json = "{\n" +
                "              \"type\" : \"fetch\",\n" +
                "              \"pipeline\" : \"pip\",\n" +
                "              \"stage\" : \"build1\",\n" +
                "              \"job\" : \"build\",\n" +
                "              \"source\" : \"bin\",\n" +
                "              \"run_if\" : \"passed\",\n" +
                "              \"artifact_origin\" : \"gocd\"\n" +
                "            }";
        CRFetchArtifactTask deserializedValue = (CRFetchArtifactTask)gson.fromJson(json,CRTask.class);

        assertThat(deserializedValue.getPipeline(),is("pip"));
        assertThat(deserializedValue.getJob(),is("build"));
        assertThat(deserializedValue.getStage(),is("build1"));
        assertThat(deserializedValue.getSource(),is("bin"));
        assertThat(deserializedValue.getRunIf(),is(CRRunIf.passed));
        assertNull(deserializedValue.getDestination());
        assertThat(deserializedValue.sourceIsDirectory(),is(true));
        assertThat(deserializedValue.getArtifactOrigin(), is(CRAbstractFetchTask.ArtifactOrigin.gocd));
    }

    @Test
    public void shouldDeserializeWhenArtifactOriginIsNull()
    {
        String json = "{\n" +
                "              \"type\" : \"fetch\",\n" +
                "              \"pipeline\" : \"pip\",\n" +
                "              \"stage\" : \"build1\",\n" +
                "              \"job\" : \"build\",\n" +
                "              \"source\" : \"bin\",\n" +
                "              \"run_if\" : \"passed\"\n" +
                "            }";
        CRFetchArtifactTask deserializedValue = (CRFetchArtifactTask)gson.fromJson(json,CRTask.class);

        assertThat(deserializedValue.getPipeline(),is("pip"));
        assertThat(deserializedValue.getJob(),is("build"));
        assertThat(deserializedValue.getStage(),is("build1"));
        assertThat(deserializedValue.getSource(),is("bin"));
        assertThat(deserializedValue.getRunIf(),is(CRRunIf.passed));
        assertNull(deserializedValue.getDestination());
        assertThat(deserializedValue.sourceIsDirectory(),is(true));
        assertThat(deserializedValue.getArtifactOrigin(), is(CRAbstractFetchTask.ArtifactOrigin.gocd));
    }
}
