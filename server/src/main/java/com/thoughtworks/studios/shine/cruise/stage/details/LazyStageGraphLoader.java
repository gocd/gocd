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
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.grddl.XSLTTransformerRegistry;
import com.thoughtworks.studios.shine.semweb.sesame.InMemoryTempGraphFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

@Component
public class LazyStageGraphLoader implements StageGraphLoader {
    private final StageResourceImporter importer;
    private final StageStorage stageStorage;
    private final XSLTTransformerRegistry transformerRegistry;

    @Autowired
    public LazyStageGraphLoader(StageResourceImporter importer, StageStorage stageStorage) {
        this.importer = importer;
        this.stageStorage = stageStorage;
        this.transformerRegistry = new XSLTTransformerRegistry();
    }

    public Graph load(StageIdentifier stageIdentifier) {
        if (stageStorage.isStageStored(stageIdentifier)) {
            return stageStorage.load(stageIdentifier);
        }

        Graph graph;
        try {
            graph = importer.load(stageIdentifier, new InMemoryTempGraphFactory(), transformerRegistry);
        } catch (Exception e) {
            throw bomb(e);
        }
        stageStorage.save(graph);
        return graph;
    }
}
