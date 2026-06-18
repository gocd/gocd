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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.thoughtworks.go.domain.cctray.ProjectStatus.Key.keyFrom;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.stream.Collectors.toMap;

/* Understands how to cache CcTray statuses, for every stage and job (project). */
@Component
public class CcTrayCache {
    private static final Comparator<ProjectStatus> DISPLAY_SORT = Comparator
        .<ProjectStatus, String>comparing(p -> p.key().pipeline())
        .thenComparing(ProjectStatus::stageOrder)
        .thenComparing(p -> p.key().job(), nullsFirst(naturalOrder()));
    /**
     * Marker for a job name at the upper range, doesn't have to be valid within a job name.
     */
    private static final String MAX_JOB = "\uffff";

    /**
     * Assumption: Methods that hange this cache will always be called from the same thread (queueProcessor in CcTrayActivityListener).
     * Even get() is called only from that thread. So, not synchronized. Map uses a defined order for storage to allow us to
     * easily find chunks of the map via parts of the composite key.
     */
    private final NavigableMap<ProjectStatus.Key, ProjectStatus> byKey = new TreeMap<>(
        Comparator.comparing(ProjectStatus.Key::pipeline, nullsFirst(naturalOrder()))
            .thenComparing(ProjectStatus.Key::stage, nullsFirst(naturalOrder()))
            .thenComparing(ProjectStatus.Key::job, nullsFirst(naturalOrder()))
    );

    /**
     * List is swapped out for updates; and can be read by multiple threads.
     */
    private volatile List<ProjectStatus> orderedEntries = Collections.emptyList();

    public @Nullable ProjectStatus get(ProjectStatus.Key identifier) {
        return byKey.get(identifier);
    }

    public @NotNull ProjectStatus getOrDefault(ProjectStatus.Key identifier, int stageOrderId) {
        ProjectStatus projectStatus = get(identifier);
        return projectStatus != null ? projectStatus : new ProjectStatus.NullProjectStatus(identifier, stageOrderId);
    }

    public void put(ProjectStatus status) {
        this.byKey.put(status.key(), status);
        cacheHasChanged();
    }

    public void replaceForStage(String pipelineName, String stageName, List<ProjectStatus> statuses) {
        cacheStageView(pipelineName, stageName).clear();
        byKey.putAll(createReplacementItems(statuses));
        cacheHasChanged();
    }

    public void replaceForPipeline(String pipelineName, List<ProjectStatus> statuses) {
        cachePipelineView(pipelineName).clear();
        byKey.putAll(createReplacementItems(statuses));
        cacheHasChanged();
    }

    private NavigableMap<ProjectStatus.Key, ProjectStatus> cachePipelineView(String pipelineName) {
        return byKey.subMap(
            keyFrom(pipelineName, null, null), true,
            keyFrom(pipelineName, MAX_JOB, MAX_JOB), true
        );
    }

    void replaceAll(List<ProjectStatus> projectStatuses) {
        byKey.clear();
        byKey.putAll(createReplacementItems(projectStatuses));
        cacheHasChanged();
    }

    public List<ProjectStatus> allEntriesInOrder() {
        return this.orderedEntries;
    }

    private NavigableMap<ProjectStatus.Key, ProjectStatus> cacheStageView(String pipelineName, String stageName) {
        return byKey.subMap(
            keyFrom(pipelineName, stageName, null), true,
            keyFrom(pipelineName, stageName, MAX_JOB), true
        );
    }

    private void cacheHasChanged() {
        this.orderedEntries = byKey.values().stream().sorted(DISPLAY_SORT).toList();
    }

    private Map<ProjectStatus.Key, ProjectStatus> createReplacementItems(List<ProjectStatus> statuses) {
        return statuses.stream()
            .collect(toMap(ProjectStatus::key, s -> s, (a, b) -> b, LinkedHashMap::new));
    }
}
