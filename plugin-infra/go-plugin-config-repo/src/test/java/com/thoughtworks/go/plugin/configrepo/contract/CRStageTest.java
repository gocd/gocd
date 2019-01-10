/*
 * Copyright 2018 ThoughtWorks, Inc.
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

public class CRStageTest extends CRBaseTest<CRStage> {

    private final CRStage stage;
    private final CRStage stageWith2Jobs;
    private final CRStage stageWithEnv;
    private final CRStage stageWithApproval;
    private final CRStage invalidNoName;
    private final CRStage invalidNoJobs;
    private final CRStage invalidSameEnvironmentVariableTwice;
    private final CRStage invalidSameJobNameTwice;

    public CRStageTest()
    {
        CRBuildTask rakeTask = CRBuildTask.rake();
        CRBuildTask antTask = CRBuildTask.ant();
        CRJob buildRake = new CRJob("build", rakeTask);
        CRJob build2Rakes = new CRJob("build", rakeTask, CRBuildTask.rake("Rakefile.rb", "compile"));

        CRJob jobWithVar = new CRJob("build", rakeTask);
        jobWithVar.addEnvironmentVariable("key1","value1");

        CRJob jobWithResource = new CRJob("test", antTask);
        jobWithResource.addResource("linux");

        stage = new CRStage("build",buildRake);
        stageWith2Jobs = new CRStage("build",build2Rakes,jobWithResource);
        stageWithEnv = new CRStage("test",jobWithResource);
        stageWithEnv.addEnvironmentVariable("TEST_NUM","1");

        CRApproval manualWithAuth = new CRApproval(CRApprovalCondition.manual);
        manualWithAuth.setAuthorizedRoles(Arrays.asList("manager"));
        stageWithApproval = new CRStage("deploy",buildRake);
        stageWithApproval.setApproval(manualWithAuth);

        invalidNoName = new CRStage(null,jobWithResource);
        invalidNoJobs = new CRStage("build");

        invalidSameEnvironmentVariableTwice = new CRStage("build",buildRake);
        invalidSameEnvironmentVariableTwice.addEnvironmentVariable("key","value1");
        invalidSameEnvironmentVariableTwice.addEnvironmentVariable("key","value2");

        invalidSameJobNameTwice = new CRStage("build",buildRake,build2Rakes);
    }

    @Override
    public void addGoodExamples(Map<String, CRStage> examples) {
        examples.put("stage",stage);
        examples.put("stageWith2Jobs",stageWith2Jobs);
        examples.put("stageWithEnv",stageWithEnv);
        examples.put("stageWithApproval",stageWithApproval);
    }

    @Override
    public void addBadExamples(Map<String, CRStage> examples) {
        examples.put("invalidNoName",invalidNoName);
        examples.put("invalidNoJobs",invalidNoJobs);
        examples.put("invalidSameEnvironmentVariableTwice",invalidSameEnvironmentVariableTwice);
        examples.put("invalidSameJobNameTwice",invalidSameJobNameTwice);
    }

    @Test
    public void shouldCheckErrorsInJobs()
    {
        CRStage withNamelessJob = new CRStage("build",new CRJob());

        ErrorCollection errors = new ErrorCollection();
        withNamelessJob.getErrors(errors,"TEST");

        String fullError = errors.getErrorsAsText();

        assertThat(fullError,contains("TEST; Stage (build)"));
        assertThat(fullError,contains("Missing field 'name'"));
    }
}
