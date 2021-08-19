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
package com.thoughtworks.go.domain.materials.git;

import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.domain.materials.ModifiedFile;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.helper.GitRepoContainingSubmodule;
import com.thoughtworks.go.mail.SysOutStreamConsumer;
import com.thoughtworks.go.util.DateUtils;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.NamedProcessTag;
import com.thoughtworks.go.util.TempDirUtils;
import com.thoughtworks.go.util.command.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.domain.materials.git.GitTestRepo.*;
import static com.thoughtworks.go.util.DateUtils.parseRFC822;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.regex.Pattern.compile;
import static java.util.regex.Pattern.quote;
import static org.apache.commons.io.filefilter.FileFilterUtils.*;
import static org.apache.commons.lang3.time.DateUtils.addDays;
import static org.apache.commons.lang3.time.DateUtils.setMilliseconds;
import static org.junit.jupiter.api.Assertions.*;

public class GitCommandTest {
    private static GitCommand withBranch(String branch) {
        return new GitCommand(null, null, branch, false, null);
    }

    private static GitCommand withBranch(String branch, File cwd) {
        return new GitCommand(null, cwd, branch, false, null);
    }

    @Nested
    class RefSpec {
        @Test
        void extractsLocalBranchFromRefSpec() {
            assertEquals("whatever", withBranch("whatever").localBranch());
            assertEquals("whatever", withBranch("refs/anything/goes:whatever").localBranch());
            assertEquals("with/slashes", withBranch("refs/anything/goes:with/slashes").localBranch());
            assertEquals("whatever", withBranch("refs/anything/goes:refs/heads/whatever").localBranch());
            assertEquals("whatever", withBranch("refs/anything/goes:refs/remotes/origin/whatever").localBranch());
            assertEquals("whatever", withBranch("refs/anything/goes:refs/remotes/upstream/whatever").localBranch());
            assertEquals("refs/remotes/oops-did-not-specify-branch", withBranch("refs/anything/goes:refs/remotes/oops-did-not-specify-branch").localBranch());
            assertEquals("refs/full/location", withBranch("refs/anything/goes:refs/full/location").localBranch());
        }

        @Test
        void extractsRemoteBranchFromRefSpec() {
            assertEquals("origin/anywhere", withBranch("anywhere").remoteBranch());
            assertEquals("origin/whatever", withBranch("refs/anything/goes:whatever").remoteBranch());
            assertEquals("origin/with/slashes", withBranch("refs/anything/goes:with/slashes").remoteBranch());
            assertEquals("refs/full/location", withBranch("refs/anything/goes:refs/full/location").remoteBranch());
            assertEquals("origin/any", withBranch("refs/anything/goes:refs/remotes/origin/any").remoteBranch());
            assertEquals("other/remote", withBranch("refs/anything/goes:refs/remotes/other/remote").remoteBranch());
            assertEquals("refs/remotes/oops-did-not-specify-branch", withBranch("refs/anything/goes:refs/remotes/oops-did-not-specify-branch").remoteBranch());
        }

        @Test
        void fullUpstreamRefIsRefSpecAware() {
            assertEquals("refs/heads/dev", withBranch("dev").fullUpstreamRef());
            assertEquals("refs/anything/goes", withBranch("refs/anything/goes:whatever").fullUpstreamRef());
        }

        @Test
        void expandRefSpecEnsuresAbsoluteDestination() {
            assertEquals("branch", withBranch("branch").expandRefSpec());
            assertEquals("refs/a/*:refs/remotes/origin/a-*", withBranch("refs/a/*:a-*").expandRefSpec());
            assertEquals("refs/a/*:refs/remotes/origin/a/b/c-*", withBranch("refs/a/*:a/b/c-*").expandRefSpec());
            assertEquals("refs/a/*:refs/b/*", withBranch("refs/a/*:refs/b/*").expandRefSpec());
            assertEquals("refs/a/*:refs/remotes/origin/a-*", withBranch("refs/a/*:refs/remotes/origin/a-*").expandRefSpec());
        }

