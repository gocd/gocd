/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.MaterialService;
import com.thoughtworks.go.server.service.materials.MaterialPoller;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;

import static java.lang.String.format;

/**
 * Updates configuration from repositories.
 */
@Service
public class ConfigMaterialUpdater implements GoMessageListener<MaterialUpdateCompletedMessage>
{
    private static final Logger LOGGER = Logger.getLogger(ConfigMaterialUpdater.class);

    private GoRepoConfigDataSource repoConfigDataSource;
    private MaterialRepository materialRepository;
    private MaterialChecker materialChecker;
    private ConfigMaterialUpdateCompletedTopic configCompleted;
    private MaterialUpdateCompletedTopic topic;
    private MaterialService materialService;
    private SubprocessExecutionContext subprocessExecutionContext;

    @Autowired
    public ConfigMaterialUpdater(GoRepoConfigDataSource repoConfigDataSource,
                                 MaterialRepository materialRepository,
                                 MaterialChecker materialChecker,
                                 ConfigMaterialUpdateCompletedTopic configCompletedTopic,
                                 MaterialUpdateCompletedTopic topic,
                                 MaterialService materialService,
                                 SubprocessExecutionContext subprocessExecutionContext)
    {
        this.repoConfigDataSource = repoConfigDataSource;
        this.materialChecker = materialChecker;
        this.materialRepository = materialRepository;
        this.configCompleted = configCompletedTopic;
        this.topic = topic;
        this.materialService = materialService;
        this.subprocessExecutionContext = subprocessExecutionContext;

        this.configCompleted.addListener(this);
    }

    @Override
    public void onMessage(MaterialUpdateCompletedMessage message) {
        Material material = message.getMaterial();
        //MDU is done using the checkout, it has done db update and stored latest changes
        // but MUS is still waiting for material updated message on MaterialUpdateCompletedTopic
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format("[Config Material Update] Config material update completed for material %s. Starting parse process", material));
        }
        try {
            if(message instanceof MaterialUpdateFailedMessage)
            {
                MaterialUpdateFailedMessage failure = (MaterialUpdateFailedMessage)message;
                LOGGER.warn(String.format("[Config Material Update] Cannot update configuration part because material update has failed. Reason: %s",
                        failure.getReason()));
            }
            else {
                File folder = materialRepository.folderFor(material);
                MaterialRevisions latestModification = materialRepository.findLatestModification(material);
                Revision revision = latestModification.firstModifiedMaterialRevision().getRevision();

                MaterialRevision lastParseRevision = getMaterialRevisionAtLastParseAttempt(message);
                if (lastParseRevision == null) {
                    //never parsed
                    updateConfigurationFromCheckout(folder, revision, material);
                } else if (latestModification.findRevisionFor(material.config())
                        .hasChangedSince(lastParseRevision)) {
                    // revision has changed. the config files might have been updated
                    updateConfigurationFromCheckout(folder, revision, material);
                } else {
                    // revision is the same as last time, no need to parse again
                }
            }
        }
        finally {
            // always post the original message further
            // this will remove material from inProgress in MUS
            topic.post(message);
        }
    }

    private void updateConfigurationFromCheckout(File folder, Revision revision, Material material) {
        MaterialPoller poller = this.materialService.getPollerImplementation(material);
        poller.checkout(material,folder, revision,this.subprocessExecutionContext);
        this.repoConfigDataSource.onCheckoutComplete(material.config(),folder, revision.getRevision());
    }

    private MaterialRevision getMaterialRevisionAtLastParseAttempt(MaterialUpdateCompletedMessage message) {
        MaterialRevision lastParseRevision;
        try {
            String materialRevisionAtLastAttempt = repoConfigDataSource.getRevisionAtLastAttempt(message.getMaterial().config());
            if(materialRevisionAtLastAttempt == null)
                return null;
            lastParseRevision = materialChecker.findSpecificRevision(message.getMaterial(),
                    materialRevisionAtLastAttempt);
        } catch (Exception ex) {
            LOGGER.error(String.format("[Config Material Update] failed to get last parsed material revision. Reason: %s",
                    ex.getMessage()));
            lastParseRevision = null;
        }
        return lastParseRevision;
    }
}
