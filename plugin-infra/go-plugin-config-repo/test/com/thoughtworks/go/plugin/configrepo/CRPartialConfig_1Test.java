package com.thoughtworks.go.plugin.configrepo;

import java.util.Map;

public class CRPartialConfig_1Test extends CRBaseTest<CRPartialConfig_1> {

    private final CRPartialConfig_1 empty;
    private final CRPartialConfig_1 withEnvironment;
    private final CRPartialConfig_1 invalidWith2SameEnvironments;

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
    }

    @Override
    public void addGoodExamples(Map<String, CRPartialConfig_1> examples) {
        examples.put("empty",empty);
        examples.put("withEnvironment",withEnvironment);
    }

    @Override
    public void addBadExamples(Map<String, CRPartialConfig_1> examples) {
        examples.put("invalidWith2SameEnvironments",invalidWith2SameEnvironments);
    }
}
