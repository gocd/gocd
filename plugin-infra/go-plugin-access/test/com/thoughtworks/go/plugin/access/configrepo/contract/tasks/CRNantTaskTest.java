package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRNantTaskTest extends CRBaseTest<CRNantTask> {

    private final CRNantTask nantTask;
    private final CRNantTask nantCompileFileTask;
    private final CRNantTask nantCompileTask;
    private final CRNantTask nantWithDirTask;
    private final CRNantTask nantWithPath;

    public CRNantTaskTest()
    {
        nantTask = CRBuildTask.nant();
        nantCompileFileTask = CRBuildTask.nant("mybuild.xml", "compile");
        nantCompileTask = CRBuildTask.nant(null, "compile");
        nantWithDirTask = CRBuildTask.nant(null, "build", "src/tasks");
        nantWithPath = CRBuildTask.nant("mybuild.xml", "build", "src/tasks","/path/to/nant");
    }

    @Override
    public void addGoodExamples(Map<String, CRNantTask> examples) {
        examples.put("nantTask",nantTask);
        examples.put("nantCompileFileTask",nantCompileFileTask);
        examples.put("nantCompileTask",nantCompileTask);
        examples.put("nantWithPath",nantWithPath);
        examples.put("nantWithDirTask",nantWithDirTask);
    }

    @Override
    public void addBadExamples(Map<String, CRNantTask> examples) {

    }

    @Test
    public void shouldAppendTypeFieldWhenSerializingNantTask()
    {
        CRTask value = nantWithPath;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is("nant"));
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializingNantTask()
    {
        CRTask value = nantTask;
        String json = gson.toJson(value);

        CRBuildTask deserializedValue = (CRBuildTask)gson.fromJson(json,CRTask.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
}
