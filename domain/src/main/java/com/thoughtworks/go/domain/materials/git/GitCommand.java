/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import com.thoughtworks.go.util.command.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.domain.materials.ModifiedAction.parseGitAction;
import static com.thoughtworks.go.util.DateUtils.formatRFC822;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;

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
    private Map<String, String> environment;

    public GitCommand(String materialFingerprint, File workingDir, String branch, boolean isSubmodule, Map<String,
            String> environment, List<SecretString> secrets) {
        super(materialFingerprint);
        this.workingDir = workingDir;
        this.secrets = secrets != null ? secrets : new ArrayList<>();
        this.branch = StringUtils.isBlank(branch) ? GitMaterialConfig.DEFAULT_BRANCH : branch;
        this.isSubmodule = isSubmodule;
        this.environment = environment;
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

    private CommandLine cloneCommand() {
        return git(environment)
                .withArg("clone")
                .withArg(String.format("--branch=%s", branch));
    }

    // http://www.kernel.org/pub/software/scm/git/docs/git-log.html
    private String modificationTemplate(String separator) {
        return "%cn <%ce>%n%H%n%ai%n%n%s%n%b%n" + separator;
    }

    public List<Modification> latestModification() {
        return gitLog("-1", "--date=iso", "--pretty=medium", "--no-color", remoteBranch());

    }

    public List<Modification> modificationsSince(Revision revision) {
        return gitLog("--date=iso", "--pretty=medium", "--no-color", String.format("%s..%s", revision.getRevision(), remoteBranch()));
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

        CommandLine gitCmd = git(environment).withArg("log").withArgs(args).withWorkingDir(workingDir);
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


    public void resetWorkingDir(ConsoleOutputStreamConsumer outputStreamConsumer, Revision revision) {
        outputStreamConsumer.stdOutput(String.format("[GIT] Reset working directory %s", workingDir));
        cleanAllUnversionedFiles(outputStreamConsumer);
        resetHard(outputStreamConsumer, revision);
        checkoutAllModifiedFilesInSubmodules(outputStreamConsumer);
        removeSubmoduleSectionsFromGitConfig(outputStreamConsumer);
        submoduleSync();
        updateSubmoduleWithInit(outputStreamConsumer);
        cleanAllUnversionedFiles(outputStreamConsumer);
    }

    private void checkoutAllModifiedFilesInSubmodules(ConsoleOutputStreamConsumer outputStreamConsumer) {
        outputStreamConsumer.stdOutput("[GIT] Removing modified files in submodules");
        runOrBomb(git(environment).withArgs("submodule", "foreach", "--recursive", "git", "checkout", ".").withWorkingDir(workingDir));
    }

    private void cleanAllUnversionedFiles(ConsoleOutputStreamConsumer outputStreamConsumer) {
        outputStreamConsumer.stdOutput("[GIT] Cleaning all unversioned files in working copy");
        cleanUnversionedFilesInAllSubmodulesRecursively();
        cleanUnversionedFiles(workingDir);
    }

    private void cleanUnversionedFilesInAllSubmodulesRecursively() {
        CommandLine gitCmd = git(environment)
                .withArgs("submodule", "foreach", "--recursive", "git", "clean", gitCleanArgs())
                .withWorkingDir(workingDir);
        runOrBomb(gitCmd);
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
        outputStreamConsumer.stdOutput("[GIT] Git sub-module status");
        CommandLine gitCmd = git(environment).withArgs("submodule", "status").withWorkingDir(workingDir);
        run(gitCmd, outputStreamConsumer);
    }

    public void resetHard(ConsoleOutputStreamConsumer outputStreamConsumer, Revision revision) {
        outputStreamConsumer.stdOutput("[GIT] Updating working copy to revision " + revision.getRevision());
        CommandLine gitCmd = git(environment)
                .withArgs("reset", "--hard", revision.getRevision())
                .withWorkingDir(workingDir);
        int result = run(gitCmd, outputStreamConsumer);
        if (result != 0) {
            throw new RuntimeException(String.format("git reset failed for [%s]", this.workingDir));
        }
    }

    private CommandLine git(Map<String, String> environment) {
        CommandLine git = CommandLine.createCommandLine("git").withEncoding("UTF-8");
        git.withNonArgSecrets(secrets);
        return git.withEnv(environment);
    }

    public void fetchAndResetToHead(ConsoleOutputStreamConsumer outputStreamConsumer) {
        fetch(outputStreamConsumer);
        resetWorkingDir(outputStreamConsumer, new StringRevision(remoteBranch()));
    }

    public void updateSubmoduleWithInit(ConsoleOutputStreamConsumer outputStreamConsumer) {
        if (!gitSubmoduleEnabled()) {
            return;
        }
        outputStreamConsumer.stdOutput("[GIT] Initializing and Updating git sub-modules");

        CommandLine initUpdateCmd = git(environment)
                .withArgs("submodule", "update", "--init", "--recursive")
                .withWorkingDir(workingDir);
        runOrBomb(initUpdateCmd);

        submoduleSync();

        outputStreamConsumer.stdOutput("[GIT] Cleaning unversioned files and sub-modules");
        printSubmoduleStatus(outputStreamConsumer);
    }

    private void cleanUnversionedFiles(File workingDir) {
        CommandLine gitCmd = git(environment)
                .withArgs("clean", gitCleanArgs())
                .withWorkingDir(workingDir);
        runOrBomb(gitCmd);
    }

    private void removeSubmoduleSectionsFromGitConfig(ConsoleOutputStreamConsumer outputStreamConsumer) {
        outputStreamConsumer.stdOutput("[GIT] Deinit submodules");
        CommandLine gitCmd = git(environment).withArgs("submodule", "deinit", "-f", "--all");
        runOrBomb(gitCmd);
    }

    private void configRemoveSection(String section) {
        String[] args = new String[]{"config", "--remove-section", section};
        CommandLine gitCmd = git(environment).withArgs(args).withWorkingDir(workingDir);
        runOrBomb(gitCmd, false);
    }

    private boolean gitSubmoduleEnabled() {
        return new File(workingDir, ".gitmodules").exists();
    }

    public UrlArgument workingRepositoryUrl() {
        String[] args = new String[]{"config", "remote.origin.url"};
        CommandLine gitConfig = git(environment).withArgs(args).withWorkingDir(workingDir);

        return new UrlArgument(runOrBomb(gitConfig).outputForDisplay().get(0));
    }

    public void checkConnection(UrlArgument repoUrl, String branch, Map<String, String> environment) {
        CommandLine commandLine = git(environment).withArgs("ls-remote").withArg(repoUrl).withArg("refs/heads/" + branch);
        ConsoleResult result = commandLine.runOrBomb(repoUrl.forDisplay());
        if (!hasOnlyOneMatchingBranch(result)) {
            throw new CommandLineException(String.format("The branch %s could not be found.", branch));
        }
    }

    private static boolean hasOnlyOneMatchingBranch(ConsoleResult branchList) {
        return (branchList.output().size() == 1);
    }

    public String version(Map<String, String> map) {
        CommandLine gitLsRemote = git(map).withArgs("version");

        return gitLsRemote.runOrBomb("git version check").outputAsString();
    }

    public void add(File fileToAdd) {
        String[] args = new String[]{"add", fileToAdd.getName()};
        CommandLine gitAdd = git(environment).withArgs(args).withWorkingDir(workingDir);
        runOrBomb(gitAdd);
    }

    public void commit(String message) {
        String[] args = new String[]{"commit", "-m", message};
        CommandLine gitCommit = git(environment).withArgs(args).withWorkingDir(workingDir);
        runOrBomb(gitCommit);
    }

    public void push() {
        String[] args = new String[]{"push"};
        CommandLine gitCommit = git(environment).withArgs(args).withWorkingDir(workingDir);
        runOrBomb(gitCommit);
    }

    public void pull() {
        String[] args = new String[]{"pull"};
        CommandLine gitCommit = git(environment).withArgs(args).withWorkingDir(workingDir);
        runOrBomb(gitCommit);
    }

    public void commitOnDate(String message, Date commitDate) {
        HashMap<String, String> env = new HashMap<>();
        env.put("GIT_AUTHOR_DATE", formatRFC822(commitDate));
        CommandLine gitCmd = git(environment).withArgs("commit", "-m", message).withEnv(env).withWorkingDir(workingDir);
        runOrBomb(gitCmd);
    }

    public void checkoutRemoteBranchToLocal() {
        CommandLine gitCmd = git(environment).withArgs("checkout", "-b", branch, remoteBranch()).withWorkingDir(workingDir);
        runOrBomb(gitCmd);
    }

    private String remoteBranch() {
        return "origin/" + branch;
    }

    public void fetch(ConsoleOutputStreamConsumer outputStreamConsumer) {
        outputStreamConsumer.stdOutput("[GIT] Fetching changes");
        CommandLine gitFetch = git(environment).withArgs("fetch", "origin", "--prune").withWorkingDir(workingDir);

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
        outputStreamConsumer.stdOutput(String.format("[GIT] Unshallowing repository with depth %d", depth));
        CommandLine gitFetch = git(environment)
                .withArgs("fetch", "origin")
                .withArg(String.format("--depth=%d", depth))
                .withWorkingDir(workingDir);

        int result = run(gitFetch, outputStreamConsumer);
        if (result != 0) {
            throw new RuntimeException(String.format("Unshallow repository failed for [%s]", this.workingRepositoryUrl()));
        }
    }


    private int gc(ConsoleOutputStreamConsumer outputStreamConsumer) {
        outputStreamConsumer.stdOutput("[GIT] Performing git gc");
        CommandLine gitGc = git(environment).withArgs("gc", "--auto").withWorkingDir(workingDir);
        return run(gitGc, outputStreamConsumer);
    }

    public void init() {
        CommandLine gitCmd = git(environment).withArgs("init").withWorkingDir(workingDir);
        runOrBomb(gitCmd);
    }

    public List<String> submoduleFolders() {
        CommandLine gitCmd = git(environment).withArgs("submodule", "status").withWorkingDir(workingDir);
        ConsoleResult result = runOrBomb(gitCmd);
        return submoduleFolders(result.output());
    }

    public ConsoleResult diffTree(String node) {
        CommandLine gitCmd = git(environment).withArgs("diff-tree", "--name-status", "--root", "-r", node).withWorkingDir(workingDir);
        return runOrBomb(gitCmd);
    }

    public void submoduleAdd(String repoUrl, String submoduleNameToPutInGitSubmodules, String folder) {
        String[] addSubmoduleWithSameNameArgs = new String[]{"submodule", "add", repoUrl, folder};
        String[] changeSubmoduleNameInGitModules = new String[]{"config", "--file", ".gitmodules", "--rename-section", "submodule." + folder, "submodule." + submoduleNameToPutInGitSubmodules};
        String[] addGitModules = new String[]{"add", ".gitmodules"};
        String[] changeSubmoduleNameInGitConfig = new String[]{"config", "--file", ".git/config", "--rename-section", "submodule." + folder, "submodule." + submoduleNameToPutInGitSubmodules};

        runOrBomb(git(environment).withArgs(addSubmoduleWithSameNameArgs).withWorkingDir(workingDir));
        runOrBomb(git(environment).withArgs(changeSubmoduleNameInGitModules).withWorkingDir(workingDir));
        runOrBomb(git(environment).withArgs(addGitModules).withWorkingDir(workingDir));
    }

    public void submoduleRemove(String folderName) {
        configRemoveSection("submodule." + folderName);
        CommandLine gitConfig = git(environment).withArgs("config", "-f", ".gitmodules", "--remove-section", "submodule." + folderName).withWorkingDir(workingDir);
        runOrBomb(gitConfig);

        CommandLine gitAdd = git(environment).withArgs("add", ".gitmodules").withWorkingDir(workingDir);
        runOrBomb(gitAdd);

        CommandLine gitRm = git(environment).withArgs("rm", "--cached", folderName).withWorkingDir(workingDir);
        runOrBomb(gitRm);
        FileUtils.deleteQuietly(new File(workingDir, folderName));
    }

    public String currentRevision() {
        String[] args = new String[]{"log", "-1", "--pretty=format:%H"};

        CommandLine gitCmd = git(environment).withArgs(args).withWorkingDir(workingDir);
        return runOrBomb(gitCmd).outputAsString();
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

    public Map<String, String> submoduleUrls() {
        String[] args = new String[]{"config", "--get-regexp", "^submodule\\..+\\.url"};

        CommandLine gitCmd = git(environment).withArgs(args).withWorkingDir(workingDir);
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
        CommandLine getCurrentBranchCommand = git(environment).withArg("rev-parse").withArg("--abbrev-ref").withArg("HEAD").withWorkingDir(workingDir);
        ConsoleResult consoleResult = runOrBomb(getCurrentBranchCommand);
        return consoleResult.outputAsString();
    }

    public void changeSubmoduleUrl(String submoduleName, String newUrl) {
        String[] args = new String[]{"config", "--file", ".gitmodules", "submodule." + submoduleName + ".url", newUrl};
        CommandLine gitConfig = git(environment).withArgs(args).withWorkingDir(workingDir);
        runOrBomb(gitConfig);
    }

    public void submoduleSync() {
        String[] syncArgs = new String[]{"submodule", "sync", "--recursive"};
        CommandLine syncCmd = git(environment).withArgs(syncArgs).withWorkingDir(workingDir);
        runOrBomb(syncCmd);
    }

    public boolean isShallow() {
        return new File(workingDir, ".git/shallow").exists();
    }

    public boolean containsRevisionInBranch(Revision revision) {
        String[] args = {"branch", "-r", "--contains", revision.getRevision()};
        CommandLine gitCommand = git(environment).withArgs(args).withWorkingDir(workingDir);
        try {
            ConsoleResult consoleResult = runOrBomb(gitCommand);
            return (consoleResult.outputAsString()).contains(remoteBranch());
        } catch (CommandLineException e) {
            return false;
        }
    }

}
