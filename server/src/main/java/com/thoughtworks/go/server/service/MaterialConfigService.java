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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.server.service.result.OperationResult;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

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
			result.unauthorized("Unauthorized", "Do not have view permission to this material", HealthStateType.general(HealthStateScope.GLOBAL));
			return null;
		}

		return materialConfig;
	}
}
