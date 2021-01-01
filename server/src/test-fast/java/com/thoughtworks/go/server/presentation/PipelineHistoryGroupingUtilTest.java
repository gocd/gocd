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
package com.thoughtworks.go.server.presentation;

import com.thoughtworks.go.helper.PipelineHistoryItemMother;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.server.presentation.models.PipelineHistoryGroups;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

public class PipelineHistoryGroupingUtilTest {
    private PipelineHistoryGroupingUtil groupingUtil;

    @Before
    public void setUp() throws Exception {
        groupingUtil = new PipelineHistoryGroupingUtil();
    }

    @Test
    public void shouldNotCreateGroupForEmptyHistory() throws Exception {
        PipelineInstanceModels history = PipelineInstanceModels.createPipelineInstanceModels();
        PipelineHistoryGroups historyGroups = groupingUtil.createGroups(history);
        assertThat(historyGroups.size(), is(0));
    }

    @Test
    public void shouldCreateOneGroupForOnePipelineHistoryItem() throws Exception {
        PipelineInstanceModel pipelineInstanceModel = PipelineHistoryItemMother.custom("stage1", "stage2");
        PipelineHistoryGroups historyGroups = groupingUtil.createGroups(
                PipelineInstanceModels.createPipelineInstanceModels(pipelineInstanceModel));
        assertThat(historyGroups.size(), is(1));
        assertThat(historyGroups.first().hasSameStagesAs(pipelineInstanceModel), is(true));
    }

    @Test
    public void shouldCreateOneGroupWithMultiplePipelineHistoryItems() throws Exception {
        PipelineInstanceModel pipelineHistoryItem1 = PipelineHistoryItemMother.custom("stage1", "stage2");
        PipelineInstanceModel pipelineHistoryItem2 = PipelineHistoryItemMother.custom("stage1", "stage2");
        PipelineInstanceModels history = PipelineInstanceModels.createPipelineInstanceModels(pipelineHistoryItem1, pipelineHistoryItem2);
        PipelineHistoryGroups historyGroups = groupingUtil.createGroups(history);
        assertThat(historyGroups.size(), is(1));
        assertThat(historyGroups.first().hasSameStagesAs(pipelineHistoryItem1), is(true));
        assertThat(historyGroups.first().hasSameStagesAs(pipelineHistoryItem2), is(true));
    }

    @Test
    public void shouldCreateTwoGroupsWithMultiplePipelineHistoryItems() throws Exception {
        PipelineInstanceModel pipelineHistoryItem1 = PipelineHistoryItemMother.custom("stage1", "stage2");
        PipelineInstanceModel pipelineHistoryItem2 = PipelineHistoryItemMother.custom("stage1", "stage3");
        PipelineInstanceModels history = PipelineInstanceModels.createPipelineInstanceModels(pipelineHistoryItem1, pipelineHistoryItem2);
        PipelineHistoryGroups historyGroups = groupingUtil.createGroups(history);
        assertThat(historyGroups.size(), is(2));
        assertThat(historyGroups.first().hasSameStagesAs(pipelineHistoryItem1), is(true));
        assertThat(historyGroups.get(1).hasSameStagesAs(pipelineHistoryItem2), is(true));
    }

    @Test
    public void shouldCreateTwoGroupsWithOneGroupHasMultiplePipelineHistoryItems() throws Exception {
        PipelineInstanceModel pipelineHistoryItem1 = PipelineHistoryItemMother.custom("stage1", "stage2");
        PipelineInstanceModel pipelineHistoryItem2 = PipelineHistoryItemMother.custom("stage1", "stage2");
        PipelineInstanceModel pipelineHistoryItem3 = PipelineHistoryItemMother.custom("stage2", "stage1");
        PipelineInstanceModel pipelineHistoryItem4 = PipelineHistoryItemMother.custom("stage1", "stage3");
        PipelineInstanceModels history = PipelineInstanceModels.createPipelineInstanceModels(pipelineHistoryItem1, pipelineHistoryItem2,
                pipelineHistoryItem3,
                pipelineHistoryItem4);
        PipelineHistoryGroups historyGroups = groupingUtil.createGroups(history);
        assertThat(historyGroups.size(), is(3));
        assertThat(historyGroups.first().hasSameStagesAs(pipelineHistoryItem1), is(true));
        assertThat(historyGroups.get(1).hasSameStagesAs(pipelineHistoryItem3), is(true));
        assertThat(historyGroups.get(2).hasSameStagesAs(pipelineHistoryItem4), is(true));
    }
}
