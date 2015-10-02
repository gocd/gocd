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

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.util.command.CommandLine.createCommandLine;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;

public class GitTestRepo extends TestRepo {
    private static final String GIT_3_REVISIONS_BUNDLE = "../common/test-resources/unit/data/git/git-3-revisions.git";
    public static final String GIT_FOO_BRANCH_BUNDLE = "../common/test-resources/unit/data/git/foo-branch.git";
    public static final String GIT_SUBMODULE_REF_BUNDLE = "../common/test-resources/unit/data/git/referenced-submodule.git";
    private File gitRepo;

    public GitTestRepo(String path) throws IOException {
        this(new File(path));
    }

    public static GitTestRepo testRepoAtBranch(String gitBundleFilePath, String branch) throws IOException {
        GitTestRepo testRepo = new GitTestRepo(gitBundleFilePath);
        testRepo.checkoutRemoteBranchToLocal(branch);
        return testRepo;
    }


    public GitTestRepo() throws IOException {
        this(GIT_3_REVISIONS_BUNDLE);
    }
    
    public GitMaterial createMaterial(String dest) {
        GitMaterial gitMaterial = new GitMaterial(this.projectRepositoryUrl());
        gitMaterial.setFolder(dest);
        return gitMaterial;
    }

    public GitTestRepo(File gitBundleFile) {
        gitRepo = new File(TestFileUtil.createUniqueTempFolder("GitTestRepo"), "repo");
        cloneBundleToFolder(gitBundleFile, gitRepo);
        tmpFolders.add(gitRepo);
    }

    public String projectRepositoryUrl() {
        return gitRepo.getAbsolutePath();
    }

    @Override public List<Modification> checkInOneFile(String fileName, String comment) throws IOException {
        return addFileAndPush(fileName ,comment);
    }

    @Override public Modification latestModification() {
        return git(gitRepo).latestModification();
    }

    @Override public Material material() {
        return createMaterial();
    }

    public File gitRepository() {
        return gitRepo;
    }

    private void cloneBundleToFolder(File from, File workingDir) {
        GitCommand git = git(workingDir);
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        int returnValue = git.cloneFrom(outputStreamConsumer, from.getAbsolutePath());
        if (returnValue != 0) {
            throw new RuntimeException(String.format("[ERROR] Failed to clone. URL [%s] exit value [%d] output [%s]", from.getAbsolutePath(), returnValue, outputStreamConsumer.getAllOutput()));
        }

        createCommandLine("git").withEncoding("UTF-8").withWorkingDir(workingDir).withArgs("config", "user.name", "go_test").runOrBomb(true, "git_config");
        createCommandLine("git").withEncoding("UTF-8").withWorkingDir(workingDir).withArgs("config", "user.email", "go_test@go_test.me").runOrBomb(true, "git_config");
    }

    private GitCommand git(File workingDir) {
        return new GitCommand(null, workingDir, GitMaterialConfig.DEFAULT_BRANCH, false);
    }

    public GitMaterial createMaterial() {
        return new GitMaterial(this.projectRepositoryUrl());
    }

    public List<Modification> latestModifications() {
        ArrayList<Modification> list = new ArrayList<Modification>();
        list.add(latestModification());
        return list;
    }

    private void checkoutRemoteBranchToLocal(String branch) {
        new GitCommand(null, gitRepo, branch, false).checkoutRemoteBranchToLocal();
    }

    public List<Modification> addFileAndPush(String fileName, String message) throws IOException {
        File newFile = new File(gitRepo, fileName);
        newFile.createNewFile();
        new GitCommand(null, gitRepo, GitMaterialConfig.DEFAULT_BRANCH, false).add(newFile);
        new GitCommand(null, gitRepo, GitMaterialConfig.DEFAULT_BRANCH, false).commit(message);
        return createMaterial().latestModification(TestFileUtil.createUniqueTempFolder("working-dir-"), new TestSubprocessExecutionContext());
    }
}
