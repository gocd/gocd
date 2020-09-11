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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.listener.ConfigChangedListener;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.materials.postcommit.PostCommitHookImplementer;
import com.thoughtworks.go.server.materials.postcommit.PostCommitHookMaterialType;
import com.thoughtworks.go.server.materials.postcommit.PostCommitHookMaterialTypeResolver;
import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.server.messaging.GoMessageQueue;
import com.thoughtworks.go.server.perf.MDUPerformanceLogger;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.MaintenanceModeService;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.server.service.SecretParamResolver;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.MaterialFingerprintTag;
import com.thoughtworks.go.util.ProcessManager;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.thoughtworks.go.serverhealth.HealthStateType.general;
import static com.thoughtworks.go.serverhealth.ServerHealthState.warning;

/**
 * @understands when to send requests to update a material on the database
 */
@Service
public class MaterialUpdateService implements GoMessageListener<MaterialUpdateCompletedMessage>, ConfigChangedListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(MaterialUpdateService.class);

    private final MaterialUpdateQueue updateQueue;
    private final ConfigMaterialUpdateQueue configUpdateQueue;
    private final DependencyMaterialUpdateQueue dependencyMaterialUpdateQueue;
    private final MaintenanceModeService maintenanceModeService;
    private final SecretParamResolver secretParamResolver;
    private final ExponentialBackoffService exponentialBackoffService;
    private final GoConfigWatchList watchList;
    private final GoConfigService goConfigService;
    private final SystemEnvironment systemEnvironment;
    private ServerHealthService serverHealthService;

    private ConcurrentMap<Material, Date> inProgress = new ConcurrentHashMap<>();

    private final PostCommitHookMaterialTypeResolver postCommitHookMaterialType;
    private final MDUPerformanceLogger mduPerformanceLogger;
    private final MaterialConfigConverter materialConfigConverter;
    private final Set<MaterialSource> materialSources = new HashSet<>();
    private final Set<MaterialUpdateCompleteListener> materialUpdateCompleteListeners = new HashSet<>();
    public static final String TYPE = "post_commit_hook_material_type";

    @Autowired
    public MaterialUpdateService(MaterialUpdateQueue queue, ConfigMaterialUpdateQueue configUpdateQueue,
                                 MaterialUpdateCompletedTopic completed, GoConfigWatchList watchList,
                                 GoConfigService goConfigService, SystemEnvironment systemEnvironment,
                                 ServerHealthService serverHealthService, PostCommitHookMaterialTypeResolver postCommitHookMaterialType,
                                 MDUPerformanceLogger mduPerformanceLogger, MaterialConfigConverter materialConfigConverter,
                                 DependencyMaterialUpdateQueue dependencyMaterialUpdateQueue, MaintenanceModeService maintenanceModeService,
                                 SecretParamResolver secretParamResolver, ExponentialBackoffService exponentialBackoffService) {
        this.watchList = watchList;
        this.goConfigService = goConfigService;
        this.systemEnvironment = systemEnvironment;
        this.updateQueue = queue;
        this.configUpdateQueue = configUpdateQueue;
        this.serverHealthService = serverHealthService;
        this.postCommitHookMaterialType = postCommitHookMaterialType;
        this.mduPerformanceLogger = mduPerformanceLogger;
        this.materialConfigConverter = materialConfigConverter;
        this.dependencyMaterialUpdateQueue = dependencyMaterialUpdateQueue;
        this.maintenanceModeService = maintenanceModeService;
        this.secretParamResolver = secretParamResolver;
        this.exponentialBackoffService = exponentialBackoffService;
        completed.addListener(this);
    }

    public void initialize() {
        goConfigService.register(this);
        goConfigService.register(pipelineConfigChangedListener());
    }

    public void onTimer() {
        if (maintenanceModeService.isMaintenanceMode()) {
            LOGGER.debug("[Maintenance Mode] GoCD server is in 'maintenance' mode, skip checking for MDU.");
            return;
        }

        for (MaterialSource materialSource : materialSources) {
            Set<Material> materialsForUpdate = materialSource.materialsForUpdate();
            LOGGER.debug("[Material Update] [On Timer] materials IN-PROGRESS: {}, ALL-MATERIALS: {}", inProgress, materialsForUpdate);

            for (Material material : materialsForUpdate) {
                BackOffResult backOffResult = exponentialBackoffService.shouldBackOff(material);
                if (backOffResult.shouldBackOff()) {
                    LOGGER.debug("[Material Update] [On Timer] Backing Off Material Update for: {}, failing since: {}, last failure time: {}, next retry will be attempted after: {}",
                            material, backOffResult.getFailureStartTime(), backOffResult.getLastFailureTime(), backOffResult.getNextRetryAttempt());
                    continue;
                }

                updateMaterial(material);
            }
        }
    }

    public void notifyMaterialsForUpdate(Username username, Object params, HttpLocalizedOperationResult result) {
        if (!goConfigService.isUserAdmin(username)) {
            result.forbidden("Unauthorized to access this API.", HealthStateType.forbidden());
            return;
        }
        final Map attributes = (Map) params;
        if (attributes.containsKey(MaterialUpdateService.TYPE)) {
            PostCommitHookMaterialType materialType = postCommitHookMaterialType.toType((String) attributes.get(MaterialUpdateService.TYPE));
            if (!materialType.isKnown()) {
                result.badRequest("The request could not be understood by Go Server due to malformed syntax. The client SHOULD NOT repeat the request without modifications.");
                return;
            }
            final PostCommitHookImplementer materialTypeImplementer = materialType.getImplementer();
            final CruiseConfig cruiseConfig = goConfigService.currentCruiseConfig();
            Set<Material> allUniquePostCommitSchedulableMaterials = materialConfigConverter.toMaterials(cruiseConfig.getAllUniquePostCommitSchedulableMaterials());
            resolveSecretForSvnMaterials(allUniquePostCommitSchedulableMaterials);
            final Set<Material> prunedMaterialList = materialTypeImplementer.prune(allUniquePostCommitSchedulableMaterials, attributes);

            if (prunedMaterialList.isEmpty()) {
                result.notFound("Unable to find material. Materials must be configured not to poll for new changes before they can be used with the notification mechanism.", HealthStateType.general(HealthStateScope.GLOBAL));
                return;
            }

            for (Material material : prunedMaterialList) {
                updateMaterial(material);
            }

            result.accepted("The material is now scheduled for an update. Please check relevant pipeline(s) for status.");
        } else {
            result.badRequest("The request could not be understood by Go Server due to malformed syntax. The client SHOULD NOT repeat the request without modifications.");
        }
    }

    public boolean updateGitMaterial(String branchName, Collection<String> possibleUrls) {
        final CruiseConfig cruiseConfig = goConfigService.currentCruiseConfig();
        Set<Material> allUniquePostCommitSchedulableMaterials = materialConfigConverter.toMaterials(cruiseConfig.getAllUniquePostCommitSchedulableMaterials());

        Predicate<Material> predicate = new MaterialPredicate(branchName, possibleUrls);
        Set<Material> allGitMaterials = allUniquePostCommitSchedulableMaterials.stream().filter(predicate).collect(Collectors.toSet());

        allGitMaterials.forEach(MaterialUpdateService.this::updateMaterial);

        return !allGitMaterials.isEmpty();
    }

    public boolean updateMaterial(Material material) {
        Date inProgressSince = inProgress.putIfAbsent(material, new Date());
        if (inProgressSince == null || !material.isAutoUpdate()) {
            LOGGER.debug("[Material Update] Starting update of material {}", material);
            try {
                long trackingId = mduPerformanceLogger.materialSentToUpdateQueue(material);
                queueFor(material).post(new MaterialUpdateMessage(material, trackingId));

                return true;
            } catch (RuntimeException e) {
                inProgress.remove(material);
                throw e;
            }
        } else {
            LOGGER.warn("[Material Update] Skipping update of material {} which has been in-progress since {}", material, inProgressSince);
            long idleTime = getProcessManager().getIdleTimeFor(new MaterialFingerprintTag(material.getFingerprint()));
            if (idleTime > getMaterialUpdateInActiveTimeoutInMillis()) {
                HealthStateScope scope = HealthStateScope.forMaterialUpdate(material);
                serverHealthService.removeByScope(scope);
                serverHealthService.update(warning("Material update for " + material.getUriForDisplay() + " hung:",
                        "Material update is currently running but has not shown any activity in the last " + idleTime / 60000 + " minute(s). This may be hung. Details - " + material.getLongDescription(),
                        general(scope)));
            }
            return false;
        }
    }

    private void resolveSecretForSvnMaterials(Set<Material> allUniquePostCommitSchedulableMaterials) {
//      Secrets are resolved only for SvnMaterials, since only SvnMaterial prune requires resolved password.

        allUniquePostCommitSchedulableMaterials.stream()
                .filter(material -> material instanceof SvnMaterial)
                .forEach(material -> secretParamResolver.resolve((SvnMaterial) material));
    }

    public void registerMaterialSources(MaterialSource materialSource) {
        this.materialSources.add(materialSource);
    }

    @Override
    public void onMessage(MaterialUpdateCompletedMessage message) {
        if (message instanceof MaterialUpdateSkippedMessage) {
            inProgress.remove(message.getMaterial());
            return;
        }

        try {
            LOGGER.debug("[Material Update] Material update completed for material {}", message.getMaterial());

            Date addedOn = inProgress.remove(message.getMaterial());
            serverHealthService.removeByScope(HealthStateScope.forMaterialUpdate(message.getMaterial()));
            if (addedOn == null) {
                LOGGER.warn("[Material Update] Material {} was not removed from those inProgress. This might result in it's pipelines not getting scheduled. in-progress: {}", message.getMaterial(), inProgress);
            }

            for (MaterialUpdateCompleteListener listener : materialUpdateCompleteListeners) {
                listener.onMaterialUpdate(message.getMaterial());
            }
        } finally {
            mduPerformanceLogger.completionMessageForMaterialReceived(message.trackingId(), message.getMaterial());
        }
    }

    @Override
    public void onConfigChange(CruiseConfig newCruiseConfig) {
        Set<HealthStateScope> materialScopes = toHealthStateScopes(newCruiseConfig.getAllUniqueMaterials());
        for (ServerHealthState state : serverHealthService.logs()) {
            HealthStateScope currentScope = state.getType().getScope();
            if (currentScope.isForMaterial() && !materialScopes.contains(currentScope)) {
                serverHealthService.removeByScope(currentScope);
            }
        }
    }

    protected EntityConfigChangedListener<PipelineConfig> pipelineConfigChangedListener() {
        final MaterialUpdateService self = this;
        return new EntityConfigChangedListener<PipelineConfig>() {
            @Override
            public void onEntityConfigChange(PipelineConfig pipelineConfig) {
                self.onConfigChange(goConfigService.getCurrentConfig());
            }
        };
    }

    private Set<HealthStateScope> toHealthStateScopes(Set<MaterialConfig> materialConfigs) {
        Set<HealthStateScope> scopes = new HashSet<>();
        for (MaterialConfig materialConfig : materialConfigs) {
            scopes.add(HealthStateScope.forMaterialConfig(materialConfig));
        }
        return scopes;
    }

    private boolean isConfigMaterial(Material material) {
        return watchList.hasConfigRepoWithFingerprint(material.getFingerprint());
    }

    private Long getMaterialUpdateInActiveTimeoutInMillis() {
        return systemEnvironment.get(SystemEnvironment.MATERIAL_UPDATE_INACTIVE_TIMEOUT) * 60 * 1000L;
    }

    private GoMessageQueue<MaterialUpdateMessage> queueFor(Material material) {
        if (isConfigMaterial(material)) {
            return configUpdateQueue;
        }

        return (material instanceof DependencyMaterial) ? dependencyMaterialUpdateQueue : updateQueue;
    }

    ProcessManager getProcessManager() {
        return ProcessManager.getInstance();
    }

    public boolean isInProgress(Material material) {
        for (Material m : this.inProgress.keySet()) {
            if (m.isSameFlyweight(material))
                return true;
        }
        return false;
    }

    public void registerMaterialUpdateCompleteListener(MaterialUpdateCompleteListener materialUpdateCompleteListener) {
        this.materialUpdateCompleteListeners.add(materialUpdateCompleteListener);
    }

    private static class MaterialPredicate implements Predicate<Material> {
        private final String branchName;
        private final Set<String> possibleUrls;

        public MaterialPredicate(String branchName, Collection<String> possibleUrls) {
            this.branchName = branchName;
            this.possibleUrls = new HashSet<>(possibleUrls);
        }

        @Override
        public boolean test(Material material) {
            return material instanceof GitMaterial &&
                    ((GitMaterial) material).getBranch().equals(branchName) &&
                    possibleUrls.contains(((GitMaterial) material).getUrlArgument().withoutCredentials());
        }
    }
}
