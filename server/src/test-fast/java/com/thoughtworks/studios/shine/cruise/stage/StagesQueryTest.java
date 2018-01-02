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

package com.thoughtworks.studios.shine.cruise.stage;

import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.server.dao.sparql.RdfResultMapper;
import com.thoughtworks.studios.shine.cruise.GoOntology;
import com.thoughtworks.studios.shine.cruise.stage.details.StageStorage;
import com.thoughtworks.studios.shine.semweb.BoundVariables;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.TempGraphFactory;
import com.thoughtworks.studios.shine.semweb.URIReference;
import com.thoughtworks.studios.shine.semweb.sesame.InMemoryTempGraphFactory;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class StagesQueryTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private StageStorage graphLoader;
    private TempGraphFactory graphFactory;

    @Before
    public void setUp() throws Exception {
        File tempFolder = temporaryFolder.newFolder("shine");
        graphLoader = new StageStorage(tempFolder.getAbsolutePath());
        graphLoader.clear();
        graphFactory = new InMemoryTempGraphFactory();
    }

    @Test
    public void testRunSparqlAgainstMultipleStagesWithEmptyCacheWillPopulateCache() {
        populateStage("p", 1, "s", 2, "http://job/1");
        populateStage("p", 2, "s", 2, "http://job/2");


        InMemoryCache cache = new InMemoryCache();

        StagesQuery stagesQuery = new StagesQuery(graphLoader, cache);
        String sparql = "PREFIX cruise:<" + GoOntology.URI + ">"
                + "SELECT ?job WHERE {"
                + "  ?job a cruise:Job . "
                + "}";

        StageIdentifier stageIdentifierOne = new StageIdentifier("p", 1, "s", "2");
        StageIdentifier stageIdentifierTwo = new StageIdentifier("p", 2, "s", "2");
        List<TestModel> bvs = stagesQuery.select(sparql, Arrays.asList(stageIdentifierOne, stageIdentifierTwo), new RdfResultMapper<TestModel>() {
            public TestModel map(BoundVariables aRow) {
                return new TestModel(aRow.getAsString("job"));
            }
        });

        assertEquals(2, bvs.size());
        assertEquals(new TestModel("http://job/1"), bvs.get(0));
        assertEquals(new TestModel("http://job/2"), bvs.get(1));

        assertNotNull(cache.get(new StagesQueryCache.CacheKey(sparql, stageIdentifierOne)));
        assertNotNull(cache.get(new StagesQueryCache.CacheKey(sparql, stageIdentifierTwo)));
    }

    private static class TestModel {

        private final String job;

        public TestModel(String job) {
            this.job = job;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TestModel)) {
                return false;
            }

            TestModel testModel = (TestModel) o;

            if (job != null ? !job.equals(testModel.job) : testModel.job != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return job != null ? job.hashCode() : 0;
        }
    }

    @Test
    public void testQueryResultWillBeGetFromCacheIfCacheIsHit() {
        InMemoryCache cache = new InMemoryCache();
        StagesQuery stagesQuery = new StagesQuery(graphLoader, cache);
        String sparql = "PREFIX cruise:<" + GoOntology.URI + ">"
                + "SELECT ?job WHERE {"
                + "  ?job a cruise:Job . "
                + "}";

        StageIdentifier stageIdentifier = new StageIdentifier("p", 1, "s", "2");
        cache.put(Arrays.<BoundVariables>asList(new BoundVariablesStub("job", "http://job/1")), new StagesQueryCache.CacheKey(sparql, stageIdentifier));


        List<BoundVariables> bvs = stagesQuery.select(sparql, Arrays.asList(stageIdentifier), new RdfResultMapper<BoundVariables>() {
            public BoundVariables map(BoundVariables aRow) {
                return aRow;
            }
        });
        assertEquals(1, bvs.size());
        assertEquals("http://job/1", bvs.get(0).getAsString("job"));
    }

    private void populateStage(String pipelineName, int pipelineCounter, String stageName, int stageCounter, String job) {

        graphLoader.save(graphWithRDF("" +
                "@prefix cruise: <" + GoOntology.URI + "> . " +
                "@prefix rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> . " +
                "@prefix xsd: <http://www.w3.org/2001/XMLSchema#> . " +
                "" +
                "<http://pipeline/1> a cruise:Pipeline ." +
                "<http://pipeline/1> cruise:pipelineName \"" + pipelineName + "\"^^xsd:string ." +
                "<http://pipeline/1> cruise:pipelineCounter " + pipelineCounter + " ." +
                "<http://pipeline/1> cruise:hasStage <http://stage/1> ." +
                "<http://stage/1> rdf:type cruise:Stage . " +
                "<http://stage/1> cruise:stageName \"" + stageName + "\"^^xsd:string . " +
                "<http://stage/1> cruise:stageCounter " + stageCounter + " . " +
                "<http://stage/1> cruise:hasJob <" + job + "> . " +
                "<" + job + "> a cruise:Job . " +
                ""));
    }

    private Graph graphWithRDF(String rdf) {
        Graph graph = graphFactory.createTempGraph();
        graph.addTriplesFromTurtle(rdf);
        return graph;
    }

    private class BoundVariablesStub implements BoundVariables {
        private String variable;
        private String value;

        public BoundVariablesStub(String variable, String value) {

            this.variable = variable;
            this.value = value;
        }

        public String getAsString(String boundName) {
            if (boundName.equals(variable)) {
                return value;
            }
            return null;
        }

        public Boolean getBoolean(String boundName) {
            return null;
        }

        public List<String> getBoundVariableNames() {
            return null;
        }

        public Integer getInt(String boundName) {
            return null;
        }

        public String getString(String boundName) {
            return null;
        }

        public URIReference getURIReference(String boundName) {
            return null;
        }
    }

    private class InMemoryCache extends HashMap<String, List> implements StagesQueryCache {
        public List get(CacheKey key) {
            return this.get(key.getKey());
        }

        public void put(List boundVariables, CacheKey key) {
            this.put(key.getKey(), boundVariables);
        }
    }
}
