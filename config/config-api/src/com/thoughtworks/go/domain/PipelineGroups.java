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

package com.thoughtworks.go.domain;

import java.util.*;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.PipelineGroupNotFoundException;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.util.Pair;

@ConfigCollection(value = BasicPipelineConfigs.class)
public class PipelineGroups extends BaseCollection<PipelineConfigs> implements Validatable {
    private final ConfigErrors configErrors = new ConfigErrors();
    private Map<String, List<Pair<PipelineConfig, PipelineConfigs>>> packageToPipelineMap;
    private Map<String, List<Pair<PipelineConfig, PipelineConfigs>>> pluggableSCMMaterialToPipelineMap;

    public PipelineGroups() {
    }

    public PipelineGroups(PipelineConfigs... configses) {
        for (PipelineConfigs configs : configses) {
            this.add(configs);
        }
    }

    public void update(String groupName, String pipelineName, PipelineConfig pipeline) {
        for (PipelineConfigs pipelines : this) {
            pipelines.update(groupName, pipeline, pipelineName);
        }
    }

    public void addPipeline(String groupName, PipelineConfig pipeline) {
        String sanitizedGroupName = BasicPipelineConfigs.sanitizedGroupName(groupName);
        if (!this.hasGroup(sanitizedGroupName)) {
            createNewGroup(sanitizedGroupName, pipeline);
            return;
        }
        for (PipelineConfigs pipelines : this) {
            if (pipelines.save(pipeline, sanitizedGroupName)) {
                return;
            }
        }
    }

    public void addPipelineWithoutValidation(String groupName, PipelineConfig pipelineConfig) {
        if (!this.hasGroup(groupName)) {
            createNewGroup(groupName, pipelineConfig);
        } else {
            PipelineConfigs group = findGroup(groupName);
            group.addWithoutValidation(pipelineConfig);
        }
    }

    public void deletePipeline(PipelineConfig pipelineConfig) {
        for (PipelineConfigs group : this) {
            if(group.hasPipeline(pipelineConfig.name())){
                group.remove(pipelineConfig);
                return;
            }
        }
    }

    private void createNewGroup(String sanitizedGroupName, PipelineConfig pipeline) {
        PipelineConfigs configs = new BasicPipelineConfigs(pipeline);
        configs.setGroup(sanitizedGroupName);
        this.add(0, configs);
    }


    public PipelineConfigs findGroup(String groupName) {
        for (PipelineConfigs pipelines : this) {
            if (pipelines.isNamed(groupName)) {
                return pipelines;
            }
        }
        throw new PipelineGroupNotFoundException("Failed to find the group [" + groupName + "]");
    }

    public boolean hasGroup(String groupName) {
        try {
            findGroup(groupName);
            return true;
        } catch (PipelineGroupNotFoundException e) {
            return false;
        }
    }

    public PipelineConfig findPipeline(String groupName, int pipelineIndex) {
        return findGroup(groupName).get(pipelineIndex);
    }

    public void accept(PipelineGroupVisitor visitor) {
        for (PipelineConfigs group : this) {
            visitor.visit(group);
        }
    }

    public String findGroupNameByPipeline(CaseInsensitiveString pipelineName) {
        for (PipelineConfigs group : this) {
            if (group.hasPipeline(pipelineName)) {
                return group.getGroup();
            }
        }
        return null;
    }

    public void validate(ValidationContext validationContext) {
        Map<String, PipelineConfigs> nameToConfig = new HashMap<>();
        List<PipelineConfigs> visited = new ArrayList();
        for (PipelineConfigs group : this) {
            group.validateNameUniqueness(nameToConfig);
        }
        validatePipelineNameUniqueness();
    }

    public void validatePipelineNameUniqueness() {
        List<PipelineConfig> visited = new ArrayList<>();
        HashMap<CaseInsensitiveString, Set<String>> duplicates = new HashMap<>();
        for (PipelineConfigs group : this) {
            for (PipelineConfig pipeline : group) {
                for (PipelineConfig visitedPipeline : visited) {
                    if (visitedPipeline.name().equals(pipeline.name())) {
                        if(!duplicates.containsKey(pipeline.name())){
                            duplicates.put(pipeline.name(), new HashSet<String>());
                        }
                        duplicates.get(pipeline.name()).add(pipeline.getOriginDisplayName());
                        duplicates.get(pipeline.name()).add(visitedPipeline.getOriginDisplayName());
                        pipeline.errors().remove(PipelineConfig.NAME);
                        pipeline.addError(PipelineConfig.NAME, String.format("You have defined multiple pipelines named '%s'. Pipeline names must be unique. Source(s): %s", pipeline.name(), duplicates.get(pipeline.name())));
                        visitedPipeline.errors().remove(PipelineConfig.NAME);
                        visitedPipeline.addError(PipelineConfig.NAME, String.format("You have defined multiple pipelines named '%s'. Pipeline names must be unique. Source(s): %s", pipeline.name(), duplicates.get(pipeline.name())));
                    }
                }
                visited.add(pipeline);
            }
        }
    }

