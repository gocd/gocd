/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.mail.SysOutStreamConsumer;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.command.*;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestRule;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.domain.materials.git.GitTestRepo.*;
import static com.thoughtworks.go.util.DateUtils.parseRFC822;
import static com.thoughtworks.go.util.ReflectionUtil.getField;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.io.filefilter.FileFilterUtils.*;
import static org.apache.commons.lang3.time.DateUtils.addDays;
import static org.apache.commons.lang3.time.DateUtils.setMilliseconds;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.MockitoAnnotations.initMocks;

@EnableRuleMigrationSupport
public class GitCommandTest {
    private static final String BRANCH = "foo";
    private static final String SUBMODULE = "submodule-1";

    private GitCommand git;
    private String repoUrl;
    private File repoLocation;
    private static final Date THREE_DAYS_FROM_NOW = setMilliseconds(addDays(new Date(), 3), 0);
    private GitTestRepo gitRepo;
    private File gitLocalRepoDir;
    private GitTestRepo gitFooBranchBundle;

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public final TestRule restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

    @BeforeEach
    void setup() throws Exception {
        gitRepo = new GitTestRepo(temporaryFolder);
        gitLocalRepoDir = createTempWorkingDirectory();
        git = new GitCommand(null, gitLocalRepoDir, GitMaterialConfig.DEFAULT_BRANCH, false, null);
        repoLocation = gitRepo.gitRepository();
        repoUrl = gitRepo.projectRepositoryUrl();
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        int returnCode = git.cloneWithNoCheckout(outputStreamConsumer, repoUrl);
        if (returnCode > 0) {
            fail(outputStreamConsumer.getAllOutput());
        }
        gitFooBranchBundle = GitTestRepo.testRepoAtBranch(GIT_FOO_BRANCH_BUNDLE, BRANCH, temporaryFolder);
        initMocks(this);
    }

    @AfterEach
    void teardown() throws Exception {
        unsetColoring();
        unsetLogDecoration();
        TestRepo.internalTearDown();
    }

    @Test
    void shouldDefaultToMasterIfNoBranchIsSpecified() {
        assertThat(getField(new GitCommand(null, gitLocalRepoDir, null, false, null), "branch")).isEqualTo("master");
        assertThat(getField(new GitCommand(null, gitLocalRepoDir, " ", false, null), "branch")).isEqualTo("master");
        assertThat(getField(new GitCommand(null, gitLocalRepoDir, "master", false, null), "branch")).isEqualTo("master");
        assertThat(getField(new GitCommand(null, gitLocalRepoDir, "branch", false, null), "branch")).isEqualTo("branch");
    }

    @Test
    void shouldCloneFromMasterWhenNoBranchIsSpecified() {
        InMemoryStreamConsumer output = inMemoryConsumer();
        git.clone(output, repoUrl);
        CommandLine commandLine = CommandLine.createCommandLine("git").withEncoding("UTF-8").withArg("branch").withWorkingDir(gitLocalRepoDir);
        commandLine.run(output, null);
        assertThat(output.getStdOut()).isEqualTo("* master");
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
        assertThat(git.isShallow()).isFalse();
    }

    @Test
    void shouldOnlyCloneLimitedRevisionsIfDepthSpecified() throws Exception {
        FileUtils.deleteQuietly(this.gitLocalRepoDir);
        git.clone(inMemoryConsumer(), repoUrl, 2);
        assertThat(git.isShallow()).isTrue();
        assertThat(git.containsRevisionInBranch(GitTestRepo.REVISION_4)).isTrue();
        assertThat(git.containsRevisionInBranch(GitTestRepo.REVISION_3)).isTrue();
        // can not assert on revision_2, because on old version of git (1.7)
        // depth '2' actually clone 3 revisions
        assertThat(git.containsRevisionInBranch(GitTestRepo.REVISION_1)).isFalse();
        assertThat(git.containsRevisionInBranch(GitTestRepo.REVISION_0)).isFalse();

    }

    @Test
    void unshallowALocalRepoWithArbitraryDepth() throws Exception {
        FileUtils.deleteQuietly(this.gitLocalRepoDir);
        git.clone(inMemoryConsumer(), repoUrl, 2);
        git.unshallow(inMemoryConsumer(), 3);
        assertThat(git.isShallow()).isTrue();
        assertThat(git.containsRevisionInBranch(GitTestRepo.REVISION_2)).isTrue();
        // can not assert on revision_1, because on old version of git (1.7)
        // depth '3' actually clone 4 revisions
        assertThat(git.containsRevisionInBranch(GitTestRepo.REVISION_0)).isFalse();

        git.unshallow(inMemoryConsumer(), Integer.MAX_VALUE);
        assertThat(git.isShallow()).isFalse();

        assertThat(git.containsRevisionInBranch(GitTestRepo.REVISION_0)).isTrue();
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
        assertThat(output.getStdOut()).isEqualTo("* foo");
    }

