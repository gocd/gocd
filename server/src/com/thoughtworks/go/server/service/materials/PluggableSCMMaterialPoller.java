/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.service.materials;

import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.scm.PluggableSCMMaterialRevision;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.access.scm.SCMProperty;
import com.thoughtworks.go.plugin.access.scm.SCMPropertyConfiguration;
import com.thoughtworks.go.plugin.access.scm.material.MaterialPollResult;
import com.thoughtworks.go.plugin.access.scm.revision.ModifiedAction;
import com.thoughtworks.go.plugin.access.scm.revision.ModifiedFile;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.api.response.Result;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.json.JsonHelper;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.io.File;
import java.util.List;
import java.util.Map;

public class PluggableSCMMaterialPoller implements MaterialPoller<PluggableSCMMaterial> {
    private MaterialRepository materialRepository;
    private SCMExtension scmExtension;
    private TransactionTemplate transactionTemplate;

    public PluggableSCMMaterialPoller(MaterialRepository materialRepository, SCMExtension scmExtension, TransactionTemplate transactionTemplate) {
        this.materialRepository = materialRepository;
        this.scmExtension = scmExtension;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public List<Modification> latestModification(final PluggableSCMMaterial material, File baseDir, SubprocessExecutionContext execCtx) {
        SCMPropertyConfiguration scmPropertyConfiguration = buildSCMPropertyConfigurations(material.getScmConfig());
        final MaterialInstance materialInstance = materialRepository.findMaterialInstance(material);
        MaterialPollResult pollResult = scmExtension.getLatestRevision(material.getPluginId(), scmPropertyConfiguration, materialInstance.getAdditionalDataMap(), baseDir.getAbsolutePath());

        final Map<String, String> materialData = pollResult.getMaterialData();
        if (materialInstance.requiresUpdate(materialData)) {
            updateAdditionalData(materialInstance.getId(), materialData);
        }
        SCMRevision scmRevision = pollResult.getLatestRevision();
        return scmRevision == null ? new Modifications() : new Modifications(getModification(scmRevision));
    }

    @Override
    public List<Modification> modificationsSince(final PluggableSCMMaterial material, File baseDir, final Revision revision, SubprocessExecutionContext execCtx) {
        SCMPropertyConfiguration scmPropertyConfiguration = buildSCMPropertyConfigurations(material.getScmConfig());
        MaterialInstance materialInstance = materialRepository.findMaterialInstance(material);
        PluggableSCMMaterialRevision pluggableSCMMaterialRevision = (PluggableSCMMaterialRevision) revision;
        SCMRevision previouslyKnownRevision = new SCMRevision(pluggableSCMMaterialRevision.getRevision(), pluggableSCMMaterialRevision.getTimestamp(), null, null, pluggableSCMMaterialRevision.getData(), null);
        MaterialPollResult pollResult = scmExtension.latestModificationSince(material.getPluginId(), scmPropertyConfiguration, materialInstance.getAdditionalDataMap(), baseDir.getAbsolutePath(), previouslyKnownRevision);

        final Map<String, String> materialData = pollResult.getMaterialData();
        if (materialInstance.requiresUpdate(materialData)) {
            updateAdditionalData(materialInstance.getId(), materialData);
        }
        List<SCMRevision> scmRevisions = pollResult.getRevisions();
        return getModifications(scmRevisions);
    }

    @Override
    public void checkout(PluggableSCMMaterial material, File baseDir, Revision revision, SubprocessExecutionContext execCtx) {
        SCMPropertyConfiguration scmPropertyConfiguration = buildSCMPropertyConfigurations(material.getScmConfig());
        MaterialInstance materialInstance = materialRepository.findMaterialInstance(material);
        PluggableSCMMaterialRevision pluggableSCMMaterialRevision = (PluggableSCMMaterialRevision) revision;
        SCMRevision scmRevision = new SCMRevision(
                pluggableSCMMaterialRevision.getRevision(),
                pluggableSCMMaterialRevision.getTimestamp(), null, null,
                pluggableSCMMaterialRevision.getData(), null);
        Result result = scmExtension.checkout(material.getPluginId(), scmPropertyConfiguration, baseDir.getAbsolutePath(), scmRevision);
        if(!result.isSuccessful())
            throw new RuntimeException("Failed to perform checkout on pluggable SCM");
    }

    private SCMPropertyConfiguration buildSCMPropertyConfigurations(SCM scmConfig) {
        SCMPropertyConfiguration scmPropertyConfiguration = new SCMPropertyConfiguration();
        populateConfiguration(scmConfig.getConfiguration(), scmPropertyConfiguration);
        return scmPropertyConfiguration;
    }

    private void populateConfiguration(Configuration configuration, com.thoughtworks.go.plugin.api.config.Configuration pluginConfiguration) {
        for (ConfigurationProperty configurationProperty : configuration) {
            pluginConfiguration.add(new SCMProperty(configurationProperty.getConfigurationKey().getName(), configurationProperty.getValue()));
        }
    }

    private void updateAdditionalData(final long materialId, final Map<String, String> materialData) {
        transactionTemplate.execute(new TransactionCallback() {
            @Override
            public Object doInTransaction(TransactionStatus transactionStatus) {
                MaterialInstance materialInstance = materialRepository.find(materialId);
                String additionalData = (materialData == null || materialData.isEmpty()) ? null : JsonHelper.toJsonString(materialData);
                materialInstance.setAdditionalData(additionalData);
                materialRepository.saveOrUpdate(materialInstance);
                return materialInstance;
            }
        });
    }

    private List<Modification> getModifications(List<SCMRevision> scmRevisions) {
        Modifications modifications = new Modifications();
        if (scmRevisions == null || scmRevisions.isEmpty()) {
            return modifications;
        }
        for (SCMRevision scmRevision : scmRevisions) {
            modifications.add(getModification(scmRevision));
        }
        return modifications;
    }

    private Modification getModification(SCMRevision scmRevision) {
        String additionalData = (scmRevision.getData() == null || scmRevision.getData().isEmpty()) ? null : JsonHelper.toJsonString(scmRevision.getData());
        Modification modification = new Modification(scmRevision.getUser(), scmRevision.getRevisionComment(), null,
                scmRevision.getTimestamp(), scmRevision.getRevision(), additionalData);
        if (scmRevision.getModifiedFiles() != null && !scmRevision.getModifiedFiles().isEmpty()) {
            for (ModifiedFile modifiedFile : scmRevision.getModifiedFiles()) {
                modification.createModifiedFile(modifiedFile.getFileName(), null, convertAction(modifiedFile.getAction()));
            }
        }
        return modification;
    }

    private com.thoughtworks.go.domain.materials.ModifiedAction convertAction(ModifiedAction modifiedFile) {
        if (modifiedFile == ModifiedAction.added) {
            return com.thoughtworks.go.domain.materials.ModifiedAction.added;
        } else if (modifiedFile == ModifiedAction.modified) {
            return com.thoughtworks.go.domain.materials.ModifiedAction.modified;
        } else if (modifiedFile == ModifiedAction.deleted) {
            return com.thoughtworks.go.domain.materials.ModifiedAction.deleted;
        }
        return com.thoughtworks.go.domain.materials.ModifiedAction.unknown;
    }
}
