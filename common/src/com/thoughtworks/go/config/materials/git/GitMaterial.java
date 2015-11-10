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

package com.thoughtworks.go.config.materials.git;

import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.domain.materials.git.GitCommand;
import com.thoughtworks.go.domain.materials.git.GitMaterialInstance;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.domain.materials.svn.MaterialUrl;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.util.ExceptionUtils.*;
import static com.thoughtworks.go.util.FileUtil.createParentFolderIfNotExist;
import static com.thoughtworks.go.util.FileUtil.deleteDirectoryNoisily;
import static com.thoughtworks.go.util.FileUtil.deleteFolder;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.lang.String.format;

/**
 * Understands configuration for git version control
 */
public class GitMaterial extends ScmMaterial {
    private static final Logger LOG = Logger.getLogger(GitMaterial.class);

    private UrlArgument url;
    private String branch = GitMaterialConfig.DEFAULT_BRANCH;
    private String submoduleFolder;

    //TODO: use iBatis to set the type for us, and we can get rid of this field.
    public static final String TYPE = "GitMaterial";
    private static final Pattern GIT_VERSION_PATTERN = Pattern.compile(".*\\s+(\\d(\\.\\d)+).*");
    private static final String ERR_GIT_NOT_FOUND = "Failed to find 'git' on your PATH. Please ensure 'git' is executable by the Go Server and on the Go Agents where this material will be used.";
    public static final String ERR_GIT_OLD_VERSION = "Please install Git-core 1.6 or above. ";

    public GitMaterial(String url) {
        super(TYPE);
        this.url = new UrlArgument(url);
    }

    @Override
    public void checkout(File baseDir, Revision revision, SubprocessExecutionContext execCtx) {
        InMemoryStreamConsumer output = ProcessOutputStreamConsumer.inMemoryConsumer();
        this.updateTo(output,revision,baseDir,execCtx);
    }

    public GitMaterial(String url, String branch) {
        this(url);
        if (branch != null) {
            this.branch = branch;
        }
    }

    public GitMaterial(String url, String branch, String folder) {
        this(url, branch);
        this.folder = folder;
    }

    public GitMaterial(GitMaterialConfig config) {
        this(config.getUrl(), config.getBranch(), config.getFolder());
        this.autoUpdate = config.getAutoUpdate();
        this.filter = config.rawFilter();
        this.name = config.getName();
        this.submoduleFolder = config.getSubmoduleFolder();
    }

    @Override
    public MaterialConfig config() {
        return new GitMaterialConfig(url, branch, submoduleFolder, autoUpdate, filter, folder, name);
    }

    public List<Modification> latestModification(File baseDir, final SubprocessExecutionContext execCtx) {
        ArrayList<Modification> mods = new ArrayList<Modification>();
        mods.add(getGit(baseDir).latestModification());
        return mods;
    }

    public List<Modification> modificationsSince(File baseDir, Revision revision, final SubprocessExecutionContext execCtx) {
        GitCommand gitCommand = getGit(baseDir);
        return gitCommand.modificationsSince(revision);
    }

    public MaterialInstance createMaterialInstance() {
        return new GitMaterialInstance(url.forCommandline(), branch, submoduleFolder, UUID.randomUUID().toString());
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        parameters.put(ScmMaterialConfig.URL, url.forCommandline());
        parameters.put("branch", branch);
    }

    @Override
    protected void appendAttributes(Map<String, Object> parameters) {
        parameters.put("url", url);
        parameters.put("branch", branch);
    }

    public void updateTo(ProcessOutputStreamConsumer outputStreamConsumer, Revision revision, File baseDir, final SubprocessExecutionContext execCtx) {
        try {
            outputStreamConsumer.stdOutput(format("[%s] Start updating %s at revision %s from %s", GoConstants.PRODUCT_NAME, updatingTarget(), revision.getRevision(), url));
            GitCommand git = git(outputStreamConsumer, workingdir(baseDir));
            git.fetchAndReset(outputStreamConsumer, revision);
            outputStreamConsumer.stdOutput(format("[%s] Done.\n", GoConstants.PRODUCT_NAME));
        } catch (Exception e) {
            bomb(e);
        }
    }

    public ValidationBean checkConnection(final SubprocessExecutionContext execCtx) {
        try {
            GitCommand.checkConnection(url);
            return ValidationBean.valid();
        } catch (Exception e) {
            try {
                return handleException(e, GitCommand.version());
            } catch (Exception notInstallGitException) {
                return ValidationBean.notValid(ERR_GIT_NOT_FOUND);
            }
        }
    }

    public ValidationBean handleException(Exception e, String gitVersionConsoleOut) {
        ValidationBean defaultResponse = ValidationBean.notValid(
                "Repository " + url + " not found!" + " : \n" + e.getMessage());
        try {
            if (!isVersionOnedotSixOrHigher(gitVersionConsoleOut)) {
                return ValidationBean.notValid(ERR_GIT_OLD_VERSION + gitVersionConsoleOut);
            } else {
                return defaultResponse;
            }
        } catch (Exception ex) {
            return defaultResponse;
        }
    }

    boolean isVersionOnedotSixOrHigher(String hgout) {
        String hgVersion = parseGitVersion(hgout);
        Float aFloat = NumberUtils.createFloat(hgVersion.subSequence(0, 3).toString());
        return aFloat >= 1.6;
    }

