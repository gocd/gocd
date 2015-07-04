package com.thoughtworks.go.plugin.configrepo;

import java.util.Map;

public class CREnvironment_1Test extends CRBaseTest<CREnvironment_1> {

    private CREnvironment_1 empty;

    private CREnvironment_1 devWithVariable;
    private CREnvironment_1 prodWithAgent;
    private CREnvironment_1 uatWithPipeline;

    private CREnvironment_1 invalidSameEnvironmentVariableTwice;
    private final CREnvironment_1 invalidSameAgentTwice;
    private final CREnvironment_1 invalidSamePipelineTwice;

    public CREnvironment_1Test()
    {
        empty = new CREnvironment_1("dev");

        devWithVariable = new CREnvironment_1("dev");
        devWithVariable.addEnvironmentVariable("key1","value1");

        prodWithAgent = new CREnvironment_1("prod");
        prodWithAgent.addAgent("123-745");

        uatWithPipeline = new CREnvironment_1("UAT");
        uatWithPipeline.addPipeline("pipeline1");

        invalidSameEnvironmentVariableTwice = new CREnvironment_1("badenv");
        invalidSameEnvironmentVariableTwice.addEnvironmentVariable("key","value1");
        invalidSameEnvironmentVariableTwice.addEnvironmentVariable("key","value2");

        invalidSameAgentTwice = new CREnvironment_1("badenv2");
        invalidSameAgentTwice.addAgent("123");
        invalidSameAgentTwice.addAgent("123");

        invalidSamePipelineTwice = new CREnvironment_1("badenv3");
        invalidSamePipelineTwice.addPipeline("pipe1");
        invalidSamePipelineTwice.addPipeline("pipe1");
    }


    @Override
    public void addGoodExamples(Map<String, CREnvironment_1> examples) {
        examples.put("empty",empty);
        examples.put("devWithVariable",devWithVariable);
        examples.put("prodWithAgent",prodWithAgent);
        examples.put("uatWithPipeline",uatWithPipeline);
    }

    @Override
    public void addBadExamples(Map<String, CREnvironment_1> examples) {
        examples.put("invalidSameEnvironmentVariableTwice",invalidSameEnvironmentVariableTwice);
        examples.put("invalidSameAgentTwice",invalidSameAgentTwice);
        examples.put("invalidSamePipelineTwice",invalidSamePipelineTwice);
    }
}
