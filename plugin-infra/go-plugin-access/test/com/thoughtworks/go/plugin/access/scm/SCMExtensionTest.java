/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.access.scm;

import com.thoughtworks.go.plugin.access.scm.material.MaterialPollResult;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class SCMExtensionTest {
    public static final String PLUGIN_ID = "plugin-id";

    @Mock
    private PluginManager pluginManager;
    @Mock
    private JsonMessageHandler1_0 jsonMessageHandler;
    private SCMExtension scmExtension;
    private String requestBody = "expected-request";
    private String responseBody = "expected-response";
    private SCMPropertyConfiguration scmPropertyConfiguration;
    private Map<String, String> materialData;
    private ArgumentCaptor<GoPluginApiRequest> requestArgumentCaptor;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        scmExtension = new SCMExtension(pluginManager);
        scmExtension.getMessageHandlerMap().put("1.0", jsonMessageHandler);

        scmPropertyConfiguration = new SCMPropertyConfiguration();
        materialData = new HashMap<String, String>();

        requestArgumentCaptor = ArgumentCaptor.forClass(GoPluginApiRequest.class);

        when(pluginManager.resolveExtensionVersion(PLUGIN_ID, asList("1.0"))).thenReturn("1.0");
        when(pluginManager.isPluginOfType(SCMExtension.EXTENSION_NAME, PLUGIN_ID)).thenReturn(true);
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenReturn(DefaultGoPluginApiResponse.success(responseBody));
    }

    @Test
    public void shouldTalkToPluginToGetSCMConfiguration() throws Exception {
        SCMPropertyConfiguration deserializedResponse = new SCMPropertyConfiguration();
        when(jsonMessageHandler.responseMessageForSCMConfiguration(responseBody)).thenReturn(deserializedResponse);

        SCMPropertyConfiguration response = scmExtension.getSCMConfiguration(PLUGIN_ID);

        assertRequest(requestArgumentCaptor.getValue(), SCMExtension.EXTENSION_NAME, "1.0", SCMExtension.REQUEST_SCM_CONFIGURATION, null);
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

        assertRequest(requestArgumentCaptor.getValue(), SCMExtension.EXTENSION_NAME, "1.0", SCMExtension.REQUEST_SCM_VIEW, null);
        verify(jsonMessageHandler).responseMessageForSCMView(responseBody);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldTalkToPluginToCheckIfSCMConfigurationIsValid() throws Exception {
        when(jsonMessageHandler.requestMessageForIsSCMConfigurationValid(scmPropertyConfiguration)).thenReturn(requestBody);
        ValidationResult deserializedResponse = new ValidationResult();
        when(jsonMessageHandler.responseMessageForIsSCMConfigurationValid(responseBody)).thenReturn(deserializedResponse);

        ValidationResult response = scmExtension.isSCMConfigurationValid(PLUGIN_ID, scmPropertyConfiguration);

        assertRequest(requestArgumentCaptor.getValue(), SCMExtension.EXTENSION_NAME, "1.0", SCMExtension.REQUEST_VALIDATE_SCM_CONFIGURATION, requestBody);
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

        assertRequest(requestArgumentCaptor.getValue(), SCMExtension.EXTENSION_NAME, "1.0", SCMExtension.REQUEST_CHECK_SCM_CONNECTION, requestBody);
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

        assertRequest(requestArgumentCaptor.getValue(), SCMExtension.EXTENSION_NAME, "1.0", SCMExtension.REQUEST_LATEST_REVISION, requestBody);
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

        assertRequest(requestArgumentCaptor.getValue(), SCMExtension.EXTENSION_NAME, "1.0", SCMExtension.REQUEST_LATEST_REVISIONS_SINCE, requestBody);
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

        assertRequest(requestArgumentCaptor.getValue(), SCMExtension.EXTENSION_NAME, "1.0", SCMExtension.REQUEST_CHECKOUT, requestBody);
        verify(jsonMessageHandler).requestMessageForCheckout(scmPropertyConfiguration, destination, revision);
        verify(jsonMessageHandler).responseMessageForCheckout(responseBody);
        assertSame(response, deserializedResponse);
    }

    @Test
    public void shouldHandleExceptionDuringPluginInteraction() throws Exception {
        when(pluginManager.submitTo(eq(PLUGIN_ID), requestArgumentCaptor.capture())).thenThrow(new RuntimeException("exception-from-plugin"));
        try {
            scmExtension.checkConnectionToSCM(PLUGIN_ID, scmPropertyConfiguration);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Interaction with plugin with id 'plugin-id' implementing 'scm' extension failed while requesting for 'check-scm-connection'. Reason: [exception-from-plugin]"));
        }
    }

    private void assertRequest(GoPluginApiRequest goPluginApiRequest, String extensionName, String version, String requestName, String requestBody) {
        assertThat(goPluginApiRequest.extension(), is(extensionName));
        assertThat(goPluginApiRequest.extensionVersion(), is(version));
        assertThat(goPluginApiRequest.requestName(), is(requestName));
        assertThat(goPluginApiRequest.requestBody(), is(requestBody));
    }
}
