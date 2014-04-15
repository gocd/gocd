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

package com.thoughtworks.go.feature;

import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfigs;

public class ViewPermissionFeatureValidator extends NonPipelineFeatureValidator {

    public boolean support(EnterpriseFeature enterpriseFeature) {
        return EnterpriseFeature.VIEW_PERMISSION.equals(enterpriseFeature);
    }

    public boolean isValidFeature(CruiseConfig cruiseConfig) {
        return groupWithViewPermissions(cruiseConfig)==null;
    }

     private PipelineConfigs groupWithViewPermissions(CruiseConfig cruiseConfig){
        for (PipelineConfigs group : cruiseConfig.getGroups()) {
            if (group.hasViewPermissionDefined()) {
                return group;
            }
        }
        return null;
    }
}
