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
package com.thoughtworks.go.server.scheduling;

import com.thoughtworks.go.server.messaging.GoMessageListener;
import com.thoughtworks.go.server.perf.SchedulingPerformanceLogger;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScheduleCheckListener implements GoMessageListener<ScheduleCheckMessage> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScheduleCheckListener.class);
    private BuildCauseProducerService producer;
    private ScheduleCheckCompletedTopic checkCompletedTopic;
    private SchedulingPerformanceLogger schedulingPerformanceLogger;

    public ScheduleCheckListener(ScheduleCheckCompletedTopic checkCompletedTopic,
                                 BuildCauseProducerService producer,
                                 SchedulingPerformanceLogger schedulingPerformanceLogger) {
        this.producer = producer;
        this.checkCompletedTopic = checkCompletedTopic;
        this.schedulingPerformanceLogger = schedulingPerformanceLogger;
    }

    @Override
    public void onMessage(ScheduleCheckMessage message) {
        LOGGER.debug("ScheduleCheckMessage for {} is received", message.getPipelineName());
        try {
            schedulingPerformanceLogger.pickedUpPipelineForScheduleCheck(message.trackingId(), message.getPipelineName());

            producer.autoSchedulePipeline(message.getPipelineName(), new ServerHealthStateOperationResult(), message.trackingId());
            LOGGER.debug("Finished checking for pipeline {}", message.getPipelineName());
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            checkCompletedTopic.post(new ScheduleCheckCompletedMessage(message.getPipelineName(), message.trackingId()));

            schedulingPerformanceLogger.postingMessageAboutScheduleCheckCompletion(message.trackingId(), message.getPipelineName());
            LOGGER.debug("Finished posting materialCheckCompletedMessage for pipeline {}", message.getPipelineName());
        }

    }
}
