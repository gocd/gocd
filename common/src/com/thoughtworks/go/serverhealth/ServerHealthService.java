/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.serverhealth;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.thoughtworks.go.config.CruiseConfig;
import org.springframework.stereotype.Service;

@Service
public class ServerHealthService {
    private Map<HealthStateType, ServerHealthState> serverHealth;

    public ServerHealthService() {
        this.serverHealth = new ConcurrentHashMap<HealthStateType, ServerHealthState>();
    }

    public void removeByScope(HealthStateScope scope) {
        for (HealthStateType healthStateType : entryKeys()) {
            if (healthStateType.isSameScope(scope)) {
                serverHealth.remove(healthStateType);
            }
        }
    }

    private Set<HealthStateType> entryKeys() {
        return new HashSet<HealthStateType>(serverHealth.keySet());
    }

    public List<ServerHealthState> filterByScope(HealthStateScope scope) {
        List<ServerHealthState> filtered = new ArrayList<ServerHealthState>();
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
        for (Map.Entry<HealthStateType, ServerHealthState> entry : new HashSet<Map.Entry<HealthStateType, ServerHealthState>>(serverHealth.entrySet())) {
            ServerHealthState value = entry.getValue();
            if (value.hasExpired()) {
                serverHealth.remove(entry.getKey());
            }
        }
    }

    private void removeByScope(HealthStateType type) {
        removeByScope(type.getScope());
    }

    private ServerHealthStates logs() {
        ArrayList<ServerHealthState> logs = new ArrayList<ServerHealthState>();
        for (Map.Entry<HealthStateType, ServerHealthState> entry : sortedEntries()) {
            logs.add(entry.getValue());
        }
        return new ServerHealthStates(logs);
    }

    private List<Map.Entry<HealthStateType, ServerHealthState>> sortedEntries() {
        List<Map.Entry<HealthStateType, ServerHealthState>> entries = new ArrayList<Map.Entry<HealthStateType, ServerHealthState>>(serverHealth.entrySet());
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
