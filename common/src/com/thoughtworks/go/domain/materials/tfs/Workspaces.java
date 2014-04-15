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

package com.thoughtworks.go.domain.materials.tfs;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
* @understands
*/
@XStreamAlias("workspaces")
public class Workspaces {
    @XStreamImplicit
    private List<Workspace> workspaces;

    public Workspaces(List<Workspace> workspaces) {
        this.workspaces = workspaces;
    }

    public Workspaces(){
        this(new ArrayList<Workspace>());
    }

    public boolean hasWorkspace(String workspaceName) {
        return getWorkspace(workspaceName) != null;
    }

    public void add(Workspace workspace) {
        workspaces.add(workspace);
    }

    public Workspace getWorkspace(String workspaceName) {
        if (workspaces == null) { return null; }
        for (Workspace workspace : workspaces) {
            if (workspace.matchesName(workspaceName)) {
                return workspace;
            }
        }
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Workspaces that = (Workspaces) o;

        if (workspaces != null ? !workspaces.equals(that.workspaces) : that.workspaces != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return workspaces != null ? workspaces.hashCode() : 0;
    }

    @Override public String toString() {
        return "Workspaces{" +
                "workspaces=" + workspaces +
                '}';
    }

    public List<Workspace> getWorkspaces() {
        return workspaces;
    }

    public Workspace last() {
        return workspaces.get(workspaces.size() - 1);
    }
}
