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

package com.thoughtworks.go.server.service;

import java.util.Map;
import java.util.TreeSet;

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.PipelineDependencyGraphOld;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModels;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModel;
import com.thoughtworks.go.presentation.pipelinehistory.StageInstanceModels;
import com.thoughtworks.go.helper.PipelineHistoryMother;
import org.junit.Test;

import static com.thoughtworks.go.helper.MaterialConfigsMother.dependencyMaterialConfig;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PipelineDependencyGraphOldTest {

    @Test
    public void shouldFilterPIMS() throws Exception {
        PipelineDependencyGraphOld graph = new PipelineDependencyGraphOld(pim("upstream"), PipelineInstanceModels.createPipelineInstanceModels(pim("pavan"), pim("raghu")));
        PipelineDependencyGraphOld.Filterer filterer = mock(PipelineDependencyGraphOld.Filterer.class);
        when(filterer.filterPipeline("raghu")).thenReturn(false);
        when(filterer.filterPipeline("pavan")).thenReturn(true);
        graph.filterDependencies(filterer);
        PipelineInstanceModels models = graph.dependencies();
        assertThat(models.size(), is(1));
        assertThat(models.get(0).getName(), is("pavan"));
    }

    @Test
    public void shouldGroupPiplineInstancesByName() throws Exception {
        PipelineInstanceModel raghu1 = pim("raghu");
        raghu1.setCounter(1);
        PipelineInstanceModel raghu2 = pim("raghu");
        raghu2.setCounter(2);
        PipelineInstanceModel phavan = pim("pavan");
        PipelineDependencyGraphOld graph = new PipelineDependencyGraphOld(pim("upstream"), PipelineInstanceModels.createPipelineInstanceModels(raghu2, phavan, raghu1));
        Map<String, TreeSet<PipelineInstanceModel>> map = graph.groupedDependencies();
        assertThat(map.size(), is(2));
        assertOrderIsMainted(map);
        assertThat(map.get("pavan").size(), is(1));
        assertThat(map.get("pavan"), hasItem(phavan));
        assertThat(map.get("raghu").size(), is(2));
        assertThat(map.get("raghu").first(), is(raghu2));
        assertThat(map.get("raghu").last(), is(raghu1));

    }

    @Test
    public void shouldProvideMaterialRevisionForAGivenDownStreamPipeline() throws Exception {
        StageInstanceModels stages = new StageInstanceModels();
        stages.add(new StageInstanceModel("stage-0","21", StageResult.Cancelled, new StageIdentifier("blahUpStream",23,"stage-0","21")));
        stages.add(new StageInstanceModel("stage-1","2", StageResult.Cancelled, new StageIdentifier("blahUpStream",23,"stage-1","2")));
        PipelineInstanceModel upStream = PipelineHistoryMother.singlePipeline("blahUpStream", stages);

        PipelineInstanceModel down1 = pim("blahDown1");
        down1.setMaterialConfigs(new MaterialConfigs(dependencyMaterialConfig("blahUpStream", "stage-0")));
        PipelineInstanceModel down2 = pim("blahDown2");
        down2.setMaterialConfigs(new MaterialConfigs(dependencyMaterialConfig("blahUpStream", "stage-1")));

        PipelineDependencyGraphOld graph = new PipelineDependencyGraphOld(upStream, PipelineInstanceModels.createPipelineInstanceModels(down1,down2));
        assertThat(graph.dependencyRevisionFor(down1),is("blahUpStream/23/stage-0/21"));
        assertThat(graph.dependencyRevisionFor(down2),is("blahUpStream/23/stage-1/2"));
    }

    @Test
    public void shouldShouldbeAbleToTellIfUpStreamMaterialIsAvailableForTrigger() throws Exception {
        StageInstanceModels stages = new StageInstanceModels();
        stages.add(new StageInstanceModel("stage-0","21", StageResult.Cancelled, new StageIdentifier("blahUpStream",23,"stage-0","21")));
        PipelineInstanceModel upStream = PipelineHistoryMother.singlePipeline("blahUpStream", stages);
        PipelineInstanceModel down1 = pim("blahDown1");
        down1.setMaterialConfigs(new MaterialConfigs(dependencyMaterialConfig("blahUpStream", "stage-1")));
        PipelineDependencyGraphOld graph = new PipelineDependencyGraphOld(upStream, PipelineInstanceModels.createPipelineInstanceModels(down1));
        assertThat(graph.hasUpStreamRevisionFor(down1),is(false));
    }

    @Test
    public void shouldShouldbeAbleToTellIfUpStreamMaterialIsAvailableForTriggerOnlyIfTheUpstreamStageHasPassed() throws Exception {
        assertThatTriggerIsPossibleOnlyIfUpStreamPassed(StageResult.Cancelled);
        assertThatTriggerIsPossibleOnlyIfUpStreamPassed(StageResult.Failed);
        assertThatTriggerIsPossibleOnlyIfUpStreamPassed(StageResult.Unknown);
        assertThatTriggerIsPossibleOnlyIfUpStreamPassed(StageResult.Passed);
    }

    private void assertThatTriggerIsPossibleOnlyIfUpStreamPassed(StageResult upstreamResult) {
        StageInstanceModels stages = new StageInstanceModels();
        stages.add(new StageInstanceModel("stage-0","21", upstreamResult, new StageIdentifier("blahUpStream",23,"stage-0","21")));
        PipelineInstanceModel upStream = PipelineHistoryMother.singlePipeline("blahUpStream", stages);
        PipelineInstanceModel down1 = pim("blahDown1");
        down1.setMaterialConfigs(new MaterialConfigs(dependencyMaterialConfig("blahUpStream", "stage-0")));
        PipelineDependencyGraphOld graph = new PipelineDependencyGraphOld(upStream, PipelineInstanceModels.createPipelineInstanceModels(down1));
        assertThat(graph.hasUpStreamRevisionFor(graph.dependencies().find("blahDown1")),is(upstreamResult== StageResult.Passed));
    }


    private void assertOrderIsMainted(Map<String, TreeSet<PipelineInstanceModel>> map) {
        String[] keys = map.keySet().toArray(new String[0]);
        assertThat(keys[0],is("raghu"));
        assertThat(keys[1],is("pavan"));
    }

    private PipelineInstanceModel pim(String pipeline) {
        return PipelineHistoryMother.singlePipeline(pipeline, new StageInstanceModels());
    }
}
