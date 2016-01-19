package com.thoughtworks.go.plugin.configrepo;

import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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

    @Test
    public void shouldDeserializeFromAPILikeObject()
    {
        String json = "{\n" +
                "      \"name\": \"coverage.class\",\n" +
                "      \"source\": \"target/emma/coverage.xml\",\n" +
                "      \"xpath\": \"substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')\"\n" +
                "    }";
        CRPropertyGenerator_1 deserializedValue = gson.fromJson(json,CRPropertyGenerator_1.class);

        assertThat(deserializedValue.getName(),is("coverage.class"));
        assertThat(deserializedValue.getSrc(),is("target/emma/coverage.xml"));
        assertThat(deserializedValue.getXpath(),is("substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')"));

        ErrorCollection errors = deserializedValue.getErrors();
        assertTrue(errors.isEmpty());
    }
}
