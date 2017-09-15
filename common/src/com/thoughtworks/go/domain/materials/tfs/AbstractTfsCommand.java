/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.materials.tfs;


import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.SCMCommand;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.command.CommandArgument;
import org.apache.commons.io.FileUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTfsCommand extends SCMCommand implements TfsCommand {

    protected static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(AbstractTfsCommand.class);

    private final CommandArgument url;
    private final String domain;
    private final String userName;
    private final String password;
    private final String workspace;
    private final String projectPath;

    public AbstractTfsCommand(String materialFingerprint, CommandArgument url, String domain, String userName, String password, String workspace, String projectPath) {
        super(materialFingerprint);
        this.url = url;
        this.domain = domain;
        this.userName = userName;
        this.password = password;
        this.workspace = workspace;
        this.projectPath = projectPath;
    }

    protected CommandArgument getUrl() {
        return url;
    }

    protected String getUserName() {
        return userName;
    }

    protected String getPassword() {
        return password;
    }

    protected String getWorkspace() {
        return workspace;
    }

    protected String getProjectPath() {
        return projectPath;
    }

    protected String getDomain() {
        return domain;
    }

    @Override
    public final void checkout(File workDir, Revision revision) {
        try {
            if (workDir.exists()) {
                FileUtils.deleteQuietly(workDir);
            }
            setupWorkspace(workDir);
            LOGGER.debug("[TFS] Retrieving Files from Workspace {}, Working Folder {}, Revision {} ", workspace, workDir, revision);
            retrieveFiles(workDir, revision);
        } catch (Exception e) {
            String exceptionMessage = String.format("Failed while checking out into Working Folder: %s, Project Path: %s, Workspace: %s, Username: %s, Domain: %s, Root Cause: %s", workDir, projectPath,
                    workspace,
                    userName,
                    domain, e.getMessage());
            throw new RuntimeException(exceptionMessage, e);
        } finally {
            clearMapping(workDir);
        }
    }

    @Override
    public final List<Modification> latestModification(File workDir) {
        try {
            LOGGER.debug("[TFS] History for Workspace: {}, Working Folder: {}, Latest Revision: null, RevToLoad: 1", workspace, workDir);
            return latestInHistory();
        } catch (Exception e) {
            String message = String.format("[TFS] Failed while checking for latest modifications on Server: %s, Project Path: %s, Username: %s, Domain: %s", url,
                    projectPath,
                    userName,
                    domain);
            throw new RuntimeException(message, e);
        }
    }

    @Override
    public final List<Modification> modificationsSince(File workDir, Revision revision) {
        try {
            LOGGER.debug("[TFS] Modification check for Workspace: {}, Working Folder {}, Revision {} ", workspace, workDir, revision);
            return modificationsSinceRevInHistory(revision);
        } catch (Exception e) {
            String message = String.format("Failed while checking for modifications since revision %s on Server: %s, Project Path: %s, Username: %s, Domain: %s,"
                            + " Root Cause: %s",
                    revision.getRevision(),
                    url, projectPath,
                    userName,
                    domain, e.getMessage());
            throw new RuntimeException(message, e);
        }
    }

    @Override
    public final void checkConnection() {
        LOGGER.info("[TFS] Checking Connection: Server {}, Domain {}, User {}, Project Path {}", url, domain, userName, projectPath);
        try {
            List<Modification> modifications = latestInHistory();
            if (modifications.isEmpty()) {
                throw new IllegalStateException("There might be no commits on the project path, project path might be invalid or user may have insufficient permissions.");
            }
        } catch (Exception e) {
            String message = String.format("Failed while checking connection using Url: %s, Project Path: %s, Username: %s, Domain: %s, Root Cause: %s", url, projectPath,
                    userName,
                    domain, e.getMessage());
            throw new RuntimeException(message, e);
        }
    }

    private void setupWorkspace(File workDir) {
        LOGGER.debug("[TFS] Initializing Workspace {}, Working Folder {}", workspace, workDir);
        initializeWorkspace(workDir);
    }


    private void clearMapping(File workDir) {
        LOGGER.debug("[TFS] Unmapping Project Path {} for Workspace {}, User {} ", projectPath, workspace, userName);
        try {
            unMap(workDir);
        } catch (Exception e) {
            LOGGER.warn("[TFS] Unmapping Project Path {} failed for Workspace {}, User {}", projectPath, workspace, userName, e);
        }
    }

    protected abstract void unMap(File workDir) throws IOException;

    private List<Modification> modificationsSinceRevInHistory(Revision revision) {
        Modification latest = latestInHistory().get(0);
        long latestRev = Long.parseLong(latest.getRevision());
        long sinceRev = Long.parseLong(revision.getRevision());
        long numberOfModifications = latestRev - sinceRev;
        List<Modification> newMods = new ArrayList<>();
        if (numberOfModifications > 0) {
            List<Modification> history = history(latest.getRevision(), numberOfModifications);
            for (Modification listedMod : history) {
                String listedRev = listedMod.getRevision();
                long listedRevNumber = Long.parseLong(listedRev);
                if (listedRevNumber <= sinceRev) {
                    break;
                }
                newMods.add(listedMod);
            }
        }
        return newMods;
    }


    private List<Modification> latestInHistory() {
        return history(null, 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AbstractTfsCommand that = (AbstractTfsCommand) o;

        if (domain != null ? !domain.equals(that.domain) : that.domain != null) {
            return false;
        }
        if (password != null ? !password.equals(that.password) : that.password != null) {
            return false;
        }
        if (projectPath != null ? !projectPath.equals(that.projectPath) : that.projectPath != null) {
            return false;
        }
        if (url != null ? !url.equals(that.url) : that.url != null) {
            return false;
        }
        if (userName != null ? !userName.equals(that.userName) : that.userName != null) {
            return false;
        }
        if (workspace != null ? !workspace.equals(that.workspace) : that.workspace != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = url != null ? url.hashCode() : 0;
        result = 31 * result + (domain != null ? domain.hashCode() : 0);
        result = 31 * result + (userName != null ? userName.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        result = 31 * result + (workspace != null ? workspace.hashCode() : 0);
        result = 31 * result + (projectPath != null ? projectPath.hashCode() : 0);
        return result;
    }

    protected URI getUri() {
        try {
            return new URL(url.toString()).toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(String.format("[TFS] Failed when converting the url string to a uri: %s, Project Path: %s, Username: %s, Domain: %s", url, projectPath, userName, domain), e);
        } catch (MalformedURLException e) {
            throw new RuntimeException(String.format("[TFS] Failed when converting the url string to a uri: %s, Project Path: %s, Username: %s, Domain: %s", url, projectPath, userName, domain), e);
        }
    }

    protected String usernameWithDomain() {
        if (StringUtil.isBlank(domain)) {
            return getUserName();
        } else {
            return String.format("%s\\%s", getDomain(), getUserName());
        }
    }

    protected abstract List<Modification> history(final String beforeRevision, final long revsToLoad);

    protected abstract void retrieveFiles(File workDir, Revision revision);

    protected abstract void initializeWorkspace(File workDir);

}
