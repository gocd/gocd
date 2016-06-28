/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.materials.MatchedRevision;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.materials.packagematerial.PackageMaterialInstance;
import com.thoughtworks.go.domain.materials.packagematerial.PackageMaterialRevision;
import com.thoughtworks.go.domain.packagerepository.*;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.util.json.JsonHelper;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother.create;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.*;

public class PackageMaterialTest {
    @Test
    public void shouldCreatePackageMaterialInstance() {
        PackageMaterial material = MaterialsMother.packageMaterial();
        PackageMaterialInstance materialInstance = (PackageMaterialInstance) material.createMaterialInstance();

        assertThat(materialInstance, is(notNullValue()));
        assertThat(materialInstance.getFlyweightName(), is(notNullValue()));
        assertThat(materialInstance.getConfiguration(), is(JsonHelper.toJsonString(material)));
    }

    @Test
    public void shouldGetMaterialInstanceType() {
        assertThat(new PackageMaterial().getInstanceType().equals(PackageMaterialInstance.class), is(true));
    }

    @Test
    public void shouldGetSqlCriteria() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        Map<String, Object> criteria = material.getSqlCriteria();
        assertThat((String) criteria.get("type"), is(PackageMaterial.class.getSimpleName()));
        assertThat((String) criteria.get("fingerprint"), is(material.getFingerprint()));
    }

    @Test
    public void shouldGetFingerprintForMaterial() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo", "pluginid", "version",
                new Configuration(ConfigurationPropertyMother.create("k1", false, "v1"), ConfigurationPropertyMother.create("secure-key", true, "secure-value")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        assertThat(material.getFingerprint(), is(CachedDigestUtils.sha256Hex("plugin-id=pluginid<|>k2=v2<|>k1=v1<|>secure-key=secure-value")));
    }

    @Test
    public void shouldGetDifferentFingerprintWhenPluginIdChanges() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo", "yum-1", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id-1", "name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));


        PackageMaterial anotherMaterial = new PackageMaterial();
        PackageRepository anotherRepository = PackageRepositoryMother.create("repo-id", "repo", "yum-2", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        anotherMaterial.setPackageDefinition(PackageDefinitionMother.create("p-id-2", "name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), anotherRepository));

        assertThat(material.getFingerprint().equals(anotherMaterial.getFingerprint()), is(false));
    }

    @Test
    public void shouldGetDescription() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        assertThat(material.getDescription(), is("repo-name:package-name"));
    }

    @Test
    public void shouldGetDisplayName() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        assertThat(material.getDisplayName(), is("repo-name:package-name"));
    }

    @Test
    public void shouldTypeForDisplay() {
        PackageMaterial material = new PackageMaterial();
        assertThat(material.getTypeForDisplay(), is("Package"));
    }

    @Test
    public void shouldGetAttributesForXml() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        Map<String, Object> attributesForXml = material.getAttributesForXml();
        assertThat(attributesForXml.get("type").toString(), is("PackageMaterial"));
        assertThat(attributesForXml.get("repositoryName").toString(), is("repo-name"));
        assertThat(attributesForXml.get("packageName").toString(), is("package-name"));
    }

    @Test
    public void shouldConvertPackageMaterialToJsonFormatToBeStoredInDb() {
        GoCipher cipher = new GoCipher();
        ConfigurationProperty secureRepoProperty = new ConfigurationProperty(new ConfigurationKey("secure-key"), null, new EncryptedConfigurationValue("hnfcyX5dAvd82AWUyjfKCQ\u003d\u003d"), cipher);
        ConfigurationProperty repoProperty = new ConfigurationProperty(new ConfigurationKey("non-secure-key"), new ConfigurationValue("value"), null, cipher);
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.setPluginConfiguration(new PluginConfiguration("plugin-id", "1.0"));
        packageRepository.setConfiguration(new Configuration(secureRepoProperty, repoProperty));

        ConfigurationProperty securePackageProperty = new ConfigurationProperty(new ConfigurationKey("secure-key"), null, new EncryptedConfigurationValue("hnfcyX5dAvd82AWUyjfKCQ\u003d\u003d"),
                cipher);
        ConfigurationProperty packageProperty = new ConfigurationProperty(new ConfigurationKey("non-secure-key"), new ConfigurationValue("value"), null, cipher);
        PackageDefinition packageDefinition = new PackageDefinition("id", "name", new Configuration(securePackageProperty, packageProperty));
        packageDefinition.setRepository(packageRepository);

        PackageMaterial packageMaterial = new PackageMaterial("id");
        packageMaterial.setPackageDefinition(packageDefinition);

        String json = JsonHelper.toJsonString(packageMaterial);

        String expected = "{\"package\":{\"config\":[{\"configKey\":{\"name\":\"secure-key\"},\"encryptedConfigValue\":{\"value\":\"hnfcyX5dAvd82AWUyjfKCQ\\u003d\\u003d\"}},{\"configKey\":{\"name\":\"non-secure-key\"},\"configValue\":{\"value\":\"value\"}}],\"repository\":{\"plugin\":{\"id\":\"plugin-id\",\"version\":\"1.0\"},\"config\":[{\"configKey\":{\"name\":\"secure-key\"},\"encryptedConfigValue\":{\"value\":\"hnfcyX5dAvd82AWUyjfKCQ\\u003d\\u003d\"}},{\"configKey\":{\"name\":\"non-secure-key\"},\"configValue\":{\"value\":\"value\"}}]}}}";

        assertThat(json, is(expected));
        assertThat(JsonHelper.fromJson(expected, PackageMaterial.class), is(packageMaterial));
    }

    @Test
    public void shouldGetJsonRepresentationForPackageMaterial() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = create("repo-id", "repo-name", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        Map<String, String> jsonMap = new LinkedHashMap<>();
        material.toJson(jsonMap, new PackageMaterialRevision("rev123", new Date()));

        assertThat(jsonMap.get("scmType"), is("Package"));
        assertThat(jsonMap.get("materialName"), is("repo-name:package-name"));
        assertThat(jsonMap.get("action"), is("Modified"));
        assertThat(jsonMap.get("location"), is(material.getUriForDisplay()));
    }

    @Test
    public void shouldGetEmailContentForPackageMaterial() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));

        StringBuilder content = new StringBuilder();
        Date date = new Date(1367472329111L);
        material.emailContent(content, new Modification(null, null, null, date, "rev123"));

        assertThat(content.toString(), is(String.format("Package : repo-name:package-name\nrevision: rev123, completed on %s", date.toString())));
    }

    @Test
    public void shouldReturnFalseForIsUsedInFetchArtifact() {
        PackageMaterial material = new PackageMaterial();
        assertThat(material.isUsedInFetchArtifact(new PipelineConfig()), is(false));
    }

    @Test
    public void shouldReturnMatchedRevisionForPackageMaterial() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));

        Date timestamp = new Date();
        MatchedRevision matchedRevision = material.createMatchedRevision(new Modification("go", "comment", null, timestamp, "rev123"), "rev");
        assertThat(matchedRevision.getShortRevision(), is("rev123"));
        assertThat(matchedRevision.getLongRevision(), is("rev123"));
        assertThat(matchedRevision.getCheckinTime(), is(timestamp));
        assertThat(matchedRevision.getUser(), is("go"));
        assertThat(matchedRevision.getComment(), is("comment"));
    }

    @Test
    public void shouldGetNameFromRepoNameAndPackageName() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        assertThat(material.getName().toString(), is("repo-name:package-name"));
    }

    @Test
    public void shouldPopulateEnvironmentContext() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "tw-dev", "pluginid", "version",
                new Configuration(ConfigurationPropertyMother.create("k1", false, "v1"), ConfigurationPropertyMother.create("repo-secure", true, "value")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "go-agent",
                new Configuration(ConfigurationPropertyMother.create("k2", false, "v2"), ConfigurationPropertyMother.create("pkg-secure", true, "value")), repository));
        material.setName(new CaseInsensitiveString("tw-dev:go-agent"));
        Modifications modifications = new Modifications(new Modification(null, null, null, new Date(), "revision-123"));
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();
        material.populateEnvironmentContext(environmentVariableContext, new MaterialRevision(material, modifications), null);
        assertThat(environmentVariableContext.getProperty("GO_REPO_TW_DEV_GO_AGENT_K1"), is("v1"));
        assertThat(environmentVariableContext.getProperty("GO_REPO_TW_DEV_GO_AGENT_REPO_SECURE"), is("value"));
        assertThat(environmentVariableContext.getPropertyForDisplay("GO_REPO_TW_DEV_GO_AGENT_REPO_SECURE"), is(EnvironmentVariableContext.EnvironmentVariable.MASK_VALUE));
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_K2"), is("v2"));
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_PKG_SECURE"), is("value"));
        assertThat(environmentVariableContext.getPropertyForDisplay("GO_PACKAGE_TW_DEV_GO_AGENT_PKG_SECURE"), is(EnvironmentVariableContext.EnvironmentVariable.MASK_VALUE));
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_LABEL"), is("revision-123"));
    }

    @Test
    public void shouldPopulateEnvironmentContextWithEnvironmentVariablesCreatedOutOfAdditionalDataFromModification() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "tw-dev", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "go-agent", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        material.setName(new CaseInsensitiveString("tw-dev:go-agent"));
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("MY_NEW_KEY", "my_value");
        Modification modification = new Modification("loser", "comment", "email", new Date(), "revision-123", JsonHelper.toJsonString(map));
        Modifications modifications = new Modifications(modification);

        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

        material.populateEnvironmentContext(environmentVariableContext, new MaterialRevision(material, modifications), null);

        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_LABEL"), is("revision-123"));
        assertThat(environmentVariableContext.getProperty("GO_REPO_TW_DEV_GO_AGENT_K1"), is("v1"));
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_K2"), is("v2"));
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_MY_NEW_KEY"), is("my_value"));
    }

    @Test
    public void shouldMarkEnvironmentContextCreatedForAdditionalDataAsSecureIfTheValueContainsAnySpecialCharacters() throws UnsupportedEncodingException {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "tw-dev", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "go-agent", new Configuration(ConfigurationPropertyMother.create("k2", true, "!secure_value:with_special_chars"),
                ConfigurationPropertyMother.create("k3", true, "secure_value_with_regular_chars")), repository));
        material.setName(new CaseInsensitiveString("tw-dev:go-agent"));
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("ADDITIONAL_DATA_ONE", "foobar:!secure_value:with_special_chars");
        map.put("ADDITIONAL_DATA_URL_ENCODED", "something:%21secure_value%3Awith_special_chars");
        map.put("ADDITIONAL_DATA_TWO", "foobar:secure_value_with_regular_chars");
        Modification modification = new Modification("loser", "comment", "email", new Date(), "revision-123", JsonHelper.toJsonString(map));
        Modifications modifications = new Modifications(modification);

        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

        material.populateEnvironmentContext(environmentVariableContext, new MaterialRevision(material, modifications), null);

        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_LABEL"), is("revision-123"));
        assertThat(environmentVariableContext.getProperty("GO_REPO_TW_DEV_GO_AGENT_K1"), is("v1"));
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_K2"), is("!secure_value:with_special_chars"));
        assertThat(environmentVariableContext.getPropertyForDisplay("GO_PACKAGE_TW_DEV_GO_AGENT_K2"), is("********"));
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_ADDITIONAL_DATA_ONE"), is("foobar:!secure_value:with_special_chars"));

        assertThat(environmentVariableContext.getPropertyForDisplay("GO_PACKAGE_TW_DEV_GO_AGENT_ADDITIONAL_DATA_ONE"), is("foobar:!secure_value:with_special_chars"));
        assertThat(environmentVariableContext.getPropertyForDisplay("GO_PACKAGE_TW_DEV_GO_AGENT_ADDITIONAL_DATA_TWO"), is("foobar:secure_value_with_regular_chars"));
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_ADDITIONAL_DATA_URL_ENCODED"), is("something:%21secure_value%3Awith_special_chars"));
        assertThat(environmentVariableContext.getPropertyForDisplay("GO_PACKAGE_TW_DEV_GO_AGENT_ADDITIONAL_DATA_URL_ENCODED"), is("********"));
    }

    @Test
    public void shouldNotThrowUpWhenAdditionalDataIsNull() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "tw-dev", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "go-agent", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        material.setName(new CaseInsensitiveString("tw-dev:go-agent"));
        Modifications modifications = new Modifications(new Modification("loser", "comment", "email", new Date(), "revision-123", null));
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

        material.populateEnvironmentContext(environmentVariableContext, new MaterialRevision(material, modifications), null);

        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_LABEL"), is("revision-123"));
        assertThat(environmentVariableContext.getProperty("GO_REPO_TW_DEV_GO_AGENT_K1"), is("v1"));
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_K2"), is("v2"));
    }

    @Test
    public void shouldNotThrowUpWhenAdditionalDataIsRandomJunkAndNotJSON() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "tw-dev", "pluginid", "version", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", "go-agent", new Configuration(ConfigurationPropertyMother.create("k2", false, "v2")), repository));
        material.setName(new CaseInsensitiveString("tw-dev:go-agent"));
        Modifications modifications = new Modifications(new Modification("loser", "comment", "email", new Date(), "revision-123", "salkdfjdsa-jjgkj!!!vcxknbvkjk"));
        EnvironmentVariableContext environmentVariableContext = new EnvironmentVariableContext();

        material.populateEnvironmentContext(environmentVariableContext, new MaterialRevision(material, modifications), null);

        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_LABEL"), is("revision-123"));
        assertThat(environmentVariableContext.getProperty("GO_REPO_TW_DEV_GO_AGENT_K1"), is("v1"));
        assertThat(environmentVariableContext.getProperty("GO_PACKAGE_TW_DEV_GO_AGENT_K2"), is("v2"));
    }

    @Test
    public void shouldGetUriForDisplay() {
        RepositoryMetadataStore.getInstance().addMetadataFor("some-plugin", new PackageConfigurations());
        PackageMetadataStore.getInstance().addMetadataFor("some-plugin", new PackageConfigurations());

        PackageMaterial material = new PackageMaterial();
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("k1", false, "repo-v1"), ConfigurationPropertyMother.create("k2", false, "repo-v2"));
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "some-plugin", "version", configuration);
        PackageDefinition packageDefinition = PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k3", false, "package-v1")), repository);
        material.setPackageDefinition(packageDefinition);
        assertThat(material.getUriForDisplay(), is("Repository: [k1=repo-v1, k2=repo-v2] - Package: [k3=package-v1]"));
    }

    @Test
    public void shouldGetUriForDisplayNameIfNameIsNull() {
        PackageMaterial material = new PackageMaterial();
        PackageRepository repository = PackageRepositoryMother.create("repo-id", null, "pluginid", "version",
                new Configuration(ConfigurationPropertyMother.create("k1", false, "repo-v1"), ConfigurationPropertyMother.create("k2", false, "repo-v2")));
        material.setPackageDefinition(PackageDefinitionMother.create("p-id", null, new Configuration(ConfigurationPropertyMother.create("k3", false, "package-v1")), repository));
        assertThat(material.getDisplayName(), is(material.getUriForDisplay()));
    }

    @Test
    public void shouldGetLongDescription() {
        PackageMaterial material = new PackageMaterial();
        Configuration configuration = new Configuration(ConfigurationPropertyMother.create("k1", false, "repo-v1"), ConfigurationPropertyMother.create("k2", false, "repo-v2"));
        PackageRepository repository = PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", configuration);
        PackageDefinition packageDefinition = PackageDefinitionMother.create("p-id", "package-name", new Configuration(ConfigurationPropertyMother.create("k3", false, "package-v1")), repository);
        material.setPackageDefinition(packageDefinition);
        assertThat(material.getLongDescription(), is(material.getUriForDisplay()));
    }

    @Test
    public void shouldPassEqualsCheckIfFingerprintIsSame() {
        PackageMaterial material1 = MaterialsMother.packageMaterial();
        material1.setName(new CaseInsensitiveString("name1"));
        PackageMaterial material2 = MaterialsMother.packageMaterial();
        material2.setName(new CaseInsensitiveString("name2"));

        assertThat(material1.equals(material2), is(true));
    }

    @Test
    public void shouldFailEqualsCheckIfFingerprintDiffers() {
        PackageMaterial material1 = MaterialsMother.packageMaterial();
        material1.getPackageDefinition().getConfiguration().first().setConfigurationValue(new ConfigurationValue("new-url"));
        PackageMaterial material2 = MaterialsMother.packageMaterial();

        assertThat(material1.equals(material2), is(false));
    }

    @Test
    public void shouldReturnSomethingMoreSaneForToString() throws Exception {
        PackageMaterial material = MaterialsMother.packageMaterial();

        RepositoryMetadataStore.getInstance().addMetadataFor(material.getPluginId(), new PackageConfigurations());
        PackageMetadataStore.getInstance().addMetadataFor(material.getPluginId(), new PackageConfigurations());

        assertThat(material.toString(), is("'PackageMaterial{Repository: [k1=repo-v1, k2=repo-v2] - Package: [k3=package-v1]}'"));
    }

    @Test
    public void shouldReturnNameAsNullIfPackageDefinitionIsNotSet() {
        assertThat(new PackageMaterial().getName(), is(nullValue()));
    }

    @Test
    public void shouldNotCalculateFingerprintWhenAvailable() {
        String fingerprint = "fingerprint";
        PackageDefinition packageDefinition = mock(PackageDefinition.class);
        PackageMaterial packageMaterial = new PackageMaterial();
        packageMaterial.setPackageDefinition(packageDefinition);
        packageMaterial.setFingerprint(fingerprint);
        assertThat(packageMaterial.getFingerprint(),is(fingerprint));
        verify(packageDefinition,never()).getFingerprint(anyString());
    }

    @Test
    public void shouldTakeValueOfIsAutoUpdateFromPackageDefinition() throws Exception {
        PackageMaterial material = MaterialsMother.packageMaterial();

        material.getPackageDefinition().setAutoUpdate(true);
        assertThat(material.isAutoUpdate(), is(true));

        material.getPackageDefinition().setAutoUpdate(false);
        assertThat(material.isAutoUpdate(), is(false));
    }

    @Test
    public void shouldGetAttributesWithSecureFields() {
        PackageMaterial material = createPackageMaterialWithSecureConfiguration();
        Map<String, Object> attributes = material.getAttributes(true);

        assertThat((String) attributes.get("type"), is("package"));
        assertThat((String) attributes.get("plugin-id"), is("pluginid"));
        Map<String, Object> repositoryConfiguration = (Map<String, Object>) attributes.get("repository-configuration");
        assertThat((String) repositoryConfiguration.get("k1"), is("repo-v1"));
        assertThat((String) repositoryConfiguration.get("k2"), is("repo-v2"));
        Map<String, Object> packageConfiguration = (Map<String, Object>) attributes.get("package-configuration");
        assertThat((String) packageConfiguration.get("k3"), is("package-v1"));
        assertThat((String) packageConfiguration.get("k4"), is("package-v2"));
    }

    @Test
    public void shouldGetAttributesWithoutSecureFields() {
        PackageMaterial material = createPackageMaterialWithSecureConfiguration();
        Map<String, Object> attributes = material.getAttributes(false);

        assertThat((String) attributes.get("type"), is("package"));
        assertThat((String) attributes.get("plugin-id"), is("pluginid"));
        Map<String, Object> repositoryConfiguration = (Map<String, Object>) attributes.get("repository-configuration");
        assertThat((String) repositoryConfiguration.get("k1"), is("repo-v1"));
        assertThat(repositoryConfiguration.get("k2"), is(nullValue()));
        Map<String, Object> packageConfiguration = (Map<String, Object>) attributes.get("package-configuration");
        assertThat((String) packageConfiguration.get("k3"), is("package-v1"));
        assertThat(packageConfiguration.get("k4"), is(nullValue()));
    }

    private PackageMaterial createPackageMaterialWithSecureConfiguration() {
        PackageMaterial material = MaterialsMother.packageMaterial();
        material.getPackageDefinition().getRepository().getConfiguration().get(1).handleSecureValueConfiguration(true);
        material.getPackageDefinition().getConfiguration().addNewConfigurationWithValue("k4", "package-v2", false);
        material.getPackageDefinition().getConfiguration().get(1).handleSecureValueConfiguration(true);
        return material;
    }

    @Test
    public void shouldReturnFalseForPackageMaterial_supportsDestinationFolder() throws Exception {
        PackageMaterial material = new PackageMaterial();
        assertThat(material.supportsDestinationFolder(), is(false));
    }
}
