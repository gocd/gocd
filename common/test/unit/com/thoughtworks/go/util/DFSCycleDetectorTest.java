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

package com.thoughtworks.go.util;

import java.util.Hashtable;

import com.thoughtworks.go.config.CaseInsensitiveString;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;

public class DFSCycleDetectorTest {
    public Hashtable<CaseInsensitiveString, Node> hashtable;
    private DFSCycleDetector project;

    @Before
    public void setUp() {
        hashtable = new Hashtable<CaseInsensitiveString, Node>();
        project = new DFSCycleDetector();
    }

    @Test
    public void shouldThrowExceptionWhenCycleDepencyFound() throws Exception {
        createNode(new CaseInsensitiveString("a"), new CaseInsensitiveString("b"));
        createNode(new CaseInsensitiveString("b"), new CaseInsensitiveString("c"));
        createNode(new CaseInsensitiveString("c"), new CaseInsensitiveString("a"));
        try {
            project.topoSort(new CaseInsensitiveString("a"), hashtable);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Circular dependency: a <- c <- b <- a"));
        }
    }

    @Test
    public void shouldNotThrowExceptionWhenCycleDepencyFound() throws Exception {
        createNode(new CaseInsensitiveString("a"), new CaseInsensitiveString("b"));
        createNode(new CaseInsensitiveString("b"), new CaseInsensitiveString("c"));
        createNode(new CaseInsensitiveString("c"));
        project.topoSort(new CaseInsensitiveString("a"), hashtable);
    }

    @Test public void shouldThrowExceptionWhenDependencyExistsWithUnknownPipeline() throws Exception {
        createNode(new CaseInsensitiveString("a"), new CaseInsensitiveString("b"));
        createNode(new CaseInsensitiveString("b"), new CaseInsensitiveString("z"));
        try {
            project.topoSort(new CaseInsensitiveString("a"), hashtable);
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Pipeline \"z\" does not exist. It is used from pipeline \"b\"."));
        }
    }

    private Node createNode(CaseInsensitiveString name, CaseInsensitiveString... deps) {
        Node node = new Node(deps);
        hashtable.put(name, node);
        return node;
    }
}
