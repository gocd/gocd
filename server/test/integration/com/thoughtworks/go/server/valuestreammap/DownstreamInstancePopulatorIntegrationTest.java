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

import java.util.List;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.valuestreammap.Node;
import com.thoughtworks.go.domain.valuestreammap.PipelineDependencyNode;
import com.thoughtworks.go.domain.valuestreammap.PipelineRevision;
import com.thoughtworks.go.domain.valuestreammap.Revision;
import com.thoughtworks.go.domain.valuestreammap.SCMDependencyNode;
import com.thoughtworks.go.domain.valuestreammap.ValueStreamMap;
import com.thoughtworks.go.helper.ModificationsMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.ScheduleTestUtil;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class DownstreamInstancePopulatorIntegrationTest {

    private ScheduleTestUtil u;
    @Autowired private GoCache goCache;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private PipelineDao pipelineDao;
    private GoConfigFileHelper configHelper;
    private DownstreamInstancePopulator downstreamInstancePopulator;


    @Before
    public void setup() throws Exception {
        configHelper = new GoConfigFileHelper();
        goCache.clear();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        dbHelper.onSetUp();
        u = new ScheduleTestUtil(transactionTemplate, materialRepository, dbHelper, configHelper);
        downstreamInstancePopulator = new DownstreamInstancePopulator(pipelineDao);
    }


    @After
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }


    @Test
    public void shouldPopulateInstancesBuiltFromCurrentPipeline() {

        /*
            g1 -> P -> P2  -->  P4
                        \      /
                         + P3 +
        */

        ValueStreamMap valueStreamMap = new ValueStreamMap("p", new PipelineRevision("p", 1, "13.1.1"));
        Node nodep2 = valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p2", "p2"), "p");
        Node nodep3 = valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p3", "p3"), "p2");
        Node nodep4 = valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p4", "p4"), "p3");
        valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p4", "p4"), "p2");

        valueStreamMap.addUpstreamMaterialNode(new SCMDependencyNode("g1", "g1", "git"),new CaseInsensitiveString("git"), new Modifications(ModificationsMother.multipleModificationList()), "p");

        GitMaterial g1 = u.wf(new GitMaterial("g1"), "folder3");
        u.checkinInOrder(g1,"g_1");

        ScheduleTestUtil.AddedPipeline p = u.saveConfigWith("p", u.m(g1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p2));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", u.m(p2), u.m(p3));

        String p_1 = u.runAndPass(p, "g_1");
        String p2_1 = u.runAndPass(p2, p_1);
        String p3_1 = u.runAndPass(p3, p2_1);
        String p4_1 = u.runAndPass(p4, p2_1, p3_1);

        downstreamInstancePopulator.apply(valueStreamMap);

        assertInstances(nodep2, "p2", 1);
        assertInstances(nodep3, "p3", 1);
        assertInstances(nodep4, "p4", 1);
    }

    @Test
    public void shouldPopulateMultipleInstancesBuiltFromUpstreamRevisions() {

        /*
            g1 -> P -> P2  -->  P4 --> P5
                        \      /
                         + P3 +
        */

        ValueStreamMap valueStreamMap = new ValueStreamMap("p", new PipelineRevision("p", 1, "13.1.1"));
        Node nodep2 = valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p2", "p2"), "p");
        Node nodep3 = valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p3", "p3"), "p2");
        Node nodep4 = valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p4", "p4"), "p3");
        valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p4", "p4"), "p2");
        valueStreamMap.addUpstreamMaterialNode(new SCMDependencyNode("g1", "g1", "git"),new CaseInsensitiveString("git"), new Modifications(ModificationsMother.multipleModificationList()), "p");

        GitMaterial g1 = u.wf(new GitMaterial("g1"), "folder3");
        u.checkinInOrder(g1,"g_1");

        ScheduleTestUtil.AddedPipeline p = u.saveConfigWith("p", u.m(g1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p2));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", u.m(p2), u.m(p3));

        String p_1 = u.runAndPass(p, "g_1");
        String p2_1 = u.runAndPass(p2, p_1);
        String p2_2 = u.runAndPass(p2, p_1);
        String p3_1 = u.runAndPass(p3, p2_2);
        String p4_1 = u.runAndPass(p4, p2_1, p3_1);
        String p4_2 = u.runAndPass(p4, p2_2, p3_1);

        downstreamInstancePopulator.apply(valueStreamMap);

        assertInstances(nodep2, "p2", 1,2);
        assertInstances(nodep3, "p3", 1);
        assertInstances(nodep4, "p4", 1,2);
    }

    @Test
    public void shouldPopulateMultipleInstancesBuiltFromDifferentUpstreamPipelines() {

        /*
            g1 -> P -> P2 -> P4 --> P5
                     \    /
                       P3
        */

        ValueStreamMap valueStreamMap = new ValueStreamMap("p", new PipelineRevision("p", 1, "13.1.1"));
        Node nodep2 = valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p2", "p2"), "p");
        Node nodep3 = valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p3", "p3"), "p");
        Node nodep4 = valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p4", "p4"), "p2");
        valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p4", "p4"), "p3");
        Node nodep5 = valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p5", "p5"), "p4");
        valueStreamMap.addUpstreamMaterialNode(new SCMDependencyNode("g1", "g1", "git"),new CaseInsensitiveString("git"), new Modifications(ModificationsMother.multipleModificationList()), "p");

        GitMaterial g1 = u.wf(new GitMaterial("g1"), "folder3");
        u.checkinInOrder(g1,"g_1");
        u.checkinInOrder(g1,"g_2");

        ScheduleTestUtil.AddedPipeline p = u.saveConfigWith("p", u.m(g1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", u.m(p2), u.m(p3));
        ScheduleTestUtil.AddedPipeline p5 = u.saveConfigWith("p5", u.m(p4));

        String p_1 = u.runAndPass(p, "g_1");
        String p_2 = u.runAndPass(p, "g_2");
        String p2_1 = u.runAndPass(p2, p_1);
        String p2_2 = u.runAndPass(p2, p_1);
        String p2_3 = u.runAndPass(p2, p_2);
        String p3_1 = u.runAndPass(p3, p_1);
        String p3_2 = u.runAndPass(p3, p_1);
        String p4_1 = u.runAndPass(p4, p2_1, p3_1);
        String p4_2 = u.runAndPass(p4, p2_2, p3_1);
        String p4_3 = u.runAndPass(p4, p2_3, p3_2);
        String p5_1 = u.runAndPass(p5, p4_1);
        String p5_2 = u.runAndPass(p5, p4_2);
        String p5_3 = u.runAndPass(p5, p4_3);

        downstreamInstancePopulator.apply(valueStreamMap);

        assertInstances(nodep2, "p2", 1, 2);
        assertInstances(nodep3, "p3", 1, 2);
        assertInstances(nodep4, "p4", 1, 2, 3);
        assertInstances(nodep5, "p5", 1,2, 3);
    }

	@Test
	public void shouldPopulateInstancesBuiltFromCurrentMaterial() {

		/*
			g1 -> P -> P2  -->  P4
			   |		\      /
			   |		 + P3 +
			   +-> Q
			   |
			   +-> R
		*/

		GitMaterial g1 = u.wf(new GitMaterial("g1"), "folder3");
		u.checkinInOrder(g1, "g_1", "g_2");

		ScheduleTestUtil.AddedPipeline p = u.saveConfigWith("p", u.m(g1));
		ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p));
		ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p2));
		ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", u.m(p2), u.m(p3));
		ScheduleTestUtil.AddedPipeline q = u.saveConfigWith("q", u.m(g1));
		ScheduleTestUtil.AddedPipeline r = u.saveConfigWith("r", u.m(g1));

		String p_1 = u.runAndPass(p, "g_1");
		String p2_1 = u.runAndPass(p2, p_1);
		String p3_1 = u.runAndPass(p3, p2_1);
		String p4_1 = u.runAndPass(p4, p2_1, p3_1);
		String q_1 = u.runAndPass(q, "g_1");
		String q_2 = u.runAndPass(q, "g_1");

		MaterialInstance g1Instance = materialRepository.findMaterialInstance(g1);
		Modification g1Modification = materialRepository.findModificationWithRevision(g1, "g_1");

		ValueStreamMap valueStreamMap = new ValueStreamMap(g1, g1Instance, g1Modification);
		Node gitNode = valueStreamMap.getCurrentMaterial();
		Node nodep1 = valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p", "p"), gitNode.getId());
		Node nodep2 = valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p2", "p2"), "p");
		Node nodep3 = valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p3", "p3"), "p2");
		Node nodep4 = valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p4", "p4"), "p3");
		valueStreamMap.addDownstreamNode(new PipelineDependencyNode("p4", "p4"), "p2");
		Node nodep5 = valueStreamMap.addDownstreamNode(new PipelineDependencyNode("q", "q"), gitNode.getId());
		Node nodep6 = valueStreamMap.addDownstreamNode(new PipelineDependencyNode("r", "r"), gitNode.getId());

		downstreamInstancePopulator.apply(valueStreamMap);

		assertInstances(nodep1, "p", 1);
		assertInstances(nodep2, "p2", 1);
		assertInstances(nodep3, "p3", 1);
		assertInstances(nodep4, "p4", 1);
		assertInstances(nodep5, "q", 1, 2);
		assertThat(nodep6.revisions().size(), is(0));
	}

    private void assertInstances(Node node, String pipelineName, Integer... pipelineCounters) {
        List<Revision> revisions = node.revisions();
        assertThat(revisions.size(), is(pipelineCounters.length));
        for (Integer pipelineCounter : pipelineCounters) {
            assertThat(revisions.contains(new PipelineRevision(pipelineName, pipelineCounter, pipelineCounter.toString())), is(true));
        }
    }
}
