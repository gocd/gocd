package com.thoughtworks.go.plugin.configrepo.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRTfsMaterial_1Test extends CRBaseTest<CRTfsMaterial_1> {

    private final CRTfsMaterial_1 simpleTfs;
    private final CRTfsMaterial_1 customTfs;
    private final CRTfsMaterial_1 invalidTfsNoUrl;
    private final CRTfsMaterial_1 invalidTfsNoUser;
    private final CRTfsMaterial_1 invalidTfsNoProject;
    private final CRTfsMaterial_1 invalidPasswordAndEncyptedPasswordSet;

    public CRTfsMaterial_1Test()
    {
        simpleTfs = new CRTfsMaterial_1("url1","user1","projectDir");

        customTfs = new CRTfsMaterial_1("tfsMaterialName", "dir1", false,"url3","user4",
            "pass",null,"projectDir","example.com","tools","externals");

        invalidTfsNoUrl = new CRTfsMaterial_1(null,"user1","projectDir");
        invalidTfsNoUser = new CRTfsMaterial_1("url1",null,"projectDir");
        invalidTfsNoProject = new CRTfsMaterial_1("url1","user1",null);

        invalidPasswordAndEncyptedPasswordSet = new CRTfsMaterial_1("url1","user1","projectDir");
        invalidPasswordAndEncyptedPasswordSet.setPassword("pa$sw0rd");
        invalidPasswordAndEncyptedPasswordSet.setEncryptedPassword("26t=$j64");
    }

    @Override
    public void addGoodExamples(Map<String, CRTfsMaterial_1> examples) {
        examples.put("simpleTfs",simpleTfs);
        examples.put("customTfs",customTfs);
    }

    @Override
    public void addBadExamples(Map<String, CRTfsMaterial_1> examples) {
        examples.put("invalidTfsNoUrl",invalidTfsNoUrl);
        examples.put("invalidTfsNoUser",invalidTfsNoUser);
        examples.put("invalidTfsNoProject",invalidTfsNoProject);
    }


    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        CRMaterial_1 value = customTfs;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRTfsMaterial_1.TYPE_NAME));
    }
    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRMaterial_1 value = customTfs;
        String json = gson.toJson(value);

        CRTfsMaterial_1 deserializedValue = (CRTfsMaterial_1)gson.fromJson(json,CRMaterial_1.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
}
