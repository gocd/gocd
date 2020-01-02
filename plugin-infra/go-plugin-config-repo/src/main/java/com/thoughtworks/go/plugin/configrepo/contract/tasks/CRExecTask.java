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

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class CRExecTask extends CRTask {
    public static final String TYPE_NAME = "exec";

    @SerializedName("command")
    @Expose
    private String command;
    @SerializedName("working_directory")
    @Expose
    private String workingDirectory;
    @SerializedName("timeout")
    @Expose
    private long timeout;
    @SerializedName("arguments")
    @Expose
    private List<String> arguments = new ArrayList<>();

    public CRExecTask() {
        this(null, null, null, null, 0);
    }

    public CRExecTask(CRRunIf runIf, CRTask onCancel, String command, String workingDirectory, long timeout) {
        super(TYPE_NAME, runIf, onCancel);
        this.command = command;
        this.workingDirectory = workingDirectory;
        this.timeout = timeout;
    }

    public void addArgument(String arg) {
        this.arguments.add(arg);
    }

    @Override
    public void getErrors(ErrorCollection errors, String parentLocation) {
        String location = getLocation(parentLocation);
        errors.checkMissing(location, "command", command);
    }

    @Override
    public String getLocation(String parent) {
        String myLocation = getLocation() == null ? parent : getLocation();
        String command = getCommand() != null ? getCommand() : "unknown command";
        return String.format("%s; exec task (%s)", myLocation, command);
    }
}
