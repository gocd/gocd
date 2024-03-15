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
import com.thoughtworks.go.util.NamedProcessTag;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.domain.materials.git.GitTestRepo.*;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.junit.jupiter.api.Assertions.*;

public class GitCommandTest {
    static GitCommand withBranch(String branch) {
        return new GitCommand(null, null, branch, false, null);
    }

    static GitCommand withBranch(String branch, File cwd) {
        return new GitCommand(null, cwd, branch, false, null);
    }

    @Nested
    class BasicCloneIntegration extends GitCommandIntegrationTestBase {

        @Test
        void shouldCloneFromMasterWhenNoBranchIsSpecified() {
            InMemoryStreamConsumer output = inMemoryConsumer();
            git.clone(output, repoUrl);
            CommandLine commandLine = CommandLine.createCommandLine("git").withEncoding(UTF_8).withArg("branch").withWorkingDir(gitLocalRepoDir);
            commandLine.run(output, null);
            assertEquals("* master", output.getStdOut());
        }

        @Test
        void freshCloneIsNonShallowWithoutWorkingCopy() {
            assertWorkingCopyNotCheckedOut();
            assertFalse(git.isShallow());
            assertTrue(git.containsRevisionInBranch(REVISION_4));
            assertFalse(git.containsRevisionInBranch(NON_EXISTENT_REVISION));
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

        private void assertWorkingCopyCheckedOut(File workingDir) {
            assertTrue(requireNonNull(workingDir.listFiles()).length > 1);
        }
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
                    withEncoding(UTF_8).
                    withWorkingDir(cwd).
                    withArgs(args).runOrBomb(new NamedProcessTag(null)).outputAsString();
        }
    }

}
