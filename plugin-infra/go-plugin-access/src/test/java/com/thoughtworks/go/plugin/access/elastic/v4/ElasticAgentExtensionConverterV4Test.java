/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.plugin.access.elastic.v4;

import com.google.gson.Gson;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class ElasticAgentExtensionConverterV4Test {
    private JobIdentifier jobIdentifier;

    @Before
    public void setUp() throws Exception {
        jobIdentifier = new JobIdentifier("test-pipeline", 1, "Test Pipeline", "test-stage", "1", "test-job");
        jobIdentifier.setBuildId(100L);
    }

    @Test
    public void shouldUnJSONizeCanHandleResponseBody() {
        assertTrue(new Gson().fromJson("true", Boolean.class));
        assertFalse(new Gson().fromJson("false", Boolean.class));
    }

    @Test
    public void shouldUnJSONizeShouldAssignWorkResponseFromBody() {
        assertTrue(new ElasticAgentExtensionConverterV4().shouldAssignWorkResponseFromBody("true"));
        assertFalse(new ElasticAgentExtensionConverterV4().shouldAssignWorkResponseFromBody("false"));
    }

    @Test
    public void shouldJSONizeCreateAgentRequestBody() throws Exception {
        Map<String, String> configuration = new HashMap<>();
        configuration.put("key1", "value1");
        configuration.put("key2", "value2");
        String json = new ElasticAgentExtensionConverterV4().createAgentRequestBody("secret-key", "prod", configuration, jobIdentifier);
        assertThatJson(json).isEqualTo("{" +
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
                "}");
    }

    @Test
    public void shouldJSONizeShouldAssignWorkRequestBody() throws Exception {
        HashMap<String, String> configuration = new HashMap<>();
        configuration.put("property_name", "property_value");
        String actual = new ElasticAgentExtensionConverterV4().shouldAssignWorkRequestBody(elasticAgent(), "prod", configuration, jobIdentifier);
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

        assertThatJson(expected).isEqualTo(actual);
    }

    @Test
    public void shouldJSONizeJobCompletionRequestBody() throws Exception {
        String actual = new ElasticAgentExtensionConverterV4().getJobCompletionRequestBody("ea1", jobIdentifier);

        String expected = "{" +
                "  \"elastic_agent_id\":\"ea1\"," +
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

        assertThatJson(expected).isEqualTo(actual);
    }

    @Test
    public void shouldJSONizeElasticAgentStatusReportRequestBodyWhenElasticAgentIdIsProvided() throws Exception {
        String elasticAgentId = "my-fancy-elastic-agent-id";
        String actual = new ElasticAgentExtensionConverterV4().getAgentStatusReportRequestBody(null, elasticAgentId);
        String expected = format("{" +
                "  \"elastic_agent_id\": \"%s\"" +
                "}", elasticAgentId);

        assertThatJson(expected).isEqualTo(actual);
    }

    @Test
    public void shouldJSONizeElasticAgentStatusReportRequestBodyWhenJobIdentifierIsProvided() throws Exception {
        String actual = new ElasticAgentExtensionConverterV4().getAgentStatusReportRequestBody(jobIdentifier, null);
        String expected = "{" +
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

        assertThatJson(expected).isEqualTo(actual);
    }

    @Test
    public void shouldConstructValidationRequest() {
        HashMap<String, String> configuration = new HashMap<>();
        configuration.put("key1", "value1");
        configuration.put("key2", "value2");
        configuration.put("key3", null);
        String requestBody = new ElasticAgentExtensionConverterV4().validateElasticProfileRequestBody(configuration);
        assertThatJson(requestBody).isEqualTo("{\"key3\":null,\"key2\":\"value2\",\"key1\":\"value1\"}");
    }

    @Test
    public void shouldHandleValidationResponse() {
        String responseBody = "[{\"key\":\"key-one\",\"message\":\"error on key one\"}, {\"key\":\"key-two\",\"message\":\"error on key two\"}]";
        ValidationResult result = new ElasticAgentExtensionConverterV4().getElasticProfileValidationResultResponseFromBody(responseBody);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.getErrors().size(), is(2));
        assertThat(result.getErrors().get(0).getKey(), is("key-one"));
        assertThat(result.getErrors().get(0).getMessage(), is("error on key one"));
        assertThat(result.getErrors().get(1).getKey(), is("key-two"));
        assertThat(result.getErrors().get(1).getMessage(), is("error on key two"));
    }

    @Test
    public void shouldUnJSONizeGetProfileViewResponseFromBody() {
        String template = new ElasticAgentExtensionConverterV4().getProfileViewResponseFromBody("{\"template\":\"foo\"}");
        assertThat(template, is("foo"));
    }

    @Test
    public void shouldUnJSONizeGetImageResponseFromBody() {
        com.thoughtworks.go.plugin.domain.common.Image image = new ElasticAgentExtensionConverterV4().getImageResponseFromBody("{\"content_type\":\"foo\", \"data\":\"bar\"}");
        assertThat(image.getContentType(), is("foo"));
        assertThat(image.getData(), is("bar"));
    }

    @Test
    public void shouldGetStatusReportViewFromResponseBody() {
        String template = new ElasticAgentExtensionConverterV4().getStatusReportView("{\"view\":\"foo\"}");
        assertThat(template, is("foo"));
    }

    @Test
    public void shouldGetCapabilitiesFromResponseBody() {
        String responseBody = "{\"supports_status_report\":\"true\",\"supports_agent_status_report\":\"true\"}";

        Capabilities capabilities = new ElasticAgentExtensionConverterV4().getCapabilitiesFromResponseBody(responseBody);

        assertTrue(capabilities.supportsPluginStatusReport());
        assertTrue(capabilities.supportsAgentStatusReport());
    }

    private AgentMetadata elasticAgent() {
        return new AgentMetadata("42", "Idle", "Idle", "Enabled");
    }
}
