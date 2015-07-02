package com.thoughtworks.go.plugin.access.configrepo;

import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.access.common.settings.AbstractExtension;
import com.thoughtworks.go.plugin.access.common.settings.PluginSettingsJsonMessageHandler1_0;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRPartialConfig;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

@Extension
public class ConfigRepoExtension extends AbstractExtension implements ConfigRepoExtensionContract {
    public static final String EXTENSION_NAME = "configrepo";
    private static final List<String> goSupportedVersions = asList("1.0");

    private Map<String, JsonMessageHandler> messageHandlerMap = new HashMap<String, JsonMessageHandler>();

    @Autowired
    public ConfigRepoExtension(PluginManager pluginManager) {
        super(pluginManager, new PluginRequestHelper(pluginManager, goSupportedVersions, EXTENSION_NAME));
        pluginSettingsMessageHandlerMap.put("1.0", new PluginSettingsJsonMessageHandler1_0());
        messageHandlerMap.put("1.0", new JsonMessageHandler1_0());
    }

    @Override
    public PartialConfig ParseCheckout(String pluginId, String destinationFolder) {
        return null;
    }
}
