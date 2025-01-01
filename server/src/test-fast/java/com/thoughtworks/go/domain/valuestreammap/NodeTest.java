/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.domain.valuestreammap;

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class NodeTest {
    @Test
    public void shouldReplaceParentNodeAtTheSameIndex() {
        /*
        g  -> p1 -> p3
           |        ^
           +-> p2 --+
         */
        CaseInsensitiveString gitFingerprint = new CaseInsensitiveString("git-fingerprint");
        Node git = new PipelineDependencyNode(gitFingerprint, "git");
        git.setLevel(0);
        CaseInsensitiveString p1Name = new CaseInsensitiveString("p1");
        CaseInsensitiveString p2Name = new CaseInsensitiveString("p2");
        CaseInsensitiveString dummyName = new CaseInsensitiveString("dummy");
        CaseInsensitiveString p3Name = new CaseInsensitiveString("p3");
        Node p1 = new PipelineDependencyNode(p1Name, p1Name.toString());
        p1.setLevel(1);
        Node p2 = new PipelineDependencyNode(p2Name, p2Name.toString());
        p2.setLevel(1);
        Node dummy = new PipelineDependencyNode(dummyName, dummyName.toString());
        dummy.setLevel(1);
        Node p3 = new PipelineDependencyNode(p3Name, p3Name.toString());
        p2.setLevel(2);
        git.addChildIfAbsent(p1);
        git.addChildIfAbsent(p2);
        p1.addParentIfAbsent(git);
        p2.addParentIfAbsent(git);
        p1.addChildIfAbsent(p3);
        p2.addChildIfAbsent(p3);
        p3.addParentIfAbsent(p1);
        p3.addParentIfAbsent(p2);

        assertThat(p3.getParents().size()).isEqualTo(2);
        assertThat(p3.getParents()).contains(p1, p2);

        p3.replaceParentWith(p1, dummy);
        assertThat(p3.getParents().size()).isEqualTo(2);
        assertThat(p3.getParents()).contains(dummy, p2);
    }

    @Test
    public void shouldReplaceChildNodeAtTheSameIndex() {
        /*
        g  -> p1
           |
           +-> p2
         */
        Node git = new PipelineDependencyNode(new CaseInsensitiveString("git-fingerprint"), "git");
        git.setLevel(0);
        Node p1 = new PipelineDependencyNode(new CaseInsensitiveString("p1"), "p1");
        p1.setLevel(1);
        Node p2 = new PipelineDependencyNode(new CaseInsensitiveString("p2"), "p2");
        p2.setLevel(1);
        Node dummy = new PipelineDependencyNode(new CaseInsensitiveString("dummy"), "dummy");
        dummy.setLevel(0);
        git.addChildIfAbsent(p1);
        git.addChildIfAbsent(p2);
        p1.addParentIfAbsent(git);
        p2.addParentIfAbsent(git);

        assertThat(git.getChildren().size()).isEqualTo(2);
        assertThat(git.getChildren()).contains(p1, p2);

        git.replaceChildWith(p1, dummy);
        assertThat(git.getChildren().size()).isEqualTo(2);
        assertThat(git.getChildren()).contains(dummy, p2);
    }

    @Test
    public void shouldGetTypeForNode() {
        Node g1 = new SCMDependencyNode("g1", "g1", "git");
        Node p2 = new PipelineDependencyNode(new CaseInsensitiveString("p2"), "p2");
        Node dummy = new DummyNode("dummy", "dummy");

        assertThat(g1.getType()).isEqualTo(DependencyNodeType.MATERIAL);
        assertThat(p2.getType()).isEqualTo(DependencyNodeType.PIPELINE);
        assertThat(dummy.getType()).isEqualTo(DependencyNodeType.DUMMY);
    }

    @Test
    public void shouldCompareTwoNodesBasedOnBarycenterValue() {
        PipelineDependencyNode p1 = new PipelineDependencyNode(new CaseInsensitiveString("p1"), "p1");
        p1.setLevel(1);
        p1.setDepth(5);
        PipelineDependencyNode p2 = new PipelineDependencyNode(new CaseInsensitiveString("p2"), "p2");
        p2.setLevel(1);
        p2.setDepth(1);
        PipelineDependencyNode p3 = new PipelineDependencyNode(new CaseInsensitiveString("p3"), "p3");
        p3.setLevel(1);
        p3.setDepth(4);

        List<PipelineDependencyNode> nodes = Stream.of(p1, p2, p3).sorted().collect(Collectors.toList());
        assertThat(nodes).isEqualTo(List.of(p2, p3, p1));
    }

    @Test
    public void shouldAddUniqueAndNotNullRevisionsToANode() throws Exception {
        Revision p11 = new PipelineRevision("p1", 1, "label1");
        Revision p12 = new PipelineRevision("p1", 2, "label2");

        PipelineDependencyNode node = new PipelineDependencyNode(new CaseInsensitiveString("p1"), "p1");
        node.setLevel(1);
        node.addRevision(p11);
        node.addRevision(null);
        node.addRevision(p12);
        node.addRevision(p11);

        List<Revision> revisions = node.revisions();
        assertThat(revisions.size()).isEqualTo(2);
        assertThat(revisions).contains(p11, p12);
    }

    @Test
    public void shouldGetRevisionsSortedInOrderOfPipelineCounters() {
        Node node = new PipelineDependencyNode(new CaseInsensitiveString("p"), "p");
        Revision revision_2 = new PipelineRevision("p", 2, "2");
        Revision revision_1 = new PipelineRevision("p", 1, "1");
        Revision revision_3 = new PipelineRevision("p", 3, "3");
        node.addRevision(revision_2);
        node.addRevision(revision_1);
        node.addRevision(revision_3);
        assertThat(node.revisions()).isEqualTo(List.of(revision_3, revision_2, revision_1));
    }
}
