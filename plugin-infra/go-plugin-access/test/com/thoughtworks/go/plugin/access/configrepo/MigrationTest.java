package com.thoughtworks.go.plugin.access.configrepo;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.access.configrepo.codec.GsonCodec;
import com.thoughtworks.go.plugin.access.configrepo.contract.CREnvironment;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRError;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPipeline;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Just a PoC of how we can handle migration of plugin messages.
 * http://jolt-demo.appspot.com/ is good for prototyping and learning
 */
public class MigrationTest {
    /**
     * Example of V2 message
     */
    class PluginMessageV2 {
        private String target_version;
        private ConfigurationV2 configuration;
        private List<CRError> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();
    }
    class ConfigurationV2 {
        private Collection<CREnvironment> environments = new ArrayList<>();
        private Collection<CRPipeline> pipelines = new ArrayList<>();
    }
    @Test
    public void shouldTransformToNewMessageLayout()
    {
        List chainrSpecJSON = JsonUtils.jsonToList("[\n" +
                " {\n" +
                "  \"operation\": \"shift\",\n" +
                "  \"spec\": {\n" +
                "   \"pipelines\": {\n" +
                "    \"@\": \"configuration.pipelines\"\n" +
                "   },\n" +
                "   \"environments\": {\n" +
                "    \"@\": \"configuration.environments\"\n" +
                "   }\n" +
                "  }\n" +
                " },\n" +
                " {\n" +
                "  \"operation\": \"default\",\n" +
                "  \"spec\": {\n" +
                "   \"warnings\": [\n" +
                "    \"migrated from V1\"\n" +
                "   ]\n" +
                "  }\n" +
                " }\n" +
                "]" );
        Chainr chainr = Chainr.fromSpec( chainrSpecJSON );

        Object inputJSON = JsonUtils.jsonToMap(
                "  {\n" +
                "    \"pipelines\": [\n" +
                "      {\n" +
                "        \"name\": \"firstpipe\",\n" +
                "        \"environment_variables\": [],\n" +
                "        \"group\": \"configrepo-example\",\n" +
                "        \"materials\": [\n" +
                "          {\n" +
                "            \"url\": \"https://github.com/tomzo/gocd-json-config-example.git\",\n" +
                "            \"type\": \"git\"\n" +
                "          }\n" +
                "        ],\n" +
                "        \"stages\": [\n" +
                "          {\n" +
                "            \"name\": \"build\",\n" +
                "            \"fetch_materials\": true,\n" +
                "            \"never_cleanup_artifacts\": false,\n" +
                "            \"clean_working_directory\": false,\n" +
                "            \"environment_variables\": [],\n" +
                "            \"jobs\": [\n" +
                "              {\n" +
                "                \"name\": \"build\",\n" +
                "                \"environment_variables\": [],\n" +
                "                \"tabs\": [],\n" +
                "                \"resources\": [],\n" +
                "                \"artifacts\": [],\n" +
                "                \"properties\": [],\n" +
                "                \"run_instance_count\": null,\n" +
                "                \"timeout\": 0,\n" +
                "                \"tasks\": [\n" +
                "                  {\n" +
                "                    \"type\": \"rake\"\n" +
                "                  }\n" +
                "                ]\n" +
                "              }\n" +
                "            ]\n" +
                "          }\n" +
                "        ]\n" +
                "      }\n" +
                "    ]\n" +
                "  }"
                );

        Object transformedOutput = chainr.transform( inputJSON );
        String migratedMessage = JsonUtils.toJsonString( transformedOutput );

        PluginMessageV2 v2Message = new GsonCodec().getGson().fromJson(migratedMessage, PluginMessageV2.class);
        assertThat(v2Message.warnings.size(),is(1));
        assertThat(v2Message.configuration.pipelines.size(),is(1));
    }
}
