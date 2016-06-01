package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CRTabTest extends CRBaseTest<CRTab> {

    private final CRTab tab;
    private final CRTab invalidTabNoName;
    private final CRTab invalidTabNoPath;

    public CRTabTest()
    {
        tab = new CRTab("results","test.xml");

        invalidTabNoName = new CRTab(null,"test.xml");
        invalidTabNoPath = new CRTab("results",null);
    }

    @Override
    public void addGoodExamples(Map<String, CRTab> examples) {
        examples.put("tab",tab);
    }

    @Override
    public void addBadExamples(Map<String, CRTab> examples) {
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
        CRTab deserializedValue = gson.fromJson(json,CRTab.class);

        assertThat(deserializedValue.getName(),is("cobertura"));
        assertThat(deserializedValue.getPath(),is("target/site/cobertura/index.html"));

        ErrorCollection errors = deserializedValue.getErrors();
        assertTrue(errors.isEmpty());
    }
}
