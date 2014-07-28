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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
		List<String> groupNames = goConfigService.allGroups();
		for (String groupName : groupNames) {
			if (securityService.hasViewPermissionForGroup(username, groupName)) {
				PipelineConfigs pipelinesInGroup = goConfigService.getAllPipelinesInGroup(groupName);
				for (PipelineConfig pipelineConfig : pipelinesInGroup) {
					MaterialConfigs materialsForPipeline = pipelineConfig.materialConfigs();
					materialConfigs.addAll(materialsForPipeline);
				}
			}
		}
		return materialConfigs;
	}
}
