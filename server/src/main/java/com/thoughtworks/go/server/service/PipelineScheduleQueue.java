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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.NullPipeline;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.SchedulingContext;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.buildcause.BuildCauseOutOfDateException;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.Clock;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PipelineScheduleQueue {
    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineScheduleQueue.class);

    private PipelineService pipelineService;
    private TransactionTemplate transactionTemplate;
    private Map<CaseInsensitiveString, BuildCause> toBeScheduled = new ConcurrentHashMap<>();
    private Map<CaseInsensitiveString, BuildCause> mostRecentScheduled = new ConcurrentHashMap<>();
    private InstanceFactory instanceFactory;

    @Autowired
    public PipelineScheduleQueue(PipelineService pipelineService, TransactionTemplate transactionTemplate, InstanceFactory instanceFactory) {
        this.pipelineService = pipelineService;
        this.transactionTemplate = transactionTemplate;
        this.instanceFactory = instanceFactory;
    }

    public BuildCause mostRecentScheduled(CaseInsensitiveString pipelineName) {
        synchronized (mutexForPipelineName(pipelineName)) {
            BuildCause buildCause = mostRecentScheduled.get(pipelineName);
            if (buildCause != null) {
                return buildCause;
            }

            mostRecentScheduled.put(pipelineName, mostRecentScheduledBuildCause(pipelineName));
            return mostRecentScheduled.get(pipelineName);
        }
    }

    private BuildCause mostRecentScheduledBuildCause(CaseInsensitiveString pipelineName) {
        Pipeline pipeline = pipelineService.mostRecentFullPipelineByName(pipelineName.toString());
        return pipeline instanceof NullPipeline ? BuildCause.createNeverRun() : pipeline.getBuildCause();
    }

    public void schedule(CaseInsensitiveString pipelineName, BuildCause buildCause) {
        synchronized (mutexForPipelineName(pipelineName)) {
            BuildCause current = toBeScheduled.get(pipelineName);
            if (current == null || buildCause.trumps(current)) {
                toBeScheduled.put(pipelineName, buildCause);
            }
        }
    }

    public void cancelSchedule(CaseInsensitiveString pipelineName) {
        synchronized (mutexForPipelineName(pipelineName)) {
            toBeScheduled.remove(pipelineName);
        }
    }

    public synchronized Map<CaseInsensitiveString, BuildCause> toBeScheduled() {
        return new HashMap<>(toBeScheduled);
    }

    public void finishSchedule(CaseInsensitiveString pipelineName, BuildCause buildCause, BuildCause newCause) {
        synchronized (mutexForPipelineName(pipelineName)) {
            if (buildCause.equals(toBeScheduled.get(pipelineName))) {
                toBeScheduled.remove(pipelineName);
            }
            mostRecentScheduled.put(pipelineName, newCause);
        }
    }

    public void clearPipeline(CaseInsensitiveString pipelineName) {
        synchronized (mutexForPipelineName(pipelineName)) {
            toBeScheduled.remove(pipelineName);
            mostRecentScheduled.remove(pipelineName);
        }
    }

    //TODO: #5163 - this is a concurrency issue - talk to Rajesh or JJ
    public boolean hasBuildCause(CaseInsensitiveString pipelineName) {
        BuildCause buildCause = toBeScheduled.get(pipelineName);
        return buildCause != null && buildCause.getMaterialRevisions().totalNumberOfModifications() > 0;
    }

    public boolean hasForcedBuildCause(CaseInsensitiveString pipelineName) {
        synchronized (mutexForPipelineName(pipelineName)) {
            BuildCause buildCause = toBeScheduled.get(pipelineName);
            return buildCause != null && buildCause.isForced();
        }
    }

    @TestOnly
    public void clear() {
        mostRecentScheduled.clear();
        toBeScheduled.clear();
    }

    public Pipeline createPipeline(final BuildCause buildCause, final PipelineConfig pipelineConfig, final SchedulingContext context, final String md5, final Clock clock) {
        return (Pipeline) transactionTemplate.execute((TransactionCallback) status -> {
            Pipeline pipeline = null;

            if (shouldCancel(buildCause, pipelineConfig.name())) {
                LOGGER.debug("[Pipeline Schedule] Cancelling scheduling as build cause {} is the same as the most recent schedule", buildCause);
                cancelSchedule(pipelineConfig.name());
            } else {
                try {
                    Pipeline newPipeline = instanceFactory.createPipelineInstance(pipelineConfig, buildCause, context, md5, clock);
                    pipeline = pipelineService.save(newPipeline);
                    finishSchedule(pipelineConfig.name(), buildCause, pipeline.getBuildCause());
                    LOGGER.debug("[Pipeline Schedule] Successfully scheduled pipeline {}, buildCause:{}, configOrigin: {}", pipelineConfig.name(), buildCause, pipelineConfig.getOrigin());
                } catch (BuildCauseOutOfDateException e) {
                    cancelSchedule(pipelineConfig.name());
                    LOGGER.info("[Pipeline Schedule] Build cause {} is out of date. Scheduling is cancelled. Go will reschedule this pipeline. configOrigin: {}", buildCause, pipelineConfig.getOrigin());
                }
            }
            return pipeline;
        });
    }

    private boolean shouldCancel(BuildCause buildCause, CaseInsensitiveString pipelineName) {
        return !buildCause.isForced() && buildCause.isSameAs(mostRecentScheduled(pipelineName));
    }

    private String mutexForPipelineName(CaseInsensitiveString pipelineName) {
        return String.format("%s-%s", PipelineScheduleQueue.class.getName(), pipelineName.toLower()).intern();
    }

}
