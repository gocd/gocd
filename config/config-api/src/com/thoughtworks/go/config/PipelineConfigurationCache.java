/*************************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.util.Node;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PipelineConfigurationCache {
    private PipelineConfigMap pipelineConfigHashMap;
    private MaterialConfigFingerprintMap materialConfigsFingerprintMap;

    private static PipelineConfigurationCache pipelineConfigurationCache = new PipelineConfigurationCache();
    private CruiseConfig cruiseConfig;
    private  Hashtable<CaseInsensitiveString, Node> dependencies;

    private PipelineConfigurationCache() {
    }

    public static PipelineConfigurationCache getInstance() {
        return pipelineConfigurationCache;
    }

    public void onConfigChange(CruiseConfig cruiseConfig) {
        this.cruiseConfig = cruiseConfig;
        pipelineConfigHashMap = null;
        materialConfigsFingerprintMap = null;
        dependencies = null;
    }

    public MaterialConfigs getMatchingMaterialsFromConfig(String fingerprint) {
        initMaterialConfigMap();
        return materialConfigsFingerprintMap.get(fingerprint);
    }

    private void initMaterialConfigMap() {
        if (materialConfigsFingerprintMap == null) {
            materialConfigsFingerprintMap = new MaterialConfigFingerprintMap(cruiseConfig);
        }
    }

    public PipelineConfig getPipelineConfig(String pipelineName) {
        initPipelineConfigMap();
        return pipelineConfigHashMap.getPipelineConfig(pipelineName);
    }

    private void initPipelineConfigMap() {
        if (pipelineConfigHashMap == null) {
            pipelineConfigHashMap = new PipelineConfigMap(cruiseConfig);
        }
    }

    public void onPipelineConfigChange(PipelineConfig pipelineConfig) {
        initMaterialConfigMap();
        getDependencies().put(pipelineConfig.name(), pipelineConfig.getDependenciesAsNode());
        pipelineConfigHashMap.update(pipelineConfig);
        materialConfigsFingerprintMap.update(pipelineConfig);
    }

    public Node getDependencyMaterialsFor(CaseInsensitiveString pipelineName) {
        return getDependencies().get(pipelineName) != null? getDependencies().get(pipelineName): new Node(new ArrayList<Node.DependencyNode>());
    }

    private Hashtable<CaseInsensitiveString, Node> getDependencies(){
        initPipelineConfigMap();
        initDependencies();
        return dependencies;
    }

    private void initDependencies() {
        if(dependencies == null){
            dependencies = new Hashtable<>();
            for (CaseInsensitiveString pipeline : pipelineConfigHashMap.map.keySet()) {
                dependencies.put(pipeline, pipelineConfigHashMap.getPipelineConfig(pipeline.toString()).getDependenciesAsNode());
            }
        }
    }

    public Set<CaseInsensitiveString> getPipelinesWithDependencyMaterials() {
        return getDependencies().keySet();
    }

    private class PipelineConfigMap {
        private Map<CaseInsensitiveString, PipelineConfig> map = new ConcurrentHashMap<>();

        public PipelineConfigMap(CruiseConfig cruiseConfig) {
            PipelineGroups groups = cruiseConfig.getGroups();
            for (PipelineConfigs group : groups) {
                for (PipelineConfig pipelineConfig : group) {
                    updatePipelineData(pipelineConfig);
                }
            }
        }

        private void updatePipelineData(PipelineConfig pipelineConfig) {
            this.map.put(pipelineConfig.name(), pipelineConfig);
        }

        public PipelineConfig getPipelineConfig(String pipelineName) {
            if (map.containsKey(new CaseInsensitiveString(pipelineName)))
                return map.get(new CaseInsensitiveString(pipelineName));
            return null;
        }

        public void update(PipelineConfig pipelineConfig) {
            updatePipelineData(pipelineConfig);
        }
    }

    private class MaterialConfigFingerprintMap {
        private Map<String, MaterialConfigs> map = new ConcurrentHashMap<>();
        private Map<String, MaterialConfigs> pipelineMaterialMap = new ConcurrentHashMap<>();

        public MaterialConfigFingerprintMap(CruiseConfig cruiseConfig) {
            for (PipelineConfigs group : cruiseConfig.getGroups()) {
                for (PipelineConfig pipelineConfig : group) {
                    for (MaterialConfig material : pipelineConfig.materialConfigs()) {
                        String fingerprint = material.getFingerprint();
                        if (!map.containsKey(fingerprint)) {
                            map.put(fingerprint, new MaterialConfigs());
                        }
                        map.get(fingerprint).add(material);

                        if (!pipelineMaterialMap.containsKey(pipelineConfig.name().toString())) {
                            pipelineMaterialMap.put(pipelineConfig.name().toString(), new MaterialConfigs());
                        }
                        pipelineMaterialMap.get(pipelineConfig.name().toString()).add(material);
                    }
                }
            }
        }

        public void update(PipelineConfig pipelineConfig) {
            MaterialConfigs knownMaterialsForPipeline = pipelineMaterialMap.get(pipelineConfig.name().toString());
            MaterialConfigs currentMaterials = pipelineConfig.materialConfigs();
            if(knownMaterialsForPipeline!=null){
                for (MaterialConfig old : knownMaterialsForPipeline) {
                    map.get(old.getFingerprint()).remove(old);
                }
            }
            for (MaterialConfig currentMaterial : currentMaterials) {
                if (!map.containsKey(currentMaterial.getFingerprint())) {
                    map.put(currentMaterial.getFingerprint(), new MaterialConfigs());
                }
                map.get(currentMaterial.getFingerprint()).add(currentMaterial);
            }
            pipelineMaterialMap.put(pipelineConfig.name().toString(), currentMaterials);
        }

        public MaterialConfigs get(String fingerprint) {
            return map.get(fingerprint);
        }
    }
}
