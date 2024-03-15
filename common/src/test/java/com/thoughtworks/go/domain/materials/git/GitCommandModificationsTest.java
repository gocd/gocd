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
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.domain.materials.ModifiedFile;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.util.DateUtils;
import com.thoughtworks.go.util.command.CommandLineException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import static com.thoughtworks.go.domain.materials.git.GitTestRepo.GIT_FOO_BRANCH_BUNDLE;
import static com.thoughtworks.go.domain.materials.git.GitTestRepo.REVISION_4;
import static com.thoughtworks.go.util.DateUtils.parseRFC822;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

public class GitCommandModificationsTest extends GitCommandIntegrationTestBase {

    @Test
    void shouldRetrieveLatestModification() {
        Modification mod = git.latestModification().get(0);
        assertEquals("Chris Turner <cturner@thoughtworks.com>", mod.getUserName());
        assertEquals("Added 'run-till-file-exists' ant target", mod.getComment());
        assertEquals(parseRFC822("Fri, 12 Feb 2010 16:12:04 -0800"), mod.getModifiedTime());
        assertEquals("5def073a425dfe239aabd4bf8039ffe3b0e8856b", mod.getRevision());

        List<ModifiedFile> files = mod.getModifiedFiles();
        assertEquals(1, files.size());
        assertEquals("build.xml", files.get(0).getFileName());
        assertEquals(ModifiedAction.modified, files.get(0).getAction());
    }

    @Test
    void shouldRetrieveLatestModificationWhenColoringIsSetToAlways() {
        setColoring();
        Modification mod = git.latestModification().get(0);
        assertEquals("Chris Turner <cturner@thoughtworks.com>", mod.getUserName());
        assertEquals("Added 'run-till-file-exists' ant target", mod.getComment());
        assertEquals(parseRFC822("Fri, 12 Feb 2010 16:12:04 -0800"), mod.getModifiedTime());
        assertEquals("5def073a425dfe239aabd4bf8039ffe3b0e8856b", mod.getRevision());

        List<ModifiedFile> files = mod.getModifiedFiles();
        assertEquals(1, files.size());
        assertEquals("build.xml", files.get(0).getFileName());
        assertEquals(ModifiedAction.modified, files.get(0).getAction());
    }

    @Test
    void shouldRetrieveLatestModificationWhenLogDecorationIsPresent() {
        setLogDecoration();
        Modification mod = git.latestModification().get(0);
        assertEquals("Chris Turner <cturner@thoughtworks.com>", mod.getUserName());
        assertEquals("Added 'run-till-file-exists' ant target", mod.getComment());
        assertEquals(parseRFC822("Fri, 12 Feb 2010 16:12:04 -0800"), mod.getModifiedTime());
        assertEquals("5def073a425dfe239aabd4bf8039ffe3b0e8856b", mod.getRevision());

        List<ModifiedFile> files = mod.getModifiedFiles();
        assertEquals(1, files.size());
        assertEquals("build.xml", files.get(0).getFileName());
        assertEquals(ModifiedAction.modified, files.get(0).getAction());
    }

    @Test
    void modificationChecksShouldNotResultInWorkingCopyCheckOut() {
        git.latestModification();
        assertWorkingCopyNotCheckedOut();
        git.modificationsSince(GitTestRepo.REVISION_2);
        assertWorkingCopyNotCheckedOut();
    }

    @Test
    void shouldReturnNothingForModificationsSinceIfARebasedCommitShaIsPassed() throws IOException {
        GitTestRepo remoteRepo = new GitTestRepo(tempDir);
        gitInRepo("remote", "rm", "origin");
        gitInRepo("remote", "add", "origin", remoteRepo.projectRepositoryUrl());
        GitCommand command = new GitCommand(remoteRepo.createMaterial().getFingerprint(), gitLocalRepoDir, "master", false, null);

        Modification modification = remoteRepo.addFileAndAmend("foo", "amendedCommit").get(0);

        assertTrue(command.modificationsSince(new StringRevision(modification.getRevision())).isEmpty());

    }

    @Test
    void shouldReturnTheRebasedCommitForModificationsSinceTheRevisionBeforeRebase() throws IOException {
        GitTestRepo remoteRepo = new GitTestRepo(tempDir);
        gitInRepo("remote", "rm", "origin");
        gitInRepo("remote", "add", "origin", remoteRepo.projectRepositoryUrl());
        GitCommand command = new GitCommand(remoteRepo.createMaterial().getFingerprint(), gitLocalRepoDir, "master", false, null);

        Modification modification = remoteRepo.addFileAndAmend("foo", "amendedCommit").get(0);

        assertEquals(modification, command.modificationsSince(REVISION_4).get(0));
    }

