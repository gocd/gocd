package com.thoughtworks.go.plugin.configrepo;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.material.CRDependencyMaterial_1;
import com.thoughtworks.go.plugin.configrepo.material.CRMaterial_1;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.Map;

import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRMaterial_1Test extends CRBaseTest<CRMaterial_1> {

    private CRDependencyMaterial_1 dependsOnPipeline;

    public CRMaterial_1Test()
    {
        dependsOnPipeline = new CRDependencyMaterial_1("pipe1","pipeline1","build");
    }

    @Override
    public void addGoodExamples(Map<String, CRMaterial_1> examples) {
        examples.put("dependsOnPipeline",dependsOnPipeline);
    }

    @Override
    public void addBadExamples(Map<String, CRMaterial_1> examples) {

    }

    @Test
    public void shouldAppendTypeFieldWhenSerializingMaterials()
    {
        Map<String, CRMaterial_1> examples = getExamples();
        for(Map.Entry<String,CRMaterial_1> example : examples.entrySet()) {
            CRMaterial_1 value = example.getValue();
            JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
            assertNotNull(jsonObject.get("type"));
        }
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializing_CRDependencyMaterial_1()
    {
        Map<String, CRMaterial_1> examples = getExamples();
        for(Map.Entry<String,CRMaterial_1> example : examples.entrySet())
        {
            CRMaterial_1 value = example.getValue();
            String json = gson.toJson(value);

            CRDependencyMaterial_1 deserializedValue = (CRDependencyMaterial_1)gson.fromJson(json,CRMaterial_1.class);
            assertThat(String.format("Example %s - Deserialized value should equal to value before serialization",example.getKey()),
                    deserializedValue,is(value));
        }
    }

}
