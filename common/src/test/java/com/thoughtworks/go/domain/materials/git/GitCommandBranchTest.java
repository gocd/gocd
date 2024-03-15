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

import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.mail.SysOutStreamConsumer;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.domain.materials.git.GitTestRepo.GIT_FOO_BRANCH_BUNDLE;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

public class GitCommandBranchTest extends GitCommandIntegrationTestBase {

    @Test
    void shouldCloneFromBranchWhenMaterialPointsToABranch() throws IOException {
        GitTestRepo gitFooBranchBundle = GitTestRepo.testRepoAtBranch(GIT_FOO_BRANCH_BUNDLE, TEST_BRANCH, tempDir);
        gitLocalRepoDir = createTempWorkingDirectory();
        git = new GitCommand(null, gitLocalRepoDir, TEST_BRANCH, false, null);
        GitCommand branchedGit = new GitCommand(null, gitLocalRepoDir, TEST_BRANCH, false, null);
        branchedGit.clone(inMemoryConsumer(), gitFooBranchBundle.projectRepositoryUrl());
        InMemoryStreamConsumer output = inMemoryConsumer();
        CommandLine.createCommandLine("git").withEncoding(UTF_8).withArg("branch").withWorkingDir(gitLocalRepoDir).run(output, null);
        assertEquals("* foo", output.getStdOut());
    }

    @Test
    void shouldGetTheCurrentBranchForTheCheckedOutRepo() throws IOException {
        GitTestRepo gitFooBranchBundle = GitTestRepo.testRepoAtBranch(GIT_FOO_BRANCH_BUNDLE, TEST_BRANCH, tempDir);
        gitLocalRepoDir = createTempWorkingDirectory();
        CommandLine gitCloneCommand = CommandLine.createCommandLine("git").withEncoding(UTF_8).withArg("clone");
        gitCloneCommand.withArg("--branch=" + TEST_BRANCH).withArg(new UrlArgument(gitFooBranchBundle.projectRepositoryUrl())).withArg(gitLocalRepoDir.getAbsolutePath());
        gitCloneCommand.run(inMemoryConsumer(), null);
        git = new GitCommand(null, gitLocalRepoDir, TEST_BRANCH, false, null);
        assertEquals(TEST_BRANCH, git.getCurrentBranch());
    }

    @Test
    void shouldBombForFetchFailure() {
        gitInRepo("remote", "rm", "origin");
        gitInRepo("remote", "add", "origin", "git://user:secret@foo.bar/baz");
        final String message = assertThrows(Exception.class, () -> git.fetch(inMemoryConsumer())).getMessage();
        assertEquals("git fetch failed for [git://user:******@foo.bar/baz]", message);
    }

    @Test
    void shouldBombForResettingFailure() {
        final String message = assertThrows(
            Exception.class,
            () -> git.resetWorkingDir(new SysOutStreamConsumer(), new StringRevision("abcdef"), false)
        ).getMessage();
        assertEquals(format("git reset failed for [%s]", gitLocalRepoDir), message);
    }

    @Test
    void shouldCleanIgnoredFilesIfToggleIsDisabled() throws IOException {
        InMemoryStreamConsumer output = inMemoryConsumer();
        File gitIgnoreFile = new File(repoLocation, ".gitignore");
        FileUtils.writeStringToFile(gitIgnoreFile, "*.foo", UTF_8);
        gitRepo.addFileAndPush(gitIgnoreFile, "added gitignore");
        git.fetchAndResetToHead(output, false);

        File ignoredFile = new File(gitLocalRepoDir, "ignored.foo");
        assertTrue(ignoredFile.createNewFile());
        git.fetchAndResetToHead(output, false);
        assertFalse(ignoredFile.exists());
    }

    @Test
    void shouldNotCleanIgnoredFilesIfToggleIsEnabled() throws IOException {
        systemProperties.set(GitCommand.GIT_CLEAN_KEEP_IGNORED_FILES_FLAG, "Y");
        InMemoryStreamConsumer output = inMemoryConsumer();
        File gitIgnoreFile = new File(repoLocation, ".gitignore");
        FileUtils.writeStringToFile(gitIgnoreFile, "*.foo", UTF_8);
        gitRepo.addFileAndPush(gitIgnoreFile, "added gitignore");
        git.fetchAndResetToHead(output, false);

        File ignoredFile = new File(gitLocalRepoDir, "ignored.foo");
        assertTrue(ignoredFile.createNewFile());
        git.fetchAndResetToHead(output, false);
        assertTrue(ignoredFile.exists());
    }
}
