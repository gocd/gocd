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

package com.thoughtworks.go.remote.work;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

public class BuildAssignment implements Serializable {
    private final File buildWorkingDirectory;
    private final List<Builder> builders;
    private final JobPlan plan;
    private final BuildCause buildCause;
    private final EnvironmentVariableContext initialContext = new EnvironmentVariableContext();

    private BuildAssignment(BuildCause buildCause, File buildWorkingDirectory, List<Builder> builder, JobPlan plan) {
        this.buildCause = buildCause;
        this.buildWorkingDirectory = buildWorkingDirectory;
        this.builders = builder;
        this.plan = plan;
    }

    public String toString() {
        return "plan: [" + plan + "] buildCause: ["+ buildCause + "]";
    }

    public JobPlan getPlan() {
        return plan;
    }

    public static BuildAssignment create(JobPlan plan, BuildCause buildCause, List<Builder> builder, File file) {
        return new BuildAssignment(buildCause, file, builder, plan);
    }

    public MaterialRevisions materialRevisions() {
        return buildCause.getMaterialRevisions();
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BuildAssignment that = (BuildAssignment) o;

        if (buildCause != null ? !buildCause.equals(that.buildCause) : that.buildCause != null) {
            return false;
        }
        if (plan != null ? !plan.equals(that.plan) : that.plan != null) {
            return false;
        }
        if (buildWorkingDirectory != null ? !buildWorkingDirectory.equals(
                that.buildWorkingDirectory) : that.buildWorkingDirectory != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (plan != null ? plan.hashCode() : 0);
        result = 31 * result + (buildWorkingDirectory != null ? buildWorkingDirectory.hashCode() : 0);
        result = 31 * result + (buildCause != null ? buildCause.hashCode() : 0);
        return result;
    }

    public File getWorkingDirectory() {
        return buildWorkingDirectory;
    }

    public List<Builder> getBuilders() {
        return builders;
    }

    public void enhanceEnvironmentVariables(EnvironmentVariableContext context) {
        initialContext.addAll(context);
    }

    public EnvironmentVariableContext initialEnvironmentVariableContext() {
        return initialContext;
    }

    public String getBuildApprover(){
        return buildCause.getApprover();
    }
}
