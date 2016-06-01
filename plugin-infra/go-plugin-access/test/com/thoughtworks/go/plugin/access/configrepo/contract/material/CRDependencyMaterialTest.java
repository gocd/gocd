package com.thoughtworks.go.plugin.access.configrepo.contract.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRDependencyMaterialTest extends CRBaseTest<CRDependencyMaterial> {

    private final CRDependencyMaterial namedDependsOnPipeline;
    private final CRDependencyMaterial invalidNoPipeline;
    private final CRDependencyMaterial invalidNoStage;
    private CRDependencyMaterial dependsOnPipeline;

    public CRDependencyMaterialTest()
    {
        namedDependsOnPipeline = new CRDependencyMaterial("pipe2","pipeline2","build");
        dependsOnPipeline = new CRDependencyMaterial("pipeline2","build");

        invalidNoPipeline = new CRDependencyMaterial();
        invalidNoPipeline.setStageName("build");

        invalidNoStage = new CRDependencyMaterial();
        invalidNoStage.setPipelineName("pipeline1");
    }

    @Override
    public void addGoodExamples(Map<String, CRDependencyMaterial> examples) {
        examples.put("dependsOnPipeline",dependsOnPipeline);
        examples.put("namedDependsOnPipeline",namedDependsOnPipeline);
    }

    @Override
    public void addBadExamples(Map<String, CRDependencyMaterial> examples) {
        examples.put("invalidNoPipeline",invalidNoPipeline);
        examples.put("invalidNoStage",invalidNoStage);
    }


    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        CRMaterial value = dependsOnPipeline;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRDependencyMaterial.TYPE_NAME));
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRMaterial value = dependsOnPipeline;
        String json = gson.toJson(value);

        CRDependencyMaterial deserializedValue = (CRDependencyMaterial)gson.fromJson(json,CRMaterial.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }

}
