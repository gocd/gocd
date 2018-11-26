/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config.remote;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.GoConfigMother;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ConfigRepoConfigTest {
    @Test
    public void shouldReturnPluginNameWhenSpecified() {
        ConfigRepoConfig config = new ConfigRepoConfig();
        config.setPluginId("myplugin");
        assertThat(config.getPluginId(),is("myplugin"));
    }
    @Test
    public void shouldReturnNullPluginNameWhenEmpty() {
        ConfigRepoConfig config = new ConfigRepoConfig();
        config.setPluginId("");
        assertNull(config.getPluginId());
    }

    @Test
    public void validate_shouldNotAllowDisabledAutoUpdate() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();

        SvnMaterialConfig svn = new SvnMaterialConfig("url", false);
        svn.setAutoUpdate(false);

        ConfigRepoConfig configRepoConfig = new ConfigRepoConfig(svn,"plug");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig));

        ConfigSaveValidationContext validationContext = ConfigSaveValidationContext.forChain(cruiseConfig);
        configRepoConfig.validate(validationContext);

        assertThat(svn.errors().isEmpty(),is(false));
        assertThat(svn.errors().on("autoUpdate"),
                is("Configuration repository material 'url' must have autoUpdate enabled."));
    }

    @Test
    public void validate_shouldCheckUniquenessOfId() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();

        ConfigRepoConfig configRepoConfig1 = new ConfigRepoConfig(null,"plug", "id_1");
        ConfigRepoConfig configRepoConfig2 = new ConfigRepoConfig(null,"plug", "id_1");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig1, configRepoConfig2));

        ConfigSaveValidationContext validationContext = ConfigSaveValidationContext.forChain(cruiseConfig);
        configRepoConfig1.validate(validationContext);

        assertThat(configRepoConfig1.errors().isEmpty(),is(false));
        assertThat(configRepoConfig1.errors().on("id"),
                is("You have defined multiple configuration repositories with the same id - 'id_1'."));
    }

    @Test
    public void validate_shouldCheckUniquenessOfMaterial() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        SvnMaterialConfig svn = new SvnMaterialConfig("url", false);

        ConfigRepoConfig configRepoConfig1 = new ConfigRepoConfig(svn,"plug", "id_1");
        ConfigRepoConfig configRepoConfig2 = new ConfigRepoConfig(svn,"plug", "id_2");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig1, configRepoConfig2));

        ConfigSaveValidationContext validationContext = ConfigSaveValidationContext.forChain(cruiseConfig);
        configRepoConfig1.validate(validationContext);

        assertThat(configRepoConfig1.errors().isEmpty(),is(false));
        assertThat(configRepoConfig1.errors().on("material"),
                is("You have defined multiple configuration repositories with the same repository - 'url'."));
    }

    @Test
    public void validate_shouldValidateTheMaterialConfig() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        GitMaterialConfig materialConfig = new GitMaterialConfig(null, "master");

        ConfigRepoConfig configRepoConfig = new ConfigRepoConfig(materialConfig, "plug");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig));

        ConfigSaveValidationContext validationContext = ConfigSaveValidationContext.forChain(cruiseConfig);
        configRepoConfig.validate(validationContext);

        assertThat(configRepoConfig.getMaterialConfig().errors().on("url"), is("URL cannot be blank"));
    }

    @Test
    public void validateTree_shouldValidateTheMaterialConfig() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        MaterialConfig materialConfig = mock(MaterialConfig.class);
        when(materialConfig.errors()).thenReturn(new ConfigErrors());

        ConfigRepoConfig configRepoConfig = new ConfigRepoConfig(materialConfig, "plug");
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

        ConfigRepoConfig configRepoConfig = new ConfigRepoConfig(materialConfig, "plug", "id");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig));
        ConfigSaveValidationContext validationContext = ConfigSaveValidationContext.forChain(cruiseConfig);

        assertFalse(configRepoConfig.validateTree(validationContext));
        assertTrue(configRepoConfig.errors().isEmpty());
        assertFalse(configRepoConfig.getMaterialConfig().errors().isEmpty());
    }

    @Test
    public void validate_shouldNotAllowPipelineWithSameRepositoryAndDisabledAutoUpdate() {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        GoConfigMother mother = new GoConfigMother();

        MaterialConfigs materialConfigs = new MaterialConfigs();
        SvnMaterialConfig svnInConfigRepo = new SvnMaterialConfig("url", false);
        SvnMaterialConfig svnInPipelineConfig = new SvnMaterialConfig("url", false);
        svnInConfigRepo.setAutoUpdate(true);
        svnInPipelineConfig.setAutoUpdate(false);
        materialConfigs.add(svnInPipelineConfig);

        ConfigRepoConfig configRepoConfig = new ConfigRepoConfig(svnInConfigRepo,"plug");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(configRepoConfig));

        PipelineConfig pipeline1 = mother.addPipeline(cruiseConfig, "badpipe", "build", materialConfigs, "build");

        configRepoConfig.validate(ConfigSaveValidationContext.forChain(cruiseConfig, new BasicPipelineConfigs(), pipeline1));

        assertThat(svnInConfigRepo.errors().isEmpty(),is(false));
        assertThat(svnInConfigRepo.errors().on("autoUpdate"),
                is("Material of type Subversion (url) is specified as a configuration repository and pipeline material with disabled autoUpdate. All copies of this material must have autoUpdate enabled or configuration repository must be removed"));
    }

    @Test
    public void hasSameMaterial_shouldReturnTrueWhenFingerprintEquals(){
        MaterialConfig configRepo = new GitMaterialConfig("url","branch");
        MaterialConfig someRepo = new GitMaterialConfig("url","branch");
        ConfigRepoConfig config = new ConfigRepoConfig(configRepo,"myplugin");

        assertThat(config.hasSameMaterial(someRepo),is(true));
    }

    @Test
    public void hasSameMaterial_shouldReturnFalseWhenFingerprintNotEquals(){
        MaterialConfig configRepo = new GitMaterialConfig("url","branch");
        MaterialConfig someRepo = new GitMaterialConfig("url","branch1");
        ConfigRepoConfig config = new ConfigRepoConfig(configRepo,"myplugin");

        assertThat(config.hasSameMaterial(someRepo),is(false));
    }

    @Test
    public void hasSameMaterial_shouldReturnTrueWhenFingerprintEquals_AndDestinationDirectoriesAreDifferent(){
        MaterialConfig configRepo = new GitMaterialConfig("url","branch");
        GitMaterialConfig someRepo = new GitMaterialConfig("url","branch");
        someRepo.setFolder("someFolder");
        ConfigRepoConfig config = new ConfigRepoConfig(configRepo,"myplugin");

        assertThat(config.hasSameMaterial(someRepo),is(true));
    }

    @Test
    public void hasMaterialWithFingerprint_shouldReturnTrueWhenFingerprintEquals(){
        MaterialConfig configRepo = new GitMaterialConfig("url","branch");
        GitMaterialConfig someRepo = new GitMaterialConfig("url","branch");
        someRepo.setFolder("someFolder");
        ConfigRepoConfig config = new ConfigRepoConfig(configRepo,"myplugin");

        assertThat(config.hasMaterialWithFingerprint(someRepo.getFingerprint()),is(true));
    }

    @Test
    public void hasMaterialWithFingerprint_shouldReturnFalseWhenFingerprintNotEquals(){
        MaterialConfig configRepo = new GitMaterialConfig("url","branch");
        GitMaterialConfig someRepo = new GitMaterialConfig("url","branch1");
        ConfigRepoConfig config = new ConfigRepoConfig(configRepo,"myplugin");

        assertThat(config.hasMaterialWithFingerprint(someRepo.getFingerprint()),is(false));
    }
}
