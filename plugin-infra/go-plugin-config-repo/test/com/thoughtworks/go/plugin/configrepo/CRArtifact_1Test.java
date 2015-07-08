package com.thoughtworks.go.plugin.configrepo;

import java.util.Map;

public class CRArtifact_1Test extends CRBaseTest<CRArtifact_1> {

    private final CRArtifact_1 artifact;
    private final CRArtifact_1 invalidNoSource;

    public CRArtifact_1Test()
    {
        artifact = new CRArtifact_1("src","dest");
        invalidNoSource = new CRArtifact_1(null,"dest");

    }

    @Override
    public void addGoodExamples(Map<String, CRArtifact_1> examples) {
        examples.put("artifact",artifact);
    }

    @Override
    public void addBadExamples(Map<String, CRArtifact_1> examples) {
        examples.put("invalidNoSource",invalidNoSource);
    }
}
