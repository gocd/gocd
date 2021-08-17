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
package com.thoughtworks.go.domain;

import static java.util.Arrays.asList;
import java.util.HashSet;
import java.util.Set;

import org.hamcrest.Matcher;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    @Test public void shouldNotReturnActiveStatusIfCancelled() {
        isActiveShouldBe(false, StageState.Cancelled, StageState.Passed, StageState.Unknown, StageState.Failed);
        isActiveShouldBe(true, StageState.Building, StageState.Failing);
    }

    @Test public void shouldMapStageStatesToStageResults() {
        assertStageResultIs(StageResult.Cancelled, StageState.Cancelled);
        assertStageResultIs(StageResult.Unknown, StageState.Building, StageState.Unknown);
        assertStageResultIs(StageResult.Passed, StageState.Passed);
        assertStageResultIs(StageResult.Failed, StageState.Failing, StageState.Failed);
        shouldCoverAllResults();
    }

    @Test public void shouldConvertToCctrayStatus() throws Exception {
        assertCCTrayStatus(StageState.Passed, is("Success"));
        assertCCTrayStatus(StageState.Failed, is("Failure"));
        assertCCTrayStatus(StageState.Building, is("Success"));
        assertCCTrayStatus(StageState.Unknown, is("Success"));
        assertCCTrayStatus(StageState.Cancelled, is("Failure"));
        assertCCTrayStatus(StageState.Failing, is("Failure"));
    }

    @Test
    public void shouldConvertToCctrayActivity() throws Exception {
        assertCCTrayActivity(StageState.Passed, is("Sleeping"));
        assertCCTrayActivity(StageState.Failed, is("Sleeping"));
        assertCCTrayActivity(StageState.Building, is("Building"));
        assertCCTrayActivity(StageState.Unknown, is("Sleeping"));
        assertCCTrayActivity(StageState.Cancelled, is("Sleeping"));
        assertCCTrayActivity(StageState.Failing, is("Building"));
    }

    @Test
    public void shouldReturnStatusAsCompletedForPassesFailedCancelled() {
        assertStatus(StageState.Passed, is("Completed"));
        assertStatus(StageState.Failed, is("Completed"));
        assertStatus(StageState.Cancelled, is("Completed"));
        assertStatus(StageState.Building, is("Building"));
        assertStatus(StageState.Unknown, is("Unknown"));
        assertStatus(StageState.Failing, is("Failing"));
    }

    private void assertStatus(StageState state, Matcher<String> matcher) {
        testedStates.add(state);
        assertThat(state.status(),matcher);
    }

    private void assertCCTrayActivity(StageState state, Matcher<String> stringMatcher) {
        testedStates.add(state);
        assertThat(state.cctrayActivity(), stringMatcher);
    }

    private void assertCCTrayStatus(StageState passed, Matcher<String> stringMatcher) {
        testedStates.add(passed);
        assertThat(passed.cctrayStatus(), stringMatcher);
    }


    private void shouldCoverAllResults() {
        Set<StageResult> missingResults = new HashSet<>(asList(StageResult.values()));
        missingResults.removeAll(testedResults);
        assertThat("Update all tests when you add a new StageResult (missing " + missingResults + ")",
                missingResults.isEmpty(), is(true));
    }

    private void shouldCoverAllStates() {
        Set<StageState> missingStates = new HashSet<>(asList(StageState.values()));
        missingStates.removeAll(testedStates);
        assertThat("Update all tests when you add a new StageState (missing " + missingStates + ")",
                missingStates.isEmpty(), is(true));
    }

    private void isActiveShouldBe(boolean active, StageState... states) {
        for (StageState state : states) {
            testedStates.add(state);
            assertThat(state.isActive(), is(active));
        }
    }

    private void assertStageResultIs(StageResult result, StageState... states) {
        testedResults.add(result);
        for (StageState state : states) {
            testedStates.add(state);
            assertThat(state.stageResult(), is(result));
        }
    }

}
