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

import java.util.Date;
import java.util.List;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.NullStage;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.domain.Stages;
import com.thoughtworks.go.domain.valuestreammap.Node;
import com.thoughtworks.go.domain.valuestreammap.ValueStreamMap;
import com.thoughtworks.go.domain.valuestreammap.PipelineDependencyNode;
import com.thoughtworks.go.domain.valuestreammap.PipelineRevision;
import com.thoughtworks.go.domain.valuestreammap.Revision;
import com.thoughtworks.go.domain.valuestreammap.UnrunPipelineRevision;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.helper.StageMother;
import com.thoughtworks.go.server.service.GoConfigService;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnrunStagesPopulatorTest {

    private GoConfigService goConfigService;
    private UnrunStagesPopulator unrunStagesPopulator;

    @Before
    public void setUp() throws Exception {
        goConfigService = mock(GoConfigService.class);
        unrunStagesPopulator = new UnrunStagesPopulator(goConfigService);
    }

    @Test
    public void shouldPopulateRemainingStagesFromConfigurationForCurrentPipeline() throws Exception {
        ValueStreamMap valueStreamMap = new ValueStreamMap("p", new PipelineRevision("p", 10, "10"));
        Stages stages = new Stages(StageMother.createPassedStage("p", 10, "s1", 1, "b", new Date()));
        stages.add(StageMother.scheduledStage("p", 10, "s3", 1, "b"));
        PipelineRevision pipelineRevision = (PipelineRevision) valueStreamMap.getCurrentPipeline().revisions().get(0);
        pipelineRevision.addStages(stages);

        CruiseConfig cruiseConfig = GoConfigMother.pipelineHavingJob("p", "s1", "b", "filePath", "dirPath");
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("p"));
        pipelineConfig.add(StageConfigMother.stageConfig("s2"));
        pipelineConfig.add(StageConfigMother.stageConfig("s3"));
        pipelineConfig.add(StageConfigMother.stageConfig("s4"));
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);

        unrunStagesPopulator.apply(valueStreamMap);
        assertRevision(valueStreamMap.getCurrentPipeline().revisions().get(0));
    }

    @Test
    public void shouldNotAddRemainingStagesWhenTheyAreReordered() throws Exception {
        ValueStreamMap valueStreamMap = new ValueStreamMap("p", new PipelineRevision("p", 10, "10"));
        Stages stages = new Stages(StageMother.createPassedStage("p", 10, "s2", 1, "b", new Date()));
        stages.add(StageMother.scheduledStage("p", 10, "s1", 1, "b"));
        PipelineRevision pipelineRevision = (PipelineRevision) valueStreamMap.getCurrentPipeline().revisions().get(0);
        pipelineRevision.addStages(stages);

        CruiseConfig cruiseConfig = GoConfigMother.pipelineHavingJob("p", "s1", "b", "filePath", "dirPath");
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("p"));
        pipelineConfig.add(StageConfigMother.stageConfig("s2"));
        pipelineConfig.add(StageConfigMother.stageConfig("s3"));
        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);

        unrunStagesPopulator.apply(valueStreamMap);
        PipelineRevision revision = (PipelineRevision) valueStreamMap.getCurrentPipeline().revisions().get(0);
        MatcherAssert.assertThat(revision.getStages(), Matchers.hasSize(2));
        MatcherAssert.assertThat(revision.getStages().get(0).getName(), Matchers.is("s2"));
        MatcherAssert.assertThat(revision.getStages().get(1).getName(), Matchers.is("s1"));
    }

    @Test
    public void shouldAddRemainingStagesToAllDownstreamPipelines() throws Exception {

        /*
            p --> p1 --> p2
             \
              +-> p3
         */
        ValueStreamMap valueStreamMap = new ValueStreamMap("p", revision("p", 1));
        Node p1_node = valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p1", "p1"), "p");
        Node p2_node = valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p2", "p2"), "p1");
        Node p3_node = valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p3", "p3"), "p");
        addRevisions(p1_node);
        addRevisions(p2_node);
        addRevisions(p3_node);

        CruiseConfig cruiseConfig = new CruiseConfig();
        String grp = "first";
        cruiseConfig.addPipeline(grp, pipelineConfig("p"));
        cruiseConfig.addPipeline(grp, pipelineConfig("p1"));
        cruiseConfig.addPipeline(grp, pipelineConfig("p2"));
        cruiseConfig.addPipeline(grp, pipelineConfig("p3"));

        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);

        unrunStagesPopulator.apply(valueStreamMap);

        assertRevision(valueStreamMap.getCurrentPipeline().revisions().get(0));
        assertStages(p1_node);
        assertStages(p2_node);
        assertStages(p3_node);
    }

    @Test
    public void shouldPopulateConfiguredStagesWhenThereAreNoRevisionsForDownstream() throws Exception {
        /*
            p --> p1 --> p2
             \
              +-> p3
         */

        ValueStreamMap graph = new ValueStreamMap("p", revision("p", 1));
        Node p1_node = graph.addDownstreamNode(new PipelineDependencyNode("p1", "p1"), "p");
        Node p2_node = graph.addDownstreamNode(new PipelineDependencyNode("p2", "p2"), "p1");
        Node p3_node = graph.addDownstreamNode(new PipelineDependencyNode("p3", "p3"), "p");

        addRevisions(p1_node);

        CruiseConfig cruiseConfig = new CruiseConfig();
        String group = "first";
        cruiseConfig.addPipeline(group, pipelineConfig("p"));
        cruiseConfig.addPipeline(group, pipelineConfig("p1"));
        cruiseConfig.addPipeline(group, pipelineConfig("p2"));
        cruiseConfig.addPipeline(group, pipelineConfig("p3"));

        when(goConfigService.getCurrentConfig()).thenReturn(cruiseConfig);
        unrunStagesPopulator.apply(graph);

        assertThat(p2_node.revisions(), hasSize(1));
        PipelineRevision empty_p2_revision = (PipelineRevision) p2_node.revisions().get(0);
        assertThat(empty_p2_revision.getPipelineIdentifier(), Matchers.is(new UnrunPipelineRevision("p2").getPipelineIdentifier()));
        assertThat(empty_p2_revision.getStages(), is(new Stages(new NullStage("s1"), new NullStage("s2"), new NullStage("s3"), new NullStage("s4"))));

        assertThat(p3_node.revisions(), hasSize(1));
        PipelineRevision empty_p3_revision = (PipelineRevision) p3_node.revisions().get(0);
        assertThat(empty_p2_revision.getPipelineIdentifier(),is(new UnrunPipelineRevision("p2").getPipelineIdentifier()));
        assertThat(empty_p3_revision.getStages(), is(new Stages(new NullStage("s1"), new NullStage("s2"), new NullStage("s3"), new NullStage("s4"))));
    }

    private void assertStages(Node node) {
        List<Revision> revisions = node.revisions();
        for (Revision revision : revisions) {
            assertRevision(revision);
        }
    }

    private void assertRevision(Revision revision) {
        PipelineRevision pipelineRevision = (PipelineRevision) revision;
        MatcherAssert.assertThat(pipelineRevision.getStages(), Matchers.hasSize(3));
        MatcherAssert.assertThat(pipelineRevision.getStages().get(0).getName(), Matchers.is("s1"));
        MatcherAssert.assertThat(pipelineRevision.getStages().get(1).getName(), Matchers.is("s3"));
        MatcherAssert.assertThat(pipelineRevision.getStages().get(2).getName(), Matchers.is("s4"));
        MatcherAssert.assertThat(pipelineRevision.getStages().get(2).getState(), Matchers.is(StageState.Unknown));
    }

    private PipelineConfig pipelineConfig(String pipelineName) {
        return PipelineConfigMother.pipelineConfig(pipelineName,
                StageConfigMother.stageConfig("s1"),
                StageConfigMother.stageConfig("s2"),
                StageConfigMother.stageConfig("s3"),
                StageConfigMother.stageConfig("s4"));
    }

    private void addRevisions(Node node) {
        node.addRevision(revision(node.getName(), 1));
        node.addRevision(revision(node.getName(), 2));
    }

    private PipelineRevision revision(String name, int pipelineCounter) {
        PipelineRevision revision = new PipelineRevision(name, pipelineCounter, String.valueOf(pipelineCounter));
        Stages stages = new Stages();
        stages.add(StageMother.custom("s1"));
        stages.add(StageMother.custom("s3"));
        revision.addStages(stages);
        return revision;
    }
}