        @Test
        void defaultsToMasterWhenNoBranchIsSpecified() {
            assertEquals("master", withBranch(null).localBranch());
            assertEquals("master", withBranch(" ").localBranch());
            assertEquals("master", withBranch("master").localBranch());
            assertEquals("branch", withBranch("branch").localBranch());

            assertEquals("origin/master", withBranch(null).remoteBranch());
            assertEquals("origin/master", withBranch(" ").remoteBranch());
            assertEquals("origin/master", withBranch("master").remoteBranch());
            assertEquals("origin/branch", withBranch("branch").remoteBranch());

            assertEquals("refs/heads/master", withBranch(null).fullUpstreamRef());
            assertEquals("refs/heads/master", withBranch(" ").fullUpstreamRef());
            assertEquals("refs/heads/master", withBranch("master").fullUpstreamRef());
            assertEquals("refs/heads/branch", withBranch("branch").fullUpstreamRef());

            assertEquals("master", withBranch(null).expandRefSpec());
            assertEquals("master", withBranch(" ").expandRefSpec());
            assertEquals("master", withBranch("master").expandRefSpec());
            assertEquals("branch", withBranch("branch").expandRefSpec());
        }
    }

    @Nested
    class RefSpecIntegration {
        @Test
        void clonedRepoReflectsDestinationRef(@TempDir File sandbox) {
            final String refSpec = "refs/pull/1/head:refs/remotes/origin/pull/1/head";
            final GitCommand git = withBranch(refSpec, sandbox);

            final InMemoryStreamConsumer out = inMemoryConsumer();
            assertEquals(0, git.clone(out, GIT_CUSTOM_REFS_BUNDLE), "git clone failed with:\n" + out.getStdError());
            assertEquals("pull/1/head", git.getCurrentBranch());

            assertDoesNotThrow(() -> git.fetch(out), () -> "git fetch failed with:\n" + out.getStdError());
            assertEquals("+" + git.expandRefSpec(), git.getConfigValue("remote.origin.fetch"));
            assertEquals(git(sandbox, "rev-parse", "HEAD"), git(sandbox, "rev-parse", "origin/pull/1/head"));
            assertEquals(git(sandbox, "ls-remote", "origin", "refs/pull/1/head").split("\\s")[0], git(sandbox, "rev-parse", "HEAD"));
        }

        @Test
        void cloneNoCheckoutReflectsDestinationRef(@TempDir File sandbox) {
            final String refSpec = "refs/random/things:refs/remotes/origin/randomness";
            final GitCommand git = withBranch(refSpec, sandbox);

            final InMemoryStreamConsumer out = inMemoryConsumer();
            assertEquals(0, git.cloneWithNoCheckout(out, GIT_CUSTOM_REFS_BUNDLE), "git clone failed with:\n" + out.getStdError());
            assertEquals("randomness", git.getCurrentBranch());

            assertDoesNotThrow(() -> git.fetch(out), () -> "git fetch failed with:\n" + out.getStdError());
            assertEquals("+" + git.expandRefSpec(), git.getConfigValue("remote.origin.fetch"));
            assertEquals(git(sandbox, "rev-parse", "HEAD"), git(sandbox, "rev-parse", "origin/randomness"));
            assertEquals(git(sandbox, "ls-remote", "origin", "refs/random/things").split("\\s")[0], git(sandbox, "rev-parse", "HEAD"));
        }

