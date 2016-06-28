/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.StringUtil;
import com.thoughtworks.go.util.command.*;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.domain.materials.ModifiedAction.parseGitAction;
import static com.thoughtworks.go.util.DateUtils.formatRFC822;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.ListUtil.join;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;

public class GitCommand extends SCMCommand {
    private static final Pattern GIT_SUBMODULE_STATUS_PATTERN = Pattern.compile("^.[0-9a-fA-F]{40} (.+?)( \\(.+\\))?$");
    private static final Pattern GIT_SUBMODULE_URL_PATTERN = Pattern.compile("^submodule\\.(.+)\\.url (.+)$");
    private static final Pattern GIT_DIFF_TREE_PATTERN = Pattern.compile("^(.)\\s+(.+)$");

    private final File workingDir;
    private final String branch;
    private final boolean isSubmodule;
    private Map<String, String> environment;

    public GitCommand(String materialFingerprint, File workingDir, String branch, boolean isSubmodule, Map<String, String> environment) {
        super(materialFingerprint);
        this.workingDir = workingDir;
        this.branch = StringUtil.isBlank(branch)? GitMaterialConfig.DEFAULT_BRANCH : branch ;
        this.isSubmodule = isSubmodule;
        this.environment = environment;
    }

    public int cloneWithNoCheckout(ProcessOutputStreamConsumer outputStreamConsumer, String url) {
        CommandLine gitClone = cloneCommand().withArg("--no-checkout");

        gitClone.withArg(new UrlArgument(url)).withArg(workingDir.getAbsolutePath());

        return run(gitClone, outputStreamConsumer);
    }

    public int clone(ProcessOutputStreamConsumer outputStreamConsumer, String url) {
        return clone(outputStreamConsumer, url, Integer.MAX_VALUE);
    }

