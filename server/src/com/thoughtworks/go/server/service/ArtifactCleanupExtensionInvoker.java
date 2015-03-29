package com.thoughtworks.go.server.service;


import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageConfigIdentifier;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.config.ArtifactCleanupStrategy;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.plugin.access.artifactcleanup.ArtifactCleanupExtension;
import com.thoughtworks.go.plugin.access.artifactcleanup.ArtifactExtensionStageConfiguration;
import com.thoughtworks.go.plugin.access.artifactcleanup.ArtifactExtensionStageInstance;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ArtifactCleanupExtensionInvoker implements ConfigChangedListener {

    private static final Logger LOGGER = Logger.getLogger(ArtifactCleanupExtensionInvoker.class);


    private ArtifactCleanupExtension artifactCleanupExtension;
    private GoConfigService goConfigService;
    private ArtifactsService artifactsService;
    private Map<PluginConfiguration, List<StageConfigIdentifier>> artifactCleanupPluginMap = null;
    private static final Object mutex = new Object();

    @Autowired
    public ArtifactCleanupExtensionInvoker(ArtifactCleanupExtension artifactCleanupExtension, GoConfigService goConfigService, ArtifactsService artifactsService) {
        this.artifactCleanupExtension = artifactCleanupExtension;
        this.goConfigService = goConfigService;
        this.artifactsService = artifactsService;
        goConfigService.register(this);
    }


    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        synchronized (mutex) {
            artifactCleanupPluginMap = null;
        }
    }

    Map<PluginConfiguration, List<StageConfigIdentifier>> buildArtifactCleanupPluginMap() {
        if (artifactCleanupPluginMap == null) {
            synchronized (mutex) {
                if (artifactCleanupPluginMap == null) {
                    artifactCleanupPluginMap = updatedPluginMap();
                }
            }
        }
        return Collections.unmodifiableMap(artifactCleanupPluginMap);
    }

    Map<PluginConfiguration, List<StageConfigIdentifier>> updatedPluginMap() {
        Map<PluginConfiguration, List<StageConfigIdentifier>> result = new HashMap<PluginConfiguration, List<StageConfigIdentifier>>();
        PluginConfiguration globalArtifactCleanupPlugin = globalArtifactCleanupPlugin();
        if (globalArtifactCleanupPlugin != null) {
            result.put(globalArtifactCleanupPlugin, new ArrayList<StageConfigIdentifier>());
        }

        for (PipelineConfig pipelineConfig : goConfigService.getCurrentConfig().getAllPipelineConfigs()) {
            for (StageConfig stageConfig : pipelineConfig) {
                StageConfigIdentifier stageConfigIdentifier = new StageConfigIdentifier(pipelineConfig.name(), stageConfig.name());
                PluginConfiguration stageArtifactCleanupPlugin = stageArtifactCleanupPlugin(stageConfig);
                if (stageArtifactCleanupPlugin != null) {
                    mapStageToArtifactCleanupPlugin(result, stageArtifactCleanupPlugin, stageConfigIdentifier);
                } else if (globalArtifactCleanupPlugin != null) {
                    result.get(globalArtifactCleanupPlugin).add(stageConfigIdentifier);
                }
            }
        }
        return result;
    }

    public int invokeStageLevelArtifactCleanupPlugins() {
        int numberOfStagesPurged = 0;

        List<PluginConfiguration> pluginConfigurations = getStageLevelArtifactCleanupPlugins();
        for (PluginConfiguration pluginConfiguration : pluginConfigurations) {
            List<ArtifactExtensionStageConfiguration> stageConfigurationList = map(buildArtifactCleanupPluginMap().get(pluginConfiguration));
            List<ArtifactExtensionStageInstance> stageInstances = artifactCleanupExtension.getStageInstancesForArtifactCleanup(pluginConfiguration.getId(), stageConfigurationList);
            numberOfStagesPurged += cleanupArtifacts(pluginConfiguration, stageInstances);

        }

        return numberOfStagesPurged;
    }

    public int invokeGlobalArtifactCleanupPlugin() {
        PluginConfiguration globalArtifactCleanupPlugin = globalArtifactCleanupPlugin();
        List<StageConfigIdentifier> list = buildArtifactCleanupPluginMap().get(globalArtifactCleanupPlugin);
        List<ArtifactExtensionStageInstance> stageInstances = artifactCleanupExtension.getStageInstancesForArtifactCleanup(globalArtifactCleanupPlugin.getId(), map(list));
        return cleanupArtifacts(globalArtifactCleanupPlugin, stageInstances);
    }

    int cleanupArtifacts(PluginConfiguration pluginConfiguration, List<ArtifactExtensionStageInstance> stageInstances) {
        int numberOfStagesPurged = 0;
        for (ArtifactExtensionStageInstance stageInstance : stageInstances) {
            if (!buildArtifactCleanupPluginMap().get(pluginConfiguration).contains(new StageConfigIdentifier(stageInstance.getPipelineName(), stageInstance.getStageName()))) {
                LOGGER.warn("this plugin is not allowed to cleanup artifacts for this stage");
                continue;
            }
            Stage stage = new Stage();
            stage.setId(stageInstance.getId());
            StageIdentifier stageIdentifier = new StageIdentifier(stageInstance.getPipelineName(), stageInstance.getPipelineCounter(), stageInstance.getStageName(), stageInstance.getStageCounter());
            stage.setIdentifier(stageIdentifier);

            if (!stageInstance.getIncludePaths().isEmpty()) {
                artifactsService.purgeArtifactsForStage(stage, stageInstance.getIncludePaths());
            } else if (!stageInstance.getExcludePaths().isEmpty()) {
                artifactsService.purgeArtifactsForStageExcept(stage, stageInstance.getExcludePaths());
            } else {
                artifactsService.purgeArtifactsForStage(stage);
            }

            numberOfStagesPurged++;
        }
        return numberOfStagesPurged;
    }


    private List<ArtifactExtensionStageConfiguration> map(List<StageConfigIdentifier> list) {
        ArrayList<ArtifactExtensionStageConfiguration> result = new ArrayList<ArtifactExtensionStageConfiguration>();
        for (StageConfigIdentifier stageConfigIdentifier : list) {
            result.add(new ArtifactExtensionStageConfiguration(stageConfigIdentifier.getPipelineName(), stageConfigIdentifier.getStageName()));
        }
        return result;
    }

    private PluginConfiguration stageArtifactCleanupPlugin(StageConfig stageConfig) {
        ArtifactCleanupStrategy artifactCleanupStrategy = stageConfig.getArtifactCleanupStrategy();
        if (artifactCleanupStrategy == null) return null;
        return artifactCleanupStrategy.getPluginConfiguration();
    }

    private PluginConfiguration globalArtifactCleanupPlugin() {
        ArtifactCleanupStrategy artifactCleanupStrategy = goConfigService.getCurrentConfig().server().getArtifactCleanupStrategy();
        if (artifactCleanupStrategy == null) return null;
        return artifactCleanupStrategy.getPluginConfiguration();
    }

    private void mapStageToArtifactCleanupPlugin(Map<PluginConfiguration, List<StageConfigIdentifier>> result, PluginConfiguration stageArtifactCleanupPlugin, StageConfigIdentifier stageConfigIdentifier) {
        if (!result.containsKey(stageArtifactCleanupPlugin)) {
            result.put(stageArtifactCleanupPlugin, new ArrayList<StageConfigIdentifier>());
        }
        result.get(stageArtifactCleanupPlugin).add(stageConfigIdentifier);
    }

    private List<PluginConfiguration> getStageLevelArtifactCleanupPlugins() {
        PluginConfiguration globalArtifactCleanupPlugin = globalArtifactCleanupPlugin();
        ArrayList<PluginConfiguration> result = new ArrayList<PluginConfiguration>();
        for (PluginConfiguration pluginConfiguration : buildArtifactCleanupPluginMap().keySet()) {
            if (!pluginConfiguration.equals(globalArtifactCleanupPlugin)) {
                result.add(pluginConfiguration);
            }
        }
        return result;
    }
}
