/*
 * Copyright 2019 ThoughtWorks, Inc.
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

/*  @description
 * Keeps track all config repo parsing states
 */


import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.server.service.ConfigRepoService;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthState;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigReposMaterialParseResultManager {
    private Map<String, PartialConfigParseResult> fingerprintOfPartialToParseResultMap = new ConcurrentHashMap<>();
    private ServerHealthService serverHealthService;
    private ConfigRepoService configRepoService;

    public ConfigReposMaterialParseResultManager(ServerHealthService serverHealthService, ConfigRepoService configRepoService) {
        this.serverHealthService = serverHealthService;
        this.configRepoService = configRepoService;
    }

    public PartialConfigParseResult get(String fingerprint) {
        PartialConfigParseResult result = fingerprintOfPartialToParseResultMap.get(fingerprint);
        if (result == null) {
            // config repository was never parsed, check if there are any material clone or update related errors
            result = getMaterialResult(fingerprint);
        }

        if (result !=null) {
            //config repository was parsed, but does not have merge or clone related errors.

            HealthStateScope healthStateScope = HealthStateScope.forPartialConfigRepo(fingerprint);
            List<ServerHealthState> serverHealthStates = serverHealthService.filterByScope(healthStateScope);
            // It should be noted that the server health state and the result can be thought of as the following:
            // serverHealthState is the current state
            // result.isSuccessfully() is the state at the last parse

            // here we're checking that the current state is bad, but the previous state was good
            if (!serverHealthStates.isEmpty() && result.isSuccessful()) {
                result.setException(represent(serverHealthStates.get(0)));

                //clear out the good modification, in case good modification is same as of latest parsed modification
                if (result.getLatestParsedModification().equals(result.getGoodModification())) {
                    result.setGoodModification(null);
                    result.setPartialConfig(null);
                }
            // here we're checking that the current state is good, but the previous state was bad
            } else if (serverHealthStates.isEmpty() && !result.isSuccessful()) {
                // The issues that caused the previous state to be bad are gone.
                // An example would be that previously a upstream pipeline didn't exist, but now does
                // Clearing latest parsed modification to allow for another parse attempt
                result.setLatestParsedModification(null);
            }
        }

        return result;
    }

    private PartialConfigParseResult getMaterialResult(String fingerprint) {
        HealthStateScope healthStateScope = HealthStateScope.forMaterialConfig(configRepoService.findByFingerprint(fingerprint).getMaterialConfig());
        List<ServerHealthState> serverHealthStates = serverHealthService.filterByScope(healthStateScope);
        if (!serverHealthStates.isEmpty()) {
            return PartialConfigParseResult.parseFailed(null, represent(serverHealthStates.get(0)));
        }
        return null;
    }

    private Exception represent(ServerHealthState serverHealthState) {
        String message = new StringBuilder()
                .append(serverHealthState.getMessage().toUpperCase())
                .append("\n")
                .append(serverHealthState.getDescription())
                .toString();

        return new Exception(message);
    }

    public Set<String> allFingerprints() {
        return fingerprintOfPartialToParseResultMap.keySet();
    }

    public void remove(String fingerprint) {
        fingerprintOfPartialToParseResultMap.remove(fingerprint);
    }

    public PartialConfigParseResult parseFailed(String fingerprint, Modification modification, Exception exception) {
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

    public PartialConfigParseResult parseSuccess(String fingerprint, Modification modification, PartialConfig newPart) {
        //if no result exists in the map, create a new one
        //if already a result exists in the map, override the result, as the latest modification is successful. regardless of the result being successful or failed
        return fingerprintOfPartialToParseResultMap.put(fingerprint, PartialConfigParseResult.parseSuccess(modification, newPart));
    }
}
