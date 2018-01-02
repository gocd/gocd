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

import com.thoughtworks.go.domain.buildcause.BuildCause;

public class Assignment {
    private JobInstance job;
    private final Pipeline pipeline;
    private final Stage stageByBuild;

    public Assignment(Pipeline pipeline, Stage stageByBuild, JobInstance job) {
        this.pipeline = pipeline;
        this.stageByBuild = stageByBuild;
        this.job = job;
    }

    public Pipeline pipeline() {
        return pipeline;
    }

    public String stageName() {
        return stageByBuild.getName();
    }

    public JobInstance build() {
        return job;
    }

    public BuildCause getBuildCause() {
        return pipeline.getBuildCause();
    }


    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Assignment that = (Assignment) o;

        if (job != null ? !job.equals(that.job) : that.job != null) {
            return false;
        }
        if (pipeline != null ? !pipeline.equals(that.pipeline) : that.pipeline != null) {
            return false;
        }
        if (stageByBuild != null ? !stageByBuild.equals(that.stageByBuild) : that.stageByBuild != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (job != null ? job.hashCode() : 0);
        result = 31 * result + (pipeline != null ? pipeline.hashCode() : 0);
        result = 31 * result + (stageByBuild != null ? stageByBuild.hashCode() : 0);
        return result;
    }
}
