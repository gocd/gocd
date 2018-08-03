/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.materials.mercurial;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.googlecode.junit.ext.checkers.OSChecker;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.domain.materials.mercurial.HgCommand;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.helper.HgTestRepo;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.util.JsonValue;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.command.ConsoleResult;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.io.FileUtils;
import org.hamcrest.Matchers;
import org.hamcrest.core.StringContains;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.util.JsonUtils.from;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.lang.String.format;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(JunitExtRunner.class)
public class HgMaterialTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private HgMaterial hgMaterial;
    private HgTestRepo hgTestRepo;
    private File workingFolder;
    private InMemoryStreamConsumer outputStreamConsumer;
    private static final String LINUX_HG_094 = "Mercurial Distributed SCM (version 0.9.4)\n"
            + "\n"
            + "Copyright (C) 2005-2007 Matt Mackall <mpm@selenic.com> and others\n"
            + "This is free software; see the source for copying conditions. There is NO\n"
            + "warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.";
    private static final String LINUX_HG_101 = "Mercurial Distributed SCM (version 1.0.1)\n"
            + "\n"
            + "Copyright (C) 2005-2007 Matt Mackall <mpm@selenic.com> and others\n"
            + "This is free software; see the source for copying conditions. There is NO\n"
            + "warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.";
    private static final String LINUX_HG_10 = "Mercurial Distributed SCM (version 1.0)\n"
            + "\n"
            + "Copyright (C) 2005-2007 Matt Mackall <mpm@selenic.com> and others\n"
            + "This is free software; see the source for copying conditions. There is NO\n"
            + "warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.";
    private static final String WINDOWS_HG_OFFICAL_102 = "Mercurial Distributed SCM (version 1.0.2+20080813)\n"
            + "\n"
            + "Copyright (C) 2005-2008 Matt Mackall <mpm@selenic.com>; and others\n"
            + "This is free software; see the source for copying conditions. There is NO\n"
            + "warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.";
    private static final String WINDOWS_HG_TORTOISE = "Mercurial Distributed SCM (version 626cb86a6523+tortoisehg)";
    private static final String REVISION_0 = "b61d12de515d82d3a377ae3aae6e8abe516a2651";
    private static final String REVISION_1 = "35ff2159f303ecf986b3650fc4299a6ffe5a14e1";
    private static final String REVISION_2 = "ca3ebb67f527c0ad7ed26b789056823d8b9af23f";

    @Before
    public void setUp() throws Exception {
        hgTestRepo = new HgTestRepo("hgTestRepo1", temporaryFolder);
        hgMaterial = MaterialsMother.hgMaterial(hgTestRepo.projectRepositoryUrl());
        workingFolder = temporaryFolder.newFolder("workingFolder");
        outputStreamConsumer = inMemoryConsumer();
    }

    @After
    public void teardown() {
        FileUtils.deleteQuietly(workingFolder);
        TestRepo.internalTearDown();
    }

    @Test
    public void shouldRefreshWorkingFolderWhenRepositoryChanged() throws Exception {
        new HgCommand(null, workingFolder, "default", hgTestRepo.url().forCommandline(), null).clone(inMemoryConsumer(), hgTestRepo.url());
        File testFile = createNewFileInWorkingFolder();

        HgTestRepo hgTestRepo2 = new HgTestRepo("hgTestRepo2", temporaryFolder);
        hgMaterial = MaterialsMother.hgMaterial(hgTestRepo2.projectRepositoryUrl());
        hgMaterial.latestModification(workingFolder, new TestSubprocessExecutionContext());

        String workingUrl = new HgCommand(null, workingFolder, "default", hgTestRepo.url().forCommandline(), null).workingRepositoryUrl().outputAsString();
        assertThat(workingUrl, is(hgTestRepo2.projectRepositoryUrl()));
        assertThat(testFile.exists(), is(false));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = OSChecker.LINUX)
    public void shouldNotRefreshWorkingFolderWhenFileProtocolIsUsedOnLinux() throws Exception {
        final UrlArgument repoUrl = hgTestRepo.url();
        new HgCommand(null, workingFolder, "default", repoUrl.forCommandline(), null).clone(inMemoryConsumer(), repoUrl);
        File testFile = createNewFileInWorkingFolder();

        hgMaterial = MaterialsMother.hgMaterial("file://" + new File(hgTestRepo.projectRepositoryUrl()));
        updateMaterial(hgMaterial, new StringRevision("0"));

        String workingUrl = new HgCommand(null, workingFolder, "default", repoUrl.forCommandline(), null).workingRepositoryUrl().outputAsString();
        assertThat(workingUrl, is(hgTestRepo.projectRepositoryUrl()));
        assertThat(testFile.exists(), is(true));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = OSChecker.WINDOWS)
    public void shouldNotRefreshWorkingFolderWhenFileProtocolIsUsedOnWindows() throws Exception {
        final UrlArgument repoUrl = hgTestRepo.url();
        new HgCommand(null, workingFolder, "default", repoUrl.forCommandline(), null).clone(inMemoryConsumer(), repoUrl);
        File testFile = createNewFileInWorkingFolder();

        hgMaterial = MaterialsMother.hgMaterial("file:///" + new File(hgTestRepo.projectRepositoryUrl()));

        updateMaterial(hgMaterial, new StringRevision("0"));

        String workingUrl = new HgCommand(null, workingFolder, "default", repoUrl.forCommandline(), null).workingRepositoryUrl().outputAsString();
        assertThat(workingUrl, is(hgTestRepo.projectRepositoryUrl()));
        assertThat(testFile.exists(), is(true));
    }

    private void updateMaterial(HgMaterial hgMaterial, StringRevision revision) {
        hgMaterial.updateTo(outputStreamConsumer, workingFolder, new RevisionContext(revision), new TestSubprocessExecutionContext());
    }

    @Test
    public void shouldNotRefreshWorkingDirectoryIfDefaultRemoteUrlDoesNotContainPasswordButMaterialUrlDoes() throws Exception {
        final HgMaterial material = new HgMaterial("http://user:pwd@domain:9999/path", null);
        final HgCommand hgCommand = mock(HgCommand.class);
        final ConsoleResult consoleResult = mock(ConsoleResult.class);
        when(consoleResult.outputAsString()).thenReturn("http://user@domain:9999/path");
        when(hgCommand.workingRepositoryUrl()).thenReturn(consoleResult);
        assertFalse((Boolean) ReflectionUtil.invoke(material, "isRepositoryChanged", hgCommand));
    }

    @Test
    public void shouldRefreshWorkingDirectoryIfUsernameInDefaultRemoteUrlIsDifferentFromOneInMaterialUrl() throws Exception {
        final HgMaterial material = new HgMaterial("http://some_new_user:pwd@domain:9999/path", null);
        final HgCommand hgCommand = mock(HgCommand.class);
        final ConsoleResult consoleResult = mock(ConsoleResult.class);
        when(consoleResult.outputAsString()).thenReturn("http://user:pwd@domain:9999/path");
        when(hgCommand.workingRepositoryUrl()).thenReturn(consoleResult);
        assertTrue((Boolean) ReflectionUtil.invoke(material, "isRepositoryChanged", hgCommand));
    }

    @Test
    public void shouldGetModifications() {
        List<Modification> mods = hgMaterial.modificationsSince(workingFolder, new StringRevision(REVISION_0), new TestSubprocessExecutionContext());
        assertThat(mods.size(), is(2));
        Modification modification = mods.get(0);
        assertThat(modification.getRevision(), is(REVISION_2));
        assertThat(modification.getModifiedFiles().size(), is(1));
    }

    @Test
    public void shouldNotAppendDestinationDirectoryWhileFetchingModifications() {
        hgMaterial.setFolder("dest");
        hgMaterial.modificationsSince(workingFolder, new StringRevision(REVISION_0), new TestSubprocessExecutionContext());
        assertThat(new File(workingFolder, "dest").exists(), is(false));
    }

    @Test
    public void shouldGetModificationsBasedOnRevision() {
        List<Modification> modificationsSince = hgMaterial.modificationsSince(workingFolder,
                new StringRevision(REVISION_0), new TestSubprocessExecutionContext());

        assertThat(modificationsSince.get(0).getRevision(), is(REVISION_2));
        assertThat(modificationsSince.get(1).getRevision(), is(REVISION_1));
        assertThat(modificationsSince.size(), is(2));
    }

    @Test
    public void shouldReturnLatestRevisionIfNoModificationsDetected() {
        List<Modification> modification = hgMaterial.modificationsSince(workingFolder,
                new StringRevision(REVISION_2), new TestSubprocessExecutionContext());
        assertThat(modification.isEmpty(), is(true));
    }

    @Test
    public void shouldUpdateToSpecificRevision() {
        updateMaterial(hgMaterial, new StringRevision("0"));
        File end2endFolder = new File(workingFolder, "end2end");
        assertThat(end2endFolder.listFiles().length, is(3));
        assertThat(outputStreamConsumer.getStdOut(), is(not("")));
        updateMaterial(hgMaterial, new StringRevision("1"));
        assertThat(end2endFolder.listFiles().length, is(4));
    }

    @Test
    public void shouldUpdateToDestinationFolder() {
        hgMaterial.setFolder("dest");
        updateMaterial(hgMaterial, new StringRevision("0"));
        File end2endFolder = new File(workingFolder, "dest/end2end");
        assertThat(end2endFolder.exists(), is(true));
    }

    @Test
    public void shouldLogRepoInfoToConsoleOutWithoutFolder() {
        updateMaterial(hgMaterial, new StringRevision("0"));
        assertThat(outputStreamConsumer.getStdOut(), containsString(
                format("Start updating %s at revision %s from %s", "files", "0",
                        hgMaterial.getUrl())));
    }

    @Test
    public void shouldDeleteWorkingFolderWhenItIsNotAnHgRepository() throws Exception {
        File testFile = createNewFileInWorkingFolder();
        hgMaterial.latestModification(workingFolder, new TestSubprocessExecutionContext());

        assertThat(testFile.exists(), is(false));
    }

    @Test
    public void shouldThrowExceptionWithUsefulInfoIfFailedToFindModifications() {
        final String url = "/tmp/notExistDir";
        hgMaterial = MaterialsMother.hgMaterial(url);
        try {
            hgMaterial.latestModification(workingFolder, new TestSubprocessExecutionContext());
            fail("Should have thrown an exception when failed to clone from an invalid url");
        } catch (Exception e) {
            assertThat(e.getMessage(), StringContains.containsString("abort: repository " + url + " not found!"));
        }
    }

    private File createNewFileInWorkingFolder() throws IOException {
        if (!workingFolder.exists()) {
            workingFolder.mkdirs();
        }
        File testFile = new File(workingFolder, "not_in_hg_repository.txt");
        testFile.createNewFile();
        return testFile;
    }


    @Test
    public void shouldBeEqualWhenUrlSameForHgMaterial() {
        final Material material = MaterialsMother.hgMaterials("url1", "hgdir").get(0);
        final Material anotherMaterial = MaterialsMother.hgMaterials("url1", "hgdir").get(0);
        assertThat(material.equals(anotherMaterial), is(true));
        assertThat(anotherMaterial.equals(material), is(true));
    }

    @Test
    public void shouldNotBeEqualWhenUrlDifferent() {
        final Material material1 = MaterialsMother.hgMaterials("url1", "hgdir").get(0);
        final Material material2 = MaterialsMother.hgMaterials("url2", "hgdir").get(0);
        assertThat(material1.equals(material2), is(false));
        assertThat(material2.equals(material1), is(false));
    }

    @Test
    public void shouldNotBeEqualWhenTypeDifferent() {
        final Material material = MaterialsMother.hgMaterials("url1", "hgdir").get(0);
        final Material svnMaterial = MaterialsMother.defaultSvnMaterialsWithUrl("url1").get(0);
        assertThat(material.equals(svnMaterial), is(false));
        assertThat(svnMaterial.equals(material), is(false));
    }

    @Test
    public void shouldBeEqual() {
        final Material hgMaterial1 = MaterialsMother.hgMaterials("url1", "hgdir").get(0);
        final Material hgMaterial2 = MaterialsMother.hgMaterials("url1", "hgdir").get(0);
        assertThat(hgMaterial1.equals(hgMaterial2), is(true));
        assertThat(hgMaterial1.hashCode(), is(hgMaterial2.hashCode()));
    }

    @Test
    public void shouldReturnTrueForLinuxDistributeLowerThanOneZero() {
        assertThat(hgMaterial.isVersionOnedotZeorOrHigher(LINUX_HG_094), is(false));
    }

    @Test
    public void shouldReturnTrueForLinuxDistributeHigerThanOneZero() {
        assertThat(hgMaterial.isVersionOnedotZeorOrHigher(LINUX_HG_101), is(true));
    }

    @Test
    public void shouldReturnTrueForLinuxDistributeEqualsOneZero() {
        assertThat(hgMaterial.isVersionOnedotZeorOrHigher(LINUX_HG_10), is(true));
    }

    @Test
    public void shouldReturnTrueForWindowsDistributionHigerThanOneZero() {
        assertThat(hgMaterial.isVersionOnedotZeorOrHigher(WINDOWS_HG_OFFICAL_102), is(true));
    }

    @Test(expected = Exception.class)
    public void shouldReturnFalseWhenVersionIsNotRecgonized() {
        hgMaterial.isVersionOnedotZeorOrHigher(WINDOWS_HG_TORTOISE);
    }

    @Test
    public void shouldCheckConnection() {
        ValidationBean validation = hgMaterial.checkConnection(new TestSubprocessExecutionContext());
        assertThat(validation.isValid(), is(true));
        String notExistUrl = "http://notExisthost/hg";
        hgMaterial = MaterialsMother.hgMaterial(notExistUrl);
        validation = hgMaterial.checkConnection(new TestSubprocessExecutionContext());
        assertThat(validation.isValid(), is(false));
    }

    @Test
    public void shouldReturnInvalidBeanWithRootCauseAsLowerVersionInstalled() {
        ValidationBean validationBean = hgMaterial.handleException(new Exception(), LINUX_HG_094);
        assertThat(validationBean.isValid(), is(false));
        assertThat(validationBean.getError(), containsString("Please install Mercurial Version 1.0 or above"));
    }

    @Test
    public void shouldReturnInvalidBeanWithRootCauseAsRepositoryURLIsNotFound() {
        ValidationBean validationBean = hgMaterial.handleException(new Exception(), WINDOWS_HG_OFFICAL_102);
        assertThat(validationBean.isValid(), is(false));
        assertThat(validationBean.getError(), containsString("not found!"));
    }


    @Test
    public void shouldReturnInvalidBeanWithRootCauseAsRepositoryURLIsNotFoundIfVersionIsNotKnown() {
        ValidationBean validationBean = hgMaterial.handleException(new Exception(), WINDOWS_HG_TORTOISE);
        assertThat(validationBean.isValid(), is(false));
        assertThat(validationBean.getError(), containsString("not found!"));
    }


    @Test
    public void shouldBeAbleToConvertToJson() {
        Map<String, Object> json = new LinkedHashMap<>();
        hgMaterial.toJson(json, new StringRevision("123"));

        JsonValue jsonValue = from(json);
        assertThat(jsonValue.getString("scmType"), is("Mercurial"));
        assertThat(new File(jsonValue.getString("location")), is(new File(hgTestRepo.projectRepositoryUrl())));
        assertThat(jsonValue.getString("action"), is("Modified"));
    }

    @Test
    public void shouldRemoveTheForwardSlashAndApplyThePattern() {
        Material material = MaterialsMother.hgMaterial();

        assertThat(material.matches("a.doc", "/a.doc"), is(true));
        assertThat(material.matches("/a.doc", "a.doc"), is(false));
    }

    @Test
    public void shouldApplyThePatternDirectly() {
        Material material = MaterialsMother.hgMaterial();

        assertThat(material.matches("a.doc", "a.doc"), is(true));
    }

    @Test
    // #3103
    public void shouldParseComplexCommitMessage() throws Exception {
        String comment = "changeset:   8139:b1a0b0bbb4d1\n"
                + "branch:      trunk\n"
                + "user:        QYD\n"
                + "date:        Tue Jun 30 14:56:37 2009 +0800\n"
                + "summary:     add story #3001 - 'Pipelines should use the latest version of ...";
        hgTestRepo.commitAndPushFile("SomeDocumentation.txt", comment);

        List<Modification> modification = hgMaterial.latestModification(workingFolder, new TestSubprocessExecutionContext());

        assertThat(modification.size(), Matchers.is(1));
        assertThat(modification.get(0).getComment(), is(comment));
    }

    @Test
    public void shouldGenerateSqlCriteriaMapInSpecificOrder() {
        Map<String, Object> map = hgMaterial.getSqlCriteria();
        assertThat(map.size(), is(2));
        Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();
        assertThat(iter.next().getKey(), is("type"));
        assertThat(iter.next().getKey(), is("url"));
    }

    /**
     * An hg abbreviated hash is 12 chars. See the hg documentation.
     * %h:	short-form changeset hash (12 bytes of hexadecimal)
     */
    @Test
    public void shouldtruncateHashTo12charsforAShortRevision() {
        Material hgMaterial = new HgMaterial("file:///foo", null);
        assertThat(hgMaterial.getShortRevision("dc3d7e656831d1b203d8b7a63c4de82e26604e52"), is("dc3d7e656831"));
        assertThat(hgMaterial.getShortRevision("dc3d7e65683"), is("dc3d7e65683"));
        assertThat(hgMaterial.getShortRevision("dc3d7e6568312"), is("dc3d7e656831"));
        assertThat(hgMaterial.getShortRevision("24"), is("24"));
        assertThat(hgMaterial.getShortRevision(null), is(nullValue()));
    }

    @Test
    public void shouldNotDisplayPasswordInToString() {
        HgMaterial hgMaterial = new HgMaterial("https://user:loser@foo.bar/baz?quux=bang", null);
        assertThat(hgMaterial.toString(), not(containsString("loser")));
    }

    @Test
    public void shouldGetLongDescriptionForMaterial() {
        HgMaterial material = new HgMaterial("http://url/", "folder");
        assertThat(material.getLongDescription(), is("URL: http://url/"));
    }

    @Test
    public void shouldFindTheBranchName() {
        HgMaterial material = new HgMaterial("http://url/##foo", "folder");
        assertThat(material.getBranch(), is("foo"));
    }

    @Test
    public void shouldSetDefaultAsBranchNameIfBranchNameIsNotSpecifiedInUrl() {
        HgMaterial material = new HgMaterial("http://url/", "folder");
        assertThat(material.getBranch(), is("default"));
    }

    @Test
    public void shouldMaskThePasswordInDisplayName() {
        HgMaterial material = new HgMaterial("http://user:pwd@url##branch", "folder");
        assertThat(material.getDisplayName(), is("http://user:******@url##branch"));
    }

    @Test
    public void shouldGetAttributesWithSecureFields() {
        HgMaterial material = new HgMaterial("http://username:password@hgrepo.com", null);
        Map<String, Object> attributes = material.getAttributes(true);

        assertThat(attributes.get("type"), is("mercurial"));
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("mercurial-configuration");
        assertThat(configuration.get("url"), is("http://username:password@hgrepo.com"));
    }

    @Test
    public void shouldGetAttributesWithoutSecureFields() {
        HgMaterial material = new HgMaterial("http://username:password@hgrepo.com", null);
        Map<String, Object> attributes = material.getAttributes(false);

        assertThat(attributes.get("type"), is("mercurial"));
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("mercurial-configuration");
        assertThat(configuration.get("url"), is("http://username:******@hgrepo.com"));
    }
}
