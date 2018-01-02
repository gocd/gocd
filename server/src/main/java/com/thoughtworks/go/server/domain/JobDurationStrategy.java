/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.domain;

import com.thoughtworks.go.domain.JobInstance;

/**
 * @understands how to calculate the expected duration of a job
 */
public interface JobDurationStrategy {

    JobDurationStrategy ALWAYS_ZERO = new ConstantJobDuration(0);

    class ConstantJobDuration implements JobDurationStrategy {
        private final int durationInMillis;

        public ConstantJobDuration(int durationInMillis) {
            this.durationInMillis = durationInMillis;
        }

        public long getExpectedDurationMillis(String pipelineName, String stageName, JobInstance job) {
            return durationInMillis;
        }
    }


    /**
     * TODO: We want to use org.joda.time.Duration here but we have a class loading issue that means we can't
     * We should nove to a richer object when we can fix that issue
     *
     * @return duration in millis
     */
    long getExpectedDurationMillis(String pipelineName, String stageName, JobInstance job);

}
