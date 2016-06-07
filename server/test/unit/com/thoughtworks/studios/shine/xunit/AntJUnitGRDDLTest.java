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

import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.grddl.GRDDLTransformer;
import com.thoughtworks.studios.shine.semweb.grddl.XSLTTransformerRegistry;
import com.thoughtworks.studios.shine.semweb.sesame.InMemoryTempGraphFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static com.thoughtworks.studios.shine.AssertUtils.assertAskIsFalse;
import static com.thoughtworks.studios.shine.AssertUtils.assertAskIsTrue;

public class AntJUnitGRDDLTest {
    String minimalXUnitXML =
            "<?xml version='1.0' encoding='UTF-8' ?>" +
                    "<testsuite>" +
                    "<testcase name='anError'>" +
                    "<error message='A message for an error' />" +
                    "</testcase>" +
                    "<testcase name='oneThatWorks'/>" +
                    "<testcase name='aFailure'>" +
                    "<failure message='A message for a failure.'><![CDATA[Aren't you glad you are not a failure?]]></failure>" +
                    "</testcase>" +
                    "</testsuite>";

    Graph minimalDataGraph;
    private GRDDLTransformer transformer;

    @Before
    public void setUp() throws Exception {
        InputStream xunitXMLStream = new ByteArrayInputStream(minimalXUnitXML.getBytes());
        XSLTTransformerRegistry xsltTransformerRegistry = new XSLTTransformerRegistry();

        transformer = new GRDDLTransformer(xsltTransformerRegistry, XSLTTransformerRegistry.XUNIT_ANT_JUNIT_GRDDL_XSL);
        minimalDataGraph = transformer.transform(xunitXMLStream, new InMemoryTempGraphFactory());
    }

    @Test
    public void happyPathConversionWithAllData() throws Exception {
        String kitchenSinkXUnitXML =
                "<?xml version='1.0' encoding='UTF-8' ?>" +
                        "<testsuite errors='0' failures='0' hostname='phydeaux3.corporate.thoughtworks.com' name='com.thoughtworks.studios.shine.cruise.GoOntologyTest' tests='2' time='0.601' timestamp='2009-12-18T20:26:42'>" +
                        "<properties>" +
                        "<property name='java.runtime.name' value='Java SE Runtime Environment' />" +
                        "<property name='sun.boot.library.path' value='/System/Library/Frameworks/JavaVM.framework/Versions/1.6.0/Libraries' />" +
                        "</properties>" +
                        "<testcase classname='com.thoughtworks.studios.shine.cruise.GoOntologyTest' name='canCreateGraph' time='0.57' />" +
                        "<system-out><![CDATA[]]></system-out>" +
                        "<system-err><![CDATA[]]></system-err>" +
                        "</testsuite>";

        InputStream xunitXMLStream = new ByteArrayInputStream(kitchenSinkXUnitXML.getBytes());

        Graph graph = transformer.transform(xunitXMLStream, new InMemoryTempGraphFactory());

        String sparql =
                "PREFIX xunit: <" + XUnitOntology.URI + "> " +
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +

                        "ASK WHERE { " +
                        "?testCase a xunit:TestCase ; " +
                        "xunit:testCaseName \"canCreateGraph\"^^xsd:string ; " +
                        "xunit:testCaseClassName \"com.thoughtworks.studios.shine.cruise.GoOntologyTest\"^^xsd:string ; " +
                        "xunit:testSuiteName \"com.thoughtworks.studios.shine.cruise.GoOntologyTest\"^^xsd:string . " +
                        "}";

        assertAskIsTrue(graph, sparql);
    }

    @Test
    public void makeSureHavingExtraXMLNodesInDataFileDoesntBreakEVERYTHING() throws Exception {
        String xmlWithExtraStuff =
                "<?xml version='1.0' encoding='UTF-8' ?>" +
                        "<testsuite>" +
                        "<testcase name='anError'>" +
                        "<error message='A message for an error' />" +
                        "</testcase>" +
                        "<testcase name='oneThatWorks'/>" +
                        "<testcase name='aFailure'>" +
                        "<failure message='A message for a failure.'><![CDATA[Aren't you glad you are not a failure?]]></failure>" +
                        "</testcase>" +
                        "<whatDoesNotBelong>This does not belong</whatDoesNotBelong>" +
                        "</testsuite>";

        InputStream xunitXMLStream = new ByteArrayInputStream(xmlWithExtraStuff.getBytes());

        transformer.transform(xunitXMLStream, new InMemoryTempGraphFactory());
    }

    @Test
    public void failureIsCorrectlyTransformed() throws Exception {
        String askIfFailureIsCorrectlyTransformed =
                "PREFIX xunit: <" + XUnitOntology.URI + "> " +
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +

                        "ASK WHERE { " +
                        "?testCase a xunit:TestCase ; " +
                        "xunit:testCaseName \"aFailure\"^^xsd:string ; " +
                        "xunit:hasFailure ?failure . " +
                        "?failure a xunit:Failure ; " +
                        "xunit:failureMessage \"A message for a failure.\"^^xsd:string ; " +
                        "xunit:failureStackTrace \"Aren't you glad you are not a failure?\"^^xsd:string ; " +
                        "xunit:isError \"false\"^^xsd:boolean " +
                        "}";

        assertAskIsTrue(minimalDataGraph, askIfFailureIsCorrectlyTransformed);
    }

    @Test
    public void passingIsCorrectlyTransformed() throws Exception {
        String askIfPassingIsCorrectlyTransformed =
                "PREFIX xunit: <" + XUnitOntology.URI + "> " +
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +

                        "ASK WHERE { " +
                        "?testCase a xunit:TestCase ; " +
                        "xunit:testCaseName \"oneThatWorks\"^^xsd:string ; " +
                        "}";

        assertAskIsTrue(minimalDataGraph, askIfPassingIsCorrectlyTransformed);
    }

    @Test
    public void passinghasAFailure() throws Exception {
        String askIfPassinghasAFailure =
                "PREFIX xunit: <" + XUnitOntology.URI + "> " +
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +

                        "ASK WHERE { " +
                        "?testCase a xunit:TestCase ; " +
                        "xunit:testCaseName \"oneThatWorks\"^^xsd:string ; " +
                        "xunit:hasFailure ?failure " +
                        "}";

        assertAskIsFalse(minimalDataGraph, askIfPassinghasAFailure);
    }

    @Test
    public void errorIsCorrectlyTransformed() throws Exception {
        String askIfErrorIsCorrectlyTransformed =
                "PREFIX xunit: <" + XUnitOntology.URI + "> " +
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +

                        "ASK WHERE { " +
                        "?testCase a xunit:TestCase ; " +
                        "xunit:testCaseName \"anError\"^^xsd:string ; " +
                        "xunit:hasFailure ?failure . " +
                        "?failure a xunit:Failure ; " +
                        "xunit:failureMessage \"A message for an error\"^^xsd:string ; " +
                        "xunit:isError \"true\"^^xsd:boolean " +
                        "}";

        assertAskIsTrue(minimalDataGraph, askIfErrorIsCorrectlyTransformed);
    }
}
