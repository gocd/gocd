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
package com.thoughtworks.go.plugin.configrepo.contract;

import com.thoughtworks.go.plugin.configrepo.contract.tasks.CRBuildTask;
import com.thoughtworks.go.plugin.configrepo.contract.tasks.CRTask;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;

public class CRJobTest extends AbstractCRTest<CRJob> {

    private final CRBuildTask rakeTask = CRBuildTask.rake();
    private final CRBuildTask antTask = CRBuildTask.ant();

    private final CRJob buildRake;
    private final CRJob build2Rakes;
    private final CRJob jobWithVar;
    private final CRJob jobWithResource;
    private final CRJob jobWithTab;
    private final CRJob jobWithProp;
    private final CRJob invalidJobNoName;
    private final CRJob invalidJobResourcesAndElasticProfile;

    public CRJobTest() {
        buildRake = new CRJob("build");
        buildRake.addTask(rakeTask);
        build2Rakes = new CRJob("build");
        build2Rakes.addTask(rakeTask);
        build2Rakes.addTask(CRBuildTask.rake("Rakefile.rb", "compile"));

        jobWithVar = new CRJob("build");
        jobWithVar.addTask(rakeTask);
        jobWithVar.addEnvironmentVariable("key1", "value1");

        jobWithResource = new CRJob("test");
        jobWithResource.addTask(antTask);
        jobWithResource.addResource("linux");

        jobWithTab = new CRJob("test");
        jobWithTab.addTask(antTask);
        jobWithTab.addTab(new CRTab("test", "results.xml"));

        jobWithProp = new CRJob("perfTest");
        jobWithProp.addTask(rakeTask);

        invalidJobNoName = new CRJob();

        invalidJobResourcesAndElasticProfile = new CRJob("build");
        invalidJobResourcesAndElasticProfile.addTask(rakeTask);
        invalidJobResourcesAndElasticProfile.addResource("linux");
        invalidJobResourcesAndElasticProfile.setElasticProfileId("profile");
    }

    @Override
    public void addGoodExamples(Map<String, CRJob> examples) {
        examples.put("buildRake", buildRake);
        examples.put("build2Rakes", build2Rakes);
        examples.put("jobWithVar", jobWithVar);
        examples.put("jobWithResource", jobWithResource);
        examples.put("jobWithTab", jobWithTab);
        examples.put("jobWithProp", jobWithProp);
    }

    @Override
    public void addBadExamples(Map<String, CRJob> examples) {
        examples.put("invalidJobNoName", invalidJobNoName);
        examples.put("invalidJobResourcesAndElasticProfile", invalidJobResourcesAndElasticProfile);
    }


    @Test
    public void shouldHandlePolymorphismWhenDeserializingTasks() {
        String json = gson.toJson(build2Rakes);

        CRJob deserializedValue = gson.fromJson(json, CRJob.class);

        CRTask task1 = deserializedValue.getTasks().get(1);
        assertThat(task1 instanceof CRBuildTask, is(true));
        assertThat(((CRBuildTask) task1).getBuildFile(), is("Rakefile.rb"));
    }
}
