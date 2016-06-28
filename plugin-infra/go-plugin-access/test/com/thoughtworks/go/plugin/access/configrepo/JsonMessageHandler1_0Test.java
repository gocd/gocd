package com.thoughtworks.go.plugin.access.configrepo;


import com.thoughtworks.go.plugin.access.configrepo.contract.CRParseResult;
import org.junit.Test;

import static com.thoughtworks.go.util.TestUtils.contains;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class JsonMessageHandler1_0Test {

    private final JsonMessageHandler1_0 handler;

    public JsonMessageHandler1_0Test()
    {
        handler = new JsonMessageHandler1_0();
    }

    @Test
    public void shouldErrorWhenMissingTargetVersionInResponse()
    {
        String json = "{\n" +
                "  \"environments\" : [],\n" +
                "  \"pipelines\" : [],\n" +
                "  \"errors\" : []\n" +
                "}";

        CRParseResult result = handler.responseMessageForParseDirectory(json);
        assertThat(result.getErrors().getErrorsAsText(),contains("missing 'target_version' field"));
    }

    @Test
    public void shouldNotErrorWhenTargetVersionInResponse()
    {
        String json = "{\n" +
                "  \"target_version\" : 1,\n" +
                "  \"pipelines\" : [],\n" +
                "  \"errors\" : []\n" +
                "}";

        CRParseResult result = handler.responseMessageForParseDirectory(json);
        assertFalse(result.hasErrors());
    }

    @Test
    public void shouldAppendPluginErrorsToAllErrors()
    {
        String json = "{\n" +
                "  \"target_version\" : 1,\n" +
                "  \"pipelines\" : [],\n" +
                "  \"errors\" : [{\"location\" : \"somewhere\", \"message\" : \"failed to parse pipeline.json\"}]\n" +
                "}";
        CRParseResult result = handler.responseMessageForParseDirectory(json);
        assertTrue(result.hasErrors());
    }
}
