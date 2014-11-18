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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.NullPipeline;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.SchedulingContext;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.buildcause.BuildCauseOutOfDateException;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.Pair;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PipelineScheduleQueue {
    private static final Logger LOGGER = Logger.getLogger(PipelineScheduleQueue.class);

    private PipelineService pipelineService;
    private TransactionTemplate transactionTemplate;
    private Map<String, Pair<Long,BuildCause>> toBeScheduled = new ConcurrentHashMap<String, Pair<Long,BuildCause>>();
    private Map<String, BuildCause> mostRecentScheduled = new ConcurrentHashMap<String, BuildCause>();
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

    public void schedule(String pipelineName, BuildCause buildCause, long trackingId) {
        synchronized (mutexForPipelineName(pipelineName)) {
            Pair<Long,BuildCause> currentCauseWithTrackingId = toBeScheduled.get(pipelineName);
            if (currentCauseWithTrackingId == null || currentCauseWithTrackingId.last() == null || buildCause.trumps(currentCauseWithTrackingId.last())) {
                toBeScheduled.put(pipelineName, new Pair<Long, BuildCause>(trackingId, buildCause));
            }
        }
    }

    public void cancelSchedule(String pipelineName) {
        synchronized (mutexForPipelineName(pipelineName)) {
            toBeScheduled.remove(pipelineName);
        }
    }

    public synchronized Map<String, Pair<Long,BuildCause>> toBeScheduled() {
        return new HashMap<String, Pair<Long,BuildCause>>(toBeScheduled);
    }

    public void finishSchedule(String pipelineName, BuildCause buildCause, BuildCause newCause) {
        synchronized (mutexForPipelineName(pipelineName)) {
            Pair<Long, BuildCause> buildCauseWithTrackingId = toBeScheduled.get(pipelineName);
            if (buildCause.equals(buildCauseWithTrackingId.last())) {
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
        Pair<Long,BuildCause> buildCauseWithTrackingId = toBeScheduled.get(pipelineName);
        return buildCauseWithTrackingId != null && buildCauseWithTrackingId.last() != null &&
                buildCauseWithTrackingId.last().getMaterialRevisions().totalNumberOfModifications() > 0;
    }

    public boolean hasForcedBuildCause(String pipelineName) {
        synchronized (mutexForPipelineName(pipelineName)) {
            Pair<Long,BuildCause> buildCauseWithTrackingId = toBeScheduled.get(pipelineName);
            return buildCauseWithTrackingId != null && buildCauseWithTrackingId.last() != null && buildCauseWithTrackingId.last().isForced();
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
                            LOGGER.debug(String.format("[Pipeline Schedule] Successfully scheduled pipeline %s, buildCause:%s", pipelineName, buildCause));
                        }
                    } catch (BuildCauseOutOfDateException e) {
                        cancelSchedule(pipelineName);
                        LOGGER.info(String.format("[Pipeline Schedule] Build cause %s is out of date. Scheduling is cancelled. Go will reschedule this pipeline.", buildCause));
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
