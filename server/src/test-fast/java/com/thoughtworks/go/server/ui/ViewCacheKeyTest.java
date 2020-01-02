/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.ui;

import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.presentation.pipelinehistory.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class ViewCacheKeyTest {
    private ViewCacheKey viewCacheKey;

    @BeforeEach
    void setUp() {
        viewCacheKey = new ViewCacheKey();
    }

    @Nested
    class ForFailedBuildHistoryStage {
        @Test
        void shouldGenerateCacheKey() {
            Stage stage = StageMother.passedStageInstance("stage", "job", "pipeline-name");
            assertThat(viewCacheKey.forFailedBuildHistoryStage(stage, "html"))
                    .isEqualTo("com.thoughtworks.go.server.ui.ViewCacheKey.$view.$stageFailedBuildHistoryView.$pipeline-name/1/stage/1.$Passed.$html");
        }

        @Test
        void shouldGenerateADifferentCacheKeyWhenPartOfPipelineIsInterchangedWithStageName() {
            Stage stageOne = StageMother.passedStageInstance("foo", "bar_baz", "up42");
            Stage stageTwo = StageMother.passedStageInstance("foo_bar", "baz", "up42");
            Stage differentPipeline = StageMother.passedStageInstance("foo", "bar", "baz_up42");
            assertThat(viewCacheKey.forFailedBuildHistoryStage(stageOne, "html"))
                    .isNotEqualTo(viewCacheKey.forFailedBuildHistoryStage(stageTwo, "html"))
                    .isNotEqualTo(viewCacheKey.forFailedBuildHistoryStage(differentPipeline, "html"));


            stageOne = StageMother.passedStageInstance("foo", "bar-baz", "up42");
            stageTwo = StageMother.passedStageInstance("foo-bar", "baz", "up42");
            differentPipeline = StageMother.passedStageInstance("foo", "bar", "baz-up42");
            assertThat(viewCacheKey.forFailedBuildHistoryStage(stageOne, "html"))
                    .isNotEqualTo(viewCacheKey.forFailedBuildHistoryStage(stageTwo, "html"))
                    .isNotEqualTo(viewCacheKey.forFailedBuildHistoryStage(differentPipeline, "html"));
        }
    }

    @Nested
    class ForFbhOfStagesUnderPipeline {
        @Test
        void shouldGenerateCacheKey() {
            final PipelineIdentifier pipelineIdentifier = new PipelineIdentifier("up42", 1, "label");
            assertThat(viewCacheKey.forFbhOfStagesUnderPipeline(pipelineIdentifier))
                    .isEqualTo("com.thoughtworks.go.server.ui.ViewCacheKey.$view.$fbhOfStagesUnderPipeline.$up42/1");
        }
    }

    @Nested
    class ForPipelineModelBox {
        @Test
        void shouldGenerateCacheKey() {
            PipelineModel model = new PipelineModel("pipelineName", true, true, PipelinePauseInfo.notPaused()).updateAdministrability(true);
            StageInstanceModels stages = new StageInstanceModels();
            stages.add(stageInstance("stageName", 13, JobState.Building, JobResult.Unknown));
            stages.add(new NullStageHistoryItem("stage2", true));
            PipelineInstanceModel pipelineInstance = PipelineInstanceModel.createPipeline("pipelineName", 10, "label-10", BuildCause.createExternal(), stages);
            pipelineInstance.setId(12);
            model.addPipelineInstance(pipelineInstance);

            StageInstanceModels stages2 = new StageInstanceModels();
            stages2.add(stageInstance("stageName", 7, JobState.Completed, JobResult.Passed));
            stages2.add(stageInstance("stage2", 10, JobState.Assigned, JobResult.Unknown));
            PipelineInstanceModel pipelineInstance2 = PipelineInstanceModel.createPipeline("pipelineName", 7, "label-7", BuildCause.createExternal(), stages2);
            pipelineInstance2.setId(14);
            model.addPipelineInstance(pipelineInstance2);

            assertThat(viewCacheKey.forPipelineModelBox(model))
                    .isEqualTo("com.thoughtworks.go.server.ui.ViewCacheKey.$view.$dashboardPipelineFragment.$pipelineName.${false|false|false}.$[12|stageName|13|Building|stage2|0|Unknown|][14|stageName|7|Passed|stage2|10|Building|].$true.$true.$false.$.$.$true");

        }

        @Test
        void shouldGenerateKeyForPipelineModelViewFragmentWithoutSpecialCharactersInPauseCause() {
            PipelinePauseInfo pauseInfo = new PipelinePauseInfo(true, "pause& @Cause #with $special %char &*(){';/.,<>?", "admin");
            PipelineModel model = new PipelineModel("pipelineName", true, true, pauseInfo).updateAdministrability(true);
            StageInstanceModels stages = new StageInstanceModels();
            stages.add(stageInstance("stageName", 13, JobState.Building, JobResult.Unknown));
            stages.add(new NullStageHistoryItem("stage2", true));
            PipelineInstanceModel pipelineInstance = PipelineInstanceModel.createPipeline("pipelineName", 10, "label-10", BuildCause.createExternal(), stages);
            pipelineInstance.setId(12);
            model.addPipelineInstance(pipelineInstance);

            StageInstanceModels stages2 = new StageInstanceModels();
            stages2.add(stageInstance("stageName", 7, JobState.Completed, JobResult.Passed));
            stages2.add(stageInstance("stage2", 10, JobState.Assigned, JobResult.Unknown));
            PipelineInstanceModel pipelineInstance2 = PipelineInstanceModel.createPipeline("pipelineName", 7, "label-7", BuildCause.createExternal(), stages2);
            pipelineInstance2.setId(14);
            model.addPipelineInstance(pipelineInstance2);

            assertThat(viewCacheKey.forPipelineModelBox(model))
                    .isEqualTo("com.thoughtworks.go.server.ui.ViewCacheKey.$view.$dashboardPipelineFragment.$pipelineName.${false|false|false}.$[12|stageName|13|Building|stage2|0|Unknown|][14|stageName|7|Passed|stage2|10|Building|].$true.$true.$true.$pauseCausewithspecialchar.$admin.$true");
        }

        @Test
        void shouldGenerateKeyForPipelineModelViewFragmentWithLockStatus() {
            PipelineModel model = new PipelineModel("pipelineName", true, true, PipelinePauseInfo.notPaused()).updateAdministrability(true);
            StageInstanceModels stages = new StageInstanceModels();
            stages.add(stageInstance("stageName", 13, JobState.Building, JobResult.Unknown));
            stages.add(new NullStageHistoryItem("stage2", true));
            PipelineInstanceModel pipelineInstance = PipelineInstanceModel.createPipeline("pipelineName", 10, "label-10", BuildCause.createExternal(), stages);
            pipelineInstance.setId(12);
            pipelineInstance.setCanUnlock(false);
            pipelineInstance.setIsLockable(true);
            pipelineInstance.setCurrentlyLocked(true);
            model.addPipelineInstance(pipelineInstance);

            assertThat(viewCacheKey.forPipelineModelBox(model))
                    .isEqualTo("com.thoughtworks.go.server.ui.ViewCacheKey.$view.$dashboardPipelineFragment.$pipelineName.${true|true|false}.$[12|stageName|13|Building|stage2|0|Unknown|].$true.$true.$false.$.$.$true");
        }

        @Test
        void shouldGenerateKeyForPipelineModelViewIncludingUserAdminStatus() {
            PipelineModel model = new PipelineModel("pipelineName", true, true, PipelinePauseInfo.notPaused()).updateAdministrability(true);
            StageInstanceModels stages = new StageInstanceModels();
            stages.add(stageInstance("stageName", 13, JobState.Building, JobResult.Unknown));
            stages.add(new NullStageHistoryItem("stage2", true));
            PipelineInstanceModel pipelineInstance = PipelineInstanceModel.createPipeline("pipelineName", 10, "label-10", BuildCause.createExternal(), stages);
            pipelineInstance.setId(12);
            pipelineInstance.setCanUnlock(false);
            pipelineInstance.setIsLockable(true);
            pipelineInstance.setCurrentlyLocked(true);
            model.addPipelineInstance(pipelineInstance);

            assertThat(viewCacheKey.forPipelineModelBox(model))
                    .isEqualTo("com.thoughtworks.go.server.ui.ViewCacheKey.$view.$dashboardPipelineFragment.$pipelineName.${true|true|false}.$[12|stageName|13|Building|stage2|0|Unknown|].$true.$true.$false.$.$.$true");

            model.updateAdministrability(false);
            assertThat(viewCacheKey.forPipelineModelBox(model))
                    .isEqualTo("com.thoughtworks.go.server.ui.ViewCacheKey.$view.$dashboardPipelineFragment.$pipelineName.${true|true|false}.$[12|stageName|13|Building|stage2|0|Unknown|].$true.$true.$false.$.$.$false");
        }

    }

    @Nested
    class ForEnvironmentPipelineBox {
        @Test
        void shouldGenerateCacheKey() {
            MaterialRevisions materialRevisions = ModificationsMother.createHgMaterialRevisions();
            Modification latestModification = materialRevisions.getMaterialRevision(0).getModifications().remove(0);

            PipelineModel model = new PipelineModel("pipelineName", true, true, PipelinePauseInfo.notPaused()).updateAdministrability(true);
            StageInstanceModels stages = new StageInstanceModels();
            stages.add(stageInstance("stageName", 13, JobState.Building, JobResult.Unknown));
            stages.add(new NullStageHistoryItem("stage2", true));
            PipelineInstanceModel pipelineInstance = PipelineInstanceModel.createPipeline("pipelineName", 10, "label-10", BuildCause.createWithModifications(materialRevisions, "someone"), stages);
            pipelineInstance.setMaterialConfigs(materialRevisions.getMaterials().convertToConfigs());
            pipelineInstance.setLatestRevisions(new MaterialRevisions(new MaterialRevision(materialRevisions.getMaterialRevision(0).getMaterial(), latestModification)));
            pipelineInstance.setId(12);
            model.addPipelineInstance(pipelineInstance);

            assertThat(model.hasNewRevisions()).isTrue();
            assertThat(viewCacheKey.forEnvironmentPipelineBox(model))
                    .isEqualTo("com.thoughtworks.go.server.ui.ViewCacheKey.$view.$environmentPipelineFragment.$pipelineName.${false|false|false}.$[12|stageName|13|Building|stage2|0|Unknown|].$true.$true.$false.$.$.$true|true");

            model.updateAdministrability(false);
            assertThat(viewCacheKey.forEnvironmentPipelineBox(model))
                    .isEqualTo("com.thoughtworks.go.server.ui.ViewCacheKey.$view.$environmentPipelineFragment.$pipelineName.${false|false|false}.$[12|stageName|13|Building|stage2|0|Unknown|].$true.$true.$false.$.$.$false|true");
        }
    }

    @Nested
    class ForPipelineModelBuildCauses {
        @Test
        void shouldGenerateCacheKey() {
            final PipelinePauseInfo pipelinePauseInfo = new PipelinePauseInfo(true, "Paused by Bob", "bob");
            final PipelineModel pipelineModel = new PipelineModel("up42,", true, true, pipelinePauseInfo);

            final PipelineInstanceModel pipelineInstanceModelOne = new PipelineInstanceModel("up42", 1, "label-1", BuildCause.createManualForced(), new StageInstanceModels());
            pipelineInstanceModelOne.setId(12);
            TrackingTool trackingTool = new TrackingTool("link", "regex");
            pipelineInstanceModelOne.setTrackingTool(trackingTool);
            pipelineModel.addPipelineInstance(pipelineInstanceModelOne);

            final PipelineInstanceModel pipelineInstanceModelTwo = new PipelineInstanceModel("up42", 2, "label-2", BuildCause.createManualForced(), new StageInstanceModels());
            pipelineInstanceModelTwo.setId(14);
            pipelineModel.addPipelineInstance(pipelineInstanceModelTwo);

            assertThat(viewCacheKey.forPipelineModelBuildCauses(pipelineModel))
                    .isEqualTo(String.format("com.thoughtworks.go.server.ui.ViewCacheKey.$view.$buildCausesForPipelineModel.$up42,.$[12|%s][14|%s]", trackingTool.hashCode(), -1));
        }
    }

    private StageInstanceModel stageInstance(String name, int id, JobState state, JobResult jobResult) {
        JobHistory jobs = new JobHistory();
        jobs.addJob("dev", state, jobResult, new Date());
        StageInstanceModel stageInstance = new StageInstanceModel(name, "2", jobs);
        stageInstance.setId(id);
        return stageInstance;
    }

}
