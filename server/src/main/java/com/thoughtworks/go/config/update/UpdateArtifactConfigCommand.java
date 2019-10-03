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

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.ErrorCollector;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.domain.ConfigErrors;

import java.util.List;

public class UpdateArtifactConfigCommand implements EntityConfigUpdateCommand<ArtifactConfig> {
    private ArtifactConfig artifactConfig;
    private ArtifactConfig preprocessedArtifactConfig;

    public UpdateArtifactConfigCommand(ArtifactConfig artifactConfig) {
        this.artifactConfig = artifactConfig;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        ArtifactDirectory artifactsDir = artifactConfig.getArtifactsDir();
        PurgeSettings purgeSettings = artifactConfig.getPurgeSettings();

        ArtifactConfig artifactConfig = new ArtifactConfig();
        artifactConfig.setArtifactsDir(artifactsDir);
        artifactConfig.setPurgeSettings(purgeSettings);

        preprocessedConfig.server().setArtifactConfig(artifactConfig);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        preprocessedArtifactConfig = preprocessedConfig.server().getArtifactConfig();
        preprocessedArtifactConfig.validate(new ConfigSaveValidationContext(preprocessedConfig));

        BasicCruiseConfig.copyErrors(preprocessedArtifactConfig, artifactConfig);
        List<ConfigErrors> allErrors = ErrorCollector.getAllErrors(artifactConfig);

        if (!allErrors.isEmpty()) {
            throw new GoConfigInvalidException(preprocessedConfig, allErrors);
        }

        return true;
    }

    @Override
    public void clearErrors() {

    }

    @Override
    public ArtifactConfig getPreprocessedEntityConfig() {
        return preprocessedArtifactConfig;
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return true;
    }

    protected EntityType getObjectDescriptor() {
        return EntityType.ArtifactConfig;
    }
}
