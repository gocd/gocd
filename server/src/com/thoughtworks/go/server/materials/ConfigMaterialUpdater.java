package com.thoughtworks.go.server.materials;

import com.thoughtworks.go.config.GoRepoConfigDataSource;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.server.persistence.MaterialRepository;
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

    @Autowired
    public ConfigMaterialUpdater(GoRepoConfigDataSource repoConfigDataSource,
                                 MaterialRepository materialRepository,
                                 MaterialChecker materialChecker,
                                 ConfigMaterialUpdateCompletedTopic configCompletedTopic,
                                 MaterialUpdateCompletedTopic topic)
    {
        this.repoConfigDataSource = repoConfigDataSource;
        this.materialChecker = materialChecker;
        this.materialRepository = materialRepository;
        this.configCompleted = configCompletedTopic;
        this.topic = topic;
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
            File folder = materialRepository.folderFor(material);
            MaterialRevisions latestModification = materialRepository.findLatestModification(material);
            String revision = latestModification.latestRevision();

            MaterialRevision lastParseRevision = getMaterialRevisionAtLastParseAttempt(message);
            if (lastParseRevision == null) {
                //never parsed
                UpdateConfigurationFromCheckout(folder, revision, material);
            } else if (latestModification.findRevisionFor(material.config())
                    .hasChangedSince(lastParseRevision)) {
                // revision has changed. the config files might have been updated
                UpdateConfigurationFromCheckout(folder, revision, material);
            } else {
                // revision is the same as last time, no need to parse again
            }
        }
        finally {
            // always post the original message further
            // this will remove material from inProgress in MUS
            topic.post(message);
        }
    }

    private void UpdateConfigurationFromCheckout(File folder, String revision, Material material) {
        this.repoConfigDataSource.onCheckoutComplete(material.config(),folder, revision);
    }

    private MaterialRevision getMaterialRevisionAtLastParseAttempt(MaterialUpdateCompletedMessage message) {
        MaterialRevision lastParseRevision;
        try {
            String materialRevisionAtLastAttempt = repoConfigDataSource.getRevisionAtLastAttempt(message.getMaterial().config());
            if(materialRevisionAtLastAttempt == null)
                return null;
            lastParseRevision = materialChecker.findSpecificRevision(message.getMaterial(),
                    materialRevisionAtLastAttempt);
        }
        catch (Exception ex)
        {
            LOGGER.error(String.format("[Config Material Update] failed to get last parsed material revision. Reason: ",
                    ex.getMessage()));
            lastParseRevision = null;
        }
        return lastParseRevision;
    }
}
