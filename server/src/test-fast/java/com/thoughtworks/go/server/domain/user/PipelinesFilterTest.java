/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.domain.user;

import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.thoughtworks.go.server.domain.user.DashboardFilter.BUILDING_STATE;
import static com.thoughtworks.go.server.domain.user.DashboardFilter.FAILED_STATE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PipelinesFilterTest {
    @Test
    public void shouldReturnTrueIfStageIsBuildingWhenFilteringByBuilding() {
        StageInstanceModel stage = mock(StageInstanceModel.class);
        when(stage.isRunning()).thenReturn(true);
        Set<String> state = new HashSet<>();
        state.add(BUILDING_STATE);
        assertTrue(new PipelinesFilter(state, null).filterByState(stage));
    }

    @Test
    public void shouldReturnFalseIfStageIsBuildingWhenFilteringByBuilding() {
        StageInstanceModel stage = mock(StageInstanceModel.class);
        when(stage.isRunning()).thenReturn(false);
        Set<String> state = new HashSet<>();
        state.add(BUILDING_STATE);
        assertFalse(new PipelinesFilter(state, null).filterByState(stage));
    }

    @Test
    public void shouldReturnTrueIfStageIsFailingWhenFilteringByFailing() {
        StageInstanceModel stage = mock(StageInstanceModel.class);
        when(stage.hasFailed()).thenReturn(true);
        Set<String> state = new HashSet<>();
        state.add(FAILED_STATE);
        assertTrue(new PipelinesFilter(state, null).filterByState(stage));
    }

    @Test
    public void shouldReturnFalseIfStageIsFailingWhenFilteringByFailing() {
        StageInstanceModel stage = mock(StageInstanceModel.class);
        when(stage.hasFailed()).thenReturn(false);
        Set<String> state = new HashSet<>();
        state.add(FAILED_STATE);
        assertFalse(new PipelinesFilter(state, null).filterByState(stage));
    }

    @Test
    public void shouldReturnTrueIfStageIsEitherBuildingOrFailedWhenFilteringByBothStates() {
        StageInstanceModel stage = mock(StageInstanceModel.class);
        when(stage.hasFailed()).thenReturn(false);
        when(stage.isRunning()).thenReturn(true);
        Set<String> state = new HashSet<>();
        state.add(FAILED_STATE);
        state.add(BUILDING_STATE);
        assertTrue(new PipelinesFilter(state, null).filterByState(stage));

        when(stage.hasFailed()).thenReturn(true);
        when(stage.isRunning()).thenReturn(false);
        assertTrue(new PipelinesFilter(state, null).filterByState(stage));

        when(stage.hasFailed()).thenReturn(true);
        when(stage.isRunning()).thenReturn(true);
        assertTrue(new PipelinesFilter(state, null).filterByState(stage));
    }

    @Test
    public void shouldReturnTrueIfStateIsNull() {
        StageInstanceModel stage = mock(StageInstanceModel.class);
        when(stage.hasFailed()).thenReturn(false);
        when(stage.isRunning()).thenReturn(true);
        assertTrue(new PipelinesFilter(null, null).filterByState(stage));
    }

    @Test
    public void shouldReturnTrueIfStateIsEmpty() {
        StageInstanceModel stage = mock(StageInstanceModel.class);
        when(stage.hasFailed()).thenReturn(false);
        when(stage.isRunning()).thenReturn(true);
        Set<String> state = new HashSet<>();
        assertTrue(new PipelinesFilter(state, null).filterByState(stage));
    }
}
