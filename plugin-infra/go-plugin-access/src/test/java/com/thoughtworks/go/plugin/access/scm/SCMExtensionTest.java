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
package com.thoughtworks.go.plugin.access.scm;

import com.thoughtworks.go.plugin.access.ExtensionsRegistry;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConfiguration;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsConstants;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.scm.material.MaterialPollResult;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.SCM_EXTENSION;
import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SCMExtensionTest {
    public static final String PLUGIN_ID = "plugin-id";

    @Mock(lenient = true)
    private PluginManager pluginManager;
    @Mock
    private ExtensionsRegistry extensionsRegistry;
    @Mock
    private PluginSettingsJsonMessageHandler1_0 pluginSettingsJSONMessageHandler;
    @Mock
    private JsonMessageHandler1_0 jsonMessageHandler;
    private SCMExtension scmExtension;
    private String requestBody = "expected-request";
    private String responseBody = "expected-response";
    private PluginSettingsConfiguration pluginSettingsConfiguration;
    private SCMPropertyConfiguration scmPropertyConfiguration;
    private Map<String, String> materialData;
    private ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;

    @BeforeEach
    public void setUp() throws Exception {
        scmExtension = new SCMExtension(pluginManager, extensionsRegistry);
        scmExtension.getPluginSettingsMessageHandlerMap().put("1.0", pluginSettingsJSONMessageHandler);
        scmExtension.getMessageHandlerMap().put("1.0", jsonMessageHandler);

        pluginSettingsConfiguration = new PluginSettingsConfiguration();
        scmPropertyConfiguration = new SCMPropertyConfiguration();
        materialData = new HashMap<>();

        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);

        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, SCM_EXTENSION, asList("1.0"))).thenReturn("1.0");
        when(pluginManager.isPluginOfType(SCM_EXTENSION, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(SCM_EXTENSION), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));
    }

    @Test
    public void shouldExtendAbstractExtension() throws Exception {
        assertTrue(scmExtension instanceof AbstractExtension);
    }

    @Test
    public void shouldTalkToPluginToGetPluginSettingsConfiguration() throws Exception {
        PluginSettingsConfiguration deserializedResponse = new PluginSettingsConfiguration();
        when(pluginSettingsJSONMessageHandler.responseMessageForPluginSettingsConfiguration(responseBody)).thenReturn(deserializedResponse);

        PluginSettingsConfiguration response = scmExtension.getPluginSettingsConfiguration(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), SCM_EXTENSION, "1.0", PluginSettingsConstants.REQUEST_PLUGIN_SETTINGS_CONFIGURATION, null);
        verify(pluginSettingsJSONMessageHandler).responseMessageForPluginSettingsConfiguration(responseBody);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToGetPluginSettingsView() throws Exception {
        String deserializedResponse = "";
        when(pluginSettingsJSONMessageHandler.responseMessageForPluginSettingsView(responseBody)).thenReturn(deserializedResponse);

        String response = scmExtension.getPluginSettingsView(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), SCM_EXTENSION, "1.0", PluginSettingsConstants.REQUEST_PLUGIN_SETTINGS_VIEW, null);
        verify(pluginSettingsJSONMessageHandler).responseMessageForPluginSettingsView(responseBody);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToGetValidatePluginSettings() throws Exception {
        when(pluginSettingsJSONMessageHandler.requestMessageForPluginSettingsValidation(pluginSettingsConfiguration)).thenReturn(requestBody);
        ValidationResult deserializedResponse = new ValidationResult();
        when(pluginSettingsJSONMessageHandler.responseMessageForPluginSettingsValidation(responseBody)).thenReturn(deserializedResponse);

        ValidationResult response = scmExtension.validatePluginSettings(PLUGIN_ID, pluginSettingsConfiguration);

        assertRequest(requestArgumentCaptor.getValue(), SCM_EXTENSION, "1.0", PluginSettingsConstants.REQUEST_VALIDATE_PLUGIN_SETTINGS, requestBody);
        verify(pluginSettingsJSONMessageHandler).responseMessageForPluginSettingsValidation(responseBody);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToGetSCMConfiguration() throws Exception {
        SCMPropertyConfiguration deserializedResponse = new SCMPropertyConfiguration();
        when(jsonMessageHandler.responseMessageForSCMConfiguration(responseBody)).thenReturn(deserializedResponse);

        SCMPropertyConfiguration response = scmExtension.getSCMConfiguration(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), SCM_EXTENSION, "1.0", SCMExtension.REQUEST_SCM_CONFIGURATION, null);
        verify(jsonMessageHandler).responseMessageForSCMConfiguration(responseBody);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToGetSCMView() throws Exception {
        SCMView deserializedResponse = new SCMView() {
            @Override
            public String displayValue() {
                return null;
            }

            @Override
            public String template() {
                return null;
            }
        };
        when(jsonMessageHandler.responseMessageForSCMView(responseBody)).thenReturn(deserializedResponse);

        SCMView response = scmExtension.getSCMView(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), SCM_EXTENSION, "1.0", SCMExtension.REQUEST_SCM_VIEW, null);
        verify(jsonMessageHandler).responseMessageForSCMView(responseBody);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToCheckIfSCMConfigurationIsValid() throws Exception {
        when(jsonMessageHandler.requestMessageForIsSCMConfigurationValid(scmPropertyConfiguration)).thenReturn(requestBody);
        ValidationResult deserializedResponse = new ValidationResult();
        when(jsonMessageHandler.responseMessageForIsSCMConfigurationValid(responseBody)).thenReturn(deserializedResponse);

        ValidationResult response = scmExtension.isSCMConfigurationValid(PLUGIN_ID, scmPropertyConfiguration);

        assertRequest(requestArgumentCaptor.getValue(), SCM_EXTENSION, "1.0", SCMExtension.REQUEST_VALIDATE_SCM_CONFIGURATION, requestBody);
        verify(jsonMessageHandler).requestMessageForIsSCMConfigurationValid(scmPropertyConfiguration);
        verify(jsonMessageHandler).responseMessageForIsSCMConfigurationValid(responseBody);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToCheckSCMConnectionSuccessful() throws Exception {
        when(jsonMessageHandler.requestMessageForCheckConnectionToSCM(scmPropertyConfiguration)).thenReturn(requestBody);
        Result deserializedResponse = new Result();
        when(jsonMessageHandler.responseMessageForCheckConnectionToSCM(responseBody)).thenReturn(deserializedResponse);

        Result response = scmExtension.checkConnectionToSCM(PLUGIN_ID, scmPropertyConfiguration);

        assertRequest(requestArgumentCaptor.getValue(), SCM_EXTENSION, "1.0", SCMExtension.REQUEST_CHECK_SCM_CONNECTION, requestBody);
        verify(jsonMessageHandler).requestMessageForCheckConnectionToSCM(scmPropertyConfiguration);
        verify(jsonMessageHandler).responseMessageForCheckConnectionToSCM(responseBody);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToGetLatestModification() throws Exception {
        String flyweight = "flyweight";
        when(jsonMessageHandler.requestMessageForLatestRevision(scmPropertyConfiguration, materialData, flyweight)).thenReturn(requestBody);
        MaterialPollResult deserializedResponse = new MaterialPollResult();
        when(jsonMessageHandler.responseMessageForLatestRevision(responseBody)).thenReturn(deserializedResponse);

        MaterialPollResult response = scmExtension.getLatestRevision(PLUGIN_ID, scmPropertyConfiguration, materialData, flyweight);

        assertRequest(requestArgumentCaptor.getValue(), SCM_EXTENSION, "1.0", SCMExtension.REQUEST_LATEST_REVISION, requestBody);
        verify(jsonMessageHandler).requestMessageForLatestRevision(scmPropertyConfiguration, materialData, flyweight);
        verify(jsonMessageHandler).responseMessageForLatestRevision(responseBody);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToGetLatestModificationSinceLastRevision() throws Exception {
        String flyweight = "flyweight";
        SCMRevision previouslyKnownRevision = new SCMRevision();
        when(jsonMessageHandler.requestMessageForLatestRevisionsSince(scmPropertyConfiguration, materialData, flyweight, previouslyKnownRevision)).thenReturn(requestBody);
        MaterialPollResult deserializedResponse = new MaterialPollResult();
        when(jsonMessageHandler.responseMessageForLatestRevisionsSince(responseBody)).thenReturn(deserializedResponse);

        MaterialPollResult response = scmExtension.latestModificationSince(PLUGIN_ID, scmPropertyConfiguration, materialData, flyweight, previouslyKnownRevision);

        assertRequest(requestArgumentCaptor.getValue(), SCM_EXTENSION, "1.0", SCMExtension.REQUEST_LATEST_REVISIONS_SINCE, requestBody);
        verify(jsonMessageHandler).requestMessageForLatestRevisionsSince(scmPropertyConfiguration, materialData, flyweight, previouslyKnownRevision);
        verify(jsonMessageHandler).responseMessageForLatestRevisionsSince(responseBody);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToCheckout() throws Exception {
        String destination = "destination";
        SCMRevision revision = new SCMRevision();
        when(jsonMessageHandler.requestMessageForCheckout(scmPropertyConfiguration, destination, revision)).thenReturn(requestBody);
        Result deserializedResponse = new Result();
        when(jsonMessageHandler.responseMessageForCheckout(responseBody)).thenReturn(deserializedResponse);

        Result response = scmExtension.checkout(PLUGIN_ID, scmPropertyConfiguration, destination, revision);

        assertRequest(requestArgumentCaptor.getValue(), SCM_EXTENSION, "1.0", SCMExtension.REQUEST_CHECKOUT, requestBody);
        verify(jsonMessageHandler).requestMessageForCheckout(scmPropertyConfiguration, destination, revision);
        verify(jsonMessageHandler).responseMessageForCheckout(responseBody);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldHandleExceptionDuringPluginInteraction() throws Exception {
        when(pluginManager.submitTo(eq(PLUGIN_ID), eq(SCM_EXTENSION), requestArgumentCaptor.capture())).thenThrow(new RuntimeException("exception-from-plugin"));
        try {
            scmExtension.checkConnectionToSCM(PLUGIN_ID, scmPropertyConfiguration);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("exception-from-plugin"));
        }
    }

    private void assertRequest(GoPluginApiRequest goPluginApiRequest, String extensionName, String version, String requestName, String requestBody) {
        assertThat(goPluginApiRequest.extension(), is(extensionName));
        assertThat(goPluginApiRequest.extensionVersion(), is(version));
        assertThat(goPluginApiRequest.requestName(), is(requestName));
        assertThat(goPluginApiRequest.requestBody(), is(requestBody));
    }
}
