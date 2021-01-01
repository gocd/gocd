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
package com.thoughtworks.go.domain.materials.dependency;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.GoConfigMother;


public class NewGoConfigMother {
    GoConfigMother mother = new GoConfigMother();
    private CruiseConfig config = new BasicCruiseConfig();

    public NewGoConfigMother() {
    }

    public CruiseConfig cruiseConfig() {
        return config;
    }

    public PipelineConfig addPipeline(String pipelineName, String stageName, String... buildNames) throws Exception {
        return mother.addPipeline(config, pipelineName, stageName, buildNames);
    }

    public PipelineConfig addPipeline(String pipelineName, String stageName, String buildName, MaterialConfig materialConfig) {
        return mother.addPipeline(config, pipelineName, stageName, new MaterialConfigs(materialConfig), buildName);
    }
}
