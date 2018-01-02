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

package com.thoughtworks.studios.shine.semweb.grddl;

import com.thoughtworks.studios.shine.cruise.GoOntology;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.sesame.InMemoryTempGraphFactory;
import org.junit.Test;

import javax.xml.transform.Templates;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static com.thoughtworks.studios.shine.AssertUtils.assertAskIsTrue;
import static org.junit.Assert.assertTrue;

public class GRDDLTransformerTest {
    @Test
    public void checkXMLBecomesRDF() throws Exception {
        String inputXML = "<foo>bar</foo>";
        String xsl = "" +
                "<?xml version='1.0' encoding='UTF-8'?>" +
                "<xsl:stylesheet xmlns:xsl='http://www.w3.org/1999/XSL/Transform' " +
                "xmlns:ex='http://www.example.com/ontology.owl#' " +
                "xmlns:rdf='http://www.w3.org/1999/02/22-rdf-syntax-ns#' version='1.0'>" +
                "<xsl:template match='/'>" +
                "<rdf:RDF>" +
                "<xsl:apply-templates />" +
                "</rdf:RDF>" +
                "</xsl:template>" +
                "<xsl:template match='foo'>" +
                "<ex:Foo>" +
                "<ex:hasBar rdf:datatype='http://www.w3.org/2001/XMLSchema#string'><xsl:value-of select='.'/></ex:hasBar>" +
                "</ex:Foo>" +
                "</xsl:template>" +
                "</xsl:stylesheet>";

        String ask =
                "prefix ex: <http://www.example.com/ontology.owl#> " +
                        "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " +

                        "ASK WHERE { " +
                        "?foo a ex:Foo ; " +
                        "ex:hasBar 'bar'^^xsd:string . " +
                        "}" +
                        "";

        InputStream xslAsStream = new ByteArrayInputStream(xsl.getBytes());
        InputStream xmlAsStream = new ByteArrayInputStream(inputXML.getBytes());

        GRDDLTransformer transformer = new GRDDLTransformer(transformerForXSLStream(xslAsStream, "foo.xml"), "foo.xml");
        Graph graph = transformer.transform(xmlAsStream, new InMemoryTempGraphFactory());

        assertAskIsTrue(graph, (ask));
    }

    @Test
    public void checkJobTransformationWon() throws Exception {
        InputStream xsl = getClass().getResourceAsStream("/cruise/job-grddl.xsl");
        InputStream jobXML = getClass().getResourceAsStream("/cruise/jobs/job-1622.xml");

        GRDDLTransformer transformer = new GRDDLTransformer(transformerForXSLStream(xsl, "foo.xsl"), "foo.xsl");
        Graph transformedGraph = transformer.transform(jobXML, new InMemoryTempGraphFactory());

        String askIfJobIsGood = "" +
                "PREFIX cruise: <" + GoOntology.URI + "> " +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                "ASK WHERE {" +
                "  <http://10.2.12.30:8153/go/api/jobs/1622.xml> a cruise:Job . " +
                "  <http://10.2.12.30:8153/go/api/jobs/1622.xml> cruise:hasArtifacts ?artifacts . " +
                "  FILTER(isIRI(?artifacts)) " +
                "  FILTER(regex(str(?artifacts), '^urn:uuid:.+')) " +
                "  ?artifacts cruise:artifactsBaseURL 'http://10.2.12.30:8153/go/files/sf04/62/build/1/firefox-1'^^xsd:string . " +
                "  ?artifacts cruise:pathFromArtifactRoot 'pipelines/sf04/62/build/1/firefox-1'^^xsd:string . " +
                "  ?artifacts cruise:hasArtifact ?artifact1 . " +
                "  FILTER(isIRI(?artifact1)) " +
                "  FILTER(regex(str(?artifact1), '^urn:uuid:.+')) " +
                "  ?artifact1 cruise:artifactPath 'cruise-output/log.xml'^^xsd:string . " +
                "  ?artifact1 cruise:artifactType 'file'^^xsd:string . " +
                "  ?artifacts cruise:hasArtifact ?artifact2 . " +
                "  FILTER(isIRI(?artifact2)) " +
                "  FILTER(regex(str(?artifact2), '^urn:uuid:.+')) " +
                "  ?artifact2 cruise:artifactPath 'reports'^^xsd:string . " +
                "  ?artifact2 cruise:artifactType 'unit'^^xsd:string . " +
                "}";

        assertTrue(transformedGraph.ask(askIfJobIsGood));
    }


    @Test
    public void checkPipelineTransformation() throws Exception {
        InputStream xsl = getClass().getResourceAsStream("/cruise/pipeline-graph-grddl.xsl");
        String rawPipelineXML =
                "<pipeline name='RPNCalculator' counter='1' label='foo-1'>" +
                        "    <link rel='self' href='http://localhost:8153/go/api/pipelines/RPNCalculator/1.xml'/>" +
                        "    <scheduleTime>2010-01-04T16:02:27-08:00</scheduleTime>" +
                        "    <materials>" +
                        "        <material>" +
                        "            <modifications>" +
                        "                    <changeset changesetUri='http://cruise03:8153/go/api/materials/13/changeset/5c186460acca8e07972e7cf54935814005c38833.xml'>" +
                        "                        <user><![CDATA[sjin <sjin@thoughtworks.com>]]></user>" +
                        "                        <checkinTime>2010-01-04T16:01:21-08:00</checkinTime>" +
                        "                        <revision>938ab08dc5bd43884a106fe253dbf072b4968b5b</revision>" +
                        "                        <message><![CDATA[added a failing test]]></message>" +
                        "                        <file name='test/calculator/RPNIntegerCalculatorTest.java' action='modified' />" +
                        "                    </changeset>" +
                        "            </modifications>" +
                        "        </material>" +
                        "    </materials>" +
                        "    <stages>" +
                        "        <stage href='http://localhost:8153/go/api/stages/6.xml'/>" +
                        "    </stages>" +
                        "    <approvedBy><![CDATA[bigbird]]></approvedBy>" +
                        "</pipeline>";

        InputStream pipelineXML = new ByteArrayInputStream(rawPipelineXML.getBytes());

        GRDDLTransformer transformer = new GRDDLTransformer(transformerForXSLStream(xsl, "foo.xsl"), "foo.xsl");
        Graph transformedGraph = transformer.transform(pipelineXML, new InMemoryTempGraphFactory());

        String askIfPipelineWasTransformedWell =
                "PREFIX cruise: <" + GoOntology.URI + "> " +
                        "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +

                        "ASK WHERE { " +
                        "?pipeline a cruise:Pipeline ; " +
                        "cruise:pipelineCounter \"1\"^^xsd:integer ; " +
                        "cruise:pipelineName \"RPNCalculator\"^^xsd:string ; " +
                        "}";

        assertAskIsTrue(transformedGraph, askIfPipelineWasTransformedWell);
    }

    private XSLTTransformerRegistry transformerForXSLStream(final InputStream xsl, final String key) throws TransformerConfigurationException {
        {
            return new XSLTTransformerRegistry() {
                {
                    TransformerFactory transformerFactory = TransformerFactory.newInstance();
                    Templates templates = transformerFactory.newTemplates(new StreamSource(xsl));
                    transformerMap.put(key, templates);
                }
            };

        }
    }
}
