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
package com.thoughtworks.go.presentation.pipelinehistory;

import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.MaterialsMother;
import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;

import static com.thoughtworks.go.domain.buildcause.BuildCause.createWithEmptyModifications;
import static com.thoughtworks.go.helper.PipelineHistoryMother.job;
import static com.thoughtworks.go.helper.PipelineHistoryMother.stagePerJob;
import static org.assertj.core.api.Assertions.assertThat;

class PipelineInstanceModelTest {
    private static final Modification HG_MATERIAL_MODIFICATION = new Modification("user", "Comment", "email", new Date(), "a087402bd2a7828a130c1bdf43f2d9ef8f48fd46");

    @Test
    void shouldUnderstandActiveStage() {
        StageInstanceModels stages = new StageInstanceModels();

        stages.addStage("unit1", JobHistory.withJob("test", JobState.Completed, JobResult.Passed, new Date()));

        JobHistory history = new JobHistory();
        history.add(new JobHistoryItem("test-1", JobState.Completed, JobResult.Failed, new Date()));
        history.add(new JobHistoryItem("test-2", JobState.Building, JobResult.Unknown, new Date()));
        StageInstanceModel activeStage = new StageInstanceModel("unit2", "1", history);
        stages.add(activeStage);

        stages.addFutureStage("unit3", false);

        PipelineInstanceModel model = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createManualForced(), stages);

