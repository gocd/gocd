/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.tfssdk14;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.*;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.domain.materials.ModifiedFile;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.tfs.AbstractTfsCommand;
import com.thoughtworks.go.tfssdk14.wrapper.GoTfsVersionControlClient;
import com.thoughtworks.go.tfssdk14.wrapper.GoTfsWorkspace;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.CommandArgument;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TfsSDKCommand extends AbstractTfsCommand {

    private final SystemEnvironment systemEnvironment;

    private GoTfsVersionControlClient client;
    private TFSTeamProjectCollection collection;

    public TfsSDKCommand(String materialFingerprint, CommandArgument url, String domain, String userName, String password, String workspace, String projectPath) {
        super(materialFingerprint, url, domain, userName, password, workspace, projectPath);
        systemEnvironment = new SystemEnvironment();
    }

    @TestOnly
    TfsSDKCommand(GoTfsVersionControlClient client, TFSTeamProjectCollection collection, String materialFingerprint, CommandArgument url, String domain, String userName, String password, String workspace, String projectPath) {
        this(materialFingerprint, url, domain, userName, password, workspace, projectPath);
        this.client = client;
        this.collection = collection;
    }

    @Override
    protected void unMap(File workDir) throws IOException {
        GoTfsWorkspace localWorkspace = client.findLocalWorkspace(workDir);
        if (localWorkspace != null) {
            localWorkspace.deleteWorkingFolder(new WorkingFolder(getProjectPath(), workDir.toString()));
        }

        GoTfsWorkspace remoteWorkspace = client.queryWorkspace(getWorkspace(), getUserName());
        if (remoteWorkspace != null) {
            client.deleteWorkspace(remoteWorkspace);
        }
    }

    @Override
    protected void initializeWorkspace(File workDir) {
        GoTfsWorkspace workspace = initAndGetWorkSpace();
        mapWorkingDirectory(workspace, workDir);
    }

    protected void deleteWorkspace() {
        LOGGER.debug("[TFS SDK] Deleting TFS workspace {} for user {} ", getWorkspace(), getUserName());
        GoTfsWorkspace workspace = client.queryWorkspace(getWorkspace(), getUserName());
        client.deleteWorkspace(workspace);
    }

    @Override
    protected void retrieveFiles(File workDir, Revision revision) {
        LOGGER.debug("[TFS SDK] Getting Files for TFS workspace {} for user {} ", getWorkspace(), getUserName());
        GoTfsWorkspace workspace = client.queryWorkspace(getWorkspace(), getUserName());
        ItemSpec spec = new ItemSpec(getCanonicalPath(workDir), RecursionType.FULL);
        VersionSpec versionSpec = getVersionSpec(revision);
        GetRequest request = new GetRequest(spec, versionSpec);
        if (FileUtil.isFolderEmpty(workDir)) {
            workspace.get(request, GetOptions.GET_ALL);
        } else {
            workspace.get(request, GetOptions.NONE);
        }
    }

    private VersionSpec getVersionSpec(Revision revision) {
        if (revision == null) {
            return LatestVersionSpec.INSTANCE;
        }
        return new ChangesetVersionSpec(Integer.parseInt(revision.getRevision()));
    }


    GoTfsWorkspace initAndGetWorkSpace() {
        String workspaceName = getWorkspace();
        GoTfsWorkspace[] workspaces = client.queryWorkspaces(workspaceName, getUserName());
        if (workspaces.length == 0) {
            return createWorkspace();
        }
        return workspaces[0];
    }

    private GoTfsWorkspace createWorkspace() {
        LOGGER.debug("[TFS SDK] Creating workspace {} ", getWorkspace());
        return client.createWorkspace(getWorkspace());
    }

    @Override
    public List<Modification> history(String latestRevision, long revsToLoad) {
        LOGGER.debug("[TFS SDK] History for Server: {}, Project Path: {}, Latest Revision: {}, RevToLoad: {}", getUrl(), getProjectPath(), latestRevision, revsToLoad);
        Changeset[] changesets = retrieveChangeset(latestRevision, (int) revsToLoad);
        List<Modification> modifications = new ArrayList<>();
        for (Changeset changeset : changesets) {
            Modification modification = new Modification(changeset.getCommitter(), changeset.getComment(), null, changeset.getDate().getTime(), String.valueOf(changeset.getChangesetID()));
            modification.setModifiedFiles(getModifiedFiles(changeset));
            modifications.add(modification);
        }
        return modifications;

    }


    List<ModifiedFile> getModifiedFiles(Changeset changeset) {
        List<ModifiedFile> files = new ArrayList<>();
        for (Change change : changeset.getChanges()) {
            ModifiedFile modifiedFile = new ModifiedFile(change.getItem().getServerItem(), "", ModifiedAction.unknown);
            files.add(modifiedFile);
        }
        return files;
    }

    private Changeset[] retrieveChangeset(String latestRevision, int revsToLoad) {
        ChangesetVersionSpec uptoRevision = initAndGetUptoVersionSpec(latestRevision);
        return client.queryHistory(getProjectPath(), uptoRevision, revsToLoad);
    }

    private ChangesetVersionSpec initAndGetUptoVersionSpec(String latestRevision) {
        ChangesetVersionSpec uptilRevision;
        if (latestRevision == null) {
            uptilRevision = null;
        } else {
            uptilRevision = new ChangesetVersionSpec(Integer.parseInt(latestRevision));
        }
        return uptilRevision;
    }

    private void mapWorkingDirectory(GoTfsWorkspace workspace, File workDir) {
        LOGGER.debug("[TFS SDK] Mapping Folder: {}, Workspace: {}, Username: {}", workDir, getWorkspace(), getUserName());
        if (!workspace.isLocalPathMapped(getCanonicalPath(workDir))) {
            WorkingFolder workingFolder = new WorkingFolder(
                    getProjectPath(),
                    getCanonicalPath(workDir));
            workspace.createWorkingFolder(workingFolder);
        }
    }

    public void init() {
        LOGGER.debug("[TFS SDK] Init TFS Collection & Client for URL: {}, Username: {}, Domain: {}", getUrl(), getUserName(), getDomain());
        try {
            collection = new TFSTeamProjectCollection(getUri(), createCredentials());
            collection.getHTTPClient().getParams().setSoTimeout(systemEnvironment.getTfsSocketTimeout());
            client = new GoTfsVersionControlClient(collection.getVersionControlClient());
        } catch (Exception e) {
            throw new RuntimeException(String.format("Unable to connect to TFS Collection %s " + e, getUrl().toString()), e);
        }
    }

    private UsernamePasswordCredentials createCredentials() {
        return new UsernamePasswordCredentials(usernameWithDomain(), getPassword());
    }

    public void destroy() {
        LOGGER.debug("[TFS SDK] Destroying TFS Collection & Client for URL: {}, Username: {}, Domain: {}", getUrl(), getUserName(), getDomain());
        closeClient();
        closeCollection();
    }

    private void closeCollection() {
        if (collection != null) {
            try {
                collection.close();
            } catch (Exception ignore) {
            }
            collection = null;
        }
    }

    private void closeClient() {
        if (client != null) {
            try {
                client.close();
            } catch (Exception ignore) {
            }
            client = null;
        }

    }

    private static String getCanonicalPath(File workDir) {
        try {
            return workDir.getCanonicalPath();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
