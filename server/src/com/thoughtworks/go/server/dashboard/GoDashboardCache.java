/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineModel;
import org.springframework.stereotype.Component;

import java.util.*;

/* Understands how to cache dashboard statuses, for every pipeline. */
@Component
public class GoDashboardCache {
    /**
     * Assumption: The put() and replaceAllEntriesInCacheWith() methods, which change this cache,
     * will always be called from the same thread (queueProcessor in GoDashboardActivityListener). Even get() will be.
     * So, not surrounding it with a synchronizedMap. Also, uses {@link LinkedHashMap} to preserve insertion order. That
     * order is not very important in this case, but it comes for free (almost) because of the map.
     */
    private LinkedHashMap<CaseInsensitiveString, GoDashboardPipeline> cache;
    private volatile List<GoDashboardPipeline> orderedEntries;

    public GoDashboardCache() {
        cache = new LinkedHashMap<>();
        orderedEntries = new ArrayList<>();
    }

    public GoDashboardPipeline get(CaseInsensitiveString pipelineName) {
        return cache.get(pipelineName);
    }

    public void put(GoDashboardPipeline pipeline) {
        cache.put(pipeline.name(), pipeline);
        cacheHasChanged();
    }

    public void replaceAllEntriesInCacheWith(List<GoDashboardPipeline> newPipelinesToCache) {
        cache.clear();
        cache.putAll(createMapFor(newPipelinesToCache));
        cacheHasChanged();
    }

    public List<GoDashboardPipeline> allEntriesInOrder() {
        return orderedEntries;
    }

    private void cacheHasChanged() {
        orderedEntries = new ArrayList<>(cache.values());
    }

    private Map<CaseInsensitiveString, GoDashboardPipeline> createMapFor(List<GoDashboardPipeline> pipelines) {
        Map<CaseInsensitiveString, GoDashboardPipeline> result = new LinkedHashMap<>();
        for (GoDashboardPipeline pipeline : pipelines) {
            result.put(pipeline.name(), pipeline);
        }
        return result;
    }
}
