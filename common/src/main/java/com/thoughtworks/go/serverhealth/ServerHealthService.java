/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.serverhealth;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.CruiseConfigProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ServerHealthService {
    private static final Logger LOG = LoggerFactory.getLogger(ServerHealthService.class);

    private HashMap<ServerHealthState, Set<String>> pipelinesWithErrors;
    private Map<HealthStateType, ServerHealthState> serverHealth;

    public ServerHealthService() {
        this.serverHealth = new ConcurrentHashMap<>();
        this.pipelinesWithErrors = new HashMap<>();
    }

    public void removeByScope(HealthStateScope scope) {
        for (HealthStateType healthStateType : entryKeys()) {
            if (healthStateType.isSameScope(scope)) {
                serverHealth.remove(healthStateType);
            }
        }
    }

    private Set<HealthStateType> entryKeys() {
        return new HashSet<>(serverHealth.keySet());
    }

    public List<ServerHealthState> filterByScope(HealthStateScope scope) {
        List<ServerHealthState> filtered = new ArrayList<>();
        for (Map.Entry<HealthStateType, ServerHealthState> entry : sortedEntries()) {
            HealthStateType type = entry.getKey();
            if (type.isSameScope(scope)) {
                filtered.add(entry.getValue());
            }
        }
        return filtered;
    }

    public HealthStateType update(ServerHealthState serverHealthState) {
        HealthStateType type = serverHealthState.getType();
        if (serverHealthState.getLogLevel() == HealthStateLevel.OK) {
            if (serverHealth.containsKey(type)) {
                serverHealth.remove(type);
            }
            return null;
        } else {
            serverHealth.put(type, serverHealthState);
            return type;
        }
    }

    // called from spring timer
    public synchronized void onTimer(CruiseConfigProvider provider) {
        CruiseConfig currentConfig = provider.getCurrentConfig();
        getAllValidLogs(currentConfig);
        LOG.debug("Recomputing material to pipeline mappings.");

        HashMap<ServerHealthState, Set<String>> erroredPipelines = new HashMap<>();

        for (Map.Entry<HealthStateType, ServerHealthState> entry : serverHealth.entrySet()) {
            erroredPipelines.put(entry.getValue(), entry.getValue().getPipelineNames(currentConfig));
        }
        pipelinesWithErrors = erroredPipelines;
        LOG.debug("Done recomputing material to pipeline mappings.");
    }

    public Set<String> getPipelinesWithErrors(ServerHealthState serverHealthState) {
        return pipelinesWithErrors.get(serverHealthState);
    }

    public ServerHealthStates getAllValidLogs(CruiseConfig cruiseConfig) {
        removeMessagesForElementsNoLongerInConfig(cruiseConfig);
        removeExpiredMessages();
        return logs();
    }

    public ServerHealthStates getAllLogs() {
        return logs();
    }

    @Deprecated // Remove once we get rid of SpringJUnitTestRunner
    public void removeAllLogs() {
        serverHealth.clear();
    }

    private void removeMessagesForElementsNoLongerInConfig(CruiseConfig cruiseConfig) {
        for (HealthStateType type : entryKeys()) {
            if (type.isRemovedFromConfig(cruiseConfig)) {
                this.removeByScope(type);
            }
        }
    }

    private void removeExpiredMessages() {
        for (Map.Entry<HealthStateType, ServerHealthState> entry : new HashSet<>(serverHealth.entrySet())) {
            ServerHealthState value = entry.getValue();
            if (value.hasExpired()) {
                serverHealth.remove(entry.getKey());
            }
        }
    }

    private void removeByScope(HealthStateType type) {
        removeByScope(type.getScope());
    }

    public ServerHealthStates logs() {
        ArrayList<ServerHealthState> logs = new ArrayList<>();
        for (Map.Entry<HealthStateType, ServerHealthState> entry : sortedEntries()) {
            logs.add(entry.getValue());
        }
        return new ServerHealthStates(logs);
    }

    private List<Map.Entry<HealthStateType, ServerHealthState>> sortedEntries() {
        List<Map.Entry<HealthStateType, ServerHealthState>> entries = new ArrayList<>(serverHealth.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<HealthStateType, ServerHealthState>>() {
            public int compare(Map.Entry<HealthStateType, ServerHealthState> one, Map.Entry<HealthStateType, ServerHealthState> other) {
                return one.getKey().compareTo(other.getKey());
            }
        });
        return entries;
    }

    public String getLogsAsText() {
        StringBuilder text = new StringBuilder();
        for (ServerHealthState state : logs()) {
            text.append(state.getDescription());
            text.append("\n\t");
            text.append(state.getMessage());
            text.append("\n");
        }
        return text.toString();
    }

    public boolean containsError(HealthStateType type, HealthStateLevel level) {
        ServerHealthStates allLogs = getAllLogs();
        for (ServerHealthState log : allLogs) {
            if (log.getType().equals(type) && log.getLogLevel() == level) {
                return true;
            }
        }
        return false;
    }
}
