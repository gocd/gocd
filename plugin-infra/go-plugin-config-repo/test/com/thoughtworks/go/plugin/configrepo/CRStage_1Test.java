package com.thoughtworks.go.plugin.configrepo;

import com.thoughtworks.go.plugin.configrepo.tasks.CRBuildTask_1;

import java.util.Arrays;
import java.util.Map;

public class CRStage_1Test extends CRBaseTest<CRStage_1> {

    private final CRStage_1 stage;
    private final CRStage_1 stageWith2Jobs;
    private final CRStage_1 stageWithEnv;
    private final CRStage_1 stageWithApproval;
    private final CRStage_1 invalidNoName;
    private final CRStage_1 invalidNoJobs;
    private final CRStage_1 invalidSameEnvironmentVariableTwice;
    private final CRStage_1 invalidSameJobNameTwice;

    public CRStage_1Test()
    {
        CRBuildTask_1 rakeTask = CRBuildTask_1.rake();
        CRBuildTask_1 antTask = CRBuildTask_1.ant();
        CRJob_1 buildRake = new CRJob_1("build", rakeTask);
        CRJob_1 build2Rakes = new CRJob_1("build", rakeTask, CRBuildTask_1.rake("Rakefile.rb", "compile"));

        CRJob_1 jobWithVar = new CRJob_1("build", rakeTask);
        jobWithVar.addEnvironmentVariable("key1","value1");

        CRJob_1 jobWithResource = new CRJob_1("test", antTask);
        jobWithResource.addResource("linux");

        stage = new CRStage_1("build",buildRake);
        stageWith2Jobs = new CRStage_1("build",build2Rakes,jobWithResource);
        stageWithEnv = new CRStage_1("test",jobWithResource);
        stageWithEnv.addEnvironmentVariable("TEST_NUM","1");

        CRApproval_1 manualWithAuth = new CRApproval_1("manual");
        manualWithAuth.setAuthorizedRoles(Arrays.asList("manager"));
        stageWithApproval = new CRStage_1("deploy",buildRake);
        stageWithApproval.setApproval(manualWithAuth);

        invalidNoName = new CRStage_1(null,jobWithResource);
        invalidNoJobs = new CRStage_1("build");

        invalidSameEnvironmentVariableTwice = new CRStage_1("build",buildRake);
        invalidSameEnvironmentVariableTwice.addEnvironmentVariable("key","value1");
        invalidSameEnvironmentVariableTwice.addEnvironmentVariable("key","value2");

        invalidSameJobNameTwice = new CRStage_1("build",buildRake,build2Rakes);
    }

    @Override
    public void addGoodExamples(Map<String, CRStage_1> examples) {
        examples.put("stage",stage);
        examples.put("stageWith2Jobs",stageWith2Jobs);
        examples.put("stageWithEnv",stageWithEnv);
        examples.put("stageWithApproval",stageWithApproval);
    }

    @Override
    public void addBadExamples(Map<String, CRStage_1> examples) {
        examples.put("invalidNoName",invalidNoName);
        examples.put("invalidNoJobs",invalidNoJobs);
        examples.put("invalidSameEnvironmentVariableTwice",invalidSameEnvironmentVariableTwice);
        examples.put("invalidSameJobNameTwice",invalidSameJobNameTwice);
    }
}
