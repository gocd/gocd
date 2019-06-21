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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.domain.JobIdentifier;
import com.thoughtworks.go.remote.work.BuildAssignment;
import com.thoughtworks.go.server.exceptions.RulesViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static com.thoughtworks.go.server.exceptions.RulesViolationException.throwCannotRefer;
import static com.thoughtworks.go.server.exceptions.RulesViolationException.throwSecretConfigNotFound;
import static java.lang.String.format;

@Service
public class RulesService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RulesService.class);
    private GoConfigService goConfigService;

    @Autowired
    public RulesService(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public boolean validateSecretConfigReferences(ScmMaterial scmMaterial) {
        List<CaseInsensitiveString> pipelines = goConfigService.pipelinesWithMaterial(scmMaterial.getFingerprint());
        SecretParams secretParams = scmMaterial.getSecretParams();

        List<String> missingSecretConfigs = secretParams.stream()
                .filter(secretParam -> goConfigService.cruiseConfig().getSecretConfigs().find(secretParam.getSecretConfigId()) == null)
                .map(SecretParam::getSecretConfigId)
                .collect(Collectors.toList());

        if (!missingSecretConfigs.isEmpty()) {
            throwSecretConfigNotFound("ScmMaterial", scmMaterial.getUriForDisplay(), String.join(", ", missingSecretConfigs));
        }

        HashMap<CaseInsensitiveString, List<String>> pipelinesWithViolatedSecretConfigIds = new HashMap<>();
        secretParams.stream()
                .map(secretParam -> goConfigService.cruiseConfig().getSecretConfigs().find(secretParam.getSecretConfigId()))
                .forEach(secretConfig -> {
                    pipelines.forEach(pipelineName -> {
                        PipelineConfigs group = goConfigService.findGroupByPipeline(pipelineName);
                        if (!secretConfig.canRefer(group.getClass(), group.getGroup())) {
                            if (!pipelinesWithViolatedSecretConfigIds.containsKey(pipelineName)) {
                                pipelinesWithViolatedSecretConfigIds.put(pipelineName, new ArrayList<>());
                            }
                            LOGGER.error("[Material Update] Failure", format("Pipeline '%s' does not have permission to refer to secrets using SecretConfig: '%s'", pipelineName.toString(), secretConfig.getId()));
                            pipelinesWithViolatedSecretConfigIds.get(pipelineName).add(secretConfig.getId());

                        }
                    });
                });

        if (pipelinesWithViolatedSecretConfigIds.size() == pipelines.size()) {
            String secretConfigId = secretParams.get(0).getSecretConfigId();
            String errorMessage = format("Material with url: '%s' does not have permissions to refer to secret config with id: '%s'. The pipelines which use this material are: '%s'", scmMaterial.getUriForDisplay(), secretConfigId, pipelinesWithViolatedSecretConfigIds.keySet().toString());
            LOGGER.error("[Material Update] Failure", errorMessage);
            throw new RulesViolationException(errorMessage);
        }
        return true;


    }

    public boolean validateSecretConfigReferences(EnvironmentConfig environmentConfig) {
        SecretParams secretParams = environmentConfig.getSecretParams();
        validateSecretConfigReferences(secretParams, environmentConfig.getClass(), environmentConfig.name().toString(), "Environment");
        return true;
    }

    public void validateSecretConfigReferences(BuildAssignment buildAssignment) {
        SecretParams secretParams = buildAssignment.getSecretParams();
        if (secretParams.isEmpty()) {
            LOGGER.debug("No secret params available in build assignment {}.", buildAssignment.getJobIdentifier());
            return;
        }

        JobIdentifier jobIdentifier = buildAssignment.getJobIdentifier();
        PipelineConfigs group = goConfigService.findGroupByPipeline(new CaseInsensitiveString(jobIdentifier.getPipelineName()));
        String errorMessagePrefix = format("Job: '%s' in Pipeline: '%s' and Pipeline Group:", jobIdentifier.getBuildName(), jobIdentifier.getPipelineName());
        validateSecretConfigReferences(secretParams, group.getClass(), group.getGroup(), errorMessagePrefix);
    }

    private void validateSecretConfigReferences(SecretParams secretParams, Class<? extends Validatable> entityClass, String entityName, String entityNameOrErrorMessagePrefix) {
        secretParams.forEach(secretParam -> {
            SecretConfig secretConfig = goConfigService.cruiseConfig().getSecretConfigs().find(secretParam.getSecretConfigId());

            if (secretConfig == null) {
                throwSecretConfigNotFound(entityNameOrErrorMessagePrefix, entityName, secretParam.getSecretConfigId());
            }

            if (!secretConfig.canRefer(entityClass, entityName)) {
                throwCannotRefer(entityNameOrErrorMessagePrefix, entityName, secretParam.getSecretConfigId());
            }
        });
    }
}
