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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ArtifactStoreTest {
    private ArtifactMetadataStore store;

    @BeforeEach
    public void setUp() {
        store = ArtifactMetadataStore.instance();
    }

    @AfterEach
    public void tearDown() {
        store.clear();
    }

    @Test
    public void addConfigurations_shouldAddConfigurationsWithValue() {
        ConfigurationProperty property = create("username", false, "some_value");

        ArtifactStore artifactStore = new ArtifactStore("id", "plugin_id");
        artifactStore.addConfigurations(List.of(property));

        assertThat(artifactStore.size()).isEqualTo(1);
        assertThat(artifactStore).contains(create("username", false, "some_value"));
    }

    @Test
    public void addConfigurations_shouldAddConfigurationsWithEncryptedValue() {
        ConfigurationProperty property = create("username", true, "some_value");

        ArtifactStore artifactStore = new ArtifactStore("id", "plugin_id");
        artifactStore.addConfigurations(List.of(property));

        assertThat(artifactStore.size()).isEqualTo(1);
        assertThat(artifactStore).contains(create("username", true, "some_value"));
    }

    @Test
    public void shouldReturnObjectDescription() {
        assertThat(new ArtifactStore().getObjectDescription()).isEqualTo("Artifact store");
    }

    @Test
    public void shouldNotAllowNullPluginIdOrArtifactStoreId() {
        ArtifactStore store = new ArtifactStore();

        store.validate(null);
        assertThat(store.errors().size()).isEqualTo(2);
        assertThat(store.errors().on(ArtifactStore.PLUGIN_ID)).isEqualTo("Artifact store cannot have a blank plugin id.");
        assertThat(store.errors().on(ArtifactStore.ID)).isEqualTo("Artifact store cannot have a blank id.");
    }

    @Test
    public void shouldValidateArtifactStoreIdPattern() {
        ArtifactStore store = new ArtifactStore("!123", "docker");
        store.validate(null);
        assertThat(store.errors().size()).isEqualTo(1);
        assertThat(store.errors().on(ArtifactStore.ID)).isEqualTo("Invalid id '!123'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
    }

    @Test
    public void shouldValidateConfigPropertyNameUniqueness() {
        ConfigurationProperty prop1 = ConfigurationPropertyMother.create("USERNAME");
        ConfigurationProperty prop2 = ConfigurationPropertyMother.create("USERNAME");
        ArtifactStore store = new ArtifactStore("s3.plugin", "cd.go.s3.plugin", prop1, prop2);

        store.validate(null);

        assertThat(store.errors().size()).isEqualTo(0);

        assertThat(prop1.errors().size()).isEqualTo(1);
        assertThat(prop2.errors().size()).isEqualTo(1);

        assertThat(prop1.errors().on(ConfigurationProperty.CONFIGURATION_KEY)).isEqualTo("Duplicate key 'USERNAME' found for Artifact store 's3.plugin'");
        assertThat(prop2.errors().on(ConfigurationProperty.CONFIGURATION_KEY)).isEqualTo("Duplicate key 'USERNAME' found for Artifact store 's3.plugin'");
    }

    @Test
    public void shouldReturnTrueIfPluginInfoIsDefined() {
        final ArtifactPluginInfo pluginInfo = new ArtifactPluginInfo(pluginDescriptor("plugin_id"), null, null, null, null, null);
        store.setPluginInfo(pluginInfo);

        final ArtifactStore artifactStore = new ArtifactStore("id", "plugin_id");

        assertTrue(artifactStore.hasPluginInfo());
    }

    @Test
    public void shouldReturnFalseIfPluginInfoIsDefined() {
        final ArtifactStore artifactStore = new ArtifactStore("id", "plugin_id");

        assertFalse(artifactStore.hasPluginInfo());
    }

    @Test
    public void postConstruct_shouldEncryptSecureConfigurations() {
        final PluggableInstanceSettings storeConfig = new PluggableInstanceSettings(
                List.of(new PluginConfiguration("password", new Metadata(true, true)))
        );

        final ArtifactPluginInfo pluginInfo = new ArtifactPluginInfo(pluginDescriptor("plugin_id"), storeConfig, null, null, null, null);

        store.setPluginInfo(pluginInfo);
        ArtifactStore artifactStore = new ArtifactStore("id", "plugin_id", new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass")));

        artifactStore.encryptSecureConfigurations();

        assertThat(artifactStore.size()).isEqualTo(1);
        assertTrue(artifactStore.first().isSecure());
    }

    @Test
    public void postConstruct_shouldIgnoreEncryptionIfPluginInfoIsNotDefined() {
        ArtifactStore artifactStore = new ArtifactStore("id", "plugin_id", new ConfigurationProperty(new ConfigurationKey("password"), new ConfigurationValue("pass")));

        artifactStore.encryptSecureConfigurations();

        assertThat(artifactStore.size()).isEqualTo(1);
        assertFalse(artifactStore.first().isSecure());
    }

    private PluginDescriptor pluginDescriptor(String pluginId) {
        return new PluginDescriptor() {
            @Override
            public String id() {
                return pluginId;
            }

            @Override
            public String version() {
                return null;
            }

            @Override
            public About about() {
                return null;
            }
        };
    }
}