        @Test
        void clonedRepoReflectsShortDestinationRef(@TempDir File sandbox) {
            final String refSpec = "refs/pull/1/head:pull/1/head";
            final GitCommand git = withBranch(refSpec, sandbox);

            final InMemoryStreamConsumer out = inMemoryConsumer();
            assertEquals(0, git.clone(out, GIT_CUSTOM_REFS_BUNDLE), "git clone failed with:\n" + out.getStdError());
            assertEquals("pull/1/head", git.getCurrentBranch());

            assertDoesNotThrow(() -> git.fetch(out), () -> "git fetch failed with:\n" + out.getStdError());
            assertEquals("+" + git.expandRefSpec(), git.getConfigValue("remote.origin.fetch"));
            assertEquals(git(sandbox, "rev-parse", "HEAD"), git(sandbox, "rev-parse", "origin/pull/1/head"));
            assertEquals(git(sandbox, "ls-remote", "origin", "refs/pull/1/head").split("\\s")[0], git(sandbox, "rev-parse", "HEAD"));
        }

        @Test
        void cloneNoCheckoutReflectsShortDestinationRef(@TempDir File sandbox) {
            final String refSpec = "refs/random/things:randomness";
            final GitCommand git = withBranch(refSpec, sandbox);

            final InMemoryStreamConsumer out = inMemoryConsumer();
            assertEquals(0, git.cloneWithNoCheckout(out, GIT_CUSTOM_REFS_BUNDLE), "git clone failed with:\n" + out.getStdError());
            assertEquals("randomness", git.getCurrentBranch());

            assertDoesNotThrow(() -> git.fetch(out), () -> "git fetch failed with:\n" + out.getStdError());
            assertEquals("+" + git.expandRefSpec(), git.getConfigValue("remote.origin.fetch"));
            assertEquals(git(sandbox, "rev-parse", "HEAD"), git(sandbox, "rev-parse", "origin/randomness"));
            assertEquals(git(sandbox, "ls-remote", "origin", "refs/random/things").split("\\s")[0], git(sandbox, "rev-parse", "HEAD"));
        }

        private String git(File cwd, String... args) {
            return CommandLine.createCommandLine("git").
                    withEncoding("UTF-8").
                    withWorkingDir(cwd).
                    withArgs(args).runOrBomb(new NamedProcessTag(null)).outputAsString();
        }
    }

    @Nested
    @ExtendWith(SystemStubsExtension.class)
    class Integration {
        private static final String BRANCH = "foo";
        private static final String SUBMODULE = "submodule-1";

        @TempDir
        Path tempDir;

        @SystemStub
        private SystemProperties systemProperties;
        private final Date THREE_DAYS_FROM_NOW = setMilliseconds(addDays(new Date(), 3), 0);
        private GitCommand git;
        private String repoUrl;
        private File repoLocation;
        private GitTestRepo gitRepo;
        private File gitLocalRepoDir;
        private GitTestRepo gitFooBranchBundle;

        @BeforeEach
        void setup() throws IOException {
            gitRepo = new GitTestRepo(tempDir);
            gitLocalRepoDir = createTempWorkingDirectory();
            git = new GitCommand(null, gitLocalRepoDir, GitMaterialConfig.DEFAULT_BRANCH, false, null);
            repoLocation = gitRepo.gitRepository();
            repoUrl = gitRepo.projectRepositoryUrl();
            InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
            int returnCode = git.cloneWithNoCheckout(outputStreamConsumer, repoUrl);
            if (returnCode > 0) {
                fail(outputStreamConsumer.getAllOutput());
            }
            gitFooBranchBundle = GitTestRepo.testRepoAtBranch(GIT_FOO_BRANCH_BUNDLE, BRANCH, tempDir);
        }

        @AfterEach
        void teardown() {
            unsetColoring();
            unsetLogDecoration();
        }

        @Test
        void shouldCloneFromMasterWhenNoBranchIsSpecified() {
            InMemoryStreamConsumer output = inMemoryConsumer();
            git.clone(output, repoUrl);
            CommandLine commandLine = CommandLine.createCommandLine("git").withEncoding("UTF-8").withArg("branch").withWorkingDir(gitLocalRepoDir);
            commandLine.run(output, null);
            assertEquals("* master", output.getStdOut());
        }

        @Test
        void freshCloneDoesNotHaveWorkingCopy() {
            assertWorkingCopyNotCheckedOut();
        }

