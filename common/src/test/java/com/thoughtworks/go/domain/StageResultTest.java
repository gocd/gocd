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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class StageResultTest {

    @Test
    public void shouldGenerateEventByResults() {
        assertThat(StageResult.Cancelled.describeChangeEvent(StageResult.Passed), is(StageEvent.Cancelled));
        assertThat(StageResult.Cancelled.describeChangeEvent(StageResult.Failed), is(StageEvent.Cancelled));
        assertThat(StageResult.Cancelled.describeChangeEvent(StageResult.Cancelled), is(StageEvent.Cancelled));
        assertThat(StageResult.Cancelled.describeChangeEvent(StageResult.Unknown), is(StageEvent.Cancelled));

        assertThat(StageResult.Passed.describeChangeEvent(StageResult.Failed), is(StageEvent.Fixed));
        assertThat(StageResult.Passed.describeChangeEvent(StageResult.Cancelled), is(StageEvent.Passes));
        assertThat(StageResult.Passed.describeChangeEvent(StageResult.Passed), is(StageEvent.Passes));
        assertThat(StageResult.Passed.describeChangeEvent(StageResult.Unknown), is(StageEvent.Passes));

        assertThat(StageResult.Failed.describeChangeEvent(StageResult.Passed), is(StageEvent.Breaks));
        assertThat(StageResult.Failed.describeChangeEvent(StageResult.Failed), is(StageEvent.Fails));
        assertThat(StageResult.Failed.describeChangeEvent(StageResult.Cancelled), is(StageEvent.Fails));
        assertThat(StageResult.Failed.describeChangeEvent(StageResult.Unknown), is(StageEvent.Fails));
    }

    @Test
    public void shouldThrowExceptionIfNewStatusIsUnknown() {
        try {
            StageResult.Unknown.describeChangeEvent(StageResult.Passed);
            fail("shouldThrowExceptionIfNewStatusIsUnknown");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Current result can not be Unknown"));
        }
    }

    @Test
    public void shouldMakeSureThatAnyChangeToThisEnumIsReflectedInQueriesInStagesXML() throws Exception {
        assertThat(StageResult.values().length, is(4));
        List<StageResult> actualStageResults = Arrays.asList(StageResult.values());

        List<String> names = new ArrayList<>();
        for (StageResult stageResult : actualStageResults) {
            names.add(stageResult.name());
        }

        assertThat("If this test fails, it means that either a stage result has been added/removed or renamed.\n"
                + " This might cause Stage queries in Stages.xml, especially, allPassedStageAsDMRsAfter, which uses something like: stages.result <> 'Failed' AND stages.result <> 'Unknown' AND stages.result <> 'Cancelled'",
                names, hasItems("Passed", "Cancelled", "Failed", "Unknown"));
    }
}
