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
package com.thoughtworks.go.config.materials.git;

import com.thoughtworks.go.config.materials.PasswordAwareMaterial;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.ScmMaterialConfig;
import com.thoughtworks.go.config.materials.SubprocessExecutionContext;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.domain.materials.git.GitCommand;
import com.thoughtworks.go.domain.materials.git.GitMaterialInstance;
import com.thoughtworks.go.domain.materials.git.GitVersion;
import com.thoughtworks.go.domain.materials.svn.MaterialUrl;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.command.*;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.io.File;
import java.net.URISyntaxException;
import java.util.*;

import static com.thoughtworks.go.config.materials.git.RefSpecHelper.localBranch;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ExceptionUtils.bombIfFailedToRunCommandLine;
import static com.thoughtworks.go.util.FileUtil.createParentFolderIfNotExist;
import static com.thoughtworks.go.util.FileUtil.deleteDirectoryNoisily;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isAllBlank;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class GitMaterial extends ScmMaterial implements PasswordAwareMaterial {
    public static final int UNSHALLOW_TRYOUT_STEP = 100;
    public static final int DEFAULT_SHALLOW_CLONE_DEPTH = 2;
    public static final String GO_MATERIAL_BRANCH = "GO_MATERIAL_BRANCH";
    //TODO: use iBatis to set the type for us, and we can get rid of this field.
    public static final String TYPE = "GitMaterial";
    public static final String ERR_GIT_OLD_VERSION = "Please install Git-core 1.9 or above. Currently installed version is ";
    private static final Logger LOG = LoggerFactory.getLogger(GitMaterial.class);
    private static final String ERR_GIT_NOT_FOUND = "Failed to find 'git' on your PATH. Please ensure 'git' is executable by the Go Server and on the Go Agents where this material will be used.";
    private final UrlArgument url;
    private String refSpecOrBranch = GitMaterialConfig.DEFAULT_BRANCH;
    private boolean shallowClone = false;
    private String submoduleFolder;

    public GitMaterial(String url) {
        super(TYPE);
        this.url = new UrlArgument(url);
    }

    public GitMaterial(String url, boolean shallowClone) {
        this(url, null, null, shallowClone);
    }


    public GitMaterial(String url, String refSpecOrBranch) {
        this(url);
        if (refSpecOrBranch != null) {
            this.refSpecOrBranch = refSpecOrBranch;
        }
    }

    public GitMaterial(String url, String refSpecOrBranch, String folder) {
        this(url, refSpecOrBranch);
        this.folder = folder;
    }

    public GitMaterial(String url, String refSpecOrBranch, String folder, Boolean shallowClone) {
        this(url, refSpecOrBranch, folder);
        if (shallowClone != null) {
            this.shallowClone = shallowClone;
        }
    }

    public GitMaterial(GitMaterialConfig config) {
        this(config.getUrl(), config.getBranch(), config.getFolder(), config.isShallowClone());
        this.autoUpdate = config.getAutoUpdate();
        this.filter = config.rawFilter();
        this.name = config.getName();
        this.submoduleFolder = config.getSubmoduleFolder();
        this.invertFilter = config.getInvertFilter();
        this.userName = config.getUserName();
        setPassword(config.getPassword());
    }

    @Override
    public MaterialConfig config() {
        GitMaterialConfig gitMaterialConfig = new GitMaterialConfig();
        gitMaterialConfig.setUrl(this.url.originalArgument());
        gitMaterialConfig.setUserName(this.userName);
        gitMaterialConfig.setPassword(getPassword());
        gitMaterialConfig.setSubmoduleFolder(this.submoduleFolder);
        gitMaterialConfig.setAutoUpdate(this.autoUpdate);
        gitMaterialConfig.setFilter(this.filter);
        gitMaterialConfig.setInvertFilter(this.invertFilter);
        gitMaterialConfig.setFolder(this.folder);
        gitMaterialConfig.setName(this.name);
        gitMaterialConfig.setShallowClone(this.shallowClone);
        Optional.ofNullable(this.refSpecOrBranch).ifPresent(gitMaterialConfig::setBranch);
        return gitMaterialConfig;
    }

    public List<Modification> latestModification(File baseDir, final SubprocessExecutionContext execCtx) {
        return getGit(baseDir, execCtx).latestModification();
    }

    public List<Modification> modificationsSince(File baseDir, Revision revision, final SubprocessExecutionContext execCtx) {
        GitCommand gitCommand = getGit(baseDir, execCtx);
        if (!execCtx.isGitShallowClone()) {
            fullyUnshallow(gitCommand, inMemoryConsumer());
        }
        if (gitCommand.containsRevisionInBranch(revision)) {
            return gitCommand.modificationsSince(revision);
        } else {
            return latestModification(baseDir, execCtx);
        }
    }

    @Override
    public MaterialInstance createMaterialInstance() {
        return new GitMaterialInstance(url.originalArgument(), userName, refSpecOrBranch, submoduleFolder, UUID.randomUUID().toString());
    }

    @Override
    public void updateTo(ConsoleOutputStreamConsumer outputStreamConsumer, File baseDir, RevisionContext revisionContext, final SubprocessExecutionContext execCtx) {
        Revision revision = revisionContext.getLatestRevision();
        try {
            outputStreamConsumer.stdOutput(format("[%s] Start updating %s at revision %s from %s", GoConstants.PRODUCT_NAME, updatingTarget(), revision.getRevision(), getUriForDisplay()));
            File workingDir = execCtx.isServer() ? baseDir : workingdir(baseDir);
            GitCommand git = git(outputStreamConsumer, workingDir, revisionContext.numberOfModifications() + 1, execCtx);
            git.fetch(outputStreamConsumer);
            unshallowIfNeeded(git, outputStreamConsumer, revisionContext.getOldestRevision());
            git.resetWorkingDir(outputStreamConsumer, revision, shallowClone);
            outputStreamConsumer.stdOutput(format("[%s] Done.\n", GoConstants.PRODUCT_NAME));
        } catch (Exception e) {
            bomb(e);
        }
    }

    public ValidationBean checkConnection(final SubprocessExecutionContext execCtx) {
        GitCommand gitCommand = new GitCommand(null, null, refSpecOrBranch, false, secrets());
        try {
            gitCommand.checkConnection(new UrlArgument(urlForCommandLine()));
            return ValidationBean.valid();
        } catch (Exception e) {
            try {
                return handleException(e, gitCommand.version());
            } catch (Exception notInstallGitException) {
                return ValidationBean.notValid(ERR_GIT_NOT_FOUND);
            }
        }
    }

    public ValidationBean handleException(Exception e, GitVersion gitVersion) {
        ValidationBean defaultResponse = ValidationBean.notValid(e.getMessage());
        try {
            if (!gitVersion.isMinimumSupportedVersionOrHigher()) {
                return ValidationBean.notValid(ERR_GIT_OLD_VERSION + gitVersion.getVersion().toString());
            } else {
                return defaultResponse;
            }
        } catch (Exception ex) {
            return defaultResponse;
        }
    }

    /**
     * @deprecated Breaks encapsulation really badly. But we need it for IBatis :-(
     */
    @Override
    public String getUrl() {
        return url.originalArgument();
    }

    @Override
    public String urlForCommandLine() {
        try {
            if (credentialsAreNotProvided()) {
                return this.url.originalArgument();
            }

            return new URIBuilder(this.url.originalArgument())
                    .setUserInfo(new UrlUserInfo(this.userName, this.passwordForCommandLine()).asString())
                    .build().toString();

        } catch (URISyntaxException e) {
            return this.url.originalArgument();
        }
    }

    @Override
    public UrlArgument getUrlArgument() {
        return url;
    }

    @Override
    public String getLongDescription() {
        return String.format("URL: %s, Branch: %s", url.forDisplay(), refSpecOrBranch);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        GitMaterial that = (GitMaterial) o;
        return Objects.equals(url, that.url) &&
                Objects.equals(refSpecOrBranch, that.refSpecOrBranch) &&
                Objects.equals(submoduleFolder, that.submoduleFolder);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), url, refSpecOrBranch, submoduleFolder);
    }

    @Override
    public String getTypeForDisplay() {
        return "Git";
    }

    public String getBranch() {
        return this.refSpecOrBranch;
    }

    public String getSubmoduleFolder() {
        return submoduleFolder;
    }

    public void setSubmoduleFolder(String submoduleFolder) {
        this.submoduleFolder = submoduleFolder;
    }

    @Override
    public boolean isCheckExternals() {
        return false;
    }

    public boolean isShallowClone() {
        return shallowClone;
    }

    @Override
    public String getShortRevision(String revision) {
        if (revision == null) return null;
        if (revision.length() < 7) return revision;
        return revision.substring(0, 7);
    }

    @Override
    public Map<String, Object> getAttributes(boolean addSecureFields) {
        Map<String, Object> materialMap = new HashMap<>();
        materialMap.put("type", "git");
        Map<String, Object> configurationMap = new HashMap<>();
        if (addSecureFields) {
            configurationMap.put("url", url.forCommandLine());
        } else {
            configurationMap.put("url", url.forDisplay());
        }
        configurationMap.put("branch", refSpecOrBranch);
        configurationMap.put("shallow-clone", shallowClone);
        materialMap.put("git-configuration", configurationMap);
        return materialMap;
    }

    @Override
    public Class getInstanceType() {
        return GitMaterialInstance.class;
    }

    @Override
    public String toString() {
        return "GitMaterial{" +
                "url=" + url +
                ", branch='" + refSpecOrBranch + '\'' +
                ", shallowClone=" + shallowClone +
                ", submoduleFolder='" + submoduleFolder + '\'' +
                '}';
    }

    @Override
    public void updateFromConfig(MaterialConfig materialConfig) {
        super.updateFromConfig(materialConfig);
        this.shallowClone = ((GitMaterialConfig) materialConfig).isShallowClone();
    }

    public GitMaterial withShallowClone(boolean value) {
        GitMaterialConfig config = (GitMaterialConfig) config();
        config.setShallowClone(value);
        GitMaterial gitMaterial = new GitMaterial(config);
        gitMaterial.secretParamsForPassword = this.secretParamsForPassword;

        return gitMaterial;
    }

    public String effectiveLocalBranch() {
        return localBranch(isBlank(refSpecOrBranch) ? GitMaterialConfig.DEFAULT_BRANCH : refSpecOrBranch);
    }

    @Override
    protected void appendCriteria(Map<String, Object> parameters) {
        parameters.put(ScmMaterialConfig.URL, url.originalArgument());
        parameters.put("branch", refSpecOrBranch);
    }

    @Override
    protected void appendAttributes(Map<String, Object> parameters) {
        parameters.put("url", url);
        parameters.put("branch", refSpecOrBranch);
        parameters.put("shallowClone", shallowClone);
    }

    @Override
    protected String getLocation() {
        return url.forDisplay();
    }

    @Override
    protected void setGoMaterialVariables(EnvironmentVariableContext environmentVariableContext) {
        super.setGoMaterialVariables(environmentVariableContext);
        setVariableWithName(environmentVariableContext, effectiveLocalBranch(), GO_MATERIAL_BRANCH);
    }

    private GitCommand getGit(File workingdir, SubprocessExecutionContext executionContext) {
        InMemoryStreamConsumer output = inMemoryConsumer();
        try {
            return git(output, workingdir, DEFAULT_SHALLOW_CLONE_DEPTH, executionContext);
        } catch (Exception e) {
            throw bomb(e.getMessage() + " " + output.getStdError(), e);
        }
    }

    private GitCommand git(ConsoleOutputStreamConsumer outputStreamConsumer, final File workingFolder, int preferredCloneDepth, SubprocessExecutionContext executionContext) throws Exception {
        if (isSubmoduleFolder()) {
            return new GitCommand(getFingerprint(), new File(workingFolder.getPath()), GitMaterialConfig.DEFAULT_BRANCH, true, secrets());
        }

        GitCommand gitCommand = new GitCommand(getFingerprint(), workingFolder, refSpecOrBranch, false, secrets());
        if (!isGitRepository(workingFolder) || isRepositoryChanged(gitCommand, workingFolder)) {
            LOG.debug("Invalid git working copy or repository changed. Delete folder: {}", workingFolder);
            deleteDirectoryNoisily(workingFolder);
        }
        createParentFolderIfNotExist(workingFolder);
        if (!workingFolder.exists()) {
            TransactionSynchronizationManager txManager = new TransactionSynchronizationManager();
            if (txManager.isActualTransactionActive()) {
                txManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != TransactionSynchronization.STATUS_COMMITTED) {
                            FileUtils.deleteQuietly(workingFolder);
                        }
                    }
                });
            }
            int cloneDepth = shallowClone ? preferredCloneDepth : Integer.MAX_VALUE;
            int returnValue;
            if (executionContext.isServer()) {
                returnValue = gitCommand.cloneWithNoCheckout(outputStreamConsumer, urlForCommandLine());
            } else {
                returnValue = gitCommand.clone(outputStreamConsumer, urlForCommandLine(), cloneDepth);
            }
            bombIfFailedToRunCommandLine(returnValue, "Failed to run git clone command");
        }
        return gitCommand;
    }

    private List<SecretString> secrets() {
        SecretString secretSubstitution = line -> line.replace(urlForCommandLine(), getUriForDisplay());
        return Collections.singletonList(secretSubstitution);
    }

    // Unshallow local repo to include a revision operating on via two step process:
    // First try to fetch forward 100 level with "git fetch -depth 100". If revision still missing,
    // unshallow the whole repo with "git fetch --2147483647".
    private void unshallowIfNeeded(GitCommand gitCommand, ConsoleOutputStreamConsumer streamConsumer, Revision revision) {
        if (gitCommand.isShallow() && !gitCommand.containsRevisionInBranch(revision)) {
            gitCommand.unshallow(streamConsumer, UNSHALLOW_TRYOUT_STEP);

            if (gitCommand.isShallow() && !gitCommand.containsRevisionInBranch(revision)) {
                fullyUnshallow(gitCommand, streamConsumer);
            }
        }
    }

    private void fullyUnshallow(GitCommand gitCommand, ConsoleOutputStreamConsumer streamConsumer) {
        if (gitCommand.isShallow()) {
            gitCommand.unshallow(streamConsumer, Integer.MAX_VALUE);
        }
    }

    private boolean isSubmoduleFolder() {
        return getSubmoduleFolder() != null;
    }

    private boolean isGitRepository(File workingFolder) {
        return new File(workingFolder, ".git").isDirectory();
    }

    private boolean isRepositoryChanged(GitCommand command, File workingDirectory) {
        UrlArgument currentWorkingUrl = command.workingRepositoryUrl();
        LOG.trace("Current repository url of [{}]: {}", workingDirectory, currentWorkingUrl);
        LOG.trace("Target repository url: {}", url);
        return !MaterialUrl.sameUrl(url.forDisplay(), currentWorkingUrl.forDisplay())
                || !isRemoteFetchConfigEqual(command)
                || !isBranchEqual(command)
                || (!shallowClone && command.isShallow());
    }

    private boolean isRemoteFetchConfigEqual(GitCommand command) {
        if (command.hasRefSpec()) {
            try {
                return ("+" + command.expandRefSpec()).equals(command.getConfigValue("remote.origin.fetch"));
            } catch (Throwable ignored) {
                return false;
            }
        }
        return true;
    }

    private boolean isBranchEqual(GitCommand command) {
        return effectiveLocalBranch().equals(command.getCurrentBranch());
    }

    private boolean credentialsAreNotProvided() {
        return isAllBlank(this.userName, this.getPassword());
    }
}
