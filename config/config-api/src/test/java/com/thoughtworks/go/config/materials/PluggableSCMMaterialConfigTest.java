/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.plugin.access.scm.SCMView;
import com.thoughtworks.go.util.TempDirUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.config.materials.ScmMaterialConfig.FOLDER;
import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

public class PluggableSCMMaterialConfigTest {
    private PluggableSCMMaterialConfig pluggableSCMMaterialConfig;
    @TempDir
    public Path temporaryFolder;

    @BeforeEach
    public void setUp() {
        pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig("scm-id");
        SCMMetadataStore.getInstance().clear();
    }

    @AfterEach
    public void tearDown() {
        SCMMetadataStore.getInstance().clear();
    }

    @Test
    public void shouldAddErrorIfMaterialDoesNotHaveASCMId() {
        pluggableSCMMaterialConfig.setScmId(null);
        pluggableSCMMaterialConfig.validateConcreteMaterial(new ConfigSaveValidationContext(null, null));

        assertThat(pluggableSCMMaterialConfig.errors().getAll().size()).isEqualTo(1);
        assertThat(pluggableSCMMaterialConfig.errors().firstErrorOn(PluggableSCMMaterialConfig.SCM_ID)).isEqualTo("Please select a SCM");
    }

    @Test
    public void shouldAddErrorIfSCMNameUniquenessValidationFails() {
        Map<CaseInsensitiveString, AbstractMaterialConfig> nameToMaterialMap = new HashMap<>();
        PluggableSCMMaterialConfig existingMaterial = new PluggableSCMMaterialConfig("scm-id");
        nameToMaterialMap.put(new CaseInsensitiveString("scm-id"), existingMaterial);
        nameToMaterialMap.put(new CaseInsensitiveString("foo"), git("url"));

        pluggableSCMMaterialConfig.validateNameUniqueness(nameToMaterialMap);

        assertThat(pluggableSCMMaterialConfig.errors().getAll().size()).isEqualTo(1);
        assertThat(pluggableSCMMaterialConfig.errors().firstErrorOn(PluggableSCMMaterialConfig.SCM_ID)).isEqualTo("Duplicate SCM material detected!");
        assertThat(existingMaterial.errors().getAll().size()).isEqualTo(1);
        assertThat(existingMaterial.errors().firstErrorOn(PluggableSCMMaterialConfig.SCM_ID)).isEqualTo("Duplicate SCM material detected!");
        assertThat(nameToMaterialMap.size()).isEqualTo(2);
    }

    @Test
    public void shouldPassMaterialUniquenessIfIfNoDuplicateSCMFound() {
        Map<CaseInsensitiveString, AbstractMaterialConfig> nameToMaterialMap = new HashMap<>();
        nameToMaterialMap.put(new CaseInsensitiveString("scm-id-new"), new PluggableSCMMaterialConfig("scm-id-new"));
        nameToMaterialMap.put(new CaseInsensitiveString("foo"), git("url"));

        pluggableSCMMaterialConfig.validateNameUniqueness(nameToMaterialMap);

        assertThat(pluggableSCMMaterialConfig.errors().getAll().size()).isEqualTo(0);
        assertThat(nameToMaterialMap.size()).isEqualTo(3);
    }

    @Test
    public void shouldNotAddErrorDuringUniquenessValidationIfSCMNameIsEmpty() {
        pluggableSCMMaterialConfig.setScmId("");

        Map<CaseInsensitiveString, AbstractMaterialConfig> nameToMaterialMap = new HashMap<>();

        pluggableSCMMaterialConfig.validateNameUniqueness(nameToMaterialMap);

        assertThat(pluggableSCMMaterialConfig.errors().getAll().size()).isEqualTo(0);
        assertThat(nameToMaterialMap.size()).isEqualTo(0);
    }

