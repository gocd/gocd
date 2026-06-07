/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.config.security.users.Users;
import org.springframework.stereotype.Component;

import java.util.*;

import static java.util.stream.Collectors.toMap;

/* Understands how to cache CcTray statuses, for every stage and job (project). */
@Component
public class CcTrayCache {
    /**
     * Assumption: The put(), putAll() and replaceAllEntriesInCacheWith() methods, which change this cache,
     * will always be called from the same thread (queueProcessor in CcTrayActivityListener). Even get() is
     * called only from that thread. So, not surrounding it with a synchronizedMap. Also, uses {@link LinkedHashMap}
     * to preserve insertion order.
     */
    private final SequencedMap<String, ProjectStatus> byProjectName = new LinkedHashMap<>();
    private volatile List<ProjectStatus> orderedEntries = Collections.emptyList();

    ProjectStatus get(String projectName) {
        return byProjectName.get(projectName);
    }

    public void put(ProjectStatus status) {
        this.byProjectName.put(status.name(), status);
        cacheHasChanged();
    }

    public void putAll(List<ProjectStatus> statuses) {
        byProjectName.putAll(createReplacementItems(statuses));
        cacheHasChanged();
    }

    void replaceAllEntriesInCacheWith(List<ProjectStatus> projectStatuses) {
        this.byProjectName.clear();
        this.byProjectName.putAll(createReplacementItems(projectStatuses));
        cacheHasChanged();
    }

    public List<ProjectStatus> allEntriesInOrder() {
        return this.orderedEntries;
    }

    private void cacheHasChanged() {
        this.orderedEntries = List.copyOf(byProjectName.values());
    }

    private Map<String, ProjectStatus> createReplacementItems(List<ProjectStatus> statuses) {
        return statuses.stream()
            .filter(s -> !Users.NOONE.equals(s.viewers())) // TODO this isn't correct to do - what if security disabled?
            .collect(toMap(ProjectStatus::name, s -> s, (a, b) -> b, LinkedHashMap::new));
    }
}
