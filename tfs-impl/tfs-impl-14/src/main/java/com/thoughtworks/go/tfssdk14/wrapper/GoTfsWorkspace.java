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
package com.thoughtworks.go.tfssdk14.wrapper;

import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;

import java.io.IOException;

public class GoTfsWorkspace {
    private final Workspace workspace;

    public GoTfsWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void deleteWorkingFolder(WorkingFolder workingFolder) throws IOException {
        this.workspace.deleteWorkingFolder(workingFolder);
    }

    public void get(GetRequest request, GetOptions forceGetAll) {
        this.workspace.get(request, forceGetAll);
    }

    public boolean isLocalPathMapped(String canonicalPath) {
        return this.workspace.isLocalPathMapped(canonicalPath);
    }

    public void createWorkingFolder(WorkingFolder workingFolder) {
        this.workspace.createWorkingFolder(workingFolder);
    }
}
