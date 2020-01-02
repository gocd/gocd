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
    ConcurrentSkipListSet<CaseInsensitiveString> triggeredPipelines = new ConcurrentSkipListSet<>();

    public boolean isAlreadyTriggered(CaseInsensitiveString pipelineName) {
        return triggeredPipelines.contains(pipelineName);
    }

    public boolean markPipelineAsAlreadyTriggered(PipelineConfig pipelineConfig) {
        return markPipelineAsAlreadyTriggered(pipelineConfig.name());
    }

    public boolean markPipelineAsAlreadyTriggered(CaseInsensitiveString pipelineName) {
        return triggeredPipelines.add(pipelineName);
    }

    public void markPipelineAsCanBeTriggered(PipelineConfig pipelineConfig) {
        triggeredPipelines.remove(pipelineConfig.name());
    }

    public void clear_for_test() {
        triggeredPipelines.clear();
    }
}
