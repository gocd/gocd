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
package com.thoughtworks.go.domain;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class StageResultTest {

    @Test
    public void shouldGenerateEventByResults() {
        assertThat(StageResult.Cancelled.describeChangeEvent(StageResult.Passed)).isEqualTo(StageEvent.Cancelled);
        assertThat(StageResult.Cancelled.describeChangeEvent(StageResult.Failed)).isEqualTo(StageEvent.Cancelled);
        assertThat(StageResult.Cancelled.describeChangeEvent(StageResult.Cancelled)).isEqualTo(StageEvent.Cancelled);
        assertThat(StageResult.Cancelled.describeChangeEvent(StageResult.Unknown)).isEqualTo(StageEvent.Cancelled);

        assertThat(StageResult.Passed.describeChangeEvent(StageResult.Failed)).isEqualTo(StageEvent.Fixed);
        assertThat(StageResult.Passed.describeChangeEvent(StageResult.Cancelled)).isEqualTo(StageEvent.Passes);
        assertThat(StageResult.Passed.describeChangeEvent(StageResult.Passed)).isEqualTo(StageEvent.Passes);
        assertThat(StageResult.Passed.describeChangeEvent(StageResult.Unknown)).isEqualTo(StageEvent.Passes);

        assertThat(StageResult.Failed.describeChangeEvent(StageResult.Passed)).isEqualTo(StageEvent.Breaks);
        assertThat(StageResult.Failed.describeChangeEvent(StageResult.Failed)).isEqualTo(StageEvent.Fails);
        assertThat(StageResult.Failed.describeChangeEvent(StageResult.Cancelled)).isEqualTo(StageEvent.Fails);
        assertThat(StageResult.Failed.describeChangeEvent(StageResult.Unknown)).isEqualTo(StageEvent.Fails);
    }

    @Test
    public void shouldThrowExceptionIfNewStatusIsUnknown() {
        try {
            StageResult.Unknown.describeChangeEvent(StageResult.Passed);
            fail("shouldThrowExceptionIfNewStatusIsUnknown");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Current result can not be Unknown");
        }
    }

    @Test
    public void shouldMakeSureThatAnyChangeToThisEnumIsReflectedInQueriesInStagesXML() {
        assertThat(StageResult.values().length).isEqualTo(4);
        List<StageResult> actualStageResults = List.of(StageResult.values());

        List<String> names = new ArrayList<>();
        for (StageResult stageResult : actualStageResults) {
            names.add(stageResult.name());
        }

        assertThat(names).describedAs("If this test fails, it means that either a stage result has been added/removed or renamed.\n"
                + " This might cause Stage queries in Stages.xml, especially, allPassedStageAsDMRsAfter, which uses something like: stages.result <> 'Failed' AND stages.result <> 'Unknown' AND stages.result <> 'Cancelled'")
            .contains("Passed", "Cancelled", "Failed", "Unknown");
    }
}