        @Test
        void freshCloneOnAgentSideShouldHaveWorkingCopyCheckedOut() throws IOException {
            InMemoryStreamConsumer output = inMemoryConsumer();
            File workingDir = createTempWorkingDirectory();
            GitCommand git = new GitCommand(null, workingDir, GitMaterialConfig.DEFAULT_BRANCH, false, null);

            git.clone(output, repoUrl);

            assertWorkingCopyCheckedOut(workingDir);
        }

        @Test
        void fullCloneIsNotShallow() {
            assertFalse(git.isShallow());
        }

        @Test
        void shouldOnlyCloneLimitedRevisionsIfDepthSpecified() {
            FileUtils.deleteQuietly(this.gitLocalRepoDir);
            git.clone(inMemoryConsumer(), repoUrl, 2);
            assertTrue(git.isShallow());
            assertTrue(git.containsRevisionInBranch(GitTestRepo.REVISION_4));
            assertTrue(git.containsRevisionInBranch(GitTestRepo.REVISION_3));
            // can not assert on revision_2, because on old version of git (1.7)
            // depth '2' actually clone 3 revisions
            assertFalse(git.containsRevisionInBranch(GitTestRepo.REVISION_1));
            assertFalse(git.containsRevisionInBranch(GitTestRepo.REVISION_0));

        }

        @Test
        void unshallowALocalRepoWithArbitraryDepth() {
            FileUtils.deleteQuietly(this.gitLocalRepoDir);
            git.clone(inMemoryConsumer(), repoUrl, 2);
            git.unshallow(inMemoryConsumer(), 3);
            assertTrue(git.isShallow());
            assertTrue(git.containsRevisionInBranch(GitTestRepo.REVISION_2));
            // can not assert on revision_1, because on old version of git (1.7)
            // depth '3' actually clone 4 revisions
            assertFalse(git.containsRevisionInBranch(GitTestRepo.REVISION_0));

            git.unshallow(inMemoryConsumer(), Integer.MAX_VALUE);
            assertFalse(git.isShallow());

            assertTrue(git.containsRevisionInBranch(GitTestRepo.REVISION_0));
        }

        @Test
        void unshallowShouldNotResultInWorkingCopyCheckout() {
            FileUtils.deleteQuietly(this.gitLocalRepoDir);
            git.cloneWithNoCheckout(inMemoryConsumer(), repoUrl);
            git.unshallow(inMemoryConsumer(), 3);
            assertWorkingCopyNotCheckedOut();
        }

        @Test
        void shouldCloneFromBranchWhenMaterialPointsToABranch() throws IOException {
            gitLocalRepoDir = createTempWorkingDirectory();
            git = new GitCommand(null, gitLocalRepoDir, BRANCH, false, null);
            GitCommand branchedGit = new GitCommand(null, gitLocalRepoDir, BRANCH, false, null);
            branchedGit.clone(inMemoryConsumer(), gitFooBranchBundle.projectRepositoryUrl());
            InMemoryStreamConsumer output = inMemoryConsumer();
            CommandLine.createCommandLine("git").withEncoding("UTF-8").withArg("branch").withWorkingDir(gitLocalRepoDir).run(output, null);
            assertEquals("* foo", output.getStdOut());
        }

