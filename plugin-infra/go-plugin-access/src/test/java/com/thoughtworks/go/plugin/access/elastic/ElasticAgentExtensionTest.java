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
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.serverinfo.MessageHandlerForServerInfoRequestProcessor;
import com.thoughtworks.go.plugin.access.common.serverinfo.MessageHandlerForServerInfoRequestProcessor1_0;
import com.thoughtworks.go.plugin.access.common.settings.MessageHandlerForPluginSettingsRequestProcessor;
import com.thoughtworks.go.plugin.access.common.settings.MessageHandlerForPluginSettingsRequestProcessor1_0;
import com.thoughtworks.go.plugin.access.elastic.models.AgentMetadata;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.validation.ValidationError;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.elastic.Capabilities;
import com.thoughtworks.go.plugin.infra.PluginManager;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.ReflectionUtil;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.ArgumentCaptor;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension.SUPPORTED_VERSIONS;
import static com.thoughtworks.go.plugin.access.elastic.v3.ElasticAgentPluginConstantsV3.*;
import static com.thoughtworks.go.plugin.domain.common.PluginConstants.ELASTIC_AGENT_EXTENSION;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElasticAgentExtensionTest {
    private static final String PLUGIN_ID = "cd.go.example.plugin";
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    protected PluginManager pluginManager;
    protected ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;
    protected GoPluginDescriptor descriptor;
    protected ElasticAgentExtension extension;


    @Before
    public void setUp() throws Exception {
        pluginManager = mock(PluginManager.class);
        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);
        descriptor = mock(GoPluginDescriptor.class);
        extension = new ElasticAgentExtension(pluginManager);

        when(descriptor.id()).thenReturn(PLUGIN_ID);

        when(pluginManager.getPluginDescriptorFor(PLUGIN_ID)).thenReturn(descriptor);
        when(pluginManager.isPluginOfType(ELASTIC_AGENT_EXTENSION, PLUGIN_ID)).thenReturn(true);

        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, ELASTIC_AGENT_EXTENSION, SUPPORTED_VERSIONS)).thenReturn("3.0");
    }

    @Test
    public void shouldHaveVersionedElasticAgentExtensionForAllSupportedVersions() {
        for (String supportedVersion : SUPPORTED_VERSIONS) {
            final String message = String.format("Must define versioned extension class for %s extension with version %s", ELASTIC_AGENT_EXTENSION, supportedVersion);

            when(pluginManager.resolveExtensionVersion(PLUGIN_ID, ELASTIC_AGENT_EXTENSION, SUPPORTED_VERSIONS)).thenReturn(supportedVersion);

            final VersionedElasticAgentExtension extension = this.extension.getVersionedElasticAgentExtension(PLUGIN_ID);

            assertNotNull(message, extension);

            assertThat(ReflectionUtil.getField(extension, "VERSION"), is(supportedVersion));
        }
    }

    @Test
    public void shouldCallTheVersionedExtensionBasedOnResolvedVersion() throws JSONException {
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(ELASTIC_AGENT_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success("{\"content_type\":\"image/png\",\"data\":\"Zm9vYmEK\"}"));

        extension.getIcon(PLUGIN_ID);

        assertExtensionRequest("3.0", REQUEST_GET_PLUGIN_SETTINGS_ICON, null);
    }

    @Test
    public void shouldGetCapabilitiesOfAPlugin() {
        final String responseBody = "{\"supports_status_report\":\"true\", \"supports_agent_status_report\":\"true\"}";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(ELASTIC_AGENT_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        final Capabilities capabilities = extension.getCapabilities(PLUGIN_ID);

        assertTrue(capabilities.supportsStatusReport());
        assertTrue(capabilities.supportsAgentStatusReport());
    }

    @Test
    public void shouldGetProfileMetadata() throws JSONException {
        String responseBody = "[{\"key\":\"Username\",\"metadata\":{\"required\":true,\"secure\":false}},{\"key\":\"Password\",\"metadata\":{\"required\":true,\"secure\":true}}]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(ELASTIC_AGENT_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        final List<PluginConfiguration> metadata = extension.getProfileMetadata(PLUGIN_ID);

        MatcherAssert.assertThat(metadata, hasSize(2));
        MatcherAssert.assertThat(metadata, containsInAnyOrder(
                new PluginConfiguration("Username", new Metadata(true, false)),
                new PluginConfiguration("Password", new Metadata(true, true))
        ));

        assertExtensionRequest("3.0", REQUEST_GET_PROFILE_METADATA, null);
    }

    @Test
    public void shouldGetProfileView() throws JSONException {
        String responseBody = "{ \"template\": \"<div>This is profile view snippet</div>\" }";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(ELASTIC_AGENT_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        final String view = extension.getProfileView(PLUGIN_ID);

        MatcherAssert.assertThat(view, is("<div>This is profile view snippet</div>"));

        assertExtensionRequest("3.0", REQUEST_GET_PROFILE_VIEW, null);
    }

    @Test
    public void shouldValidateProfile() throws JSONException {
        String responseBody = "[{\"message\":\"Url must not be blank.\",\"key\":\"Url\"},{\"message\":\"SearchBase must not be blank.\",\"key\":\"SearchBase\"}]";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(ELASTIC_AGENT_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        final ValidationResult result = extension.validate(PLUGIN_ID, Collections.emptyMap());

        MatcherAssert.assertThat(result.isSuccessful(), is(false));
        MatcherAssert.assertThat(result.getErrors(), containsInAnyOrder(
                new ValidationError("Url", "Url must not be blank."),
                new ValidationError("SearchBase", "SearchBase must not be blank.")
        ));

        assertExtensionRequest("3.0", REQUEST_VALIDATE_PROFILE, "{}");
    }

    @Test
    public void shouldMakeCreateAgentCall() throws JSONException {
        final Map<String, String> profile = Collections.singletonMap("ServerURL", "https://example.com/go");
        final JobIdentifier jobIdentifier = new JobIdentifier("up42", 2, "Test", "up42_stage", "10", "up42_job");
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(ELASTIC_AGENT_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(null));

        extension.createAgent(PLUGIN_ID, "auto-registration-key", "test-env", profile, jobIdentifier);

        String expectedRequestBody = "{\n" +
                "  \"auto_register_key\": \"auto-registration-key\",\n" +
                "  \"properties\": {\n" +
                "    \"ServerURL\": \"https://example.com/go\"\n" +
                "  },\n" +
                "  \"environment\": \"test-env\",\n" +
                "  \"job_identifier\": {\n" +
                "    \"pipeline_name\": \"up42\",\n" +
                "    \"pipeline_label\": \"Test\",\n" +
                "    \"pipeline_counter\": 2,\n" +
                "    \"stage_name\": \"up42_stage\",\n" +
                "    \"stage_counter\": \"10\",\n" +
                "    \"job_name\": \"up42_job\",\n" +
                "    \"job_id\": -1\n" +
                "  }\n" +
                "}";
        assertExtensionRequest("3.0", REQUEST_CREATE_AGENT, expectedRequestBody);
    }

    @Test
    public void shouldSendServerPing() throws JSONException {
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(ELASTIC_AGENT_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(null));

        extension.serverPing(PLUGIN_ID);

        assertExtensionRequest("3.0", REQUEST_SERVER_PING, null);
    }

    @Test
    public void shouldMakeShouldAssignWorkCall() throws JSONException {
        final Map<String, String> profile = Collections.singletonMap("ServerURL", "https://example.com/go");
        final AgentMetadata agentMetadata = new AgentMetadata("foo-agent-id", "Idle", "Idle", "Enabled");
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(ELASTIC_AGENT_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success("true"));

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
                "  },\n" +
                "  \"job_identifier\": {}\n" +
                "}";

        assertExtensionRequest("3.0", REQUEST_SHOULD_ASSIGN_WORK, expectedRequestBody);
    }

    @Test
    public void shouldGetStatusReport() throws JSONException {
        final String responseBody = "{\"view\":\"<div>This is a status report snippet.</div>\"}";
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(ELASTIC_AGENT_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        final String statusReportView = extension.getPluginStatusReport(PLUGIN_ID);

        MatcherAssert.assertThat(statusReportView, is("<div>This is a status report snippet.</div>"));
        assertExtensionRequest("3.0", REQUEST_STATUS_REPORT, null);
    }

    @Test
    public void shouldGetAgentStatusReport() throws JSONException {
        final String responseBody = "{\"view\":\"<div>This is a status report snippet.</div>\"}";
        final JobIdentifier jobIdentifier = new JobIdentifier("up42", 2, "Test", "up42_stage", "10", "up42_job");

        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(ELASTIC_AGENT_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));

        extension.getAgentStatusReport(PLUGIN_ID, jobIdentifier, "GoCD193659b3b930480287b898eeef0ade37");

        final String requestBody = "{\n" +
                "  \"job_identifier\": {\n" +
                "    \"pipeline_name\": \"up42\",\n" +
                "    \"pipeline_label\": \"Test\",\n" +
                "    \"pipeline_counter\": 2,\n" +
                "    \"stage_name\": \"up42_stage\",\n" +
                "    \"stage_counter\": \"10\",\n" +
                "    \"job_name\": \"up42_job\",\n" +
                "    \"job_id\": -1\n" +
                "  },\n" +
                "  \"elastic_agent_id\": \"GoCD193659b3b930480287b898eeef0ade37\"\n" +
                "}";

        assertExtensionRequest("3.0", REQUEST_AGENT_STATUS_REPORT, requestBody);
    }


    @Test
    public void shouldExtendAbstractExtension() {
        assertTrue(new ElasticAgentExtension(pluginManager) instanceof AbstractExtension);
    }

    @Test
    public void shouldHaveMessageHandlerForPluginSettingsRequestProcessorForAllExtensionVersions() {
        final ElasticAgentExtensionExposingHandlers extension = new ElasticAgentExtensionExposingHandlers(pluginManager);

        assertThat(extension.messageHandlerForPluginSettingsRequestProcessor("1.0"),
                instanceOf(MessageHandlerForPluginSettingsRequestProcessor1_0.class));

        assertThat(extension.messageHandlerForPluginSettingsRequestProcessor("2.0"),
                instanceOf(MessageHandlerForPluginSettingsRequestProcessor1_0.class));

        assertThat(extension.messageHandlerForPluginSettingsRequestProcessor("3.0"),
                instanceOf(MessageHandlerForPluginSettingsRequestProcessor1_0.class));
    }

    @Test
    public void shouldHaveMessageHandlerForServerInfoRequestProcessorForAllExtensionVersions() {
        final ElasticAgentExtensionExposingHandlers extension = new ElasticAgentExtensionExposingHandlers(pluginManager);

        assertThat(extension.messageHandlerForServerInfoRequestProcessor("1.0"),
                instanceOf(MessageHandlerForServerInfoRequestProcessor1_0.class));

        assertThat(extension.messageHandlerForServerInfoRequestProcessor("2.0"),
                instanceOf(MessageHandlerForServerInfoRequestProcessor1_0.class));

        assertThat(extension.messageHandlerForServerInfoRequestProcessor("3.0"),
                instanceOf(MessageHandlerForServerInfoRequestProcessor1_0.class));
    }

    class ElasticAgentExtensionExposingHandlers extends ElasticAgentExtension {
        public ElasticAgentExtensionExposingHandlers(PluginManager pluginManager) {
            super(pluginManager);
        }

        @Override
        public MessageHandlerForPluginSettingsRequestProcessor messageHandlerForPluginSettingsRequestProcessor(String pluginVersion) {
            return super.messageHandlerForPluginSettingsRequestProcessor(pluginVersion);
        }

        @Override
        public MessageHandlerForServerInfoRequestProcessor messageHandlerForServerInfoRequestProcessor(String pluginVersion) {
            return super.messageHandlerForServerInfoRequestProcessor(pluginVersion);
        }

        @Override
        public VersionedElasticAgentExtension getVersionedElasticAgentExtension(String pluginId) {
            return super.getVersionedElasticAgentExtension(pluginId);
        }
    }

    private void assertExtensionRequest(String extensionVersion, String requestName, String requestBody) throws JSONException {
        final GoPluginApiRequest request = requestArgumentCaptor.getValue();
        Assert.assertThat(request.requestName(), Matchers.is(requestName));
        Assert.assertThat(request.extensionVersion(), Matchers.is(extensionVersion));
        Assert.assertThat(request.extension(), Matchers.is(ELASTIC_AGENT_EXTENSION));
        JSONAssert.assertEquals(requestBody, request.requestBody(), true);
    }
}