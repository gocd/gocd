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

package com.thoughtworks.go.presentation.pipelinehistory;

import com.thoughtworks.go.domain.PipelinePauseInfo;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PipelineGroupModelTest {

    @Test
    public void shouldSayContainsPipelineIrrespectiveOfPipelineNameCase() {
        PipelineGroupModel groupModel = new PipelineGroupModel("group");
        groupModel.add(pipelineModel("pipeline"));
        assertThat(groupModel.containsPipeline("PIPELINE"), is(true));
    }

    private PipelineModel pipelineModel(String pipelineName) {
        PipelineModel pipelineModel = new PipelineModel(pipelineName, pipelineName, true, true, PipelinePauseInfo.notPaused());
        pipelineModel.addPipelineInstance(new PipelineInstanceModel(pipelineName, pipelineName, 1, "label", BuildCause.createManualForced(), new StageInstanceModels()));
        return pipelineModel;
    }
}
