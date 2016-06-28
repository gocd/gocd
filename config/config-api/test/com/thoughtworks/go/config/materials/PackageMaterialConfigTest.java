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

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.PipelineConfigSaveValidationContext;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageDefinitionMother;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.*;

public class PackageMaterialConfigTest {
    @Test
    public void shouldAddErrorIfMaterialDoesNotHaveAPackageId() throws Exception {
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig();
        packageMaterialConfig.validateConcreteMaterial(new ConfigSaveValidationContext(null, null));

        assertThat(packageMaterialConfig.errors().getAll().size(), is(1));
        assertThat(packageMaterialConfig.errors().on(PackageMaterialConfig.PACKAGE_ID), is("Please select a repository and package"));
    }

    @Test
    public void shouldAddErrorIfPackageDoesNotExistsForGivenPackageId() throws Exception {
        PipelineConfigSaveValidationContext configSaveValidationContext = mock(PipelineConfigSaveValidationContext.class);
        when(configSaveValidationContext.findPackageById(anyString())).thenReturn(mock(PackageRepository.class));
        PackageRepository packageRepository = mock(PackageRepository.class);
        when(packageRepository.doesPluginExist()).thenReturn(true);
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig(new CaseInsensitiveString("package-name"), "package-id", PackageDefinitionMother.create("package-id"));
        packageMaterialConfig.getPackageDefinition().setRepository(packageRepository);

        packageMaterialConfig.validateTree(configSaveValidationContext);

        assertThat(packageMaterialConfig.errors().getAll().size(), is(1));
        assertThat(packageMaterialConfig.errors().on(PackageMaterialConfig.PACKAGE_ID), is("Could not find plugin for given package id:[package-id]."));
    }

    @Test
    public void shouldAddErrorIfPackagePluginDoesNotExistsForGivenPackageId() throws Exception {
        PipelineConfigSaveValidationContext configSaveValidationContext = mock(PipelineConfigSaveValidationContext.class);
        when(configSaveValidationContext.findPackageById(anyString())).thenReturn(mock(PackageRepository.class));
        PackageRepository packageRepository = mock(PackageRepository.class);
        when(packageRepository.doesPluginExist()).thenReturn(false);
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig(new CaseInsensitiveString("package-name"), "package-id", PackageDefinitionMother.create("package-id"));
        packageMaterialConfig.getPackageDefinition().setRepository(packageRepository);

        packageMaterialConfig.validateTree(configSaveValidationContext);

        assertThat(packageMaterialConfig.errors().getAll().size(), is(1));
        assertThat(packageMaterialConfig.errors().on(PackageMaterialConfig.PACKAGE_ID), is("Could not find plugin for given package id:[package-id]."));
    }

    @Test
    public void shouldAddErrorIfMaterialNameUniquenessValidationFails() throws Exception {
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig("package-id");

        Map<CaseInsensitiveString, AbstractMaterialConfig> nameToMaterialMap = new HashMap<CaseInsensitiveString, AbstractMaterialConfig>();
        PackageMaterialConfig existingMaterial = new PackageMaterialConfig("package-id");
        nameToMaterialMap.put(new CaseInsensitiveString("package-id"), existingMaterial);
        nameToMaterialMap.put(new CaseInsensitiveString("foo"), new GitMaterialConfig("url"));

        packageMaterialConfig.validateNameUniqueness(nameToMaterialMap);

        assertThat(packageMaterialConfig.errors().getAll().size(), is(1));
        assertThat(packageMaterialConfig.errors().on(PackageMaterialConfig.PACKAGE_ID), is("Duplicate package material detected!"));
        assertThat(existingMaterial.errors().getAll().size(), is(1));
        assertThat(existingMaterial.errors().on(PackageMaterialConfig.PACKAGE_ID), is("Duplicate package material detected!"));
        assertThat(nameToMaterialMap.size(), is(2));
    }

