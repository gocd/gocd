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

package com.thoughtworks.studios.shine.xunit;

import java.io.StringReader;

import static com.thoughtworks.studios.shine.AssertUtils.assertAskIsTrue;
import com.thoughtworks.studios.shine.cruise.GoOntology;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.grddl.XSLTTransformerRegistry;
import com.thoughtworks.studios.shine.semweb.sesame.InMemoryTempGraphFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

public class AntJUnitReportRDFizerTest {
    private XSLTTransformerRegistry XSLTTransformerRegistry;

    @Before
    public void setup() {
        XSLTTransformerRegistry = new XSLTTransformerRegistry();
    }

    @Test
    public void testLoadingFailingJUnit() throws Exception {
        String failingTestXML = "<?xml version='1.0' encoding='UTF-8' ?>" +
                "<testsuite errors='1' failures='1' hostname='fake-host-name' name='com.example.TheTest' tests='1' time='1.058' timestamp='2009-12-25T00:34:36'>" +
                "<testcase classname='com.example.TheTest' name='testSomething' time='0.011'>" +
                "<failure message='foo' type='junit.framework.AssertionFailedError'>junit.framework.AssertionFailedError: foo" +
                "\tat com.example.TheTest.testSomething(TheTest.java:96)" +
                "</failure>" +
                "</testcase>" +
                "</testsuite>";

        AntJUnitReportRDFizer junitRDFizer = new AntJUnitReportRDFizer(new InMemoryTempGraphFactory(), XSLTTransformerRegistry);

        Graph graph = junitRDFizer.importFile("http://job", document(failingTestXML));

        String ask =
                "prefix cruise: <" + GoOntology.URI + "> " +
                        "PREFIX xunit: <" + XUnitOntology.URI + "> " +
                        "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " +

                        "ASK WHERE { " +
                        "<http://job> xunit:hasTestCase _:testCase . " +
                        "_:testCase a xunit:TestCase . " +
                        "_:testCase xunit:testSuiteName 'com.example.TheTest'^^xsd:string . " +
                        "_:testCase xunit:testCaseName 'testSomething'^^xsd:string . " +
                        "_:testCase xunit:hasFailure _:failure . " +
                        "_:failure xunit:isError 'false'^^xsd:boolean " +
                        "}";

        assertAskIsTrue(graph, ask);
    }

    private Document document(String xml) throws DocumentException {
        return new SAXReader().read(new StringReader(xml));
    }

    @Test
    public void testLoadingPassingJUnit() throws Exception {
        String passingTestXML = "<?xml version='1.0' encoding='UTF-8' ?>" +
                "<testsuite name='com.example.TheTest'>" +
                "<testcase classname='com.example.TheTest' name='testSomething' />" +
                "</testsuite>";

        AntJUnitReportRDFizer junitRDFizer = new AntJUnitReportRDFizer(new InMemoryTempGraphFactory(), XSLTTransformerRegistry);

        Graph graph = junitRDFizer.importFile("http://job", document(passingTestXML));

        String ask =
                "prefix cruise: <" + GoOntology.URI + "> " +
                        "prefix xunit: <" + XUnitOntology.URI + "> " +
                        "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " +

                        "ASK WHERE { " +
                        "<http://job> xunit:hasTestCase _:testCase . " +
                        "_:testCase a xunit:TestCase . " +
                        "_:testCase xunit:testSuiteName 'com.example.TheTest'^^xsd:string . " +
                        "_:testCase xunit:testCaseName 'testSomething'^^xsd:string . " +
                        "OPTIONAL { ?testCase xunit:hasFailure ?failure } . " +
                        "FILTER (!bound(?failure)) " +
                        "}";

        assertAskIsTrue(graph, ask);
    }

    @Test
    public void testDoesNotHandleNonJUnitXMLFile() throws DocumentException {
        String invalidXML = "<?xml version='1.0' encoding='UTF-8' ?><foo/>";

        AntJUnitReportRDFizer junitRDFizer = new AntJUnitReportRDFizer(new InMemoryTempGraphFactory(), XSLTTransformerRegistry);

        assertFalse(junitRDFizer.canHandle(document(invalidXML)));
    }

    @Test
    public void testDoesHandleJUnitXMLFile() throws DocumentException {
        String invalidXML = "<?xml version='1.0' encoding='UTF-8' ?><testsuite/>";

        AntJUnitReportRDFizer junitRDFizer = new AntJUnitReportRDFizer(new InMemoryTempGraphFactory(), XSLTTransformerRegistry);

        assertTrue(junitRDFizer.canHandle(document(invalidXML)));
    }
}
