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
import com.thoughtworks.go.config.materials.AbstractMaterial;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.config.EncryptedConfigurationValue;
import com.thoughtworks.go.domain.config.RepositoryMetadataStoreHelper;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

public class PackageDefinitionTest extends PackageMaterialTestBase {
    @Before
    public void setup() {
        RepositoryMetadataStoreHelper.clear();
    }

    @After
    public void teardown() {
        RepositoryMetadataStoreHelper.clear();
    }

    @Test
    public void shouldCheckForEqualityOfPackageDefinition() {
        Configuration configuration = new Configuration();
        PackageDefinition packageDefinition = new PackageDefinition("id", "name", configuration);
        assertThat(packageDefinition, is(new PackageDefinition("id", "name", configuration)));
    }

    @Test
    public void shouldOnlyDisplayFieldsWhichAreNonSecureAndPartOfIdentityInGetConfigForDisplayWhenPluginExists() {
        String pluginId = "plugin-id";
        PackageConfigurations repositoryConfigurations = new PackageConfigurations();
        repositoryConfigurations.add(new PackageConfiguration("rk1", "rv1").with(PackageConfiguration.PART_OF_IDENTITY, true).with(PackageConfiguration.SECURE, false));
        repositoryConfigurations.add(new PackageConfiguration("rk2", "rv2").with(PackageConfiguration.PART_OF_IDENTITY, false).with(PackageConfiguration.SECURE, false));
        repositoryConfigurations.add(new PackageConfiguration("rk3", "rv3").with(PackageConfiguration.PART_OF_IDENTITY, true).with(PackageConfiguration.SECURE, true));
        RepositoryMetadataStore.getInstance().addMetadataFor(pluginId, repositoryConfigurations);

        PackageConfigurations packageConfigurations = new PackageConfigurations();
        packageConfigurations.add(new PackageConfiguration("pk1", "pv1").with(PackageConfiguration.PART_OF_IDENTITY, true).with(PackageConfiguration.SECURE, false));
        packageConfigurations.add(new PackageConfiguration("pk2", "pv2").with(PackageConfiguration.PART_OF_IDENTITY, false).with(PackageConfiguration.SECURE, false));
        packageConfigurations.add(new PackageConfiguration("pk3", "pv3").with(PackageConfiguration.PART_OF_IDENTITY, true).with(PackageConfiguration.SECURE, true));
        packageConfigurations.add(new PackageConfiguration("pk4", "pv4").with(PackageConfiguration.PART_OF_IDENTITY, false).with(PackageConfiguration.SECURE, true));
        packageConfigurations.add(new PackageConfiguration("pk5", "pv5").with(PackageConfiguration.PART_OF_IDENTITY, true).with(PackageConfiguration.SECURE, false));
        PackageMetadataStore.getInstance().addMetadataFor(pluginId, packageConfigurations);

        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo", pluginId, "version",
                new Configuration(create("rk1", false, "rv1"), create("rk2", false, "rv2"), create("rk3", true, "rv3")));
        Configuration packageConfig = new Configuration(create("pk1", false, "pv1"), create("pk2", false, "pv2"), create("pk3", true, "pv3"), create("pk4", true, "pv4"), create("pk5", false, "pv5"));
        PackageDefinition packageDefinition = PackageDefinitionMother.create("p-id", "name", packageConfig, repository);
        packageDefinition.setRepository(repository);

