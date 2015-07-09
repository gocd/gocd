package com.thoughtworks.go.plugin.configrepo;

import com.thoughtworks.go.plugin.configrepo.material.CRGitMaterial_1;
import com.thoughtworks.go.plugin.configrepo.tasks.CRBuildTask_1;

import java.util.Map;

public class CRPartialConfig_1Test extends CRBaseTest<CRPartialConfig_1> {

    private final CRPartialConfig_1 empty;
    private final CRPartialConfig_1 withEnvironment;
    private final CRPartialConfig_1 invalidWith2SameEnvironments;
    private final CRPartialConfig_1 withGroup;

    public CRPartialConfig_1Test()
    {
        empty = new CRPartialConfig_1();

        withEnvironment = new CRPartialConfig_1();
        CREnvironment_1 devEnvironment = new CREnvironment_1("dev");
        devEnvironment.addEnvironmentVariable("key1","value1");
        devEnvironment.addAgent("123-745");
        devEnvironment.addPipeline("pipeline1");
        withEnvironment.addEnvironment(devEnvironment);

        invalidWith2SameEnvironments = new CRPartialConfig_1();
        invalidWith2SameEnvironments.addEnvironment(devEnvironment);
        invalidWith2SameEnvironments.addEnvironment(new CREnvironment_1("dev"));

        CRBuildTask_1 rakeTask = CRBuildTask_1.rake();
        CRJob_1 buildRake = new CRJob_1("build", rakeTask);
        CRGitMaterial_1 veryCustomGit = new CRGitMaterial_1("gitMaterial1", "dir1", false, "gitrepo", "feature12", "externals", "tools");
        CRStage_1 buildStage = new CRStage_1("build", buildRake);
        CRPipeline_1 pipe1 = new CRPipeline_1("pipe1", veryCustomGit, buildStage);
        CRPipelineGroup_1 group1 = new CRPipelineGroup_1("group1",pipe1);

        withGroup = new CRPartialConfig_1();
        withGroup.addGroup(group1);
    }

    @Override
    public void addGoodExamples(Map<String, CRPartialConfig_1> examples) {
        examples.put("empty",empty);
        examples.put("withEnvironment",withEnvironment);
        examples.put("withGroup",withGroup);
    }

    @Override
    public void addBadExamples(Map<String, CRPartialConfig_1> examples) {
        examples.put("invalidWith2SameEnvironments",invalidWith2SameEnvironments);
    }
}
