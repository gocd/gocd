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
import com.thoughtworks.go.domain.Stage;
import com.thoughtworks.go.domain.StageFinder;
import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.presentation.pipelinehistory.PipelineInstanceModel;
import com.thoughtworks.go.server.domain.xml.PipelineXmlViewModel;
import com.thoughtworks.go.server.domain.xml.StageXmlViewModel;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.PipelineInstanceLoader;
import com.thoughtworks.go.server.service.XmlApiService;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.studios.shine.cruise.GoGRDDLResourceRDFizer;
import com.thoughtworks.studios.shine.cruise.GoIntegrationException;
import com.thoughtworks.studios.shine.cruise.GoOntology;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.TempGraphFactory;
import com.thoughtworks.studios.shine.semweb.grddl.XSLTTransformerRegistry;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
public class StageResourceImporter {
    private final static Logger LOGGER = Logger.getLogger(StageResourceImporter.class);

    private String artifactsBaseDir;
    private final XmlApiService xmlApiService;
    private final StageFinder stageFinder;
    private final PipelineInstanceLoader pipelineInstanceLoader;
    private GoConfigService goConfigService;
    private SystemEnvironment systemEnvironment;

    @Autowired
    public StageResourceImporter(GoConfigService goConfigService, XmlApiService xmlApiService, StageFinder stageFinder, PipelineInstanceLoader pipelineInstanceLoader, SystemEnvironment systemEnvironment) {
        this.xmlApiService = xmlApiService;
        this.stageFinder = stageFinder;
        this.pipelineInstanceLoader = pipelineInstanceLoader;
        this.goConfigService = goConfigService;
        this.systemEnvironment = systemEnvironment;
    }

//used for tests
    public StageResourceImporter(String artifactsBaseDir, XmlApiService xmlApiService, StageFinder stageFinder, PipelineInstanceLoader pipelineInstanceLoader, SystemEnvironment systemEnvironment) {
        this.artifactsBaseDir = artifactsBaseDir;
        this.xmlApiService = xmlApiService;
        this.stageFinder = stageFinder;
        this.pipelineInstanceLoader = pipelineInstanceLoader;
        this.systemEnvironment = systemEnvironment;
    }

    public void initialize(){
       this.artifactsBaseDir = goConfigService.artifactsDir().getAbsolutePath();
    }

    public Graph load(StageIdentifier stageIdentifier, TempGraphFactory tempGraphFactory, final XSLTTransformerRegistry transformerRegistry) throws GoIntegrationException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Attempting to import stage with url <" + stageIdentifier + "> !");
        }
        long importStartingTime = System.currentTimeMillis();
        try {
            Stage stage = stageFinder.findStageWithIdentifier(stageIdentifier);
            String baseUri = "https://localhost:8154/go";
            Graph graph = loadIsolatedStageGraph(stageIdentifier, tempGraphFactory, transformerRegistry, stage, baseUri);
            importPipeline(pipelineInstanceLoader.loadPipelineForShine(stage.getPipelineId()), graph, transformerRegistry, baseUri);
            importJobs(graph, transformerRegistry, stage, baseUri);
            return graph;
        } finally {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Done importing stage with url <" + stageIdentifier + "> with " + (System.currentTimeMillis() - importStartingTime) + " ms!");
            }
        }
    }

    private Graph loadIsolatedStageGraph(StageIdentifier stageIdentifier, TempGraphFactory tempGraphFactory, XSLTTransformerRegistry transformerRegistry,
                                         final Stage stageWithIdentifier, final String baseUri) throws GoIntegrationException {
        GoGRDDLResourceRDFizer stageRdfizer = new GoGRDDLResourceRDFizer("stage", XSLTTransformerRegistry.CRUISE_STAGE_GRAPH_GRDDL_XSL, tempGraphFactory, transformerRegistry, xmlApiService);
        Graph graph = stageRdfizer.importURIUsingGRDDL(new StageXmlViewModel(stageWithIdentifier), baseUri);
        if (!stageCompleted(graph)) {
            throw new CanNotImportABuildingStageException(stageIdentifier + " is not completed yet, can not load test details");
        }
        return graph;
    }

    private boolean stageCompleted(Graph graph) {
        String completedAsk = "" +
                "PREFIX cruise: <" + GoOntology.URI + "> " +
                "PREFIX xsd:<http://www.w3.org/2001/XMLSchema#> " +
                "ASK WHERE {" +
                "?stage a cruise:Stage . " +
                "?stage cruise:stageState \"Completed\"^^xsd:string . " +
                "}";
        return graph.ask(completedAsk);
    }

    private void importPipeline(PipelineInstanceModel pipelineInstance, Graph graph, XSLTTransformerRegistry transformerRegistry, String baseUri) throws GoIntegrationException {
        final GoGRDDLResourceRDFizer pipeline = new GoGRDDLResourceRDFizer("pipeline", XSLTTransformerRegistry.CRUISE_PIPELINE_GRAPH_GRDDL_XSL, graph, transformerRegistry, xmlApiService);
        Graph pipelineGraph = pipeline.importURIUsingGRDDL(new PipelineXmlViewModel(pipelineInstance), baseUri);

        graph.addTriplesFromGraph(pipelineGraph);
    }

    private void importJobs(Graph stageGraph, XSLTTransformerRegistry transformerRegistry, Stage stage, final String baseUri) throws GoIntegrationException {
        JobResourceImporter importer = new JobResourceImporter(artifactsBaseDir, stageGraph, transformerRegistry, xmlApiService, systemEnvironment);
        for (JobInstance instance : stage.getJobInstances()) {
            stageGraph.addTriplesFromGraph(importer.importJob(instance, baseUri));
        }
    }
}
