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
package com.thoughtworks.go.config.materials.git;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.SecretParam;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.config.materials.PasswordAwareMaterial;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.domain.materials.git.GitCommand;
import com.thoughtworks.go.domain.materials.git.GitTestRepo;
import com.thoughtworks.go.domain.materials.git.GitVersion;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.helper.GitRepoContainingSubmodule;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.util.JsonValue;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.util.*;

import static com.thoughtworks.go.domain.materials.git.GitTestRepo.GIT_FOO_BRANCH_BUNDLE;
import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static com.thoughtworks.go.matchers.FileExistsMatcher.exists;
import static com.thoughtworks.go.util.JsonUtils.from;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@EnableRuleMigrationSupport
public class GitMaterialTest {
    public static final GitVersion GIT_VERSION_1_9 = GitVersion.parse("git version 1.9.0");
    public static final GitVersion GIT_VERSION_1_5 = GitVersion.parse("git version 1.5.4.3");
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public TestName name = new TestName();
    private static final String BRANCH = "foo";
    private static final String SUBMODULE = "submodule-1";
    private InMemoryStreamConsumer outputStreamConsumer;

    @BeforeEach
    void setUp() {
        outputStreamConsumer = inMemoryConsumer();
    }

    @Nested
    class PasswordAware {
        private GitMaterial material;

        @BeforeEach
        void setUp() {
            material = new GitMaterial("some-url");
        }

        @Test
        void shouldBePasswordAwareMaterial() {
            assertThat(material).isInstanceOf(PasswordAwareMaterial.class);
        }

        @Test
        void shouldUpdatePasswordFromConfig() {
            assertThat(material.getPassword()).isNull();

            material.updateFromConfig(git("some-url", "bob", "badger"));

            assertThat(material.getPassword()).isEqualTo("badger");
        }
    }

    @Nested
    class SlowOldTestWhichUsesGitCheckout {
        private GitMaterial git;
        private File workingDir;
        private String repositoryUrl;
        private GitTestRepo gitFooBranchBundle;

        @BeforeEach
        void setup() throws IOException {
            temporaryFolder.create();
            workingDir = temporaryFolder.newFolder("workingDir-" + System.currentTimeMillis());

            GitTestRepo gitRepo = new GitTestRepo(temporaryFolder);

            repositoryUrl = gitRepo.projectRepositoryUrl();
            git = new GitMaterial(repositoryUrl);
            gitFooBranchBundle = GitTestRepo.testRepoAtBranch(GIT_FOO_BRANCH_BUNDLE, BRANCH, temporaryFolder);
        }

        @AfterEach
        void teardown() {
            temporaryFolder.delete();
            TestRepo.internalTearDown();
        }

        @Test
        void shouldNotDisplayPasswordInStringRepresentation() {
            GitMaterial git = new GitMaterial("https://user:loser@foo.bar/baz?quux=bang");
            assertThat(git.toString()).doesNotContain("loser");
        }

        @Test
        void shouldGetLatestModification() {
            List<Modification> modifications = git.latestModification(workingDir, new TestSubprocessExecutionContext());
            assertThat(modifications.size()).isEqualTo(1);
        }

        @Test
        void shouldNotCheckingOutWorkingCopyUponCallingGetLatestModification() {
            git.latestModification(workingDir, new TestSubprocessExecutionContext(true));
            assertWorkingCopyNotCheckedOut(workingDir);
        }

        @Test
        void shouldGetLatestModificationUsingPassword() {
            GitCommand git = new GitCommand(null, new File(""), GitMaterialConfig.DEFAULT_BRANCH, false, null);

            GitMaterial gitMaterial = new GitMaterial("http://username:password@0.0.0.0");
            try {
                gitMaterial.latestModification(workingDir, new TestSubprocessExecutionContext());
                fail("should throw exception because url is not reachable");
            } catch (Exception e) {
                if (!git.version().requiresSubmoduleCommandFix()) {
                    //ugly hack for git >= 2.22. The error message has changed and shows 'connection refused' without any password
                    assertThat(e.getMessage()).contains("******");
                }
                assertThat(e.getMessage()).doesNotContain("password");
            }
        }

        @Test
        void shouldGetLatestModificationFromBranch() throws IOException {
            GitTestRepo branchedTestRepo = GitTestRepo.testRepoAtBranch(GIT_FOO_BRANCH_BUNDLE, BRANCH, temporaryFolder);
            GitMaterial branchedGit = new GitMaterial(branchedTestRepo.projectRepositoryUrl(), BRANCH);
            List<Modification> modifications = branchedGit.latestModification(temporaryFolder.newFolder(), new TestSubprocessExecutionContext());
            assertThat(modifications.size()).isEqualTo(1);
            assertThat(modifications.get(0).getComment()).isEqualTo("Started foo branch");
        }