    private String parseGitVersion(String hgOut) {
        String[] lines = hgOut.split("\n");
        String firstLine = lines[0];
        Matcher m = GIT_VERSION_PATTERN.matcher(firstLine);
        if (m.matches()) {
            return m.group(1);
        } else {
            throw bomb("can not parse hgout : " + hgOut);
        }
    }

    private GitCommand getGit(File workingdir) {
        InMemoryStreamConsumer output = inMemoryConsumer();
        try {
            return git(output, workingdir);
        } catch (Exception e) {
            throw bomb(e.getMessage() + " " + output.getStdError(), e);
        }
    }

    private GitCommand git(ProcessOutputStreamConsumer outputStreamConsumer, final File workingFolder) throws Exception {
        if (isSubmoduleFolder()) {
            return new GitCommand(getFingerprint(), new File(workingFolder.getPath()), GitMaterialConfig.DEFAULT_BRANCH, true);
        }

        GitCommand gitCommand = new GitCommand(getFingerprint(), workingFolder, getBranch(), false);
        if (!isGitRepository(workingFolder) || isRepositoryChanged(gitCommand, workingFolder)) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Invalid git working copy or repository changed. Delete folder: " + workingFolder);
            }
            deleteDirectoryNoisily(workingFolder);
        }
        createParentFolderIfNotExist(workingFolder);
        if (!workingFolder.exists()) {
            TransactionSynchronizationManager txManager = new TransactionSynchronizationManager();
            if (txManager.isActualTransactionActive()) {
                txManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override public void afterCompletion(int status) {
                        if (status != TransactionSynchronization.STATUS_COMMITTED) {
                            FileUtils.deleteQuietly(workingFolder);
                        }
                    }
                });
            }
            int returnValue = gitCommand.cloneFrom(outputStreamConsumer, url.forCommandline());
            bombIfFailedToRunCommandLine(returnValue, "Failed to run git clone command");
        }
        return gitCommand;
    }

    private boolean isSubmoduleFolder() {
        return getSubmoduleFolder() != null;
    }

    private boolean isGitRepository(File workingFolder) {
        return new File(workingFolder, ".git").isDirectory();
    }

    private boolean isRepositoryChanged(GitCommand command, File workingDirectory) {
        UrlArgument currentWorkingUrl = command.workingRepositoryUrl();
        if (LOG.isTraceEnabled()) {
            LOG.trace("Current repository url of [" + workingDirectory + "]: " + currentWorkingUrl);
            LOG.trace("Target repository url: " + url);
        }
        return !MaterialUrl.sameUrl(url.forCommandline(), currentWorkingUrl.forCommandline()) || !getBranch().equals(command.getCurrentBranch());
    }

    /**
     * @deprecated Breaks encapsulation really badly. But we need it for IBatis :-(
     */
    public String getUrl() {
        return url.forCommandline();
    }

    public UrlArgument getUrlArgument() {
        return url;
    }

    public String getLongDescription(){
        return String.format("URL: %s, Branch: %s", url.forDisplay(), branch);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        GitMaterial that = (GitMaterial) o;

        if (branch != null ? !branch.equals(that.branch) : that.branch != null) {
            return false;
        }
        if (submoduleFolder != null ? !submoduleFolder.equals(that.submoduleFolder) : that.submoduleFolder != null) {
            return false;
        }
        if (url != null ? !url.equals(that.url) : that.url != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (branch != null ? branch.hashCode() : 0);
        result = 31 * result + (submoduleFolder != null ? submoduleFolder.hashCode() : 0);
        return result;
    }

    protected String getLocation() {
        return url.forDisplay();
    }

    public String getTypeForDisplay() {
        return "Git";
    }

    public String getBranch() {
        return this.branch;
    }

    public String getSubmoduleFolder() {
        return submoduleFolder;
    }

    public void setSubmoduleFolder(String submoduleFolder) {
        this.submoduleFolder = submoduleFolder;
    }

    public String getUserName() {
        return null;
    }

    public String getPassword() {
        return null;
    }

    public String getEncryptedPassword() {
        return null;
    }

    public boolean isCheckExternals() {
        return false;
    }

    @Override public String getShortRevision(String revision) {
        if (revision == null) return null;
        if (revision.length()<7) return revision;
        return revision.substring(0,7);
    }

    @Override
    public Map<String, Object> getAttributes(boolean addSecureFields) {
        Map<String, Object> materialMap = new HashMap<String, Object>();
        materialMap.put("type", "git");
        Map<String, Object> configurationMap = new HashMap<String, Object>();
        if (addSecureFields) {
            configurationMap.put("url", url.forCommandline());
        } else {
            configurationMap.put("url", url.forDisplay());
        }
        configurationMap.put("branch", branch);
        materialMap.put("git-configuration", configurationMap);
        return materialMap;
    }

    public Class getInstanceType() {
        return GitMaterialInstance.class;
    }

    @Override public String toString() {
        return "GitMaterial{" +
                "url=" + url +
                ", branch='" + branch + '\'' +
                ", submoduleFolder='" + submoduleFolder + '\'' +
                '}';
    }

}
