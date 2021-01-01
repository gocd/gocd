/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.sweepers;

import java.util.TreeSet;

import com.thoughtworks.go.domain.PipelineTimelineEntry;
import com.thoughtworks.go.listener.TimelineUpdateListener;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.ui.ViewCacheKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @understands cleaning up fbh frag cache in case of bisect scenario
 */
@Component
public class FailedBuildHistoryCacheSweeper implements TimelineUpdateListener {
    private final GoCache goCache;
    private final ViewCacheKey key = new ViewCacheKey();

    @Autowired
    public FailedBuildHistoryCacheSweeper(GoCache goCache) {
        this.goCache = goCache;
    }

    @Override
    public void added(PipelineTimelineEntry newlyAddedEntry, TreeSet<PipelineTimelineEntry> timeline) {
        for (PipelineTimelineEntry pipelineTimelineEntry : timeline.tailSet(newlyAddedEntry)) {
            goCache.remove(key.forFbhOfStagesUnderPipeline(pipelineTimelineEntry.getPipelineLocator()));
        }
    }
}