        @Test
        void shouldFindAllModificationsSinceARevision() {
            List<Modification> modifications = git.modificationsSince(workingDir, GitTestRepo.REVISION_0, new TestSubprocessExecutionContext());
            assertThat(modifications.size()).isEqualTo(4);
            assertThat(modifications.get(0).getRevision()).isEqualTo(GitTestRepo.REVISION_4.getRevision());
            assertThat(modifications.get(0).getComment()).isEqualTo("Added 'run-till-file-exists' ant target");
            assertThat(modifications.get(1).getRevision()).isEqualTo(GitTestRepo.REVISION_3.getRevision());
            assertThat(modifications.get(1).getComment()).isEqualTo("adding build.xml");
            assertThat(modifications.get(2).getRevision()).isEqualTo(GitTestRepo.REVISION_2.getRevision());
            assertThat(modifications.get(2).getComment()).isEqualTo("Created second.txt from first.txt");
            assertThat(modifications.get(3).getRevision()).isEqualTo(GitTestRepo.REVISION_1.getRevision());
            assertThat(modifications.get(3).getComment()).isEqualTo("Added second line");
        }

        @Test
        void shouldRetrieveLatestModificationIfRevisionIsNotFound() throws IOException {
            List<Modification> modifications = git.modificationsSince(workingDir, GitTestRepo.NON_EXISTENT_REVISION, new TestSubprocessExecutionContext());
            assertThat(modifications).isEqualTo(git.latestModification(workingDir, new TestSubprocessExecutionContext()));
        }

        @Test
        void shouldNotCheckingOutWorkingCopyUponCallingModificationsSinceARevision() {
            SystemEnvironment mockSystemEnvironment = mock(SystemEnvironment.class);
            GitMaterial material = new GitMaterial(repositoryUrl, true);
            when(mockSystemEnvironment.get(SystemEnvironment.GO_SERVER_SHALLOW_CLONE)).thenReturn(false);

            material.modificationsSince(workingDir, GitTestRepo.REVISION_0, new TestSubprocessExecutionContext(mockSystemEnvironment, true));
            assertWorkingCopyNotCheckedOut(workingDir);
        }

        @Test
        void shouldRetrieveModifiedFiles() {
            List<Modification> mods = git.modificationsSince(workingDir, GitTestRepo.REVISION_0, new TestSubprocessExecutionContext());
            List<ModifiedFile> mod1Files = mods.get(0).getModifiedFiles();
            assertThat(mod1Files.size()).isEqualTo(1);
            assertThat(mod1Files.get(0).getFileName()).isEqualTo("build.xml");
            assertThat(mod1Files.get(0).getAction()).isEqualTo(ModifiedAction.modified);
        }

        @Test
        void shouldUpdateToSpecificRevision() {
            File newFile = new File(workingDir, "second.txt");
            assertThat(outputStreamConsumer.getStdError()).isEqualTo("");

            InMemoryStreamConsumer output = inMemoryConsumer();
            git.updateTo(output, workingDir, new RevisionContext(GitTestRepo.REVISION_1, GitTestRepo.REVISION_0, 2), new TestSubprocessExecutionContext());
            assertThat(output.getStdOut()).contains("Start updating files at revision " + GitTestRepo.REVISION_1.getRevision());
            assertThat(newFile.exists()).isFalse();

            output = inMemoryConsumer();
            git.updateTo(output, workingDir, new RevisionContext(GitTestRepo.REVISION_2, GitTestRepo.REVISION_1, 2), new TestSubprocessExecutionContext());
            assertThat(output.getStdOut()).contains("Start updating files at revision " + GitTestRepo.REVISION_2.getRevision());
            assertThat(newFile.exists()).isTrue();
        }

        @Test
        void shouldRemoveSubmoduleFolderFromWorkingDirWhenSubmoduleIsRemovedFromRepo() throws Exception {
            GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
            submoduleRepos.addSubmodule(SUBMODULE, "sub1");
            GitMaterial gitMaterial = new GitMaterial(submoduleRepos.mainRepo().getUrl());

            StringRevision revision = new StringRevision("origin/master");
            gitMaterial.updateTo(outputStreamConsumer, workingDir, new RevisionContext(revision), new TestSubprocessExecutionContext());
            assertThat(new File(workingDir, "sub1")).exists();

            submoduleRepos.removeSubmodule("sub1");

            outputStreamConsumer = inMemoryConsumer();
            gitMaterial.updateTo(outputStreamConsumer, workingDir, new RevisionContext(revision), new TestSubprocessExecutionContext());
            assertThat(new File(workingDir, "sub1")).isNotEqualTo(exists());
        }

