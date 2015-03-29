package com.thoughtworks.go.plugin.access.artifactcleanup;

import java.util.List;

public interface JsonMessageHandler {

    String requestGetStageInstancesForArtifactCleanup(List<ArtifactExtensionStageConfiguration> stageConfigurations);

    List<ArtifactExtensionStageInstance> responseGetStageInstancesForArtifactCleanup(String responseBody);

}

