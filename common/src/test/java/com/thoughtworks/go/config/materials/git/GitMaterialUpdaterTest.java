/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.materials.git;

import com.thoughtworks.go.buildsession.BuildSession;
import com.thoughtworks.go.buildsession.BuildSessionBasedTestCase;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.domain.materials.git.GitCommand;
import com.thoughtworks.go.domain.materials.git.GitMaterialUpdater;
import com.thoughtworks.go.domain.materials.git.GitTestRepo;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.helper.GitRepoContainingSubmodule;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.domain.materials.git.GitTestRepo.*;
import static com.thoughtworks.go.matchers.ConsoleOutMatcherJunit5.assertConsoleOut;
import static com.thoughtworks.go.matchers.FileExistsMatcher.exists;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static junit.framework.TestCase.assertTrue;
import static org.apache.commons.io.filefilter.FileFilterUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

class GitMaterialUpdaterTest extends BuildSessionBasedTestCase {
    private static final String SUBMODULE = "submodule-1";

    private File workingDir;

    @BeforeEach
    void setup() {
        workingDir = new File(sandbox, "working");
    }

    @AfterEach
    void teardown() {
        TestRepo.internalTearDown();
    }

    @Test
    void shouldCreateBuildCommandUpdateToSpecificRevision() throws Exception {
        GitMaterial material = new GitMaterial(new GitTestRepo(temporaryFolder).projectRepositoryUrl(), true);
        File newFile = new File(workingDir, "second.txt");
        updateTo(material, new RevisionContext(REVISION_1, REVISION_0, 2), JobResult.Passed);
        assertThat(console.output()).contains("Start updating files at revision " + REVISION_1.getRevision());
        assertThat(newFile.exists()).isFalse();

        console.clear();
        updateTo(material, new RevisionContext(REVISION_2, REVISION_1, 2), JobResult.Passed);

        assertThat(console.output()).contains("Start updating files at revision " + REVISION_2.getRevision());
        assertThat(newFile.exists()).isTrue();
    }

