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
package com.thoughtworks.go.plugin.configrepo.contract;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.plugin.configrepo.contract.tasks.CRTask;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CRJob extends CRBase {
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("environment_variables")
    @Expose
    private Collection<CREnvironmentVariable> environmentVariables = new ArrayList<>();
    @SerializedName("tabs")
    @Expose
    private Collection<CRTab> tabs = new ArrayList<>();
    @SerializedName("resources")
    @Expose
    private Collection<String> resources = new ArrayList<>();
    @SerializedName("artifacts")
    @Expose
    private Collection<CRArtifact> artifacts = new ArrayList<>();
    @SerializedName("elastic_profile_id")
    @Expose
    private String elasticProfileId;
    @SerializedName("run_instance_count")
    @Expose
    private String runInstanceCount;
    @SerializedName("timeout")
    @Expose
    private int timeout = 0;

    @SerializedName("tasks")
    @Expose
    private List<CRTask> tasks = new ArrayList<>();

    public CRJob() {
    }

    public CRJob(String name) {
        this.name = name;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = this.getLocation(parentLocation);
        errors.checkMissing(location, "name", name);
        validateEnvironmentVariableUniqueness(errors, location);
        validateTabs(errors, location);
        validateArtifacts(errors, location);
        validateTasks(errors, location);
        validateElasticProfile(errors, location);
    }

    private void validateElasticProfile(ErrorCollection errors, String location) {
        if (elasticProfileId != null) {
            if (this.resources != null && this.resources.size() > 0) {
                errors.addError(location, "elastic_profile_id cannot be specified together with resources");
            }
        }
    }

    private void validateTasks(ErrorCollection errors, String location) {
        errors.checkMissing(location, "tasks", tasks);
        if (tasks != null)
            for (CRTask task : tasks) {
                task.getErrors(errors, location);
            }
    }

    private void validateArtifacts(ErrorCollection errors, String location) {
        if (artifacts == null)
            return;
        for (CRArtifact artifact : artifacts) {
            artifact.getErrors(errors, location);
        }
    }

    private void validateTabs(ErrorCollection errors, String location) {
        if (tabs == null)
            return;
        for (CRTab tab : tabs) {
            tab.getErrors(errors, location);
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

    public void addTask(CRTask task) {
        tasks.add(task);
    }

    public void addEnvironmentVariable(String key, String value) {
        CREnvironmentVariable variable = new CREnvironmentVariable(key);
        variable.setValue(value);
        this.environmentVariables.add(variable);
    }

    public void addEnvironmentVariable(CREnvironmentVariable variable) {
        this.environmentVariables.add(variable);
    }

    public boolean hasEnvironmentVariable(String key) {
        for (CREnvironmentVariable var : environmentVariables) {
            if (var.getName().equals(key)) {
                return true;
            }
        }
        return false;
    }

    public void addArtifact(CRArtifact artifact) {
        this.artifacts.add(artifact);
    }

    public boolean isRunOnAllAgents() {
        return runInstanceCount != null && runInstanceCount.equalsIgnoreCase("all");
    }

    public void setRunOnAllAgents(boolean runOnAllAgents) {
        if (runOnAllAgents)
            this.runInstanceCount = "all";
        else
            this.runInstanceCount = null;
    }

    public Integer getRunInstanceCount() {
        if (runInstanceCount == null)
            return null;
        return Integer.parseInt(runInstanceCount);
    }

    public void setRunInstanceCount(int runInstanceCount) {
        this.runInstanceCount = Integer.toString(runInstanceCount);
    }

    public void addResource(String resource) {
        this.resources.add(resource);
    }

    public void addTab(CRTab tab) {
        this.tabs.add(tab);
    }

    public String validateNameUniqueness(HashSet<String> names) {
        if (names.contains(this.getName()))
            return String.format("Job %s is defined more than once", this.getName());
        else
            names.add(this.getName());
        return null;
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String stage = getName() == null ? "unknown name" : getName();
        return String.format("%s; Job (%s)", myLocation, stage);
    }

}
