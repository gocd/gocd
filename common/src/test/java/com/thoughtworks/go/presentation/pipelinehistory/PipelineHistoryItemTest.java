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
package com.thoughtworks.go.presentation.pipelinehistory;

import com.thoughtworks.go.helper.PipelineHistoryItemMother;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

public class PipelineHistoryItemTest {

    @Test
    public void shouldReturnFalseForEmptyPipelineHistory() throws Exception {
        PipelineInstanceModel emptyOne = PipelineInstanceModel.createEmptyModel();
        assertThat(emptyOne.hasPreviousStageBeenScheduled("stage1"), is(false));
    }

    @Test
    public void shouldReturnTrueForFirstStage() throws Exception {
        assertThat(PipelineHistoryItemMother.custom("stage1").hasPreviousStageBeenScheduled("stage1"), is(true));
        assertThat(PipelineHistoryItemMother.custom("stage1", "stage2").hasPreviousStageBeenScheduled("stage1"),
                is(true));
    }

    @Test
    public void shouldCheckIfPreviousStageInstanceExist() throws Exception {
        PipelineInstanceModel twoStages = PipelineHistoryItemMother.custom("stage1", "stage2");
        assertThat(twoStages.hasPreviousStageBeenScheduled("stage2"), is(true));
    }

    @Test
    public void shouldReturnFalseIfPreviousStageHasNotBeenScheduled() throws Exception {
        PipelineInstanceModel twoStages = PipelineHistoryItemMother.custom(new NullStageHistoryItem("stage1"),
                new StageInstanceModel("stage2", "1", new JobHistory()));
        assertThat(twoStages.hasPreviousStageBeenScheduled("stage2"), is(false));
        PipelineInstanceModel threeStages = PipelineHistoryItemMother.custom(new NullStageHistoryItem("stage1"),
                new NullStageHistoryItem("stage2"),
                new StageInstanceModel("stage3", "1", new JobHistory()));
        assertThat(threeStages.hasPreviousStageBeenScheduled("stage3"), is(false));
    }

}
