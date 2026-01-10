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
package com.thoughtworks.go.server.presentation;

import com.thoughtworks.go.helper.PipelineInstanceModelMother;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.server.presentation.models.PipelineHistoryGroups;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PipelineHistoryGroupingUtilTest {
    private PipelineHistoryGroupingUtil groupingUtil;

    @BeforeEach
    public void setUp() {
        groupingUtil = new PipelineHistoryGroupingUtil();
    }

    @Test
    public void shouldNotCreateGroupForEmptyHistory() {
        PipelineInstanceModels history = PipelineInstanceModels.createPipelineInstanceModels();
        PipelineHistoryGroups historyGroups = groupingUtil.createGroups(history);
        assertThat(historyGroups.size()).isEqualTo(0);
    }

    @Test
    public void shouldCreateOneGroupForOnePipelineHistoryItem() {
        PipelineInstanceModel pipelineInstanceModel = PipelineInstanceModelMother.custom("stage1", "stage2");
        PipelineHistoryGroups historyGroups = groupingUtil.createGroups(
                PipelineInstanceModels.createPipelineInstanceModels(pipelineInstanceModel));
        assertThat(historyGroups.size()).isEqualTo(1);
        assertThat(historyGroups.getFirst().hasSameStagesAs(pipelineInstanceModel)).isTrue();
    }

    @Test
    public void shouldCreateOneGroupWithMultiplePipelineHistoryItems() {
        PipelineInstanceModel pipelineHistoryItem1 = PipelineInstanceModelMother.custom("stage1", "stage2");
        PipelineInstanceModel pipelineHistoryItem2 = PipelineInstanceModelMother.custom("stage1", "stage2");
        PipelineInstanceModels history = PipelineInstanceModels.createPipelineInstanceModels(pipelineHistoryItem1, pipelineHistoryItem2);
        PipelineHistoryGroups historyGroups = groupingUtil.createGroups(history);
        assertThat(historyGroups.size()).isEqualTo(1);
        assertThat(historyGroups.getFirst().hasSameStagesAs(pipelineHistoryItem1)).isTrue();
        assertThat(historyGroups.getFirst().hasSameStagesAs(pipelineHistoryItem2)).isTrue();
    }

    @Test
    public void shouldCreateTwoGroupsWithMultiplePipelineHistoryItems() {
        PipelineInstanceModel pipelineHistoryItem1 = PipelineInstanceModelMother.custom("stage1", "stage2");
        PipelineInstanceModel pipelineHistoryItem2 = PipelineInstanceModelMother.custom("stage1", "stage3");
        PipelineInstanceModels history = PipelineInstanceModels.createPipelineInstanceModels(pipelineHistoryItem1, pipelineHistoryItem2);
        PipelineHistoryGroups historyGroups = groupingUtil.createGroups(history);
        assertThat(historyGroups.size()).isEqualTo(2);
        assertThat(historyGroups.getFirst().hasSameStagesAs(pipelineHistoryItem1)).isTrue();
        assertThat(historyGroups.getLast().hasSameStagesAs(pipelineHistoryItem2)).isTrue();
    }

    @Test
    public void shouldCreateTwoGroupsWithOneGroupHasMultiplePipelineHistoryItems() {
        PipelineInstanceModel pipelineHistoryItem1 = PipelineInstanceModelMother.custom("stage1", "stage2");
        PipelineInstanceModel pipelineHistoryItem2 = PipelineInstanceModelMother.custom("stage1", "stage2");
        PipelineInstanceModel pipelineHistoryItem3 = PipelineInstanceModelMother.custom("stage2", "stage1");
        PipelineInstanceModel pipelineHistoryItem4 = PipelineInstanceModelMother.custom("stage1", "stage3");
        PipelineInstanceModels history = PipelineInstanceModels.createPipelineInstanceModels(pipelineHistoryItem1, pipelineHistoryItem2,
                pipelineHistoryItem3,
                pipelineHistoryItem4);
        PipelineHistoryGroups historyGroups = groupingUtil.createGroups(history);
        assertThat(historyGroups.size()).isEqualTo(3);
        assertThat(historyGroups.get(0).hasSameStagesAs(pipelineHistoryItem1)).isTrue();
        assertThat(historyGroups.get(1).hasSameStagesAs(pipelineHistoryItem3)).isTrue();
        assertThat(historyGroups.get(2).hasSameStagesAs(pipelineHistoryItem4)).isTrue();
    }
}
