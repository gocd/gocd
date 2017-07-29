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

package com.thoughtworks.studios.shine.cruise.stage.details;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.server.domain.xml.JobXmlViewModel;
import com.thoughtworks.go.server.service.XmlApiService;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.studios.shine.cruise.GoIntegrationException;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.grddl.XSLTTransformerRegistry;
import com.thoughtworks.studios.shine.semweb.sesame.InMemoryTempGraphFactory;
import com.thoughtworks.studios.shine.time.Clock;
import com.thoughtworks.studios.shine.xunit.XUnitOntology;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.studios.shine.AssertUtils.assertAskIsFalse;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItem;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobResourceImporterTest {
    private final SystemEnvironment systemEnvironment = new SystemEnvironment();
    private XSLTTransformerRegistry transformerRegistry;
    private String baseUri;
    private XmlApiService xmlApiService;

    @Before
    public void setup() {
        transformerRegistry = new XSLTTransformerRegistry();
        baseUri = "https://localhost:8154/go";
        xmlApiService = mock(XmlApiService.class);
    }

    @After
    public void resetClock() {
        Clock.reset();
    }

    @Test
    public void testLoadingFileThatDoesNotExist() throws GoIntegrationException, IOException, DocumentException {
        Clock.fakeNowUTC(2008, 1, 20, 0, 0, 1);

        when(xmlApiService.write(any(JobXmlViewModel.class), eq(baseUri))).thenReturn(docFor(jobWithArtifactsMissing));

        JobResourceImporter rdfImporter = new JobResourceImporter("test/data/cruise/artifacts", new InMemoryTempGraphFactory(), transformerRegistry, xmlApiService, systemEnvironment);

        Graph jobGraph = rdfImporter.importJob(JobInstanceMother.passed("foo"), "https://localhost:8154/go");

        String ask = "PREFIX xunit: <" + XUnitOntology.URI + "> ASK WHERE { [] a xunit:TestCase }";

        assertAskIsFalse(jobGraph, ask);
    }

    private Document docFor(String jobWithArtifactsMissing) throws DocumentException {
        return new SAXReader().read(new ByteArrayInputStream(jobWithArtifactsMissing.getBytes()));
    }

    @Test
    public void testArtifactsAreImported() throws Exception {
        Clock.fakeNowUTC(2008, 1, 20, 0, 0, 1);

        JobInstance failedJob = JobInstanceMother.failed("test");
        Document document = docFor(happyJobResourceXML);
        document.selectSingleNode("//link[@rel='self']/@href").setText(new JobXmlViewModel(failedJob).httpUrl(baseUri));

        when(xmlApiService.write(any(JobXmlViewModel.class), eq(baseUri))).thenReturn(document);

        JobResourceImporter rdfImporter = new JobResourceImporter("test/data/cruise/artifacts", new InMemoryTempGraphFactory(), transformerRegistry, xmlApiService, systemEnvironment);

        Graph graph = rdfImporter.importJob(failedJob, baseUri);

        String testCountSelect = "PREFIX xunit: <" + XUnitOntology.URI + "> SELECT ?testcase WHERE { ?testCase a xunit:TestCase }";

        assertEquals(5, graph.select(testCountSelect).size());
    }

    @Test
    public void listFilesOfTypeWhenThereAreNoMatches() {
        JobResourceImporter rdfImporter = new JobResourceImporter("test/data/cruise", new InMemoryTempGraphFactory(), transformerRegistry, xmlApiService, systemEnvironment);

        File[] files = rdfImporter.getArtifactFilesOfType("artifacts/job2", "junit-reports", "boo");
        assertThat(files.length, equalTo(0));
    }

    @Test
    public void listArtifactFilesOfTypeWhenThereAreMatchesWithCaseInSensitiveExtensions() {
        JobResourceImporter rdfImporter = new JobResourceImporter("test/data/cruise", new InMemoryTempGraphFactory(), transformerRegistry, xmlApiService, systemEnvironment);
        File[] files = rdfImporter.getArtifactFilesOfType("artifacts", "job2", "xml");
        assertThat(files.length, equalTo(4));

        List<String> actualFileNames = new ArrayList<>();
        for (File f : files) {
            actualFileNames.add(f.getName());
        }
        assertThat(actualFileNames, hasItem("junit-report-failed.xml"));
        assertThat(actualFileNames, hasItem("junit-report-passed.xml"));
        assertThat(actualFileNames, hasItem("junit-report-passed-with-capitalcase-extension.XML"));
        assertThat(actualFileNames, hasItem("junit-report-passed-with-mixedcase-extension.Xml"));

    }

    @Test
    public void listArtifactFilesReturnsEmptyListWhenJobArtifactPathIsAFile() {
        JobResourceImporter rdfImporter = new JobResourceImporter("test/data/cruise", new InMemoryTempGraphFactory(), transformerRegistry, xmlApiService, systemEnvironment);
        File[] files = rdfImporter.getArtifactFilesOfType("artifacts/job2/junit-reports", "junit-report-failed.xml", "xml");
        assertThat(files.length, equalTo(0));
    }

    @Test
    public void listArtifactFilesReturnsEmptyListWhenJobArtifactPathIsAGlob() {
        JobResourceImporter rdfImporter = new JobResourceImporter("test/data/cruise", new InMemoryTempGraphFactory(), transformerRegistry, xmlApiService, systemEnvironment);
        File[] files = rdfImporter.getArtifactFilesOfType("artifacts", "job2/**/*.xml", "xml");
        assertThat(files.length, equalTo(0));
    }


    String jobWithArtifactsMissing = "" +
            "<job name='test'>" +
            "<link rel='self' href='http://job/1'/>" +
            "<result>Passed</result>" +
            "<properties>" +
            "<property name='cruise_job_id'><![CDATA[111]]></property>" +
            "<property name='cruise_timestamp_06_completed'>2008-01-15T16:36:19-08:00</property>" +
            "<property name='os'><![CDATA[OpenBSD]]></property>" +
            "</properties>" +
            "<artifacts baseUrl='http://cruise/missing-artifacts' pathFromArtifactRoot='/do/you/really/care'>" +
            "  <artifact src='' dest='duh' type='unit' />" +
            "</artifacts>" +
            "</job>";


    String happyJobResourceXML = "" +
            "<job name='test'>" +
            " <link rel='self' href='http://job/4'/>" +
            " <result>Passed</result>" +
            " <properties>" +
            "  <property name='cruise_job_id'><![CDATA[110]]></property>" +
            "  <property name='cruise_job_duration'><![CDATA[651]]></property>" +
            "  <property name='cruise_timestamp_06_completed'>2008-01-15T16:36:19-08:00</property>" +
            "  <property name='os'><![CDATA[OpenBSD]]></property>" +
            " </properties>" +
            " <artifacts baseUrl='http://cruise/' pathFromArtifactRoot='job4'>" +
            "  <artifact dest='junit-reports' src='some/**/path' type='unit' />" +
            "  <artifact dest='' src='simple/path' />" +
            " </artifacts>" +
            "</job>";


}
