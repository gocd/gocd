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
package com.thoughtworks.go.config.remote;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.rules.RuleAwarePluginProfile;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoMetadataStore;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.Arrays;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static com.thoughtworks.go.helper.MaterialConfigsMother.svn;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ConfigRepoConfigTest extends AbstractRuleAwarePluginProfileTest {
    private ConfigRepoMetadataStore store = ConfigRepoMetadataStore.instance();

    @AfterEach
    void tearDown() {
        store.clear();
    }

    @Test
    public void shouldReturnPluginNameWhenSpecified() {
        ConfigRepoConfig config = new ConfigRepoConfig();
        config.setPluginId("myplugin");
        assertThat(config.getPluginId(), is("myplugin"));
    }

    @Test
    public void validate_shouldNotAllowDisabledAutoUpdate() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();

        SvnMaterialConfig svn = svn("url", false);
        svn.setAutoUpdate(false);

        ConfigRepoConfig configRepoConfig = ConfigRepoConfig.createConfigRepoConfig(svn, "plug", "id");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig));

        ConfigSaveValidationContext validationContext = ConfigSaveValidationContext.forChain(cruiseConfig);
        configRepoConfig.validate(validationContext);

        assertThat(svn.errors().isEmpty(), is(false));
        assertThat(svn.errors().on("autoUpdate"),
                is("Configuration repository material 'url' must have autoUpdate enabled."));
    }

    @Test
    public void validate_shouldCheckUniquenessOfId() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();

        ConfigRepoConfig configRepoConfig1 = ConfigRepoConfig.createConfigRepoConfig(null, "plug", "id_1");
        ConfigRepoConfig configRepoConfig2 = ConfigRepoConfig.createConfigRepoConfig(null, "plug", "id_1");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig1, configRepoConfig2));

        ConfigSaveValidationContext validationContext = ConfigSaveValidationContext.forChain(cruiseConfig);
        configRepoConfig1.validate(validationContext);

        assertThat(configRepoConfig1.errors().isEmpty(), is(false));
        assertThat(configRepoConfig1.errors().on("id"),
                is("You have defined multiple configuration repositories with the same id - 'id_1'."));
    }

    @Test
    public void validate_shouldCheckPresenceOfPluginId() {
        ConfigSaveValidationContext validationContext = ConfigSaveValidationContext.forChain(new BasicCruiseConfig());

        ConfigRepoConfig configRepo = ConfigRepoConfig.createConfigRepoConfig(null, null, "id_1");
        configRepo.validate(validationContext);

        assertThat(configRepo.errors().isEmpty(), is(false));
        assertThat(configRepo.errors().on("pluginId"),
                is("Configuration repository cannot have a blank plugin id."));
    }

    @Test
    public void validate_shouldCheckUniquenessOfMaterial() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        SvnMaterialConfig svn = svn("url", false);

        ConfigRepoConfig configRepoConfig1 = ConfigRepoConfig.createConfigRepoConfig(svn, "plug", "id_1");
        ConfigRepoConfig configRepoConfig2 = ConfigRepoConfig.createConfigRepoConfig(svn, "plug", "id_2");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig1, configRepoConfig2));

        ConfigSaveValidationContext validationContext = ConfigSaveValidationContext.forChain(cruiseConfig);
        configRepoConfig1.validate(validationContext);

        assertThat(configRepoConfig1.errors().isEmpty(), is(false));
        assertThat(configRepoConfig1.errors().on("material"),
                is("You have defined multiple configuration repositories with the same repository - 'url'."));
    }

    @Test
    public void validate_shouldValidateTheMaterialConfig() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        GitMaterialConfig materialConfig = git(null, "master");

        ConfigRepoConfig configRepoConfig = ConfigRepoConfig.createConfigRepoConfig(materialConfig, "plug", "id");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig));

        ConfigSaveValidationContext validationContext = ConfigSaveValidationContext.forChain(cruiseConfig);
        configRepoConfig.validate(validationContext);

        assertThat(configRepoConfig.getRepo().errors().on("url"), is("URL cannot be blank"));
    }

    @Test
    public void validateTree_shouldValidateTheMaterialConfig() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        MaterialConfig materialConfig = mock(MaterialConfig.class);
        when(materialConfig.errors()).thenReturn(new ConfigErrors());

        ConfigRepoConfig configRepoConfig = ConfigRepoConfig.createConfigRepoConfig(materialConfig, "plug", "id");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig));
        ConfigSaveValidationContext validationContext = ConfigSaveValidationContext.forChain(cruiseConfig);

        configRepoConfig.validateTree(validationContext);

        verify(materialConfig).validateTree(validationContext);
    }

    @Test
    public void validateTree_configRepoShouldBeInvalidIfMaterialConfigHasErrors() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        MaterialConfig materialConfig = mock(MaterialConfig.class);
        when(materialConfig.errors()).thenReturn(new ConfigErrors());

        ConfigRepoConfig configRepoConfig = ConfigRepoConfig.createConfigRepoConfig(materialConfig, "plug", "id");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig));
        ConfigSaveValidationContext validationContext = ConfigSaveValidationContext.forChain(cruiseConfig);

        configRepoConfig.validateTree(validationContext);
        assertTrue(configRepoConfig.errors().isEmpty());
        assertFalse(configRepoConfig.getRepo().errors().isEmpty());
    }

    @Test
    public void validate_shouldNotAllowPipelineWithSameRepositoryAndDisabledAutoUpdate() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        GoConfigMother mother = new GoConfigMother();

        MaterialConfigs materialConfigs = new MaterialConfigs();
        SvnMaterialConfig svnInConfigRepo = svn("url", false);
        SvnMaterialConfig svnInPipelineConfig = svn("url", false);
        svnInConfigRepo.setAutoUpdate(true);
        svnInPipelineConfig.setAutoUpdate(false);
        materialConfigs.add(svnInPipelineConfig);

        ConfigRepoConfig configRepoConfig = ConfigRepoConfig.createConfigRepoConfig(svnInConfigRepo, "plug", "id");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig));

        PipelineConfig pipeline1 = mother.addPipeline(cruiseConfig, "badpipe", "build", materialConfigs, "build");

        configRepoConfig.validate(ConfigSaveValidationContext.forChain(cruiseConfig, new BasicPipelineConfigs(), pipeline1));

        assertThat(svnInConfigRepo.errors().isEmpty(), is(false));
        assertThat(svnInConfigRepo.errors().on("autoUpdate"),
                is("The material of type Subversion (url) is used elsewhere with a different value for autoUpdate (\"Poll for changes\"). All copies of this material must have autoUpdate enabled or configuration repository must be removed. Config Repository: id (auto update enabled). Pipelines: badpipe (auto update disabled)"));
    }

    @Test
    public void hasSameMaterial_shouldReturnTrueWhenFingerprintEquals() {
        MaterialConfig configRepo = git("url", "branch");
        MaterialConfig someRepo = git("url", "branch");
        ConfigRepoConfig config = ConfigRepoConfig.createConfigRepoConfig(configRepo, "myplugin", "id");

        assertThat(config.hasSameMaterial(someRepo), is(true));
    }

    @Test
    public void hasSameMaterial_shouldReturnFalseWhenFingerprintNotEquals() {
        MaterialConfig configRepo = git("url", "branch");
        MaterialConfig someRepo = git("url", "branch1");
        ConfigRepoConfig config = ConfigRepoConfig.createConfigRepoConfig(configRepo, "myplugin", "id");

        assertThat(config.hasSameMaterial(someRepo), is(false));
    }

    @Test
    public void hasSameMaterial_shouldReturnTrueWhenFingerprintEquals_AndDestinationDirectoriesAreDifferent() {
        MaterialConfig configRepo = git("url", "branch");
        GitMaterialConfig someRepo = git("url", "branch");
        someRepo.setFolder("someFolder");
        ConfigRepoConfig config = ConfigRepoConfig.createConfigRepoConfig(configRepo, "myplugin", "id");

        assertThat(config.hasSameMaterial(someRepo), is(true));
    }

    @Test
    public void hasMaterialWithFingerprint_shouldReturnTrueWhenFingerprintEquals() {
        MaterialConfig configRepo = git("url", "branch");
        GitMaterialConfig someRepo = git("url", "branch");
        someRepo.setFolder("someFolder");
        ConfigRepoConfig config = ConfigRepoConfig.createConfigRepoConfig(configRepo, "myplugin", "id");

        assertThat(config.hasMaterialWithFingerprint(someRepo.getFingerprint()), is(true));
    }

    @Test
    public void hasMaterialWithFingerprint_shouldReturnFalseWhenFingerprintNotEquals() {
        MaterialConfig configRepo = git("url", "branch");
        GitMaterialConfig someRepo = git("url", "branch1");
        ConfigRepoConfig config = ConfigRepoConfig.createConfigRepoConfig(configRepo, "myplugin", "id");

        assertThat(config.hasMaterialWithFingerprint(someRepo.getFingerprint()), is(false));
    }

    @Override
    protected RuleAwarePluginProfile newPluginProfile(String id, String pluginId, ConfigurationProperty... configurationProperties) {
        RuleAwarePluginProfile profile = new ConfigRepoConfig().setId(id).setPluginId(pluginId);
        profile.addConfigurations(Arrays.asList(configurationProperties));
        ((ConfigRepoConfig) profile).setRepo(new GitMaterialConfig());
        return profile;
    }

    @Override
    protected String getObjectDescription() {
        return "Configuration repository";
    }

    @Override
    public ValidationContext getValidationContext(RuleAwarePluginProfile profile) {
        return ConfigSaveValidationContext.forChain(new BasicCruiseConfig());
    }
}
