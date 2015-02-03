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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CcTrayCache {
    private ConcurrentHashMap<String, ProjectStatus> cache;
    private List<ProjectStatus> allEntries;

    public CcTrayCache() {
        /* Ideally concurrencyLevel should be set to 1, here. Leaving it at default (16). */
        this.cache = new ConcurrentHashMap<String, ProjectStatus>();
        this.allEntries = new ArrayList<ProjectStatus>();
    }

    public ProjectStatus get(String projectName) {
        return cache.get(projectName);
    }

    public void put(ProjectStatus status) {
        this.cache.put(status.name(), status);
    }

    public void putAll(List<ProjectStatus> statuses) {
        cache.putAll(createReplacementItems(statuses));
    }

    /* clear() + putAll() do not guarantee atomicity. A call to get() during this time will fail to find an item.
     * Considered using a volatile and replacing the whole cache. Not doing it now. Will be based on need. */
    public void replaceAllEntriesInCacheWith(List<ProjectStatus> projectStatuses) {
        this.cache.clear();
        this.cache.putAll(createReplacementItems(projectStatuses));
        this.allEntries = projectStatuses;
    }

    private HashMap<String, ProjectStatus> createReplacementItems(List<ProjectStatus> statuses) {
        HashMap<String, ProjectStatus> replacementItems = new HashMap<String, ProjectStatus>();
        for (ProjectStatus status : statuses) {
            replacementItems.put(status.name(), status);
        }
        return replacementItems;
    }

    public List<ProjectStatus> allEntriesInOrder() {
        return allEntries;
    }
}
