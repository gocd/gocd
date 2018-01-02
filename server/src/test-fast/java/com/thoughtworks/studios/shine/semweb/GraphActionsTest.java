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

package com.thoughtworks.studios.shine.semweb;

import static org.junit.Assert.assertEquals;

import com.thoughtworks.studios.shine.cruise.GoOntology;
import com.thoughtworks.studios.shine.semweb.sesame.InMemoryTempGraphFactory;
import org.junit.Test;

public class GraphActionsTest {

    @Test
    public void canRemoveStatements() {
        Graph graph = new InMemoryTempGraphFactory().createTempGraph();
        String rdf = "" +
                "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> ." +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> ." +
                "" +
                "<http://pipeline/1> rdf:type cruise:Pipeline ." +
                "<http://pipeline/1> cruise:hasStage <http://stage/1> ." +
                "<http://pipeline/1> cruise:hasStage <http://stage/2> ." +
                "<http://stage/1> rdf:type cruise:Stage .  " +
                "<http://stage/2> rdf:type cruise:Stage .  ";

        GraphActions actions = new GraphActions(graph);

        actions.queueRemoveResourceStatement(
                graph.createURIReference(GoOntology.PIPELINE_RESOURCE, "http://pipeline/1"),
                GoOntology.HAS_STAGE,
                graph.createURIReference(GoOntology.STAGE_RESOURCE, "http://stage/1"));

        actions.queueRemoveResourceStatement(
                graph.createURIReference(GoOntology.PIPELINE_RESOURCE, "http://pipeline/1"),
                RDFOntology.TYPE,
                graph.createURIReference(GoOntology.STAGE_RESOURCE, "http://stage/2"));

        actions.execute();

        assertEquals(3L, graph.size());
    }

}
