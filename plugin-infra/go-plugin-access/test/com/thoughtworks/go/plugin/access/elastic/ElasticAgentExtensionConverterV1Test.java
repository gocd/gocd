/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.plugin.access.elastic;

import com.thoughtworks.go.plugin.api.config.Configuration;
import com.thoughtworks.go.plugin.api.config.Property;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class ElasticAgentExtensionConverterV1Test {

    @Test
    public void shouldUnJSONizeCanHandleResponseBody() throws Exception {
        assertTrue(new ElasticAgentExtensionConverterV1().canHandlePluginResponseFromBody("true"));
        assertFalse(new ElasticAgentExtensionConverterV1().canHandlePluginResponseFromBody("false"));
    }

    @Test
    public void shouldUnJSONizeShouldAssignWorkResponseFromBody() throws Exception {
        assertTrue(new ElasticAgentExtensionConverterV1().shouldAssignWorkResponseFromBody("true"));
        assertFalse(new ElasticAgentExtensionConverterV1().shouldAssignWorkResponseFromBody("false"));
    }

    @Test
    public void shouldJSONizeCreateAgentRequestBody() throws Exception {
        Map<String, String> configuration = new HashMap<>();
        configuration.put("key1", "value1");
        configuration.put("key2", "value2");
        String json = new ElasticAgentExtensionConverterV1().createAgentRequestBody("secret-key", "prod", configuration);
        JSONAssert.assertEquals(json, "{\"auto_register_key\":\"secret-key\",\"properties\":{\"key1\":\"value1\",\"key2\":\"value2\"},\"environment\":\"prod\"}", true);
    }

    @Test
    public void shouldJSONizeShouldAssignWorkRequestBody() throws Exception {
        HashMap<String, String> configuration = new HashMap<>();
        configuration.put("property_name", "property_value");
        String json = new ElasticAgentExtensionConverterV1().shouldAssignWorkRequestBody(elasticAgent(), "prod", configuration);
        JSONAssert.assertEquals(json, "{\"environment\":\"prod\",\"agent\":{\"agent_id\":\"42\",\"agent_state\":\"Idle\",\"build_state\":\"Idle\",\"config_state\":\"Enabled\"},\"properties\":{\"property_name\":\"property_value\"}}", true);
    }

    @Test
    public void shouldJSONizesListAgentsResponseBody() throws Exception {
        String json = new ElasticAgentExtensionConverterV1().listAgentsResponseBody(Arrays.asList(new AgentMetadata("42", "AgentState", "BuildState", "ConfigState")));
        JSONAssert.assertEquals(json, "[{\"agent_id\":\"42\",\"agent_state\":\"AgentState\",\"config_state\":\"ConfigState\",\"build_state\":\"BuildState\"}]", true);
    }

    @Test
    public void shouldUnJSONizeProfileMetadata() throws Exception {
        Configuration metadata = new ElasticAgentExtensionConverterV1().getProfileMetadataResponseFromBody("[{\n" +
                "  \"key\": \"foo\",\n" +
                "  \"metadata\": {\n" +
                "    \"secure\": true,\n" +
                "    \"required\": false\n" +
                "  }\n" +
                "}, {\n" +
                "  \"key\": \"bar\"\n" +
                "}]");
        assertThat(metadata.size(), is(2));
        Property foo = metadata.get("foo");
        assertThat(foo.getOptions().findOption(Property.REQUIRED).getValue(), is(false));
        assertThat(foo.getOptions().findOption(Property.SECURE).getValue(), is(true));

        Property bar = metadata.get("bar");
        assertThat(bar.getOptions().size(), is(2));
        assertThat(bar.getOptions().findOption(Property.REQUIRED).getValue(), is(false));
        assertThat(bar.getOptions().findOption(Property.SECURE).getValue(), is(false));
    }

    private AgentMetadata elasticAgent() {
        return new AgentMetadata("42", "Idle", "Idle", "Enabled");
    }

}

