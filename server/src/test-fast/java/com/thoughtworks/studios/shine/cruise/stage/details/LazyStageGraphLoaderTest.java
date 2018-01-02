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

import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.studios.shine.cruise.GoIntegrationException;
import com.thoughtworks.studios.shine.cruise.GoOntology;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.TempGraphFactory;
import com.thoughtworks.studios.shine.semweb.grddl.XSLTTransformerRegistry;
import com.thoughtworks.studios.shine.semweb.sesame.InMemoryTempGraphFactory;
import com.thoughtworks.studios.shine.xunit.XUnitOntology;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.concurrent.Semaphore;

import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.*;

public class LazyStageGraphLoaderTest {
    private StageStorage stageStorage;
    private InMemoryTempGraphFactory graphFactory;

    @Before
    public void initStageStorage() {
        stageStorage = new StageStorage(new File(System.getProperty("java.io.tmpdir"), "shine").getAbsolutePath());
        stageStorage.clear();
        graphFactory = new InMemoryTempGraphFactory();
    }

    @Test
    public void directLoadFromStageStorageIfStageAlreadyStored() {
        Graph stageGraph = graphFactory.createTempGraph();
        stageGraph.addTriplesFromTurtle("" +
                "@prefix cruise: <" + GoOntology.URI + "> . " +
                "@prefix xunit: <" + XUnitOntology.URI + "> . " +
                "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +
                "" +
                " <http://pipeline/1> a cruise:Pipeline . " +
                " <http://pipeline/1> cruise:pipelineName \"pipeline-foo\"^^xsd:string ." +
                " <http://pipeline/1> cruise:pipelineCounter \"23\"^^xsd:integer . " +
                " <http://pipeline/1> cruise:hasStage <http://stage/1> . " +
                " <http://stage/1> a cruise:Stage . " +
                " <http://stage/1> cruise:stageName \"stage-1\"^^xsd:string . " +
                " <http://stage/1> cruise:stageCounter \"1\"^^xsd:integer . " +
                "");


        stageStorage.save(stageGraph);
        LazyStageGraphLoader loader = new LazyStageGraphLoader(null, stageStorage);
        Graph loadedStageGraph = loader.load(new StageIdentifier("pipeline-foo", 23, "stage-1", "1"));
        assertTrue(loadedStageGraph.containsResourceWithURI("http://stage/1"));
    }

    @Test
    public void loadsFromRealLoaderAndSavesToStorageWhenNotAlreadyStoried() {
        StageIdentifier stageId = new StageIdentifier("pipeline-foo", 23, "stage-1", "1");

        DummyStageResourceImporter realLoader = new DummyStageResourceImporter(realGraph(), stageId);

        LazyStageGraphLoader loader = new LazyStageGraphLoader(realLoader, stageStorage);

        Graph stageGraph = loader.load(stageId);
        assertTrue(stageGraph.containsResourceWithURI("http://stage/1"));
        assertTrue(stageStorage.isStageStored(stageId));
    }

    private Graph realGraph() {
        final Graph realStageGraph = graphFactory.createTempGraph();
        realStageGraph.addTriplesFromTurtle("" +
                "@prefix cruise: <" + GoOntology.URI + "> . " +
                "@prefix xunit: <" + XUnitOntology.URI + "> . " +
                "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +
                "" +
                " <http://pipeline/1> a cruise:Pipeline . " +
                " <http://pipeline/1> cruise:pipelineName \"pipeline-foo\"^^xsd:string ." +
                " <http://pipeline/1> cruise:pipelineCounter \"23\"^^xsd:integer . " +
                " <http://pipeline/1> cruise:hasStage <http://stage/1> . " +
                " <http://stage/1> a cruise:Stage . " +
                " <http://stage/1> cruise:stageName \"stage-1\"^^xsd:string . " +
                " <http://stage/1> cruise:stageCounter \"1\"^^xsd:integer . " +
                " <http://stage/1> cruise:hasJob <http://job/1> . " +  // only job in the stage graph
                "");
        return realStageGraph;
    }

    @Test
    public void shouldReuseTransformerAcrossSerialInvocations() {
        StageIdentifier stageId = new StageIdentifier("pipeline-foo", 23, "stage-1", "1");

        DummyStageResourceImporter realLoader = new DummyStageResourceImporter(realGraph(), stageId, new Semaphore(2));

        LazyStageGraphLoader loader = new LazyStageGraphLoader(realLoader, stageStorage);

        loader.load(stageId);
        XSLTTransformerRegistry transformerRegistryFromFirstLoad = realLoader.transformerRegistry;

        stageStorage.clear();
        loader.load(stageId);
        XSLTTransformerRegistry transformerRegistryFromSecondLoad = realLoader.transformerRegistry;

        assertThat(transformerRegistryFromFirstLoad, sameInstance(transformerRegistryFromSecondLoad));
    }

    class DummyStageResourceImporter extends StageResourceImporter {
        private Graph importedStageGraph;
        private StageIdentifier expectedStageId;
        private Semaphore semaphore;
        private XSLTTransformerRegistry transformerRegistry;

        DummyStageResourceImporter(Graph importedStageGraph, StageIdentifier stageId) {
            this(importedStageGraph, stageId, new Semaphore(1));
        }

        DummyStageResourceImporter(Graph importedStageGraph, StageIdentifier stageId, Semaphore semaphore) {
            super((String) null, null, null, null,null);
            this.importedStageGraph = importedStageGraph;
            this.expectedStageId = stageId;
            this.semaphore = semaphore;
        }

        @Override public Graph load(StageIdentifier stageIdentifier, TempGraphFactory tempGraphFactory, final XSLTTransformerRegistry transformerRegistry) throws GoIntegrationException {
            this.transformerRegistry = transformerRegistry;
            try {
                semaphore.acquire();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            assertEquals(expectedStageId, stageIdentifier);
            return importedStageGraph;
        }
    }
}
