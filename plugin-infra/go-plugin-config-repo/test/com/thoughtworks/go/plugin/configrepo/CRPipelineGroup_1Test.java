package com.thoughtworks.go.plugin.configrepo;

import com.thoughtworks.go.plugin.configrepo.material.CRGitMaterial_1;
import com.thoughtworks.go.plugin.configrepo.tasks.CRBuildTask_1;

import java.util.Map;

public class CRPipelineGroup_1Test extends CRBaseTest<CRPipelineGroup_1> {

    private final CRPipelineGroup_1 group1;
    private final CRPipelineGroup_1 empty;

    public CRPipelineGroup_1Test()
    {
        CRBuildTask_1 rakeTask = CRBuildTask_1.rake();
        CRJob_1 buildRake = new CRJob_1("build", rakeTask);
        CRGitMaterial_1 veryCustomGit = new CRGitMaterial_1("gitMaterial1", "dir1", false, "gitrepo", "feature12", "externals", "tools");

        CRStage_1 buildStage = new CRStage_1("build", buildRake);
        CRPipeline_1 pipe1 = new CRPipeline_1("pipe1", veryCustomGit, buildStage);
        group1 = new CRPipelineGroup_1("group1",pipe1);

        empty = new CRPipelineGroup_1("empty");
    }

    @Override
    public void addGoodExamples(Map<String, CRPipelineGroup_1> examples) {
        examples.put("group1",group1);
        examples.put("empty",empty);
    }

    @Override
    public void addBadExamples(Map<String, CRPipelineGroup_1> examples) {

    }
}
