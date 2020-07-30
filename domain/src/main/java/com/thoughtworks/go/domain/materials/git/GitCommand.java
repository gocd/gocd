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
package com.thoughtworks.go.domain.materials.git;

import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.git.RefSpecHelper;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.SCMCommand;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.util.NamedProcessTag;
import com.thoughtworks.go.util.command.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.TestOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.config.materials.git.GitMaterial.UNSHALLOW_TRYOUT_STEP;
import static com.thoughtworks.go.config.materials.git.RefSpecHelper.REFS_HEADS;
import static com.thoughtworks.go.domain.materials.ModifiedAction.parseGitAction;
import static com.thoughtworks.go.util.DateUtils.formatRFC822;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.lang.String.format;
import static java.util.Arrays.asList;

public class GitCommand extends SCMCommand {
    private static final Logger LOG = LoggerFactory.getLogger(GitCommand.class);

    private static final Pattern GIT_SUBMODULE_STATUS_PATTERN = Pattern.compile("^.[0-9a-fA-F]{40} (.+?)( \\(.+\\))?$");
    private static final Pattern GIT_SUBMODULE_URL_PATTERN = Pattern.compile("^submodule\\.(.+)\\.url (.+)$");
    private static final Pattern GIT_DIFF_TREE_PATTERN = Pattern.compile("^(.)\\s+(.+)$");
    private static final String GIT_CLEAN_KEEP_IGNORED_FILES_FLAG = "toggle.agent.git.clean.keep.ignored.files";

    private final File workingDir;
    private final List<SecretString> secrets;
    private final String branch;
    private final boolean isSubmodule;

    public GitCommand(String materialFingerprint, File workingDir, String branch, boolean isSubmodule, List<SecretString> secrets) {
        super(materialFingerprint);
        this.workingDir = workingDir;
        this.secrets = secrets != null ? secrets : new ArrayList<>();
        this.branch = StringUtils.defaultIfBlank(branch, GitMaterialConfig.DEFAULT_BRANCH);
        this.isSubmodule = isSubmodule;
    }

    private static boolean hasExactlyOneMatchingBranch(ConsoleResult branchList) {
        return branchList.output().size() == 1;
    }

    public String localBranch() {
        return RefSpecHelper.localBranch(branch);
    }

    public String remoteBranch() {
        return RefSpecHelper.remoteBranch(RefSpecHelper.expandRefSpec(branch));
    }

    public String fullUpstreamRef() {
        return RefSpecHelper.fullUpstreamRef(branch);
    }

    public String expandRefSpec() {
        return RefSpecHelper.expandRefSpec(branch);
    }

    public boolean hasRefSpec() {
        return RefSpecHelper.hasRefSpec(branch);
    }

    public String getConfigValue(String key) {
        return runOrBomb(git_C().withArgs("config", "--get-all", key)).outputAsString();
    }

    public int cloneWithNoCheckout(ConsoleOutputStreamConsumer outputStreamConsumer, String url) {
        CommandLine gitClone = cloneCommand().
                when(!hasRefSpec(), git -> git.withArgs("--branch", branch)).
                withArg("--no-checkout").
                withArg(new UrlArgument(url)).
                withArg(workingDir.getAbsolutePath());

        if (!hasRefSpec()) {
            return run(gitClone, outputStreamConsumer);
        }

        final String abbrevBranch = localBranch();
        final String fullLocalRef = abbrevBranch.startsWith("refs/") ? abbrevBranch : REFS_HEADS + abbrevBranch;

        return runCascade(outputStreamConsumer,
                gitClone,
                git_C().withArgs("config", "--replace-all", "remote.origin.fetch", "+" + expandRefSpec()),
                git_C().withArgs("fetch", "--prune", "--recurse-submodules=no"),
                // Enter a detached head state without updating/restoring files in the workspace.
                // This covers an edge-case where the destination ref is the same as the default
                // branch, which would otherwise cause `git branch -f <local-ref> <remote-ref>` to
                // fail when local-ref == current-ref.
                git_C().withArgs("update-ref", "--no-deref", "HEAD", "HEAD"),
                // Important to create a "real" local branch and not just use `symbolic-ref`
                // to update HEAD in order to ensure that GitMaterial#isBranchEqual() passes;
                // failing this check will cause the working directory to be obliterated and we
                // will re-clone the given repository every time. Yikes!
                git_C().withArgs("branch", "-f", abbrevBranch, remoteBranch()),
                git_C().withArgs("symbolic-ref", "HEAD", fullLocalRef)
        );
    }

