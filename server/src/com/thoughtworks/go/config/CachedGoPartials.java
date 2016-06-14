/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@Component
public class CachedGoPartials {
    private final ServerHealthService serverHealthService;

    @Autowired
    public CachedGoPartials(ServerHealthService serverHealthService) {

        this.serverHealthService = serverHealthService;
    }

    public List<PartialConfig> lastValidPartials() {
        return getPartialConfigs(fingerprintToLatestValidConfigMap);
    }

    public List<PartialConfig> lastKnownPartials() {
        return getPartialConfigs(fingerprintToLatestKnownConfigMap);
    }

    private List<PartialConfig> getPartialConfigs(Map<String, UpdatedPartial> map) {
        List<PartialConfig> list = new ArrayList<>();
        for (UpdatedPartial partialConfig : map.values()) {
            list.add(partialConfig.partialConfig);
        }
        return list;
    }

    public void removeKnown(String fingerprint) {
        if (fingerprintToLatestKnownConfigMap.containsKey(fingerprint)) {
            fingerprintToLatestKnownConfigMap.remove(fingerprint);
        }
    }

    public PartialConfig getValid(String fingerprint) {
        return getPartialConfig(fingerprint, fingerprintToLatestValidConfigMap);
    }

    public PartialConfig getKnown(String fingerprint) {
        return getPartialConfig(fingerprint, fingerprintToLatestKnownConfigMap);
    }

    private PartialConfig getPartialConfig(String fingerprint, Map<String, UpdatedPartial> map) {
        if (map.containsKey(fingerprint)) {
            return map.get(fingerprint).partialConfig;
        }
        return null;
    }

    public void removeValid(String fingerprint) {
        if (fingerprintToLatestValidConfigMap.containsKey(fingerprint)) {
            fingerprintToLatestValidConfigMap.remove(fingerprint);
            serverHealthService.removeByScope(HealthStateScope.forPartialConfigRepo(fingerprint));
        }
    }


    public void addOrUpdate(String fingerprint, PartialConfig newPart) {
        fingerprintToLatestKnownConfigMap.put(fingerprint, new UpdatedPartial(newPart, new DateTime()));
    }

    public void markAsValid(String fingerprint, PartialConfig newPart) {
        DateTime lastUpdated = fingerprintToLatestKnownConfigMap.get(fingerprint).lastUpdated;
        if (fingerprintToLatestValidConfigMap.containsKey(fingerprint)) {
            if (!fingerprintToLatestValidConfigMap.get(fingerprint).partialConfig.getOrigin().equals(newPart.getOrigin())) {
                markAsValid(fingerprint, newPart, lastUpdated);
            }
        } else {
            markAsValid(fingerprint, newPart, lastUpdated);
        }
    }

    private void markAsValid(String fingerprint, PartialConfig newPart, DateTime lastUpdated) {
        fingerprintToLatestValidConfigMap.put(fingerprint, new UpdatedPartial(newPart, lastUpdated));
        serverHealthService.removeByScope(HealthStateScope.forPartialConfigRepo(fingerprint));
    }

    public void markAsValid(List<PartialConfig> partials) {
        for (PartialConfig partial : partials) {
            if (partial.getOrigin() instanceof RepoConfigOrigin) {
                String fingerprint = ((RepoConfigOrigin) partial.getOrigin()).getMaterial().getFingerprint();
                markAsValid(fingerprint, partial);
            }
        }
    }


    private class UpdatedPartial {
        private PartialConfig partialConfig;
        private DateTime lastUpdated;

        public UpdatedPartial(PartialConfig partialConfig, DateTime lastUpdated) {
            this.partialConfig = partialConfig;
            this.lastUpdated = lastUpdated;
        }
    }

    private Map<String, UpdatedPartial> fingerprintToLatestValidConfigMap = new ConcurrentHashMap<>();
    private Map<String, UpdatedPartial> fingerprintToLatestKnownConfigMap = new ConcurrentHashMap<>();

    public Map<String, UpdatedPartial> getFingerprintToLatestKnownConfigMap() {
        return fingerprintToLatestKnownConfigMap;
    }

    public Map<String, UpdatedPartial> getFingerprintToLatestValidConfigMap() {
        return fingerprintToLatestValidConfigMap;
    }

    public void clear(){
        fingerprintToLatestKnownConfigMap.clear();
        fingerprintToLatestValidConfigMap.clear();
    }

    public void markAllKnownAsValid() {
        markAsValid(lastKnownPartials());
    }
}
