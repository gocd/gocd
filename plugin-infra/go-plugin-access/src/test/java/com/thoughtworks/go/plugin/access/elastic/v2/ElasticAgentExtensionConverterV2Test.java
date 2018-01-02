/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.elastic.v2;

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ElasticAgentExtensionConverterV2Test {
    private JobIdentifier jobIdentifier;

    @Before
    public void setUp() throws Exception {
        jobIdentifier = new JobIdentifier("test-pipeline", 1, "Test Pipeline", "test-stage", "1", "test-job");
        jobIdentifier.setBuildId(100L);
    }

    @Test
    public void shouldUnJSONizeCanHandleResponseBody() throws Exception {
        assertTrue(new ElasticAgentExtensionConverterV2().canHandlePluginResponseFromBody("true"));
        assertFalse(new ElasticAgentExtensionConverterV2().canHandlePluginResponseFromBody("false"));
    }

    @Test
    public void shouldUnJSONizeShouldAssignWorkResponseFromBody() throws Exception {
        assertTrue(new ElasticAgentExtensionConverterV2().shouldAssignWorkResponseFromBody("true"));
        assertFalse(new ElasticAgentExtensionConverterV2().shouldAssignWorkResponseFromBody("false"));
    }

    @Test
    public void shouldJSONizeCreateAgentRequestBody() throws Exception {
        Map<String, String> configuration = new HashMap<>();
        configuration.put("key1", "value1");
        configuration.put("key2", "value2");
        String json = new ElasticAgentExtensionConverterV2().createAgentRequestBody("secret-key", "prod", configuration, jobIdentifier);
        JSONAssert.assertEquals(json, "{" +
                "  \"auto_register_key\":\"secret-key\"," +
                "  \"properties\":{" +
                "    \"key1\":\"value1\"," +
                "    \"key2\":\"value2\"" +
                "    }," +
                "  \"environment\":\"prod\"," +
                "  \"job_identifier\": {\n" +
                "    \"pipeline_name\": \"test-pipeline\",\n" +
                "    \"pipeline_counter\": 1,\n" +
                "    \"pipeline_label\": \"Test Pipeline\",\n" +
                "    \"stage_name\": \"test-stage\",\n" +
                "    \"stage_counter\": \"1\",\n" +
                "    \"job_name\": \"test-job\",\n" +
                "    \"job_id\": 100\n" +
                "  }\n" +
                "}", JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void shouldJSONizeShouldAssignWorkRequestBody() throws Exception {
        HashMap<String, String> configuration = new HashMap<>();
        configuration.put("property_name", "property_value");
        String actual = new ElasticAgentExtensionConverterV2().shouldAssignWorkRequestBody(elasticAgent(), "prod", configuration, jobIdentifier);
        String expected = "{" +
                "  \"environment\":\"prod\"," +
                "  \"agent\":{" +
                "    \"agent_id\":\"42\"," +
                "    \"agent_state\":\"Idle\"," +
                "    \"build_state\":\"Idle\"," +
                "    \"config_state\":\"Enabled\"" +
                "  }," +
                "  \"properties\":{" +
                "    \"property_name\":\"property_value\"" +
                "  }," +
                "  \"job_identifier\": {\n" +
                "    \"pipeline_name\": \"test-pipeline\",\n" +
                "    \"pipeline_counter\": 1,\n" +
                "    \"pipeline_label\": \"Test Pipeline\",\n" +
                "    \"stage_name\": \"test-stage\",\n" +
                "    \"stage_counter\": \"1\",\n" +
                "    \"job_name\": \"test-job\",\n" +
                "    \"job_id\": 100\n" +
                "  }\n" +
                "}";

        JSONAssert.assertEquals(expected, actual, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void shouldJSONizesListAgentsResponseBody() throws Exception {
        String json = new ElasticAgentExtensionConverterV2().listAgentsResponseBody(Arrays.asList(new AgentMetadata("42", "AgentState", "BuildState", "ConfigState")));
        JSONAssert.assertEquals(json, "[{\"agent_id\":\"42\",\"agent_state\":\"AgentState\",\"config_state\":\"ConfigState\",\"build_state\":\"BuildState\"}]", JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void shouldConstructValidationRequest() throws JSONException {
        HashMap<String, String> configuration = new HashMap<>();
        configuration.put("key1", "value1");
        configuration.put("key2", "value2");
        configuration.put("key3", null);
        String requestBody = new ElasticAgentExtensionConverterV2().validateRequestBody(configuration);
        JSONAssert.assertEquals(requestBody, "{\"key3\":null,\"key2\":\"value2\",\"key1\":\"value1\"}", JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    public void shouldHandleValidationResponse() {
        String responseBody = "[{\"key\":\"key-one\",\"message\":\"error on key one\"}, {\"key\":\"key-two\",\"message\":\"error on key two\"}]";
        ValidationResult result = new ElasticAgentExtensionConverterV2().getValidationResultResponseFromBody(responseBody);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.getErrors().size(), is(2));
        assertThat(result.getErrors().get(0).getKey(), is("key-one"));
        assertThat(result.getErrors().get(0).getMessage(), is("error on key one"));
        assertThat(result.getErrors().get(1).getKey(), is("key-two"));
        assertThat(result.getErrors().get(1).getMessage(), is("error on key two"));
    }

    @Test
    public void shouldUnJSONizeGetProfileViewResponseFromBody() throws Exception {
        String template = new ElasticAgentExtensionConverterV2().getProfileViewResponseFromBody("{\"template\":\"foo\"}");
        assertThat(template, is("foo"));
    }

    @Test
    public void shouldUnJSONizeGetImageResponseFromBody() throws Exception {
        com.thoughtworks.go.plugin.domain.common.Image image = new ElasticAgentExtensionConverterV2().getImageResponseFromBody("{\"content_type\":\"foo\", \"data\":\"bar\"}");
        assertThat(image.getContentType(), is("foo"));
        assertThat(image.getData(), is("bar"));
    }

    @Test
    public void shouldGetStatusReportViewFromResponseBody() throws Exception {
        String template = new ElasticAgentExtensionConverterV2().getStatusReportView("{\"view\":\"foo\"}");
        assertThat(template, is("foo"));
    }

    @Test
    public void shouldGetCapabilitiesFromResponseBody() throws Exception {
        String responseBody = "{\"supports_status_report\":\"true\"}";

        Capabilities capabilities = new ElasticAgentExtensionConverterV2().getCapabilitiesFromResponseBody(responseBody);

        assertTrue(capabilities.supportsStatusReport());
    }

    private AgentMetadata elasticAgent() {
        return new AgentMetadata("42", "Idle", "Idle", "Enabled");
    }

}

