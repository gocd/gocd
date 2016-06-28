/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.plugin.access.scm.SCMView;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.*;

public class PluggableSCMMaterialConfigTest {
    @Before
    public void setUp() {
        SCMMetadataStore.getInstance().clear();
    }

    @After
    public void tearDown() {
        SCMMetadataStore.getInstance().clear();
    }

    @Test
    public void shouldAddErrorIfMaterialDoesNotHaveASCMId() throws Exception {
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig();
        pluggableSCMMaterialConfig.validateConcreteMaterial(new ConfigSaveValidationContext(null, null));

        assertThat(pluggableSCMMaterialConfig.errors().getAll().size(), is(1));
        assertThat(pluggableSCMMaterialConfig.errors().on(PluggableSCMMaterialConfig.SCM_ID), is("Please select a SCM"));
    }

    @Test
    public void shouldAddErrorIfSCMNameUniquenessValidationFails() throws Exception {
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig("scm-id");

        Map<CaseInsensitiveString, AbstractMaterialConfig> nameToMaterialMap = new HashMap<CaseInsensitiveString, AbstractMaterialConfig>();
        PluggableSCMMaterialConfig existingMaterial = new PluggableSCMMaterialConfig("scm-id");
        nameToMaterialMap.put(new CaseInsensitiveString("scm-id"), existingMaterial);
        nameToMaterialMap.put(new CaseInsensitiveString("foo"), new GitMaterialConfig("url"));

        pluggableSCMMaterialConfig.validateNameUniqueness(nameToMaterialMap);

        assertThat(pluggableSCMMaterialConfig.errors().getAll().size(), is(1));
        assertThat(pluggableSCMMaterialConfig.errors().on(PluggableSCMMaterialConfig.SCM_ID), is("Duplicate SCM material detected!"));
        assertThat(existingMaterial.errors().getAll().size(), is(1));
        assertThat(existingMaterial.errors().on(PluggableSCMMaterialConfig.SCM_ID), is("Duplicate SCM material detected!"));
        assertThat(nameToMaterialMap.size(), is(2));
    }

    @Test
    public void shouldPassMaterialUniquenessIfIfNoDuplicateSCMFound() throws Exception {
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig("scm-id");

        Map<CaseInsensitiveString, AbstractMaterialConfig> nameToMaterialMap = new HashMap<CaseInsensitiveString, AbstractMaterialConfig>();
        nameToMaterialMap.put(new CaseInsensitiveString("scm-id-new"), new PluggableSCMMaterialConfig("scm-id-new"));
        nameToMaterialMap.put(new CaseInsensitiveString("foo"), new GitMaterialConfig("url"));

        pluggableSCMMaterialConfig.validateNameUniqueness(nameToMaterialMap);

        assertThat(pluggableSCMMaterialConfig.errors().getAll().size(), is(0));
        assertThat(nameToMaterialMap.size(), is(3));
    }

    @Test
    public void shouldNotAddErrorDuringUniquenessValidationIfSCMNameIsEmpty() throws Exception {
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig("");

        Map<CaseInsensitiveString, AbstractMaterialConfig> nameToMaterialMap = new HashMap<CaseInsensitiveString, AbstractMaterialConfig>();

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
        Map<String, String> attributes = new HashMap<String, String>();
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
        Map<String, String> attributes = new HashMap<String, String>();
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
        Map<String, String> attributes = new HashMap<String, String>();
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig("scm-id");

        pluggableSCMMaterialConfig.setConfigAttributes(attributes);

        assertThat(pluggableSCMMaterialConfig.getScmId(), is(nullValue()));
    }

    @Test
    public void shouldSetSCMIdAsNullIfSCMConfigIsNull() {
        PluggableSCMMaterialConfig materialConfig = new PluggableSCMMaterialConfig("1");

        materialConfig.setSCMConfig(null);

        assertThat(materialConfig.getScmId(), is(nullValue()));
        assertThat(materialConfig.getSCMConfig(), is(nullValue()));
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
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig("scm-id");
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
}