    @Test
    void shouldGetTheCurrentBranchForTheCheckedOutRepo() throws IOException {
        gitLocalRepoDir = createTempWorkingDirectory();
        CommandLine gitCloneCommand = CommandLine.createCommandLine("git").withEncoding("UTF-8").withArg("clone");
        gitCloneCommand.withArg("--branch=" + BRANCH).withArg(new UrlArgument(gitFooBranchBundle.projectRepositoryUrl())).withArg(gitLocalRepoDir.getAbsolutePath());
        gitCloneCommand.run(inMemoryConsumer(), null);
        git = new GitCommand(null, gitLocalRepoDir, BRANCH, false, null);
        assertThat(git.getCurrentBranch()).isEqualTo(BRANCH);
    }

    @Test
    void shouldBombForFetchFailure() throws IOException {
        executeOnGitRepo("git", "remote", "rm", "origin");
        executeOnGitRepo("git", "remote", "add", "origin", "git://user:secret@foo.bar/baz");
        try {
            InMemoryStreamConsumer output = new InMemoryStreamConsumer();
            git.fetch(output);
            fail("should have failed for non 0 return code. Git output was:\n " + output.getAllOutput());
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("git fetch failed for [git://user:******@foo.bar/baz]");
        }
    }

    @Test
    void shouldBombForResettingFailure() throws IOException {
        try {
            git.resetWorkingDir(new SysOutStreamConsumer(), new StringRevision("abcdef"), false);
            fail("should have failed for non 0 return code");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo(String.format("git reset failed for [%s]", gitLocalRepoDir));
        }
    }

