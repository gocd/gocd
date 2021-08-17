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
package com.thoughtworks.go.plugin.configrepo.contract.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.contract.AbstractCRTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CRDependencyMaterialTest extends AbstractCRTest<CRDependencyMaterial> {

    private final CRDependencyMaterial namedDependsOnPipeline;
    private final CRDependencyMaterial invalidNoPipeline;
    private final CRDependencyMaterial invalidNoStage;
    private CRDependencyMaterial dependsOnPipeline;

    public CRDependencyMaterialTest()
    {
        namedDependsOnPipeline = new CRDependencyMaterial("pipe2","pipeline2","build", false);
        dependsOnPipeline = new CRDependencyMaterial("pipeline2","build", false);

        invalidNoPipeline = new CRDependencyMaterial();
        invalidNoPipeline.setStage("build");

        invalidNoStage = new CRDependencyMaterial();
        invalidNoStage.setPipeline("pipeline1");
    }

    @Override
    public void addGoodExamples(Map<String, CRDependencyMaterial> examples) {
        examples.put("dependsOnPipeline",dependsOnPipeline);
        examples.put("namedDependsOnPipeline",namedDependsOnPipeline);
    }

    @Override
    public void addBadExamples(Map<String, CRDependencyMaterial> examples) {
        examples.put("invalidNoPipeline",invalidNoPipeline);
        examples.put("invalidNoStage",invalidNoStage);
    }


    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        CRMaterial value = dependsOnPipeline;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRDependencyMaterial.TYPE_NAME));
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRMaterial value = dependsOnPipeline;
        String json = gson.toJson(value);

        CRDependencyMaterial deserializedValue = (CRDependencyMaterial)gson.fromJson(json,CRMaterial.class);
        assertThat("Deserialized value should equal to value before serialization",
                deserializedValue,is(value));
    }

}
