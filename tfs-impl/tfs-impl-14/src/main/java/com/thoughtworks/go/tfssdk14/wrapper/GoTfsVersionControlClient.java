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

import com.microsoft.tfs.core.clients.versioncontrol.VersionControlClient;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Workspace;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;

import java.io.File;
import java.util.ArrayList;

import static com.microsoft.tfs.core.clients.versioncontrol.WorkspaceLocation.SERVER;

public class GoTfsVersionControlClient {
    private final VersionControlClient client;

    public GoTfsVersionControlClient(VersionControlClient client) {
        this.client = client;
    }

    public GoTfsWorkspace queryWorkspace(String workspaceName, String userName) {
        Workspace workspace = client.queryWorkspace(workspaceName, userName);
        return workspace == null ? null : new GoTfsWorkspace(workspace);
    }

    public GoTfsWorkspace findLocalWorkspace(File workDir) {
        Workspace workspace = client.getLocalWorkspace(workDir.getAbsolutePath(), false);
        return workspace == null ? null : new GoTfsWorkspace(workspace);
    }

    public void deleteWorkspace(GoTfsWorkspace workspace) {
        client.deleteWorkspace(workspace.getWorkspace());
    }

    public GoTfsWorkspace[] queryWorkspaces(String workspaceName, String userName) {
        ArrayList<GoTfsWorkspace> goTfsWorkspaces = new ArrayList<>();
        Workspace[] workspaces = client.queryWorkspaces(workspaceName, userName, null);
        for (Workspace workspace : workspaces) {
            goTfsWorkspaces.add(new GoTfsWorkspace(workspace));
        }
        return goTfsWorkspaces.toArray(new GoTfsWorkspace[]{});
    }

    public GoTfsWorkspace createWorkspace(String workspace) {
        return new GoTfsWorkspace(client.createWorkspace(null, workspace, null, SERVER, null, null));
    }

    public Changeset[] queryHistory(String projectPath, ChangesetVersionSpec uptoRevision, int revsToLoad) {
        return client.queryHistory(projectPath, LatestVersionSpec.INSTANCE, 0, RecursionType.FULL, null, null,
                uptoRevision, revsToLoad, true, true, false, false);
    }

    public void close() {
        client.close();
    }

}
