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

package com.thoughtworks.go.server.controller.beans;

import com.thoughtworks.go.config.AntTask;
import com.thoughtworks.go.config.ArtifactPlan;
import com.thoughtworks.go.config.ArtifactPlans;
import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.config.NantTask;
import com.thoughtworks.go.config.RakeTask;
import com.thoughtworks.go.config.TestArtifactPlan;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class PipelineBeanTest {
        private PipelineBean builderBean(String buildfile, String target, String builder) {
        return new PipelineBean("", null, builder, buildfile, target, null, null, null, null, null);
    }

    private PipelineBean execBean(String builder, String command, String arguments) {
        return new PipelineBean("", null, builder, null, null, null, null, null, command, arguments);
    }

    @Test
    public void shouldReturnAntTasks() {
        PipelineBean antBean = builderBean("oldbuild.xml", "clean", "ant");
        assertThat(antBean.getTasks().get(0), instanceOf(AntTask.class));
    }

    @Test
    public void shouldReturnArtifact() throws Exception {
        String[] src = new String[]{"log/log.xml",   "logoutput/log.xml"};
        String[] dest = new String[]{"test/test1",   ""};
        String[] type = new String[]{"artifact",     "test"};
        PipelineBean bean = new PipelineBean("", null, "ant", "build.xml", "clean", src, dest, type, null, null);
        ArtifactPlans artifactPlans = bean.getArtifactPlans();
        ArtifactPlan artifactPlan = artifactPlans.get(0);
        assertThat(artifactPlan.getSrc(), is("log/log.xml"));
        assertThat(artifactPlan.getDest(), is("test/test1"));
        assertThat(artifactPlan, is(instanceOf(ArtifactPlan.class)));
    }

    @Test
    public void shouldReturnTestArtifact() throws Exception {
        String[] src = new String[]{"log/log.xml",   "logoutput/log.xml"};
        String[] dest = new String[]{"test/test1",   ""};
        String[] type = new String[]{"artifact",     "test"};
        PipelineBean bean = new PipelineBean("", null, "ant", "build.xml", "clean", src, dest, type, null, null);
        ArtifactPlans artifactPlans = bean.getArtifactPlans();
        ArtifactPlan artifactPlan = artifactPlans.get(1);
        assertThat(artifactPlan.getSrc(), is("logoutput/log.xml"));
        assertThat(artifactPlan.getDest(), is(""));
        assertThat(artifactPlan, is(instanceOf(TestArtifactPlan.class)));
    }

    @Test
    public void shouldReturnRakeTasks() {
        PipelineBean rakeBean = builderBean("mybuild.rb", "clean", "rake");
        assertThat(rakeBean.getTasks().get(0), instanceOf(RakeTask.class));
    }

    @Test
    public void shouldReturnNantTasks() {
        PipelineBean nantBean = builderBean("oldbuild.xml", "clean", "nant");
        assertThat(nantBean.getTasks().get(0), instanceOf(NantTask.class));
    }

    @Test
    public void shouldReturnExecTasksWithParameters() {
        PipelineBean execBean = execBean("exec", "java", "-DDEBUG_MODE=true -Xms=1024 -jar agent.jar");
        ExecTask execTask = new ExecTask("java", "-DDEBUG_MODE=true -Xms=1024 -jar agent.jar", (String) null);
        assertThat(execBean.getTasks().get(0), is(execTask));
    }

    @Test
    public void shouldReturnExecTasksWithoutParameters() {
        PipelineBean execBean = execBean("exec", "ls", null);
        ExecTask execTask = new ExecTask("ls", "", (String) null);
        assertThat(execBean.getTasks().get(0), is(execTask));

        execBean = execBean("exec", " ls ", null);
        execTask = new ExecTask("ls", "", (String) null);
        assertThat(execBean.getTasks().get(0), is(execTask));
    }
}
