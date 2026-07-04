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

package com.thoughtworks.go.server.messaging;

import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.dao.StageDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.StageNotificationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StageNotificationListenerTest {
    @Mock private StageNotificationService stageNotificationService;
    @Mock private GoConfigService goConfigService;
    @Mock private StageDao stageDao;
    @Mock private StageStatusTopic stageStatusTopic;

    @InjectMocks private StageNotificationListener listener;

    private final StageConfigIdentifier stage = new StageConfigIdentifier("cruise", "dev");

    @AfterEach
    void tearDown() {
        verify(stageStatusTopic).addListener(listener);
    }

    @Test
    public void shouldReturnUnknownForStageWithNoHistory() {
        assertThat(listener.computeEventFrom(stage, StageResult.Passed)).isEqualTo(StageEvent.Passes);
    }

    @Test
    public void shouldUpdatePreviousResultWhenNewResultComeIn() {
        assertThat(listener.computeEventFrom(stage, StageResult.Failed)).isEqualTo(StageEvent.Fails);
        assertThat(listener.computeEventFrom(stage, StageResult.Passed)).isEqualTo(StageEvent.Fixed);
        assertThat(listener.computeEventFrom(stage, StageResult.Failed)).isEqualTo(StageEvent.Breaks);
    }

    @Test
    public void shouldLoadStageResultFromDatabaseWhenNoGivenStageInCache() {
        when(stageDao.mostRecentCompleted(stage)).thenReturn(StageMother.passedStageInstance(stage.getPipelineName(), stage.getStageName(), "anyJob"));
        assertThat(listener.computeEventFrom(stage, StageResult.Failed)).isEqualTo(StageEvent.Breaks);
    }

    @Test
    public void shouldSendStageResultMessageWhenStageComplete() {
        when(goConfigService.isSecurityEnabled()).thenReturn(true);
        StageIdentifier stageId = mock(StageIdentifier.class);
        when(stageId.stageConfigIdentifier()).thenReturn(stage);
        listener.onMessage(new StageStatusMessage(stageId, StageState.Passed, StageResult.Passed));
        verify(stageNotificationService).sendNotifications(stageId, StageEvent.Passes, Username.BLANK);

        listener.onMessage(new StageStatusMessage(stageId, StageState.Failed, StageResult.Failed));
        verify(stageNotificationService).sendNotifications(stageId, StageEvent.Breaks, Username.BLANK);
    }
}