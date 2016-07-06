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

package com.thoughtworks.go.domain.packagerepository;

import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.config.BasicCruiseConfig;
import com.thoughtworks.go.config.ConfigSaveValidationContext;
import com.thoughtworks.go.config.helper.ConfigurationHolder;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;
import com.thoughtworks.go.domain.config.RepositoryMetadataStoreHelper;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration.PART_OF_IDENTITY;
import static com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration.SECURE;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;

public class PackageRepositoryTest extends PackageMaterialTestBase {

    @Before
    public void setUp() throws Exception {
        RepositoryMetadataStoreHelper.clear();
    }

    @Test
    public void shouldCheckEqualityOfPackageRepository() {
        Configuration configuration = new Configuration();
        Packages packages = new Packages(new PackageDefinition());
        PackageRepository packageRepository = createPackageRepository("plugin-id", "version", "id", "name", configuration, packages);
        assertThat(packageRepository, is(createPackageRepository("plugin-id", "version", "id", "name", configuration, packages)));
    }

    @Test
    public void shouldCheckForFieldAssignments() {
        Configuration configuration = new Configuration();
        Packages packages = new Packages(new PackageDefinition());
        PackageRepository packageRepository = createPackageRepository("plugin-id", "version", "id", "name", configuration, packages);
        assertThat(packageRepository.getPluginConfiguration().getId(), is("plugin-id"));
        assertThat(packageRepository.getPluginConfiguration().getVersion(), is("version"));
        assertThat(packageRepository.getId(), is("id"));
        assertThat(packageRepository.getName(), is("name"));
    }

    @Test
    public void shouldSetRepositoryOnAllAssociatedPackages() {
        Configuration configuration = new Configuration();
        PackageDefinition packageDefinition = new PackageDefinition();
        PackageRepository packageRepository = createPackageRepository("plugin-id", "version", "id", "name", configuration, new Packages(packageDefinition));
        packageRepository.setRepositoryReferenceOnPackages();
        assertThat(packageDefinition.getRepository(), is(packageRepository));
    }

    @Test
    public void shouldOnlyDisplayFieldsWhichAreNonSecureAndPartOfIdentityInGetConfigForDisplayWhenPluginExists() throws Exception {
        PackageConfigurations repositoryConfiguration = new PackageConfigurations();
        repositoryConfiguration.addConfiguration(new PackageConfiguration("key1").with(PART_OF_IDENTITY, true).with(SECURE, false));
        repositoryConfiguration.addConfiguration(new PackageConfiguration("key2").with(PART_OF_IDENTITY, false).with(SECURE, false));
        repositoryConfiguration.addConfiguration(new PackageConfiguration("key3").with(PART_OF_IDENTITY, true).with(SECURE, true));
        repositoryConfiguration.addConfiguration(new PackageConfiguration("key4").with(PART_OF_IDENTITY, false).with(SECURE, true));
        repositoryConfiguration.addConfiguration(new PackageConfiguration("key5").with(PART_OF_IDENTITY, true).with(SECURE, false));
        RepositoryMetadataStore.getInstance().addMetadataFor("plugin1", repositoryConfiguration);

        Configuration configuration = new Configuration(create("key1", false, "value1"), create("key2", false, "value2"), create("key3", true, "value3"), create("key4", true, "value4"), create("key5", false, "value5"));
        PackageRepository repository = PackageRepositoryMother.create("repo1", "repo1-name", "plugin1", "1", configuration);

        assertThat(repository.getConfigForDisplay(), is("Repository: [key1=value1, key5=value5]"));
    }

    @Test
    public void shouldConvertKeysToLowercaseInGetConfigForDisplay() throws Exception {
        RepositoryMetadataStore.getInstance().addMetadataFor("some-plugin", new PackageConfigurations());
        PackageMetadataStore.getInstance().addMetadataFor("some-plugin", new PackageConfigurations());

        Configuration configuration = new Configuration(create("kEY1", false, "vALue1"), create("KEY_MORE_2", false, "VALUE_2"), create("key_3", false, "value3"));
        PackageRepository repository = PackageRepositoryMother.create("repo1", "repo1-name", "some-plugin", "1", configuration);

        assertThat(repository.getConfigForDisplay(), is("Repository: [key1=vALue1, key_more_2=VALUE_2, key_3=value3]"));
    }

