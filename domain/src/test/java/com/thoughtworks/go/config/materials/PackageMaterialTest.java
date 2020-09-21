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
package com.thoughtworks.go.config.materials;

import com.google.gson.Gson;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.SecretParam;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.materials.MatchedRevision;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.materials.packagematerial.PackageMaterialInstance;
import com.thoughtworks.go.domain.materials.packagematerial.PackageMaterialRevision;
import com.thoughtworks.go.domain.packagerepository.*;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.json.JsonHelper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother.create;
import static com.thoughtworks.go.util.command.EnvironmentVariableContext.EnvironmentVariable.MASK_VALUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class PackageMaterialTest {
    @Test
    void shouldCreatePackageMaterialInstance() {
        PackageMaterial material = MaterialsMother.packageMaterial();
        PackageMaterialInstance materialInstance = (PackageMaterialInstance) material.createMaterialInstance();

        assertThat(materialInstance).isNotNull();
        assertThat(materialInstance.getFlyweightName()).isNotNull();
        assertThat(materialInstance.getConfiguration()).isEqualTo(JsonHelper.toJsonString(material));
    }

    @Test
    void shouldGetMaterialInstanceType() {
        assertThat(new PackageMaterial().getInstanceType().equals(PackageMaterialInstance.class)).isTrue();
    }

    @Test
    void shouldGetSqlCriteria() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        Map<String, Object> criteria = material.getSqlCriteria();
        assertThat(criteria.get("type")).isEqualTo(PackageMaterial.class.getSimpleName());
        assertThat(criteria.get("fingerprint")).isEqualTo(material.getFingerprint());
    }

    @Test
    void shouldGetFingerprintForMaterial() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo", "pluginid", "version",
                new Configuration(ConfigurationPropertyMother.create("k1", false, "v1"), ConfigurationPropertyMother.create("secure-key", true, "secure-value")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        assertThat(material.getFingerprint()).isEqualTo(CachedDigestUtils.sha256Hex("plugin-id=pluginid<|>k2=v2<|>k1=v1<|>secure-key=secure-value"));
    }

    @Test
    void shouldGetDifferentFingerprintWhenPluginIdChanges() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo", "yum-1", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id-1", "name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));


        PackageMaterial anotherMaterial = new PackageMaterial();
        PackageRepository anotherRepository = PackageRepositoryMother.create("repo-id", "repo", "yum-2", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        anotherMaterial.setPackageDefinition(PackageDefinitionMother.create("p-id-2", "name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), anotherRepository));

        assertThat(material.getFingerprint().equals(anotherMaterial.getFingerprint())).isFalse();
    }

    @Test
    void shouldGetDescription() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        assertThat(material.getDescription()).isEqualTo("repo-name_package-name");
    }

    @Test
    void shouldGetDisplayName() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        assertThat(material.getDisplayName()).isEqualTo("repo-name_package-name");
    }

    @Test
    void shouldTypeForDisplay() {
        PackageMaterial material = new PackageMaterial();
        assertThat(material.getTypeForDisplay()).isEqualTo("Package");
    }

    @Test
    void shouldGetAttributesForXml() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        Map<String, Object> attributesForXml = material.getAttributesForXml();
        assertThat(attributesForXml.get("type").toString()).isEqualTo("PackageMaterial");
        assertThat(attributesForXml.get("repositoryName").toString()).isEqualTo("repo-name");
        assertThat(attributesForXml.get("packageName").toString()).isEqualTo("package-name");
    }

    @Test
    void shouldConvertPackageMaterialToJsonFormatToBeStoredInDb() throws CryptoException {
        GoCipher cipher = new GoCipher();
        String encryptedPassword = cipher.encrypt("password");
        ConfigurationProperty secureRepoProperty = new ConfigurationProperty(new ConfigurationKey("secure-key"), null, new EncryptedConfigurationValue(encryptedPassword), cipher);
        ConfigurationProperty repoProperty = new ConfigurationProperty(new ConfigurationKey("non-secure-key"), new ConfigurationValue("value"), null, cipher);
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.setPluginConfiguration(new PluginConfiguration("plugin-id", "1.0"));
        packageRepository.setConfiguration(new Configuration(secureRepoProperty, repoProperty));

        ConfigurationProperty securePackageProperty = new ConfigurationProperty(new ConfigurationKey("secure-key"), null, new EncryptedConfigurationValue(encryptedPassword),
                cipher);
        ConfigurationProperty packageProperty = new ConfigurationProperty(new ConfigurationKey("non-secure-key"), new ConfigurationValue("value"), null, cipher);
        PackageDefinition packageDefinition = new PackageDefinition("id", "name", new Configuration(securePackageProperty, packageProperty));
        packageDefinition.setRepository(packageRepository);

        PackageMaterial packageMaterial = new PackageMaterial("id");
        packageMaterial.setPackageDefinition(packageDefinition);

        String json = JsonHelper.toJsonString(packageMaterial);

        String expected = "{\"package\":{\"config\":[{\"configKey\":{\"name\":\"secure-key\"},\"encryptedConfigValue\":{\"value\":" + new Gson().toJson(encryptedPassword) + "}},{\"configKey\":{\"name\":\"non-secure-key\"},\"configValue\":{\"value\":\"value\"}}],\"repository\":{\"plugin\":{\"id\":\"plugin-id\",\"version\":\"1.0\"},\"config\":[{\"configKey\":{\"name\":\"secure-key\"},\"encryptedConfigValue\":{\"value\":" + new Gson().toJson(encryptedPassword) + "}},{\"configKey\":{\"name\":\"non-secure-key\"},\"configValue\":{\"value\":\"value\"}}]}}}";

        assertThat(json).isEqualTo(expected);
        assertThat(JsonHelper.fromJson(expected, PackageMaterial.class)).isEqualTo(packageMaterial);
    }

    @Test
    void shouldGetJsonRepresentationForPackageMaterial() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = create("repo-id", "repo-name", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        Map<String, String> jsonMap = new LinkedHashMap<>();
        material.toJson(jsonMap, new PackageMaterialRevision("rev123", new Date()));

        assertThat(jsonMap.get("scmType")).isEqualTo("Package");
        assertThat(jsonMap.get("materialName")).isEqualTo("repo-name_package-name");
        assertThat(jsonMap.get("action")).isEqualTo("Modified");
        assertThat(jsonMap.get("location")).isEqualTo(material.getUriForDisplay());
    }

    @Test
    void shouldGetEmailContentForPackageMaterial() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));

        StringBuilder content = new StringBuilder();
        Date date = new Date(1367472329111L);
        material.emailContent(content, new Modification(null, null, null, date, "rev123"));

        assertThat(content.toString()).isEqualTo(String.format("Package : repo-name_package-name\nrevision: rev123, completed on %s", date.toString()));
    }

    @Test
    void shouldReturnFalseForIsUsedInFetchArtifact() {
        PackageMaterial material = new PackageMaterial();
        assertThat(material.isUsedInFetchArtifact(new PipelineConfig())).isFalse();
    }

    @Test
    void shouldReturnMatchedRevisionForPackageMaterial() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));

        Date timestamp = new Date();
        MatchedRevision matchedRevision = material.createMatchedRevision(new Modification("go", "comment", null, timestamp, "rev123"), "rev");
        assertThat(matchedRevision.getShortRevision()).isEqualTo("rev123");
        assertThat(matchedRevision.getLongRevision()).isEqualTo("rev123");
        assertThat(matchedRevision.getCheckinTime()).isEqualTo(timestamp);
        assertThat(matchedRevision.getUser()).isEqualTo("go");
        assertThat(matchedRevision.getComment()).isEqualTo("comment");
    }

    @Test
    void shouldGetNameFromRepoNameAndPackageName() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        assertThat(material.getName().toString()).isEqualTo("repo-name_package-name");
    }

    @Test
    void shouldPopulateEnvironmentContext() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "tw-dev", "pluginid", "version",
                new Configuration(ConfigurationPropertyMother.create("k1", false, "v1"), ConfigurationPropertyMother.create("repo-secure", true, "value")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "go-agent",
                new Configuration(ConfigurationPropertyMother.create("k2", false, "v2"), ConfigurationPropertyMother.create("pkg-secure", true, "value")), repository));
        material.setName(new CaseInsensitiveString("tw-dev:go-agent"));
        Modifications modifications = new Modifications(new Modification(null, null, null, new Date(), "revision-123"));
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
        material.populateEnvironmentContext(environmentVariableContext, new MaterialRevision(material, modifications), null);
        assertThat(environmentVariableContext.getProperty("GO_REPO_TW_DEV_GO_AGENT_K1")).isEqualTo("v1");
        assertThat(environmentVariableContext.getProperty("GO_REPO_TW_DEV_GO_AGENT_REPO_SECURE")).isEqualTo("value");
        assertThat(environmentVariableContext.getPropertyForDisplay("GO_REPO_TW_DEV_GO_AGENT_REPO_SECURE")).isEqualTo(MASK_VALUE);
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_K2")).isEqualTo("v2");
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_PKG_SECURE")).isEqualTo("value");
        assertThat(environmentVariableContext.getPropertyForDisplay("GO_PACKAGE_TW_DEV_GO_AGENT_PKG_SECURE")).isEqualTo(MASK_VALUE);
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_LABEL")).isEqualTo("revision-123");
    }

    @Test
    void shouldPopulateEnvironmentContextWithEnvironmentVariablesCreatedOutOfAdditionalDataFromModification() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "tw-dev", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "go-agent", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        material.setName(new CaseInsensitiveString("tw-dev:go-agent"));
        HashMap<String, String> map = new HashMap<>();
        map.put("MY_NEW_KEY", "my_value");
        Modification modification = new Modification("loser", "comment", "email", new Date(), "revision-123", JsonHelper.toJsonString(map));
        Modifications modifications = new Modifications(modification);

        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

        material.populateEnvironmentContext(environmentVariableContext, new MaterialRevision(material, modifications), null);

        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_LABEL")).isEqualTo("revision-123");
        assertThat(environmentVariableContext.getProperty("GO_REPO_TW_DEV_GO_AGENT_K1")).isEqualTo("v1");
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_K2")).isEqualTo("v2");
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_MY_NEW_KEY")).isEqualTo("my_value");
    }

    @Test
    void shouldMarkEnvironmentContextCreatedForAdditionalDataAsSecureIfTheValueContainsAnySpecialCharacters() throws UnsupportedEncodingException {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "tw-dev", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "go-agent", new Configuration(ConfigurationPropertyMother.create("k2", true, "!secure_value:with_special_chars"),
                ConfigurationPropertyMother.create("k3", true, "secure_value_with_regular_chars")), repository));
        material.setName(new CaseInsensitiveString("tw-dev:go-agent"));
        HashMap<String, String> map = new HashMap<>();
        map.put("ADDITIONAL_DATA_ONE", "foobar:!secure_value:with_special_chars");
        map.put("ADDITIONAL_DATA_URL_ENCODED", "something:%21secure_value%3Awith_special_chars");
        map.put("ADDITIONAL_DATA_TWO", "foobar:secure_value_with_regular_chars");
        Modification modification = new Modification("loser", "comment", "email", new Date(), "revision-123", JsonHelper.toJsonString(map));
        Modifications modifications = new Modifications(modification);

        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

        material.populateEnvironmentContext(environmentVariableContext, new MaterialRevision(material, modifications), null);

        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_LABEL")).isEqualTo("revision-123");
        assertThat(environmentVariableContext.getProperty("GO_REPO_TW_DEV_GO_AGENT_K1")).isEqualTo("v1");
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_K2")).isEqualTo("!secure_value:with_special_chars");
        assertThat(environmentVariableContext.getPropertyForDisplay("GO_PACKAGE_TW_DEV_GO_AGENT_K2")).isEqualTo("********");
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_ADDITIONAL_DATA_ONE")).isEqualTo("foobar:!secure_value:with_special_chars");

        assertThat(environmentVariableContext.getPropertyForDisplay("GO_PACKAGE_TW_DEV_GO_AGENT_ADDITIONAL_DATA_ONE")).isEqualTo("foobar:!secure_value:with_special_chars");
        assertThat(environmentVariableContext.getPropertyForDisplay("GO_PACKAGE_TW_DEV_GO_AGENT_ADDITIONAL_DATA_TWO")).isEqualTo("foobar:secure_value_with_regular_chars");
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_ADDITIONAL_DATA_URL_ENCODED")).isEqualTo("something:%21secure_value%3Awith_special_chars");
        assertThat(environmentVariableContext.getPropertyForDisplay("GO_PACKAGE_TW_DEV_GO_AGENT_ADDITIONAL_DATA_URL_ENCODED")).isEqualTo("********");
    }

    @Test
    void shouldNotThrowUpWhenAdditionalDataIsNull() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "tw-dev", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "go-agent", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        material.setName(new CaseInsensitiveString("tw-dev:go-agent"));
        Modifications modifications = new Modifications(new Modification("loser", "comment", "email", new Date(), "revision-123", null));
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

        material.populateEnvironmentContext(environmentVariableContext, new MaterialRevision(material, modifications), null);

        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_LABEL")).isEqualTo("revision-123");
        assertThat(environmentVariableContext.getProperty("GO_REPO_TW_DEV_GO_AGENT_K1")).isEqualTo("v1");
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_K2")).isEqualTo("v2");
    }

    @Test
    void shouldNotThrowUpWhenAdditionalDataIsRandomJunkAndNotJSON() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "tw-dev", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "go-agent", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        material.setName(new CaseInsensitiveString("tw-dev:go-agent"));
        Modifications modifications = new Modifications(new Modification("loser", "comment", "email", new Date(), "revision-123", "salkdfjdsa-jjgkj!!!vcxknbvkjk"));
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

        material.populateEnvironmentContext(environmentVariableContext, new MaterialRevision(material, modifications), null);

        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_LABEL")).isEqualTo("revision-123");
        assertThat(environmentVariableContext.getProperty("GO_REPO_TW_DEV_GO_AGENT_K1")).isEqualTo("v1");
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_K2")).isEqualTo("v2");
    }

    @Test
    void shouldGetUriForDisplay() {
        RepositoryMetadataStore.getInstance().addMetadataFor("some-plugin", new PackageConfigurations());
        PackageMetadataStore.getInstance().addMetadataFor("some-plugin", new PackageConfigurations());

        PackageMaterial material = new PackageMaterial();
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("k1", false, "repo-v1"), ConfigurationPropertyMother.create("k2", false, "repo-v2"));
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "some-plugin", "version", configuration);
        PackageDefinition packageDefinition = PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k3", false, "package-v1")), repository);
        material.setPackageDefinition(packageDefinition);
        assertThat(material.getUriForDisplay()).isEqualTo("Repository: [k1=repo-v1, k2=repo-v2] - Package: [k3=package-v1]");
    }

    @Test
    void shouldGetUriForDisplayNameIfNameIsNull() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", null, "pluginid", "version",
                new Configuration(ConfigurationPropertyMother.create("k1", false, "repo-v1"), ConfigurationPropertyMother.create("k2", false, "repo-v2")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", null, new Configuration(ConfigurationPropertyMother.create("k3", false, "package-v1")), repository));
        assertThat(material.getDisplayName()).isEqualTo(material.getUriForDisplay());
    }

    @Test
    void shouldGetLongDescription() {
        PackageMaterial material = new PackageMaterial();
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("k1", false, "repo-v1"), ConfigurationPropertyMother.create("k2", false, "repo-v2"));
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", configuration);
        PackageDefinition packageDefinition = PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k3", false, "package-v1")), repository);
        material.setPackageDefinition(packageDefinition);
        assertThat(material.getLongDescription()).isEqualTo(material.getUriForDisplay());
    }

    @Test
    void shouldPassEqualsCheckIfFingerprintIsSame() {
        PackageMaterial material1 = MaterialsMother.packageMaterial();
        material1.setName(new CaseInsensitiveString("name1"));
        PackageMaterial material2 = MaterialsMother.packageMaterial();
        material2.setName(new CaseInsensitiveString("name2"));

        assertThat(material1.equals(material2)).isTrue();
    }

    @Test
    void shouldFailEqualsCheckIfFingerprintDiffers() {
        PackageMaterial material1 = MaterialsMother.packageMaterial();
        material1.getPackageDefinition().getConfiguration().first().setConfigurationValue(new ConfigurationValue("new-url"));
        PackageMaterial material2 = MaterialsMother.packageMaterial();

        assertThat(material1.equals(material2)).isFalse();
    }

    @Test
    void shouldReturnSomethingMoreSaneForToString() throws Exception {
        PackageMaterial material = MaterialsMother.packageMaterial();

        RepositoryMetadataStore.getInstance().addMetadataFor(material.getPluginId(), new PackageConfigurations());
        PackageMetadataStore.getInstance().addMetadataFor(material.getPluginId(), new PackageConfigurations());

        assertThat(material.toString()).isEqualTo("'PackageMaterial{Repository: [k1=repo-v1, k2=repo-v2] - Package: [k3=package-v1]}'");
    }

    @Test
    void shouldReturnNameAsNullIfPackageDefinitionIsNotSet() {
        assertThat(new PackageMaterial().getName()).isNull();
    }

    @Test
    void shouldNotCalculateFingerprintWhenAvailable() {
        String fingerprint = "fingerprint";
        PackageDefinition packageDefinition = mock(PackageDefinition.class);
        PackageMaterial packageMaterial = new PackageMaterial();
        packageMaterial.setPackageDefinition(packageDefinition);
        packageMaterial.setFingerprint(fingerprint);
        assertThat(packageMaterial.getFingerprint()).isEqualTo(fingerprint);
        verify(packageDefinition, never()).getFingerprint(anyString());
    }

    @Test
    void shouldTakeValueOfIsAutoUpdateFromPackageDefinition() throws Exception {
        PackageMaterial material = MaterialsMother.packageMaterial();

        material.getPackageDefinition().setAutoUpdate(true);
        assertThat(material.isAutoUpdate()).isTrue();

        material.getPackageDefinition().setAutoUpdate(false);
        assertThat(material.isAutoUpdate()).isFalse();
    }

    @Test
    void shouldGetAttributesWithSecureFields() {
        PackageMaterial material = createPackageMaterialWithSecureConfiguration();
        Map<String, Object> attributes = material.getAttributes(true);

        assertThat(attributes.get("type")).isEqualTo("package");
        assertThat(attributes.get("plugin-id")).isEqualTo("pluginid");
        Map<String, Object> repositoryConfiguration = (Map<String, Object>) attributes.get("repository-configuration");
        assertThat(repositoryConfiguration.get("k1")).isEqualTo("repo-v1");
        assertThat(repositoryConfiguration.get("k2")).isEqualTo("repo-v2");
        Map<String, Object> packageConfiguration = (Map<String, Object>) attributes.get("package-configuration");
        assertThat(packageConfiguration.get("k3")).isEqualTo("package-v1");
        assertThat(packageConfiguration.get("k4")).isEqualTo("package-v2");
    }

    @Test
    void shouldGetAttributesWithoutSecureFields() {
        PackageMaterial material = createPackageMaterialWithSecureConfiguration();
        Map<String, Object> attributes = material.getAttributes(false);

        assertThat(attributes.get("type")).isEqualTo("package");
        assertThat(attributes.get("plugin-id")).isEqualTo("pluginid");
        Map<String, Object> repositoryConfiguration = (Map<String, Object>) attributes.get("repository-configuration");
        assertThat(repositoryConfiguration.get("k1")).isEqualTo("repo-v1");
        assertThat(repositoryConfiguration.get("k2")).isNull();
        Map<String, Object> packageConfiguration = (Map<String, Object>) attributes.get("package-configuration");
        assertThat(packageConfiguration.get("k3")).isEqualTo("package-v1");
        assertThat(packageConfiguration.get("k4")).isNull();
    }

    private PackageMaterial createPackageMaterialWithSecureConfiguration() {
        PackageMaterial material = MaterialsMother.packageMaterial();
        material.getPackageDefinition().getRepository().getConfiguration().get(1).handleSecureValueConfiguration(true);
        material.getPackageDefinition().getConfiguration().addNewConfigurationWithValue("k4", "package-v2", false);
        material.getPackageDefinition().getConfiguration().get(1).handleSecureValueConfiguration(true);
        return material;
    }

    @Test
    void shouldReturnFalseForPackageMaterial_supportsDestinationFolder() throws Exception {
        PackageMaterial material = new PackageMaterial();
        assertThat(material.supportsDestinationFolder()).isFalse();
    }

    @Nested
    class HasSecretParams {
        @Test
        void shouldBeTrueIfPkgMaterialHasSecretParam() {
            PackageMaterial material = MaterialsMother.packageMaterial();
            material.getPackageDefinition().getRepository().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][lookup_token]}}"));
            material.getPackageDefinition().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][lookup_password]}}"));

            assertThat(material.hasSecretParams()).isTrue();
        }

        @Test
        void shouldBeFalseIfPkgMaterialDoesNotHaveSecretParams() {
            PackageMaterial material = createPackageMaterialWithSecureConfiguration();

            assertThat(material.hasSecretParams()).isFalse();
        }
    }

    @Nested
    class GetSecretParams {
        @Test
        void shouldReturnAListOfSecretParams() {
            PackageMaterial material = MaterialsMother.packageMaterial();
            material.getPackageDefinition().getRepository().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][lookup_username]}}"));
            material.getPackageDefinition().getConfiguration().get(0).setConfigurationValue(new ConfigurationValue("{{SECRET:[secret_config_id][lookup_password]}}"));

            assertThat(material.getSecretParams().size()).isEqualTo(2);
            assertThat(material.getSecretParams().get(0)).isEqualTo(new SecretParam("secret_config_id", "lookup_username"));
            assertThat(material.getSecretParams().get(1)).isEqualTo(new SecretParam("secret_config_id", "lookup_password"));
        }

        @Test
        void shouldBeAnEmptyListInAbsenceOfSecretParamsInPkgMaterial() {
            PackageMaterial material = createPackageMaterialWithSecureConfiguration();

            assertThat(material.getSecretParams()).isEmpty();
        }
    }

    @Test
    void shouldPopulateEnvironmentContextWithConfigurationWithSecretParamsAsSecure() {
        ConfigurationProperty k1 = ConfigurationPropertyMother.create("k1", false, "{{SECRET:[secret_config_id][lookup_username]}}");
        k1.getSecretParams().get(0).setValue("some-resolved-value");
        ConfigurationProperty k2 = ConfigurationPropertyMother.create("k2", false, "{{SECRET:[secret_config_id][lookup_password]}}");
        k2.getSecretParams().get(0).setValue("some-resolved-password");
        PackageDefinition pkgDef = new PackageDefinition("id", "name", new Configuration(k2));
        PackageRepository pkgRepo = new PackageRepository("pkg-repo-id", "pkg-repo-name", new PluginConfiguration(), new Configuration(k1));
        pkgDef.setRepository(pkgRepo);
        PackageMaterial material = new PackageMaterial();
        material.setPackageDefinition(pkgDef);

        material.setName(new CaseInsensitiveString("tw-dev:go-agent"));
        Modifications modifications = new Modifications(new Modification(null, null, null, new Date(), "revision-123"));
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
        material.populateEnvironmentContext(environmentVariableContext, new MaterialRevision(material, modifications), null);

        assertThat(environmentVariableContext.getProperties().size()).isEqualTo(3);
        assertThat(environmentVariableContext.getProperty("GO_REPO_TW_DEV_GO_AGENT_K1")).isEqualTo("some-resolved-value");
        assertThat(environmentVariableContext.getPropertyForDisplay("GO_REPO_TW_DEV_GO_AGENT_K1")).isEqualTo(MASK_VALUE);
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_K2")).isEqualTo("some-resolved-password");
        assertThat(environmentVariableContext.getPropertyForDisplay("GO_PACKAGE_TW_DEV_GO_AGENT_K2")).isEqualTo(MASK_VALUE);
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_LABEL")).isEqualTo("revision-123");
    }

    @Test
    void shouldUpdateMaterialFromMaterialConfig() {
        PackageMaterial material = MaterialsMother.packageMaterial();
        PackageMaterialConfig materialConfig = MaterialConfigsMother.packageMaterialConfig("pkg-repo-name", "pkg-name");
        Configuration configuration = new Configuration(new ConfigurationProperty(new ConfigurationKey("new_key"), new ConfigurationValue("new_value")));
        materialConfig.getPackageDefinition().setConfiguration(configuration);

        material.updateFromConfig(materialConfig);
        assertThat(material.getPackageDefinition().getConfiguration()).isEqualTo(materialConfig.getPackageDefinition().getConfiguration());
        assertThat(material.getPackageDefinition().getRepository().getConfiguration()).isEqualTo(materialConfig.getPackageDefinition().getRepository().getConfiguration());
        assertThat(material.getPackageDefinition().getRepository().getName()).isEqualTo(materialConfig.getPackageDefinition().getRepository().getName());
    }
}
