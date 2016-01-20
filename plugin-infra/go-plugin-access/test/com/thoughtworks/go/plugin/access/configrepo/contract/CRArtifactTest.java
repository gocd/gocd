package com.thoughtworks.go.plugin.access.configrepo.contract;

import com.thoughtworks.go.plugin.access.configrepo.ErrorCollection;
import org.junit.Test;

import java.util.Map;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class CRArtifactTest extends CRBaseTest<CRArtifact> {

    private final CRArtifact artifact;
    private final CRArtifact invalidNoSource;

    public CRArtifactTest()
    {
        artifact = new CRArtifact("src","dest",CRArtifactType.build);
        invalidNoSource = new CRArtifact(null,"dest",CRArtifactType.test);
    }

    @Override
    public void addGoodExamples(Map<String, CRArtifact> examples) {
        examples.put("artifact",artifact);
    }

    @Override
    public void addBadExamples(Map<String, CRArtifact> examples) {
        examples.put("invalidNoSource",invalidNoSource);
    }


    @Test
    public void shouldDeserializeFromAPILikeObject()
    {
        String json = "{\n" +
                "      \"source\": \"test\",\n" +
                "      \"destination\": \"res1\",\n" +
                "      \"type\": \"test\"\n" +
                "    }";
        CRArtifact deserializedValue = gson.fromJson(json,CRArtifact.class);

        assertThat(deserializedValue.getSource(),is("test"));
        assertThat(deserializedValue.getDestination(),is("res1"));
        assertThat(deserializedValue.getType(),is(CRArtifactType.test));

        ErrorCollection errors = deserializedValue.getErrors();
        assertTrue(errors.isEmpty());
    }

    @Test
    public void shouldHandleBadArtifactTypeWhenDeserializing()
    {
        String json = "{\n" +
                "      \"source\": \"test\",\n" +
                "      \"destination\": \"res1\",\n" +
                "      \"type\": \"bla\"\n" +
                "    }";
        CRArtifact deserializedValue = gson.fromJson(json,CRArtifact.class);

        assertThat(deserializedValue.getSource(),is("test"));
        assertThat(deserializedValue.getDestination(),is("res1"));
        assertNull(deserializedValue.getType());

        ErrorCollection errors = deserializedValue.getErrors();
        assertFalse(errors.isEmpty());
    }
}
