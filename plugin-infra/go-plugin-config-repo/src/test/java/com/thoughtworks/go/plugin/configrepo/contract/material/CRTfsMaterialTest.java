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
package com.thoughtworks.go.plugin.configrepo.contract.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.contract.AbstractCRTest;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;

public class CRTfsMaterialTest extends AbstractCRTest<CRTfsMaterial> {

    private final CRTfsMaterial simpleTfs;
    private final CRTfsMaterial customTfs;
    private final CRTfsMaterial invalidTfsNoUrl;
    private final CRTfsMaterial invalidTfsNoUser;
    private final CRTfsMaterial invalidTfsNoProject;
    private final CRTfsMaterial invalidPasswordAndEncyptedPasswordSet;

    public CRTfsMaterialTest() {
        simpleTfs = new CRTfsMaterial(null, null, false, false, "user1", null, "url1", "projectDir", null);

        customTfs = new CRTfsMaterial("tfsMaterialName", "dir1", false, false, "user4", Arrays.asList("tools", "externals"), "url3", "projectDir", "example.com");
        customTfs.setPassword("pass");

        invalidTfsNoUrl = new CRTfsMaterial(null, null, false, false, "user1", null, null, "projectDir", null);
        invalidTfsNoUser = new CRTfsMaterial(null, null, false, false, null, null, "url1", "projectDir", null);
        invalidTfsNoProject = new CRTfsMaterial(null, null, false, false, "user1", null, "url1", null, null);

        invalidPasswordAndEncyptedPasswordSet = new CRTfsMaterial(null, null, false, false, "user1", null, "url1", "projectDir", null);
        invalidPasswordAndEncyptedPasswordSet.setPassword("pa$sw0rd");
        invalidPasswordAndEncyptedPasswordSet.setEncryptedPassword("26t=$j64");
    }

    @Override
    public void addGoodExamples(Map<String, CRTfsMaterial> examples) {
        examples.put("simpleTfs", simpleTfs);
        examples.put("customTfs", customTfs);
    }

    @Override
    public void addBadExamples(Map<String, CRTfsMaterial> examples) {
        examples.put("invalidTfsNoUrl", invalidTfsNoUrl);
        examples.put("invalidTfsNoUser", invalidTfsNoUser);
        examples.put("invalidTfsNoProject", invalidTfsNoProject);
    }


    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials() {
        CRMaterial value = customTfs;
        JsonObject jsonObject = (JsonObject) gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRTfsMaterial.TYPE_NAME));
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializing() {
        CRMaterial value = customTfs;
        String json = gson.toJson(value);

        CRTfsMaterial deserializedValue = (CRTfsMaterial) gson.fromJson(json, CRMaterial.class);
        assertThat("Deserialized value should equal to value before serialization",
                deserializedValue, is(value));
    }
}
