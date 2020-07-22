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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CachedGoPartials implements PartialConfigResolver {
    private final ServerHealthService serverHealthService;
    private final Map<String, PartialConfig> fingerprintToLatestValidConfigMap = new ConcurrentHashMap<>();
    private final Map<String, PartialConfig> fingerprintToLatestKnownConfigMap = new ConcurrentHashMap<>();

    @Autowired
    public CachedGoPartials(ServerHealthService serverHealthService) {
        this.serverHealthService = serverHealthService;
    }

    public List<PartialConfig> lastValidPartials() {
        return new ArrayList<>(fingerprintToLatestValidConfigMap.values());
    }

    public List<PartialConfig> lastKnownPartials() {
        return new ArrayList<>(fingerprintToLatestKnownConfigMap.values());
    }

    public void removeKnown(String fingerprint) {
        if (fingerprintToLatestKnownConfigMap.containsKey(fingerprint)) {
            fingerprintToLatestKnownConfigMap.remove(fingerprint);
            serverHealthService.removeByScope(HealthStateScope.forPartialConfigRepo(fingerprint));
        }
    }

    public PartialConfig getValid(String fingerprint) {
        return getPartialConfig(fingerprint, fingerprintToLatestValidConfigMap);
    }

    public PartialConfig getKnown(String fingerprint) {
        return getPartialConfig(fingerprint, fingerprintToLatestKnownConfigMap);
    }

    public void removeValid(String fingerprint) {
        if (fingerprintToLatestValidConfigMap.containsKey(fingerprint)) {
            fingerprintToLatestValidConfigMap.remove(fingerprint);
            serverHealthService.removeByScope(HealthStateScope.forPartialConfigRepo(fingerprint));
        }
    }

    public void removeValidWithoutClearingServerHealthMessage(String fingerprint) {
        if (fingerprintToLatestValidConfigMap.containsKey(fingerprint)) {
            fingerprintToLatestValidConfigMap.remove(fingerprint);
        }
    }

    public void cacheAsLastKnown(String fingerprint, PartialConfig newPart) {
        fingerprintToLatestKnownConfigMap.put(fingerprint, newPart);
    }

    public void markAsValid(String fingerprint, PartialConfig newPart) {
        if (fingerprintToLatestValidConfigMap.containsKey(fingerprint)) {
            if (!fingerprintToLatestValidConfigMap.get(fingerprint).getOrigin().equals(newPart.getOrigin())) {
                markAsValidAndUpdateServerHealthMessage(fingerprint, newPart);
            }
        } else {
            markAsValidAndUpdateServerHealthMessage(fingerprint, newPart);
        }
    }

    public void markAsValid(List<PartialConfig> partials) {
        for (PartialConfig partial : partials) {
            if (partial.getOrigin() instanceof RepoConfigOrigin) {
                String fingerprint = ((RepoConfigOrigin) partial.getOrigin()).getMaterial().getFingerprint();
                markAsValid(fingerprint, partial);
            }
        }
    }

    public Map<String, PartialConfig> getFingerprintToLatestKnownConfigMap() {
        return fingerprintToLatestKnownConfigMap;
    }

    public Map<String, PartialConfig> getFingerprintToLatestValidConfigMap() {
        return fingerprintToLatestValidConfigMap;
    }

    public void clear() {
        fingerprintToLatestKnownConfigMap.clear();
        fingerprintToLatestValidConfigMap.clear();
    }

    public void markAllKnownAsValid() {
        markAsValid(lastKnownPartials());
    }

    @Override
    public PartialConfig findPartialByFingerprint(CruiseConfig cruiseConfig, String fingerprint) {
        PartialConfig matchingPartial = findPartialByFingerprint(cruiseConfig, fingerprint, getValid(fingerprint));
        if (matchingPartial == null) {
            matchingPartial = findPartialByFingerprint(cruiseConfig, fingerprint, getKnown(fingerprint));
        }
        return matchingPartial;
    }

    private PartialConfig getPartialConfig(String fingerprint, Map<String, PartialConfig> map) {
        if (map.containsKey(fingerprint)) {
            return map.get(fingerprint);
        }
        return null;
    }

    private void markAsValidAndUpdateServerHealthMessage(String fingerprint, PartialConfig newPart) {
        fingerprintToLatestValidConfigMap.put(fingerprint, newPart);
        serverHealthService.removeByScope(HealthStateScope.forPartialConfigRepo(fingerprint));
    }

    private PartialConfig findPartialByFingerprint(CruiseConfig cruiseConfig, String fingerprint, PartialConfig partial) {
        PartialConfig matching = null;
        if (partial != null) {
            for (PartialConfig partialConfig : cruiseConfig.getPartials()) {
                if (partialConfig.getOrigin() instanceof RepoConfigOrigin && (((RepoConfigOrigin) partialConfig.getOrigin()).getMaterial().getFingerprint().equals(fingerprint))) {
                    matching = partialConfig;
                    break;
                }
            }
        }
        return matching;
    }
}
