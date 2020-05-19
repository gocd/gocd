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
package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.elastic.ElasticProfiles;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

public class ElasticAgentProfileUpdateCommand extends ElasticAgentProfileCommand {
    private final EntityHashingService hashingService;
    private final String digest;

    public ElasticAgentProfileUpdateCommand(GoConfigService goConfigService, ElasticProfile newProfile, ElasticAgentExtension extension, Username currentUser, LocalizedOperationResult result, EntityHashingService hashingService, String digest) {
        super(goConfigService, newProfile, extension, currentUser, result);
        this.hashingService = hashingService;
        this.digest = digest;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) {
        ElasticProfile existingProfile = findExistingProfile(preprocessedConfig);
        ElasticProfiles profiles = getPluginProfiles(preprocessedConfig);
        profiles.set(profiles.indexOf(existingProfile), elasticProfile);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        return isValidForCreateOrUpdate(preprocessedConfig);
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return super.canContinue(cruiseConfig) && isRequestFresh(cruiseConfig);
    }

    @Override
    public void encrypt(CruiseConfig preProcessedConfig) {
        this.elasticProfile.encryptSecureProperties(preProcessedConfig);
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        ElasticProfile existingProfile = findExistingProfile(cruiseConfig);
        boolean freshRequest = hashingService.hashForEntity(existingProfile).equals(digest);
        if (!freshRequest) {
            result.stale(getObjectDescriptor().staleConfig(existingProfile.getId()));
        }
        return freshRequest;
    }
}
