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

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.domain.PipelineTimelineEntry;
import com.thoughtworks.go.domain.materials.MaterialConfig;

public class ReportingRootFanInNode extends ReportingFanInNode {
    private static List<Class<? extends MaterialConfig>> ROOT_NODE_TYPES = new ArrayList<>();
    PipelineTimelineEntry.Revision scmRevision;

    static {
        ROOT_NODE_TYPES.add(ScmMaterialConfig.class);
        ROOT_NODE_TYPES.add(PackageMaterialConfig.class);
    }

    ReportingRootFanInNode(MaterialConfig material) {
        super(material);
        for (Class<? extends MaterialConfig> clazz : ROOT_NODE_TYPES) {
            if (clazz.isAssignableFrom(material.getClass())) {
                return;
            }
        }
        throw new RuntimeException("Not a valid root node material type");
    }
}
