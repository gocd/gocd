package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRGitMaterialTest extends CRBaseTest<CRGitMaterial> {
    private String url1 = "http://my.git.repository.com";
    private String url2 = "http://other.git.repository.com";

    private final CRGitMaterial simpleGit;
    private final CRGitMaterial simpleGitBranch;
    private final CRGitMaterial veryCustomGit;
    private final CRGitMaterial invalidNoUrl;

    public CRGitMaterialTest()
    {
        simpleGit = new CRGitMaterial();
        simpleGit.setUrl(url1);

        simpleGitBranch = new CRGitMaterial();
        simpleGitBranch.setUrl(url2);
        simpleGitBranch.setBranch("develop");

        veryCustomGit = new CRGitMaterial("gitMaterial1","dir1",false,url1,"feature12","externals","tools");

        invalidNoUrl = new CRGitMaterial("gitMaterial1","dir1",false,null,"feature12","externals","tools");
    }

    @Override
    public void addGoodExamples(Map<String, CRGitMaterial> examples) {
        examples.put("simpleGit",simpleGit);
        examples.put("simpleGitBranch",simpleGitBranch);
        examples.put("veryCustomGit",veryCustomGit);
    }

    @Override
    public void addBadExamples(Map<String, CRGitMaterial> examples) {
        examples.put("invalidNoUrl",invalidNoUrl);
    }


    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        CRMaterial value = veryCustomGit;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRGitMaterial.TYPE_NAME));
    }
    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRMaterial value = veryCustomGit;
        String json = gson.toJson(value);

        CRGitMaterial deserializedValue = (CRGitMaterial)gson.fromJson(json,CRMaterial.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }

}
