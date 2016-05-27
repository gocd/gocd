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

package com.thoughtworks.go.domain.valuestreammap;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class NodeTest {
    @Test
    public void shouldReplaceParentNodeAtTheSameIndex() {
        /*
        g  -> p1 -> p3
           |        ^
           +-> p2 --+
         */
        Node git = new PipelineDependencyNode("git-fingerprint", "git");
        git.setLevel(0);
        Node p1 = new PipelineDependencyNode("p1", "p1");
        p1.setLevel(1);
        Node p2 = new PipelineDependencyNode("p2", "p2");
        p2.setLevel(1);
        Node dummy = new PipelineDependencyNode("dummy", "dummy");
        dummy.setLevel(1);
        Node p3 = new PipelineDependencyNode("p3", "p3");
        p2.setLevel(2);
        git.addChildIfAbsent(p1);
        git.addChildIfAbsent(p2);
        p1.addParentIfAbsent(git);
        p2.addParentIfAbsent(git);
        p1.addChildIfAbsent(p3);
        p2.addChildIfAbsent(p3);
        p3.addParentIfAbsent(p1);
        p3.addParentIfAbsent(p2);

        assertThat(p3.getParents().size(), is(2));
        assertThat(p3.getParents(), hasItems(p1, p2));

        p3.replaceParentWith(p1, dummy);
        assertThat(p3.getParents().size(), is(2));
        assertThat(p3.getParents(), hasItems(dummy, p2));
    }

    @Test
    public void shouldReplaceChildNodeAtTheSameIndex() {
        /*
        g  -> p1
           |
           +-> p2
         */
        Node git = new PipelineDependencyNode("git-fingerprint", "git");
        git.setLevel(0);
        Node p1 = new PipelineDependencyNode("p1", "p1");
        p1.setLevel(1);
        Node p2 = new PipelineDependencyNode("p2", "p2");
        p2.setLevel(1);
        Node dummy = new PipelineDependencyNode("dummy", "dummy");
        dummy.setLevel(0);
        git.addChildIfAbsent(p1);
        git.addChildIfAbsent(p2);
        p1.addParentIfAbsent(git);
        p2.addParentIfAbsent(git);

        assertThat(git.getChildren().size(), is(2));
        assertThat(git.getChildren(), hasItems(p1, p2));

        git.replaceChildWith(p1, dummy);
        assertThat(git.getChildren().size(), is(2));
        assertThat(git.getChildren(), hasItems(dummy, p2));
    }

    @Test
    public void shouldGetTypeForNode() {
        Node g1 = new SCMDependencyNode("g1", "g1", "git");
        Node p2 = new PipelineDependencyNode("p2", "p2");
        Node dummy = new DummyNode("dummy", "dummy");

        assertThat(g1.getType(), is(DependencyNodeType.MATERIAL));
        assertThat(p2.getType(), is(DependencyNodeType.PIPELINE));
        assertThat(dummy.getType(), is(DependencyNodeType.DUMMY));
    }

    @Test
    public void shouldCompareTwoNodesBasedOnBarycenterValue() {
        PipelineDependencyNode p1 = new PipelineDependencyNode("p1", "p1");
        p1.setLevel(1);
        p1.setDepth(5);
        PipelineDependencyNode p2 = new PipelineDependencyNode("p2", "p2");
        p2.setLevel(1);
        p2.setDepth(1);
        PipelineDependencyNode p3 = new PipelineDependencyNode("p3", "p3");
        p3.setLevel(1);
        p3.setDepth(4);

        List<PipelineDependencyNode> nodes = Arrays.asList(p1, p2, p3);
        Collections.sort(nodes);
        assertThat(nodes, is(Arrays.asList(p2, p3, p1)));
    }

    @Test
    public void shouldAddUniqueAndNotNullRevisionsToANode() throws Exception {
        Revision p11 = new PipelineRevision("p1", 1, "label1");
        Revision p12 = new PipelineRevision("p1", 2, "label2");

        PipelineDependencyNode node = new PipelineDependencyNode("p1", "p1");
        node.setLevel(1);
        node.addRevision(p11);
        node.addRevision(null);
        node.addRevision(p12);
        node.addRevision(p11);

        List<Revision> revisions = node.revisions();
        assertThat(revisions.toString(), revisions.size(), is(2));
        assertThat(revisions, hasItems(p11, p12));
    }

    @Test
    public void shouldGetRevisionsSortedInOrderOfPipelineCounters() {
        Node node = new PipelineDependencyNode("p", "p");
        Revision revision_2 = new PipelineRevision("p", 2, "2");
        Revision revision_1 = new PipelineRevision("p", 1, "1");
        Revision revision_3 = new PipelineRevision("p", 3, "3");
        node.addRevision(revision_2);
        node.addRevision(revision_1);
        node.addRevision(revision_3);
        assertThat(node.revisions(), is(Arrays.asList(revision_3, revision_2, revision_1)));
    }
}
