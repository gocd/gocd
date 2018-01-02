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

package com.thoughtworks.go.server.service.dd.reporting;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.util.Pair;

public class ReportingFanInGraph {

    private final CruiseConfig cruiseConfig;
    private CaseInsensitiveString rootName;

    private final List<ReportingFanInNode> nodes = new ArrayList<>();
    private final Map<String, MaterialConfig> fingerprintScmMaterialMap = new HashMap<>();
    private final Map<String, DependencyMaterialConfig> fingerprintDepMaterialMap = new HashMap<>();
    private final Map<DependencyMaterialConfig, Set<String>> dependencyMaterialFingerprintMap = new HashMap<>();

    private ReportingDependencyFanInNode root;
    private final PipelineDao pipelineDao;

    public ReportingFanInGraph(CruiseConfig cruiseConfig, String root, PipelineDao pipelineDao) {
        this.cruiseConfig = cruiseConfig;
        rootName = new CaseInsensitiveString(root);
        this.pipelineDao = pipelineDao;

        PipelineConfig target = cruiseConfig.pipelineConfigByName(rootName);
        this.root = new ReportingDependencyFanInNode(new DependencyMaterialConfig(target.name(), target.get(0).name()));

        buildGraph(target);
    }

    private void buildGraph(PipelineConfig target) {
        nodes.add(this.root);
        final HashSet<String> scmMaterials = new HashSet<>();
        buildRestOfTheGraph(this.root, target, scmMaterials);
        dependencyMaterialFingerprintMap.put((DependencyMaterialConfig) this.root.materialConfig, scmMaterials);
    }

    private void buildRestOfTheGraph(ReportingDependencyFanInNode root, PipelineConfig target, HashSet<String> scmMaterialSet) {
        for (MaterialConfig material : target.materialConfigs()) {
            ReportingFanInNode node = createNode(material);
            root.children.add(node);
            node.parents.add(root);
            if (node instanceof ReportingDependencyFanInNode) {
                DependencyMaterialConfig dependencyMaterial = (DependencyMaterialConfig) material;
                fingerprintDepMaterialMap.put(dependencyMaterial.getFingerprint(), dependencyMaterial);
                handleDependencyMaterial(scmMaterialSet, dependencyMaterial, (ReportingDependencyFanInNode) node);
            } else {
                handleScmMaterial(scmMaterialSet, material);
            }
        }
    }

    private void handleScmMaterial(HashSet<String> scmMaterialSet, MaterialConfig material) {
        final String fingerprint = material.getFingerprint();
        scmMaterialSet.add(fingerprint);
        fingerprintScmMaterialMap.put(fingerprint, material);
    }

    private void handleDependencyMaterial(HashSet<String> scmMaterialSet, DependencyMaterialConfig depMaterial, ReportingDependencyFanInNode node) {
        final HashSet<String> scmMaterialFingerprintSet = new HashSet<>();
        buildRestOfTheGraph(node, cruiseConfig.pipelineConfigByName(depMaterial.getPipelineName()), scmMaterialFingerprintSet);
        scmMaterialFingerprintSet.addAll(scmMaterialFingerprintSet);
        dependencyMaterialFingerprintMap.put(depMaterial, scmMaterialFingerprintSet);
        scmMaterialSet.addAll(scmMaterialFingerprintSet);
    }

    private ReportingFanInNode createNode(MaterialConfig material) {
        ReportingFanInNode node = getNodeIfExists(material);
        if (node == null) {
            node = ReportingFanInNodeFactory.create(material);
            nodes.add(node);
        }
        return node;
    }

    private ReportingFanInNode getNodeIfExists(MaterialConfig material) {
        int i = nodes.indexOf(ReportingFanInNodeFactory.create(material));
        return i == -1 ? null : nodes.get(i);
    }

    public Map<DependencyMaterialConfig, Set<MaterialConfig>> getPipelineScmDepMap() {
        Map<DependencyMaterialConfig, Set<MaterialConfig>> dependencyMaterialListMap = new HashMap<>();

        for (Map.Entry<DependencyMaterialConfig, Set<String>> materialSetEntry : dependencyMaterialFingerprintMap.entrySet()) {
            HashSet<MaterialConfig> scmMaterials = new HashSet<>();
            for (String fingerprint : materialSetEntry.getValue()) {
                scmMaterials.add(fingerprintScmMaterialMap.get(fingerprint));
            }
            dependencyMaterialListMap.put(materialSetEntry.getKey(), scmMaterials);
        }

        return dependencyMaterialListMap;
    }

    public String computeRevisions(PipelineTimeline pipelineTimeline) {
        Pair<List<ReportingRootFanInNode>, List<ReportingDependencyFanInNode>> scmAndDepMaterialsChildren = getScmAndDepMaterialsChildren();
        List<ReportingDependencyFanInNode> depChildren = scmAndDepMaterialsChildren.last();

        if (depChildren.isEmpty()) {
            return "No Dependency Children,No Fan-in";
        }

        ReportingFanInGraphContext context = buildContext(pipelineTimeline);
        initRootNode(context);

        initChildren(depChildren, rootName, context);

        context.out.close();

        return context.sw.toString();
    }


    private Pair<List<ReportingRootFanInNode>, List<ReportingDependencyFanInNode>> getScmAndDepMaterialsChildren() {
        List<ReportingRootFanInNode> scmMaterials = new ArrayList<>();
        List<ReportingDependencyFanInNode> depMaterials = new ArrayList<>();
        for (ReportingFanInNode child : root.children) {
            if (child instanceof ReportingRootFanInNode) {
                scmMaterials.add((ReportingRootFanInNode) child);
            } else {
                depMaterials.add((ReportingDependencyFanInNode) child);
            }
        }
        return new Pair<>(scmMaterials, depMaterials);
    }


    private void initChildren(List<ReportingDependencyFanInNode> depChildren, CaseInsensitiveString pipelineName, ReportingFanInGraphContext context) {
        for (ReportingDependencyFanInNode child : depChildren) {
            context.out.printf("========================Child: %s========================\n", child.materialConfig);
            child.populateRevisions(context);
        }
    }

    private void initRootNode(ReportingFanInGraphContext context) {
        context.out.printf("========================Root Node: %s========================\n", root.materialConfig);
        root.populateRevisions(context);
    }


    private ReportingFanInGraphContext buildContext(PipelineTimeline pipelineTimeline) {
        ReportingFanInGraphContext context = new ReportingFanInGraphContext();
        context.pipelineTimeline = pipelineTimeline;
        context.fingerprintScmMaterialMap = fingerprintScmMaterialMap;
        context.pipelineScmDepMap = getPipelineScmDepMap();
        context.fingerprintDepMaterialMap = fingerprintDepMaterialMap;
        context.pipelineDao = pipelineDao;
        context.sw = new StringWriter();
        context.out = new PrintWriter(context.sw);
        return context;
    }


}
