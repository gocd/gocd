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

import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ElasticAgentExtensionConverterV1Test {

    @Test
    public void shouldJSONizeCanHandleRequestBody() throws Exception {
        String json = new ElasticAgentExtensionConverterV1().canHandlePluginRequestBody(Arrays.asList("foo", "bar"), "prod");
        JSONAssert.assertEquals(json, "{\"resources\":[\"foo\",\"bar\"],\"environment\":\"prod\"}", true);
    }

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
        String json = new ElasticAgentExtensionConverterV1().createAgentRequestBody("secret-key", Arrays.asList("foo", "bar"), "prod");
        JSONAssert.assertEquals(json, "{\"resources\":[\"foo\",\"bar\"],\"environment\":\"prod\",\"auto_register_key\":\"secret-key\"}", true);
    }

    @Test
    public void shouldJSONizeShouldAssignWorkRequestBody() throws Exception {
        String json = new ElasticAgentExtensionConverterV1().shouldAssignWorkRequestBody(elasticAgent(), Arrays.asList("foo", "bar"), "prod");
        JSONAssert.assertEquals(json, "{\"resources\":[\"foo\",\"bar\"],\"environment\":\"prod\",\"agent\":{\"agent_id\":\"42\",\"agent_state\":\"Idle\",\"build_state\":\"Idle\",\"config_state\":\"Enabled\"}}", true);
    }

    @Test
    public void shouldJSONizeNotifyAgentBusyRequestBody() throws Exception {
        String json = new ElasticAgentExtensionConverterV1().notifyAgentBusyRequestBody(elasticAgent());
        JSONAssert.assertEquals(json, "{\"agent_id\":\"42\",\"agent_state\":\"Idle\",\"build_state\":\"Idle\",\"config_state\":\"Enabled\"}", true);
    }

    @Test
    public void shouldJSONizeNotifyAgentIdleRequestBody() throws Exception {
        String json = new ElasticAgentExtensionConverterV1().notifyAgentIdleRequestBody(elasticAgent());
        JSONAssert.assertEquals(json, "{\"agent_id\":\"42\",\"agent_state\":\"Idle\",\"build_state\":\"Idle\",\"config_state\":\"Enabled\"}", true);
    }

    @Test
    public void shouldJSONizesServerPingRequestBody() throws Exception {
        String json = new ElasticAgentExtensionConverterV1().serverPingRequestBody(Arrays.asList(new AgentMetadata("42", "AgentState", "BuildState", "ConfigState")));
        JSONAssert.assertEquals(json, "[{\"agent_id\":\"42\",\"agent_state\":\"AgentState\",\"config_state\":\"ConfigState\",\"build_state\":\"BuildState\"}]", true);
    }

    private AgentMetadata elasticAgent() {
        return new AgentMetadata("42", "Idle", "Idle", "Enabled");
    }

}

