/*
 * Copyright 2024 Thoughtworks, Inc.
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
import org.jetbrains.annotations.TestOnly;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Service
public class ServerHealthService implements ApplicationContextAware {
    private final ConcurrentMap<HealthStateType, ServerHealthState> serverHealth = new ConcurrentHashMap<>();
    private ApplicationContext applicationContext;

    public List<ServerHealthState> logsSortedForScope(HealthStateScope scope) {
        return serverHealth.entrySet().stream()
            .filter(entry -> entry.getKey().isSameScope(scope))
            .sorted(Map.Entry.comparingByKey())
            .map(Map.Entry::getValue)
            .collect(toList());
    }

    public ServerHealthStates logsSorted() {
        return new ServerHealthStates(sortedEntries().map(Map.Entry::getValue).collect(toList()));
    }

    public HealthStateType update(ServerHealthState serverHealthState) {
        HealthStateType type = serverHealthState.getType();
        if (serverHealthState.getLogLevel() == HealthStateLevel.OK) {
            serverHealth.remove(type);
            return null;
        } else {
            serverHealth.put(type, serverHealthState);
            return type;
        }
    }

    // called from spring timer
    public synchronized void onTimer() {
        CruiseConfig currentConfig = applicationContext.getBean(CruiseConfigProvider.class).getCurrentConfig();
        purgeStaleHealthMessages(currentConfig);
    }

    void purgeStaleHealthMessages(CruiseConfig cruiseConfig) {
        removeMessagesForElementsNoLongerInConfig(cruiseConfig);
        removeExpiredMessages();
    }

    @TestOnly // Remove once we get rid of SpringJUnitTestRunner
    public void removeAllLogs() {
        serverHealth.clear();
    }

    public void removeByScope(HealthStateScope scope) {
       removeByScopeMatcher(scope::equals);
    }

    public void removeByScopeMatcher(Predicate<HealthStateScope> matcher) {
        serverHealth.keySet().removeIf(healthStateType -> matcher.test(healthStateType.getScope()));
    }

    private void removeMessagesForElementsNoLongerInConfig(CruiseConfig cruiseConfig) {
        serverHealth.keySet().removeIf(healthStateType -> healthStateType.isRemovedFromConfig(cruiseConfig));
    }

    private void removeExpiredMessages() {
        serverHealth.entrySet().removeIf(typeHealthEntry -> typeHealthEntry.getValue().hasExpired());
    }

    private Stream<Map.Entry<HealthStateType, ServerHealthState>> sortedEntries() {
        return serverHealth.entrySet().stream().sorted(Map.Entry.comparingByKey());
    }

    public boolean containsError(HealthStateType type, HealthStateLevel level) {
        return serverHealth.values().stream().anyMatch(state -> state.getType().equals(type) && state.getLogLevel() == level);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
