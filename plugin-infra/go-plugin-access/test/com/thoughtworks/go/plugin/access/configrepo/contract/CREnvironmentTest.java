package com.thoughtworks.go.plugin.access.configrepo.contract;

import java.util.Map;

public class CREnvironmentTest extends CRBaseTest<CREnvironment> {

    private CREnvironment empty;

    private CREnvironment devWithVariable;
    private CREnvironment prodWithAgent;
    private CREnvironment uatWithPipeline;

    private CREnvironment invalidSameEnvironmentVariableTwice;
    private final CREnvironment invalidSameAgentTwice;
    private final CREnvironment invalidSamePipelineTwice;

    public CREnvironmentTest()
    {
        empty = new CREnvironment("dev");

        devWithVariable = new CREnvironment("dev");
        devWithVariable.addEnvironmentVariable("key1","value1");

        prodWithAgent = new CREnvironment("prod");
        prodWithAgent.addAgent("123-745");

        uatWithPipeline = new CREnvironment("UAT");
        uatWithPipeline.addPipeline("pipeline1");

        invalidSameEnvironmentVariableTwice = new CREnvironment("badenv");
        invalidSameEnvironmentVariableTwice.addEnvironmentVariable("key","value1");
        invalidSameEnvironmentVariableTwice.addEnvironmentVariable("key","value2");

        invalidSameAgentTwice = new CREnvironment("badenv2");
        invalidSameAgentTwice.addAgent("123");
        invalidSameAgentTwice.addAgent("123");

        invalidSamePipelineTwice = new CREnvironment("badenv3");
        invalidSamePipelineTwice.addPipeline("pipe1");
        invalidSamePipelineTwice.addPipeline("pipe1");
    }


    @Override
    public void addGoodExamples(Map<String, CREnvironment> examples) {
        examples.put("empty",empty);
        examples.put("devWithVariable",devWithVariable);
        examples.put("prodWithAgent",prodWithAgent);
        examples.put("uatWithPipeline",uatWithPipeline);
    }

    @Override
    public void addBadExamples(Map<String, CREnvironment> examples) {
        examples.put("invalidSameEnvironmentVariableTwice",invalidSameEnvironmentVariableTwice);
        examples.put("invalidSameAgentTwice",invalidSameAgentTwice);
        examples.put("invalidSamePipelineTwice",invalidSamePipelineTwice);
    }
}
