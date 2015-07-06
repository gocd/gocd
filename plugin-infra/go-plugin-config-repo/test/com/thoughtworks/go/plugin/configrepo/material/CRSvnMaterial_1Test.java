package com.thoughtworks.go.plugin.configrepo.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRSvnMaterial_1Test extends CRBaseTest<CRSvnMaterial_1> {

    private final CRSvnMaterial_1 simpleSvn;
    private final CRSvnMaterial_1 simpleSvnAuth;
    private final CRSvnMaterial_1 customSvn;
    private final CRSvnMaterial_1 invalidNoUrl;
    private final CRSvnMaterial_1 invalidPasswordAndEncyptedPasswordSet;

    public CRSvnMaterial_1Test()
    {
        simpleSvn = new CRSvnMaterial_1();
        simpleSvn.setUrl("http://mypublicrepo");

        simpleSvnAuth = new CRSvnMaterial_1();
        simpleSvnAuth.setUrl("http://myprivaterepo");
        simpleSvnAuth.setUserName("john");
        simpleSvnAuth.setPassword("pa$sw0rd");

        customSvn = new CRSvnMaterial_1("svnMaterial1","destDir1", false,
                "http://svn","user1","pass1",true,"tools","lib");

        invalidNoUrl = new CRSvnMaterial_1();
        invalidPasswordAndEncyptedPasswordSet = new CRSvnMaterial_1();
        invalidPasswordAndEncyptedPasswordSet.setUrl("http://myprivaterepo");
        invalidPasswordAndEncyptedPasswordSet.setPassword("pa$sw0rd");
        invalidPasswordAndEncyptedPasswordSet.setEncryptedPassword("26t=$j64");
    }

    @Override
    public void addGoodExamples(Map<String, CRSvnMaterial_1> examples) {
        examples.put("simpleSvn",simpleSvn);
        examples.put("simpleSvnAuth",simpleSvnAuth);
        examples.put("customSvn",customSvn);
    }

    @Override
    public void addBadExamples(Map<String, CRSvnMaterial_1> examples) {
        examples.put("invalidNoUrl",invalidNoUrl);
        examples.put("invalidPasswordAndEncyptedPasswordSet",invalidPasswordAndEncyptedPasswordSet);
    }

    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        CRMaterial_1 value = customSvn;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRSvnMaterial_1.TYPE_NAME));
    }
    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRMaterial_1 value = customSvn;
        String json = gson.toJson(value);

        CRSvnMaterial_1 deserializedValue = (CRSvnMaterial_1)gson.fromJson(json,CRMaterial_1.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
}