    @Test
    public void shouldNotDisplayEmptyValuesInGetConfigForDisplay() throws Exception {
        RepositoryMetadataStore.getInstance().addMetadataFor("some-plugin", new PackageConfigurations());
        PackageMetadataStore.getInstance().addMetadataFor("some-plugin", new PackageConfigurations());

        Configuration configuration = new Configuration(create("rk1", false, ""), create("rk2", false, "some-non-empty-value"), create("rk3", false, null));
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo", "some-plugin", "version", configuration);

        assertThat(repository.getConfigForDisplay(), is("Repository: [rk2=some-non-empty-value]"));
    }

    @Test
    public void shouldDisplayAllNonSecureFieldsInGetConfigForDisplayWhenPluginDoesNotExist() {
        Configuration configuration = new Configuration(create("key1", false, "value1"), create("key2", true, "value2"), create("key3", false, "value3"));
        PackageRepository repository = PackageRepositoryMother.create("repo1", "repo1-name", "some-plugin-which-does-not-exist", "1", configuration);

        assertThat(repository.getConfigForDisplay(), is("WARNING! Plugin missing for Repository: [key1=value1, key3=value3]"));
    }

    @Test
    public void shouldMakeConfigurationSecureBasedOnMetadata() throws Exception {
        GoCipher goCipher = new GoCipher();

        /*secure property is set based on metadata*/
        ConfigurationProperty secureProperty = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, goCipher);
        ConfigurationProperty nonSecureProperty = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, goCipher);
        PackageDefinition packageDefinition = new PackageDefinition("go", "name", new Configuration(secureProperty, nonSecureProperty));

        //meta data of package
        PackageConfigurations packageConfigurations = new PackageConfigurations();
        packageConfigurations.addConfiguration(new PackageConfiguration("key1").with(SECURE, true));
        packageConfigurations.addConfiguration(new PackageConfiguration("key2").with(SECURE, false));
        PackageMetadataStore.getInstance().addMetadataFor("plugin-id", packageConfigurations);


        /*secure property is set based on metadata*/
        ConfigurationProperty secureRepoProperty = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, goCipher);
        ConfigurationProperty nonSecureRepoProperty = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, goCipher);
        PackageRepository packageRepository = createPackageRepository("plugin-id", "version", "id", "name",
                new Configuration(secureRepoProperty, nonSecureRepoProperty),
                new Packages(packageDefinition));

        //meta data of repo
        PackageConfigurations repositoryConfiguration = new PackageConfigurations();
        repositoryConfiguration.addConfiguration(new PackageConfiguration("key1").with(SECURE, true));
        repositoryConfiguration.addConfiguration(new PackageConfiguration("key2").with(SECURE, false));
        RepositoryMetadataStore.getInstance().addMetadataFor("plugin-id", repositoryConfiguration);


        packageRepository.applyPackagePluginMetadata();


        //assert package properties
        assertThat(secureProperty.isSecure(), is(true));
        assertThat(secureProperty.getEncryptedConfigurationValue(), is(notNullValue()));
        assertThat(secureProperty.getEncryptedValue(), is(goCipher.encrypt("value1")));

        assertThat(nonSecureProperty.isSecure(), is(false));
        assertThat(nonSecureProperty.getValue(), is("value2"));

        //assert repository properties
        assertThat(secureRepoProperty.isSecure(), is(true));
        assertThat(secureRepoProperty.getEncryptedConfigurationValue(), is(notNullValue()));
        assertThat(secureRepoProperty.getEncryptedValue(), is(goCipher.encrypt("value1")));

        assertThat(nonSecureRepoProperty.isSecure(), is(false));
        assertThat(nonSecureRepoProperty.getValue(), is("value2"));
    }

    @Test
    public void shouldNotUpdateSecurePropertyWhenPluginIsMissing() {
        GoCipher goCipher = new GoCipher();
        ConfigurationProperty secureProperty = new ConfigurationProperty(new ConfigurationKey("key1"), null, new EncryptedConfigurationValue("value"), goCipher);
        ConfigurationProperty nonSecureProperty = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, goCipher);
        PackageDefinition packageDefinition = new PackageDefinition("go", "name", new Configuration(secureProperty, nonSecureProperty));

        ConfigurationProperty nonSecureRepoProperty = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, goCipher);
        ConfigurationProperty secureRepoProperty = new ConfigurationProperty(new ConfigurationKey("key2"), null, new EncryptedConfigurationValue("value"), goCipher);
        PackageRepository packageRepository = createPackageRepository("plugin-id", "version", "id", "name", new Configuration(secureRepoProperty, nonSecureRepoProperty),
                new Packages(packageDefinition));

        packageRepository.applyPackagePluginMetadata();

        assertThat(secureProperty.getEncryptedConfigurationValue(), is(notNullValue()));
        assertThat(secureProperty.getConfigurationValue(), is(nullValue()));

        assertThat(nonSecureProperty.getConfigurationValue(), is(notNullValue()));
        assertThat(nonSecureProperty.getEncryptedConfigurationValue(), is(nullValue()));

        assertThat(secureRepoProperty.getEncryptedConfigurationValue(), is(notNullValue()));
        assertThat(secureRepoProperty.getConfigurationValue(), is(nullValue()));

        assertThat(nonSecureRepoProperty.getConfigurationValue(), is(notNullValue()));
        assertThat(nonSecureRepoProperty.getEncryptedConfigurationValue(), is(nullValue()));
    }

    @Test
    public void shouldSetConfigAttributesAsAvailable() throws Exception {
        //metadata setup
        PackageConfigurations repositoryConfiguration = new PackageConfigurations();
        repositoryConfiguration.add(new PackageConfiguration("url"));
        repositoryConfiguration.add(new PackageConfiguration("username"));
        repositoryConfiguration.add(new PackageConfiguration("password").with(SECURE, true));
        repositoryConfiguration.add(new PackageConfiguration("secureKeyNotChanged").with(SECURE, true));
        RepositoryMetadataStore.getInstance().addMetadataFor("yum", repositoryConfiguration);

        String name = "go-server";
        String repoId = "repo-id";
        String pluginId = "yum";
        ConfigurationHolder url = new ConfigurationHolder("url", "http://test.com");
        ConfigurationHolder username = new ConfigurationHolder("username", "user");
        String oldEncryptedValue = "oldEncryptedValue";
        ConfigurationHolder password = new ConfigurationHolder("password", "pass", oldEncryptedValue, true, "1");
        ConfigurationHolder secureKeyNotChanged = new ConfigurationHolder("secureKeyNotChanged", "pass", oldEncryptedValue, true, "0");
        Map attributes = createPackageRepositoryConfiguration(name, pluginId, repoId, url, username, password, secureKeyNotChanged);

        PackageRepository packageRepository = new PackageRepository();
        Packages packages = new Packages();
        packageRepository.setPackages(packages);


        packageRepository.setConfigAttributes(attributes);

        assertThat(packageRepository.getName(), is(name));
        assertThat(packageRepository.getId(), is(repoId));
        assertThat(packageRepository.getPluginConfiguration().getId(), is(pluginId));

        assertThat(packageRepository.getConfiguration().get(0).getConfigurationKey().getName(), is(url.name));
        assertThat(packageRepository.getConfiguration().get(0).getConfigurationValue().getValue(), is(url.value));

        assertThat(packageRepository.getConfiguration().get(1).getConfigurationKey().getName(), is(username.name));
        assertThat(packageRepository.getConfiguration().get(1).getConfigurationValue().getValue(), is(username.value));

        assertThat(packageRepository.getConfiguration().get(2).getConfigurationKey().getName(), is(password.name));
        assertThat(packageRepository.getConfiguration().get(2).getEncryptedValue(), is(new GoCipher().encrypt(password.value)));
        assertThat(packageRepository.getConfiguration().get(2).getConfigurationValue(), is(nullValue()));

        assertThat(packageRepository.getConfiguration().get(3).getConfigurationKey().getName(), is(secureKeyNotChanged.name));
        assertThat(packageRepository.getConfiguration().get(3).getEncryptedValue(), is(oldEncryptedValue));
        assertThat(packageRepository.getConfiguration().get(3).getConfigurationValue(), is(nullValue()));

        assertSame(packageRepository.getPackages(), packages);
    }

    @Test
    public void shouldValidateIfNameIsMissing() {
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.validate(new ConfigSaveValidationContext(new BasicCruiseConfig(), null));
        assertThat(packageRepository.errors().getAllOn("name"), is(asList("Please provide name")));
    }

    @Test
    public void shouldAddPackageDefinitionToRepo() {
        PackageRepository repository = PackageRepositoryMother.create("repo1");
        String existingPackageId = repository.getPackages().get(0).getId();
        PackageDefinition pkg = PackageDefinitionMother.create("pkg");

        repository.addPackage(pkg);

        assertThat(repository.getPackages().size(), is(2));
        assertThat(repository.getPackages().get(0).getId(), is(existingPackageId));
        assertThat(repository.getPackages().get(1).getId(), is(pkg.getId()));
    }

    @Test
    public void shouldFindPackageById() throws Exception {
        PackageRepository repository = PackageRepositoryMother.create("repo-id2", "repo2", "plugin-id", "1.0", null);
        PackageDefinition p1 = PackageDefinitionMother.create("id1", "pkg1", null, repository);
        PackageDefinition p2 = PackageDefinitionMother.create("id2", "pkg2", null, repository);
        Packages packages = new Packages(p1, p2);
        repository.setPackages(packages);
        assertThat(repository.findPackage("id2"), is(p2));
    }

    @Test
    public void shouldClearConfigurationsWhichAreEmptyAndNoErrors() throws Exception {
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.getConfiguration().add(new ConfigurationProperty(new ConfigurationKey("name-one"), new ConfigurationValue()));
        packageRepository.getConfiguration().add(new ConfigurationProperty(new ConfigurationKey("name-two"), new EncryptedConfigurationValue()));
        packageRepository.getConfiguration().add(new ConfigurationProperty(new ConfigurationKey("name-three"), null, new EncryptedConfigurationValue(), null));

        ConfigurationProperty configurationProperty = new ConfigurationProperty(new ConfigurationKey("name-four"), null, new EncryptedConfigurationValue(), null);
        configurationProperty.addErrorAgainstConfigurationValue("error");
        packageRepository.getConfiguration().add(configurationProperty);

        packageRepository.clearEmptyConfigurations();

        assertThat(packageRepository.getConfiguration().size(), is(1));
        assertThat(packageRepository.getConfiguration().get(0).getConfigurationKey().getName(), is("name-four"));

    }

    @Test
    public void shouldValidateName() throws Exception {
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.setName("some name");
        packageRepository.validate(new ConfigSaveValidationContext(null));
        assertThat(packageRepository.errors().isEmpty(), is(false));
        assertThat(packageRepository.errors().getAllOn(PackageRepository.NAME).get(0),
                is("Invalid PackageRepository name 'some name'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldRemoveGivenPackageFromTheRepository() throws Exception {
        PackageDefinition packageDefinitionOne = new PackageDefinition("pid1", "pname1", null);
        PackageDefinition packageDefinitionTwo = new PackageDefinition("pid2", "pname2", null);
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.addPackage(packageDefinitionOne);
        packageRepository.addPackage(packageDefinitionTwo);
        packageRepository.removePackage("pid1");

        assertThat(packageRepository.getPackages().size(), is(1));
        assertThat(packageRepository.getPackages(), hasItems(packageDefinitionTwo));
    }

    @Test
    public void shouldThrowErrorWhenGivenPackageNotFoundDuringRemove() throws Exception {
        PackageRepository packageRepository = new PackageRepository();
        try {
            packageRepository.removePackage("invalid");
        } catch (RuntimeException e) {
            assertThat(e.getMessage(), is("Could not find package with id:[invalid]"));
        }
    }

    @Test
    public void shouldFindPackageDefinitionBasedOnParams() throws Exception {
        PackageRepository packageRepository = PackageRepositoryMother.create("repo-id1", "packageRepository", "plugin-id", "1.0", null);
        PackageDefinition packageDefinitionOne = PackageDefinitionMother.create("pid1", packageRepository);
        PackageDefinition packageDefinitionTwo = PackageDefinitionMother.create("pid2", packageRepository);
        packageRepository.getPackages().addAll(asList(packageDefinitionOne, packageDefinitionTwo));

        Map attributes = new HashMap();
        attributes.put("packageId", "pid1");

        PackageDefinition actualPackageDefinition = packageRepository.findOrCreatePackageDefinition(attributes);
        assertThat(actualPackageDefinition, is(packageDefinitionOne));
    }

    @Test
    public void shouldCreatePackageBasedOnParams() throws Exception {
        PackageRepository packageRepository = PackageRepositoryMother.create("repo-id1", "packageRepository", "plugin-id", "1.0", null);
        Map packageDefAttr = createPackageDefinitionConfiguration("package_name", "pluginId", new ConfigurationHolder("key1", "value1"), new ConfigurationHolder("key2", "value2"));
        Map map = new HashMap();
        map.put("package_definition", packageDefAttr);
        PackageDefinition actualPackageDefinition = packageRepository.findOrCreatePackageDefinition(map);
        assertThat(actualPackageDefinition, is(PackageDefinitionMother.create(null, "package_name",
                new Configuration(create("key1", false, "value1"), create("key2", false, "value2")), packageRepository)));
        assertThat(actualPackageDefinition.getRepository(), is(packageRepository));
    }

    @Test
    public void shouldValidateUniqueNames() {
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.setName("REPO");
        HashMap<String, PackageRepository> nameMap = new HashMap<String, PackageRepository>();
        PackageRepository original = new PackageRepository();
        original.setName("repo");
        nameMap.put("repo", original);
        packageRepository.validateNameUniqueness(nameMap);
        assertThat(
                packageRepository.errors().getAllOn(PackageRepository.NAME).contains("You have defined multiple repositories called 'REPO'. Repository names are case-insensitive and must be unique."),
                is(true));
        assertThat(original.errors().getAllOn(PackageRepository.NAME).contains("You have defined multiple repositories called 'REPO'. Repository names are case-insensitive and must be unique."),
                is(true));
    }

    @Test
    public void shouldValidateUniqueKeysInConfiguration() {
        ConfigurationProperty one = new ConfigurationProperty(new ConfigurationKey("one"), new ConfigurationValue("value1"));
        ConfigurationProperty duplicate1 = new ConfigurationProperty(new ConfigurationKey("ONE"), new ConfigurationValue("value2"));
        ConfigurationProperty duplicate2 = new ConfigurationProperty(new ConfigurationKey("ONE"), new ConfigurationValue("value3"));
        ConfigurationProperty two = new ConfigurationProperty(new ConfigurationKey("two"), new ConfigurationValue());
        PackageRepository repository = new PackageRepository();
        repository.setConfiguration(new Configuration(one, duplicate1, duplicate2, two));
        repository.setName("yum");

        repository.validate(null);
        assertThat(one.errors().isEmpty(), is(false));
        assertThat(one.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'ONE' found for Repository 'yum'"), is(true));
        assertThat(duplicate1.errors().isEmpty(), is(false));
        assertThat(one.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'ONE' found for Repository 'yum'"), is(true));
        assertThat(duplicate2.errors().isEmpty(), is(false));
        assertThat(one.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'ONE' found for Repository 'yum'"), is(true));
        assertThat(two.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldGenerateIdIfNotAssigned(){
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.ensureIdExists();
        assertThat(packageRepository.getId(), is(notNullValue()));

        packageRepository = new PackageRepository();
        packageRepository.setId("id");
        packageRepository.ensureIdExists();
        assertThat(packageRepository.getId(), is("id"));
    }

    private PackageRepository createPackageRepository(String pluginId, String version, String id, String name, Configuration configuration, Packages packages) {
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.setConfiguration(configuration);
        packageRepository.setPackages(packages);
        packageRepository.setPluginConfiguration(new PluginConfiguration(pluginId, version));
        packageRepository.setId(id);
        packageRepository.setName(name);
        return packageRepository;
    }

    private Map createPackageRepositoryConfiguration(String name, String pluginId, String repoId, ConfigurationHolder... configurations) {
        Map attributes = new HashMap();
        attributes.put(PackageRepository.NAME, name);
        attributes.put(PackageRepository.REPO_ID, repoId);

        HashMap pluginConfiguration = new HashMap();
        pluginConfiguration.put(PluginConfiguration.ID, pluginId);
        attributes.put(PackageRepository.PLUGIN_CONFIGURATION, pluginConfiguration);

        createPackageConfigurationsFor(attributes, configurations);
        return attributes;
    }
}
