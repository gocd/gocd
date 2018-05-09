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

package com.thoughtworks.studios.shine.semweb.sesame;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.List;

import com.thoughtworks.studios.shine.semweb.BoundVariables;
import com.thoughtworks.studios.shine.semweb.Graph;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml",
        "classpath:testPropertyConfigurer.xml"
})
public class SesameBoundVariablesTest {
    private Graph graph;

    @Before
    public void setup() {
        graph = new InMemoryTempGraphFactory().createTempGraph();
    }

    @After
    public void tearDown() {
        graph.close();
        graph = null;
    }

    @Test
    public void checkGetBoolean() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +

                        "<http://example.com/1> ex:is \"true\"^^xsd:boolean . " +
                        "<http://example.com/2> ex:is \"false\"^^xsd:boolean . ";

        graph.addTriplesFromTurtle(turtle);

        String sparqlSelect =
                "PREFIX ex: <http://example.com/ontology#> " +

                        "SELECT ?one ?two ?three WHERE { " +
                        "<http://example.com/1> ex:is ?one . " +
                        "<http://example.com/2> ex:is ?two . " +
                        "OPTIONAL { <http://example.com/3> ex:is ?three } " +
                        "}";

        BoundVariables bv = graph.selectFirst(sparqlSelect);

        assertTrue(bv.getBoolean("one"));
        assertFalse(bv.getBoolean("two"));
        assertNull(bv.getBoolean("three"));

