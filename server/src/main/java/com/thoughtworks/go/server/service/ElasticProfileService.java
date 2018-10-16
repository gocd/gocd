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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.PluginProfiles;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.update.ElasticAgentProfileCreateCommand;
import com.thoughtworks.go.config.update.ElasticAgentProfileDeleteCommand;
import com.thoughtworks.go.config.update.ElasticAgentProfileUpdateCommand;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ElasticProfileService extends PluginProfilesService<ElasticProfile> {
    private final ElasticAgentExtension elasticAgentExtension;

    @Autowired
    public ElasticProfileService(GoConfigService goConfigService, EntityHashingService hashingService, ElasticAgentExtension elasticAgentExtension) {
        super(goConfigService, hashingService);
        this.elasticAgentExtension = elasticAgentExtension;
    }

    @Override
    public PluginProfiles<ElasticProfile> getPluginProfiles() {
        return goConfigService.getElasticConfig().getProfiles();
    }

    public void update(Username currentUser, String md5, ElasticProfile newProfile, LocalizedOperationResult result) {
        ElasticAgentProfileUpdateCommand command = new ElasticAgentProfileUpdateCommand(goConfigService, newProfile, elasticAgentExtension, currentUser, result, hashingService, md5);
        update(currentUser, newProfile, result, command);
    }

    public void delete(Username currentUser, ElasticProfile elasticProfile, LocalizedOperationResult result) {
        update(currentUser, elasticProfile, result, new ElasticAgentProfileDeleteCommand(goConfigService, elasticProfile, elasticAgentExtension, currentUser, result));
        if (result.isSuccessful()) {
            result.setMessage(LocalizedMessage.resourceDeleteSuccessful("elastic agent profile", elasticProfile.getId()));
        }
    }

    public void create(Username currentUser, ElasticProfile elasticProfile, LocalizedOperationResult result) {
        ElasticAgentProfileCreateCommand command = new ElasticAgentProfileCreateCommand(goConfigService, elasticProfile, elasticAgentExtension, currentUser, result);
        update(currentUser, elasticProfile, result, command);
    }
}