    @TestOnly
    public int clone(ConsoleOutputStreamConsumer outputStreamConsumer, String url) {
        return clone(outputStreamConsumer, url, Integer.MAX_VALUE);
    }

    // Clone repository from url with specified depth.
    // Special depth 2147483647 (Integer.MAX_VALUE) are treated as full clone
    public int clone(ConsoleOutputStreamConsumer outputStreamConsumer, String url, Integer depth) {
        CommandLine gitClone = cloneCommand().
                when(!hasRefSpec(), git -> git.withArgs("--branch", branch)).
                when(depth < Integer.MAX_VALUE, git -> git.withArg(format("--depth=%s", depth))).
                withArg(new UrlArgument(url)).withArg(workingDir.getAbsolutePath());

        if (!hasRefSpec()) {
            return run(gitClone, outputStreamConsumer);
        }

        return runCascade(outputStreamConsumer,
                gitClone,
                git_C().withArgs("config", "--replace-all", "remote.origin.fetch", "+" + expandRefSpec()),
                git_C().withArgs("fetch", "--prune", "--recurse-submodules=no"),
                git_C().withArgs("checkout", "-B", localBranch(), remoteBranch())
        );
    }

    public List<Modification> latestModification() {
        return gitLog("-1", "--date=iso", "--no-decorate", "--pretty=medium", "--no-color", remoteBranch());

    }

    public List<Modification> modificationsSince(Revision revision) {
        return gitLog("--date=iso", "--pretty=medium", "--no-decorate", "--no-color", format("%s..%s", revision.getRevision(), remoteBranch()));
    }

    public void resetWorkingDir(ConsoleOutputStreamConsumer outputStreamConsumer, Revision revision, boolean shallow) {
        log(outputStreamConsumer, "Reset working directory %s", workingDir);
        cleanAllUnversionedFiles(outputStreamConsumer);
        removeSubmoduleSectionsFromGitConfig(outputStreamConsumer);
        resetHard(outputStreamConsumer, revision);
        checkoutAllModifiedFilesInSubmodules(outputStreamConsumer);
        updateSubmoduleWithInit(outputStreamConsumer, shallow);
        cleanAllUnversionedFiles(outputStreamConsumer);
    }

    public void resetHard(ConsoleOutputStreamConsumer outputStreamConsumer, Revision revision) {
        log(outputStreamConsumer, "Updating working copy to revision " + revision.getRevision());
        String[] args = new String[]{"reset", "--hard", revision.getRevision()};
        CommandLine gitCmd = gitWd().withArgs(args);
        int result = run(gitCmd, outputStreamConsumer);
        if (result != 0) {
            throw new RuntimeException(format("git reset failed for [%s]", this.workingDir));
        }
    }

    @TestOnly
    public void fetchAndResetToHead(ConsoleOutputStreamConsumer outputStreamConsumer, boolean shallow) {
        fetch(outputStreamConsumer);
        resetWorkingDir(outputStreamConsumer, new StringRevision(remoteBranch()), shallow);
    }

