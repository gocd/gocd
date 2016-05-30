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

package com.thoughtworks.go.server.valuestreammap;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.Stages;
import com.thoughtworks.go.domain.valuestreammap.SCMDependencyNode;
import com.thoughtworks.go.domain.valuestreammap.Node;
import com.thoughtworks.go.domain.valuestreammap.VSMTestHelper;
import com.thoughtworks.go.domain.valuestreammap.ValueStreamMap;
import com.thoughtworks.go.domain.valuestreammap.PipelineDependencyNode;
import com.thoughtworks.go.domain.valuestreammap.PipelineRevision;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.dao.StageDao;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class RunStagesPopulatorTest {
    private RunStagesPopulator runStagesPopulator;
    private StageDao stageDao;

    @Before
    public void setup() {
        stageDao = mock(StageDao.class);
        runStagesPopulator = new RunStagesPopulator(stageDao);
    }

    @Test
    public void shouldPopulateStageDetailsForNodesInGraph() throws Exception {
        /*
        * git---> p1 ---> p3
        * |      v      ^
        * +---> p2 -----+
        * **/

        ValueStreamMap graph = new ValueStreamMap("p3", new PipelineRevision("p3", 1, "labelp3-1"));
        graph.addUpstreamNode(new PipelineDependencyNode("p1", "p1"), new PipelineRevision("p1", 1, "labelp1-1"), "p3");
        graph.addUpstreamNode(new PipelineDependencyNode("p2", "p2"), new PipelineRevision("p2", 1, "labelp2-1"), "p3");
        graph.addUpstreamNode(new PipelineDependencyNode("p1", "p1"), new PipelineRevision("p1", 2, "labelp1-2"), "p2");
        graph.addUpstreamMaterialNode(new SCMDependencyNode("g1", "g1", "git"), new CaseInsensitiveString("git"), "p1", new MaterialRevision(null));
        graph.addUpstreamMaterialNode(new SCMDependencyNode("g1", "g1", "git"), new CaseInsensitiveString("git"), "p2", new MaterialRevision(null));

        Stages stagesForP1_1 = stages("stages-for-p1-1");
        Stages stagesForP1_2 = stages("stages-for-p1-2");
        Stages stagesForP2_1 = stages("stages-for-p2-1");
        Stages stagesForP3_1 = stages("stages-for-p3-1");

        when(stageDao.findAllStagesFor("p1", 1)).thenReturn(stagesForP1_1);
        when(stageDao.findAllStagesFor("p1", 2)).thenReturn(stagesForP1_2);
        when(stageDao.findAllStagesFor("p2", 1)).thenReturn(stagesForP2_1);
        when(stageDao.findAllStagesFor("p3", 1)).thenReturn(stagesForP3_1);

        runStagesPopulator.apply(graph);

        VSMTestHelper.assertStageDetailsOf(graph, "p1", "1", stagesForP1_1);
        VSMTestHelper.assertStageDetailsOf(graph, "p1", "2", stagesForP1_2);
        VSMTestHelper.assertStageDetailsOf(graph, "p2", "1", stagesForP2_1);
        VSMTestHelper.assertStageDetailsOf(graph, "p3", "1", stagesForP3_1);
    }

    @Test
    public void shouldPopulateOnlyLatestRunOfEachStageForEachPipelineNode() throws Exception {
        /*
            git --> p1 --> p2 --> p3
         */
        ValueStreamMap graph = new ValueStreamMap("p2", new PipelineRevision("p2", 1, ""));

        graph.addUpstreamNode(new PipelineDependencyNode("p1", "p1"), new PipelineRevision("p1", 1, "1"), "p2");
        graph.addUpstreamMaterialNode(new SCMDependencyNode("g1", "g1", "git"), new CaseInsensitiveString("git"), "p1", new MaterialRevision(null));
        Node p3_node = graph.addDownstreamNode(new PipelineDependencyNode("p3", "p3"), "p2");

        p3_node.addRevision(new PipelineRevision("p3", 1, "1"));

        Stages stagesForP1_1 = stages("s1_p1");
        Stages stagesForP2_1 = stages("s1_p2");
        Stages stagesForP3_1 = stages("s1_p3");
        stagesForP1_1.first().setLatestRun(false);
        Stage latestStage = StageMother.createPassedStage("p1", 1, "s1_p1", 2, "sample", new Date());
        stagesForP1_1.add(latestStage);

        when(stageDao.findAllStagesFor("p1", 1)).thenReturn(stagesForP1_1);
        when(stageDao.findAllStagesFor("p2", 1)).thenReturn(stagesForP2_1);
        when(stageDao.findAllStagesFor("p3", 1)).thenReturn(stagesForP3_1);

        runStagesPopulator.apply(graph);

        VSMTestHelper.assertStageDetailsOf(graph, "p1", "1", new Stages(latestStage));
        VSMTestHelper.assertStageDetailsOf(graph, "p2", "1", stagesForP2_1);
        VSMTestHelper.assertStageDetailsOf(graph, "p3", "1", stagesForP3_1);
    }

    private Stages stages(String stageName) {
        ArrayList<Stage> stages = new ArrayList<Stage>();
        stages.add(StageMother.completedStageInstanceWithTwoPlans(stageName));
        return new Stages(stages);
    }
}
