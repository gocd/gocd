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

import java.util.List;

import org.junit.Test;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class NodeLevelMapTest {

    @Test
    public void shouldGetNodeLevelsList() throws Exception {
        NodeLevelMap nodeLevelMap = new NodeLevelMap();
        Node svn = new SCMDependencyNode("svn-fingerprint", "svn", "svn");
        svn.setLevel(-1);
        Node current = new PipelineDependencyNode("current","current");
        current.setLevel(0);
        Node p1 = new PipelineDependencyNode("p1","p1");
        p1.setLevel(1);
        Node p2 = new PipelineDependencyNode("p2","p2");
        p2.setLevel(1);

        svn.addEdge(current);
        current.addEdge(p1);
        current.addEdge(p2);
        current.addEdge(p2);

        nodeLevelMap.add(svn);
        nodeLevelMap.add(p1);
        nodeLevelMap.add(p2);
        nodeLevelMap.add(current);

        List<List<Node>> nodeLevels = nodeLevelMap.nodesAtEachLevel();
        assertThat(nodeLevels.size(), is(3));
        assertThat(nodeLevels.get(0), contains(svn));
        assertThat(nodeLevels.get(1), contains(current));
        assertThat(nodeLevels.get(2), contains(p1, p2));
    }
}
