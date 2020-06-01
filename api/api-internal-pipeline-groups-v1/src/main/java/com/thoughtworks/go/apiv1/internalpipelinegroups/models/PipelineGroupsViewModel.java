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

package com.thoughtworks.go.apiv1.internalpipelinegroups.models;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.EnvironmentsConfig;
import com.thoughtworks.go.config.PipelineTemplateConfig;
import com.thoughtworks.go.domain.PipelineGroups;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

@Data
@Accessors(chain = true)
public class PipelineGroupsViewModel {
    private PipelineGroups pipelineGroups;
    private EnvironmentsConfig environmentsConfig;
    private Map<CaseInsensitiveString, PipelineTemplateConfig> templateConfigMap;
    private Map<CaseInsensitiveString, EnvironmentConfig> pipelineEnvironmentMap;

    public PipelineGroupsViewModel(PipelineGroups pipelineGroups, EnvironmentsConfig environmentsConfig) {
        this.pipelineGroups = pipelineGroups;
        this.environmentsConfig = environmentsConfig;
        this.pipelineEnvironmentMap = pipelineEnvironmentMap(environmentsConfig);
    }

    private Map<CaseInsensitiveString, EnvironmentConfig> pipelineEnvironmentMap(EnvironmentsConfig environmentConfigs) {
        HashMap<CaseInsensitiveString, EnvironmentConfig> map = new HashMap<>();

        environmentConfigs.forEach(environmentConfig -> {
            environmentConfig.getPipelineNames().forEach(name -> map.put(name, environmentConfig));
        });
        return map;
    }

    public EnvironmentConfig environmentFor(CaseInsensitiveString pipelineName) {
        return pipelineEnvironmentMap.get(pipelineName);
    }
}
