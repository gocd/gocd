package com.thoughtworks.go.plugin.access.configrepo;

import com.thoughtworks.go.plugin.access.PluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.common.settings.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRConfigurationProperty;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRParseResult;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

@Component
public class ConfigRepoExtension extends AbstractExtension implements ConfigRepoExtensionContract {
    public static final String EXTENSION_NAME = "configrepo";
    public static final String REQUEST_PARSE_DIRECTORY = "parse-directory";

    private static final List<String> goSupportedVersions = asList("1.0");

    private Map<String, JsonMessageHandler> messageHandlerMap = new HashMap<String, JsonMessageHandler>();

    @Autowired
    public ConfigRepoExtension(PluginManager pluginManager) {
        super(pluginManager, new PluginRequestHelper(pluginManager, goSupportedVersions, EXTENSION_NAME),EXTENSION_NAME);
        pluginSettingsMessageHandlerMap.put("1.0", new PluginSettingsJsonMessageHandler1_0());
        messageHandlerMap.put("1.0", new JsonMessageHandler1_0());
    }

    @Override
    public CRParseResult parseDirectory(String pluginId, final String destinationFolder, final Collection<CRConfigurationProperty> configurations) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_PARSE_DIRECTORY, new PluginInteractionCallback<CRParseResult>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestMessageForParseDirectory(destinationFolder,configurations);
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
        return pluginManager.isPluginOfType(ConfigRepoExtension.EXTENSION_NAME, pluginId);
    }
}
