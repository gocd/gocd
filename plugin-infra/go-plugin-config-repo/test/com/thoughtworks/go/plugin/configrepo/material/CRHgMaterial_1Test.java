package com.thoughtworks.go.plugin.configrepo.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRHgMaterial_1Test extends CRBaseTest<CRHgMaterial_1> {

    private final CRHgMaterial_1 simpleHg;
    private final CRHgMaterial_1 customHg;
    private final CRHgMaterial_1 invalidHgNoUrl;

    public CRHgMaterial_1Test()
    {
        simpleHg = new CRHgMaterial_1();
        simpleHg.setUrl("myHgRepo");

        customHg = new CRHgMaterial_1("hgMaterial1","dir1",false,"repos/myhg","externals","tools");

        invalidHgNoUrl = new CRHgMaterial_1();
    }

    @Override
    public void addGoodExamples(Map<String, CRHgMaterial_1> examples) {
        examples.put("simpleHg",simpleHg);
        examples.put("customHg",customHg);
    }

    @Override
    public void addBadExamples(Map<String, CRHgMaterial_1> examples) {
        examples.put("invalidHgNoUrl",invalidHgNoUrl);
    }

    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        CRMaterial_1 value = customHg;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRHgMaterial_1.TYPE_NAME));
    }
    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRMaterial_1 value = customHg;
        String json = gson.toJson(value);

        CRHgMaterial_1 deserializedValue = (CRHgMaterial_1)gson.fromJson(json,CRMaterial_1.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
}
