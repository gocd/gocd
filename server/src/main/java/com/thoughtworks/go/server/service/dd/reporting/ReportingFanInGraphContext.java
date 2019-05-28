/*
 * Copyright 2019 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.service.dd.reporting;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.Set;

import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.domain.PipelineTimeline;

public class ReportingFanInGraphContext {
    Map<String, MaterialConfig> fingerprintScmMaterialMap;
    PipelineTimeline pipelineTimeline;
    Map<DependencyMaterialConfig, Set<MaterialConfig>> pipelineScmDepMap;
    Map<String, DependencyMaterialConfig> fingerprintDepMaterialMap;
    StringWriter sw;
    PrintWriter out;
    PipelineDao pipelineDao;
}