        try {
            bv.getBoolean("baz");
            fail("Illegal argument exception expected.");
        } catch (IllegalArgumentException e) {
            assertEquals("boundName 'baz' is not in the list of possible values ('one', 'two', 'three')", e.getMessage());
        }
    }

    @Test
    public void checkGetInt() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +

                        "<http://example.com/1> ex:is \"1\"^^xsd:integer . " +
                        "<http://example.com/2> ex:is \"2\"^^xsd:integer . ";

        graph.addTriplesFromTurtle(turtle);

        String sparqlSelect =
                "PREFIX ex: <http://example.com/ontology#> " +

                        "SELECT ?one ?two ?three WHERE { " +
                        "<http://example.com/1> ex:is ?one . " +
                        "<http://example.com/2> ex:is ?two . " +
                        "OPTIONAL { <http://example.com/3> ex:is ?three } " +
                        "}";

        BoundVariables bv = graph.selectFirst(sparqlSelect);

        assertEquals((Integer) 1, bv.getInt("one"));
        assertEquals((Integer) 2, bv.getInt("two"));
        assertNull(bv.getInt("three"));

        try {
            bv.getInt("baz");
            fail("Illegal argument exception expected.");
        } catch (IllegalArgumentException e) {
            assertEquals("boundName 'baz' is not in the list of possible values ('one', 'two', 'three')", e.getMessage());
        }
    }

    @Test
    public void checkGetString() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +

                        "<http://example.com/1> ex:is \"won\"^^xsd:string . " +
                        "<http://example.com/2> ex:is \"too\"^^xsd:string . ";

        graph.addTriplesFromTurtle(turtle);

        String sparqlSelect =
                "PREFIX ex: <http://example.com/ontology#> " +

                        "SELECT ?one ?two ?three WHERE { " +
                        "<http://example.com/1> ex:is ?one . " +
                        "<http://example.com/2> ex:is ?two . " +
                        "OPTIONAL { <http://example.com/3> ex:is ?three } " +
                        "}";

        BoundVariables bv = graph.selectFirst(sparqlSelect);

        assertEquals("won", bv.getString("one"));
        assertEquals("too", bv.getString("two"));
        assertNull(bv.getString("three"));

        try {
            bv.getString("baz");
            fail("Illegal argument exception expected.");
        } catch (IllegalArgumentException e) {
            assertEquals("boundName 'baz' is not in the list of possible values ('one', 'two', 'three')", e.getMessage());
        }
    }

    @Test
    public void checkGetURIReference() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +

                        "<http://example.com/1> ex:is <http://hello> . ";

        graph.addTriplesFromTurtle(turtle);

        String sparqlSelect =
                "PREFIX ex: <http://example.com/ontology#> " +

                        "SELECT ?one ?two WHERE { " +
                        "<http://example.com/1> ex:is ?one . " +
                        "OPTIONAL { [] ex:nope ?two } " +
                        "}";

        BoundVariables bv = graph.selectFirst(sparqlSelect);

        assertEquals("http://hello", bv.getURIReference("one").getURIText());
        assertNull(bv.getURIReference("two"));

        try {
            bv.getURIReference("baz");
            fail("Illegal argument exception expected.");
        } catch (IllegalArgumentException e) {
            assertEquals("boundName 'baz' is not in the list of possible values ('one', 'two')", e.getMessage());
        }
    }

    @Test
    public void checkGetAsStringCanConvertAllTypesToTheirStringForm() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +

                        "<http://example.com/1> ex:is _:foo . " +
                        "<http://example.com/2> ex:is \"true\"^^xsd:boolean . " +
                        "<http://example.com/3> ex:is \"3\"^^xsd:integer . " +
                        "<http://example.com/4> ex:is \"ho hum\"^^xsd:string . " +
                        "<http://example.com/5> ex:is <http://yadda/> . ";

        graph.addTriplesFromTurtle(turtle);

        String sparqlSelect =
                "PREFIX ex: <http://example.com/ontology#> " +

                        "SELECT ?blankNode ?boolean ?int ?string ?uri ?unbound WHERE { " +
                        "<http://example.com/1> ex:is ?blankNode . " +
                        "<http://example.com/2> ex:is ?boolean . " +
                        "<http://example.com/3> ex:is ?int . " +
                        "<http://example.com/4> ex:is ?string . " +
                        "<http://example.com/5> ex:is ?uri . " +
                        "OPTIONAL { <http://example.com/6> ex:is ?unbound }" +
                        "}";

        BoundVariables bv = graph.selectFirst(sparqlSelect);

        assertNotNull(bv.getAsString("blankNode"));
        assertEquals("true", bv.getAsString("boolean"));
        assertEquals("3", bv.getAsString("int"));
        assertEquals("ho hum", bv.getAsString("string"));
        assertEquals("http://yadda/", bv.getAsString("uri"));
        assertNull(bv.getAsString("unbound"));

        try {
            bv.getAsString("baz");
            fail("Illegal argument exception expected.");
        } catch (IllegalArgumentException e) {
            assertEquals("boundName 'baz' is not in the list of possible values ('blankNode', 'boolean', 'int', 'string', 'uri', 'unbound')", e.getMessage());
        }
    }

    @Test
    public void checkGetBoundVariableNames() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +

                        "<http://example.com/1> ex:is <http://hello> . ";

        graph.addTriplesFromTurtle(turtle);

        String sparqlSelect =
                "PREFIX ex: <http://example.com/ontology#> " +

                        "SELECT ?one ?two WHERE { " +
                        "<http://example.com/1> ex:is ?one . " +
                        "OPTIONAL { [] ex:nope ?two } " +
                        "}";

        BoundVariables bv = graph.selectFirst(sparqlSelect);

        assertEquals("one", bv.getBoundVariableNames().get(0));
        assertEquals("two", bv.getBoundVariableNames().get(1));
    }

    @Test
    public void shouldBeSerializable() throws Exception {
        String turtle = "<http://s1> <http://p1> <http://o1> . ";
        graph.addTriplesFromTurtle(turtle);

        String sparqlSelect = "SELECT ?s ?p ?o { ?s ?p ?o . }";

        List<BoundVariables> bvs = graph.select(sparqlSelect);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream objectOS = new ObjectOutputStream(os);

        objectOS.writeObject(bvs);

        ObjectInputStream objectIS = new ObjectInputStream(new ByteArrayInputStream(os.toByteArray()));
        List<BoundVariables>  bvsFromInputStream = (List<BoundVariables>) objectIS.readObject();
        assertEquals(1, bvsFromInputStream.size());

        assertEquals("http://s1", bvsFromInputStream.get(0).getAsString("s"));
        assertEquals("http://p1", bvsFromInputStream.get(0).getAsString("p"));
        assertEquals("http://o1", bvsFromInputStream.get(0).getAsString("o"));

    }
}
