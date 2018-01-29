/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.domain.JobConfigIdentifier;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PluginProfileNotFoundException;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class ArtifactStoreDeleteCommand extends ArtifactStoreCommand {

    public ArtifactStoreDeleteCommand(GoConfigService goConfigService, ArtifactStore newArtifactStore, ArtifactExtension extension, Username currentUser, LocalizedOperationResult result) {
        super(goConfigService, newArtifactStore, extension, currentUser, result);
    }

    @Override
    public void update(CruiseConfig modifiedConfig) throws Exception {
        preprocessedProfile = findExistingProfile(modifiedConfig);
        if (preprocessedProfile == null) {
            throw new PluginProfileNotFoundException();
        }
        getPluginProfiles(modifiedConfig).remove(preprocessedProfile);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        return isValidForDelete(preprocessedConfig);
    }

    private boolean isValidForDelete(CruiseConfig preprocessedConfig) {
        List<PipelineConfig> allPipelineConfigs = preprocessedConfig.getAllPipelineConfigs();
        List<Map<JobConfigIdentifier, List<PluggableArtifactConfig>>> usedByPipelines = new ArrayList<>();

        for (PipelineConfig pipelineConfig : allPipelineConfigs) {
            populateReferences(usedByPipelines, pipelineConfig);
        }

        if (!usedByPipelines.isEmpty()) {
            result.unprocessableEntity(LocalizedMessage.string("CANNOT_DELETE_RESOURCE_REFERENCED_BY_PIPELINES", getObjectDescriptor().toLowerCase(), profile.getId(), usedByPipelines));
            throw new GoConfigInvalidException(preprocessedConfig, String.format("The %s '%s' is being referenced by pipeline(s): %s.", getObjectDescriptor().toLowerCase(), profile.getId(), StringUtils.join(usedByPipelines, ", ")));
        }
        return true;
    }

    private void populateReferences(List<Map<JobConfigIdentifier, List<PluggableArtifactConfig>>> usedByPipelines, PipelineConfig pipelineConfig) {
        for (StageConfig stage : pipelineConfig) {
            JobConfigs jobs = stage.getJobs();
            for (JobConfig job : jobs) {
                final List<PluggableArtifactConfig> artifactConfigs = job.artifactConfigs().findByStoreId(profile.getId());
                if (!artifactConfigs.isEmpty()) {
                    usedByPipelines.add(Collections.singletonMap(new JobConfigIdentifier(pipelineConfig.name(), stage.name(), job.name()), artifactConfigs));
                }
            }
        }
    }
}