        @Test
        void shouldDeleteAndRecheckoutDirectoryWhenUrlChanges() throws IOException {
            git.latestModification(workingDir, new TestSubprocessExecutionContext());

            File shouldBeRemoved = new File(workingDir, "shouldBeRemoved");
            shouldBeRemoved.createNewFile();
            assertThat(shouldBeRemoved.exists()).isTrue();

            git = new GitMaterial(new GitTestRepo(temporaryFolder).projectRepositoryUrl());
            git.latestModification(workingDir, new TestSubprocessExecutionContext());
            assertThat(shouldBeRemoved.exists()).as("Should have deleted whole folder").isFalse();
        }

        @Test
        void shouldNotDeleteAndRecheckoutDirectoryWhenUrlSame() throws IOException {
            git.latestModification(workingDir, new TestSubprocessExecutionContext());

            File shouldNotBeRemoved = new File(new File(workingDir, ".git"), "shouldNotBeRemoved");
            FileUtils.writeStringToFile(shouldNotBeRemoved, "gundi", UTF_8);
            assertThat(shouldNotBeRemoved.exists()).isTrue();

            git = new GitMaterial(repositoryUrl);
            git.latestModification(workingDir, new TestSubprocessExecutionContext());
            assertThat(shouldNotBeRemoved.exists()).as("Should not have deleted whole folder").isTrue();
        }

        @Test
        void shouldNotDeleteAndRecheckoutDirectoryWhenUrlsOnlyDifferInFileProtocol() throws IOException {
            git.latestModification(workingDir, new TestSubprocessExecutionContext());

            File shouldNotBeRemoved = new File(new File(workingDir, ".git"), "shouldNotBeRemoved");
            FileUtils.writeStringToFile(shouldNotBeRemoved, "gundi", UTF_8);
            assertThat(shouldNotBeRemoved.exists()).isTrue();

            git = new GitMaterial(repositoryUrl.replace("file://", ""));
            git.latestModification(workingDir, new TestSubprocessExecutionContext());
            assertThat(shouldNotBeRemoved.exists()).as("Should not have deleted whole folder").isTrue();
        }

        /* This is to test the functionality of the private method isRepositoryChanged() */
        @Test
        void shouldNotDeleteAndRecheckoutDirectoryWhenBranchIsBlank() throws IOException {
            git.latestModification(workingDir, new TestSubprocessExecutionContext());

            File shouldNotBeRemoved = new File(new File(workingDir, ".git"), "shouldNotBeRemoved");
            FileUtils.writeStringToFile(shouldNotBeRemoved, "Text file", UTF_8);

            git = new GitMaterial(repositoryUrl, " ");
            git.latestModification(workingDir, new TestSubprocessExecutionContext());
            assertThat(shouldNotBeRemoved.exists()).as("Should not have deleted whole folder").isTrue();
        }

        @Test
        @EnabledOnOs({OS.WINDOWS})
        void shouldThrowExceptionWhenWorkingDirectoryIsNotGitRepoAndItsUnableToDeleteIt() throws IOException {
            File fileToBeLocked = new File(workingDir, "file");
            RandomAccessFile lockedFile = new RandomAccessFile(fileToBeLocked, "rw");
            FileLock lock = lockedFile.getChannel().lock();
            try {
                git.latestModification(workingDir, new TestSubprocessExecutionContext());
                fail("Should have failed to check modifications since the file is locked and cannot be removed.");
            } catch (Exception e) {
                assertThat("Failed to delete directory: " + workingDir.getAbsolutePath().trim()).isEqualTo(e.getMessage().trim());
                assertThat(fileToBeLocked.exists()).isTrue();
            } finally {
                lock.release();
            }
        }

        @Test
        void shouldDeleteAndRecheckoutDirectoryWhenBranchChanges() throws Exception {
            git = new GitMaterial(gitFooBranchBundle.projectRepositoryUrl());
            git.latestModification(workingDir, new TestSubprocessExecutionContext());
            InMemoryStreamConsumer output = inMemoryConsumer();
            CommandLine.createCommandLine("git").withEncoding("UTF-8").withArg("branch").withWorkingDir(workingDir).run(output, null);
            assertThat(output.getStdOut()).isEqualTo("* master");

            git = new GitMaterial(gitFooBranchBundle.projectRepositoryUrl(), "foo");
            git.latestModification(workingDir, new TestSubprocessExecutionContext());
            output = inMemoryConsumer();
            CommandLine.createCommandLine("git").withEncoding("UTF-8").withArg("branch").withWorkingDir(workingDir).run(output, null);
            assertThat(output.getStdOut()).isEqualTo("* foo");
        }

