/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.listener.EntityConfigChangedListener;
import com.thoughtworks.go.server.service.ConfigRepoService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks all config-repo parsing states
 */
public class ConfigReposMaterialParseResultManager {
    private final Map<String, PartialConfigParseResult> fingerprintOfPartialToParseResultMap = new ConcurrentHashMap<>();
    private final ServerHealthService serverHealthService;
    private final ConfigRepoService configRepoService;

    ConfigReposMaterialParseResultManager(ServerHealthService serverHealthService, ConfigRepoService configRepoService) {
        this.serverHealthService = serverHealthService;
        this.configRepoService = configRepoService;
    }

    public PartialConfigParseResult get(String fingerprint) {
        PartialConfigParseResult result = fingerprintOfPartialToParseResultMap.get(fingerprint);
        // config repository was never parsed, check if there are any material clone or update related errors
        if (result == null) {
            result = checkForMaterialErrors(fingerprint);
        }

        //config repository was parsed, but does not have merge or clone related errors.
        if (result != null && result.isSuccessful()) {
            HealthStateScope healthStateScope = HealthStateScope.forPartialConfigRepo(fingerprint);
            List<ServerHealthState> serverHealthStates = serverHealthService.filterByScope(healthStateScope);
            if (!serverHealthStates.isEmpty()) {
                result.setException(asError(serverHealthStates.get(0)));

                //clear out the good modification, in case good modification is same as of latest parsed modification
                if (Objects.equals(result.getLatestParsedModification(), result.getGoodModification())) {
                    result.setGoodModification(null);
                    result.setPartialConfig(null);
                }
            }
        }

        return result;
    }

    /**
     * Registers the EntityChangedListener for various classes to mark config-repos for reparse on config updates.
     *
     * @param configService the {@link GoConfigService} to register events
     */
    void attachConfigUpdateListeners(GoConfigService configService) {
        configService.register(new ConfigRepoReparseListener(this));
    }

    /**
     * After a successful update of the server config, we should reparse any failed config-repos in case
     * any upstream dependency issues have been resolved.
     * <p>
     * This clears the last parse states and errors related to config-repos so as to allow reparsing. Should be
     * used by ConfigChangedListener and EntityConfigChangedListener instances to hook into config update events.
     */
    void markFailedResultsForReparse() {
        for (Map.Entry<String, PartialConfigParseResult> entry : fingerprintOfPartialToParseResultMap.entrySet()) {
            String fingerprint = entry.getKey();
            PartialConfigParseResult result = entry.getValue();

            HealthStateScope scope = HealthStateScope.forPartialConfigRepo(fingerprint);
            List<ServerHealthState> serverHealthErrors = serverHealthService.filterByScope(scope);

            if (!serverHealthErrors.isEmpty() || !result.isSuccessful()) {
                result.setLatestParsedModification(null); // clear modification to allow a reparse to happen
            }
        }
    }

    private PartialConfigParseResult checkForMaterialErrors(String fingerprint) {
        MaterialConfig naterial = configRepoService.findByFingerprint(fingerprint).getRepo();
        HealthStateScope healthStateScope = HealthStateScope.forMaterialConfig(naterial);
        List<ServerHealthState> serverHealthStates = serverHealthService.filterByScope(healthStateScope);

        return serverHealthStates.isEmpty() ?
                null :
                PartialConfigParseResult.parseFailed(null, asError(serverHealthStates.get(0)));
    }

    private Exception asError(ServerHealthState serverHealthState) {
        return new Exception(serverHealthState.getMessage().toUpperCase() + "\n" + serverHealthState.getDescription());
    }

    Set<String> allFingerprints() {
        return fingerprintOfPartialToParseResultMap.keySet();
    }

    public void remove(String fingerprint) {
        fingerprintOfPartialToParseResultMap.remove(fingerprint);
    }

    PartialConfigParseResult parseFailed(String fingerprint, Modification modification, Exception exception) {
        PartialConfigParseResult existingResult = get(fingerprint);
        if (existingResult == null) { //if no result exists in the map, create a new one
            return fingerprintOfPartialToParseResultMap.put(fingerprint, PartialConfigParseResult.parseFailed(modification, exception));
        } else {
            PartialConfigParseResult newResult = PartialConfigParseResult.parseFailed(modification, exception);
            newResult.setGoodModification(existingResult.getGoodModification());
            newResult.setPartialConfig(existingResult.lastGoodPartialConfig());
            return fingerprintOfPartialToParseResultMap.put(fingerprint, newResult);
        }
    }

    PartialConfigParseResult parseSuccess(String fingerprint, Modification modification, PartialConfig newPart) {
        //if no result exists in the map, create a new one
        //if already a result exists in the map, override the result, as the latest modification is successful. regardless of the result being successful or failed
        return fingerprintOfPartialToParseResultMap.put(fingerprint, PartialConfigParseResult.parseSuccess(modification, newPart));
    }

    static class ConfigRepoReparseListener extends EntityConfigChangedListener<Object> {
        private final List<Class<?>> configClassesToCareAbout = Arrays.asList(
                PipelineConfig.class,
                EnvironmentConfig.class,
                PipelineTemplateConfig.class,
                SCM.class,
                ConfigRepoConfig.class,
                ElasticProfile.class
        );
        private final ConfigReposMaterialParseResultManager configReposMaterialParseResultManager;

        ConfigRepoReparseListener(ConfigReposMaterialParseResultManager configReposMaterialParseResultManager) {
            this.configReposMaterialParseResultManager = configReposMaterialParseResultManager;
        }

        @Override
        public boolean shouldCareAbout(Object entity) {
            return configClassesToCareAbout.stream().anyMatch(aClass -> aClass.isAssignableFrom(entity.getClass()));
        }

        @Override
        public void onEntityConfigChange(Object entity) {
            configReposMaterialParseResultManager.markFailedResultsForReparse();
        }

        @Override
        public void onConfigChange(CruiseConfig newCruiseConfig) {
            configReposMaterialParseResultManager.markFailedResultsForReparse();
        }
    }
}
