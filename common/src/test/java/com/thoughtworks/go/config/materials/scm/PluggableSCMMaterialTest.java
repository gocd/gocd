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
package com.thoughtworks.go.config.materials.scm;

import com.google.gson.Gson;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.SecretParam;
import com.thoughtworks.go.config.materials.PluggableSCMMaterial;
import com.thoughtworks.go.config.materials.PluggableSCMMaterialConfig;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.materials.MatchedRevision;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.materials.scm.PluggableSCMMaterialInstance;
import com.thoughtworks.go.domain.materials.scm.PluggableSCMMaterialRevision;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.plugin.access.scm.SCMConfigurations;
import com.thoughtworks.go.plugin.access.scm.SCMMetadataStore;
import com.thoughtworks.go.plugin.access.scm.SCMView;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.json.JsonHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static com.thoughtworks.go.util.command.EnvironmentVariableContext.EnvironmentVariable.MASK_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PluggableSCMMaterialTest {

    @BeforeEach
    void setUp() {
        SCMMetadataStore.getInstance().clear();
    }

    @AfterEach
    void tearDown() {
        SCMMetadataStore.getInstance().clear();
    }

    @Test
    void shouldCreatePluggableSCMMaterialInstance() {
        PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
        PluggableSCMMaterialInstance materialInstance = (PluggableSCMMaterialInstance) material.createMaterialInstance();

        assertThat(materialInstance).isNotNull();
        assertThat(materialInstance.getFlyweightName()).isNotNull();
        assertThat(materialInstance.getConfiguration()).isEqualTo(JsonHelper.toJsonString(material));
    }

    @Test
    void shouldGetMaterialInstanceType() {
        assertThat(new PluggableSCMMaterial().getInstanceType().equals(PluggableSCMMaterialInstance.class)).isTrue();
    }

    @Test
    void shouldGetSqlCriteria() {
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setSCMConfig(scmConfig);

        Map<String, Object> criteria = material.getSqlCriteria();
        assertThat(criteria.get("type")).isEqualTo(PluggableSCMMaterial.class.getSimpleName());
        assertThat(criteria.get("fingerprint")).isEqualTo(material.getFingerprint());
    }

    @Test
    void shouldGetFingerprintForMaterial() {
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
        ConfigurationProperty k2 = ConfigurationPropertyMother.create("secure-key", true, "secure-value");
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "pluginid", "version", new Configuration(k1, k2));
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setSCMConfig(scmConfig);

        assertThat(material.getFingerprint()).isEqualTo(CachedDigestUtils.sha256Hex("plugin-id=pluginid<|>k1=v1<|>secure-key=secure-value"));
    }

    @Test
    void shouldGetDifferentFingerprintWhenPluginIdChanges() {
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "plugin-1", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setSCMConfig(scmConfig);

        SCM anotherSCMConfig = SCMMother.create("scm-id", "scm-name", "plugin-2", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        PluggableSCMMaterial anotherMaterial = new PluggableSCMMaterial();
        anotherMaterial.setSCMConfig(anotherSCMConfig);

        assertThat(material.getFingerprint().equals(anotherMaterial.getFingerprint())).isFalse();
    }

    @Test
    void shouldGetDescription() {
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "pluginid", "version", new Configuration(k1));
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setSCMConfig(scmConfig);

        assertThat(material.getDescription()).isEqualTo("scm-name");
    }

    @Test
    void shouldGetDisplayName() {
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "pluginid", "version", new Configuration(k1));
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setSCMConfig(scmConfig);

        assertThat(material.getDisplayName()).isEqualTo("scm-name");
    }

    @Test
    void shouldTypeForDisplay() {
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        assertThat(material.getTypeForDisplay()).isEqualTo("SCM");
    }

    @Test
    void shouldGetAttributesForXml() {
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "pluginid", "version", new Configuration(k1));
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setSCMConfig(scmConfig);

        Map<String, Object> attributesForXml = material.getAttributesForXml();

        assertThat(attributesForXml.get("type").toString()).isEqualTo(PluggableSCMMaterial.class.getSimpleName());
        assertThat(attributesForXml.get("scmName").toString()).isEqualTo("scm-name");
    }

    @Test
    void shouldConvertPluggableSCMMaterialToJsonFormatToBeStoredInDb() throws CryptoException {
        GoCipher cipher = new GoCipher();
        String encryptedPassword = cipher.encrypt("password");
        ConfigurationProperty secureSCMProperty = new ConfigurationProperty(new ConfigurationKey("secure-key"), null, new EncryptedConfigurationValue(encryptedPassword), cipher);
        ConfigurationProperty scmProperty = new ConfigurationProperty(new ConfigurationKey("non-secure-key"), new ConfigurationValue("value"), null, cipher);
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "plugin-id", "1.0", new Configuration(secureSCMProperty, scmProperty));

        PluggableSCMMaterial pluggableSCMMaterial = new PluggableSCMMaterial();
        pluggableSCMMaterial.setSCMConfig(scmConfig);

        String json = JsonHelper.toJsonString(pluggableSCMMaterial);

        String expected = "{\"scm\":{\"plugin\":{\"id\":\"plugin-id\",\"version\":\"1.0\"},\"config\":[{\"configKey\":{\"name\":\"secure-key\"},\"encryptedConfigValue\":{\"value\":" + new Gson().toJson(encryptedPassword) + "}},{\"configKey\":{\"name\":\"non-secure-key\"},\"configValue\":{\"value\":\"value\"}}]}}";
        assertThat(json).isEqualTo(expected);
        assertThat(JsonHelper.fromJson(expected, PluggableSCMMaterial.class)).isEqualTo(pluggableSCMMaterial);
    }

    @Test
    void shouldGetJsonRepresentationForPluggableSCMMaterial() {
        ConfigurationProperty k1 = create("k1", false, "v1");
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "pluginid", "version", new Configuration(k1));
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setSCMConfig(scmConfig);
        material.setFolder("folder");
        Map<String, String> jsonMap = new LinkedHashMap<>();
        material.toJson(jsonMap, new PluggableSCMMaterialRevision("rev123", new Date()));

        assertThat(jsonMap.get("scmType")).isEqualTo("SCM");
        assertThat(jsonMap.get("materialName")).isEqualTo("scm-name");
        assertThat(jsonMap.get("location")).isEqualTo(material.getUriForDisplay());
        assertThat(jsonMap.get("folder")).isEqualTo("folder");
        assertThat(jsonMap.get("action")).isEqualTo("Modified");
    }

    @Test
    void shouldGetEmailContentForPluggableSCMMaterial() {
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "pluginid", "version", new Configuration(k1));
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setSCMConfig(scmConfig);

        StringBuilder content = new StringBuilder();
        Date date = new Date(1367472329111L);
        material.emailContent(content, new Modification(null, "comment", null, date, "rev123"));

        assertThat(content.toString()).isEqualTo(String.format("SCM : scm-name\nrevision: rev123, completed on %s\ncomment", date.toString()));
    }

    @Test
    void shouldReturnFalseForIsUsedInFetchArtifact() {
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        assertThat(material.isUsedInFetchArtifact(new PipelineConfig())).isFalse();
    }

    @Test
    void shouldReturnMatchedRevisionForPluggableSCMMaterial() {
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "pluginid", "version", new Configuration(k1));
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setSCMConfig(scmConfig);

        Date timestamp = new Date();
        MatchedRevision matchedRevision = material.createMatchedRevision(new Modification("go", "comment", null, timestamp, "rev123"), "rev");

        assertThat(matchedRevision.getShortRevision()).isEqualTo("rev123");
        assertThat(matchedRevision.getLongRevision()).isEqualTo("rev123");
        assertThat(matchedRevision.getCheckinTime()).isEqualTo(timestamp);
        assertThat(matchedRevision.getUser()).isEqualTo("go");
        assertThat(matchedRevision.getComment()).isEqualTo("comment");
    }

    @Test
    void shouldGetNameFromSCMName() {
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "pluginid", "version", new Configuration(k1));
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setSCMConfig(scmConfig);

        assertThat(material.getName().toString()).isEqualTo("scm-name");
    }

    @Test
    void shouldPopulateEnvironmentContext() {
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
        ConfigurationProperty k2 = ConfigurationPropertyMother.create("scm-secure", true, "value");
        SCM scmConfig = SCMMother.create("scm-id", "tw-dev", "pluginid", "version", new Configuration(k1, k2));
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setSCMConfig(scmConfig);
        material.setName(new CaseInsensitiveString("tw-dev:go-agent"));
        Modifications modifications = new Modifications(new Modification(null, null, null, new Date(), "revision-123"));
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
        material.populateEnvironmentContext(environmentVariableContext, new MaterialRevision(material, modifications), null);

        assertThat(environmentVariableContext.getProperty("GO_SCM_TW_DEV_GO_AGENT_K1")).isEqualTo("v1");
        assertThat(environmentVariableContext.getProperty("GO_SCM_TW_DEV_GO_AGENT_SCM_SECURE")).isEqualTo("value");
        assertThat(environmentVariableContext.getPropertyForDisplay("GO_SCM_TW_DEV_GO_AGENT_SCM_SECURE")).isEqualTo(MASK_VALUE);
        assertThat(environmentVariableContext.getProperty("GO_SCM_TW_DEV_GO_AGENT_LABEL")).isEqualTo("revision-123");
    }

    @Test
    void shouldPopulateEnvironmentContextWithEnvironmentVariablesCreatedOutOfAdditionalDataFromModification() {
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
        SCM scmConfig = SCMMother.create("scm-id", "tw-dev", "pluginid", "version", new Configuration(k1));
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setSCMConfig(scmConfig);
        material.setName(new CaseInsensitiveString("tw-dev:go-agent"));
        HashMap<String, String> map = new HashMap<>();
        map.put("MY_NEW_KEY", "my_value");
        Modification modification = new Modification("loser", "comment", "email", new Date(), "revision-123", JsonHelper.toJsonString(map));
        Modifications modifications = new Modifications(modification);

        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

        material.populateEnvironmentContext(environmentVariableContext, new MaterialRevision(material, modifications), null);

        assertThat(environmentVariableContext.getProperty("GO_SCM_TW_DEV_GO_AGENT_LABEL")).isEqualTo("revision-123");
        assertThat(environmentVariableContext.getProperty("GO_SCM_TW_DEV_GO_AGENT_K1")).isEqualTo("v1");
        assertThat(environmentVariableContext.getProperty("GO_SCM_TW_DEV_GO_AGENT_MY_NEW_KEY")).isEqualTo("my_value");
    }

    @Test
    void shouldMarkEnvironmentContextCreatedForAdditionalDataAsSecureIfTheValueContainsAnySpecialCharacters() throws UnsupportedEncodingException {
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
        ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", true, "!secure_value:with_special_chars");
        SCM scmConfig = SCMMother.create("scm-id", "tw-dev", "pluginid", "version", new Configuration(k1, k2));
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setSCMConfig(scmConfig);
        material.setName(new CaseInsensitiveString("tw-dev:go-agent"));
        HashMap<String, String> map = new HashMap<>();
        map.put("ADDITIONAL_DATA_ONE", "foobar:!secure_value:with_special_chars");
        map.put("ADDITIONAL_DATA_URL_ENCODED", "something:%21secure_value%3Awith_special_chars");
        map.put("ADDITIONAL_DATA_TWO", "foobar:secure_value_with_regular_chars");
        Modification modification = new Modification("loser", "comment", "email", new Date(), "revision-123", JsonHelper.toJsonString(map));
        Modifications modifications = new Modifications(modification);

        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

        material.populateEnvironmentContext(environmentVariableContext, new MaterialRevision(material, modifications), null);

        assertThat(environmentVariableContext.getProperty("GO_SCM_TW_DEV_GO_AGENT_LABEL")).isEqualTo("revision-123");
        assertThat(environmentVariableContext.getProperty("GO_SCM_TW_DEV_GO_AGENT_K1")).isEqualTo("v1");
        assertThat(environmentVariableContext.getProperty("GO_SCM_TW_DEV_GO_AGENT_K2")).isEqualTo("!secure_value:with_special_chars");
        assertThat(environmentVariableContext.getPropertyForDisplay("GO_SCM_TW_DEV_GO_AGENT_K2")).isEqualTo(MASK_VALUE);
        assertThat(environmentVariableContext.getProperty("GO_SCM_TW_DEV_GO_AGENT_ADDITIONAL_DATA_ONE")).isEqualTo("foobar:!secure_value:with_special_chars");
        assertThat(environmentVariableContext.getPropertyForDisplay("GO_SCM_TW_DEV_GO_AGENT_ADDITIONAL_DATA_ONE")).isEqualTo("foobar:!secure_value:with_special_chars");
        assertThat(environmentVariableContext.getPropertyForDisplay("GO_SCM_TW_DEV_GO_AGENT_ADDITIONAL_DATA_TWO")).isEqualTo("foobar:secure_value_with_regular_chars");
        assertThat(environmentVariableContext.getProperty("GO_SCM_TW_DEV_GO_AGENT_ADDITIONAL_DATA_URL_ENCODED")).isEqualTo("something:%21secure_value%3Awith_special_chars");
        assertThat(environmentVariableContext.getPropertyForDisplay("GO_SCM_TW_DEV_GO_AGENT_ADDITIONAL_DATA_URL_ENCODED")).isEqualTo(MASK_VALUE);
    }

    @Test
    void shouldNotThrowUpWhenAdditionalDataIsNull() {
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
        SCM scmConfig = SCMMother.create("scm-id", "tw-dev", "pluginid", "version", new Configuration(k1));
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setSCMConfig(scmConfig);
        material.setName(new CaseInsensitiveString("tw-dev:go-agent"));
        Modifications modifications = new Modifications(new Modification("loser", "comment", "email", new Date(), "revision-123", null));
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

        material.populateEnvironmentContext(environmentVariableContext, new MaterialRevision(material, modifications), null);

        assertThat(environmentVariableContext.getProperty("GO_SCM_TW_DEV_GO_AGENT_LABEL")).isEqualTo("revision-123");
        assertThat(environmentVariableContext.getProperty("GO_SCM_TW_DEV_GO_AGENT_K1")).isEqualTo("v1");
    }

    @Test
    void shouldNotThrowUpWhenAdditionalDataIsRandomJunkAndNotJSON() {
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "v1");
        SCM scmConfig = SCMMother.create("scm-id", "tw-dev", "pluginid", "version", new Configuration(k1));
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setSCMConfig(scmConfig);
        material.setName(new CaseInsensitiveString("tw-dev:go-agent"));
        Modifications modifications = new Modifications(new Modification("loser", "comment", "email", new Date(), "revision-123", "salkdfjdsa-jjgkj!!!vcxknbvkjk"));
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

        material.populateEnvironmentContext(environmentVariableContext, new MaterialRevision(material, modifications), null);

        assertThat(environmentVariableContext.getProperty("GO_SCM_TW_DEV_GO_AGENT_LABEL")).isEqualTo("revision-123");
        assertThat(environmentVariableContext.getProperty("GO_SCM_TW_DEV_GO_AGENT_K1")).isEqualTo("v1");
    }

    @Test
    void shouldGetUriForDisplay() {
        SCMMetadataStore.getInstance().addMetadataFor("some-plugin", new SCMConfigurations(), null);

        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "scm-v1");
        ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "scm-v2");
        Configuration configuration = new Configuration(k1, k2);
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "some-plugin", "version", configuration);
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setSCMConfig(scmConfig);

        assertThat(material.getUriForDisplay()).isEqualTo("[k1=scm-v1, k2=scm-v2]");
    }

    @Test
    void shouldGetUriForDisplayNameIfNameIsNull() {
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "scm-v1");
        ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "scm-v2");
        SCM scmConfig = SCMMother.create("scm-id", null, "pluginid", "version", new Configuration(k1, k2));
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setSCMConfig(scmConfig);

        assertThat(material.getDisplayName()).isEqualTo(material.getUriForDisplay());
    }

    @Test
    void shouldGetLongDescription() {
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "scm-v1");
        ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "scm-v2");
        Configuration configuration = new Configuration(k1, k2);
        SCM scmConfig = SCMMother.create("scm-id", "scm-name", "pluginid", "version", configuration);
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setSCMConfig(scmConfig);

        assertThat(material.getLongDescription()).isEqualTo(material.getUriForDisplay());
    }

    @Test
    void shouldPassEqualsCheckIfFingerprintIsSame() {
        PluggableSCMMaterial material1 = MaterialsMother.pluggableSCMMaterial();
        material1.setName(new CaseInsensitiveString("name1"));
        PluggableSCMMaterial material2 = MaterialsMother.pluggableSCMMaterial();
        material2.setName(new CaseInsensitiveString("name2"));

        assertThat(material1.equals(material2)).isTrue();
    }

    @Test
    void shouldFailEqualsCheckIfFingerprintDiffers() {
        PluggableSCMMaterial material1 = MaterialsMother.pluggableSCMMaterial();
        material1.getScmConfig().getConfiguration().first().setConfigurationValue(new ConfigurationValue("new-url"));
        PluggableSCMMaterial material2 = MaterialsMother.pluggableSCMMaterial();

        assertThat(material1.equals(material2)).isFalse();
    }

    @Test
    void shouldReturnSomethingMoreSaneForToString() throws Exception {
        PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();

        SCMMetadataStore.getInstance().addMetadataFor(material.getPluginId(), new SCMConfigurations(), null);

        assertThat(material.toString()).isEqualTo("'PluggableSCMMaterial{[k1=v1, k2=v2]}'");
    }

    @Test
    void shouldReturnNameAsNullIfSCMConfigIsNotSet() {
        assertThat(new PluggableSCMMaterial().getName()).isNull();
    }

    @Test
    void shouldNotCalculateFingerprintWhenAvailable() {
        String fingerprint = "fingerprint";
        SCM scmConfig = mock(SCM.class);
        when(scmConfig.getConfiguration()).thenReturn(new Configuration());

        PluggableSCMMaterial pluggableSCMMaterial = new PluggableSCMMaterial();
        pluggableSCMMaterial.setSCMConfig(scmConfig);
        pluggableSCMMaterial.setFingerprint(fingerprint);

        assertThat(pluggableSCMMaterial.getFingerprint()).isEqualTo(fingerprint);
        verify(scmConfig, never()).getFingerprint();
    }

    @Test
    void shouldTakeValueOfIsAutoUpdateFromSCMConfig() throws Exception {
        PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();

        material.getScmConfig().setAutoUpdate(true);
        assertThat(material.isAutoUpdate()).isTrue();

        material.getScmConfig().setAutoUpdate(false);
        assertThat(material.isAutoUpdate()).isFalse();
    }

    @Test
    void shouldReturnWorkingDirectoryCorrectly() {
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setFolder("dest");
        String baseFolder = new File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
        String workingFolder = new File(baseFolder, "dest").getAbsolutePath();
        assertThat(material.workingDirectory(new File(baseFolder)).getAbsolutePath()).isEqualTo(workingFolder);
        material.setFolder(null);
        assertThat(material.workingDirectory(new File(baseFolder)).getAbsolutePath()).isEqualTo(baseFolder);
    }

    @Test
    void shouldGetAttributesWithSecureFields() {
        PluggableSCMMaterial material = createPluggableSCMMaterialWithSecureConfiguration();
        Map<String, Object> attributes = material.getAttributes(true);

        assertThat(attributes.get("type")).isEqualTo("scm");
        assertThat(attributes.get("plugin-id")).isEqualTo("pluginid");
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("scm-configuration");
        assertThat(configuration.get("k1")).isEqualTo("v1");
        assertThat(configuration.get("k2")).isEqualTo("v2");
    }

    @Test
    void shouldGetAttributesWithoutSecureFields() {
        PluggableSCMMaterial material = createPluggableSCMMaterialWithSecureConfiguration();
        Map<String, Object> attributes = material.getAttributes(false);

        assertThat(attributes.get("type")).isEqualTo("scm");
        assertThat(attributes.get("plugin-id")).isEqualTo("pluginid");
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("scm-configuration");
        assertThat(configuration.get("k1")).isEqualTo("v1");
        assertThat(configuration.get("k2")).isNull();
    }

    @Test
    void shouldCorrectlyGetTypeDisplay() {
        PluggableSCMMaterial pluggableSCMMaterial = new PluggableSCMMaterial("scm-id");
        assertThat(pluggableSCMMaterial.getTypeForDisplay()).isEqualTo("SCM");

        pluggableSCMMaterial.setSCMConfig(SCMMother.create("scm-id"));
        assertThat(pluggableSCMMaterial.getTypeForDisplay()).isEqualTo("SCM");

        SCMMetadataStore.getInstance().addMetadataFor("plugin", null, null);
        assertThat(pluggableSCMMaterial.getTypeForDisplay()).isEqualTo("SCM");

        SCMView scmView = mock(SCMView.class);
        when(scmView.displayValue()).thenReturn("scm-name");
        SCMMetadataStore.getInstance().addMetadataFor("plugin", null, scmView);
        assertThat(pluggableSCMMaterial.getTypeForDisplay()).isEqualTo("scm-name");
    }

    @Test
    void shouldReturnTrueForPluggableScmMaterial_supportsDestinationFolder() throws Exception {
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        assertThat(material.supportsDestinationFolder()).isTrue();
    }

    @Test
    void shouldUpdateMaterialFromMaterialConfig() {
        PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
        PluggableSCMMaterialConfig materialConfig = MaterialConfigsMother.pluggableSCMMaterialConfig("some-scm-name");
        Configuration configuration = new Configuration(new ConfigurationProperty(new ConfigurationKey("new_key"), new ConfigurationValue("new_value")));
        materialConfig.getSCMConfig().setConfiguration(configuration);

        material.updateFromConfig(materialConfig);
        assertThat(material.getScmConfig().getConfiguration()).isEqualTo(materialConfig.getSCMConfig().getConfiguration());
        assertThat(material.getScmConfig().getName()).isEqualTo(materialConfig.getSCMConfig().getName());
    }

    private PluggableSCMMaterial createPluggableSCMMaterialWithSecureConfiguration() {
        PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
        material.getScmConfig().getConfiguration().get(1).handleSecureValueConfiguration(true);
        return material;
    }

    @Nested
    class HasSecretParams {
        @Test
        void shouldBeTrueIfScmConfigHasSecretParam() {
            PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
            material.getScmConfig().getConfiguration().get(1).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][lookup_password]}}"));
            material.getScmConfig().getConfiguration().get(1).handleSecureValueConfiguration(true);

            assertThat(material.hasSecretParams()).isTrue();
        }

        @Test
        void shouldBeFalseIfScmCOnfigDoesNotHaveSecretParams() {
            PluggableSCMMaterial material = createPluggableSCMMaterialWithSecureConfiguration();

            assertThat(material.hasSecretParams()).isFalse();
        }
    }

    @Nested
    class GetSecretParams {
        @Test
        void shouldReturnAListOfSecretParams() {
            PluggableSCMMaterial material = MaterialsMother.pluggableSCMMaterial();
            material.getScmConfig().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][lookup_username]}}"));
            material.getScmConfig().getConfiguration().get(1).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][lookup_password]}}"));
            material.getScmConfig().getConfiguration().get(1).handleSecureValueConfiguration(true);

            assertThat(material.getSecretParams().size()).isEqualTo(2);
            assertThat(material.getSecretParams().get(0)).isEqualTo(new SecretParam("secret_config_id", "lookup_username"));
            assertThat(material.getSecretParams().get(1)).isEqualTo(new SecretParam("secret_config_id", "lookup_password"));
        }

        @Test
        void shouldBeAnEmptyListInAbsenceOfSecretParamsInScmConfig() {
            PluggableSCMMaterial material = createPluggableSCMMaterialWithSecureConfiguration();

            assertThat(material.getSecretParams()).isEmpty();
        }
    }

    @Test
    void shouldPopulateEnvironmentContextWithConfigurationWithSecretParamsAsSecure() {
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
        k1.getSecretParams().get(0).setValue("some-resolved-value");
        ConfigurationProperty k2 = ConfigurationPropertyMother.create("scm-secure", true, "value");
        PluggableSCMMaterial material = new PluggableSCMMaterial();
        material.setSCMConfig(SCMMother.create("scm-id", "tw-dev", "pluginid", "version", new Configuration(k1, k2)));
        material.setName(new CaseInsensitiveString("tw-dev:go-agent"));
        Modifications modifications = new Modifications(new Modification(null, null, null, new Date(), "revision-123"));
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
        material.populateEnvironmentContext(environmentVariableContext, new MaterialRevision(material, modifications), null);

        assertThat(environmentVariableContext.getProperty("GO_SCM_TW_DEV_GO_AGENT_K1")).isEqualTo("some-resolved-value");
        assertThat(environmentVariableContext.getPropertyForDisplay("GO_SCM_TW_DEV_GO_AGENT_K1")).isEqualTo(MASK_VALUE);
        assertThat(environmentVariableContext.getProperty("GO_SCM_TW_DEV_GO_AGENT_SCM_SECURE")).isEqualTo("value");
        assertThat(environmentVariableContext.getPropertyForDisplay("GO_SCM_TW_DEV_GO_AGENT_SCM_SECURE")).isEqualTo(MASK_VALUE);
        assertThat(environmentVariableContext.getProperty("GO_SCM_TW_DEV_GO_AGENT_LABEL")).isEqualTo("revision-123");
    }
}
