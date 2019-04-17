/*
 * Copyright 2017 ThoughtWorks, Inc.
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

public class CRNantTaskTest extends AbstractCRTest<CRNantTask> {

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
        assertThat("Deserialized value should equal to value before serialization",
                deserializedValue,is(value));
    }
}
