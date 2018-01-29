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

import com.thoughtworks.go.config.ArtifactStore;
import com.thoughtworks.go.config.ArtifactStores;
import com.thoughtworks.go.config.update.ArtifactStoreCreateCommand;
import com.thoughtworks.go.config.update.ArtifactStoreDeleteCommand;
import com.thoughtworks.go.config.update.ArtifactStoreUpdateCommand;
import com.thoughtworks.go.plugin.access.artifact.ArtifactExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.i18n.LocalizedMessage.string;

@Component
public class ArtifactStoreService extends PluginProfilesService<ArtifactStore> {
    private final ArtifactExtension artifactExtension;

    @Autowired
    public ArtifactStoreService(GoConfigService goConfigService, EntityHashingService hashingService, ArtifactExtension artifactExtension) {
        super(goConfigService, hashingService);
        this.artifactExtension = artifactExtension;
    }

    public ArtifactStore findArtifactStore(String storeId) {
        return goConfigService.artifactStores().find(storeId);
    }

    @Override
    public ArtifactStores getPluginProfiles() {
        return goConfigService.artifactStores();
    }

    public void create(Username currentUser, ArtifactStore newArtifactStore, LocalizedOperationResult result) {
        update(currentUser, newArtifactStore, result, new ArtifactStoreCreateCommand(goConfigService, newArtifactStore, artifactExtension, currentUser, result));
    }

    public void update(Username currentUser, String md5, ArtifactStore newArtifactStore, LocalizedOperationResult result) {
        update(currentUser, newArtifactStore, result, new ArtifactStoreUpdateCommand(goConfigService, newArtifactStore, artifactExtension, currentUser, result, hashingService, md5));
    }

    public void delete(Username currentUser, ArtifactStore newArtifactStore, LocalizedOperationResult result) {
        update(currentUser, newArtifactStore, result, new ArtifactStoreDeleteCommand(goConfigService, newArtifactStore, artifactExtension, currentUser, result));
        if (result.isSuccessful()) {
            result.setMessage(string("RESOURCE_DELETE_SUCCESSFUL", "artifactStore", newArtifactStore.getId()));
        }
    }
}
