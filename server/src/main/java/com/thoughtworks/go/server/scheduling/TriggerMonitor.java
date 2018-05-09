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

package com.thoughtworks.go.server.scheduling;

import java.util.concurrent.ConcurrentSkipListSet;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import org.springframework.stereotype.Component;

/**
 * @understands what is currently being schedule
 */
@Component
public class TriggerMonitor {
    ConcurrentSkipListSet<String> triggeredPipelines = new ConcurrentSkipListSet<>();

    public boolean isAlreadyTriggered(String pipelineName) {
        return triggeredPipelines.contains(pipelineName.toLowerCase());
    }

    public boolean markPipelineAsAlreadyTriggered(PipelineConfig pipelineConfig) {
        String s = CaseInsensitiveString.str(pipelineConfig.name());
        return markPipelineAsAlreadyTriggered(s);
    }

    public boolean markPipelineAsAlreadyTriggered(String pipelineName) {
        return triggeredPipelines.add(pipelineName.toLowerCase());
    }

    public void markPipelineAsCanBeTriggered(PipelineConfig pipelineConfig) {
        triggeredPipelines.remove(pipelineConfig.name().toLower());
    }

    public void clear_for_test() {
        triggeredPipelines.clear();
    }
}
