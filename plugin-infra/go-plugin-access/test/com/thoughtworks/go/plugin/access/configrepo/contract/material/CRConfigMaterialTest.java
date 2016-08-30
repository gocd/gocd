package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRBaseTest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRConfigMaterialTest extends CRBaseTest<CRConfigMaterial> {
    private final CRConfigMaterial named;
    private final CRConfigMaterial namedDest;
    private final CRConfigMaterial blacklist;
    private final CRConfigMaterial invalidList;

    public CRConfigMaterialTest() {
        named = new CRConfigMaterial("primary", null,null);
        namedDest = new CRConfigMaterial("primary", "folder",null);
        List<String> patterns = new ArrayList<>();
        patterns.add("externals");
        patterns.add("tools");
        blacklist = new CRConfigMaterial("primary", "folder",new CRFilter(patterns,false));

        CRFilter badFilter = new CRFilter(patterns,false);
        badFilter.setWhitelistNoCheck(patterns);
        invalidList = new CRConfigMaterial("primary", "folder",badFilter);
    }

    @Override
    public void addGoodExamples(Map<String, CRConfigMaterial> examples) {
        examples.put("namedExample", named);
        examples.put("namedDest", namedDest);
        examples.put("blacklist", blacklist);
    }

    @Override
    public void addBadExamples(Map<String, CRConfigMaterial> examples) {
        examples.put("invalidList",invalidList);
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