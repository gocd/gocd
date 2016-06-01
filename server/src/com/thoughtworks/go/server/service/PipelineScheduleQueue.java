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

package com.thoughtworks.go.server.service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.NullPipeline;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.SchedulingContext;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.buildcause.BuildCauseOutOfDateException;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.Clock;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

@Component
public class PipelineScheduleQueue {
    private static final Logger LOGGER = Logger.getLogger(PipelineScheduleQueue.class);

    private PipelineService pipelineService;
    private TransactionTemplate transactionTemplate;
    private Map<String, BuildCause> toBeScheduled = new ConcurrentHashMap<>();
    private Map<String, BuildCause> mostRecentScheduled = new ConcurrentHashMap<>();
    private InstanceFactory instanceFactory;

    @Autowired
    public PipelineScheduleQueue(PipelineService pipelineService, TransactionTemplate transactionTemplate, InstanceFactory instanceFactory) {
        this.pipelineService = pipelineService;
        this.transactionTemplate = transactionTemplate;
        this.instanceFactory = instanceFactory;
    }

    public BuildCause mostRecentScheduled(String pipelineName) {
        synchronized (mutexForPipelineName(pipelineName)) {
            BuildCause buildCause = mostRecentScheduled.get(pipelineName);
            if (buildCause != null) {
                return buildCause;
            }

            mostRecentScheduled.put(pipelineName, mostRecentScheduledBuildCause(pipelineName));
            return mostRecentScheduled.get(pipelineName);
        }
    }

    private BuildCause mostRecentScheduledBuildCause(String pipelineName) {
        Pipeline pipeline = pipelineService.mostRecentFullPipelineByName(pipelineName);
        return pipeline instanceof NullPipeline ? BuildCause.createNeverRun() : pipeline.getBuildCause();
    }

    public void schedule(String pipelineName, BuildCause buildCause) {
        synchronized (mutexForPipelineName(pipelineName)) {
            BuildCause current = toBeScheduled.get(pipelineName);
            if (current == null || buildCause.trumps(current)) {
                toBeScheduled.put(pipelineName, buildCause);
            }
        }
    }

    public void cancelSchedule(String pipelineName) {
        synchronized (mutexForPipelineName(pipelineName)) {
            toBeScheduled.remove(pipelineName);
        }
    }

    public synchronized Map<String, BuildCause> toBeScheduled() {
        return new HashMap<>(toBeScheduled);
    }

    public void finishSchedule(String pipelineName, BuildCause buildCause, BuildCause newCause) {
        synchronized (mutexForPipelineName(pipelineName)) {
            if (buildCause.equals(toBeScheduled.get(pipelineName))) {
                toBeScheduled.remove(pipelineName);
            }
            mostRecentScheduled.put(pipelineName, newCause);
        }
    }

    public void clearPipeline(String pipelineName) {
        synchronized (mutexForPipelineName(pipelineName)) {
            toBeScheduled.remove(pipelineName);
            mostRecentScheduled.remove(pipelineName);
        }
    }

    //TODO: #5163 - this is a concurrency issue - talk to Rajesh or JJ
    public boolean hasBuildCause(String pipelineName) {
        BuildCause buildCause = toBeScheduled.get(pipelineName);
        return buildCause != null && buildCause.getMaterialRevisions().totalNumberOfModifications() > 0;
    }

    public boolean hasForcedBuildCause(String pipelineName) {
        synchronized (mutexForPipelineName(pipelineName)) {
            BuildCause buildCause = toBeScheduled.get(pipelineName);
            return buildCause != null && buildCause.isForced();
        }
    }

    /**
     * @deprecated Only for test
     */
    public void clear() {
        mostRecentScheduled.clear();
        toBeScheduled.clear();
    }

    public Pipeline createPipeline(final BuildCause buildCause, final PipelineConfig pipelineConfig, final SchedulingContext context, final String md5, final Clock clock) {
        return (Pipeline) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                String pipelineName = CaseInsensitiveString.str(pipelineConfig.name());
                Pipeline pipeline = null;

                if (shouldCancel(buildCause, pipelineName)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug(String.format("[Pipeline Schedule] Cancelling scheduling as build cause %s is the same as the most recent schedule", buildCause));
                    }
                    cancelSchedule(pipelineName);
                } else {
                    try {
                        Pipeline newPipeline = instanceFactory.createPipelineInstance(pipelineConfig, buildCause, context, md5, clock);
                        pipeline = pipelineService.save(newPipeline);
                        finishSchedule(pipelineName, buildCause, pipeline.getBuildCause());
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(String.format("[Pipeline Schedule] Successfully scheduled pipeline %s, buildCause:%s, configOrigin: %s",
                                    pipelineName, buildCause,pipelineConfig.getOrigin()));
                        }
                    } catch (BuildCauseOutOfDateException e) {
                        cancelSchedule(pipelineName);
                        LOGGER.info(String.format("[Pipeline Schedule] Build cause %s is out of date. Scheduling is cancelled. Go will reschedule this pipeline. configOrigin: %s",
                                buildCause, pipelineConfig.getOrigin()));
                    }
                }
                return pipeline;
            }
        });
    }

    private boolean shouldCancel(BuildCause buildCause, String pipelineName) {
        return !buildCause.isForced() && buildCause.isSameAs(mostRecentScheduled(pipelineName));
    }

    private String mutexForPipelineName(String pipelineName) {
        return String.format("%s-%s", PipelineScheduleQueue.class.getName(), pipelineName).intern();
    }

}
