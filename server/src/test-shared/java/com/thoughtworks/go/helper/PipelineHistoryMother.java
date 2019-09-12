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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.presentation.pipelinehistory.*;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.InstanceFactory;
import com.thoughtworks.go.server.service.MaterialConfigConverter;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.TimeProvider;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.thoughtworks.go.config.CaseInsensitiveString.str;
import static com.thoughtworks.go.helper.ModificationsMother.*;
import static com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel.createPipeline;

public class PipelineHistoryMother {
    public static final String REVISION = "svn.100";
    public static final String APPROVED_BY = "lgao";
    private static String md5 = "md5-test";

    public static PipelineInstanceModels pipelineHistory(PipelineConfig pipelineConfig, Date modificationDate) {
        return pipelineHistory(pipelineConfig, modificationDate, modificationDate);
    }

    public static PipelineInstanceModels pipelineHistory(PipelineConfig pipelineConfig, Date scheduleDate, Date modificationDate) {
        return pipelineHistory(pipelineConfig, scheduleDate, modificationDate, REVISION);
    }

    public static PipelineInstanceModels pipelineHistory(PipelineConfig pipelineConfig, Date scheduleDate, Date modificationDate, String revision) {
        return pipelineHistory(pipelineConfig, scheduleDate, modificationDate, revision, "user", "Comment", "email", "file", "dir", "1");
    }

    public static PipelineInstanceModels pipelineHistory(PipelineConfig pipelineConfig, Date scheduleDate, Date modificationDate, String revision, String committer, String commitMessage,
                                                         String commiterEmail, String commitedFileName, String dirModified, String label) {
        PipelineInstanceModels history = PipelineInstanceModels.createPipelineInstanceModels();
        Modification modification = new Modification(committer, commitMessage, commiterEmail, modificationDate, revision);
        modification.createModifiedFile(commitedFileName, dirModified, ModifiedAction.added);
        MaterialRevisions revisions = new MaterialRevisions();
        Material material = new MaterialConfigConverter().toMaterial(pipelineConfig.materialConfigs().first());
        material.setId(10);
        revisions.addRevision(material, modification);
        BuildCause buildCause = BuildCause.createManualForced(revisions, Username.ANONYMOUS);
        PipelineInstanceModel item = createPipeline(str(pipelineConfig.name()), -1, label, buildCause, stageHistory(pipelineConfig, scheduleDate));
        item.setCounter(1);
        item.setId(1);
        item.setComment("build comment");
        history.add(item);
        return history;
    }

    public static PipelineInstanceModel pipelineInstanceModel(String pipelineName, Integer pipelineCounter, Date scheduled) {
        MaterialRevisions revisions = createSvnMaterialWithMultipleRevisions(-1, multipleModificationList().toArray(new Modification[0]));
        BuildCause buildCause = BuildCause.createManualForced(revisions, Username.ANONYMOUS);
        List<String> stages = List.of("unit-tests", "integration-tests", "functional-tests");

        StageInstanceModels stageInstanceModels = new StageInstanceModels();
        for (int i = 0; i < stages.size(); i++) {
            stageInstanceModels.add(stageInstanceModel(pipelineName, pipelineCounter, stages.get(i), "51", scheduled));
        }
        return createPipeline(pipelineName, pipelineCounter, null, buildCause, stageInstanceModels);
    }

    public static PipelineInstanceModels pipelineHistoryWithErrorMessage(PipelineConfig pipelineConfig, Date modificationDate) {
        PipelineInstanceModels history = PipelineInstanceModels.createPipelineInstanceModels();
        Modification modification = new Modification("user", "Comment", "email", modificationDate, REVISION);
        modification.createModifiedFile("file", "dir", ModifiedAction.added);
        MaterialRevisions revisions = new MaterialRevisions();
        Material material = new MaterialConfigConverter().toMaterial(pipelineConfig.materialConfigs().first());
        material.setId(10);
        revisions.addRevision(material, modification);
        BuildCause buildCause = BuildCause.createManualForced(revisions, Username.ANONYMOUS);
        PipelineInstanceModel item = createPipeline(str(pipelineConfig.name()), -1, "1", buildCause, stageHistoryWithErrorMessage(pipelineConfig, modificationDate));
        item.setCounter(1);
        item.setId(1);
        item.setComment("build comment");
        history.add(item);
        return history;
    }

