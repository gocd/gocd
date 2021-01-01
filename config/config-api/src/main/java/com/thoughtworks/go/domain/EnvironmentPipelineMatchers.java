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
package com.thoughtworks.go.domain;

import java.util.Collection;

/**
 * @understands matching job with agents in situations where either, both or none are in environment
 */
public class EnvironmentPipelineMatchers extends BaseCollection<EnvironmentPipelineMatcher> {

    public EnvironmentPipelineMatchers(Collection<EnvironmentPipelineMatcher> referencedEnvironments) {        
        addAll(referencedEnvironments);
    }

    public EnvironmentPipelineMatchers() {}

    public boolean match(String pipelineName, String uuid) {
        for(EnvironmentPipelineMatcher matcher : this) {
            if (matcher.match(pipelineName, uuid)) {
                return true;
            }
        }
        return !pipelineReferenced(pipelineName) && !agentReferenced(uuid);
    }

    private boolean agentReferenced(String uuid) {
        for(EnvironmentPipelineMatcher matcher : this) {
            if (matcher.hasAgent(uuid)) {
                return true;
            }
        }
        return false;
    }

    private boolean pipelineReferenced(String pipelineName) {
        for(EnvironmentPipelineMatcher matcher : this) {
            if (matcher.hasPipeline(pipelineName)) {
                return true;
            }
        }
        return false;
    }
}
