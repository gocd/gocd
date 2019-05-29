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
package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.plugin.access.scm.SCMView;
import org.junit.*;
import org.junit.rules.TemporaryFolder;

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
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PluggableSCMMaterialConfigTest {
    private PluggableSCMMaterialConfig pluggableSCMMaterialConfig;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws IOException {
        pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig("scm-id");
        SCMMetadataStore.getInstance().clear();
    }

    @After
    public void tearDown() {
        SCMMetadataStore.getInstance().clear();
    }

    @Test
    public void shouldAddErrorIfMaterialDoesNotHaveASCMId() throws Exception {
        pluggableSCMMaterialConfig.setScmId(null);
        pluggableSCMMaterialConfig.validateConcreteMaterial(new ConfigSaveValidationContext(null, null));

        assertThat(pluggableSCMMaterialConfig.errors().getAll().size(), is(1));
        assertThat(pluggableSCMMaterialConfig.errors().on(PluggableSCMMaterialConfig.SCM_ID), is("Please select a SCM"));
    }

    @Test
    public void shouldAddErrorIfSCMNameUniquenessValidationFails() throws Exception {
        Map<CaseInsensitiveString, AbstractMaterialConfig> nameToMaterialMap = new HashMap<>();
        PluggableSCMMaterialConfig existingMaterial = new PluggableSCMMaterialConfig("scm-id");
        nameToMaterialMap.put(new CaseInsensitiveString("scm-id"), existingMaterial);
        nameToMaterialMap.put(new CaseInsensitiveString("foo"), git("url"));

        pluggableSCMMaterialConfig.validateNameUniqueness(nameToMaterialMap);

        assertThat(pluggableSCMMaterialConfig.errors().getAll().size(), is(1));
        assertThat(pluggableSCMMaterialConfig.errors().on(PluggableSCMMaterialConfig.SCM_ID), is("Duplicate SCM material detected!"));
        assertThat(existingMaterial.errors().getAll().size(), is(1));
        assertThat(existingMaterial.errors().on(PluggableSCMMaterialConfig.SCM_ID), is("Duplicate SCM material detected!"));
        assertThat(nameToMaterialMap.size(), is(2));
    }

    @Test
    public void shouldPassMaterialUniquenessIfIfNoDuplicateSCMFound() throws Exception {
        Map<CaseInsensitiveString, AbstractMaterialConfig> nameToMaterialMap = new HashMap<>();
        nameToMaterialMap.put(new CaseInsensitiveString("scm-id-new"), new PluggableSCMMaterialConfig("scm-id-new"));
        nameToMaterialMap.put(new CaseInsensitiveString("foo"), git("url"));

        pluggableSCMMaterialConfig.validateNameUniqueness(nameToMaterialMap);

        assertThat(pluggableSCMMaterialConfig.errors().getAll().size(), is(0));
        assertThat(nameToMaterialMap.size(), is(3));
    }

    @Test
    public void shouldNotAddErrorDuringUniquenessValidationIfSCMNameIsEmpty() throws Exception {
        pluggableSCMMaterialConfig.setScmId("");

        Map<CaseInsensitiveString, AbstractMaterialConfig> nameToMaterialMap = new HashMap<>();

        pluggableSCMMaterialConfig.validateNameUniqueness(nameToMaterialMap);

        assertThat(pluggableSCMMaterialConfig.errors().getAll().size(), is(0));
        assertThat(nameToMaterialMap.size(), is(0));
    }

    @Test
    public void shouldAddErrorIDestinationIsNotValid() throws Exception {
        ConfigSaveValidationContext configSaveValidationContext = mock(ConfigSaveValidationContext.class);
        SCM scmConfig = mock(SCM.class);
        when(configSaveValidationContext.findScmById(anyString())).thenReturn(scmConfig);
        when(scmConfig.doesPluginExist()).thenReturn(true);
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(null, scmConfig, "/usr/home", null);
        pluggableSCMMaterialConfig.setScmId("scm-id");
        pluggableSCMMaterialConfig.validateConcreteMaterial(configSaveValidationContext);

        assertThat(pluggableSCMMaterialConfig.errors().getAll().size(), is(1));
        assertThat(pluggableSCMMaterialConfig.errors().on(PluggableSCMMaterialConfig.FOLDER), is("Dest folder '/usr/home' is not valid. It must be a sub-directory of the working folder."));

        pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(null, scmConfig, "./../crap", null);
        pluggableSCMMaterialConfig.setScmId("scm-id");
        pluggableSCMMaterialConfig.validateConcreteMaterial(configSaveValidationContext);

        assertThat(pluggableSCMMaterialConfig.errors().getAll().size(), is(2));
        assertThat(pluggableSCMMaterialConfig.errors().on(PluggableSCMMaterialConfig.FOLDER), is("Invalid directory name './../crap'. It should be a valid relative path."));
    }


    @Test
    public void shouldAddErrorWhenMatchingScmConfigDoesNotExist() throws Exception {
        PipelineConfigSaveValidationContext validationContext = mock(PipelineConfigSaveValidationContext.class);
        when(validationContext.findScmById(anyString())).thenReturn(null);
        SCM scmConfig = mock(SCM.class);
        when(scmConfig.doesPluginExist()).thenReturn(true);
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(null, scmConfig, "usr/home", null);
        pluggableSCMMaterialConfig.setScmId("scm-id");
        pluggableSCMMaterialConfig.validateTree(validationContext);
        assertThat(pluggableSCMMaterialConfig.errors().getAll().size(), is(1));
        assertThat(pluggableSCMMaterialConfig.errors().on(PluggableSCMMaterialConfig.SCM_ID), is("Could not find SCM for given scm-id: [scm-id]."));
    }

    @Test
    public void shouldAddErrorWhenAssociatedSCMPluginIsMissing() throws Exception {
        PipelineConfigSaveValidationContext configSaveValidationContext = mock(PipelineConfigSaveValidationContext.class);
        when(configSaveValidationContext.findScmById(anyString())).thenReturn(mock(SCM.class));
        SCM scmConfig = mock(SCM.class);
        when(scmConfig.doesPluginExist()).thenReturn(false);
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(null, scmConfig, "usr/home", null);
        pluggableSCMMaterialConfig.setScmId("scm-id");
        pluggableSCMMaterialConfig.validateTree(configSaveValidationContext);
        assertThat(pluggableSCMMaterialConfig.errors().getAll().size(), is(1));
        assertThat(pluggableSCMMaterialConfig.errors().on(PluggableSCMMaterialConfig.SCM_ID), is("Could not find plugin for scm-id: [scm-id]."));
    }

    @Test
    public void shouldSetConfigAttributesForSCMMaterial() throws Exception {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(PluggableSCMMaterialConfig.SCM_ID, "scm-id");
        attributes.put(PluggableSCMMaterialConfig.FOLDER, "dest");
        attributes.put(PluggableSCMMaterialConfig.FILTER, "/foo/**.*,/another/**.*,bar");

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig();
        pluggableSCMMaterialConfig.setConfigAttributes(attributes);

        assertThat(pluggableSCMMaterialConfig.getScmId(), is("scm-id"));
        assertThat(pluggableSCMMaterialConfig.getFolder(), is("dest"));
        assertThat(pluggableSCMMaterialConfig.filter(), is(new Filter(new IgnoredFiles("/foo/**.*"), new IgnoredFiles("/another/**.*"), new IgnoredFiles("bar"))));
    }

    @Test
    public void shouldSetConfigAttributesForSCMMaterialWhenDataIsEmpty() throws Exception {
        Map<String, String> attributes = new HashMap<>();
        attributes.put(PluggableSCMMaterialConfig.SCM_ID, "scm-id");
        attributes.put(PluggableSCMMaterialConfig.FOLDER, "");
        attributes.put(PluggableSCMMaterialConfig.FILTER, "");

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig();
        pluggableSCMMaterialConfig.setFolder("dest");
        pluggableSCMMaterialConfig.setFilter(new Filter(new IgnoredFiles("/foo/**.*")));
        pluggableSCMMaterialConfig.setConfigAttributes(attributes);

        assertThat(pluggableSCMMaterialConfig.getScmId(), is("scm-id"));
        assertThat(pluggableSCMMaterialConfig.getFolder(), is(nullValue()));
        assertThat(pluggableSCMMaterialConfig.filter(), is(new Filter()));
    }

    @Test
    public void shouldGetFilterAsString() {
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig();
        pluggableSCMMaterialConfig.setFilter(new Filter(new IgnoredFiles("/foo/**.*"), new IgnoredFiles("/another/**.*"), new IgnoredFiles("bar")));
        assertThat(pluggableSCMMaterialConfig.getFilterAsString(), is("/foo/**.*,/another/**.*,bar"));

        pluggableSCMMaterialConfig.setFilter(new Filter());
        assertThat(pluggableSCMMaterialConfig.getFilterAsString(), is(""));
    }

    @Test
    public void shouldSetSCMIdToNullIfConfigAttributesForSCMMaterialDoesNotContainSCMId() throws Exception {
        Map<String, String> attributes = new HashMap<>();

        pluggableSCMMaterialConfig.setConfigAttributes(attributes);

        assertThat(pluggableSCMMaterialConfig.getScmId(), is(nullValue()));
    }

    @Test
    public void shouldSetSCMIdAsNullIfSCMConfigIsNull() {
        pluggableSCMMaterialConfig.setSCMConfig(null);

        assertThat(pluggableSCMMaterialConfig.getScmId(), is(nullValue()));
        assertThat(pluggableSCMMaterialConfig.getSCMConfig(), is(nullValue()));
    }

    @Test
    public void shouldGetNameFromSCMName() {
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig();
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "plugin-id", "1.0", new Configuration(create("k1", false, "v1")));
        pluggableSCMMaterialConfig.setSCMConfig(scmConfig);
        assertThat(pluggableSCMMaterialConfig.getName().toString(), is("scm-name"));

        pluggableSCMMaterialConfig.setSCMConfig(null);
        assertThat(pluggableSCMMaterialConfig.getName(), is(nullValue()));
    }

    @Test
    public void shouldCheckEquals() throws Exception {
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "plugin-id", "1.0", new Configuration(create("k1", false, "v1")));

        // same fingerprint
        PluggableSCMMaterialConfig p1 = new PluggableSCMMaterialConfig();
        p1.setSCMConfig(scmConfig);

        PluggableSCMMaterialConfig p2 = new PluggableSCMMaterialConfig();
        p2.setSCMConfig(scmConfig);
        assertThat(p1.equals(p2), is(true));

        // folder
        p2.setFolder("dest");
        assertThat(p1.equals(p2), is(false));

        // scmConfig null
        p1 = new PluggableSCMMaterialConfig();
        p2 = new PluggableSCMMaterialConfig();
        assertThat(p1.equals(p2), is(true));

        p2.setSCMConfig(scmConfig);
        assertThat(p1.equals(p2), is(false));

        p1.setSCMConfig(scmConfig);
        p2 = new PluggableSCMMaterialConfig();
        assertThat(p1.equals(p2), is(false));

        p2.setSCMConfig(scmConfig);
        assertThat(p1.equals(p2), is(true));

        // null comparison
        assertThat(p1.equals(null), is(false));
    }

    @Test
    public void shouldDelegateToSCMConfigForAutoUpdate() throws Exception {
        SCM scm = mock(SCM.class);
        when(scm.isAutoUpdate()).thenReturn(false);
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(new CaseInsensitiveString("scm-name"), scm, null, null);

        assertThat(pluggableSCMMaterialConfig.isAutoUpdate(), is(false));

        verify(scm).isAutoUpdate();
    }

    @Test
    public void shouldCorrectlyGet_Name_DisplayName_Description_LongDescription_UriForDisplay() {
        SCM scmConfig = mock(SCM.class);
        when(scmConfig.getName()).thenReturn("scm-name");
        when(scmConfig.getConfigForDisplay()).thenReturn("k1:v1");
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(null, scmConfig, null, null);

        assertThat(pluggableSCMMaterialConfig.getName(), is(new CaseInsensitiveString("scm-name")));
        assertThat(pluggableSCMMaterialConfig.getDisplayName(), is("scm-name"));
        assertThat(pluggableSCMMaterialConfig.getLongDescription(), is("k1:v1"));
        assertThat(pluggableSCMMaterialConfig.getUriForDisplay(), is("k1:v1"));

        when(scmConfig.getName()).thenReturn(null);
        pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(null, scmConfig, null, null);

        assertThat(pluggableSCMMaterialConfig.getName(), is(nullValue()));
        assertThat(pluggableSCMMaterialConfig.getDisplayName(), is("k1:v1"));
    }

    @Test
    public void shouldCorrectlyGetTypeDisplay() {
        assertThat(pluggableSCMMaterialConfig.getTypeForDisplay(), is("SCM"));

        pluggableSCMMaterialConfig.setSCMConfig(SCMMother.create("scm-id"));
        assertThat(pluggableSCMMaterialConfig.getTypeForDisplay(), is("SCM"));

        SCMMetadataStore.getInstance().addMetadataFor("plugin", null, null);
        assertThat(pluggableSCMMaterialConfig.getTypeForDisplay(), is("SCM"));

        SCMView scmView = mock(SCMView.class);
        when(scmView.displayValue()).thenReturn("scm-name");
        SCMMetadataStore.getInstance().addMetadataFor("plugin", null, scmView);
        assertThat(pluggableSCMMaterialConfig.getTypeForDisplay(), is("scm-name"));
    }

    @Test
    public void shouldFailValidationIfDestinationDirectoryIsNested() {
        pluggableSCMMaterialConfig.setFolder("f1");
        pluggableSCMMaterialConfig.validateNotSubdirectoryOf("f1/f2");
        assertFalse(pluggableSCMMaterialConfig.errors().isEmpty());
        Assert.assertThat(pluggableSCMMaterialConfig.errors().on(FOLDER), is("Invalid Destination Directory. Every material needs a different destination directory and the directories should not be nested."));
    }

    @Test
    public void shouldNotFailValidationIfDestinationDirectoryIsMultilevelButNotNested() {
        pluggableSCMMaterialConfig.setFolder("f1/f2/f3");
        pluggableSCMMaterialConfig.validateNotSubdirectoryOf("f1/f2/f");

        assertNull(pluggableSCMMaterialConfig.errors().getAllOn(FOLDER));
    }

    @Test
    public void shouldFailValidationIfDestinationDirectoryIsOutsideCurrentWorkingDirectoryAfterNormalization() {
        pluggableSCMMaterialConfig.setFolder("f1/../../f3");

        pluggableSCMMaterialConfig.validateConcreteMaterial(null);
        Assert.assertThat(pluggableSCMMaterialConfig.errors().on(FOLDER), is("Dest folder 'f1/../../f3' is not valid. It must be a sub-directory of the working folder."));
    }

    @Test
    public void shouldFailValidationIfDestinationDirectoryIsNestedAfterNormalization() {
        pluggableSCMMaterialConfig.setFolder("f1/f2/../../f3");
        pluggableSCMMaterialConfig.validateNotSubdirectoryOf("f3/f4");
        Assert.assertThat(pluggableSCMMaterialConfig.errors().on(FOLDER), is("Invalid Destination Directory. Every material needs a different destination directory and the directories should not be nested."));
    }

    @Test
    public void shouldNotValidateNestingOfMaterialDirectoriesBasedOnServerSideFileSystem() throws IOException {
        final File workingDir = temporaryFolder.newFolder("go-working-dir");
        final File material1 = new File(workingDir, "material1");
        material1.mkdirs();

        final Path material2 = Files.createSymbolicLink(Paths.get(new File(workingDir, "material2").getPath()), Paths.get(material1.getPath()));

        pluggableSCMMaterialConfig.setFolder(material1.getAbsolutePath());
        pluggableSCMMaterialConfig.validateNotSubdirectoryOf(material2.toAbsolutePath().toString());

        assertNull(pluggableSCMMaterialConfig.errors().getAllOn(FOLDER));
    }
}