    public static StageInstanceModels stageHistoryWithErrorMessage(PipelineConfig pipelineConfig, Date modificationDate) {
        StageInstanceModels history = new StageInstanceModels();
        StageConfig devConfig = pipelineConfig.get(0);
        StageInstanceModel devModel = new StageInstanceModel(str(devConfig.name()), "1", buildCancelledHistory(devConfig, modificationDate));
        devModel.setCounter("1");
        devModel.setCanRun(true);
        devModel.setApprovalType("success");
        devModel.setApprovedBy(GoConstants.DEFAULT_APPROVED_BY);
        history.add(devModel);


        StageConfig ftConfig = pipelineConfig.get(1);
        StageInstanceModel ftModel = new StageInstanceModel(str(ftConfig.name()), "1", buildUnknownHistory(ftConfig, modificationDate));
        ftModel.setCounter("1");
        ftModel.setApprovalType("manual");
        ftModel.setApprovedBy("");
        ftModel.setErrorMessage("Cannot schedule ft as the previous stage dev has Cancelled!");
        ftModel.setScheduled(false);
        history.add(ftModel);

        return history;
    }

    public static StageInstanceModels stageHistory(PipelineConfig pipelineConfig, Date modificationDate) {
        StageInstanceModels history = new StageInstanceModels();
        for (StageConfig stageConfig : pipelineConfig) {
            StageInstanceModel item = new StageInstanceModel(str(stageConfig.name()), "1", buildHistory(stageConfig, modificationDate));
            item.setCounter("1");
            item.setApprovalType(new InstanceFactory().createStageInstance(stageConfig, new DefaultSchedulingContext("anyone"), md5, new TimeProvider()).getApprovalType());
            if (stageConfig.requiresApproval()) {
                item.setApprovedBy(APPROVED_BY);
            } else {
                item.setApprovedBy(GoConstants.DEFAULT_APPROVED_BY);
            }
            history.add(item);
        }
        return history;
    }

    public static StageInstanceModel stageInstanceModel(String pipelineName, Integer pipelineCounter, String stageName,
                                                        String stageCounter, Date scheduled) {
        StageIdentifier stageIdentifier = new StageIdentifier(pipelineName, pipelineCounter, stageName, stageCounter);
        JobHistory jobHistory = new JobHistory();
        jobHistory.addJob(stageName + "-job", JobState.Completed, JobResult.Passed, scheduled);
        StageInstanceModel stageInstanceModel = new StageInstanceModel(stageName, stageCounter, jobHistory, stageIdentifier);
        stageInstanceModel.setApprovedBy("changes");
        return stageInstanceModel;
    }

    public static JobHistory buildHistory(StageConfig stageConfig, Date modificationDate) {
        JobHistory history = new JobHistory();
        for (JobConfig jobConfig : stageConfig.allBuildPlans()) {
            history.addJob(str(jobConfig.name()), JobState.Completed, JobResult.Passed, modificationDate);
        }
        return history;
    }

    public static JobHistory buildCancelledHistory(StageConfig stageConfig, Date modificationDate) {
        JobHistory history = new JobHistory();
        for (JobConfig jobConfig : stageConfig.allBuildPlans()) {
            history.addJob(str(jobConfig.name()), JobState.Unknown, JobResult.Cancelled, modificationDate);
        }
        return history;
    }

    public static JobHistory buildUnknownHistory(StageConfig stageConfig, Date modificationDate) {
        JobHistory history = new JobHistory();
        for (JobConfig jobConfig : stageConfig.allBuildPlans()) {
            history.addJob(str(jobConfig.name()), JobState.Unknown, JobResult.Unknown, modificationDate);
        }
        return history;
    }

