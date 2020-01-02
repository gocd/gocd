/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config;

import com.thoughtworks.go.service.TaskFactory;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OnCancelConfigTest {

    private TaskFactory taskFactory;

    @Before public void setUp() throws Exception {
        taskFactory = mock(TaskFactory.class);
    }

    @Test
    public void shouldReturnTheOnCancelOptionBasedOnWhatTheOnCancelTaskIs() {
        assertThat(new OnCancelConfig().onCancelOption(), is(""));
        assertThat(new OnCancelConfig(new ExecTask()).onCancelOption(), is("Custom Command"));
        assertThat(new OnCancelConfig(new AntTask()).onCancelOption(), is("Ant"));
        assertThat(new OnCancelConfig(new RakeTask()).onCancelOption(), is("Rake"));
    }

    @Test
    public void shouldAddErrorOnErrorCollection() {
        OnCancelConfig onCancelConfig = new OnCancelConfig();
        onCancelConfig.addError("key", "some error");
        assertThat(onCancelConfig.errors().on("key"), is("some error"));
    }

    @Test
    public void shouldSetPrimitiveAttributesForExecTask() {
        Map hashMap = new HashMap();
        hashMap.put(OnCancelConfig.ON_CANCEL_OPTIONS, "exec");
        Map valueMap = new HashMap();
        valueMap.put(ExecTask.COMMAND, "ls");
        valueMap.put(ExecTask.ARGS, "blah");
        valueMap.put(ExecTask.WORKING_DIR, "pwd");
        hashMap.put(OnCancelConfig.EXEC_ON_CANCEL, valueMap);
        hashMap.put(OnCancelConfig.ANT_ON_CANCEL, new HashMap());

        ExecTask execTask = new ExecTask();
        when(taskFactory.taskInstanceFor(execTask.getTaskType())).thenReturn(execTask);
        OnCancelConfig cancelConfig = OnCancelConfig.create(hashMap, taskFactory);

        assertThat(cancelConfig.getTask(), is(new ExecTask("ls", "blah", "pwd")));
    }

    @Test
    public void shouldSetPrimitiveAttributesForAntTask() {
        Map hashMap = new HashMap();
        hashMap.put(OnCancelConfig.ON_CANCEL_OPTIONS, "ant");
        Map valueMap = new HashMap();
        valueMap.put(BuildTask.BUILD_FILE, "build.xml");
        valueMap.put(BuildTask.TARGET, "blah");
        valueMap.put(BuildTask.WORKING_DIRECTORY, "pwd");
        hashMap.put(OnCancelConfig.ANT_ON_CANCEL, valueMap);
        hashMap.put(OnCancelConfig.EXEC_ON_CANCEL, new HashMap());


        when(taskFactory.taskInstanceFor(new AntTask().getTaskType())).thenReturn(new AntTask());
        OnCancelConfig cancelConfig = OnCancelConfig.create(hashMap, taskFactory);

        AntTask expectedAntTask = new AntTask();
        expectedAntTask.setBuildFile("build.xml");
        expectedAntTask.setTarget("blah");
        expectedAntTask.setWorkingDirectory("pwd");
        assertThat(cancelConfig.getTask(), is(expectedAntTask));
    }

    @Test
    public void shouldSetPrimitiveAttributesForNantTask() {
        Map hashMap = new HashMap();
        hashMap.put(OnCancelConfig.ON_CANCEL_OPTIONS, "nant");
        Map valueMap = new HashMap();
        valueMap.put(BuildTask.BUILD_FILE, "default.build");
        valueMap.put(BuildTask.TARGET, "compile");
        valueMap.put(BuildTask.WORKING_DIRECTORY, "pwd");
        valueMap.put(NantTask.NANT_PATH, "/usr/bin/nant");
        hashMap.put(OnCancelConfig.NANT_ON_CANCEL, valueMap);
        hashMap.put(OnCancelConfig.EXEC_ON_CANCEL, new HashMap());
        hashMap.put(OnCancelConfig.ANT_ON_CANCEL, new HashMap());
        hashMap.put(OnCancelConfig.RAKE_ON_CANCEL, new HashMap());

        when(taskFactory.taskInstanceFor(new NantTask().getTaskType())).thenReturn(new NantTask());
        OnCancelConfig cancelConfig = OnCancelConfig.create(hashMap, taskFactory);

        NantTask expectedNantTask = new NantTask();
        expectedNantTask.setBuildFile("default.build");
        expectedNantTask.setTarget("compile");
        expectedNantTask.setWorkingDirectory("pwd");
        expectedNantTask.setNantPath("/usr/bin/nant");
        assertThat(cancelConfig.getTask(), is(expectedNantTask));
    }

    @Test
    public void shouldSetPrimitiveAttributesForRakeTask() {
        Map hashMap = new HashMap();
        hashMap.put(OnCancelConfig.ON_CANCEL_OPTIONS, "rake");
        Map valueMap = new HashMap();
        valueMap.put(BuildTask.BUILD_FILE, "rakefile");
        valueMap.put(BuildTask.TARGET, "build");
        valueMap.put(BuildTask.WORKING_DIRECTORY, "pwd");
        hashMap.put(OnCancelConfig.RAKE_ON_CANCEL, valueMap);
        hashMap.put(OnCancelConfig.EXEC_ON_CANCEL, new HashMap());

        when(taskFactory.taskInstanceFor(new RakeTask().getTaskType())).thenReturn(new RakeTask());
        OnCancelConfig cancelConfig = OnCancelConfig.create(hashMap, taskFactory);

        RakeTask expectedRakeTask = new RakeTask();
        expectedRakeTask.setBuildFile("rakefile");
        expectedRakeTask.setTarget("build");
        expectedRakeTask.setWorkingDirectory("pwd");
        assertThat(cancelConfig.getTask(), is(expectedRakeTask));
    }
}
