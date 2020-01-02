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
package com.thoughtworks.go.plugin.configrepo.contract.tasks;

import com.thoughtworks.go.plugin.configrepo.contract.AbstractCRTest;
import com.thoughtworks.go.plugin.configrepo.contract.CRConfigurationProperty;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;

public class CRFetchPluggableArtifactTaskTest extends AbstractCRTest<CRFetchPluggableArtifactTask> {

    private final CRFetchPluggableArtifactTask fetch;
    private final CRFetchPluggableArtifactTask fetchFromPipe;

    private final CRFetchPluggableArtifactTask invalidFetchNoStoreId;
    private final CRFetchPluggableArtifactTask invalidFetchNoJob;
    private final CRFetchPluggableArtifactTask invalidFetchNoStage;
    private final CRFetchPluggableArtifactTask invalidFetchWithDuplicateProperties;

    public CRFetchPluggableArtifactTaskTest() {
        CRConfigurationProperty crConfigurationProperty = new CRConfigurationProperty("k1", "v1", null);

        fetch = new CRFetchPluggableArtifactTask(null, null, null, "build", "buildjob", "storeId", Arrays.asList(crConfigurationProperty));
        fetchFromPipe = new CRFetchPluggableArtifactTask(null, null, null, "build", "buildjob", "storeId", Arrays.asList(crConfigurationProperty));
        fetchFromPipe.setPipeline("pipeline1");

        invalidFetchNoStoreId = new CRFetchPluggableArtifactTask(null, null, null, "build", "buildjob", null, null);
        invalidFetchNoJob = new CRFetchPluggableArtifactTask(null, null, null, "build", null, "storeId", null);
        invalidFetchNoStage = new CRFetchPluggableArtifactTask(null, null, null, null, "buildjob", "storeId", null);
        invalidFetchWithDuplicateProperties = new CRFetchPluggableArtifactTask(null, null, null, "build", "buildjob", "storeId", Arrays.asList(crConfigurationProperty, crConfigurationProperty));
    }

    @Override
    public void addGoodExamples(Map<String, CRFetchPluggableArtifactTask> examples) {
        examples.put("fetch", fetch);
        examples.put("fetchFromPipe", fetchFromPipe);
    }

    @Override
    public void addBadExamples(Map<String, CRFetchPluggableArtifactTask> examples) {
        examples.put("invalidFetchNoStoreId", invalidFetchNoStoreId);
        examples.put("invalidFetchNoJob", invalidFetchNoJob);
        examples.put("invalidFetchNoStage", invalidFetchNoStage);
        examples.put("invalidFetchWithDuplicateProperties", invalidFetchWithDuplicateProperties);
    }

    @Test
    public void shouldDeserializeWhenConfigurationIsNull() {
        String json = "{\n" +
                "              \"type\" : \"fetch\",\n" +
                "              \"pipeline\" : \"pip\",\n" +
                "              \"stage\" : \"build1\",\n" +
                "              \"job\" : \"build\",\n" +
                "              \"artifact_id\" : \"s3\",\n" +
                "              \"run_if\" : \"passed\",\n" +
                "              \"artifact_origin\" : \"external\"\n" +
                "            }";
        CRFetchPluggableArtifactTask deserializedValue = (CRFetchPluggableArtifactTask) gson.fromJson(json, CRTask.class);

        assertThat(deserializedValue.getPipeline(), is("pip"));
        assertThat(deserializedValue.getJob(), is("build"));
        assertThat(deserializedValue.getStage(), is("build1"));
        assertThat(deserializedValue.getArtifactId(), is("s3"));
        assertThat(deserializedValue.getRunIf(), is(CRRunIf.passed));
        assertNull(deserializedValue.getConfiguration());
    }
}
