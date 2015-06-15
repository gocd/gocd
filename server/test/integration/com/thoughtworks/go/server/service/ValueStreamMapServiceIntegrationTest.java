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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.StageState;
import com.thoughtworks.go.domain.valuestreammap.*;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.presentation.models.ValueStreamMapPresentationModel;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.ReflectionUtil;
import org.apache.commons.collections.Transformer;
import org.apache.commons.httpclient.HttpStatus;
import org.hamcrest.core.Is;
import org.hamcrest.core.IsCollectionContaining;
import org.hamcrest.core.IsNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collection;
import java.util.List;

import static com.thoughtworks.go.domain.valuestreammap.VSMTestHelper.assertInstances;
import static com.thoughtworks.go.domain.valuestreammap.VSMTestHelper.assertStageDetails;
import static org.apache.commons.collections.CollectionUtils.collect;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class ValueStreamMapServiceIntegrationTest {
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoCache goCache;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private ValueStreamMapService valueStreamMapService;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private Localizer localizer;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private ScheduleTestUtil u;
    private HttpLocalizedOperationResult result;
    private Username username;

    @Before
    public void setUp() throws Exception {
        goCache.clear();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        configHelper.turnOnSecurity();
        configHelper.addAdmins("admin");

        dbHelper.onSetUp();
        u = new ScheduleTestUtil(transactionTemplate, materialRepository, dbHelper, configHelper);
        result = new HttpLocalizedOperationResult();

        username = new Username(new CaseInsensitiveString("admin"));

    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test // Scenario: #7192
    public void shouldDrawPDGForCrissCrossDependencies() {
/*
    +--------------------------------------------+
    |        +--------------------------------+  |
    |        +                                v  v
   G1 -----> P2----->P3------------------------->C5
    |  +---> +        +--------------+         ^ ^ ^
    |  |     |                       |         | | |
    |  |     |  +--------------+     |         | | |
    |  |     ---)--------+     |     V         | | |
    |  |        |         |    |---->P4--------+ | |
    |  |        |         |         |            | |
    |--)----+   |         |         |   +--------+ |
    |  |    |   |         |         +---)--+       |
    |  |    |   |         |             |  |       |
    |  |    |   |         |         +---+  |       |
    +--)----)---)---------)---------)------)----+  |
       |    |   |         |         |      |    |  |
       |    |   |         +-------->C4--+  |    |  |
       |    |   |                   ^   |  |    |  |
       |    |   |                   |   |  |    |  |
       |    |   |     +-------------+   |  |    |  |
   +---+    |   |     |                 |  |    |  |
   |        |   |     |                 |  |    |  |
   +       v +-+     |                  v  +--->v  |
   G2 ----->C2------>c3------------------------>P5 |
   |        |                                  ^ ^ |
   |        +---------------------------------+ |  |
   |                                            |  |
   +-------+------------------------------------+  |
   |                                               |
   |-----------------------------------------------+
*/


        int i = 0;
        GitMaterial g1 = u.wf(new GitMaterial("git-1"), "folder3");
        u.checkinInOrder(g1, u.d(i++), "g1-1");

        GitMaterial g2 = u.wf(new GitMaterial("git-2"), "folder4");
        u.checkinInOrder(g2, u.d(i++), "g2-1");

        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(g1), u.m(g2));
        ScheduleTestUtil.AddedPipeline c2 = u.saveConfigWith("C2", u.m(g1), u.m(g2));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("P3", u.m(p2));
        ScheduleTestUtil.AddedPipeline c3 = u.saveConfigWith("C3", u.m(c2));
        ScheduleTestUtil.AddedPipeline c4 = u.saveConfigWith("C4", u.m(c3), u.m(p2));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("P4", u.m(p3), u.m(c2));
        ScheduleTestUtil.AddedPipeline c5 = u.saveConfigWith("C5", u.m(p3), u.m(p2), u.m(g1), u.m(p4), u.m(c4), u.m(g2));
        ScheduleTestUtil.AddedPipeline p5 = u.saveConfigWith("P5", u.m(c3), u.m(c2), u.m(g1), u.m(p4), u.m(c4), u.m(g2));

        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "g1-1", "g2-1");
        String c2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(c2, u.d(i++), "g1-1", "g2-1");
        String p3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), p2_1);
        String c3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(c3, u.d(i++), c2_1);
        String c4_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(c4, u.d(i++), c3_1, p2_1);
        String p4_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p4, u.d(i++), p3_1, c2_1);
        String c5_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(c5, u.d(i++), p3_1, p2_1, "g1-1", p4_1, c4_1, "g2-1");
        String p5_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p5, u.d(i++), c3_1, c2_1, "g1-1", p4_1, c4_1, "g2-1");

        // PDG for C5
        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(pipelineName(c5), 1, username, result);
        List<List<Node>> nodesAtEachLevel = graph.getNodesAtEachLevel();

        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(4), 0, pipelineName(c5));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(3), 4, pipelineName(p4), pipelineName(c4));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(2), 5, pipelineName(p3), pipelineName(c3));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(1), 2, pipelineName(p2), pipelineName(c2));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(0), 0, g1.getFingerprint(), g2.getFingerprint());

        VSMTestHelper.assertDepth(graph, pipelineName(c5), 1);
        VSMTestHelper.assertDepth(graph, pipelineName(p4), 4);
        VSMTestHelper.assertDepth(graph, pipelineName(c4), 5);
        VSMTestHelper.assertDepth(graph, pipelineName(p3), 2);
        VSMTestHelper.assertDepth(graph, pipelineName(c3), 6);
        VSMTestHelper.assertDepth(graph, pipelineName(p2), 2);
        VSMTestHelper.assertDepth(graph, pipelineName(c2), 4);
        VSMTestHelper.assertDepth(graph, g1.getFingerprint(), 1);
        VSMTestHelper.assertDepth(graph, g2.getFingerprint(), 2);

        // PDG for C4
        graph = valueStreamMapService.getValueStreamMap(pipelineName(c4), 1, username, result);
        nodesAtEachLevel = valueStreamMapService.getValueStreamMap(pipelineName(c4), 1, username, result).getNodesAtEachLevel();
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(4), 0, pipelineName(c5), pipelineName(p5));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(3), 0, pipelineName(c4));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(2), 0, pipelineName(c3), pipelineName(p2));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(1), 2, pipelineName(c2));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(0), 0, g1.getFingerprint(), g2.getFingerprint());

        VSMTestHelper.assertDepth(graph, pipelineName(p5), 1);
        VSMTestHelper.assertDepth(graph, pipelineName(c5), 2);
        VSMTestHelper.assertDepth(graph, pipelineName(c4), 1);
        VSMTestHelper.assertDepth(graph, pipelineName(c3), 1);
        VSMTestHelper.assertDepth(graph, pipelineName(p2), 2);
        VSMTestHelper.assertDepth(graph, pipelineName(c2), 1);
        VSMTestHelper.assertDepth(graph, g1.getFingerprint(), 1);
        VSMTestHelper.assertDepth(graph, g2.getFingerprint(), 2);

        // PDG for p3
        nodesAtEachLevel = valueStreamMapService.getValueStreamMap(pipelineName(p3), 1, username, result).getNodesAtEachLevel();
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(4), 0, pipelineName(c5), pipelineName(p5));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(3), 1, pipelineName(p4));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(2), 0, pipelineName(p3));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(1), 0, pipelineName(p2));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(0), 0, g1.getFingerprint(), g2.getFingerprint());
    }

    @Test //Scenario: #7181
    public void shouldIncludeNewerDownstreamDependenciesIfConfigWasChangedAfterPipelineRun() {
/*
*    -----------Before --------------
*   g1-----+
*          |
*          +---------P1------> P2
*          |
*   g2-----+
*   *
*   -----After config change ------
*
*   g1-----+
*          |
*   g3-----+---------P1----->P3-------> P2
*          |
*   g2-----+
* */
        int i = 0;
        GitMaterial g1 = u.wf(new GitMaterial("git-1"), "folder3");
        u.checkinInOrder(g1, u.d(i++), "g1-1");

        GitMaterial g2 = u.wf(new GitMaterial("git-2"), "folder4");
        u.checkinInOrder(g2, u.d(i++), "g2-1");

        GitMaterial g3 = u.wf(new GitMaterial("git-3"), "folder5");
        u.checkinInOrder(g3, u.d(i++), "g3-1");

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(g1), u.m(g2));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(p1));
        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g1-1", "g2-1");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_1);

        // PDG for P1/1
        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap(pipelineName(p1), 1, username, result);
        List<List<Node>> nodesAtEachLevel = graph.getNodesAtEachLevel();
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(2), 0, pipelineName(p2));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(1), 0, pipelineName(p1));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(0), 0, g1.getFingerprint(), g2.getFingerprint());

        // PDG for P2/1
        nodesAtEachLevel = valueStreamMapService.getValueStreamMap(pipelineName(p2), 1, username, result).getNodesAtEachLevel();
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(2), 0, pipelineName(p2));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(1), 0, pipelineName(p1));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(0), 0, g1.getFingerprint(), g2.getFingerprint());

        // add g3 to p1, and add P3 dependent on p1
        u.addMaterialToPipeline(p1, u.m(g3));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("P3", u.m(p1));
        p2 = u.addMaterialToPipeline(p2, u.m(p3));
        p2 = u.removeMaterialFromPipeline(p2, u.m(p1));

        // PDG for P1/1 after config change, newly added material does not show up
        nodesAtEachLevel = valueStreamMapService.getValueStreamMap(pipelineName(p1), 1, username, result).getNodesAtEachLevel();
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(3), 0, pipelineName(p2));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(2), 0, pipelineName(p3));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(1), 0, pipelineName(p1));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(0), 0, g1.getFingerprint(), g2.getFingerprint());

        // PDG for P2/1, remains same after config change
        nodesAtEachLevel = valueStreamMapService.getValueStreamMap(pipelineName(p2), 1, username, result).getNodesAtEachLevel();
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(2), 0, pipelineName(p2));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(1), 0, pipelineName(p1));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(0), 0, g1.getFingerprint(), g2.getFingerprint());

        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g1-1", "g2-1", "g3-1");
        String p3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), p1_2);
        String p2_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p3_1);

        // PDG for P2/2, with updated dependencies
        nodesAtEachLevel = valueStreamMapService.getValueStreamMap(pipelineName(p2), 2, username, result).getNodesAtEachLevel();
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(3), 0, pipelineName(p2));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(2), 0, pipelineName(p3));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(1), 0, pipelineName(p1));
        VSMTestHelper.assertThatLevelHasNodes(nodesAtEachLevel.get(0), 0, g1.getFingerprint(), g2.getFingerprint());

    }

    @Test
    public void shouldReturnNullIfThePipelineHasNeverEverRun() {
        GitMaterial git = new GitMaterial("git");
        String pipeline = "NewlyCreated";

        u.saveConfigWith("P1", u.m(git));

        assertThat(valueStreamMapService.getValueStreamMap(pipeline, 1, username, result), is(IsNull.nullValue()));
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(HttpStatus.SC_NOT_FOUND));
        assertThat(result.message(localizer), is("Pipeline 'NewlyCreated' with counter '1' not found."));
    }

    @Test
    public void shouldNotReturnGraphForNonExistentPipeline() {
        assertThat(valueStreamMapService.getValueStreamMap("does_not_exist", 1, username, result), is(IsNull.nullValue()));
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(HttpStatus.SC_NOT_FOUND));
        assertThat(result.message(localizer), is("Pipeline 'does_not_exist' with counter '1' not found."));
    }

    @Test
    public void shouldPopulateInstanceDetailsForUpstreamAndDownstreamPipelines() throws Exception {
        /*
        g1 --> p1 --> p --> p2 --> p4
                     / \
                   g2    +-> p3
         */

        GitMaterial g1 = u.wf(new GitMaterial("g1"), "f1");
        u.checkinInOrder(g1, "g1-1");

        GitMaterial g2 = u.wf(new GitMaterial("g2"), "f2");
        u.checkinInOrder(g2, "g2-1");

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(g1));
        ScheduleTestUtil.AddedPipeline p = u.saveConfigWith("p", u.m(g2), u.m(p1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", u.m(p2));

        String p1_1 = u.runAndPass(p1, "g1-1");
        String p_1 = u.runAndPass(p, "g2-1", p1_1);
        String p2_1 = u.runAndPass(p2, p_1);
        String p2_2 = u.runAndPass(p2, p_1);
        String p3_1 = u.runAndPass(p3, p_1);
        String p4_1 = u.runAndPass(p4, p2_1);

        configHelper.addStageToPipeline("p2", "unrun_stage");

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap("p", 1, username, result);

        List<List<Node>> allLevels = graph.getNodesAtEachLevel();
        int CURRENT_PIPELINE_LEVEL = 2;
        Node currentNode = allLevels.get(CURRENT_PIPELINE_LEVEL).get(0);
        assertEquals(currentNode.revisions().get(0), new PipelineRevision(currentNode.getName(), 1, "1"));

        Node p1_node = allLevels.get(CURRENT_PIPELINE_LEVEL - 1).get(1);
        assertInstances(p1_node, "p1", 1);
        assertStageDetails(p1_node, 1, "s", 1, StageState.Passed);

        assertInstances(allLevels.get(CURRENT_PIPELINE_LEVEL + 1).get(0), "p3", 1);

        Node p2_node = allLevels.get(CURRENT_PIPELINE_LEVEL + 1).get(1);
        assertInstances(p2_node, "p2", 1, 2);
        assertStageDetails(p2_node, 1, "s", 1, StageState.Passed);
        assertStageDetails(p2_node, 2, "s", 1, StageState.Passed);
        assertStageDetails(p2_node, 1, "unrun_stage", 0, StageState.Unknown);
        assertStageDetails(p2_node, 2, "unrun_stage", 0, StageState.Unknown);

        assertInstances(allLevels.get(CURRENT_PIPELINE_LEVEL + 2).get(0), "p4", 1);
    }

    @Test
    public void shouldEmptyRevisionsOfPipelineWhenUserDoesNotHaveViewPermissionForThePipeline() throws Exception {
        /*
        g1 --> p1 --> p --> p2 --> p4
                     / \
                   g2    +-> p3
         */

        GitMaterial g1 = u.wf(new GitMaterial("g1"), "f1");
        u.checkinInOrder(g1, "g1-1");

        GitMaterial g2 = u.wf(new GitMaterial("g2"), "f2");
        u.checkinInOrder(g2, "g2-1");

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWithGroup("g1", "p1", u.m(g1));
        ScheduleTestUtil.AddedPipeline p = u.saveConfigWithGroup("g2", "p", u.m(g2), u.m(p1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWithGroup("g2", "p2", u.m(p));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWithGroup("g2", "p3", u.m(p));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWithGroup("g1", "p4", u.m(p2));

        String p1_1 = u.runAndPass(p1, "g1-1");
        String p_1 = u.runAndPass(p, "g2-1", p1_1);
        String p2_1 = u.runAndPass(p2, p_1);
        String p2_2 = u.runAndPass(p2, p_1);
        String p3_1 = u.runAndPass(p3, p_1);
        String p4_1 = u.runAndPass(p4, p2_1);

        Username groupAdmin = new Username(new CaseInsensitiveString("pg2-admin"));
        configHelper.addAuthorizedUserForPipelineGroup("pg1-admin", "g1");
        configHelper.addAuthorizedUserForPipelineGroup("pg2-admin", "g2");

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap("p", 1, groupAdmin, result);

        List<List<Node>> allLevels = graph.getNodesAtEachLevel();
        int CURRENT_PIPELINE_LEVEL = 2;

        PipelineDependencyNode p1_node = (PipelineDependencyNode) allLevels.get(CURRENT_PIPELINE_LEVEL - 1).get(1);
        assertThat(p1_node.revisions().toString(), p1_node.revisions().isEmpty(), is(true));
        assertThat((String) ReflectionUtil.getField((p1_node.getMessage()), "key"), is("VSM_PIPELINE_UNAUTHORIZED"));
        assertThat(p1_node.getViewType(), Is.is(VSMViewType.NO_PERMISSION));

        PipelineDependencyNode currentNode = (PipelineDependencyNode) allLevels.get(CURRENT_PIPELINE_LEVEL).get(0);
        assertThat(currentNode.revisions().toString(), currentNode.revisions().isEmpty(), is(false));

        PipelineDependencyNode p2_node = (PipelineDependencyNode) allLevels.get(CURRENT_PIPELINE_LEVEL + 1).get(0);
        assertThat(p2_node.revisions().toString(), p2_node.revisions().isEmpty(), is(false));

        PipelineDependencyNode p3_node = (PipelineDependencyNode) allLevels.get(CURRENT_PIPELINE_LEVEL + 1).get(1);
        assertThat(p3_node.revisions().toString(), p3_node.revisions().isEmpty(), is(false));

        PipelineDependencyNode p4_node = (PipelineDependencyNode) allLevels.get(CURRENT_PIPELINE_LEVEL + 2).get(0);
        assertThat(p4_node.revisions().toString(), p4_node.revisions().isEmpty(), is(true));
        assertThat((String) ReflectionUtil.getField((p4_node.getMessage()), "key"), is("VSM_PIPELINE_UNAUTHORIZED"));
        assertThat(p1_node.getViewType(), is(VSMViewType.NO_PERMISSION));
    }

    @Test
    public void shouldReturnEmptyRevisionsOfPipelineWhenViewUserDoesNotHaveViewPermissionForSomeUpstreamPipelines() throws Exception {
        /*
        g1 --> p1 --> p --> p2 --> p4
                     / \
                   g2    +-> p3
         */

        GitMaterial g1 = u.wf(new GitMaterial("g1"), "f1");
        u.checkinInOrder(g1, "g1-1");

        GitMaterial g2 = u.wf(new GitMaterial("g2"), "f2");
        u.checkinInOrder(g2, "g2-1");

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWithGroup("g1", "p1", u.m(g1));
        ScheduleTestUtil.AddedPipeline p = u.saveConfigWithGroup("g2", "p", u.m(g2), u.m(p1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWithGroup("g2", "p2", u.m(p));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWithGroup("g2", "p3", u.m(p));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWithGroup("g1", "p4", u.m(p2));

        String p1_1 = u.runAndPass(p1, "g1-1");
        String p_1 = u.runAndPass(p, "g2-1", p1_1);
        String p2_1 = u.runAndPass(p2, p_1);
        String p2_2 = u.runAndPass(p2, p_1);
        String p3_1 = u.runAndPass(p3, p_1);
        String p4_1 = u.runAndPass(p4, p2_1);

        Username viewOnlyUser = new Username(new CaseInsensitiveString("pg2-view"));
        configHelper.setViewPermissionForGroup("g1", "pg1-view");
        configHelper.setViewPermissionForGroup("g2", "pg2-view");

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap("p", 1, viewOnlyUser, result);

        List<List<Node>> allLevels = graph.getNodesAtEachLevel();
        int CURRENT_PIPELINE_LEVEL = 2;

        PipelineDependencyNode p1_node = (PipelineDependencyNode) allLevels.get(CURRENT_PIPELINE_LEVEL - 1).get(1);
        assertThat(p1_node.revisions().toString(), p1_node.revisions().isEmpty(), is(true));
        assertThat((String) ReflectionUtil.getField((p1_node.getMessage()), "key"), is("VSM_PIPELINE_UNAUTHORIZED"));
        assertThat(p1_node.getViewType(), is(VSMViewType.NO_PERMISSION));

        PipelineDependencyNode currentNode = (PipelineDependencyNode) allLevels.get(CURRENT_PIPELINE_LEVEL).get(0);
        assertThat(currentNode.revisions().toString(), currentNode.revisions().isEmpty(), is(false));

        PipelineDependencyNode p2_node = (PipelineDependencyNode) allLevels.get(CURRENT_PIPELINE_LEVEL + 1).get(0);
        assertThat(p2_node.revisions().toString(), p2_node.revisions().isEmpty(), is(false));

        PipelineDependencyNode p3_node = (PipelineDependencyNode) allLevels.get(CURRENT_PIPELINE_LEVEL + 1).get(1);
        assertThat(p3_node.revisions().toString(), p3_node.revisions().isEmpty(), is(false));

        PipelineDependencyNode p4_node = (PipelineDependencyNode) allLevels.get(CURRENT_PIPELINE_LEVEL + 2).get(0);
        assertThat(p4_node.revisions().toString(), p4_node.revisions().isEmpty(), is(true));
        assertThat((String) ReflectionUtil.getField((p4_node.getMessage()), "key"), is("VSM_PIPELINE_UNAUTHORIZED"));
        assertThat(p1_node.getViewType(), is(VSMViewType.NO_PERMISSION));
    }

    @Test
    public void shouldShowAllRevisionsWhenFanInIsOff() {
        GitMaterial g1 = u.wf(new GitMaterial("g1"), "f1");
        u.checkinInOrder(g1, "g1-1");
        u.checkinInOrder(g1, "g1-2");

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWithGroup("g1", "p1", u.m(g1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWithGroup("g1", "p2", u.m(g1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWithGroup("g2", "p3", u.m(p1), u.m(p2));

        String p1_1 = u.runAndPass(p1, "g1-1");
        String p2_1 = u.runAndPass(p2, "g1-2");
        String p3_1 = u.runAndPass(p3, p1_1, p2_1);

        ValueStreamMapPresentationModel graph = valueStreamMapService.getValueStreamMap("p3", 1, username, result);
        Node nodeForGit = graph.getNodesAtEachLevel().get(0).get(0);
        assertThat(nodeForGit.revisions().size(), is(2));
        Collection<String> revisionStrings = collect(nodeForGit.revisions(), new Transformer() {
            @Override
            public String transform(Object o) {
                Revision revision = (Revision) o;
                return revision.getRevisionString();
            }
        });
        assertThat(revisionStrings, IsCollectionContaining.hasItems("g1-1", "g1-2"));
    }

    @Test
    public void shouldDetectCycleWhenDependenciesAreInverted() {
        /*
        * git2 ----->
        *           |---->P1 ----> P2
        * git1 ----->
        *
        * run pipelines once, config changes to :
        *
        * git1 -> P2 ----->P1
        *                 ^
        *         git2 ---|
        * */

        GitMaterial g1 = u.wf(new GitMaterial("g1"), "f1");
        u.checkinInOrder(g1, "g1-1");

        GitMaterial g2 = u.wf(new GitMaterial("g2"), "f2");
        u.checkinInOrder(g2, "g2-1");

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWithGroup("g1", "p1", u.m(g1), u.m(g2));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWithGroup("g1", "p2", u.m(p1));

        String p1_1 = u.runAndPass(p1, "g1-1", "g2-1");
        String p2_1 = u.runAndPass(p2, p1_1);

        p2 = u.addMaterialToPipeline(p2, u.m(g1));
        p2 = u.removeMaterialFromPipeline(p2, u.m(p1));
        p1 = u.addMaterialToPipeline(p1, u.m(p2));
        p1 = u.removeMaterialFromPipeline(p1, u.m(g1));

        String p1_2 = u.runAndPass(p1, "g2-1", p2_1);

        valueStreamMapService.getValueStreamMap("p1", 2, username, result);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.httpCode(), is(HttpStatus.SC_NOT_IMPLEMENTED));
        assertThat(result.message(localizer), is(String.format(
                "Value Stream Map of Pipeline 'p1' with counter '2' can not be rendered. Changes to the configuration have introduced complex dependencies for this instance which are not supported currently.",
                "p1", 2)));
    }

    private String pipelineName(ScheduleTestUtil.AddedPipeline c5) {
        return c5.config.name().toString();
    }
}
