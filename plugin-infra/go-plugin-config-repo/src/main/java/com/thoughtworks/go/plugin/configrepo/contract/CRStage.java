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
package com.thoughtworks.go.plugin.configrepo.contract;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CRStage extends CRBase {
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("fetch_materials")
    @Expose
    private boolean fetchMaterials = true;
    @SerializedName("never_cleanup_artifacts")
    @Expose
    private boolean neverCleanupArtifacts;
    @SerializedName("clean_working_directory")
    @Expose
    private boolean cleanWorkingDirectory;
    @SerializedName("approval")
    @Expose
    private CRApproval approval;
    @SerializedName("environment_variables")
    @Expose
    private Collection<CREnvironmentVariable> environmentVariables = new ArrayList<>();
    @SerializedName("jobs")
    @Expose
    private Collection<CRJob> jobs = new ArrayList<>();

    public CRStage() {
    }

    public CRStage(String name) {
        this.name = name;
    }

    public void addEnvironmentVariable(String key, String value) {
        CREnvironmentVariable variable = new CREnvironmentVariable(key);
        variable.setValue(value);
        this.environmentVariables.add(variable);
    }

    public void addEnvironmentVariable(CREnvironmentVariable variable) {
        this.environmentVariables.add(variable);
    }

    private void validateJobNameUniqueness(ErrorCollection errors, String location) {
        if (this.jobs == null)
            return;
        HashSet<String> keys = new HashSet<>();
        for (CRJob var : jobs) {
            String error = var.validateNameUniqueness(keys);
            if (error != null)
                errors.addError(location, error);
        }
    }

    private void validateEnvironmentVariableUniqueness(ErrorCollection errors, String location) {
        HashSet<String> keys = new HashSet<>();
        for (CREnvironmentVariable var : environmentVariables) {
            String error = var.validateNameUniqueness(keys);
            if (error != null)
                errors.addError(location, error);
        }
    }

    private void validateAtLeastOneJob(ErrorCollection errors, String location) {
        if (this.jobs == null || this.jobs.isEmpty())
            errors.addError(location, "Stage has no jobs");
    }

    public boolean hasEnvironmentVariable(String key) {
        for (CREnvironmentVariable var : environmentVariables) {
            if (var.getName().equals(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);
        errors.checkMissing(location, "name", name);
        validateAtLeastOneJob(errors, location);
        validateEnvironmentVariableUniqueness(errors, location);
        validateJobNameUniqueness(errors, location);
        if (approval != null)
            approval.getErrors(errors, location);
        if (jobs != null) {
            for (CRJob job : jobs) {
                job.getErrors(errors, location);
            }
        }
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String stage = getName() == null ? "unknown name" : getName();
        return String.format("%s; Stage (%s)", myLocation, stage);
    }

    public String validateNameUniqueness(HashSet<String> keys) {
        if (keys.contains(this.getName()))
            return String.format("Stage named %s is defined more than once", this.getName());
        else
            keys.add(this.getName());
        return null;
    }

    public void addJob(CRJob crJob) {
        this.jobs.add(crJob);
    }
}
