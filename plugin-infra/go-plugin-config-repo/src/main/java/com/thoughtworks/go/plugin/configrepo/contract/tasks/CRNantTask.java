/*
 * Copyright 2018 ThoughtWorks, Inc.
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

public class CRNantTask extends CRBuildTask {
    private String nant_path;

    public CRNantTask(String type, String buildFile, String target, String workingDirectory, String nantPath) {
        super(type, buildFile, target, workingDirectory);
        this.nant_path = nantPath;
    }

    public CRNantTask(CRRunIf runIf, CRTask onCancel, String buildFile, String target, String workingDirectory,String nantPath) {
        super(runIf, onCancel, buildFile, target, workingDirectory, CRBuildFramework.nant);
        this.nant_path = nantPath;
    }

    public String getNantPath() {
        return nant_path;
    }

    public void setNantPath(String nantPath) {
        this.nant_path = nantPath;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        CRNantTask buildTask = (CRNantTask)o;
        if(buildTask == null)
            return  false;

        if(!super.equals(buildTask))
            return false;

        if (nant_path != null ? !nant_path.equals(buildTask.nant_path) : buildTask.nant_path != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (nant_path != null ? nant_path.hashCode() : 0);
        return result;
    }
}