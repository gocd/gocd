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

package com.thoughtworks.go.server.ui;

import java.util.Date;

import com.thoughtworks.go.config.MingleConfig;
import com.thoughtworks.go.config.TrackingTool;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.presentation.pipelinehistory.JobHistory;
import com.thoughtworks.go.presentation.pipelinehistory.NullStageHistoryItem;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineModel;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ViewCacheKeyTest {
    private ViewCacheKey viewCacheKey;

    @Before
    public void setUp() {
        viewCacheKey = new ViewCacheKey();
    }

    @Test
    public void shouldUseFormatAsAPartOfStageFbhKey() {
        Stage stage = StageMother.passedStageInstance("stage", "job", "pipeline-name");
        String key = viewCacheKey.forFailedBuildHistoryStage(stage, "html");
        assertThat(key, is("view_stageFailedBuildHistoryView_pipeline-name/1/stage/1_Passed_html"));
    }

    @Test
    public void shouldGenerateKeyForBuildCause() {
        PipelineModel model = new PipelineModel("pipelineName", true, true, PipelinePauseInfo.notPaused());
        PipelineInstanceModel pipelineInstance = PipelineInstanceModel.createPipeline("pipelineName", 10, "label-10", BuildCause.createExternal(), new StageInstanceModels());
        pipelineInstance.setId(12);
        TrackingTool trackingTool = new TrackingTool("link", "regex");
        pipelineInstance.setTrackingTool(trackingTool);
        model.addPipelineInstance(pipelineInstance);

        PipelineInstanceModel pipelineInstance2 = PipelineInstanceModel.createPipeline("pipelineName", 7, "label-7", BuildCause.createExternal(), new StageInstanceModels());
        pipelineInstance2.setId(14);
        MingleConfig mingleConfig = new MingleConfig("mingle", "project", "mql");
        pipelineInstance2.setMingleConfig(mingleConfig);
        model.addPipelineInstance(pipelineInstance2);

        assertThat(viewCacheKey.forPipelineModelBuildCauses(model), is(String.format("view_buildCausesForPipelineModel_pipelineName[12|%s|%s][14|%s|%s]", trackingTool.hashCode(), -1, -1, mingleConfig.hashCode())));
    }

    @Test
    public void shouldGenerateKeyForEnvironmentPipelineFragment() {
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

        assertThat(model.hasNewRevisions(), is(true));
        assertThat(viewCacheKey.forEnvironmentPipelineBox(model), is("view_environmentPipelineFragment_pipelineName{false|false|false}[12|stageName|13|Building|stage2|0|Unknown|]true|true|false|||true|true"));

        model.updateAdministrability(false);
        assertThat(viewCacheKey.forEnvironmentPipelineBox(model), is("view_environmentPipelineFragment_pipelineName{false|false|false}[12|stageName|13|Building|stage2|0|Unknown|]true|true|false|||false|true"));
    }

    @Test
    public void shouldGenerateKeyForPipelineModelViewFragment() {
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
        
        assertThat(viewCacheKey.forPipelineModelBox(model), is("view_dashboardPipelineFragment_pipelineName{false|false|false}[12|stageName|13|Building|stage2|0|Unknown|][14|stageName|7|Passed|stage2|10|Building|]true|true|false|||true"));
    }

    @Test
    public void shouldGenerateKeyForPipelineModelViewFragmentWithoutSpecialCharactersInPauseCause() {
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

        assertThat(viewCacheKey.forPipelineModelBox(model),
                is("view_dashboardPipelineFragment_pipelineName{false|false|false}[12|stageName|13|Building|stage2|0|Unknown|][14|stageName|7|Passed|stage2|10|Building|]true|true|true|pauseCausewithspecialchar|admin|true"));
    }

    @Test
    public void shouldGenerateKeyForPipelineModelViewFragmentWithLockStatus() {
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
        
        assertThat(viewCacheKey.forPipelineModelBox(model), is("view_dashboardPipelineFragment_pipelineName{true|true|false}[12|stageName|13|Building|stage2|0|Unknown|]true|true|false|||true"));
    }

    @Test
    public void shouldGenerateKeyForPipelineModelViewIncludingUserAdminStatus() {
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

        assertThat(viewCacheKey.forPipelineModelBox(model), is("view_dashboardPipelineFragment_pipelineName{true|true|false}[12|stageName|13|Building|stage2|0|Unknown|]true|true|false|||true"));

        model.updateAdministrability(false);
        assertThat(viewCacheKey.forPipelineModelBox(model), is("view_dashboardPipelineFragment_pipelineName{true|true|false}[12|stageName|13|Building|stage2|0|Unknown|]true|true|false|||false"));
    }

    private StageInstanceModel stageInstance(String name, int id, JobState state, JobResult jobResult) {
        JobHistory jobs = new JobHistory();
        jobs.addJob("dev", state, jobResult, new Date());
        StageInstanceModel stageInstance = new StageInstanceModel(name, "2", jobs);
        stageInstance.setId(id);
        return stageInstance;
    }

}
