package com.thoughtworks.go.plugin.configrepo;

import java.util.Map;

public class CRPropertyGenerator_1Test extends CRBaseTest<CRPropertyGenerator_1> {

    private final CRPropertyGenerator_1 invalidNoXPath;
    private final CRPropertyGenerator_1 invalidNoSrc;
    private final CRPropertyGenerator_1 invalidNoName;
    private String xpath = "substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')";

    private final CRPropertyGenerator_1 propGen;

    public CRPropertyGenerator_1Test(){
        propGen = new CRPropertyGenerator_1("coverage.class","target/emma/coverage.xml",xpath);

        invalidNoXPath = new CRPropertyGenerator_1("coverage.class","target/emma/coverage.xml",null);
        invalidNoSrc = new CRPropertyGenerator_1("coverage.class",null,xpath);
        invalidNoName = new CRPropertyGenerator_1(null,"target/emma/coverage.xml",xpath);
    }

    @Override
    public void addGoodExamples(Map<String, CRPropertyGenerator_1> examples) {
        examples.put("propGen",propGen);
    }

    @Override
    public void addBadExamples(Map<String, CRPropertyGenerator_1> examples) {
        examples.put("invalidNoXPath",invalidNoXPath);
        examples.put("invalidNoSrc",invalidNoSrc);
        examples.put("invalidNoName",invalidNoName);
    }
}
