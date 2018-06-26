/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.packagerepository;


import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.RepositoryMetadataStoreHelper;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.plugin.api.config.Property.PART_OF_IDENTITY;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class PackageRepositoriesTest {

    @Test
    public void shouldCheckEqualityOfPackageRepositories() {
        PackageRepository packageRepository = new PackageRepository();
        PackageRepositories packageRepositories = new PackageRepositories(packageRepository);
        assertThat(packageRepositories, is(new PackageRepositories(packageRepository)));
    }

    @Test
    public void shouldFindRepositoryGivenTheRepoId() throws Exception {
        PackageRepository packageRepository1 = PackageRepositoryMother.create("repo-id1", "repo1", "plugin-id", "1.0", null);
        PackageRepository packageRepository2 = PackageRepositoryMother.create("repo-id2", "repo2", "plugin-id", "1.0", null);

        PackageRepositories packageRepositories = new PackageRepositories(packageRepository1, packageRepository2);
        assertThat(packageRepositories.find("repo-id2"), is(packageRepository2));
    }

    @Test
    public void shouldReturnNullIfNoMatchingRepoFound() throws Exception {
        PackageRepositories packageRepositories = new PackageRepositories();
        assertThat(packageRepositories.find("not-found"), is(nullValue()));
    }

    @Test
    public void shouldGetPackageRepositoryForGivenPackageId() throws Exception {
        PackageRepository repo1 = PackageRepositoryMother.create("repo-id1", "repo1", "plugin-id", "1.0", null);
        PackageDefinition packageDefinitionOne = PackageDefinitionMother.create("pid1", repo1);
        PackageDefinition packageDefinitionTwo = PackageDefinitionMother.create("pid2", repo1);
        repo1.getPackages().addAll(asList(packageDefinitionOne, packageDefinitionTwo));

        PackageRepository repo2 = PackageRepositoryMother.create("repo-id2", "repo2", "plugin-id", "1.0", null);
        PackageDefinition packageDefinitionThree = PackageDefinitionMother.create("pid3", repo2);
        PackageDefinition packageDefinitionFour = PackageDefinitionMother.create("pid4", repo2);
        repo2.getPackages().addAll(asList(packageDefinitionThree, packageDefinitionFour));


        PackageRepositories packageRepositories = new PackageRepositories(repo1, repo2);

        assertThat(packageRepositories.findPackageRepositoryHaving("pid3"), is(repo2));
        assertThat(packageRepositories.findPackageRepositoryWithPackageIdOrBomb("pid3"), is(repo2));
    }

    @Test
    public void shouldReturnNullWhenRepositoryForGivenPackageNotFound() throws Exception {
        PackageRepositories packageRepositories = new PackageRepositories();
        assertThat(packageRepositories.findPackageRepositoryHaving("invalid"), is(nullValue()));
    }

    @Test
    public void shouldThrowExceptionWhenRepositoryForGivenPackageNotFound() throws Exception {
        PackageRepositories packageRepositories = new PackageRepositories();

        try {
            packageRepositories.findPackageRepositoryWithPackageIdOrBomb("invalid");
            fail("should have thrown exception for not finding package repository");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Could not find repository for given package id:[invalid]"));
        }
    }

    @Test
    public void shouldFindPackageRepositoryById() throws Exception {
        PackageRepositories packageRepositories = new PackageRepositories();
        packageRepositories.add(PackageRepositoryMother.create("repo1"));
        PackageRepository repo2 = PackageRepositoryMother.create("repo2");
        packageRepositories.add(repo2);
        packageRepositories.removePackageRepository("repo1");

        assertThat(packageRepositories, Matchers.contains(repo2));
    }

    @Test
    public void shouldReturnNullExceptionWhenRepoIdIsNotFound() throws Exception {
        PackageRepositories packageRepositories = new PackageRepositories();
        try {
            packageRepositories.removePackageRepository("repo1");
            fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), is(String.format("Could not find repository with id '%s'", "repo1")));
        }
    }

    @Test
    public void shouldValidateForCaseInsensitiveNameAndIdUniqueness() {
        PackageRepository repo1 = PackageRepositoryMother.create("repo1");
        PackageRepository duplicate = PackageRepositoryMother.create("REPO1");
        PackageRepository unique = PackageRepositoryMother.create("unique");
        PackageRepositories packageRepositories = new PackageRepositories();
        packageRepositories.add(repo1);
        packageRepositories.add(duplicate);
        packageRepositories.add(unique);

        packageRepositories.validate(null);
        assertThat(repo1.errors().isEmpty(), is(false));
        String nameError = String.format("You have defined multiple repositories called '%s'. Repository names are case-insensitive and must be unique.", duplicate.getName());
        assertThat(repo1.errors().getAllOn(PackageRepository.NAME).contains(nameError), is(true));
        assertThat(duplicate.errors().isEmpty(), is(false));
        assertThat(duplicate.errors().getAllOn(PackageRepository.NAME).contains(nameError), is(true));
        assertThat(unique.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldFailValidationIfMaterialWithDuplicateFingerprintIsFound() {

        com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration = new com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration();
        packageConfiguration.add(new PackageMaterialProperty("k1"));
        packageConfiguration.add(new PackageMaterialProperty("k2").with(PART_OF_IDENTITY, false));

        PackageMetadataStore.getInstance().addMetadataFor("plugin", new PackageConfigurations(packageConfiguration));

        String expectedErrorMessage = "Cannot save package or repo, found duplicate packages. [Repo Name: 'repo-repo1', Package Name: 'pkg1'], [Repo Name: 'repo-repo1', Package Name: 'pkg3'], [Repo Name: 'repo-repo1', Package Name: 'pkg5']";
        PackageRepository repository = PackageRepositoryMother.create("repo1");
        PackageDefinition definition1 = PackageDefinitionMother.create("1", "pkg1", new Configuration(new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("v1"))), repository);
        PackageDefinition definition2 = PackageDefinitionMother.create("2", "pkg2", new Configuration(new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("v2"))), repository);
        PackageDefinition definition3 = PackageDefinitionMother.create("3", "pkg3", new Configuration(new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("v1"))), repository);
        PackageDefinition definition4 = PackageDefinitionMother.create("4", "pkg4", new Configuration(new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("V1"))), repository);
        PackageDefinition definition5 = PackageDefinitionMother.create("5", "pkg5", new Configuration(new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("v1")), new ConfigurationProperty(new ConfigurationKey("k2"), new ConfigurationValue("v2"))), repository);

        repository.setPackages(new Packages(definition1, definition2, definition3, definition4, definition5));

        PackageRepositories packageRepositories = new PackageRepositories(repository);

        packageRepositories.validate(null);

        assertThat(definition1.errors().getAllOn(PackageDefinition.ID), is(asList(expectedErrorMessage)));
        assertThat(definition3.errors().getAllOn(PackageDefinition.ID), is(asList(expectedErrorMessage)));
        assertThat(definition3.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER).equals(definition1.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER)), is(true));
        assertThat(definition5.errors().getAllOn(PackageDefinition.ID), is(asList(expectedErrorMessage)));
        assertThat(definition5.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER).equals(definition1.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER)), is(true));

        assertThat(definition2.errors().getAllOn(PackageDefinition.ID), is(nullValue()));
        assertThat(definition2.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER).equals(definition1.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER)), is(false));
        assertThat(definition4.errors().getAllOn(PackageDefinition.ID), is(nullValue()));
        assertThat(definition4.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER).equals(definition1.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER)), is(false));
    }

    @Test
    public void shouldGetPackageDefinitionForGivenPackageId() throws Exception {
        PackageRepository repo1 = PackageRepositoryMother.create("repo-id1", "repo1", "plugin-id", "1.0", null);
        PackageDefinition packageDefinitionOne = PackageDefinitionMother.create("pid1", repo1);
        PackageDefinition packageDefinitionTwo = PackageDefinitionMother.create("pid2", repo1);
        repo1.getPackages().addAll(asList(packageDefinitionOne, packageDefinitionTwo));

        PackageRepository repo2 = PackageRepositoryMother.create("repo-id2", "repo2", "plugin-id", "1.0", null);
        PackageDefinition packageDefinitionThree = PackageDefinitionMother.create("pid3", repo2);
        PackageDefinition packageDefinitionFour = PackageDefinitionMother.create("pid4", repo2);
        repo2.getPackages().addAll(asList(packageDefinitionThree, packageDefinitionFour));


        PackageRepositories packageRepositories = new PackageRepositories(repo1, repo2);
        assertThat(packageRepositories.findPackageDefinitionWith("pid3"), is(packageDefinitionThree));
        assertThat(packageRepositories.findPackageDefinitionWith("pid5"), is(nullValue()));
    }

    @Before
    public void setup() throws Exception {
        RepositoryMetadataStoreHelper.clear();
    }

    @After
    public void tearDown() throws Exception {
        RepositoryMetadataStoreHelper.clear();
    }
}
