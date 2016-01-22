/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

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
import com.thoughtworks.go.plugin.access.packagematerial.PackageAsRepositoryExtension;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageRevision;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.util.json.JsonHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static com.thoughtworks.go.domain.packagerepository.PackageDefinitionMother.create;
import static net.javacrumbs.jsonunit.JsonMatchers.jsonEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PackageMaterialPollerTest {

    private PackageMaterial material;
    private PackageAsRepositoryExtension packageAsRepositoryExtension;
    private ArgumentCaptor<com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration> packageConfiguration;
    private ArgumentCaptor<RepositoryConfiguration> repositoryConfiguration;
    private com.thoughtworks.go.server.service.materials.PackageMaterialPoller poller;

    @Before
    public void setup() {
        //setup material
        material = new PackageMaterial();
        PackageRepository packageRepository = PackageRepositoryMother.create("id", "name", "plugin-id", "plugin-version",
                new Configuration(ConfigurationPropertyMother.create("url", false, "http://some-url")));
        PackageDefinition packageDefinition = create("id", "package",
                new Configuration(ConfigurationPropertyMother.create("name", false, "go-agent"), ConfigurationPropertyMother.create("secure", true, "value")), packageRepository);
        material.setPackageDefinition(packageDefinition);

        packageAsRepositoryExtension = mock(PackageAsRepositoryExtension.class);


        poller = new com.thoughtworks.go.server.service.materials.PackageMaterialPoller(packageAsRepositoryExtension);

        packageConfiguration = ArgumentCaptor.forClass(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class);
        repositoryConfiguration = ArgumentCaptor.forClass(RepositoryConfiguration.class);

    }

    @Test
    public void shouldGetLatestModificationsAlongWithAdditionalDataFromThePackageRevision() {
        Date timestamp = new Date();

        PackageRevision packageRevision = new PackageRevision("revision-123", timestamp, "user");
        String dataKey = "extra_data";
        String dataValue = "value";
        packageRevision.addData(dataKey, dataValue);
        when(packageAsRepositoryExtension.getLatestRevision(eq(material.getPluginId()), packageConfiguration.capture(), repositoryConfiguration.capture())).thenReturn(packageRevision);

        HashMap<String, String> expected = new HashMap<String, String>();
        expected.put(dataKey, dataValue);

        List<Modification> modifications = poller.latestModification(material, null, null);

        assertThat(modifications.get(0).getRevision(), is("revision-123"));
        assertThat(modifications.get(0).getModifiedTime(), is(timestamp));
        assertThat(modifications.get(0).getUserName(), is("user"));
        assertThat(modifications.get(0).getComment(), is(notNullValue()));
        assertThat(modifications.get(0).getAdditionalData(), is(JsonHelper.toJsonString(expected)));
        assertConfiguration(packageConfiguration.getValue(), material.getPackageDefinition().getConfiguration());
        assertConfiguration(repositoryConfiguration.getValue(), material.getPackageDefinition().getRepository().getConfiguration());
    }

    private void assertConfiguration(com.thoughtworks.go.plugin.api.config.Configuration configurationsSentToPlugin, Configuration configurationInMaterial) {
        assertThat(configurationsSentToPlugin.size(), is(configurationInMaterial.size()));
        for (ConfigurationProperty property : configurationInMaterial) {
            Property configuration = configurationsSentToPlugin.get(property.getConfigurationKey().getName());
            assertThat(configuration.getValue(), is(property.getValue()));
        }
    }

    @Test
    public void shouldGetModificationsSinceAGivenRevisionAlongWithAdditionalDataFromThePackageRevision() {
        String previousRevision = "rev-122";
        Date timestamp = new Date();
        HashMap<String, String> dataInPreviousRevision = new HashMap<String, String>();
        dataInPreviousRevision.put("1", "one");
        PackageMaterialRevision knownRevision = new PackageMaterialRevision(previousRevision, timestamp, dataInPreviousRevision);
        ArgumentCaptor<PackageRevision> knownPackageRevision = ArgumentCaptor.forClass(PackageRevision.class);

        PackageRevision latestRevision = new PackageRevision("rev-123", timestamp, "user");
        String dataKey = "2";
        String dataValue = "two";
        latestRevision.addData(dataKey, dataValue);

        when(packageAsRepositoryExtension.latestModificationSince(eq(material.getPluginId()), packageConfiguration.capture(), repositoryConfiguration.capture(), knownPackageRevision.capture())).thenReturn(latestRevision);

        List<Modification> modifications = poller.modificationsSince(material, null, knownRevision, null);

        assertThat(knownPackageRevision.getValue().getRevision(), is(previousRevision));
        assertThat(knownPackageRevision.getValue().getTimestamp(), is(timestamp));
        assertThat(knownPackageRevision.getValue().getData(), is(notNullValue()));
        assertThat(knownPackageRevision.getValue().getData().size(), is(dataInPreviousRevision.size()));
        assertThat(knownPackageRevision.getValue().getData().get("1"), is(dataInPreviousRevision.get("1")));

        HashMap<String, String> expected = new HashMap<String, String>();
        expected.put(dataKey, dataValue);
        String expectedDataString = JsonHelper.toJsonString(expected);

        Modification firstModification = modifications.get(0);
        assertThat(firstModification.getRevision(), is("rev-123"));
        assertThat(firstModification.getModifiedTime(), is(timestamp));
        assertThat(firstModification.getUserName(), is("user"));
        assertThat(firstModification.getComment(), is(notNullValue()));
        assertThat(firstModification.getAdditionalData(), is(expectedDataString));
    }

    @Test
    public void shouldTalkToPlugInToGetLatestModifications() {
        Date timestamp = new Date();

        when(packageAsRepositoryExtension.getLatestRevision(eq(material.getPluginId()), packageConfiguration.capture(), repositoryConfiguration.capture())).thenReturn(new PackageRevision("revision-123", timestamp, "user"));


        List<Modification> modifications = poller.latestModification(material, null, null);

        assertThat(modifications.get(0).getRevision(), is("revision-123"));
        assertThat(modifications.get(0).getModifiedTime(), is(timestamp));
        assertThat(modifications.get(0).getUserName(), is("user"));
        assertThat(modifications.get(0).getComment(), is(notNullValue()));
        assertConfiguration(packageConfiguration.getValue(), "name", "go-agent");
        assertConfiguration(repositoryConfiguration.getValue(), "url", "http://some-url");

    }

    @Test
    public void shouldReturnEmptyModificationWhenPackageRevisionIsNullForLatestModification() {
        when(packageAsRepositoryExtension.getLatestRevision(eq(material.getPluginId()), packageConfiguration.capture(), repositoryConfiguration.capture())).thenReturn(null);
        List<Modification> modifications = poller.latestModification(material, null, null);
        assertThat(modifications, is(notNullValue()));
        assertThat(modifications.isEmpty(), is(true));
    }

    @Test
    public void shouldTalkToPlugInToGetModificationsSinceAGivenRevision() {
        Date timestamp = new Date();
        PackageMaterialRevision knownRevision = new PackageMaterialRevision("rev-122", timestamp);
        ArgumentCaptor<PackageRevision> knownPackageRevision = ArgumentCaptor.forClass(PackageRevision.class);
        PackageRevision latestRevision = new PackageRevision("rev-123", timestamp, "user");

        when(packageAsRepositoryExtension.latestModificationSince(eq(material.getPluginId()), packageConfiguration.capture(), repositoryConfiguration.capture(), knownPackageRevision.capture())).thenReturn(latestRevision);

        List<Modification> modifications = poller.modificationsSince(material, null, knownRevision, null);

        assertThat(modifications.get(0).getRevision(), is("rev-123"));
        assertThat(modifications.get(0).getModifiedTime(), is(timestamp));
        assertThat(modifications.get(0).getUserName(), is("user"));
        assertThat(modifications.get(0).getComment(), is(notNullValue()));
        assertConfiguration(packageConfiguration.getValue(), "name", "go-agent");
        assertConfiguration(repositoryConfiguration.getValue(), "url", "http://some-url");
        assertThat(knownPackageRevision.getValue().getRevision(), is("rev-122"));
        assertThat(knownPackageRevision.getValue().getTimestamp(), is(timestamp));

    }

    @Test
    public void shouldReturnEmptyModificationWhenPackageRevisionIsNullForLatestModificationSince() {
        PackageMaterialRevision knownRevision = new PackageMaterialRevision("rev-122", new Date());
        ArgumentCaptor<PackageRevision> knownPackageRevision = ArgumentCaptor.forClass(PackageRevision.class);
        when(packageAsRepositoryExtension.latestModificationSince(eq(material.getPluginId()), packageConfiguration.capture(), repositoryConfiguration.capture(), knownPackageRevision.capture())).thenReturn(null);
        List<Modification> modifications = poller.modificationsSince(material, null, knownRevision, null);
        assertThat(modifications, is(notNullValue()));
        assertThat(modifications.isEmpty(), is(true));
    }

    @Test
    public void shouldPopulatePackageModificationComment_WithTrackbackUrlAndComment() throws Exception {
        PackageRevision packageRevision = new PackageRevision(null, null, null, "Built on host1", "http://google.com");
        PackageMaterial packageMaterial = MaterialsMother.packageMaterial();
        when(packageAsRepositoryExtension.getLatestRevision(eq(packageMaterial.getPluginId()), any(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class), any(RepositoryConfiguration.class))).thenReturn(packageRevision);

        List<Modification> modifications = poller.latestModification(packageMaterial, null, null);
        assertThat(modifications.get(0).getComment(), jsonEquals("{\"COMMENT\":\"Built on host1\",\"TRACKBACK_URL\":\"http://google.com\",\"TYPE\":\"PACKAGE_MATERIAL\"}"));
    }

    @Test
    public void shouldPopulatePackageModificationComment_WithTrackbackUrl_ForModificationsSince() throws Exception {
        PackageRevision packageRevision = new PackageRevision(null, null, null, "some comment", "http://google.com");
        PackageMaterialRevision previousRevision = new PackageMaterialRevision("rev", new Date());
        PackageMaterial packageMaterial = MaterialsMother.packageMaterial();
        when(packageAsRepositoryExtension.latestModificationSince(eq(packageMaterial.getPluginId()), any(com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration.class), any(RepositoryConfiguration.class), any(PackageRevision.class))).thenReturn(packageRevision);

        List<Modification> modifications = poller.modificationsSince(packageMaterial, null, previousRevision, null);
        assertThat(modifications.get(0).getComment(), jsonEquals("{\"COMMENT\":\"some comment\",\"TRACKBACK_URL\":\"http://google.com\",\"TYPE\":\"PACKAGE_MATERIAL\"}"));
    }

    private void assertConfiguration(com.thoughtworks.go.plugin.api.config.Configuration actualPackageConfiguration, String key, String value) {
        assertThat(actualPackageConfiguration.get(key), is(notNullValue()));
        assertThat(actualPackageConfiguration.get(key).getValue(), is(value));
    }

}
