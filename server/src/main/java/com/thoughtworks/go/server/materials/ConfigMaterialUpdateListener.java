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
package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.GoRepoConfigDataSource;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.MaterialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Updates configuration from repositories.
 */
public class ConfigMaterialUpdateListener implements GoMessageListener<MaterialUpdateCompletedMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigMaterialUpdateListener.class);

    private GoRepoConfigDataSource repoConfigDataSource;
    private MaterialRepository materialRepository;
    private MaterialChecker materialChecker;
    private MaterialUpdateCompletedTopic topic;
    private MaterialService materialService;
    private SubprocessExecutionContext subprocessExecutionContext;

    public ConfigMaterialUpdateListener(GoRepoConfigDataSource repoConfigDataSource,
                                        MaterialRepository materialRepository,
                                        MaterialChecker materialChecker,
                                        MaterialUpdateCompletedTopic topic,
                                        MaterialService materialService,
                                        SubprocessExecutionContext subprocessExecutionContext) {
        this.repoConfigDataSource = repoConfigDataSource;
        this.materialChecker = materialChecker;
        this.materialRepository = materialRepository;
        this.topic = topic;
        this.materialService = materialService;
        this.subprocessExecutionContext = subprocessExecutionContext;
    }

    @Override
    public void onMessage(MaterialUpdateCompletedMessage message) {
        Material material = message.getMaterial();
        // MDU is done using the checkout, it has done db update and stored latest changes
        // but MUS is still waiting for material updated message on MaterialUpdateCompletedTopic
        LOGGER.debug("[Config Material Update] Config material update completed for material {}. Starting parse process", material);
        try {
            if (message instanceof MaterialUpdateFailedMessage) {
                MaterialUpdateFailedMessage failure = (MaterialUpdateFailedMessage) message;
                LOGGER.warn("[Config Material Update] Cannot update configuration part because material update has failed. Reason: {}", failure.getReason());
            } else {
                File folder = materialRepository.folderFor(material);
                MaterialRevisions latestModification = materialRepository.findLatestModification(material);
                Modification modification = latestModification.firstModifiedMaterialRevision().getLatestModification();

                MaterialRevision lastParseRevision = getMaterialRevisionAtLastParseAttempt(message);
                if (lastParseRevision == null) {
                    //never parsed
                    updateConfigurationFromCheckout(folder, modification, material);
                } else if (latestModification.findRevisionFor(material.config()).hasChangedSince(lastParseRevision) ||
                        this.repoConfigDataSource.hasConfigRepoConfigChangedSinceLastUpdate(material.config())) {
                    // revision has changed. the config files might have been updated
                    updateConfigurationFromCheckout(folder, modification, material);
                } else {
                    // revision is the same as last time, no need to parse again
                    LOGGER.debug("[Config Material Update] Skipping parsing of Config material {} since material has no change since last parse.", material);
                }
            }
            LOGGER.debug("[Config Material Update] Completed parsing of Config material {}.", material);
        } catch (Exception ex) {
            LOGGER.error("[Config Material Update] Error updating config material: {} . Reason: {}", material, ex.getMessage());
        } finally {
            // always post the original message further
            // this will remove material from inProgress in MUS
            topic.post(message);
        }
    }

    private void updateConfigurationFromCheckout(File folder, Modification modification, Material material) {
        Revision revision = new StringRevision(modification.getRevision());
        this.materialService.checkout(material, folder, revision, this.subprocessExecutionContext);
        this.repoConfigDataSource.onCheckoutComplete(material.config(), folder, modification);
    }

    private MaterialRevision getMaterialRevisionAtLastParseAttempt(MaterialUpdateCompletedMessage message) {
        MaterialRevision lastParseRevision;
        try {
            String materialRevisionAtLastAttempt = repoConfigDataSource.getRevisionAtLastAttempt(message.getMaterial().config());
            if (materialRevisionAtLastAttempt == null)
                return null;
            lastParseRevision = materialChecker.findSpecificRevision(message.getMaterial(),
                    materialRevisionAtLastAttempt);
        } catch (Exception ex) {
            LOGGER.error("[Config Material Update] failed to get last parsed material revision. Reason: {}", ex.getMessage());
            lastParseRevision = null;
        }
        return lastParseRevision;
    }
}
