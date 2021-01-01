/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.exceptions.NotAuthorizedException;
import com.thoughtworks.go.config.exceptions.RecordNotFoundException;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

import static java.util.stream.Collectors.toList;

/**
 * @understands providing services around a pipeline configuration
 */
@Service
public class MaterialConfigService {
    private final GoConfigService goConfigService;
    private final SecurityService securityService;

    @Autowired
    public MaterialConfigService(GoConfigService goConfigService, SecurityService securityService) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
    }

    public MaterialConfigs getMaterialConfigs(String username) {
        MaterialConfigs materialConfigs = new MaterialConfigs();
        Set<String> materialFingerprints = new HashSet<>();
        for (PipelineConfigs pipelineGroup : goConfigService.groups()) {
            if (securityService.hasViewPermissionForGroup(username, pipelineGroup.getGroup())) {
                for (PipelineConfig pipelineConfig : pipelineGroup) {
                    for (MaterialConfig currentMaterialConfig : pipelineConfig.materialConfigs()) {
                        if (!materialFingerprints.contains(currentMaterialConfig.getFingerprint())) {
                            materialConfigs.add(currentMaterialConfig);
                            materialFingerprints.add(currentMaterialConfig.getFingerprint());
                        }
                    }
                }
            }
        }
        return materialConfigs;
    }

    public MaterialConfig getMaterialConfig(String username, String materialFingerprint, OperationResult result) {
        MaterialConfig materialConfig = null;
        boolean hasViewPermissionForMaterial = false;
        for (PipelineConfigs pipelineGroup : goConfigService.groups()) {
            boolean hasViewPermissionForGroup = securityService.hasViewPermissionForGroup(username, pipelineGroup.getGroup());
            for (PipelineConfig pipelineConfig : pipelineGroup) {
                for (MaterialConfig currentMaterialConfig : pipelineConfig.materialConfigs()) {
                    if (currentMaterialConfig.getFingerprint().equals(materialFingerprint)) {
                        materialConfig = currentMaterialConfig;
                        if (hasViewPermissionForGroup) {
                            hasViewPermissionForMaterial = true;
                            break;
                        }
                    }
                }
            }
        }
        if (materialConfig == null) {
            result.notFound("Not Found", "Material not found", HealthStateType.general(HealthStateScope.GLOBAL));
            return null;
        }

        if (!hasViewPermissionForMaterial) {
            result.forbidden("Unauthorized", "Do not have view permission to this material", HealthStateType.general(HealthStateScope.GLOBAL));
            return null;
        }

        return materialConfig;
    }

    public List<String> getUsagesForMaterial(String username, String fingerprint) {
        return goConfigService.groups()
                .stream()
                .filter((grp) -> securityService.hasViewPermissionForGroup(username, grp.getGroup()))
                .flatMap((grp) -> grp.getPipelines()
                        .stream()
                        .filter((pipeline) -> pipeline.materialConfigs().getByMaterialFingerPrint(fingerprint) != null))
                .map((pipeline) -> pipeline.name().toString())
                .collect(toList());
    }

    public Map<MaterialConfig, Boolean> getMaterialConfigsWithPermissions(String username) {
        Map<MaterialConfig, Boolean> materialConfigs = new HashMap<>();
        Map<String, Boolean> materialFingerprints = new HashMap<>();
        goConfigService.groups()
                .stream()
                .filter((grp) -> securityService.hasViewPermissionForGroup(username, grp.getGroup()))
                .forEach((grp) -> {
                    grp.forEach((pipelineConfig) -> {
                        boolean hasOperatePermission = securityService.hasOperatePermissionForGroup(new CaseInsensitiveString(username), grp.getGroup());
                        pipelineConfig.materialConfigs()
                                .forEach((materialConfig) -> {
                                    if (!materialFingerprints.containsKey(materialConfig.getFingerprint())) {
                                        materialFingerprints.put(materialConfig.getFingerprint(), hasOperatePermission);
                                        materialConfigs.put(materialConfig, hasOperatePermission);
                                    } else {
                                        Boolean existingValue = materialFingerprints.get(materialConfig.getFingerprint());
                                        materialConfigs.replace(materialConfig, existingValue || hasOperatePermission);
                                    }
                                });
                    });
                });
        return materialConfigs;
    }


    public MaterialConfig getMaterialConfig(String username, String materialFingerprint) {
        MaterialConfig materialConfig = null;
        boolean hasViewPermissionForMaterial = false;
        boolean hasOperatePermissionForMaterial = false;
        for (PipelineConfigs pipelineGroup : goConfigService.groups()) {
            boolean hasViewPermissionForGroup = securityService.hasViewPermissionForGroup(username, pipelineGroup.getGroup());
            boolean hasOperatePermissionForGroup = securityService.hasOperatePermissionForGroup(new CaseInsensitiveString(username), pipelineGroup.getGroup());
            for (PipelineConfig pipelineConfig : pipelineGroup) {
                for (MaterialConfig currentMaterialConfig : pipelineConfig.materialConfigs()) {
                    if (currentMaterialConfig.getFingerprint().equals(materialFingerprint)) {
                        materialConfig = currentMaterialConfig;
                        hasViewPermissionForMaterial = hasViewPermissionForMaterial || hasViewPermissionForGroup;
                        if (hasOperatePermissionForGroup) {
                            hasOperatePermissionForMaterial = hasOperatePermissionForGroup;
                            break;
                        }
                    }
                }
            }
        }
        if (materialConfig == null) {
            throw new RecordNotFoundException("Material not found");
        }

        if (!hasViewPermissionForMaterial) {
            throw new NotAuthorizedException("Do not have view permission to this material");
        }

        if (!hasOperatePermissionForMaterial) {
            throw new NotAuthorizedException("Do not have permission to trigger this material");
        }

        return materialConfig;
    }
}
