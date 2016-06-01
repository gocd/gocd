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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.config.materials.tfs.TfsMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.materials.*;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @understands interactions between material-config, repository and modifications
 */
@Service
public class MaterialService {
    private final MaterialRepository materialRepository;
    private final GoConfigService goConfigService;
    private final SecurityService securityService;
    private PackageAsRepositoryExtension packageAsRepositoryExtension;
    private SCMExtension scmExtension;
    private TransactionTemplate transactionTemplate;
    private Map<Class, MaterialPoller> materialPollerMap = new HashMap<>();

    @Autowired
    public MaterialService(MaterialRepository materialRepository, GoConfigService goConfigService, SecurityService securityService,
                           PackageAsRepositoryExtension packageAsRepositoryExtension, SCMExtension scmExtension, TransactionTemplate transactionTemplate) {
        this.materialRepository = materialRepository;
        this.goConfigService = goConfigService;
        this.securityService = securityService;
        this.packageAsRepositoryExtension = packageAsRepositoryExtension;
        this.scmExtension = scmExtension;
        this.transactionTemplate = transactionTemplate;
        populatePollerImplementations();
    }

    private void populatePollerImplementations() {
        materialPollerMap.put(GitMaterial.class, new GitPoller());
        materialPollerMap.put(HgMaterial.class, new HgPoller());
        materialPollerMap.put(SvnMaterial.class, new SvnPoller());
        materialPollerMap.put(TfsMaterial.class, new TfsPoller());
        materialPollerMap.put(P4Material.class, new P4Poller());
        materialPollerMap.put(DependencyMaterial.class, new DependencyMaterialPoller());
        materialPollerMap.put(PackageMaterial.class, new PackageMaterialPoller(packageAsRepositoryExtension));
        materialPollerMap.put(PluggableSCMMaterial.class, new PluggableSCMMaterialPoller(materialRepository, scmExtension, transactionTemplate));
    }

    public boolean hasModificationFor(Material material) {
        return !materialRepository.findLatestModification(material).isEmpty();
    }

    public List<MatchedRevision> searchRevisions(String pipelineName, String fingerprint, String searchString, Username username, LocalizedOperationResult result) {
        if (!securityService.hasViewPermissionForPipeline(username, pipelineName)) {
            result.unauthorized(LocalizedMessage.cannotViewPipeline(pipelineName), HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
            return new ArrayList<>();
        }
        try {
            MaterialConfig materialConfig = goConfigService.materialForPipelineWithFingerprint(pipelineName, fingerprint);
            return materialRepository.findRevisionsMatching(materialConfig, searchString);
        } catch (RuntimeException e) {
            result.notFound(LocalizedMessage.materialWithFingerPrintNotFound(pipelineName, fingerprint), HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
            return new ArrayList<>();
        }
    }

    public List<Modification> latestModification(Material material, File baseDir, final SubprocessExecutionContext execCtx) {
        return getPollerImplementation(material).latestModification(material, baseDir, execCtx);
    }

    public List<Modification> modificationsSince(Material material, File baseDir, Revision revision, final SubprocessExecutionContext execCtx) {
        return getPollerImplementation(material).modificationsSince(material, baseDir, revision, execCtx);
    }

    public MaterialPoller getPollerImplementation(Material material) {
        MaterialPoller materialPoller = materialPollerMap.get(getMaterialClass(material));
        return materialPoller == null ? new NoOpPoller() : materialPoller;
    }

	public Long getTotalModificationsFor(MaterialConfig materialConfig) {
		MaterialInstance materialInstance = materialRepository.findMaterialInstance(materialConfig);

		return materialRepository.getTotalModificationsFor(materialInstance);
	}

	public Modifications getModificationsFor(MaterialConfig materialConfig, Pagination pagination) {
		MaterialInstance materialInstance = materialRepository.findMaterialInstance(materialConfig);

		return materialRepository.getModificationsFor(materialInstance, pagination);
	}

    Class<? extends Material> getMaterialClass(Material material) {
        return material.getClass();
    }
}
