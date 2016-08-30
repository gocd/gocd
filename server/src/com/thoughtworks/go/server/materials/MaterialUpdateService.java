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
import com.thoughtworks.go.domain.PipelineGroups;
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

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
    private final GoConfigWatchList watchList;
    private final GoConfigService goConfigService;
    private final SystemEnvironment systemEnvironment;
    private ServerHealthService serverHealthService;

    private ConcurrentMap<Material, Date> inProgress = new ConcurrentHashMap<>();
    private ConcurrentMap<Material, Long> materialLastUpdateTimeMap = new ConcurrentHashMap<>();

    private final PostCommitHookMaterialTypeResolver postCommitHookMaterialType;
    private final MDUPerformanceLogger mduPerformanceLogger;
    private final MaterialConfigConverter materialConfigConverter;
    public static final String TYPE = "post_commit_hook_material_type";
    private final long materialUpdateInterval;
    private Set<Material> schedulableMaterials;

    @Autowired
    public MaterialUpdateService(MaterialUpdateQueue queue,ConfigMaterialUpdateQueue configUpdateQueue,
                                 MaterialUpdateCompletedTopic completed,
                                 GoConfigWatchList watchList,
                                 GoConfigService goConfigService,
                                 SystemEnvironment systemEnvironment, ServerHealthService serverHealthService,
                                 PostCommitHookMaterialTypeResolver postCommitHookMaterialType,
                                 MDUPerformanceLogger mduPerformanceLogger, MaterialConfigConverter materialConfigConverter) {
        this.watchList = watchList;
        this.goConfigService = goConfigService;
        this.systemEnvironment = systemEnvironment;
        this.updateQueue = queue;
        this.configUpdateQueue = configUpdateQueue;
        this.serverHealthService = serverHealthService;
        this.postCommitHookMaterialType = postCommitHookMaterialType;
        this.mduPerformanceLogger = mduPerformanceLogger;
        this.materialConfigConverter = materialConfigConverter;
        this.materialUpdateInterval = systemEnvironment.getMaterialUpdateIdleInterval();
        completed.addListener(this);
    }

    public void initialize() {
        goConfigService.register(this);
        goConfigService.register(pipelineConfigChangedListener());
    }

    protected EntityConfigChangedListener<PipelineConfig> pipelineConfigChangedListener() {
        final MaterialUpdateService materialUpdateService = this;
        return new EntityConfigChangedListener<PipelineConfig>() {
            @Override
            public void onEntityConfigChange(PipelineConfig pipelineConfig) {
                materialUpdateService.onConfigChange(goConfigService.getCurrentConfig());
            }
        };
    }

    public void onTimer() {
        updateSchedulableMaterials(false);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(format("[Material Update] [On Timer] materials IN-PROGRESS: %s, ALL-MATERIALS: %s", inProgress, schedulableMaterials));
        }
        for (Material material : schedulableMaterials) {
            if (hasUpdateIntervalElapsedForScmMaterial(material)) {
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

    public void updateMaterial(Material material) {
        Date inProgressSince = inProgress.putIfAbsent(material, new Date());
        if (inProgressSince == null || !material.isAutoUpdate()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(format("[Material Update] Updating material %s", material));
            }
            try {
                long trackingId = mduPerformanceLogger.materialSentToUpdateQueue(material);
                if(isConfigMaterial(material))
                    configUpdateQueue.post(new MaterialUpdateMessage(material, trackingId));
                else
                    updateQueue.post(new MaterialUpdateMessage(material, trackingId));
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
        }
    }

    private boolean isConfigMaterial(Material material) {
        return watchList.hasConfigRepoWithFingerprint(material.getFingerprint());
    }

    private Long getMaterialUpdateInActiveTimeoutInMillis() {
        return systemEnvironment.get(SystemEnvironment.MATERIAL_UPDATE_INACTIVE_TIMEOUT) * 60 * 1000L;
    }

    public void onMessage(MaterialUpdateCompletedMessage message) {
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(format("[Material Update] Material update completed for material %s", message.getMaterial()));
            }
            updateLastUpdateTimeForScmMaterial(message.getMaterial());
            Date addedOn = inProgress.remove(message.getMaterial());
            serverHealthService.removeByScope(HealthStateScope.forMaterialUpdate(message.getMaterial()));
            if (addedOn == null) {
                LOGGER.warn(format("[Material Update] Material %s was not removed from those inProgress. This might result in it's pipelines not getting scheduled. in-progress: %s",
                        message.getMaterial(), inProgress));
            }
        } finally {
            mduPerformanceLogger.completionMessageForMaterialReceived(message.trackingId(), message.getMaterial());
        }
    }

    public void onConfigChange(CruiseConfig newCruiseConfig) {
        updateSchedulableMaterials(true);
        Set<HealthStateScope> materialScopes = toHealthStateScopes(newCruiseConfig.getAllUniqueMaterials());
        for (ServerHealthState state : serverHealthService.getAllLogs()) {
            HealthStateScope currentScope = state.getType().getScope();
            if (currentScope.isForMaterial() && !materialScopes.contains(currentScope)) {
                serverHealthService.removeByScope(currentScope);
            }
        }
    }

    ProcessManager getProcessManager() {
        return ProcessManager.getInstance();
    }

    private Set<HealthStateScope> toHealthStateScopes(Set<MaterialConfig> materialConfigs) {
        Set<HealthStateScope> scopes = new HashSet<>();
        for (MaterialConfig materialConfig : materialConfigs) {
            scopes.add(HealthStateScope.forMaterialConfig(materialConfig));
        }
        return scopes;
    }

    boolean hasUpdateIntervalElapsedForScmMaterial(Material material) {
        if (materialLastUpdateTimeMap.containsKey(material)) {
            Long lastMaterialUpdateTime = materialLastUpdateTimeMap.get(material);
            boolean shouldUpdateMaterial = (System.currentTimeMillis() - lastMaterialUpdateTime) >= materialUpdateInterval;
            if (LOGGER.isDebugEnabled() && !shouldUpdateMaterial) {
                LOGGER.debug(format("[Material Update] Skipping update of material %s which has been last updated at %s", material, new Date(lastMaterialUpdateTime)));
            }
            return shouldUpdateMaterial;
        }
        return true;
    }

    private void updateLastUpdateTimeForScmMaterial(Material material) {
        materialLastUpdateTimeMap.put(material, System.currentTimeMillis());
    }

    private void updateSchedulableMaterials(boolean forceLoad) {
        if (forceLoad || schedulableMaterials == null) {
            schedulableMaterials = materialConfigConverter.toMaterials(goConfigService.getSchedulableMaterials());
        }
    }

    //used in tests
    public boolean isInProgress(Material material) {
        for(Material m : this.inProgress.keySet())
        {
            if(m.isSameFlyweight(material))
                return true;
        }
        return false;
    }
}
