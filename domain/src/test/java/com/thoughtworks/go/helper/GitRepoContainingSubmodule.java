/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.helper;

import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.TestSubprocessExecutionContext;
import com.thoughtworks.go.domain.materials.git.GitCommand;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.MaterialFingerprintTag;
import com.thoughtworks.go.util.NamedProcessTag;
import com.thoughtworks.go.util.command.CommandLine;
import org.apache.commons.io.FileUtils;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.util.command.CommandLine.createCommandLine;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static com.thoughtworks.go.utils.CommandUtils.exec;
import static java.nio.charset.StandardCharsets.UTF_8;

public class GitRepoContainingSubmodule extends TestRepo {
    public static final String NAME = "repo-containing-submodule";
    private final File workingDir;
    private File remoteRepoDir;

    public GitRepoContainingSubmodule(TemporaryFolder temporaryFolder) throws Exception {
        super(temporaryFolder);
        this.workingDir = temporaryFolder.newFolder();
        remoteRepoDir = createRepo(NAME);
    }

    public File addSubmodule(String repoName, String folderName) throws Exception {
        return addSubmodule(repoName, folderName, folderName);
    }

    public File addSubmodule(String repoName, String submoduleNameToPutInGitSubmodules, String folderName) throws Exception {
        File submodule = createRepo(repoName);
        git(remoteRepoDir).submoduleAdd(FileUtil.toFileURI(submodule.getAbsolutePath()), submoduleNameToPutInGitSubmodules, folderName);
        git(remoteRepoDir).commit("Added submodule " + folderName);
        return submodule;
    }

    public void removeSubmodule(String folderName) throws Exception {
        git(remoteRepoDir).updateSubmoduleWithInit(inMemoryConsumer(), false);
        git(remoteRepoDir).submoduleRemove(folderName);
        git(remoteRepoDir).commit("Removed submodule " + folderName);
    }

    public String currentRevision(String repoFolder) {
        return git(workingCopy(repoFolder)).currentRevision();
    }

    public List<File> files(String repoFolder) {
        return new ArrayList<>(Arrays.asList(workingCopy(repoFolder).listFiles()));
    }

    public GitMaterial mainRepo() {
        return material();
    }

    @Override
    public String projectRepositoryUrl() {
        return FileUtil.toFileURI(remoteRepoDir.getAbsoluteFile());
    }

    @Override
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

        CommandLine.createCommandLine("git").withEncoding("UTF-8").withArg("pull").
                withWorkingDir(new File(remoteRepoDir, submoduleNameInRepo)).
                runOrBomb(new MaterialFingerprintTag(null));
        checkInOneFile(remoteRepoDir, new File(submoduleNameInRepo), comment);

        return latestModification();
    }

    public void goBackOneCommitInSubmoduleAndUpdateMainRepo(String submoduleNameInRepo) {
        File submoduleDir = new File(remoteRepoDir, submoduleNameInRepo);
        exec(submoduleDir, "git", "reset", "HEAD~1");
        exec(submoduleDir, "git", "clean", "-dffx");
        exec(remoteRepoDir, "git", "add", ".");
        git(remoteRepoDir).commit("Went to previous commit in submodule");
    }

    @Override
    public List<Modification> latestModification() {
        File dir = workingCopy("local-working-copy");
        return mainRepo().latestModification(dir, new TestSubprocessExecutionContext());
    }

    @Override
    public GitMaterial material() {
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

    private File workingCopy(String repoFolder) {
        return new File(workingDir, repoFolder);
    }

    private File createRepo(String repoName) throws Exception {
        File withSubmodules = new File(workingDir, repoName);
        withSubmodules.mkdirs();
        git(withSubmodules).init();
        createCommandLine("git").withEncoding("UTF-8").withWorkingDir(withSubmodules).withArgs("config", "user.name", "go_test").runOrBomb(true, new NamedProcessTag("git_config"));
        createCommandLine("git").withEncoding("UTF-8").withWorkingDir(withSubmodules).withArgs("config", "user.email", "go_test@go_test.me").runOrBomb(true, new NamedProcessTag("git_config"));
        createCommandLine("git").withEncoding("UTF-8").withWorkingDir(withSubmodules).withArgs("config", "commit.gpgSign", "false").runOrBomb(true, new NamedProcessTag("git_config"));

        String fileName = "file-" + System.currentTimeMillis();
        addAndCommitNewFile(withSubmodules, fileName, "Added " + fileName);

        fileName = "file-" + System.currentTimeMillis();
        addAndCommitNewFile(withSubmodules, fileName, "Added " + fileName);

        return withSubmodules;
    }

    private void addAndCommitNewFile(File repoFolder, String fileName, String comments) throws Exception {
        File testFile = new File(repoFolder, fileName);
        testFile.createNewFile();
        checkInOneFile(repoFolder, testFile, comments);
    }

    private void checkInOneFile(File repoFolder, File testFile, String comments) {
        git(repoFolder).add(testFile);
        git(repoFolder).commit(comments);
    }

    private GitCommand git(File workingDir) {
        return new GitCommand(null, workingDir, GitMaterialConfig.DEFAULT_BRANCH, false, null);
    }

    private void changeFile(File parentDir, String fileName, String newFileContent) throws IOException {
        File fileToChange = new File(parentDir, fileName);
        FileUtils.writeStringToFile(fileToChange, newFileContent, UTF_8);
    }
}
