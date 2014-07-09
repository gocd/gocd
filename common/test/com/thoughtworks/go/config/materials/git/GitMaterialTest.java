/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config.materials.git;

import com.thoughtworks.go.config.materials.Materials;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.materials.git.GitTestRepo;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.helper.GitSubmoduleRepos;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.util.JsonUtils;
import com.thoughtworks.go.util.JsonValue;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.command.CommandLine;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.ProcessOutputStreamConsumer;
import com.thoughtworks.go.util.json.JsonMap;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.File;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.matchers.FileExistsMatcher.exists;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.lang.String.format;
import static junit.framework.Assert.fail;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;

public class GitMaterialTest {
    @Rule public TestName name = new TestName();
    private static final String BRANCH = "foo";

    private GitMaterial git;
    private File workingDir;
    private String repositoryUrl;
    private InMemoryStreamConsumer outputStreamConsumer;

    private static final String GIT_VERSION_1_6_0_2 = "git version 1.6.0.2";
    private static final String GIT_VERSION_1_5_4_3 = "git version 1.5.4.3";
    private static final String GIT_VERSION_1_6_0_2_ON_WINDOWS = "git version 1.6.0.2.1172.ga5ed0";
    private static final String GIT_VERSION_NODE_ON_WINDOWS = "git version ga5ed0asdasd.ga5ed0";
    private static final StringRevision REVISION_0 = new StringRevision("55502a724dd8574f1e4bcf19b605a1f4f182e892");
    private static final StringRevision REVISION_1 = new StringRevision("b613aee673d96e967100306222246aa9decbc53c");
    private static final StringRevision REVISION_2 = new StringRevision("4ab7833f55024c975cf5b918af640954c108825e");
    private static final StringRevision REVISION_3 = new StringRevision("ab9ff2cee965ae4d0778dbcda1fadffbbc202e85");
    private static final StringRevision REVISION_4 = new StringRevision("5def073a425dfe239aabd4bf8039ffe3b0e8856b");
    private static final String SUBMODULE = "submodule-1";

    @Before
    public void setup() throws Exception {
        GitTestRepo gitRepo = new GitTestRepo();
        outputStreamConsumer = inMemoryConsumer();
        workingDir = TestFileUtil.createTempFolder("workingDir-" + System.currentTimeMillis());

        repositoryUrl = gitRepo.projectRepositoryUrl();
        git = new GitMaterial(repositoryUrl);
    }

    @After
    public void teardown() throws Exception {
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
        GitTestRepo branchedTestRepo = GitTestRepo.testRepoAtBranch(GitTestRepo.GIT_FOO_BRANCH_BUNDLE, BRANCH);
        GitMaterial branchedGit = new GitMaterial(branchedTestRepo.projectRepositoryUrl(), BRANCH);
        List<Modification> modifications = branchedGit.latestModification(TestFileUtil.createTempFolder(
                "workingDir-" + System.currentTimeMillis()), new TestSubprocessExecutionContext());
        assertThat(modifications.size(), is(1));
        assertThat(modifications.get(0).getComment(), Matchers.is("Started foo branch"));
    }

    @Test
    public void shouldFindAllModificationsSinceARevision() throws Exception {
        List<Modification> modifications = git.modificationsSince(workingDir, REVISION_0, new TestSubprocessExecutionContext());
        assertThat(modifications.size(), is(4));
        assertThat(modifications.get(0).getRevision(), is(REVISION_4.getRevision()));
        assertThat(modifications.get(0).getComment(), is("Added 'run-till-file-exists' ant target"));
        assertThat(modifications.get(1).getRevision(), is(REVISION_3.getRevision()));
        assertThat(modifications.get(1).getComment(), is("adding build.xml"));
        assertThat(modifications.get(2).getRevision(), is(REVISION_2.getRevision()));
        assertThat(modifications.get(2).getComment(), is("Created second.txt from first.txt"));
        assertThat(modifications.get(3).getRevision(), is(REVISION_1.getRevision()));
        assertThat(modifications.get(3).getComment(), is("Added second line"));
    }

