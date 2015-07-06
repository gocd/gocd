package com.thoughtworks.go.plugin.configrepo.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRP4Material_1Test extends CRBaseTest<CRP4Material_1> {

    private final String exampleView = "//depot/dev/src...          //anything/src/...";

    private final CRP4Material_1 p4simple;
    private final CRP4Material_1 p4custom;
    private final CRP4Material_1 invalidP4NoView;
    private final CRP4Material_1 invalidP4NoServer;
    private final CRP4Material_1 invalidPasswordAndEncyptedPasswordSet;


    public CRP4Material_1Test()
    {
        p4simple = new CRP4Material_1("10.18.3.102:1666",exampleView);

        p4custom= new CRP4Material_1(
                "p4materialName", "dir1", false,"10.18.3.102:1666",exampleView,"user1","pass1", false,"lib","tools");

        invalidP4NoView = new CRP4Material_1("10.18.3.102:1666",null);
        invalidP4NoServer =  new CRP4Material_1(null,exampleView);
        invalidPasswordAndEncyptedPasswordSet = new CRP4Material_1("10.18.3.102:1666",null);
        invalidPasswordAndEncyptedPasswordSet.setPassword("pa$sw0rd");
        invalidPasswordAndEncyptedPasswordSet.setEncryptedPassword("26t=$j64");
    }

    @Override
    public void addGoodExamples(Map<String, CRP4Material_1> examples) {
        examples.put("p4simple",p4simple);
        examples.put("p4custom",p4custom);
    }

    @Override
    public void addBadExamples(Map<String, CRP4Material_1> examples) {
        examples.put("invalidP4NoView",invalidP4NoView);
        examples.put("invalidP4NoServer",invalidP4NoServer);
        examples.put("invalidPasswordAndEncyptedPasswordSet",invalidPasswordAndEncyptedPasswordSet);
    }


    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        CRMaterial_1 value = p4custom;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRP4Material_1.TYPE_NAME));
    }
    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRMaterial_1 value = p4custom;
        String json = gson.toJson(value);

        CRP4Material_1 deserializedValue = (CRP4Material_1)gson.fromJson(json,CRMaterial_1.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
}
