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
package com.thoughtworks.go.server.perf;

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SchedulingPerformanceLogger {
    private PerformanceLogger performanceLogger;
    private long currentTrackingId = 0;

    @Autowired
    public SchedulingPerformanceLogger(PerformanceLogger performanceLogger) {
        this.performanceLogger = performanceLogger;
    }

    public long pipelineSentToScheduleCheckQueue(String pipelineName) {
        long trackingId = currentTrackingId++;
        performanceLogger.log("SCH-CHECK-QUEUE-PUT {} {}", trackingId, pipelineName);
        return trackingId;
    }

    public void pickedUpPipelineForScheduleCheck(long trackingId, String pipelineName) {
        performanceLogger.log("SCH-CHECK-START {} {}", trackingId, pipelineName);
    }

    public void autoSchedulePipelineStart(long trackingId, String pipelineName) {
        performanceLogger.log("SCH-AUTO-START {} {}", trackingId, pipelineName);
    }

    public void autoSchedulePipelineFinish(long trackingId, String pipelineName) {
        performanceLogger.log("SCH-AUTO-DONE {} {}", trackingId, pipelineName);
    }

    public void postingMessageAboutScheduleCheckCompletion(long trackingId, String pipelineName) {
        performanceLogger.log("SCH-CHECK-DONE {} {}", trackingId, pipelineName);
    }

    public void completionMessageForScheduleCheckReceived(long trackingId, String pipelineName) {
        performanceLogger.log("SCH-CHECK-QUEUE-REMOVE {} {}", trackingId, pipelineName);
    }

    public long manualSchedulePipelineStart(String pipelineName) {
        long trackingId = currentTrackingId++;
        performanceLogger.log("SCH-MANUAL-START {} {}", trackingId, pipelineName);
        return trackingId;
    }

    public void manualSchedulePipelineFinish(long trackingId, String pipelineName) {
        performanceLogger.log("SCH-MANUAL-DONE {} {}", trackingId, pipelineName);
    }

    public long timerSchedulePipelineStart(String pipelineName) {
        long trackingId = currentTrackingId++;
        performanceLogger.log("SCH-TIMER-START {} {}", trackingId, pipelineName);
        return trackingId;
    }

    public void timerSchedulePipelineFinish(long trackingId, String pipelineName) {
        performanceLogger.log("SCH-TIMER-DONE {} {}", trackingId, pipelineName);
    }

    public void sendingPipelineToTheToBeScheduledQueue(long trackingId, String pipelineName) {
        performanceLogger.log("SCH-TO-BE-SCHEDULED-QUEUE-PUT {} {}", trackingId, pipelineName);
    }

    public void scheduledPipeline(CaseInsensitiveString pipelineName, int toBeScheduledQueueSize, long schedulePipelineStartTime, long schedulePipelineEndTime) {
        performanceLogger.log("SCH-SCHEDULED {} {} {} {}", pipelineName, toBeScheduledQueueSize, schedulePipelineStartTime, schedulePipelineEndTime);
    }
}
