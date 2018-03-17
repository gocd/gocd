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
    private static final Pattern GIT_SUBMODULE_PATH_PATTERN = Pattern.compile("^submodule\\.(.+)\\.path (.+)$");
    private static final Pattern GIT_DIFF_TREE_PATTERN = Pattern.compile("^(.)\\s+(.+)$");

    private final File workingDir;
    private final String branch;
    private final boolean isSubmodule;

    public GitCommand(String materialFingerprint, File workingDir, String branch, boolean isSubmodule) {
        super(materialFingerprint);
        this.workingDir = workingDir;
        this.branch = StringUtil.isBlank(branch)? GitMaterialConfig.DEFAULT_BRANCH : branch ;
        this.isSubmodule = isSubmodule;
    }

    public int cloneFrom(ProcessOutputStreamConsumer outputStreamConsumer, String url) {
        CommandLine gitClone = git().withArg("clone").withArg(String.format("--branch=%s", branch)).withArg(new UrlArgument(url)).withArg(workingDir.getAbsolutePath());
        return run(gitClone, outputStreamConsumer);
    }

    // http://www.kernel.org/pub/software/scm/git/docs/git-log.html
    private String modificationTemplate(String separator) {
        return "%cn <%ce>%n%H%n%ai%n%n%s%n%b%n" + separator;
    }

    public Modification latestModification() {
        return gitLog("-1", "--date=iso", "--pretty=medium").get(0);
    }

    public List<Modification> modificationsSince(Revision revision) {
        return gitLog("--date=iso", "--pretty=medium", String.format("%s..", revision.getRevision()));
    }

    private List<Modification> gitLog(String... args) {
        // Git log will only show changes before the currently checked out revision
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();

        try {
            if (!isSubmodule) {
                fetchAndResetToHead(outputStreamConsumer);
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


    public void fetchAndReset(ProcessOutputStreamConsumer outputStreamConsumer, Revision revision) {
        outputStreamConsumer.stdOutput(String.format("[GIT] Fetch and reset in working directory %s", workingDir));
        cleanAllUnversionedFiles(outputStreamConsumer);
        removeSubmoduleSectionsFromGitConfig(outputStreamConsumer);
        fetch(outputStreamConsumer);
        gc(outputStreamConsumer);
        resetHard(outputStreamConsumer, revision);
        checkoutAllModifiedFilesInSubmodules(outputStreamConsumer);
        updateSubmoduleWithInit(outputStreamConsumer);
        cleanAllUnversionedFiles(outputStreamConsumer);
    }

    private void checkoutAllModifiedFilesInSubmodules(ProcessOutputStreamConsumer outputStreamConsumer) {
        outputStreamConsumer.stdOutput("[GIT] Removing modified files in submodules");
        runOrBomb(git().withArgs("submodule", "foreach", "--recursive", "git", "checkout", ".").withWorkingDir(workingDir));
    }

    private void cleanAllUnversionedFiles(ProcessOutputStreamConsumer outputStreamConsumer) {
        outputStreamConsumer.stdOutput("[GIT] Cleaning all unversioned files in working copy");
        for (Map.Entry<String, String> submoduleFolder : submodulePaths().entrySet()) {
            cleanUnversionedFiles(new File(workingDir, submoduleFolder.getValue()));
        }
        cleanUnversionedFiles(workingDir);
    }

    private void printSubmoduleStatus(ProcessOutputStreamConsumer outputStreamConsumer) {
        outputStreamConsumer.stdOutput("[GIT] Git sub-module status");
        CommandLine gitCmd = git().withArgs("submodule", "status").withWorkingDir(workingDir);
        run(gitCmd, outputStreamConsumer);
    }

    public void resetHard(ProcessOutputStreamConsumer outputStreamConsumer, Revision revision) {
        outputStreamConsumer.stdOutput("[GIT] Updating working copy to revision " + revision.getRevision());
        String[] args = new String[]{"reset", "--hard", revision.getRevision()};
        CommandLine gitCmd = git().withArgs(args).withWorkingDir(workingDir);
        int result = run(gitCmd, outputStreamConsumer);
        if (result != 0) {
            throw new RuntimeException(String.format("git reset failed for [%s]", this.workingDir));
        }
    }

    private static CommandLine git() {
        return CommandLine.createCommandLine("git").withEncoding("UTF-8");
    }

    public void fetchAndResetToHead(ProcessOutputStreamConsumer outputStreamConsumer) {
        fetchAndReset(outputStreamConsumer, new StringRevision("origin/" + branch));
    }

    public void updateSubmoduleWithInit(ProcessOutputStreamConsumer outputStreamConsumer) {
        if (!gitsubmoduleEnabled()) {
            return;
        }
        outputStreamConsumer.stdOutput("[GIT] Updating git sub-modules");

        String[] initArgs = new String[]{"submodule", "init"};
        CommandLine initCmd = git().withArgs(initArgs).withWorkingDir(workingDir);
        runOrBomb(initCmd);

        submoduleSync();

        String[] updateArgs = new String[]{"submodule", "update"};
        CommandLine updateCmd = git().withArgs(updateArgs).withWorkingDir(workingDir);
        runOrBomb(updateCmd);

        outputStreamConsumer.stdOutput("[GIT] Cleaning unversioned files and sub-modules");
        printSubmoduleStatus(outputStreamConsumer);
    }

    private void cleanUnversionedFiles(File workingDir) {
        String[] args = new String[]{"clean", "-dff"};
        CommandLine gitCmd = git().withArgs(args).withWorkingDir(workingDir);
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
        CommandLine gitCmd = git().withArgs(args).withWorkingDir(workingDir);
        runOrBomb(gitCmd, false);
    }

    private boolean gitsubmoduleEnabled() {
        return new File(workingDir, ".gitmodules").exists();
    }

    public UrlArgument workingRepositoryUrl() {
        String[] args = new String[]{"config", "remote.origin.url"};
        CommandLine gitConfig = git().withArgs(args).withWorkingDir(workingDir);

        return new UrlArgument(runOrBomb(gitConfig).outputForDisplay().get(0));
    }

    public static void checkConnection(UrlArgument repoUrl) {
        commandToCheckConnection(repoUrl).runOrBomb(repoUrl.forDisplay());
    }

    public static CommandLine commandToCheckConnection(UrlArgument url) {
        return git().withArgs("ls-remote").withArg(url);
    }

    public static String version() {
        CommandLine gitLsRemote = git().withArgs("version");

        return gitLsRemote.runOrBomb("git version check").outputAsString();
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
        HashMap<String, String> env = new HashMap<String, String>();
        env.put("GIT_AUTHOR_DATE", formatRFC822(commitDate));
        CommandLine gitCmd = git().withArgs("commit", "-m", message).withEnv(env).withWorkingDir(workingDir);
        runOrBomb(gitCmd);
    }

    public void checkoutRemoteBranchToLocal() {
        CommandLine gitCmd = git().withArgs("checkout", "-b", branch, "origin/" + branch).withWorkingDir(workingDir);
        runOrBomb(gitCmd);
    }

    public void fetch(ProcessOutputStreamConsumer outputStreamConsumer) {
        outputStreamConsumer.stdOutput("[GIT] Fetching changes");
        CommandLine gitFetch = git().withArgs("fetch", "origin").withWorkingDir(workingDir);

        int result = run(gitFetch, outputStreamConsumer);
        if (result != 0) {
            throw new RuntimeException(String.format("git fetch failed for [%s]", this.workingRepositoryUrl()));
        }
    }

    private int gc(ProcessOutputStreamConsumer outputStreamConsumer) {
        outputStreamConsumer.stdOutput("[GIT] Performing git gc");
        CommandLine gitGc = git().withArgs("gc", "--auto").withWorkingDir(workingDir);
        return run(gitGc, outputStreamConsumer);
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
        String[] changeSubmoduleNameInGitConfig = new String[]{"config", "--file", ".git/config", "--rename-section", "submodule." + folder, "submodule." + submoduleNameToPutInGitSubmodules};

        runOrBomb(git().withArgs(addSubmoduleWithSameNameArgs).withWorkingDir(workingDir));
        runOrBomb(git().withArgs(changeSubmoduleNameInGitModules).withWorkingDir(workingDir));
        runOrBomb(git().withArgs(addGitModules).withWorkingDir(workingDir));
    }

    public void submoduleRemove(String folderName) {
        configRemoveSection("submodule." + folderName);
        CommandLine gitConfig = git().withArgs("config", "-f", ".gitmodules", "--remove-section", "submodule." + folderName).withWorkingDir(workingDir);
        runOrBomb(gitConfig);

        CommandLine gitRm = git().withArgs("rm", "--cached", folderName).withWorkingDir(workingDir);
        runOrBomb(gitRm);
        FileUtil.deleteFolder(new File(workingDir, folderName));
    }

    public String currentRevision() {
        String[] args = new String[]{"log", "-1", "--pretty=format:%H"};

        CommandLine gitCmd = git().withArgs(args).withWorkingDir(workingDir);
        return runOrBomb(gitCmd).outputAsString();
    }

    private List<String> submoduleFolders(List<String> submoduleLines) {
        ArrayList<String> submoduleFolders = new ArrayList<String>();
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

        CommandLine gitCmd = git().withArgs(args).withWorkingDir(workingDir);
        ConsoleResult result = runOrBomb(gitCmd, false);
        List<String> submoduleList = result.output();
        HashMap<String, String> submoduleUrls = new HashMap<String, String>();
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

    public Map<String, String> submodulePaths() {
        String[] args = new String[]{"config", "--file", ".gitmodules", "--get-regexp", "^submodule\\..+\\.path"};
        
        CommandLine gitCmd = git().withArgs(args).withWorkingDir(workingDir);
        ConsoleResult result = runOrBomb(gitCmd, false);
        List<String> submoduleList = result.output();
        HashMap<String, String> submodulePaths = new HashMap<String, String>();
        for (String submoduleLine : submoduleList) {
            Matcher m = GIT_SUBMODULE_PATH_PATTERN.matcher(submoduleLine);
            if (!m.find()) {
                bomb("Unable to parse git-config output line: " + result.replaceSecretInfo(submoduleLine) + "\n"
                        + "From output:\n"
                        + result.replaceSecretInfo(join(submoduleList, "\n")));
            }
            submodulePaths.put(m.group(1), m.group(2));
        }
        return submodulePaths;
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

        String[] foreachArgs = new String[]{"submodule", "foreach", "--recursive", "git", "submodule", "sync"};
        CommandLine foreachCmd = git().withArgs(foreachArgs).withWorkingDir(workingDir);
        runOrBomb(foreachCmd);
    }
}
