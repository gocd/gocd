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
package com.thoughtworks.go.server.service.dd;

import java.util.Map;
import java.util.Set;

import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.domain.PipelineTimeline;

public class FanInGraphContext {
    int revBatchCount;
    Map<String, MaterialConfig> fingerprintScmMaterialMap;
    PipelineTimeline pipelineTimeline;
    public Map<DependencyMaterialConfig, Set<MaterialConfig>> pipelineScmDepMap;
    public Map<String, DependencyMaterialConfig> fingerprintDepMaterialMap;
    public PipelineDao pipelineDao;
    public int maxBackTrackLimit;
}
