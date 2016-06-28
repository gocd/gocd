/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.studios.shine.xunit;

import com.thoughtworks.studios.shine.semweb.*;
import com.thoughtworks.studios.shine.semweb.grddl.GRDDLTransformer;
import com.thoughtworks.studios.shine.semweb.grddl.GrddlTransformException;
import com.thoughtworks.studios.shine.semweb.grddl.XSLTTransformerRegistry;
import org.dom4j.Document;

import java.util.ArrayList;
import java.util.List;

public class AntJUnitReportRDFizer implements XMLRDFizer {
    private TempGraphFactory graphFactory;

    private static final String SELECT_TEST_CASES_SPARQL = "" +
            XUnitOntology.URI_PREFIX +
            "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " +
            "" +
            "SELECT DISTINCT ?testCase WHERE { " +
            "?testCase a xunit:TestCase " +
            "}";
    private GRDDLTransformer grddlTransformer;

    public AntJUnitReportRDFizer(TempGraphFactory graphFactory, XSLTTransformerRegistry xsltTransformerRegistry) {
        this.graphFactory = graphFactory;
        grddlTransformer = new GRDDLTransformer(xsltTransformerRegistry, XSLTTransformerRegistry.XUNIT_ANT_JUNIT_GRDDL_XSL);
    }

    public boolean canHandle(Document doc) {
        String root = doc.getRootElement().getName();
        return "testsuite".equals(root) || "testsuites".equals(root);
    }

    public Graph importFile(String parentURI, Document doc) throws GrddlTransformException {
        Graph graph = graphFactory.createTempGraph();
        importXUnit(graph, parentURI, doc);
        return graph;
    }

    private void importXUnit(Graph graph, String parentURI, Document doc) throws GrddlTransformException {
        Graph xunitGraph = grddlTransformer.transform(doc, graphFactory);
        List<URIReference> testCaseResources = testCases(xunitGraph);

        URIReference jobResource = graph.getURIReference(parentURI);
        for (Resource testCaseResource : testCaseResources) {
            graph.addStatement(jobResource, XUnitOntology.HAS_TEST_CASE, testCaseResource);
        }

        graph.addTriplesFromGraph(xunitGraph);
    }

    private List<URIReference> testCases(Graph xunitGraph) {
        List<URIReference> testCases = new ArrayList<>();

        for (BoundVariables boundVariables : xunitGraph.select(SELECT_TEST_CASES_SPARQL)) {
            testCases.add(boundVariables.getURIReference("testCase"));
        }

        return testCases;
    }
}
