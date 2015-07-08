package com.thoughtworks.go.plugin.configrepo;

import java.util.Map;

public class CRTrackingTool_1Test extends CRBaseTest<CRTrackingTool_1> {

    private final CRTrackingTool_1 tracking;
    private final CRTrackingTool_1 invalidNoLink;
    private final CRTrackingTool_1 invalidNoRegex;

    public CRTrackingTool_1Test()
    {
        tracking  = new CRTrackingTool_1("http://your-trackingtool/yourproject/${ID}","evo-(\\d+)");
        invalidNoLink = new CRTrackingTool_1(null, "evo-(\\d+)");
        invalidNoRegex  = new CRTrackingTool_1("http://your-trackingtool/yourproject/${ID}",null);
    }

    @Override
    public void addGoodExamples(Map<String, CRTrackingTool_1> examples) {
        examples.put("tracking",tracking);
    }

    @Override
    public void addBadExamples(Map<String, CRTrackingTool_1> examples) {
        examples.put("invalidNoLink",invalidNoLink);
        examples.put("invalidNoRegex",invalidNoRegex);
    }
}
