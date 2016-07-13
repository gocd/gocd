package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRConfigMaterialTest extends CRBaseTest<CRConfigMaterial> {
    private final CRConfigMaterial named;
    private final CRConfigMaterial namedDest;

    public CRConfigMaterialTest() {
        named = new CRConfigMaterial("primary", null);
        namedDest = new CRConfigMaterial("primary", "folder");
    }

    @Override
    public void addGoodExamples(Map<String, CRConfigMaterial> examples) {
        examples.put("namedExample", named);
        examples.put("namedDest", namedDest);
    }

    @Override
    public void addBadExamples(Map<String, CRConfigMaterial> examples) {
    }

    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        CRMaterial value = named;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRConfigMaterial.TYPE_NAME));
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRMaterial value = named;
        String json = gson.toJson(value);

        CRConfigMaterial deserializedValue = (CRConfigMaterial)gson.fromJson(json,CRMaterial.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }

}