/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.domain;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class StageStateTest {
    private Set<StageState> testedStates;
    private Set<StageResult> testedResults;

    @BeforeEach
    public void shouldBeUpdatedWhenEnumerationsChange() {
        testedStates = new HashSet<>();
        testedResults = new HashSet<>();
    }

    @AfterEach
    public void checkAllStatesAreCovered() {
        shouldCoverAllStates();
    }

    @Test
    public void shouldNotReturnActiveStatusIfCancelled() {
        isActiveShouldBe(false, StageState.Cancelled, StageState.Passed, StageState.Unknown, StageState.Failed);
        isActiveShouldBe(true, StageState.Building, StageState.Failing);
    }

    @Test
    public void shouldMapStageStatesToStageResults() {
        assertStageResultIs(StageResult.Cancelled, StageState.Cancelled);
        assertStageResultIs(StageResult.Unknown, StageState.Building, StageState.Unknown);
        assertStageResultIs(StageResult.Passed, StageState.Passed);
        assertStageResultIs(StageResult.Failed, StageState.Failing, StageState.Failed);
        shouldCoverAllResults();
    }

    @Test
    public void shouldConvertToCctrayStatus() {
        assertCCTrayStatus(StageState.Passed, "Success");
        assertCCTrayStatus(StageState.Failed, "Failure");
        assertCCTrayStatus(StageState.Building, "Success");
        assertCCTrayStatus(StageState.Unknown, "Success");
        assertCCTrayStatus(StageState.Cancelled, "Failure");
        assertCCTrayStatus(StageState.Failing, "Failure");
    }

    @Test
    public void shouldConvertToCctrayActivity() {
        assertCCTrayActivity(StageState.Passed, "Sleeping");
        assertCCTrayActivity(StageState.Failed, "Sleeping");
        assertCCTrayActivity(StageState.Building, "Building");
        assertCCTrayActivity(StageState.Unknown, "Sleeping");
        assertCCTrayActivity(StageState.Cancelled, "Sleeping");
        assertCCTrayActivity(StageState.Failing, "Building");
    }

    @Test
    public void shouldReturnStatusAsCompletedForPassesFailedCancelled() {
        assertStatus(StageState.Passed, "Completed");
        assertStatus(StageState.Failed, "Completed");
        assertStatus(StageState.Cancelled, "Completed");
        assertStatus(StageState.Building, "Building");
        assertStatus(StageState.Unknown, "Unknown");
        assertStatus(StageState.Failing, "Failing");
    }

    private void assertStatus(StageState state, String expectedStatus) {
        testedStates.add(state);
        assertThat(state.status()).isEqualTo(expectedStatus);
    }

    private void assertCCTrayActivity(StageState state, String expectedStatus) {
        testedStates.add(state);
        assertThat(state.cctrayActivity()).isEqualTo(expectedStatus);
    }

    private void assertCCTrayStatus(StageState passed, String expectedStatus) {
        testedStates.add(passed);
        assertThat(passed.cctrayStatus()).isEqualTo(expectedStatus);
    }


    private void shouldCoverAllResults() {
        Set<StageResult> missingResults = new HashSet<>(Set.of(StageResult.values()));
        missingResults.removeAll(testedResults);
        assertThat(missingResults.isEmpty()).isTrue();
    }

    private void shouldCoverAllStates() {
        Set<StageState> missingStates = new HashSet<>(Set.of(StageState.values()));
        missingStates.removeAll(testedStates);
        assertThat(missingStates.isEmpty()).isTrue();
    }

    private void isActiveShouldBe(boolean active, StageState... states) {
        for (StageState state : states) {
            testedStates.add(state);
            assertThat(state.isActive()).isEqualTo(active);
        }
    }

    private void assertStageResultIs(StageResult result, StageState... states) {
        testedResults.add(result);
        for (StageState state : states) {
            testedStates.add(state);
            assertThat(state.stageResult()).isEqualTo(result);
        }
    }

}
