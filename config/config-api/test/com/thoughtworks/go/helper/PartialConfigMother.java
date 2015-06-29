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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.Authorization;
import com.thoughtworks.go.config.BasicPipelineConfigs;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.domain.PipelineGroups;

/**
 * Created by tomzo on 6/13/15.
 */
public class PartialConfigMother {
    public static PartialConfig empty() {
        return  new PartialConfig();
    }
    public static PartialConfig withPipeline(String name) {
        PipelineConfig pipe = PipelineConfigMother.pipelineConfig(name);
        BasicPipelineConfigs pipes = new BasicPipelineConfigs(pipe);
        return new PartialConfig(new PipelineGroups(pipes));
    }

    public static PartialConfig withPipelineInGroup(String pipelineName, String groupName) {
        PipelineConfig pipe = PipelineConfigMother.pipelineConfig(pipelineName);
        BasicPipelineConfigs pipes = new BasicPipelineConfigs(groupName,new Authorization(),pipe);
        return new PartialConfig(new PipelineGroups(pipes));
    }
}