    @Test
    void shouldOutputSubmoduleRevisionsAfterUpdate() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitCommand gitWithSubmodule = new GitCommand(null, createTempWorkingDirectory(), GitMaterialConfig.DEFAULT_BRANCH, false, null);
        gitWithSubmodule.clone(inMemoryConsumer(), submoduleRepos.mainRepo().getUrl());
        InMemoryStreamConsumer outConsumer = new InMemoryStreamConsumer();
        gitWithSubmodule.resetWorkingDir(outConsumer, new StringRevision("HEAD"), false);
        Matcher matcher = Pattern.compile(".*^\\s[a-f0-9A-F]{40} sub1 \\(heads/master\\)$.*", Pattern.MULTILINE | Pattern.DOTALL).matcher(outConsumer.getAllOutput());
        assertThat(matcher.matches()).isTrue();
    }

    @Test
    void shouldBombForResetWorkingDirWhenSubmoduleUpdateFails() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        File submoduleFolder = submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitCommand gitWithSubmodule = new GitCommand(null, createTempWorkingDirectory(), GitMaterialConfig.DEFAULT_BRANCH, false, null);
        gitWithSubmodule.clone(inMemoryConsumer(), submoduleRepos.mainRepo().getUrl());
        FileUtils.deleteDirectory(submoduleFolder);

        assertThat(submoduleFolder.exists()).isFalse();
        try {
            gitWithSubmodule.resetWorkingDir(new SysOutStreamConsumer(), new StringRevision("HEAD"), false);
            fail("should have failed for non 0 return code");
        } catch (Exception e) {
            assertThat(e.getMessage()).containsPattern(
                String.format("[Cc]lone of '%s' into submodule path '((.*)[\\/])?sub1' failed",
                    Pattern.quote(FileUtil.toFileURI(submoduleFolder.getAbsolutePath()) + "/")));
        }
    }

    @Test
    void shouldRetrieveLatestModification() {
        Modification mod = git.latestModification().get(0);
        assertThat(mod.getUserName()).isEqualTo("Chris Turner");
        assertThat(mod.getEmailAddress()).isEqualTo("cturner@thoughtworks.com");
        assertThat(mod.getComment()).isEqualTo("Added 'run-till-file-exists' ant target");
        assertThat(mod.getModifiedTime()).isEqualTo(parseRFC822("Fri, 12 Feb 2010 16:12:04 -0800"));
        assertThat(mod.getRevision()).isEqualTo("5def073a425dfe239aabd4bf8039ffe3b0e8856b");

        List<ModifiedFile> files = mod.getModifiedFiles();
        assertThat(files.size()).isEqualTo(1);
        assertThat(files.get(0).getFileName()).isEqualTo("build.xml");
        assertThat(files.get(0).getAction()).isEqualTo(ModifiedAction.modified);
    }

    @Test
    void shouldRetrieveLatestModificationWhenColoringIsSetToAlways() {
        setColoring();
        Modification mod = git.latestModification().get(0);
        assertThat(mod.getUserName()).isEqualTo("Chris Turner");
        assertThat(mod.getEmailAddress()).isEqualTo("cturner@thoughtworks.com");
        assertThat(mod.getComment()).isEqualTo("Added 'run-till-file-exists' ant target");
        assertThat(mod.getModifiedTime()).isEqualTo(parseRFC822("Fri, 12 Feb 2010 16:12:04 -0800"));
        assertThat(mod.getRevision()).isEqualTo("5def073a425dfe239aabd4bf8039ffe3b0e8856b");

        List<ModifiedFile> files = mod.getModifiedFiles();
        assertThat(files.size()).isEqualTo(1);
        assertThat(files.get(0).getFileName()).isEqualTo("build.xml");
        assertThat(files.get(0).getAction()).isEqualTo(ModifiedAction.modified);
    }

    @Test
    void shouldRetrieveLatestModificationWhenLogDecorationIsPresent() throws Exception {
        setLogDecoration();
        Modification mod = git.latestModification().get(0);
        assertThat(mod.getUserName()).isEqualTo("Chris Turner");
        assertThat(mod.getEmailAddress()).isEqualTo("cturner@thoughtworks.com");
        assertThat(mod.getComment()).isEqualTo("Added 'run-till-file-exists' ant target");
        assertThat(mod.getModifiedTime()).isEqualTo(parseRFC822("Fri, 12 Feb 2010 16:12:04 -0800"));
        assertThat(mod.getRevision()).isEqualTo("5def073a425dfe239aabd4bf8039ffe3b0e8856b");

        List<ModifiedFile> files = mod.getModifiedFiles();
        assertThat(files.size()).isEqualTo(1);
        assertThat(files.get(0).getFileName()).isEqualTo("build.xml");
        assertThat(files.get(0).getAction()).isEqualTo(ModifiedAction.modified);
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
        GitTestRepo remoteRepo = new GitTestRepo(temporaryFolder);
        executeOnGitRepo("git", "remote", "rm", "origin");
        executeOnGitRepo("git", "remote", "add", "origin", remoteRepo.projectRepositoryUrl());
        GitCommand command = new GitCommand(remoteRepo.createMaterial().getFingerprint(), gitLocalRepoDir, "master", false, null);

        Modification modification = remoteRepo.addFileAndAmend("foo", "amendedCommit").get(0);

        assertThat(command.modificationsSince(new StringRevision(modification.getRevision()))).isEmpty();

    }

    @Test
    void shouldReturnTheRebasedCommitForModificationsSinceTheRevisionBeforeRebase() throws IOException {
        GitTestRepo remoteRepo = new GitTestRepo(temporaryFolder);
        executeOnGitRepo("git", "remote", "rm", "origin");
        executeOnGitRepo("git", "remote", "add", "origin", remoteRepo.projectRepositoryUrl());
        GitCommand command = new GitCommand(remoteRepo.createMaterial().getFingerprint(), gitLocalRepoDir, "master", false, null);

        Modification modification = remoteRepo.addFileAndAmend("foo", "amendedCommit").get(0);

        assertThat(command.modificationsSince(REVISION_4).get(0)).isEqualTo(modification);
    }

    @Test
    void shouldReturnTheRebasedCommitForModificationsSinceTheRevisionBeforeRebaseWithColoringIsSetToAlways() throws IOException {
        GitTestRepo remoteRepo = new GitTestRepo(temporaryFolder);
        executeOnGitRepo("git", "remote", "rm", "origin");
        executeOnGitRepo("git", "remote", "add", "origin", remoteRepo.projectRepositoryUrl());
        GitCommand command = new GitCommand(remoteRepo.createMaterial().getFingerprint(), gitLocalRepoDir, "master", false, null);

        Modification modification = remoteRepo.addFileAndAmend("foo", "amendedCommit").get(0);
        setColoring();

        assertThat(command.modificationsSince(REVISION_4).get(0)).isEqualTo(modification);
    }

    @Test
    void shouldReturnTheRebasedCommitForModificationsSinceTheRevisionBeforeRebaseWithLogDecoration() throws IOException {
        GitTestRepo remoteRepo = new GitTestRepo(temporaryFolder);
        executeOnGitRepo("git", "remote", "rm", "origin");
        executeOnGitRepo("git", "remote", "add", "origin", remoteRepo.projectRepositoryUrl());
        GitCommand command = new GitCommand(remoteRepo.createMaterial().getFingerprint(), gitLocalRepoDir, "master", false, null);

        Modification modification = remoteRepo.addFileAndAmend("foo", "amendedCommit").get(0);
        setLogDecoration();

        assertThat(command.modificationsSince(REVISION_4).get(0)).isEqualTo(modification);
    }

    @Test
    void shouldBombIfCheckedForModificationsSinceWithASHAThatNoLongerExists() throws IOException {
        GitTestRepo remoteRepo = new GitTestRepo(temporaryFolder);
        executeOnGitRepo("git", "remote", "rm", "origin");
        executeOnGitRepo("git", "remote", "add", "origin", remoteRepo.projectRepositoryUrl());
        GitCommand command = new GitCommand(remoteRepo.createMaterial().getFingerprint(), gitLocalRepoDir, "master", false, null);

        Modification modification = remoteRepo.checkInOneFile("foo", "Adding a commit").get(0);
        remoteRepo.addFileAndAmend("bar", "amendedCommit");

        assertThatCode(() -> command.modificationsSince(new StringRevision(modification.getRevision())))
            .isInstanceOf(CommandLineException.class);
    }

    @Test
    void shouldBombIfCheckedForModificationsSinceWithANonExistentRef() throws IOException {
        GitTestRepo remoteRepo = new GitTestRepo(temporaryFolder);
        executeOnGitRepo("git", "remote", "rm", "origin");
        executeOnGitRepo("git", "remote", "add", "origin", remoteRepo.projectRepositoryUrl());
        GitCommand command = new GitCommand(remoteRepo.createMaterial().getFingerprint(), gitLocalRepoDir, "non-existent-branch", false, null);

        Modification modification = remoteRepo.checkInOneFile("foo", "Adding a commit").get(0);

        assertThatCode(() -> command.modificationsSince(new StringRevision(modification.getRevision())))
            .isInstanceOf(CommandLineException.class);
    }

    @Test
    void shouldBombWhileRetrievingLatestModificationFromANonExistentRef() throws IOException {
        expectedException.expect(CommandLineException.class);
        expectedException.expectMessage("ambiguous argument 'origin/non-existent-branch': unknown revision or path not in the working tree.");
        GitTestRepo remoteRepo = new GitTestRepo(temporaryFolder);
        executeOnGitRepo("git", "remote", "rm", "origin");
        executeOnGitRepo("git", "remote", "add", "origin", remoteRepo.projectRepositoryUrl());
        GitCommand command = new GitCommand(remoteRepo.createMaterial().getFingerprint(), gitLocalRepoDir, "non-existent-branch", false, null);

        command.latestModification();
    }

    @Test
    void shouldReturnTrueIfTheGivenBranchContainsTheRevision() {
        assertThat(git.containsRevisionInBranch(REVISION_4)).isTrue();
    }

    @Test
    void shouldReturnFalseIfTheGivenBranchDoesNotContainTheRevision() {
        assertThat(git.containsRevisionInBranch(NON_EXISTENT_REVISION)).isFalse();
    }

    @Test
    void shouldRetrieveFilenameForInitialRevision() throws IOException {
        GitTestRepo testRepo = new GitTestRepo(GitTestRepo.GIT_SUBMODULE_REF_BUNDLE, temporaryFolder);
        GitCommand gitCommand = new GitCommand(null, testRepo.gitRepository(), GitMaterialConfig.DEFAULT_BRANCH, false, null);
        Modification modification = gitCommand.latestModification().get(0);
        assertThat(modification.getModifiedFiles()).hasSize(1);
        assertThat(modification.getModifiedFiles().get(0).getFileName()).isEqualTo("remote.txt");
    }

    @Test
    void shouldRetrieveLatestModificationFromBranch() throws Exception {
        GitTestRepo branchedRepo = GitTestRepo.testRepoAtBranch(GIT_FOO_BRANCH_BUNDLE, BRANCH, temporaryFolder);
        GitCommand branchedGit = new GitCommand(null, createTempWorkingDirectory(), BRANCH, false, null);
        branchedGit.clone(inMemoryConsumer(), branchedRepo.projectRepositoryUrl());

        Modification mod = branchedGit.latestModification().get(0);

        assertThat(mod.getUserName()).isEqualTo("Chris Turner");
        assertThat(mod.getEmailAddress()).isEqualTo("cturner@thoughtworks.com");
        assertThat(mod.getComment()).isEqualTo("Started foo branch");
        assertThat(mod.getModifiedTime()).isEqualTo(parseRFC822("Tue, 05 Feb 2009 14:28:08 -0800"));
        assertThat(mod.getRevision()).isEqualTo("b4fa7271c3cef91822f7fa502b999b2eab2a380d");

        List<ModifiedFile> files = mod.getModifiedFiles();
        assertThat(files).hasSize(1);
        assertThat(files.get(0).getFileName()).isEqualTo("first.txt");
        assertThat(files.get(0).getAction()).isEqualTo(ModifiedAction.modified);
    }

    @Test
    void shouldRetrieveListOfSubmoduleFolders() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitCommand gitWithSubmodule = new GitCommand(null, createTempWorkingDirectory(), GitMaterialConfig.DEFAULT_BRANCH, false, null);
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        gitWithSubmodule.clone(outputStreamConsumer, submoduleRepos.mainRepo().getUrl());
        gitWithSubmodule.fetchAndResetToHead(outputStreamConsumer);
        gitWithSubmodule.updateSubmoduleWithInit(outputStreamConsumer, false);
        List<String> folders = gitWithSubmodule.submoduleFolders();
        assertThat(folders).hasSize(1);
        assertThat(folders.get(0)).isEqualTo("sub1");
    }

    @Test
    void shouldNotThrowErrorWhenConfigRemoveSectionFails() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitCommand gitWithSubmodule = new GitCommand(null, createTempWorkingDirectory(), GitMaterialConfig.DEFAULT_BRANCH, false, null) {
            //hack to reproduce synchronization issue
            @Override
            public Map<String, String> submoduleUrls() {
                return Collections.singletonMap("submodule", "submodule");
            }
        };
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        gitWithSubmodule.clone(outputStreamConsumer, submoduleRepos.mainRepo().getUrl());

        gitWithSubmodule.updateSubmoduleWithInit(outputStreamConsumer, false);

    }

    @Test
    void shouldNotFailIfUnableToRemoveSubmoduleEntryFromConfig() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitCommand gitWithSubmodule = new GitCommand(null, createTempWorkingDirectory(), GitMaterialConfig.DEFAULT_BRANCH, false, null);
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        gitWithSubmodule.clone(outputStreamConsumer, submoduleRepos.mainRepo().getUrl());
        gitWithSubmodule.fetchAndResetToHead(outputStreamConsumer);
        gitWithSubmodule.updateSubmoduleWithInit(outputStreamConsumer, false);
        List<String> folders = gitWithSubmodule.submoduleFolders();
        assertThat(folders).hasSize(1);
        assertThat(folders.get(0)).isEqualTo("sub1");
    }

    @Test
    void shouldRetrieveSubmoduleUrls() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        File submodule = submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitCommand gitWithSubmodule = new GitCommand(null, createTempWorkingDirectory(), GitMaterialConfig.DEFAULT_BRANCH, false, null);
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        gitWithSubmodule.clone(outputStreamConsumer, submoduleRepos.mainRepo().getUrl());
        gitWithSubmodule.fetchAndResetToHead(outputStreamConsumer);

        gitWithSubmodule.updateSubmoduleWithInit(outputStreamConsumer, false);
        Map<String, String> urls = gitWithSubmodule.submoduleUrls();
        assertThat(urls).hasSize(1);
        assertThat(urls.containsKey("sub1")).isTrue();
        assertThat(urls.get("sub1")).isEqualTo(FileUtil.toFileURI(submodule));
    }

    @Test
    void shouldRetrieveZeroSubmoduleUrlsIfTheyAreNotConfigured() {
        Map<String, String> submoduleUrls = git.submoduleUrls();
        assertThat(submoduleUrls).isEmpty();
    }

    @Test
    void shouldRetrieveRemoteRepoValue() {
        assertThat(git.workingRepositoryUrl().originalArgument()).startsWith(repoUrl);
    }

    @Test
    void shouldCheckIfRemoteRepoExists() {
        GitCommand gitCommand = new GitCommand(null, null, null, false, null);
        gitCommand.checkConnection(git.workingRepositoryUrl(), "master");
    }

    @Test
    void shouldThrowExceptionWhenRepoNotExist() {
        GitCommand gitCommand = new GitCommand(null, null, null, false, null);

        assertThatCode(() -> gitCommand.checkConnection(new UrlArgument("git://somewhere.is.not.exist"), "master"))
            .isInstanceOf(Exception.class);
    }

    @Test
    void shouldThrowExceptionWhenRemoteBranchDoesNotExist() {
        GitCommand gitCommand = new GitCommand(null, null, null, false, null);

        assertThatCode(() -> gitCommand.checkConnection(new UrlArgument(gitRepo.projectRepositoryUrl()), "Invalid_Branch"))
            .isInstanceOf(Exception.class);
    }


    @Test
    void shouldIncludeNewChangesInModificationCheck() throws Exception {
        String originalNode = git.latestModification().get(0).getRevision();
        File testingFile = checkInNewRemoteFile();

        Modification modification = git.latestModification().get(0);
        assertThat(modification.getRevision()).isNotEqualTo(originalNode);
        assertThat(modification.getComment()).isEqualTo("New checkin of " + testingFile.getName());
        assertThat(modification.getModifiedFiles()).hasSize(1);
        assertThat(modification.getModifiedFiles().get(0).getFileName()).isEqualTo(testingFile.getName());
    }

    @Test
    void shouldIncludeChangesFromTheFutureInModificationCheck() throws Exception {
        String originalNode = git.latestModification().get(0).getRevision();
        File testingFile = checkInNewRemoteFileInFuture(THREE_DAYS_FROM_NOW);

        Modification modification = git.latestModification().get(0);
        assertThat(modification.getRevision()).isNotEqualTo(originalNode);
        assertThat(modification.getComment()).isEqualTo("New checkin of " + testingFile.getName());
        assertThat(modification.getModifiedTime()).isEqualTo(THREE_DAYS_FROM_NOW);
    }

    @Test
    void shouldThrowExceptionIfRepoCanNotConnectWhenModificationCheck() {
        FileUtils.deleteQuietly(repoLocation);
        try {
            git.latestModification();
            fail("Should throw exception when repo cannot connected");
        } catch (Exception e) {
            assertThat(e.getMessage()).matches(str -> str.contains("The remote end hung up unexpectedly") ||
                str.contains("Could not read from remote repository"));
        }
    }

    @Test
    void shouldCleanUnversionedFilesInsideSubmodulesBeforeUpdating() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        String submoduleDirectoryName = "local-submodule";
        submoduleRepos.addSubmodule(SUBMODULE, submoduleDirectoryName);
        File cloneDirectory = createTempWorkingDirectory();
        GitCommand clonedCopy = new GitCommand(null, cloneDirectory, GitMaterialConfig.DEFAULT_BRANCH, false, null);
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        clonedCopy.clone(outputStreamConsumer, submoduleRepos.mainRepo().getUrl()); // Clone repository without submodules
        clonedCopy.resetWorkingDir(outputStreamConsumer, new StringRevision("HEAD"), false);  // Pull submodules to working copy - Pipeline counter 1
        File unversionedFile = new File(new File(cloneDirectory, submoduleDirectoryName), "unversioned_file.txt");
        FileUtils.writeStringToFile(unversionedFile, "this is an unversioned file. lets see you deleting me.. come on.. I dare you!!!!", UTF_8);

        clonedCopy.resetWorkingDir(outputStreamConsumer, new StringRevision("HEAD"), false); // Should clean unversioned file on next fetch - Pipeline counter 2

        assertThat(unversionedFile.exists()).isFalse();
    }

    @Test
    void shouldRemoveChangesToModifiedFilesInsideSubmodulesBeforeUpdating() throws Exception {
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        String submoduleDirectoryName = "local-submodule";
        File cloneDirectory = createTempWorkingDirectory();

        File remoteSubmoduleLocation = submoduleRepos.addSubmodule(SUBMODULE, submoduleDirectoryName);

        /* Simulate an agent checkout of code. */
        GitCommand clonedCopy = new GitCommand(null, cloneDirectory, GitMaterialConfig.DEFAULT_BRANCH, false, null);
        clonedCopy.clone(outputStreamConsumer, submoduleRepos.mainRepo().getUrl());
        clonedCopy.resetWorkingDir(outputStreamConsumer, new StringRevision("HEAD"), false);

        /* Simulate a local modification of file inside submodule, on agent side. */
        File fileInSubmodule = allFilesIn(new File(cloneDirectory, submoduleDirectoryName), "file-").get(0);
        FileUtils.writeStringToFile(fileInSubmodule, "Some other new content.", UTF_8);

        /* Commit a change to the file on the repo. */
        List<Modification> modifications = submoduleRepos.modifyOneFileInSubmoduleAndUpdateMainRepo(
            remoteSubmoduleLocation, submoduleDirectoryName, fileInSubmodule.getName(), "NEW CONTENT OF FILE");

        /* Simulate start of a new build on agent. */
        clonedCopy.fetch(outputStreamConsumer);
        clonedCopy.resetWorkingDir(outputStreamConsumer, new StringRevision(modifications.get(0).getRevision()), false);

        assertThat(FileUtils.readFileToString(fileInSubmodule, UTF_8)).isEqualTo("NEW CONTENT OF FILE");
    }

    @Test
    void shouldAllowSubmoduleUrlsToChange() throws Exception {
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        String submoduleDirectoryName = "local-submodule";
        File cloneDirectory = createTempWorkingDirectory();

        submoduleRepos.addSubmodule(SUBMODULE, submoduleDirectoryName);

        GitCommand clonedCopy = new GitCommand(null, cloneDirectory, GitMaterialConfig.DEFAULT_BRANCH, false, null);
        clonedCopy.clone(outputStreamConsumer, submoduleRepos.mainRepo().getUrl());
        clonedCopy.fetchAndResetToHead(outputStreamConsumer);

        submoduleRepos.changeSubmoduleUrl(submoduleDirectoryName);

        clonedCopy.fetchAndResetToHead(outputStreamConsumer);
    }

    @Test
    @EnabledOnGitVersionsAbove("2.10.0")
    void shouldShallowCloneSubmodulesWhenSpecified() throws Exception {
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        GitRepoContainingSubmodule repoContainingSubmodule = new GitRepoContainingSubmodule(temporaryFolder);
        String submoduleDirectoryName = "submoduleDir";
        repoContainingSubmodule.addSubmodule(SUBMODULE, submoduleDirectoryName);

        File cloneDirectory = createTempWorkingDirectory();
        GitCommand clonedCopy = new GitCommand(null, cloneDirectory, GitMaterialConfig.DEFAULT_BRANCH, false, null);
        clonedCopy.clone(outputStreamConsumer, FileUtil.toFileURI(repoContainingSubmodule.mainRepo().getUrl()), 1);
        clonedCopy.fetchAndResetToHead(outputStreamConsumer, true);
        ConsoleResult consoleResult = executeOnDir(new File(cloneDirectory, submoduleDirectoryName),
            "git", "rev-list", "--count", "master");
        assertThat(consoleResult.outputAsString()).isEqualTo("1");
    }

    @Test
    @EnabledOnGitVersionsAbove("2.10.0")
    void shouldUnshallowSubmodulesIfSubmoduleUpdateFails() throws Exception {
        InMemoryStreamConsumer outputStreamConsumer = inMemoryConsumer();
        GitRepoContainingSubmodule repoContainingSubmodule = new GitRepoContainingSubmodule(temporaryFolder);
        String submoduleDirectoryName = "submoduleDir";
        repoContainingSubmodule.addSubmodule(SUBMODULE, submoduleDirectoryName);
        repoContainingSubmodule.goBackOneCommitInSubmoduleAndUpdateMainRepo(submoduleDirectoryName);

        File cloneDirectory = createTempWorkingDirectory();
        GitCommand clonedCopy = new GitCommand(null, cloneDirectory, GitMaterialConfig.DEFAULT_BRANCH, false, null);
        clonedCopy.clone(outputStreamConsumer, FileUtil.toFileURI(repoContainingSubmodule.mainRepo().getUrl()), 1);
        clonedCopy.fetchAndResetToHead(outputStreamConsumer, true);
        ConsoleResult consoleResult = executeOnDir(new File(cloneDirectory, submoduleDirectoryName),
            "git", "rev-list", "--count", "master");
        assertThat(consoleResult.outputAsString()).isEqualTo("2");
    }

    @Test
    void shouldCleanIgnoredFilesIfToggleIsDisabled() throws IOException {
        InMemoryStreamConsumer output = inMemoryConsumer();
        File gitIgnoreFile = new File(repoLocation, ".gitignore");
        FileUtils.writeStringToFile(gitIgnoreFile, "*.foo", Charset.forName("UTF-8"));
        gitRepo.addFileAndPush(gitIgnoreFile, "added gitignore");
        git.fetchAndResetToHead(output);

        File ignoredFile = new File(gitLocalRepoDir, "ignored.foo");
        assertThat(ignoredFile.createNewFile()).isTrue();
        git.fetchAndResetToHead(output);
        assertThat(ignoredFile.exists()).isFalse();
    }

    @Test
    void shouldNotCleanIgnoredFilesIfToggleIsEnabled() throws IOException {
        System.setProperty("toggle.agent.git.clean.keep.ignored.files", "Y");
        InMemoryStreamConsumer output = inMemoryConsumer();
        File gitIgnoreFile = new File(repoLocation, ".gitignore");
        FileUtils.writeStringToFile(gitIgnoreFile, "*.foo", Charset.forName("UTF-8"));
        gitRepo.addFileAndPush(gitIgnoreFile, "added gitignore");
        git.fetchAndResetToHead(output);

        File ignoredFile = new File(gitLocalRepoDir, "ignored.foo");
        assertThat(ignoredFile.createNewFile()).isTrue();
        git.fetchAndResetToHead(output);
        assertThat(ignoredFile.exists()).isTrue();
    }

    @Test
    void shouldNotThrowExceptionWhenSubmoduleIsAddedWithACustomName() {
        executeOnDir(gitLocalRepoDir, "git", "submodule", "add", "--name", "Custom", gitFooBranchBundle.projectRepositoryUrl());
        git.fetchAndResetToHead(inMemoryConsumer());
    }

    @Test
    void shouldParseGitCommitsWithSpacesInSubject() throws IOException {
        GitTestRepo testRepo = new GitTestRepo(GIT_WITH_WHITESPACES_IN_SUBJECT, temporaryFolder);
        File tempWorkingDirectory = createTempWorkingDirectory();
        GitCommand git = new GitCommand(null, tempWorkingDirectory, GitMaterialConfig.DEFAULT_BRANCH, false, null);
        git.cloneWithNoCheckout(inMemoryConsumer(), testRepo.projectRepositoryUrl());

        List<Modification> modifications = git.modificationsSince(new StringRevision("5a428bd"));

        assertThat(modifications).hasSize(4);
        assertThat(modifications.stream().map(modification -> modification.getAdditionalDataMap().get("subject")))
            .contains(
                "         more then 3 spaces",
                "   Three spaces",
                " One space in subject line",
                "  Two spaces in subject line"
            );
    }

    @Test
    void shouldParseGitCommitsWithSpacesInMessage() throws IOException {
        GitTestRepo testRepo = new GitTestRepo(GIT_WITH_WHITESPACES_IN_COMMIT, temporaryFolder);
        File tempWorkingDirectory = createTempWorkingDirectory();
        GitCommand git = new GitCommand(null, tempWorkingDirectory, GitMaterialConfig.DEFAULT_BRANCH, false, null);
        git.cloneWithNoCheckout(inMemoryConsumer(), testRepo.projectRepositoryUrl());

        List<Modification> modifications = git.modificationsSince(new StringRevision("29cbc1491d"));

        assertThat(modifications).hasSize(2);
        String lastCommitMessage = "One space in subject line\n" +
            "\n" +
            "            Multiple spaces\n" +
            "    Four spaces\n" +
            "   Three spaces\n" +
            "  Two spaces\n" +
            " One space";
        String secondLastCommitMessage = "No spaces in commit subject\n" +
            "\n" +
            "No spaces in message as well";

        assertThat(modifications.stream().map(Modification::getComment))
            .contains(lastCommitMessage, secondLastCommitMessage);
    }

    private List<File> allFilesIn(File directory, String prefixOfFiles) {
        return new ArrayList<>(FileUtils.listFiles(directory, andFileFilter(fileFileFilter(), prefixFileFilter(prefixOfFiles)), null));
    }

    private File createTempWorkingDirectory() throws IOException {
        return temporaryFolder.newFolder("GitCommandTest" + System.currentTimeMillis(), "repo");
    }

    private File checkInNewRemoteFile() throws IOException {
        GitCommand remoteGit = new GitCommand(null, repoLocation, GitMaterialConfig.DEFAULT_BRANCH, false, null);
        File testingFile = new File(repoLocation, "testing-file" + System.currentTimeMillis() + ".txt");
        testingFile.createNewFile();
        remoteGit.add(testingFile);
        remoteGit.commit("New checkin of " + testingFile.getName());
        return testingFile;
    }

    private File checkInNewRemoteFileInFuture(Date checkinDate) throws IOException {
        GitCommand remoteGit = new GitCommand(null, repoLocation, GitMaterialConfig.DEFAULT_BRANCH, false, null);
        File testingFile = new File(repoLocation, "testing-file" + System.currentTimeMillis() + ".txt");
        testingFile.createNewFile();
        remoteGit.add(testingFile);
        remoteGit.commitOnDate("New checkin of " + testingFile.getName(), checkinDate);
        return testingFile;
    }

    private void executeOnGitRepo(String command, String... args) {
        executeOnDir(gitLocalRepoDir, command, args);
    }

    private ConsoleResult executeOnDir(File dir, String command, String... args) {
        CommandLine commandLine = CommandLine.createCommandLine(command);
        commandLine.withArgs(args);
        commandLine.withEncoding("utf-8");
        assertThat(dir.exists()).isTrue();
        commandLine.setWorkingDir(dir);
        return commandLine.runOrBomb(true, null);
    }

    private void setColoring() {
        executeOnGitRepo("git", "config", "color.diff", "always");
        executeOnGitRepo("git", "config", "color.status", "always");
        executeOnGitRepo("git", "config", "color.interactive", "always");
        executeOnGitRepo("git", "config", "color.branch", "always");
    }

    private void setLogDecoration() throws IOException {
        executeOnGitRepo("git", "config", "log.decorate", "true");
    }

    private void unsetLogDecoration() throws IOException {
        executeOnGitRepo("git", "config", "log.decorate", "off");
    }

    private void unsetColoring() {
        executeOnGitRepo("git", "config", "color.diff", "auto");
        executeOnGitRepo("git", "config", "color.status", "auto");
        executeOnGitRepo("git", "config", "color.interactive", "auto");
        executeOnGitRepo("git", "config", "color.branch", "auto");
    }

    private void assertWorkingCopyNotCheckedOut() {
        assertThat(gitLocalRepoDir.listFiles()).isEqualTo(new File[]{new File(gitLocalRepoDir, ".git")});
    }

    private void assertWorkingCopyCheckedOut(File workingDir) {
        assertThat(workingDir.listFiles().length).isGreaterThan(1);
    }
}
