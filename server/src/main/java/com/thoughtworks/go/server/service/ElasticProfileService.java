/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.elastic.ElasticProfiles;
import com.thoughtworks.go.config.exceptions.EntityType;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.update.ElasticAgentProfileCreateCommand;
import com.thoughtworks.go.config.update.ElasticAgentProfileDeleteCommand;
import com.thoughtworks.go.config.update.ElasticAgentProfileUpdateCommand;
import com.thoughtworks.go.domain.ElasticProfileUsage;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.plugin.access.exceptions.SecretResolutionFailureException;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.RulesViolationException;
import com.thoughtworks.go.server.service.plugins.validators.elastic.ElasticAgentProfileConfigurationValidator;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static com.thoughtworks.go.i18n.LocalizedMessage.entityConfigValidationFailed;
import static com.thoughtworks.go.i18n.LocalizedMessage.saveFailedWithReason;

@Component
public class ElasticProfileService {
    protected Logger LOGGER = LoggerFactory.getLogger(getClass());

    private final GoConfigService goConfigService;
    private final EntityHashingService hashingService;
    private final ElasticAgentExtension elasticAgentExtension;
    private ElasticAgentProfileConfigurationValidator profileConfigurationValidator;
    private final SecretParamResolver secretParamResolver;

    @Autowired
    public ElasticProfileService(GoConfigService goConfigService, EntityHashingService hashingService, ElasticAgentExtension elasticAgentExtension, SecretParamResolver secretParamResolver) {
        this.goConfigService = goConfigService;
        this.hashingService = hashingService;
        this.elasticAgentExtension = elasticAgentExtension;
        this.profileConfigurationValidator = new ElasticAgentProfileConfigurationValidator(elasticAgentExtension);
        this.secretParamResolver = secretParamResolver;
    }

    public ElasticProfiles getPluginProfiles() {
        return goConfigService.getConfigForEditing().getElasticConfig().getProfiles();
    }

    public void update(Username currentUser, String md5, ElasticProfile newProfile, LocalizedOperationResult result) {
        ElasticAgentProfileUpdateCommand command = new ElasticAgentProfileUpdateCommand(goConfigService, newProfile, elasticAgentExtension, currentUser, result, hashingService, md5);
        update(command, newProfile, currentUser, result, true);
    }

    public void delete(Username currentUser, ElasticProfile elasticProfile, LocalizedOperationResult result) {
        update(new ElasticAgentProfileDeleteCommand(goConfigService, elasticProfile, elasticAgentExtension, currentUser, result), elasticProfile, currentUser, result, false);
        if (result.isSuccessful()) {
            result.setMessage(EntityType.ElasticProfile.deleteSuccessful(elasticProfile.getId()));
        }
    }

    public void create(Username currentUser, ElasticProfile elasticProfile, LocalizedOperationResult result) {
        ElasticAgentProfileCreateCommand command = new ElasticAgentProfileCreateCommand(goConfigService, elasticProfile, elasticAgentExtension, currentUser, result);
        update(command, elasticProfile, currentUser, result, true);
    }

    public Collection<ElasticProfileUsage> getUsageInformation(String profileId) {
        if (findProfile(profileId) == null) {
            throw new RecordNotFoundException(EntityType.ElasticProfile, profileId);
        }

        final List<PipelineConfig> allPipelineConfigs = goConfigService.getAllPipelineConfigs();
        final Set<ElasticProfileUsage> jobsUsingElasticProfile = new HashSet<>();

        for (PipelineConfig pipelineConfig : allPipelineConfigs) {
            final PipelineConfig stages = pipelineConfig.getStages();

            for (StageConfig stage : stages) {
                final JobConfigs jobs = stage.getJobs();

                for (JobConfig job : jobs) {
                    if (StringUtils.equals(profileId, job.getElasticProfileId())) {

                        String templateName = null;
                        if (pipelineConfig.getTemplateName() != null) {
                            templateName = pipelineConfig.getTemplateName().toString();
                        }

                        String origin = pipelineConfig.getOrigin() instanceof FileConfigOrigin ? "gocd" : "config_repo";

                        jobsUsingElasticProfile.add(new ElasticProfileUsage(pipelineConfig.getName().toString(),
                                stage.name().toString(),
                                job.name().toString(),
                                templateName,
                                origin));
                    }
                }
            }
        }

        return jobsUsingElasticProfile;
    }

    public ElasticProfile findProfile(String profileId) {
        for (ElasticProfile profile : getPluginProfiles()) {
            if (profile.getId().equals(profileId)) {
                return profile;
            }
        }

        return null;
    }

    public Map<String, ElasticProfile> listAll() {
        return getPluginProfiles().stream()
                .collect(Collectors.toMap(ElasticProfile::getId, elasticAgentProfile -> elasticAgentProfile, (a, b) -> b, HashMap::new));
    }

    //used only from tests
    public void setProfileConfigurationValidator(ElasticAgentProfileConfigurationValidator profileConfigurationValidator) {
        this.profileConfigurationValidator = profileConfigurationValidator;
    }

    public List<ElasticProfile> findElasticAgentProfilesByPluginId(String pluginId) {
        ClusterProfiles allClusterProfiles = goConfigService.getElasticConfig().getClusterProfiles();

        return getPluginProfiles().stream()
                .filter(profile -> allClusterProfiles.find(profile.getClusterProfileId()).getPluginId().equals(pluginId))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    private void validatePluginProfileMetadata(ElasticProfile elasticProfile) {
        String pluginId = getPluginIdForElasticAgentProfile(elasticProfile);

        if (pluginId == null) {
            return;
        }
        secretParamResolver.resolve(elasticProfile);
        profileConfigurationValidator.validate(elasticProfile, pluginId);
    }

    private String getPluginIdForElasticAgentProfile(ElasticProfile elasticProfile) {
        ClusterProfile clusterProfile = this.goConfigService.getElasticConfig().getClusterProfiles().find(elasticProfile.getClusterProfileId());
        return (clusterProfile != null) ? clusterProfile.getPluginId() : null;
    }

    private String getTagName(Class<?> clazz) {
        return clazz.getAnnotation(ConfigTag.class).value();
    }

    protected void update(EntityConfigUpdateCommand<ElasticProfile> command, ElasticProfile elasticProfile, Username currentUser, LocalizedOperationResult result, boolean validatePluginProperties) {
        try {
            if (validatePluginProperties) {
                validatePluginProfileMetadata(elasticProfile);
            }
            goConfigService.updateConfig(command, currentUser);
        } catch (Exception e) {
            if (result.hasMessage()) {
                LOGGER.error(e.getMessage(), e);
                return;
            }
            if (e instanceof GoConfigInvalidException) {
                result.unprocessableEntity(entityConfigValidationFailed(getTagName(elasticProfile.getClass()), elasticProfile.getId(), ((GoConfigInvalidException) e).getAllErrorMessages()));
            } else if (e instanceof RulesViolationException || e instanceof SecretResolutionFailureException) {
                result.unprocessableEntity(entityConfigValidationFailed(elasticProfile.getClass().getAnnotation(ConfigTag.class).value(), elasticProfile.getId(), e.getMessage()));
            } else {
                result.internalServerError(saveFailedWithReason("An error occurred while saving the elastic agent profile. Please check the logs for more information."));
            }
        }
    }
}
