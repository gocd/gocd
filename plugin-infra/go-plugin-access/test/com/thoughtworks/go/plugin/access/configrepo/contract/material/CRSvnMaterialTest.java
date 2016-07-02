package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRSvnMaterialTest extends CRBaseTest<CRSvnMaterial> {

    private final CRSvnMaterial simpleSvn;
    private final CRSvnMaterial simpleSvnAuth;
    private final CRSvnMaterial customSvn;
    private final CRSvnMaterial invalidNoUrl;
    private final CRSvnMaterial invalidPasswordAndEncyptedPasswordSet;

    public CRSvnMaterialTest()
    {
        simpleSvn = new CRSvnMaterial();
        simpleSvn.setUrl("http://mypublicrepo");

        simpleSvnAuth = new CRSvnMaterial();
        simpleSvnAuth.setUrl("http://myprivaterepo");
        simpleSvnAuth.setUserName("john");
        simpleSvnAuth.setPassword("pa$sw0rd");

        customSvn = new CRSvnMaterial("svnMaterial1","destDir1", false,
                "http://svn","user1","pass1",true,false,"tools","lib");

        invalidNoUrl = new CRSvnMaterial();
        invalidPasswordAndEncyptedPasswordSet = new CRSvnMaterial();
        invalidPasswordAndEncyptedPasswordSet.setUrl("http://myprivaterepo");
        invalidPasswordAndEncyptedPasswordSet.setPassword("pa$sw0rd");
        invalidPasswordAndEncyptedPasswordSet.setEncryptedPassword("26t=$j64");
    }

    @Override
    public void addGoodExamples(Map<String, CRSvnMaterial> examples) {
        examples.put("simpleSvn",simpleSvn);
        examples.put("simpleSvnAuth",simpleSvnAuth);
        examples.put("customSvn",customSvn);
    }

    @Override
    public void addBadExamples(Map<String, CRSvnMaterial> examples) {
        examples.put("invalidNoUrl",invalidNoUrl);
        examples.put("invalidPasswordAndEncyptedPasswordSet",invalidPasswordAndEncyptedPasswordSet);
    }

    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        CRMaterial value = customSvn;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRSvnMaterial.TYPE_NAME));
    }
    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRMaterial value = customSvn;
        String json = gson.toJson(value);

        CRSvnMaterial deserializedValue = (CRSvnMaterial)gson.fromJson(json,CRMaterial.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
}
