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

package com.thoughtworks.studios.shine.xunit;

import com.thoughtworks.studios.shine.cruise.GoOntology;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.grddl.XSLTTransformerRegistry;
import com.thoughtworks.studios.shine.semweb.sesame.InMemoryTempGraphFactory;
import org.apache.commons.io.FileUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.StringReader;

import static com.thoughtworks.studios.shine.AssertUtils.assertAskIsTrue;
import static java.nio.charset.StandardCharsets.UTF_8;

public class AntJUnitReportRDFizerForRailsTestsReportTest {
    private XSLTTransformerRegistry XSLTTransformerRegistry;

    @Before
    public void setup() {
        XSLTTransformerRegistry = new XSLTTransformerRegistry();
    }

    @Test
    public void canReadRailsTestWithFailureOutput() throws Exception {

        AntJUnitReportRDFizer junitRDFizer = new AntJUnitReportRDFizer(new InMemoryTempGraphFactory(), XSLTTransformerRegistry);
        Graph graph = junitRDFizer.importFile("http://job", document(FileUtils.readFileToString(new File("src/test-shared/resources/ruby_xunit/failing/TEST-PostsControllerTest.xml"), UTF_8)));

        String ask =
                "prefix cruise: <" + GoOntology.URI + "> " +
                        "PREFIX xunit: <" + XUnitOntology.URI + "> " +
                        "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " +

                        "ASK WHERE { " +
                        "<http://job> xunit:hasTestCase _:testCase . " +
                        "_:testCase a xunit:TestCase . " +
                        "_:testCase xunit:testSuiteName 'PostsControllerTest'^^xsd:string . " +
                        "_:testCase xunit:testCaseName 'test_should_get_index'^^xsd:string . " +
                        "_:testCase xunit:hasFailure _:failure . " +
                        "_:failure xunit:isError 'false'^^xsd:boolean " +
                        "}";

        assertAskIsTrue(graph, ask);
    }

    @Test
    public void canReadRailsTestWithErrorOutput() throws Exception {

        AntJUnitReportRDFizer junitRDFizer = new AntJUnitReportRDFizer(new InMemoryTempGraphFactory(), XSLTTransformerRegistry);
        Graph graph = junitRDFizer.importFile("http://job", document(FileUtils.readFileToString(new File("src/test-shared/resources/ruby_xunit/errors/TEST-PostsControllerTest.xml"), UTF_8)));

        String ask =
                "prefix cruise: <" + GoOntology.URI + "> " +
                        "PREFIX xunit: <" + XUnitOntology.URI + "> " +
                        "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " +

                        "ASK WHERE { " +
                        "<http://job> xunit:hasTestCase _:testCase . " +
                        "_:testCase a xunit:TestCase . " +
                        "_:testCase xunit:testSuiteName 'PostsControllerTest'^^xsd:string . " +
                        "_:testCase xunit:testCaseName 'test_should_get_index'^^xsd:string . " +
                        "_:testCase xunit:hasFailure _:failure . " +
                        "_:failure xunit:isError 'true'^^xsd:boolean " +
                        "}";

        assertAskIsTrue(graph, ask);
    }

    @Test
    public void canReadRailsTestWithPassingOutput() throws Exception {

        AntJUnitReportRDFizer junitRDFizer = new AntJUnitReportRDFizer(new InMemoryTempGraphFactory(), XSLTTransformerRegistry);
        Graph graph = junitRDFizer.importFile("http://job", document(FileUtils.readFileToString(new File("src/test-shared/resources/ruby_xunit/passing/TEST-PurchasesControllerTest.xml"), UTF_8)));

        String ask =
                "prefix cruise: <" + GoOntology.URI + "> " +
                        "PREFIX xunit: <" + XUnitOntology.URI + "> " +
                        "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " +

                        "ASK WHERE { " +
                        "<http://job> xunit:hasTestCase _:testCase . " +
                        "_:testCase a xunit:TestCase . " +
                        "_:testCase xunit:testSuiteName 'PurchasesControllerTest'^^xsd:string . " +
                        "_:testCase xunit:testCaseName 'test_should_get_index'^^xsd:string . " +
                        "}";

        assertAskIsTrue(graph, ask);
    }


    private Document document(String xml) throws DocumentException {
        return new SAXReader().read(new StringReader(xml));
    }
}
