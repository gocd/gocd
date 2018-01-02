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

import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractURIReferenceTestBase {

    private Graph graph;

    public abstract Graph getGraph();

    @Before
    public void setup() {
        graph = getGraph();
    }

    @After
    public void tearDown() {
        graph.close();
        graph = null;
    }

    @Test
    public void canGetURIAsText() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +

                        "<http://example.com/1> a ex:Whatever . ";

        graph.addTriplesFromTurtle(turtle);

        String sparqlSelect =
                "PREFIX ex: <http://example.com/ontology#> " +

                        "SELECT ?node  WHERE { " +
                        "?node a ex:Whatever " +
                        "}";

        BoundVariables bv = graph.selectFirst(sparqlSelect);
        assertEquals("http://example.com/1", bv.getURIReference("node").getURIText());
    }

    @Test
    public void checkGetSPARQLForm() {
        RDFType thingyType = new RDFType("http://example.com/ontology#Thingy");

        URIReference uriReference = graph.createURIReference(thingyType, "http://iAm/");

        assertEquals("<http://iAm/>", uriReference.getSPARQLForm());
    }
}
