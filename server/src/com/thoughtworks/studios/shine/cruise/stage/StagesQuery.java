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

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.server.dao.sparql.RdfResultMapper;
import com.thoughtworks.studios.shine.cruise.stage.details.StageGraphLoader;
import com.thoughtworks.studios.shine.semweb.BoundVariables;
import com.thoughtworks.studios.shine.semweb.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StagesQuery {

    private final static Logger LOGGER = LoggerFactory.getLogger(StagesQuery.class);
    private StageGraphLoader stageGraphLoader;
    private StagesQueryCache cache;

    public StagesQuery(StageGraphLoader stageGraphLoader, StagesQueryCache cache) {
        this.stageGraphLoader = stageGraphLoader;
        this.cache = cache;
    }

    public <T> List<T> select(String sparql, List<StageIdentifier> identifiers, RdfResultMapper<T> mapper) {
        long queryStartTime = System.currentTimeMillis();
        List<T> result = new ArrayList<>();
        for (StageIdentifier failedStageIdentifier : identifiers) {
            List<T> resultForAStage = selectForSingleStage(sparql, failedStageIdentifier, mapper);
            result.addAll(resultForAStage);
        }
        LOGGER.debug("For stages: {}\n Running SPARQL: {}\n Get %d results using %dms.", identifiers, sparql, result.size(), (System.currentTimeMillis() - queryStartTime));
        return result;
    }

    private <T> List<T> selectForSingleStage(String sparql, StageIdentifier stageIdentifier, RdfResultMapper<T> mapper) {
        StagesQueryCache.CacheKey key = new StagesQueryCache.CacheKey(sparql, stageIdentifier);
        List<T> mappedResults = (List<T>) cache.get(key);
        if (mappedResults != null) {
            return mappedResults;
        }
        synchronized (key.getKey().intern()) {
            mappedResults = (List<T>) cache.get(key);
            if (mappedResults != null) {
                return mappedResults;
            }
            Graph graph = stageGraphLoader.load(stageIdentifier);
            List<BoundVariables> boundVariableses = graph.select(sparql);
            mappedResults = new ArrayList<>();
            for (BoundVariables bv : boundVariableses) {
                mappedResults.add(mapper.map(bv));
            }
            cache.put(mappedResults, key);
            return mappedResults;
        }
    }
}