        @Test
        void shouldCheckRemoteConnection() {
            ValidationBean validationBean = git.checkConnection(new TestSubprocessExecutionContext());
            assertThat(validationBean.isValid()).as("Connection should be valid").isTrue();
            String badHost = "http://nonExistantHost/git";
            git = new GitMaterial(badHost);
            validationBean = git.checkConnection(new TestSubprocessExecutionContext());
            assertThat(validationBean.isValid()).as("Connection should not be valid").isFalse();
            assertThat(validationBean.getError()).contains("Error performing command");
            assertThat(validationBean.getError()).contains("git ls-remote http://nonExistantHost/git refs/heads/master");
        }

        @Test
        void shouldThrowExceptionWithUsefulInfoIfFailedToFindModifications() {
            final String url = "notExistDir";
            GitMaterial git = new GitMaterial(url);

            assertThatCode(() -> git.modificationsSince(workingDir, GitTestRepo.REVISION_0, new TestSubprocessExecutionContext()))
                    .hasMessageContaining("Failed to run git clone command");
        }

        @Test
        void shouldLogRepoInfoToConsoleOutWithoutFolder() {
            git.updateTo(outputStreamConsumer, workingDir, new RevisionContext(GitTestRepo.REVISION_1), new TestSubprocessExecutionContext());
            assertThat(outputStreamConsumer.getStdOut()).contains(String.format("Start updating %s at revision %s from %s", "files", GitTestRepo.REVISION_1.getRevision(), git.getUriForDisplay()));
        }

        @Test
        void shouldNotUseWorkingDirectoryWhichHasBeenSetupOnMaterial_WhenCheckingForModifications() {
            String workingDirSpecifiedInMaterial = "some-working-dir";

            GitMaterial git = new GitMaterial(repositoryUrl, GitMaterialConfig.DEFAULT_BRANCH, workingDirSpecifiedInMaterial);
            List<Modification> modifications = git.latestModification(workingDir, new TestSubprocessExecutionContext());

            assertThat(modifications.size()).isGreaterThan(0);
            assertThat(new File(workingDir, ".git").isDirectory()).isTrue();
            assertThat(new File(new File(workingDir, workingDirSpecifiedInMaterial), ".git").isDirectory()).isFalse();
        }

        @Test
        void shouldNotUseWorkingDirectoryWhichHasBeenSetupOnMaterial_WhenLookingForModificationsSinceAGivenRevision() {
            String workingDirSpecifiedInMaterial = "some-working-dir";

            GitMaterial git = new GitMaterial(repositoryUrl, GitMaterialConfig.DEFAULT_BRANCH, workingDirSpecifiedInMaterial);
            List<Modification> modifications = git.modificationsSince(workingDir, GitTestRepo.REVISION_0, new TestSubprocessExecutionContext());

            assertThat(modifications.size()).isGreaterThan(0);
            assertThat(new File(workingDir, ".git").isDirectory()).isTrue();
            assertThat(new File(workingDir, workingDirSpecifiedInMaterial).isDirectory()).isFalse();
            assertThat(new File(new File(workingDir, workingDirSpecifiedInMaterial), ".git").isDirectory()).isFalse();
        }

        @Test
        void shouldReturnValidBean() {
            ValidationBean validationBean = git.checkConnection(new TestSubprocessExecutionContext());
            assertThat(validationBean.isValid()).as("Repository should exist").isEqualTo(true);
        }
    }

    @Test
    void shouldBeEqualWhenUrlSameForHgMaterial() {
        Material material = MaterialsMother.gitMaterials("url1").get(0);
        Material anotherMaterial = MaterialsMother.gitMaterials("url1").get(0);
        assertThat(material.equals(anotherMaterial)).isTrue();
        assertThat(anotherMaterial.equals(material)).isTrue();
        assertThat(anotherMaterial.hashCode() == material.hashCode()).isTrue();
    }

    @Test
    void shouldNotBeEqualWhenUrlDifferent() {
        Material material1 = MaterialsMother.gitMaterials("url1").get(0);
        Material material2 = MaterialsMother.gitMaterials("url2").get(0);
        assertThat(material1.equals(material2)).isFalse();
        assertThat(material2.equals(material1)).isFalse();
    }

