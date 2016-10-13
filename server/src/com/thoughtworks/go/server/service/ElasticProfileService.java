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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.ConfigTag;
import com.thoughtworks.go.config.commands.EntityConfigUpdateCommand;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.elastic.ElasticProfiles;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.update.ElasticAgentProfileCreateCommand;
import com.thoughtworks.go.config.update.ElasticAgentProfileDeleteCommand;
import com.thoughtworks.go.config.update.ElasticAgentProfileUpdateCommand;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class ElasticProfileService {
    private org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ElasticProfileService.class);

    private final GoConfigService goConfigService;
    private final EntityHashingService hashingService;

    @Autowired
    public ElasticProfileService(GoConfigService goConfigService, EntityHashingService hashingService) {
        this.goConfigService = goConfigService;
        this.hashingService = hashingService;
    }

    public ElasticProfile findProfile(String id) {
        ElasticProfiles profiles = goConfigService.serverConfig().getElasticConfig().getProfiles();
        return profiles.find(id);
    }

    public Map<String, ElasticProfile> allProfiles() {
        ElasticProfiles profiles = goConfigService.serverConfig().getElasticConfig().getProfiles();

        Map<String, ElasticProfile> result = new LinkedHashMap<>();
        for (ElasticProfile profile : profiles) {
            result.put(profile.getId(), profile);
        }

        return result;
    }

    public void update(Username currentUser, String md5, ElasticProfile newProfile, LocalizedOperationResult result) {
        update(currentUser, newProfile, result, new ElasticAgentProfileUpdateCommand(goConfigService, hashingService, newProfile, md5, result, currentUser));
    }

    public void delete(Username currentUser, ElasticProfile elasticProfile, LocalizedOperationResult result) {
        update(currentUser, elasticProfile, result, new ElasticAgentProfileDeleteCommand(elasticProfile, goConfigService, currentUser, result));
        if (result.isSuccessful()) {
            result.setMessage(LocalizedMessage.string("RESOURCE_DELETE_SUCCESSFUL", "elastic agent profile", elasticProfile.getId()));
        }
    }

    public void create(Username currentUser, ElasticProfile elasticProfile, LocalizedOperationResult result) {
        update(currentUser, elasticProfile, result, new ElasticAgentProfileCreateCommand(elasticProfile, goConfigService, currentUser, result));
    }

    private void update(Username currentUser, ElasticProfile elasticProfile, LocalizedOperationResult result, EntityConfigUpdateCommand<ElasticProfile> command) {
        try {
            goConfigService.updateConfig(command, currentUser);
        } catch (Exception e) {
            if (e instanceof GoConfigInvalidException) {
                result.unprocessableEntity(LocalizedMessage.string("ENTITY_CONFIG_VALIDATION_FAILED", elasticProfile.getClass().getAnnotation(ConfigTag.class).value(), elasticProfile.getId(), e.getMessage()));
            } else {
                if (!result.hasMessage()) {
                    LOGGER.error(e.getMessage(), e);
                    result.internalServerError(LocalizedMessage.string("SAVE_FAILED_WITH_REASON", "An error occurred while saving the profile config. Please check the logs for more information."));
                }
            }
        }
    }

}
