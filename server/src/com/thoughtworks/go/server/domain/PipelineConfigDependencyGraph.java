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

package com.thoughtworks.go.server.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.MaterialConfig;

/**
 * @understands configuration of all the upstream pipelines.
 */
public class PipelineConfigDependencyGraph {

    private final PipelineConfig current;
    private final List<PipelineConfigDependencyGraph> upstreamDependencies;

    public PipelineConfigDependencyGraph(PipelineConfig current, PipelineConfigDependencyGraph... upstreamDependencies) {
        this.current = current;
        this.upstreamDependencies = Arrays.asList(upstreamDependencies);
    }

    public PipelineConfig getCurrent() {
        return current;
    }

    public List<PipelineConfigDependencyGraph> getUpstreamDependencies() {
        return upstreamDependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PipelineConfigDependencyGraph that = (PipelineConfigDependencyGraph) o;

        if (current != null ? !current.equals(that.current) : that.current != null) {
            return false;
        }
        if (upstreamDependencies != null ? !upstreamDependencies.equals(that.upstreamDependencies) : that.upstreamDependencies != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = current != null ? current.hashCode() : 0;
        result = 31 * result + (upstreamDependencies != null ? upstreamDependencies.hashCode() : 0);
        return result;
    }

    public Queue<PipelineConfigQueueEntry> buildQueue() {
        Queue<PipelineConfigQueueEntry> configQueue = new LinkedList<>();
        Queue<PipelineConfigDependencyEntry> tmp = new LinkedList<>();
        tmp.add(new PipelineConfigDependencyEntry(this, new ArrayList<PipelineConfig>()));
        while (true) {
            PipelineConfigDependencyEntry currentHead = tmp.poll();
            if (currentHead == null) {
                break;
            }
            PipelineConfigDependencyGraph current = currentHead.getNode();
            List<PipelineConfig> currentPath = currentHead.getPath();
            currentPath.add(current.getCurrent());
            configQueue.add(new PipelineConfigQueueEntry(current.getCurrent(), new ArrayList<>(currentPath)));
            for (PipelineConfigDependencyGraph upstream : current.getUpstreamDependencies()) {
                List<PipelineConfig> parentsPath = new ArrayList<>(currentPath);
                tmp.add(new PipelineConfigDependencyEntry(upstream, parentsPath));
            }
        }
        return removeHead(configQueue);
    }

    private Queue<PipelineConfigQueueEntry> removeHead(Queue<PipelineConfigQueueEntry> configQueue) {
        configQueue.poll();
        return configQueue;
    }

    public MaterialConfigs unsharedMaterialConfigs() {
        MaterialConfigs firstOrderMaterials = new MaterialConfigs();
        List<PipelineConfigQueueEntry> queue = new ArrayList<>(buildQueue());
        for (MaterialConfig materialConfig : current.materialConfigs()) {
            if (!existsOnAnyOfPipelinesIn(queue, materialConfig)) {
                firstOrderMaterials.add(materialConfig);
            }
        }
        return firstOrderMaterials;
    }

    public Set<String> allMaterialFingerprints() {
        Set<String> materialFingerPrints = new HashSet<>();
        List<PipelineConfigQueueEntry> queue = new ArrayList<>(buildQueue());
        for (PipelineConfigQueueEntry pipelineConfigQueueEntry : queue) {
            addMaterialsForConfig(materialFingerPrints, Arrays.asList(pipelineConfigQueueEntry.node));
            addMaterialsForConfig(materialFingerPrints, pipelineConfigQueueEntry.path);
        }
        return materialFingerPrints;
    }

    private void addMaterialsForConfig(Set<String> materialFingerPrints, List<PipelineConfig> pipelineConfigs) {
        for (PipelineConfig pipelineConfig : pipelineConfigs) {
            for (MaterialConfig material : pipelineConfig.materialConfigs()) {
                materialFingerPrints.add(material.getFingerprint());
            }
        }
    }

    private boolean existsOnAnyOfPipelinesIn(List<PipelineConfigQueueEntry> queue, MaterialConfig materialConfig) {
        for (PipelineConfigQueueEntry pipelineConfigQueueEntry : queue) {
            if (pipelineConfigQueueEntry.hasMaterial(materialConfig)) {
                return true;
            }
        }
        return false;
    }

    public boolean isRevisionsOfSharedMaterialsIgnored(MaterialRevisions revisions) {
        MaterialConfigs unsharedScmMaterialConfigs = unsharedMaterialConfigs();
        List<PipelineConfigQueueEntry> queue = new ArrayList<>(buildQueue());
        for (MaterialRevision revision : revisions) {
            Material material = revision.getMaterial();
            MaterialConfig materialConfig = material.config();
            if (unsharedScmMaterialConfigs.hasMaterialWithFingerprint(materialConfig) || revision.isDependencyMaterialRevision()) {
                continue;
            }
            if (isThisMaterialIgnored(queue, revision, materialConfig)) {
                return true;
            }
        }
        return false;
    }

    private boolean isThisMaterialIgnored(List<PipelineConfigQueueEntry> queue, MaterialRevision revision, MaterialConfig materialConfig) {
        for (PipelineConfigQueueEntry pipelineConfigQueueEntry : queue) {
            if (pipelineConfigQueueEntry.hasMaterial(materialConfig)) {
                if (!pipelineConfigQueueEntry.shouldIgnore(revision)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static class PipelineConfigDependencyEntry {
        private PipelineConfigDependencyGraph node;
        private List<PipelineConfig> path;

        public PipelineConfigDependencyEntry(PipelineConfigDependencyGraph node, List<PipelineConfig> pipelineConfigs) {
            this.node = node;
            this.path = pipelineConfigs;
        }

        public PipelineConfigDependencyGraph getNode() {
            return node;
        }

        public List<PipelineConfig> getPath() {
            return path;
        }
    }

    public static class PipelineConfigQueueEntry {
        private PipelineConfig node;
        private List<PipelineConfig> path;

        public PipelineConfigQueueEntry(PipelineConfig node, List<PipelineConfig> pipelineConfigs) {
            this.node = node;
            this.path = pipelineConfigs;
        }

        public PipelineConfig getNode() {
            return node;
        }

        public List<PipelineConfig> getPath() {
            return path;
        }

        @Override
        public String toString() {
            return "PipelineConfigQueueEntry{" +
                    "node=" + node +
                    ", path=" + path +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            PipelineConfigQueueEntry that = (PipelineConfigQueueEntry) o;

            if (node != null ? !node.equals(that.node) : that.node != null) {
                return false;
            }
            if (path != null ? !path.equals(that.path) : that.path != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = node != null ? node.hashCode() : 0;
            result = 31 * result + (path != null ? path.hashCode() : 0);
            return result;
        }

        public List<PipelineConfig> pathWithoutHead() {
            List<PipelineConfig> copy = new ArrayList<>(path);
            if (!copy.isEmpty()) {
                copy.remove(0);
            }
            return copy;
        }

        public boolean containsPipelineInPath(String pipelineName) {
            for (PipelineConfig pipelineConfig : path) {
                if (CaseInsensitiveString.str(pipelineConfig.name()).equalsIgnoreCase(pipelineName)) {
                    return true;
                }
            }
            return false;
        }

        public boolean hasMaterial(MaterialConfig materialConfig) {
            return node.hasMaterialWithFingerprint(materialConfig);
        }

        public boolean shouldIgnore(MaterialRevision revision) {
            Material material = revision.getMaterial();
            for (MaterialConfig materialConfig : node.materialConfigs()) {
                if (material.hasSameFingerprint(materialConfig)) {
                    return revision.getModifications().shouldBeIgnoredByFilterIn(materialConfig);
                }
            }
            throw new RuntimeException("Material not found: " + material);//IMP: because, config can change between BCPS call and build cause production - shilpa/jj
        }
    }
}
