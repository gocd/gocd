package com.thoughtworks.go.plugin.configrepo;

import java.util.Map;

public class CRMingle_1Test extends CRBaseTest<CRMingle_1> {

    private final CRMingle_1 mingle;
    private final CRMingle_1 invalidNoUrl;
    private final CRMingle_1 invalidNoId;

    public CRMingle_1Test()
    {
        mingle = new CRMingle_1("http://mingle.example.com","my_project");
        invalidNoUrl = new CRMingle_1(null,"my_project");
        invalidNoId = new CRMingle_1("http://mingle.example.com",null);

    }

    @Override
    public void addGoodExamples(Map<String, CRMingle_1> examples) {
        examples.put("mingle",mingle);
    }

    @Override
    public void addBadExamples(Map<String, CRMingle_1> examples) {
        examples.put("invalidNoUrl",invalidNoUrl);
        examples.put("invalidNoId",invalidNoId);
    }
}
