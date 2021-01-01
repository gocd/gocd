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
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;

public class CRGitMaterialTest extends AbstractCRTest<CRGitMaterial> {
    private String url1 = "http://my.git.repository.com";
    private String url2 = "http://other.git.repository.com";

    private final CRGitMaterial simpleGit;
    private final CRGitMaterial simpleGitBranch;
    private final CRGitMaterial veryCustomGit;
    private final CRGitMaterial invalidNoUrl;
    private final CRGitMaterial withIncludes;
    private final CRGitMaterial invalidBothIncludesAndIgnores;
    private final CRGitMaterial invalidPasswordAndEncyptedPasswordSet;

    public CRGitMaterialTest() {
        simpleGit = new CRGitMaterial();
        simpleGit.setUrl(url1);

        simpleGitBranch = new CRGitMaterial();
        simpleGitBranch.setUrl(url2);
        simpleGitBranch.setBranch("develop");

        veryCustomGit = new CRGitMaterial("gitMaterial1", "dir1", false, false, null, Arrays.asList("externals", "tools"), url1, "feature12", true);
        withIncludes = new CRGitMaterial("gitMaterial1", "dir1", false, true, null, Arrays.asList("externals", "tools"), url1, "feature12", true);

        invalidNoUrl = new CRGitMaterial("gitMaterial1", "dir1", false, false, null, Arrays.asList("externals", "tools"), null, "feature12", true);
        invalidBothIncludesAndIgnores = new CRGitMaterial("gitMaterial1", "dir1", false, false, null, Arrays.asList("externals", "tools"), url1, "feature12", true);
        invalidBothIncludesAndIgnores.setIncludesNoCheck("src", "tests");

        invalidPasswordAndEncyptedPasswordSet = new CRGitMaterial("gitMaterial1", "dir1", false, false, null, Arrays.asList("externals", "tools"), null, "feature12", true);
        invalidPasswordAndEncyptedPasswordSet.setPassword("pa$sw0rd");
        invalidPasswordAndEncyptedPasswordSet.setEncryptedPassword("26t=$j64");
    }

    @Override
    public void addGoodExamples(Map<String, CRGitMaterial> examples) {
        examples.put("simpleGit", simpleGit);
        examples.put("simpleGitBranch", simpleGitBranch);
        examples.put("veryCustomGit", veryCustomGit);
        examples.put("withIncludes", withIncludes);
    }

    @Override
    public void addBadExamples(Map<String, CRGitMaterial> examples) {
        examples.put("invalidNoUrl", invalidNoUrl);
        examples.put("invalidBothIncludesAndIgnores", invalidBothIncludesAndIgnores);
    }

    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials() {
        CRMaterial value = veryCustomGit;
        JsonObject jsonObject = (JsonObject) gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRGitMaterial.TYPE_NAME));
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializing() {
        CRMaterial value = veryCustomGit;
        String json = gson.toJson(value);

        CRGitMaterial deserializedValue = (CRGitMaterial) gson.fromJson(json, CRMaterial.class);
        assertThat("Deserialized value should equal to value before serialization",
                deserializedValue, is(value));
    }

}
