/*
 * Copyright 2021 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.plugin.configrepo.contract.tasks;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.configrepo.contract.AbstractCRTest;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;

public class BuildTaskTest extends AbstractCRTest<CRBuildTask> {

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

        invalidTaskNoType = new CRBuildTask(null, null, null, null, null, null);
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
        assertThat("Deserialized value should equal to value before serialization",
                deserializedValue,is(value));
    }
    @Test
    public void shouldHandlePolymorphismWhenDeserializingRakeTask()
    {
        CRTask value = rakeTask;
        String json = gson.toJson(value);

        CRBuildTask deserializedValue = (CRBuildTask)gson.fromJson(json,CRTask.class);
        assertThat("Deserialized value should equal to value before serialization",
                deserializedValue,is(value));
    }
}
