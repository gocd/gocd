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

import com.thoughtworks.go.server.perf.SchedulingPerformanceLogger;
import com.thoughtworks.go.server.service.result.ServerHealthStateOperationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

public class ScheduleCheckListenerTest {
    private BuildCauseProducerService producer;
    private ScheduleCheckCompletedTopic checkCompletedTopic;
    private SchedulingPerformanceLogger schedulingPerformanceLogger;

    @BeforeEach
    public void setUp() {
        producer = mock(BuildCauseProducerService.class);
        checkCompletedTopic = mock(ScheduleCheckCompletedTopic.class);
        schedulingPerformanceLogger = mock(SchedulingPerformanceLogger.class);
    }

    @Test
    public void shouldCheckAndSendCheckCompletedMessage() {
        ScheduleCheckListener listener = new ScheduleCheckListener(checkCompletedTopic, producer, schedulingPerformanceLogger);

        listener.onMessage(new ScheduleCheckMessage("cruise", 123L));

        InOrder inOrder = inOrder(schedulingPerformanceLogger, producer, checkCompletedTopic);
        inOrder.verify(schedulingPerformanceLogger).pickedUpPipelineForScheduleCheck(123L, "cruise");
        inOrder.verify(producer).autoSchedulePipeline(eq("cruise"), any(ServerHealthStateOperationResult.class), eq(123L));
        inOrder.verify(checkCompletedTopic).post(new ScheduleCheckCompletedMessage("cruise", 123L));
        inOrder.verify(schedulingPerformanceLogger).postingMessageAboutScheduleCheckCompletion(123L, "cruise");
    }
}
