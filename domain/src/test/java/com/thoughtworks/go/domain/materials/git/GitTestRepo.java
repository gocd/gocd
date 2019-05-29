/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.NamedProcessTag;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.thoughtworks.go.util.command.CommandLine.createCommandLine;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;

public class GitTestRepo extends TestRepo {
    private static final String GIT_3_REVISIONS_BUNDLE = "../common/src/test/resources/data/git/git-3-revisions.git";
    public static final String GIT_FOO_BRANCH_BUNDLE = "../common/src/test/resources/data/git/foo-branch.git";
    public static final String GIT_SUBMODULE_REF_BUNDLE = "../common/src/test/resources/data/git/referenced-submodule.git";

    public static final StringRevision REVISION_0 = new StringRevision("55502a724dd8574f1e4bcf19b605a1f4f182e892");
    public static final StringRevision REVISION_1 = new StringRevision("b613aee673d96e967100306222246aa9decbc53c");
    public static final StringRevision REVISION_2 = new StringRevision("4ab7833f55024c975cf5b918af640954c108825e");
    public static final StringRevision REVISION_3 = new StringRevision("ab9ff2cee965ae4d0778dbcda1fadffbbc202e85");
    public static final StringRevision REVISION_4 = new StringRevision("5def073a425dfe239aabd4bf8039ffe3b0e8856b");
    public static final StringRevision NON_EXISTENT_REVISION = new StringRevision("4ffef3cb33d98b28858743a53b6ee77bfe9d21bb");
    private File gitRepo;

    public GitTestRepo(String path, TemporaryFolder temporaryFolder) throws IOException {
        this(new File(path), temporaryFolder);
    }

    public static GitTestRepo testRepoAtBranch(String gitBundleFilePath, String branch, TemporaryFolder temporaryFolder) throws IOException {
        GitTestRepo testRepo = new GitTestRepo(gitBundleFilePath, temporaryFolder);
        testRepo.checkoutRemoteBranchToLocal(branch);
        return testRepo;
    }


    public GitTestRepo(TemporaryFolder temporaryFolder) throws IOException {
        this(GIT_3_REVISIONS_BUNDLE, temporaryFolder);
    }

    public GitMaterial createMaterial(String dest) {
        GitMaterial gitMaterial = new GitMaterial(this.projectRepositoryUrl());
        gitMaterial.setFolder(dest);
        return gitMaterial;
    }

    public GitTestRepo(File gitBundleFile, TemporaryFolder temporaryFolder) throws IOException {
        super(temporaryFolder);
        gitRepo = temporaryFolder.newFolder("GitTestRepo" + UUID.randomUUID().toString(), "repo");
        cloneBundleToFolder(gitBundleFile, gitRepo);
        tmpFolders.add(gitRepo);
    }

    public String projectRepositoryUrl() {
        return FileUtil.toFileURI(gitRepo);
    }

    @Override
    public List<Modification> checkInOneFile(String fileName, String comment) throws IOException {
        return addFileAndPush(fileName, comment);
    }

    @Override
    public List<Modification> latestModification() {
        return git(gitRepo).latestModification();
    }

    @Override
    public Material material() {
        return createMaterial();
    }

    public File gitRepository() {
        return gitRepo;
    }

    private void cloneBundleToFolder(File from, File workingDir) {
        GitCommand git = git(workingDir);
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        int returnValue = git.clone(outputStreamConsumer, from.getAbsolutePath());
        if (returnValue != 0) {
            throw new RuntimeException(String.format("[ERROR] Failed to clone. URL [%s] exit value [%d] output [%s]", from.getAbsolutePath(), returnValue, outputStreamConsumer.getAllOutput()));
        }

        createCommandLine("git").withEncoding("UTF-8").withWorkingDir(workingDir).withArgs("config", "user.name", "go_test").runOrBomb(true, new NamedProcessTag("git_config"));
        createCommandLine("git").withEncoding("UTF-8").withWorkingDir(workingDir).withArgs("config", "user.email", "go_test@go_test.me").runOrBomb(true, new NamedProcessTag("git_config"));
        createCommandLine("git").withEncoding("UTF-8").withWorkingDir(workingDir).withArgs("config", "commit.gpgSign", "false").runOrBomb(true, new NamedProcessTag("git_config"));

        git.fetchAndResetToHead(outputStreamConsumer);
    }

    private GitCommand git(File workingDir) {
        return new GitCommand(null, workingDir, GitMaterialConfig.DEFAULT_BRANCH, false, null);
    }

    public GitMaterial createMaterial() {
        return new GitMaterial(this.projectRepositoryUrl());
    }

    public List<Modification> latestModifications() {
        return latestModification();
    }

    private void checkoutRemoteBranchToLocal(String branch) {
        new GitCommand(null, gitRepo, branch, false, null).checkoutRemoteBranchToLocal();
    }

    public List<Modification> addFileAndPush(String fileName, String message) throws IOException {
        File newFile = new File(gitRepo, fileName);
        newFile.createNewFile();
        return addFileAndPush(newFile, message);
    }

    public List<Modification> addFileAndPush(File newFile, String message) throws IOException {
        new GitCommand(null, gitRepo, GitMaterialConfig.DEFAULT_BRANCH, false, null).add(newFile);
        new GitCommand(null, gitRepo, GitMaterialConfig.DEFAULT_BRANCH, false, null).commit(message);
        return createMaterial().latestModification(temporaryFolder.newFolder(), new TestSubprocessExecutionContext());
    }

    public List<Modification> addFileAndAmend(String fileName, String message) throws IOException {
        File newFile = new File(gitRepo, fileName);
        newFile.createNewFile();
        new GitCommand(null, gitRepo, GitMaterialConfig.DEFAULT_BRANCH, false, null).add(newFile);
        new GitCommandWithAmend(null, gitRepo, GitMaterialConfig.DEFAULT_BRANCH, false, new HashMap<>()).commitWithAmend(message, gitRepo);
        return createMaterial().latestModification(temporaryFolder.newFolder(), new TestSubprocessExecutionContext());
    }

    private static class GitCommandWithAmend extends GitCommand {

        public GitCommandWithAmend(String materialFingerprint, File workingDir, String branch, boolean isSubmodule, Map<String, String> environment) {
            super(materialFingerprint, workingDir, branch, isSubmodule, null);
        }

        public void commitWithAmend(String message, File gitRepo) {
            String[] args = new String[]{"commit", "--amend", "-m", message};
            CommandLine gitCommit = CommandLine.createCommandLine("git").withEncoding("UTF-8").withArgs(args).withWorkingDir(gitRepo);
            runOrBomb(gitCommit);
        }

    }

}
