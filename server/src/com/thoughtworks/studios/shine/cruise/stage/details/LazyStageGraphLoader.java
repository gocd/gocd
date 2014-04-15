/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.studios.shine.cruise.stage.details;

import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.studios.shine.semweb.Graph;
import com.thoughtworks.studios.shine.semweb.grddl.XSLTTransformerRegistry;
import com.thoughtworks.studios.shine.semweb.sesame.InMemoryTempGraphFactory;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LazyStageGraphLoader implements StageGraphLoader {
    private final StageResourceImporter importer;
    private final StageStorage stageStorage;
    private final GenericObjectPool transformerRegistryPool;

    @Autowired
    public LazyStageGraphLoader(StageResourceImporter importer, StageStorage stageStorage, SystemEnvironment env) {
        this(importer, stageStorage, env.getShineXslTransformerRegistryCacheSize());
    }

    LazyStageGraphLoader(StageResourceImporter importer, StageStorage stageStorage, int transformerRegistryPoolSize) {
        this.importer = importer;
        this.stageStorage = stageStorage;
        transformerRegistryPool = new GenericObjectPool(new BasePoolableObjectFactory() {
            @Override public Object makeObject() throws Exception {
                return new XSLTTransformerRegistry();
            }

            @Override public void activateObject(Object obj) throws Exception {
                XSLTTransformerRegistry registry = (XSLTTransformerRegistry) obj;
                registry.reset();
            }
        }, transformerRegistryPoolSize, GenericObjectPool.WHEN_EXHAUSTED_GROW, GenericObjectPool.DEFAULT_MAX_WAIT);
    }

    public Graph load(StageIdentifier stageIdentifier) {
        if (stageStorage.isStageStored(stageIdentifier)) {
            return stageStorage.load(stageIdentifier);
        }

        Graph graph = null;
        XSLTTransformerRegistry transformerRegistry = null;
        try {
            transformerRegistry = (XSLTTransformerRegistry) transformerRegistryPool.borrowObject();
            graph = importer.load(stageIdentifier, new InMemoryTempGraphFactory(), transformerRegistry);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (transformerRegistry != null) {
                try {
                    transformerRegistryPool.returnObject(transformerRegistry);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
        stageStorage.save(graph);
        return graph;
    }
}
