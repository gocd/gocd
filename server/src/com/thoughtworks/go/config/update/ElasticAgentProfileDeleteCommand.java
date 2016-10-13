/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.ElasticProfileNotFoundException;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

import java.util.ArrayList;
import java.util.List;

public class ElasticAgentProfileDeleteCommand extends ElasticAgentProfileCommand {

    public ElasticAgentProfileDeleteCommand(ElasticProfile elasticProfile, GoConfigService goConfigService, Username currentUser, LocalizedOperationResult result) {
        super(elasticProfile, goConfigService, currentUser, result);
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        preprocessedElasticProfile = findExistingProfile(preprocessedConfig);

        if (preprocessedElasticProfile == null) {
            throw new ElasticProfileNotFoundException();
        }

        preprocessedConfig.server().getElasticConfig().getProfiles().remove(preprocessedElasticProfile);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        List<PipelineConfig> allPipelineConfigs = preprocessedConfig.getAllPipelineConfigs();

        List<JobConfigIdentifier> usedByPipelines = new ArrayList<>();

        for (PipelineConfig pipelineConfig : allPipelineConfigs) {
            populateDups(usedByPipelines, pipelineConfig);
        }

        if (!usedByPipelines.isEmpty()) {
            result.unprocessableEntity(LocalizedMessage.string("CANNOT_DELETE_ELASTIC_AGENT_PROFILE", elasticProfile.getId(), usedByPipelines));
            throw new GoConfigInvalidException(preprocessedConfig, String.format("The elastic agent profile '%s' is being referenced by pipeline(s): %s.", elasticProfile.getId(), usedByPipelines));
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
