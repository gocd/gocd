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
package com.thoughtworks.go.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DFSCycleDetectorTest {
    private DFSCycleDetector project;
    private PipelineDependencyState state;

    @BeforeEach
    public void setUp() {
        project = new DFSCycleDetector();
        state = mock(PipelineDependencyState.class);
    }

    @Test
    public void shouldThrowExceptionWhenCycleDependencyFound() {
        when(state.getDependencyMaterials(cis("a"))).thenReturn(new Node(new Node.DependencyNode(cis("b"), cis("stage"))));
        when(state.getDependencyMaterials(cis("b"))).thenReturn(new Node(new Node.DependencyNode(cis("c"), cis("stage"))));
        when(state.getDependencyMaterials(cis("c"))).thenReturn(new Node(new Node.DependencyNode(cis("a"), cis("stage"))));
        when(state.hasPipeline(cis("a"))).thenReturn(true);
        when(state.hasPipeline(cis("b"))).thenReturn(true);
        when(state.hasPipeline(cis("c"))).thenReturn(true);

        try {
            project.topoSort(cis("a"), state);
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Circular dependency: a <- c <- b <- a");
        }
    }

    @Test
    public void shouldNotThrowExceptionWhenCycleDependencyNotFound() {
        when(state.getDependencyMaterials(cis("a"))).thenReturn(new Node(new Node.DependencyNode(cis("b"), cis("stage"))));
        when(state.getDependencyMaterials(cis("b"))).thenReturn(new Node(new Node.DependencyNode(cis("c"), cis("stage"))));
        when(state.getDependencyMaterials(cis("c"))).thenReturn(new Node(new ArrayList<>()));

        when(state.hasPipeline(cis("a"))).thenReturn(true);
        when(state.hasPipeline(cis("b"))).thenReturn(true);
        when(state.hasPipeline(cis("c"))).thenReturn(true);
        project.topoSort(cis("a"), state);
    }

    @Test
    public void shouldThrowExceptionWhenDependencyExistsWithUnknownPipeline() {
        when(state.getDependencyMaterials(cis("a"))).thenReturn(new Node(new Node.DependencyNode(cis("b"), cis("stage"))));
        when(state.getDependencyMaterials(cis("b"))).thenReturn(new Node(new Node.DependencyNode(cis("z"), cis("stage"))));
        when(state.hasPipeline(cis("a"))).thenReturn(true);
        when(state.hasPipeline(cis("b"))).thenReturn(true);
        when(state.hasPipeline(cis("z"))).thenReturn(false);

        try {
            project.topoSort(cis("a"), state);
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Pipeline 'z' does not exist. It is used from pipeline 'b'.");
        }
    }
}
