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

package com.thoughtworks.go.tfssdk;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.versioncontrol.GetOptions;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.GetRequest;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.RecursionType;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Changeset;
import com.microsoft.tfs.core.clients.versioncontrol.soapextensions.Change;
import com.microsoft.tfs.core.clients.versioncontrol.specs.ItemSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.ChangesetVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.LatestVersionSpec;
import com.microsoft.tfs.core.clients.versioncontrol.specs.version.VersionSpec;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.domain.materials.ModifiedFile;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.tfs.AbstractTfsCommand;
import com.thoughtworks.go.tfssdk.wrapper.GoTfsVersionControlClient;
import com.thoughtworks.go.tfssdk.wrapper.GoTfsWorkspace;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.CommandArgument;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TfsSDKCommand extends AbstractTfsCommand {

    private static final Logger LOGGER = Logger.getLogger(TfsSDKCommand.class);
    private GoTfsVersionControlClient client;
    private String domain;
    private TFSTeamProjectCollection collection;
    private SystemEnvironment systemEnvironment;

    public TfsSDKCommand(String materialFingerprint, CommandArgument url, String domain, String userName, String password, String workspace, String projectPath) {
        super(materialFingerprint, url, domain, userName, password, workspace, projectPath);
        this.domain = domain;
        systemEnvironment = new SystemEnvironment();
    }

    // Used in tests only
    TfsSDKCommand(GoTfsVersionControlClient client, TFSTeamProjectCollection collection, String materialFingerprint, CommandArgument url, String domain, String userName, String password, String workspace, String projectPath) {
        this(materialFingerprint, url, domain, userName, password, workspace, projectPath);
        this.client = client;
        this.collection = collection;
    }

    @Override protected void unMap() throws IOException {
        GoTfsWorkspace workspace = client.queryWorkspace(getWorkspace(), getUserName());
        if (workspace == null) {
            return;
        }
        workspace.deleteWorkingFolder(new WorkingFolder(getProjectPath(), null));
    }

    @Override protected void initializeWorkspace(File workDir) {
        GoTfsWorkspace workspace = initAndGetWorkSpace();
        mapWorkingDirectory(workspace, workDir);
    }

    protected void deleteWorkspace() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("[TFS SDK] Deleting TFS workspace %s for user %s ", getWorkspace(), getUserName()));
        }
        GoTfsWorkspace workspace = client.queryWorkspace(getWorkspace(), getUserName());
        client.deleteWorkspace(workspace);
    }

    @Override protected void retrieveFiles(File workDir, Revision revision) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("[TFS SDK] Getting Files for TFS workspace %s for user %s ", getWorkspace(), getUserName()));
        }
        GoTfsWorkspace workspace = client.queryWorkspace(getWorkspace(), getUserName());
        ItemSpec spec = new ItemSpec(FileUtil.getCanonicalPath(workDir), RecursionType.FULL);
        VersionSpec versionSpec = getVersionSpec(revision);
        GetRequest request = new GetRequest(spec, versionSpec);
        if (FileUtil.isFolderEmpty(workDir)) {
            workspace.get(request, GetOptions.FORCE_GET_ALL);
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
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("[TFS SDK] Creating workspace %s ", getWorkspace()));
        }
        return client.createWorkspace(getWorkspace());
    }

    @Override public List<Modification> history(String latestRevision, long revsToLoad) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("[TFS SDK] History for Server: %s, Project Path: %s, Latest Revision: %s, RevToLoad: %s",
                    getUrl(), getProjectPath(), latestRevision, revsToLoad));
        }
        Changeset[] changesets = retrieveChangeset(latestRevision, (int) revsToLoad);
        ArrayList<Modification> modifications = new ArrayList<>();
        for (Changeset changeset : changesets) {
            Modification modification = new Modification(changeset.getCommitter(), changeset.getComment(), null, changeset.getDate().getTime(), String.valueOf(changeset.getChangesetID()));
            modification.setModifiedFiles(getModifiedFiles(changeset));
            modifications.add(modification);
        }
        return modifications;

    }


    ArrayList<ModifiedFile> getModifiedFiles(Changeset changeset) {
        ArrayList<ModifiedFile> files = new ArrayList<>();
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
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("[TFS SDK] Mapping Folder: %s, Workspace: %s, Username: %s", workDir, getWorkspace(), getUserName()));
        }
        if (!workspace.isLocalPathMapped(FileUtil.getCanonicalPath(workDir))) {
            com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder workingFolder = new com.microsoft.tfs.core.clients.versioncontrol.soapextensions.WorkingFolder(
                    getProjectPath(),
                    FileUtil.getCanonicalPath(workDir));
            workspace.createWorkingFolder(workingFolder);
        }
    }

    public void init() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("[TFS SDK] Init TFS Collection & Client for URL: %s, Username: %s, Domain: %s", getUrl(), getUserName(), domain));
        }
        try {
            collection = new TFSTeamProjectCollection(getUrl().toString(), getUserName(), domain, getPassword());
            collection.getHTTPClient().getParams().setSoTimeout(systemEnvironment.getTfsSocketTimeout());
            client = new GoTfsVersionControlClient(collection.getVersionControlClient());
        } catch (Exception e) {
            throw new RuntimeException(String.format("Unable to connect to TFS Collection %s " + e, getUrl().toString()), e);
        }
    }

    public void destroy() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("[TFS SDK] Destroying TFS Collection & Client for URL: %s, Username: %s, Domain: %s", getUrl(), getUserName(), domain));
        }
        closeClient();
        closeCollection();
    }

    private void closeCollection() {
        try {
            if (collection != null) {
                collection.close();
                collection = null;
            }
        } catch (Exception e) {
        }
    }

    private void closeClient() {
        try {
            if (client != null) {
                client.close();
                client = null;
            }
        } catch (Exception e) {

        }
    }
}
