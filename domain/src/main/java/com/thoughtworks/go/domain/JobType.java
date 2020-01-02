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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.JobConfig;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.server.service.InstanceFactory;
import com.thoughtworks.go.util.Clock;

/**
 * @understands how to match job instances associated with different types of JobConfigs
 */
public interface JobType {
    boolean isInstanceOf(String jobInstanceName, boolean ignoreCase, String jobConfigName);

    void createJobInstances(JobInstances jobs, SchedulingContext context, JobConfig jobConfig, String stageName, final JobNameGenerator nameGenerator, final Clock clock,
                            InstanceFactory instanceFactory);

    void createRerunInstances(JobInstance oldJob, JobInstances jobInstances, SchedulingContext context, StageConfig stageConfig, final Clock clock, InstanceFactory instanceFactory);

    static interface JobNameGenerator {
        String generateName(int counter);
    }
}
