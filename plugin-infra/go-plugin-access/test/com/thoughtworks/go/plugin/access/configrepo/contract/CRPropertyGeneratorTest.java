package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CRPropertyGeneratorTest extends CRBaseTest<CRPropertyGenerator> {

    private final CRPropertyGenerator invalidNoXPath;
    private final CRPropertyGenerator invalidNoSrc;
    private final CRPropertyGenerator invalidNoName;
    private String xpath = "substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')";

    private final CRPropertyGenerator propGen;

    public CRPropertyGeneratorTest(){
        propGen = new CRPropertyGenerator("coverage.class","target/emma/coverage.xml",xpath);

        invalidNoXPath = new CRPropertyGenerator("coverage.class","target/emma/coverage.xml",null);
        invalidNoSrc = new CRPropertyGenerator("coverage.class",null,xpath);
        invalidNoName = new CRPropertyGenerator(null,"target/emma/coverage.xml",xpath);
    }

    @Override
    public void addGoodExamples(Map<String, CRPropertyGenerator> examples) {
        examples.put("propGen",propGen);
    }

    @Override
    public void addBadExamples(Map<String, CRPropertyGenerator> examples) {
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
        CRPropertyGenerator deserializedValue = gson.fromJson(json,CRPropertyGenerator.class);

        assertThat(deserializedValue.getName(),is("coverage.class"));
        assertThat(deserializedValue.getSrc(),is("target/emma/coverage.xml"));
        assertThat(deserializedValue.getXpath(),is("substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')"));

        ErrorCollection errors = deserializedValue.getErrors();
        assertTrue(errors.isEmpty());
    }
}