        @Test
        void shouldGetTheCurrentBranchForTheCheckedOutRepo() throws IOException {
            gitLocalRepoDir = createTempWorkingDirectory();
            CommandLine gitCloneCommand = CommandLine.createCommandLine("git").withEncoding("UTF-8").withArg("clone");
            gitCloneCommand.withArg("--branch=" + BRANCH).withArg(new UrlArgument(gitFooBranchBundle.projectRepositoryUrl())).withArg(gitLocalRepoDir.getAbsolutePath());
            gitCloneCommand.run(inMemoryConsumer(), null);
            git = new GitCommand(null, gitLocalRepoDir, BRANCH, false, null);
            assertEquals(BRANCH, git.getCurrentBranch());
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
        void shouldOutputSubmoduleRevisionsAfterUpdate() throws Exception {
            GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(tempDir);
            submoduleRepos.addSubmodule(SUBMODULE, "sub1");
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
            File submoduleFolder = submoduleRepos.addSubmodule(SUBMODULE, "sub1");
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
        void retrieveLatestModificationShouldNotResultInWorkingCopyCheckOut() {
            git.latestModification();
            assertWorkingCopyNotCheckedOut();
        }

        @Test
        void getModificationsSinceShouldNotResultInWorkingCopyCheckOut() {
            git.modificationsSince(GitTestRepo.REVISION_2);
            assertWorkingCopyNotCheckedOut();
        }

        @Test
        void shouldReturnNothingForModificationsSinceIfARebasedCommitSHAIsPassed() throws IOException {
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
        void shouldBombIfCheckedForModificationsSinceWithASHAThatNoLongerExists() throws IOException {
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
        void shouldReturnTrueIfTheGivenBranchContainsTheRevision() {
            assertTrue(git.containsRevisionInBranch(REVISION_4));
        }

        @Test
        void shouldReturnFalseIfTheGivenBranchDoesNotContainTheRevision() {
            assertFalse(git.containsRevisionInBranch(NON_EXISTENT_REVISION));
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
            GitTestRepo branchedRepo = GitTestRepo.testRepoAtBranch(GIT_FOO_BRANCH_BUNDLE, BRANCH, tempDir);
            GitCommand branchedGit = new GitCommand(null, createTempWorkingDirectory(), BRANCH, false, null);
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
        void shouldRetrieveListOfSubmoduleFolders() throws Exception {
            GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(tempDir);
            submoduleRepos.addSubmodule(SUBMODULE, "sub1");
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
            submoduleRepos.addSubmodule(SUBMODULE, "sub1");
            GitCommand gitWithSubmodule = new GitCommand(null, createTempWorkingDirectory(), GitMaterialConfig.DEFAULT_BRANCH, false, null) {
                //hack to reproduce synchronization issue
                @Override
                public Map<String, String> submoduleUrls() {
                    return Collections.singletonMap("submodule", "submodule");
                }
            };
            InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
            gitWithSubmodule.clone(outputStreamConsumer, submoduleRepos.mainRepo().urlForCommandLine());

            gitWithSubmodule.updateSubmoduleWithInit(outputStreamConsumer, false);

        }

        @Test
        void shouldNotFailIfUnableToRemoveSubmoduleEntryFromConfig() throws Exception {
            GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(tempDir);
            submoduleRepos.addSubmodule(SUBMODULE, "sub1");
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
            File submodule = submoduleRepos.addSubmodule(SUBMODULE, "sub1");
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
        void shouldRetrieveRemoteRepoValue() {
            assertTrue(git.workingRepositoryUrl().originalArgument().startsWith(repoUrl));
        }

        @Test
        void shouldCheckIfRemoteRepoExists() {
            GitCommand gitCommand = withBranch("master");
            gitCommand.checkConnection(git.workingRepositoryUrl());
        }

        @Test
        void shouldThrowExceptionWhenRepoNotExist() {
            GitCommand gitCommand = withBranch("master");

            assertThrows(Exception.class, () -> gitCommand.checkConnection(new UrlArgument("git://does.not.exist")));
        }

        @Test
        void shouldThrowExceptionWhenRemoteBranchDoesNotExist() {
            GitCommand gitCommand = withBranch("Invalid_Branch");

            assertThrows(Exception.class, () -> gitCommand.checkConnection(new UrlArgument(repoUrl)));
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
            File testingFile = checkInNewRemoteFileInFuture(THREE_DAYS_FROM_NOW);

            Modification modification = git.latestModification().get(0);
            assertNotEquals(originalNode, modification.getRevision());
            assertEquals("New checkin of " + testingFile.getName(), modification.getComment());
            assertEquals(THREE_DAYS_FROM_NOW, modification.getModifiedTime());
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
            final String expected = "author:cruise <cceuser@CceDev01.(none)>\n"
                    + "node:ecfab84dd4953105e3301c5992528c2d381c1b8a\n"
                    + "date:2008-12-31 14:32:40 +0800\n"
                    + "description:Moving rakefile to build subdirectory for #2266\n"
                    + "\n"
                    + "author:CceUser <cceuser@CceDev01.(none)>\n"
                    + "node:fd16efeb70fcdbe63338c49995ce9ff7659e6e77\n"
                    + "date:2008-12-31 14:17:06 +0800\n"
                    + "description:Adding rakefile";
            assertEquals(expected, mod.getComment());
        }

        @Test
        void shouldCleanUnversionedFilesInsideSubmodulesBeforeUpdating() throws Exception {
            GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(tempDir);
            String submoduleDirectoryName = "local-submodule";
            submoduleRepos.addSubmodule(SUBMODULE, submoduleDirectoryName);
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

            File remoteSubmoduleLocation = submoduleRepos.addSubmodule(SUBMODULE, submoduleDirectoryName);

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

            submoduleRepos.addSubmodule(SUBMODULE, submoduleDirectoryName);

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
            repoContainingSubmodule.addSubmodule(SUBMODULE, submoduleDirectoryName);

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
            repoContainingSubmodule.addSubmodule(SUBMODULE, submoduleDirectoryName);
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
            System.setProperty("toggle.agent.git.clean.keep.ignored.files", "Y");
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

        @Test
        void shouldNotThrowExceptionWhenSubmoduleIsAddedWithACustomName() {
            git_C(gitLocalRepoDir, "submodule", "add", "--name", "Custom", gitFooBranchBundle.projectRepositoryUrl());
            git.fetchAndResetToHead(inMemoryConsumer(), false);
        }

        private List<File> allFilesIn(File directory) {
            return new ArrayList<>(FileUtils.listFiles(directory, and(fileFileFilter(), prefixFileFilter("file-")), null));
        }

        private File createTempWorkingDirectory() throws IOException {
            return TempDirUtils.createRandomDirectoryIn(tempDir).toFile();
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

        private void gitInRepo(String... args) {
            git_C(gitLocalRepoDir, args);
        }

        /**
         * Like {@code git -C <dir> command [args...]}
         *
         * @param dir  the directory to set as CWD
         * @param args the args to pass to {@code git}
         * @return a {@link ConsoleResult}
         */
        private ConsoleResult git_C(File dir, String... args) {
            CommandLine commandLine = CommandLine.createCommandLine("git");
            commandLine.withArgs(args);
            commandLine.withEncoding("utf-8");
            assertTrue(dir.exists());
            commandLine.setWorkingDir(dir);
            return commandLine.runOrBomb(true, null);
        }

        private void setColoring() {
            gitInRepo("config", "color.diff", "always");
            gitInRepo("config", "color.status", "always");
            gitInRepo("config", "color.interactive", "always");
            gitInRepo("config", "color.branch", "always");
        }

        private void setLogDecoration() {
            gitInRepo("config", "log.decorate", "true");
        }

        private void unsetLogDecoration() {
            gitInRepo("config", "log.decorate", "off");
        }

        private void unsetColoring() {
            gitInRepo("config", "color.diff", "auto");
            gitInRepo("config", "color.status", "auto");
            gitInRepo("config", "color.interactive", "auto");
            gitInRepo("config", "color.branch", "auto");
        }

        private void assertWorkingCopyNotCheckedOut() {
            assertArrayEquals(new File[]{new File(gitLocalRepoDir, ".git")}, gitLocalRepoDir.listFiles());
        }

        private void assertWorkingCopyCheckedOut(File workingDir) {
            assertTrue(requireNonNull(workingDir.listFiles()).length > 1);
        }
    }
}