    @Test
    void shouldNotBeEqualWhenTypeDifferent() {
        Material material = MaterialsMother.gitMaterials("url1").get(0);
        final Material hgMaterial = MaterialsMother.hgMaterials("url1", "hgdir").get(0);
        assertThat(material.equals(hgMaterial)).isFalse();
        assertThat(hgMaterial.equals(material)).isFalse();
    }

    @Test
    void shouldReturnInValidBean() {
        GitMaterial git = new GitMaterial("http://0.0.0.0");
        ValidationBean validationBean = git.checkConnection(new TestSubprocessExecutionContext());
        assertThat(validationBean.isValid()).as("Repository should not exist").isEqualTo(false);
    }

    @Test
    void shouldReturnInvalidBeanWithRootCauseAsLowerVersionInstalled() {
        ValidationBean validationBean = new GitMaterial("http://0.0.0.0").handleException(new Exception(), GIT_VERSION_1_5);
        assertThat(validationBean.isValid()).isFalse();
        assertThat(validationBean.getError()).isEqualTo("Please install Git-core 1.9 or above. Currently installed version is 1.5.4");
    }

    @Test
    void shouldReturnInvalidBeanWithRootCauseAsRepositoryURLIsNotFoundIfVersionIsAbove19() {
        ValidationBean validationBean = new GitMaterial("http://0.0.0.0").handleException(new Exception("not found!"), GIT_VERSION_1_9);
        assertThat(validationBean.isValid()).isFalse();
        assertThat(validationBean.getError()).contains("not found!");
    }

    @Test
    void shouldBeAbleToConvertToJson() {
        Map<String, Object> json = new LinkedHashMap<>();
        final GitMaterial git = new GitMaterial("http://0.0.0.0");
        git.toJson(json, new StringRevision("123"));

        JsonValue jsonValue = from(json);
        assertThat(jsonValue.getString("scmType")).isEqualTo("Git");
        assertThat(new File(jsonValue.getString("location"))).isEqualTo(new File(git.getUrl()));
        assertThat(jsonValue.getString("action")).isEqualTo("Modified");
    }

    @Test
    void shouldGetLatestModificationsFromRepoWithSubmodules() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitMaterial gitMaterial = new GitMaterial(submoduleRepos.mainRepo().getUrl());

        File workingDirectory = temporaryFolder.newFolder();
        Materials materials = new Materials();
        materials.add(gitMaterial);

