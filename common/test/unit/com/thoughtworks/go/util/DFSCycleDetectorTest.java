/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.util;

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Hashtable;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DFSCycleDetectorTest {
    public Hashtable<CaseInsensitiveString, Node> hashtable;
    private DFSCycleDetector project;
    private PipelineDependencyState state;

    @Before
    public void setUp() {
        project = new DFSCycleDetector();
        state = mock(PipelineDependencyState.class);
    }

    @Test
    public void shouldThrowExceptionWhenCycleDependencyFound() throws Exception {
        when(state.getDependencyMaterials(new CaseInsensitiveString("a"))).thenReturn(new Node(new Node.DependencyNode(new CaseInsensitiveString("b"), new CaseInsensitiveString("stage"))));
        when(state.getDependencyMaterials(new CaseInsensitiveString("b"))).thenReturn(new Node(new Node.DependencyNode(new CaseInsensitiveString("c"), new CaseInsensitiveString("stage"))));
        when(state.getDependencyMaterials(new CaseInsensitiveString("c"))).thenReturn(new Node(new Node.DependencyNode(new CaseInsensitiveString("a"), new CaseInsensitiveString("stage"))));
        when(state.hasPipeline(new CaseInsensitiveString("a"))).thenReturn(true);
        when(state.hasPipeline(new CaseInsensitiveString("b"))).thenReturn(true);
        when(state.hasPipeline(new CaseInsensitiveString("c"))).thenReturn(true);

        try {
            project.topoSort(new CaseInsensitiveString("a"), state);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Circular dependency: a <- c <- b <- a"));
        }
    }

    @Test
    public void shouldNotThrowExceptionWhenCycleDependencyNotFound() throws Exception {
        when(state.getDependencyMaterials(new CaseInsensitiveString("a"))).thenReturn(new Node(new Node.DependencyNode(new CaseInsensitiveString("b"), new CaseInsensitiveString("stage"))));
        when(state.getDependencyMaterials(new CaseInsensitiveString("b"))).thenReturn(new Node(new Node.DependencyNode(new CaseInsensitiveString("c"), new CaseInsensitiveString("stage"))));
        when(state.getDependencyMaterials(new CaseInsensitiveString("c"))).thenReturn(new Node(new ArrayList<Node.DependencyNode>()));

        when(state.hasPipeline(new CaseInsensitiveString("a"))).thenReturn(true);
        when(state.hasPipeline(new CaseInsensitiveString("b"))).thenReturn(true);
        when(state.hasPipeline(new CaseInsensitiveString("c"))).thenReturn(true);
        project.topoSort(new CaseInsensitiveString("a"), state);
    }

    @Test public void shouldThrowExceptionWhenDependencyExistsWithUnknownPipeline() throws Exception {
        when(state.getDependencyMaterials(new CaseInsensitiveString("a"))).thenReturn(new Node(new Node.DependencyNode(new CaseInsensitiveString("b"), new CaseInsensitiveString("stage"))));
        when(state.getDependencyMaterials(new CaseInsensitiveString("b"))).thenReturn(new Node(new Node.DependencyNode(new CaseInsensitiveString("z"), new CaseInsensitiveString("stage"))));
        when(state.hasPipeline(new CaseInsensitiveString("a"))).thenReturn(true);
        when(state.hasPipeline(new CaseInsensitiveString("b"))).thenReturn(true);
        when(state.hasPipeline(new CaseInsensitiveString("z"))).thenReturn(false);

        try {
            project.topoSort(new CaseInsensitiveString("a"), state);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Pipeline \"z\" does not exist. It is used from pipeline \"b\"."));
        }
    }
}
