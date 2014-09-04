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

package com.thoughtworks.studios.shine.cruise;

import java.util.List;

import com.thoughtworks.studios.shine.semweb.BoundVariables;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.sesame.InMemoryTempGraphFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SesameDateTimeOrderingTest {

    @Test
    public void sesameBetterOrderDateTimesCorrectly() throws Exception {
        Graph tempGraph = new InMemoryTempGraphFactory().createTempGraph();

        tempGraph.addTriplesFromTurtle("" +
                "@prefix xsd:<http://www.w3.org/2001/XMLSchema#>  . " +
                "<http://foo1> <http://bar> \"2005-02-28T00:00:00Z\"^^xsd:dateTime . " +
                "<http://foo2> <http://bar> \"2005-02-28T00:00:01Z\"^^xsd:dateTime . " +
                "<http://foo3> <http://bar> \"2005-02-28T00:00:00-05:00\"^^xsd:dateTime . " +
                "");

        List<BoundVariables> bvs = tempGraph.select("PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                "SELECT ?subject WHERE { " +
                "  ?subject ?p ?o . " +
                "} ORDER BY (?o)"
        );

        assertEquals("http://foo1", bvs.get(0).getAsString("subject"));
        assertEquals("http://foo2", bvs.get(1).getAsString("subject"));
        assertEquals("http://foo3", bvs.get(2).getAsString("subject"));

        bvs = tempGraph.select("PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                "SELECT ?subject WHERE { " +
                "  ?subject ?p ?o . " +
                "} ORDER BY DESC (?o)"
        );

        assertEquals("http://foo3", bvs.get(0).getAsString("subject"));
        assertEquals("http://foo2", bvs.get(1).getAsString("subject"));
        assertEquals("http://foo1", bvs.get(2).getAsString("subject"));
    }

}
