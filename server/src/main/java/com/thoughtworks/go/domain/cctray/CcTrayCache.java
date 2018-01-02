/*************************GO-LICENSE-START*********************************
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.domain.cctray;

import com.thoughtworks.go.domain.activity.ProjectStatus;
import org.springframework.stereotype.Component;

import java.util.*;

/* Understands how to cache CcTray statuses, for every stage and job (project). */
@Component
public class CcTrayCache {
    /**
     * Assumption: The put(), putAll() and replaceAllEntriesInCacheWith() methods, which change this cache,
     * will always be called from the same thread (queueProcessor in CcTrayActivityListener). Even get() is
     * called only from that thread. So, not surrounding it with a synchronizedMap. Also, uses {@link LinkedHashMap}
     * to preserve insertion order.
     */
    private LinkedHashMap<String, ProjectStatus> cache;
    private volatile List<ProjectStatus> orderedEntries;

    public CcTrayCache() {
        this.cache = new LinkedHashMap<>();
        this.orderedEntries = new ArrayList<>();
    }

    public ProjectStatus get(String projectName) {
        return cache.get(projectName);
    }

    public void put(ProjectStatus status) {
        this.cache.put(status.name(), status);
        cacheHasChanged();
    }

    public void putAll(List<ProjectStatus> statuses) {
        cache.putAll(createReplacementItems(statuses));
        cacheHasChanged();
    }

    public void replaceAllEntriesInCacheWith(List<ProjectStatus> projectStatuses) {
        this.cache.clear();
        this.cache.putAll(createReplacementItems(projectStatuses));
        cacheHasChanged();
    }

    public List<ProjectStatus> allEntriesInOrder() {
        return this.orderedEntries;
    }

    private void cacheHasChanged() {
        this.orderedEntries = new ArrayList<>(cache.values());
    }

    private Map<String, ProjectStatus> createReplacementItems(List<ProjectStatus> statuses) {
        Map<String, ProjectStatus> replacementItems = new LinkedHashMap<>();
        for (ProjectStatus status : statuses) {
            replacementItems.put(status.name(), status);
        }
        return replacementItems;
    }
}
