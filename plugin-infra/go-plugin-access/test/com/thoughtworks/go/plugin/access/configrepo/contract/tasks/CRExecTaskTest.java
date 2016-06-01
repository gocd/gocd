package com.thoughtworks.go.plugin.access.configrepo.contract.tasks;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRBaseTest;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class CRExecTaskTest extends CRBaseTest<CRExecTask> {
    private final CRExecTask simpleExecWithArgs;
    private final CRExecTask execInDir;
    private final CRExecTask simpleExecRunIf;
    private final CRExecTask customExec;
    private final CRExecTask invalidNoCommand;
    private CRExecTask simpleExec;

    public CRExecTaskTest()
    {
        simpleExec = new CRExecTask("/usr/local/bin/ruby");
        simpleExecWithArgs = new CRExecTask("/usr/local/bin/ruby");
        simpleExecWithArgs.addArgument("backup.rb");

        simpleExecRunIf = new CRExecTask("/usr/local/bin/ruby");
        simpleExecRunIf.setRunIf(CRRunIf.failed);

        customExec = new CRExecTask("rake","dir",120L, CRRunIf.any,simpleExec,"-f","Rakefile.rb");

        execInDir = new CRExecTask("/usr/local/bin/rake");
        execInDir.setWorkingDirectory("myProjectDir");

        invalidNoCommand = new CRExecTask();
    }

    @Override
    public void addGoodExamples(Map<String, CRExecTask> examples) {
        examples.put("simpleExec",simpleExec);
        examples.put("simpleExecWithArgs",simpleExecWithArgs);
        examples.put("execInDir",execInDir);
        examples.put("simpleExecRunIf",simpleExecRunIf);
        examples.put("customExec",customExec);
    }

    @Override
    public void addBadExamples(Map<String, CRExecTask> examples) {
        examples.put("invalidNoCommand",invalidNoCommand);
    }


    @Test
    public void shouldAppendTypeFieldWhenSerializingTasks()
    {
        CRTask value = simpleExecWithArgs;
        JsonObject jsonObject = (JsonObject)gson.toJsonTree(value);
        assertThat(jsonObject.get("type").getAsString(), is(CRExecTask.TYPE_NAME));
    }
    @Test
    public void shouldHandlePolymorphismWhenDeserializing()
    {
        CRTask value = simpleExecWithArgs;
        String json = gson.toJson(value);

        CRExecTask deserializedValue = (CRExecTask)gson.fromJson(json,CRTask.class);
        assertThat(String.format("Deserialized value should equal to value before serialization"),
                deserializedValue,is(value));
    }
}
