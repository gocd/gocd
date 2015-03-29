package com.thoughtworks.go.plugin.access.artifactcleanup;

import com.thoughtworks.go.plugin.access.PluginInteractionCallback;
import com.thoughtworks.go.plugin.access.PluginRequestHelper;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

@Component
public class ArtifactCleanupExtension {

    public static final String EXTENSION_NAME = "artifact-cleanup";

    public static final String REQUEST_STAGE_INSTANCE_LIST = "stage-instance-list";

    private static final List<String> goSupportedVersions = asList("1.0");

    private PluginManager pluginManager;

    private final PluginRequestHelper pluginRequestHelper;

    private Map<String, JsonMessageHandler> messageHandlerMap = new HashMap<String, JsonMessageHandler>();


    @Autowired
    public ArtifactCleanupExtension(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
        pluginRequestHelper = new PluginRequestHelper(pluginManager, goSupportedVersions, EXTENSION_NAME);
        messageHandlerMap.put("1.0", new JsonMessageHandler1_0());

    }

    public List<ArtifactExtensionStageInstance> getStageInstancesForArtifactCleanup(String pluginId, final List<ArtifactExtensionStageConfiguration> stageConfigurations) {
        return pluginRequestHelper.submitRequest(pluginId, REQUEST_STAGE_INSTANCE_LIST, new PluginInteractionCallback<List<ArtifactExtensionStageInstance>>() {
            @Override
            public String requestBody(String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).requestGetStageInstancesForArtifactCleanup(stageConfigurations);
            }

            @Override
            public Map<String, String> requestParams(String resolvedExtensionVersion) {
                return null;
            }

            @Override
            public List<ArtifactExtensionStageInstance> onSuccess(String responseBody, String resolvedExtensionVersion) {
                return messageHandlerMap.get(resolvedExtensionVersion).responseGetStageInstancesForArtifactCleanup(responseBody);
            }
        });

    }
}

