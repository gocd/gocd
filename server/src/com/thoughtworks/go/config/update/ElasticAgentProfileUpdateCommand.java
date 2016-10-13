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

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.elastic.ElasticProfiles;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.ElasticProfileNotFoundException;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

public class ElasticAgentProfileUpdateCommand extends ElasticAgentProfileCommand {
    private final EntityHashingService hashingService;
    private final String md5;

    public ElasticAgentProfileUpdateCommand(GoConfigService goConfigService, EntityHashingService hashingService, ElasticProfile newProfile, String md5, LocalizedOperationResult result, Username currentUser) {
        super(newProfile, goConfigService, currentUser, result);
        this.hashingService = hashingService;
        this.md5 = md5;
    }

    @Override
    public void update(CruiseConfig preprocessedConfig) throws Exception {
        ElasticProfile existingProfile = findExistingProfile(preprocessedConfig);

        ElasticProfiles profiles = preprocessedConfig.server().getElasticConfig().getProfiles();

        profiles.set(profiles.indexOf(existingProfile), elasticProfile);
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return super.canContinue(cruiseConfig) && isRequestFresh(cruiseConfig);
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        ElasticProfile existingProfile = findExistingProfile(cruiseConfig);

        boolean freshRequest = hashingService.md5ForEntity(existingProfile).equals(md5);

        if (!freshRequest) {
            result.stale(LocalizedMessage.string("STALE_RESOURCE_CONFIG", "Elastic agent profile", existingProfile.getId()));
        }
        return freshRequest;
    }
}
