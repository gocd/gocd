package com.thoughtworks.go.plugin.configrepo.material;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRDependencyMaterial_1Test extends CRBaseTest<CRDependencyMaterial_1> {

    private final CRDependencyMaterial_1 namedDependsOnPipeline;
    private final CRDependencyMaterial_1 invalidNoPipeline;
    private final CRDependencyMaterial_1 invalidNoStage;
    private CRDependencyMaterial_1 dependsOnPipeline;

    public CRDependencyMaterial_1Test()
    {
        namedDependsOnPipeline = new CRDependencyMaterial_1("pipe2","pipeline2","build");
        dependsOnPipeline = new CRDependencyMaterial_1("pipeline2","build");

        invalidNoPipeline = new CRDependencyMaterial_1();
        invalidNoPipeline.setStageName("build");

        invalidNoStage = new CRDependencyMaterial_1();
        invalidNoStage.setPipelineName("pipeline1");
    }

    @Override
    public void addGoodExamples(Map<String, CRDependencyMaterial_1> examples) {
        examples.put("dependsOnPipeline",dependsOnPipeline);
        examples.put("namedDependsOnPipeline",namedDependsOnPipeline);
    }

    @Override
    public void addBadExamples(Map<String, CRDependencyMaterial_1> examples) {
        examples.put("invalidNoPipeline",invalidNoPipeline);
        examples.put("invalidNoStage",invalidNoStage);
    }


    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        CRMaterial_1 value = dependsOnPipeline;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRDependencyMaterial_1.TYPE_NAME));
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRMaterial_1 value = dependsOnPipeline;
        String json = gson.toJson(value);

        CRDependencyMaterial_1 deserializedValue = (CRDependencyMaterial_1)gson.fromJson(json,CRMaterial_1.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }

}
