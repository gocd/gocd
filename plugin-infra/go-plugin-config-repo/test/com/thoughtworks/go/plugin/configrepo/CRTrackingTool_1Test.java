package com.thoughtworks.go.plugin.configrepo;

import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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

    @Test
    public void shouldDeserializeFromAPILikeObject()
    {
        String json = "{\n" +
                "    \"link\": \"https://github.com/gocd/api.go.cd/issues/${ID}\",\n" +
                "    \"regex\": \"##(d+)\"\n" +
                "  }";
        CRTrackingTool_1 deserializedValue = gson.fromJson(json,CRTrackingTool_1.class);

        assertThat(deserializedValue.getLink(),is("https://github.com/gocd/api.go.cd/issues/${ID}"));
        assertThat(deserializedValue.getRegex(),is("##(d+)"));

        ErrorCollection errors = deserializedValue.getErrors();
        assertTrue(errors.isEmpty());
    }
}
