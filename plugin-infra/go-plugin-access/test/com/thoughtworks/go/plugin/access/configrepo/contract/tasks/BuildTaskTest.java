package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class BuildTaskTest extends CRBaseTest<CRBuildTask> {

    private final CRBuildTask rakeTask;
    private final CRBuildTask rakeCompileTask;
    private final CRBuildTask rakeCompileFileTask;

    private final CRBuildTask invalidTaskNoType;
    private final CRBuildTask rakeWithDirTask;
    private final CRBuildTask antTask;
    private final CRBuildTask antCompileFileTask;
    private final CRBuildTask antCompileTask;
    private final CRBuildTask antWithDirTask;

    public BuildTaskTest(){
        rakeTask = CRBuildTask.rake();
        rakeCompileFileTask = CRBuildTask.rake("Rakefile.rb","compile");
        rakeCompileTask = CRBuildTask.rake(null,"compile");
        rakeWithDirTask = CRBuildTask.rake(null,"build","src/tasks");

        antTask = CRBuildTask.ant();
        antCompileFileTask = CRBuildTask.ant("mybuild.xml", "compile");
        antCompileTask = CRBuildTask.ant(null, "compile");
        antWithDirTask = CRBuildTask.ant(null, "build", "src/tasks");

        invalidTaskNoType = new CRBuildTask(null,null,null,null);
    }

    @Override
    public void addGoodExamples(Map<String, CRBuildTask> examples) {
        examples.put("rakeTask",rakeTask);
        examples.put("rakeCompileFileTask",rakeCompileFileTask);
        examples.put("rakeCompileTask",rakeCompileTask);
        examples.put("rakeWithDirTask",rakeWithDirTask);

        examples.put("antTask",antTask);
        examples.put("antCompileFileTask",antCompileFileTask);
        examples.put("antCompileTask",antCompileTask);
        examples.put("antWithDirTask",antWithDirTask);
    }

    @Override
    public void addBadExamples(Map<String, CRBuildTask> examples) {
        examples.put("invalidTaskNoType",invalidTaskNoType);
    }



    @Test
    public void shouldAppendTypeFieldWhenSerializingAntTask()
    {
        CRTask value = antTask;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is("ant"));
    }
    @Test
    public void shouldAppendTypeFieldWhenSerializingRakeTask()
    {
        CRTask value = rakeTask;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is("rake"));
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializingAntTask()
    {
        CRTask value = antTask;
        String json = gson.toJson(value);

        CRBuildTask deserializedValue = (CRBuildTask)gson.fromJson(json,CRTask.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
    @Test
    public void shouldHandlePolymorphismWhenDeserializingRakeTask()
    {
        CRTask value = rakeTask;
        String json = gson.toJson(value);

        CRBuildTask deserializedValue = (CRBuildTask)gson.fromJson(json,CRTask.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
}
