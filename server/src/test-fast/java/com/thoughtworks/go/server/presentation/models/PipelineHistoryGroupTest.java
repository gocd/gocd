/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.presentation.models;

import static java.util.Arrays.asList;

import com.thoughtworks.go.helper.PipelineHistoryItemMother;
import com.thoughtworks.go.helper.StageHistoryItemMother;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class PipelineHistoryGroupTest {

    @Test
    public void emptyGroupShouldNotMatchAnyItem() throws Exception {
        PipelineInstanceGroupModel group = new PipelineInstanceGroupModel();
        PipelineInstanceModel pipelineInstanceModel = PipelineHistoryItemMother.custom(
                StageHistoryItemMother.custom("stage1", true), StageHistoryItemMother.custom("stage2", false));
        assertThat(group.hasSameStagesAs(pipelineInstanceModel), is(false));
    }

    @Test
    public void shouldMatchSpecifiedItem() throws Exception {
        PipelineInstanceGroupModel group = new PipelineInstanceGroupModel();
        group.getStages().addAll(asList(new SimpleInfo("stage1", true), new SimpleInfo("stage2", false)));
        PipelineInstanceModel pipelineInstanceModel = PipelineHistoryItemMother.custom(
                StageHistoryItemMother.custom("stage1", true), StageHistoryItemMother.custom("stage2", false));
        assertThat(group.hasSameStagesAs(pipelineInstanceModel), is(true));
    }

    @Test
    public void shouldNotMatchSpecifiedItemIfNameNotMatched() throws Exception {
        PipelineInstanceGroupModel group = new PipelineInstanceGroupModel();
        group.getStages().addAll(asList(new SimpleInfo("stage1", true), new SimpleInfo("stage2", false)));
        PipelineInstanceModel pipelineInstanceModel = PipelineHistoryItemMother.custom(
                StageHistoryItemMother.custom("stage1", true), StageHistoryItemMother.custom("stage3", false));
        assertThat(group.hasSameStagesAs(pipelineInstanceModel), is(false));
    }

    @Test
    public void shouldNotMatchSpecifiedItemIfApprovalTypeNotMatched() throws Exception {
        PipelineInstanceGroupModel group = new PipelineInstanceGroupModel();
        group.getStages().addAll(asList(new SimpleInfo("stage1", true), new SimpleInfo("stage2", false)));
        PipelineInstanceModel pipelineInstanceModel = PipelineHistoryItemMother.custom(
                StageHistoryItemMother.custom("stage1", true), StageHistoryItemMother.custom("stage2", true));
        assertThat(group.hasSameStagesAs(pipelineInstanceModel), is(false));
    }

    @Test
    public void shouldNotMatchSpecifiedItemIfSizeNotMatched() throws Exception {
        PipelineInstanceGroupModel group = new PipelineInstanceGroupModel();
        group.getStages().addAll(asList(new SimpleInfo("stage1", true), new SimpleInfo("stage2", false)));
        PipelineInstanceModel pipelineInstanceModel = PipelineHistoryItemMother.custom(
                StageHistoryItemMother.custom("stage1", true), StageHistoryItemMother.custom("stage2", false),
                StageHistoryItemMother.custom("stage3", false));
        assertThat(group.hasSameStagesAs(pipelineInstanceModel), is(false));
    }

    private static class SimpleInfo implements StageConfigurationModel {
        private boolean isAutoApproved;
        private String name;

        public SimpleInfo(String name, boolean autoApproved) {
            isAutoApproved = autoApproved;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public boolean isAutoApproved() {
            return isAutoApproved;
        }
    }
}