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
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.valuestreammap.Node;
import com.thoughtworks.go.domain.valuestreammap.NodeLevelMap;
import com.thoughtworks.go.domain.valuestreammap.PipelineDependencyNode;
import com.thoughtworks.go.domain.valuestreammap.PipelineRevision;
import com.thoughtworks.go.domain.valuestreammap.SCMDependencyNode;
import com.thoughtworks.go.domain.valuestreammap.ValueStreamMap;
import com.thoughtworks.go.helper.ModificationsMother;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

public class LevelAssignmentTest {

    @Test
    public void shouldAssignLevelsForUpstreamNodesOfCurrentPipeline() throws Exception {

        /*
        ---> p1 ---
       /            \
       g -----------p3
       \            /
        ---> p2 ---
        */

        String current = "p3";
        Node p1 = new PipelineDependencyNode("p1", "p1");
        Node p2 = new PipelineDependencyNode("p2", "p2");
        Node gitNode = new SCMDependencyNode("git", "g", "git");

        ValueStreamMap valueStreamMap = new ValueStreamMap(current, new PipelineRevision(current, 1, "1"));
        valueStreamMap.addUpstreamNode(p1, new PipelineRevision("p1", 1, "1"), current);
        valueStreamMap.addUpstreamNode(p2, new PipelineRevision("p2", 1, "1"), current);
        valueStreamMap.addUpstreamMaterialNode(gitNode, new CaseInsensitiveString("trunk"), "p1", new MaterialRevision(null));
        valueStreamMap.addUpstreamMaterialNode(gitNode, new CaseInsensitiveString("main-branch"), "p3", new MaterialRevision(null));
        valueStreamMap.addUpstreamMaterialNode(gitNode, new CaseInsensitiveString("main-branch"), "p2", new MaterialRevision(null));
        NodeLevelMap levelToNodeMap = new LevelAssignment().apply(valueStreamMap);

        assertThat(valueStreamMap.getCurrentPipeline().getLevel(), is(0));
        assertThat(p1.getLevel(), is(-1));
        assertThat(p2.getLevel(), is(-1));
        assertThat(gitNode.getLevel(), is(-2));

        assertThat(levelToNodeMap.get(0), contains(valueStreamMap.getCurrentPipeline()));
        assertThat(levelToNodeMap.get(-1), containsInAnyOrder(p1, p2));
        assertThat(levelToNodeMap.get(-2), contains(gitNode));
    }

    @Test
    public void shouldAssignLevelsForDownstreamNodesOfCurrentPipeline() throws Exception {

    	/*
                ---> p1 ----
               /            \
    	git --> p ----------> p3
              \            /
               ---> p2 ----
    	*/

        String current = "p";
        Node p1 = new PipelineDependencyNode("p1", "p1");
        Node p2 = new PipelineDependencyNode("p2", "p2");
        Node p3 = new PipelineDependencyNode("p3", "p3");
        Node gitNode = new SCMDependencyNode("git", "g", "git");

        ValueStreamMap valueStreamMap = new ValueStreamMap(current, new PipelineRevision(current, 1, "1"));
        valueStreamMap.addUpstreamMaterialNode(gitNode, new CaseInsensitiveString("trunk"), "p", new MaterialRevision(null));

        valueStreamMap.addDownstreamNode(p1, current);
        valueStreamMap.addDownstreamNode(p2, current);
        valueStreamMap.addDownstreamNode(p3, p1.getId());
        valueStreamMap.addDownstreamNode(p3, p2.getId());

        NodeLevelMap levelToNodeMap = new LevelAssignment().apply(valueStreamMap);

        assertThat(valueStreamMap.getCurrentPipeline().getLevel(), is(0));
        assertThat(gitNode.getLevel(), is(-1));
        assertThat(p1.getLevel(), is(1));
        assertThat(p2.getLevel(), is(1));
        assertThat(p3.getLevel(), is(2));

        assertThat(levelToNodeMap.get(0), contains(valueStreamMap.getCurrentPipeline()));
        assertThat(levelToNodeMap.get(-1), contains(gitNode));
        assertThat(levelToNodeMap.get(1), containsInAnyOrder(p1, p2));
        assertThat(levelToNodeMap.get(2), contains(p3));
    }

	@Test
	public void shouldAssignLevelsForDownstreamNodesOfCurrentMaterial() throws Exception {

		/*
				---> p1 ----
			   /            \
		git --> p ----------> p3
			  \            /
			   ---> p2 ----
		*/

		Node p = new PipelineDependencyNode("p", "p");
		Node p1 = new PipelineDependencyNode("p1", "p1");
		Node p2 = new PipelineDependencyNode("p2", "p2");
		Node p3 = new PipelineDependencyNode("p3", "p3");

		GitMaterial material = new GitMaterial("url");
		Modification modification = ModificationsMother.aCheckIn("1");
		ValueStreamMap valueStreamMap = new ValueStreamMap(material, null, modification);
		Node gitNode = valueStreamMap.getCurrentMaterial();

		valueStreamMap.addDownstreamNode(p, gitNode.getId());
		valueStreamMap.addDownstreamNode(p1, p.getId());
		valueStreamMap.addDownstreamNode(p2, p.getId());
		valueStreamMap.addDownstreamNode(p3, p1.getId());
		valueStreamMap.addDownstreamNode(p3, p2.getId());

		NodeLevelMap levelToNodeMap = new LevelAssignment().apply(valueStreamMap);

		assertThat(valueStreamMap.getCurrentMaterial().getLevel(), is(0));
		assertThat(p.getLevel(), is(1));
		assertThat(p1.getLevel(), is(2));
		assertThat(p2.getLevel(), is(2));
		assertThat(p3.getLevel(), is(3));

		assertThat(levelToNodeMap.get(0), contains(valueStreamMap.getCurrentMaterial()));
		assertThat(levelToNodeMap.get(1), containsInAnyOrder(p));
		assertThat(levelToNodeMap.get(2), containsInAnyOrder(p1, p2));
		assertThat(levelToNodeMap.get(3), contains(p3));
	}
}
