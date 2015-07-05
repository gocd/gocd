package com.thoughtworks.go.plugin.configrepo.material;

import com.thoughtworks.go.plugin.configrepo.CRBaseTest;

import java.util.Map;

public class CRDependencyMaterial_1Test extends CRBaseTest<CRDependencyMaterial_1> {

    private final CRDependencyMaterial_1 namedDependsOnPipeline;
    private final CRDependencyMaterial_1 invalidNoPipeline;
    private final CRDependencyMaterial_1 invalidNoStage;
    private CRDependencyMaterial_1 dependsOnPipeline;

    public CRDependencyMaterial_1Test()
    {
        namedDependsOnPipeline = new CRDependencyMaterial_1("pipe2","pipeline2","build");
        dependsOnPipeline = new CRDependencyMaterial_1("pipeline2","build");

        invalidNoPipeline = new CRDependencyMaterial_1();
        invalidNoPipeline.setStageName("build");

        invalidNoStage = new CRDependencyMaterial_1();
        invalidNoStage.setPipelineName("pipeline1");
    }

    @Override
    public void addGoodExamples(Map<String, CRDependencyMaterial_1> examples) {
        examples.put("dependsOnPipeline",dependsOnPipeline);
        examples.put("namedDependsOnPipeline",namedDependsOnPipeline);
    }

    @Override
    public void addBadExamples(Map<String, CRDependencyMaterial_1> examples) {
        examples.put("invalidNoPipeline",invalidNoPipeline);
        examples.put("invalidNoStage",invalidNoStage);
    }

}