    public ConfigErrors errors() {
        return configErrors;
    }

    public void addError(String fieldName, String message) {
        configErrors.add(fieldName, message);
    }

    public Set<MaterialConfig> getAllUniquePostCommitSchedulableMaterials() {
        Set<MaterialConfig> materialConfigs = new HashSet<>();
        Set<String> uniqueMaterials = new HashSet<>();
        for (PipelineConfigs pipelineConfigs : this) {
            for (PipelineConfig pipelineConfig : pipelineConfigs) {
                for (MaterialConfig materialConfig : pipelineConfig.materialConfigs()) {
                    if ((materialConfig instanceof ScmMaterialConfig || materialConfig instanceof PluggableSCMMaterialConfig)
                            && !materialConfig.isAutoUpdate()
                            && !uniqueMaterials.contains(materialConfig.getFingerprint())) {
                        materialConfigs.add(materialConfig);
                        uniqueMaterials.add(materialConfig.getFingerprint());
                    }
                }
            }
        }
        return materialConfigs;
    }

    public Map<String, List<Pair<PipelineConfig, PipelineConfigs>>> getPackageUsageInPipelines() {
        if (packageToPipelineMap == null) {
            synchronized (this) {
                if (packageToPipelineMap == null) {
                    packageToPipelineMap = new HashMap<>();
                    for (PipelineConfigs pipelineConfigs : this) {
                        for (PipelineConfig pipelineConfig : pipelineConfigs) {
                            for (PackageMaterialConfig packageMaterialConfig : pipelineConfig.packageMaterialConfigs()) {
                                String packageId = packageMaterialConfig.getPackageId();
                                if (!packageToPipelineMap.containsKey(packageId)) {
                                    packageToPipelineMap.put(packageId, new ArrayList<Pair<PipelineConfig, PipelineConfigs>>());
                                }
                                packageToPipelineMap.get(packageId).add(new Pair<>(pipelineConfig, pipelineConfigs));
                            }
                        }
                    }
                }
            }
        }
        return packageToPipelineMap;
    }

    public boolean canDeletePackageRepository(PackageRepository packageRepository) {
        Map<String, List<Pair<PipelineConfig, PipelineConfigs>>> packageUsageInPipelines = getPackageUsageInPipelines();
        for (PackageDefinition packageDefinition : packageRepository.getPackages()) {
            if (packageUsageInPipelines.containsKey(packageDefinition.getId())) {
                return false;
            }
        }
        return true;
    }

    public Map<String, List<Pair<PipelineConfig, PipelineConfigs>>> getPluggableSCMMaterialUsageInPipelines() {
        if (pluggableSCMMaterialToPipelineMap == null) {
            synchronized (this) {
                if (pluggableSCMMaterialToPipelineMap == null) {
                    pluggableSCMMaterialToPipelineMap = new HashMap<>();
                    for (PipelineConfigs pipelineConfigs : this) {
                        for (PipelineConfig pipelineConfig : pipelineConfigs) {
                            for (PluggableSCMMaterialConfig pluggableSCMMaterialConfig : pipelineConfig.pluggableSCMMaterialConfigs()) {
                                String scmId = pluggableSCMMaterialConfig.getScmId();
                                if (!pluggableSCMMaterialToPipelineMap.containsKey(scmId)) {
                                    pluggableSCMMaterialToPipelineMap.put(scmId, new ArrayList<Pair<PipelineConfig, PipelineConfigs>>());
                                }
                                pluggableSCMMaterialToPipelineMap.get(scmId).add(new Pair<>(pipelineConfig, pipelineConfigs));
                            }
                        }
                    }
                }
            }
        }
        return pluggableSCMMaterialToPipelineMap;
    }

    public boolean canDeletePluggableSCMMaterial(SCM scmConfig) {
        Map<String, List<Pair<PipelineConfig, PipelineConfigs>>> packageUsageInPipelines = getPluggableSCMMaterialUsageInPipelines();
        if (packageUsageInPipelines.containsKey(scmConfig.getId())) {
            return false;
        }
        return true;
    }

    public PipelineGroups getLocal() {
        PipelineGroups locals = new PipelineGroups();
        for(PipelineConfigs pipelineConfigs : this)
        {
            PipelineConfigs local = pipelineConfigs.getLocal();
            if(local != null)
                locals.add(local);
        }
        return locals;
    }
}
