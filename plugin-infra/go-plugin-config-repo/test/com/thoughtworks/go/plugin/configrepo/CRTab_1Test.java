package com.thoughtworks.go.plugin.configrepo;

import java.util.Map;

public class CRTab_1Test extends CRBaseTest<CRTab_1> {

    private final CRTab_1 tab;
    private final CRTab_1 invalidTabNoName;
    private final CRTab_1 invalidTabNoPath;

    public CRTab_1Test()
    {
        tab = new CRTab_1("results","test.xml");

        invalidTabNoName = new CRTab_1(null,"test.xml");
        invalidTabNoPath = new CRTab_1("results",null);
    }

    @Override
    public void addGoodExamples(Map<String, CRTab_1> examples) {
        examples.put("tab",tab);
    }

    @Override
    public void addBadExamples(Map<String, CRTab_1> examples) {
        examples.put("invalidTabNoName",invalidTabNoName);
        examples.put("invalidTabNoPath",invalidTabNoPath);
    }
}
