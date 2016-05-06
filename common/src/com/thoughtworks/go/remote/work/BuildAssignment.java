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

package com.thoughtworks.go.remote.work;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.builder.Builder;
import com.thoughtworks.go.domain.JobPlan;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;

public class BuildAssignment implements Serializable {
    private final File buildWorkingDirectory;
    private final List<Builder> builders;
    private final JobPlan plan;
    private final EnvironmentVariableContext initialContext = new EnvironmentVariableContext();
    private final MaterialRevisions materialRevisions = new MaterialRevisions();
    private final String approver;

    private BuildAssignment(BuildCause buildCause, File buildWorkingDirectory, List<Builder> builder, JobPlan plan) {
        this.buildWorkingDirectory = buildWorkingDirectory;
        this.builders = builder;
        this.plan = plan;
        for (MaterialRevision materialRevision : buildCause.getMaterialRevisions()) {
            ArrayList<Modification> modifications = new ArrayList<>();
            for (Modification modification : materialRevision.getModifications()) {
                modifications.add(new Modification(modification, false));
            }
            materialRevisions.addRevision(new MaterialRevision(materialRevision.getMaterial(), materialRevision.isChanged(), modifications));
        }
        approver = buildCause.getApprover();
    }

    @Override
    public String toString() {
        return "BuildAssignment{" +
                "plan=" + plan +
                ", materialRevisions=" + materialRevisions +
                ", approver='" + approver + '\'' +
                '}';
    }

    public JobPlan getPlan() {
        return plan;
    }

    public static BuildAssignment create(JobPlan plan, BuildCause buildCause, List<Builder> builder, File file) {
        return new BuildAssignment(buildCause, file, builder, plan);
    }

    public MaterialRevisions materialRevisions() {
        return materialRevisions;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BuildAssignment that = (BuildAssignment) o;

        if (materialRevisions != null ? !materialRevisions.equals(that.materialRevisions) : that.materialRevisions != null) {
            return false;
        }
        if (approver != null ? !approver.equals(that.approver) : that.approver != null) {
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
        result = 31 * result + (materialRevisions != null ? materialRevisions.hashCode() : 0);
        result = 31 * result + (approver != null ? approver.hashCode() : 0);
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
        return approver;
    }
}