    public static PipelineInstanceModel pipelineHistoryItemWithOneStage(String pipelineName, String stageName, Date modifiedDate) {
        StageInstanceModels stageHistory = new StageInstanceModels();
        stageHistory.add(new StageInstanceModel(stageName, "1", StageResult.Passed, new StageIdentifier(pipelineName, 1, "1", stageName, "1")));
        return singlePipeline(pipelineName, stageHistory, modifiedDate);
    }

    public static PipelineInstanceModel singlePipeline(String pipelineName, StageInstanceModels stages) {
        return singlePipeline(pipelineName, stages, new Date());
    }

    public static PipelineInstanceModel singlePipeline(String pipelineName, StageInstanceModels stages, Date modifiedDate) {
        BuildCause manualForced = BuildCause.createManualForced(new MaterialRevisions(new MaterialRevision(MaterialsMother.hgMaterial(), new Modification(modifiedDate, "abc", "MOCK_LABEL-12", null))), Username.ANONYMOUS);
        PipelineInstanceModel model = createPipeline(pipelineName, -1, "1", manualForced, stages);
        model.setCounter(1);
        return model;
    }

    public static PipelineInstanceModel singlePipeline(String pipelineName, StageInstanceModel stage) {
        StageInstanceModels stageInstanceModels = new StageInstanceModels();
        stageInstanceModels.add(stage);
        return singlePipeline(pipelineName, stageInstanceModels, new Date());
    }

    public static StageInstanceModels stagePerJob(String baseName, List<JobHistory> histories) {
        StageInstanceModels stageInstanceModels = new StageInstanceModels();
        for (int i = 0; i < histories.size(); i++) {
            String stageName = baseName + "-" + i;
            StageInstanceModel stage = new StageInstanceModel(stageName, "1", StageResult.Passed, new StageIdentifier("pipeline", 1, "1", stageName, "1"));
            stage.setBuildHistory(histories.get(i));
            stageInstanceModels.add(stage);
            stageInstanceModels.latestStage().setApprovedBy("cruise");
        }
        return stageInstanceModels;
    }


    public static StageInstanceModels stagePerJob(String baseName, JobHistory... histories) {
        return stagePerJob(baseName, Arrays.asList(histories));
    }

    public static JobHistory job(JobResult result, Date scheduledDate) {
        return job(JobState.Completed, result, scheduledDate);
    }

    public static JobHistory job(JobResult result) {
        return job(result, new Date());
    }

    public static JobHistory job(JobState state, JobResult result) {
        return job(state, result, new Date());
    }

    public static JobHistory job(JobState state, JobResult result, Date scheduledDate) {
        return JobHistory.withJob("firstJob", state, result, scheduledDate);
    }

    public static PipelineModel pipelineWithLatestRevision(MaterialRevisions latest) {
        PipelineModel pipelineModel = pipeline();
        pipelineModel.getLatestPipelineInstance().setLatestRevisions(latest);
        return pipelineModel;
    }

    public static PipelineModel pipelineWithLatestRevisionAndMaterialRevision(MaterialRevisions latest, MaterialRevisions revision) {
        PipelineModel pipelineModel = pipelineWithLatestRevision(latest);
        pipelineModel.getLatestPipelineInstance().setMaterialRevisionsOnBuildCause(revision);
        return pipelineModel;
    }

    public static PipelineModel pipeline() {
        PipelineModel pipelineModel = new PipelineModel("pipe1", true, true, PipelinePauseInfo.notPaused());
        MaterialRevisions materialRevisions = createHgMaterialRevisions();
        PipelineInstanceModel instanceModel = createPipeline("pipe1", -1, "label1", BuildCause.createWithModifications(materialRevisions, "foo-bar"), new StageInstanceModels());
        instanceModel.setMaterialConfigs(new MaterialConfigs(materialRevisions.getMaterialRevision(0).getMaterial().config()));
        instanceModel.setLatestRevisions(materialRevisions);
        pipelineModel.addPipelineInstance(instanceModel);
        return pipelineModel;
    }
}
