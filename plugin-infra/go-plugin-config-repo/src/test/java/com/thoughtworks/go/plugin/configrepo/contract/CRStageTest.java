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
package com.thoughtworks.go.plugin.configrepo.contract;

import com.thoughtworks.go.plugin.configrepo.contract.tasks.CRBuildTask;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static com.thoughtworks.go.util.TestUtils.contains;
import static org.junit.Assert.assertThat;

public class CRStageTest extends AbstractCRTest<CRStage> {

    private final CRStage stage;
    private final CRStage stageWith2Jobs;
    private final CRStage stageWithEnv;
    private final CRStage stageWithApproval;
    private final CRStage invalidNoName;
    private final CRStage invalidNoJobs;
    private final CRStage invalidSameEnvironmentVariableTwice;
    private final CRStage invalidSameJobNameTwice;

    public CRStageTest() {
        CRBuildTask rakeTask = CRBuildTask.rake();
        CRBuildTask antTask = CRBuildTask.ant();
        CRJob buildRake = new CRJob("build");
        buildRake.addTask(rakeTask);

        CRJob build2Rakes = new CRJob("build");
        build2Rakes.addTask(rakeTask);
        build2Rakes.addTask(CRBuildTask.rake("Rakefile.rb", "compile"));

        CRJob jobWithVar = new CRJob("build");
        jobWithVar.addTask(rakeTask);
        jobWithVar.addEnvironmentVariable("key1", "value1");

        CRJob jobWithResource = new CRJob("test");
        jobWithResource.addTask(antTask);
        jobWithResource.addResource("linux");

        stage = new CRStage("build");
        stage.addJob(buildRake);

        stageWith2Jobs = new CRStage("build");
        stageWith2Jobs.addJob(build2Rakes);
        stageWith2Jobs.addJob(jobWithResource);

        stageWithEnv = new CRStage("test");
        stageWithEnv.addEnvironmentVariable("TEST_NUM", "1");
        stageWithEnv.addJob(jobWithResource);

        CRApproval manualWithAuth = new CRApproval(CRApprovalCondition.manual);
        manualWithAuth.setRoles(Arrays.asList("manager"));
        stageWithApproval = new CRStage("deploy");
        stageWithApproval.setApproval(manualWithAuth);
        stageWithApproval.addJob(buildRake);

        invalidNoName = new CRStage();
        invalidNoName.addJob(jobWithResource);

        invalidNoJobs = new CRStage("build");

        invalidSameEnvironmentVariableTwice = new CRStage("build");
        invalidSameEnvironmentVariableTwice.addEnvironmentVariable("key", "value1");
        invalidSameEnvironmentVariableTwice.addEnvironmentVariable("key", "value2");
        invalidSameEnvironmentVariableTwice.addJob(buildRake);

        invalidSameJobNameTwice = new CRStage("build");
        invalidSameJobNameTwice.addJob(buildRake);
        invalidSameJobNameTwice.addJob(build2Rakes);
    }

    @Override
    public void addGoodExamples(Map<String, CRStage> examples) {
        examples.put("stage", stage);
        examples.put("stageWith2Jobs", stageWith2Jobs);
        examples.put("stageWithEnv", stageWithEnv);
        examples.put("stageWithApproval", stageWithApproval);
    }

    @Override
    public void addBadExamples(Map<String, CRStage> examples) {
        examples.put("invalidNoName", invalidNoName);
        examples.put("invalidNoJobs", invalidNoJobs);
        examples.put("invalidSameEnvironmentVariableTwice", invalidSameEnvironmentVariableTwice);
        examples.put("invalidSameJobNameTwice", invalidSameJobNameTwice);
    }

    @Test
    public void shouldCheckErrorsInJobs() {
        CRStage withNamelessJob = new CRStage("build");
        withNamelessJob.addJob(new CRJob());

        ErrorCollection errors = new ErrorCollection();
        withNamelessJob.getErrors(errors, "TEST");

        String fullError = errors.getErrorsAsText();

        assertThat(fullError, contains("TEST; Stage (build)"));
        assertThat(fullError, contains("Missing field 'name'."));
    }
}
