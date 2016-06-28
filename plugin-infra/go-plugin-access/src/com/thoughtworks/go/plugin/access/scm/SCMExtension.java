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

package com.thoughtworks.go.plugin.access.scm;

import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.common.settings.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.scm.material.MaterialPollResult;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

@Component
public class SCMExtension extends AbstractExtension implements SCMExtensionContract {
    public static final String EXTENSION_NAME = "scm";
    private static final List<String> goSupportedVersions = asList("1.0");

    public static final String REQUEST_SCM_CONFIGURATION = "scm-configuration";
    public static final String REQUEST_SCM_VIEW = "scm-view";
    public static final String REQUEST_VALIDATE_SCM_CONFIGURATION = "validate-scm-configuration";
    public static final String REQUEST_CHECK_SCM_CONNECTION = "check-scm-connection";
    public static final String REQUEST_LATEST_REVISION = "latest-revision";
    public static final String REQUEST_LATEST_REVISIONS_SINCE = "latest-revisions-since";
    public static final String REQUEST_CHECKOUT = "checkout";

    private Map<String, JsonMessageHandler> messageHandlerMap = new HashMap<>();

    @Autowired
    public SCMExtension(PluginManager pluginManager) {
        super(pluginManager, new PluginRequestHelper(pluginManager, goSupportedVersions, EXTENSION_NAME), EXTENSION_NAME);
        pluginSettingsMessageHandlerMap.put("1.0", new PluginSettingsJsonMessageHandler1_0());
        messageHandlerMap.put("1.0", new JsonMessageHandler1_0());
    }

    public SCMPropertyConfiguration getSCMConfiguration(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_SCM_CONFIGURATION, new DefaultPluginInteractionCallback<SCMPropertyConfiguration>() {

            @Override
            public SCMPropertyConfiguration onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForSCMConfiguration(responseBody);
            }
        });
    }

    public SCMView getSCMView(String pluginId) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_SCM_VIEW, new DefaultPluginInteractionCallback<SCMView>() {

            @Override
            public SCMView onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForSCMView(responseBody);
            }
        });
    }

    public ValidationResult isSCMConfigurationValid(String pluginId, final SCMPropertyConfiguration scmConfiguration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_VALIDATE_SCM_CONFIGURATION, new DefaultPluginInteractionCallback<ValidationResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForIsSCMConfigurationValid(scmConfiguration);
            }

            @Override
            public ValidationResult onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForIsSCMConfigurationValid(responseBody);
            }
        });
    }

    public Result checkConnectionToSCM(String pluginId, final SCMPropertyConfiguration scmConfiguration) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_CHECK_SCM_CONNECTION, new DefaultPluginInteractionCallback<Result>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForCheckConnectionToSCM(scmConfiguration);
            }

            @Override
            public Result onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForCheckConnectionToSCM(responseBody);
            }
        });
    }

    public MaterialPollResult getLatestRevision(String pluginId, final SCMPropertyConfiguration scmConfiguration, final Map<String, String> materialData, final String flyweightFolder) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_LATEST_REVISION, new DefaultPluginInteractionCallback<MaterialPollResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForLatestRevision(scmConfiguration, materialData, flyweightFolder);
            }

            @Override
            public MaterialPollResult onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForLatestRevision(responseBody);
            }
        });
    }

    public MaterialPollResult latestModificationSince(String pluginId, final SCMPropertyConfiguration scmConfiguration, final Map<String, String> materialData, final String flyweightFolder, final SCMRevision previouslyKnownRevision) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_LATEST_REVISIONS_SINCE, new DefaultPluginInteractionCallback<MaterialPollResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForLatestRevisionsSince(scmConfiguration, materialData, flyweightFolder, previouslyKnownRevision);
            }

            @Override
            public MaterialPollResult onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForLatestRevisionsSince(responseBody);
            }
        });
    }

    public Result checkout(String pluginId, final SCMPropertyConfiguration scmConfiguration, final String destinationFolder, final SCMRevision revision) {
        return pluginRequestHelper.submitRequest(pluginId, SCMExtension.REQUEST_CHECKOUT, new DefaultPluginInteractionCallback<Result>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForCheckout(scmConfiguration, destinationFolder, revision);
            }

            @Override
            public Result onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForCheckout(responseBody);
            }
        });
    }

    Map<String, PluginSettingsJsonMessageHandler> getPluginSettingsMessageHandlerMap() {
        return pluginSettingsMessageHandlerMap;
    }

    Map<String, JsonMessageHandler> getMessageHandlerMap() {
        return messageHandlerMap;
    }
}
