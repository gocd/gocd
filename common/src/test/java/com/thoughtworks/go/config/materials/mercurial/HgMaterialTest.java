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
package com.thoughtworks.go.config.materials.mercurial;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.SecretParam;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.PasswordAwareMaterial;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.materials.*;
import com.thoughtworks.go.domain.materials.mercurial.HgCommand;
import com.thoughtworks.go.domain.materials.mercurial.HgVersion;
import com.thoughtworks.go.domain.materials.mercurial.StringRevision;
import com.thoughtworks.go.helper.HgTestRepo;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.TestRepo;
import com.thoughtworks.go.util.JsonValue;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.command.*;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.thoughtworks.go.helper.MaterialConfigsMother.hg;
import static com.thoughtworks.go.util.JsonUtils.from;
import static com.thoughtworks.go.util.command.ProcessOutputStreamConsumer.inMemoryConsumer;
import static java.lang.String.format;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@EnableRuleMigrationSupport
public class HgMaterialTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static final HgVersion LINUX_HG_094 = HgVersion.parse("Mercurial Distributed SCM (version 0.9.4)\n");
    private static final HgVersion WINDOWS_HG_OFFICIAL_102 = HgVersion.parse("Mercurial Distributed SCM (version 1.0.2+20080813)\n");
    private static final String WINDOWS_HG_TORTOISE = "Mercurial Distributed SCM (version 626cb86a6523+tortoisehg)";
    private static final String REVISION_0 = "b61d12de515d82d3a377ae3aae6e8abe516a2651";
    private static final String REVISION_1 = "35ff2159f303ecf986b3650fc4299a6ffe5a14e1";
    private static final String REVISION_2 = "ca3ebb67f527c0ad7ed26b789056823d8b9af23f";

    @Nested
    class PasswordAware {
        private HgMaterial material;

        @BeforeEach
        void setUp() {
            material = new HgMaterial("some-url", null);
        }

        @Test
        void shouldBePasswordAwareMaterial() {
            assertThat(material).isInstanceOf(PasswordAwareMaterial.class);
        }

        @Test
        void shouldUpdatePasswordFromConfig() {
            assertThat(material.getPassword()).isNull();

            material.updateFromConfig(hg("some-url", "bob", "badger"));

            assertThat(material.getPassword()).isEqualTo("badger");
        }
    }

    @Nested
    class SlowOldTestWhichUsesHgCheckout {
        private HgMaterial hgMaterial;
        private HgTestRepo hgTestRepo;
        private File workingFolder;
        private InMemoryStreamConsumer outputStreamConsumer;

        @BeforeEach
        void setUp() throws Exception {
            temporaryFolder.create();
            hgTestRepo = new HgTestRepo("hgTestRepo1", temporaryFolder);
            hgMaterial = MaterialsMother.hgMaterial(hgTestRepo.projectRepositoryUrl());
            workingFolder = temporaryFolder.newFolder("workingFolder");
            outputStreamConsumer = inMemoryConsumer();
        }

        @AfterEach
        void teardown() {
            temporaryFolder.delete();
            FileUtils.deleteQuietly(workingFolder);
            TestRepo.internalTearDown();
        }

        @Test
        void shouldRefreshWorkingFolderWhenRepositoryChanged() throws Exception {
            new HgCommand(null, workingFolder, "default", hgTestRepo.url().originalArgument(), null).clone(inMemoryConsumer(), hgTestRepo.url());
            File testFile = createNewFileInWorkingFolder();

            HgTestRepo hgTestRepo2 = new HgTestRepo("hgTestRepo2", temporaryFolder);
            hgMaterial = MaterialsMother.hgMaterial(hgTestRepo2.projectRepositoryUrl());
            hgMaterial.latestModification(workingFolder, new TestSubprocessExecutionContext());

            String workingUrl = new HgCommand(null, workingFolder, "default", hgTestRepo.url().originalArgument(), null).workingRepositoryUrl().outputAsString();
            assertThat(workingUrl).isEqualTo(hgTestRepo2.projectRepositoryUrl());
            assertThat(testFile.exists()).isFalse();
        }

        @Test
        @DisabledOnOs(OS.WINDOWS)
        void shouldNotRefreshWorkingFolderWhenFileProtocolIsUsedOnLinux() throws Exception {
            final UrlArgument repoUrl = hgTestRepo.url();
            new HgCommand(null, workingFolder, "default", repoUrl.originalArgument(), null).clone(inMemoryConsumer(), repoUrl);
            File testFile = createNewFileInWorkingFolder();

            hgMaterial = MaterialsMother.hgMaterial("file://" + hgTestRepo.projectRepositoryUrl());
            updateMaterial(hgMaterial, new StringRevision("0"));

            String workingUrl = new HgCommand(null, workingFolder, "default", repoUrl.originalArgument(), null).workingRepositoryUrl().outputAsString();
            assertThat(workingUrl).isEqualTo(hgTestRepo.projectRepositoryUrl());
            assertThat(testFile.exists()).isTrue();
        }

        @Test
        void shouldGetModifications() {
            List<Modification> mods = hgMaterial.modificationsSince(workingFolder, new StringRevision(REVISION_0), new TestSubprocessExecutionContext());
            assertThat(mods.size()).isEqualTo(2);
            Modification modification = mods.get(0);
            assertThat(modification.getRevision()).isEqualTo(REVISION_2);
            assertThat(modification.getModifiedFiles().size()).isEqualTo(1);
        }

        @Test
        void shouldNotAppendDestinationDirectoryWhileFetchingModifications() {
            hgMaterial.setFolder("dest");
            hgMaterial.modificationsSince(workingFolder, new StringRevision(REVISION_0), new TestSubprocessExecutionContext());
            assertThat(new File(workingFolder, "dest").exists()).isFalse();
        }

        @Test
        void shouldGetModificationsBasedOnRevision() {
            List<Modification> modificationsSince = hgMaterial.modificationsSince(workingFolder,
                    new StringRevision(REVISION_0), new TestSubprocessExecutionContext());

            assertThat(modificationsSince.get(0).getRevision()).isEqualTo(REVISION_2);
            assertThat(modificationsSince.get(1).getRevision()).isEqualTo(REVISION_1);
            assertThat(modificationsSince.size()).isEqualTo(2);
        }

        @Test
        void shouldReturnLatestRevisionIfNoModificationsDetected() {
            List<Modification> modification = hgMaterial.modificationsSince(workingFolder,
                    new StringRevision(REVISION_2), new TestSubprocessExecutionContext());
            assertThat(modification.isEmpty()).isTrue();
        }

        @Test
        void shouldUpdateToSpecificRevision() {
            updateMaterial(hgMaterial, new StringRevision("0"));
            File end2endFolder = new File(workingFolder, "end2end");
            assertThat(end2endFolder.listFiles().length).isEqualTo(3);
            assertThat(outputStreamConsumer.getStdOut()).isNotEqualTo("");
            updateMaterial(hgMaterial, new StringRevision("1"));
            assertThat(end2endFolder.listFiles().length).isEqualTo(4);
        }

        @Test
        void shouldUpdateToDestinationFolder() {
            hgMaterial.setFolder("dest");
            updateMaterial(hgMaterial, new StringRevision("0"));
            File end2endFolder = new File(workingFolder, "dest/end2end");
            assertThat(end2endFolder.exists()).isTrue();
        }

        @Test
        void shouldLogRepoInfoToConsoleOutWithoutFolder() {
            updateMaterial(hgMaterial, new StringRevision("0"));
            assertThat(outputStreamConsumer.getStdOut()).contains(format("Start updating %s at revision %s from %s", "files", "0",
                    hgMaterial.getUrl()));
        }

        @Test
        void shouldDeleteWorkingFolderWhenItIsNotAnHgRepository() throws Exception {
            File testFile = createNewFileInWorkingFolder();
            hgMaterial.latestModification(workingFolder, new TestSubprocessExecutionContext());

            assertThat(testFile.exists()).isFalse();
        }

        @Test
        void shouldThrowExceptionWithUsefulInfoIfFailedToFindModifications() {
            final String url = "/tmp/notExistDir";
            hgMaterial = MaterialsMother.hgMaterial(url);
            try {
                hgMaterial.latestModification(workingFolder, new TestSubprocessExecutionContext());
                fail("Should have thrown an exception when failed to clone from an invalid url");
            } catch (Exception e) {
                assertThat(e.getMessage()).contains("abort: repository " + url + " not found!");
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
        void shouldReturnFalseWhenVersionIsNotRecgonized() {
            assertThatCode(() -> hgMaterial.isVersionOneDotZeroOrHigher(WINDOWS_HG_TORTOISE))
                    .isInstanceOf(Exception.class);
        }

        @Test
        void shouldCheckConnection() {
            ValidationBean validation = hgMaterial.checkConnection(new TestSubprocessExecutionContext());
            assertThat(validation.isValid()).isTrue();
            String notExistUrl = "http://notExisthost/hg";
            hgMaterial = MaterialsMother.hgMaterial(notExistUrl);
            validation = hgMaterial.checkConnection(new TestSubprocessExecutionContext());
            assertThat(validation.isValid()).isFalse();
        }

        @Test
        void shouldReturnInvalidBeanWithRootCauseAsLowerVersionInstalled() {
            ValidationBean validationBean = hgMaterial.handleException(new Exception(), LINUX_HG_094);
            assertThat(validationBean.isValid()).isFalse();
            assertThat(validationBean.getError()).contains("Please install Mercurial Version 1.0 or above");
        }

        @Test
        void shouldReturnInvalidBeanWithRootCauseAsRepositoryURLIsNotFound() {
            ValidationBean validationBean = hgMaterial.handleException(new Exception(), WINDOWS_HG_OFFICIAL_102);
            assertThat(validationBean.isValid()).isFalse();
            assertThat(validationBean.getError()).contains("not found!");
        }

        @Test
        void shouldBeAbleToConvertToJson() {
            Map<String, Object> json = new LinkedHashMap<>();
            hgMaterial.toJson(json, new StringRevision("123"));

            JsonValue jsonValue = from(json);
            assertThat(jsonValue.getString("scmType")).isEqualTo("Mercurial");
            assertThat(new File(jsonValue.getString("location"))).isEqualTo(new File(hgTestRepo.projectRepositoryUrl()));
            assertThat(jsonValue.getString("action")).isEqualTo("Modified");
        }

        // #3103
        @Test
        void shouldParseComplexCommitMessage() throws Exception {
            String comment = "changeset:   8139:b1a0b0bbb4d1\n"
                    + "branch:      trunk\n"
                    + "user:        QYD\n"
                    + "date:        Tue Jun 30 14:56:37 2009 +0800\n"
                    + "summary:     add story #3001 - 'Pipelines should use the latest version of ...";
            hgTestRepo.commitAndPushFile("SomeDocumentation.txt", comment);

            List<Modification> modification = hgMaterial.latestModification(workingFolder, new TestSubprocessExecutionContext());

            assertThat(modification.size()).isEqualTo(1);
            assertThat(modification.get(0).getComment()).isEqualTo(comment);
        }

        @Test
        void shouldGenerateSqlCriteriaMapInSpecificOrder() {
            Map<String, Object> map = hgMaterial.getSqlCriteria();
            assertThat(map.size()).isEqualTo(2);
            Iterator<Map.Entry<String, Object>> iter = map.entrySet().iterator();
            assertThat(iter.next().getKey()).isEqualTo("type");
            assertThat(iter.next().getKey()).isEqualTo("url");
        }

        private void updateMaterial(HgMaterial hgMaterial, StringRevision revision) {
            hgMaterial.updateTo(outputStreamConsumer, workingFolder, new RevisionContext(revision), new TestSubprocessExecutionContext());
        }
    }

    @Test
    void checkConnectionShouldUseUrlForCommandLine() {
        final HgMaterial material = spy(new HgMaterial("http://example.com", ""));
        material.setUserName("bob");
        material.setPassword("password");

        material.checkConnection(new TestSubprocessExecutionContext());

        verify(material, atLeastOnce()).urlForCommandLine();
    }

    @Test
    void shouldNotRefreshWorkingDirectoryIfDefaultRemoteUrlDoesNotContainPasswordButMaterialUrlDoes() throws Exception {
        final HgMaterial material = new HgMaterial("http://user:pwd@domain:9999/path", null);
        final HgCommand hgCommand = mock(HgCommand.class);
        final ConsoleResult consoleResult = mock(ConsoleResult.class);
        when(consoleResult.outputAsString()).thenReturn("http://user@domain:9999/path");
        when(hgCommand.workingRepositoryUrl()).thenReturn(consoleResult);
        assertThat((Boolean) ReflectionUtil.invoke(material, "isRepositoryChanged", hgCommand)).isFalse();
    }

    @Test
    void shouldRefreshWorkingDirectoryIfUsernameInDefaultRemoteUrlIsDifferentFromOneInMaterialUrl() throws Exception {
        final HgMaterial material = new HgMaterial("http://some_new_user:pwd@domain:9999/path", null);
        final HgCommand hgCommand = mock(HgCommand.class);
        final ConsoleResult consoleResult = mock(ConsoleResult.class);
        when(consoleResult.outputAsString()).thenReturn("http://user:pwd@domain:9999/path");
        when(hgCommand.workingRepositoryUrl()).thenReturn(consoleResult);
        assertThat((Boolean) ReflectionUtil.invoke(material, "isRepositoryChanged", hgCommand)).isTrue();
    }

    @Test
    void shouldBeEqualWhenUrlSameForHgMaterial() {
        final Material material = MaterialsMother.hgMaterials("url1", "hgdir").get(0);
        final Material anotherMaterial = MaterialsMother.hgMaterials("url1", "hgdir").get(0);
        assertThat(material.equals(anotherMaterial)).isTrue();
        assertThat(anotherMaterial.equals(material)).isTrue();
    }

    @Test
    void shouldNotBeEqualWhenUrlDifferent() {
        final Material material1 = MaterialsMother.hgMaterials("url1", "hgdir").get(0);
        final Material material2 = MaterialsMother.hgMaterials("url2", "hgdir").get(0);
        assertThat(material1.equals(material2)).isFalse();
        assertThat(material2.equals(material1)).isFalse();
    }

    @Test
    void shouldNotBeEqualWhenTypeDifferent() {
        final Material material = MaterialsMother.hgMaterials("url1", "hgdir").get(0);
        final Material svnMaterial = MaterialsMother.defaultSvnMaterialsWithUrl("url1").get(0);
        assertThat(material.equals(svnMaterial)).isFalse();
        assertThat(svnMaterial.equals(material)).isFalse();
    }

    @Test
    void shouldBeEqual() {
        final Material hgMaterial1 = MaterialsMother.hgMaterials("url1", "hgdir").get(0);
        final Material hgMaterial2 = MaterialsMother.hgMaterials("url1", "hgdir").get(0);
        assertThat(hgMaterial1.equals(hgMaterial2)).isTrue();
        assertThat(hgMaterial1.hashCode()).isEqualTo(hgMaterial2.hashCode());
    }

    @Test
    void shouldRemoveTheForwardSlashAndApplyThePattern() {
        Material material = MaterialsMother.hgMaterial();

        assertThat(material.matches("a.doc", "/a.doc")).isTrue();
        assertThat(material.matches("/a.doc", "a.doc")).isFalse();
    }

    @Test
    void shouldApplyThePatternDirectly() {
        Material material = MaterialsMother.hgMaterial();

        assertThat(material.matches("a.doc", "a.doc")).isTrue();
    }

    /**
     * An hg abbreviated hash is 12 chars. See the hg documentation.
     * %h:	short-form changeset hash (12 bytes of hexadecimal)
     */
    @Test
    void shouldtruncateHashTo12charsforAShortRevision() {
        Material hgMaterial = new HgMaterial("file:///foo", null);
        assertThat(hgMaterial.getShortRevision("dc3d7e656831d1b203d8b7a63c4de82e26604e52")).isEqualTo("dc3d7e656831");
        assertThat(hgMaterial.getShortRevision("dc3d7e65683")).isEqualTo("dc3d7e65683");
        assertThat(hgMaterial.getShortRevision("dc3d7e6568312")).isEqualTo("dc3d7e656831");
        assertThat(hgMaterial.getShortRevision("24")).isEqualTo("24");
        assertThat(hgMaterial.getShortRevision(null)).isNull();
    }

    @Test
    void shouldNotDisplayPasswordInToString() {
        HgMaterial hgMaterial = new HgMaterial("https://user:loser@foo.bar/baz?quux=bang", null);
        assertThat(hgMaterial.toString()).doesNotContain("loser");
    }

    @Test
    void shouldGetLongDescriptionForMaterial() {
        HgMaterial material = new HgMaterial("http://url/", "folder");
        assertThat(material.getLongDescription()).isEqualTo("URL: http://url/");
    }

    @Test
    void shouldFindTheBranchName() {
        HgMaterial material = new HgMaterial("http://url/##foo", "folder");
        assertThat(material.getBranch()).isEqualTo("foo");
    }

    @Test
    void shouldSetDefaultAsBranchNameIfBranchNameIsNotSpecifiedInUrl() {
        HgMaterial material = new HgMaterial("http://url/", "folder");
        assertThat(material.getBranch()).isEqualTo("default");
    }

    @Test
    void shouldMaskThePasswordInDisplayName() {
        HgMaterial material = new HgMaterial("http://user:pwd@url##branch", "folder");
        assertThat(material.getDisplayName()).isEqualTo("http://user:******@url##branch");
    }

    @Test
    void shouldGetAttributesWithSecureFields() {
        HgMaterial material = new HgMaterial("http://username:password@hgrepo.com", null);
        Map<String, Object> attributes = material.getAttributes(true);

        assertThat(attributes.get("type")).isEqualTo("mercurial");
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("mercurial-configuration");
        assertThat(configuration.get("url")).isEqualTo("http://username:password@hgrepo.com");
    }

    @Test
    void shouldGetAttributesWithoutSecureFields() {
        HgMaterial material = new HgMaterial("http://username:password@hgrepo.com", null);
        Map<String, Object> attributes = material.getAttributes(false);

        assertThat(attributes.get("type")).isEqualTo("mercurial");
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("mercurial-configuration");
        assertThat(configuration.get("url")).isEqualTo("http://username:******@hgrepo.com");
    }

    @Nested
    class Equals {
        @Test
        void shouldBeEqualIfObjectsHaveSameUrlAndBranch() {
            final HgMaterial material_1 = new HgMaterial("http://example.com", "master");
            material_1.setUserName("bob");
            material_1.setBranch("feature");

            final HgMaterial material_2 = new HgMaterial("http://example.com", "master");
            material_2.setUserName("bob");
            material_2.setBranch("feature");

            assertThat(material_1.equals(material_2)).isTrue();
        }
    }

    @Nested
    class Fingerprint {
        @Test
        void shouldGenerateFingerprintForGivenMaterialUrl() {
            HgMaterial hgMaterial = new HgMaterial("https://bob:pass@github.com/gocd#feature", "dest");

            assertThat(hgMaterial.getFingerprint()).isEqualTo("d84d91f37da0367a9bd89fff0d48638f5c1bf993d637735ec26f13c21c23da19");
        }

        @Test
        void shouldConsiderBranchWhileGeneratingFingerprint_IfBranchSpecifiedAsAnAttribute() {
            HgMaterial hgMaterial = new HgMaterial("https://bob:pass@github.com/gocd", "dest");
            hgMaterial.setBranch("feature");

            assertThat(hgMaterial.getFingerprint()).isEqualTo("db13278ed2b804fc5664361103bcea3d7f5106879683085caed4311aa4d2f888");
        }

        @Test
        void branchInUrlShouldGenerateFingerprintWhichIsOtherFromBranchInAttribute() {
            HgMaterial hgMaterialWithBranchInUrl = new HgMaterial("https://github.com/gocd##feature", "dest");

            HgMaterial hgMaterialWithBranchAsAttribute = new HgMaterial("https://github.com/gocd", "dest");
            hgMaterialWithBranchAsAttribute.setBranch("feature");

            assertThat(hgMaterialWithBranchInUrl.getFingerprint())
                    .isNotEqualTo(hgMaterialWithBranchAsAttribute.getFingerprint());
        }

        @Nested
        class fingerPrintComparisionBetweenHgMaterialAndHgMaterialConfig {
            @Test
            void fingerprintGeneratedByHgMaterialAndHgMaterialConfigShouldBeSame() {
                HgMaterial hgMaterial = new HgMaterial("https://github.com/gocd#feature", "dest");

                assertThat(hgMaterial.getFingerprint()).isEqualTo(hgMaterial.config().getFingerprint());
            }

            @Test
            void fingerprintGeneratedByHgMaterialAndHgMaterialConfigShouldBeSameWhenBranchProvidedAsAttribute() {
                HgMaterial hgMaterial = new HgMaterial("https://github.com/gocd", "dest");
                hgMaterial.setBranch("feature");

                assertThat(hgMaterial.getFingerprint()).isEqualTo(hgMaterial.config().getFingerprint());
            }
        }
    }

    @Nested
    class getBranch {
        @Test
        void shouldBeTheBranchAttributeIfSpecified() {
            HgMaterial hgMaterial = new HgMaterial("https://github.com/gocd", "dest");
            hgMaterial.setBranch("feature");

            assertThat(hgMaterial.getBranch()).isEqualTo("feature");
        }

        @Test
        void shouldBeBranchFromUrlIfBranchNotSpecifedAsAttribute() {
            HgMaterial hgMaterial = new HgMaterial("https://github.com/gocd#from_url", "dest");

            assertThat(hgMaterial.getBranch()).isEqualTo("from_url");
        }

        @Test
        void shouldBeDefaultBranchIfBranchNotSpecified() {
            HgMaterial hgMaterial = new HgMaterial("https://github.com/gocd", "dest");

            assertThat(hgMaterial.getBranch()).isEqualTo("default");
        }
    }

    @Nested
    class ConfigToMaterial {
        @Test
        void shouldBuildFromConfigObject() {
            final HgMaterialConfig materialConfig = hg(new HgUrlArgument("http://example.com"), "bob", "pass",
                    "feature", true, Filter.create("igrnored"), false, "destination",
                    new CaseInsensitiveString("example"));

            final HgMaterial hgMaterial = new HgMaterial(materialConfig);

            assertThat(hgMaterial.getUrl()).isEqualTo(materialConfig.getUrl());
            assertThat(hgMaterial.getUserName()).isEqualTo(materialConfig.getUserName());
            assertThat(hgMaterial.getPassword()).isEqualTo(materialConfig.getPassword());
            assertThat(hgMaterial.getBranch()).isEqualTo(materialConfig.getBranch());
            assertThat(hgMaterial.getAutoUpdate()).isEqualTo(materialConfig.getAutoUpdate());
            assertThat(hgMaterial.getInvertFilter()).isEqualTo(materialConfig.getInvertFilter());
            assertThat(hgMaterial.getFolder()).isEqualTo(materialConfig.getFolder());
            assertThat(hgMaterial.getName()).isEqualTo(materialConfig.getName());
            assertThat(hgMaterial.getFingerprint()).isEqualTo(materialConfig.getFingerprint());
        }
    }

    @Nested
    class MaterialToConfig {
        @Test
        void shouldBuildConfigFromMaterialObject() {
            final HgMaterial hgMaterial = new HgMaterial("http://example.com", "destination");
            hgMaterial.setUserName("bob");
            hgMaterial.setPassword("pass");
            hgMaterial.setBranch("feature");
            hgMaterial.setAutoUpdate(true);
            hgMaterial.setName(new CaseInsensitiveString("example"));
            hgMaterial.setInvertFilter(true);
            hgMaterial.setFolder("destination");
            hgMaterial.setFilter(Filter.create("allow"));

            final HgMaterialConfig materialConfig = (HgMaterialConfig) hgMaterial.config();

            assertThat(hgMaterial.getUrl()).isEqualTo(materialConfig.getUrl());
            assertThat(hgMaterial.getUserName()).isEqualTo(materialConfig.getUserName());
            assertThat(hgMaterial.getPassword()).isEqualTo(materialConfig.getPassword());
            assertThat(hgMaterial.getBranch()).isEqualTo(materialConfig.getBranch());
            assertThat(hgMaterial.getAutoUpdate()).isEqualTo(materialConfig.getAutoUpdate());
            assertThat(hgMaterial.getInvertFilter()).isEqualTo(materialConfig.getInvertFilter());
            assertThat(hgMaterial.getFolder()).isEqualTo(materialConfig.getFolder());
            assertThat(hgMaterial.getName()).isEqualTo(materialConfig.getName());
            assertThat(hgMaterial.getFingerprint()).isEqualTo(materialConfig.getFingerprint());
        }
    }

    @Nested
    class urlForCommandLine {
        @Test
        void shouldBeSameAsTheConfiguredUrlForTheMaterial() {
            final HgMaterial hgMaterial = new HgMaterial("http://bob:pass@exampele.com", "destination");

            assertThat(hgMaterial.urlForCommandLine()).isEqualTo("http://bob:pass@exampele.com");
        }

        @Test
        void shouldIncludeUserInfoIfProvidedAsUsernameAndPasswordAttributes() {
            final HgMaterial hgMaterial = new HgMaterial("http://exampele.com", "destination");
            hgMaterial.setUserName("bob");
            hgMaterial.setPassword("pass");

            assertThat(hgMaterial.urlForCommandLine()).isEqualTo("http://bob:pass@exampele.com");
        }

        @Test
        void shouldIncludeUserNameIfProvidedAsUsernameAttributes() {
            final HgMaterial hgMaterial = new HgMaterial("http://exampele.com", "destination");
            hgMaterial.setUserName("bob");

            assertThat(hgMaterial.urlForCommandLine()).isEqualTo("http://bob@exampele.com");
        }

        @Test
        void shouldIncludePasswordIfProvidedAsPasswordAttribute() {
            final HgMaterial hgMaterial = new HgMaterial("http://exampele.com", "destination");
            hgMaterial.setPassword("pass");

            assertThat(hgMaterial.urlForCommandLine()).isEqualTo("http://:pass@exampele.com");
        }

        @Test
        void shouldEncodeUserInfoIfProvidedAsUsernamePasswordAttributes() {
            final HgMaterial hgMaterial = new HgMaterial("http://exampele.com", "destination");
            hgMaterial.setUserName("bob@example.com");
            hgMaterial.setPassword("p@ssw:rd");

            assertThat(hgMaterial.urlForCommandLine()).isEqualTo("http://bob%40example.com:p%40ssw:rd@exampele.com");
        }

        @Test
        void shouldHaveResolvedUserInfoIfSecretParamsIsPresent() {
            final HgMaterial gitMaterial = new HgMaterial("http://exampele.com", "destination");
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
            final HgMaterial hgMaterial = new HgMaterial("http://exampele.com#feature", "destination");
            hgMaterial.setUserName("bob");

            MaterialInstance materialInstance = hgMaterial.createMaterialInstance();

            assertThat(materialInstance.getUrl()).isEqualTo(hgMaterial.getUrl());
            assertThat(materialInstance.getUsername()).isEqualTo(hgMaterial.getUserName());
            assertThat(materialInstance.getBranch()).isNull();
        }

        @Test
        void shouldCreateMaterialInstanceObjectWithBranchProvidedAttributeInMaterial() {
            final HgMaterial hgMaterial = new HgMaterial("http://exampele.com", "destination");
            hgMaterial.setUserName("bob");
            hgMaterial.setBranch("branch-as-attribute");

            MaterialInstance materialInstance = hgMaterial.createMaterialInstance();

            assertThat(materialInstance.getUrl()).isEqualTo(hgMaterial.getUrl());
            assertThat(materialInstance.getUsername()).isEqualTo(hgMaterial.getUserName());
            assertThat(materialInstance.getBranch()).isEqualTo("branch-as-attribute");
        }
    }

    @Nested
    class secrets {
        @Test
        void shouldReplaceUrlForCommandLineWithUrlForDisplay_whenCredntialsAreProvidedInUrl() {
            HgMaterial hgMaterial = new HgMaterial("https://bob:pass@example.com", "destinations");

            String info = hgMaterial.secrets().get(0).replaceSecretInfo("https://bob:pass@example.com");

            assertThat(info).isEqualTo(hgMaterial.getUriForDisplay());
        }

        @Test
        void shouldReplaceUrlForCommandLineWithUrlForDisplay_whenCredentialsAreProvidedAsAttributes() {
            HgMaterial hgMaterial = new HgMaterial("https://example.com", "destinations");
            hgMaterial.setUserName("bob");
            hgMaterial.setPassword("pass");

            String info = hgMaterial.secrets().get(0).replaceSecretInfo("https://bob:pass@example.com");

            assertThat(info).isEqualTo(hgMaterial.getUriForDisplay());
        }
    }

    @Test
    void populateEnvContextShouldSetMaterialEnvVars() {
        HgMaterial material = new HgMaterial("https://user:password@github.com/bob/my-project", "folder");
        material.setBranch("branchName");

        EnvironmentVariableContext ctx = new EnvironmentVariableContext();
        final ArrayList<Modification> modifications = new ArrayList<>();

        modifications.add(new Modification("user2", "comment2", "email2", new Date(), "24"));
        modifications.add(new Modification("user1", "comment1", "email1", new Date(), "23"));

        MaterialRevision materialRevision = new MaterialRevision(material, modifications);
        assertThat(ctx.getProperty(ScmMaterial.GO_MATERIAL_URL)).isNull();
        assertThat(ctx.getProperty(GitMaterial.GO_MATERIAL_BRANCH)).isNull();

        material.populateEnvironmentContext(ctx, materialRevision, new File("."));
        String propertySuffix = material.getMaterialNameForEnvironmentVariable();
        assertThat(ctx.getProperty(format("%s_%s", ScmMaterial.GO_MATERIAL_URL, propertySuffix))).isEqualTo("https://github.com/bob/my-project");
        assertThat(ctx.getProperty(format("%s_%s", GitMaterial.GO_MATERIAL_BRANCH, propertySuffix))).isEqualTo("branchName");
    }

    @Test
    void shouldPopulateBranchWithDefaultIfNotSet() {
        HgMaterial material = new HgMaterial("https://user:password@github.com/bob/my-project", "folder");

        EnvironmentVariableContext ctx = new EnvironmentVariableContext();
        final ArrayList<Modification> modifications = new ArrayList<>();

        modifications.add(new Modification("user1", "comment1", "email1", new Date(), "23"));

        MaterialRevision materialRevision = new MaterialRevision(material, modifications);

        material.populateEnvironmentContext(ctx, materialRevision, new File("."));
        String propertySuffix = material.getMaterialNameForEnvironmentVariable();
        assertThat(ctx.getProperty(format("%s_%s", GitMaterial.GO_MATERIAL_BRANCH, propertySuffix))).isEqualTo("default");
    }
}
