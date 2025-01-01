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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.helper.PipelineMother;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PipelineScheduleQueueTest {

    private PipelineScheduleQueue pipelineScheduleQueue;

    @Mock
    private PipelineService pipelineService;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private InstanceFactory instanceFactory;

    @BeforeEach
    public void setUp() throws Exception {
        pipelineScheduleQueue = new PipelineScheduleQueue(pipelineService, transactionTemplate, instanceFactory);
    }

    @Test
    public void shouldConsiderPipelineNameToBeCaseInsensitiveWhileScheduling() {
        CaseInsensitiveString pipelineName = new CaseInsensitiveString("PipelinE");
        pipelineScheduleQueue.schedule(pipelineName, BuildCause.createWithModifications(new MaterialRevisions(), "u1"));
        pipelineScheduleQueue.schedule(new CaseInsensitiveString(pipelineName.toLower()), BuildCause.createWithModifications(new MaterialRevisions(), "u2"));
        pipelineScheduleQueue.schedule(new CaseInsensitiveString(pipelineName.toUpper()), BuildCause.createWithModifications(new MaterialRevisions(), "u3"));
        assertThat(pipelineScheduleQueue.toBeScheduled().get(pipelineName)).isEqualTo(BuildCause.createWithModifications(new MaterialRevisions(), "u1"));
        assertThat(pipelineScheduleQueue.toBeScheduled().size()).isEqualTo(1);
    }

    @Test
    public void shouldConsiderPipelineNameToBeCaseInsensitiveWhileCancelingAScheduledBuild() {
        CaseInsensitiveString pipelineName = new CaseInsensitiveString("PipelinE");
        pipelineScheduleQueue.schedule(pipelineName, BuildCause.createManualForced());
        pipelineScheduleQueue.cancelSchedule(new CaseInsensitiveString(pipelineName.toUpper()));
        assertTrue(pipelineScheduleQueue.toBeScheduled().isEmpty());

        pipelineScheduleQueue.schedule(pipelineName, BuildCause.createManualForced());
        pipelineScheduleQueue.cancelSchedule(new CaseInsensitiveString(pipelineName.toLower()));
        assertTrue(pipelineScheduleQueue.toBeScheduled().isEmpty());
    }

    @Test
    public void shouldConsiderPipelineNameToBeCaseInsensitive_FinishSchedule() {
        CaseInsensitiveString pipelineName = new CaseInsensitiveString("PipelinE");
        BuildCause newBuildCause = BuildCause.createManualForced(new MaterialRevisions(), new Username("u1"));
        BuildCause originalBuildCause = BuildCause.createManualForced();
        pipelineScheduleQueue.schedule(pipelineName, originalBuildCause);
        pipelineScheduleQueue.finishSchedule(pipelineName, originalBuildCause, newBuildCause);
        assertThat(pipelineScheduleQueue.hasBuildCause(pipelineName)).isFalse();
        assertThat(pipelineScheduleQueue.mostRecentScheduled(pipelineName)).isEqualTo(newBuildCause);
        pipelineScheduleQueue.clear();


        pipelineScheduleQueue.schedule(pipelineName, originalBuildCause);
        pipelineScheduleQueue.finishSchedule(new CaseInsensitiveString(pipelineName.toLower()), originalBuildCause, newBuildCause);
        assertThat(pipelineScheduleQueue.hasBuildCause(pipelineName)).isFalse();
        assertThat(pipelineScheduleQueue.mostRecentScheduled(pipelineName)).isEqualTo(newBuildCause);
        pipelineScheduleQueue.clear();

        pipelineScheduleQueue.schedule(pipelineName, originalBuildCause);
        pipelineScheduleQueue.finishSchedule(new CaseInsensitiveString(pipelineName.toUpper()), originalBuildCause, newBuildCause);
        assertThat(pipelineScheduleQueue.hasBuildCause(pipelineName)).isFalse();
        assertThat(pipelineScheduleQueue.mostRecentScheduled(pipelineName)).isEqualTo(newBuildCause);
        pipelineScheduleQueue.clear();
    }

    @Test
    public void shouldConsiderPipelineNameToBeCaseInsensitiveForMostRecentScheduledToAvoidDuplicateEntriesInCache() {
        CaseInsensitiveString pipelineName = new CaseInsensitiveString("PipelinE");
        Pipeline pipeline = PipelineMother.pipeline(pipelineName.toString(), new Stage());
        when(pipelineService.mostRecentFullPipelineByName(pipelineName.toString())).thenReturn(pipeline);

        pipelineScheduleQueue.mostRecentScheduled(pipelineName);
        pipelineScheduleQueue.mostRecentScheduled(new CaseInsensitiveString(pipelineName.toLower()));
        pipelineScheduleQueue.mostRecentScheduled(new CaseInsensitiveString(pipelineName.toUpper()));

        assertThat(pipelineScheduleQueue.mostRecentScheduled(pipelineName)).isEqualTo(pipeline.getBuildCause());
        verify(pipelineService).mostRecentFullPipelineByName(pipelineName.toString());
        verifyNoMoreInteractions(pipelineService);
    }
}