    @Test
    public void shouldAddErrorIDestinationIsNotValid() {
        ConfigSaveValidationContext configSaveValidationContext = mock(ConfigSaveValidationContext.class);
        SCM scmConfig = mock(SCM.class);
        when(configSaveValidationContext.findScmById(anyString())).thenReturn(scmConfig);
        when(scmConfig.doesPluginExist()).thenReturn(true);
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(null, scmConfig, "/usr/home", null, false);
        pluggableSCMMaterialConfig.setScmId("scm-id");
        pluggableSCMMaterialConfig.validateConcreteMaterial(configSaveValidationContext);

        assertThat(pluggableSCMMaterialConfig.errors().getAll().size()).isEqualTo(1);
        assertThat(pluggableSCMMaterialConfig.errors().firstErrorOn(PluggableSCMMaterialConfig.FOLDER)).isEqualTo("Dest folder '/usr/home' is not valid. It must be a sub-directory of the working folder.");

        pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(null, scmConfig, "./../crap", null, false);
        pluggableSCMMaterialConfig.setScmId("scm-id");
        pluggableSCMMaterialConfig.validateConcreteMaterial(configSaveValidationContext);

        assertThat(pluggableSCMMaterialConfig.errors().getAll().size()).isEqualTo(2);
        assertThat(pluggableSCMMaterialConfig.errors().firstErrorOn(PluggableSCMMaterialConfig.FOLDER)).isEqualTo("Invalid directory name './../crap'. It should be a valid relative path.");
    }


    @Test
    public void shouldAddErrorWhenMatchingScmConfigDoesNotExist() {
        PipelineConfigSaveValidationContext validationContext = mock(PipelineConfigSaveValidationContext.class);
        when(validationContext.findScmById(anyString())).thenReturn(null);
        SCM scmConfig = mock(SCM.class);
        when(scmConfig.doesPluginExist()).thenReturn(true);
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(null, scmConfig, "usr/home", null, false);
        pluggableSCMMaterialConfig.setScmId("scm-id");
        pluggableSCMMaterialConfig.validateTree(validationContext);
        assertThat(pluggableSCMMaterialConfig.errors().getAll().size()).isEqualTo(1);
        assertThat(pluggableSCMMaterialConfig.errors().firstErrorOn(PluggableSCMMaterialConfig.SCM_ID)).isEqualTo("Could not find SCM for given scm-id: [scm-id].");
    }

    @Test
    public void shouldAddErrorWhenAssociatedSCMPluginIsMissing() {
        PipelineConfigSaveValidationContext configSaveValidationContext = mock(PipelineConfigSaveValidationContext.class);
        when(configSaveValidationContext.findScmById(anyString())).thenReturn(mock(SCM.class));
        SCM scmConfig = mock(SCM.class);
        when(scmConfig.doesPluginExist()).thenReturn(false);
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(null, scmConfig, "usr/home", null, false);
        pluggableSCMMaterialConfig.setScmId("scm-id");
        pluggableSCMMaterialConfig.validateTree(configSaveValidationContext);
        assertThat(pluggableSCMMaterialConfig.errors().getAll().size()).isEqualTo(1);
        assertThat(pluggableSCMMaterialConfig.errors().firstErrorOn(PluggableSCMMaterialConfig.SCM_ID)).isEqualTo("Could not find plugin for scm-id: [scm-id].");
    }

    @Test
    public void shouldSetConfigAttributesForSCMMaterial() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(PluggableSCMMaterialConfig.SCM_ID, "scm-id");
        attributes.put(PluggableSCMMaterialConfig.FOLDER, "dest");
        attributes.put(PluggableSCMMaterialConfig.FILTER, "/foo/**.*,/another/**.*,bar");
        attributes.put(PluggableSCMMaterialConfig.INVERT_FILTER, "true");

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig();
        pluggableSCMMaterialConfig.setConfigAttributes(attributes);

