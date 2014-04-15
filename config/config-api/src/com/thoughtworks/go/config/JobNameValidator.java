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

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.validation.GoConfigValidator;
import com.thoughtworks.go.domain.JobConfigVisitor;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

/**
 * @understands enforcing RunOnAllAgents job name restrictions
 */
public class JobNameValidator implements GoConfigValidator {

    public void validate(CruiseConfig cruiseConfig) throws Exception {
        cruiseConfig.accept(new JobConfigVisitor() {
            public void visit(PipelineConfig pipelineConfig, StageConfig stageConfig, JobConfig jobConfig) {
                if (RunOnAllAgentsJobTypeConfig.hasMarker(CaseInsensitiveString.str(jobConfig.name()))) {
                    throw bomb("A job cannot have 'runOnAll' in it's name: " + jobConfig.name());
                }
            }
        });
    }
}