        MaterialRevisions materialRevisions = materials.latestModification(workingDirectory, new TestSubprocessExecutionContext());
        assertThat(materialRevisions.numberOfRevisions()).isEqualTo(1);
        MaterialRevision materialRevision = materialRevisions.getMaterialRevision(0);
        assertThat(materialRevision.getRevision().getRevision()).isEqualTo(submoduleRepos.currentRevision(GitRepoContainingSubmodule.NAME));
    }

    @Test
    void shouldUpdateSubmodules() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitMaterial gitMaterial = new GitMaterial(submoduleRepos.mainRepo().getUrl());

        File serverWorkingDir = temporaryFolder.newFolder();
        Materials materials = new Materials();
        materials.add(gitMaterial);

        MaterialRevisions materialRevisions = materials.latestModification(serverWorkingDir, new TestSubprocessExecutionContext());

        File agentWorkingDir = temporaryFolder.newFolder();
        MaterialRevision materialRevision = materialRevisions.getMaterialRevision(0);
        materialRevision.updateTo(agentWorkingDir, inMemoryConsumer(), new TestSubprocessExecutionContext());

        File localFile = submoduleRepos.files(GitRepoContainingSubmodule.NAME).get(0);
        assertThat(new File(agentWorkingDir, localFile.getName())).exists();

        File file = submoduleRepos.files(SUBMODULE).get(0);
        File workingSubmoduleFolder = new File(agentWorkingDir, "sub1");
        assertThat(new File(workingSubmoduleFolder, file.getName())).exists();
    }

    @Test
    void shouldHaveModificationsWhenSubmoduleIsAdded() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitMaterial gitMaterial = new GitMaterial(submoduleRepos.mainRepo().getUrl());

        File serverWorkingDir = temporaryFolder.newFolder();

        List<Modification> beforeAdd = gitMaterial.latestModification(serverWorkingDir, new TestSubprocessExecutionContext());

        submoduleRepos.addSubmodule(SUBMODULE, "new-submodule");

        List<Modification> afterAdd = gitMaterial.modificationsSince(serverWorkingDir, new Modifications(beforeAdd).latestRevision(gitMaterial), new TestSubprocessExecutionContext());

        assertThat(afterAdd.size()).isEqualTo(1);
        assertThat(afterAdd.get(0).getComment()).isEqualTo("Added submodule new-submodule");
    }

    @Test
    void shouldHaveModificationsWhenSubmoduleIsRemoved() throws Exception {
        GitRepoContainingSubmodule submoduleRepos = new GitRepoContainingSubmodule(temporaryFolder);
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitMaterial gitMaterial = new GitMaterial(submoduleRepos.mainRepo().getUrl());

        File serverWorkingDir = temporaryFolder.newFolder();

        List<Modification> beforeAdd = gitMaterial.latestModification(serverWorkingDir, new TestSubprocessExecutionContext());

        submoduleRepos.removeSubmodule("sub1");

        List<Modification> after = gitMaterial.modificationsSince(serverWorkingDir, new Modifications(beforeAdd).latestRevision(gitMaterial), new TestSubprocessExecutionContext());

        assertThat(after.size()).isEqualTo(1);
        assertThat(after.get(0).getComment()).isEqualTo("Removed submodule sub1");
    }

    @Test
    void shouldGenerateSqlCriteriaMapInSpecificOrder() {
        Map<String, Object> map = new GitMaterial("https://example.com").getSqlCriteria();
        assertThat(map.size()).isEqualTo(3);
        Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();
        assertThat(iter.next().getKey()).isEqualTo("type");
        assertThat(iter.next().getKey()).isEqualTo("url");
        assertThat(iter.next().getKey()).isEqualTo("branch");
    }

    /**
     * A git abbreviated hash is 7 chars. See the git documentation.
     */
    @Test
    void shouldTruncateHashTo7CharsForAShortRevision() {
        Material git = new GitMaterial("file:///foo");
        assertThat(git.getShortRevision("dc3d7e656831d1b203d8b7a63c4de82e26604e52")).isEqualTo("dc3d7e6");
        assertThat(git.getShortRevision("24")).isEqualTo("24");
        assertThat(git.getShortRevision(null)).isNull();
    }

    @Test
    void shouldGetLongDescriptionForMaterial() {
        GitMaterial gitMaterial = new GitMaterial("http://url/", "branch", "folder");
        assertThat(gitMaterial.getLongDescription()).isEqualTo("URL: http://url/, Branch: branch");
    }

    @Test
    void shouldGetAttributesWithSecureFields() {
        GitMaterial git = new GitMaterial("http://username:password@gitrepo.com", GitMaterialConfig.DEFAULT_BRANCH);
        Map<String, Object> attributes = git.getAttributes(true);

        assertThat(attributes.get("type")).isEqualTo("git");
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("git-configuration");
        assertThat(configuration.get("url")).isEqualTo("http://username:password@gitrepo.com");
        assertThat(configuration.get("branch")).isEqualTo(GitMaterialConfig.DEFAULT_BRANCH);
    }

    @Test
    void shouldGetAttributesWithoutSecureFields() {
        GitMaterial git = new GitMaterial("http://username:password@gitrepo.com", GitMaterialConfig.DEFAULT_BRANCH);
        Map<String, Object> attributes = git.getAttributes(false);

        assertThat(attributes.get("type")).isEqualTo("git");
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("git-configuration");
        assertThat(configuration.get("url")).isEqualTo("http://username:******@gitrepo.com");
        assertThat(configuration.get("branch")).isEqualTo(GitMaterialConfig.DEFAULT_BRANCH);
    }

    @Nested
    class Equals {
        @Test
        void shouldBeEqualIfObjectsHaveSameUrlBranchAndSubModuleFolder() {
            final GitMaterial material_1 = new GitMaterial("http://example.com", "master");
            material_1.setUserName("bob");
            material_1.setSubmoduleFolder("/var/lib/git");

            final GitMaterial material_2 = new GitMaterial("http://example.com", "master");
            material_2.setUserName("bob");
            material_2.setSubmoduleFolder("/var/lib/git");

            assertThat(material_1.equals(material_2)).isTrue();
        }
    }

    @Nested
    class Fingerprint {
        @Test
        void shouldGenerateFingerprintForGivenMaterialUrlAndBranch() {
            GitMaterial gitMaterial = new GitMaterial("https://bob:pass@github.com/gocd", "feature");
            gitMaterial.setUserName("bob");
            gitMaterial.setPassword("badger");

            assertThat(gitMaterial.getFingerprint()).isEqualTo("755da7fb7415c8674bdf5f8a4ba48fc3e071e5de429b1308ccf8949d215bdb08");
        }
    }

    @Nested
    class ConfigToMaterial {
        @Test
        void shouldBuildFromConfigObject() {
            final GitMaterialConfig materialConfig = git(new UrlArgument("http://example.com"), "bob", "pass", "master", "sub_module_folder",
                    true, Filter.create("igrnored"), false, "destination",
                    new CaseInsensitiveString("example"), false);

            final GitMaterial gitMaterial = new GitMaterial(materialConfig);

            assertThat(gitMaterial.getUrl()).isEqualTo(materialConfig.getUrl());
            assertThat(gitMaterial.getUserName()).isEqualTo(materialConfig.getUserName());
            assertThat(gitMaterial.getPassword()).isEqualTo(materialConfig.getPassword());
            assertThat(gitMaterial.getBranch()).isEqualTo(materialConfig.getBranch());
            assertThat(gitMaterial.getSubmoduleFolder()).isEqualTo(materialConfig.getSubmoduleFolder());
            assertThat(gitMaterial.getAutoUpdate()).isEqualTo(materialConfig.getAutoUpdate());
            assertThat(gitMaterial.getInvertFilter()).isEqualTo(materialConfig.getInvertFilter());
            assertThat(gitMaterial.getFolder()).isEqualTo(materialConfig.getFolder());
            assertThat(gitMaterial.getName()).isEqualTo(materialConfig.getName());
            assertThat(gitMaterial.isShallowClone()).isEqualTo(materialConfig.isShallowClone());
            assertThat(gitMaterial.getFingerprint()).isEqualTo(materialConfig.getFingerprint());
        }
    }

    @Nested
    class MaterialToConfig {
        @Test
        void shouldBuildConfigFromMaterialObject() {
            final GitMaterial gitMaterial = new GitMaterial("http://example.com", "master", "sub_module_folder", true);
            gitMaterial.setUserName("bob");
            gitMaterial.setPassword("pass");
            gitMaterial.setAutoUpdate(true);
            gitMaterial.setName(new CaseInsensitiveString("example"));
            gitMaterial.setInvertFilter(true);
            gitMaterial.setFolder("destination");
            gitMaterial.setFilter(Filter.create("allow"));

            final GitMaterialConfig materialConfig = (GitMaterialConfig) gitMaterial.config();

            assertThat(gitMaterial.getUrl()).isEqualTo(materialConfig.getUrl());
            assertThat(gitMaterial.getUserName()).isEqualTo(materialConfig.getUserName());
            assertThat(gitMaterial.getPassword()).isEqualTo(materialConfig.getPassword());
            assertThat(gitMaterial.getBranch()).isEqualTo(materialConfig.getBranch());
            assertThat(gitMaterial.getSubmoduleFolder()).isEqualTo(materialConfig.getSubmoduleFolder());
            assertThat(gitMaterial.getAutoUpdate()).isEqualTo(materialConfig.getAutoUpdate());
            assertThat(gitMaterial.getInvertFilter()).isEqualTo(materialConfig.getInvertFilter());
            assertThat(gitMaterial.getFolder()).isEqualTo(materialConfig.getFolder());
            assertThat(gitMaterial.getName()).isEqualTo(materialConfig.getName());
            assertThat(gitMaterial.isShallowClone()).isEqualTo(materialConfig.isShallowClone());
            assertThat(gitMaterial.getFingerprint()).isEqualTo(materialConfig.getFingerprint());
        }
    }

    @Nested
    class urlForCommandLine {
        @Test
        void shouldBeTheConfiguredUrlForTheMaterial() {
            final GitMaterial gitMaterial = new GitMaterial("http://bob:pass@exampele.com");

            assertThat(gitMaterial.urlForCommandLine()).isEqualTo("http://bob:pass@exampele.com");
        }

        @Test
        void shouldIncludeUserInfoIfProvidedAsUsernameAndPasswordAttributes() {
            final GitMaterial gitMaterial = new GitMaterial("http://exampele.com");
            gitMaterial.setUserName("bob");
            gitMaterial.setPassword("pass");

            assertThat(gitMaterial.urlForCommandLine()).isEqualTo("http://bob:pass@exampele.com");
        }

        @Test
        void shouldIncludeUserNameIfProvidedAsUsernameAttributes() {
            final GitMaterial gitMaterial = new GitMaterial("http://exampele.com");
            gitMaterial.setUserName("bob");

            assertThat(gitMaterial.urlForCommandLine()).isEqualTo("http://bob@exampele.com");
        }

        @Test
        void shouldIncludePasswordIfProvidedAsPasswordAttributes() {
            final GitMaterial gitMaterial = new GitMaterial("http://exampele.com");
            gitMaterial.setPassword("pass");

            assertThat(gitMaterial.urlForCommandLine()).isEqualTo("http://:pass@exampele.com");
        }

        @Test
        void shouldEncodeUserInfoIfProvidedAsUsernamePasswordAttributes() {
            final GitMaterial gitMaterial = new GitMaterial("http://exampele.com");
            gitMaterial.setUserName("bob@example.com");
            gitMaterial.setPassword("p@ssw:rd");

            assertThat(gitMaterial.urlForCommandLine()).isEqualTo("http://bob%40example.com:p%40ssw:rd@exampele.com");
        }

        @Test
        void shouldHaveResolvedUserInfoIfSecretParamsIsPresent() {
            final GitMaterial gitMaterial = new GitMaterial("http://exampele.com");
            gitMaterial.setUserName("bob@example.com");
            String password = "{{SECRET:[test][id]}}";
            gitMaterial.setPassword(password);

            SecretParam secretParam = gitMaterial.getSecretParams().get(0);
            secretParam.setValue("p@ssw:rd");

            assertThat(gitMaterial.urlForCommandLine()).isEqualTo("http://bob%40example.com:p%40ssw:rd@exampele.com");
        }
    }

    @Nested
    class createMaterialInstance {
        @Test
        void shouldCreateMaterialInstanceObject() {
            final GitMaterial gitMaterial = new GitMaterial("http://exampele.com", "feature");
            gitMaterial.setUserName("bob");

            MaterialInstance materialInstance = gitMaterial.createMaterialInstance();

            assertThat(materialInstance.getUrl()).isEqualTo(gitMaterial.getUrl());
            assertThat(materialInstance.getUsername()).isEqualTo(gitMaterial.getUserName());
            assertThat(materialInstance.getBranch()).isEqualTo("feature");
        }
    }

    @Nested
    class WithShallowClone {
        @Test
        void shouldCopyOverSecretResolvedParams() {
            GitMaterial gitMaterial = new GitMaterial("http://exampele.com");
            gitMaterial.setPassword("{{SECRET:[secret_config_id][git_password]}}");
            gitMaterial.getSecretParams().findFirst("git_password").ifPresent(param -> param.setValue("resolved-password"));

            assertThat(gitMaterial.passwordForCommandLine()).isEqualTo("resolved-password");
            assertThat(gitMaterial.isShallowClone()).isFalse();

            GitMaterial copyWithShallowClone = gitMaterial.withShallowClone(true);

            assertThat(copyWithShallowClone.passwordForCommandLine()).isEqualTo("resolved-password");
            assertThat(copyWithShallowClone.isShallowClone()).isTrue();
        }
    }

    @Test
    void populateEnvContextShouldSetMaterialEnvVars() {
        GitMaterial material = new GitMaterial("https://user:password@github.com/bob/my-project", "branchName");

        EnvironmentVariableContext ctx = new EnvironmentVariableContext();
        final ArrayList<Modification> modifications = new ArrayList<>();

        modifications.add(new Modification("user2", "comment2", "email2", new Date(), "24"));
        modifications.add(new Modification("user1", "comment1", "email1", new Date(), "23"));

        MaterialRevision materialRevision = new MaterialRevision(material, modifications);
        assertThat(ctx.getProperty(ScmMaterial.GO_MATERIAL_URL)).isNull();
        assertThat(ctx.getProperty(GitMaterial.GO_MATERIAL_BRANCH)).isNull();

        material.populateEnvironmentContext(ctx, materialRevision, new File("."));

        assertThat(ctx.getProperty(ScmMaterial.GO_MATERIAL_URL)).isEqualTo("https://github.com/bob/my-project");
        assertThat(ctx.getProperty(GitMaterial.GO_MATERIAL_BRANCH)).isEqualTo("branchName");
    }

    @Test
    void shouldPopulateBranchWithDefaultIfNotSet() {
        GitMaterial material = new GitMaterial("https://user:password@github.com/bob/my-project");

        EnvironmentVariableContext ctx = new EnvironmentVariableContext();
        final ArrayList<Modification> modifications = new ArrayList<>();

        modifications.add(new Modification("user1", "comment1", "email1", new Date(), "23"));

        MaterialRevision materialRevision = new MaterialRevision(material, modifications);
        material.populateEnvironmentContext(ctx, materialRevision, new File("."));

        assertThat(ctx.getProperty(GitMaterial.GO_MATERIAL_BRANCH)).isEqualTo("master");
    }

    private void assertWorkingCopyNotCheckedOut(File localWorkingDir) {
        assertThat(localWorkingDir.listFiles()).isEqualTo(new File[]{new File(localWorkingDir, ".git")});
    }
}