    public void updateSubmoduleWithInit(ConsoleOutputStreamConsumer outputStreamConsumer, boolean shallow) {
        if (!gitSubmoduleEnabled()) {
            return;
        }
        log(outputStreamConsumer, "Updating git sub-modules");

        String[] initArgs = new String[]{"submodule", "init"};
        CommandLine initCmd = gitWd().withArgs(initArgs);
        runOrBomb(initCmd);

        submoduleSync();

        if (shallow && version().supportsSubmoduleDepth()) {
            tryToShallowUpdateSubmodules();
        } else {
            updateSubmodule();
        }

        log(outputStreamConsumer, "Cleaning unversioned files and sub-modules");
        printSubmoduleStatus(outputStreamConsumer);
    }

    public UrlArgument workingRepositoryUrl() {
        String[] args = new String[]{"config", "remote.origin.url"};
        CommandLine gitConfig = gitWd().withArgs(args);

        return new UrlArgument(runOrBomb(gitConfig).outputForDisplay().get(0));
    }

    public void checkConnection(UrlArgument repoUrl) {
        final String ref = fullUpstreamRef();
        final CommandLine commandLine = git().withArgs("ls-remote").withArg(repoUrl).withArg(ref);
        final ConsoleResult result = commandLine.runOrBomb(new NamedProcessTag(repoUrl.forDisplay()));

        if (!hasExactlyOneMatchingBranch(result)) {
            throw new CommandLineException(format("The ref %s could not be found.", ref));
        }
    }

    public GitVersion version() {
        CommandLine gitVersion = git().withArgs("version");

        String gitVersionString = gitVersion.runOrBomb(new NamedProcessTag("git version check")).outputAsString();
        return GitVersion.parse(gitVersionString);
    }

    @TestOnly
    public void add(File fileToAdd) {
        String[] args = new String[]{"add", fileToAdd.getName()};
        CommandLine gitAdd = gitWd().withArgs(args);
        runOrBomb(gitAdd);
    }

    @TestOnly
    public void commit(String message) {
        String[] args = new String[]{"commit", "-m", message};
        CommandLine gitCommit = gitWd().withArgs(args);
        runOrBomb(gitCommit);
    }

    @TestOnly
    public void commitOnDate(String message, Date commitDate) {
        HashMap<String, String> env = new HashMap<>();
        env.put("GIT_AUTHOR_DATE", formatRFC822(commitDate));
        CommandLine gitCmd = gitWd().withArgs("commit", "-m", message).withEnv(env);
        runOrBomb(gitCmd);
    }

    @TestOnly
    public void checkoutRemoteBranchToLocal() {
        CommandLine gitCmd = gitWd().withArgs("checkout", "-b", localBranch(), remoteBranch());
        runOrBomb(gitCmd);
    }

    public void fetch(ConsoleOutputStreamConsumer outputStreamConsumer) {
        log(outputStreamConsumer, "Fetching changes");
        CommandLine gitFetch = gitWd().withArgs("fetch", "origin", "--prune", "--recurse-submodules=no");

        int result = run(gitFetch, outputStreamConsumer);
        if (result != 0) {
            throw new RuntimeException(format("git fetch failed for [%s]", this.workingRepositoryUrl()));
        }
        gc(outputStreamConsumer);
    }

    // Unshallow a shallow cloned repository with "git fetch --depth n".
    // Special depth 2147483647 (Integer.MAX_VALUE) are treated as infinite -- fully unshallow
    // https://git-scm.com/docs/git-fetch-pack
    public void unshallow(ConsoleOutputStreamConsumer outputStreamConsumer, Integer depth) {
        log(outputStreamConsumer, "Unshallowing repository with depth %d", depth);
        CommandLine gitFetch = gitWd()
                .withArgs("fetch", "origin")
                .withArg(format("--depth=%d", depth));

        int result = run(gitFetch, outputStreamConsumer);
        if (result != 0) {
            throw new RuntimeException(format("Unshallow repository failed for [%s]", this.workingRepositoryUrl()));
        }
    }

    @TestOnly
    public void init() {
        CommandLine gitCmd = gitWd().withArgs("init");
        runOrBomb(gitCmd);
    }

    @TestOnly
    public List<String> submoduleFolders() {
        CommandLine gitCmd = gitWd().withArgs("submodule", "status");
        ConsoleResult result = runOrBomb(gitCmd);
        return submoduleFolders(result.output());
    }

