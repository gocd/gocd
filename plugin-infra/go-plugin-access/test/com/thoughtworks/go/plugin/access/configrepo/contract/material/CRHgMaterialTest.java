package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRHgMaterialTest extends CRBaseTest<CRHgMaterial> {

    private final CRHgMaterial simpleHg;
    private final CRHgMaterial customHg;
    private final CRHgMaterial invalidHgNoUrl;
    private final CRHgMaterial invalidHgWhitelistAndIgnores;

    public CRHgMaterialTest()
    {
        simpleHg = new CRHgMaterial();
        simpleHg.setUrl("myHgRepo");

        customHg = new CRHgMaterial("hgMaterial1","dir1",false,"repos/myhg",false,"externals","tools");

        invalidHgNoUrl = new CRHgMaterial();
        invalidHgWhitelistAndIgnores = new CRHgMaterial("hgMaterial1","dir1",false,"repos/myhg",false,"externals","tools");
        invalidHgWhitelistAndIgnores.setWhitelistNoCheck("src","tests");
    }

    @Override
    public void addGoodExamples(Map<String, CRHgMaterial> examples) {
        examples.put("simpleHg",simpleHg);
        examples.put("customHg",customHg);
    }

    @Override
    public void addBadExamples(Map<String, CRHgMaterial> examples) {
        examples.put("invalidHgNoUrl",invalidHgNoUrl);
        examples.put("invalidHgWhitelistAndIgnores",invalidHgWhitelistAndIgnores);
    }

    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        CRMaterial value = customHg;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRHgMaterial.TYPE_NAME));
    }
    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRMaterial value = customHg;
        String json = gson.toJson(value);

        CRHgMaterial deserializedValue = (CRHgMaterial)gson.fromJson(json,CRMaterial.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
}
