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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;
import com.thoughtworks.go.plugin.domain.artifact.Capabilities;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})

public class PipelineConfigsServiceIntegrationTest {
    @Autowired
    private PipelineConfigsService pipelineConfigsService;
    @Autowired
    private GoConfigMigration goConfigMigration;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private GoConfigDao goConfigDao;
    private String xml;
    private GoConfigFileHelper configHelper;

    @Before
    public void setUp() throws Exception {
        configHelper = new GoConfigFileHelper();
        xml = goConfigMigration.upgradeIfNecessary(IOUtils.toString(getClass().getResourceAsStream("/data/config_with_pluggable_artifacts_store.xml"), UTF_8));
        setupMetadataForPlugin();

        configHelper.usingCruiseConfigDao(goConfigDao).initializeConfigFile();
        configHelper.onSetUp();
        configHelper.writeXmlToConfigFile(xml);
        goConfigService.forceNotifyListeners();
    }

    @After
    public void tearDown() throws Exception {
        ArtifactMetadataStore.instance().clear();
    }

    @Test
    public void shouldEncryptPluginPropertiesOfPublishTask() throws Exception {
        pipelineConfigsService.updateXml("first",
                groupSnippetWithSecurePropertiesBeforeEncryption(), goConfigService.configFileMd5(),
                new Username("user"), new HttpLocalizedOperationResult());


        PipelineConfig ancestor = goConfigDao.loadConfigHolder().configForEdit.pipelineConfigByName(new CaseInsensitiveString("ancestor"));
        Configuration ancestorPluggablePublishAftifactConfigAfterEncryption = ancestor
                .getExternalArtifactConfigs().get(0).getConfiguration();
        assertThat(ancestorPluggablePublishAftifactConfigAfterEncryption.getProperty("Image").getValue(), is("SECRET"));
        assertThat(ancestorPluggablePublishAftifactConfigAfterEncryption.getProperty("Image").getEncryptedValue(), startsWith("AES:"));
        assertThat(ancestorPluggablePublishAftifactConfigAfterEncryption.getProperty("Image").getConfigValue(), is(nullValue()));
    }

    @Test
    public void shouldEncryptPluginPropertiesOfFetchTask() throws Exception {
        pipelineConfigsService.updateXml("first",
                groupSnippetWithSecurePropertiesBeforeEncryption(), goConfigService.configFileMd5(),
                new Username("user"), new HttpLocalizedOperationResult());


        PipelineConfig child = goConfigDao.loadConfigHolder().configForEdit.pipelineConfigByName(new CaseInsensitiveString("child"));
        Configuration childFetchConfigAfterEncryption = ((FetchPluggableArtifactTask) child
                .get(0).getJobs().get(0).tasks().get(0)).getConfiguration();

        assertThat(childFetchConfigAfterEncryption.getProperty("FetchProperty").getValue(), is("SECRET"));
        assertThat(childFetchConfigAfterEncryption.getProperty("FetchProperty").getEncryptedValue(), startsWith("AES:"));
        assertThat(childFetchConfigAfterEncryption.getProperty("FetchProperty").getConfigValue(), is(nullValue()));
    }

    private void setupMetadataForPlugin() {
        PluginDescriptor pluginDescriptor = GoPluginDescriptor.builder().id("cd.go.artifact.docker.registry").build();
        com.thoughtworks.go.plugin.domain.common.PluginConfiguration buildFile = new com.thoughtworks.go.plugin.domain.common.PluginConfiguration("BuildFile", new Metadata(false, false));
        com.thoughtworks.go.plugin.domain.common.PluginConfiguration image = new com.thoughtworks.go.plugin.domain.common.PluginConfiguration("Image", new Metadata(false, true));
        com.thoughtworks.go.plugin.domain.common.PluginConfiguration tag = new com.thoughtworks.go.plugin.domain.common.PluginConfiguration("Tag", new Metadata(false, false));
        com.thoughtworks.go.plugin.domain.common.PluginConfiguration fetchProperty = new com.thoughtworks.go.plugin.domain.common.PluginConfiguration("FetchProperty", new Metadata(false, true));
        com.thoughtworks.go.plugin.domain.common.PluginConfiguration fetchTag = new com.thoughtworks.go.plugin.domain.common.PluginConfiguration("Tag", new Metadata(false, false));
        com.thoughtworks.go.plugin.domain.common.PluginConfiguration registryUrl = new com.thoughtworks.go.plugin.domain.common.PluginConfiguration("RegistryURL", new Metadata(true, false));
        com.thoughtworks.go.plugin.domain.common.PluginConfiguration username = new com.thoughtworks.go.plugin.domain.common.PluginConfiguration("Username", new Metadata(false, false));
        com.thoughtworks.go.plugin.domain.common.PluginConfiguration password = new PluginConfiguration("Password", new Metadata(false, true));
        PluggableInstanceSettings storeConfigSettings = new PluggableInstanceSettings(asList(registryUrl, username, password));
        PluggableInstanceSettings publishArtifactSettings = new PluggableInstanceSettings(asList(buildFile, image, tag));
        PluggableInstanceSettings fetchArtifactSettings = new PluggableInstanceSettings(asList(fetchProperty, fetchTag));
        ArtifactPluginInfo artifactPluginInfo = new ArtifactPluginInfo(pluginDescriptor, storeConfigSettings, publishArtifactSettings, fetchArtifactSettings, null, new Capabilities());
        ArtifactMetadataStore.instance().setPluginInfo(artifactPluginInfo);
    }

    private String groupSnippetWithSecurePropertiesBeforeEncryption() throws IOException {
        return IOUtils.toString(getClass().getResourceAsStream("/data/pipeline_group_snippet_with_pluggable_artifacts.xml"), UTF_8);
    }

}
