package com.thoughtworks.go.plugin.configrepo.tasks;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class BuildTask_1Test extends CRBaseTest<CRBuildTask_1> {

    private final CRBuildTask_1 rakeTask;
    private final CRBuildTask_1 rakeCompileTask;
    private final CRBuildTask_1 rakeCompileFileTask;

    private final CRBuildTask_1 invalidTaskNoType;
    private final CRBuildTask_1 rakeWithDirTask;
    private final CRBuildTask_1 antTask;
    private final CRBuildTask_1 antCompileFileTask;
    private final CRBuildTask_1 antCompileTask;
    private final CRBuildTask_1 antWithDirTask;

    public BuildTask_1Test(){
        rakeTask = CRBuildTask_1.rake();
        rakeCompileFileTask = CRBuildTask_1.rake("Rakefile.rb","compile");
        rakeCompileTask = CRBuildTask_1.rake(null,"compile");
        rakeWithDirTask = CRBuildTask_1.rake(null,"build","src/tasks");

        antTask = CRBuildTask_1.ant();
        antCompileFileTask = CRBuildTask_1.ant("mybuild.xml", "compile");
        antCompileTask = CRBuildTask_1.ant(null, "compile");
        antWithDirTask = CRBuildTask_1.ant(null, "build", "src/tasks");

        invalidTaskNoType = new CRBuildTask_1(null,null,null,null);
    }

    @Override
    public void addGoodExamples(Map<String, CRBuildTask_1> examples) {
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
    public void addBadExamples(Map<String, CRBuildTask_1> examples) {
        examples.put("invalidTaskNoType",invalidTaskNoType);
    }



    @Test
    public void shouldAppendTypeFieldWhenSerializingAntTask()
    {
        CRTask_1 value = antTask;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is("ant"));
    }
    @Test
    public void shouldAppendTypeFieldWhenSerializingRakeTask()
    {
        CRTask_1 value = rakeTask;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is("rake"));
    }

    @Test
    public void shouldHandlePolymorphismWhenDeserializingAntTask()
    {
        CRTask_1 value = antTask;
        String json = gson.toJson(value);

        CRBuildTask_1 deserializedValue = (CRBuildTask_1)gson.fromJson(json,CRTask_1.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
    @Test
    public void shouldHandlePolymorphismWhenDeserializingRakeTask()
    {
        CRTask_1 value = rakeTask;
        String json = gson.toJson(value);

        CRBuildTask_1 deserializedValue = (CRBuildTask_1)gson.fromJson(json,CRTask_1.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
}
