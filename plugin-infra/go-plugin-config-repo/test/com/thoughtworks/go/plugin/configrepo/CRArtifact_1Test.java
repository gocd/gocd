package com.thoughtworks.go.plugin.configrepo;

import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CRArtifact_1Test extends CRBaseTest<CRArtifact_1> {

    private final CRArtifact_1 artifact;
    private final CRArtifact_1 invalidNoSource;
    private final CRArtifact_1 invalidBadType;

    public CRArtifact_1Test()
    {
        artifact = new CRArtifact_1("src","dest","build");
        invalidNoSource = new CRArtifact_1(null,"dest","test");
        invalidBadType =  new CRArtifact_1("src","dest","bla");
    }

    @Override
    public void addGoodExamples(Map<String, CRArtifact_1> examples) {
        examples.put("artifact",artifact);
    }

    @Override
    public void addBadExamples(Map<String, CRArtifact_1> examples) {
        examples.put("invalidNoSource",invalidNoSource);
        examples.put("invalidBadType",invalidBadType);
    }


    @Test
    public void shouldDeserializeFromAPILikeObject()
    {
        String json = "{\n" +
                "      \"source\": \"test\",\n" +
                "      \"destination\": \"res1\",\n" +
                "      \"type\": \"test\"\n" +
                "    }";
        CRArtifact_1 deserializedValue = gson.fromJson(json,CRArtifact_1.class);

        assertThat(deserializedValue.getSource(),is("test"));
        assertThat(deserializedValue.getDestination(),is("res1"));
        assertThat(deserializedValue.getType(),is("test"));

        ErrorCollection errors = deserializedValue.getErrors();
        assertTrue(errors.isEmpty());
    }
}
