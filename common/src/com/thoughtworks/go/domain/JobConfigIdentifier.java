/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;

public class JobConfigIdentifier {
    private final String pipelineName;
    private final String stageName;
    private final String jobName;

    public JobConfigIdentifier(String pipelineName, String stageName, String jobName) {
        this.pipelineName = pipelineName;
        this.stageName = stageName;
        this.jobName = jobName;
    }

    public JobConfigIdentifier(CaseInsensitiveString pipelineName, CaseInsensitiveString stageName, CaseInsensitiveString jobName) {
        this(pipelineName.toString(), stageName.toString(), jobName.toString());
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JobConfigIdentifier that = (JobConfigIdentifier) o;

        if (jobName != null ? !jobName.equals(that.jobName) : that.jobName != null) {
            return false;
        }
        if (pipelineName != null ? !pipelineName.equals(that.pipelineName) : that.pipelineName != null) {
            return false;
        }
        if (stageName != null ? !stageName.equals(that.stageName) : that.stageName != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (pipelineName != null ? pipelineName.hashCode() : 0);
        result = 31 * result + (stageName != null ? stageName.hashCode() : 0);
        result = 31 * result + (jobName != null ? jobName.hashCode() : 0);
        return result;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getStageName() {
        return stageName;
    }

    public String getJobName() {
        return jobName;
    }

    @Override public String toString() {
        return "JobConfigIdentifier[" + pipelineName + ":" + stageName + ":" + jobName + "]";
    }
}