    @Test
    void shouldReturnTheRebasedCommitForModificationsSinceTheRevisionBeforeRebaseWithColoringIsSetToAlways() throws IOException {
        GitTestRepo remoteRepo = new GitTestRepo(tempDir);
        gitInRepo("remote", "rm", "origin");
        gitInRepo("remote", "add", "origin", remoteRepo.projectRepositoryUrl());
        GitCommand command = new GitCommand(remoteRepo.createMaterial().getFingerprint(), gitLocalRepoDir, "master", false, null);

        Modification modification = remoteRepo.addFileAndAmend("foo", "amendedCommit").get(0);
        setColoring();

        assertEquals(modification, command.modificationsSince(REVISION_4).get(0));
    }

    @Test
    void shouldReturnTheRebasedCommitForModificationsSinceTheRevisionBeforeRebaseWithLogDecoration() throws IOException {
        GitTestRepo remoteRepo = new GitTestRepo(tempDir);
        gitInRepo("remote", "rm", "origin");
        gitInRepo("remote", "add", "origin", remoteRepo.projectRepositoryUrl());
        GitCommand command = new GitCommand(remoteRepo.createMaterial().getFingerprint(), gitLocalRepoDir, "master", false, null);

        Modification modification = remoteRepo.addFileAndAmend("foo", "amendedCommit").get(0);
        setLogDecoration();

        assertEquals(modification, command.modificationsSince(REVISION_4).get(0));
    }

    @Test
    void shouldBombIfCheckedForModificationsSinceWithAShaThatNoLongerExists() throws IOException {
        GitTestRepo remoteRepo = new GitTestRepo(tempDir);
        gitInRepo("remote", "rm", "origin");
        gitInRepo("remote", "add", "origin", remoteRepo.projectRepositoryUrl());
        GitCommand command = new GitCommand(remoteRepo.createMaterial().getFingerprint(), gitLocalRepoDir, "master", false, null);

        Modification modification = remoteRepo.checkInOneFile("foo", "Adding a commit").get(0);
        remoteRepo.addFileAndAmend("bar", "amendedCommit");

        assertThrows(CommandLineException.class, () -> command.modificationsSince(new StringRevision(modification.getRevision())));
    }

    @Test
    void shouldBombIfCheckedForModificationsSinceWithANonExistentRef() throws IOException {
        GitTestRepo remoteRepo = new GitTestRepo(tempDir);
        gitInRepo("remote", "rm", "origin");
        gitInRepo("remote", "add", "origin", remoteRepo.projectRepositoryUrl());
        GitCommand command = new GitCommand(remoteRepo.createMaterial().getFingerprint(), gitLocalRepoDir, "non-existent-branch", false, null);

        Modification modification = remoteRepo.checkInOneFile("foo", "Adding a commit").get(0);

        assertThrows(CommandLineException.class, () -> command.modificationsSince(new StringRevision(modification.getRevision())));
    }

    @Test
    void shouldBombWhileRetrievingLatestModificationFromANonExistentRef() throws IOException {
        GitTestRepo remoteRepo = new GitTestRepo(tempDir);
        gitInRepo("remote", "rm", "origin");
        gitInRepo("remote", "add", "origin", remoteRepo.projectRepositoryUrl());
        GitCommand command = new GitCommand(remoteRepo.createMaterial().getFingerprint(), gitLocalRepoDir, "non-existent-branch", false, null);

        final String message = assertThrows(CommandLineException.class, command::latestModification).getMessage();
        assertTrue(message.contains("ambiguous argument 'origin/non-existent-branch': unknown revision or path not in the working tree."));
    }

    @Test
    void shouldRetrieveFilenameForInitialRevision() throws IOException {
        GitTestRepo testRepo = new GitTestRepo(GitTestRepo.GIT_SUBMODULE_REF_BUNDLE, tempDir);
        GitCommand gitCommand = new GitCommand(null, testRepo.gitRepository(), GitMaterialConfig.DEFAULT_BRANCH, false, null);
        Modification modification = gitCommand.latestModification().get(0);
        assertEquals(1, modification.getModifiedFiles().size());
        assertEquals("remote.txt", modification.getModifiedFiles().get(0).getFileName());
    }