    @TestOnly
    public void submoduleAdd(String repoUrl, String submoduleNameToPutInGitSubmodules, String folder) {
        String[] addSubmoduleWithSameNameArgs = new String[]{"submodule", "add", repoUrl, folder};
        String[] changeSubmoduleNameInGitModules = new String[]{"config", "--file", ".gitmodules", "--rename-section", "submodule." + folder, "submodule." + submoduleNameToPutInGitSubmodules};
        String[] addGitModules = new String[]{"add", ".gitmodules"};

        runOrBomb(gitWd().withArgs(addSubmoduleWithSameNameArgs));
        runOrBomb(gitWd().withArgs(changeSubmoduleNameInGitModules));
        runOrBomb(gitWd().withArgs(addGitModules));
    }

    @TestOnly
    public void submoduleRemove(String folderName) {
        configRemoveSection("submodule." + folderName);
        CommandLine gitConfig = gitWd().withArgs("config", "-f", ".gitmodules", "--remove-section", "submodule." + folderName);
        runOrBomb(gitConfig);

        CommandLine gitAdd = gitWd().withArgs("add", ".gitmodules");
        runOrBomb(gitAdd);

        CommandLine gitRm = gitWd().withArgs("rm", "--cached", folderName);
        runOrBomb(gitRm);
        FileUtils.deleteQuietly(new File(workingDir, folderName));
    }

    @TestOnly
    public String currentRevision() {
        String[] args = new String[]{"log", "-1", "--pretty=format:%H"};

        CommandLine gitCmd = gitWd().withArgs(args);
        return runOrBomb(gitCmd).outputAsString();
    }

    public String getCurrentBranch() {
        CommandLine getCurrentBranchCommand = gitWd().withArg("rev-parse").withArg("--abbrev-ref").withArg("HEAD");
        ConsoleResult consoleResult = runOrBomb(getCurrentBranchCommand);
        return consoleResult.outputAsString();
    }

    @TestOnly
    public void changeSubmoduleUrl(String submoduleName, String newUrl) {
        String[] args = new String[]{"config", "--file", ".gitmodules", "submodule." + submoduleName + ".url", newUrl};
        CommandLine gitConfig = gitWd().withArgs(args);
        runOrBomb(gitConfig);
    }

    public void submoduleSync() {
        String[] syncArgs = new String[]{"submodule", "sync"};
        CommandLine syncCmd = gitWd().withArgs(syncArgs);
        runOrBomb(syncCmd);

        List<String> foreachArgs = submoduleForEachRecursive(asList("git", "submodule", "sync"));
        CommandLine foreachCmd = gitWd().withArgs(foreachArgs);
        runOrBomb(foreachCmd);
    }

    public boolean isShallow() {
        return new File(workingDir, ".git/shallow").exists();
    }

    public boolean containsRevisionInBranch(Revision revision) {
        String[] args = {"branch", "-r", "--contains", revision.getRevision()};
        CommandLine gitCommand = gitWd().withArgs(args);
        try {
            ConsoleResult consoleResult = runOrBomb(gitCommand);
            return (consoleResult.outputAsString()).contains(remoteBranch());
        } catch (CommandLineException e) {
            return false;
        }
    }

    protected Map<String, String> submoduleUrls() {
        String[] args = new String[]{"config", "--get-regexp", "^submodule\\..+\\.url"};

        CommandLine gitCmd = gitWd().withArgs(args);
        ConsoleResult result = runOrBomb(gitCmd, false);
        List<String> submoduleList = result.output();
        HashMap<String, String> submoduleUrls = new HashMap<>();
        for (String submoduleLine : submoduleList) {
            Matcher m = GIT_SUBMODULE_URL_PATTERN.matcher(submoduleLine);
            if (!m.find()) {
                bomb("Unable to parse git-config output line: " + result.replaceSecretInfo(submoduleLine) + "\n"
                        + "From output:\n"
                        + result.replaceSecretInfo(StringUtils.join(submoduleList, "\n")));
            }
            submoduleUrls.put(m.group(1), m.group(2));
        }
        return submoduleUrls;
    }

