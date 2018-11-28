/*
 * Copyright 2017 ThoughtWorks, Inc.
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
import com.thoughtworks.go.plugin.configrepo.contract.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRHgMaterialTest extends CRBaseTest<CRHgMaterial> {

    private final CRHgMaterial simpleHg;
    private final CRHgMaterial customHg;
    private final CRHgMaterial invalidHgNoUrl;
    private final CRHgMaterial invalidHgWhitelistAndIgnores;

    public CRHgMaterialTest()
    {
        simpleHg = new CRHgMaterial();
        simpleHg.setUrl("myHgRepo");

        customHg = new CRHgMaterial("hgMaterial1","dir1",false,"repos/myhg",false,"externals","tools");

        invalidHgNoUrl = new CRHgMaterial();
        invalidHgWhitelistAndIgnores = new CRHgMaterial("hgMaterial1","dir1",false,"repos/myhg",false,"externals","tools");
        invalidHgWhitelistAndIgnores.setWhitelistNoCheck("src","tests");
    }

    @Override
    public void addGoodExamples(Map<String, CRHgMaterial> examples) {
        examples.put("simpleHg",simpleHg);
        examples.put("customHg",customHg);
    }

    @Override
    public void addBadExamples(Map<String, CRHgMaterial> examples) {
        examples.put("invalidHgNoUrl",invalidHgNoUrl);
        examples.put("invalidHgWhitelistAndIgnores",invalidHgWhitelistAndIgnores);
    }

    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        CRMaterial value = customHg;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRHgMaterial.TYPE_NAME));
    }
    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRMaterial value = customHg;
        String json = gson.toJson(value);

        CRHgMaterial deserializedValue = (CRHgMaterial)gson.fromJson(json,CRMaterial.class);
        assertThat("Deserialized value should equal to value before serialization",
                deserializedValue,is(value));
    }
}