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

package com.thoughtworks.go.domain;

import org.apache.commons.lang.StringUtils;

/**
 *
 */
public class PipelineBuildingInfo {
    public Stages stages;

    public PipelineBuildingInfo() {
        this(new Stages());
    }

    public PipelineBuildingInfo(Stages stages) {
        this.stages = stages;
    }

    public boolean isBuildingInAnyPipeline(String name) {
        for (Stage stage : stages) {
            if (StringUtils.equals(stage.getName(), name)) {
                return true;
            }
        }
        return false;
    }
}
