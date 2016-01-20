package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CRTrackingToolTest extends CRBaseTest<CRTrackingTool> {

    private final CRTrackingTool tracking;
    private final CRTrackingTool invalidNoLink;
    private final CRTrackingTool invalidNoRegex;

    public CRTrackingToolTest()
    {
        tracking  = new CRTrackingTool("http://your-trackingtool/yourproject/${ID}","evo-(\\d+)");
        invalidNoLink = new CRTrackingTool(null, "evo-(\\d+)");
        invalidNoRegex  = new CRTrackingTool("http://your-trackingtool/yourproject/${ID}",null);
    }

    @Override
    public void addGoodExamples(Map<String, CRTrackingTool> examples) {
        examples.put("tracking",tracking);
    }

    @Override
    public void addBadExamples(Map<String, CRTrackingTool> examples) {
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
        CRTrackingTool deserializedValue = gson.fromJson(json,CRTrackingTool.class);

        assertThat(deserializedValue.getLink(),is("https://github.com/gocd/api.go.cd/issues/${ID}"));
        assertThat(deserializedValue.getRegex(),is("##(d+)"));

        ErrorCollection errors = deserializedValue.getErrors();
        assertTrue(errors.isEmpty());
    }
}