    @Test
    void shouldRetrieveLatestModificationFromBranch() throws Exception {
        GitTestRepo branchedRepo = GitTestRepo.testRepoAtBranch(GIT_FOO_BRANCH_BUNDLE, TEST_BRANCH, tempDir);
        GitCommand branchedGit = new GitCommand(null, createTempWorkingDirectory(), TEST_BRANCH, false, null);
        branchedGit.clone(inMemoryConsumer(), branchedRepo.projectRepositoryUrl());

        Modification mod = branchedGit.latestModification().get(0);

        assertEquals("Chris Turner <cturner@thoughtworks.com>", mod.getUserName());
        assertEquals("Started foo branch", mod.getComment());
        assertEquals(parseRFC822("Tue, 05 Feb 2009 14:28:08 -0800"), mod.getModifiedTime());
        assertEquals("b4fa7271c3cef91822f7fa502b999b2eab2a380d", mod.getRevision());

        List<ModifiedFile> files = mod.getModifiedFiles();
        assertEquals(1, files.size());
        assertEquals("first.txt", files.get(0).getFileName());
        assertEquals(ModifiedAction.modified, files.get(0).getAction());
    }


    @Test
    void shouldIncludeNewChangesInModificationCheck() throws Exception {
        String originalNode = git.latestModification().get(0).getRevision();
        File testingFile = checkInNewRemoteFile();

        Modification modification = git.latestModification().get(0);
        assertNotEquals(originalNode, modification.getRevision());
        assertEquals("New checkin of " + testingFile.getName(), modification.getComment());
        assertEquals(1, modification.getModifiedFiles().size());
        assertEquals(testingFile.getName(), modification.getModifiedFiles().get(0).getFileName());
    }

    @Test
    void shouldIncludeChangesFromTheFutureInModificationCheck() throws Exception {
        String originalNode = git.latestModification().get(0).getRevision();
        Date threeDaysFromNow = new Date(Instant.now().plus(3, ChronoUnit.DAYS).with(ChronoField.NANO_OF_SECOND, 0).toEpochMilli());
        File testingFile = checkInNewRemoteFileInFuture(threeDaysFromNow);

        Modification modification = git.latestModification().get(0);
        assertNotEquals(originalNode, modification.getRevision());
        assertEquals("New checkin of " + testingFile.getName(), modification.getComment());
        assertEquals(threeDaysFromNow, modification.getModifiedTime());
    }

    @Test
    void shouldThrowExceptionIfRepoCanNotConnectWhenModificationCheck() {
        FileUtils.deleteQuietly(repoLocation);

        final String message = assertThrows(Exception.class, git::latestModification).getMessage();
        assertTrue(
            message.contains("The remote end hung up unexpectedly") ||
                message.contains("Could not read from remote repository")
        );
    }

    @Test
    void shouldParseGitOutputCorrectly() throws IOException {
        List<String> stringList;
        try (InputStream resourceAsStream = getClass().getResourceAsStream("git_sample_output.text")) {
            stringList = IOUtils.readLines(resourceAsStream, UTF_8);
        }

        GitModificationParser parser = new GitModificationParser();
        List<Modification> mods = parser.parse(stringList);
        assertEquals(3, mods.size());

        Modification mod = mods.get(2);
        assertEquals("46cceff864c830bbeab0a7aaa31707ae2302762f", mod.getRevision());
        assertEquals(DateUtils.parseISO8601("2009-08-11 12:37:09 -0700"), mod.getModifiedTime());
        assertEquals("Cruise Developer <cruise@cruise-sf3.(none)>", mod.getUserDisplayName());
        final String expected = """
            author:cruise <cceuser@CceDev01.(none)>
            node:ecfab84dd4953105e3301c5992528c2d381c1b8a
            date:2008-12-31 14:32:40 +0800
            description:Moving rakefile to build subdirectory for #2266

            author:CceUser <cceuser@CceDev01.(none)>
            node:fd16efeb70fcdbe63338c49995ce9ff7659e6e77
            date:2008-12-31 14:17:06 +0800
            description:Adding rakefile""";
        assertEquals(expected, mod.getComment());
    }

    private File checkInNewRemoteFile() throws IOException {
        GitCommand remoteGit = new GitCommand(null, repoLocation, GitMaterialConfig.DEFAULT_BRANCH, false, null);
        File testingFile = new File(repoLocation, "testing-file" + System.currentTimeMillis() + ".txt");
        //noinspection ResultOfMethodCallIgnored
        testingFile.createNewFile();
        remoteGit.add(testingFile);
        remoteGit.commit("New checkin of " + testingFile.getName());
        return testingFile;
    }

    private File checkInNewRemoteFileInFuture(Date checkinDate) throws IOException {
        GitCommand remoteGit = new GitCommand(null, repoLocation, GitMaterialConfig.DEFAULT_BRANCH, false, null);
        File testingFile = new File(repoLocation, "testing-file" + System.currentTimeMillis() + ".txt");
        //noinspection ResultOfMethodCallIgnored
        testingFile.createNewFile();
        remoteGit.add(testingFile);
        remoteGit.commitOnDate("New checkin of " + testingFile.getName(), checkinDate);
        return testingFile;
    }
}
