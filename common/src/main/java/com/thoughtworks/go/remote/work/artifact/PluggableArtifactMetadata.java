/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.remote.work.artifact;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.thoughtworks.go.util.GoConstants.PRODUCT_NAME;
import static java.lang.String.format;

public class PluggableArtifactMetadata {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluggableArtifactMetadata.class);
    private final Map<String, Map<String, Map<String, Object>>> metadataPerPlugin = new HashMap<>();

    public void addMetadata(String pluginId, String artifactId, Map<String, Object> map) {
        metadataPerPlugin
            .computeIfAbsent(pluginId, k -> new HashMap<>())
            .put(artifactId, map);
    }

    public Map<String, Map<String, Map<String, Object>>> getMetadataPerPlugin() {
        return metadataPerPlugin;
    }

    public boolean isEmpty() {
        return metadataPerPlugin.isEmpty();
    }

    public File write(File workingDirectory) {
        final File pluggableArtifactMetadataFolder = new File(workingDirectory, UUID.randomUUID().toString());

        if (!pluggableArtifactMetadataFolder.mkdirs()) {
            throw new RuntimeException(format("[%s] Could not create pluggable artifact metadata folder `%s`.", PRODUCT_NAME, pluggableArtifactMetadataFolder.getName()));
        }

        for (Map.Entry<String, Map<String, Map<String, Object>>> entry : this.getMetadataPerPlugin().entrySet()) {
            writeMetadataFile(pluggableArtifactMetadataFolder, entry.getKey(), entry.getValue());
        }

        return pluggableArtifactMetadataFolder;
    }

    private void writeMetadataFile(File pluggableArtifactMetadataFolder, String pluginId, Map<String, Map<String, Object>> responseMetadata) {
        if (responseMetadata == null || responseMetadata.isEmpty()) {
            LOGGER.info(String.format("No metadata to write for plugin `%s`.", pluginId));
            return;
        }

        try {
            LOGGER.info(String.format("Writing metadata file for plugin `%s`.", pluginId));
            Files.writeString(new File(pluggableArtifactMetadataFolder, format("%s.json", pluginId)).toPath(), new Gson().toJson(responseMetadata), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
