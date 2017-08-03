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

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigWatchList;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.i18n.LocalizedMessage;
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
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.ProcessManager;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.thoughtworks.go.serverhealth.HealthStateType.general;
import static com.thoughtworks.go.serverhealth.ServerHealthState.warning;
import static java.lang.String.format;

/**
 * @understands when to send requests to update a material on the database
 */
@Service
public class MaterialUpdateService implements GoMessageListener<MaterialUpdateCompletedMessage>, ConfigChangedListener {
    private static final Logger LOGGER = Logger.getLogger(MaterialUpdateService.class);

    private final MaterialUpdateQueue updateQueue;
    private final ConfigMaterialUpdateQueue configUpdateQueue;
    private final DependencyMaterialUpdateQueue dependencyMaterialUpdateQueue;
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
                                 DependencyMaterialUpdateQueue dependencyMaterialUpdateQueue) {
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
        completed.addListener(this);
    }

    public void initialize() {
        goConfigService.register(this);
        goConfigService.register(pipelineConfigChangedListener());
    }

    public void onTimer() {
        for (MaterialSource materialSource : materialSources) {
            Set<Material> materialsForUpdate = materialSource.materialsForUpdate();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(format("[Material Update] [On Timer] materials IN-PROGRESS: %s, ALL-MATERIALS: %s", inProgress, materialsForUpdate));
            }

            for (Material material : materialsForUpdate) {
                updateMaterial(material);
            }
        }
    }

    public void notifyMaterialsForUpdate(Username username, Object params, HttpLocalizedOperationResult result) {
        if (!goConfigService.isUserAdmin(username)) {
            result.unauthorized(LocalizedMessage.string("API_ACCESS_UNAUTHORIZED"), HealthStateType.unauthorised());
            return;
        }
        final Map attributes = (Map) params;
        if (attributes.containsKey(MaterialUpdateService.TYPE)) {
            PostCommitHookMaterialType materialType = postCommitHookMaterialType.toType((String) attributes.get(MaterialUpdateService.TYPE));
            if (!materialType.isKnown()) {
                result.badRequest(LocalizedMessage.string("API_BAD_REQUEST"));
                return;
            }
            final PostCommitHookImplementer materialTypeImplementer = materialType.getImplementer();
            final CruiseConfig cruiseConfig = goConfigService.currentCruiseConfig();
            Set<Material> allUniquePostCommitSchedulableMaterials = materialConfigConverter.toMaterials(cruiseConfig.getAllUniquePostCommitSchedulableMaterials());
            final Set<Material> prunedMaterialList = materialTypeImplementer.prune(allUniquePostCommitSchedulableMaterials, attributes);

            if (prunedMaterialList.isEmpty()) {
                result.notFound(LocalizedMessage.string("MATERIAL_SUITABLE_FOR_NOTIFICATION_NOT_FOUND"), HealthStateType.general(HealthStateScope.GLOBAL));
                return;
            }

            for (Material material : prunedMaterialList) {
                updateMaterial(material);
            }

            result.accepted(LocalizedMessage.string("MATERIAL_SCHEDULE_NOTIFICATION_ACCEPTED"));
        } else {
            result.badRequest(LocalizedMessage.string("API_BAD_REQUEST"));
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
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(format("[Material Update] Starting update of material %s", material));
            }
            try {
                long trackingId = mduPerformanceLogger.materialSentToUpdateQueue(material);
                queueFor(material).post(new MaterialUpdateMessage(material, trackingId));

                return true;

            } catch (RuntimeException e) {
                inProgress.remove(material);
                throw e;
            }
        } else {
            LOGGER.warn(format("[Material Update] Skipping update of material %s which has been in-progress since %s", material, inProgressSince));
            long idleTime = getProcessManager().getIdleTimeFor(material.getFingerprint());
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

    public void registerMaterialSources(MaterialSource materialSource) {
        this.materialSources.add(materialSource);
    }

    public void onMessage(MaterialUpdateCompletedMessage message) {
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(format("[Material Update] Material update completed for material %s", message.getMaterial()));
            }

            Date addedOn = inProgress.remove(message.getMaterial());
            serverHealthService.removeByScope(HealthStateScope.forMaterialUpdate(message.getMaterial()));
            if (addedOn == null) {
                LOGGER.warn(format("[Material Update] Material %s was not removed from those inProgress. This might result in it's pipelines not getting scheduled. in-progress: %s",
                        message.getMaterial(), inProgress));
            }

            for (MaterialUpdateCompleteListener listener : materialUpdateCompleteListeners) {
                listener.onMaterialUpdate(message.getMaterial());
            }
        } finally {
            mduPerformanceLogger.completionMessageForMaterialReceived(message.trackingId(), message.getMaterial());
        }
    }

    public void onConfigChange(CruiseConfig newCruiseConfig) {
        Set<HealthStateScope> materialScopes = toHealthStateScopes(newCruiseConfig.getAllUniqueMaterials());
        for (ServerHealthState state : serverHealthService.getAllLogs()) {
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

    //used in tests
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
