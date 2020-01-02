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
package com.thoughtworks.go.domain.materials;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4Material;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.util.command.ConsoleOutputStreamConsumer;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

import static com.thoughtworks.go.helper.MaterialConfigsMother.hg;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

@EnableRuleMigrationSupport
public class MaterialsTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeEach
    void setup() throws IOException {
        temporaryFolder.create();
    }

    @AfterEach
    void tearDown() {
        temporaryFolder.delete();
    }

    @Test
    void shouldKnowModificationCheckInterval() {
        final Materials materials = new Materials(42, new ArrayList<>());
        assertThat(materials.interval()).isEqualTo(42);
    }

    @Test
    void shouldGetMaterialByFolder() {
        Materials materials = new Materials();
        HgMaterial material1 = MaterialsMother.hgMaterial();
        material1.setFolder("folder1");

        HgMaterial material2 = MaterialsMother.hgMaterial();
        material2.setFolder("folder2");

        materials.add(material1);
        materials.add(material2);

        assertThat(materials.byFolder("folder1")).isEqualTo(material1);
    }

    @Test
    void shouldNotGetDependencyMaterialWhenOneOtherScmMaterialWithNoFolder() {
        Materials materials = new Materials();
        Material material1 = new DependencyMaterial(new CaseInsensitiveString("foo"), new CaseInsensitiveString("bar"));

        Material material2 = new HgMaterial("", null);

        materials.add(material1);
        materials.add(material2);

        assertThat(materials.byFolder(null)).isEqualTo(material2);
    }

    @Test
    void shouldGetMaterialByFolderWhenHasOnlyOneMaterial() {
        Materials materials = new Materials();
        HgMaterial material1 = MaterialsMother.hgMaterial();

        materials.add(material1);

        assertThat(materials.byFolder(material1.getFolder())).isEqualTo(material1);
    }

    @Test
    void shouldNotGetPackageMaterialWhenOneOtherScmMaterialWithNoFolder() {
        Materials materials = new Materials();
        Material material1 = new PackageMaterial("pid");
        Material material2 = new HgMaterial("", null);
        materials.add(material1);
        materials.add(material2);

        assertThat(materials.byFolder(null)).isEqualTo(material2);
    }

    @Test
    void shouldGetPluggableSCMMaterial_byFolder() {
        Materials materials = new Materials();
        PluggableSCMMaterial material1 = new PluggableSCMMaterial("scm-id");
        material1.setFolder("folder");
        Material material2 = new HgMaterial("", "folder");
        materials.add(material1);
        materials.add(material2);

        assertThat(materials.byFolder("folder")).isEqualTo(material1);
    }

    @Test
    void shouldReturnMaterialMatchingTheGivenMaterial() {
        Materials materials = new Materials();
        HgMaterial material1 = MaterialsMother.hgMaterial();
        material1.setFilter(new Filter(new IgnoredFiles("patter")));
        SvnMaterial material2 = MaterialsMother.svnMaterial();

        materials.add(material1);
        materials.add(material2);

        assertThat(materials.get(MaterialsMother.hgMaterial())).isEqualTo(material1);
        try {
            materials.get(MaterialsMother.p4Material());
            fail("Must not have found the p4 material");
        } catch (Exception expected) {
        }
    }

    @Test
    void shouldReturnMaterialBasedOnPiplineUniqueFingerPrint() {
        Materials materials = new Materials();
        HgMaterial expectedMaterial = MaterialsMother.hgMaterial();
        materials.add(expectedMaterial);
        materials.add(MaterialsMother.gitMaterial("url"));
        materials.add(MaterialsMother.svnMaterial("url", "folder"));

        Material actualMaterial = materials.getByFingerPrint(expectedMaterial.getPipelineUniqueFingerprint());
        assertThat(actualMaterial).isEqualTo(expectedMaterial);
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldFailIfMultipleMaterialsHaveSameFolderNameSet_CaseInSensitive() {
        HgMaterialConfig materialOne = hg("http://url1", null);
        materialOne.setConfigAttributes(Collections.singletonMap(ScmMaterialConfig.FOLDER, "folder"));
        HgMaterialConfig materialTwo = hg("http://url2", null);
        materialTwo.setConfigAttributes(Collections.singletonMap(ScmMaterialConfig.FOLDER, "foLder"));
        CruiseConfig config = GoConfigMother.configWithPipelines("one");
        PipelineConfig pipelineOne = config.pipelineConfigByName(new CaseInsensitiveString("one"));
        pipelineOne.setMaterialConfigs(new MaterialConfigs(materialOne, materialTwo));

        MaterialConfigs materials = pipelineOne.materialConfigs();
        materials.validate(ConfigSaveValidationContext.forChain(config));

        assertThat(materials.get(0).errors().isEmpty()).isFalse();
        assertThat(materials.get(1).errors().isEmpty()).isFalse();

        assertThat(materials.get(0).errors().on(ScmMaterialConfig.FOLDER)).isEqualTo("The destination directory must be unique across materials.");
        assertThat(materials.get(1).errors().on(ScmMaterialConfig.FOLDER)).isEqualTo("The destination directory must be unique across materials.");
    }

    @Test
    void shouldReturnTrueIfScmMaterialHasNoDestinationFolderSet() {
        Materials materials = new Materials();
        SvnMaterial material1 = new SvnMaterial("url", "user", "pass", false);
        DependencyMaterial material2 = new DependencyMaterial(new CaseInsensitiveString("pipelineName"), new CaseInsensitiveString("stageName"));
        SvnMaterial material3 = new SvnMaterial("url", "user", "pass", false);
        material3.setFolder("foo");
        materials.add(material1);
        materials.add(material2);

        assertThat(materials.scmMaterialsHaveDestination()).isFalse();
    }

    @Test
    void shouldReturnANewSvnMaterialIfTheMaterialsCollectionDoesNotHaveASvnMaterial() {
        assertThat(new Materials().getSvnMaterial()).isEqualTo(new SvnMaterial("", "", "", false));
    }

    @Test
    void shouldReturnExistingSvnMaterialFromMaterialsIfItContainsOne() {
        Materials materials = new Materials();
        SvnMaterial existingMaterial = new SvnMaterial("foo", "bar", "blah", true);
        materials.add(existingMaterial);
        assertThat(materials.getSvnMaterial()).isSameAs(existingMaterial);
    }

    @Test
    void shouldReturnANewGitMaterialIfTheMaterialsCollectionDoesNotHaveAGitMaterial() {
        assertThat(new Materials().getGitMaterial()).isEqualTo(new GitMaterial(""));
    }

    @Test
    void shouldReturnExistingGitMaterialFromMaterialsIfItContainsOne() {
        Materials materials = new Materials();
        GitMaterial existingMaterial = new GitMaterial("foo");
        materials.add(existingMaterial);
        assertThat(materials.getGitMaterial()).isSameAs(existingMaterial);
    }

    @Test
    void shouldReturnAP4SvnMaterialIfTheMaterialsCollectionDoesNotHaveAP4Material() {
        assertThat(new Materials().getP4Material()).isEqualTo(new P4Material("", ""));
    }

    @Test
    void shouldReturnExistingP4MaterialFromMaterialsIfItContainsOne() {
        Materials materials = new Materials();
        P4Material existingMaterial = new P4Material("foo", "bar");
        materials.add(existingMaterial);
        assertThat(materials.getP4Material()).isSameAs(existingMaterial);
    }

    @Test
    void shouldReturnANewHgMaterialIfTheMaterialsCollectionDoesNotHaveAHgMaterial() {
        assertThat(new Materials().getHgMaterial()).isEqualTo(new HgMaterial("", null));
    }

    @Test
    void shouldReturnExistingHgMaterialFromMaterialsIfItContainsOne() {
        Materials materials = new Materials();
        HgMaterial existingMaterial = new HgMaterial("foo", null);
        materials.add(existingMaterial);
        assertThat(materials.getHgMaterial()).isSameAs(existingMaterial);
    }

    @Test
    void shouldReturnANewDependencyMaterialIfTheMaterialsCollectionDoesNotHaveAHgMaterial() {
        assertThat(new Materials().getDependencyMaterial()).isEqualTo(new DependencyMaterial(new CaseInsensitiveString(""), new CaseInsensitiveString("")));
    }

    @Test
    void shouldReturnExistingDependencyMaterialFromMaterialsIfItContainsOne() {
        Materials materials = new Materials();
        DependencyMaterial existingMaterial = new DependencyMaterial(new CaseInsensitiveString("foo"), new CaseInsensitiveString("bar"));
        materials.add(existingMaterial);
        assertThat(materials.getDependencyMaterial()).isSameAs(existingMaterial);
    }

    @Test
    void shouldRemoveJunkFoldersWhenCleanUpIsCalled_hasOneMaterialUseBaseFolderReturnsFalse() throws Exception {
        File junkFolder = temporaryFolder.newFolder("junk-folder");
        Materials materials = new Materials();
        GitMaterial gitMaterial = new GitMaterial("http://some-url.com", "some-branch", "some-folder");
        materials.add(gitMaterial);

        materials.cleanUp(temporaryFolder.getRoot(), mock(ConsoleOutputStreamConsumer.class));

        assertThat(junkFolder.exists()).isFalse();
        temporaryFolder.delete();
    }

    @Test
    void shouldNotRemoveJunkFoldersWhenCleanUpIsCalled_hasOneMaterialUseBaseFolderReturnsTrue() throws Exception {
        File junkFolder = temporaryFolder.newFolder("junk-folder");
        Materials materials = new Materials();
        GitMaterial gitMaterial = new GitMaterial("http://some-url.com", "some-branch");
        materials.add(gitMaterial);

        materials.cleanUp(temporaryFolder.getRoot(), mock(ConsoleOutputStreamConsumer.class));

        assertThat(junkFolder.exists()).isTrue();
        temporaryFolder.delete();
    }

}