    // Clone repository from url with specified depth.
    // Special depth 2147483647 (Integer.MAX_VALUE) are treated as full clone
    public int clone(ProcessOutputStreamConsumer outputStreamConsumer, String url, Integer depth) {
        CommandLine gitClone = cloneCommand();

        if(depth < Integer.MAX_VALUE) {
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
        return gitLog("-1", "--date=iso", "--pretty=medium", remoteBranch());

    }

    public List<Modification> modificationsSince(Revision revision) {
        return gitLog("--date=iso", "--pretty=medium", String.format("%s..%s", revision.getRevision(), remoteBranch()));
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


    public void resetWorkingDir(ProcessOutputStreamConsumer outputStreamConsumer, Revision revision) {
        outputStreamConsumer.stdOutput(String.format("[GIT] Reset working directory %s", workingDir));
        cleanAllUnversionedFiles(outputStreamConsumer);
        removeSubmoduleSectionsFromGitConfig(outputStreamConsumer);
        resetHard(outputStreamConsumer, revision);
        checkoutAllModifiedFilesInSubmodules(outputStreamConsumer);
        updateSubmoduleWithInit(outputStreamConsumer);
        cleanAllUnversionedFiles(outputStreamConsumer);
    }

    private void checkoutAllModifiedFilesInSubmodules(ProcessOutputStreamConsumer outputStreamConsumer) {
        outputStreamConsumer.stdOutput("[GIT] Removing modified files in submodules");
        runOrBomb(git(environment).withArgs("submodule", "foreach", "--recursive", "git", "checkout", ".").withWorkingDir(workingDir));
    }

    private void cleanAllUnversionedFiles(ProcessOutputStreamConsumer outputStreamConsumer) {
        outputStreamConsumer.stdOutput("[GIT] Cleaning all unversioned files in working copy");
        for (Map.Entry<String, String> submoduleFolder : submoduleUrls().entrySet()) {
            cleanUnversionedFiles(new File(workingDir, submoduleFolder.getKey()));
        }
        cleanUnversionedFiles(workingDir);
    }

    private void printSubmoduleStatus(ProcessOutputStreamConsumer outputStreamConsumer) {
        outputStreamConsumer.stdOutput("[GIT] Git sub-module status");
        CommandLine gitCmd = git(environment).withArgs("submodule", "status").withWorkingDir(workingDir);
        run(gitCmd, outputStreamConsumer);
    }

    public void resetHard(ProcessOutputStreamConsumer outputStreamConsumer, Revision revision) {
        outputStreamConsumer.stdOutput("[GIT] Updating working copy to revision " + revision.getRevision());
        String[] args = new String[]{"reset", "--hard", revision.getRevision()};
        CommandLine gitCmd = git(environment).withArgs(args).withWorkingDir(workingDir);
        int result = run(gitCmd, outputStreamConsumer);
        if (result != 0) {
            throw new RuntimeException(String.format("git reset failed for [%s]", this.workingDir));
        }
    }

    private static CommandLine git(Map<String, String> environment) {
        CommandLine git = CommandLine.createCommandLine("git").withEncoding("UTF-8");
        return git.withEnv(environment);
    }

    public void fetchAndResetToHead(ProcessOutputStreamConsumer outputStreamConsumer) {
        fetch(outputStreamConsumer);
        resetWorkingDir(outputStreamConsumer, new StringRevision(remoteBranch()));
    }

    public void updateSubmoduleWithInit(ProcessOutputStreamConsumer outputStreamConsumer) {
        if (!gitsubmoduleEnabled()) {
            return;
        }
        outputStreamConsumer.stdOutput("[GIT] Updating git sub-modules");

        String[] initArgs = new String[]{"submodule", "init"};
        CommandLine initCmd = git(environment).withArgs(initArgs).withWorkingDir(workingDir);
        runOrBomb(initCmd);

        submoduleSync();

        String[] updateArgs = new String[]{"submodule", "update"};
        CommandLine updateCmd = git(environment).withArgs(updateArgs).withWorkingDir(workingDir);
        runOrBomb(updateCmd);

        outputStreamConsumer.stdOutput("[GIT] Cleaning unversioned files and sub-modules");
        printSubmoduleStatus(outputStreamConsumer);
    }

    private void cleanUnversionedFiles(File workingDir) {
        String[] args = new String[]{"clean", "-dff"};
        CommandLine gitCmd = git(environment).withArgs(args).withWorkingDir(workingDir);
        runOrBomb(gitCmd);
    }

    private void removeSubmoduleSectionsFromGitConfig(ProcessOutputStreamConsumer outputStreamConsumer) {
        outputStreamConsumer.stdOutput("[GIT] Cleaning submodule configurations in .git/config");
        for (String submoduleFolder : submoduleUrls().keySet()) {
            configRemoveSection("submodule." + submoduleFolder);
        }
    }

    private void configRemoveSection(String section) {
        String[] args = new String[]{"config", "--remove-section", section};
        CommandLine gitCmd = git(environment).withArgs(args).withWorkingDir(workingDir);
        runOrBomb(gitCmd, false);
    }

    private boolean gitsubmoduleEnabled() {
        return new File(workingDir, ".gitmodules").exists();
    }

    public UrlArgument workingRepositoryUrl() {
        String[] args = new String[]{"config", "remote.origin.url"};
        CommandLine gitConfig = git(environment).withArgs(args).withWorkingDir(workingDir);

        return new UrlArgument(runOrBomb(gitConfig).outputForDisplay().get(0));
    }

    public static void checkConnection(UrlArgument repoUrl, String branch, Map<String, String> environment) {
        CommandLine commandLine = git(environment).withArgs("ls-remote").withArg(repoUrl).withArg("refs/heads/" + branch);
        ConsoleResult result = commandLine.runOrBomb(repoUrl.forDisplay());
        if(!hasOnlyOneMatchingBranch(result)){
            throw new CommandLineException(String.format("The branch %s could not be found.", branch));
        }
    }

    private static boolean hasOnlyOneMatchingBranch(ConsoleResult branchList) {
        return (branchList.output().size() == 1);
    }

    public static CommandLine commandToCheckConnection(UrlArgument url, Map<String, String> environment) {
        return git(environment).withArgs("ls-remote").withArg(url);
    }

    public static String version(Map<String, String> map) {
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

    public void fetch(ProcessOutputStreamConsumer outputStreamConsumer) {
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
    public void unshallow(ProcessOutputStreamConsumer outputStreamConsumer, Integer depth) {
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


    private int gc(ProcessOutputStreamConsumer outputStreamConsumer) {
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
        FileUtil.deleteFolder(new File(workingDir, folderName));
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
                        + join(submoduleLines, "\n"));
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
                        + result.replaceSecretInfo(join(submoduleList, "\n")));
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
        String[] syncArgs = new String[]{"submodule", "sync"};
        CommandLine syncCmd = git(environment).withArgs(syncArgs).withWorkingDir(workingDir);
        runOrBomb(syncCmd);

        String[] foreachArgs = new String[]{"submodule", "foreach", "--recursive", "git", "submodule", "sync"};
        CommandLine foreachCmd = git(environment).withArgs(foreachArgs).withWorkingDir(workingDir);
        runOrBomb(foreachCmd);
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
