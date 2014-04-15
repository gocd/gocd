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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.util.command.UrlArgument;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * @understands
 */
@XStreamAlias("workspace")
public class Workspace {
    @XStreamAsAttribute
    private String name;

    @XStreamAsAttribute
    private String owner;

    @XStreamAsAttribute
    private String computer;

    @XStreamAsAttribute
    private String comment;

    @XStreamAsAttribute
    @XStreamAlias("server")
    private String url;

    @XStreamAlias("working-folder")
    @XStreamImplicit
    private List<WorkingFolder> workingFolders;


    public Workspace(String name, String owner, String computer, String comment, String url, List<WorkingFolder> workingFolders) {
        this.name = name;
        this.owner = owner;
        this.computer = computer;
        this.comment = comment;
        this.url = url;
        this.workingFolders = workingFolders;
    }

    public Workspace(String name, String owner, String computer, String comment, UrlArgument url) {
        this(name, owner, computer, comment, url.forCommandline(), new ArrayList<WorkingFolder>());
    }

    public Workspace(String workspaceName) {
        this(workspaceName, "", "", "", new UrlArgument(""));
    }

    public Workspace() {
        this("");
    }

    public boolean matchesName(String workspaceName) {
        return name != null ? name.equals(workspaceName) : workspaceName == null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Workspace workspace = (Workspace) o;

        if (comment != null ? !comment.equals(workspace.comment) : workspace.comment != null) {
            return false;
        }
        if (computer != null ? !computer.toLowerCase().equals(workspace.computer.toLowerCase()) : workspace.computer != null) {
            return false;
        }
        if (hasDifferentAttributes(workspace)) {
            return false;
        }
        return !(workingFolders != null ? !workingFolders.equals(workspace.workingFolders) : workspace.workingFolders != null);

    }

    private boolean hasDifferentAttributes(Workspace workspace) {
        if (name != null ? !name.equals(workspace.name) : workspace.name != null) {
            return true;
        }
        if (owner != null ? !owner.equals(workspace.owner) : workspace.owner != null) {
            return true;
        }
        if (url != null ? !url.equals(workspace.url) : workspace.url != null) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (computer != null ? computer.hashCode() : 0);
        result = 31 * result + (comment != null ? comment.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (workingFolders != null ? workingFolders.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return "Workspace{" +
                "name='" + name + '\'' +
                ", owner='" + owner + '\'' +
                ", computer='" + computer + '\'' +
                ", comment='" + comment + '\'' +
                ", url='" + url + '\'' +
                ", workingFolder=" + workingFolders +
                '}';
    }

    public String getName() {
        return name;
    }


    public boolean hasWorkFolder(File workingDir) throws IOException {
        if (workingFolders == null) {
            return false;
        }
        for (WorkingFolder workingFolder : workingFolders) {
            if (workingFolder.matchesLocalDir(workingDir)) {
                return true;
            }

        }
        return false;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public void setComputer(String computer) {
        this.computer = computer;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setUrl(String server) {
        this.url = server;
    }

    public void addWorkingFolder(WorkingFolder workingFolder) {
        this.workingFolders.add(workingFolder);
    }
}
