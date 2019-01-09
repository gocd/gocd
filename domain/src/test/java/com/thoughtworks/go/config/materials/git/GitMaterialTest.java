/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.domain.materials.git.GitTestRepo;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.helper.GitSubmoduleRepos;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.util.JsonValue;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.domain.materials.git.GitTestRepo.GIT_FOO_BRANCH_BUNDLE;
import static com.thoughtworks.go.matchers.FileExistsMatcher.exists;
import static com.thoughtworks.go.util.JsonUtils.from;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(JunitExtRunner.class)
public class GitMaterialTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public TestName name = new TestName();
    private static final String BRANCH = "foo";

    private GitMaterial git;
    private File workingDir;
    private String repositoryUrl;
    private InMemoryStreamConsumer outputStreamConsumer;

    private static final String GIT_VERSION_1_6_0_2 = "git version 1.6.0.2";
    private static final String GIT_VERSION_1_5_4_3 = "git version 1.5.4.3";
    private static final String GIT_VERSION_1_6_0_2_ON_WINDOWS = "git version 1.6.0.2.1172.ga5ed0";
    private static final String GIT_VERSION_NODE_ON_WINDOWS = "git version ga5ed0asdasd.ga5ed0";
    private static final String SUBMODULE = "submodule-1";
    private GitTestRepo gitFooBranchBundle;

    @Before
    public void setup() throws Exception {
        temporaryFolder.create();
        GitTestRepo gitRepo = new GitTestRepo(temporaryFolder);
        outputStreamConsumer = inMemoryConsumer();
        workingDir = temporaryFolder.newFolder("workingDir-" + System.currentTimeMillis());

        repositoryUrl = gitRepo.projectRepositoryUrl();
        git = new GitMaterial(repositoryUrl);
        gitFooBranchBundle = GitTestRepo.testRepoAtBranch(GIT_FOO_BRANCH_BUNDLE, BRANCH, temporaryFolder);
    }

    @After
    public void teardown() throws Exception {
        temporaryFolder.delete();
        TestRepo.internalTearDown();
    }

    @Test
    public void shouldNotDisplayPasswordInStringRepresentation() {
        GitMaterial git = new GitMaterial("https://user:loser@foo.bar/baz?quux=bang");
        assertThat(git.toString(), not(containsString("loser")));
    }

    @Test
    public void shouldGetLatestModification() throws Exception {
        List<Modification> modifications = git.latestModification(workingDir, new TestSubprocessExecutionContext());
        assertThat(modifications.size(), is(1));
    }

    @Test
    public void shouldNotCheckingOutWorkingCopyUponCallingGetLatestModification() throws Exception {
        git.latestModification(workingDir, new TestSubprocessExecutionContext(true));
        assertWorkingCopyNotCheckedOut(workingDir);
    }

    @Test
    public void shouldGetLatestModificationUsingPassword() {
        GitMaterial gitMaterial = new GitMaterial("http://username:password@0.0.0.0");
        try {
            gitMaterial.latestModification(workingDir, new TestSubprocessExecutionContext());
            fail("should throw exception because url is not reachable");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("******"));
            assertThat(e.getMessage(), not(containsString("password")));
        }
    }

    @Test
    public void shouldGetLatestModificationFromBranch() throws Exception {
        GitTestRepo branchedTestRepo = GitTestRepo.testRepoAtBranch(GIT_FOO_BRANCH_BUNDLE, BRANCH, temporaryFolder);
        GitMaterial branchedGit = new GitMaterial(branchedTestRepo.projectRepositoryUrl(), BRANCH);
        List<Modification> modifications = branchedGit.latestModification(temporaryFolder.newFolder(), new TestSubprocessExecutionContext());
        assertThat(modifications.size(), is(1));
        assertThat(modifications.get(0).getComment(), Matchers.is("Started foo branch"));
    }

    @Test
    public void shouldFindAllModificationsSinceARevision() throws Exception {
        List<Modification> modifications = git.modificationsSince(workingDir, GitTestRepo.REVISION_0, new TestSubprocessExecutionContext());
        assertThat(modifications.size(), is(4));
        assertThat(modifications.get(0).getRevision(), is(GitTestRepo.REVISION_4.getRevision()));
        assertThat(modifications.get(0).getComment(), is("Added 'run-till-file-exists' ant target"));
        assertThat(modifications.get(1).getRevision(), is(GitTestRepo.REVISION_3.getRevision()));
        assertThat(modifications.get(1).getComment(), is("adding build.xml"));
        assertThat(modifications.get(2).getRevision(), is(GitTestRepo.REVISION_2.getRevision()));
        assertThat(modifications.get(2).getComment(), is("Created second.txt from first.txt"));
        assertThat(modifications.get(3).getRevision(), is(GitTestRepo.REVISION_1.getRevision()));
        assertThat(modifications.get(3).getComment(), is("Added second line"));
    }

    @Test
    public void shouldRetrieveLatestModificationIfRevisionIsNotFound() throws IOException {
        List<Modification> modifications = git.modificationsSince(workingDir, GitTestRepo.NON_EXISTENT_REVISION, new TestSubprocessExecutionContext());
        assertThat(modifications, is(git.latestModification(workingDir, new TestSubprocessExecutionContext())));
    }

    @Test
    public void shouldNotCheckingOutWorkingCopyUponCallingModificationsSinceARevision() throws Exception {
        SystemEnvironment mockSystemEnvironment = Mockito.mock(SystemEnvironment.class);
        GitMaterial material = new GitMaterial(repositoryUrl, true);
        when(mockSystemEnvironment.get(SystemEnvironment.GO_SERVER_SHALLOW_CLONE)).thenReturn(false);

        material.modificationsSince(workingDir, GitTestRepo.REVISION_0, new TestSubprocessExecutionContext(mockSystemEnvironment, true));
        assertWorkingCopyNotCheckedOut(workingDir);
    }

    @Test
    public void shouldRetrieveModifiedFiles() throws Exception {
        List<Modification> mods = git.modificationsSince(workingDir, GitTestRepo.REVISION_0, new TestSubprocessExecutionContext());
        List<ModifiedFile> mod1Files = mods.get(0).getModifiedFiles();
        assertThat(mod1Files.size(), Matchers.is(1));
        assertThat(mod1Files.get(0).getFileName(), Matchers.is("build.xml"));
        assertThat(mod1Files.get(0).getAction(), Matchers.is(ModifiedAction.modified));
    }

    @Test
    public void shouldUpdateToSpecificRevision() throws Exception {
        File newFile = new File(workingDir, "second.txt");
        assertThat(outputStreamConsumer.getStdError(), is(""));

        InMemoryStreamConsumer output = inMemoryConsumer();
        git.updateTo(output, workingDir, new RevisionContext(GitTestRepo.REVISION_1, GitTestRepo.REVISION_0, 2), new TestSubprocessExecutionContext());
        assertThat(output.getStdOut(),
                containsString("Start updating files at revision " + GitTestRepo.REVISION_1.getRevision()));
        assertThat(newFile.exists(), is(false));

        output = inMemoryConsumer();
        git.updateTo(output, workingDir, new RevisionContext(GitTestRepo.REVISION_2, GitTestRepo.REVISION_1, 2), new TestSubprocessExecutionContext());
        assertThat(output.getStdOut(),
                containsString("Start updating files at revision " + GitTestRepo.REVISION_2.getRevision()));
        assertThat(newFile.exists(), is(true));
    }

    @Test
    public void shouldRemoveSubmoduleFolderFromWorkingDirWhenSubmoduleIsRemovedFromRepo() throws Exception {
        GitSubmoduleRepos submoduleRepos = new GitSubmoduleRepos(temporaryFolder);
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitMaterial gitMaterial = new GitMaterial(submoduleRepos.mainRepo().getUrl());

        StringRevision revision = new StringRevision("origin/master");
        gitMaterial.updateTo(outputStreamConsumer, workingDir, new RevisionContext(revision), new TestSubprocessExecutionContext());
        assertThat(new File(workingDir, "sub1"), exists());

        submoduleRepos.removeSubmodule("sub1");

        outputStreamConsumer = inMemoryConsumer();
        gitMaterial.updateTo(outputStreamConsumer, workingDir, new RevisionContext(revision), new TestSubprocessExecutionContext());
        assertThat(new File(workingDir, "sub1"), not(exists()));
    }

    @Test
    public void shouldDeleteAndRecheckoutDirectoryWhenUrlChanges() throws Exception {
        git.latestModification(workingDir, new TestSubprocessExecutionContext());

        File shouldBeRemoved = new File(workingDir, "shouldBeRemoved");
        shouldBeRemoved.createNewFile();
        assertThat(shouldBeRemoved.exists(), is(true));

        git = new GitMaterial(new GitTestRepo(temporaryFolder).projectRepositoryUrl());
        git.latestModification(workingDir, new TestSubprocessExecutionContext());
        assertThat("Should have deleted whole folder", shouldBeRemoved.exists(), is(false));
    }

    @Test
    public void shouldNotDeleteAndRecheckoutDirectoryWhenUrlSame() throws Exception {
        git.latestModification(workingDir, new TestSubprocessExecutionContext());

        File shouldNotBeRemoved = new File(new File(workingDir, ".git"), "shouldNotBeRemoved");
        FileUtils.writeStringToFile(shouldNotBeRemoved, "gundi", UTF_8);
        assertThat(shouldNotBeRemoved.exists(), is(true));

        git = new GitMaterial(repositoryUrl);
        git.latestModification(workingDir, new TestSubprocessExecutionContext());
        assertThat("Should not have deleted whole folder", shouldNotBeRemoved.exists(), is(true));
    }

    @Test
    public void shouldNotDeleteAndRecheckoutDirectoryWhenUrlsOnlyDifferInFileProtocol() throws Exception {
        git.latestModification(workingDir, new TestSubprocessExecutionContext());

        File shouldNotBeRemoved = new File(new File(workingDir, ".git"), "shouldNotBeRemoved");
        FileUtils.writeStringToFile(shouldNotBeRemoved, "gundi", UTF_8);
        assertThat(shouldNotBeRemoved.exists(), is(true));

        git = new GitMaterial(repositoryUrl.replace("file://", ""));
        git.latestModification(workingDir, new TestSubprocessExecutionContext());
        assertThat("Should not have deleted whole folder", shouldNotBeRemoved.exists(), is(true));
    }

    /* This is to test the functionality of the private method isRepositoryChanged() */
    @Test
    public void shouldNotDeleteAndRecheckoutDirectoryWhenBranchIsBlank() throws Exception {
        git.latestModification(workingDir, new TestSubprocessExecutionContext());

        File shouldNotBeRemoved = new File(new File(workingDir, ".git"), "shouldNotBeRemoved");
        FileUtils.writeStringToFile(shouldNotBeRemoved, "Text file", UTF_8);

        git = new GitMaterial(repositoryUrl, " ");
        git.latestModification(workingDir, new TestSubprocessExecutionContext());
        assertThat("Should not have deleted whole folder", shouldNotBeRemoved.exists(), is(true));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {EnhancedOSChecker.WINDOWS})
    public void shouldThrowExceptionWhenWorkingDirectoryIsNotGitRepoAndItsUnableToDeleteIt() throws Exception {
        File fileToBeLocked = new File(workingDir, "file");
        RandomAccessFile lockedFile = new RandomAccessFile(fileToBeLocked, "rw");
        FileLock lock = lockedFile.getChannel().lock();
        try {
            git.latestModification(workingDir, new TestSubprocessExecutionContext());
            fail("Should have failed to check modifications since the file is locked and cannot be removed.");
        } catch (Exception e) {
            assertEquals(e.getMessage().trim(), "Failed to delete directory: " + workingDir.getAbsolutePath().trim());
            assertEquals(true, fileToBeLocked.exists());
        }
        finally {
            lock.release();
        }
    }

    @Test
    public void shouldDeleteAndRecheckoutDirectoryWhenBranchChanges() throws Exception {
        git = new GitMaterial(gitFooBranchBundle.projectRepositoryUrl());
        git.latestModification(workingDir, new TestSubprocessExecutionContext());
        InMemoryStreamConsumer output = inMemoryConsumer();
        CommandLine.createCommandLine("git").withEncoding("UTF-8").withArg("branch").withWorkingDir(workingDir).run(output, "");
        assertThat(output.getStdOut(), is("* master"));

        git = new GitMaterial(gitFooBranchBundle.projectRepositoryUrl(), "foo");
        git.latestModification(workingDir, new TestSubprocessExecutionContext());
        output = inMemoryConsumer();
        CommandLine.createCommandLine("git").withEncoding("UTF-8").withArg("branch").withWorkingDir(workingDir).run(output, "");
        assertThat(output.getStdOut(), is("* foo"));
    }

    @Test
    public void shouldCheckRemoteConnection() throws Exception {
        ValidationBean validationBean = git.checkConnection(new TestSubprocessExecutionContext());
        assertThat("Connection should be valid", validationBean.isValid(), is(true));
        String badHost = "http://nonExistantHost/git";
        git = new GitMaterial(badHost);
        validationBean = git.checkConnection(new TestSubprocessExecutionContext());
        assertThat("Connection should not be valid", validationBean.isValid(), is(false));
        assertThat(validationBean.getError(), containsString("Error performing command"));
        assertThat(validationBean.getError(), containsString("git ls-remote http://nonExistantHost/git refs/heads/master"));
    }

    @Test
    public void shouldBeEqualWhenUrlSameForHgMaterial() throws Exception {
        Material material = MaterialsMother.gitMaterials("url1").get(0);
        Material anotherMaterial = MaterialsMother.gitMaterials("url1").get(0);
        assertThat(material.equals(anotherMaterial), is(true));
        assertThat(anotherMaterial.equals(material), is(true));
        assertThat(anotherMaterial.hashCode() == material.hashCode(), is(true));
    }

    @Test
    public void shouldNotBeEqualWhenUrlDifferent() throws Exception {
        Material material1 = MaterialsMother.gitMaterials("url1").get(0);
        Material material2 = MaterialsMother.gitMaterials("url2").get(0);
        assertThat(material1.equals(material2), is(false));
        assertThat(material2.equals(material1), is(false));
    }

    @Test
    public void shouldNotBeEqualWhenTypeDifferent() throws Exception {
        Material material = MaterialsMother.gitMaterials("url1").get(0);
        final Material hgMaterial = MaterialsMother.hgMaterials("url1", "hgdir").get(0);
        assertThat(material.equals(hgMaterial), is(false));
        assertThat(hgMaterial.equals(material), is(false));
    }

    @Test
    public void shouldReturnValidateBean() throws Exception {
        ValidationBean validationBean = git.checkConnection(new TestSubprocessExecutionContext());
        assertThat("Repository should exist", validationBean.isValid(), Matchers.is(true));
    }


    @Test
    public void shouldReturnInValidBean() throws Exception {
        git = new GitMaterial("http://0.0.0.0");
        ValidationBean validationBean = git.checkConnection(new TestSubprocessExecutionContext());
        assertThat("Repository should not exist", validationBean.isValid(), Matchers.is(false));
    }

    @Test
    public void shouldReturnTrueIfVersionHigherThan1Dot6OnLinux() throws Exception {
        assertThat(git.isVersionOnedotSixOrHigher(GIT_VERSION_1_6_0_2), is(true));
    }

    @Test
    public void shouldReturnTrueIfVersionHigherThan1Dot5OnLinux() throws Exception {
        assertThat(git.isVersionOnedotSixOrHigher(GIT_VERSION_1_5_4_3), is(false));
    }

    @Test
    public void shouldReturnTrueIfVersionHigherThan1Dot6OnWindows() throws Exception {
        assertThat(git.isVersionOnedotSixOrHigher(GIT_VERSION_1_6_0_2_ON_WINDOWS), is(true));
    }

    @Test(expected = Exception.class)
    public void shouldReturnFalseWhenVersionIsNotRecgonized() throws Exception {
        git.isVersionOnedotSixOrHigher(GIT_VERSION_NODE_ON_WINDOWS);
    }

    @Test
    public void shouldReturnInvalidBeanWithRootCauseAsLowerVersionInstalled() throws Exception {
        ValidationBean validationBean = git.handleException(new Exception(), GIT_VERSION_1_5_4_3);
        assertThat(validationBean.isValid(), is(false));
        assertThat(validationBean.getError(), containsString(GitMaterial.ERR_GIT_OLD_VERSION));
    }

    @Test
    public void shouldReturnInvalidBeanWithRootCauseAsRepositoryURLIsNotFoundIfVersionIsAbvoe16OnLinux()
            throws Exception {
        ValidationBean validationBean = git.handleException(new Exception("not found!"), GIT_VERSION_1_6_0_2);
        assertThat(validationBean.isValid(), is(false));
        assertThat(validationBean.getError(), containsString("not found!"));
    }

    @Test
    public void shouldReturnInvalidBeanWithRootCauseAsRepositoryURLIsNotFoundIfVersionIsAbvoe16OnWindows()
            throws Exception {
        ValidationBean validationBean = git.handleException(new Exception("not found!"), GIT_VERSION_1_6_0_2_ON_WINDOWS);
        assertThat(validationBean.isValid(), is(false));
        assertThat(validationBean.getError(), containsString("not found!"));
    }


    @Test
    public void shouldReturnInvalidBeanWithRootCauseAsRepositoryURLIsNotFoundIfVersionIsNotKnown() throws Exception {
        ValidationBean validationBean = git.handleException(new Exception("not found!"), GIT_VERSION_NODE_ON_WINDOWS);
        assertThat(validationBean.isValid(), is(false));
        assertThat(validationBean.getError(), containsString("not found!"));
    }

    @Test
    public void shouldThrowExceptionWithUsefulInfoIfFailedToFindModifications() throws Exception {
        final String url = "notExistDir";
        git = new GitMaterial(url);
        try {
            git.modificationsSince(workingDir, GitTestRepo.REVISION_0, new TestSubprocessExecutionContext());
            fail("Should have thrown an exception when failed to clone from an invalid url");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Failed to run git clone command"));
        }
    }

    @Test
    public void shouldBeAbleToConvertToJson() {
        Map<String, Object> json = new LinkedHashMap<>();
        git.toJson(json, new StringRevision("123"));

        JsonValue jsonValue = from(json);
        assertThat(jsonValue.getString("scmType"), is("Git"));
        assertThat(new File(jsonValue.getString("location")), is(new File(git.getUrl())));
        assertThat(jsonValue.getString("action"), is("Modified"));
    }

    @Test
    public void shouldLogRepoInfoToConsoleOutWithoutFolder() throws Exception {
        git.updateTo(outputStreamConsumer, workingDir, new RevisionContext(GitTestRepo.REVISION_1), new TestSubprocessExecutionContext());
        assertThat(outputStreamConsumer.getStdOut(), containsString(
                String.format("Start updating %s at revision %s from %s", "files", GitTestRepo.REVISION_1.getRevision(),
                        git.getUrl())));
    }

    @Test
    public void shouldGetLatestModificationsFromRepoWithSubmodules() throws Exception {
        GitSubmoduleRepos submoduleRepos = new GitSubmoduleRepos(temporaryFolder);
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitMaterial gitMaterial = new GitMaterial(submoduleRepos.mainRepo().getUrl());

        File workingDirectory = temporaryFolder.newFolder();
        Materials materials = new Materials();
        materials.add(gitMaterial);

        MaterialRevisions materialRevisions = materials.latestModification(workingDirectory, new TestSubprocessExecutionContext());
        assertThat(materialRevisions.numberOfRevisions(), is(1));
        MaterialRevision materialRevision = materialRevisions.getMaterialRevision(0);
        assertThat(materialRevision.getRevision().getRevision(), is(submoduleRepos.currentRevision(GitSubmoduleRepos.NAME)));
    }

    @Test
    public void shouldUpdateSubmodules() throws Exception {
        GitSubmoduleRepos submoduleRepos = new GitSubmoduleRepos(temporaryFolder);
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitMaterial gitMaterial = new GitMaterial(submoduleRepos.mainRepo().getUrl());

        File serverWorkingDir = temporaryFolder.newFolder();
        Materials materials = new Materials();
        materials.add(gitMaterial);

        MaterialRevisions materialRevisions = materials.latestModification(serverWorkingDir, new TestSubprocessExecutionContext());

        File agentWorkingDir = temporaryFolder.newFolder();
        MaterialRevision materialRevision = materialRevisions.getMaterialRevision(0);
        materialRevision.updateTo(agentWorkingDir, inMemoryConsumer(), new TestSubprocessExecutionContext());

        File localFile = submoduleRepos.files(GitSubmoduleRepos.NAME).get(0);
        assertThat(new File(agentWorkingDir, localFile.getName()), exists());

        File file = submoduleRepos.files(SUBMODULE).get(0);
        File workingSubmoduleFolder = new File(agentWorkingDir, "sub1");
        assertThat(new File(workingSubmoduleFolder, file.getName()), exists());
    }

    @Test
    public void shouldHaveModificationsWhenSubmoduleIsAdded() throws Exception {
        GitSubmoduleRepos submoduleRepos = new GitSubmoduleRepos(temporaryFolder);
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitMaterial gitMaterial = new GitMaterial(submoduleRepos.mainRepo().getUrl());

        File serverWorkingDir = temporaryFolder.newFolder();

        List<Modification> beforeAdd = gitMaterial.latestModification(serverWorkingDir, new TestSubprocessExecutionContext());

        submoduleRepos.addSubmodule(SUBMODULE, "new-submodule");

        List<Modification> afterAdd = gitMaterial.modificationsSince(serverWorkingDir, new Modifications(beforeAdd).latestRevision(gitMaterial), new TestSubprocessExecutionContext());

        assertThat(afterAdd.size(), is(1));
        assertThat(afterAdd.get(0).getComment(), is("Added submodule new-submodule"));
    }

    @Test
    public void shouldHaveModificationsWhenSubmoduleIsRemoved() throws Exception {
        GitSubmoduleRepos submoduleRepos = new GitSubmoduleRepos(temporaryFolder);
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitMaterial gitMaterial = new GitMaterial(submoduleRepos.mainRepo().getUrl());

        File serverWorkingDir = temporaryFolder.newFolder();

        List<Modification> beforeAdd = gitMaterial.latestModification(serverWorkingDir, new TestSubprocessExecutionContext());

        submoduleRepos.removeSubmodule("sub1");

        List<Modification> after = gitMaterial.modificationsSince(serverWorkingDir, new Modifications(beforeAdd).latestRevision(gitMaterial), new TestSubprocessExecutionContext());

        assertThat(after.size(), is(1));
        assertThat(after.get(0).getComment(), is("Removed submodule sub1"));
    }

    @Test
    public void shouldGenerateSqlCriteriaMapInSpecificOrder() throws Exception {
        Map<String, Object> map = git.getSqlCriteria();
        assertThat(map.size(), is(3));
        Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();
        assertThat(iter.next().getKey(), is("type"));
        assertThat(iter.next().getKey(), is("url"));
        assertThat(iter.next().getKey(), is("branch"));
    }

    /**
     * A git abbreviated hash is 7 chars. See the git documentation.
     */
    @Test
    public void shouldtruncateHashTo7charsforAShortRevision() throws Exception {
        Material git = new GitMaterial("file:///foo");
        assertThat(git.getShortRevision("dc3d7e656831d1b203d8b7a63c4de82e26604e52"), is("dc3d7e6"));
        assertThat(git.getShortRevision("24"), is("24"));
        assertThat(git.getShortRevision(null), is(nullValue()));
    }

    @Test
    public void shouldGetLongDescriptionForMaterial() {
        GitMaterial gitMaterial = new GitMaterial("http://url/", "branch", "folder");
        assertThat(gitMaterial.getLongDescription(), is("URL: http://url/, Branch: branch"));
    }

    @Test
    public void shouldNotUseWorkingDirectoryWhichHasBeenSetupOnMaterial_WhenCheckingForModifications() throws Exception {
        String workingDirSpecifiedInMaterial = "some-working-dir";

        GitMaterial git = new GitMaterial(repositoryUrl, GitMaterialConfig.DEFAULT_BRANCH, workingDirSpecifiedInMaterial);
        List<Modification> modifications = git.latestModification(workingDir, new TestSubprocessExecutionContext());

        assertThat(modifications.size(), is(greaterThan(0)));
        assertThat(new File(workingDir, ".git").isDirectory(), is(true));
        assertThat(new File(new File(workingDir, workingDirSpecifiedInMaterial), ".git").isDirectory(), is(false));
    }

    @Test
    public void shouldNotUseWorkingDirectoryWhichHasBeenSetupOnMaterial_WhenLookingForModificationsSinceAGivenRevision() throws Exception {
        String workingDirSpecifiedInMaterial = "some-working-dir";

        GitMaterial git = new GitMaterial(repositoryUrl, GitMaterialConfig.DEFAULT_BRANCH, workingDirSpecifiedInMaterial);
        List<Modification> modifications = git.modificationsSince(workingDir, GitTestRepo.REVISION_0, new TestSubprocessExecutionContext());

        assertThat(modifications.size(), is(greaterThan(0)));
        assertThat(new File(workingDir, ".git").isDirectory(), is(true));
        assertThat(new File(workingDir, workingDirSpecifiedInMaterial).isDirectory(), is(false));
        assertThat(new File(new File(workingDir, workingDirSpecifiedInMaterial), ".git").isDirectory(), is(false));
    }

    @Test
    public void shouldGetAttributesWithSecureFields() {
        GitMaterial git = new GitMaterial("http://username:password@gitrepo.com", GitMaterialConfig.DEFAULT_BRANCH);
        Map<String, Object> attributes = git.getAttributes(true);

        assertThat(attributes.get("type"), is("git"));
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("git-configuration");
        assertThat(configuration.get("url"), is("http://username:password@gitrepo.com"));
        assertThat(configuration.get("branch"), is(GitMaterialConfig.DEFAULT_BRANCH));
    }

    @Test
    public void shouldGetAttributesWithoutSecureFields() {
        GitMaterial git = new GitMaterial("http://username:password@gitrepo.com", GitMaterialConfig.DEFAULT_BRANCH);
        Map<String, Object> attributes = git.getAttributes(false);

        assertThat(attributes.get("type"), is("git"));
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("git-configuration");
        assertThat(configuration.get("url"), is("http://username:******@gitrepo.com"));
        assertThat(configuration.get("branch"), is(GitMaterialConfig.DEFAULT_BRANCH));
    }

    private void assertWorkingCopyNotCheckedOut(File localWorkingDir) {
        assertThat(localWorkingDir.listFiles(), is(new File[]{new File(localWorkingDir, ".git")}));
    }
}
