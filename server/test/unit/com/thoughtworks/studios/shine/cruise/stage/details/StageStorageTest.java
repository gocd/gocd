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

import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.util.TempFiles;
import com.thoughtworks.studios.shine.ShineRuntimeException;
import com.thoughtworks.studios.shine.cruise.GoOntology;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.TempGraphFactory;
import com.thoughtworks.studios.shine.semweb.sesame.InMemoryTempGraphFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static com.thoughtworks.studios.shine.AssertUtils.assertAskIsTrue;

public class StageStorageTest {
    private StageStorage stageStorage;
    private TempGraphFactory graphFactory;
    private TempFiles tempFiles;


    @Before
    public void setUp() throws Exception {
        tempFiles = new TempFiles();
        File tempFolder = tempFiles.createUniqueFolder("shine");
        stageStorage = new StageStorage(tempFolder.getAbsolutePath());
        stageStorage.clear();
        graphFactory = new InMemoryTempGraphFactory();
    }

    @After
    public void tearDown() {
        tempFiles.cleanUp();
    }

    @Test
    public void saveStageGraphAndLoadOutSameGraph() throws Exception {
        Graph graph = graphWithRDF("" +
                "@prefix cruise: <" + GoOntology.URI + "> . " +
                "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +
                "" +
                "<http://pipeline/1> a cruise:Pipeline ." +
                "<http://pipeline/1> cruise:pipelineName \"p\"^^xsd:string ." +
                "<http://pipeline/1> cruise:pipelineCounter 1 ." +
                "<http://pipeline/1> cruise:hasStage <http://stage/1> ." +
                "<http://stage/1> rdf:type cruise:Stage . " +
                "<http://stage/1> cruise:stageName \"s\"^^xsd:string . " +
                "<http://stage/1> cruise:stageCounter 2 . " +
                "<http://stage/1> cruise:hasJob <http://job/1> . " +
                "<http://job/1> a cruise:Job . " +
                "");

        stageStorage.save(graph);
        Graph loadedGraph = stageStorage.load(new StageIdentifier("p", 1, "s", "2"));

        assertAskIsTrue(loadedGraph, "" +
                "PREFIX cruise: <" + GoOntology.URI + "> " +
                "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#> " +
                "ASK WHERE {" +
                "  <http://pipeline/1> a cruise:Pipeline ." +
                "  <http://pipeline/1> cruise:pipelineName \"p\"^^xsd:string ." +
                "  <http://pipeline/1> cruise:pipelineCounter 1 ." +
                "  <http://pipeline/1> cruise:hasStage <http://stage/1> ." +
                "  <http://stage/1> rdf:type cruise:Stage . " +
                "  <http://stage/1> cruise:stageName \"s\"^^xsd:string . " +
                "  <http://stage/1> cruise:stageCounter 2 . " +
                "  <http://stage/1> cruise:hasJob <http://job/1> . " +
                "  <http://job/1> a cruise:Job . " +
                "}");
    }


    @Test(expected = ShineRuntimeException.class)
    public void shouldThrowExceptionWhenTryToLoadAStageNotPersisted() throws Exception {
        stageStorage.load(new StageIdentifier("p", 1, "s", "2"));
    }

    @Test(expected = ShineRuntimeException.class)
    public void shouldThrowExceptionWhenTryToSaveAGraphWithoutStageIdentifyInformation() throws Exception {
        Graph graph = graphWithRDF("" +
                "@prefix cruise: <" + GoOntology.URI + "> . " +
                "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +
                "" +
                "<http://pipeline/1> a cruise:Pipeline ." +
                "<http://pipeline/1> cruise:pipelineName \"p\"^^xsd:string ." +
                "<http://pipeline/1> cruise:hasStage <http://stage/1> ." +
                "<http://stage/1> rdf:type cruise:Stage . " +
                "<http://stage/1> cruise:stageName \"s\"^^xsd:string . " +
                "<http://stage/1> cruise:stageCounter 2 . " +
                "<http://stage/1> cruise:hasJob <http://job/1> . " +
                "<http://job/1> a cruise:Job . " +
                "");

        stageStorage.save(graph);
    }


    private Graph graphWithRDF(String rdf) {
        Graph graph = graphFactory.createTempGraph();
        graph.addTriplesFromTurtle(rdf);
        return graph;
    }
}
