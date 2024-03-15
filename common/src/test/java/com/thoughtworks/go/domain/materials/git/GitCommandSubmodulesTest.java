/*
 * Copyright 2024 Thoughtworks, Inc.
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
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.helper.GitRepoContainingSubmodule;
import com.thoughtworks.go.mail.SysOutStreamConsumer;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.command.ConsoleResult;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.domain.materials.git.GitTestRepo.GIT_FOO_BRANCH_BUNDLE;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;
import static org.apache.commons.io.filefilter.FileFilterUtils.*;
import static org.junit.jupiter.api.Assertions.*;

public class GitCommandSubmodulesTest extends GitCommandIntegrationTestBase {

    @Test
    void shouldOutputSubmoduleRevisionsAfterUpdate() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(tempDir);
        submoduleRepos.addSubmodule(TEST_SUBMODULE, "sub1");
        GitCommand gitWithSubmodule = new GitCommand(null, createTempWorkingDirectory(), GitMaterialConfig.DEFAULT_BRANCH, false, null);
        gitWithSubmodule.clone(inMemoryConsumer(), submoduleRepos.mainRepo().urlForCommandLine());
        InMemoryStreamConsumer outConsumer = new InMemoryStreamConsumer();
        gitWithSubmodule.resetWorkingDir(outConsumer, new StringRevision("HEAD"), false);
        Matcher matcher = compile(".*^\\s[a-f0-9A-F]{40} sub1 \\(heads/master\\)$.*", Pattern.MULTILINE | Pattern.DOTALL).matcher(outConsumer.getAllOutput());
        assertTrue(matcher.matches());
    }

    @Test
    void shouldBombForResetWorkingDirWhenSubmoduleUpdateFails() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(tempDir);
        File submoduleFolder = submoduleRepos.addSubmodule(TEST_SUBMODULE, "sub1");
        GitCommand gitWithSubmodule = new GitCommand(null, createTempWorkingDirectory(), GitMaterialConfig.DEFAULT_BRANCH, false, null);
        gitWithSubmodule.clone(inMemoryConsumer(), submoduleRepos.mainRepo().urlForCommandLine());
        FileUtils.deleteDirectory(submoduleFolder);

        assertFalse(submoduleFolder.exists());

        final String message = assertThrows(
            Exception.class,
            () -> gitWithSubmodule.resetWorkingDir(new SysOutStreamConsumer(), new StringRevision("HEAD"), false))
            .getMessage();

        final String expectedError = format("[Cc]lone of '%s' into submodule path '((.*)[\\/])?sub1' failed",
            quote(FileUtil.toFileURI(submoduleFolder.getAbsolutePath()) + "/"));
        assertTrue(compile(expectedError).matcher(message).find());
    }

    @Test
    void shouldRetrieveListOfSubmoduleFolders() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(tempDir);
        submoduleRepos.addSubmodule(TEST_SUBMODULE, "sub1");
        GitCommand gitWithSubmodule = new GitCommand(null, createTempWorkingDirectory(), GitMaterialConfig.DEFAULT_BRANCH, false, null);
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        gitWithSubmodule.clone(outputStreamConsumer, submoduleRepos.mainRepo().urlForCommandLine());
        gitWithSubmodule.fetchAndResetToHead(outputStreamConsumer, false);
        gitWithSubmodule.updateSubmoduleWithInit(outputStreamConsumer, false);
        List<String> folders = gitWithSubmodule.submoduleFolders();
        assertEquals(1, folders.size());
        assertEquals("sub1", folders.get(0));
    }

    @Test
    void shouldNotThrowErrorWhenConfigRemoveSectionFails() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(tempDir);
        submoduleRepos.addSubmodule(TEST_SUBMODULE, "sub1");
        GitCommand gitWithSubmodule = new GitCommand(null, createTempWorkingDirectory(), GitMaterialConfig.DEFAULT_BRANCH, false, null) {
            //hack to reproduce synchronization issue
            @Override
            public Map<String, String> submoduleUrls() {
                return Map.of("submodule", "submodule");
            }
        };
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        gitWithSubmodule.clone(outputStreamConsumer, submoduleRepos.mainRepo().urlForCommandLine());

        gitWithSubmodule.updateSubmoduleWithInit(outputStreamConsumer, false);

    }

    @Test
    void shouldNotFailIfUnableToRemoveSubmoduleEntryFromConfig() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(tempDir);
        submoduleRepos.addSubmodule(TEST_SUBMODULE, "sub1");
        GitCommand gitWithSubmodule = new GitCommand(null, createTempWorkingDirectory(), GitMaterialConfig.DEFAULT_BRANCH, false, null);
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        gitWithSubmodule.clone(outputStreamConsumer, submoduleRepos.mainRepo().urlForCommandLine());
        gitWithSubmodule.fetchAndResetToHead(outputStreamConsumer, false);
        gitWithSubmodule.updateSubmoduleWithInit(outputStreamConsumer, false);
        List<String> folders = gitWithSubmodule.submoduleFolders();
        assertEquals(1, folders.size());
        assertEquals("sub1", folders.get(0));
    }

    @Test
    void shouldRetrieveSubmoduleUrls() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(tempDir);
        File submodule = submoduleRepos.addSubmodule(TEST_SUBMODULE, "sub1");
        GitCommand gitWithSubmodule = new GitCommand(null, createTempWorkingDirectory(), GitMaterialConfig.DEFAULT_BRANCH, false, null);
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        gitWithSubmodule.clone(outputStreamConsumer, submoduleRepos.mainRepo().urlForCommandLine());
        gitWithSubmodule.fetchAndResetToHead(outputStreamConsumer, false);

        gitWithSubmodule.updateSubmoduleWithInit(outputStreamConsumer, false);
        Map<String, String> urls = gitWithSubmodule.submoduleUrls();
        assertEquals(1, urls.size());
        assertTrue(urls.containsKey("sub1"));
        assertEquals(FileUtil.toFileURI(submodule), urls.get("sub1"));
    }

    @Test
    void shouldRetrieveZeroSubmoduleUrlsIfTheyAreNotConfigured() {
        Map<String, String> submoduleUrls = git.submoduleUrls();
        assertTrue(submoduleUrls.isEmpty());
    }

    @Test
    void shouldCleanUnversionedFilesInsideSubmodulesBeforeUpdating() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(tempDir);
        String submoduleDirectoryName = "local-submodule";
        submoduleRepos.addSubmodule(TEST_SUBMODULE, submoduleDirectoryName);
        File cloneDirectory = createTempWorkingDirectory();
        GitCommand clonedCopy = new GitCommand(null, cloneDirectory, GitMaterialConfig.DEFAULT_BRANCH, false, null);
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        clonedCopy.clone(outputStreamConsumer, submoduleRepos.mainRepo().urlForCommandLine()); // Clone repository without submodules
        clonedCopy.resetWorkingDir(outputStreamConsumer, new StringRevision("HEAD"), false);  // Pull submodules to working copy - Pipeline counter 1
        File unversionedFile = new File(new File(cloneDirectory, submoduleDirectoryName), "unversioned_file.txt");
        FileUtils.writeStringToFile(unversionedFile, "this is an unversioned file. lets see you deleting me.. come on.. I dare you!!!!", UTF_8);

        clonedCopy.resetWorkingDir(outputStreamConsumer, new StringRevision("HEAD"), false); // Should clean unversioned file on next fetch - Pipeline counter 2

        assertFalse(unversionedFile.exists());
    }

    @Test
    void shouldRemoveChangesToModifiedFilesInsideSubmodulesBeforeUpdating() throws Exception {
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(tempDir);
        String submoduleDirectoryName = "local-submodule";
        File cloneDirectory = createTempWorkingDirectory();

        File remoteSubmoduleLocation = submoduleRepos.addSubmodule(TEST_SUBMODULE, submoduleDirectoryName);

        /* Simulate an agent checkout of code. */
        GitCommand clonedCopy = new GitCommand(null, cloneDirectory, GitMaterialConfig.DEFAULT_BRANCH, false, null);
        clonedCopy.clone(outputStreamConsumer, submoduleRepos.mainRepo().urlForCommandLine());
        clonedCopy.resetWorkingDir(outputStreamConsumer, new StringRevision("HEAD"), false);

        /* Simulate a local modification of file inside submodule, on agent side. */
        File fileInSubmodule = allFilesIn(new File(cloneDirectory, submoduleDirectoryName)).get(0);
        FileUtils.writeStringToFile(fileInSubmodule, "Some other new content.", UTF_8);

        /* Commit a change to the file on the repo. */
        List<Modification> modifications = submoduleRepos.modifyOneFileInSubmoduleAndUpdateMainRepo(
            remoteSubmoduleLocation, submoduleDirectoryName, fileInSubmodule.getName(), "NEW CONTENT OF FILE");

        /* Simulate start of a new build on agent. */
        clonedCopy.fetch(outputStreamConsumer);
        clonedCopy.resetWorkingDir(outputStreamConsumer, new StringRevision(modifications.get(0).getRevision()), false);

        assertEquals("NEW CONTENT OF FILE", FileUtils.readFileToString(fileInSubmodule, UTF_8));
    }

    @Test
    void shouldAllowSubmoduleUrlsToChange() throws Exception {
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(tempDir);
        String submoduleDirectoryName = "local-submodule";
        File cloneDirectory = createTempWorkingDirectory();

        submoduleRepos.addSubmodule(TEST_SUBMODULE, submoduleDirectoryName);

        GitCommand clonedCopy = new GitCommand(null, cloneDirectory, GitMaterialConfig.DEFAULT_BRANCH, false, null);
        clonedCopy.clone(outputStreamConsumer, submoduleRepos.mainRepo().urlForCommandLine());
        clonedCopy.fetchAndResetToHead(outputStreamConsumer, false);

        submoduleRepos.changeSubmoduleUrl(submoduleDirectoryName);

        clonedCopy.fetchAndResetToHead(outputStreamConsumer, false);
    }

    @Test
    @EnabledOnGitVersions(from = "2.10.0")
    void shouldShallowCloneSubmodulesWhenSpecified() throws Exception {
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        GitRepoContainingSubmodule repoContainingSubmodule = new GitRepoContainingSubmodule(tempDir);
        String submoduleDirectoryName = "submoduleDir";
        repoContainingSubmodule.addSubmodule(TEST_SUBMODULE, submoduleDirectoryName);

        File cloneDirectory = createTempWorkingDirectory();
        GitCommand clonedCopy = new GitCommand(null, cloneDirectory, GitMaterialConfig.DEFAULT_BRANCH, false, null);
        clonedCopy.clone(outputStreamConsumer, FileUtil.toFileURI(repoContainingSubmodule.mainRepo().urlForCommandLine()), 1);
        clonedCopy.fetchAndResetToHead(outputStreamConsumer, true);
        ConsoleResult consoleResult = git_C(new File(cloneDirectory, submoduleDirectoryName),
            "rev-list", "--count", "master");
        assertEquals("1", consoleResult.outputAsString());
    }

    @Test
    @EnabledOnGitVersions(from = "2.10.0", through = "2.25.4")
    void shouldUnshallowSubmodulesIfSubmoduleUpdateFails() throws Exception {
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        GitRepoContainingSubmodule repoContainingSubmodule = new GitRepoContainingSubmodule(tempDir);
        String submoduleDirectoryName = "submoduleDir";
        repoContainingSubmodule.addSubmodule(TEST_SUBMODULE, submoduleDirectoryName);
        repoContainingSubmodule.goBackOneCommitInSubmoduleAndUpdateMainRepo(submoduleDirectoryName);

        File cloneDirectory = createTempWorkingDirectory();
        GitCommand clonedCopy = new GitCommand(null, cloneDirectory, GitMaterialConfig.DEFAULT_BRANCH, false, null);
        clonedCopy.clone(outputStreamConsumer, FileUtil.toFileURI(repoContainingSubmodule.mainRepo().urlForCommandLine()), 1);
        clonedCopy.fetchAndResetToHead(outputStreamConsumer, true);
        ConsoleResult consoleResult = git_C(new File(cloneDirectory, submoduleDirectoryName),
            "rev-list", "--count", "master");
        assertEquals("2", consoleResult.outputAsString());
    }

    @Test
    void shouldNotThrowExceptionWhenSubmoduleIsAddedWithACustomName() throws IOException {
        GitTestRepo gitFooBranchBundle = GitTestRepo.testRepoAtBranch(GIT_FOO_BRANCH_BUNDLE, TEST_BRANCH, tempDir);
        git_C(gitLocalRepoDir, "-c", "protocol.file.allow=always", "submodule", "add", "--name", "Custom", gitFooBranchBundle.projectRepositoryUrl());
        git.fetchAndResetToHead(inMemoryConsumer(), false);
    }

    private List<File> allFilesIn(File directory) {
        return new ArrayList<>(FileUtils.listFiles(directory, and(fileFileFilter(), prefixFileFilter("file-")), null));
    }
}
