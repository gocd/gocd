package com.thoughtworks.go.plugin.configrepo;

import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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


    @Test
    public void shouldDeserializeFromAPILikeObject()
    {
        String json = "{\n" +
                "      \"name\": \"cobertura\",\n" +
                "      \"path\": \"target/site/cobertura/index.html\"\n" +
                "    }";
        CRTab_1 deserializedValue = gson.fromJson(json,CRTab_1.class);

        assertThat(deserializedValue.getName(),is("cobertura"));
        assertThat(deserializedValue.getPath(),is("target/site/cobertura/index.html"));

        ErrorCollection errors = deserializedValue.getErrors();
        assertTrue(errors.isEmpty());
    }
}