    @Test
    void shouldRemoveSubmoduleFolderFromWorkingDirWhenSubmoduleIsRemovedFromRepo() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitMaterial gitMaterial = new GitMaterial(submoduleRepos.mainRepo().getUrl(), true);
        StringRevision revision = new StringRevision("origin/master");
        updateTo(gitMaterial, new RevisionContext(revision), JobResult.Passed);
        assertThat(new File(workingDir, "sub1")).exists();
        submoduleRepos.removeSubmodule("sub1");
        updateTo(gitMaterial, new RevisionContext(revision), JobResult.Passed);
        assertThat(new File(workingDir, "sub1")).isNotEqualTo(exists());
    }

    @Test
    void shouldDeleteAndRecheckoutDirectoryWhenUrlChanges() throws Exception {
        updateTo(new GitMaterial(new GitTestRepo(temporaryFolder).projectRepositoryUrl(), true),
                new RevisionContext(new StringRevision("origin/master")), JobResult.Passed);

        File shouldBeRemoved = new File(workingDir, "shouldBeRemoved");
        shouldBeRemoved.createNewFile();
        assertThat(shouldBeRemoved.exists()).isTrue();

        String repositoryUrl = new GitTestRepo(temporaryFolder).projectRepositoryUrl();
        GitMaterial material = new GitMaterial(repositoryUrl, true);
        updateTo(material, new RevisionContext(REVISION_4), JobResult.Passed);
        assertThat(localRepoFor(material).workingRepositoryUrl().originalArgument()).isEqualTo(repositoryUrl);
        assertThat(shouldBeRemoved.exists()).isFalse();
    }

    @Test
    void shouldNotDeleteAndRecheckoutDirectoryWhenUrlSame() throws Exception {
        GitMaterial material = new GitMaterial(new GitTestRepo(temporaryFolder).projectRepositoryUrl(), true);
        updateTo(material, new RevisionContext(new StringRevision("origin/master")), JobResult.Passed);
        File shouldNotBeRemoved = new File(new File(workingDir, ".git"), "shouldNotBeRemoved");
        FileUtils.writeStringToFile(shouldNotBeRemoved, "gundi", UTF_8);
        assertThat(shouldNotBeRemoved.exists()).isTrue();
        updateTo(material, new RevisionContext(new StringRevision("origin/master")), JobResult.Passed);
        assertThat(shouldNotBeRemoved.exists()).as("Should not have deleted whole folder").isTrue();
    }

    /* This is to test the functionality of the private method isRepositoryChanged() */
    @Test
    void shouldNotDeleteAndRecheckoutDirectoryWhenBranchIsBlank() throws Exception {
        String repositoryUrl = new GitTestRepo(temporaryFolder).projectRepositoryUrl();
        GitMaterial material = new GitMaterial(repositoryUrl, false);
        updateTo(material, new RevisionContext(new StringRevision("origin/master")), JobResult.Passed);

        File shouldNotBeRemoved = new File(new File(workingDir, ".git"), "shouldNotBeRemoved");
        FileUtils.writeStringToFile(shouldNotBeRemoved, "Text file", UTF_8);

        GitMaterial material1 = new GitMaterial(repositoryUrl, " ");
        updateTo(material1, new RevisionContext(new StringRevision("origin/master")), JobResult.Passed);
        assertThat(shouldNotBeRemoved.exists()).as("Should not have deleted whole folder").isTrue();
    }

    @Test
    void shouldDeleteAndRecheckoutDirectoryWhenBranchChanges() throws Exception {
        GitTestRepo repoWithBranch = GitTestRepo.testRepoAtBranch(GIT_FOO_BRANCH_BUNDLE, "foo", temporaryFolder);
        GitMaterial material = new GitMaterial(repoWithBranch.projectRepositoryUrl(), true);
        updateTo(material, new RevisionContext(new StringRevision("origin/master")), JobResult.Passed);
        InMemoryStreamConsumer output = inMemoryConsumer();
        CommandLine.createCommandLine("git").withEncoding("UTF-8").withArg("branch").withWorkingDir(workingDir).run(output, "");
        assertThat(output.getStdOut()).isEqualTo("* master");

        GitMaterial material1 = new GitMaterial(repoWithBranch.projectRepositoryUrl(), "foo", null, true);
        updateTo(material1, new RevisionContext(new StringRevision("origin/foo")), JobResult.Passed);

        output = inMemoryConsumer();
        CommandLine.createCommandLine("git").withEncoding("UTF-8").withArg("branch").withWorkingDir(workingDir).run(output, "");
        assertThat(output.getStdOut()).isEqualTo("* foo");
    }

    @Test
    void shouldLogRepoInfoToConsoleOutWithoutFolder() throws Exception {
        String repositoryUrl = new GitTestRepo(temporaryFolder).projectRepositoryUrl();
        GitMaterial material = new GitMaterial(repositoryUrl, false);
        updateTo(material, new RevisionContext(REVISION_1), JobResult.Passed);
        assertThat(console.output()).contains(format("Start updating %s at revision %s from %s", "files", REVISION_1.getRevision(),
                repositoryUrl));
    }

    @Test
    void shouldConvertExistingRepoToFullRepoWhenShallowCloneIsOff() throws IOException {
        String repositoryUrl = new GitTestRepo(temporaryFolder).projectRepositoryUrl();
        GitMaterial shallowMaterial = new GitMaterial(repositoryUrl, true);
        updateTo(shallowMaterial, new RevisionContext(REVISION_3), JobResult.Passed);
        assertThat(localRepoFor(shallowMaterial).isShallow()).isTrue();
        GitMaterial fullMaterial = new GitMaterial(repositoryUrl, false);
        updateTo(fullMaterial, new RevisionContext(REVISION_4), JobResult.Passed);
        assertThat(localRepoFor(fullMaterial).isShallow()).isFalse();
    }

    @Test
    void shouldCleanDirtyFilesUponUpdate() throws IOException {
        String repositoryUrl = new GitTestRepo(temporaryFolder).projectRepositoryUrl();
        GitMaterial material = new GitMaterial(repositoryUrl, true);
        updateTo(material, new RevisionContext(REVISION_4), JobResult.Passed);
        File shouldBeRemoved = new File(workingDir, "shouldBeRemoved");
        assertTrue(shouldBeRemoved.createNewFile());
        updateTo(material, new RevisionContext(REVISION_4), JobResult.Passed);
        assertThat(shouldBeRemoved.exists()).isFalse();
    }

    @Test
    void cloneWithDeepWorkingDir() throws Exception {
        GitMaterial material = new GitMaterial(new GitTestRepo(temporaryFolder).projectRepositoryUrl(), "", "foo/bar/baz", true);
        updateTo(material, new RevisionContext(REVISION_4), JobResult.Passed);
        assertThat(new File(workingDir, "foo/bar/baz/build.xml").exists()).isTrue();
    }

    @Test
    void failureCommandShouldNotLeakPasswordOnUrl() {
        GitMaterial material = new GitMaterial("https://foo:foopassword@this.is.absolute.not.exists", true);
        updateTo(material, new RevisionContext(new StringRevision("origin/master")), JobResult.Failed);
        assertThat(console.output()).contains("https://foo:******@this.is.absolute.not.exists/");
        assertThat(console.output()).doesNotContain("foopassword");
    }

    @Test
    void shouldCleanUnversionedFilesInsideSubmodulesBeforeUpdating() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        String submoduleDirectoryName = "local-submodule";
        submoduleRepos.addSubmodule(SUBMODULE, submoduleDirectoryName);
        GitMaterial material = new GitMaterial(submoduleRepos.projectRepositoryUrl(), true);

        updateTo(material, new RevisionContext(new StringRevision("origin/HEAD")), JobResult.Passed);
        File unversionedFile = new File(new File(workingDir, submoduleDirectoryName), "unversioned_file.txt");
        FileUtils.writeStringToFile(unversionedFile, "this is an unversioned file. lets see you deleting me.. come on.. I dare you!!!!", UTF_8);
        updateTo(material, new RevisionContext(new StringRevision("origin/HEAD")), JobResult.Passed);
        assertThat(unversionedFile.exists()).isEqualTo(false);
    }

    @Test
    void shouldRemoveChangesToModifiedFilesInsideSubmodulesBeforeUpdating() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        String submoduleDirectoryName = "local-submodule";
        File remoteSubmoduleLocation = submoduleRepos.addSubmodule(SUBMODULE, submoduleDirectoryName);
        GitMaterial material = new GitMaterial(submoduleRepos.projectRepositoryUrl(), true);

        updateTo(material, new RevisionContext(new StringRevision("origin/HEAD")), JobResult.Passed);

        /* Simulate a local modification of file inside submodule, on agent side. */
        File fileInSubmodule = allFilesIn(new File(workingDir, submoduleDirectoryName), "file-").get(0);
        FileUtils.writeStringToFile(fileInSubmodule, "Some other new content.", UTF_8);

        /* Commit a change to the file on the repo. */
        List<Modification> modifications = submoduleRepos.modifyOneFileInSubmoduleAndUpdateMainRepo(
                remoteSubmoduleLocation, submoduleDirectoryName, fileInSubmodule.getName(), "NEW CONTENT OF FILE");

        updateTo(material, new RevisionContext(new StringRevision(modifications.get(0).getRevision())), JobResult.Passed);
        assertThat(FileUtils.readFileToString(fileInSubmodule, UTF_8)).isEqualTo("NEW CONTENT OF FILE");
    }

    @Test
    void shouldAllowSubmoduleUrlsToChange() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        String submoduleDirectoryName = "local-submodule";
        submoduleRepos.addSubmodule(SUBMODULE, submoduleDirectoryName);
        GitMaterial material = new GitMaterial(submoduleRepos.projectRepositoryUrl(), true);
        updateTo(material, new RevisionContext(new StringRevision("origin/HEAD")), JobResult.Passed);
        submoduleRepos.changeSubmoduleUrl(submoduleDirectoryName);
        updateTo(material, new RevisionContext(new StringRevision("origin/HEAD")), JobResult.Passed);
        assertThat(console.output()).contains("Synchronizing submodule url for 'local-submodule'");
    }

    @Test
    void shouldOutputSubmoduleRevisionsAfterUpdate() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitMaterial material = new GitMaterial(submoduleRepos.projectRepositoryUrl(), true);
        updateTo(material, new RevisionContext(new StringRevision("origin/HEAD")), JobResult.Passed);
        Matcher matcher = Pattern.compile(".*^\\s[a-f0-9A-F]{40} sub1 \\(heads/master\\)$.*", Pattern.MULTILINE | Pattern.DOTALL).matcher(console.output());
        assertThat(matcher.matches()).isEqualTo(true);
    }

    @Test
    void shouldBombForFetchAndResetWhenSubmoduleUpdateFails() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        File submoduleFolder = submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitMaterial material = new GitMaterial(submoduleRepos.projectRepositoryUrl(), true);
        FileUtils.deleteDirectory(submoduleFolder);
        assertThat(submoduleFolder.exists()).isEqualTo(false);
        updateTo(material, new RevisionContext(new StringRevision("origin/HEAD")), JobResult.Failed);
        assertConsoleOut(console.output()).matchUsingRegex(String.format("[Cc]lone of '%s' into submodule path '((.*)[\\/])?sub1' failed",
                Pattern.quote(FileUtil.toFileURI(submoduleFolder.getAbsolutePath()) + "/")));
    }

    private void updateTo(GitMaterial material, RevisionContext revisionContext, JobResult expectedResult) {
        BuildSession buildSession = newBuildSession();
        JobResult result = buildSession.build(new GitMaterialUpdater(material).updateTo("working", revisionContext));
        assertThat(result).as(buildInfo()).isEqualTo(expectedResult);
    }

    private GitCommand localRepoFor(GitMaterial material) {
        return new GitCommand(material.getFingerprint(), workingDir, GitMaterialConfig.DEFAULT_BRANCH, false, new HashMap<>(), null);
    }

    private List<File> allFilesIn(File directory, String prefixOfFiles) {
        return new ArrayList<>(FileUtils.listFiles(directory, andFileFilter(fileFileFilter(), prefixFileFilter(prefixOfFiles)), null));
    }
}

