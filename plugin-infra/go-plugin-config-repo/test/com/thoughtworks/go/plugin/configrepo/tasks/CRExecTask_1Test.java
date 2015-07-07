package com.thoughtworks.go.plugin.configrepo.tasks;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRExecTask_1Test extends CRBaseTest<CRExecTask_1> {
    private final CRExecTask_1 simpleExecWithArgs;
    private final CRExecTask_1 execInDir;
    private final CRExecTask_1 simpleExecRunIf;
    private final CRExecTask_1 customExec;
    private final CRExecTask_1 invalidNoCommand;
    private CRExecTask_1 simpleExec;

    public CRExecTask_1Test()
    {
        simpleExec = new CRExecTask_1("/usr/local/bin/ruby");
        simpleExecWithArgs = new CRExecTask_1("/usr/local/bin/ruby");
        simpleExecWithArgs.addArgument("backup.rb");

        simpleExecRunIf = new CRExecTask_1("/usr/local/bin/ruby");
        simpleExecRunIf.setRunIf(CRRunIf_1.failed);

        customExec = new CRExecTask_1("rake","dir",120L, CRRunIf_1.any,simpleExec,"-f","Rakefile.rb");

        execInDir = new CRExecTask_1("/usr/local/bin/rake");
        execInDir.setWorkingDirectory("myProjectDir");

        invalidNoCommand = new CRExecTask_1();
    }

    @Override
    public void addGoodExamples(Map<String, CRExecTask_1> examples) {
        examples.put("simpleExec",simpleExec);
        examples.put("simpleExecWithArgs",simpleExecWithArgs);
        examples.put("execInDir",execInDir);
        examples.put("simpleExecRunIf",simpleExecRunIf);
        examples.put("customExec",customExec);
    }

    @Override
    public void addBadExamples(Map<String, CRExecTask_1> examples) {
        examples.put("invalidNoCommand",invalidNoCommand);
    }


    @Test
    public void shouldAppendTypeFieldWhenSerializingTasks()
    {
        CRTask_1 value = simpleExecWithArgs;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRExecTask_1.TYPE_NAME));
    }
    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRTask_1 value = simpleExecWithArgs;
        String json = gson.toJson(value);

        CRExecTask_1 deserializedValue = (CRExecTask_1)gson.fromJson(json,CRTask_1.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
}