        assertThat(pluggableSCMMaterialConfig.getScmId()).isEqualTo("scm-id");
        assertThat(pluggableSCMMaterialConfig.getFolder()).isEqualTo("dest");
        assertThat(pluggableSCMMaterialConfig.filter()).isEqualTo(new Filter(new IgnoredFiles("/foo/**.*"), new IgnoredFiles("/another/**.*"), new IgnoredFiles("bar")));
        assertThat(pluggableSCMMaterialConfig.isInvertFilter()).isTrue();
    }

    @Test
    public void shouldSetConfigAttributesForSCMMaterialWhenDataIsEmpty() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(PluggableSCMMaterialConfig.SCM_ID, "scm-id");
        attributes.put(PluggableSCMMaterialConfig.FOLDER, "");
        attributes.put(PluggableSCMMaterialConfig.FILTER, "");

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig();
        pluggableSCMMaterialConfig.setFolder("dest");
        pluggableSCMMaterialConfig.setFilter(new Filter(new IgnoredFiles("/foo/**.*")));
        pluggableSCMMaterialConfig.setConfigAttributes(attributes);

        assertThat(pluggableSCMMaterialConfig.getScmId()).isEqualTo("scm-id");
        assertThat(pluggableSCMMaterialConfig.getFolder()).isNull();
        assertThat(pluggableSCMMaterialConfig.filter()).isEqualTo(new Filter());
    }

    @Test
    public void shouldGetFilterAsString() {
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig();
        pluggableSCMMaterialConfig.setFilter(new Filter(new IgnoredFiles("/foo/**.*"), new IgnoredFiles("/another/**.*"), new IgnoredFiles("bar")));
        assertThat(pluggableSCMMaterialConfig.getFilterAsString()).isEqualTo("/foo/**.*,/another/**.*,bar");

        pluggableSCMMaterialConfig.setFilter(new Filter());
        assertThat(pluggableSCMMaterialConfig.getFilterAsString()).isEqualTo("");
    }

    @Test
    public void shouldSetSCMIdToNullIfConfigAttributesForSCMMaterialDoesNotContainSCMId() {
        Map<String, String> attributes = new HashMap<>();

        pluggableSCMMaterialConfig.setConfigAttributes(attributes);

        assertThat(pluggableSCMMaterialConfig.getScmId()).isNull();
    }

    @Test
    public void shouldSetSCMIdAsNullIfSCMConfigIsNull() {
        pluggableSCMMaterialConfig.setSCMConfig(null);

        assertThat(pluggableSCMMaterialConfig.getScmId()).isNull();
        assertThat(pluggableSCMMaterialConfig.getSCMConfig()).isNull();
    }

    @Test
    public void shouldGetNameFromSCMName() {
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig();
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "plugin-id", "1.0", new Configuration(create("k1", false, "v1")));
        pluggableSCMMaterialConfig.setSCMConfig(scmConfig);
        assertThat(pluggableSCMMaterialConfig.getName().toString()).isEqualTo("scm-name");

        pluggableSCMMaterialConfig.setSCMConfig(null);
        assertThat(pluggableSCMMaterialConfig.getName()).isNull();
    }

    @Test
    public void shouldCheckEquals() {
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "plugin-id", "1.0", new Configuration(create("k1", false, "v1")));

        // same fingerprint
        PluggableSCMMaterialConfig p1 = new PluggableSCMMaterialConfig();
        p1.setSCMConfig(scmConfig);

        PluggableSCMMaterialConfig p2 = new PluggableSCMMaterialConfig();
        p2.setSCMConfig(scmConfig);
        assertThat(p1.equals(p2)).isTrue();

        // folder
        p2.setFolder("dest");
        assertThat(p1.equals(p2)).isFalse();

        // scmConfig null
        p1 = new PluggableSCMMaterialConfig();
        p2 = new PluggableSCMMaterialConfig();
        assertThat(p1.equals(p2)).isTrue();

        p2.setSCMConfig(scmConfig);
        assertThat(p1.equals(p2)).isFalse();

        p1.setSCMConfig(scmConfig);
        p2 = new PluggableSCMMaterialConfig();
        assertThat(p1.equals(p2)).isFalse();

        p2.setSCMConfig(scmConfig);
        assertThat(p1.equals(p2)).isTrue();

        // null comparison
        //noinspection ConstantValue
        assertThat(p1.equals(null)).isFalse();
    }

    @Test
    public void shouldDelegateToSCMConfigForAutoUpdate() {
        SCM scm = mock(SCM.class);
        when(scm.isAutoUpdate()).thenReturn(false);
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(new CaseInsensitiveString("scm-name"), scm, null, null, false);

        assertThat(pluggableSCMMaterialConfig.isAutoUpdate()).isFalse();

        verify(scm).isAutoUpdate();
    }

    @Test
    public void shouldCorrectlyGet_Name_DisplayName_Description_LongDescription_UriForDisplay() {
        SCM scmConfig = mock(SCM.class);
        when(scmConfig.getName()).thenReturn("scm-name");
        when(scmConfig.getConfigForDisplay()).thenReturn("k1:v1");
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(null, scmConfig, null, null, false);

        assertThat(pluggableSCMMaterialConfig.getName()).isEqualTo(new CaseInsensitiveString("scm-name"));
        assertThat(pluggableSCMMaterialConfig.getDisplayName()).isEqualTo("scm-name");
        assertThat(pluggableSCMMaterialConfig.getLongDescription()).isEqualTo("k1:v1");
        assertThat(pluggableSCMMaterialConfig.getUriForDisplay()).isEqualTo("k1:v1");

        when(scmConfig.getName()).thenReturn(null);
        pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(null, scmConfig, null, null, false);

        assertThat(pluggableSCMMaterialConfig.getName()).isNull();
        assertThat(pluggableSCMMaterialConfig.getDisplayName()).isEqualTo("k1:v1");
    }

    @Test
    public void shouldCorrectlyGetTypeDisplay() {
        assertThat(pluggableSCMMaterialConfig.getTypeForDisplay()).isEqualTo("SCM");

        pluggableSCMMaterialConfig.setSCMConfig(SCMMother.create("scm-id"));
        assertThat(pluggableSCMMaterialConfig.getTypeForDisplay()).isEqualTo("SCM");

        SCMMetadataStore.getInstance().addMetadataFor("plugin", null, null);
        assertThat(pluggableSCMMaterialConfig.getTypeForDisplay()).isEqualTo("SCM");

        SCMView scmView = mock(SCMView.class);
        when(scmView.displayValue()).thenReturn("scm-name");
        SCMMetadataStore.getInstance().addMetadataFor("plugin", null, scmView);
        assertThat(pluggableSCMMaterialConfig.getTypeForDisplay()).isEqualTo("scm-name");
    }

    @Test
    public void shouldFailValidationIfDestinationDirectoryIsNested() {
        pluggableSCMMaterialConfig.setFolder("f1");
        pluggableSCMMaterialConfig.validateNotSubdirectoryOf("f1/f2");
        assertFalse(pluggableSCMMaterialConfig.errors().isEmpty());
        assertThat(pluggableSCMMaterialConfig.errors().firstErrorOn(FOLDER)).isEqualTo("Invalid destination directory. Every material needs a different destination directory and the directories should not be nested.");
    }

    @Test
    public void shouldNotFailValidationIfDestinationDirectoryIsMultilevelButNotNested() {
        pluggableSCMMaterialConfig.setFolder("f1/f2/f3");
        pluggableSCMMaterialConfig.validateNotSubdirectoryOf("f1/f2/f");

        assertThat(pluggableSCMMaterialConfig.errors().getAllOn(FOLDER)).isEmpty();
    }

    @Test
    public void shouldFailValidationIfDestinationDirectoryIsOutsideCurrentWorkingDirectoryAfterNormalization() {
        pluggableSCMMaterialConfig.setFolder("f1/../../f3");

        pluggableSCMMaterialConfig.validateConcreteMaterial(null);
        assertThat(pluggableSCMMaterialConfig.errors().firstErrorOn(FOLDER)).isEqualTo("Dest folder 'f1/../../f3' is not valid. It must be a sub-directory of the working folder.");
    }

    @Test
    public void shouldFailValidationIfDestinationDirectoryIsNestedAfterNormalization() {
        pluggableSCMMaterialConfig.setFolder("f1/f2/../../f3");
        pluggableSCMMaterialConfig.validateNotSubdirectoryOf("f3/f4");
        assertThat(pluggableSCMMaterialConfig.errors().firstErrorOn(FOLDER)).isEqualTo("Invalid destination directory. Every material needs a different destination directory and the directories should not be nested.");
    }

    @Test
    public void shouldNotValidateNestingOfMaterialDirectoriesBasedOnServerSideFileSystem() throws IOException {
        final File workingDir = TempDirUtils.createTempDirectoryIn(temporaryFolder, "go-working-dir").toFile();
        final File material1 = new File(workingDir, "material1");
        material1.mkdirs();

        final Path material2 = Files.createSymbolicLink(Paths.get(new File(workingDir, "material2").getPath()), Paths.get(material1.getPath()));

        pluggableSCMMaterialConfig.setFolder(material1.getAbsolutePath());
        pluggableSCMMaterialConfig.validateNotSubdirectoryOf(material2.toAbsolutePath().toString());

        assertThat(pluggableSCMMaterialConfig.errors().getAllOn(FOLDER)).isEmpty();
    }
}
