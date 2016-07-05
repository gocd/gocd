package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRTfsMaterialTest extends CRBaseTest<CRTfsMaterial> {

    private final CRTfsMaterial simpleTfs;
    private final CRTfsMaterial customTfs;
    private final CRTfsMaterial invalidTfsNoUrl;
    private final CRTfsMaterial invalidTfsNoUser;
    private final CRTfsMaterial invalidTfsNoProject;
    private final CRTfsMaterial invalidPasswordAndEncyptedPasswordSet;

    public CRTfsMaterialTest()
    {
        simpleTfs = new CRTfsMaterial("url1","user1","projectDir");

        customTfs = new CRTfsMaterial("tfsMaterialName", "dir1", false,"url3","user4",
            "pass",null,"projectDir","example.com",false,"tools","externals");

        invalidTfsNoUrl = new CRTfsMaterial(null,"user1","projectDir");
        invalidTfsNoUser = new CRTfsMaterial("url1",null,"projectDir");
        invalidTfsNoProject = new CRTfsMaterial("url1","user1",null);

        invalidPasswordAndEncyptedPasswordSet = new CRTfsMaterial("url1","user1","projectDir");
        invalidPasswordAndEncyptedPasswordSet.setPassword("pa$sw0rd");
        invalidPasswordAndEncyptedPasswordSet.setEncryptedPassword("26t=$j64");
    }

    @Override
    public void addGoodExamples(Map<String, CRTfsMaterial> examples) {
        examples.put("simpleTfs",simpleTfs);
        examples.put("customTfs",customTfs);
    }

    @Override
    public void addBadExamples(Map<String, CRTfsMaterial> examples) {
        examples.put("invalidTfsNoUrl",invalidTfsNoUrl);
        examples.put("invalidTfsNoUser",invalidTfsNoUser);
        examples.put("invalidTfsNoProject",invalidTfsNoProject);
    }


    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        CRMaterial value = customTfs;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRTfsMaterial.TYPE_NAME));
    }
    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRMaterial value = customTfs;
        String json = gson.toJson(value);

        CRTfsMaterial deserializedValue = (CRTfsMaterial)gson.fromJson(json,CRMaterial.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
}
