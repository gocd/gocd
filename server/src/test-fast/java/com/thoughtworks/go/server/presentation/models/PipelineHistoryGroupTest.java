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
package com.thoughtworks.go.server.presentation.models;

import com.thoughtworks.go.helper.PipelineInstanceModelMother;
import com.thoughtworks.go.helper.StageInstanceModelMother;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class PipelineHistoryGroupTest {

    @Test
    public void emptyGroupShouldNotMatchAnyItem() throws Exception {
        PipelineInstanceGroupModel group = new PipelineInstanceGroupModel();
        PipelineInstanceModel pipelineInstanceModel = PipelineInstanceModelMother.custom(
                StageInstanceModelMother.custom("stage1", true), StageInstanceModelMother.custom("stage2", false));
        assertThat(group.hasSameStagesAs(pipelineInstanceModel)).isFalse();
    }

    @Test
    public void shouldMatchSpecifiedItem() throws Exception {
        PipelineInstanceGroupModel group = new PipelineInstanceGroupModel();
        group.getStages().addAll(List.of(new SimpleInfo("stage1", true), new SimpleInfo("stage2", false)));
        PipelineInstanceModel pipelineInstanceModel = PipelineInstanceModelMother.custom(
                StageInstanceModelMother.custom("stage1", true), StageInstanceModelMother.custom("stage2", false));
        assertThat(group.hasSameStagesAs(pipelineInstanceModel)).isTrue();
    }

    @Test
    public void shouldNotMatchSpecifiedItemIfNameNotMatched() throws Exception {
        PipelineInstanceGroupModel group = new PipelineInstanceGroupModel();
        group.getStages().addAll(List.of(new SimpleInfo("stage1", true), new SimpleInfo("stage2", false)));
        PipelineInstanceModel pipelineInstanceModel = PipelineInstanceModelMother.custom(
                StageInstanceModelMother.custom("stage1", true), StageInstanceModelMother.custom("stage3", false));
        assertThat(group.hasSameStagesAs(pipelineInstanceModel)).isFalse();
    }

    @Test
    public void shouldNotMatchSpecifiedItemIfApprovalTypeNotMatched() throws Exception {
        PipelineInstanceGroupModel group = new PipelineInstanceGroupModel();
        group.getStages().addAll(List.of(new SimpleInfo("stage1", true), new SimpleInfo("stage2", false)));
        PipelineInstanceModel pipelineInstanceModel = PipelineInstanceModelMother.custom(
                StageInstanceModelMother.custom("stage1", true), StageInstanceModelMother.custom("stage2", true));
        assertThat(group.hasSameStagesAs(pipelineInstanceModel)).isFalse();
    }

    @Test
    public void shouldNotMatchSpecifiedItemIfSizeNotMatched() throws Exception {
        PipelineInstanceGroupModel group = new PipelineInstanceGroupModel();
        group.getStages().addAll(List.of(new SimpleInfo("stage1", true), new SimpleInfo("stage2", false)));
        PipelineInstanceModel pipelineInstanceModel = PipelineInstanceModelMother.custom(
                StageInstanceModelMother.custom("stage1", true), StageInstanceModelMother.custom("stage2", false),
                StageInstanceModelMother.custom("stage3", false));
        assertThat(group.hasSameStagesAs(pipelineInstanceModel)).isFalse();
    }

    private static class SimpleInfo implements StageConfigurationModel {
        private boolean isAutoApproved;
        private String name;

        public SimpleInfo(String name, boolean autoApproved) {
            isAutoApproved = autoApproved;
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public boolean isAutoApproved() {
            return isAutoApproved;
        }
    }
}
