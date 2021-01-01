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
package com.thoughtworks.go.plugin.configrepo.contract.material;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.plugin.configrepo.contract.ErrorCollection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CRDependencyMaterial extends CRMaterial {
    public static final String TYPE_NAME = "dependency";
    @SerializedName("pipeline")
    @Expose
    private String pipeline;
    @SerializedName("stage")
    @Expose
    private String stage;
    @SerializedName("ignore_for_scheduling")
    @Expose
    private boolean ignoreForScheduling;

    public CRDependencyMaterial() {
        type = TYPE_NAME;
    }

    public CRDependencyMaterial(String name, String pipelineName, String stageName, boolean ignoreForScheduling) {
        super(TYPE_NAME, name);
        this.pipeline = pipelineName;
        this.stage = stageName;
        this.ignoreForScheduling = ignoreForScheduling;
    }

    public CRDependencyMaterial(String pipelineName, String stageName, boolean ignoreForScheduling) {
        type = TYPE_NAME;
        this.pipeline = pipelineName;
        this.stage = stageName;
        this.ignoreForScheduling = ignoreForScheduling;
    }

    @Override
    public String typeName() {
        return TYPE_NAME;
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location, "pipeline", pipeline);
        errors.checkMissing(location, "stage", stage);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String name = getName() == null ? "" : getName();
        String pipe = getPipeline() != null ? getPipeline() : "unknown pipeline";
        String stage = getStage() != null ? getStage() : "unknown stage";
        return String.format("%s; Dependency material %s on %s/%s", myLocation, name, pipe, stage);
    }
}
