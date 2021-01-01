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
package com.thoughtworks.go.presentation.pipelinehistory;

import java.util.Date;

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.JobState;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.helper.ModificationsMother;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PipelineModelTest {

    @Test
    public void shouldUnderstandIfHasNewRevisions() throws Exception {
        PipelineInstanceModel instanceModel = PipelineInstanceModel.createPipeline("pipelineName", -1, "1", BuildCause.createManualForced(), new StageInstanceModels());
        MaterialRevisions latest = ModificationsMother.createHgMaterialRevisions();
        instanceModel.setMaterialConfigs(new MaterialConfigs(latest.getMaterialRevision(0).getMaterial().config()));
        instanceModel.setLatestRevisions(latest);
        PipelineModel pipelineModel = new PipelineModel(instanceModel.getName(), true, true, PipelinePauseInfo.notPaused());
        pipelineModel.addPipelineInstance(instanceModel);
        instanceModel.setMaterialRevisionsOnBuildCause(MaterialRevisions.EMPTY);
        assertThat(pipelineModel.hasNewRevisions(), is(true));
        instanceModel.setMaterialRevisionsOnBuildCause(latest);
        assertThat(pipelineModel.hasNewRevisions(), is(false));
    }

    @Test
    public void shouldNotBeAbleToscheduleIfTheLatestPipelineIsPreparingToSchedule() throws Exception {
        PipelineInstanceModel instanceModel = PipelineInstanceModel.createPreparingToSchedule("pipelineName", new StageInstanceModels());

        PipelineModel pipelineModel = new PipelineModel(instanceModel.getName(), true, true, PipelinePauseInfo.notPaused());
        pipelineModel.addPipelineInstance(instanceModel);

        assertThat(pipelineModel.canForce(), is(false));
    }

    @Test
    public void shouldUnderstandCanOperateAndCanForce() {
        PipelineModel foo = new PipelineModel("foo", true, true, PipelinePauseInfo.notPaused());
        foo.addPipelineInstance(pipelineNamed("foo"));
        PipelineModel bar = new PipelineModel("bar", false, false, PipelinePauseInfo.notPaused());
        bar.addPipelineInstance(pipelineNamed("bar"));
        PipelineModel baz = new PipelineModel("baz", false, true, PipelinePauseInfo.notPaused());
        baz.addPipelineInstance(pipelineNamed("baz"));
        assertThat(foo.canOperate(), is(true));
        assertThat(foo.canForce(), is(true));
        assertThat(bar.canOperate(), is(false));
        assertThat(bar.canForce(), is(false));
        assertThat(baz.canOperate(), is(true));
        assertThat(baz.canForce(), is(false));
    }

    private PipelineInstanceModel pipelineNamed(String name) {
        StageInstanceModels stages = new StageInstanceModels();
        stages.add(new StageInstanceModel("dev", "10", JobHistory.withJob("dev", JobState.Completed, JobResult.Failed, new Date())));
        return PipelineInstanceModel.createPipeline(name, -1, "1.0", BuildCause.createWithEmptyModifications(), stages);
    }

}
