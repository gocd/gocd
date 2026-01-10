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

import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.update.FullConfigUpdateCommand;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;
import com.thoughtworks.go.plugin.domain.artifact.Capabilities;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TestFileUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public abstract class FullConfigSaveFlowTestBase {
    @Autowired
    private GoConfigMigration goConfigMigration;
    @Autowired
    private ConfigElementImplementationRegistry registry;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private GoConfigService goConfigService;
    private MagicalGoConfigXmlLoader loader;
    private String xml;
    private GoConfigFileHelper configHelper;

    @BeforeEach
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper(goConfigDao);
        configHelper.onSetUp();
        xml = goConfigMigration.upgradeIfNecessary(TestFileUtil.resourceToString("/data/pluggable_artifacts_with_params.xml"));
        loader = new MagicalGoConfigXmlLoader(registry);
        setupMetadataForPlugin();
    }

    @AfterEach
    public void tearDown() {
        ArtifactMetadataStore.instance().clear();
        configHelper.onTearDown();
    }

    protected abstract FullConfigSaveFlow getImplementer();

    @Test
    public void shouldEncryptPluginPropertiesOfPublishTask() throws Exception {
        CruiseConfig cruiseConfig = loader.deserializeConfig(xml);
        Configuration ancestorPluggablePublishArtifactConfigBeforeEncryption = cruiseConfig
                .pipelineConfigByName(new CaseInsensitiveString("ancestor"))
                .getExternalArtifactConfigs().getFirst().getConfiguration();
        assertThat(ancestorPluggablePublishArtifactConfigBeforeEncryption.getProperty("Image").getValue()).isEqualTo("IMAGE_SECRET");
        assertThat(ancestorPluggablePublishArtifactConfigBeforeEncryption.getProperty("Image").getEncryptedValue()).isNull();
        assertThat(ancestorPluggablePublishArtifactConfigBeforeEncryption.getProperty("Image").getConfigValue()).isEqualTo("IMAGE_SECRET");

        GoConfigHolder configHolder = getImplementer().execute(new FullConfigUpdateCommand(cruiseConfig, configHelper.currentConfig().getMd5()), new ArrayList<>(), "Upgrade");
        Configuration ancestorPluggablePublishArtifactConfigAfterEncryption = configHolder.configForEdit
                .pipelineConfigByName(new CaseInsensitiveString("ancestor"))
                .getExternalArtifactConfigs().getFirst().getConfiguration();

        assertThat(ancestorPluggablePublishArtifactConfigAfterEncryption.getProperty("Image").getValue()).isEqualTo("IMAGE_SECRET");
        assertThat(ancestorPluggablePublishArtifactConfigAfterEncryption.getProperty("Image").getEncryptedValue()).startsWith("AES:");
        assertThat(ancestorPluggablePublishArtifactConfigAfterEncryption.getProperty("Image").getConfigValue()).isNull();

        //verify xml on disk contains encrypted Image plugin property
        assertThat(configHelper.getCurrentXml()).contains(ancestorPluggablePublishArtifactConfigAfterEncryption.getProperty("Image").getEncryptedValue());
        //verify xml from GoConfigHolder contains encrypted Image plugin property
        assertThat(getImplementer().toXmlString(configHolder.configForEdit)).contains(ancestorPluggablePublishArtifactConfigAfterEncryption.getProperty("Image").getEncryptedValue());
    }

    @Test
    public void shouldEncryptPluginPropertiesOfFetchTask() throws Exception {
        CruiseConfig cruiseConfig = loader.deserializeConfig(xml);
        Configuration childFetchConfigBeforeEncryption = ((FetchPluggableArtifactTask) cruiseConfig
                .pipelineConfigByName(new CaseInsensitiveString("child"))
                .getFirst().getJobs().getFirst().tasks().getFirst()).getConfiguration();


        assertThat(childFetchConfigBeforeEncryption.getProperty("FetchProperty").getValue()).isEqualTo("SECRET");
        assertThat(childFetchConfigBeforeEncryption.getProperty("FetchProperty").getEncryptedValue()).isNull();
        assertThat(childFetchConfigBeforeEncryption.getProperty("FetchProperty").getConfigValue()).isEqualTo("SECRET");

        GoConfigHolder configHolder = getImplementer().execute(new FullConfigUpdateCommand(cruiseConfig, configHelper.currentConfig().getMd5()), new ArrayList<>(), "Upgrade");
        Configuration childFetchConfigAfterEncryption = ((FetchPluggableArtifactTask) configHolder.configForEdit
                .pipelineConfigByName(new CaseInsensitiveString("child"))
                .getFirst().getJobs().getFirst().tasks().getFirst()).getConfiguration();
        assertThat(childFetchConfigAfterEncryption.getProperty("FetchProperty").getValue()).isEqualTo("SECRET");
        assertThat(childFetchConfigAfterEncryption.getProperty("FetchProperty").getEncryptedValue()).startsWith("AES:");
        assertThat(childFetchConfigAfterEncryption.getProperty("FetchProperty").getConfigValue()).isNull();
    }

    private void setupMetadataForPlugin() {
        PluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id("cd.go.artifact.docker.registry").build();
        PluginConfiguration buildFile = new PluginConfiguration("BuildFile", new Metadata(false, false));
        PluginConfiguration image = new PluginConfiguration("Image", new Metadata(false, true));
        PluginConfiguration tag = new PluginConfiguration("Tag", new Metadata(false, false));
        PluginConfiguration fetchProperty = new PluginConfiguration("FetchProperty", new Metadata(false, true));
        PluginConfiguration fetchTag = new PluginConfiguration("Tag", new Metadata(false, false));
        PluginConfiguration registryUrl = new PluginConfiguration("RegistryURL", new Metadata(true, false));
        PluginConfiguration username = new PluginConfiguration("Username", new Metadata(false, false));
        PluginConfiguration password = new PluginConfiguration("Password", new Metadata(false, true));
        PluggableInstanceSettings storeConfigSettings = new PluggableInstanceSettings(List.of(registryUrl, username, password));
        PluggableInstanceSettings publishArtifactSettings = new PluggableInstanceSettings(List.of(buildFile, image, tag));
        PluggableInstanceSettings fetchArtifactSettings = new PluggableInstanceSettings(List.of(fetchProperty, fetchTag));
        ArtifactPluginInfo artifactPluginInfo = new ArtifactPluginInfo(pluginDescriptor, storeConfigSettings, publishArtifactSettings, fetchArtifactSettings, null, new Capabilities());
        ArtifactMetadataStore.instance().setPluginInfo(artifactPluginInfo);
    }
}
