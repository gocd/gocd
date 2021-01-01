/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.domain.JobConfigIdentifier;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.i18n.LocalizedMessage.cannotDeleteResourceBecauseOfDependentPipelines;

public class ElasticAgentProfileDeleteCommand extends ElasticAgentProfileCommand {

    public ElasticAgentProfileDeleteCommand(GoConfigService goConfigService, ElasticProfile elasticProfile, ElasticAgentExtension extension, Username currentUser, LocalizedOperationResult result) {
        super(goConfigService, elasticProfile, extension, currentUser, result);
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) {
        preprocessedProfile = findExistingProfile(preprocessedConfig);
        getPluginProfiles(preprocessedConfig).remove(preprocessedProfile);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        return isValidForDelete(preprocessedConfig);
    }


    private boolean isValidForDelete(CruiseConfig preprocessedConfig) {
        List<PipelineConfig> allPipelineConfigs = preprocessedConfig.getAllPipelineConfigs();
        List<JobConfigIdentifier> usedByPipelines = new ArrayList<>();

        for (PipelineConfig pipelineConfig : allPipelineConfigs) {
            populateDups(usedByPipelines, pipelineConfig);
        }

        List<String> pipelineNames = new ArrayList<>();
        for (JobConfigIdentifier usedByPipeline : usedByPipelines) {
            String toString = usedByPipeline.toString();
            pipelineNames.add(toString);
        }

        if (!usedByPipelines.isEmpty()) {
            result.unprocessableEntity(cannotDeleteResourceBecauseOfDependentPipelines(getObjectDescriptor().getEntityNameLowerCase(), elasticProfile.getId(), pipelineNames));
            throw new GoConfigInvalidException(preprocessedConfig, String.format("The %s '%s' is being referenced by pipeline(s): %s.", getObjectDescriptor().getEntityNameLowerCase(), elasticProfile.getId(), StringUtils.join(pipelineNames, ", ")));
        }
        return true;
    }

    private void populateDups(List<JobConfigIdentifier> usedByPipelines, PipelineConfig pipelineConfig) {
        for (StageConfig stage : pipelineConfig) {
            JobConfigs jobs = stage.getJobs();
            for (JobConfig job : jobs) {
                String id = elasticProfile.getId();
                if (id.equals(job.getElasticProfileId())) {
                    usedByPipelines.add(new JobConfigIdentifier(pipelineConfig.name(), stage.name(), job.name()));
                }
            }
        }
    }

}
