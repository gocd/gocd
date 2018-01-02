/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.studios.shine.semweb.sesame;

import com.thoughtworks.studios.shine.ShineRuntimeException;
import com.thoughtworks.studios.shine.semweb.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import java.io.StringReader;
import java.util.List;

import static com.thoughtworks.studios.shine.AssertUtils.assertAskIsFalse;
import static com.thoughtworks.studios.shine.AssertUtils.assertAskIsTrue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SesameGraphTest {
    private final static String CONTEXT = "http://foo.com/context";

    Repository sesameRepository = InMemoryRepositoryFactory.emptyRepository();
    InMemoryTempGraphFactory graphFactory = new InMemoryTempGraphFactory();
    protected Graph contextAwareGraph;
    protected Graph contextUnawareGraph;

    @Before
    public void setUp() {
        contextUnawareGraph = createSesameGraph(null);

        contextAwareGraph = createSesameGraph(CONTEXT);
    }

    @After
    public void tearDown() {
        contextAwareGraph.close();
        contextAwareGraph = null;

        contextUnawareGraph.close();
        contextUnawareGraph = null;
    }

    private Graph createSesameGraph(String contextURI) {
        try {
            return new SesameGraph(sesameRepository.getConnection(), contextURI);
        } catch (RepositoryException ex) {
            throw new ShineRuntimeException("Unable to get connection to repos!", ex);
        }
    }

    @Test
    public void checkAddStatementToGraphWithURIReferenceAsObject() {
        Resource subject = contextUnawareGraph.createURIReference(new RDFType("http://example.com/subject"), "http://example.com/1");
        RDFProperty predicate = new RDFProperty("http://example.com/predicate");
        Resource object = contextUnawareGraph.createURIReference(new RDFType("http://example.com/object"), "http://example.com/2");

        contextUnawareGraph.addStatement(subject, predicate, object);

        String sparql =
                "ASK WHERE { " +
                        "<http://example.com/1> <http://example.com/predicate> <http://example.com/2> . " +
                        "<http://example.com/1> a <http://example.com/subject> . " +
                        "<http://example.com/2> a <http://example.com/object> " +
                        "}";

        assertAskIsTrue(contextUnawareGraph, sparql);
    }

    @Test
    public void checkAddStatementToGraphWithURIReferenceAsObjectInContext() {
        Resource subject = contextAwareGraph.createURIReference(new RDFType("http://example.com/subject"), "http://example.com/1");
        RDFProperty predicate = new RDFProperty("http://example.com/predicate");
        Resource object = contextAwareGraph.createURIReference(new RDFType("http://example.com/object"), "http://example.com/2");

        contextAwareGraph.addStatement(subject, predicate, object);

        String sparqlWithContext =
                "ASK " +
                        "FROM NAMED <http://foo.com/context> " +
                        "WHERE { " +
                        " GRAPH <" + CONTEXT + "> { " +
                        "  <http://example.com/1> <http://example.com/predicate> <http://example.com/2> . " +
                        "  <http://example.com/1> a <http://example.com/subject> . " +
                        "  <http://example.com/2> a <http://example.com/object> " +
                        " } " +
                        "}";

        assertAskIsTrue(contextUnawareGraph, (sparqlWithContext));
    }

    @Test
    public void checkAddStatementWithIntegerObject() {
        Resource subject = contextUnawareGraph.createURIReference(new RDFType("http://example.com/subject"), "http://example.com/1");
        RDFProperty predicate = new RDFProperty("http://example.com/predicate");

        contextUnawareGraph.addStatement(subject, predicate, 3);

        String sparql =
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                        "ASK WHERE { " +
                        "<http://example.com/1> <http://example.com/predicate> \"3\"^^xsd:integer . " +
                        "}";

        assertAskIsTrue(contextUnawareGraph, sparql);
    }

    @Test
    public void checkAddStatementWithIntegerObjectInContext() {
        Resource subject = contextAwareGraph.createURIReference(new RDFType("http://example.com/subject"), "http://example.com/1");
        RDFProperty predicate = new RDFProperty("http://example.com/predicate");

        contextAwareGraph.addStatement(subject, predicate, 3);

        String sparqlWithContext =
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                        "ASK " +
                        "FROM NAMED <http://foo.com/context> " +
                        "WHERE { " +
                        "GRAPH <http://foo.com/context> {" +
                        "  <http://example.com/1> <http://example.com/predicate> \"3\"^^xsd:integer . " +
                        "  <http://example.com/1> a <http://example.com/subject> . " +
                        " } " +
                        "}";

        assertAskIsTrue(contextUnawareGraph, sparqlWithContext);
    }

    @Test
    public void checkAddStatementWithStringObject() {
        Resource subject = contextUnawareGraph.createURIReference(new RDFType("http://example.com/subject"), "http://example.com/1");
        RDFProperty predicate = new RDFProperty("http://example.com/predicate");

        contextUnawareGraph.addStatement(subject, predicate, "boo");

        String sparql =
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                        "ASK WHERE { " +
                        "  <http://example.com/1> <http://example.com/predicate> \"boo\"^^xsd:string . " +
                        "  <http://example.com/1> a <http://example.com/subject> . " +
                        "}";

        assertAskIsTrue(contextUnawareGraph, sparql);
    }

    @Test
    public void checkAddStatementWithStringObjectInContext() {

        Resource subject = contextAwareGraph.createURIReference(new RDFType("http://example.com/subject"), "http://example.com/1");
        RDFProperty predicate = new RDFProperty("http://example.com/predicate");

        contextAwareGraph.addStatement(subject, predicate, "boo");

        String sparqlWithContext =
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                        "ASK FROM NAMED <http://foo.com/context> WHERE { " +
                        " GRAPH <http://foo.com/context> {" +
                        "  <http://example.com/1> <http://example.com/predicate> \"boo\"^^xsd:string . " +
                        "  <http://example.com/1> a <http://example.com/subject> . " +
                        " } " +
                        "}";

        assertAskIsTrue(contextUnawareGraph, sparqlWithContext);
    }

    @Test
    public void checkAddStatementWithBooleanObject() {
        Resource subject = contextUnawareGraph.createURIReference(new RDFType("http://example.com/subject"), "http://example.com/1");
        RDFProperty predicate = new RDFProperty("http://example.com/predicate");

        contextUnawareGraph.addStatement(subject, predicate, false);

        String sparql =
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                        "ASK WHERE { " +
                        "<http://example.com/1> <http://example.com/predicate> \"false\"^^xsd:boolean . " +
                        "}";

        assertAskIsTrue(contextUnawareGraph, sparql);
    }

    @Test
    public void checkAddStatementWithBooleanObjectInContext() {
        Resource subject = contextAwareGraph.createURIReference(new RDFType("http://example.com/subject"), "http://example.com/1");
        RDFProperty predicate = new RDFProperty("http://example.com/predicate");

        contextAwareGraph.addStatement(subject, predicate, false);

        String sparqlWithContext =
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                        "ASK FROM NAMED <http://foo.com/context> WHERE { " +
                        " GRAPH <http://foo.com/context> {" +
                        "  <http://example.com/1> <http://example.com/predicate> \"false\"^^xsd:boolean . " +
                        " } " +
                        "}";

        assertAskIsTrue(contextUnawareGraph, sparqlWithContext);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateURIRefenceExplodesWhenGivenANullType() {
        contextUnawareGraph.createURIReference(null, "http://example.com/1");
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateURIRefenceExplodesWhenGivenANullURI() {
        contextUnawareGraph.createURIReference(new RDFType("http://example.com/subject"), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ensureCreateURIRefenceExplodesWhenGivenAnInvalidURI() {
        contextUnawareGraph.createURIReference(new RDFType("http://example.com/subject"), "_:iAmABlankNodeNotAnURIIShouldBreak");
    }

    @Test
    public void ensureThatAskWorks() {
        String rdf =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "<http://example.com/1> ex:Predicate <http://example.com/2> . ";

        contextUnawareGraph.addTriplesFromTurtle(rdf);

        String falseSparql =
                "PREFIX ex: <http://example.com/ontology#>" +
                        "ASK WHERE { " +
                        " <http://example.com/1> ex:Predicate <http://example.com/254645> . " +
                        "}";

        assertAskIsFalse(contextUnawareGraph, falseSparql);

        String trueSparql =
                "PREFIX ex: <http://example.com/ontology#>" +
                        "ASK WHERE { " +
                        " <http://example.com/1> ex:Predicate <http://example.com/2> . " +
                        "}";

        assertAskIsTrue(contextUnawareGraph, trueSparql);
    }

    @Test
    public void ensureThatAskWorksWithContext() {
        String rdf =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "<http://example.com/1> ex:Predicate <http://example.com/2> . ";

        contextAwareGraph.addTriplesFromTurtle(rdf);

        String falseSparql =
                "PREFIX ex: <http://example.com/ontology#>" +
                        "ASK WHERE { " +
                        " <http://example.com/1> ex:Predicate <http://example.com/254645> . " +
                        "}";

        assertAskIsFalse(contextAwareGraph, falseSparql);
        assertAskIsFalse(contextUnawareGraph, falseSparql);

        String trueSparql =
                "PREFIX ex: <http://example.com/ontology#>" +
                        "ASK WHERE { " +
                        " <http://example.com/1> ex:Predicate <http://example.com/2> . " +
                        "}";

        assertAskIsTrue(contextAwareGraph, trueSparql);
        assertAskIsTrue(contextUnawareGraph, trueSparql);

        String falseSparqlWithContext =
                "PREFIX ex: <http://example.com/ontology#>" +
                        "ASK FROM NAMED <http://foo.com/context> WHERE { " +
                        " GRAPH <http://foo.com/context> {" +
                        "  <http://example.com/1> ex:Predicate <http://example.com/254645> . " +
                        " }" +
                        "}";

        assertAskIsFalse(contextUnawareGraph, falseSparqlWithContext);

        String trueSparqlWithContext =
                "PREFIX ex: <http://example.com/ontology#>" +
                        "ASK FROM NAMED <http://foo.com/context> WHERE { " +
                        " GRAPH <http://foo.com/context> {" +
                        "  <http://example.com/1> ex:Predicate <http://example.com/2> . " +
                        " }" +
                        "}";

        assertAskIsTrue(contextUnawareGraph, trueSparqlWithContext);
    }

    @Test
    public void ensureAddTriplesFromTURTLEWorks() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> rdf:type ex:Yadda . ";

        contextUnawareGraph.addTriplesFromTurtle(turtle);

        assertAskIsTrue(contextUnawareGraph, "PREFIX ex: <http://example.com/ontology#> ASK WHERE { <http://example.com/1> a ex:Yadda }");
    }

    @Test
    public void ensureAddTriplesFromTURTLEWorksWithContext() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> rdf:type ex:Yadda . ";

        contextAwareGraph.addTriplesFromTurtle(turtle);

        assertAskIsTrue(contextAwareGraph, "PREFIX ex: <http://example.com/ontology#> ASK WHERE { <http://example.com/1> a ex:Yadda }");
        assertAskIsTrue(contextUnawareGraph, "PREFIX ex: <http://example.com/ontology#> ASK WHERE { <http://example.com/1> a ex:Yadda }");

        assertAskIsTrue(contextUnawareGraph,
                "PREFIX ex: <http://example.com/ontology#> ASK FROM NAMED <http://foo.com/context> WHERE { GRAPH <http://foo.com/context> {<http://example.com/1> a ex:Yadda } }");
    }

    @Test
    public void ensureAddTriplesFromXMLRDFAbbrevWorks() {
        String rdf =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
                        "" +
                        "<rdf:Description rdf:about=\"http://example.com/1\">" +
                        "<rdf:type rdf:resource=\"http://example.com/ontology#Yadda\"/>" +
                        "</rdf:Description>" +
                        "" +
                        "</rdf:RDF>";

        contextUnawareGraph.addTriplesFromRDFXMLAbbrev(new StringReader(rdf));

        assertAskIsTrue(contextUnawareGraph, "PREFIX ex: <http://example.com/ontology#> ASK WHERE { <http://example.com/1> a ex:Yadda }");
    }

    @Test
    public void ensureAddTriplesFromXMLRDFAbbrevWorksWithContext() {
        String rdf =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">" +
                        "" +
                        "<rdf:Description rdf:about=\"http://example.com/1\">" +
                        "<rdf:type rdf:resource=\"http://example.com/ontology#Yadda\"/>" +
                        "</rdf:Description>" +
                        "" +
                        "</rdf:RDF>";

        contextAwareGraph.addTriplesFromRDFXMLAbbrev(new StringReader(rdf));

        assertAskIsTrue(contextAwareGraph, "PREFIX ex: <http://example.com/ontology#> ASK WHERE { <http://example.com/1> a ex:Yadda }");
        assertAskIsTrue(contextUnawareGraph, "PREFIX ex: <http://example.com/ontology#> ASK WHERE { <http://example.com/1> a ex:Yadda }");


        assertAskIsTrue(contextUnawareGraph,
                "PREFIX ex: <http://example.com/ontology#> ASK FROM NAMED <http://foo.com/context> WHERE { GRAPH <http://foo.com/context> {<http://example.com/1> a ex:Yadda } }");
    }


    @Test
    public void ensureThatSelectFirstWorks() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> rdf:type ex:Yadda . ";

        contextUnawareGraph.addTriplesFromTurtle(turtle);

        String selectFirst =
                "PREFIX ex: <http://example.com/ontology#> " +
                        "SELECT ?s WHERE " +
                        "{ " +
                        "?s a ex:Yadda " +
                        "}";

        BoundVariables boundVariables = contextUnawareGraph.selectFirst(selectFirst);
        assertEquals("http://example.com/1", boundVariables.getURIReference("s").getURIText());
    }

    @Test
    public void ensureThatSelectFirstWorksWithContext() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> rdf:type ex:Yadda . ";

        contextAwareGraph.addTriplesFromTurtle(turtle);

        String selectFirst =
                "PREFIX ex: <http://example.com/ontology#> " +
                        "SELECT ?s " +
                        "WHERE { " +
                        "  ?s a ex:Yadda " +
                        "}";

        BoundVariables boundVariables = contextAwareGraph.selectFirst(selectFirst);
        assertEquals("http://example.com/1", boundVariables.getURIReference("s").getURIText());

        String selectFirstWithContext =
                "PREFIX ex: <http://example.com/ontology#> " +
                        "SELECT ?s " +
                        "FROM NAMED <http://foo.com/context> " +
                        "WHERE { " +
                        " GRAPH <http://foo.com/context> {" +
                        "  ?s a ex:Yadda " +
                        " } " +
                        "}";

        boundVariables = contextUnawareGraph.selectFirst(selectFirstWithContext);
        assertEquals("http://example.com/1", boundVariables.getURIReference("s").getURIText());
    }

    @Test(expected = MoreThanOneResultFoundException.class)
    public void ensureThatSelectFirstExplodesWithMoreThanOneResult() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> rdf:type ex:Yadda . " +
                        "<http://example.com/2> rdf:type ex:Yadda . ";

        contextUnawareGraph.addTriplesFromTurtle(turtle);

        String selectFirst =
                "PREFIX ex: <http://example.com/ontology#> " +

                        "SELECT ?s WHERE { " +
                        "?s a ex:Yadda " +
                        "}";

        contextUnawareGraph.selectFirst(selectFirst);
    }

    @Test
    public void ensureTheSelectFirstReturnsNullWhenNoResults() {
        String select =
                "PREFIX ex: <http://example.com/ontology#> " +
                        "SELECT ?s WHERE { " +
                        "?s a ex:Yadda " +
                        "}";

        assertNull(contextUnawareGraph.selectFirst(select));
    }

    @Test
    public void ensureThatSelectWorksWithMultipleResults() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> rdf:type ex:Yadda . " +
                        "<http://example.com/2> rdf:type ex:Yadda . ";

        contextUnawareGraph.addTriplesFromTurtle(turtle);

        String select =
                "PREFIX ex: <http://example.com/ontology#> " +
                        "SELECT ?s WHERE { " +
                        "?s a ex:Yadda " +
                        "} ORDER BY ?s";

        List<BoundVariables> boundVariables = contextUnawareGraph.select(select);
        assertEquals(2, boundVariables.size());
        assertEquals("http://example.com/1", boundVariables.get(0).getURIReference("s").getURIText());
        assertEquals("http://example.com/2", boundVariables.get(1).getURIReference("s").getURIText());
    }

    @Test
    public void ensureTheSelectReturnsEmptyListWhenNoResults() {
        String select =
                "PREFIX ex: <http://example.com/ontology#> " +
                        "SELECT ?s WHERE " +
                        "{ " +
                        "?s a ex:Yadda " +
                        "}";

        assertEquals(0, contextUnawareGraph.select(select).size());
    }

    @Test
    public void ensureThatSelectWithContextDoesNotGetTripelsFromDefaultGraph() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +

                        "<http://example.com/1> ex:foo \"Yadda\"^^xsd:string . ";

        contextUnawareGraph.addTriplesFromTurtle(turtle);

        String sparqlWithExplicitContext =
                "PREFIX ex: <http://example.com/ontology#> " +

                        "SELECT ?foo WHERE { " +
                        "GRAPH <" + CONTEXT + "> { " +
                        "[] ex:foo ?foo " +
                        "}" +
                        "}";

        assertEquals(0, contextUnawareGraph.select(sparqlWithExplicitContext).size());

        String sparqlWithNoContext =
                "PREFIX ex: <http://example.com/ontology#> " +

                        "SELECT ?foo WHERE { " +
                        "[] ex:foo ?foo " +
                        "}";

        assertEquals(0, contextAwareGraph.select(sparqlWithNoContext).size());
    }

    @Test
    public void ensureThatSelectWithContextDoesNotGetTripelsFromDifferentGraph() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +

                        "<http://example.com/1> ex:foo \"Yadda\"^^xsd:string . ";

        Graph otherContextAwareGraph = createSesameGraph("http://otherGraph/");
        otherContextAwareGraph.addTriplesFromTurtle(turtle);

        String sparqlWithNoContext =
                "PREFIX ex: <http://example.com/ontology#> " +

                        "SELECT ?foo WHERE { " +
                        "[] ex:foo ?foo " +
                        "}";

        // NOTE: The triples were added to a DIFFERENT context than contextAwareGraph uses
        assertEquals(0, contextAwareGraph.select(sparqlWithNoContext).size());
    }

    @Test
    public void ensureThatAskWithContextDoesNotGetTriplesFromDefaultGraph() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +

                        "<http://example.com/1> ex:foo \"Yadda\"^^xsd:string . ";

        contextUnawareGraph.addTriplesFromTurtle(turtle);

        String sparqlWithExplicitContext =
                "PREFIX ex: <http://example.com/ontology#> " +

                        "ASK WHERE { " +
                        "GRAPH <" + CONTEXT + "> { " +
                        "[] ex:foo _:foo " +
                        "}" +
                        "}";

        assertFalse(contextUnawareGraph.ask(sparqlWithExplicitContext));

        String sparqlWithNoContext =
                "PREFIX ex: <http://example.com/ontology#> " +

                        "ASK WHERE { " +
                        "[] ex:foo _:foo " +
                        "}";

        assertFalse(contextAwareGraph.ask(sparqlWithNoContext));
    }

    @Test
    public void checkSize() {
        assertEquals(0, contextUnawareGraph.size());

        String turtle1 =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +

                        "<http://example.com/1> ex:foo \"Yadda\"^^xsd:string . ";

        contextUnawareGraph.addTriplesFromTurtle(turtle1);
        assertEquals(0, contextAwareGraph.size());
        assertEquals(1, contextUnawareGraph.size());

        String turtle2 =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> rdf:type ex:Yadda . " +
                        "<http://example.com/2> rdf:type ex:Yadda . ";

        contextAwareGraph.addTriplesFromTurtle(turtle2);

        assertEquals(2, contextAwareGraph.size());
        assertEquals(3, contextUnawareGraph.size());
    }

    @Test
    public void checkWeCanAddANamespaceAndGetItBackByPrefix() {
        contextUnawareGraph.addNamespace(new Namespace("ex", "http://www.example.com/ontology#"));
        Namespace namespaceWeGetOutOfGraph = contextUnawareGraph.getNamespaceByPrefix("ex");
        assertEquals("http://www.example.com/ontology#", namespaceWeGetOutOfGraph.getURIText());
    }

    @Test
    public void checkWeCanClearAGraph() {
        String turtle1 =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +

                        "<http://example.com/1> ex:foo \"Yadda\"^^xsd:string . ";

        contextAwareGraph.addTriplesFromTurtle(turtle1);
        assertEquals(1, contextAwareGraph.size());
        assertEquals(1, contextUnawareGraph.size());

        contextAwareGraph.clearAllTriples();
        assertEquals(0, contextAwareGraph.size());
        assertEquals(0, contextUnawareGraph.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkAddingABNodeWithNullTypeExplodes() {
        contextUnawareGraph.createFakeBlankNode(null);
    }

    @Test
    public void checkWeCanAddURIReferenceToGraphWtihContext() {
        String turtle1 =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +

                        "<http://example.com/1> ex:foo \"Yadda\"^^xsd:string . ";

        contextAwareGraph.addTriplesFromTurtle(turtle1);

        String select =
                "PREFIX ex: <http://example.com/ontology#> " +
                        "SELECT ?s " +
                        "WHERE { " +
                        "  ?s ex:foo _:o " +
                        "}";

        URIReference uriReference = contextAwareGraph.createURIReference(new RDFType("http://example.com/ontology#Boo"), "http://example.com/Foo");

        URIReference foo = contextAwareGraph.selectFirst(select).getURIReference("s");
        contextAwareGraph.addStatement(foo, new RDFProperty("http://example.com/ontology#ugh"), uriReference);

        String ask =
                "PREFIX ex: <http://example.com/ontology#> " +
                        "ASK WHERE { " +
                        "  ?s ex:ugh <http://example.com/Foo>" +
                        "}";

        assertAskIsTrue(contextAwareGraph, ask);
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkAddingURIREferenceWithNullTypeExplodes() {
        contextUnawareGraph.createURIReference(null, "http://example.com/hi");
    }

    @Test(expected = IllegalArgumentException.class)
    public void checkAddingURIREferenceWithNullURIExplodes() {
        contextUnawareGraph.createURIReference(new RDFType("http://example.com/ontology#Boo"), null);
    }

    @Test
    public void checkAddTriplesFromGraph() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> rdf:type ex:Yadda . ";

        Graph sourceGraph = createSesameGraph("http://a-context/");
        sourceGraph.addTriplesFromTurtle(turtle);

        contextAwareGraph.addTriplesFromGraph(sourceGraph);

        assertTrue(contextAwareGraph.ask("PREFIX ex: <http://example.com/ontology#> ASK WHERE { <http://example.com/1> a ex:Yadda }"));
        assertEquals(1, contextAwareGraph.size());
    }

    @Test
    public void checkAddTriplesFromGraphWithContext() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> rdf:type ex:Yadda . ";

        Graph sourceGraph = graphFactory.createTempGraph();
        sourceGraph.addTriplesFromTurtle(turtle);

        contextAwareGraph.addTriplesFromGraph(sourceGraph);
        String sparql = "PREFIX ex: <http://example.com/ontology#> " +
                "ASK WHERE { " +
                "  <http://example.com/1> a ex:Yadda " +
                "}";

        assertTrue(contextAwareGraph.ask(
                sparql));

        assertTrue(contextUnawareGraph.ask(
                sparql));

        assertEquals(1, contextAwareGraph.size());
        assertEquals(1, contextUnawareGraph.size());
    }

    @Test
    public void checkCanRemoveStatementWithURIReferenceObject() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> <http://example.com/predicate> <http://example.com/2> . " +
                        "<http://example.com/3> <http://example.com/predicate> <http://example.com/4> . ";


        contextUnawareGraph.addTriplesFromTurtle(turtle);

        contextUnawareGraph.remove(contextUnawareGraph.getURIReference("http://example.com/1"), new RDFProperty("http://example.com/predicate"),
                contextUnawareGraph.getURIReference("http://example.com/2"));

        assertEquals(1, contextUnawareGraph.size());

        String ask = "" +
                "ASK WHERE {" +
                "<http://example.com/3> <http://example.com/predicate> <http://example.com/4>" +
                "}";

        assertAskIsTrue(contextUnawareGraph, (ask));
    }

    @Test
    public void checkCanRemoveStatementWithURIReferenceObjectWithContext() {
        String turtle =
                "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> <http://example.com/predicate> <http://example.com/2> . " +
                        "<http://example.com/3> <http://example.com/predicate> <http://example.com/4> . ";


        contextAwareGraph.addTriplesFromTurtle(turtle);

        contextAwareGraph.remove(contextAwareGraph.getURIReference("http://example.com/1"), new RDFProperty("http://example.com/predicate"), contextAwareGraph.getURIReference("http://example.com/2"));

        assertEquals(1, contextAwareGraph.size());

        String ask = "" +
                "ASK WHERE { " +
                "  <http://example.com/3> <http://example.com/predicate> <http://example.com/4>" +
                "}";

        assertAskIsTrue(contextAwareGraph, ask);
    }

    @Test
    public void checkCanRemoveStatementWithIntegerObject() {
        String turtle =
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +
                        "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> <http://example.com/predicate> <http://example.com/2> . " +
                        "<http://example.com/3> <http://example.com/predicate> \"3\"^^xsd:integer . ";

        contextUnawareGraph.addTriplesFromTurtle(turtle);

        contextUnawareGraph.remove(contextUnawareGraph.getURIReference("http://example.com/3"), new RDFProperty("http://example.com/predicate"), 3);

        assertEquals(1, contextUnawareGraph.size());

        String ask = "" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                "ASK WHERE {" +
                "<http://example.com/1> <http://example.com/predicate> <http://example.com/2>" +
                "}";

        assertAskIsTrue(contextUnawareGraph, (ask));
    }

    @Test
    public void checkCanRemoveStatementWithIntegerObjectWithContext() {
        String turtle =
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +
                        "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> <http://example.com/predicate> <http://example.com/2> . " +
                        "<http://example.com/3> <http://example.com/predicate> \"3\"^^xsd:integer . ";

        contextAwareGraph.addTriplesFromTurtle(turtle);

        contextAwareGraph.remove(contextAwareGraph.getURIReference("http://example.com/3"), new RDFProperty("http://example.com/predicate"), 3);

        assertEquals(1, contextAwareGraph.size());

        String ask = "" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                "ASK WHERE { " +
                "  <http://example.com/1> <http://example.com/predicate> <http://example.com/2>" +
                "}";

        assertAskIsTrue(contextAwareGraph, (ask));
    }

    @Test
    public void checkCanRemoveStatementWithStringObject() {
        String turtle =
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +
                        "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> <http://example.com/predicate> <http://example.com/2> . " +
                        "<http://example.com/3> <http://example.com/predicate> \"boo\"^^xsd:string . ";

        contextUnawareGraph.addTriplesFromTurtle(turtle);

        contextUnawareGraph.remove(contextUnawareGraph.getURIReference("http://example.com/3"), new RDFProperty("http://example.com/predicate"), "boo");

        assertEquals(1, contextUnawareGraph.size());

        String ask = "" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                "ASK WHERE {" +
                "<http://example.com/1> <http://example.com/predicate> <http://example.com/2>" +
                "}";

        assertAskIsTrue(contextUnawareGraph, (ask));
    }

    @Test
    public void checkCanRemoveStatementWithStringObjectWithContext() {
        String turtle =
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +
                        "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> <http://example.com/predicate> <http://example.com/2> . " +
                        "<http://example.com/3> <http://example.com/predicate> \"boo\"^^xsd:string . ";

        contextAwareGraph.addTriplesFromTurtle(turtle);

        contextAwareGraph.remove(contextAwareGraph.getURIReference("http://example.com/3"), new RDFProperty("http://example.com/predicate"), "boo");

        assertEquals(1, contextAwareGraph.size());

        String ask = "" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                "ASK WHERE { " +
                "  <http://example.com/1> <http://example.com/predicate> <http://example.com/2>" +
                "}";

        assertAskIsTrue(contextAwareGraph, (ask));
    }

    @Test
    public void checkCanRemoveStatementWithItsType() {
        String turtle =
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +
                        "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> <http://example.com/predicate> <http://example.com/2> . " +
                        "<http://example.com/3> a <http://example.com/type> . ";

        contextUnawareGraph.addTriplesFromTurtle(turtle);

        contextUnawareGraph.removeTypeOn(contextUnawareGraph.getURIReference("http://example.com/3"), new RDFType("http://example.com/type"));

        assertEquals(1, contextUnawareGraph.size());

        String ask = "" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                "ASK WHERE {" +
                "<http://example.com/1> <http://example.com/predicate> <http://example.com/2>" +
                "}";

        assertAskIsTrue(contextUnawareGraph, (ask));
    }

    @Test
    public void checkCanRemoveStatementWithItsTypeWithContext() {
        String turtle =
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +
                        "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> <http://example.com/predicate> <http://example.com/2> . " +
                        "<http://example.com/3> a <http://example.com/type> . ";

        contextAwareGraph.addTriplesFromTurtle(turtle);

        contextAwareGraph.removeTypeOn(contextAwareGraph.getURIReference("http://example.com/3"), new RDFType("http://example.com/type"));

        assertEquals(1, contextAwareGraph.size());

        String ask = "" +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                "ASK WHERE { " +
                "  <http://example.com/1> <http://example.com/predicate> <http://example.com/2>" +
                "}";

        assertAskIsTrue(contextAwareGraph, ask);
    }

    @Test
    public void checkCanCheckForExistenceOfResourceByURI() {
        String turtle =
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +
                        "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> <http://example.com/predicate> <http://example.com/2> . " +
                        "<http://example.com/3> <http://example.com/predicate> \"boo\"^^xsd:string . ";

        contextUnawareGraph.addTriplesFromTurtle(turtle);
        assertTrue(contextUnawareGraph.containsResourceWithURI("http://example.com/3"));
    }

    @Test
    public void checkCanCheckForExistenceOfResourceByURIWithContext() {
        String turtle =
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +
                        "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> <http://example.com/predicate> <http://example.com/2> . " +
                        "<http://example.com/3> <http://example.com/predicate> \"boo\"^^xsd:string . ";

        contextAwareGraph.addTriplesFromTurtle(turtle);
        assertTrue(contextAwareGraph.containsResourceWithURI("http://example.com/3"));
    }

    @Test(expected = UnsupportedSPARQLStatementException.class)
    public void sparqlShouldNotHaveGraphStatementWhenAskAgainstContextAwareGraph() {
        String turtle =
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +
                        "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> <http://example.com/predicate> <http://example.com/2> . " +
                        "<http://example.com/3> a <http://example.com/type> . ";

        contextUnawareGraph.addTriplesFromTurtle(turtle);

        String sparql =
                "ASK WHERE { " +
                        " GRAPH <http://foo.com/context> {" +
                        "  <http://example.com/1> <http://example.com/predicate> <http://example.com/2>" +
                        " }" +
                        "}";
        contextAwareGraph.ask(sparql);
    }

    @Test(expected = UnsupportedSPARQLStatementException.class)
    public void sparqlShouldNotHaveGraphStatementWhenSelectAgainstContextAwareGraph() {
        String turtle =
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +
                        "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> <http://example.com/predicate> <http://example.com/2> . " +
                        "<http://example.com/3> a <http://example.com/type> . ";

        contextUnawareGraph.addTriplesFromTurtle(turtle);

        String sparql =
                "SELECT ?foo WHERE { " +
                        " GRAPH <http://foo.com/context> {" +
                        "  ?foo <http://example.com/predicate> <http://example.com/2>" +
                        " }" +
                        "}";
        contextAwareGraph.select(sparql);
    }

    @Test(expected = UnsupportedSPARQLStatementException.class)
    public void sparqlShouldNotHaveGraphStatementWhenSelectFirstContextAwareGraph() {
        String turtle =
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +
                        "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> <http://example.com/predicate> <http://example.com/2> . " +
                        "<http://example.com/3> a <http://example.com/type> . ";

        contextUnawareGraph.addTriplesFromTurtle(turtle);

        String sparql =
                "SELECT ?foo WHERE { " +
                        " GRAPH <http://foo.com/context> {" +
                        "  ?foo <http://example.com/predicate> <http://example.com/2>" +
                        " }" +
                        "}";
        contextAwareGraph.selectFirst(sparql);
    }

    @Test(expected = ShineRuntimeException.class)
    public void checkWhenAddNamespaceExplodesItThrowsAShineRuntimeException() throws Exception {
        RepositoryConnection badConnection = mock(RepositoryConnection.class);
        doThrow(new RepositoryException("")).when(badConnection).setNamespace("foo", "http://example.org/");

        SesameGraph badGraph = new SesameGraph(badConnection, null);
        badGraph.addNamespace(new Namespace("foo", "http://example.org/"));
    }

    @Test(expected = ShineRuntimeException.class)
    public void checkGetNamespaceByPrefixExplodesItThrowsAShineRuntimeException() throws RepositoryException {
        RepositoryConnection badConnection = mock(RepositoryConnection.class);
        when(badConnection.getNamespace("ex")).thenThrow(new RepositoryException(""));

        SesameGraph badGraph = new SesameGraph(badConnection, null);
        badGraph.getNamespaceByPrefix("ex");
    }

    @Test(expected = ShineRuntimeException.class)
    public void checkWhenAddStatementWithResourceObjectExplodesItThrowsAShineRuntimeException() throws RepositoryException {
        RDFProperty property = new RDFProperty("http://www.example.com/ontology#foo");
        RepositoryConnection badConnection = mock(RepositoryConnection.class);
        doThrow(new RepositoryException("")).when(badConnection).add((org.openrdf.model.Resource) any(), (org.openrdf.model.URI) any(), (org.openrdf.model.Value) any());

        org.openrdf.model.ValueFactory stubValueFactory = mock(org.openrdf.model.ValueFactory.class);
        when(badConnection.getValueFactory()).thenReturn(stubValueFactory);

        SesameURIReference stubSubject = mock(SesameURIReference.class);
        when(stubSubject.getSesameNativeResource()).thenReturn(null);

        URIReference stubPredicate = mock(URIReference.class);
        when(stubPredicate.getURIText()).thenReturn("");

        SesameGraph badGraph = new SesameGraph(badConnection, null);
        badGraph.addStatement(stubSubject, property, (Integer) null);
    }

    @Test(expected = ShineRuntimeException.class)
    public void checkWhenAddStatementWithStringObjectExplodesItThrowsAShineRuntimeException() throws RepositoryException {
        RDFProperty property = new RDFProperty("http://www.example.com/ontology#foo");
        RepositoryConnection badConnection = mock(RepositoryConnection.class);
        doThrow(new RepositoryException("")).when(badConnection).add((org.openrdf.model.Resource) any(), (org.openrdf.model.URI) any(), (org.openrdf.model.Literal) any());

        org.openrdf.model.ValueFactory stubValueFactory = mock(org.openrdf.model.ValueFactory.class);
        when(badConnection.getValueFactory()).thenReturn(stubValueFactory);

        SesameURIReference stubSubject = mock(SesameURIReference.class);
        when(stubSubject.getSesameNativeResource()).thenReturn(null);

        URIReference stubPredicate = mock(URIReference.class);
        when(stubPredicate.getURIText()).thenReturn("");

        SesameGraph badGraph = new SesameGraph(badConnection, null);
        badGraph.addStatement(stubSubject, property, "foo");
    }

    @Test(expected = ShineRuntimeException.class)
    public void checkWhenAddStatementWithIntObjectExplodesItThrowsAShineRuntimeException() throws RepositoryException {
        RDFProperty property = new RDFProperty("http://www.example.com/ontology#foo");
        RepositoryConnection badConnection = mock(RepositoryConnection.class);

        doThrow(new RepositoryException("")).when(badConnection).add((org.openrdf.model.Resource) any(), (org.openrdf.model.URI) any(), (org.openrdf.model.Literal) any());

        org.openrdf.model.ValueFactory stubValueFactory = mock(org.openrdf.model.ValueFactory.class);
        when(badConnection.getValueFactory()).thenReturn(stubValueFactory);

        SesameURIReference stubSubject = mock(SesameURIReference.class);
        when(stubSubject.getSesameNativeResource()).thenReturn(null);

        URIReference stubPredicate = mock(URIReference.class);
        when(stubPredicate.getURIText()).thenReturn("");

        SesameGraph badGraph = new SesameGraph(badConnection, null);
        badGraph.addStatement(stubSubject, property, 3);
    }

    @Test(expected = ShineRuntimeException.class)
    public void checkWhenAddStatementWithBooleanObjectExplodesItThrowsAShineRuntimeException() throws RepositoryException {
        RDFProperty property = new RDFProperty("http://www.example.com/ontology#foo");
        RepositoryConnection badConnection = mock(RepositoryConnection.class);
        doThrow(new RepositoryException("")).when(badConnection).add((org.openrdf.model.Resource) any(), (org.openrdf.model.URI) any(), (org.openrdf.model.Literal) any());

        org.openrdf.model.ValueFactory stubValueFactory = mock(org.openrdf.model.ValueFactory.class);
        when(badConnection.getValueFactory()).thenReturn(stubValueFactory);

        SesameURIReference stubSubject = mock(SesameURIReference.class);
        when(stubSubject.getSesameNativeResource()).thenReturn(null);

        URIReference stubPredicate = mock(URIReference.class);
        when(stubPredicate.getURIText()).thenReturn("");

        SesameGraph badGraph = new SesameGraph(badConnection, null);
        badGraph.addStatement(stubSubject, property, false);
    }

    @Test(expected = ShineRuntimeException.class)
    public void checkWhenAddTriplesFromGraphExplodesItThrowsShineRuntimeException() throws RepositoryException {
        RepositoryConnection badConnection = mock(RepositoryConnection.class);
        doThrow(new RepositoryException("")).when(badConnection).getStatements((org.openrdf.model.Resource) any(),
                (org.openrdf.model.URI) any(), (org.openrdf.model.Value) any(), anyBoolean());

        SesameGraph otherGraph = new SesameGraph(badConnection, null);
        SesameGraph badGraph = new SesameGraph(badConnection, null);

        badGraph.addTriplesFromGraph(otherGraph);
    }

    @Test(expected = UnsupportedSPARQLStatementException.class)
    public void canValidateUnsupportedSPARQLStatement() throws Exception {
        String turtle =
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +
                        "@prefix ex: <http://example.com/ontology#> . " +
                        "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +

                        "<http://example.com/1> <http://example.com/predicate> <http://example.com/2> . " +
                        "<http://example.com/3> a <http://example.com/type> . ";

        contextUnawareGraph.addTriplesFromTurtle(turtle);

        String sparql =
                "ASK WHERE { " +
                        " GRAPH <http://foo.com/context> {" +
                        "  <http://example.com/1> <http://example.com/predicate> <http://example.com/2>" +
                        " }" +
                        "}";
        contextAwareGraph.validate(sparql);
    }

    @Test(expected = MalformedSPARQLException.class)
    public void canValidateMalformedSPARQLStatement() throws Exception {
        String sparql = "I am a teapot!";
        contextAwareGraph.validate(sparql);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotAllowCreatingNewTempGraphOnClosedGraph() throws Exception {
        Graph sesameGraph = createSesameGraph(null);
        sesameGraph.close();
        sesameGraph.createTempGraph();
    }

}
