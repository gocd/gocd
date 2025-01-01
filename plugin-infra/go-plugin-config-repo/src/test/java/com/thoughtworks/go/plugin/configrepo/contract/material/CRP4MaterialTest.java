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
package com.thoughtworks.go.plugin.configrepo.contract.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.contract.AbstractCRTest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CRP4MaterialTest extends AbstractCRTest<CRP4Material> {

    private final String exampleView = "//depot/dev/src...          //anything/src/...";

    private final CRP4Material p4simple;
    private final CRP4Material p4custom;
    private final CRP4Material invalidP4NoView;
    private final CRP4Material invalidP4NoServer;
    private final CRP4Material invalidPasswordAndEncryptedPasswordSet;


    public CRP4MaterialTest() {
        p4simple = new CRP4Material(null, null, false, false, null, null, "10.18.3.102:1666", exampleView, false);

        p4custom = new CRP4Material(
                "p4materialName", "dir1", false, false, "user1", List.of("lib", "tools"), "10.18.3.102:1666", exampleView, false);

        invalidP4NoView = new CRP4Material(null, null, false, false, null, null, "10.18.3.102:1666", null, false);
        invalidP4NoServer = new CRP4Material(null, null, false, false, null, null, null, exampleView, false);
        invalidPasswordAndEncryptedPasswordSet = new CRP4Material(null, null, false, false, null, null, "10.18.3.102:1666", null, false);
        invalidPasswordAndEncryptedPasswordSet.setPassword("pa$sw0rd");
        invalidPasswordAndEncryptedPasswordSet.setEncryptedPassword("26t=$j64");
    }

    @Override
    public void addGoodExamples(Map<String, CRP4Material> examples) {
        examples.put("p4simple", p4simple);
        examples.put("p4custom", p4custom);
    }

    @Override
    public void addBadExamples(Map<String, CRP4Material> examples) {
        examples.put("invalidP4NoView", invalidP4NoView);
        examples.put("invalidP4NoServer", invalidP4NoServer);
        examples.put("invalidPasswordAndEncryptedPasswordSet", invalidPasswordAndEncryptedPasswordSet);
    }


    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials() {
        JsonObject jsonObject = (JsonObject) gson.toJsonTree(p4custom);
        assertThat(jsonObject.get("type").getAsString()).isEqualTo(CRP4Material.TYPE_NAME);
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializing() {
        CRMaterial value = p4custom;
        String json = gson.toJson(value);

        CRP4Material deserializedValue = (CRP4Material) gson.fromJson(json, CRMaterial.class);
        assertThat(deserializedValue)
            .describedAs("Deserialized value should equal to value before serialization")
            .isEqualTo(value);
    }
}
