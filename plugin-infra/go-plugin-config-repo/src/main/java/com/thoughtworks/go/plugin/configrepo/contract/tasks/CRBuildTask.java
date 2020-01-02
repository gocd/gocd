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
package com.thoughtworks.go.plugin.configrepo.contract.tasks;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.go.plugin.configrepo.contract.ErrorCollection;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CRBuildTask extends CRTask {
    public static CRBuildTask rake() {
        return new CRBuildTask(CRBuildFramework.rake);
    }

    public static CRBuildTask rake(String rakeFile) {
        return new CRBuildTask(CRBuildFramework.rake, null, null, rakeFile, null, null);
    }

    public static CRBuildTask rake(String rakeFile, String target) {
        return new CRBuildTask(CRBuildFramework.rake, null, null, rakeFile, target, null);
    }

    public static CRBuildTask rake(String rakeFile, String target, String workingDirectory) {
        return new CRBuildTask(CRBuildFramework.rake, null, null, rakeFile, target, workingDirectory);
    }

    public static CRBuildTask ant() {
        return new CRBuildTask(CRBuildFramework.ant, null, null, null, null, null);
    }

    public static CRBuildTask ant(String antFile) {
        return new CRBuildTask(CRBuildFramework.ant, null, null, antFile, null, null);
    }

    public static CRBuildTask ant(String antFile, String target) {
        return new CRBuildTask(CRBuildFramework.ant, null, null, antFile, target, null);
    }

    public static CRBuildTask ant(String antFile, String target, String workingDirectory) {
        return new CRBuildTask(CRBuildFramework.ant, null, null, antFile, target, workingDirectory);
    }

    public static CRNantTask nant() {
        return new CRNantTask(null, null, null, null, null, null);
    }

    public static CRNantTask nant(String nantPath) {
        return new CRNantTask(null, null, null, null, null, nantPath);
    }

    public static CRNantTask nant(String nantFile, String target) {
        return new CRNantTask(null, null, nantFile, target, null, null);
    }

    public static CRNantTask nant(String nantFile, String target, String workingDirectory) {
        return new CRNantTask(null, null, nantFile, target, workingDirectory, null);
    }

    public static CRNantTask nant(String nantFile, String target, String workingDirectory, String nantPath) {
        return new CRNantTask(null, null, nantFile, target, workingDirectory, nantPath);
    }

    public static CRBuildTask rake(CRRunIf runIf, CRTask onCancel, String buildFile, String target, String workingDirectory) {
        return new CRBuildTask(CRBuildFramework.rake, runIf, onCancel, buildFile, target, workingDirectory);
    }

    public static CRBuildTask ant(CRRunIf runIf, CRTask onCancel, String buildFile, String target, String workingDirectory) {
        return new CRBuildTask(CRBuildFramework.ant, runIf, onCancel, buildFile, target, workingDirectory);
    }

    @SerializedName("build_file")
    @Expose
    private String buildFile;
    @SerializedName("target")
    @Expose
    private String target;
    @SerializedName("working_directory")
    @Expose
    private String workingDirectory;

    public CRBuildTask(CRBuildFramework type) {
        this(type, null, null, null, null, null);
    }

    public CRBuildTask(CRBuildFramework type, CRRunIf runIf, CRTask onCancel, String buildFile, String target, String workingDirectory) {
        super(type != null ? type.toString() : null, runIf, onCancel);
        this.buildFile = buildFile;
        this.target = target;
        this.workingDirectory = workingDirectory;
    }

    public CRBuildFramework getType() {
        if (type == null)
            return null;
        return CRBuildFramework.valueOf(type);
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location, "type", type);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String type = this.getType() == null ? "unknown" : this.getType().toString();
        return String.format("%s; %s build task", myLocation, type);
    }

}