    private CommandLine cloneCommand() {
        return git().withArg("clone");
    }

    private List<Modification> gitLog(String... args) {
        // Git log will only show changes before the currently checked out revision
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();

        try {
            if (!isSubmodule) {
                fetch(outputStreamConsumer);
            }
        } catch (Exception e) {
            throw new RuntimeException(format("Working directory: %s\n%s", workingDir, outputStreamConsumer.getStdError()), e);
        }

        CommandLine gitCmd = gitWd().withArg("log").withArgs(args);
        ConsoleResult result = runOrBomb(gitCmd);

        GitModificationParser parser = new GitModificationParser();
        List<Modification> mods = parser.parse(result.output());
        for (Modification mod : mods) {
            addModifiedFiles(mod);
        }
        return mods;
    }

    private void addModifiedFiles(Modification mod) {
        ConsoleResult consoleResult = diffTree(mod.getRevision());
        List<String> result = consoleResult.output();

        for (String resultLine : result) {
            // First line is the node
            if (resultLine.equals(mod.getRevision())) {
                continue;
            }

            Matcher m = matchResultLine(resultLine);
            if (!m.find()) {
                bomb("Unable to parse git-diff-tree output line: " + consoleResult.replaceSecretInfo(resultLine) + "\n"
                        + "From output:\n"
                        + consoleResult.outputForDisplayAsString());
            }
            mod.createModifiedFile(m.group(2), null, parseGitAction(m.group(1).charAt(0)));
        }
    }

    private ConsoleResult diffTree(String node) {
        CommandLine gitCmd = gitWd().withArgs("diff-tree", "--name-status", "--root", "-r", node);
        return runOrBomb(gitCmd);
    }

    private Matcher matchResultLine(String resultLine) {
        return GIT_DIFF_TREE_PATTERN.matcher(resultLine);
    }

    private void checkoutAllModifiedFilesInSubmodules(ConsoleOutputStreamConsumer outputStreamConsumer) {
        log(outputStreamConsumer, "Removing modified files in submodules");
        List<String> submoduleForEachRecursive = submoduleForEachRecursive(asList("git", "checkout", "."));
        runOrBomb(gitWd().withArgs(submoduleForEachRecursive));
    }

    private String gitCleanArgs() {
        if ("Y".equalsIgnoreCase(System.getProperty(GIT_CLEAN_KEEP_IGNORED_FILES_FLAG))) {
            LOG.info("{} = Y. Using old behaviour for clean using `-dff`", GIT_CLEAN_KEEP_IGNORED_FILES_FLAG);
            return "-dff";
        } else {
            return "-dffx";
        }
    }

    private void printSubmoduleStatus(ConsoleOutputStreamConsumer outputStreamConsumer) {
        log(outputStreamConsumer, "Git sub-module status");
        CommandLine gitCmd = gitWd().withArgs("submodule", "status");
        run(gitCmd, outputStreamConsumer);
    }

    private CommandLine git() {
        CommandLine git = CommandLine.createCommandLine("git").withEncoding("UTF-8");
        return git.withNonArgSecrets(secrets);
    }

    /**
     * Creates git command that includes {@code -C workingDir}. This differs from {@link #gitWd()}
     * in that it does not assert/validate the directory's existence on construction.
     *
     * @return a {@code git} {@link CommandLine} that will switch to {@code workingDir}
     */
    private CommandLine git_C() {
        return git().withArgs("-C", workingDir.getAbsolutePath());
    }

    /**
     * Sets (and verifies existence of) CWD for a git command
     *
     * @return a {@code git} {@link CommandLine} with CWD set to {@code workingDir}.
     */
    private CommandLine gitWd() {
        return git().withWorkingDir(workingDir);
    }