    @Test
    public void shouldPassMaterialUniquenessIfIfNoDuplicateMaterialFound() throws Exception {
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig("package-id");

        Map<CaseInsensitiveString, AbstractMaterialConfig> nameToMaterialMap = new HashMap<CaseInsensitiveString, AbstractMaterialConfig>();
        nameToMaterialMap.put(new CaseInsensitiveString("repo-name:pkg-name"), new PackageMaterialConfig("package-id-new"));
        nameToMaterialMap.put(new CaseInsensitiveString("foo"), new GitMaterialConfig("url"));

        packageMaterialConfig.validateNameUniqueness(nameToMaterialMap);

        assertThat(packageMaterialConfig.errors().getAll().size(), is(0));
        assertThat(nameToMaterialMap.size(), is(3));
    }

    @Test
    public void shouldNotAddErrorDuringUniquenessValidationIfMaterialNameIsEmpty() throws Exception {
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig("");

        Map<CaseInsensitiveString, AbstractMaterialConfig> nameToMaterialMap = new HashMap<CaseInsensitiveString, AbstractMaterialConfig>();

        packageMaterialConfig.validateNameUniqueness(nameToMaterialMap);

        assertThat(packageMaterialConfig.errors().getAll().size(), is(0));
        assertThat(nameToMaterialMap.size(), is(0));
    }

    @Test
    public void shouldSetConfigAttributesForThePackageMaterial() throws Exception {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put(PackageMaterialConfig.PACKAGE_ID, "packageId");

        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig();
        packageMaterialConfig.setConfigAttributes(attributes);
        assertThat(packageMaterialConfig.getPackageId(), is("packageId"));
    }

    @Test
    public void shouldSetPackageIdToNullIfConfigAttributesForThePackageMaterialDoesNotContainPackageId() throws Exception {
        Map<String, String> attributes = new HashMap<String, String>();
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig("id");
        packageMaterialConfig.setConfigAttributes(attributes);
        assertThat(packageMaterialConfig.getPackageId(), is(nullValue()));
    }

    @Test
    public void shouldSetPackageIdAsNullIfPackageDefinitionIsNull(){
        PackageMaterialConfig materialConfig = new PackageMaterialConfig("1");
        materialConfig.setPackageDefinition(null);
        assertThat(materialConfig.getPackageId(), is(nullValue()));
        assertThat(materialConfig.getPackageDefinition(), is(nullValue()));
    }

    @Test
    public void shouldGetNameFromRepoNameAndPackageName() {
        PackageMaterialConfig materialConfig = new PackageMaterialConfig();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        materialConfig.setPackageDefinition(PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        assertThat(materialConfig.getName().toString(), is("repo-name:package-name"));
        materialConfig.setPackageDefinition(null);
        assertThat(materialConfig.getName(), is(nullValue()));
    }

    @Test
    public void shouldCheckEquals() throws Exception {
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        PackageDefinition packageDefinition = PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository);

        PackageMaterialConfig p1 = new PackageMaterialConfig();
        p1.setPackageDefinition(packageDefinition);

        PackageMaterialConfig p2 = new PackageMaterialConfig();
        p2.setPackageDefinition(packageDefinition);
        assertThat(p1.equals(p2), is(true));


        p1 = new PackageMaterialConfig();
        p2 = new PackageMaterialConfig();
        assertThat(p1.equals(p2), is(true));

        p2.setPackageDefinition(packageDefinition);
        assertThat(p1.equals(p2), is(false));

        p1.setPackageDefinition(packageDefinition);
        p2 = new PackageMaterialConfig();
        assertThat(p1.equals(p2), is(false));

        assertThat(p1.equals(null), is(false));
    }

    @Test
    public void shouldDelegateToPackageDefinitionForAutoUpdate() throws Exception {
        PackageDefinition packageDefinition = mock(PackageDefinition.class);
        when(packageDefinition.isAutoUpdate()).thenReturn(false);
        PackageMaterialConfig materialConfig = new PackageMaterialConfig(new CaseInsensitiveString("name"), "package-id", packageDefinition);

        assertThat(materialConfig.isAutoUpdate(), is(false));

        verify(packageDefinition).isAutoUpdate();
    }
}
