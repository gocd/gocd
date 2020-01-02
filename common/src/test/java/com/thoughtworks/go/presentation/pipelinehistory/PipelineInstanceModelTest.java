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
package com.thoughtworks.go.presentation.pipelinehistory;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.helper.PipelineHistoryMother;
import org.joda.time.DateTime;
import org.junit.Test;

import static com.thoughtworks.go.domain.buildcause.BuildCause.createWithEmptyModifications;
import static com.thoughtworks.go.helper.MaterialsMother.hgMaterial;
import static com.thoughtworks.go.helper.MaterialsMother.svnMaterial;
import static com.thoughtworks.go.helper.PipelineHistoryMother.job;
import static com.thoughtworks.go.helper.PipelineHistoryMother.stagePerJob;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class PipelineInstanceModelTest {
    private static final Modification HG_MATERIAL_MODIFICATION = new Modification("user", "Comment", "email", new Date(), "a087402bd2a7828a130c1bdf43f2d9ef8f48fd46");

    @Test
    public void shouldUnderstandActiveStage() {
        StageInstanceModels stages = new StageInstanceModels();

        stages.addStage("unit1", JobHistory.withJob("test", JobState.Completed, JobResult.Passed, new Date()));

        JobHistory history = new JobHistory();
        history.add(new JobHistoryItem("test-1", JobState.Completed, JobResult.Failed, new Date()));
        history.add(new JobHistoryItem("test-2", JobState.Building, JobResult.Unknown, new Date()));
        StageInstanceModel activeStage = new StageInstanceModel("unit2", "1", history);
        stages.add(activeStage);

        stages.addFutureStage("unit3", false);

        PipelineInstanceModel model = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createManualForced(), stages);

        assertThat(model.activeStage(), is(activeStage));
    }

    @Test
    public void shouldReturnNullWhenNoActiveStage() {
        StageInstanceModels stages = new StageInstanceModels();

        stages.addStage("unit1", JobHistory.withJob("test", JobState.Completed, JobResult.Passed, new Date()));

        JobHistory history = new JobHistory();
        history.add(new JobHistoryItem("test-1", JobState.Completed, JobResult.Failed, new Date()));
        history.add(new JobHistoryItem("test-2", JobState.Completed, JobResult.Passed, new Date()));
        stages.add(new StageInstanceModel("unit2", "1", history));

        stages.addFutureStage("unit3", false);

        PipelineInstanceModel model = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createManualForced(), stages);

        assertThat(model.activeStage(), is(nullValue()));
    }

    @Test public void shouldUnderstandPipelineStatusMessage() throws Exception {
        MaterialRevisions revisions = ModificationsMother.modifyOneFile(MaterialsMother.hgMaterials("url"), "revision");

        StageInstanceModels stages = new StageInstanceModels();
        stages.addStage("unit1", JobHistory.withJob("test", JobState.Completed, JobResult.Passed, new Date()));
        stages.addFutureStage("unit2", false);

        PipelineInstanceModel model = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createWithModifications(revisions, ""), stages);

        assertThat(model.getPipelineStatusMessage(), is("Passed: unit1"));
    }


    @Test
    public void shouldGetCurrentRevisionForMaterial() {
        MaterialRevisions revisions = new MaterialRevisions();
        HgMaterial material = MaterialsMother.hgMaterial();
        revisions.addRevision(material, HG_MATERIAL_MODIFICATION);
        PipelineInstanceModel model = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createWithModifications(revisions, ""), new StageInstanceModels());

        assertThat(model.getCurrentRevision(material.config()).getRevision(), is("a087402bd2a7828a130c1bdf43f2d9ef8f48fd46"));
    }

    @Test
    public void shouldGetCurrentMaterialRevisionForMaterial() {
        MaterialRevisions revisions = new MaterialRevisions();
        HgMaterial material = MaterialsMother.hgMaterial();
        revisions.addRevision(material, HG_MATERIAL_MODIFICATION);
        PipelineInstanceModel model = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createWithModifications(revisions, ""), new StageInstanceModels());

        assertThat(model.findCurrentMaterialRevisionForUI(material.config()), is(revisions.getMaterialRevision(0)));
    }

    @Test
    public void shouldFallbackToPipelineFingerpringWhenGettingCurrentMaterialRevisionForMaterialIsNull() {
        MaterialRevisions revisions = new MaterialRevisions();
        HgMaterial material = MaterialsMother.hgMaterial();
        HgMaterial materialWithDifferentDest = MaterialsMother.hgMaterial();
        materialWithDifferentDest.setFolder("otherFolder");
        revisions.addRevision(material, HG_MATERIAL_MODIFICATION);
        PipelineInstanceModel model = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createWithModifications(revisions, ""), new StageInstanceModels());

        assertThat(model.findCurrentMaterialRevisionForUI(materialWithDifferentDest.config()), is(revisions.getMaterialRevision(0)));
    }

    @Test
    public void shouldGetCurrentRevisionForMaterialByName() {
        MaterialRevisions revisions = new MaterialRevisions();
        HgMaterial material = MaterialsMother.hgMaterial();
        SvnMaterial svnMaterial = MaterialsMother.svnMaterial();
        material.setName(new CaseInsensitiveString("hg_material"));
        revisions.addRevision(svnMaterial, new Modification(new Date(), "1024", "MOCK_LABEL-12", null));
        revisions.addRevision(material, HG_MATERIAL_MODIFICATION);
        BuildCause buildCause = BuildCause.createWithModifications(revisions, "");
        PipelineInstanceModel model = PipelineInstanceModel.createPipeline("pipeline", -1, "label", buildCause, new StageInstanceModels());

        assertThat(model.getCurrentRevision("hg_material").getRevision(), is("a087402bd2a7828a130c1bdf43f2d9ef8f48fd46"));
    }

    @Test
    public void shouldGetLatestMaterialRevisionForMaterial() {
        HgMaterial material = MaterialsMother.hgMaterial();
        assertThat(setUpModificationForHgMaterial().getLatestMaterialRevision(material.config()), is(new MaterialRevision(material, HG_MATERIAL_MODIFICATION)));
    }

    @Test
    public void shouldGetLatestRevisionForMaterial() {
        HgMaterialConfig hgMaterialConfig = MaterialConfigsMother.hgMaterialConfig();
        assertThat(setUpModificationForHgMaterial().getLatestRevision(hgMaterialConfig).getRevision(), is("a087402bd2a7828a130c1bdf43f2d9ef8f48fd46"));
    }

    @Test
    public void shouldGetLatestRevisionForMaterialWithNoModifications() {
        assertThat(hgMaterialWithNoModifications().getLatestRevision(MaterialConfigsMother.hgMaterialConfig()).getRevision(), is("No historical data"));
    }

    @Test
    public void shouldGetCurrentRevisionForMaterialName() {
        HgMaterial material = MaterialsMother.hgMaterial();
        material.setName(new CaseInsensitiveString("foo"));
        assertThat(setUpModificationFor(material).getCurrentRevision(CaseInsensitiveString.str(material.getName())).getRevision(), is("a087402bd2a7828a130c1bdf43f2d9ef8f48fd46"));
    }

    @Test
    public void shouldThrowExceptionWhenCurrentRevisionForUnknownMaterialNameRequested() {
        HgMaterial material = MaterialsMother.hgMaterial();
        material.setName(new CaseInsensitiveString("foo"));
        try {
            assertThat(setUpModificationFor(material).getCurrentRevision("blah").getRevision(), is("a087402bd2a7828a130c1bdf43f2d9ef8f48fd46"));
            fail("should have raised an exeption for unknown material name");
        } catch (Exception ignored) {
        }
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

    @Test public void shouldKnowIfLatestRevisionIsReal() throws Exception {
        assertThat(setUpModificationForHgMaterial().hasModificationsFor(MaterialConfigsMother.hgMaterialConfig()), is(true));
    }

    @Test
    public void shouldKnowApproverAsApproverForTheFirstStage() {
        MaterialRevisions revisions = new MaterialRevisions();
        StageInstanceModels models = new StageInstanceModels();
        StageInstanceModel firstStage = new StageInstanceModel("dev", "1", new JobHistory());
        firstStage.setApprovedBy("some_user");
        models.add(firstStage);
        PipelineInstanceModel model = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createWithModifications(revisions, ""), models);
        assertThat(model.getApprovedBy(), is("some_user"));
        assertThat(model.getApprovedByForDisplay(), is("Triggered by some_user"));
    }

    @Test
    public void shouldUnderstandIfReal() {
        assertThat(PipelineInstanceModel.createEmptyModel().hasHistoricalData(), is(true));
        assertThat(PipelineInstanceModel.createEmptyPipelineInstanceModel("pipeline", createWithEmptyModifications(), new StageInstanceModels()).hasHistoricalData(), is(false));
    }

    //Pipeline: Red -> Green -> Has_Not_Run_Yet
    @Test
    public void shouldBeSucessfulOnAForceContinuedPass_Red_AND_Green_AND_Has_Not_Run_Yet() {
        Date occuredFirst = new Date(2008, 12, 13);
        Date occuredSecond = new Date(2008, 12, 14);

        StageInstanceModels stageInstanceModels = stagePerJob("stage", job(JobResult.Failed, occuredFirst), job(JobResult.Passed, occuredSecond));
        stageInstanceModels.add(new NullStageHistoryItem("stage-3", false));

        PipelineInstanceModel instanceModel = PipelineInstanceModel.createPipeline("pipeline", -1, "label", createWithEmptyModifications(), stageInstanceModels);

        assertThat(instanceModel.isLatestStageUnsuccessful(), is(false));
        assertThat(instanceModel.isLatestStageSuccessful(), is(true));
        assertThat(instanceModel.isRunning(), is(true));
    }

    //Pipeline: Red(Rerun after second stage passed i.e. latest stage) -> Green -> Has_Not_Run_Yet
    @Test
    public void shouldReturnStatusOfAfailedRerunAndIncompleteStage() {
        Date occuredFirst = new Date(2008, 12, 13);
        Date occuredSecond = new Date(2008, 12, 14);

        StageInstanceModels stageInstanceModels = stagePerJob("stage", job(JobResult.Failed, occuredSecond), job(JobResult.Passed, occuredFirst));
        stageInstanceModels.add(new NullStageHistoryItem("stage-3", false));
        PipelineInstanceModel instanceModel = PipelineInstanceModel.createPipeline("pipeline", -1, "label", createWithEmptyModifications(), stageInstanceModels);

        assertThat(instanceModel.isLatestStageUnsuccessful(), is(true));
        assertThat(instanceModel.isLatestStageSuccessful(), is(false));
        assertThat(instanceModel.isRunning(), is(true));
    }

    @Test
    public void shouldReturnStatusOfAFailedRerun() {
        Date occuredFirst = new Date(2008, 12, 13);
        Date occuredSecond = new Date(2008, 12, 14);

        StageInstanceModels stageInstanceModels = stagePerJob("stage", job(JobResult.Failed, occuredSecond), job(JobResult.Passed, occuredFirst));


        PipelineInstanceModel instanceModel = PipelineInstanceModel.createPipeline("pipeline", -1, "label", createWithEmptyModifications(), stageInstanceModels);

        assertThat(instanceModel.isLatestStageUnsuccessful(), is(true));
        assertThat(instanceModel.isLatestStageSuccessful(), is(false));
        assertThat(instanceModel.isRunning(), is(false));
    }

    @Test
    public void shouldReturnStatusOfAPassedForceThrough() {
        Date occuredFirst = new Date(2008, 12, 13);
        Date occuredSecond = new Date(2008, 12, 14);

        StageInstanceModels stageInstanceModels = stagePerJob("stage", job(JobResult.Failed, occuredFirst), job(JobResult.Passed, occuredSecond));


        PipelineInstanceModel instanceModel = PipelineInstanceModel.createPipeline("pipeline", -1, "label", createWithEmptyModifications(), stageInstanceModels);

        assertThat(instanceModel.isLatestStageUnsuccessful(), is(false));
        assertThat(instanceModel.isLatestStageSuccessful(), is(true));
        assertThat(instanceModel.isRunning(), is(false));
    }

    @Test
    public void shouldReturnPipelineStatusAsPassedWhenAllTheStagesPass() {
        Date occuredFirst = new Date(2008, 12, 13);
        Date occuredSecond = new Date(2008, 12, 14);

        StageInstanceModels stageInstanceModels = stagePerJob("stage", job(JobResult.Passed, occuredSecond), job(JobResult.Passed, occuredFirst));


        PipelineInstanceModel instanceModel = PipelineInstanceModel.createPipeline("pipeline", -1, "label", createWithEmptyModifications(), stageInstanceModels);

        assertThat(instanceModel.isLatestStageUnsuccessful(), is(false));
        assertThat(instanceModel.isLatestStageSuccessful(), is(true));
        assertThat(instanceModel.isRunning(), is(false));
    }

    @Test
    public void shouldReturnTheLatestStageEvenWhenThereIsANullStage() {
        Date occuredFirst = new DateTime().minusDays(1).toDate();
        Date occuredSecond = new DateTime().toDate();
        StageInstanceModels stageInstanceModels = stagePerJob("stage", job(JobResult.Passed, occuredSecond), job(JobResult.Passed, occuredFirst));
        NullStageHistoryItem stageHistoryItem = new NullStageHistoryItem("not_yet_run", false);
        stageInstanceModels.add(stageHistoryItem);

        PipelineInstanceModel instanceModel = PipelineInstanceModel.createPipeline("pipeline", -1, "label", createWithEmptyModifications(), stageInstanceModels);
        StageInstanceModel value = stageInstanceModels.get(0);
        assertThat(instanceModel.latestStage(), is(value));
    }

    @Test
    public void shouldReturnIfAStageIsLatest() {
        Date occuredFirst = new DateTime().minusDays(1).toDate();
        Date occuredSecond = new DateTime().toDate();

        StageInstanceModels stageInstanceModels = stagePerJob("stage", job(JobResult.Passed, occuredSecond), job(JobResult.Passed, occuredFirst));
        NullStageHistoryItem stageHistoryItem = new NullStageHistoryItem("not_yet_run", false);
        stageInstanceModels.add(stageHistoryItem);

        PipelineInstanceModel instanceModel = PipelineInstanceModel.createPipeline("pipeline", -1, "label", createWithEmptyModifications(), stageInstanceModels);

        assertThat(instanceModel.isLatestStage(stageInstanceModels.get(0)), is(true));
        assertThat(instanceModel.isLatestStage(stageInstanceModels.get(1)), is(false));
    }

    @Test
    public void shouldReturnIfAnyMaterialHasModifications() {
        final SvnMaterial svnMaterial = svnMaterial("http://svnurl");
        final HgMaterial hgMaterial = hgMaterial("http://hgurl", "hgdir");

        MaterialRevisions currentRevisions = ModificationsMother.getMaterialRevisions(new HashMap<Material, String>() {{
            put(svnMaterial, "1");
            put(hgMaterial, "a");
        }});

        MaterialRevisions latestRevisions = ModificationsMother.getMaterialRevisions(new HashMap<Material, String>() {{
            put(svnMaterial, "1");
            put(hgMaterial, "b");
        }});

        MaterialConfigs materialConfigs = new MaterialConfigs();
        materialConfigs.add(svnMaterial.config());
        materialConfigs.add(hgMaterial.config());

        StageInstanceModels stages = new StageInstanceModels();
        stages.addStage("unit1", JobHistory.withJob("test", JobState.Completed, JobResult.Passed, new Date()));
        stages.addFutureStage("unit2", false);

        PipelineInstanceModel model = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createWithModifications(currentRevisions, ""), stages);
        model.setLatestRevisions(latestRevisions);
        model.setMaterialConfigs(materialConfigs);

        assertThat("svnMaterial hasNewRevisions", model.hasNewRevisions(svnMaterial.config()), is(false));
        assertThat("hgMaterial hasNewRevisions", model.hasNewRevisions(hgMaterial.config()), is(true));
        assertThat("all materials hasNewRevisions", model.hasNewRevisions(), is(true));
    }

    @Test
    public void shouldUnderstandIfItHasNeverCheckedForRevisions() {
        StageInstanceModels stages = new StageInstanceModels();
        stages.addStage("unit1", JobHistory.withJob("test", JobState.Completed, JobResult.Passed, new Date()));
        stages.addFutureStage("unit2", false);

        PipelineInstanceModel pim = PipelineInstanceModel.createPipeline("pipeline", -1, "label", BuildCause.createNeverRun(), stages);
        pim.setLatestRevisions(MaterialRevisions.EMPTY);

        assertThat("pim.hasNeverCheckedForRevisions()", pim.hasNeverCheckedForRevisions(), is(true));

    }
    @Test
    public void shouldReturnTrueIfThePipelineHasStage() {
        PipelineInstanceModel pim = PipelineHistoryMother.pipelineHistoryItemWithOneStage("pipeline", "stage", new Date());
        assertThat(pim.hasStage(pim.getStageHistory().first().getIdentifier()), is(true));
        assertThat(pim.hasStage(new StageIdentifier("pipeline",1,"1", "stagex", "2")), is(false));

    }

    @Test
    public void shouldSetAndGetComment() {
        PipelineInstanceModel pim = PipelineInstanceModel.createEmptyModel();
        pim.setComment("test comment");
        assertThat("PipelineInstanceModel.getComment()", pim.getComment(), is("test comment"));
    }
}
