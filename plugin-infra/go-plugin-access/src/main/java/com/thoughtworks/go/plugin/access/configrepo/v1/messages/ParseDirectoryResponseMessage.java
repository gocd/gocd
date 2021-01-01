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
package com.thoughtworks.go.plugin.access.configrepo.v1.messages;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.plugin.configrepo.contract.CREnvironment;
import com.thoughtworks.go.plugin.configrepo.contract.CRError;
import com.thoughtworks.go.plugin.configrepo.contract.CRPipeline;
import com.thoughtworks.go.plugin.configrepo.contract.ErrorCollection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ParseDirectoryResponseMessage {
    @Expose
    @SerializedName("target_version")
    private String targetVersion;
    @Expose
    @SerializedName("environments")
    private Collection<CREnvironment> environments = new ArrayList<>();
    @Expose
    @SerializedName("pipelines")
    private Collection<CRPipeline> pipelines = new ArrayList<>();
    @Expose
    @SerializedName("errors")
    private List<CRError> errors = new ArrayList<>();

    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public void setTargetVersion(String targetVersion) {
        this.targetVersion = targetVersion;
    }

    public void validateResponse(ErrorCollection errors) {
        String location = "Plugin response message";
        errors.checkMissing(location, "target_version", targetVersion);
        for (CRPipeline pipeline : pipelines) {
            pipeline.getErrors(errors, location);
        }
        for (CREnvironment environment : environments) {
            environment.getErrors(errors, location);
        }
    }

    public Collection<CREnvironment> getEnvironments() {
        return environments;
    }

    public void setEnvironments(Collection<CREnvironment> environments) {
        this.environments = environments;
    }

    public Collection<CRPipeline> getPipelines() {
        return pipelines;
    }

    public void setPipelines(Collection<CRPipeline> pipelines) {
        this.pipelines = pipelines;
    }

    public List<CRError> getPluginErrors() {
        return errors;
    }
}