        assertThat(packageDefinition.getConfigForDisplay(), is("Repository: [rk1=rv1] - Package: [pk1=pv1, pk5=pv5]"));
    }

    @Test
    public void shouldDisplayAllNonSecureFieldsInGetConfigForDisplayWhenPluginDoesNotExist() {
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo", "some-plugin-which-does-not-exist", "version",
                new Configuration(create("rk1", false, "rv1"), create("rk2", true, "rv2")));
        PackageDefinition packageDefinition = PackageDefinitionMother.create("p-id", "name", new Configuration(create("pk1", false, "pv1"), create("pk2", true, "pv2"), create("pk3", false, "pv3")),
                repository);
        packageDefinition.setRepository(repository);

        assertThat(packageDefinition.getConfigForDisplay(), is("WARNING! Plugin missing for Repository: [rk1=rv1] - Package: [pk1=pv1, pk3=pv3]"));
    }

    @Test
    public void shouldConvertKeysToLowercaseInGetConfigForDisplay() throws Exception {
        RepositoryMetadataStore.getInstance().addMetadataFor("some-plugin", new PackageConfigurations());
        PackageMetadataStore.getInstance().addMetadataFor("some-plugin", new PackageConfigurations());

        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo", "some-plugin", "version", new Configuration(create("rk1", false, "rv1")));
        PackageDefinition packageDefinition = PackageDefinitionMother.create("p-id", "name",
                new Configuration(create("pack_key_1", false, "pack_value_1"), create("PACK_KEY_2", false, "PACK_VALUE_2"), create("pacK_KeY3", false, "pacKValue_3")), repository);
        packageDefinition.setRepository(repository);

        assertThat(packageDefinition.getConfigForDisplay(), is("Repository: [rk1=rv1] - Package: [pack_key_1=pack_value_1, pack_key_2=PACK_VALUE_2, pack_key3=pacKValue_3]"));
    }

    @Test
    public void shouldNotDisplayEmptyValuesInGetConfigForDisplay() throws Exception {
        RepositoryMetadataStore.getInstance().addMetadataFor("some-plugin", new PackageConfigurations());
        PackageMetadataStore.getInstance().addMetadataFor("some-plugin", new PackageConfigurations());

        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo", "some-plugin", "version", new Configuration(create("rk1", false, "rv1")));
        PackageDefinition packageDefinition = PackageDefinitionMother.create("p-id", "name",
                new Configuration(create("pk1", false, ""), create("pk2", false, "pack_value_2"), create("pk3", false, null)), repository);
        packageDefinition.setRepository(repository);

        assertThat(packageDefinition.getConfigForDisplay(), is("Repository: [rk1=rv1] - Package: [pk2=pack_value_2]"));
    }

    @Test
    public void shouldGetFingerprint() {
        String pluginId = "pluginid";
        PackageConfigurations repositoryConfigurations = new PackageConfigurations();
        repositoryConfigurations.add(new PackageConfiguration("k1", "v1").with(PackageConfiguration.PART_OF_IDENTITY, true));
        RepositoryMetadataStore.getInstance().addMetadataFor(pluginId, repositoryConfigurations);

        PackageConfigurations packageConfigurations = new PackageConfigurations();
        packageConfigurations.add(new PackageConfiguration("k2", "v2").with(PackageConfiguration.PART_OF_IDENTITY, true));
        PackageMetadataStore.getInstance().addMetadataFor(pluginId, packageConfigurations);

        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo", pluginId, "version", new Configuration(create("k1", false, "v1")));
        PackageDefinition packageDefinition = PackageDefinitionMother.create("p-id", "name", new Configuration(create("k2", false, "v2")), repository);

        String fingerprint = packageDefinition.getFingerprint(AbstractMaterial.FINGERPRINT_DELIMITER);

        assertThat(fingerprint, is(CachedDigestUtils.sha256Hex("plugin-id=pluginid<|>k2=v2<|>k1=v1")));
    }

    @Test
    public void shouldNotConsiderPropertiesMarkedAsNotPartOfIdentity_GetFingerprint() {
        String pluginId = "plugin-id";
        PackageConfigurations repositoryConfigurations = new PackageConfigurations();
        repositoryConfigurations.add(new PackageConfiguration("rk1", "rv1").with(PackageConfiguration.PART_OF_IDENTITY, true));
        repositoryConfigurations.add(new PackageConfiguration("rk2", "rv2").with(PackageConfiguration.PART_OF_IDENTITY, false));
        RepositoryMetadataStore.getInstance().addMetadataFor(pluginId, repositoryConfigurations);

        PackageConfigurations packageConfigurations = new PackageConfigurations();
        packageConfigurations.add(new PackageConfiguration("pk1", "pv1").with(PackageConfiguration.PART_OF_IDENTITY, false));
        packageConfigurations.add(new PackageConfiguration("pk2", "pv2").with(PackageConfiguration.PART_OF_IDENTITY, true));
        PackageMetadataStore.getInstance().addMetadataFor(pluginId, packageConfigurations);

        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo", pluginId, "version",
                new Configuration(create("rk1", false, "rv1"), create("rk2", false, "rv2")));
        PackageDefinition packageDefinition = PackageDefinitionMother.create("p-id", "name",
                new Configuration(create("pk1", false, "pv1"), create("pk2", false, "pv2")), repository);

        String fingerprint = packageDefinition.getFingerprint(AbstractMaterial.FINGERPRINT_DELIMITER);

        assertThat(fingerprint, is(CachedDigestUtils.sha256Hex("plugin-id=plugin-id<|>pk2=pv2<|>rk1=rv1")));
    }

    @Test
    public void shouldNotConsiderAllPropertiesForFingerprintWhenMetadataIsNotAvailable() {
        String pluginId = "plugin-id";

        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo", pluginId, "version",
                new Configuration(create("rk1", false, "rv1"), create("rk2", false, "rv2")));
        PackageDefinition packageDefinition = PackageDefinitionMother.create("p-id", "name",
                new Configuration(create("pk1", false, "pv1"), create("pk2", false, "pv2")), repository);

        String fingerprint = packageDefinition.getFingerprint(AbstractMaterial.FINGERPRINT_DELIMITER);

        assertThat(fingerprint, is(CachedDigestUtils.sha256Hex("plugin-id=plugin-id<|>pk1=pv1<|>pk2=pv2<|>rk1=rv1<|>rk2=rv2")));
    }

    @Test
    public void shouldMakeConfigurationSecureBasedOnMetadata() throws Exception {
        /*secure property is set based on metadata*/
        ConfigurationProperty secureProperty = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, new GoCipher());
        ConfigurationProperty nonSecureProperty = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, new GoCipher());
        PackageDefinition packageDefinition = new PackageDefinition("go", "name", new Configuration(secureProperty, nonSecureProperty));
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.setPluginConfiguration(new PluginConfiguration("plugin-id", "1.0"));
        packageDefinition.setRepository(packageRepository);

        PackageConfigurations packageConfigurations = new PackageConfigurations();
        packageConfigurations.addConfiguration(new PackageConfiguration("key1").with(PackageConfiguration.SECURE, true));
        packageConfigurations.addConfiguration(new PackageConfiguration("key2").with(PackageConfiguration.SECURE, false));
        PackageMetadataStore.getInstance().addMetadataFor("plugin-id", packageConfigurations);

        packageDefinition.applyPackagePluginMetadata("plugin-id");

        assertThat(secureProperty.isSecure(), is(true));
        assertThat(nonSecureProperty.isSecure(), is(false));
    }

    @Test
    public void shouldSetConfigAttributes() throws Exception {
        PackageDefinition definition = new PackageDefinition();
        String pluginId = "plugin";
        Map config = createPackageDefinitionConfiguration("package-name", pluginId, new ConfigurationHolder("key1", "value1"), new ConfigurationHolder("key2", "value2", "encrypted-value", true, "1"),
                new ConfigurationHolder("key3", "test", "encrypted-value", true, "0"));
        PackageConfigurations metadata = new PackageConfigurations();
        metadata.addConfiguration(new PackageConfiguration("key1"));
        metadata.addConfiguration(new PackageConfiguration("key2").with(PackageConfiguration.SECURE, true));
        metadata.addConfiguration(new PackageConfiguration("key3").with(PackageConfiguration.SECURE, true));
        PackageMetadataStore.getInstance().addMetadataFor(pluginId, metadata);
        definition.setRepository(PackageRepositoryMother.create("1"));
        definition.setConfigAttributes(config);

        String encryptedValue = new GoCipher().encrypt("value2");

        assertThat(definition.getName(), is("package-name"));
        assertThat(definition.getConfiguration().size(), is(3));
        assertThat(definition.getConfiguration().getProperty("key1").getConfigurationValue().getValue(), is("value1"));
        assertThat(definition.getConfiguration().getProperty("key1").getEncryptedConfigurationValue(), is(nullValue()));
        assertThat(definition.getConfiguration().getProperty("key2").getEncryptedValue(), is(encryptedValue));
        assertThat(definition.getConfiguration().getProperty("key2").getConfigurationValue(), is(nullValue()));
        assertThat(definition.getConfiguration().getProperty("key3").getEncryptedValue(), is("encrypted-value"));
        assertThat(definition.getConfiguration().getProperty("key3").getConfigurationValue(), is(nullValue()));
    }

    @Test
    public void shouldValidateIfNameIsMissing() {
        PackageDefinition packageDefinition = new PackageDefinition();
        packageDefinition.validate(new ConfigSaveValidationContext(new BasicCruiseConfig(), null));
        assertThat(packageDefinition.errors().isEmpty(), is(false));
        assertThat(packageDefinition.errors().getAllOn("name"), is(asList("Package name is mandatory")));
    }

    @Test
    public void shouldAddErrorToGivenKey() throws Exception {
        PackageDefinition packageDefinition = new PackageDefinition();
        packageDefinition.addError("field", "error message");
        assertThat(packageDefinition.errors().getAllOn("field").contains("error message"), is(true));
    }

    @Test
    public void shouldClearConfigurationsWhichAreEmptyAndNoErrors() throws Exception {
        PackageDefinition packageDefinition = new PackageDefinition();
        packageDefinition.getConfiguration().add(new ConfigurationProperty(new ConfigurationKey("name-one"), new ConfigurationValue()));
        packageDefinition.getConfiguration().add(new ConfigurationProperty(new ConfigurationKey("name-two"), new EncryptedConfigurationValue()));
        packageDefinition.getConfiguration().add(new ConfigurationProperty(new ConfigurationKey("name-three"), null, new EncryptedConfigurationValue(), null));

        ConfigurationProperty configurationProperty = new ConfigurationProperty(new ConfigurationKey("name-four"), null, new EncryptedConfigurationValue(), null);
        configurationProperty.addErrorAgainstConfigurationValue("error");
        packageDefinition.getConfiguration().add(configurationProperty);

        packageDefinition.clearEmptyConfigurations();

        assertThat(packageDefinition.getConfiguration().size(), is(1));
        assertThat(packageDefinition.getConfiguration().get(0).getConfigurationKey().getName(), is("name-four"));
    }

    @Test
    public void shouldValidateName() throws Exception {
        PackageDefinition packageDefinition = new PackageDefinition();
        packageDefinition.setName("some name");
        packageDefinition.validate(new ConfigSaveValidationContext(null));
        assertThat(packageDefinition.errors().isEmpty(), is(false));
        assertThat(packageDefinition.errors().getAllOn(PackageDefinition.NAME).get(0),
                is("Invalid Package name 'some name'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldSetAutoUpdateValue() throws Exception {
        PackageDefinition aPackage = new PackageDefinition();
        assertThat(aPackage.isAutoUpdate(), is(true));
        aPackage.setAutoUpdate(false);
        assertThat(aPackage.isAutoUpdate(), is(false));

    }

    @Test
    public void shouldValidateUniqueNames() {
        PackageDefinition packageDefinition = new PackageDefinition();
        packageDefinition.setName("PKG");
        HashMap<String, PackageDefinition> nameMap = new HashMap<String, PackageDefinition>();
        PackageDefinition original = new PackageDefinition();
        original.setName("pkg");
        nameMap.put("pkg", original);
        packageDefinition.validateNameUniqueness(nameMap);
        assertThat(packageDefinition.errors().isEmpty(), is(false));

        assertThat(
                packageDefinition.errors().getAllOn(PackageDefinition.NAME).contains(
                        "You have defined multiple packages called 'PKG'. Package names are case-insensitive and must be unique within a repository."),
                is(true));
    }

    @Test
    public void shouldValidateUniqueKeysInConfiguration() {
        ConfigurationProperty one = new ConfigurationProperty(new ConfigurationKey("one"), new ConfigurationValue("value1"));
        ConfigurationProperty duplicate1 = new ConfigurationProperty(new ConfigurationKey("ONE"), new ConfigurationValue("value2"));
        ConfigurationProperty duplicate2 = new ConfigurationProperty(new ConfigurationKey("ONE"), new ConfigurationValue("value3"));
        ConfigurationProperty two = new ConfigurationProperty(new ConfigurationKey("two"), new ConfigurationValue());
        PackageDefinition packageDefinition = new PackageDefinition();
        packageDefinition.setConfiguration(new Configuration(one, duplicate1, duplicate2, two));
        packageDefinition.setName("go-server");

        packageDefinition.validate(null);
        assertThat(one.errors().isEmpty(), is(false));
        assertThat(one.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'ONE' found for Package 'go-server'"), is(true));
        assertThat(duplicate1.errors().isEmpty(), is(false));
        assertThat(one.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'ONE' found for Package 'go-server'"), is(true));
        assertThat(duplicate2.errors().isEmpty(), is(false));
        assertThat(one.errors().getAllOn(ConfigurationProperty.CONFIGURATION_KEY).contains("Duplicate key 'ONE' found for Package 'go-server'"), is(true));
        assertThat(two.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldGenerateIdIfNotAssigned() {
        PackageDefinition packageDefinition = new PackageDefinition();
        packageDefinition.ensureIdExists();
        assertThat(packageDefinition.getId(), is(notNullValue()));

        packageDefinition = new PackageDefinition();
        packageDefinition.setId("id");
        packageDefinition.ensureIdExists();
        assertThat(packageDefinition.getId(), is("id"));
    }

    @Test
    public void shouldAddFingerprintFieldErrorWhenPackageDefinitionWithSameFingerprintExist() throws Exception {
        String expectedErrorMessage = "Cannot save package or repo, found duplicate packages. [Repo Name: 'repo-repo1', Package Name: 'pkg1'], [Repo Name: 'repo-repo1', Package Name: 'pkg3']";
        PackageRepository repository = PackageRepositoryMother.create("repo1");
        PackageDefinition definition1 = PackageDefinitionMother.create("1", "pkg1", new Configuration(new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("v1"))), repository);
        PackageDefinition definition2 = PackageDefinitionMother.create("2", "pkg2", new Configuration(new ConfigurationProperty(new ConfigurationKey("k2"), new ConfigurationValue("v2"))), repository);
        PackageDefinition definition3 = PackageDefinitionMother.create("3", "pkg3", new Configuration(new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("v1"))), repository);


        HashMap<String, Packages> map = new HashMap<String, Packages>();
        map.put(definition1.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER), new Packages(definition1, definition3));
        map.put(definition2.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER), new Packages(definition2));
        definition1.validateFingerprintUniqueness(map);
        definition2.validateFingerprintUniqueness(map);
        definition3.validateFingerprintUniqueness(map);

        assertThat(definition1.errors().getAllOn(PackageDefinition.ID), is(asList(expectedErrorMessage)));
        assertThat(definition3.errors().getAllOn(PackageDefinition.ID), is(asList(expectedErrorMessage)));

        assertThat(definition2.errors().getAllOn(PackageDefinition.ID), is(nullValue()));
    }

    @Test
    public void shouldNotAddFingerprintFieldErrorWhenPackageDefinitionWithSameFingerprintNotFound() throws Exception {

        PackageRepository repository = PackageRepositoryMother.create("repo1");
        PackageDefinition packageDefinition = PackageDefinitionMother.create("1", "pkg1", new Configuration(new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("v1"))), repository);

        HashMap<String, Packages> map = new HashMap<String, Packages>();
        map.put(packageDefinition.getFingerprint(AbstractMaterialConfig.FINGERPRINT_DELIMITER), new Packages(packageDefinition));
        packageDefinition.validateFingerprintUniqueness(map);

        assertThat(packageDefinition.errors().getAllOn(PackageDefinition.ID), is(nullValue()));
    }
}
