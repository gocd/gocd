/*
 * Copyright 2022 ThoughtWorks, Inc.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class CRConfigMaterialTest extends AbstractCRTest<CRConfigMaterial> {
    private final CRConfigMaterial named;
    private final CRConfigMaterial namedDest;
    private final CRConfigMaterial materialWithIgnores;
    private final CRConfigMaterial invalidList;

    public CRConfigMaterialTest() {
        named = new CRConfigMaterial("primary", null,null);
        namedDest = new CRConfigMaterial("primary", "folder",null);
        List<String> patterns = new ArrayList<>();
        patterns.add("externals");
        patterns.add("tools");
        materialWithIgnores = new CRConfigMaterial("primary", "folder",new CRFilter(patterns,false));

        CRFilter badFilter = new CRFilter(patterns,false);
        badFilter.setIncludesNoCheck(patterns);
        invalidList = new CRConfigMaterial("primary", "folder",badFilter);
    }

    @Override
    public void addGoodExamples(Map<String, CRConfigMaterial> examples) {
        examples.put("namedExample", named);
        examples.put("namedDest", namedDest);
        examples.put("ignoreFilter", materialWithIgnores);
    }

    @Override
    public void addBadExamples(Map<String, CRConfigMaterial> examples) {
        examples.put("invalidList",invalidList);
    }

    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        CRMaterial value = named;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRConfigMaterial.TYPE_NAME));
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRMaterial value = named;
        String json = gson.toJson(value);

        CRConfigMaterial deserializedValue = (CRConfigMaterial)gson.fromJson(json,CRMaterial.class);
        assertThat("Deserialized value should equal to value before serialization",
                deserializedValue,is(value));
    }

}
