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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.elastic.ClusterProfile;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.preprocessor.ConfigParamPreprocessor;
import com.thoughtworks.go.config.remote.*;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationKey;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.ConfigurationValue;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.plugin.access.artifact.ArtifactMetadataStore;
import com.thoughtworks.go.plugin.access.authorization.AuthorizationMetadataStore;
import com.thoughtworks.go.plugin.access.elastic.ElasticAgentMetadataStore;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.artifact.ArtifactPluginInfo;
import com.thoughtworks.go.plugin.domain.artifact.Capabilities;
import com.thoughtworks.go.plugin.domain.authorization.AuthorizationPluginInfo;
import com.thoughtworks.go.plugin.domain.authorization.SupportedAuthType;
import com.thoughtworks.go.plugin.domain.common.Metadata;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConfiguration;
import com.thoughtworks.go.plugin.domain.elastic.ElasticAgentPluginInfo;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.security.ResetCipher;
import com.thoughtworks.go.util.ClonerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.helper.MaterialConfigsMother.*;
import static com.thoughtworks.go.helper.PipelineConfigMother.*;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class BasicCruiseConfigTest extends CruiseConfigTestBase {
    @Rule
    public final ResetCipher resetCipher = new ResetCipher();

    @Before
    public void setup() throws Exception {
        pipelines = new BasicPipelineConfigs("existing_group", new Authorization());
        cruiseConfig = new BasicCruiseConfig(pipelines);
        goConfigMother = new GoConfigMother();
    }

    @After
    public void clear() {
        ArtifactMetadataStore.instance().clear();
    }

    @Override
    protected BasicCruiseConfig createCruiseConfig(BasicPipelineConfigs pipelineConfigs) {
        return new BasicCruiseConfig(pipelineConfigs);
    }

    @Override
    protected BasicCruiseConfig createCruiseConfig() {
        return new BasicCruiseConfig();
    }

    @Test
    public void getAllLocalPipelineConfigs_shouldReturnOnlyLocalPipelinesWhenNoRemotes() {
        PipelineConfig pipeline1 = createPipelineConfig("local-pipe-1", "stage1");
        cruiseConfig.getGroups().addPipeline("existing_group", pipeline1);

        List<PipelineConfig> localPipelines = cruiseConfig.getAllLocalPipelineConfigs(false);
        assertThat(localPipelines.size(), is(1));
        assertThat(localPipelines, hasItem(pipeline1));
    }

    @Test
    public void shouldGenerateAMapOfAllPipelinesAndTheirParentDependencies() {
        /*
        *    -----+ p2 --> p4
        *  p1
        *    -----+ p3
        *
        * */
        PipelineConfig p1 = createPipelineConfig("p1", "s1", "j1");
        PipelineConfig p2 = createPipelineConfig("p2", "s2", "j1");
        p2.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p1"), new CaseInsensitiveString("s1")));
        PipelineConfig p3 = createPipelineConfig("p3", "s3", "j1");
        p3.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p1"), new CaseInsensitiveString("s1")));
        PipelineConfig p4 = createPipelineConfig("p4", "s4", "j1");
        p4.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString("p2"), new CaseInsensitiveString("s2")));
        pipelines.addAll(asList(p4, p2, p1, p3));
        Map<CaseInsensitiveString, List<PipelineConfig>> expectedPipelines = cruiseConfig.generatePipelineVsDownstreamMap();
        assertThat(expectedPipelines.size(), is(4));
        assertThat(expectedPipelines.get(new CaseInsensitiveString("p1")), hasItems(p2, p3));
        assertThat(expectedPipelines.get(new CaseInsensitiveString("p2")), hasItems(p4));
        assertThat(expectedPipelines.get(new CaseInsensitiveString("p3")).isEmpty(), is(true));
        assertThat(expectedPipelines.get(new CaseInsensitiveString("p4")).isEmpty(), is(true));
    }


    @Test
    public void shouldSetOriginInPipelines() {
        pipelines = new BasicPipelineConfigs("group_main", new Authorization(), pipelineConfig("pipe1"));
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        PipelineConfig pipe = pipelines.get(0);
        mainCruiseConfig.setOrigins(new FileConfigOrigin());
        assertThat(pipe.getOrigin(), is(new FileConfigOrigin()));
    }

    @Test
    public void shouldSetOriginInEnvironments() {
        BasicCruiseConfig mainCruiseConfig = new BasicCruiseConfig(pipelines);
        BasicEnvironmentConfig env = new BasicEnvironmentConfig(new CaseInsensitiveString("e"));
        mainCruiseConfig.addEnvironment(env);
        mainCruiseConfig.setOrigins(new FileConfigOrigin());
        assertThat(env.getOrigin(), is(new FileConfigOrigin()));
    }


    @Test
    public void shouldGetPipelinesWithGroupName() throws Exception {
        PipelineConfigs group1 = createGroup("group1", createPipelineConfig("pipeline1", "stage1"));
        PipelineConfigs group2 = createGroup("group2", createPipelineConfig("pipeline2", "stage2"));
        CruiseConfig config = createCruiseConfig();
        config.setGroup(new PipelineGroups(group1, group2));


        assertThat(config.pipelines("group1"), is(group1));
        assertThat(config.pipelines("group2"), is(group2));
    }

    @Test
    public void shouldReturnTrueForPipelineThatInFirstGroup() {
        PipelineConfigs group1 = createGroup("group1", createPipelineConfig("pipeline1", "stage1"));
        CruiseConfig config = new BasicCruiseConfig(group1);
        assertThat("shouldReturnTrueForPipelineThatInFirstGroup", config.isInFirstGroup(new CaseInsensitiveString("pipeline1")), is(true));
    }

    @Test
    public void shouldReturnFalseForPipelineThatNotInFirstGroup() {
        PipelineConfigs group1 = createGroup("group1", createPipelineConfig("pipeline1", "stage1"));
        PipelineConfigs group2 = createGroup("group2", createPipelineConfig("pipeline2", "stage2"));
        CruiseConfig config = new BasicCruiseConfig(group1, group2);
        assertThat("shouldReturnFalseForPipelineThatNotInFirstGroup", config.isInFirstGroup(new CaseInsensitiveString("pipeline2")), is(false));
    }

    @Test
    public void shouldIncludeRemotePipelinesAsPartOfCachedPipelineConfigs() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1", "p2");
        ConfigRepoConfig repoConfig1 = ConfigRepoConfig.createConfigRepoConfig(gitMaterialConfig("url1"), "plugin", "id-1");
        ConfigRepoConfig repoConfig2 = ConfigRepoConfig.createConfigRepoConfig(gitMaterialConfig("url2"), "plugin", "id-1");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(repoConfig1, repoConfig2));
        PartialConfig partialConfigInRepo1 = PartialConfigMother.withPipeline("pipeline_in_repo1", new RepoConfigOrigin(repoConfig1, "repo1_r1"));
        PartialConfig partialConfigInRepo2 = PartialConfigMother.withPipeline("pipeline_in_repo2", new RepoConfigOrigin(repoConfig2, "repo2_r1"));

        cruiseConfig.merge(asList(partialConfigInRepo1, partialConfigInRepo2), false);
        assertThat(cruiseConfig.getAllPipelineNames().contains(new CaseInsensitiveString("pipeline_in_repo1")), is(true));
        assertThat(cruiseConfig.getAllPipelineNames().contains(new CaseInsensitiveString("pipeline_in_repo2")), is(true));
    }

    @Test
    public void shouldRejectRemotePipelinesNotOriginatingFromRegisteredConfigReposFromCachedPipelineConfigs() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("p1", "p2");
        ConfigRepoConfig repoConfig1 = ConfigRepoConfig.createConfigRepoConfig(gitMaterialConfig("url1"), "plugin", "id-1");
        ConfigRepoConfig repoConfig2 = ConfigRepoConfig.createConfigRepoConfig(gitMaterialConfig("url2"), "plugin", "id-1");
        cruiseConfig.setConfigRepos(new ConfigReposConfig(repoConfig2));
        PartialConfig partialConfigInRepo1 = PartialConfigMother.withPipeline("pipeline_in_repo1", new RepoConfigOrigin(repoConfig1, "repo1_r1"));
        PartialConfig partialConfigInRepo2 = PartialConfigMother.withPipeline("pipeline_in_repo2", new RepoConfigOrigin(repoConfig2, "repo2_r1"));
        cruiseConfig.merge(asList(partialConfigInRepo1, partialConfigInRepo2), false);
        assertThat(cruiseConfig.getAllPipelineNames().contains(new CaseInsensitiveString("pipeline_in_repo1")), is(false));
        assertThat(cruiseConfig.getAllPipelineNames().contains(new CaseInsensitiveString("pipeline_in_repo2")), is(true));
    }

    @Test
    public void shouldReturnAListOfPipelineNamesAssociatedWithOneTemplate() {
        ArrayList<CaseInsensitiveString> pipelinesAssociatedWithATemplate = new ArrayList<>();
        pipelinesAssociatedWithATemplate.add(new CaseInsensitiveString("p1"));
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");

        assertThat(cruiseConfig.pipelinesAssociatedWithTemplate(new CaseInsensitiveString("t1")), is(pipelinesAssociatedWithATemplate));
    }

    @Test
    public void shouldReturnNullForAssociatedPipelineNamesWhenTemplateNameIsBlank() {
        ArrayList<CaseInsensitiveString> pipelinesAssociatedWithATemplate = new ArrayList<>();
        pipelinesAssociatedWithATemplate.add(new CaseInsensitiveString("p1"));
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");

        assertThat(cruiseConfig.pipelinesAssociatedWithTemplate(new CaseInsensitiveString("")), is(new ArrayList<CaseInsensitiveString>()));
    }

    @Test
    public void shouldReturnAnEmptyListForPipelinesIfTemplateNameIsNull() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();

        assertThat(cruiseConfig.pipelinesAssociatedWithTemplate(null).isEmpty(), is(true));
    }

    @Test
    public void shouldReturnAnEmptyListIfThereAreNoPipelinesAssociatedWithGivenTemplate() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();

        assertThat(cruiseConfig.pipelinesAssociatedWithTemplate(new CaseInsensitiveString("non-existent-template")).isEmpty(), is(true));
    }

    @Test
    public void shouldGetAllGroupsForAnAdminUser() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        new GoConfigMother().addPipelineWithGroup(cruiseConfig, "group", "p1", "s1", "j1");
        GoConfigMother.enableSecurityWithPasswordFilePlugin(cruiseConfig);
        GoConfigMother.addUserAsSuperAdmin(cruiseConfig, "superadmin");

        List<String> groupsForUser = cruiseConfig.getGroupsForUser(new CaseInsensitiveString("superadmin"), new ArrayList<>());

        assertThat(groupsForUser, contains("group"));
    }

    @Test
    public void shouldGetAllGroupsForUserInAnAdminRole() {
        GoConfigMother goConfigMother = new GoConfigMother();
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        goConfigMother.addPipelineWithGroup(cruiseConfig, "group", "p1", "s1", "j1");
        GoConfigMother.enableSecurityWithPasswordFilePlugin(cruiseConfig);

        Role role = goConfigMother.createRole("role1", "foo", "bar");
        cruiseConfig.server().security().addRole(role);
        goConfigMother.addRoleAsSuperAdmin(cruiseConfig, "role1");

        ArrayList<Role> roles = new ArrayList<>();
        roles.add(role);

        List<String> groupsForUser = cruiseConfig.getGroupsForUser(new CaseInsensitiveString("foo"), roles);

        assertThat(groupsForUser, contains("group"));
    }

    @Test
    public void shouldGetSpecificGroupsForAGroupAdminUser() {
        GoConfigMother goConfigMother = new GoConfigMother();
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        GoConfigMother.enableSecurityWithPasswordFilePlugin(cruiseConfig);
        GoConfigMother.addUserAsSuperAdmin(cruiseConfig, "superadmin");
        goConfigMother.addPipelineWithGroup(cruiseConfig, "group1", "p1", "s1", "j1");
        goConfigMother.addPipelineWithGroup(cruiseConfig, "group2", "p2", "s1", "j1");
        goConfigMother.addPipelineWithGroup(cruiseConfig, "group3", "p3", "s1", "j1");

        goConfigMother.addAdminUserForPipelineGroup(cruiseConfig, "foo", "group1");
        goConfigMother.addAdminUserForPipelineGroup(cruiseConfig, "foo", "group2");

        List<String> groupsForUser = cruiseConfig.getGroupsForUser(new CaseInsensitiveString("foo"), new ArrayList<>());

        assertThat(groupsForUser, not(contains("group3")));
        assertThat(groupsForUser, containsInAnyOrder("group2", "group1"));
    }

    @Test
    public void shouldGetSpecificGroupsForAUserInGroupAdminRole() {
        GoConfigMother goConfigMother = new GoConfigMother();
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        GoConfigMother.enableSecurityWithPasswordFilePlugin(cruiseConfig);
        GoConfigMother.addUserAsSuperAdmin(cruiseConfig, "superadmin");
        goConfigMother.addPipelineWithGroup(cruiseConfig, "group1", "p1", "s1", "j1");
        goConfigMother.addPipelineWithGroup(cruiseConfig, "group2", "p2", "s1", "j1");
        goConfigMother.addPipelineWithGroup(cruiseConfig, "group3", "p3", "s1", "j1");

        Role role = goConfigMother.createRole("role1", "foo", "bar");
        cruiseConfig.server().security().addRole(role);
        goConfigMother.addAdminRoleForPipelineGroup(cruiseConfig, "role1", "group1");
        goConfigMother.addAdminRoleForPipelineGroup(cruiseConfig, "role1", "group2");

        ArrayList<Role> roles = new ArrayList<>();
        roles.add(role);
        List<String> groupsForUser = cruiseConfig.getGroupsForUser(new CaseInsensitiveString("foo"), roles);

        assertThat(groupsForUser, not(contains("group3")));
        assertThat(groupsForUser, containsInAnyOrder("group2", "group1"));
    }

    @Test
    public void shouldEncryptSecurePluggableArtifactConfigPropertiesOfAllTemplatesInConfig() throws IOException, CryptoException {
        setArtifactPluginInfo();
        resetCipher.setupAESCipherFile();
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.getArtifactStores().add(new ArtifactStore("store1", "cd.go.s3"));
        PipelineConfig pipelineConfig = new GoConfigMother().addPipelineWithTemplate(cruiseConfig, "p1", "t1", "s1", "j1");
        cruiseConfig.addPipeline("first", pipelineConfig);
        PipelineTemplateConfig templateConfig = cruiseConfig.getTemplates().first();
        JobConfig jobConfig = templateConfig.getStages().get(0).getJobs().get(0);
        PluggableArtifactConfig artifactConfig = new PluggableArtifactConfig("foo", "store1");
        artifactConfig.addConfigurations(asList(
                new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("pub_v1")),
                new ConfigurationProperty(new ConfigurationKey("k2"), new ConfigurationValue("pub_v2")),
                new ConfigurationProperty(new ConfigurationKey("k3"), new ConfigurationValue("pub_v3"))));
        jobConfig.artifactTypeConfigs().add(artifactConfig);

        BasicCruiseConfig preprocessed = ClonerFactory.instance().deepClone(cruiseConfig);
        new ConfigParamPreprocessor().process(preprocessed);
        cruiseConfig.encryptSecureProperties(preprocessed);

        Configuration properties = ((PluggableArtifactConfig) cruiseConfig.getTemplates().get(0).getStages().get(0).getJobs().get(0).artifactTypeConfigs().get(0)).getConfiguration();

        GoCipher goCipher = new GoCipher();
        assertThat(properties.getProperty("k1").getEncryptedValue(), is(goCipher.encrypt("pub_v1")));
        assertThat(properties.getProperty("k1").getConfigValue(), is(nullValue()));
        assertThat(properties.getProperty("k1").getValue(), is("pub_v1"));
        assertThat(properties.getProperty("k2").getEncryptedValue(), is(nullValue()));
        assertThat(properties.getProperty("k2").getConfigValue(), is("pub_v2"));
        assertThat(properties.getProperty("k2").getValue(), is("pub_v2"));
        assertThat(properties.getProperty("k3").getEncryptedValue(), is(goCipher.encrypt("pub_v3")));
        assertThat(properties.getProperty("k3").getConfigValue(), is(nullValue()));
        assertThat(properties.getProperty("k3").getValue(), is("pub_v3"));
    }

    @Test
    public void shouldEncryptSecureRoleConfigProperties() throws IOException, CryptoException {
        setAuthorizationPluginInfo();
        resetCipher.setupAESCipherFile();
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.server().security().securityAuthConfigs().add(new SecurityAuthConfig("auth1", "cd.go.github"));
        PluginRoleConfig pluginRole = new PluginRoleConfig("role1", "auth1");
        pluginRole.addConfigurations(asList(
                new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("pub_v1")),
                new ConfigurationProperty(new ConfigurationKey("k2"), new ConfigurationValue("pub_v2")),
                new ConfigurationProperty(new ConfigurationKey("k3"), new ConfigurationValue("pub_v3"))));

        cruiseConfig.server().security().getRoles().add(pluginRole);

        BasicCruiseConfig preprocessed = ClonerFactory.instance().deepClone(cruiseConfig);
        new ConfigParamPreprocessor().process(preprocessed);
        cruiseConfig.encryptSecureProperties(preprocessed);

        Configuration properties = cruiseConfig.server().security().getRoles().getPluginRoleConfigs().get(0);

        GoCipher goCipher = new GoCipher();
        assertThat(properties.getProperty("k1").getEncryptedValue(), is(goCipher.encrypt("pub_v1")));
        assertThat(properties.getProperty("k1").getConfigValue(), is(nullValue()));
        assertThat(properties.getProperty("k1").getValue(), is("pub_v1"));
        assertThat(properties.getProperty("k2").getEncryptedValue(), is(nullValue()));
        assertThat(properties.getProperty("k2").getConfigValue(), is("pub_v2"));
        assertThat(properties.getProperty("k2").getValue(), is("pub_v2"));
        assertThat(properties.getProperty("k3").getEncryptedValue(), is(goCipher.encrypt("pub_v3")));
        assertThat(properties.getProperty("k3").getConfigValue(), is(nullValue()));
        assertThat(properties.getProperty("k3").getValue(), is("pub_v3"));
    }

    @Test
    public void shouldEncryptElasticAgentProfileConfigProperties() throws IOException, CryptoException {
        setEAPluginInfo();
        resetCipher.setupAESCipherFile();
        BasicCruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.getElasticConfig().getClusterProfiles().add(new ClusterProfile("prod-cluster", "ecs"));
        ElasticProfile elasticProfile = new ElasticProfile("profile1", "prod-cluster");
        elasticProfile.addConfigurations(asList(
                new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("pub_v1")),
                new ConfigurationProperty(new ConfigurationKey("k2"), new ConfigurationValue("pub_v2")),
                new ConfigurationProperty(new ConfigurationKey("k3"), new ConfigurationValue("pub_v3"))));

        cruiseConfig.getElasticConfig().getProfiles().add(elasticProfile);

        BasicCruiseConfig preprocessed = ClonerFactory.instance().deepClone(cruiseConfig);
        new ConfigParamPreprocessor().process(preprocessed);
        cruiseConfig.encryptSecureProperties(preprocessed);

        Configuration properties = cruiseConfig.getElasticConfig().getProfiles().get(0);

        GoCipher goCipher = new GoCipher();
        assertThat(properties.getProperty("k1").getEncryptedValue(), is(goCipher.encrypt("pub_v1")));
        assertThat(properties.getProperty("k1").getConfigValue(), is(nullValue()));
        assertThat(properties.getProperty("k1").getValue(), is("pub_v1"));
        assertThat(properties.getProperty("k2").getEncryptedValue(), is(nullValue()));
        assertThat(properties.getProperty("k2").getConfigValue(), is("pub_v2"));
        assertThat(properties.getProperty("k2").getValue(), is("pub_v2"));
        assertThat(properties.getProperty("k3").getEncryptedValue(), is(goCipher.encrypt("pub_v3")));
        assertThat(properties.getProperty("k3").getConfigValue(), is(nullValue()));
        assertThat(properties.getProperty("k3").getValue(), is("pub_v3"));
    }

    @Test
    public void shouldEncryptSecurePluggableArtifactConfigPropertiesOfAllPipelinesInConfig() throws IOException, CryptoException {
        // ancestor => parent => child [fetch pluggable artifact(ancestor), fetch pluggable artifact(parent)]

        resetCipher.setupAESCipherFile();
        BasicCruiseConfig config = setupPipelines();
        BasicCruiseConfig preprocessed = ClonerFactory.instance().deepClone(config);
        new ConfigParamPreprocessor().process(preprocessed);
        config.encryptSecureProperties(preprocessed);
        PipelineConfig ancestor = config.pipelineConfigByName(new CaseInsensitiveString("ancestor"));
        PipelineConfig child = config.pipelineConfigByName(new CaseInsensitiveString("child"));

        Configuration ancestorPublishArtifactConfig = ancestor.getStage("stage1").jobConfigByConfigName("job1").artifactTypeConfigs().getPluggableArtifactConfigs().get(0).getConfiguration();
        GoCipher goCipher = new GoCipher();
        assertThat(ancestorPublishArtifactConfig.getProperty("k1").getEncryptedValue(), is(goCipher.encrypt("pub_v1")));
        assertThat(ancestorPublishArtifactConfig.getProperty("k1").getConfigValue(), is(nullValue()));
        assertThat(ancestorPublishArtifactConfig.getProperty("k1").getValue(), is("pub_v1"));
        assertThat(ancestorPublishArtifactConfig.getProperty("k2").getEncryptedValue(), is(nullValue()));
        assertThat(ancestorPublishArtifactConfig.getProperty("k2").getConfigValue(), is("pub_v2"));
        assertThat(ancestorPublishArtifactConfig.getProperty("k2").getValue(), is("pub_v2"));
        assertThat(ancestorPublishArtifactConfig.getProperty("k3").getEncryptedValue(), is(goCipher.encrypt("pub_v3")));
        assertThat(ancestorPublishArtifactConfig.getProperty("k3").getConfigValue(), is(nullValue()));
        assertThat(ancestorPublishArtifactConfig.getProperty("k3").getValue(), is("pub_v3"));

        Configuration childFetchFromAncestorConfig = ((FetchPluggableArtifactTask) child.getStage("stage1")
                .jobConfigByConfigName("job1").tasks().get(0)).getConfiguration();
        assertThat(childFetchFromAncestorConfig.getProperty("k1").getEncryptedValue(), is(goCipher.encrypt("fetch_v1")));
        assertThat(childFetchFromAncestorConfig.getProperty("k1").getConfigValue(), is(nullValue()));
        assertThat(childFetchFromAncestorConfig.getProperty("k1").getValue(), is("fetch_v1"));
        assertThat(childFetchFromAncestorConfig.getProperty("k2").getEncryptedValue(), is(nullValue()));
        assertThat(childFetchFromAncestorConfig.getProperty("k2").getConfigValue(), is("fetch_v2"));
        assertThat(childFetchFromAncestorConfig.getProperty("k2").getValue(), is("fetch_v2"));
        assertThat(childFetchFromAncestorConfig.getProperty("k3").getEncryptedValue(), is(goCipher.encrypt("fetch_v3")));
        assertThat(childFetchFromAncestorConfig.getProperty("k3").getConfigValue(), is(nullValue()));
        assertThat(childFetchFromAncestorConfig.getProperty("k3").getValue(), is("fetch_v3"));

        Configuration childFetchFromParentConfig = ((FetchPluggableArtifactTask) child.getStage("stage1")
                .jobConfigByConfigName("job1").tasks().get(1)).getConfiguration();
        assertThat(childFetchFromParentConfig.getProperty("k1").getEncryptedValue(), is(goCipher.encrypt("fetch_v1")));
        assertThat(childFetchFromParentConfig.getProperty("k1").getConfigValue(), is(nullValue()));
        assertThat(childFetchFromParentConfig.getProperty("k1").getValue(), is("fetch_v1"));
        assertThat(childFetchFromParentConfig.getProperty("k2").getEncryptedValue(), is(nullValue()));
        assertThat(childFetchFromParentConfig.getProperty("k2").getConfigValue(), is("fetch_v2"));
        assertThat(childFetchFromParentConfig.getProperty("k2").getValue(), is("fetch_v2"));
        assertThat(childFetchFromParentConfig.getProperty("k3").getEncryptedValue(), is(goCipher.encrypt("fetch_v3")));
        assertThat(childFetchFromParentConfig.getProperty("k3").getConfigValue(), is(nullValue()));
        assertThat(childFetchFromParentConfig.getProperty("k3").getValue(), is("fetch_v3"));
    }

    private BasicCruiseConfig setupPipelines() {
        BasicCruiseConfig config = GoConfigMother.configWithPipelines("ancestor", "parent", "child");
        config.getArtifactStores().add(new ArtifactStore("cd.go.s3", "cd.go.s3"));
        PipelineConfig ancestor = config.pipelineConfigByName(new CaseInsensitiveString("ancestor"));
        ancestor.add(StageConfigMother.stageConfig("stage1", new JobConfigs(new JobConfig("job1"))));
        PluggableArtifactConfig pluggableArtifactConfig = new PluggableArtifactConfig("art_1", "cd.go.s3",
                new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("pub_v1")),
                new ConfigurationProperty(new ConfigurationKey("k2"), new ConfigurationValue("pub_v2")),
                new ConfigurationProperty(new ConfigurationKey("k3"), new ConfigurationValue("pub_v3")));
        ancestor.getStage("stage1").getJobs().first().artifactTypeConfigs().add(pluggableArtifactConfig);

        PipelineConfig parent = config.pipelineConfigByName(new CaseInsensitiveString("parent"));
        parent.add(StageConfigMother.stageConfig("stage1", new JobConfigs(new JobConfig("job1"))));
        parent.getStage("stage1").jobConfigByConfigName("job1").artifactTypeConfigs()
                .add(new PluggableArtifactConfig("art_2", "cd.go.s3"));

        PipelineConfig child = config.pipelineConfigByName(new CaseInsensitiveString("child"));
        child.addParam(new ParamConfig("UPSTREAM_PIPELINE", "ancestor/parent"));
        child.addParam(new ParamConfig("UPSTREAM_STAGE", "stage1"));
        child.addParam(new ParamConfig("UPSTREAM_JOB", "job1"));
        child.addParam(new ParamConfig("ARTIFACT_ID", "art_1"));
        child.setMaterialConfigs(new MaterialConfigs(dependencyMaterialConfig("parent", "stage1")));
        child.add(StageConfigMother.stageConfig("stage1", new JobConfigs(new JobConfig("job1"))));
        FetchPluggableArtifactTask fetchFromAncestor = new FetchPluggableArtifactTask(
                new CaseInsensitiveString("#{UPSTREAM_PIPELINE}"),
                new CaseInsensitiveString("#{UPSTREAM_STAGE}"),
                new CaseInsensitiveString("#{UPSTREAM_JOB}"), "#{ARTIFACT_ID}");
        fetchFromAncestor.addConfigurations(asList(
                new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("fetch_v1")),
                new ConfigurationProperty(new ConfigurationKey("k2"), new ConfigurationValue("fetch_v2")),
                new ConfigurationProperty(new ConfigurationKey("k3"), new ConfigurationValue("fetch_v3"))));
        child.getStage("stage1").getJobs().get(0).addTask(fetchFromAncestor);
        FetchPluggableArtifactTask fetchFromParent = new FetchPluggableArtifactTask(
                new CaseInsensitiveString("parent"),
                new CaseInsensitiveString("stage1"),
                new CaseInsensitiveString("job1"), "art_2");
        fetchFromParent.addConfigurations(asList(
                new ConfigurationProperty(new ConfigurationKey("k1"), new ConfigurationValue("fetch_v1")),
                new ConfigurationProperty(new ConfigurationKey("k2"), new ConfigurationValue("fetch_v2")),
                new ConfigurationProperty(new ConfigurationKey("k3"), new ConfigurationValue("fetch_v3"))));
        child.getStage("stage1").getJobs().get(0).addTask(fetchFromParent);

        setArtifactPluginInfo();
        return config;
    }

    private void setArtifactPluginInfo() {
        PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);
        PluggableInstanceSettings storeConfigSettings = new PluggableInstanceSettings(asList());
        PluginConfiguration k1 = new PluginConfiguration("k1", new Metadata(false, true));
        PluginConfiguration k2 = new PluginConfiguration("k2", new Metadata(false, false));
        PluginConfiguration k3 = new PluginConfiguration("k3", new Metadata(false, true));
        PluggableInstanceSettings publishArtifactSettings = new PluggableInstanceSettings(asList(k1, k2, k3));
        PluggableInstanceSettings fetchArtifactSettings = new PluggableInstanceSettings(asList(k1, k2, k3));
        ArtifactPluginInfo artifactPluginInfo = new ArtifactPluginInfo(pluginDescriptor, storeConfigSettings, publishArtifactSettings, fetchArtifactSettings, null, new Capabilities());
        when(pluginDescriptor.id()).thenReturn("cd.go.s3");
        ArtifactMetadataStore.instance().setPluginInfo(artifactPluginInfo);
    }

    private void setAuthorizationPluginInfo() {
        PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);

        PluginConfiguration k1 = new PluginConfiguration("k1", new Metadata(false, true));
        PluginConfiguration k2 = new PluginConfiguration("k2", new Metadata(false, false));
        PluginConfiguration k3 = new PluginConfiguration("k3", new Metadata(false, true));

        PluggableInstanceSettings authConfigSettins = new PluggableInstanceSettings(asList(k1, k2, k3));
        PluggableInstanceSettings roleConfigSettings = new PluggableInstanceSettings(asList(k1, k2, k3));

        com.thoughtworks.go.plugin.domain.authorization.Capabilities capabilities = new com.thoughtworks.go.plugin.domain.authorization.Capabilities(SupportedAuthType.Web, true, true, true);
        AuthorizationPluginInfo artifactPluginInfo = new AuthorizationPluginInfo(pluginDescriptor, authConfigSettins, roleConfigSettings, null, capabilities);
        when(pluginDescriptor.id()).thenReturn("cd.go.github");
        AuthorizationMetadataStore.instance().setPluginInfo(artifactPluginInfo);
    }

    private void setEAPluginInfo() {
        PluginDescriptor pluginDescriptor = mock(PluginDescriptor.class);

        PluginConfiguration k1 = new PluginConfiguration("k1", new Metadata(false, true));
        PluginConfiguration k2 = new PluginConfiguration("k2", new Metadata(false, false));
        PluginConfiguration k3 = new PluginConfiguration("k3", new Metadata(false, true));

        PluggableInstanceSettings clusterProfileSettings = new PluggableInstanceSettings(asList(k1, k2, k3));
        PluggableInstanceSettings profileSettings = new PluggableInstanceSettings(asList(k1, k2, k3));

        com.thoughtworks.go.plugin.domain.elastic.Capabilities capabilities = new com.thoughtworks.go.plugin.domain.elastic.Capabilities(true);
        ElasticAgentPluginInfo ecsPluginInfo = new ElasticAgentPluginInfo(pluginDescriptor, clusterProfileSettings, profileSettings, null, null, capabilities);
        when(pluginDescriptor.id()).thenReturn("ecs");
        ElasticAgentMetadataStore.instance().setPluginInfo(ecsPluginInfo);
    }

    @Test
    public void shouldDeletePipelineGroupWithGroupName() {
        PipelineConfigs group = createGroup("group", new PipelineConfig[]{});
        CruiseConfig config = createCruiseConfig();
        config.setGroup(new PipelineGroups(group));

        assertThat(config.getGroups().isEmpty(), is(false));
        config.deletePipelineGroup("group");
        assertThat(config.getGroups().isEmpty(), is(true));
    }

    @Test(expected = UnprocessableEntityException.class)
    public void shouldNotDeletePipelineGroupIfNotEmpty() {
        PipelineConfigs group = createGroup("group", createPipelineConfig("pipeline", "stage"));
        CruiseConfig config = createCruiseConfig();
        config.setGroup(new PipelineGroups(group));

        assertThat(config.getGroups().isEmpty(), is(false));
        config.deletePipelineGroup("group");
    }

    @Test
    public void shouldListAllPipelinesAssociatedWithThePackage() {
        GitMaterialConfig git = git("http://example.com");
        PackageMaterialConfig packageMaterialConfig = packageMaterialConfig();
        PipelineConfig p1 = pipelineConfig("p1", new MaterialConfigs(git, packageMaterialConfig));
        PipelineConfig p2 = pipelineConfig("p2", new MaterialConfigs(git));
        PipelineConfig p3 = pipelineConfig("p3", new MaterialConfigs(packageMaterialConfig));

        PipelineConfigs group1 = createGroup("group1", p1, p2);
        PipelineConfigs group2 = createGroup("group2", p3);

        CruiseConfig config = createCruiseConfig();
        config.setGroup(new PipelineGroups(group1, group2));

        List<PipelineConfig> pipelineConfigs = config.pipelinesAssociatedWithPackage(packageMaterialConfig.getPackageDefinition());

        assertThat(pipelineConfigs.size(), is(2));
        assertThat(pipelineConfigs.get(0).getName(), is(new CaseInsensitiveString("p1")));
        assertThat(pipelineConfigs.get(1).getName(), is(new CaseInsensitiveString("p3")));
    }

    @Test
    public void shouldListAllPipelinesAssociatedWithThePackageRepository() {
        GitMaterialConfig git = git("http://example.com");
        PackageMaterialConfig packageMaterialConfig = packageMaterialConfig();
        PipelineConfig p1 = pipelineConfig("p1", new MaterialConfigs(git, packageMaterialConfig));
        PipelineConfig p2 = pipelineConfig("p2", new MaterialConfigs(git));
        PipelineConfig p3 = pipelineConfig("p3", new MaterialConfigs(packageMaterialConfig));

        PipelineConfigs group1 = createGroup("group1", p1, p2);
        PipelineConfigs group2 = createGroup("group2", p3);

        CruiseConfig config = createCruiseConfig();
        config.setGroup(new PipelineGroups(group1, group2));

        List<PipelineConfig> pipelineConfigs = config.pipelinesAssociatedWithPackageRepository(packageMaterialConfig.getPackageDefinition().getRepository());

        assertThat(pipelineConfigs.size(), is(2));
        assertThat(pipelineConfigs.get(0).getName(), is(new CaseInsensitiveString("p1")));
        assertThat(pipelineConfigs.get(1).getName(), is(new CaseInsensitiveString("p3")));
    }
}
