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
package com.thoughtworks.go.server.dashboard;

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/* Understands how to cache dashboard statuses, for every pipeline. */
@Component
public class GoDashboardCache {
    private final TimeStampBasedCounter timeStampBasedCounter;
    /**
     * Assumption: The put() and replaceAllEntriesInCacheWith() methods, which change this cache,
     * will always be called from the same thread (queueProcessor in GoDashboardActivityListener). Even get() will be.
     * So, not surrounding it with a synchronizedMap. Also, uses {@link LinkedHashMap} to preserve insertion order. That
     * order is not very important in this case, but it comes for free (almost) because of the map.
     */
    private LinkedHashMap<CaseInsensitiveString, GoDashboardPipeline> cache;
    private volatile GoDashboardPipelines dashboardPipelines;

    @Autowired
    public GoDashboardCache(TimeStampBasedCounter timeStampBasedCounter) {
        this.timeStampBasedCounter = timeStampBasedCounter;
        cache = new LinkedHashMap<>();
        dashboardPipelines = new GoDashboardPipelines(new HashMap<>(), timeStampBasedCounter);
    }

    public void put(GoDashboardPipeline pipeline) {
        cache.put(pipeline.name(), pipeline);
        cacheHasChanged();
    }

    public void remove(CaseInsensitiveString pipelineName) {
        cache.remove(pipelineName);
    }

    public void replaceAllEntriesInCacheWith(List<GoDashboardPipeline> newPipelinesToCache) {
        cache.clear();
        cache.putAll(createMapFor(newPipelinesToCache));
        cacheHasChanged();
    }

    public GoDashboardPipelines allEntries() {
        return dashboardPipelines;
    }

    private void cacheHasChanged() {
        dashboardPipelines = new GoDashboardPipelines(new HashMap<>(cache), timeStampBasedCounter);
    }

    private Map<CaseInsensitiveString, GoDashboardPipeline> createMapFor(List<GoDashboardPipeline> pipelines) {
        Map<CaseInsensitiveString, GoDashboardPipeline> result = new LinkedHashMap<>();
        for (GoDashboardPipeline pipeline : pipelines) {
            result.put(pipeline.name(), pipeline);
        }
        return result;
    }
}
