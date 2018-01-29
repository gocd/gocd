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

import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.ArtifactStores;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.EntityHashingService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;

public class ArtifactStoreUpdateCommand extends ArtifactStoreCommand {
    private final EntityHashingService hashingService;
    private final String md5;

    public ArtifactStoreUpdateCommand(GoConfigService goConfigService, ArtifactStore newArtifactStore, ArtifactExtension extension, Username currentUser, LocalizedOperationResult result, EntityHashingService hashingService, String md5) {
        super(goConfigService, newArtifactStore, extension, currentUser, result);
        this.hashingService = hashingService;
        this.md5 = md5;
    }

    @Override
    public void update(CruiseConfig modifiedConfig) throws Exception {
        ArtifactStore existingProfile = findExistingProfile(modifiedConfig);
        ArtifactStores profiles = getPluginProfiles(modifiedConfig);
        profiles.set(profiles.indexOf(existingProfile), profile);
    }

    @Override
    public boolean isValid(CruiseConfig preprocessedConfig) {
        return isValidForCreateOrUpdate(preprocessedConfig);
    }

    @Override
    public boolean canContinue(CruiseConfig cruiseConfig) {
        return super.canContinue(cruiseConfig) && isRequestFresh(cruiseConfig);
    }

    private boolean isRequestFresh(CruiseConfig cruiseConfig) {
        ArtifactStore existingProfile = findExistingProfile(cruiseConfig);
        boolean freshRequest = hashingService.md5ForEntity(existingProfile).equals(md5);
        if (!freshRequest) {
            result.stale(LocalizedMessage.string("STALE_RESOURCE_CONFIG", getObjectDescriptor(), existingProfile.getId()));
        }
        return freshRequest;
    }
}