    @Test
    public void shouldRetrieveModifiedFiles() throws Exception {
        List<Modification> mods = git.modificationsSince(workingDir, REVISION_0, new TestSubprocessExecutionContext());
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
        git.updateToInternal(output, REVISION_1, workingDir, new TestSubprocessExecutionContext());
        assertThat(output.getStdOut(),
                containsString("Start updating files at revision " + REVISION_1.getRevision()));
        assertThat(newFile.exists(), is(false));

        output = inMemoryConsumer();
        git.updateToInternal(output, REVISION_2, workingDir, new TestSubprocessExecutionContext());
        assertThat(output.getStdOut(),
                containsString("Start updating files at revision " + REVISION_2.getRevision()));
        assertThat(newFile.exists(), is(true));
    }

    @Test
    public void shouldRemoveSubmoduleFolderFromWorkingDirWhenSubmoduleIsRemovedFromRepo() throws Exception {
        GitSubmoduleRepos submoduleRepos = new GitSubmoduleRepos();
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitMaterial gitMaterial = new GitMaterial(submoduleRepos.mainRepo().getUrl());

        gitMaterial.updateToInternal(outputStreamConsumer, new StringRevision("origin/master"), workingDir, new TestSubprocessExecutionContext());
        assertThat(new File(workingDir, "sub1"), exists());

        submoduleRepos.removeSubmodule("sub1");

        outputStreamConsumer = inMemoryConsumer();
        gitMaterial.updateToInternal(outputStreamConsumer, new StringRevision("origin/master"), workingDir, new TestSubprocessExecutionContext());
        assertThat(new File(workingDir, "sub1"), not(exists()));
    }

    @Test
    public void shouldDeleteAndRecheckoutDirectoryWhenUrlChanges() throws Exception {
        git.latestModification(workingDir, new TestSubprocessExecutionContext());

        File shouldBeRemoved = new File(workingDir, "shouldBeRemoved");
        shouldBeRemoved.createNewFile();
        assertThat(shouldBeRemoved.exists(), is(true));

        git = new GitMaterial(new GitTestRepo().projectRepositoryUrl());
        git.latestModification(workingDir, new TestSubprocessExecutionContext());
        assertThat("Should have deleted whole folder", shouldBeRemoved.exists(), is(false));
    }

    @Test
    public void shouldNotDeleteAndRecheckoutDirectoryWhenUrlSame() throws Exception {
        git.latestModification(workingDir, new TestSubprocessExecutionContext());

        File shouldNotBeRemoved = new File(new File(workingDir, ".git"), "shouldNotBeRemoved");
        FileUtils.writeStringToFile(shouldNotBeRemoved, "gundi");
        assertThat(shouldNotBeRemoved.exists(), is(true));

        git = new GitMaterial(repositoryUrl);
        git.latestModification(workingDir, new TestSubprocessExecutionContext());
        assertThat("Should not have deleted whole folder", shouldNotBeRemoved.exists(), is(true));
    }

    @Test
    public void shouldNotDeleteAndRecheckoutDirectoryWhenUrlsOnlyDifferInFileProtocol() throws Exception {
        git.latestModification(workingDir, new TestSubprocessExecutionContext());

        File shouldNotBeRemoved = new File(new File(workingDir, ".git"), "shouldNotBeRemoved");
        FileUtils.writeStringToFile(shouldNotBeRemoved, "gundi");
        assertThat(shouldNotBeRemoved.exists(), is(true));

        git = new GitMaterial("file://" + repositoryUrl);
        git.latestModification(workingDir, new TestSubprocessExecutionContext());
        assertThat("Should not have deleted whole folder", shouldNotBeRemoved.exists(), is(true));
    }

