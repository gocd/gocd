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

package com.thoughtworks.studios.shine.cruise.stage.details;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.server.domain.xml.JobXmlViewModel;
import com.thoughtworks.go.server.service.XmlApiService;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.studios.shine.cruise.GoGRDDLResourceRDFizer;
import com.thoughtworks.studios.shine.cruise.GoIntegrationException;
import com.thoughtworks.studios.shine.cruise.GoOntology;
import com.thoughtworks.studios.shine.semweb.BoundVariables;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.TempGraphFactory;
import com.thoughtworks.studios.shine.semweb.URIReference;
import com.thoughtworks.studios.shine.semweb.grddl.XSLTTransformerRegistry;
import com.thoughtworks.studios.shine.xunit.AntJUnitReportRDFizer;
import com.thoughtworks.studios.shine.xunit.NUnitRDFizer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class JobResourceImporter {

    private final static Logger LOGGER = Logger.getLogger(JobResourceImporter.class);

    private final XMLArtifactImporter importer;
    private final GoGRDDLResourceRDFizer rdfizer;
    private String artifactBaseDir;

    public JobResourceImporter(String artifactBaseDir, TempGraphFactory graphFactory, XSLTTransformerRegistry transformerRegistry, XmlApiService xmlApiService, SystemEnvironment systemEnvironment) {
        this.artifactBaseDir = artifactBaseDir;
        rdfizer = new GoGRDDLResourceRDFizer("job", XSLTTransformerRegistry.CRUISE_JOB_GRDDL_XSL, graphFactory, transformerRegistry, xmlApiService);
        importer = new XMLArtifactImporter(systemEnvironment);
        AntJUnitReportRDFizer junitRDFizer = new AntJUnitReportRDFizer(graphFactory, transformerRegistry);
        importer.registerHandler(junitRDFizer);
        importer.registerHandler(new NUnitRDFizer(junitRDFizer, transformerRegistry));
    }

    public Graph importJob(JobInstance job, final String baseUri) throws GoIntegrationException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Attempting to import job " + job);
        }

        JobXmlViewModel xmlModel = new JobXmlViewModel(job);
        Graph jobGraph = rdfizer.importURIUsingGRDDL(xmlModel, baseUri);
        URIReference jobURI = jobGraph.getURIReference(xmlModel.httpUrl(baseUri));
        importArtifactsForJob(jobURI, jobGraph);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Done building jobs graph with " + jobGraph.size() + " triples");
        }

        return jobGraph;
    }

    private void importArtifactsForJob(URIReference jobResource, Graph jobsGraph) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Importing artifacts for job " + jobResource.getURIText());
        }

        String artifactsPathFromRoot = jobArtifactsPath(jobResource, jobsGraph);

        for (String jobArtifactPath : unitTestArtifactPathsForJob(jobsGraph, jobResource)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Found artifact at pathFromArtifactRoot: " + artifactsPathFromRoot + " and artifactPath: " + jobArtifactPath);
            }

            for (File testSuiteXMLFile : getArtifactFilesOfType(artifactsPathFromRoot, jobArtifactPath, "xml")) {
                try {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Trying to import " + testSuiteXMLFile);
                    }
                    importer.importFile(jobsGraph, jobResource, new FileInputStream(testSuiteXMLFile));
                } catch (FileNotFoundException e) {
                    LOGGER.debug("Unable to find file: " + testSuiteXMLFile.toString());
                }
            }
        }
    }

    private String jobArtifactsPath(URIReference jobResource, Graph jobsGraph) {
        String selectArtifactRoot = GoOntology.URI_PREFIX +
                "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " +

                "SELECT DISTINCT ?pathFromArtifactRoot WHERE { " +
                "<" + jobResource.getURIText() + "> cruise:hasArtifacts ?artifacts . " +
                "  ?artifacts a cruise:Artifacts . " +
                "  ?artifacts cruise:pathFromArtifactRoot ?pathFromArtifactRoot ." +
                "}";

        BoundVariables bv = jobsGraph.selectFirst(selectArtifactRoot);
        return (bv == null) ? null : bv.getString("pathFromArtifactRoot");
    }

    private List<String> unitTestArtifactPathsForJob(Graph graph, URIReference jobURI) {
        String selectArtifactPaths = GoOntology.URI_PREFIX +
                "prefix xsd: <http://www.w3.org/2001/XMLSchema#> " +

                "SELECT DISTINCT ?artifactPath WHERE { " +
                "<" + jobURI.getURIText() + "> cruise:hasArtifacts ?artifacts . " +
                "?artifacts a cruise:Artifacts . " +
                "?artifacts cruise:hasArtifact ?artifact . " +
                "?artifact cruise:artifactPath ?artifactPath . " +
                "?artifact cruise:artifactType 'unit'^^xsd:string " +
                "}";

        List<BoundVariables> bvs = graph.select(selectArtifactPaths);

        List<String> result = new ArrayList<>(bvs.size());
        for (BoundVariables bv : bvs) {
            result.add(bv.getString("artifactPath"));
        }
        return result;
    }

    File[] getArtifactFilesOfType(String artifactsPathFromRoot, String jobArtifactPath, final String fileExtension) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("getArtifactFilesOfType(" + artifactsPathFromRoot + ", " + jobArtifactPath + ", " + fileExtension + ")");
        }

        File jobArtifactFile = new File(artifactBaseDir, artifactsPathFromRoot + File.separator + jobArtifactPath);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Artifact directory calculated to be " + jobArtifactFile.getAbsolutePath());
        }

        if (!jobArtifactFile.exists() || !jobArtifactFile.isDirectory()) {
            return new File[0];
        }

        Collection collection = FileUtils.listFiles(jobArtifactFile, new SuffixFileFilter(fileExtension, IOCase.INSENSITIVE), TrueFileFilter.INSTANCE);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("" + collection.size() + " artifact files found.");
        }
        return (File[]) collection.toArray(new File[0]);
    }
}
