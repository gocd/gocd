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
package com.thoughtworks.go.dto;

/**
 * It's at times like this that I get sick of Java
 */
public class DurationBean {
    private Long jobId;
    private long duration;

    public DurationBean() {
    }

    public DurationBean(Long jobId, long duration) {
        this.jobId = jobId;
        this.duration = duration;
    }

    public DurationBean(long jobId) {
        this(jobId, 0L);
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getDuration() {
        return duration;
    }

    public Long getJobId() {
        return jobId;
    }
}