        assertThat(model.activeStage()).isEqualTo(activeStage);
    }

    @Test
    void shouldReturnNullWhenNoActiveStage() {
        StageInstanceModels stages = new StageInstanceModels();

        stages.addStage("unit1", JobHistory.withJob("test", JobState.Completed, JobResult.Passed, new Date()));

        JobHistory history = new JobHistory();
        history.add(new JobHistoryItem("test-1", JobState.Completed, JobResult.Failed, new Date()));
        history.add(new JobHistoryItem("test-2", JobState.Completed, JobResult.Passed, new Date()));
        stages.add(new StageInstanceModel("unit2", "1", history));

        stages.addFutureStage("unit3", false);

        PipelineInstanceModel model = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createManualForced(), stages);

        assertThat(model.activeStage()).isNull();
    }

    @Test
    void shouldGetCurrentMaterialRevisionForMaterial() {
        MaterialRevisions revisions = new MaterialRevisions();
        HgMaterial material = MaterialsMother.hgMaterial();
        revisions.addRevision(material, HG_MATERIAL_MODIFICATION);
        PipelineInstanceModel model = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createWithModifications(revisions, ""), new StageInstanceModels());

        assertThat(model.findCurrentMaterialRevisionForUI(material.config())).isEqualTo(revisions.getMaterialRevision(0));
    }

    @Test
    void shouldFallbackToPipelineFingerprintWhenGettingCurrentMaterialRevisionForMaterialIsNull() {
        MaterialRevisions revisions = new MaterialRevisions();
        HgMaterial material = MaterialsMother.hgMaterial();
        HgMaterial materialWithDifferentDest = MaterialsMother.hgMaterial();
        materialWithDifferentDest.setFolder("otherFolder");
        revisions.addRevision(material, HG_MATERIAL_MODIFICATION);
        PipelineInstanceModel model = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createWithModifications(revisions, ""), new StageInstanceModels());

        assertThat(model.findCurrentMaterialRevisionForUI(materialWithDifferentDest.config())).isEqualTo(revisions.getMaterialRevision(0));
    }

    @Test
    void shouldGetLatestMaterialRevisionForMaterial() {
        HgMaterial material = MaterialsMother.hgMaterial();
        assertThat(setUpModificationForHgMaterial().getLatestMaterialRevision(material.config())).isEqualTo(new MaterialRevision(material, HG_MATERIAL_MODIFICATION));
    }

    @Test
    void shouldGetLatestRevisionForMaterial() {
        HgMaterialConfig hgMaterialConfig = MaterialConfigsMother.hgMaterialConfig();
        assertThat(setUpModificationForHgMaterial().getLatestRevision(hgMaterialConfig).getRevision()).isEqualTo("a087402bd2a7828a130c1bdf43f2d9ef8f48fd46");
    }

    @Test
    void shouldGetLatestRevisionForMaterialWithNoModifications() {
        assertThat(hgMaterialWithNoModifications().getLatestRevision(MaterialConfigsMother.hgMaterialConfig()).getRevision()).isEqualTo("No historical data");
    }

    @Test
    void shouldKnowIfLatestRevisionIsReal() {
        assertThat(setUpModificationForHgMaterial().hasModificationsFor(MaterialConfigsMother.hgMaterialConfig())).isTrue();
    }

    @Test
    void shouldKnowApproverAsApproverForTheFirstStage() {
        MaterialRevisions revisions = new MaterialRevisions();
        StageInstanceModels models = new StageInstanceModels();
        StageInstanceModel firstStage = new StageInstanceModel("dev", "1", new JobHistory());
        firstStage.setApprovedBy("some_user");
        models.add(firstStage);
        PipelineInstanceModel model = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createWithModifications(revisions, ""), models);
        assertThat(model.getApprovedBy()).isEqualTo("some_user");
        assertThat(model.getApprovedByForDisplay()).isEqualTo("Triggered by some_user");
    }

    @SuppressWarnings("SameParameterValue")
    private static ZonedDateTime newDate(int year, int month, int dayOfMonth) {
        return ZonedDateTime.of(year, month, dayOfMonth, 0, 0, 0, 0, ZoneId.systemDefault());
    }

    @Test
    void shouldReturnTheLatestStageEvenWhenThereIsANullStage() {
        ZonedDateTime occurredFirst = ZonedDateTime.now().minusDays(1);
        ZonedDateTime occurredSecond = ZonedDateTime.now();
        StageInstanceModels stageInstanceModels = stagePerJob("stage", job(JobResult.Passed, occurredSecond.toInstant()), job(JobResult.Passed, occurredFirst.toInstant()));
        NullStageHistoryItem stageHistoryItem = new NullStageHistoryItem("not_yet_run", false);
        stageInstanceModels.add(stageHistoryItem);

        PipelineInstanceModel instanceModel = PipelineInstanceModel.createPipeline("pipeline", -1, "label", createWithEmptyModifications(), stageInstanceModels);
        StageInstanceModel value = stageInstanceModels.getFirst();
        assertThat(instanceModel.latestStage()).isEqualTo(value);
    }

    @Test
    void shouldSetAndGetComment() {
        PipelineInstanceModel pim = PipelineInstanceModel.createEmptyModel();
        pim.setComment("test comment");
        assertThat(pim.getComment()).as("PipelineInstanceModel.getComment()").isEqualTo("test comment");
    }

    @Test
    void shouldSetTheBuildCauseMessageIfBuildCauseIsNull() {
        PipelineInstanceModel model = PipelineInstanceModel.createEmptyModel();

        assertThat(model.getBuildCause()).isNull();

        model.setBuildCauseMessage("some build cause msg");
        assertThat(model.getBuildCause()).isNotNull();
        assertThat(model.getBuildCauseMessage()).isEqualTo("some build cause msg");
    }

    private PipelineInstanceModel setUpModificationForHgMaterial() {
        MaterialRevisions revisions = new MaterialRevisions();

        revisions.addRevision(MaterialsMother.hgMaterial(), HG_MATERIAL_MODIFICATION);
        PipelineInstanceModel model = PipelineInstanceModel.createPipeline("pipeline", -1, "label", null, new StageInstanceModels());
        model.setLatestRevisions(revisions);
        return model;
    }

    private PipelineInstanceModel hgMaterialWithNoModifications() {
        MaterialRevisions revisions = new MaterialRevisions();

        revisions.addRevision(MaterialsMother.hgMaterial(), new ArrayList<>());
        PipelineInstanceModel model = PipelineInstanceModel.createPipeline("pipeline", -1, "label", null, new StageInstanceModels());
        model.setLatestRevisions(revisions);
        return model;
    }

    private PipelineInstanceModel setUpModificationFor(HgMaterial material) {
        MaterialRevisions revisions = new MaterialRevisions();

        revisions.addRevision(material, HG_MATERIAL_MODIFICATION);
        return PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createWithModifications(revisions, ""), new StageInstanceModels());
    }
}