    private void updateSubmodule() {
        CommandLine updateCmd = gitWd().withArgs("submodule", "update");
        runOrBomb(updateCmd);
    }

    private void tryToShallowUpdateSubmodules() {
        if (updateSubmoduleWithDepth(1)) {
            return;
        }
        LOG.warn("git submodule update with --depth=1 failed. Attempting again with --depth={}", UNSHALLOW_TRYOUT_STEP);
        if (updateSubmoduleWithDepth(UNSHALLOW_TRYOUT_STEP)) {
            return;
        }
        LOG.warn("git submodule update with depth={} failed. Attempting again with --depth=Integer.MAX", UNSHALLOW_TRYOUT_STEP);
        if (!updateSubmoduleWithDepth(Integer.MAX_VALUE)) {
            bomb("Failed to update submodule");
        }
    }

    private boolean updateSubmoduleWithDepth(int depth) {
        List<String> updateArgs = new ArrayList<>(asList("submodule", "update"));
        updateArgs.add("--depth=" + depth);
        CommandLine commandLine = gitWd().withArgs(updateArgs);
        try {
            runOrBomb(commandLine);
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
            return false;
        }
        return true;
    }

    private void cleanAllUnversionedFiles(ConsoleOutputStreamConsumer outputStreamConsumer) {
        log(outputStreamConsumer, "Cleaning all unversioned files in working copy");
        cleanUnversionedFilesInAllSubmodules();
        cleanUnversionedFiles();
    }

    private void cleanUnversionedFiles() {
        runOrBomb(gitWd().withArgs("clean", gitCleanArgs()));
    }

    private void cleanUnversionedFilesInAllSubmodules() {
        List<String> args = submoduleForEachRecursive(asList("git", "clean", gitCleanArgs()));
        runOrBomb(gitWd().withArgs(args));
    }

    private void removeSubmoduleSectionsFromGitConfig(ConsoleOutputStreamConsumer outputStreamConsumer) {
        log(outputStreamConsumer, "Cleaning submodule configurations in .git/config");
        for (String submoduleFolder : submoduleUrls().keySet()) {
            configRemoveSection("submodule." + submoduleFolder);
        }
    }

    private void configRemoveSection(String section) {
        String[] args = new String[]{"config", "--remove-section", section};
        CommandLine gitCmd = gitWd().withArgs(args);
        runOrBomb(gitCmd, false);
    }

    private boolean gitSubmoduleEnabled() {
        return new File(workingDir, ".gitmodules").exists();
    }

    private void gc(ConsoleOutputStreamConsumer outputStreamConsumer) {
        log(outputStreamConsumer, "Performing git gc");
        CommandLine gitGc = gitWd().withArgs("gc", "--auto");
        run(gitGc, outputStreamConsumer);
    }

    @TestOnly
    private List<String> submoduleFolders(List<String> submoduleLines) {
        ArrayList<String> submoduleFolders = new ArrayList<>();
        for (String submoduleLine : submoduleLines) {
            Matcher m = GIT_SUBMODULE_STATUS_PATTERN.matcher(submoduleLine);
            if (!m.find()) {
                bomb("Unable to parse git-submodule output line: " + submoduleLine + "\n"
                        + "From output:\n"
                        + StringUtils.join(submoduleLines, "\n"));
            }
            submoduleFolders.add(m.group(1));
        }
        return submoduleFolders;
    }

    private void log(ConsoleOutputStreamConsumer outputStreamConsumer, String message, Object... args) {
        LOG.debug(format(message, args));
        outputStreamConsumer.stdOutput(format("[GIT] " + message, args));
    }

    private List<String> submoduleForEachRecursive(List<String> args) {
        List<String> forEachArgs = new ArrayList<>(asList("submodule", "foreach", "--recursive"));
        if (version().requiresSubmoduleCommandFix()) {
            forEachArgs.add(StringUtils.join(args, " "));
        } else {
            forEachArgs.addAll(args);
        }
        return forEachArgs;
    }
}
