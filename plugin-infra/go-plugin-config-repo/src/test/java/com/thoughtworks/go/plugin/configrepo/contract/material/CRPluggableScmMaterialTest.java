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
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CRPluggableScmMaterialTest extends AbstractCRTest<CRPluggableScmMaterial> {

    private final CRPluggableScmMaterial pluggableGit;
    private final CRPluggableScmMaterial pluggableGitWith2Filters;
    private final CRPluggableScmMaterial simplePluggableGit;
    private final CRPluggableScmMaterial simpleNamedPluggableGit;
    private final CRPluggableScmMaterial pluggableGitWithFilter;

    private final CRPluggableScmMaterial invalidNoScmId;

    public CRPluggableScmMaterialTest() {
        pluggableGit = new CRPluggableScmMaterial("myPluggableGit", "someScmGitRepositoryId", "destinationDir", null, false);
        pluggableGitWithFilter = new CRPluggableScmMaterial("myPluggableGit", "someScmGitRepositoryId", "destinationDir", List.of("mydir"), false);
        pluggableGitWith2Filters = new CRPluggableScmMaterial("myPluggableGit", "someScmGitRepositoryId", "destinationDir", List.of("dir1", "dir2"), false);

        simplePluggableGit = new CRPluggableScmMaterial();
        simplePluggableGit.setScmId("mygit-id");

        simpleNamedPluggableGit = new CRPluggableScmMaterial();
        simpleNamedPluggableGit.setScmId("mygit-id");
        simpleNamedPluggableGit.setName("myGitMaterial");

        invalidNoScmId = new CRPluggableScmMaterial();
    }

    @Override
    public void addGoodExamples(Map<String, CRPluggableScmMaterial> examples) {
        examples.put("pluggableGit", pluggableGit);
        examples.put("pluggableGitWith2Filters", pluggableGitWith2Filters);
        examples.put("simplePluggableGit", simplePluggableGit);
        examples.put("simpleNamedPluggableGit", simpleNamedPluggableGit);
        examples.put("pluggableGitWithFilter", pluggableGitWithFilter);
    }

    @Override
    public void addBadExamples(Map<String, CRPluggableScmMaterial> examples) {
        examples.put("invalidNoScmId", invalidNoScmId);
    }

    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials() {
        JsonObject jsonObject = (JsonObject) gson.toJsonTree(pluggableGit);
        assertThat(jsonObject.get("type").getAsString()).isEqualTo(CRPluggableScmMaterial.TYPE_NAME);
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializing() {
        CRMaterial value = pluggableGit;
        String json = gson.toJson(value);

        CRPluggableScmMaterial deserializedValue = (CRPluggableScmMaterial) gson.fromJson(json, CRMaterial.class);
        assertThat(deserializedValue).describedAs("Deserialized value should equal to value before serialization")
            .isEqualTo(value);
    }

    @Test
    public void isWhiteList_shouldBeTrueInPresenceOfIncludesFilter() {
        CRPluggableScmMaterial pluggableScmMaterial = new CRPluggableScmMaterial("myPluggableGit", "someScmGitRepositoryId",
            "destinationDir", List.of("mydir"), true);

        assertTrue(pluggableScmMaterial.isWhitelist());
    }
}
