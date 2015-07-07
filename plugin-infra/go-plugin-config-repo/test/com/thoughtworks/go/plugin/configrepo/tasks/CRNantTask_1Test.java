package com.thoughtworks.go.plugin.configrepo.tasks;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRNantTask_1Test extends CRBaseTest<CRNantTask_1> {

    private final CRNantTask_1 nantTask;
    private final CRNantTask_1 nantCompileFileTask;
    private final CRNantTask_1 nantCompileTask;
    private final CRNantTask_1 nantWithDirTask;
    private final CRNantTask_1 nantWithPath;

    public CRNantTask_1Test()
    {
        nantTask = CRBuildTask_1.nant();
        nantCompileFileTask = CRBuildTask_1.nant("mybuild.xml", "compile");
        nantCompileTask = CRBuildTask_1.nant(null, "compile");
        nantWithDirTask = CRBuildTask_1.nant(null, "build", "src/tasks");
        nantWithPath = CRBuildTask_1.nant("mybuild.xml", "build", "src/tasks","/path/to/nant");
    }

    @Override
    public void addGoodExamples(Map<String, CRNantTask_1> examples) {
        examples.put("nantTask",nantTask);
        examples.put("nantCompileFileTask",nantCompileFileTask);
        examples.put("nantCompileTask",nantCompileTask);
        examples.put("nantWithPath",nantWithPath);
        examples.put("nantWithDirTask",nantWithDirTask);
    }

    @Override
    public void addBadExamples(Map<String, CRNantTask_1> examples) {

    }

    @Test
    public void shouldAppendTypeFieldWhenSerializingNantTask()
    {
        CRTask_1 value = nantWithPath;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is("nant"));
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializingNantTask()
    {
        CRTask_1 value = nantTask;
        String json = gson.toJson(value);

        CRBuildTask_1 deserializedValue = (CRBuildTask_1)gson.fromJson(json,CRTask_1.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
}
