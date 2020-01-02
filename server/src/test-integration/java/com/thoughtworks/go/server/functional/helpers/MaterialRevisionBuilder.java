/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.functional.helpers;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.JobConfigs;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.dependency.DependencyMaterialRevision;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.domain.PipelineConfigDependencyGraph;
import com.thoughtworks.go.server.persistence.MaterialRepository;

import java.util.*;

public class MaterialRevisionBuilder {

    private final PipelineSqlMapDao pipelineDao;
    private final MaterialRepository materialRepository;
    private final Map<String, MaterialRevision> instanceToRevision = new HashMap<>();
    private long dataBaseId = 1;
    private MaterialRevision currentRevision;
    private static final String STAGE_NAME = "stagename";

    public static class Tuple {

        private final PipelineConfigDependencyGraph graph;

        private final MaterialRevision revision;

        private Tuple(PipelineConfigDependencyGraph graph, MaterialRevision revision) {
            this.graph = graph;
            this.revision = revision;
        }

        public MaterialRevision getRevision() {
            return revision;
        }

        public PipelineConfigDependencyGraph getGraph() {
            return graph;
        }

    }

    public MaterialRevisionBuilder(PipelineSqlMapDao pipelineDao, MaterialRepository materialRepository) {
        this.pipelineDao = pipelineDao;
        this.materialRepository = materialRepository;
    }

    public Tuple depInstance(String pipelineName, int counter, Date modifiedTime, Tuple... buildCause) {
        String key = key(pipelineName, counter, modifiedTime);
        if (!instanceToRevision.containsKey(key)) {
            if (buildCause.length == 0) {
                throw new RuntimeException("Cannot create instance without a buildcause. You can retrive it without buildcause once it has been created");
            }
            DependencyMaterial material = new DependencyMaterial(new CaseInsensitiveString(pipelineName), new CaseInsensitiveString(STAGE_NAME));
            DependencyMaterialRevision revision = DependencyMaterialRevision.create(pipelineName, counter, "label", STAGE_NAME, 1);
            instanceToRevision.put(key, revision.convert(material, modifiedTime));
            final long id = getNextId();
            org.mockito.Mockito.when(pipelineDao.findPipelineByNameAndCounter(pipelineName, counter)).thenReturn(pipeline(id));
            org.mockito.Mockito.when(materialRepository.findMaterialRevisionsForPipeline(id)).thenReturn(buildCauseOfThisPipeline(buildCause));
        }
        MaterialRevision materialRevision = instanceToRevision.get(key);
        Materials materials = new Materials();
        for (MaterialRevision revision : buildCauseOfThisPipeline(buildCause)) {
            materials.add(revision.getMaterial());
        }
        PipelineConfig config = new PipelineConfig(new CaseInsensitiveString(pipelineName), materials.convertToConfigs(), new StageConfig(new CaseInsensitiveString(STAGE_NAME), new JobConfigs()));
        return new Tuple(new PipelineConfigDependencyGraph(config, dependencyGraphsFor(buildCause)), materialRevision);
    }

    private PipelineConfigDependencyGraph[] dependencyGraphsFor(Tuple[] buildCause) {
        List<PipelineConfigDependencyGraph> childGraphs = new ArrayList<>();
        for (Tuple tuple : buildCause) {
            if (tuple.graph != null) {
                childGraphs.add(tuple.graph);
            }
        }
        return childGraphs.toArray(new PipelineConfigDependencyGraph[0]);
    }

    public Tuple svnInstance(String revision, Date modifiedTime) {
        String key = key("svn", revision, modifiedTime);
        insertIfNotPresent(new SvnMaterial("url", "username", "password", false), key, revision, modifiedTime);
        return new Tuple(null, instanceToRevision.get(key));
    }

    public Tuple hgInstance(String revision, Date modifiedTime) {
        String key = key("hg", revision, modifiedTime);
        insertIfNotPresent(new HgMaterial("url", null), key, revision, modifiedTime);
        return new Tuple(null, instanceToRevision.get(key));
    }

    private void insertIfNotPresent(Material material, String key, String revision, Date modifiedTime) {
        if (!instanceToRevision.containsKey(key)) {
            Modification modification = new Modification("username", "comment", "email", modifiedTime, revision);
            instanceToRevision.put(key, new MaterialRevision(material, modification));
        }
    }

    private String key(Object... parts) {
        StringBuilder builder = new StringBuilder();
        for (Object part : parts) {
            builder.append(part);
        }
        return builder.toString();
    }

    private MaterialRevisions buildCauseOfThisPipeline(Tuple... buildCause) {
        MaterialRevisions materialRevisions = new MaterialRevisions();
        for (Tuple materialRevision : buildCause) {
            materialRevisions.addRevision(materialRevision.revision);
        }
        return materialRevisions;
    }

    private Pipeline pipeline(long id) {
        Pipeline pipeline = new Pipeline();
        pipeline.setId(id);
        return pipeline;
    }

    private long getNextId() {
        return dataBaseId++;
    }

    public MaterialRevisionBuilder lookingAtDep(String pipelineName, int counter, Date date) {
        return lookingAt(key(pipelineName, counter, date));
    }

    private MaterialRevisionBuilder lookingAt(String key) {
        MaterialRevision revision = instanceToRevision.get(key);
        this.currentRevision = new MaterialRevision(revision.getMaterial(), revision.getModifications());
        return this;
    }

    public MaterialRevisionBuilder lookingAtHg(String revision, Date date) {
        return lookingAt(key("hg", revision, date));
    }

    public MaterialRevisionBuilder markAsChanged() {
        this.currentRevision.markAsChanged();
        return this;
    }

    public MaterialRevision revision() {
        return currentRevision;
    }
}
