/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.access.elastic.ElasticAgentPluginConstants.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public class ElasticAgentExtensionTestV1 extends ElasticAgentExtensionTest {
    private static final String PLUGIN_ID = "cd.go.example.plugin";

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        setExtensionVersionForPluginTo(PLUGIN_ID, "1.0");
    }

    @Test
    public void shouldGetPluginIcon() throws JSONException {
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success("{\"content_type\":\"image/png\",\"data\":\"Zm9vYmEK\"}"));
        final Image icon = extension.getIcon(PLUGIN_ID);

        assertThat(icon.getContentType(), is("image/png"));
        assertThat(icon.getData(), is("Zm9vYmEK"));

        assertExtensionRequest("1.0", REQUEST_GET_PLUGIN_SETTINGS_ICON, null);
    }

    @Test
    public void shouldGetCapabilitiesOfAPlugin() {
        final Capabilities capabilities = extension.getCapabilities(PLUGIN_ID);

        assertFalse(capabilities.supportsAgentStatusReport());
        assertFalse(capabilities.supportsStatusReport());
    }

    @Test
    public void shouldGetProfileMetadata() throws JSONException {
        String responseBody = "[{\"key\":\"username\",\"metadata\":{\"required\":true,\"secure\":false}},{\"key\":\"password\",\"metadata\":{\"required\":true,\"secure\":true}}]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        final List<PluginConfiguration> metadata = extension.getProfileMetadata(PLUGIN_ID);

        assertThat(metadata, hasSize(2));
        assertThat(metadata, containsInAnyOrder(
                new PluginConfiguration("username", new Metadata(true, false)),
                new PluginConfiguration("password", new Metadata(true, true))
        ));

        assertExtensionRequest("1.0", REQUEST_GET_PROFILE_METADATA, null);
    }

    @Test
    public void shouldGetProfileView() throws JSONException {
        String responseBody = "{ \"template\": \"<div>This is view snippet</div>\" }";
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        final String view = extension.getProfileView(PLUGIN_ID);

        assertThat(view, is("<div>This is view snippet</div>"));

        assertExtensionRequest("1.0", REQUEST_GET_PROFILE_VIEW, null);
    }

    @Test
    public void shouldValidateProfile() throws JSONException {
        String responseBody = "[{\"message\":\"Url must not be blank.\",\"key\":\"Url\"},{\"message\":\"SearchBase must not be blank.\",\"key\":\"SearchBase\"}]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        final ValidationResult result = extension.validate(PLUGIN_ID, Collections.emptyMap());

        assertThat(result.isSuccessful(), is(false));
        assertThat(result.getErrors(), containsInAnyOrder(
                new ValidationError("Url", "Url must not be blank."),
                new ValidationError("SearchBase", "SearchBase must not be blank.")
        ));

        assertExtensionRequest("1.0", REQUEST_VALIDATE_PROFILE, "{}");
    }

    @Test
    public void shouldMakeCreateAgentCall() throws JSONException {
        final Map<String, String> profile = Collections.singletonMap("ServerURL", "https://example.com/go");
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(null));

        extension.createAgent(PLUGIN_ID, "auto-registration-key", "test-env", profile, new JobIdentifier());

        String expectedRequestBody = "{\n" +
                "  \"auto_register_key\": \"auto-registration-key\",\n" +
                "  \"properties\": {\n" +
                "    \"ServerURL\": \"https://example.com/go\"\n" +
                "  },\n" +
                "  \"environment\": \"test-env\"\n" +
                "}";
        assertExtensionRequest("1.0", REQUEST_CREATE_AGENT, expectedRequestBody);
    }

    @Test
    public void shouldSendServerPing() throws JSONException {
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(null));

        extension.serverPing(PLUGIN_ID);

        assertExtensionRequest("1.0", REQUEST_SERVER_PING, null);
    }

    @Test
    public void shouldMakeShouldAssignWorkCall() throws JSONException {
        final Map<String, String> profile = Collections.singletonMap("ServerURL", "https://example.com/go");
        final AgentMetadata agentMetadata = new AgentMetadata("foo-agent-id", "Idle", "Idle", "Enabled");
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success("true"));

        final boolean shouldAssignWork = extension.shouldAssignWork(PLUGIN_ID, agentMetadata, "test-env", profile, new JobIdentifier());

        assertTrue(shouldAssignWork);

        String expectedRequestBody = "{\n" +
                "  \"properties\": {\n" +
                "    \"ServerURL\": \"https://example.com/go\"\n" +
                "  },\n" +
                "  \"environment\": \"test-env\",\n" +
                "  \"agent\": {\n" +
                "    \"agent_id\": \"foo-agent-id\",\n" +
                "    \"agent_state\": \"Idle\",\n" +
                "    \"build_state\": \"Idle\",\n" +
                "    \"config_state\": \"Enabled\"\n" +
                "  }\n" +
                "}";

        assertExtensionRequest("1.0", REQUEST_SHOULD_ASSIGN_WORK, expectedRequestBody);
    }

    @Test
    public void shouldErrorOutForGetStatusReport() throws JSONException {
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenThrow(new RuntimeException("error"));

        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Interaction with plugin with id 'cd.go.example.plugin' implementing 'elastic-agent' extension failed while requesting for 'go.cd.elastic-agent.status-report'. Reason: [error]");

        try {
            extension.getPluginStatusReport(PLUGIN_ID);
        } catch (Exception e) {
            assertExtensionRequest("1.0", REQUEST_STATUS_REPORT, null);
            throw e;
        }
    }

    @Test
    public void shouldErrorOutForGetAgentStatusReport() {
        thrown.expect(RuntimeException.class);
        thrown.expectMessage("Interaction with plugin with id 'cd.go.example.plugin' implementing 'elastic-agent' extension failed while requesting for 'go.cd.elastic-agent.agent-status-report'. Reason: [Agent status report is not supported in elastic-agent extension v1.]");

        extension.getAgentStatusReport(PLUGIN_ID, null, null);
    }
}