    @Test
    public void shouldDeleteAndRecheckoutDirectoryWhenBranchChanges() throws Exception {
        git = new GitMaterial(GitTestRepo.GIT_FOO_BRANCH_BUNDLE.getAbsolutePath());
        git.latestModification(workingDir, new TestSubprocessExecutionContext());
        InMemoryStreamConsumer output = inMemoryConsumer();
        CommandLine.createCommandLine("git").withEncoding("UTF-8").withArg("branch").withWorkingDir(workingDir).run(output, "");
        assertThat(output.getStdOut(), Is.is("* master"));

        git = new GitMaterial(GitTestRepo.GIT_FOO_BRANCH_BUNDLE.getAbsolutePath(), "foo");
        git.latestModification(workingDir, new TestSubprocessExecutionContext());
        output = inMemoryConsumer();
        CommandLine.createCommandLine("git").withEncoding("UTF-8").withArg("branch").withWorkingDir(workingDir).run(output, "");
        assertThat(output.getStdOut(), Is.is("* foo"));
    }

    @Test
    public void shouldCheckRemoteConnection() throws Exception {
        ValidationBean validationBean = git.checkConnection(new TestSubprocessExecutionContext());
        assertThat("Connection should be valid", validationBean.isValid(), is(true));
        String badHost = "http://nonExistantHost/git";
        git = new GitMaterial(badHost);
        validationBean = git.checkConnection(new TestSubprocessExecutionContext());
        assertThat("Connection should not be valid", validationBean.isValid(), is(false));
        assertThat(validationBean.getError(), containsString("Repository " + badHost + " not found!"));
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
        ValidationBean validationBean = git.handleException(new Exception(), GIT_VERSION_1_6_0_2);
        assertThat(validationBean.isValid(), is(false));
        assertThat(validationBean.getError(), containsString("not found!"));
    }

    @Test
    public void shouldReturnInvalidBeanWithRootCauseAsRepositoryURLIsNotFoundIfVersionIsAbvoe16OnWindows()
            throws Exception {
        ValidationBean validationBean = git.handleException(new Exception(), GIT_VERSION_1_6_0_2_ON_WINDOWS);
        assertThat(validationBean.isValid(), is(false));
        assertThat(validationBean.getError(), containsString("not found!"));
    }


    @Test
    public void shouldReturnInvalidBeanWithRootCauseAsRepositoryURLIsNotFoundIfVersionIsNotKnown() throws Exception {
        ValidationBean validationBean = git.handleException(new Exception(), GIT_VERSION_NODE_ON_WINDOWS);
        assertThat(validationBean.isValid(), is(false));
        assertThat(validationBean.getError(), containsString("not found!"));
    }

    @Test
    public void shouldThrowExceptionWithUsefulInfoIfFailedToFindModifications() throws Exception {
        final String url = "notExistDir";
        git = new GitMaterial(url);
        try {
            git.modificationsSince(workingDir, REVISION_0, new TestSubprocessExecutionContext());
            fail("Should have thrown an exception when failed to clone from an invalid url");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Failed to run git clone command"));
        }
    }

    @Test
    public void shouldBeAbleToConvertToJson() {
        JsonMap json = new JsonMap();
        git.toJson(json, new StringRevision("123"));

        JsonValue jsonValue = JsonUtils.from(json);
        assertThat(jsonValue.getString("scmType"), is("Git"));
        assertThat(new File(jsonValue.getString("location")), is(new File(git.getUrl())));
        assertThat(jsonValue.getString("action"), is("Modified"));
    }

    @Test
    public void shouldLogRepoInfoToConsoleOutWithoutFolder() throws Exception {
        git.updateToInternal(outputStreamConsumer, REVISION_1, workingDir, new TestSubprocessExecutionContext());
        assertThat(outputStreamConsumer.getStdOut(), containsString(
                format("Start updating %s at revision %s from %s", "files", REVISION_1.getRevision(),
                        git.getUrl())));
    }

