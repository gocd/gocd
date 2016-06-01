package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRPackageMaterialTest extends CRBaseTest<CRPackageMaterial> {

    private CRPackageMaterial packageMaterial = new CRPackageMaterial("apt-package-plugin-id");
    private CRPackageMaterial namedPackageMaterial = new CRPackageMaterial("myapt","apt-repo-id");

    private CRPackageMaterial invalidPackageMaterialNoId = new CRPackageMaterial();

    @Override
    public void addGoodExamples(Map<String, CRPackageMaterial> examples) {
        examples.put("packageMaterial",packageMaterial);
        examples.put("namedPackageMaterial",namedPackageMaterial);
    }

    @Override
    public void addBadExamples(Map<String, CRPackageMaterial> examples) {
        examples.put("invalidPackageMaterialNoId",invalidPackageMaterialNoId);
    }

    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        CRMaterial value = packageMaterial;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRPackageMaterial.TYPE_NAME));
    }
    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRMaterial value = packageMaterial;
        String json = gson.toJson(value);

        CRPackageMaterial deserializedValue = (CRPackageMaterial)gson.fromJson(json,CRMaterial.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
}
