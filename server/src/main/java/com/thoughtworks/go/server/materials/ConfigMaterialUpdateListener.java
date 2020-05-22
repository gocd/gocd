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

import com.thoughtworks.go.config.GoConfigRepoConfigDataSource;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
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

    private final GoConfigRepoConfigDataSource repoConfigDataSource;
    private final MaterialRepository materialRepository;
    private final MaterialUpdateCompletedTopic topic;
    private final MaterialService materialService;
    private final SubprocessExecutionContext subprocessExecutionContext;

    public ConfigMaterialUpdateListener(GoConfigRepoConfigDataSource repoConfigDataSource,
                                        MaterialRepository materialRepository,
                                        MaterialUpdateCompletedTopic topic,
                                        MaterialService materialService,
                                        SubprocessExecutionContext subprocessExecutionContext) {
        this.repoConfigDataSource = repoConfigDataSource;
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

                // Previously, we only forced a parse when the repo had never been parsed before and if a new revision
                // was pushed to the material; if no new revisions existed, we did a no-op.
                //
                // Now, we need to reparse every time to support ref/branch scanning; even if there are no changes to
                // the current material, new branches/refs could have been created that might change the output of
                // templated config repo definitions that use this feature.
                //
                // In theory, this should be inexpensive, as the working copies of materials are cached on disk. This
                // will cause more frequent `parse-directory` messages, however. Generally (crosses fingers), evaluating
                // this is fast, but we may need to consider only merging into the main config if different?
                //
                // Open to any better ideas :).
                updateConfigurationFromCheckout(folder, modification, material);
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
}
