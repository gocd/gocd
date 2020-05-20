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
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Revision;
import com.thoughtworks.go.domain.materials.SCMCommand;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.util.NamedProcessTag;
import com.thoughtworks.go.util.command.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.config.materials.git.GitMaterial.UNSHALLOW_TRYOUT_STEP;
import static com.thoughtworks.go.domain.materials.ModifiedAction.parseGitAction;
import static com.thoughtworks.go.util.DateUtils.formatRFC822;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
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
        this.branch = StringUtils.isBlank(branch) ? GitMaterialConfig.DEFAULT_BRANCH : branch;
        this.isSubmodule = isSubmodule;
    }

    private static boolean hasOnlyOneMatchingBranch(ConsoleResult branchList) {
        return (branchList.output().size() == 1);
    }

    public int cloneWithNoCheckout(ConsoleOutputStreamConsumer outputStreamConsumer, String url) {
        CommandLine gitClone = cloneCommand().withArg("--no-checkout");

        gitClone.withArg(new UrlArgument(url)).withArg(workingDir.getAbsolutePath());

        return run(gitClone, outputStreamConsumer);
    }

    public int clone(ConsoleOutputStreamConsumer outputStreamConsumer, String url) {
        return clone(outputStreamConsumer, url, Integer.MAX_VALUE);
    }

    // Clone repository from url with specified depth.
    // Special depth 2147483647 (Integer.MAX_VALUE) are treated as full clone
    public int clone(ConsoleOutputStreamConsumer outputStreamConsumer, String url, Integer depth) {
        CommandLine gitClone = cloneCommand();

        if (depth < Integer.MAX_VALUE) {
            gitClone.withArg(String.format("--depth=%s", depth));
        }
        gitClone.withArg(new UrlArgument(url)).withArg(workingDir.getAbsolutePath());

        return run(gitClone, outputStreamConsumer);
    }

    public List<Modification> latestModification() {
        return gitLog("-1", "--date=iso", "--no-decorate", "--pretty=medium", "--no-color", remoteBranch());

    }

    public List<Modification> modificationsSince(Revision revision) {
        return gitLog("--date=iso", "--pretty=medium", "--no-decorate", "--no-color", String.format("%s..%s", revision.getRevision(), remoteBranch()));
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
        CommandLine gitCmd = git().withArgs(args).withWorkingDir(workingDir);
        int result = run(gitCmd, outputStreamConsumer);
        if (result != 0) {
            throw new RuntimeException(String.format("git reset failed for [%s]", this.workingDir));
        }
    }

    public void fetchAndResetToHead(ConsoleOutputStreamConsumer outputStreamConsumer) {
        fetchAndResetToHead(outputStreamConsumer, false);
    }

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
        CommandLine initCmd = git().withArgs(initArgs).withWorkingDir(workingDir);
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
        CommandLine gitConfig = git().withArgs(args).withWorkingDir(workingDir);

        return new UrlArgument(runOrBomb(gitConfig).outputForDisplay().get(0));
    }

    public void checkConnection(UrlArgument repoUrl, String branch) {
        CommandLine commandLine = git().withArgs("ls-remote").withArg(repoUrl).withArg("refs/heads/" + branch);
        ConsoleResult result = commandLine.runOrBomb(new NamedProcessTag(repoUrl.forDisplay()));
        if (!hasOnlyOneMatchingBranch(result)) {
            throw new CommandLineException(String.format("The branch %s could not be found.", branch));
        }
    }

    public GitVersion version() {
        CommandLine gitLsRemote = git().withArgs("version");

        String gitVersionString = gitLsRemote.runOrBomb(new NamedProcessTag("git version check")).outputAsString();
        return GitVersion.parse(gitVersionString);
    }

    public void add(File fileToAdd) {
        String[] args = new String[]{"add", fileToAdd.getName()};
        CommandLine gitAdd = git().withArgs(args).withWorkingDir(workingDir);
        runOrBomb(gitAdd);
    }

    public void commit(String message) {
        String[] args = new String[]{"commit", "-m", message};
        CommandLine gitCommit = git().withArgs(args).withWorkingDir(workingDir);
        runOrBomb(gitCommit);
    }

    public void push() {
        String[] args = new String[]{"push"};
        CommandLine gitCommit = git().withArgs(args).withWorkingDir(workingDir);
        runOrBomb(gitCommit);
    }

    public void pull() {
        String[] args = new String[]{"pull"};
        CommandLine gitCommit = git().withArgs(args).withWorkingDir(workingDir);
        runOrBomb(gitCommit);
    }

    public void commitOnDate(String message, Date commitDate) {
        HashMap<String, String> env = new HashMap<>();
        env.put("GIT_AUTHOR_DATE", formatRFC822(commitDate));
        CommandLine gitCmd = git().withArgs("commit", "-m", message).withEnv(env).withWorkingDir(workingDir);
        runOrBomb(gitCmd);
    }

    public void checkoutRemoteBranchToLocal() {
        CommandLine gitCmd = git().withArgs("checkout", "-b", branch, remoteBranch()).withWorkingDir(workingDir);
        runOrBomb(gitCmd);
    }

    public void fetch(ConsoleOutputStreamConsumer outputStreamConsumer) {
        log(outputStreamConsumer, "Fetching changes");
        CommandLine gitFetch = git().withArgs("fetch", "origin", "--prune", "--recurse-submodules=no").withWorkingDir(workingDir);

        int result = run(gitFetch, outputStreamConsumer);
        if (result != 0) {
            throw new RuntimeException(String.format("git fetch failed for [%s]", this.workingRepositoryUrl()));
        }
        gc(outputStreamConsumer);
    }

    // Unshallow a shallow cloned repository with "git fetch --depth n".
    // Special depth 2147483647 (Integer.MAX_VALUE) are treated as infinite -- fully unshallow
    // https://git-scm.com/docs/git-fetch-pack
    public void unshallow(ConsoleOutputStreamConsumer outputStreamConsumer, Integer depth) {
        log(outputStreamConsumer, "Unshallowing repository with depth %d", depth);
        CommandLine gitFetch = git()
                .withArgs("fetch", "origin")
                .withArg(String.format("--depth=%d", depth))
                .withWorkingDir(workingDir);

        int result = run(gitFetch, outputStreamConsumer);
        if (result != 0) {
            throw new RuntimeException(String.format("Unshallow repository failed for [%s]", this.workingRepositoryUrl()));
        }
    }

    public void init() {
        CommandLine gitCmd = git().withArgs("init").withWorkingDir(workingDir);
        runOrBomb(gitCmd);
    }

    public List<String> submoduleFolders() {
        CommandLine gitCmd = git().withArgs("submodule", "status").withWorkingDir(workingDir);
        ConsoleResult result = runOrBomb(gitCmd);
        return submoduleFolders(result.output());
    }

    public ConsoleResult diffTree(String node) {
        CommandLine gitCmd = git().withArgs("diff-tree", "--name-status", "--root", "-r", node).withWorkingDir(workingDir);
        return runOrBomb(gitCmd);
    }

    public void submoduleAdd(String repoUrl, String submoduleNameToPutInGitSubmodules, String folder) {
        String[] addSubmoduleWithSameNameArgs = new String[]{"submodule", "add", repoUrl, folder};
        String[] changeSubmoduleNameInGitModules = new String[]{"config", "--file", ".gitmodules", "--rename-section", "submodule." + folder, "submodule." + submoduleNameToPutInGitSubmodules};
        String[] addGitModules = new String[]{"add", ".gitmodules"};

        runOrBomb(git().withArgs(addSubmoduleWithSameNameArgs).withWorkingDir(workingDir));
        runOrBomb(git().withArgs(changeSubmoduleNameInGitModules).withWorkingDir(workingDir));
        runOrBomb(git().withArgs(addGitModules).withWorkingDir(workingDir));
    }

    public void submoduleRemove(String folderName) {
        configRemoveSection("submodule." + folderName);
        CommandLine gitConfig = git().withArgs("config", "-f", ".gitmodules", "--remove-section", "submodule." + folderName).withWorkingDir(workingDir);
        runOrBomb(gitConfig);

        CommandLine gitAdd = git().withArgs("add", ".gitmodules").withWorkingDir(workingDir);
        runOrBomb(gitAdd);

        CommandLine gitRm = git().withArgs("rm", "--cached", folderName).withWorkingDir(workingDir);
        runOrBomb(gitRm);
        FileUtils.deleteQuietly(new File(workingDir, folderName));
    }

    public String currentRevision() {
        String[] args = new String[]{"log", "-1", "--pretty=format:%H"};

        CommandLine gitCmd = git().withArgs(args).withWorkingDir(workingDir);
        return runOrBomb(gitCmd).outputAsString();
    }

    public Map<String, String> submoduleUrls() {
        String[] args = new String[]{"config", "--get-regexp", "^submodule\\..+\\.url"};

        CommandLine gitCmd = git().withArgs(args).withWorkingDir(workingDir);
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

    public String getCurrentBranch() {
        CommandLine getCurrentBranchCommand = git().withArg("rev-parse").withArg("--abbrev-ref").withArg("HEAD").withWorkingDir(workingDir);
        ConsoleResult consoleResult = runOrBomb(getCurrentBranchCommand);
        return consoleResult.outputAsString();
    }

    public void changeSubmoduleUrl(String submoduleName, String newUrl) {
        String[] args = new String[]{"config", "--file", ".gitmodules", "submodule." + submoduleName + ".url", newUrl};
        CommandLine gitConfig = git().withArgs(args).withWorkingDir(workingDir);
        runOrBomb(gitConfig);
    }

    public void submoduleSync() {
        String[] syncArgs = new String[]{"submodule", "sync"};
        CommandLine syncCmd = git().withArgs(syncArgs).withWorkingDir(workingDir);
        runOrBomb(syncCmd);

        List<String> foreachArgs = submoduleForEachRecursive(asList("git", "submodule", "sync"));
        CommandLine foreachCmd = git().withArgs(foreachArgs).withWorkingDir(workingDir);
        runOrBomb(foreachCmd);
    }

    public boolean isShallow() {
        return new File(workingDir, ".git/shallow").exists();
    }

    public boolean containsRevisionInBranch(Revision revision) {
        String[] args = {"branch", "-r", "--contains", revision.getRevision()};
        CommandLine gitCommand = git().withArgs(args).withWorkingDir(workingDir);
        try {
            ConsoleResult consoleResult = runOrBomb(gitCommand);
            return (consoleResult.outputAsString()).contains(remoteBranch());
        } catch (CommandLineException e) {
            return false;
        }
    }

    private CommandLine cloneCommand() {
        return git()
                .withArg("clone")
                .withArg(String.format("--branch=%s", branch));
    }

    private List<Modification> gitLog(String... args) {
        // Git log will only show changes before the currently checked out revision
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();

        try {
            if (!isSubmodule) {
                fetch(outputStreamConsumer);
            }
        } catch (Exception e) {
            throw new RuntimeException(String.format("Working directory: %s\n%s", workingDir, outputStreamConsumer.getStdError()), e);
        }

        CommandLine gitCmd = git().withArg("log").withArgs(args).withWorkingDir(workingDir);
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

    private Matcher matchResultLine(String resultLine) {
        return GIT_DIFF_TREE_PATTERN.matcher(resultLine);
    }

    private void checkoutAllModifiedFilesInSubmodules(ConsoleOutputStreamConsumer outputStreamConsumer) {
        log(outputStreamConsumer, "Removing modified files in submodules");
        List<String> submoduleForEachRecursive = submoduleForEachRecursive(asList("git", "checkout", "."));
        runOrBomb(git().withArgs(submoduleForEachRecursive).withWorkingDir(workingDir));
    }

    private void cleanAllUnversionedFiles(ConsoleOutputStreamConsumer outputStreamConsumer) {
        log(outputStreamConsumer, "Cleaning all unversioned files in working copy");
        cleanUnversionedFilesInAllSubmodules();
        cleanUnversionedFiles(workingDir);
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
        CommandLine gitCmd = git().withArgs("submodule", "status").withWorkingDir(workingDir);
        run(gitCmd, outputStreamConsumer);
    }

    private CommandLine git() {
        CommandLine git = CommandLine.createCommandLine("git").withEncoding("UTF-8");
        return git.withNonArgSecrets(secrets);
    }

    private void updateSubmodule() {
        CommandLine updateCmd = git().withArgs("submodule", "update").withWorkingDir(workingDir);
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
        CommandLine commandLine = git().withArgs(updateArgs).withWorkingDir(workingDir);
        try {
            runOrBomb(commandLine);
        } catch (Exception e) {
            LOG.warn(e.getMessage(), e);
            return false;
        }
        return true;
    }

    private void cleanUnversionedFiles(File workingDir) {
        CommandLine gitCmd = git()
                .withArgs("clean", gitCleanArgs())
                .withWorkingDir(workingDir);
        runOrBomb(gitCmd);
    }

    private void cleanUnversionedFilesInAllSubmodules() {
        List<String> args = submoduleForEachRecursive(asList("git", "clean", gitCleanArgs()));
        CommandLine gitCmd = git()
                .withArgs(args)
                .withWorkingDir(workingDir);
        runOrBomb(gitCmd);
    }

    private void removeSubmoduleSectionsFromGitConfig(ConsoleOutputStreamConsumer outputStreamConsumer) {
        log(outputStreamConsumer, "Cleaning submodule configurations in .git/config");
        for (String submoduleFolder : submoduleUrls().keySet()) {
            configRemoveSection("submodule." + submoduleFolder);
        }
    }

    private void configRemoveSection(String section) {
        String[] args = new String[]{"config", "--remove-section", section};
        CommandLine gitCmd = git().withArgs(args).withWorkingDir(workingDir);
        runOrBomb(gitCmd, false);
    }

    private boolean gitSubmoduleEnabled() {
        return new File(workingDir, ".gitmodules").exists();
    }

    private String remoteBranch() {
        return "origin/" + branch;
    }

    private void gc(ConsoleOutputStreamConsumer outputStreamConsumer) {
        log(outputStreamConsumer, "Performing git gc");
        CommandLine gitGc = git().withArgs("gc", "--auto").withWorkingDir(workingDir);
        run(gitGc, outputStreamConsumer);
    }

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
        LOG.debug(String.format(message, args));
        outputStreamConsumer.stdOutput(String.format("[GIT] " + message, args));
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
