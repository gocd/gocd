/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.scm.PluggableSCMMaterialRevision;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.plugin.access.scm.SCMExtension;
import com.thoughtworks.go.plugin.access.scm.SCMPropertyConfiguration;
import com.thoughtworks.go.plugin.access.scm.material.MaterialPollResult;
import com.thoughtworks.go.plugin.access.scm.revision.ModifiedAction;
import com.thoughtworks.go.plugin.access.scm.revision.ModifiedFile;
import com.thoughtworks.go.plugin.access.scm.revision.SCMRevision;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.json.JsonHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.io.File;
import java.util.*;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class PluggableSCMMaterialPollerTest {
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private SCMExtension scmExtension;
    @Mock
    private TransactionTemplate transactionTemplate;
    private PluggableSCMMaterial material;
    private ArgumentCaptor<SCMPropertyConfiguration> scmConfiguration;
    private ArgumentCaptor<Map> materialData;
    private PluggableSCMMaterialPoller poller;
    private String flyweightFolderPath;

    @Before
    public void setup() {
        initMocks(this);
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
        ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", true, "v2");
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "plugin-id", "1.0", new Configuration(k1, k2));
        material = new PluggableSCMMaterial();
        material.setSCMConfig(scmConfig);

        MaterialInstance materialInstance = material.createMaterialInstance();
        Map<String, String> data = new HashMap<>();
        data.put("mk-1", "mv-1");
        materialInstance.setAdditionalData(JsonHelper.toJsonString(data));
        when(materialRepository.findMaterialInstance(material)).thenReturn(materialInstance);

        poller = new PluggableSCMMaterialPoller(materialRepository, scmExtension, transactionTemplate);

        scmConfiguration = ArgumentCaptor.forClass(SCMPropertyConfiguration.class);
        materialData = ArgumentCaptor.forClass(Map.class);

        flyweightFolderPath = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
    }

    @Test
    public void shouldTalkToPlugInToGetLatestModifications() {
        Date timestamp = new Date();
        SCMRevision scmRevision = new SCMRevision("revision-123", timestamp, "user", "comment", null, null);
        MaterialPollResult materialPollResult = new MaterialPollResult(null, scmRevision);
        when(scmExtension.getLatestRevision(eq(material.getPluginId()), scmConfiguration.capture(), materialData.capture(), eq(flyweightFolderPath))).thenReturn(materialPollResult);

        List<Modification> modifications = poller.latestModification(material, new File(flyweightFolderPath), null);

        assertThat(modifications.get(0).getRevision(), is("revision-123"));
        assertThat(modifications.get(0).getModifiedTime(), is(timestamp));
        assertThat(modifications.get(0).getUserName(), is("user"));
        assertThat(modifications.get(0).getComment(), is("comment"));
        assertConfiguration(scmConfiguration.getValue(), "k1", "v1");
        assertConfiguration(scmConfiguration.getValue(), "k2", "v2");
        assertThat(materialData.getValue().size(), is(1));
        assertThat(materialData.getValue().get("mk-1"), is("mv-1"));
    }

    @Test
    public void shouldReturnEmptyModificationWhenSCMRevisionIsNull_latestModification() {
        when(scmExtension.getLatestRevision(eq(material.getPluginId()), scmConfiguration.capture(), materialData.capture(), eq(flyweightFolderPath))).thenReturn(new MaterialPollResult());

        List<Modification> modifications = poller.latestModification(material, new File(flyweightFolderPath), null);

        assertThat(modifications, is(notNullValue()));
        assertThat(modifications.isEmpty(), is(true));
    }

    @Test
    public void shouldGetLatestModificationAlongWithAdditionalDataFromTheSCMRevision() {
        Date timestamp = new Date();
        Map<String, String> data = new HashMap<>();
        String dataKey = "revision_data";
        String dataValue = "revision_value";
        data.put(dataKey, dataValue);
        List<ModifiedFile> modifiedFiles = new ArrayList<>(asList(new ModifiedFile("f1", ModifiedAction.added), new ModifiedFile("f2", ModifiedAction.modified), new ModifiedFile("f3", ModifiedAction.deleted)));
        SCMRevision scmRevision = new SCMRevision("revision-123", timestamp, "user", "comment", data, modifiedFiles);
        MaterialPollResult materialPollResult = new MaterialPollResult(null, scmRevision);
        when(scmExtension.getLatestRevision(eq(material.getPluginId()), scmConfiguration.capture(), materialData.capture(), eq(flyweightFolderPath))).thenReturn(materialPollResult);

        List<Modification> modifications = poller.latestModification(material, new File(flyweightFolderPath), null);

        assertThat(modifications.get(0).getRevision(), is("revision-123"));
        assertThat(modifications.get(0).getModifiedTime(), is(timestamp));
        assertThat(modifications.get(0).getUserName(), is("user"));
        assertThat(modifications.get(0).getComment(), is("comment"));
        assertThat(modifications.get(0).getAdditionalData(), is(JsonHelper.toJsonString(data)));
        assertThat(modifications.get(0).getModifiedFiles().size(), is(3));
        com.thoughtworks.go.domain.materials.ModifiedFile f1 = new com.thoughtworks.go.domain.materials.ModifiedFile("f1", null, com.thoughtworks.go.domain.materials.ModifiedAction.added);
        com.thoughtworks.go.domain.materials.ModifiedFile f2 = new com.thoughtworks.go.domain.materials.ModifiedFile("f2", null, com.thoughtworks.go.domain.materials.ModifiedAction.modified);
        com.thoughtworks.go.domain.materials.ModifiedFile f3 = new com.thoughtworks.go.domain.materials.ModifiedFile("f3", null, com.thoughtworks.go.domain.materials.ModifiedAction.deleted);
        assertThat(new HashSet(modifications.get(0).getModifiedFiles()), is(new HashSet(asList(f1, f2, f3))));
        assertConfiguration(scmConfiguration.getValue(), material.getScmConfig().getConfiguration());
        assertThat(materialData.getValue().size(), is(1));
        assertThat(materialData.getValue().get("mk-1"), is("mv-1"));
    }

    @Test
    public void shouldTalkToPlugInToGetModificationsSinceAGivenRevision() {
        Date timestamp = new Date();
        PluggableSCMMaterialRevision knownRevision = new PluggableSCMMaterialRevision("rev-122", timestamp);
        ArgumentCaptor<SCMRevision> knownSCMRevision = ArgumentCaptor.forClass(SCMRevision.class);
        SCMRevision latestRevision = new SCMRevision("rev-123", timestamp, "user", null, null, null);
        MaterialPollResult materialPollResult = new MaterialPollResult(null, asList(latestRevision));
        when(scmExtension.latestModificationSince(eq(material.getPluginId()), scmConfiguration.capture(), materialData.capture(), eq(flyweightFolderPath), knownSCMRevision.capture())).thenReturn(materialPollResult);

        List<Modification> modifications = poller.modificationsSince(material, new File(flyweightFolderPath), knownRevision, null);

        assertThat(modifications.get(0).getRevision(), is("rev-123"));
        assertThat(modifications.get(0).getModifiedTime(), is(timestamp));
        assertThat(modifications.get(0).getUserName(), is("user"));
        assertThat(modifications.get(0).getComment(), is(nullValue()));
        assertConfiguration(scmConfiguration.getValue(), "k1", "v1");
        assertConfiguration(scmConfiguration.getValue(), "k2", "v2");
        assertThat(knownSCMRevision.getValue().getRevision(), is("rev-122"));
        assertThat(knownSCMRevision.getValue().getTimestamp(), is(timestamp));
        assertThat(materialData.getValue().size(), is(1));
        assertThat(materialData.getValue().get("mk-1"), is("mv-1"));
    }

    @Test
    public void shouldReturnEmptyModificationWhenSCMRevisionIsNullFor_latestModificationSince() {
        PluggableSCMMaterialRevision knownRevision = new PluggableSCMMaterialRevision("rev-122", new Date());
        ArgumentCaptor<SCMRevision> knownSCMRevision = ArgumentCaptor.forClass(SCMRevision.class);
        when(scmExtension.latestModificationSince(eq(material.getPluginId()), scmConfiguration.capture(), materialData.capture(), eq(flyweightFolderPath), knownSCMRevision.capture())).thenReturn(new MaterialPollResult());

        List<Modification> modifications = poller.modificationsSince(material, new File(flyweightFolderPath), knownRevision, null);

        assertThat(modifications, is(notNullValue()));
        assertThat(modifications.isEmpty(), is(true));
    }

    @Test
    public void shouldGetModificationsSinceAGivenRevisionAlongWithAdditionalDataFromTheSCMRevision() {
        String previousRevision = "rev-122";
        Date timestamp = new Date();
        Map<String, String> dataInPreviousRevision = new HashMap<>();
        dataInPreviousRevision.put("1", "one");
        PluggableSCMMaterialRevision knownRevision = new PluggableSCMMaterialRevision(previousRevision, timestamp, dataInPreviousRevision);
        ArgumentCaptor<SCMRevision> knownSCMRevision = ArgumentCaptor.forClass(SCMRevision.class);

        Map<String, String> data = new HashMap<>();
        String dataKey = "2";
        String dataValue = "two";
        data.put(dataKey, dataValue);
        SCMRevision latestRevision = new SCMRevision("rev-123", timestamp, "user", "comment-123", data, null);

        MaterialPollResult materialPollResult = new MaterialPollResult(null, asList(latestRevision));
        when(scmExtension.latestModificationSince(eq(material.getPluginId()), scmConfiguration.capture(), materialData.capture(), eq(flyweightFolderPath), knownSCMRevision.capture())).thenReturn(materialPollResult);

        List<Modification> modifications = poller.modificationsSince(material, new File(flyweightFolderPath), knownRevision, null);

        assertThat(knownSCMRevision.getValue().getRevision(), is(previousRevision));
        assertThat(knownSCMRevision.getValue().getTimestamp(), is(timestamp));
        assertThat(knownSCMRevision.getValue().getData(), is(notNullValue()));
        assertThat(knownSCMRevision.getValue().getData().size(), is(dataInPreviousRevision.size()));
        assertThat(knownSCMRevision.getValue().getData().get("1"), is(dataInPreviousRevision.get("1")));

        HashMap<String, String> expected = new HashMap<>();
        expected.put(dataKey, dataValue);

        Modification firstModification = modifications.get(0);
        assertThat(firstModification.getRevision(), is("rev-123"));
        assertThat(firstModification.getModifiedTime(), is(timestamp));
        assertThat(firstModification.getUserName(), is("user"));
        assertThat(firstModification.getComment(), is("comment-123"));
        assertThat(firstModification.getAdditionalData(), is(JsonHelper.toJsonString(expected)));
        assertThat(firstModification.getModifiedFiles().isEmpty(), is(true));
        assertThat(materialData.getValue().size(), is(1));
        assertThat(materialData.getValue().get("mk-1"), is("mv-1"));
    }

    private void assertConfiguration(com.thoughtworks.go.plugin.api.config.Configuration configurationsSentToPlugin, Configuration configurationInMaterial) {
        assertThat(configurationsSentToPlugin.size(), is(configurationInMaterial.size()));
        for (ConfigurationProperty property : configurationInMaterial) {
            Property configuration = configurationsSentToPlugin.get(property.getConfigurationKey().getName());
            assertThat(configuration.getValue(), is(property.getValue()));
        }
    }

    private void assertConfiguration(com.thoughtworks.go.plugin.api.config.Configuration actualSCMConfiguration, String key, String value) {
        assertThat(actualSCMConfiguration.get(key), is(notNullValue()));
        assertThat(actualSCMConfiguration.get(key).getValue(), is(value));
    }
}
