package com.thoughtworks.go.plugin.configrepo.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRPackageMaterial_1Test extends CRBaseTest<CRPackageMaterial_1> {

    private CRPackageMaterial_1 packageMaterial = new CRPackageMaterial_1("apt-package-plugin-id");
    private CRPackageMaterial_1 namedPackageMaterial = new CRPackageMaterial_1("myapt","apt-repo-id");

    private CRPackageMaterial_1 invalidPackageMaterialNoId = new CRPackageMaterial_1();

    @Override
    public void addGoodExamples(Map<String, CRPackageMaterial_1> examples) {
        examples.put("packageMaterial",packageMaterial);
        examples.put("namedPackageMaterial",namedPackageMaterial);
    }

    @Override
    public void addBadExamples(Map<String, CRPackageMaterial_1> examples) {
        examples.put("invalidPackageMaterialNoId",invalidPackageMaterialNoId);
    }

    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        CRMaterial_1 value = packageMaterial;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRPackageMaterial_1.TYPE_NAME));
    }
    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRMaterial_1 value = packageMaterial;
        String json = gson.toJson(value);

        CRPackageMaterial_1 deserializedValue = (CRPackageMaterial_1)gson.fromJson(json,CRMaterial_1.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
}
