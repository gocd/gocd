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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.helper.ValidationContextMother;
import com.thoughtworks.go.domain.ArtifactType;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluggableArtifactConfigTest {

    private ArtifactPluginInfo artifactPluginInfo;

    @Before
    public void setup() {
        artifactPluginInfo = mock(ArtifactPluginInfo.class);
        PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);
        when(artifactPluginInfo.getDescriptor()).thenReturn(pluginDescriptor);
        when(pluginDescriptor.id()).thenReturn("cd.go.s3");
        ArtifactMetadataStore.instance().setPluginInfo(artifactPluginInfo);
    }

    @After
    public void clear() {
        ArtifactMetadataStore.instance().setPluginInfo(null);
    }

    @Test
    public void shouldCreatePluggableArtifact() {
        final PluggableArtifactConfig artifactConfig = new PluggableArtifactConfig("Artifact-ID", "Store-ID", create("Foo", false, "Bar"));

        assertThat(artifactConfig.getId(), is("Artifact-ID"));
        assertThat(artifactConfig.getStoreId(), is("Store-ID"));
        assertThat(artifactConfig.getArtifactType(), is(ArtifactType.external));
        assertThat(artifactConfig.getArtifactTypeValue(), is("Pluggable Artifact"));
        assertThat(artifactConfig.getConfiguration().get(0), is(create("Foo", false, "Bar")));
    }

    @Test
    public void validate_shouldValidatePluggableArtifactStoreId() {
        final ValidationContext validationContext = mock(ValidationContext.class);
        final PluggableArtifactConfig artifactConfig = new PluggableArtifactConfig("Artifact-ID", "Store-ID");
        final ArtifactStores artifactStores = mock(ArtifactStores.class);
        assertFalse(artifactConfig.hasErrors());

        when(validationContext.artifactStores()).thenReturn(artifactStores);
        when(validationContext.isWithinPipelines()).thenReturn(true);
        when(validationContext.getPipeline()).thenReturn(PipelineConfigMother.pipelineConfig("pipe"));
        when(artifactStores.find("Store-ID")).thenReturn(null);

        artifactConfig.validate(validationContext);

        assertTrue(artifactConfig.hasErrors());
        assertThat(artifactConfig.errors().getAll(), hasSize(1));
        assertThat(artifactConfig.errors().getAllOn("storeId"), hasSize(1));
        assertThat(artifactConfig.errors().on("storeId"), is("Artifact store with id `Store-ID` does not exist. Please correct the `storeId` attribute on pipeline `pipe`."));
    }

    @Test
    public void validate_shouldValidateArtifactPropertiesConfigurationKeyUniqueness() {
        final ValidationContext validationContext = mock(ValidationContext.class);
        final PluggableArtifactConfig artifactConfig = new PluggableArtifactConfig("Artifact-ID", "Store-ID", create("Foo", false, "Bar"), create("Foo", true, "Bar"));
        final ArtifactStores artifactStores = mock(ArtifactStores.class);
        assertFalse(artifactConfig.hasErrors());

        when(validationContext.artifactStores()).thenReturn(artifactStores);
        when(artifactStores.find("Store-ID")).thenReturn(new ArtifactStore("Store-ID", "pluginId"));

        artifactConfig.validate(validationContext);
        Configuration configuration = artifactConfig.getConfiguration();

        assertThat(configuration.get(0).errors().getAllOn("configurationKey"), is(Arrays.asList("Duplicate key 'Foo' found for Pluggable Artifact")));
        assertThat(configuration.get(1).errors().getAllOn("configurationKey"), is(Arrays.asList("Duplicate key 'Foo' found for Pluggable Artifact")));
    }

    @Test
    public void validate_shouldValidateUniquenessOnId() {
        final PluggableArtifactConfig existingConfig = new PluggableArtifactConfig("Artifact-ID", "Store-ID");
        final List<ArtifactTypeConfig> artifactConfigConfigs = Arrays.asList(existingConfig);

        final PluggableArtifactConfig newConfig = new PluggableArtifactConfig("Artifact-ID", "Store-ID");
        newConfig.validateUniqueness(artifactConfigConfigs);

        assertTrue(newConfig.hasErrors());
        assertTrue(existingConfig.hasErrors());

        assertThat(newConfig.errors().on("id"), is("Duplicate pluggable artifacts  with id `Artifact-ID` defined."));
        assertThat(existingConfig.errors().on("id"), is("Duplicate pluggable artifacts  with id `Artifact-ID` defined."));
    }

    @Test
    public void validate_shouldValidateArtifactPropertiesConfig() {
        final PluggableArtifactConfig existingConfig = new PluggableArtifactConfig("id1", "Store-ID", create("Foo", false, "Bar"));
        final List<ArtifactTypeConfig> artifactConfigConfigs = Arrays.asList(existingConfig);

        final PluggableArtifactConfig newConfig = new PluggableArtifactConfig("id2", "Store-ID", create("Foo", false, "Bar"));
        newConfig.validateUniqueness(artifactConfigConfigs);

        assertTrue(newConfig.hasErrors());
        assertTrue(existingConfig.hasErrors());

        assertThat(newConfig.errors().on("id"), is("Duplicate pluggable artifacts  configuration defined."));
        assertThat(existingConfig.errors().on("id"), is("Duplicate pluggable artifacts  configuration defined."));
    }

    @Test
    public void validate_shouldNotErrorWhenArtifactPropertiesConfigurationIsSameForDifferentStores() {
        final PluggableArtifactConfig existingConfig = new PluggableArtifactConfig("id1", "storeId1", create("Foo", false, "Bar"));
        final List<ArtifactTypeConfig> artifactConfigConfigs = Arrays.asList(existingConfig);

        final PluggableArtifactConfig newConfig = new PluggableArtifactConfig("id2", "storeId2", create("Foo", false, "Bar"));
        newConfig.validateUniqueness(artifactConfigConfigs);

        assertFalse(newConfig.hasErrors());
        assertFalse(existingConfig.hasErrors());

        assertNull(newConfig.errors().on("id"));
        assertNull(existingConfig.errors().on("id"));
    }

    @Test
    public void shouldSerializeToJson() {
        final PluggableArtifactConfig config = new PluggableArtifactConfig("id1", "Store-ID", create("Foo", false, "Bar"));

        final String actual = config.toJSON();

        assertThat(actual, is("{\"configuration\":{\"Foo\":\"Bar\"},\"id\":\"id1\",\"storeId\":\"Store-ID\"}"));
    }

    @Test
    public void validateTree_shouldValidatePresenceOfId() {
        final PluggableArtifactConfig artifactConfig = new PluggableArtifactConfig("", "s3");
        final ArtifactStores artifactStores = new ArtifactStores(new ArtifactStore("s3", "cd.go.s3"));

        final boolean result = artifactConfig.validateTree(ValidationContextMother.validationContext(artifactStores));

        assertFalse(result);
        assertThat(artifactConfig.errors().getAllOn("id"), is(Arrays.asList("\"Id\" is required for PluggableArtifact", "Invalid pluggable artifact id name ''. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.")));
    }

    @Test
    public void validateTree_shouldValidateNullId() {
        PluggableArtifactConfig artifactConfig = new PluggableArtifactConfig(null, "s3");

        final ArtifactStores artifactStores = new ArtifactStores(new ArtifactStore("s3", "cd.go.s3"));
        final boolean result = artifactConfig.validateTree(ValidationContextMother.validationContext(artifactStores));

        assertFalse(result);
    }

    @Test
    public void validateTree_presenceOfStoreIdInArtifactStores() {
        PluggableArtifactConfig artifactConfig = new PluggableArtifactConfig("installer", "");

        final ArtifactStores artifactStores = new ArtifactStores(new ArtifactStore("docker", "cd.go.docker"));
        final boolean result = artifactConfig.validateTree(ValidationContextMother.validationContext(artifactStores));

        assertFalse(result);
        assertThat(artifactConfig.errors().getAllOn("storeId"), is(Arrays.asList("\"Store id\" is required for PluggableArtifact")));
    }

    @Test
    public void validate_shouldAddAnErrorIfArtifactIdIsInvalid() {
        PluggableArtifactConfig artifactConfig = new PluggableArtifactConfig("asf@%", "f");

        final ArtifactStores artifactStores = new ArtifactStores(new ArtifactStore("docker", "cd.go.docker"));
        final boolean result = artifactConfig.validateTree(ValidationContextMother.validationContext(artifactStores));

        assertFalse(result);
        assertThat(artifactConfig.errors().getAllOn("id"), is(Arrays.asList("Invalid pluggable artifact id name 'asf@%'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.")));
    }

    @Test
    public void shouldHandleEncryptionOfConfigProperties() throws CryptoException {
        GoCipher goCipher = new GoCipher();

        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();
        pluginConfigurations.add(new PluginConfiguration("key1", new Metadata(true, true)));
        pluginConfigurations.add(new PluginConfiguration("key2", new Metadata(true, false)));
        when(artifactPluginInfo.getArtifactConfigSettings()).thenReturn(new PluggableInstanceSettings(pluginConfigurations));

        ConfigurationProperty secureProperty = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, goCipher);
        ConfigurationProperty nonSecureProperty = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, goCipher);
        PluggableArtifactConfig pluggableArtifactConfig = new PluggableArtifactConfig("id", "store-id", secureProperty, nonSecureProperty);

        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.getArtifactStores().add(new ArtifactStore("store-id", "cd.go.s3"));

        pluggableArtifactConfig.encryptSecureProperties(cruiseConfig, pluggableArtifactConfig);

        assertThat(secureProperty.isSecure(), is(true));
        assertThat(secureProperty.getEncryptedConfigurationValue(), is(notNullValue()));
        assertThat(secureProperty.getEncryptedValue(), is(goCipher.encrypt("value1")));

        assertThat(nonSecureProperty.isSecure(), is(false));
        assertThat(nonSecureProperty.getValue(), is("value2"));

    }

    @Test
    public void shouldNotEncryptConfigPropertiesWhenSpecifiedAsParameters() throws CryptoException {
        GoCipher goCipher = new GoCipher();

        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();
        pluginConfigurations.add(new PluginConfiguration("key1", new Metadata(true, true)));
        pluginConfigurations.add(new PluginConfiguration("key2", new Metadata(true, false)));
        when(artifactPluginInfo.getArtifactConfigSettings()).thenReturn(new PluggableInstanceSettings(pluginConfigurations));

        ConfigurationProperty secureProperty = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("#{value1}"), null, goCipher);
        ConfigurationProperty nonSecureProperty = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, goCipher);
        PluggableArtifactConfig pluggableArtifactConfig = new PluggableArtifactConfig("id", "store-id", secureProperty, nonSecureProperty);

        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.getArtifactStores().add(new ArtifactStore("store-id", "cd.go.s3"));

        pluggableArtifactConfig.encryptSecureProperties(cruiseConfig, pluggableArtifactConfig);

        assertThat(secureProperty.isSecure(), is(false));
        assertThat(secureProperty.getEncryptedConfigurationValue(), is(nullValue()));
        assertThat(secureProperty.getValue(), is("#{value1}"));

        assertThat(nonSecureProperty.isSecure(), is(false));
        assertThat(nonSecureProperty.getValue(), is("value2"));
    }

    @Test
    public void shouldHandleEncryptionOfConfigPropertiesIfStoreIdIsAValidParam() throws Exception {
        GoCipher goCipher = new GoCipher();

        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();
        pluginConfigurations.add(new PluginConfiguration("key1", new Metadata(true, true)));
        pluginConfigurations.add(new PluginConfiguration("key2", new Metadata(true, false)));
        when(artifactPluginInfo.getArtifactConfigSettings()).thenReturn(new PluggableInstanceSettings(pluginConfigurations));

        ConfigurationProperty secureProperty = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, goCipher);
        ConfigurationProperty nonSecureProperty = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, goCipher);
        PluggableArtifactConfig pluggableArtifactConfig = new PluggableArtifactConfig("id", "#{storeId}", secureProperty, nonSecureProperty);
        PluggableArtifactConfig preprocessedPluggableArtifactConfig = new PluggableArtifactConfig("id", "store-id", secureProperty, nonSecureProperty);

        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.getArtifactStores().add(new ArtifactStore("store-id", "cd.go.s3"));

        pluggableArtifactConfig.encryptSecureProperties(cruiseConfig, preprocessedPluggableArtifactConfig);

        assertThat(secureProperty.isSecure(), is(true));
        assertThat(secureProperty.getEncryptedConfigurationValue(), is(notNullValue()));
        assertThat(secureProperty.getEncryptedValue(), is(goCipher.encrypt("value1")));

        assertThat(nonSecureProperty.isSecure(), is(false));
        assertThat(nonSecureProperty.getValue(), is("value2"));

    }

    @Test
    public void shouldIgnoreEncryptionOfSecurePropertyForNonExistentParam() {
        GoCipher goCipher = new GoCipher();

        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();
        pluginConfigurations.add(new PluginConfiguration("key1", new Metadata(true, true)));
        pluginConfigurations.add(new PluginConfiguration("key2", new Metadata(true, false)));
        when(artifactPluginInfo.getArtifactConfigSettings()).thenReturn(new PluggableInstanceSettings(pluginConfigurations));

        ConfigurationProperty secureProperty = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, goCipher);
        ConfigurationProperty nonSecureProperty = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, goCipher);
        PluggableArtifactConfig pluggableArtifactConfig1 = new PluggableArtifactConfig("id", "#{non-existent-param}", secureProperty, nonSecureProperty);

        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.getArtifactStores().add(new ArtifactStore("store-id", "cd.go.s3"));

        pluggableArtifactConfig1.encryptSecureProperties(cruiseConfig, pluggableArtifactConfig1);

        assertThat(secureProperty.isSecure(), is(false));
        assertThat(secureProperty.getValue(), is("value1"));

        assertThat(nonSecureProperty.isSecure(), is(false));
        assertThat(nonSecureProperty.getValue(), is("value2"));
    }

    @Test
    public void shouldIgnoreEncryptionOfSecurePropertyIfParamsIsUndefined() {
        GoCipher goCipher = new GoCipher();

        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();
        pluginConfigurations.add(new PluginConfiguration("key1", new Metadata(true, true)));
        pluginConfigurations.add(new PluginConfiguration("key2", new Metadata(true, false)));
        when(artifactPluginInfo.getArtifactConfigSettings()).thenReturn(new PluggableInstanceSettings(pluginConfigurations));

        ConfigurationProperty secureProperty1 = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, goCipher);
        ConfigurationProperty secureProperty2 = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, goCipher);
        ConfigurationProperty nonSecureProperty1 = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, goCipher);
        ConfigurationProperty nonSecureProperty2 = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, goCipher);
        PluggableArtifactConfig pluggableArtifactConfig1 = new PluggableArtifactConfig("id", "#{storeId}", secureProperty1, nonSecureProperty1);
        PluggableArtifactConfig pluggableArtifactConfig2 = new PluggableArtifactConfig("id", "#{storeId}", secureProperty2, nonSecureProperty2);

        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.getArtifactStores().add(new ArtifactStore("store-id", "cd.go.s3"));

        pluggableArtifactConfig1.encryptSecureProperties(cruiseConfig, pluggableArtifactConfig1);
        pluggableArtifactConfig2.encryptSecureProperties(cruiseConfig, pluggableArtifactConfig2);

        assertThat(secureProperty1.isSecure(), is(false));
        assertThat(secureProperty1.getValue(), is("value1"));
        assertThat(nonSecureProperty1.isSecure(), is(false));
        assertThat(nonSecureProperty1.getValue(), is("value2"));

        assertThat(secureProperty2.isSecure(), is(false));
        assertThat(secureProperty2.getValue(), is("value1"));
        assertThat(nonSecureProperty2.isSecure(), is(false));
        assertThat(nonSecureProperty2.getValue(), is("value2"));
    }

    @Test
    public void shouldIgnoreEncryptionOfSecurePropertyForInvalidParamSpecification() {
        GoCipher goCipher = new GoCipher();

        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();
        pluginConfigurations.add(new PluginConfiguration("key1", new Metadata(true, true)));
        pluginConfigurations.add(new PluginConfiguration("key2", new Metadata(true, false)));
        when(artifactPluginInfo.getArtifactConfigSettings()).thenReturn(new PluggableInstanceSettings(pluginConfigurations));

        ConfigurationProperty secureProperty = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, goCipher);
        ConfigurationProperty nonSecureProperty = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, goCipher);
        PluggableArtifactConfig pluggableArtifactConfig = new PluggableArtifactConfig("id", "#{#{invalid}}", secureProperty, nonSecureProperty);

        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.getArtifactStores().add(new ArtifactStore("store-id", "cd.go.s3"));

        pluggableArtifactConfig.encryptSecureProperties(cruiseConfig, pluggableArtifactConfig);

        assertThat(secureProperty.isSecure(), is(false));
        assertThat(secureProperty.getValue(), is("value1"));

        assertThat(nonSecureProperty.isSecure(), is(false));
        assertThat(nonSecureProperty.getValue(), is("value2"));
    }

    @Test
    public void shouldIgnoreEncryptionOfSecurePropertyIfStoreIdIsNull() {
        GoCipher goCipher = new GoCipher();

        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();
        pluginConfigurations.add(new PluginConfiguration("key1", new Metadata(true, true)));
        pluginConfigurations.add(new PluginConfiguration("key2", new Metadata(true, false)));
        when(artifactPluginInfo.getArtifactConfigSettings()).thenReturn(new PluggableInstanceSettings(pluginConfigurations));

        ConfigurationProperty secureProperty = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, goCipher);
        ConfigurationProperty nonSecureProperty = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, goCipher);
        PluggableArtifactConfig pluggableArtifactConfig = new PluggableArtifactConfig("id", null, secureProperty, nonSecureProperty);

        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.getArtifactStores().add(new ArtifactStore("store-id", "cd.go.s3"));

        pluggableArtifactConfig.encryptSecureProperties(cruiseConfig, pluggableArtifactConfig);

        assertThat(secureProperty.isSecure(), is(false));
        assertThat(secureProperty.getValue(), is("value1"));

        assertThat(nonSecureProperty.isSecure(), is(false));
        assertThat(nonSecureProperty.getValue(), is("value2"));
    }

    @Test
    public void addConfigurations_shouldLeaveUserEnteredValuesAsIsIfArtifactStoreIsNull() throws CryptoException {
        PluggableArtifactConfig pluggableArtifactConfig = new PluggableArtifactConfig("id", "non-existent-store-id");
        ArrayList<ConfigurationProperty> configurationProperties = new ArrayList<>();
        configurationProperties.add(ConfigurationPropertyMother.create("plain", false, "plain"));
        configurationProperties.add(ConfigurationPropertyMother.create("secure", true, new GoCipher().encrypt("password")));

        pluggableArtifactConfig.addConfigurations(configurationProperties);

        assertThat(pluggableArtifactConfig.getConfiguration(), is(configurationProperties));
    }

    @Test
    public void addConfigurations_shouldLeaveUserEnteredValuesAsIsIfPluginIsMissing() throws CryptoException {
        ArtifactMetadataStore.instance().remove("cd.go.s3");
        PluggableArtifactConfig pluggableArtifactConfig = new PluggableArtifactConfig("id", "storeId");
        ArrayList<ConfigurationProperty> configurationProperties = new ArrayList<>();
        configurationProperties.add(ConfigurationPropertyMother.create("plain", false, "plain"));
        configurationProperties.add(ConfigurationPropertyMother.create("secure", true, new GoCipher().encrypt("password")));

        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.getArtifactStores().add(new ArtifactStore("storeId", "cd.go.s3"));

        pluggableArtifactConfig.addConfigurations(configurationProperties);

        assertThat(pluggableArtifactConfig.getConfiguration(), is(configurationProperties));
    }

    @Test
    public void addConfigurations_shouldSetUserSpecifiedConfigurationAsIs() throws CryptoException {
        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();

        pluginConfigurations.add(new PluginConfiguration("secure_property1", new Metadata(true, true)));
        pluginConfigurations.add(new PluginConfiguration("secure_property2", new Metadata(true, true)));
        pluginConfigurations.add(new PluginConfiguration("plain", new Metadata(true, false)));
        when(artifactPluginInfo.getArtifactConfigSettings()).thenReturn(new PluggableInstanceSettings(pluginConfigurations));

        PluggableArtifactConfig pluggableArtifactConfig = new PluggableArtifactConfig("id", "storeId");

        ArrayList<ConfigurationProperty> configurationProperties = new ArrayList<>();
        configurationProperties.add(new ConfigurationProperty(new ConfigurationKey("plain"), new ConfigurationValue("plain")));
        configurationProperties.add(new ConfigurationProperty(new ConfigurationKey("secure_property1"), new ConfigurationValue("password")));
        configurationProperties.add(new ConfigurationProperty(new ConfigurationKey("secure_property2"), new EncryptedConfigurationValue(new GoCipher().encrypt("secret"))));

        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        cruiseConfig.getArtifactStores().add(new ArtifactStore("storeId", "cd.go.s3"));

        pluggableArtifactConfig.addConfigurations(configurationProperties);

        assertThat(pluggableArtifactConfig.getConfiguration(), is(configurationProperties));
    }

    @Test
    public void hasValidPluginAndStore_shouldReturnFalseIfStoreDoesNotExist() {
        PluggableArtifactConfig pluggableArtifactConfig = new PluggableArtifactConfig("dist", "s3");

        assertFalse(pluggableArtifactConfig.hasValidPluginAndStore(new ArtifactStore("docker", "cd.go.docker")));
    }

    @Test
    public void hasValidPluginAndStore_shouldReturnFalseIfPluginDoesNotExist() {
        ArtifactMetadataStore.instance().remove("cd.go.s3");
        PluggableArtifactConfig pluggableArtifactConfig = new PluggableArtifactConfig("dist", "s3");

        assertFalse(pluggableArtifactConfig.hasValidPluginAndStore(new ArtifactStore("s3", "cd.go.s3")));
    }

    @Test
    public void hasValidPluginAndStore_shouldReturnTrueIfPluginAndStoreExist() {
        PluggableArtifactConfig pluggableArtifactConfig = new PluggableArtifactConfig("dist", "s3");

        assertTrue(pluggableArtifactConfig.hasValidPluginAndStore(new ArtifactStore("s3", "cd.go.s3")));
    }
}
