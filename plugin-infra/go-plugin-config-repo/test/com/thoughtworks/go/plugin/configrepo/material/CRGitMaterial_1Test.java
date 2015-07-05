package com.thoughtworks.go.plugin.configrepo.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRGitMaterial_1Test extends CRBaseTest<CRGitMaterial_1> {
    private String url1 = "http://my.git.repository.com";
    private String url2 = "http://other.git.repository.com";

    private final CRGitMaterial_1 simpleGit;
    private final CRGitMaterial_1 simpleGitBranch;
    private final CRGitMaterial_1 veryCustomGit;
    private final CRGitMaterial_1 invalidNoUrl;

    public CRGitMaterial_1Test()
    {
        simpleGit = new CRGitMaterial_1();
        simpleGit.setUrl(url1);

        simpleGitBranch = new CRGitMaterial_1();
        simpleGitBranch.setUrl(url2);
        simpleGitBranch.setBranch("develop");

        veryCustomGit = new CRGitMaterial_1("gitMaterial1","dir1",false,url1,"feature12","externals","tools");

        invalidNoUrl = new CRGitMaterial_1("gitMaterial1","dir1",false,null,"feature12","externals","tools");
    }

    @Override
    public void addGoodExamples(Map<String, CRGitMaterial_1> examples) {
        examples.put("simpleGit",simpleGit);
        examples.put("simpleGitBranch",simpleGitBranch);
        examples.put("veryCustomGit",veryCustomGit);
    }

    @Override
    public void addBadExamples(Map<String, CRGitMaterial_1> examples) {
        examples.put("invalidNoUrl",invalidNoUrl);
    }


    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        CRMaterial_1 value = veryCustomGit;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRGitMaterial_1.TYPE_NAME));
    }
    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRMaterial_1 value = veryCustomGit;
        String json = gson.toJson(value);

        CRGitMaterial_1 deserializedValue = (CRGitMaterial_1)gson.fromJson(json,CRMaterial_1.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }

}
