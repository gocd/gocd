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

package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.git.GitCommand;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import static com.thoughtworks.go.util.ArrayUtil.asList;
import static com.thoughtworks.go.util.command.CommandLine.createCommandLine;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;

public class GitSubmoduleRepos extends TestRepo {
    private final File temporaryFolder;
    private File remoteRepoDir;
    public static final String NAME = "with-submodules";

    public GitSubmoduleRepos() throws Exception {
        temporaryFolder = TestFileUtil.createTempFolder("gitRepos-" + System.currentTimeMillis());
        tmpFolders.add(temporaryFolder);
        remoteRepoDir = createRepo(NAME);
    }

    public File addSubmodule(String repoName, String folderName) throws Exception {
        return addSubmodule(repoName, folderName, folderName);
    }

    public File addSubmodule(String repoName, String submoduleNameToPutInGitSubmodules, String folderName) throws Exception {
        File submodule = createRepo(repoName);
        git(remoteRepoDir).submoduleAdd(submodule.getAbsolutePath(), submoduleNameToPutInGitSubmodules, folderName);
        git(remoteRepoDir).commit("Added submodule " + folderName);
        return submodule;
    }

    public void removeSubmodule(String folderName) throws Exception {
        git(remoteRepoDir).updateSubmoduleWithInit(inMemoryConsumer());
        git(remoteRepoDir).submoduleRemove(folderName);
        git(remoteRepoDir).commit("Removed submodule " + folderName);
    }

    public String currentRevision(String repoFolder) {
        return git(workingCopy(repoFolder)).currentRevision();
    }

    private File workingCopy(String repoFolder) {
        return new File(temporaryFolder, repoFolder);
    }

    public List<File> files(String repoFolder) {
        return asList(workingCopy(repoFolder).listFiles());
    }

    private File createRepo(String repoName) throws Exception {
        File withSubmodules = TestFileUtil.createTestFolder(temporaryFolder, repoName);
        git(withSubmodules).init();
        createCommandLine("git").withEncoding("UTF-8").withWorkingDir(withSubmodules).withArgs("config", "user.name", "go_test").runOrBomb(true, "git_config");
        createCommandLine("git").withEncoding("UTF-8").withWorkingDir(withSubmodules).withArgs("config", "user.email", "go_test@go_test.me").runOrBomb(true, "git_config");
        String fileName = "file-" + System.currentTimeMillis();
        addAndCommitNewFile(withSubmodules, fileName, "Added " + fileName);
        return withSubmodules;
    }

    private void addAndCommitNewFile(File repoFolder, String fileName, String comments) throws Exception {
        File testFile = TestFileUtil.createTestFile(repoFolder, fileName);
        checkInOneFile(repoFolder, testFile, comments);
    }

    private void checkInOneFile(File repoFolder, File testFile, String comments) {
        git(repoFolder).add(testFile);
        git(repoFolder).commit(comments);
    }

    private GitCommand git(File workingDir) {
        return new GitCommand(null, workingDir, GitMaterialConfig.DEFAULT_BRANCH, false, new HashMap<String, String>());
    }

    public GitMaterial mainRepo() {
        return material();
    }

    public String projectRepositoryUrl() {
        return FileUtil.toFileURI(remoteRepoDir.getAbsoluteFile());
    }

    public List<Modification> checkInOneFile(String fileName, String comment) throws Exception {
        addAndCommitNewFile(remoteRepoDir, fileName, comment);
        return latestModification();
    }

    public List<Modification> modifyOneFileInSubmoduleAndUpdateMainRepo(File remoteSubmoduleRepoLocation,
                                                                        String submoduleNameInRepo, String fileName,
                                                                        String newContentOfFile) throws Exception {
        String comment = "Changed file: " + fileName + " in submodule: " + remoteSubmoduleRepoLocation;
        changeFile(remoteSubmoduleRepoLocation, fileName, newContentOfFile);
        checkInOneFile(remoteSubmoduleRepoLocation, new File(fileName), comment);

        git(new File(remoteRepoDir, submoduleNameInRepo)).pull();
        checkInOneFile(remoteRepoDir, new File(submoduleNameInRepo), comment);

        return latestModification();
    }

    private void changeFile(File parentDir, String fileName, String newFileContent) throws IOException {
        File fileToChange = new File(parentDir, fileName);
        FileUtils.writeStringToFile(fileToChange, newFileContent);
    }

    public List<Modification> latestModification() {
        File dir = workingCopy("local-working-copy");
        return mainRepo().latestModification(dir, new TestSubprocessExecutionContext());
    }

    @Override public GitMaterial material() {
        return new GitMaterial(remoteRepoDir.getAbsolutePath());
    }

    public void changeSubmoduleUrl(String submoduleName) throws Exception {
        File newSubmodule = createRepo("new-submodule");
        addAndCommitNewFile(newSubmodule, "new", "make a commit");

        git(remoteRepoDir).changeSubmoduleUrl(submoduleName, newSubmodule.getAbsolutePath());
        git(remoteRepoDir).submoduleSync();

        git(new File(remoteRepoDir, "local-submodule")).fetch(inMemoryConsumer());
        git(new File(remoteRepoDir, "local-submodule")).resetHard(inMemoryConsumer(), new StringRevision("origin/master"));
        git(remoteRepoDir).add(new File(".gitmodules"));
        git(remoteRepoDir).add(new File("local-submodule"));
        git(remoteRepoDir).commit("change submodule url");
    }
}
