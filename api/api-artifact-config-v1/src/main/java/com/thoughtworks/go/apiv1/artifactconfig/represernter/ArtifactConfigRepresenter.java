/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv1.artifactconfig.represernter;

import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.api.representers.ErrorGetter;
import com.thoughtworks.go.api.representers.JsonReader;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.spark.Routes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl;

public class ArtifactConfigRepresenter {
    public static void toJSON(OutputWriter outputWriter, ArtifactConfig artifacts) {
        Double purgeStartDiskSpace = artifacts.getPurgeSettings().getPurgeStart().getPurgeStartDiskSpace();
        Double purgeUptoDiskSpace = artifacts.getPurgeSettings().getPurgeUpto().getPurgeUptoDiskSpace();
        ArtifactDirectory artifactsDir = artifacts.getArtifactsDir();

        outputWriter.addLinks(outputLinkWriter -> outputLinkWriter
                .addAbsoluteLink("doc", apiDocsUrl("#artifact_config"))
                .addLink("self", Routes.ArtifactConfig.BASE))
                .add("artifacts_dir", artifactsDir.getArtifactDir());

        if (purgeStartDiskSpace != null && purgeUptoDiskSpace != null) {
            outputWriter.addChild("purge_settings", purgeSettingWriter -> PurgeSettingsRepresenter.toJSON(purgeSettingWriter, artifacts.getPurgeSettings()));

        }

        if (!artifactsDir.errors().isEmpty()) {
            Map<String, String> fieldMapping = new HashMap<>();
            fieldMapping.put("artifactsDir", "artifacts_dir");
            outputWriter.addChild("errors", errorWriter -> new ErrorGetter(fieldMapping).toJSON(errorWriter, artifactsDir));
        }
    }

    public static ArtifactConfig fromJSON(JsonReader jsonReader) {
        ArtifactConfig updatedArtifactConfig = new ArtifactConfig();

        ArtifactDirectory artifactsDir = updatedArtifactConfig.getArtifactsDir();
        jsonReader.readStringIfPresent("artifacts_dir", artifactsDir::setArtifactDir);

        Optional<JsonReader> purge_settings = jsonReader.optJsonObject("purge_settings");
        purge_settings.ifPresent(filterReader -> {
            PurgeStart purgeStart = updatedArtifactConfig.getPurgeSettings().getPurgeStart();
            PurgeUpto purgeUpto = updatedArtifactConfig.getPurgeSettings().getPurgeUpto();
            filterReader.readStringIfPresent("purge_start_disk_space", purgeStart::setPurgeStartDiskSpace);
            filterReader.readStringIfPresent("purge_upto_disk_space", purgeUpto::setPurgeUptoDiskSpace);
        });

        return updatedArtifactConfig;
    }
}
