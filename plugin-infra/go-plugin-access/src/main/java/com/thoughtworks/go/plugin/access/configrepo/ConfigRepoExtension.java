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

package com.thoughtworks.go.plugin.access.configrepo;

import com.thoughtworks.go.plugin.access.DefaultPluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.common.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.MessageHandlerForPluginSettingsRequestProcessor1_0;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler2_0;
import com.thoughtworks.go.plugin.configrepo.codec.GsonCodec;
import com.thoughtworks.go.plugin.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.configrepo.contract.CRParseResult;
import com.thoughtworks.go.plugin.configrepo.contract.CRPipeline;
import com.thoughtworks.go.plugin.access.configrepo.v1.JsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.configrepo.v2.JsonMessageHandler2_0;
import com.thoughtworks.go.plugin.domain.configrepo.Capabilities;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.CONFIG_REPO_EXTENSION;
import static java.util.Arrays.asList;

@Component
public class ConfigRepoExtension extends AbstractExtension implements ConfigRepoExtensionContract {
    public static final String REQUEST_PARSE_DIRECTORY = "parse-directory";
    public static final String REQUEST_PIPELINE_EXPORT = "pipeline-export";
    public static final String REQUEST_CAPABILITIES = "get-capabilities";

    private static final List<String> goSupportedVersions = asList("1.0", "2.0");

    private Map<String, JsonMessageHandler> messageHandlerMap = new HashMap<>();

    @Autowired
    public ConfigRepoExtension(PluginManager pluginManager) {
        super(pluginManager, new PluginRequestHelper(pluginManager, goSupportedVersions, CONFIG_REPO_EXTENSION), CONFIG_REPO_EXTENSION);
        registerHandler("1.0", new PluginSettingsJsonMessageHandler1_0());
        messageHandlerMap.put("1.0", new JsonMessageHandler1_0(new GsonCodec(), new ConfigRepoMigrator()));
        registerMessageHandlerForPluginSettingsRequestProcessor("1.0", new MessageHandlerForPluginSettingsRequestProcessor1_0());

        registerHandler("2.0", new PluginSettingsJsonMessageHandler2_0());
        messageHandlerMap.put("2.0", new JsonMessageHandler2_0(new GsonCodec(), new ConfigRepoMigrator()));
        registerMessageHandlerForPluginSettingsRequestProcessor("2.0", new MessageHandlerForPluginSettingsRequestProcessor1_0());
    }

    @Override
    public String pipelineExport(final String pluginId, final CRPipeline pipeline) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_PIPELINE_EXPORT, new DefaultPluginInteractionCallback<String>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForPipelineExport(pipeline);
            }

            @Override
            public Map<String, String> requestParams(String resolvedExtensionVersion) {
                return null;
            }

            @Override
            public String onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForPipelineExport(responseBody);
            }
        });
    }

    @Override
    public Capabilities getCapabilities(String pluginId) {
        String resolvedExtensionVersion = pluginManager.resolveExtensionVersion(pluginId, CONFIG_REPO_EXTENSION, goSupportedVersions);
        if (resolvedExtensionVersion.equals("1.0")) {
            return new Capabilities(false);
        }
       return pluginRequestHelper.submitRequest(pluginId, REQUEST_CAPABILITIES, new DefaultPluginInteractionCallback<Capabilities>() {
           @Override
           public Capabilities onSuccess(String responseBody, String resolvedExtensionVersion) {
               return messageHandlerMap.get(resolvedExtensionVersion).getCapabilitiesFromResponse(responseBody);
           }
       });
    }

    @Override
    public CRParseResult parseDirectory(String pluginId, final String destinationFolder, final Collection<CRConfigurationProperty> configurations) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_PARSE_DIRECTORY, new DefaultPluginInteractionCallback<CRParseResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForParseDirectory(destinationFolder, configurations);
            }

            @Override
            public Map<String, String> requestParams(String resolvedExtensionVersion) {
                return null;
            }

            @Override
            public CRParseResult onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseMessageForParseDirectory(responseBody);
            }
        });
    }

    public Map<String, JsonMessageHandler> getMessageHandlerMap() {
        return messageHandlerMap;
    }

    public boolean isConfigRepoPlugin(String pluginId) {
        return pluginManager.isPluginOfType(CONFIG_REPO_EXTENSION, pluginId);
    }

    @Override
    protected List<String> goSupportedVersions() {
        return goSupportedVersions;
    }

    @Override
    public String serverInfoJSON(String pluginId, String serverId, String siteUrl, String secureSiteUrl) {
        throw new UnsupportedOperationException("Fetch Server Info is not supported by ConfigRepo endpoint.");
    }
}
