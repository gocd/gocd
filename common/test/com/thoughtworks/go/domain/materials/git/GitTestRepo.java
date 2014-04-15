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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.util.TestFileUtil;

import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;

public class GitTestRepo extends TestRepo {
    public static final File GIT_3_REVISIONS_BUNDLE = new File("../common/test-resources/data/git/git-3-revisions.git");
    public static final File GIT_FOO_BRANCH_BUNDLE = new File("../common/test-resources/data/git/foo-branch.git");
    public static final File GIT_SUBMODULE_BUNDLE = new File("../common/test-resources/data/git/with-submodules.git");
    public static final File GIT_SUBMODULE_REF_BUNDLE = new File("../common/test-resources/data/git/referenced-submodule.git");
    private File gitRepo;

    public static GitTestRepo testRepoAtBranch(File gitBundleFile, String branch) {
        GitTestRepo testRepo = new GitTestRepo(gitBundleFile);
        testRepo.checkoutRemoteBranchToLocal(branch);
        return testRepo;
    }


    public GitTestRepo() {
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
        git(workingDir).cloneFrom(inMemoryConsumer(), from.getAbsolutePath());
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
