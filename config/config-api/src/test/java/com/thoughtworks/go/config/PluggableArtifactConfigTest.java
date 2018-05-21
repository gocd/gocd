/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.ArtifactType;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.security.GoCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PluggableArtifactConfigTest {
    @Test
    public void shouldCreatePluggableArtifact() {
        final PluggableArtifactConfig artifactConfig = new PluggableArtifactConfig("Artifact-ID", "Store-ID", create("Foo", false, "Bar"));

        assertThat(artifactConfig.getId(), is("Artifact-ID"));
        assertThat(artifactConfig.getStoreId(), is("Store-ID"));
        assertThat(artifactConfig.getArtifactType(), is(ArtifactType.plugin));
        assertThat(artifactConfig.getArtifactTypeValue(), is("Pluggable Artifact"));
        assertThat(artifactConfig.getConfiguration().get(0), is(create("Foo", false, "Bar")));
    }

    @Test
    public void validate_shouldValidatePluggableArtifactStoreId() {
        final ValidationContext validationContext = mock(ValidationContext.class);
        final PluggableArtifactConfig artifactConfig = new PluggableArtifactConfig("Artifact-ID", "Store-ID");
        final ArtifactStores artifactStores = mock(ArtifactStores.class);
        assertFalse(artifactConfig.hasErrors());

        when(artifactStores.find("Store-ID")).thenReturn(null);

        artifactConfig.validate(validationContext);

        assertTrue(artifactConfig.hasErrors());
        assertThat(artifactConfig.errors().getAll(), hasSize(1));
        assertThat(artifactConfig.errors().getAllOn("storeId"), hasSize(1));
        assertThat(artifactConfig.errors().on("storeId"), is("Artifact store with id `Store-ID` does not exist."));
    }

    @Test
    public void validate_shouldValidateArtifactPropertiesConfigurationKeyUniqueness() {
        final ValidationContext validationContext = mock(ValidationContext.class);
        final PluggableArtifactConfig artifactConfig = new PluggableArtifactConfig("Artifact-ID", "Store-ID", create("Foo", false, "Bar"), create("Foo", true, "Bar"));
        final ArtifactStores artifactStores = mock(ArtifactStores.class);
        assertFalse(artifactConfig.hasErrors());

        when(artifactStores.find("Store-ID")).thenReturn(new ArtifactStore("Store-ID", "pluginId"));

        artifactConfig.validate(validationContext);
        Configuration configuration = artifactConfig.getConfiguration();

        assertThat(configuration.get(0).errors().getAllOn("configurationKey"), is(Arrays.asList("Duplicate key 'Foo' found for Pluggable Artifact")));
        assertThat(configuration.get(1).errors().getAllOn("configurationKey"), is(Arrays.asList("Duplicate key 'Foo' found for Pluggable Artifact")));
    }

    @Test
    public void validate_shouldValidateUniquenessOnId() {
        final PluggableArtifactConfig existingConfig = new PluggableArtifactConfig("Artifact-ID", "Store-ID");
        final List<ArtifactConfig> artifactConfigConfigs = Arrays.asList(existingConfig);

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
        final List<ArtifactConfig> artifactConfigConfigs = Arrays.asList(existingConfig);

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
        final List<ArtifactConfig> artifactConfigConfigs = Arrays.asList(existingConfig);

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

        final boolean result = artifactConfig.validateTree(null);

        assertFalse(result);
    }

    @Test
    public void validateTree_shouldValidateNullId() {
        PluggableArtifactConfig artifactConfig = new PluggableArtifactConfig(null, "s3");

        final boolean result = artifactConfig.validateTree(null);

        assertFalse(result);
    }

    @Test
    public void validateTree_presenceStoreId() {
        PluggableArtifactConfig artifactConfig = new PluggableArtifactConfig("installer", "");

        final boolean result = artifactConfig.validateTree(null);

        assertFalse(result);
    }

    @Test
    public void validateTree_presenceOfStoreIdInArtifactStores() {
        PluggableArtifactConfig artifactConfig = new PluggableArtifactConfig("installer", "s3");

        final boolean result = artifactConfig.validateTree(null);

        assertFalse(result);
    }

    @Test
    public void postConstruct_shouldHandleEncryptionOfConfigProperties() throws InvalidCipherTextException {
        GoCipher goCipher = new GoCipher();

        ArtifactPluginInfo artifactPluginInfo = mock(ArtifactPluginInfo.class);
        PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);
        when(artifactPluginInfo.getDescriptor()).thenReturn(pluginDescriptor);
        when(pluginDescriptor.id()).thenReturn("cd.go.s3");
        ArtifactMetadataStore.instance().setPluginInfo(artifactPluginInfo);

        ArrayList<PluginConfiguration> pluginConfigurations = new ArrayList<>();
        pluginConfigurations.add(new PluginConfiguration("key1", new Metadata(true, true)));
        pluginConfigurations.add(new PluginConfiguration("key2", new Metadata(true, false)));
        when(artifactPluginInfo.getArtifactConfigSettings()).thenReturn(new PluggableInstanceSettings(pluginConfigurations));

        ConfigurationProperty secureProperty = new ConfigurationProperty(new ConfigurationKey("key1"), new ConfigurationValue("value1"), null, goCipher);
        ConfigurationProperty nonSecureProperty = new ConfigurationProperty(new ConfigurationKey("key2"), new ConfigurationValue("value2"), null, goCipher);
        PluggableArtifactConfig pluggableArtifactConfig = new PluggableArtifactConfig("id", "store-id", secureProperty, nonSecureProperty);
        pluggableArtifactConfig.setArtifactStore(new ArtifactStore("store-id", "cd.go.s3"));

        pluggableArtifactConfig.encryptSecureConfigurations();

        assertThat(secureProperty.isSecure(), is(true));
        assertThat(secureProperty.getEncryptedConfigurationValue(), is(notNullValue()));
        assertThat(secureProperty.getEncryptedValue(), is(goCipher.encrypt("value1")));

        assertThat(nonSecureProperty.isSecure(), is(false));
        assertThat(nonSecureProperty.getValue(), is("value2"));
    }
}