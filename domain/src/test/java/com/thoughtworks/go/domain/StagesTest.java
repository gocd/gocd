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

import com.thoughtworks.go.helper.StageMother;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;

public class StagesTest {

    @Test
    public void shouldFindStageById() {
        Stage expected = new Stage();
        expected.setId(1);
        assertThat(new Stages(expected).byId(1)).isEqualTo(expected);
    }

    @Test
    public void shouldThrowExceptionWhenIdNotFound() {
        assertThatThrownBy(() -> new Stages().byId(100)).isExactlyInstanceOf(RuntimeException.class);
    }

    @Test
    public void shouldReturnFalseWhenNoStageExist() {
        Stages stages = new Stages();
        assertThat(stages.isAnyStageActive()).isFalse();
    }

    @Test
    public void shouldReturnTrueIfAnyStageIsActive() {
        Stage notActiveFt = StageMother.completedFailedStageInstance("pipeline-name", "dev", "ft");
        Stage activeStage = StageMother.scheduledStage("pipeline-name", 1, "dev", 1, "ut");
        Stages stages = new Stages(notActiveFt, activeStage);
        assertThat(stages.isAnyStageActive()).isTrue();
    }

    @Test
    public void shouldReturnFalseIfNoStageIsActive() {
        Stage notActiveUt = StageMother.completedFailedStageInstance("pipeline-name", "dev", "ut");
        Stage notActiveFt = StageMother.completedFailedStageInstance("pipeline-name", "dev", "ft");
        Stages stages = new Stages(notActiveUt, notActiveFt);
        assertThat(stages.isAnyStageActive()).isFalse();
    }

    @Test
    public void shouldDescribeStagesIfCannotFindByCounter() {
        Stage run1 = StageMother.createPassedStage("pipeline", 1, "stage", 1, "job", Instant.now());
        Stage run2 = StageMother.createPassedStage("pipeline", 1, "stage", 2, "job", Instant.now());
        Stage run3 = StageMother.createPassedStage("pipeline", 1, "stage", 3, "job", Instant.now());
        Stages stages = new Stages(run1, run2, run3);
        assertThat(stages.byCounter(2)).isEqualTo(run2);
        try {
            assertThat(stages.byCounter(4)).isEqualTo(run2);
            fail("Should throw exception if the stage does not exist");
        }
        catch (Exception e) {
            assertThat(e.getMessage()).contains(run1.toString());
            assertThat(e.getMessage()).contains(run2.toString());
            assertThat(e.getMessage()).contains(run3.toString());
        }
    }

    @Test
    public void shouldFindStageWhenStageNameIsOfDifferentCase() {
        Stages stages = new Stages(StageMother.custom("stageName"));
        assertThat(stages.hasStage("Stagename")).isEqualTo(true);
        assertThat(stages.hasStage("stageName")).isEqualTo(true);
    }

    @Test
    public void shouldGetLatestStagesInRunOrder() {
        Stage s1_1 = StageMother.createPassedStage("p", 1, "s1", 1, "b", Instant.now());
        s1_1.setOrderId(1);
        s1_1.setLatestRun(false);
        Stage s2_1 = StageMother.createPassedStage("p", 1, "s2", 1, "b", Instant.now());
        s2_1.setOrderId(2);
        Stage s1_2 = StageMother.createPassedStage("p", 1, "s1", 2, "b", Instant.now());
        s1_2.setOrderId(1);

        Stages stages = new Stages(s2_1, s1_2, s1_1);
        Stages latestStagesInRunOrder = stages.latestStagesInRunOrder();
        assertThat(latestStagesInRunOrder).isEqualTo(new Stages(s1_2,s2_1));
    }
}
