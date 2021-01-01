/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.service.materials;

import com.thoughtworks.go.config.materials.PackageMaterial;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.packagematerial.PackageMaterialRevision;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.plugin.access.packagematerial.PackageRepositoryExtension;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.util.json.JsonHelper;
import net.javacrumbs.jsonunit.fluent.JsonFluentAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.thoughtworks.go.domain.packagerepository.PackageDefinitionMother.create;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PackageMaterialPollerTest {

    private PackageMaterial material;
    private PackageRepositoryExtension packageRepositoryExtension;
    private ArgumentCaptor<com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration> packageConfiguration;
    private ArgumentCaptor<RepositoryConfiguration> repositoryConfiguration;
    private com.thoughtworks.go.server.service.materials.PackageMaterialPoller poller;

    @BeforeEach
    void setup() {
        //setup material
        material = new PackageMaterial();
        PackageRepository packageRepository = PackageRepositoryMother.create("id", "name", "plugin-id", "plugin-version",
                new Configuration(ConfigurationPropertyMother.create("url", false, "http://some-url")));
        PackageDefinition packageDefinition = create("id", "package",
                new Configuration(ConfigurationPropertyMother.create("name", false, "go-agent"), ConfigurationPropertyMother.create("secure", true, "value")), packageRepository);
        material.setPackageDefinition(packageDefinition);

        packageRepositoryExtension = mock(PackageRepositoryExtension.class);


        poller = new com.thoughtworks.go.server.service.materials.PackageMaterialPoller(packageRepositoryExtension);

        packageConfiguration = ArgumentCaptor.forClass(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class);
        repositoryConfiguration = ArgumentCaptor.forClass(RepositoryConfiguration.class);

    }

    @Test
    void shouldGetLatestModificationsAlongWithAdditionalDataFromThePackageRevision() {
        Date timestamp = new Date();

        PackageRevision packageRevision = new PackageRevision("revision-123", timestamp, "user");
        String dataKey = "extra_data";
        String dataValue = "value";
        packageRevision.addData(dataKey, dataValue);
        when(packageRepositoryExtension.getLatestRevision(eq(material.getPluginId()), packageConfiguration.capture(), repositoryConfiguration.capture())).thenReturn(packageRevision);

        HashMap<String, String> expected = new HashMap<>();
        expected.put(dataKey, dataValue);

        List<Modification> modifications = poller.latestModification(material, null, null);

        assertThat(modifications.get(0).getRevision()).isEqualTo("revision-123");
        assertThat(modifications.get(0).getModifiedTime()).isEqualTo(timestamp);
        assertThat(modifications.get(0).getUserName()).isEqualTo("user");
        assertThat(modifications.get(0).getComment()).isNotNull();
        assertThat(modifications.get(0).getAdditionalData()).isEqualTo(JsonHelper.toJsonString(expected));
        assertConfiguration(packageConfiguration.getValue(), material.getPackageDefinition().getConfiguration());
        assertConfiguration(repositoryConfiguration.getValue(), material.getPackageDefinition().getRepository().getConfiguration());
    }

    private void assertConfiguration(com.thoughtworks.go.plugin.api.config.Configuration configurationsSentToPlugin, Configuration configurationInMaterial) {
        assertThat(configurationsSentToPlugin.size()).isEqualTo(configurationInMaterial.size());
        for (ConfigurationProperty property : configurationInMaterial) {
            Property configuration = configurationsSentToPlugin.get(property.getConfigurationKey().getName());
            assertThat(configuration.getValue()).isEqualTo(property.getValue());
        }
    }

    @Test
    void shouldGetModificationsSinceAGivenRevisionAlongWithAdditionalDataFromThePackageRevision() {
        String previousRevision = "rev-122";
        Date timestamp = new Date();
        HashMap<String, String> dataInPreviousRevision = new HashMap<>();
        dataInPreviousRevision.put("1", "one");
        PackageMaterialRevision knownRevision = new PackageMaterialRevision(previousRevision, timestamp, dataInPreviousRevision);
        ArgumentCaptor<PackageRevision> knownPackageRevision = ArgumentCaptor.forClass(PackageRevision.class);

        PackageRevision latestRevision = new PackageRevision("rev-123", timestamp, "user");
        String dataKey = "2";
        String dataValue = "two";
        latestRevision.addData(dataKey, dataValue);

        when(packageRepositoryExtension.latestModificationSince(eq(material.getPluginId()), packageConfiguration.capture(), repositoryConfiguration.capture(), knownPackageRevision.capture())).thenReturn(latestRevision);

        List<Modification> modifications = poller.modificationsSince(material, null, knownRevision, null);

        assertThat(knownPackageRevision.getValue().getRevision()).isEqualTo(previousRevision);
        assertThat(knownPackageRevision.getValue().getTimestamp()).isEqualTo(timestamp);
        assertThat(knownPackageRevision.getValue().getData()).isNotNull();
        assertThat(knownPackageRevision.getValue().getData().size()).isEqualTo(dataInPreviousRevision.size());
        assertThat(knownPackageRevision.getValue().getData().get("1")).isEqualTo(dataInPreviousRevision.get("1"));

        HashMap<String, String> expected = new HashMap<>();
        expected.put(dataKey, dataValue);
        String expectedDataString = JsonHelper.toJsonString(expected);

        Modification firstModification = modifications.get(0);
        assertThat(firstModification.getRevision()).isEqualTo("rev-123");
        assertThat(firstModification.getModifiedTime()).isEqualTo(timestamp);
        assertThat(firstModification.getUserName()).isEqualTo("user");
        assertThat(firstModification.getComment()).isNotNull();
        assertThat(firstModification.getAdditionalData()).isEqualTo(expectedDataString);
    }

    @Test
    void shouldTalkToPlugInToGetLatestModifications() {
        Date timestamp = new Date();

        when(packageRepositoryExtension.getLatestRevision(eq(material.getPluginId()), packageConfiguration.capture(), repositoryConfiguration.capture())).thenReturn(new PackageRevision("revision-123", timestamp, "user"));


        List<Modification> modifications = poller.latestModification(material, null, null);

        assertThat(modifications.get(0).getRevision()).isEqualTo("revision-123");
        assertThat(modifications.get(0).getModifiedTime()).isEqualTo(timestamp);
        assertThat(modifications.get(0).getUserName()).isEqualTo("user");
        assertThat(modifications.get(0).getComment()).isNotNull();
        assertConfiguration(packageConfiguration.getValue(), "name", "go-agent");
        assertConfiguration(repositoryConfiguration.getValue(), "url", "http://some-url");

    }

    @Test
    void shouldReturnEmptyModificationWhenPackageRevisionIsNullForLatestModification() {
        when(packageRepositoryExtension.getLatestRevision(eq(material.getPluginId()), packageConfiguration.capture(), repositoryConfiguration.capture())).thenReturn(null);
        List<Modification> modifications = poller.latestModification(material, null, null);
        assertThat(modifications).isNotNull();
        assertThat(modifications.isEmpty()).isTrue();
    }

    @Test
    void shouldTalkToPlugInToGetModificationsSinceAGivenRevision() {
        Date timestamp = new Date();
        PackageMaterialRevision knownRevision = new PackageMaterialRevision("rev-122", timestamp);
        ArgumentCaptor<PackageRevision> knownPackageRevision = ArgumentCaptor.forClass(PackageRevision.class);
        PackageRevision latestRevision = new PackageRevision("rev-123", timestamp, "user");

        when(packageRepositoryExtension.latestModificationSince(eq(material.getPluginId()), packageConfiguration.capture(), repositoryConfiguration.capture(), knownPackageRevision.capture())).thenReturn(latestRevision);

        List<Modification> modifications = poller.modificationsSince(material, null, knownRevision, null);

        assertThat(modifications.get(0).getRevision()).isEqualTo("rev-123");
        assertThat(modifications.get(0).getModifiedTime()).isEqualTo(timestamp);
        assertThat(modifications.get(0).getUserName()).isEqualTo("user");
        assertThat(modifications.get(0).getComment()).isNotNull();
        assertConfiguration(packageConfiguration.getValue(), "name", "go-agent");
        assertConfiguration(repositoryConfiguration.getValue(), "url", "http://some-url");
        assertThat(knownPackageRevision.getValue().getRevision()).isEqualTo("rev-122");
        assertThat(knownPackageRevision.getValue().getTimestamp()).isEqualTo(timestamp);

    }

    @Test
    void shouldReturnEmptyModificationWhenPackageRevisionIsNullForLatestModificationSince() {
        PackageMaterialRevision knownRevision = new PackageMaterialRevision("rev-122", new Date());
        ArgumentCaptor<PackageRevision> knownPackageRevision = ArgumentCaptor.forClass(PackageRevision.class);
        when(packageRepositoryExtension.latestModificationSince(eq(material.getPluginId()), packageConfiguration.capture(), repositoryConfiguration.capture(), knownPackageRevision.capture())).thenReturn(null);
        List<Modification> modifications = poller.modificationsSince(material, null, knownRevision, null);
        assertThat(modifications).isNotNull();
        assertThat(modifications.isEmpty()).isTrue();
    }

    @Test
    void shouldPopulatePackageModificationComment_WithTrackbackUrlAndComment() throws Exception {
        PackageRevision packageRevision = new PackageRevision(null, null, null, "Built on host1", "http://google.com");
        PackageMaterial packageMaterial = MaterialsMother.packageMaterial();
        when(packageRepositoryExtension.getLatestRevision(eq(packageMaterial.getPluginId()), any(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class), any(RepositoryConfiguration.class))).thenReturn(packageRevision);

        List<Modification> modifications = poller.latestModification(packageMaterial, null, null);
        JsonFluentAssert.assertThatJson(modifications.get(0).getComment()).isEqualTo("{\"COMMENT\":\"Built on host1\",\"TRACKBACK_URL\":\"http://google.com\",\"TYPE\":\"PACKAGE_MATERIAL\"}");
    }

    @Test
    void shouldPopulatePackageModificationComment_WithTrackbackUrl_ForModificationsSince() throws Exception {
        PackageRevision packageRevision = new PackageRevision(null, null, null, "some comment", "http://google.com");
        PackageMaterialRevision previousRevision = new PackageMaterialRevision("rev", new Date());
        PackageMaterial packageMaterial = MaterialsMother.packageMaterial();
        when(packageRepositoryExtension.latestModificationSince(eq(packageMaterial.getPluginId()), any(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class), any(RepositoryConfiguration.class), any(PackageRevision.class))).thenReturn(packageRevision);

        List<Modification> modifications = poller.modificationsSince(packageMaterial, null, previousRevision, null);
        JsonFluentAssert.assertThatJson(modifications.get(0).getComment()).isEqualTo("{\"COMMENT\":\"some comment\",\"TRACKBACK_URL\":\"http://google.com\",\"TYPE\":\"PACKAGE_MATERIAL\"}");
    }

    @Nested
    class ShouldReplaceSecrets {
        @Test
        void forLatestModification() {
            material.getPackageDefinition().getConfiguration().addNewConfigurationWithValue("abc", "{{SECRET:[secret_config_id][lookup_password]}}", false);
            material.getPackageDefinition().getConfiguration().getProperty("abc").getSecretParams().get(0).setValue("some-password");
            material.getPackageDefinition().getRepository().getConfiguration().addNewConfigurationWithValue("xyz", "{{SECRET:[secret_config_id][lookup_token]}}", false);
            material.getPackageDefinition().getRepository().getConfiguration().getProperty("xyz").getSecretParams().get(0).setValue("some-token");
            Date timestamp = new Date();

            PackageRevision packageRevision = new PackageRevision("revision-123", timestamp, "user");
            String dataKey = "extra_data";
            String dataValue = "value";
            packageRevision.addData(dataKey, dataValue);
            when(packageRepositoryExtension.getLatestRevision(eq(material.getPluginId()), packageConfiguration.capture(), repositoryConfiguration.capture())).thenReturn(packageRevision);

            List<Modification> modifications = poller.latestModification(material, null, null);

            assertConfiguration(packageConfiguration.getValue(), "abc", "some-password");
            assertConfiguration(repositoryConfiguration.getValue(), "xyz", "some-token");
        }

        @Test
        void forModificationsSince() {
            material.getPackageDefinition().getConfiguration().addNewConfigurationWithValue("abc", "{{SECRET:[secret_config_id][lookup_password]}}", false);
            material.getPackageDefinition().getConfiguration().getProperty("abc").getSecretParams().get(0).setValue("some-password");
            material.getPackageDefinition().getRepository().getConfiguration().addNewConfigurationWithValue("xyz", "{{SECRET:[secret_config_id][lookup_token]}}", false);
            material.getPackageDefinition().getRepository().getConfiguration().getProperty("xyz").getSecretParams().get(0).setValue("some-token");

            String previousRevision = "rev-122";
            Date timestamp = new Date();
            HashMap<String, String> dataInPreviousRevision = new HashMap<>();
            dataInPreviousRevision.put("1", "one");
            PackageMaterialRevision knownRevision = new PackageMaterialRevision(previousRevision, timestamp, dataInPreviousRevision);

            PackageRevision packageRevision = new PackageRevision("revision-123", timestamp, "user");
            String dataKey = "extra_data";
            String dataValue = "value";
            packageRevision.addData(dataKey, dataValue);
            when(packageRepositoryExtension.latestModificationSince(eq(material.getPluginId()), packageConfiguration.capture(), repositoryConfiguration.capture(), any(PackageRevision.class))).thenReturn(packageRevision);

            List<Modification> modifications = poller.modificationsSince(material, null, knownRevision, null);

            assertConfiguration(packageConfiguration.getValue(), "abc", "some-password");
            assertConfiguration(repositoryConfiguration.getValue(), "xyz", "some-token");
        }
    }

    private void assertConfiguration(com.thoughtworks.go.plugin.api.config.Configuration actualPackageConfiguration, String key, String value) {
        assertThat(actualPackageConfiguration.get(key)).isNotNull();
        assertThat(actualPackageConfiguration.get(key).getValue()).isEqualTo(value);
    }

}
