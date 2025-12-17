/*
 * Copyright Thoughtworks, Inc.
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

import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.server.dao.PipelineDao;
import com.thoughtworks.go.server.domain.PipelineTimeline;

import java.util.Map;
import java.util.Set;
import java.util.function.IntSupplier;

record FanInGraphContext(
    Map<String, MaterialConfig> fingerprintScmMaterialMap,
    PipelineTimeline pipelineTimeline,
    Map<DependencyMaterialConfig, Set<MaterialConfig>> pipelineScmDepMap,
    Map<String, DependencyMaterialConfig> fingerprintDepMaterialMap,
    PipelineDao pipelineDao,
    IntSupplier maxBackTrackLimit
) {
    boolean isDependencyMaterial(String fingerprint) {
        return fingerprintDepMaterialMap.containsKey(fingerprint);
    }

    boolean isScmMaterial(String fingerprint) {
        return fingerprintScmMaterialMap.containsKey(fingerprint);
    }
}
