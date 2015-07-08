package com.thoughtworks.go.plugin.configrepo;

import java.util.Map;

public class CRTimer_1Test extends CRBaseTest<CRTimer_1> {

    private final CRTimer_1 timer;
    private final CRTimer_1 invalidNoTimerSpec;

    public CRTimer_1Test()
    {
        timer = new CRTimer_1("0 15 10 * * ? *");

        invalidNoTimerSpec = new CRTimer_1();
    }

    @Override
    public void addGoodExamples(Map<String, CRTimer_1> examples) {
        examples.put("timer",timer);
    }

    @Override
    public void addBadExamples(Map<String, CRTimer_1> examples) {
        examples.put("invalidNoTimerSpec",invalidNoTimerSpec);
    }
}