    @Test
    public void shouldGetLatestModificationsFromSubmodules() throws Exception {
        GitSubmoduleRepos submoduleRepos = new GitSubmoduleRepos();
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitMaterial gitMaterial = new GitMaterial(submoduleRepos.mainRepo().getUrl());

        File workingDirectory = TestFileUtil.createTempFolder("workingDir-" + System.currentTimeMillis());
        Materials materials = new Materials();
        materials.add(gitMaterial);

        MaterialRevisions materialRevisions = materials.latestModification(workingDirectory, new TestSubprocessExecutionContext());
        assertThat(materialRevisions.numberOfRevisions(), is(1));
        MaterialRevision materialRevision = materialRevisions.getMaterialRevision(0);
        assertThat(materialRevision.getRevision().getRevision(), is(submoduleRepos.currentRevision(GitSubmoduleRepos.NAME)));

        File file = submoduleRepos.files(SUBMODULE).get(0);
        File workingSubmoduleFolder = new File(workingDirectory, "sub1");
        assertThat(new File(workingSubmoduleFolder, file.getName()), exists());
    }

    @Test
    public void shouldUpdateSubmodules() throws Exception {
        GitSubmoduleRepos submoduleRepos = new GitSubmoduleRepos();
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitMaterial gitMaterial = new GitMaterial(submoduleRepos.mainRepo().getUrl());

        File serverWorkingDir = TestFileUtil.createTempFolder("workingDir-" + System.currentTimeMillis());
        Materials materials = new Materials();
        materials.add(gitMaterial);

        MaterialRevisions materialRevisions = materials.latestModification(serverWorkingDir, new TestSubprocessExecutionContext());

        File agentWorkingDir = TestFileUtil.createTempFolder("workingDir-" + System.currentTimeMillis());
        MaterialRevision materialRevision = materialRevisions.getMaterialRevision(0);
        materialRevision.updateTo(agentWorkingDir, ProcessOutputStreamConsumer.inMemoryConsumer(), new TestSubprocessExecutionContext());

        File localFile = submoduleRepos.files(GitSubmoduleRepos.NAME).get(0);
        assertThat(new File(agentWorkingDir, localFile.getName()), exists());

        File file = submoduleRepos.files(SUBMODULE).get(0);
        File workingSubmoduleFolder = new File(agentWorkingDir, "sub1");
        assertThat(new File(workingSubmoduleFolder, file.getName()), exists());
    }

    @Test
    public void shouldHaveModificationsWhenSubmoduleIsAdded() throws Exception {
        GitSubmoduleRepos submoduleRepos = new GitSubmoduleRepos();
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitMaterial gitMaterial = new GitMaterial(submoduleRepos.mainRepo().getUrl());

        File serverWorkingDir = TestFileUtil.createTempFolder("workingDir-" + System.currentTimeMillis());

        List<Modification> beforeAdd = gitMaterial.latestModification(serverWorkingDir, new TestSubprocessExecutionContext());

        submoduleRepos.addSubmodule(SUBMODULE, "new-submodule");

        List<Modification> afterAdd = gitMaterial.modificationsSince(serverWorkingDir, new Modifications(beforeAdd).latestRevision(gitMaterial), new TestSubprocessExecutionContext());

        assertThat(afterAdd.size(), is(1));
        assertThat(afterAdd.get(0).getComment(), is("Added submodule new-submodule"));
    }

    @Test
    public void shouldHaveModificationsWhenSubmoduleIsRemoved() throws Exception {
        GitSubmoduleRepos submoduleRepos = new GitSubmoduleRepos();
        submoduleRepos.addSubmodule(SUBMODULE, "sub1");
        GitMaterial gitMaterial = new GitMaterial(submoduleRepos.mainRepo().getUrl());

        File serverWorkingDir = TestFileUtil.createTempFolder("workingDir-" + System.currentTimeMillis());

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
        List<Modification> modifications = git.modificationsSince(workingDir, REVISION_0, new TestSubprocessExecutionContext());

        assertThat(modifications.size(), is(greaterThan(0)));
        assertThat(new File(workingDir, ".git").isDirectory(), is(true));
        assertThat(new File(workingDir, workingDirSpecifiedInMaterial).isDirectory(), is(false));
        assertThat(new File(new File(workingDir, workingDirSpecifiedInMaterial), ".git").isDirectory(), is(false));
    }
}
