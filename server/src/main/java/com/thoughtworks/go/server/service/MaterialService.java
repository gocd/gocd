/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.config.SecretParamAware;
import com.thoughtworks.go.config.exceptions.EntityType;
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
import com.thoughtworks.go.domain.PipelineRunIdInfo;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.server.dao.FeedModifier;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.materials.*;
import com.thoughtworks.go.server.service.result.LocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.util.Pagination;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static com.thoughtworks.go.server.service.HistoryUtil.validateCursor;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Understands interactions between material-config, repository and modifications
 */
@Service
public class MaterialService {
    private final MaterialRepository materialRepository;
    private final GoConfigService goConfigService;
    private final SecurityService securityService;
    private final PackageRepositoryExtension packageRepositoryExtension;
    private final SCMExtension scmExtension;
    private final TransactionTemplate transactionTemplate;
    private final SecretParamResolver secretParamResolver;
    private final Map<Class<? extends Material>, MaterialPoller<? extends Material>> materialPollerMap = new HashMap<>();

    @Autowired
    public MaterialService(MaterialRepository materialRepository,
                           GoConfigService goConfigService,
                           SecurityService securityService,
                           PackageRepositoryExtension packageRepositoryExtension,
                           SCMExtension scmExtension,
                           TransactionTemplate transactionTemplate,
                           SecretParamResolver secretParamResolver) {
        this.materialRepository = materialRepository;
        this.goConfigService = goConfigService;
        this.securityService = securityService;
        this.packageRepositoryExtension = packageRepositoryExtension;
        this.scmExtension = scmExtension;
        this.transactionTemplate = transactionTemplate;
        this.secretParamResolver = secretParamResolver;
        populatePollerImplementations();
    }

    private void populatePollerImplementations() {
        materialPollerMap.put(GitMaterial.class, new GitPoller());
        materialPollerMap.put(HgMaterial.class, new HgPoller());
        materialPollerMap.put(SvnMaterial.class, new SvnPoller());
        materialPollerMap.put(TfsMaterial.class, new TfsPoller());
        materialPollerMap.put(P4Material.class, new P4Poller());
        materialPollerMap.put(DependencyMaterial.class, new DependencyMaterialPoller());
        materialPollerMap.put(PackageMaterial.class, new PackageMaterialPoller(packageRepositoryExtension));
        materialPollerMap.put(PluggableSCMMaterial.class, new PluggableSCMMaterialPoller(materialRepository, scmExtension, transactionTemplate));
    }

    public boolean hasModificationFor(Material material) {
        return !materialRepository.findLatestModification(material).isEmpty();
    }

    public List<MatchedRevision> searchRevisions(String pipelineName,
                                                 String fingerprint,
                                                 String searchString,
                                                 Username username,
                                                 LocalizedOperationResult result) {
        if (!securityService.hasViewPermissionForPipeline(username, pipelineName)) {
            result.forbidden(EntityType.Pipeline.forbiddenToView(pipelineName, username.getUsername()), HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
            return new ArrayList<>();
        }
        try {
            MaterialConfig materialConfig = goConfigService.materialForPipelineWithFingerprint(pipelineName, fingerprint);
            return materialRepository.findRevisionsMatching(materialConfig, searchString);
        } catch (RuntimeException e) {
            result.notFound("Pipeline '" + pipelineName + "' does not contain material with fingerprint '" + fingerprint + "'.", HealthStateType.general(HealthStateScope.forPipeline(pipelineName)));
            return new ArrayList<>();
        }
    }

    public List<Modification> latestModification(Material material,
                                                 File baseDir,
                                                 final SubprocessExecutionContext execCtx) {
        resolveSecretParams(material);
        return getPollerImplementation(material).latestModification(material, baseDir, execCtx);
    }

    public List<Modification> modificationsSince(Material material,
                                                 File baseDir,
                                                 Revision revision,
                                                 final SubprocessExecutionContext execCtx) {
        resolveSecretParams(material);
        return getPollerImplementation(material).modificationsSince(material, baseDir, revision, execCtx);
    }

    public void checkout(Material material, File baseDir, Revision revision, final SubprocessExecutionContext execCtx) {
        resolveSecretParams(material);

        getPollerImplementation(material).checkout(material, baseDir, revision, execCtx);
    }

    protected MaterialPoller<Material> getPollerImplementation(Material material) {
        Class<? extends Material> materialClass = getMaterialClass(material);
         @SuppressWarnings("unchecked") MaterialPoller<Material> materialPoller = (MaterialPoller<Material>) materialPollerMap.get(materialClass);
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

    private void resolveSecretParams(Material material) {
        if ((material instanceof SecretParamAware)) {
            this.secretParamResolver.resolve(material);
        }
    }

    Class<? extends Material> getMaterialClass(Material material) {
        return material.getClass();
    }

    public Map<String, Modification> getLatestModificationForEachMaterial() {
        List<Modification> modifications = materialRepository.getLatestModificationForEachMaterial();
        return modifications
                .stream()
                .collect(toMap(mod -> mod.getMaterialInstance().getFingerprint(), Function.identity()));
    }

    public PipelineRunIdInfo getLatestAndOldestModification(MaterialConfig materialConfig, String pattern) {
        MaterialInstance materialInstance = materialRepository.findMaterialInstance(materialConfig);
        if (materialInstance == null) {
            return null;
        }
        return materialRepository.getOldestAndLatestModificationId(materialInstance.getId(), pattern);
    }

    public List<Modification> getModificationsFor(MaterialConfig materialConfig, String pattern, long after, long before, Integer pageSize) {
        MaterialInstance materialInstance = materialRepository.findMaterialInstance(materialConfig);

        if (materialInstance == null) {
            return null;
        }

        return isBlank(pattern)
                ? getModificationsFor(materialInstance, after, before, pageSize)
                : findMatchingModifications(materialInstance, pattern, after, before, pageSize);
    }

    private List<Modification> getModificationsFor(MaterialInstance materialInstance, long afterCursor, long beforeCursor, Integer pageSize) {
        Pair<Long, FeedModifier> cursor = cursor(afterCursor, beforeCursor);

        return materialRepository.loadHistory(materialInstance.getId(), cursor.last(), cursor.first(), pageSize);
    }

    private List<Modification> findMatchingModifications(MaterialInstance materialInstance, String pattern, long afterCursor, long beforeCursor, Integer pageSize) {
        Pair<Long, FeedModifier> cursor = cursor(afterCursor, beforeCursor);

        return materialRepository.findMatchingModifications(materialInstance.getId(), pattern, cursor.last(), cursor.first(), pageSize);
    }

    private Pair<Long, FeedModifier> cursor(long afterCursor, long beforeCursor) {
        if (validateCursor(afterCursor, "after")) {
            return new Pair<>(afterCursor, FeedModifier.After);
        } else if (validateCursor(beforeCursor, "before")) {
            return new Pair<>(beforeCursor, FeedModifier.Before);
        }

        return new Pair<>(0L, FeedModifier.Latest);
    }
}
