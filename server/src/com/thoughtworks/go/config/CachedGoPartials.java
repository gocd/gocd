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
import org.joda.time.DateTime;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@Component
public class CachedGoPartials {
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
        if (fingerprintToLatestValidConfigMap.containsKey(fingerprint)) {
            return fingerprintToLatestValidConfigMap.get(fingerprint).partialConfig;
        }
        return null;
    }

    public void removeValid(String fingerprint) {
        if (fingerprintToLatestValidConfigMap.containsKey(fingerprint)) {
            fingerprintToLatestValidConfigMap.remove(fingerprint);
        }
    }


    public void addOrUpdate(String fingerprint, PartialConfig newPart) {
        fingerprintToLatestKnownConfigMap.put(fingerprint, new UpdatedPartial(newPart, new DateTime()));
    }

    public void markAsValid(String fingerprint, PartialConfig newPart) {
        DateTime lastUpdated = fingerprintToLatestKnownConfigMap.get(fingerprint).lastUpdated;
        fingerprintToLatestValidConfigMap.put(fingerprint, new UpdatedPartial(newPart, lastUpdated));
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

    public boolean areAllKnownPartialsValid() {
        return fingerprintToLatestValidConfigMap.equals(fingerprintToLatestKnownConfigMap);
    }

    public void markAllKnownAsValid() {
        for (Map.Entry<String, UpdatedPartial> entry : fingerprintToLatestKnownConfigMap.entrySet()) {
            fingerprintToLatestValidConfigMap.put(entry.getKey(), entry.getValue());
        }
    }
}
