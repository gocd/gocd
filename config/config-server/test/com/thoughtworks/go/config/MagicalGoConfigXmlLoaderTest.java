/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.materials.*;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.materials.tfs.TfsMaterialConfig;
import com.thoughtworks.go.config.merge.MergeConfigOrigin;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.preprocessor.ConfigParamPreprocessor;
import com.thoughtworks.go.config.preprocessor.ConfigRepoPartialPreprocessor;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.server.security.ldap.BaseConfig;
import com.thoughtworks.go.config.validation.*;
import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.config.Admin;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.RepositoryMetadataStoreHelper;
import com.thoughtworks.go.domain.label.PipelineLabel;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfiguration;
import com.thoughtworks.go.plugin.access.packagematerial.PackageConfigurations;
import com.thoughtworks.go.plugin.access.packagematerial.PackageMetadataStore;
import com.thoughtworks.go.plugin.access.packagematerial.RepositoryMetadataStore;
import com.thoughtworks.go.plugin.access.pluggabletask.PluggableTaskConfigStore;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskPreference;
import com.thoughtworks.go.plugin.api.config.Property;
import com.thoughtworks.go.plugin.api.material.packagerepository.PackageMaterialProperty;
import com.thoughtworks.go.plugin.api.material.packagerepository.RepositoryConfiguration;
import com.thoughtworks.go.plugin.api.response.validation.ValidationResult;
import com.thoughtworks.go.plugin.api.task.TaskConfig;
import com.thoughtworks.go.plugin.api.task.TaskExecutor;
import com.thoughtworks.go.plugin.api.task.TaskView;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.XsdValidationException;
import com.thoughtworks.go.util.command.HgUrlArgument;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.thoughtworks.go.helper.ConfigFileFixture.CONFIG_WITH_ANT_BUILDER;
import static com.thoughtworks.go.helper.ConfigFileFixture.CONFIG_WITH_NANT_AND_EXEC_BUILDER;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.WINDOWS;
import static com.thoughtworks.go.plugin.api.config.Property.*;
import static com.thoughtworks.go.util.GoConstants.CONFIG_SCHEMA_VERSION;
import static com.thoughtworks.go.util.TestUtils.sizeIs;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.collections.CollectionUtils.collect;
import static org.apache.commons.io.IOUtils.toInputStream;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsCollectionContaining.hasItem;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;

@RunWith(JunitExtRunner.class)
public class MagicalGoConfigXmlLoaderTest {
    private MagicalGoConfigXmlLoader xmlLoader;
    static final String INVALID_DESTINATION_DIRECTORY_MESSAGE = "Invalid Destination Directory. Every material needs a different destination directory and the directories should not be nested";
    private ConfigCache configCache = new ConfigCache();

    @Before
    public void setup() throws Exception {
        RepositoryMetadataStoreHelper.clear();
        xmlLoader = new MagicalGoConfigXmlLoader(configCache, ConfigElementImplementationRegistryMother.withNoPlugins());
    }

    @After
    public void tearDown() throws Exception {
        RepositoryMetadataStoreHelper.clear();
    }

    @Test
    public void shouldLoadConfigFile() throws Exception {
        CruiseConfig cruiseConfig = xmlLoader.loadConfigHolder(ConfigFileFixture.CONFIG).config;
        PipelineConfig pipelineConfig1 = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        assertThat(pipelineConfig1.size(), is(2));
        assertThat(pipelineConfig1.getLabelTemplate(), is(PipelineLabel.COUNT_TEMPLATE));

        StageConfig stage1 = pipelineConfig1.get(0);
        assertThat(stage1.name(), is(new CaseInsensitiveString("stage1")));
        assertThat(stage1.allBuildPlans().size(), is(1));
        assertThat("Should require approval", stage1.requiresApproval(), is(true));
        AdminsConfig admins = stage1.getApproval().getAuthConfig();
        assertThat(admins, hasItem((Admin) new AdminRole(new CaseInsensitiveString("admin"))));
        assertThat(admins, hasItem((Admin) new AdminRole(new CaseInsensitiveString("qa_lead"))));
        assertThat(admins, hasItem((Admin) new AdminUser(new CaseInsensitiveString("jez"))));

        StageConfig stage2 = pipelineConfig1.get(1);
        assertThat("Should not require approval", stage2.requiresApproval(), is(false));

        JobConfig plan = stage1.jobConfigByInstanceName("plan1", true);
        assertThat(plan.name(), is(new CaseInsensitiveString("plan1")));
        assertThat(plan.resources(), is(new Resources("tiger, lion")));
        assertThat(plan.getTabs().size(), is(2));
        assertThat(plan.getTabs().first().getName(), is("Emma"));
        assertThat(plan.getTabs().first().getPath(), is("logs/emma/index.html"));
        assertThat(pipelineConfig1.materialConfigs().size(), is(1));
        shouldBeSvnMaterial(pipelineConfig1.materialConfigs().first());

        PipelineConfig pipelineConfig2 = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline2"));
        shouldBeHgMaterial(pipelineConfig2.materialConfigs().first());

        PipelineConfig pipelineConfig3 = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline3"));
        MaterialConfig p4Material = pipelineConfig3.materialConfigs().first();
        shouldBeP4Material(p4Material);

        PipelineConfig pipelineConfig4 = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline4"));
        shouldBeGitMaterial(pipelineConfig4.materialConfigs().first());
    }

    @Test
    public void shouldLoadConfigWithConfigRepo() throws Exception {
        CruiseConfig cruiseConfig = xmlLoader.loadConfigHolder(ConfigFileFixture.ONE_CONFIG_REPO).config;
        assertThat(cruiseConfig.getConfigRepos().size(), is(1));
        ConfigRepoConfig configRepo = cruiseConfig.getConfigRepos().get(0);
        assertThat(configRepo.getMaterialConfig(), Is.<MaterialConfig>is(new GitMaterialConfig("https://github.com/tomzo/gocd-indep-config-part.git")));
    }

    @Test
    public void shouldLoadConfigWithConfigRepoAndPluginName() throws Exception {
        CruiseConfig cruiseConfig = xmlLoader.loadConfigHolder(ConfigFileFixture.configWithConfigRepos(
                "  <config-repos>\n"
                        + "    <config-repo plugin=\"myplugin\">\n"
                        + "      <git url=\"https://github.com/tomzo/gocd-indep-config-part.git\" />\n"
                        + "    </config-repo >\n"
                        + "  </config-repos>\n"
        )).config;
        assertThat(cruiseConfig.getConfigRepos().size(), is(1));
        ConfigRepoConfig configRepo = cruiseConfig.getConfigRepos().get(0);
        assertThat(configRepo.getConfigProviderPluginName(), is("myplugin"));
    }

    @Test
    public void shouldLoadConfigWith2ConfigRepos() throws Exception {
        CruiseConfig cruiseConfig = xmlLoader.loadConfigHolder(ConfigFileFixture.configWithConfigRepos(
                "  <config-repos>\n"
                        + "    <config-repo plugin=\"myplugin\">\n"
                        + "      <git url=\"https://github.com/tomzo/gocd-indep-config-part.git\" />\n"
                        + "    </config-repo >\n"
                        + "    <config-repo plugin=\"myplugin\">\n"
                        + "      <git url=\"https://github.com/tomzo/gocd-refmain-config-part.git\" />\n"
                        + "    </config-repo >\n"
                        + "  </config-repos>\n"
        )).config;
        assertThat(cruiseConfig.getConfigRepos().size(), is(2));
        ConfigRepoConfig configRepo1 = cruiseConfig.getConfigRepos().get(0);
        assertThat(configRepo1.getMaterialConfig(), Is.<MaterialConfig>is(new GitMaterialConfig("https://github.com/tomzo/gocd-indep-config-part.git")));
        ConfigRepoConfig configRepo2 = cruiseConfig.getConfigRepos().get(1);
        assertThat(configRepo2.getMaterialConfig(), Is.<MaterialConfig>is(new GitMaterialConfig("https://github.com/tomzo/gocd-refmain-config-part.git")));
    }

    @Test
    public void shouldLoadConfigWithConfigRepoAndConfiguration() throws Exception {
        CruiseConfig cruiseConfig = xmlLoader.loadConfigHolder(ConfigFileFixture.configWithConfigRepos(
                "  <config-repos>\n"
                        + "    <config-repo >\n"
                        + "      <git url=\"https://github.com/tomzo/gocd-indep-config-part.git\" />\n"
                        + "      <configuration>\n"
                        + "        <property>\n"
                        + "          <key>pattern</key>\n"
                        + "          <value>*.gocd.xml</value>\n"
                        + "        </property>\n"
                        + "      </configuration>\n"
                        + "    </config-repo >\n"
                        + "  </config-repos>\n"
        )).config;
        assertThat(cruiseConfig.getConfigRepos().size(), is(1));
        ConfigRepoConfig configRepo = cruiseConfig.getConfigRepos().get(0);

        assertThat(configRepo.getConfiguration().size(), is(1));
        assertThat(configRepo.getConfiguration().getProperty("pattern").getValue(), is("*.gocd.xml"));
    }

    @Test(expected = XsdValidationException.class)
    public void shouldThrowXsdValidationException_WhenNoRepository() throws Exception {
        CruiseConfig cruiseConfig = xmlLoader.loadConfigHolder(ConfigFileFixture.configWithConfigRepos(
                "  <config-repos>\n"
                        + "    <config-repo plugin=\"myplugin\">\n"
                        + "    </config-repo >\n"
                        + "  </config-repos>\n"
        )).config;
    }

    @Test(expected = XsdValidationException.class)
    public void shouldThrowXsdValidationException_When2RepositoriesInSameConfigElement() throws Exception {
        CruiseConfig cruiseConfig = xmlLoader.loadConfigHolder(ConfigFileFixture.configWithConfigRepos(
                "  <config-repos>\n"
                        + "    <config-repo plugin=\"myplugin\">\n"
                        + "      <git url=\"https://github.com/tomzo/gocd-indep-config-part.git\" />\n"
                        + "      <git url=\"https://github.com/tomzo/gocd-refmain-config-part.git\" />\n"
                        + "    </config-repo >\n"
                        + "  </config-repos>\n"
        )).config;
    }

    @Test(expected = GoConfigInvalidException.class)
    public void shouldFailValidation_WhenSameMaterialUsedBy2ConfigRepos() throws Exception {
        CruiseConfig cruiseConfig = xmlLoader.loadConfigHolder(ConfigFileFixture.configWithConfigRepos(
                "  <config-repos>\n"
                        + "    <config-repo plugin=\"myplugin\">\n"
                        + "      <git url=\"https://github.com/tomzo/gocd-indep-config-part.git\" />\n"
                        + "    </config-repo >\n"
                        + "    <config-repo plugin=\"myotherplugin\">\n"
                        + "      <git url=\"https://github.com/tomzo/gocd-indep-config-part.git\" />\n"
                        + "    </config-repo >\n"
                        + "  </config-repos>\n"
        )).config;
    }

    private ConfigRepoPartialPreprocessor findConfigRepoPartialPreprocessor() {
        List<GoConfigPreprocessor> preprocessors = MagicalGoConfigXmlLoader.PREPROCESSORS;
        for (GoConfigPreprocessor preprocessor : preprocessors) {
            if (preprocessor instanceof ConfigRepoPartialPreprocessor)
                return (ConfigRepoPartialPreprocessor) preprocessor;
        }
        return null;
    }

    @Test
    public void shouldSetConfigOriginInCruiseConfig_AfterLoadingConfigFile() throws Exception {
        GoConfigHolder goConfigHolder = xmlLoader.loadConfigHolder(ConfigFileFixture.CONFIG, new MagicalGoConfigXmlLoader.Callback() {
            @Override
            public void call(CruiseConfig cruiseConfig) {
                cruiseConfig.setPartials(asList(new PartialConfig()));
            }
        });
        assertThat(goConfigHolder.config.getOrigin(), Is.<ConfigOrigin>is(new MergeConfigOrigin()));
        assertThat(goConfigHolder.configForEdit.getOrigin(), Is.<ConfigOrigin>is(new FileConfigOrigin()));
    }

    @Test
    public void shouldSetConfigOriginInPipeline_AfterLoadingConfigFile() throws Exception {
        CruiseConfig cruiseConfig = xmlLoader.loadConfigHolder(ConfigFileFixture.CONFIG).config;
        PipelineConfig pipelineConfig1 = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        assertThat(pipelineConfig1.getOrigin(), Is.<ConfigOrigin>is(new FileConfigOrigin()));
    }

    @Test
    public void shouldSetConfigOriginInEnvironment_AfterLoadingConfigFile() {
        String content = ConfigFileFixture.configWithEnvironments(
                "<environments>"
                        + "  <environment name='uat'>"
                        + "    <agents>"
                        + "      <physical uuid='1'/>"
                        + "      <physical uuid='2'/>"
                        + "    </agents>"
                        + "  </environment>"
                        + "</environments>");
        EnvironmentsConfig environmentsConfig = ConfigMigrator.loadWithMigration(content).config.getEnvironments();
        EnvironmentConfig uat = environmentsConfig.get(0);
        assertThat(uat.getOrigin(), Is.<ConfigOrigin>is(new FileConfigOrigin()));
    }

    @Test
    public void shouldSupportMultipleAgentsFromSameBox() throws Exception {
        CruiseConfig cruiseConfig = xmlLoader.loadConfigHolder(ConfigMigrator.migrate(ConfigFileFixture.WITH_MULTIPLE_LOCAL_AGENT_CONFIG)).config;
        assertThat(cruiseConfig.agents().size(), is(2));
        assertThat(cruiseConfig.agents().get(0).getHostname(), is(cruiseConfig.agents().get(1).getHostname()));
        assertThat(cruiseConfig.agents().get(0).getIpAddress(), is(cruiseConfig.agents().get(1).getIpAddress()));
        assertThat(cruiseConfig.agents().get(0).getUuid(), is(not(cruiseConfig.agents().get(1).getUuid())));
    }

    @Test
    public void shouldLoadAntBuilder() throws Exception {
        CruiseConfig cruiseConfig = xmlLoader.loadConfigHolder(FileUtil.readToEnd(toInputStream(CONFIG_WITH_ANT_BUILDER))).config;
        JobConfig plan = cruiseConfig.jobConfigByName("pipeline1", "mingle", "cardlist", true);

        assertThat(plan.tasks(), sizeIs(1));
        AntTask builder = (AntTask) plan.tasks().first();
        assertThat(builder.getTarget(), is("all"));
        final ArtifactPlans cardListArtifacts = cruiseConfig.jobConfigByName("pipeline1", "mingle",
                "cardlist", true).artifactPlans();
        assertThat(cardListArtifacts.size(), is(1));
        ArtifactPlan artifactPlan = cardListArtifacts.get(0);
        assertThat(artifactPlan.getArtifactType(), is(ArtifactType.unit));
    }

    @Test
    public void shouldLoadNAntBuilder() throws Exception {
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(toInputStream(
                CONFIG_WITH_NANT_AND_EXEC_BUILDER)).config;
        JobConfig plan = cruiseConfig.jobConfigByName("pipeline1", "mingle", "cardlist", true);
        BuildTask builder = (BuildTask) plan.tasks().findFirstByType(NantTask.class);
        assertThat(builder.getTarget(), is("all"));
    }

    @Test
    public void shouldLoadExecBuilder() throws Exception {

        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(toInputStream(CONFIG_WITH_NANT_AND_EXEC_BUILDER)).config;
        JobConfig plan = cruiseConfig.jobConfigByName("pipeline1", "mingle", "cardlist", true);
        ExecTask builder = (ExecTask) plan.tasks().findFirstByType(ExecTask.class);
        assertThat(builder, is(new ExecTask("ls", "-la", "workdir")));

        builder = (ExecTask) plan.tasks().get(2);
        assertThat(builder, is(new ExecTask("ls", "", (String) null)));
    }

    @Test
    public void shouldLoadRakeBuilderWithEmptyOnCancel() throws Exception {

        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(toInputStream(CONFIG_WITH_NANT_AND_EXEC_BUILDER)).config;
        JobConfig plan = cruiseConfig.jobConfigByName("pipeline1", "mingle", "cardlist", true);
        RakeTask builder = (RakeTask) plan.tasks().findFirstByType(RakeTask.class);
        assertThat(builder, notNullValue());
    }

    @Test
    public void shouldMigrateAnEmptyArtifactSourceToStar() throws Exception {
        GoConfigHolder holder = ConfigMigrator.loadWithMigration(toInputStream(ConfigFileFixture.configWithArtifactSourceAs("")));
        CruiseConfig cruiseConfig = holder.config;
        JobConfig plan = cruiseConfig.jobConfigByName("pipeline", "stage", "job", true);
        assertThat(plan.artifactPlans().get(0).getSrc(), is("*"));
    }

    @Test
    public void shouldMigrateAnArtifactSourceWithJustWhitespaceToStar() throws Exception {
        GoConfigHolder holder = ConfigMigrator.loadWithMigration(toInputStream(ConfigFileFixture.configWithArtifactSourceAs(" \t ")));
        CruiseConfig cruiseConfig = holder.config;
        JobConfig plan = cruiseConfig.jobConfigByName("pipeline", "stage", "job", true);
        assertThat(plan.artifactPlans().get(0).getSrc(), is("*"));
    }

    @Test
    public void shouldRetainArtifactSourceThatIsNotWhitespace() throws Exception {
        GoConfigHolder holder = ConfigMigrator.loadWithMigration(toInputStream(ConfigFileFixture.configWithArtifactSourceAs("t ")));
        CruiseConfig cruiseConfig = holder.config;
        JobConfig plan = cruiseConfig.jobConfigByName("pipeline", "stage", "job", true);
        assertThat(plan.artifactPlans().get(0).getSrc(), is("t "));
    }

    @Test
    public void shouldLoadBuildPlanFromXmlPartial() throws Exception {
        String buildXmlPartial =
                "<job name=\"functional\">\n"
                        + "  <artifacts>\n"
                        + "    <artifact src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
                        + "  </artifacts>\n"
                        + "</job>";
        JobConfig build = xmlLoader.fromXmlPartial(toInputStream(buildXmlPartial), JobConfig.class);
        assertThat(build.name(), is(new CaseInsensitiveString("functional")));
        assertThat(build.artifactPlans().size(), is(1));
    }


    @Test
    public void shouldLoadIgnoresFromSvnPartial() throws Exception {
        String buildXmlPartial =
                "<svn url=\"file:///tmp/testSvnRepo/project1/trunk\" >\n"
                        + "            <filter>\n"
                        + "                <ignore pattern=\"x\"/>\n"
                        + "            </filter>\n"
                        + "        </svn>";
        MaterialConfig svnMaterial = xmlLoader.fromXmlPartial(toInputStream(buildXmlPartial), SvnMaterialConfig.class);
        Filter parsedFilter = svnMaterial.filter();
        Filter expectedFilter = new Filter();
        expectedFilter.add(new IgnoredFiles("x"));
        assertThat(parsedFilter, is(expectedFilter));
    }

    @Test
    public void shouldLoadIgnoresFromHgPartial() throws Exception {
        String buildXmlPartial =
                "<hg url=\"file:///tmp/testSvnRepo/project1/trunk\" >\n"
                        + "            <filter>\n"
                        + "                <ignore pattern=\"x\"/>\n"
                        + "            </filter>\n"
                        + "        </hg>";
        MaterialConfig hgMaterial = xmlLoader.fromXmlPartial(toInputStream(buildXmlPartial), HgMaterialConfig.class);
        Filter parsedFilter = hgMaterial.filter();
        Filter expectedFilter = new Filter();
        expectedFilter.add(new IgnoredFiles("x"));
        assertThat(parsedFilter, is(expectedFilter));
    }

    @Test
    public void shouldLoadMaterialWithAutoUpdate() throws Exception {
        MaterialConfig material = xmlLoader.fromXmlPartial("<hg url=\"file:///tmp/testSvnRepo/project1/trunk\" autoUpdate=\"false\"/>", HgMaterialConfig.class);
        assertThat(material.isAutoUpdate(), is(false));
        material = xmlLoader.fromXmlPartial("<git url=\"file:///tmp/testSvnRepo/project1/trunk\" autoUpdate=\"false\"/>", GitMaterialConfig.class);
        assertThat(material.isAutoUpdate(), is(false));
        material = xmlLoader.fromXmlPartial("<svn url=\"file:///tmp/testSvnRepo/project1/trunk\" autoUpdate=\"false\"/>", SvnMaterialConfig.class);
        assertThat(material.isAutoUpdate(), is(false));
        material = xmlLoader.fromXmlPartial("<p4 port='localhost:1666' autoUpdate='false' ><view/></p4>", P4MaterialConfig.class);
        assertThat(material.isAutoUpdate(), is(false));
    }

    @Test
    public void autoUpdateShouldBeTrueByDefault() throws Exception {
        MaterialConfig hgMaterial = xmlLoader.fromXmlPartial("<hg url=\"file:///tmp/testSvnRepo/project1/trunk\"/>", HgMaterialConfig.class);
        assertThat(hgMaterial.isAutoUpdate(), is(true));
    }

    @Test
    public void autoUpdateShouldUnderstandTrue() throws Exception {
        MaterialConfig hgMaterial = xmlLoader.fromXmlPartial("<hg url=\"file:///tmp/testSvnRepo/project1/trunk\" autoUpdate=\"true\"/>", HgMaterialConfig.class);
        assertThat(hgMaterial.isAutoUpdate(), is(true));
    }

    @Test
    public void shouldValidateBooleanAutoUpdateOnMaterials() throws Exception {
        String noAutoUpdate =
                "  <materials>\n"
                        + "    <svn url=\"/hgrepo2\" />\n"
                        + "  </materials>\n";
        MagicalGoConfigXmlLoaderFixture.assertValid(noAutoUpdate);
        String validAutoUpdate =
                "  <materials>\n"
                        + "    <svn url=\"/hgrepo2\" autoUpdate='true'/>\n"
                        + "  </materials>\n";
        MagicalGoConfigXmlLoaderFixture.assertValid(validAutoUpdate);
        String invalidautoUpdate =
                "  <materials>\n"
                        + "    <git url=\"/hgrepo2\" autoUpdate=\"fooo\"/>\n"
                        + "  </materials>\n";
        MagicalGoConfigXmlLoaderFixture.assertNotValid("'fooo' is not a valid value for 'boolean'.", invalidautoUpdate);
    }

    @Test
    public void shouldInvalidateAutoUpdateOnDependencyMaterial() throws Exception {
        String noAutoUpdate =
                "  <materials>\n"
                        + "    <pipeline pipelineName=\"pipeline\" stageName=\"stage\" autoUpdate=\"true\"/>\n"
                        + "  </materials>\n";
        MagicalGoConfigXmlLoaderFixture.assertNotValid("Attribute 'autoUpdate' is not allowed to appear in element 'pipeline'.", noAutoUpdate);
    }

    @Test
    public void shouldInvalidateAutoUpdateIfTheSameMaterialHasDifferentValuesForAutoUpdate() throws Exception {
        String noAutoUpdate =
                "  <materials>\n"
                        + "    <svn url=\"/hgrepo2\" autoUpdate='true' dest='first'/>\n"
                        + "    <svn url=\"/hgrepo2\" autoUpdate='false' dest='second'/>\n"
                        + "  </materials>\n";
        MagicalGoConfigXmlLoaderFixture.assertNotValid(
                "Material of type Subversion (/hgrepo2) is specified more than once in the configuration with different values",
                noAutoUpdate);
    }

    @Test
    public void shouldLoadFromSvnPartial() throws Exception {
        String buildXmlPartial =
                "<svn url=\"http://foo.bar\" username=\"cruise\" password=\"password\" materialName=\"http___foo.bar\"/>";

        MaterialConfig materialConfig = xmlLoader.fromXmlPartial(toInputStream(buildXmlPartial), SvnMaterialConfig.class);
        MaterialConfig svnMaterial = MaterialConfigsMother.svnMaterialConfig("http://foo.bar", null, "cruise", "password", false, null);
        assertThat(materialConfig, is(svnMaterial));
    }

    @Test
    public void shouldAcceptLdapConfiguration_withNoManagerDn() throws Exception {
        String ldapCfg = "<ldap uri=\"foo\" searchFilter=\"filter\" >"
                + "<bases><base>base1</base></bases>"
                + "</ldap>";

        LdapConfig ldapConfig = xmlLoader.fromXmlPartial(toInputStream(ldapCfg), LdapConfig.class);

        assertThat(ldapConfig.managerDn(), is(""));
        assertThat(ldapConfig.managerPassword(), is(""));
    }


    @Test
    public void shouldAcceptLdapConfiguration_withoutSearchFilter() throws Exception {
        String ldapCfg = "<ldap uri=\"foo\" managerDn=\"foo\" managerPassword=\"bar\">"
                + "<bases><base>base1</base></bases>"
                + "</ldap>";

        LdapConfig ldapConfig = xmlLoader.fromXmlPartial(toInputStream(ldapCfg), LdapConfig.class);

        assertThat(ldapConfig.searchFilter(), is(""));
    }

    @Test
    public void shouldLoadGetFromSvnPartialForDir() throws Exception {
        String buildXmlPartial =
                "<jobs>\n"
                        + "  <job name=\"functional\">\n"
                        + "     <tasks>\n"
                        + "         <fetchartifact stage='dev' job='unit' srcdir='dist' dest='lib' />\n"
                        + "      </tasks>\n"
                        + "    </job>\n"
                        + "</jobs>";

        JobConfigs jobs = xmlLoader.fromXmlPartial(toInputStream(buildXmlPartial), JobConfigs.class);
        JobConfig job = jobs.first();
        Tasks fetch = job.tasks();
        assertThat(fetch.size(), is(1));
        FetchTask task = (FetchTask) fetch.first();
        assertThat(task.getStage(), is(new CaseInsensitiveString("dev")));
        assertThat(task.getJob().toString(), is("unit"));
        assertThat(task.getSrc(), is("dist"));
        assertThat(task.getDest(), is("lib"));
    }

    @Test
    public void shouldAllowEmptyOnCancel() throws Exception {
        String buildXmlPartial =
                "<jobs>\n"
                        + "  <job name=\"functional\">\n"
                        + "     <tasks>\n"
                        + "         <exec command='ls'>\n"
                        + "             <oncancel/>\n"
                        + "         </exec>\n"
                        + "      </tasks>\n"
                        + "    </job>\n"
                        + "</jobs>";

        JobConfigs jobs = xmlLoader.fromXmlPartial(toInputStream(buildXmlPartial), JobConfigs.class);
        JobConfig job = jobs.first();
        Tasks tasks = job.tasks();
        assertThat(tasks.size(), is(1));
        ExecTask execTask = (ExecTask) tasks.get(0);
        assertThat(execTask.cancelTask(), is(instanceOf(NullTask.class)));
    }

    @Test
    public void shouldLoadIgnoresFromGitPartial() throws Exception {
        String gitPartial =
                "<git url='file:///tmp/testGitRepo/project1' >\n"
                        + "            <filter>\n"
                        + "                <ignore pattern='x'/>\n"
                        + "            </filter>\n"
                        + "        </git>";
        GitMaterialConfig gitMaterial = xmlLoader.fromXmlPartial(toInputStream(gitPartial), GitMaterialConfig.class);
        assertThat(gitMaterial.getBranch(), is(GitMaterialConfig.DEFAULT_BRANCH));
        Filter parsedFilter = gitMaterial.filter();
        Filter expectedFilter = new Filter();
        expectedFilter.add(new IgnoredFiles("x"));
        assertThat(parsedFilter, is(expectedFilter));
    }

    @Test
    public void shouldLoadShallowFlagFromGitPartial() throws Exception {
        String gitPartial = "<git url='file:///tmp/testGitRepo/project1' shallowClone=\"true\" />";
        GitMaterialConfig gitMaterial = xmlLoader.fromXmlPartial(toInputStream(gitPartial), GitMaterialConfig.class);
        assertTrue(gitMaterial.isShallowClone());
    }

    @Test
    public void shouldLoadBranchFromGitPartial() throws Exception {
        String gitPartial = "<git url='file:///tmp/testGitRepo/project1' branch='foo'/>";
        GitMaterialConfig gitMaterial = xmlLoader.fromXmlPartial(toInputStream(gitPartial), GitMaterialConfig.class);
        assertThat(gitMaterial.getBranch(), is("foo"));
    }

    @Test
    public void shouldLoadIgnoresFromP4Partial() throws Exception {
        String gitPartial =
                "<p4 port=\"localhost:8080\">\n"
                        + "            <filter>\n"
                        + "                <ignore pattern=\"x\"/>\n"
                        + "            </filter>\n"
                        + " <view></view>\n"
                        + "</p4>";
        MaterialConfig p4Material = xmlLoader.fromXmlPartial(toInputStream(gitPartial), P4MaterialConfig.class);
        Filter parsedFilter = p4Material.filter();
        Filter expectedFilter = new Filter();
        expectedFilter.add(new IgnoredFiles("x"));
        assertThat(parsedFilter, is(expectedFilter));
    }

    @Test
    public void shouldLoadStageFromXmlPartial() throws Exception {
        String stageXmlPartial =
                "<stage name=\"mingle\">\n"
                        + "  <jobs>\n"
                        + "    <job name=\"functional\">\n"
                        + "      <artifacts>\n"
                        + "        <log src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
                        + "      </artifacts>\n"
                        + "    </job>\n"
                        + "  </jobs>\n"
                        + "</stage>\n";
        StageConfig stage = xmlLoader.fromXmlPartial(toInputStream(stageXmlPartial), StageConfig.class);
        assertThat(stage.name(), is(new CaseInsensitiveString("mingle")));
        assertThat(stage.allBuildPlans().size(), is(1));
        assertThat(stage.jobConfigByInstanceName("functional", true), is(notNullValue()));
    }

    @Test
    public void shouldLoadStageArtifactPurgeSettingsFromXmlPartial() throws Exception {
        String stageXmlPartial =
                "<stage name=\"mingle\" artifactCleanupProhibited=\"true\">\n"
                        + "  <jobs>\n"
                        + "    <job name=\"functional\">\n"
                        + "      <artifacts>\n"
                        + "        <log src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
                        + "      </artifacts>\n"
                        + "    </job>\n"
                        + "  </jobs>\n"
                        + "</stage>\n";
        StageConfig stage = xmlLoader.fromXmlPartial(toInputStream(stageXmlPartial), StageConfig.class);
        assertThat(stage.isArtifactCleanupProhibited(), is(true));

        stageXmlPartial =
                "<stage name=\"mingle\" artifactCleanupProhibited=\"false\">\n"
                        + "  <jobs>\n"
                        + "    <job name=\"functional\">\n"
                        + "      <artifacts>\n"
                        + "        <log src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
                        + "      </artifacts>\n"
                        + "    </job>\n"
                        + "  </jobs>\n"
                        + "</stage>\n";
        stage = xmlLoader.fromXmlPartial(toInputStream(stageXmlPartial), StageConfig.class);
        assertThat(stage.isArtifactCleanupProhibited(), is(false));

        stageXmlPartial =
                "<stage name=\"mingle\">\n"
                        + "  <jobs>\n"
                        + "    <job name=\"functional\">\n"
                        + "      <artifacts>\n"
                        + "        <log src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
                        + "      </artifacts>\n"
                        + "    </job>\n"
                        + "  </jobs>\n"
                        + "</stage>\n";
        stage = xmlLoader.fromXmlPartial(toInputStream(stageXmlPartial), StageConfig.class);
        assertThat(stage.isArtifactCleanupProhibited(), is(false));
    }

    @Test
    public void shouldLoadPartialConfigWithPipeline() throws Exception {
        String partialConfigWithPipeline =
                "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                        + "<pipelines group=\"first\">\n"
                        + "<pipeline name=\"pipeline\">\n"
                        + "  <materials>\n"
                        + "    <hg url=\"/hgrepo\"/>\n"
                        + "  </materials>\n"
                        + "  <stage name=\"mingle\">\n"
                        + "    <jobs>\n"
                        + "      <job name=\"functional\">\n"
                        + "        <artifacts>\n"
                        + "          <log src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
                        + "        </artifacts>\n"
                        + "      </job>\n"
                        + "    </jobs>\n"
                        + "  </stage>\n"
                        + "</pipeline>\n"
                        + "</pipelines>\n"
                        + "</cruise>\n";
        PartialConfig partialConfig = xmlLoader.fromXmlPartial(toInputStream(partialConfigWithPipeline), PartialConfig.class);
        assertThat(partialConfig.getGroups().size(), is(1));
        PipelineConfig pipeline = partialConfig.getGroups().get(0).getPipelines().get(0);
        assertThat(pipeline.name(), is(new CaseInsensitiveString("pipeline")));
        assertThat(pipeline.size(), is(1));
        assertThat(pipeline.findBy(new CaseInsensitiveString("mingle")).jobConfigByInstanceName("functional", true), is(notNullValue()));
    }

    @Test
    public void shouldLoadPartialConfigWithEnvironment() throws Exception {
        String partialConfigWithPipeline =
                "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                        + "<environments>"
                        + "  <environment name='uat'>"
                        + "    <agents>"
                        + "      <physical uuid='1'/>"
                        + "      <physical uuid='2'/>"
                        + "    </agents>"
                        + "  </environment>"
                        + "  <environment name='prod'>"
                        + "    <agents>"
                        + "      <physical uuid='2'/>"
                        + "    </agents>"
                        + "  </environment>"
                        + "</environments>"
                        + "</cruise>\n";
        PartialConfig partialConfig = xmlLoader.fromXmlPartial(toInputStream(partialConfigWithPipeline), PartialConfig.class);
        EnvironmentsConfig environmentsConfig = partialConfig.getEnvironments();
        assertThat(environmentsConfig.size(), is(2));
        EnvironmentPipelineMatchers matchers = environmentsConfig.matchers();
        assertThat(matchers.size(), is(2));
        ArrayList<String> uat_uuids = new ArrayList<String>() {{
            add("1");
            add("2");
        }};
        ArrayList<String> prod_uuids = new ArrayList<String>() {{
            add("2");
        }};
        assertThat(matchers,
                hasItem(new EnvironmentPipelineMatcher(new CaseInsensitiveString("uat"), uat_uuids, new EnvironmentPipelinesConfig())));
        assertThat(matchers,
                hasItem(new EnvironmentPipelineMatcher(new CaseInsensitiveString("prod"), prod_uuids, new EnvironmentPipelinesConfig())));
    }

    @Test
    public void shouldLoadPipelineFromXmlPartial() throws Exception {
        String pipelineXmlPartial =
                "<pipeline name=\"pipeline\">\n"
                        + "  <materials>\n"
                        + "    <hg url=\"/hgrepo\"/>\n"
                        + "  </materials>\n"
                        + "  <stage name=\"mingle\">\n"
                        + "    <jobs>\n"
                        + "      <job name=\"functional\">\n"
                        + "        <artifacts>\n"
                        + "          <log src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
                        + "        </artifacts>\n"
                        + "      </job>\n"
                        + "    </jobs>\n"
                        + "  </stage>\n"
                        + "</pipeline>\n";
        PipelineConfig pipeline = xmlLoader.fromXmlPartial(toInputStream(pipelineXmlPartial), PipelineConfig.class);
        assertThat(pipeline.name(), is(new CaseInsensitiveString("pipeline")));
        assertThat(pipeline.size(), is(1));
        assertThat(pipeline.findBy(new CaseInsensitiveString("mingle")).jobConfigByInstanceName("functional", true), is(notNullValue()));
    }

    @Test
    public void shouldBeAbleToExplicitlyLockAPipeline() throws Exception {
        String pipelineXmlPartial =
                "<pipeline name=\"pipeline\" isLocked=\"true\">\n"
                        + "  <materials>\n"
                        + "    <hg url=\"/hgrepo\"/>\n"
                        + "  </materials>\n"
                        + "  <stage name=\"mingle\">\n"
                        + "    <jobs>\n"
                        + "      <job name=\"functional\">\n"
                        + "        <artifacts>\n"
                        + "          <log src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
                        + "        </artifacts>\n"
                        + "      </job>\n"
                        + "    </jobs>\n"
                        + "  </stage>\n"
                        + "</pipeline>\n";
        PipelineConfig pipeline = xmlLoader.fromXmlPartial(toInputStream(pipelineXmlPartial), PipelineConfig.class);

        assertThat(pipeline.hasExplicitLock(), is(true));
        assertThat(pipeline.explicitLock(), is(true));
    }

    @Test
    public void shouldBeAbleToExplicitlyUnlockAPipeline() throws Exception {
        String pipelineXmlPartial =
                "<pipeline name=\"pipeline\" isLocked=\"false\">\n"
                        + "  <materials>\n"
                        + "    <hg url=\"/hgrepo\"/>\n"
                        + "  </materials>\n"
                        + "  <stage name=\"mingle\">\n"
                        + "    <jobs>\n"
                        + "      <job name=\"functional\">\n"
                        + "        <artifacts>\n"
                        + "          <log src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
                        + "        </artifacts>\n"
                        + "      </job>\n"
                        + "    </jobs>\n"
                        + "  </stage>\n"
                        + "</pipeline>\n";
        PipelineConfig pipeline = xmlLoader.fromXmlPartial(toInputStream(pipelineXmlPartial), PipelineConfig.class);

        assertThat(pipeline.hasExplicitLock(), is(true));
        assertThat(pipeline.explicitLock(), is(false));
    }

    @Test
    public void shouldUnderstandNoExplicitLockOnAPipeline() throws Exception {
        String pipelineXmlPartial =
                "<pipeline name=\"pipeline\">\n"
                        + "  <materials>\n"
                        + "    <hg url=\"/hgrepo\"/>\n"
                        + "  </materials>\n"
                        + "  <stage name=\"mingle\">\n"
                        + "    <jobs>\n"
                        + "      <job name=\"functional\">\n"
                        + "        <artifacts>\n"
                        + "          <log src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
                        + "        </artifacts>\n"
                        + "      </job>\n"
                        + "    </jobs>\n"
                        + "  </stage>\n"
                        + "</pipeline>\n";
        PipelineConfig pipeline = xmlLoader.fromXmlPartial(toInputStream(pipelineXmlPartial), PipelineConfig.class);

        assertThat(pipeline.hasExplicitLock(), is(false));
        try {
            pipeline.explicitLock();
            fail("Should throw exception if call explicit lock without first checking to see if there is one");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("There is no explicit lock on the pipeline 'pipeline'."));
        }
    }

    @Test
    public void shouldLoadPipelineWithP4MaterialFromXmlPartial() throws Exception {
        String pipelineWithP4MaterialXmlPartial =
                "<pipeline name=\"pipeline\">\n"
                        + "  <materials>\n"
                        + "    <p4 port=\"10.18.3.241:9999\" username=\"cruise\" password=\"password\" "
                        + "        useTickets=\"true\">\n"
                        + "          <view><![CDATA[//depot/dev/... //lumberjack/...]]></view>\n"
                        + "    </p4>"
                        + "  </materials>\n"
                        + "  <stage name=\"mingle\">\n"
                        + "    <jobs>\n"
                        + "      <job name=\"functional\">\n"
                        + "        <artifacts>\n"
                        + "          <log src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
                        + "        </artifacts>\n"
                        + "      </job>\n"
                        + "    </jobs>\n"
                        + "  </stage>\n"
                        + "</pipeline>\n";
        PipelineConfig pipeline = xmlLoader.fromXmlPartial(toInputStream(pipelineWithP4MaterialXmlPartial), PipelineConfig.class);
        assertThat(pipeline.name(), is(new CaseInsensitiveString("pipeline")));
        MaterialConfig material = pipeline.materialConfigs().first();
        assertThat(material, is(instanceOf(P4MaterialConfig.class)));
        assertThat(((P4MaterialConfig) material).getUseTickets(), is(true));
    }

    @Test
    public void shouldThrowExceptionWhenXmlDoesNotMapToXmlPartial() throws Exception {
        String stageXmlPartial =
                "<stage name=\"mingle\">\n"
                        + "  <jobs>\n"
                        + "    <job name=\"functional\">\n"
                        + "      <artifacts>\n"
                        + "        <log src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
                        + "      </artifacts>\n"
                        + "    </job>\n"
                        + "  </jobs>\n"
                        + "</stage>\n";
        try {
            xmlLoader.fromXmlPartial(toInputStream(stageXmlPartial), JobConfig.class);
            fail("Should not be able to load stage into jobConfig");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Unable to parse element <stage> for class JobConfig"));
        }
    }


    @Test
    public void shouldThrowExceptionWhenCommandIsEmpty() throws Exception {
        String jobWithCommand =
                "<job name=\"functional\">\n"
                        + "      <tasks>\n"
                        + "        <exec command=\"\" arguments=\"\" />\n"
                        + "      </tasks>\n"
                        + "    </job>\n";
        String configWithInvalidCommand = ConfigFileFixture.withCommand(jobWithCommand);

        try {

            ConfigMigrator.loadWithMigration(toInputStream(configWithInvalidCommand));

            fail("Should not allow empty command");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Command is invalid. \"\" should conform to the pattern - \\S+(.*\\S+)*"));
        }
    }

    @Test
    public void shouldThrowExceptionWhenCommandsContainTrailingSpaces() throws Exception {
        String configXml =
                "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n" +
                        "  <pipelines group='first'>" +
                        "    <pipeline name='Test'>" +
                        "      <materials>" +
                        "        <hg url='../manual-testing/ant_hg/dummy' />" +
                        "      </materials>" +
                        "      <stage name='Functional'>" +
                        "        <jobs>" +
                        "          <job name='Functional'>" +
                        "            <tasks>" +
                        "              <exec command='bundle  ' args='arguments' />" +
                        "            </tasks>" +
                        "           </job>" +
                        "        </jobs>" +
                        "      </stage>" +
                        "    </pipeline>" +
                        "  </pipelines>" +
                        "</cruise>";
        try {
            ConfigMigrator.loadWithMigration(configXml);

            fail("Should not allow command with trailing spaces");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Command is invalid. \"bundle  \" should conform to the pattern - \\S+(.*\\S+)*"));
        }
    }

    @Test
    public void shouldThrowExceptionWhenCommandsContainLeadingSpaces() throws Exception {
        String configXml =
                "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n" +
                        "  <pipelines group='first'>" +
                        "    <pipeline name='Test'>" +
                        "      <materials>" +
                        "        <hg url='../manual-testing/ant_hg/dummy' />" +
                        "      </materials>" +
                        "      <stage name='Functional'>" +
                        "        <jobs>" +
                        "          <job name='Functional'>" +
                        "            <tasks>" +
                        "              <exec command='    bundle' args='arguments' />" +
                        "            </tasks>" +
                        "           </job>" +
                        "        </jobs>" +
                        "      </stage>" +
                        "    </pipeline>" +
                        "  </pipelines>" +
                        "</cruise>";
        try {
            ConfigMigrator.loadWithMigration(configXml);

            fail("Should not allow command with trailing spaces");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Command is invalid. \"    bundle\" should conform to the pattern - \\S+(.*\\S+)*"));
        }
    }

    @Test
    public void shouldSupportCommandWithWhiteSpace() throws Exception {
        String jobWithCommand =
                "<job name=\"functional\">\n"
                        + "      <tasks>\n"
                        + "        <exec command=\"c:\\program files\\cmd.exe\" args=\"arguments\" />\n"
                        + "      </tasks>\n"
                        + "    </job>\n";
        String configWithCommand = ConfigFileFixture.withCommand(jobWithCommand);

        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(toInputStream(configWithCommand)).config;
        Task task = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).first().allBuildPlans().first().tasks().first();

        assertThat(task, is(instanceOf(ExecTask.class)));
        assertThat((ExecTask) task, is(new ExecTask("c:\\program files\\cmd.exe", "arguments", (String) null)));
    }

    @Test
    public void shouldLoadMingleConfigForPipeline() throws Exception {
        String configWithCommand = ConfigFileFixture.withMingleConfig("<mingle baseUrl=\"https://foo.bar/baz\" projectIdentifier=\"cruise-performance\"/>");
        MingleConfig mingleConfig = ConfigMigrator.loadWithMigration(toInputStream(configWithCommand)).config.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).getMingleConfig();
        assertThat(mingleConfig, is(new MingleConfig("https://foo.bar/baz", "cruise-performance")));

        configWithCommand = ConfigFileFixture.withMingleConfig(
                "<mingle baseUrl=\"https://foo.bar/baz\" projectIdentifier=\"cruise-performance\"><mqlGroupingConditions>foo = bar!=baz</mqlGroupingConditions></mingle>");
        mingleConfig = ConfigMigrator.loadWithMigration(toInputStream(configWithCommand)).config.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).getMingleConfig();
        assertThat(mingleConfig, is(new MingleConfig("https://foo.bar/baz", "cruise-performance", "foo = bar!=baz")));

        configWithCommand = ConfigFileFixture.withMingleConfig("<mingle baseUrl=\"https://foo.bar/baz\" projectIdentifier=\"cruise-performance\"><mqlGroupingConditions/></mingle>");
        mingleConfig = ConfigMigrator.loadWithMigration(toInputStream(configWithCommand)).config.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).getMingleConfig();
        assertThat(mingleConfig, is(new MingleConfig("https://foo.bar/baz", "cruise-performance", "")));
    }

    private void shouldBeSvnMaterial(MaterialConfig material) {
        assertThat(material, is(instanceOf(SvnMaterialConfig.class)));
        SvnMaterialConfig svnMaterial = (SvnMaterialConfig) material;
        assertThat(svnMaterial.getUrl(), is("svnUrl"));
        assertThat(svnMaterial.isCheckExternals(), is(true));
    }

    private void shouldBeHgMaterial(MaterialConfig material) {
        assertThat(material, is(instanceOf(HgMaterialConfig.class)));
        HgMaterialConfig hgMaterial = (HgMaterialConfig) material;
        assertThat((HgUrlArgument) hgMaterial.getUrlArgument(), is(new HgUrlArgument("http://username:password@hgUrl.com")));
    }

    private void shouldBeP4Material(MaterialConfig material) {
        assertThat(material, is(instanceOf(P4MaterialConfig.class)));
        P4MaterialConfig p4Material = (P4MaterialConfig) material;
        assertThat(p4Material.getServerAndPort(), is("localhost:1666"));
        assertThat(p4Material.getUserName(), is("cruise"));
        assertThat(p4Material.getPassword(), is("password"));
        assertThat(p4Material.getView(), is("//depot/dir1/... //lumberjack/..."));
    }

    private void shouldBeGitMaterial(MaterialConfig material) {
        assertThat(material, is(instanceOf(GitMaterialConfig.class)));
        GitMaterialConfig gitMaterial = (GitMaterialConfig) material;
        assertThat(gitMaterial.getUrlArgument(), is(new UrlArgument("git://username:password@gitUrl")));
    }

    @Test
    public void shouldNotAllowEmptyAuthInApproval() throws Exception {
        try {
            xmlLoader.loadConfigHolder(FileUtil.readToEnd(IOUtils.toInputStream(ConfigFileFixture.STAGE_WITH_EMPTY_AUTH)));
            fail("Should not allow approval with empty authorization");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("The content of element 'authorization' is not complete"));
        }
    }

    @Test
    public void shouldNotAllowEmptyRoles() throws Exception {
        try {
            xmlLoader.loadConfigHolder(FileUtil.readToEnd(IOUtils.toInputStream(ConfigFileFixture.CONFIG_WITH_EMPTY_ROLES)));
            fail("Should not allow approval with empty roles");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("The content of element 'roles' is not complete. One of '{role}' is expected."));
        }
    }

    @Test
    public void shouldNotAllowEmptyUser() throws Exception {
        try {
            xmlLoader.loadConfigHolder(FileUtil.readToEnd(IOUtils.toInputStream(ConfigFileFixture.CONFIG_WITH_EMPTY_USER)));
            fail("Should not allow approval with empty user");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("Value '' with length = '0' is not facet-valid with respect to minLength '1'"));
        }
    }

    @Test
    public void shouldNotAllowDuplicateRoles() throws Exception {
        try {
            xmlLoader.loadConfigHolder(FileUtil.readToEnd(IOUtils.toInputStream(ConfigFileFixture.CONFIG_WITH_DUPLICATE_ROLE)));
            fail("Should not allow approval with empty user");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("Duplicate unique value [admin] declared for identity constraint \"uniqueRole\" of element \"roles\"."));
        }
    }

    @Test
    public void shouldNotAllowDuplicateUsersInARole() throws Exception {
        try {
            xmlLoader.loadConfigHolder(FileUtil.readToEnd(IOUtils.toInputStream(ConfigFileFixture.CONFIG_WITH_DUPLICATE_USER)));
            fail("Should not allow role with duplicate user");
        } catch (RuntimeException expected) {
            assertThat(expected.getMessage(), containsString("User 'ps' already exists in 'admin'."));
        }
    }

    /**
     * This is a test for a specific bug at a customer installation caused by a StackOverflowException in Xerces.
     * It seems to be caused by a regex bug in nonEmptyString.
     */
    @Test
    public void shouldLoadConfigurationFileWithComplexNonEmptyString() throws Exception {
        String customerXML = this.getClass().getResource("/data/p4_heavy_cruise_config.xml").getFile();
        assertThat(loadWithMigration(customerXML), not(nullValue()));
    }

    private CruiseConfig loadWithMigration(String file) throws Exception {
        FileInputStream input = new FileInputStream(file);

        return ConfigMigrator.loadWithMigration(input).config;
    }

    @Test
    public void shouldNotAllowEmptyViewForPerforce() throws Exception {
        try {
            String p4XML = this.getClass().getResource("/data/p4-cruise-config-empty-view.xml").getFile();
            loadWithMigration(p4XML);
            fail("Should not accept p4 section with empty view.");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("P4 view cannot be empty."));
        }
    }

    @Test
    public void shouldLoadPipelineWithMultipleMaterials() throws Exception {
        String pipelineXmlPartial =
                "<pipeline name=\"pipeline\">\n"
                        + "  <materials>\n"
                        + "    <svn url=\"/hgrepo1\" dest=\"folder1\" />\n"
                        + "    <svn url=\"/hgrepo2\" dest=\"folder2\" />\n"
                        + "    <svn url=\"/hgrepo3\" dest=\"folder3\" />\n"
                        + "  </materials>\n"
                        + "  <stage name=\"mingle\">\n"
                        + "    <jobs>\n"
                        + "      <job name=\"functional\">\n"
                        + "        <artifacts>\n"
                        + "          <log src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
                        + "        </artifacts>\n"
                        + "      </job>\n"
                        + "    </jobs>\n"
                        + "  </stage>\n"
                        + "</pipeline>\n";
        PipelineConfig pipeline = xmlLoader.fromXmlPartial(toInputStream(pipelineXmlPartial), PipelineConfig.class);
        assertThat(pipeline.materialConfigs().size(), is(3));
        ScmMaterialConfig material = (ScmMaterialConfig) pipeline.materialConfigs().get(0);
        assertThat(material.getFolder(), is("folder1"));
    }

    @Test
    public void shouldThrowErrorIfMultipleMaterialsHaveSameFolders() throws Exception {
        String materials =
                "  <materials>\n"
                        + "    <svn url=\"/hgrepo1\" dest=\"folder1\" />\n"
                        + "    <svn url=\"/hgrepo2\" dest=\"folder1\" />\n"
                        + "  </materials>\n";
        MagicalGoConfigXmlLoaderFixture.assertNotValid(INVALID_DESTINATION_DIRECTORY_MESSAGE, materials);
    }

    @Test
    public void shouldThrowErrorIfOneOfMultipleMaterialsHasNoFolder() throws Exception {
        String materials =
                "  <materials>\n"
                        + "    <svn url=\"/hgrepo1\" />\n"
                        + "    <svn url=\"/hgrepo2\" dest=\"folder1\" />\n"
                        + "  </materials>\n";
        String message = "Destination directory is required when specifying multiple scm materials";
        MagicalGoConfigXmlLoaderFixture.assertNotValid(message, materials);
    }

    @Test
    public void shouldThrowErrorIfOneOfMultipleMaterialsIsNested() throws Exception {
        String materials =
                "  <materials>\n"
                        + "    <svn url=\"/hgrepo1\" dest=\"folder1\"/>\n"
                        + "    <svn url=\"/hgrepo2\" dest=\"folder1/folder2\" />\n"
                        + "  </materials>\n";
        MagicalGoConfigXmlLoaderFixture.assertNotValid(INVALID_DESTINATION_DIRECTORY_MESSAGE, materials);
    }

    //This is bug #2337
    @Test
    public void shouldNotThrowErrorIfMultipleMaterialsHaveSimilarNamesBug2337() throws Exception {
        String materials =
                "  <materials>\n"
                        + "    <svn url=\"/hgrepo1\" dest=\"folder1/folder2\"/>\n"
                        + "    <svn url=\"/hgrepo2\" dest=\"folder1/folder2different\" />\n"
                        + "  </materials>\n";
        assertValidMaterials(materials);
    }

    //This is bug #2337
    @Test
    public void shouldNotThrowErrorIfMultipleMaterialsHaveSimilarNamesInDifferentOrder() throws Exception {
        String materials =
                "  <materials>\n"
                        + "    <svn url=\"/hgrepo2\" dest=\"folder1/folder2different\" />\n"
                        + "    <svn url=\"/hgrepo1\" dest=\"folder1/folder2\"/>\n"
                        + "  </materials>\n";
        assertValidMaterials(materials);
    }

    @Test
    public void shouldNotAllowfoldersOutsideWorkingDirectory() throws Exception {
        String materials =
                "  <materials>\n"
                        + "    <svn url=\"/hgrepo2\" dest=\"folder1/folder2/../folder3\" />\n"
                        + "  </materials>\n";
        MagicalGoConfigXmlLoaderFixture.assertValid(materials);
        String materials2 =
                "  <materials>\n"
                        + "    <svn url=\"/hgrepo2\" dest=\"../../..\" />\n"
                        + "  </materials>\n";
        MagicalGoConfigXmlLoaderFixture.assertNotValid(
                "File path is invalid. \"../../..\" should conform to the pattern - (([.]\\/)?[.][^. ]+)|([^. ].+[^. ])|([^. ][^. ])|([^. ])", materials2);
    }

    @Test
    public void shouldAllowPathStartWithDotSlash() throws Exception {
        String materials =
                "  <materials>\n"
                        + "    <svn url=\"/hgrepo2\" dest=\"./folder3\" />\n"
                        + "  </materials>\n";
        MagicalGoConfigXmlLoaderFixture.assertValid(materials);
    }

    @Test
    public void shouldAllowHiddenFolders() throws Exception {
        String materials =
                "  <materials>\n"
                        + "    <svn url=\"/hgrepo2\" dest=\".folder3\" />\n"
                        + "  </materials>\n";
        MagicalGoConfigXmlLoaderFixture.assertValid(materials);

        materials =
                "  <materials>\n"
                        + "    <svn url=\"/hgrepo2\" dest=\"./.folder3\" />\n"
                        + "  </materials>\n";
        MagicalGoConfigXmlLoaderFixture.assertValid(materials);
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldNotAllowAbsoluteDestFolderNamesOnLinux() throws Exception {
        String materials1 =
                "  <materials>\n"
                        + "    <svn url=\"/hgrepo2\" dest=\"/tmp/foo\" />\n"
                        + "  </materials>\n";
        MagicalGoConfigXmlLoaderFixture.assertNotValid("Dest folder '/tmp/foo' is not valid. It must be a sub-directory of the working folder.",
                materials1);
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {EnhancedOSChecker.WINDOWS})
    public void shouldNotAllowAbsoluteDestFolderNamesOnWindows() throws Exception {
        String materials1 =
                "  <materials>\n"
                        + "    <svn url=\"/hgrepo2\" dest=\"C:\\tmp\\foo\" />\n"
                        + "  </materials>\n";
        MagicalGoConfigXmlLoaderFixture.assertNotValid("Dest folder 'C:\\tmp\\foo' is not valid. It must be a sub-directory of the working folder.",
                materials1);
    }

    @Test
    public void shouldNotThrowErrorIfMultipleMaterialsHaveSameNames() throws Exception {
        String materials =
                "  <materials>\n"
                        + "    <svn url=\"/hgrepo1\" dest=\"folder1/folder2\"/>\n"
                        + "    <svn url=\"/hgrepo2\" dest=\"folder1/folder2\" />\n"
                        + "  </materials>\n";
        MagicalGoConfigXmlLoaderFixture.assertNotValid(INVALID_DESTINATION_DIRECTORY_MESSAGE, materials);
    }

    @Test
    public void shouldSupportHgGitSvnP4ForMultipleMaterials() throws Exception {
        String materials =
                "  <materials>\n"
                        + "    <svn url=\"/hgrepo1\" dest=\"folder1\"/>\n"
                        + "    <git url=\"/hgrepo2\" dest=\"folder2\"/>\n"
                        + "    <hg url=\"/hgrepo2\" dest=\"folder3\"/>\n"
                        + "    <p4 port=\"localhost:1666\" dest=\"folder4\">\n"
                        + "          <view>asd</view>"
                        + "    </p4>"
                        + "  </materials>\n";
        MagicalGoConfigXmlLoaderFixture.assertValid(materials);
    }

    @Test
    public void shouldLoadPipelinesWithGroupName() throws Exception {
        CruiseConfig config = ConfigMigrator.loadWithMigration(ConfigFileFixture.PIPELINE_GROUPS).config;
        assertThat(config.getGroups().first().getGroup(), is("studios"));
        assertThat(config.getGroups().get(1).getGroup(), is("perfessionalservice"));
    }

    @Test
    public void shouldLoadTasksWithExecutionCondition() throws Exception {
        CruiseConfig config = ConfigMigrator.loadWithMigration(ConfigFileFixture.TASKS_WITH_CONDITION).config;
        JobConfig job = config.jobConfigByName("pipeline1", "mingle", "cardlist", true);

        assertThat(job.tasks().size(), is(2));
        assertThat(job.tasks().findFirstByType(AntTask.class).getConditions().get(0), is(new RunIfConfig("failed")));

        RunIfConfigs conditions = job.tasks().findFirstByType(NantTask.class).getConditions();
        assertThat(conditions.get(0), is(new RunIfConfig("failed")));
        assertThat(conditions.get(1), is(new RunIfConfig("any")));
        assertThat(conditions.get(2), is(new RunIfConfig("passed")));
    }

    @Test
    public void shouldLoadTasksWithOnCancel() throws Exception {
        CruiseConfig config = ConfigMigrator.loadWithMigration(ConfigFileFixture.TASKS_WITH_ON_CANCEL).config;
        JobConfig job = config.jobConfigByName("pipeline1", "mingle", "cardlist", true);

        Task task = job.tasks().findFirstByType(AntTask.class);
        assertThat(task.hasCancelTask(), is(true));
        assertThat((ExecTask) task.cancelTask(), is(new ExecTask("kill.rb", "", "utils")));

        Task task2 = job.tasks().findFirstByType(ExecTask.class);
        assertThat(task2.hasCancelTask(), is(false));
    }

    @Test
    public void shouldNotLoadTasksWithOnCancelTaskNested() throws Exception {
        try {
            ConfigMigrator.loadWithMigration(ConfigFileFixture.TASKS_WITH_ON_CANCEL_NESTED);
            fail("Should not allow nesting of 'oncancel' within task inside oncancel");
        } catch (Exception expected) {
            // Carrots are good for your eyes
        }
    }

    @Test
    public void shouldNotLoadTasksWithEmptyOnCancelTask() throws Exception {
        try {
            ConfigMigrator.loadWithMigration(ConfigFileFixture.TASKS_WITH_EMPTY_ON_CANCEL);
            fail("Should not allow empty 'oncancel'");
        } catch (Exception expected) {
        }
    }

    @Test
    public void shouldAllowBothCounterAndMaterialNameInLabelTemplate() throws Exception {
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(ConfigFileFixture.LABEL_TEMPLATE_WITH_LABEL_TEMPLATE("1.3.0-${COUNT}-${git}")).config;
        assertThat(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("cruise")).getLabelTemplate(), is("1.3.0-${COUNT}-${git}"));
    }

    @Test
    public void shouldAllowBothCounterAndTruncatedGitMaterialInLabelTemplate() throws Exception {
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(ConfigFileFixture.LABEL_TEMPLATE_WITH_LABEL_TEMPLATE("1.3.0-${COUNT}-${git[:7]}", CONFIG_SCHEMA_VERSION)).config;
        assertThat(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("cruise")).getLabelTemplate(), is("1.3.0-${COUNT}-${git[:7]}"));
    }

    @Test
    public void shouldAllowHashCharacterInLabelTemplate() throws Exception {
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(ConfigFileFixture.LABEL_TEMPLATE_WITH_LABEL_TEMPLATE("1.3.0-${COUNT}-${git}##", CONFIG_SCHEMA_VERSION)).config;
        assertThat(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("cruise")).getLabelTemplate(), is("1.3.0-${COUNT}-${git}#"));
    }

    @Test
    public void shouldNotAllowInvalidLabelTemplate() throws Exception {
        assertPipelineLabelTemplate("1.3.0");

        assertPipelineLabelTemplate("1.3.0-{COUNT}");
        assertPipelineLabelTemplate("1.3.0-$COUNT}");
        assertPipelineLabelTemplate("1.3.0-${COUNT");
        assertPipelineLabelTemplate("1.3.0-${}");

        assertPipelineLabelTemplate("1.3.0-${COUNT}-${git:7]}");
        assertPipelineLabelTemplate("1.3.0-${COUNT}-${git[:7}");
        assertPipelineLabelTemplate("1.3.0-${COUNT}-${git[7]}");
        assertPipelineLabelTemplate("1.3.0-${COUNT}-${git[:]}");
        assertPipelineLabelTemplate("1.3.0-${COUNT}-${git[:-1]}");
    }

    public void assertPipelineLabelTemplate(String labelTemplate) {
        try {
            ConfigMigrator.loadWithMigration(ConfigFileFixture.LABEL_TEMPLATE_WITH_LABEL_TEMPLATE(labelTemplate, 75));
            fail("should have failed");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Label is invalid."));
        }
    }

    @Test
    public void shouldLoadMaterialNameIfPresent() throws Exception {
        CruiseConfig config = ConfigMigrator.loadWithMigration(ConfigFileFixture.MATERIAL_WITH_NAME).config;
        MaterialConfigs materialConfigs = config.pipelineConfigByName(new CaseInsensitiveString("pipeline")).materialConfigs();
        assertThat(materialConfigs.get(0).getName(), is(new CaseInsensitiveString("svn")));
        assertThat(materialConfigs.get(1).getName(), is(new CaseInsensitiveString("hg")));
    }

    @Test
    public void shouldLoadPipelineWithTimer() throws Exception {
        CruiseConfig config = ConfigMigrator.loadWithMigration(ConfigFileFixture.PIPELINE_WITH_TIMER).config;
        PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString("pipeline"));
        assertThat(pipelineConfig.getTimer(), is(new TimerConfig("0 15 10 ? * MON-FRI", false)));
    }

    @Test
    public void shouldLoadConfigWithEnvironment() throws Exception {
        String content = ConfigFileFixture.configWithEnvironments(
                "<environments>"
                        + "  <environment name='uat'>"
                        + "    <agents>"
                        + "      <physical uuid='1'/>"
                        + "      <physical uuid='2'/>"
                        + "    </agents>"
                        + "  </environment>"
                        + "  <environment name='prod'>"
                        + "    <agents>"
                        + "      <physical uuid='2'/>"
                        + "    </agents>"
                        + "  </environment>"
                        + "</environments>");
        EnvironmentsConfig environmentsConfig = ConfigMigrator.loadWithMigration(content).config.getEnvironments();
        EnvironmentPipelineMatchers matchers = environmentsConfig.matchers();
        assertThat(matchers.size(), is(2));
        ArrayList<String> uat_uuids = new ArrayList<String>() {{
            add("1");
            add("2");
        }};
        ArrayList<String> prod_uuids = new ArrayList<String>() {{
            add("2");
        }};
        assertThat(matchers,
                hasItem(new EnvironmentPipelineMatcher(new CaseInsensitiveString("uat"), uat_uuids, new EnvironmentPipelinesConfig())));
        assertThat(matchers,
                hasItem(new EnvironmentPipelineMatcher(new CaseInsensitiveString("prod"), prod_uuids, new EnvironmentPipelinesConfig())));
    }

    @Test
    public void shouldNotLoadConfigWithEmptyTemplates() throws Exception {
        String content = ConfigFileFixture.configWithTemplates(
                "<templates>"
                        + "</templates>");
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("Should not allow empty templates block");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("The content of element 'templates' is not complete. One of '{pipeline}' is expected."));
        }
    }

    @Test
    public void shouldNotLoadConfigWhenPipelineHasNoStages() throws Exception {
        String content = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' />"
                + "<pipelines>\n"
                + "<pipeline name='pipeline1'>\n"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "</cruise>";
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("Should not allow Pipeline with No Stages");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("Pipeline 'pipeline1' does not have any stages configured. A pipeline must have at least one stage."));
        }
    }

    @Test
    public void shouldNotAllowReferencingTemplateThatDoesNotExist() throws Exception {
        String content = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' />"
                + "<pipelines>\n"
                + "<pipeline name='pipeline1' template='abc'>\n"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "</cruise>";
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("shouldNotAllowReferencingTemplateThatDoesNotExist");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("Pipeline 'pipeline1' refers to non-existent template 'abc'."));
        }
    }

    @Test
    public void shouldAllowPipelineToReferenceTemplate() throws Exception {
        String content = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts'>"
                + "</server>"
                + "<pipelines>\n"
                + "<pipeline name='pipeline1' template='abc'>\n"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "<templates>\n"
                + "  <pipeline name='abc'>\n"
                + "    <stage name='stage1'>"
                + "      <jobs>"
                + "        <job name='job1' />"
                + "      </jobs>"
                + "    </stage>"
                + "  </pipeline>\n"
                + "</templates>\n"
                + "</cruise>";
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        assertThat(pipelineConfig.size(), is(1));
    }

    @Test
    public void shouldAllowAdminInPipelineGroups() throws Exception {
        String content = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' >"
                + "</server>"
                + "<pipelines group=\"first\">\n"
                + "<authorization>"
                + "     <admins>\n"
                + "         <user>foo</user>\n"
                + "      </admins>"
                + "</authorization>"
                + "<pipeline name='pipeline1' template='abc'>\n"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "<templates>\n"
                + "  <pipeline name='abc'>\n"
                + "    <stage name='stage1'>"
                + "      <jobs>"
                + "        <job name='job1' />"
                + "      </jobs>"
                + "    </stage>"
                + "  </pipeline>\n"
                + "</templates>\n"
                + "</cruise>";
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        assertThat(cruiseConfig.schemaVersion(), is(CONFIG_SCHEMA_VERSION));
        assertThat(cruiseConfig.findGroup("first").isUserAnAdmin(new CaseInsensitiveString("foo"), new ArrayList<Role>()), is(true));
    }

    @Test
    public void shouldAllowAdminWithRoleInPipelineGroups() throws Exception {
        String content = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' >"
                + "<security>\n"
                + "      <passwordFile path=\"/etc/cruise/password.properties\" />\n"
                + "      <roles>\n"
                + "        <role name=\"bar\">\n"
                + "          <users>"
                + "             <user>foo</user>"
                + "          </users>"
                + "        </role>"
                + "      </roles>"
                + "</security>"
                + "</server>"
                + "<pipelines group=\"first\">\n"
                + "<authorization>"
                + "     <admins>\n"
                + "         <role>bar</role>\n"
                + "      </admins>"
                + "</authorization>"
                + "<pipeline name='pipeline1' template='abc'>\n"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "<templates>\n"
                + "  <pipeline name='abc'>\n"
                + "    <stage name='stage1'>"
                + "      <jobs>"
                + "        <job name='job1' />"
                + "      </jobs>"
                + "    </stage>"
                + "  </pipeline>\n"
                + "</templates>\n"
                + "</cruise>";
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        assertThat(cruiseConfig.schemaVersion(), is(CONFIG_SCHEMA_VERSION));
        assertThat(cruiseConfig.findGroup("first").isUserAnAdmin(new CaseInsensitiveString("foo"), asList(new Role(new CaseInsensitiveString("bar")))), is(true));
    }

    @Test
    public void shouldAddJobTimeoutAttributeToServerTagAndDefaultItTo60_37xsl() {
        String content = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' siteUrl='http://www.someurl.com/go' secureSiteUrl='https://www.someotherurl.com/go' >"
                + "</server></cruise>";
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        assertThat(cruiseConfig.server().getJobTimeout(), is("0"));
    }

    @Test
    public void shouldGetTheJobTimeoutFromServerTag_37xsl() {
        String content = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' siteUrl='http://www.someurl.com/go' secureSiteUrl='https://www.someotherurl.com/go' jobTimeout='30' >"
                + "</server></cruise>";
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        assertThat(cruiseConfig.server().getJobTimeout(), is("30"));
    }

    @Test
    public void shouldHaveJobTimeoutAttributeOnJob_37xsl() {
        String content = CONFIG_WITH_ANT_BUILDER;
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        JobConfig jobConfig = cruiseConfig.findJob("pipeline1", "mingle", "cardlist");
        assertThat(jobConfig.getTimeout(), is("5"));
    }

    @Test
    public void shouldAllowSiteUrlandSecureSiteUrlAttributes() {
        String content = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' siteUrl='http://www.someurl.com/go' secureSiteUrl='https://www.someotherurl.com/go' >"
                + "</server></cruise>";
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        assertThat(cruiseConfig.server().getSiteUrl(), is(new ServerSiteUrlConfig("http://www.someurl.com/go")));
        assertThat(cruiseConfig.server().getSecureSiteUrl(), is(new ServerSiteUrlConfig("https://www.someotherurl.com/go")));
    }

    @Test
    public void shouldAllowPurgeStartAndPurgeUptoAttributes() {
        String content = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' purgeStart='1' purgeUpto='3'>"
                + "</server></cruise>";
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        assertThat(cruiseConfig.server().getPurgeStart(), is(1.0));
        assertThat(cruiseConfig.server().getPurgeUpto(), is(3.0));
    }

    @Test
    public void shouldAllowDoublePurgeStartAndPurgeUptoAttributes() {
        String content = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' purgeStart='1.2' purgeUpto='3.4'>"
                + "</server></cruise>";
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        assertThat(cruiseConfig.server().getPurgeStart(), is(1.2));
        assertThat(cruiseConfig.server().getPurgeUpto(), is(3.4));
    }

    @Test
    public void shouldAllowNullPurgeStartAndEnd() {
        String content = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts'>"
                + "</server></cruise>";
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        assertThat(cruiseConfig.server().getPurgeStart(), is(nullValue()));
        assertThat(cruiseConfig.server().getPurgeUpto(), is(nullValue()));
    }


    @Test
    public void shouldNotAllowAPipelineThatReferencesATemplateToHaveStages() throws Exception {
        String content = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' />"
                + "<pipelines>\n"
                + "<pipeline name='pipeline1' template='abc'>\n"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "    <stage name='badstage'>"
                + "      <jobs>"
                + "        <job name='job1' />"
                + "      </jobs>"
                + "    </stage>"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "<templates>\n"
                + "  <pipeline name='abc'>\n"
                + "    <stage name='stage1'>"
                + "      <jobs>"
                + "        <job name='job1' />"
                + "      </jobs>"
                + "    </stage>"
                + "  </pipeline>\n"
                + "</templates>\n"
                + "</cruise>";
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("shouldn't have stages and template");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("Cannot add stage 'badstage' to pipeline 'pipeline1', which already references template 'abc'."));
        }
    }

    @Test
    public void shouldLoadConfigWithPipelineTemplate() throws Exception {
        String content = ConfigFileFixture.configWithTemplates(
                "<templates>"
                        + "  <pipeline name='erbshe'>"
                        + "    <stage name='stage1'>"
                        + "      <jobs>"
                        + "        <job name='job1' />"
                        + "      </jobs>"
                        + "    </stage>"
                        + "  </pipeline>"
                        + "</templates>");
        TemplatesConfig templates = ConfigMigrator.loadWithMigration(content).config.getTemplates();
        assertThat(templates.size(), is(1));
        assertThat(templates.get(0).size(), is(1));
        assertThat(templates.get(0).get(0), is(StageConfigMother.custom("stage1", "job1")));
    }

    @Test
    public void shouldLoadConfigWith2PipelineTemplates() throws Exception {
        String content = ConfigFileFixture.configWithTemplates(
                "<templates>"
                        + "  <pipeline name='erbshe'>"
                        + "    <stage name='stage1'>"
                        + "      <jobs>"
                        + "        <job name='job1' />"
                        + "      </jobs>"
                        + "    </stage>"
                        + "  </pipeline>"
                        + "  <pipeline name='erbshe2'>"
                        + "    <stage name='stage1'>"
                        + "      <jobs>"
                        + "        <job name='job1' />"
                        + "      </jobs>"
                        + "    </stage>"
                        + "  </pipeline>"
                        + "</templates>");
        TemplatesConfig templates = ConfigMigrator.loadWithMigration(content).config.getTemplates();
        assertThat(templates.size(), is(2));
        assertThat(templates.get(0).name(), is(new CaseInsensitiveString("erbshe")));
        assertThat(templates.get(1).name(), is(new CaseInsensitiveString("erbshe2")));
    }


    @Test
    public void shouldOnlySupportUniquePipelineTemplates() throws Exception {
        String content = ConfigFileFixture.configWithTemplates(
                "<templates>"
                        + "  <pipeline name='erbshe'>"
                        + "    <stage name='stage1'>"
                        + "      <jobs>"
                        + "        <job name='job1' />"
                        + "      </jobs>"
                        + "    </stage>"
                        + "  </pipeline>"
                        + "  <pipeline name='erbshe'>"
                        + "    <stage name='stage1'>"
                        + "      <jobs>"
                        + "        <job name='job1' />"
                        + "      </jobs>"
                        + "    </stage>"
                        + "  </pipeline>"
                        + "</templates>");
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("should not allow same template names");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("Duplicate unique value [erbshe] declared for identity constraint"));
        }
    }

    @Test
    public void shouldNotAllowEmptyPipelineTemplates() throws Exception {
        String content = ConfigFileFixture.configWithTemplates(
                "<templates>"
                        + "  <pipeline name='erbshe'>"
                        + "  </pipeline>"
                        + "</templates>");
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("should NotAllowEmptyPipelineTemplates");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString("The content of element 'pipeline' is not complete. One of '{authorization, stage}' is expected"));
        }
    }

    @Test
    public void shouldNotAllowJobToHaveTheRunOnAllAgentsMarkerInItsName() throws Exception {
        String invalidJobName = format("%s-%s-%s", "invalid-name", RunOnAllAgentsJobTypeConfig.MARKER, 1);
        testForInvalidJobName(invalidJobName, RunOnAllAgentsJobTypeConfig.MARKER);
    }

    @Test
    public void shouldNotAllowJobToHaveTheRunInstanceMarkerInItsName() throws Exception {
        String invalidJobName = format("%s-%s-%s", "invalid-name", RunMultipleInstanceJobTypeConfig.MARKER, 1);
        testForInvalidJobName(invalidJobName, RunMultipleInstanceJobTypeConfig.MARKER);
    }

    private void testForInvalidJobName(String invalidJobName, String marker) {
        String content = ConfigFileFixture.configWithPipeline(
                "    <pipeline name=\"dev\">\n"
                        + "      <materials>\n"
                        + "        <svn url=\"file:///tmp/svn/repos/fifth\" />\n"
                        + "      </materials>\n"
                        + "      <stage name=\"AutoStage\">\n"
                        + "        <jobs>\n"
                        + "          <job name=\"" + invalidJobName + "\">\n"
                        + "            <tasks>\n"
                        + "              <exec command=\"ls\" args=\"-lah\" />\n"
                        + "            </tasks>\n"
                        + "          </job>\n"
                        + "        </jobs>\n"
                        + "      </stage>\n"
                        + "    </pipeline>"
        );
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("should not allow jobs with with name '" + marker + "'");
        } catch (Exception expected) {
            assertThat(expected.getMessage(), containsString(String.format("A job cannot have '%s' in it's name: %s because it is a reserved keyword", marker, invalidJobName)));
        }
    }

    @Test
    public void shouldAllow_NonRunOnAllAgentJobToHavePartsOfTheRunOnAll_and_NonRunMultipleInstanceJobToHavePartsOfTheRunInstance_AgentsMarkerInItsName() throws Exception {
        String content = ConfigFileFixture.configWithPipeline(
                "    <pipeline name=\"dev\">\n"
                        + "      <materials>\n"
                        + "        <svn url=\"file:///tmp/svn/repos/fifth\" />\n"
                        + "      </materials>\n"
                        + "      <stage name=\"AutoStage\">\n"
                        + "        <jobs>\n"
                        + "          <job name=\"valid-name-runOnAll\" >\n"
                        + "            <tasks>\n"
                        + "              <exec command=\"ls\" args=\"-lah\" />\n"
                        + "            </tasks>\n"
                        + "          </job>\n"
                        + "          <job name=\"valid-name-runInstance\" >\n"
                        + "            <tasks>\n"
                        + "              <exec command=\"ls\" args=\"-lah\" />\n"
                        + "            </tasks>\n"
                        + "          </job>\n"
                        + "        </jobs>\n"
                        + "      </stage>\n"
                        + "    </pipeline>");
        ConfigMigrator.loadWithMigration(content); // should not fail with a validation exception
    }

    @Test
    public void shouldLoadLargeConfigFileInReasonableTime() throws Exception {
        String content = FileUtil.readToEnd(getClass().getResourceAsStream("/data/big-cruise-config.xml"));
//        long start = System.currentTimeMillis();
        GoConfigHolder configHolder = ConfigMigrator.loadWithMigration(content);
//        assertThat(System.currentTimeMillis() - start, lessThan(new Long(2000)));
        assertThat(configHolder.config.schemaVersion(), is(CONFIG_SCHEMA_VERSION));
    }

    @Test
    public void shouldLoadConfigWithPipelinesMatchingUpWithPipelineDefinitionCaseInsensitively() throws Exception {
        String content = ConfigFileFixture.configWithEnvironments(
                "<environments>"
                        + "  <environment name='uat'>"
                        + "    <pipelines>"
                        + "      <pipeline name='piPeline1'/>"
                        + "    </pipelines>"
                        + "  </environment>"
                        + "</environments>");
        EnvironmentsConfig environmentsConfig = ConfigMigrator.loadWithMigration(content).config.getEnvironments();
        EnvironmentPipelineMatcher matcher = environmentsConfig.matchersForPipeline("pipeline1");
        assertThat(matcher, is(new EnvironmentPipelineMatcher(new CaseInsensitiveString("uat"), new ArrayList<String>(),
                new EnvironmentPipelinesConfig(new CaseInsensitiveString("piPeline1")))));
    }

    @Test
    public void shouldNotAllowConfigWithUnknownPipeline() throws Exception {
        String content = ConfigFileFixture.configWithEnvironments(
                "<environments>"
                        + "  <environment name='uat'>"
                        + "    <pipelines>"
                        + "      <pipeline name='notpresent'/>"
                        + "    </pipelines>"
                        + "  </environment>"
                        + "</environments>");
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("Should not have allowed referencing of an unknown pipeline under an environment.");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Environment 'uat' refers to an unknown pipeline 'notpresent'."));
        }

    }

    @Test
    public void shouldNotAllowDuplicatePipelineAcrossEnvironments() throws Exception {
        String content = ConfigFileFixture.configWithEnvironments(
                "<environments>"
                        + "  <environment name='uat'>"
                        + "    <pipelines>"
                        + "      <pipeline name='pipeline1'/>"
                        + "    </pipelines>"
                        + "  </environment>"
                        + "  <environment name='prod'>"
                        + "    <pipelines>"
                        + "      <pipeline name='Pipeline1'/>"
                        + "    </pipelines>"
                        + "  </environment>"
                        + "</environments>");
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("Should not have allowed duplicate pipeline reference across environments");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Associating pipeline(s) which is already part of uat environment"));
        }
    }

    @Test
    public void shouldNotAllowDuplicatePipelinesInASingleEnvironment() throws Exception {
        String content = ConfigFileFixture.configWithEnvironments(
                "<environments>"
                        + "  <environment name='uat'>"
                        + "    <pipelines>"
                        + "      <pipeline name='pipeline1'/>"
                        + "      <pipeline name='Pipeline1'/>"
                        + "    </pipelines>"
                        + "  </environment>"
                        + "</environments>");
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("Should not have allowed duplicate pipeline reference under an environment");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Cannot add pipeline 'Pipeline1' to the environment"));
        }
    }

    @Test
    public void shouldNotAllowConfigWithEnvironmentsWithSameNames() throws Exception {
        String content = ConfigFileFixture.configWithEnvironments(
                "<environments>"
                        + "  <environment name='uat' />"
                        + "  <environment name='uat' />"
                        + "</environments>");
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("Should not support 2 environments with the same same");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Duplicate unique value [uat] declared for identity constraint \"uniqueEnvironmentName\" of element \"environments\""));
        }
    }

    @Test
    public void shouldNotAllowConfigWithInvalidName() throws Exception {
        String content = ConfigFileFixture.configWithEnvironments(
                "<environments>"
                        + "  <environment name='exclamation is invalid !' />"
                        + "</environments>");
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("XSD should not allow invalid characters");
        } catch (Exception e) {
            assertThat(e.getMessage(),
                    containsString("\"exclamation is invalid !\" should conform to the pattern - [a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*"));
        }
    }

    @Test
    public void shouldNotAllowConfigWithAbsentReferencedAgentUuid() throws Exception {
        String content = ConfigFileFixture.configWithEnvironmentsAndAgents(
                "<environments>"
                        + "  <environment name='uat'>"
                        + "    <agents>"
                        + "      <physical uuid='missing' />"
                        + "    </agents>"
                        + "  </environment>"
                        + "</environments>",

                "<agents>"
                        + "  <agent uuid='1' hostname='test1.com' ipaddress='192.168.0.1' />"
                        + "</agents>");
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("XSD should not allow reference to absent agent");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Environment 'uat' has an invalid agent uuid 'missing'"));
        }
    }

    @Test
    public void shouldAllowConfigWithEmptyPipeline() throws Exception {
        String content = ConfigFileFixture.configWithEnvironments(
                "<environments>"
                        + "  <environment name='uat'>"
                        + "    <pipelines/>"
                        + "  </environment>"
                        + "</environments>");
        try {
            ConfigMigrator.loadWithMigration(content);
        } catch (Exception e) {
            fail("should not allow empty pipelines block under an environment");
        }
    }

    @Test
    public void shouldAllowConfigWithEmptyAgents() throws Exception {
        String content = ConfigFileFixture.configWithEnvironments(
                "<environments>"
                        + "  <environment name='uat'>"
                        + "    <agents/>"
                        + "  </environment>"
                        + "</environments>");
        try {
            ConfigMigrator.loadWithMigration(content);
        } catch (Exception e) {
            fail("should not allow empty agents block under an environment");
        }
    }

    @Test
    public void shouldNotAllowConfigWithDuplicateAgentUuidInEnvironment() throws Exception {
        String content = ConfigFileFixture.configWithEnvironmentsAndAgents(
                "<environments>"
                        + "  <environment name='uat'>"
                        + "    <agents>"
                        + "      <physical uuid='1' />"
                        + "      <physical uuid='1' />"
                        + "    </agents>"
                        + "  </environment>"
                        + "</environments>",

                "<agents>"
                        + "  <agent uuid='1' hostname='test1.com' ipaddress='192.168.0.1' />"
                        + "</agents>");
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("XSD should not allow duplicate agent uuid in environment");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(
                    "Duplicate unique value [1] declared for identity constraint \"uniqueEnvironmentAgentsUuid\" of element \"agents\"."));
        }
    }

    @Test
    public void shouldNotAllowConfigWithEmptyEnvironmentsBlock() throws Exception {
        String content = ConfigFileFixture.configWithEnvironments(
                "<environments>"
                        + "</environments>");
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("XSD should not allow empty environments block");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(
                    "The content of element 'environments' is not complete. One of '{environment}' is expected."));
        }
    }

    @Test
    public void shouldAllowConfigWithNoAgentsAndNoPipelinesInEnvironment() throws Exception {
        String content = ConfigFileFixture.configWithEnvironments(
                "<environments>"
                        + "  <environment name='uat' />"
                        + "</environments>");
        CruiseConfig config = ConfigMigrator.loadWithMigration(content).config;
        assertThat(config.getEnvironments().size(), is(1));
    }

    @Test
    public void shouldAllowConfigWithEnvironmentReferencingDisabledAgent() throws Exception {
        String content = ConfigFileFixture.configWithEnvironmentsAndAgents(
                "<environments>"
                        + "  <environment name='uat'>"
                        + "    <agents>"
                        + "      <physical uuid='1' />"
                        + "    </agents>"
                        + "  </environment>"
                        + "</environments>",

                "<agents>"
                        + "  <agent uuid='1' hostname='test1.com' ipaddress='192.168.0.1' isDisabled='true' />"
                        + "</agents>");
        CruiseConfig config = ConfigMigrator.loadWithMigration(content).config;
        assertThat(config.getEnvironments().matchers().size(), is(1));
    }

    @Test
    public void shouldSupportEnvironmentVariablesInEnvironment() throws Exception {
        String content = ConfigFileFixture.configWithEnvironmentsAndAgents(
                "<environments>"
                        + "  <environment name='uat'>"
                        + "     <environmentvariables> "
                        + "         <variable name='VAR_NAME_1'><value>variable_name_value_1</value></variable>"
                        + "         <variable name='CRUISE_ENVIRONEMNT_NAME'><value>variable_name_value_2</value></variable>"
                        + "     </environmentvariables> "
                        + "  </environment>"
                        + "</environments>",

                "<agents>"
                        + "  <agent uuid='1' hostname='test1.com' ipaddress='192.168.0.1' isDisabled='true' />"
                        + "</agents>");
        CruiseConfig config = ConfigMigrator.loadWithMigration(content).config;
        EnvironmentConfig element = new BasicEnvironmentConfig(new CaseInsensitiveString("uat"));
        element.addEnvironmentVariable("VAR_NAME_1", "variable_name_value_1");
        element.addEnvironmentVariable("CRUISE_ENVIRONEMNT_NAME", "variable_name_value_2");
        assertThat(config.getEnvironments(), hasItem(element));
    }

    @Test
    public void shouldAllowCDATAInEnvironmentVariableValues() throws Exception {
        //TODO : This should be fixed as part of #4865
        //String multiLinedata = "\nsome data\nfoo bar";
        String multiLinedata = "some data\nfoo bar";
        String content = ConfigFileFixture.configWithEnvironmentsAndAgents(
                "<environments>"
                        + "  <environment name='uat'>"
                        + "     <environmentvariables> "
                        + "         <variable name='cdata'><value><![CDATA[" + multiLinedata + "]]></value></variable>"
                        + "     </environmentvariables> "
                        + "  </environment>"
                        + "</environments>",

                "<agents>"
                        + "  <agent uuid='1' hostname='test1.com' ipaddress='192.168.0.1' isDisabled='true' />"
                        + "</agents>");
        CruiseConfig config = ConfigMigrator.loadWithMigration(content).config;
        EnvironmentConfig element = new BasicEnvironmentConfig(new CaseInsensitiveString("uat"));
        element.addEnvironmentVariable("cdata", multiLinedata);
        assertThat(config.getEnvironments().get(0), is(element));
    }

    @Test
    public void shouldAllowOnlyOneTimerOnAPipeline() throws Exception {
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='pipeline1'>"
                        + "    <timer>1 1 1 * * ? *</timer>"
                        + "    <timer>2 2 2 * * ? *</timer>"
                        + "    <materials>"
                        + "      <svn url ='svnurl'/>"
                        + "    </materials>"
                        + "  <stage name='mingle'>"
                        + "    <jobs>"
                        + "      <job name='cardlist' />"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", 81);
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("XSD should not allow duplicate timer in pipeline");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Invalid content was found starting with element 'timer'."));
        }
    }

    @Test
    public void shouldValidateTimerSpec() throws Exception {
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='pipeline1'>"
                        + "    <timer>BAD BAD TIMER!!!!!</timer>"
                        + "    <materials>"
                        + "      <svn url ='svnurl'/>"
                        + "    </materials>"
                        + "  <stage name='mingle'>"
                        + "    <jobs>"
                        + "      <job name='cardlist' />"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", CONFIG_SCHEMA_VERSION);
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("XSD should validate timer spec");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Invalid cron syntax"));
        }
    }

    @Test
    public void shouldNotAllowIllegalValueForRunOnAllAgents() throws Exception {
        try {
            loadJobWithRunOnAllAgents("bad_value");
            fail("should have failed as runOnAllAgents' value is not valid(boolean)");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("'bad_value' is not a valid value for 'boolean'"));
        }
    }

    @Test
    public void shouldNotAllowIllegalValueForRunMultipleInstanceJob() throws Exception {
        try {
            loadJobWithRunMultipleInstance("-1");
            fail("should have failed as runOnAllAgents' value is not valid(boolean)");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("'-1' is not facet-valid with respect to minInclusive '1' for type 'positiveInteger'"));
        }

        try {
            loadJobWithRunMultipleInstance("abcd");
            fail("should have failed as runOnAllAgents' value is not valid(boolean)");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("'abcd' is not a valid value for 'integer'"));
        }
    }

    @Test
    public void shouldSupportEnvironmentVariablesInAJob() throws Exception {
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='pipeline1'>"
                        + "    <materials>"
                        + "      <svn url ='svnurl'/>"
                        + "    </materials>"
                        + "  <stage name='mingle'>"
                        + "    <jobs>"
                        + "      <job name='do-something'>"
                        + "      <environmentvariables>"
                        + "         <variable name='JOB_VARIABLE'><value>job variable</value></variable>"
                        + "      </environmentvariables>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", CONFIG_SCHEMA_VERSION);
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;

        JobConfig jobConfig = new JobConfig("do-something");
        jobConfig.addVariable("JOB_VARIABLE", "job variable");
        assertThat(cruiseConfig.findJob("pipeline1", "mingle", "do-something"), is(jobConfig));
    }

    @Test
    public void shouldSupportEnvironmentVariablesInAPipeline() throws Exception {
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='pipeline1'>"
                        + "  <environmentvariables>"
                        + "    <variable name='PIPELINE_VARIABLE'><value>pipeline variable</value></variable>"
                        + "  </environmentvariables>"
                        + "  <materials>"
                        + "    <svn url ='svnurl'/>"
                        + "  </materials>"
                        + "  <stage name='mingle'>"
                        + "    <jobs>"
                        + "      <job name='do-something'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", CONFIG_SCHEMA_VERSION);
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;

        assertThat(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).getVariables(), hasItem(new EnvironmentVariableConfig("PIPELINE_VARIABLE", "pipeline variable")));
    }

    @Test
    public void shouldSupportEnvironmentVariablesInAStage() throws Exception {
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='pipeline1'>"
                        + "  <materials>"
                        + "    <svn url ='svnurl'/>"
                        + "  </materials>"
                        + "  <stage name='mingle'>"
                        + "    <environmentvariables>"
                        + "      <variable name='STAGE_VARIABLE'><value>stage variable</value></variable>"
                        + "    </environmentvariables>"
                        + "    <jobs>"
                        + "      <job name='do-something'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", CONFIG_SCHEMA_VERSION);
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;

        assertThat(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).getFirstStageConfig().getVariables(),
                hasItem(new EnvironmentVariableConfig("STAGE_VARIABLE", "stage variable")));
    }

    @Test
    public void shouldNotAllowDuplicateEnvironmentVariablesInAJob() throws Exception {
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='pipeline1'>"
                        + "    <materials>"
                        + "      <svn url ='svnurl'/>"
                        + "    </materials>"
                        + "  <stage name='mingle'>"
                        + "    <jobs>"
                        + "      <job name='do-something'>"
                        + "      <environmentvariables>"
                        + "         <variable name='JOB_VARIABLE'><value>job variable</value></variable>"
                        + "         <variable name='JOB_VARIABLE'><value>job variable</value></variable>"
                        + "      </environmentvariables>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", CONFIG_SCHEMA_VERSION);
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("Should not allow duplicate variable names");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Environment Variable name 'JOB_VARIABLE' is not unique for job 'do-something'."));
        }
    }

    @Test
    public void shouldNotAllowDuplicateParamsInAPipeline() throws Exception {
        String content = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' />"
                + "<pipelines>\n"
                + "<pipeline name='dev'>\n"
                + "    <params>"
                + "        <param name='same-name'>ls</param>"
                + "        <param name='same-name'>/tmp</param>"
                + "    </params>"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "    <stage name='mingle'>"
                + "      <jobs>"
                + "        <job name='do-something'>"
                + "        </job>"
                + "      </jobs>"
                + "    </stage>"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "</cruise>";
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("Should not allow duplicate params");
        } catch (Exception e) {
            assertThat(e.getMessage(),
                    containsString("Param name 'same-name' is not unique for pipeline 'dev'."));
        }
    }

    @Test
    public void shouldNotAllowParamsToBeUsedInNames() throws Exception {
        String content = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifacts' />"
                + "<pipelines>\n"
                + "<pipeline name='dev'>\n"
                + "    <params>"
                + "        <param name='command'>ls</param>"
                + "    </params>"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "    <stage name='stage#{command}ab'>"
                + "      <jobs>"
                + "        <job name='job1'>"
                + "            <tasks>"
                + "                <exec command='/bin/#{command}##{b}' args='#{dir}'/>"
                + "            </tasks>"
                + "        </job>"
                + "      </jobs>"
                + "    </stage>"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "</cruise>";
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("Should not allow params in stage name");
        } catch (Exception e) {
            assertThat(e.getMessage(),
                    containsString("\"stage#{command}ab\" should conform to the pattern - [a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*"));
        }
    }

    @Test
    public void shouldNotAllowDuplicateEnvironmentVariablesInAPipeline() throws Exception {
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='pipeline1'>"
                        + "      <environmentvariables>"
                        + "         <variable name='PIPELINE_VARIABLE'><value>pipeline variable</value></variable>"
                        + "         <variable name='PIPELINE_VARIABLE'><value>pipeline variable</value></variable>"
                        + "      </environmentvariables>"
                        + "    <materials>"
                        + "      <svn url ='svnurl'/>"
                        + "    </materials>"
                        + "  <stage name='mingle'>"
                        + "    <jobs>"
                        + "      <job name='do-something'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", CONFIG_SCHEMA_VERSION);
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("Should not allow duplicate variable names");
        } catch (Exception e) {
            assertThat(e.getMessage(),
                    containsString("Variable name 'PIPELINE_VARIABLE' is not unique for pipeline 'pipeline1'."));
        }
    }

    @Test
    public void shouldNotAllowDuplicateEnvironmentVariablesInAStage() throws Exception {
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='pipeline1'>"
                        + "    <materials>"
                        + "      <svn url ='svnurl'/>"
                        + "    </materials>"
                        + "  <stage name='mingle'>"
                        + "      <environmentvariables>"
                        + "         <variable name='STAGE_VARIABLE'><value>stage variable</value></variable>"
                        + "         <variable name='STAGE_VARIABLE'><value>stage variable</value></variable>"
                        + "      </environmentvariables>"
                        + "    <jobs>"
                        + "      <job name='do-something'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", CONFIG_SCHEMA_VERSION);
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("Should not allow duplicate variable names");
        } catch (Exception e) {
            assertThat(e.getMessage(),
                    containsString("Variable name 'STAGE_VARIABLE' is not unique for stage 'mingle'."));
        }
    }

    @Test
    public void shouldNotAllowDuplicateEnvironmentVariablesInAnEnvironment() throws Exception {
        String content = ConfigFileFixture.configWithEnvironmentsAndAgents(
                "<environments>"
                        + "  <environment name='uat'>"
                        + "     <environmentvariables> "
                        + "         <variable name='FOO'><value>foo</value></variable>"
                        + "         <variable name='FOO'><value>foo</value></variable>"
                        + "     </environmentvariables> "
                        + "  </environment>"
                        + "</environments>",

                "<agents>"
                        + "  <agent uuid='1' hostname='test1.com' ipaddress='192.168.0.1' isDisabled='true' />"
                        + "</agents>");
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("Should not allow duplicate variable names");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Variable name 'FOO' is not unique for environment 'uat'."));
        }
    }

    @Test
    public void shouldAllowParamsInEnvironmentVariablesInAPipeline() throws Exception {
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='pipeline1'>"
                        + "    <params>"
                        + "         <param name=\"some_param\">param_name</param>"
                        + "    </params>"
                        + "      <environmentvariables>"
                        + "         <variable name='#{some_param}'><value>stage variable</value></variable>"
                        + "      </environmentvariables>"
                        + "    <materials>"
                        + "      <svn url ='svnurl'/>"
                        + "    </materials>"
                        + "  <stage name='mingle'>"
                        + "    <jobs>"
                        + "      <job name='do-something'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", CONFIG_SCHEMA_VERSION);
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;

        assertThat(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).getVariables(), hasItem(new EnvironmentVariableConfig("param_name", "stage variable")));
    }

    @Test
    public void shouldSupportRunOnAllAgents() throws Exception {
        CruiseConfig cruiseConfig = loadJobWithRunOnAllAgents("true");
        JobConfig job = cruiseConfig.findJob("pipeline1", "mingle", "do-something");
        JobConfig jobConfig = new JobConfig("do-something");
        jobConfig.setRunOnAllAgents(true);
        assertThat(job, is(jobConfig));
    }

    @Test
    public void shouldSupportRunMultipleInstance() throws Exception {
        CruiseConfig cruiseConfig = loadJobWithRunMultipleInstance("10");
        JobConfig job = cruiseConfig.findJob("pipeline1", "mingle", "do-something");
        JobConfig jobConfig = new JobConfig("do-something");
        jobConfig.setRunInstanceCount(10);
        assertThat(job, is(jobConfig));
    }

    @Test
    public void shouldUnderstandEncryptedPasswordAttributeForSvnMaterial() throws Exception {
        String password = "abc";
        String encryptedPassword = new GoCipher().encrypt(password);
        String content = ConfigFileFixture.configWithPipeline(format(
                "<pipeline name='pipeline1'>"
                        + "    <materials>"
                        + "      <svn url='svnurl' username='admin' encryptedPassword='%s'/>"
                        + "    </materials>"
                        + "  <stage name='mingle'>"
                        + "    <jobs>"
                        + "      <job name='do-something'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", encryptedPassword), CONFIG_SCHEMA_VERSION);
        GoConfigHolder configHolder = ConfigMigrator.loadWithMigration(content);
        CruiseConfig cruiseConfig = configHolder.config;
        SvnMaterialConfig svnMaterialConfig = (SvnMaterialConfig) cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).materialConfigs().get(0);
        assertThat(svnMaterialConfig.getEncryptedPassword(), is(encryptedPassword));
        assertThat(svnMaterialConfig.getPassword(), is(password));

        CruiseConfig configForEdit = configHolder.configForEdit;
        svnMaterialConfig = (SvnMaterialConfig) configForEdit.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).materialConfigs().get(0);
        assertThat(svnMaterialConfig.getEncryptedPassword(), is(encryptedPassword));
        assertThat(svnMaterialConfig.getPassword(), is("abc"));
        assertThat(ReflectionUtil.getField(svnMaterialConfig, "password"), is(nullValue()));
    }

    @Test
    public void shouldSupportEmptyPipelineGroup() throws Exception {
        PipelineConfigs group = new BasicPipelineConfigs("defaultGroup", new Authorization());
        CruiseConfig config = new BasicCruiseConfig(group);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        new MagicalGoConfigXmlWriter(configCache, ConfigElementImplementationRegistryMother.withNoPlugins()).write(config, stream, true);
        GoConfigHolder configHolder = new MagicalGoConfigXmlLoader(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins())
                .loadConfigHolder(stream.toString());
        assertThat(configHolder.config.findGroup("defaultGroup"), is(group));
    }

    private CruiseConfig loadJobWithRunOnAllAgents(String value) throws Exception {
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='pipeline1'>"
                        + "    <materials>"
                        + "      <svn url ='svnurl'/>"
                        + "    </materials>"
                        + "  <stage name='mingle'>"
                        + "    <jobs>"
                        + "      <job name='do-something' runOnAllAgents='" + value + "'/>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", CONFIG_SCHEMA_VERSION);
        return ConfigMigrator.loadWithMigration(content).config;
    }

    private CruiseConfig loadJobWithRunMultipleInstance(String value) throws Exception {
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='pipeline1'>"
                        + "    <materials>"
                        + "      <svn url ='svnurl'/>"
                        + "    </materials>"
                        + "  <stage name='mingle'>"
                        + "    <jobs>"
                        + "      <job name='do-something' runInstanceCount='" + value + "'/>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", CONFIG_SCHEMA_VERSION);
        return ConfigMigrator.loadWithMigration(content).config;
    }

    private void assertValidMaterials(String materials) throws Exception {
        createConfig(materials);
    }

    private CruiseConfig createConfig(String materials) throws Exception {
        String pipelineXmlPartial =
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                        + "<cruise "
                        + "        xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                        + "        xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" "
                        + "        schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                        + "  <server artifactsdir=\"logs\">\n"
                        + "  </server>\n"
                        + "<pipelines>\n"
                        + "  <pipeline name=\"pipeline-name\">\n"
                        + materials
                        + "    <stage name=\"mingle\">\n"
                        + "      <jobs>\n"
                        + "        <job name=\"functional\">\n"
                        + "          <artifacts>\n"
                        + "            <artifact src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
                        + "          </artifacts>\n"
                        + "        </job>\n"
                        + "      </jobs>\n"
                        + "    </stage>\n"
                        + "  </pipeline>\n"
                        + "</pipelines>"
                        + "</cruise>\n";

        return ConfigMigrator.loadWithMigration(toInputStream(pipelineXmlPartial)).config;

    }

    @Test
    public void shouldAllowResourcesWithParamsForJobs() throws Exception {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.addTemplate(new PipelineTemplateConfig(new CaseInsensitiveString("template"), stageWithJobResource("#{PLATFORM}")));

        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), new MaterialConfigs());
        pipelineConfig.setTemplateName(new CaseInsensitiveString("template"));
        pipelineConfig.addParam(new ParamConfig("PLATFORM", "windows"));
        cruiseConfig.addPipeline("group", pipelineConfig);

        List<ConfigErrors> errorses = MagicalGoConfigXmlLoader.validate(cruiseConfig);
        assertThat(errorses.isEmpty(), is(true));
    }

    //BUG: #5209
    @Test
    public void shouldAllowRoleWithParamsForStageInTemplate() throws Exception {
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.server().security().addRole(new Role(new CaseInsensitiveString("role")));

        cruiseConfig.addTemplate(new PipelineTemplateConfig(new CaseInsensitiveString("template"), stageWithAuth("#{ROLE}")));

        PipelineConfig pipelineConfig = new PipelineConfig(new CaseInsensitiveString("pipeline"), new MaterialConfigs());
        pipelineConfig.setTemplateName(new CaseInsensitiveString("template"));
        pipelineConfig.addParam(new ParamConfig("ROLE", "role"));

        cruiseConfig.addPipeline("group", pipelineConfig);

        List<ConfigErrors> errorses = MagicalGoConfigXmlLoader.validate(cruiseConfig);
        assertThat(errorses.isEmpty(), is(true));
    }

    private StageConfig stageWithAuth(String role) {
        StageConfig stage = stageWithJobResource("foo");
        stage.getApproval().getAuthConfig().add(new AdminRole(new CaseInsensitiveString(role)));
        return stage;
    }

    @Test
    public void shouldAllowOnlyOneOfTrackingToolOrMingleConfigInSourceXml() {
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='pipeline1'>"
                        + "<trackingtool link=\"https://some-tracking-tool/projects/go/cards/${ID}\" regex=\"##(\\d+)\" />"
                        + "      <mingle baseUrl=\"https://some-tracking-tool/\" projectIdentifier=\"go\">"
                        + "        <mqlGroupingConditions>status &gt; 'In Dev'</mqlGroupingConditions>"
                        + "      </mingle>"
                        + "    <materials>"
                        + "      <svn url='svnurl'/>"
                        + "    </materials>"
                        + "  <stage name='mingle'>"
                        + "    <jobs>"
                        + "      <job name='do-something'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", CONFIG_SCHEMA_VERSION);
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("Should not allow mingle config and tracking tool together");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Invalid content was found starting with element 'mingle'."));
        }
    }

    @Test
    public void shouldAllowTFSMaterial() {
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='some_pipeline'>"
                        + "    <materials>"
                        + "      <tfs url='tfsurl' username='foo' password='bar' projectPath='project-path' />"
                        + "    </materials>"
                        + "  <stage name='some_stage'>"
                        + "    <jobs>"
                        + "      <job name='some_job'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", CONFIG_SCHEMA_VERSION);
        GoConfigHolder goConfigHolder = ConfigMigrator.loadWithMigration(content);
        MaterialConfigs materialConfigs = goConfigHolder.config.pipelineConfigByName(new CaseInsensitiveString("some_pipeline")).materialConfigs();
        assertThat(materialConfigs.size(), is(1));
        TfsMaterialConfig materialConfig = (TfsMaterialConfig) materialConfigs.get(0);
        assertThat(materialConfig, is(new TfsMaterialConfig(new GoCipher(), UrlArgument.create("tfsurl"), "foo", "", "bar", "project-path")));
    }

    @Test
    public void shouldAllowAnEnvironmentVariableToBeMarkedAsSecure_WithValueInItsOwnTag() throws Exception {
        String cipherText = new GoCipher().encrypt("plainText");
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='some_pipeline'>"
                        + "<environmentvariables>\n"
                        + "        <variable name=\"var_name\" secure=\"true\"><encryptedValue>" + cipherText + "</encryptedValue></variable>\n"
                        + "      </environmentvariables>"
                        + "    <materials>"
                        + "      <svn url='svnurl'/>"
                        + "    </materials>"
                        + "  <stage name='some_stage'>"
                        + "    <jobs>"
                        + "      <job name='some_job'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", CONFIG_SCHEMA_VERSION);
        CruiseConfig config = ConfigMigrator.loadWithMigration(content).config;
        PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString("some_pipeline"));
        EnvironmentVariablesConfig variables = pipelineConfig.getVariables();
        assertThat(variables.size(), is(1));
        EnvironmentVariableConfig environmentVariableConfig = variables.get(0);
        assertThat(environmentVariableConfig.getEncryptedValue(), is(cipherText));
        assertThat(environmentVariableConfig.isSecure(), is(true));
    }

    @Test
    public void shouldMigrateEmptyEnvironmentVariable() throws Exception {
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='some_pipeline'>"
                        + "<environmentvariables>\n"
                        + "        <variable name=\"var_name\" />\n"
                        + "      </environmentvariables>"
                        + "    <materials>"
                        + "      <svn url='svnurl'/>"
                        + "    </materials>"
                        + "  <stage name='some_stage'>"
                        + "    <jobs>"
                        + "      <job name='some_job'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", 48);
        CruiseConfig config = ConfigMigrator.loadWithMigration(content).config;
        PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString("some_pipeline"));
        EnvironmentVariablesConfig variables = pipelineConfig.getVariables();
        assertThat(variables.size(), is(1));
        EnvironmentVariableConfig environmentVariableConfig = variables.get(0);
        assertThat(environmentVariableConfig.getName(), is("var_name"));
        assertThat(environmentVariableConfig.getValue().isEmpty(), is(true));
    }

    @Test
    public void should_NOT_AllowTFSMaterial_toHaveWorkSpaceNameLongerThan_30_characters() {
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='some_pipeline'>"
                        + "    <materials>"
                        + "      <tfs url='tfsurl' username='foo' password='bar' workspace='0123456789012345678901234567890' projectPath='project-path' />"
                        + "    </materials>"
                        + "  <stage name='some_stage'>"
                        + "    <jobs>"
                        + "      <job name='some_job'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", 46);
        Exception ex = null;
        try {
            ConfigMigrator.loadWithMigration(content);
            fail("Should have failed!");
        } catch (Exception e) {
            ex = e;
        }
        assertThat(ex.getMessage(), containsString("Value '0123456789012345678901234567890' with length = '31' is not facet-valid with respect to maxLength '30' for type 'tfsWorkspaceType'"));
    }

    @Test
    public void shouldAllowAnEnvironmentVariableToBeMarkedAsSecure_WithEncryptedValueInItsOwnTag() throws Exception {
        String value = "abc";
        String encryptedValue = new GoCipher().encrypt(value);
        String content = ConfigFileFixture.configWithPipeline(format(
                "<pipeline name='some_pipeline'>"
                        + "<environmentvariables>\n"
                        + "        <variable name=\"var_name\" secure=\"true\"><encryptedValue>%s</encryptedValue></variable>\n"
                        + "      </environmentvariables>"
                        + "    <materials>"
                        + "      <svn url='svnurl'/>"
                        + "    </materials>"
                        + "  <stage name='some_stage'>"
                        + "    <jobs>"
                        + "      <job name='some_job'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", encryptedValue), CONFIG_SCHEMA_VERSION);
        CruiseConfig config = ConfigMigrator.loadWithMigration(content).config;
        PipelineConfig pipelineConfig = config.pipelineConfigByName(new CaseInsensitiveString("some_pipeline"));
        EnvironmentVariablesConfig variables = pipelineConfig.getVariables();
        assertThat(variables.size(), is(1));
        EnvironmentVariableConfig environmentVariableConfig = variables.get(0);
        assertThat(environmentVariableConfig.getEncryptedValue(), is(encryptedValue));
        assertThat(environmentVariableConfig.isSecure(), is(true));
    }

    @Test
    public void shouldNotAllowWorkspaceOwnerAndWorkspaceAsAttributesOnTfsMaterial() {
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='some_pipeline'>"
                        + "    <materials>"
                        + "      <tfs url='tfsurl' username='foo' password='bar' projectPath='project-path' />"
                        + "    </materials>"
                        + "  <stage name='some_stage'>"
                        + "    <jobs>"
                        + "      <job name='some_job'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", CONFIG_SCHEMA_VERSION);
        try {
            ConfigMigrator.loadWithMigration(content);
        } catch (Exception e) {
            fail("Valid TFS tag for migration 51 and above");
        }
    }

    @Test
    public void shouldMigrateConfigToSplitUsernameAndDomainAsAttributeOnTfsMaterial() {
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='some_pipeline'>"
                        + "    <materials>"
                        + "      <tfs url='tfsurl' username='domain\\username' password='bar' projectPath='project-path' />"
                        + "    </materials>"
                        + "  <stage name='some_stage'>"
                        + "    <jobs>"
                        + "      <job name='some_job'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", 52);
        try {
            CruiseConfig config = ConfigMigrator.loadWithMigration(content).config;
            PipelineConfig pipeline = config.pipelineConfigByName(new CaseInsensitiveString("some_pipeline"));
            TfsMaterialConfig material = (TfsMaterialConfig) pipeline.materialConfigs().get(0);
            assertThat(material.getUsername(), is("username"));
            assertThat(material.getDomain(), is("domain"));
        } catch (Exception e) {
            fail("Valid TFS tag for migration 51 and above");
        }
    }

    @Test
    public void shouldAllowUserToSpecify_PathFromAncestor_forFetchArtifactFromAncestor() {
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='uppest_pipeline'>"
                        + "    <materials>"
                        + "      <git url=\"foo\" />"
                        + "    </materials>"
                        + "  <stage name='uppest_stage'>"
                        + "    <jobs>"
                        + "      <job name='uppest_job'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>"
                        + "<pipeline name='up_pipeline'>"
                        + "    <materials>"
                        + "      <pipeline pipelineName=\"uppest_pipeline\" stageName=\"uppest_stage\"/>"
                        + "    </materials>"
                        + "  <stage name='up_stage'>"
                        + "    <jobs>"
                        + "      <job name='up_job'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>"
                        + "<pipeline name='down_pipeline'>"
                        + "    <materials>"
                        + "      <pipeline pipelineName=\"up_pipeline\" stageName=\"up_stage\"/>"
                        + "    </materials>"
                        + "  <stage name='down_stage'>"
                        + "    <jobs>"
                        + "      <job name='down_job'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>"
                        + "<pipeline name='downest_pipeline'>"
                        + "    <materials>"
                        + "      <pipeline pipelineName=\"down_pipeline\" stageName=\"down_stage\"/>"
                        + "    </materials>"
                        + "  <stage name='downest_stage'>"
                        + "    <jobs>"
                        + "      <job name='downest_job'>"
                        + "        <tasks>"
                        + "          <fetchartifact pipeline=\"uppest_pipeline/up_pipeline/down_pipeline\" stage=\"uppest_stage\" job=\"uppest_job\" srcfile=\"src\" dest=\"dest\"/>"
                        + "        </tasks>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", CONFIG_SCHEMA_VERSION);

        GoConfigHolder holder = ConfigMigrator.loadWithMigration(content);
        assertThat(holder.config.pipelineConfigByName(new CaseInsensitiveString("downest_pipeline")).getFetchTasks().get(0),
                is(new FetchTask(new CaseInsensitiveString("uppest_pipeline/up_pipeline/down_pipeline"), new CaseInsensitiveString("uppest_stage"), new CaseInsensitiveString("uppest_job"), "src",
                        "dest")));
    }

    @Test
    public void should_NOT_allowUserToSpecifyFetchStage_afterUpstreamStage() {
        String content = ConfigFileFixture.configWithPipeline(
                "<pipeline name='up_pipeline'>"
                        + "  <materials>"
                        + "    <git url=\"/tmp/git\"/>"
                        + "  </materials>"
                        + "  <stage name='up_stage'>"
                        + "    <jobs>"
                        + "      <job name='up_job'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "  <stage name='up_stage_2'>"
                        + "    <jobs>"
                        + "      <job name='up_job'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "  <stage name='up_stage_3'>"
                        + "    <jobs>"
                        + "      <job name='up_job'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>"
                        + "<pipeline name='down_pipeline'>"
                        + "    <materials>"
                        + "      <pipeline pipelineName=\"up_pipeline\" stageName=\"up_stage\"/>"
                        + "    </materials>"
                        + "  <stage name='down_stage'>"
                        + "    <jobs>"
                        + "      <job name='down_job'>"
                        + "        <tasks>"
                        + "          <fetchartifact pipeline=\"up_pipeline\" stage=\"up_stage_2\" job=\"up_job\" srcfile=\"src\" dest=\"dest\"/>"
                        + "        </tasks>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", CONFIG_SCHEMA_VERSION);

        try {
            ConfigMigrator.loadWithMigration(content);
            fail("should not have permitted fetch from parent pipeline's stage after the one downstream depends on");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(
                    "\"down_pipeline :: down_stage :: down_job\" tries to fetch artifact from stage \"up_pipeline :: up_stage_2\" which does not complete before \"down_pipeline\" pipeline's dependencies."));
        }
    }

    @Test
    public void shouldAddDefaultCommndRepositoryLocationIfNoValueIsGiven() {
        String content = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server artifactsdir='artifactsDir'>"
                + "</server>"
                + "<pipelines>"
                + "<pipeline name='some_pipeline'>"
                + "    <materials>"
                + "      <hg url='hgurl' />"
                + "    </materials>"
                + "  <stage name='some_stage'>"
                + "    <jobs>"
                + "      <job name='some_job'>"
                + "      </job>"
                + "    </jobs>"
                + "  </stage>"
                + "</pipeline>"
                + "</pipelines>"
                + "</cruise>";
        try {
            GoConfigHolder goConfigHolder = ConfigMigrator.loadWithMigration(content);
            assertThat(goConfigHolder.config.server().getCommandRepositoryLocation(), is("default"));
        } catch (Exception e) {
            fail("Should not come here");
        }
    }

    @Test
    public void shouldDeserializeGroupXml() throws Exception {
        String partialXml = "<pipelines group=\"group_name\">\n"
                + "  <pipeline name=\"new_name\">\n"
                + "    <materials>\n"
                + "      <svn url=\"file:///tmp/foo\" />\n"
                + "    </materials>\n"
                + "    <stage name=\"stage_name\">\n"
                + "      <jobs>\n"
                + "        <job name=\"job_name\" />\n"
                + "      </jobs>\n"
                + "    </stage>\n"
                + "  </pipeline>\n"
                + "</pipelines>";
        PipelineConfigs pipelineConfigs = xmlLoader.fromXmlPartial(partialXml, BasicPipelineConfigs.class);
        PipelineConfig pipeline = pipelineConfigs.findBy(new CaseInsensitiveString("new_name"));
        assertThat(pipeline, is(notNullValue()));
        assertThat(pipeline.materialConfigs().size(), is(1));
        MaterialConfig material = pipeline.materialConfigs().get(0);
        assertThat(material, is(Matchers.instanceOf(SvnMaterialConfig.class)));
        assertThat(material.getUriForDisplay(), is("file:///tmp/foo"));
        assertThat(pipeline.size(), is(1));
        assertThat(pipeline.get(0).getJobs().size(), is(1));
    }

    @Test
    public void shouldRegisterAllGoConfigValidators() {
        List<String> list = (List<String>) collect(MagicalGoConfigXmlLoader.VALIDATORS, new Transformer() {
            @Override
            public Object transform(Object o) {
                return o.getClass().getCanonicalName();
            }
        });

        assertThat(list, hasItem(ArtifactDirValidator.class.getCanonicalName()));
        assertThat(list, hasItem(EnvironmentAgentValidator.class.getCanonicalName()));
        assertThat(list, hasItem(EnvironmentPipelineValidator.class.getCanonicalName()));
        assertThat(list, hasItem(ServerIdImmutabilityValidator.class.getCanonicalName()));
        assertThat(list, hasItem(CommandRepositoryLocationValidator.class.getCanonicalName()));
    }

    @Test
    public void shouldLoadAfterMigration62() {
        final String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "    <server artifactsdir=\"artifacts\">\n"
                + "      <security>"
                + "        <ldap uri='some_url' managerDn='some_manager_dn' managerPassword='foo' searchFilter='(sAMAccountName={0})'>"
                + "             <bases>"
                + "                 <base value='ou=Enterprise,ou=Principal,dc=corporate,dc=thoughtworks,dc=com'/>"
                + "             </bases>"
                + "        </ldap>"
                + "      </security>"
                + "    </server>"
                + " </cruise>";
        GoConfigHolder goConfigHolder = ConfigMigrator.loadWithMigration(content);
        BaseConfig firstBase = goConfigHolder.config.server().security().ldapConfig().getBasesConfig().first();
        assertThat(firstBase.getValue(), is("ou=Enterprise,ou=Principal,dc=corporate,dc=thoughtworks,dc=com"));
    }

    @Test
    public void shouldResolvePackageReferenceElementForAMaterialInConfig() throws Exception {
        String xml = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<repositories>\n"
                + "    <repository id='repo-id' name='name'>\n"
                + "		<pluginConfiguration id='plugin-id' version='1.0'/>\n"
                + "      <configuration>\n"
                + "        <property>\n"
                + "          <key>url</key>\n"
                + "          <value>http://go</value>\n"
                + "        </property>\n"
                + "      </configuration>\n"
                + "      <packages>\n"
                + "        <package id='package-id' name='name'>\n"
                + "          <configuration>\n"
                + "            <property>\n"
                + "              <key>name</key>\n"
                + "              <value>go-agent</value>\n"
                + "            </property>\n"
                + "          </configuration>\n"
                + "        </package>\n"
                + "      </packages>\n"
                + "    </repository>\n"
                + "  </repositories>"
                + "<pipelines group=\"group_name\">\n"
                + "  <pipeline name=\"new_name\">\n"
                + "    <materials>\n"
                + "      <package ref='package-id' />\n"
                + "    </materials>\n"
                + "    <stage name=\"stage_name\">\n"
                + "      <jobs>\n"
                + "        <job name=\"job_name\" />\n"
                + "      </jobs>\n"
                + "    </stage>\n"
                + "  </pipeline>\n"
                + "</pipelines></cruise>";

        GoConfigHolder goConfigHolder = xmlLoader.loadConfigHolder(xml);
        PackageDefinition packageDefinition = goConfigHolder.config.getPackageRepositories().first().getPackages().first();
        PipelineConfig pipelineConfig = goConfigHolder.config.pipelineConfigByName(new CaseInsensitiveString("new_name"));
        PackageMaterialConfig packageMaterialConfig = (PackageMaterialConfig) pipelineConfig.materialConfigs().get(0);
        assertThat(packageMaterialConfig.getPackageDefinition(), is(packageDefinition));
    }

    @Test
    public void shouldBeAbleToResolveSecureConfigPropertiesForPackages() throws Exception {
        String encryptedValue = new GoCipher().encrypt("secure-two");
        String xml = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<repositories>\n"
                + "    <repository id='repo-id' name='name'>\n"
                + "		<pluginConfiguration id='plugin-id' version='1.0'/>\n"
                + "      <configuration>\n"
                + "        <property>\n"
                + "          <key>plain</key>\n"
                + "          <value>value</value>\n"
                + "        </property>\n"
                + "        <property>\n"
                + "          <key>secure-one</key>\n"
                + "          <value>secure-value</value>\n"
                + "        </property>\n"
                + "        <property>\n"
                + "          <key>secure-two</key>\n"
                + "          <encryptedValue>" + encryptedValue + "</encryptedValue>\n"
                + "        </property>\n"
                + "      </configuration>\n"
                + "      <packages>\n"
                + "        <package id='package-id' name='name'>\n"
                + "          <configuration>\n"
                + "              <property>\n"
                + "                <key>plain</key>\n"
                + "                <value>value</value>\n"
                + "              </property>\n"
                + "              <property>\n"
                + "                <key>secure-one</key>\n"
                + "                <value>secure-value</value>\n"
                + "              </property>\n"
                + "              <property>\n"
                + "                <key>secure-two</key>\n"
                + "                <encryptedValue>" + encryptedValue + "</encryptedValue>\n"
                + "              </property>\n"
                + "          </configuration>\n"
                + "        </package>\n"
                + "      </packages>\n"
                + "    </repository>\n"
                + "  </repositories>"
                + "<pipelines group=\"group_name\">\n"
                + "  <pipeline name=\"new_name\">\n"
                + "    <materials>\n"
                + "      <package ref='package-id' />\n"
                + "    </materials>\n"
                + "    <stage name=\"stage_name\">\n"
                + "      <jobs>\n"
                + "        <job name=\"job_name\" />\n"
                + "      </jobs>\n"
                + "    </stage>\n"
                + "  </pipeline>\n"
                + "</pipelines></cruise>";

        //meta data of package
        PackageConfigurations packageConfigurations = new PackageConfigurations();
        packageConfigurations.addConfiguration(new PackageConfiguration("plain"));
        packageConfigurations.addConfiguration(new PackageConfiguration("secure-one").with(PackageConfiguration.SECURE, true));
        packageConfigurations.addConfiguration(new PackageConfiguration("secure-two").with(PackageConfiguration.SECURE, true));
        PackageMetadataStore.getInstance().addMetadataFor("plugin-id", packageConfigurations);
        RepositoryMetadataStore.getInstance().addMetadataFor("plugin-id", packageConfigurations);

        GoConfigHolder goConfigHolder = xmlLoader.loadConfigHolder(xml);
        PackageDefinition packageDefinition = goConfigHolder.config.getPackageRepositories().first().getPackages().first();
        PipelineConfig pipelineConfig = goConfigHolder.config.pipelineConfigByName(new CaseInsensitiveString("new_name"));
        PackageMaterialConfig packageMaterialConfig = (PackageMaterialConfig) pipelineConfig.materialConfigs().get(0);
        assertThat(packageMaterialConfig.getPackageDefinition(), is(packageDefinition));
        Configuration repoConfig = packageMaterialConfig.getPackageDefinition().getRepository().getConfiguration();
        assertThat(repoConfig.get(0).getConfigurationValue().getValue(), is("value"));
        assertThat(repoConfig.get(1).getEncryptedValue(), is(new GoCipher().encrypt("secure-value")));
        assertThat(repoConfig.get(2).getEncryptedValue(), is(encryptedValue));
        Configuration packageConfig = packageMaterialConfig.getPackageDefinition().getConfiguration();
        assertThat(packageConfig.get(0).getConfigurationValue().getValue(), is("value"));
        assertThat(packageConfig.get(1).getEncryptedValue(), is(new GoCipher().encrypt("secure-value")));
        assertThat(packageConfig.get(2).getEncryptedValue(), is(encryptedValue));
    }

    @Test
    public void shouldResolvePackageRepoReferenceElementForAPackageInConfig() throws Exception {
        String xml = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<repositories>\n"
                + "    <repository id='repo-id' name='name'>\n"
                + "		<pluginConfiguration id='plugin-id' version='1.0'/>\n"
                + "      <configuration>\n"
                + "        <property>\n"
                + "          <key>url</key>\n"
                + "          <value>http://go</value>\n"
                + "        </property>\n"
                + "      </configuration>\n"
                + "      <packages>\n"
                + "        <package id='package-id' name='name'>\n"
                + "          <configuration>\n"
                + "            <property>\n"
                + "              <key>name</key>\n"
                + "              <value>go-agent</value>\n"
                + "            </property>\n"
                + "          </configuration>\n"
                + "        </package>\n"
                + "      </packages>\n"
                + "    </repository>\n"
                + "  </repositories></cruise>";

        GoConfigHolder goConfigHolder = xmlLoader.loadConfigHolder(xml);
        PackageRepository packageRepository = goConfigHolder.config.getPackageRepositories().first();
        PackageDefinition packageDefinition = packageRepository.getPackages().first();
        assertThat(packageDefinition.getRepository(), is(packageRepository));
    }

    @Test
    public void shouldFailValidationIfPackageDefinitionWithDuplicateFingerprintExists() throws Exception {
        com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration packageConfiguration = new com.thoughtworks.go.plugin.api.material.packagerepository.PackageConfiguration();
        packageConfiguration.add(new PackageMaterialProperty("PKG-KEY1"));
        RepositoryConfiguration repositoryConfiguration = new RepositoryConfiguration();
        repositoryConfiguration.add(new PackageMaterialProperty("REPO-KEY1"));
        repositoryConfiguration.add(new PackageMaterialProperty("REPO-KEY2").with(REQUIRED, false).with(PART_OF_IDENTITY, false));
        repositoryConfiguration.add(new PackageMaterialProperty("REPO-KEY3").with(REQUIRED, false).with(PART_OF_IDENTITY, false).with(SECURE, true));
        PackageMetadataStore.getInstance().addMetadataFor("plugin-1", new PackageConfigurations(packageConfiguration));
        RepositoryMetadataStore.getInstance().addMetadataFor("plugin-1", new PackageConfigurations(repositoryConfiguration));

        String xml = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<repositories>\n"
                + "    <repository id='repo-id-1' name='name-1'>\n"
                + "		<pluginConfiguration id='plugin-1' version='1.0'/>\n"
                + "      <configuration>\n"
                + "        <property>\n"
                + "          <key>REPO-KEY1</key>\n"
                + "          <value>repo-key1</value>\n"
                + "        </property>\n"
                + "        <property>\n"
                + "          <key>REPO-KEY2</key>\n"
                + "          <value>repo-key2</value>\n"
                + "        </property>\n"
                + "        <property>\n"
                + "          <key>REPO-KEY3</key>\n"
                + "          <value>repo-key3</value>\n"
                + "        </property>\n"
                + "      </configuration>\n"
                + "      <packages>\n"
                + "        <package id='package-id-1' name='name-1'>\n"
                + "          <configuration>\n"
                + "            <property>\n"
                + "              <key>PKG-KEY1</key>\n"
                + "              <value>pkg-key1</value>\n"
                + "            </property>\n"
                + "          </configuration>\n"
                + "        </package>\n"
                + "      </packages>\n"
                + "    </repository>\n"
                + "    <repository id='repo-id-2' name='name-2'>\n"
                + "		<pluginConfiguration id='plugin-1' version='1.0'/>\n"
                + "      <configuration>\n"
                + "        <property>\n"
                + "          <key>REPO-KEY1</key>\n"
                + "          <value>repo-key1</value>\n"
                + "        </property>\n"
                + "        <property>\n"
                + "          <key>REPO-KEY2</key>\n"
                + "          <value>another-repo-key2</value>\n"
                + "        </property>\n"
                + "        <property>\n"
                + "          <key>REPO-KEY3</key>\n"
                + "          <value>another-repo-key3</value>\n"
                + "        </property>\n"
                + "      </configuration>\n"
                + "      <packages>\n"
                + "        <package id='package-id-2' name='name-2'>\n"
                + "          <configuration>\n"
                + "            <property>\n"
                + "              <key>PKG-KEY1</key>\n"
                + "              <value>pkg-key1</value>\n"
                + "            </property>\n"
                + "          </configuration>\n"
                + "        </package>\n"
                + "      </packages>\n"
                + "    </repository>\n"
                + "  </repositories>"
                + "</cruise>";

        try {
            xmlLoader.loadConfigHolder(xml);
            fail("should have thrown duplicate fingerprint exception");
        } catch (GoConfigInvalidException e) {
            assertThat(e.getMessage(), is("Cannot save package or repo, found duplicate packages. [Repo Name: 'name-1', Package Name: 'name-1'], [Repo Name: 'name-2', Package Name: 'name-2']"));
        }
    }

    final static String REPO = " <repository id='repo-id' name='name1'><pluginConfiguration id='id' version='1.0'/><configuration><property><key>url</key><value>http://go</value></property></configuration>%s</repository>";
    final static String REPO_WITH_NAME = " <repository id='%s' name='%s'><pluginConfiguration id='id' version='1.0'/><configuration><property><key>url</key><value>http://go</value></property></configuration>%s</repository>";
    final static String REPO_WITH_MISSING_ID = " <repository name='name1'><pluginConfiguration id='id' version='1.0'/><configuration><property><key>url</key><value>http://go</value></property></configuration><packages>%s</packages></repository>";
    final static String REPO_WITH_INVALID_ID = " <repository id='id with space' name='name1'><pluginConfiguration id='id' version='1.0'/><configuration><property><key>url</key><value>http://go</value></property></configuration>%s</repository>";
    final static String REPO_WITH_EMPTY_ID = " <repository id='' name='name1'><pluginConfiguration id='id' version='1.0'/><configuration><property><key>url</key><value>http://go</value></property></configuration>%s</repository>";
    final static String REPO_WITH_MISSING_NAME = " <repository id='id' ><pluginConfiguration id='id' version='1.0'/><configuration><property><key>url</key><value>http://go</value></property></configuration>%s</repository>";
    final static String REPO_WITH_INVALID_NAME = " <repository id='id' name='name with space'><pluginConfiguration id='id' version='1.0'/><configuration><property><key>url</key><value>http://go</value></property></configuration>%s</repository>";
    final static String REPO_WITH_EMPTY_NAME = " <repository id='id' name=''><pluginConfiguration id='id' version='1.0'/><configuration><property><key>url</key><value>http://go</value></property></configuration>%s</repository>";

    final static String PACKAGE = "<package id='package-id' name='name'><configuration><property><key>name</key><value>go-agent</value></property></configuration></package>";
    final static String PACKAGE_WITH_MISSING_ID = "<package name='name'><configuration><property><key>name</key><value>go-agent</value></property></configuration></package>";
    final static String PACKAGE_WITH_INVALID_ID = "<package id='id with space' name='name'><configuration><property><key>name</key><value>go-agent</value></property></configuration></package>";
    final static String PACKAGE_WITH_EMPTY_ID = "<package id='' name='name'><configuration><property><key>name</key><value>go-agent</value></property></configuration></package>";
    final static String PACKAGE_WITH_MISSING_NAME = "<package id='id'><configuration><property><key>name</key><value>go-agent</value></property></configuration></package>";
    final static String PACKAGE_WITH_INVALID_NAME = "<package id='id' name='name with space'><configuration><property><key>name</key><value>go-agent</value></property></configuration></package>";
    final static String PACKAGE_WITH_EMPTY_NAME = "<package id='id' name=''><configuration><property><key>name</key><value>go-agent</value></property></configuration></package>";

    private String withPackages(String repo, String packages) {
        return format(repo, packages);
    }

    @Test
    public void shouldThrowXsdValidationWhenPackageRepositoryIdsAreDuplicate() throws Exception {
        String xml = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'><repositories>\n" + withPackages(REPO, "") + withPackages(REPO, "") + " </repositories></cruise>";
        try {
            xmlLoader.loadConfigHolder(xml);
            fail("should have thrown XsdValidationException");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), is("Duplicate unique value [repo-id] declared for identity constraint \"uniqueRepositoryId\" of element \"repositories\"."));
        }
    }

    @Test
    public void shouldThrowXsdValidationWhenPackageRepositoryNamesAreDuplicate() throws Exception {
        String xml = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'><repositories>\n" + format(REPO_WITH_NAME, "1", "repo", "") + format(REPO_WITH_NAME, "2", "repo", "") + " </repositories></cruise>";
        try {
            xmlLoader.loadConfigHolder(xml);
            fail("should have thrown XsdValidationException");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), is("Duplicate unique value [repo] declared for identity constraint \"uniqueRepositoryName\" of element \"repositories\"."));
        }
    }

    @Test
    public void shouldThrowXsdValidationWhenPackageIdsAreDuplicate() throws Exception {
        String xml = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'><repositories>\n" + withPackages(REPO, format("<packages>%s%s</packages>",
                PACKAGE, PACKAGE)) + " </repositories></cruise>";
        try {
            xmlLoader.loadConfigHolder(xml);
            fail("should have thrown XsdValidationException");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), is("Duplicate unique value [package-id] declared for identity constraint \"uniquePackageId\" of element \"cruise\"."));
        }
    }

    @Test
    public void shouldThrowXsdValidationWhenPackageRepositoryIdIsEmpty() throws Exception {
        String xml = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'><repositories>\n" + withPackages(REPO_WITH_EMPTY_ID, "") + " </repositories></cruise>";
        try {
            xmlLoader.loadConfigHolder(xml);
            fail("should have thrown XsdValidationException");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), is("Repo id is invalid. \"\" should conform to the pattern - [a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*"));
        }
    }

    @Test
    public void shouldThrowXsdValidationWhenPackageRepositoryIdIsInvalid() throws Exception {
        String xml = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'><repositories>\n" + withPackages(REPO_WITH_INVALID_ID, "") + " </repositories></cruise>";
        try {
            xmlLoader.loadConfigHolder(xml);
            fail("should have thrown XsdValidationException");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), is("Repo id is invalid. \"id with space\" should conform to the pattern - [a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*"));
        }
    }

    @Test
    public void shouldThrowXsdValidationWhenPackageRepositoryNameIsMissing() throws Exception {
        String xml = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'><repositories>\n" + withPackages(REPO_WITH_MISSING_NAME, "") + " </repositories></cruise>";
        try {
            xmlLoader.loadConfigHolder(xml);
            fail("should have thrown XsdValidationException");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), is("\"Name\" is required for Repository"));
        }
    }

    @Test
    public void shouldThrowXsdValidationWhenPackageRepositoryNameIsEmpty() throws Exception {
        String xml = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'><repositories>\n" + withPackages(REPO_WITH_EMPTY_NAME, "") + " </repositories></cruise>";
        try {
            xmlLoader.loadConfigHolder(xml);
            fail("should have thrown XsdValidationException");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), is("Name is invalid. \"\" should conform to the pattern - [a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*"));
        }
    }

    @Test
    public void shouldThrowXsdValidationWhenPackageRepositoryNameIsInvalid() throws Exception {
        String xml = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'><repositories>\n" + withPackages(REPO_WITH_INVALID_NAME, "") + " </repositories></cruise>";
        try {
            xmlLoader.loadConfigHolder(xml);
            fail("should have thrown XsdValidationException");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), is("Name is invalid. \"name with space\" should conform to the pattern - [a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*"));
        }
    }

    @Test
    public void shouldGenerateRepoAndPkgIdWhenMissing() throws Exception {
        String xml = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'><repositories>\n" + withPackages(REPO_WITH_MISSING_ID,
                PACKAGE_WITH_MISSING_ID) + " </repositories></cruise>";
        GoConfigHolder configHolder = xmlLoader.loadConfigHolder(xml);
        assertThat(configHolder.config.getPackageRepositories().get(0).getId(), is(notNullValue()));
        assertThat(configHolder.config.getPackageRepositories().get(0).getPackages().get(0).getId(), is(notNullValue()));
    }

    @Test
    public void shouldThrowXsdValidationWhenPackageIdIsEmpty() throws Exception {
        String xml = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'><repositories>\n" + withPackages(REPO_WITH_EMPTY_ID, PACKAGE_WITH_EMPTY_ID) + " </repositories></cruise>";
        try {
            xmlLoader.loadConfigHolder(xml);
            fail("should have thrown XsdValidationException");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), is("Repo id is invalid. \"\" should conform to the pattern - [a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*"));
        }
    }

    @Test
    public void shouldThrowXsdValidationWhenPackageIdIsInvalid() throws Exception {
        String xml = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'><repositories>\n" + withPackages(REPO_WITH_INVALID_ID,
                PACKAGE_WITH_INVALID_ID) + " </repositories></cruise>";
        try {
            xmlLoader.loadConfigHolder(xml);
            fail("should have thrown XsdValidationException");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), is("Repo id is invalid. \"id with space\" should conform to the pattern - [a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*"));
        }
    }

    @Test
    public void shouldThrowXsdValidationWhenPackageNameIsMissing() throws Exception {
        String xml = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'><repositories>\n" + withPackages(REPO_WITH_MISSING_NAME,
                PACKAGE_WITH_MISSING_NAME) + " </repositories></cruise>";
        try {
            xmlLoader.loadConfigHolder(xml);
            fail("should have thrown XsdValidationException");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), is("\"Name\" is required for Repository"));
        }
    }

    @Test
    public void shouldThrowXsdValidationWhenPackageNameIsEmpty() throws Exception {
        String xml = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'><repositories>\n" + withPackages(REPO_WITH_EMPTY_NAME,
                PACKAGE_WITH_EMPTY_NAME) + " </repositories></cruise>";
        try {
            xmlLoader.loadConfigHolder(xml);
            fail("should have thrown XsdValidationException");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), is("Name is invalid. \"\" should conform to the pattern - [a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*"));
        }
    }

    @Test
    public void shouldThrowXsdValidationWhenPackageNameIsInvalid() throws Exception {
        String xml = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'><repositories>\n" + withPackages(REPO_WITH_INVALID_NAME,
                PACKAGE_WITH_INVALID_NAME) + " </repositories></cruise>";
        try {
            xmlLoader.loadConfigHolder(xml);
            fail("should have thrown XsdValidationException");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), is("Name is invalid. \"name with space\" should conform to the pattern - [a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*"));
        }
    }

    @Test
    public void shouldLoadAutoUpdateValueForPackageWhenLoadedFromConfigFile() throws Exception {
        String configTemplate = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>" +
                "<repositories>" +
                "	<repository id='2ef830d7-dd66-42d6-b393-64a84646e557' name='GoYumRepo'>" +
                "		<pluginConfiguration id='yum' version='1' />" +
                "       <configuration>" +
                "           <property>" +
                "               <key>REPO_URL</key>" +
                "               <value>http://fake-yum-repo/go/yum/no-arch</value>" +
                "               </property>" +
                "       </configuration>" +
                "	    <packages>" +
                "           <package id='88a3beca-cbe2-4c4d-9744-aa0cda3f371c' name='1' autoUpdate='%s'>" +
                "               <configuration>" +
                "                   <property>" +
                "                       <key>REPO_URL</key>" +
                "                       <value>http://fake-yum-repo/go/yum/no-arch</value>" +
                "                   </property>" +
                "               </configuration>" +
                "           </package>" +
                "	     </packages>" +
                "   </repository>" +
                "</repositories>" +
                "</cruise>";
        String configContent = String.format(configTemplate, false);
        GoConfigHolder holder = xmlLoader.loadConfigHolder(configContent);
        PackageRepository packageRepository = holder.config.getPackageRepositories().find("2ef830d7-dd66-42d6-b393-64a84646e557");
        PackageDefinition aPackage = packageRepository.findPackage("88a3beca-cbe2-4c4d-9744-aa0cda3f371c");
        assertThat(aPackage.isAutoUpdate(), is(false));

        configContent = String.format(configTemplate, true);
        holder = xmlLoader.loadConfigHolder(configContent);
        packageRepository = holder.config.getPackageRepositories().find("2ef830d7-dd66-42d6-b393-64a84646e557");
        aPackage = packageRepository.findPackage("88a3beca-cbe2-4c4d-9744-aa0cda3f371c");
        assertThat(aPackage.isAutoUpdate(), is(true));
    }

    @Test
    public void shouldAllowColonsInPipelineLabelTemplate() {
        String xml = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<repositories>\n"
                + "    <repository id='repo-id' name='repo_name'>\n"
                + "		<pluginConfiguration id='plugin-id' version='1.0'/>\n"
                + "      <configuration>\n"
                + "        <property>\n"
                + "          <key>url</key>\n"
                + "          <value>http://go</value>\n"
                + "        </property>\n"
                + "      </configuration>\n"
                + "      <packages>\n"
                + "        <package id='package-id' name='pkg_name'>\n"
                + "          <configuration>\n"
                + "            <property>\n"
                + "              <key>name</key>\n"
                + "              <value>go-agent</value>\n"
                + "            </property>\n"
                + "          </configuration>\n"
                + "        </package>\n"
                + "      </packages>\n"
                + "    </repository>\n"
                + "  </repositories>"
                + "<pipelines group=\"group_name\">\n"
                + "  <pipeline name=\"new_name\" labeltemplate=\"${COUNT}-${repo_name:pkg_name}\">\n"
                + "    <materials>\n"
                + "      <package ref='package-id' />\n"
                + "    </materials>\n"
                + "    <stage name=\"stage_name\">\n"
                + "      <jobs>\n"
                + "        <job name=\"job_name\" />\n"
                + "      </jobs>\n"
                + "    </stage>\n"
                + "  </pipeline>\n"
                + "</pipelines></cruise>";
        GoConfigHolder holder = ConfigMigrator.loadWithMigration(xml);
        assertThat(holder.config.getAllPipelineConfigs().get(0).materialConfigs().get(0).getName().toString(), is("repo_name:pkg_name"));
    }

    @Test
    public void shouldAllowEmptyAuthorizationTagUnderEachTemplateWhileLoading() throws Exception {
        String configString =
                "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n" +
                        "   <templates>" +
                        "       <pipeline name='template-name'>" +
                        "           <authorization>" +
                        "               <admins>" +
                        "               </admins>" +
                        "           </authorization>" +
                        "           <stage name='stage-name'>" +
                        "               <jobs>" +
                        "                   <job name='job-name'/>" +
                        "               </jobs>" +
                        "           </stage>" +
                        "       </pipeline>" +
                        "   </templates>" +
                        "</cruise>";
        CruiseConfig configForEdit = ConfigMigrator.loadWithMigration(configString).configForEdit;
        PipelineTemplateConfig template = configForEdit.getTemplateByName(new CaseInsensitiveString("template-name"));
        Authorization authorization = template.getAuthorization();
        assertThat(authorization, is(not(nullValue())));
        assertThat(authorization.getAdminsConfig().getUsers(), is(empty()));
        assertThat(authorization.getAdminsConfig().getRoles(), is(empty()));
    }

    @Test
    public void shouldAllowPluggableTaskConfiguration() throws Exception {
        String configString =
                "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                        + " <pipelines>"
                        + "<pipeline name='pipeline1'>"
                        + "    <materials>"
                        + "      <svn url='svnurl' username='admin' password='%s'/>"
                        + "    </materials>"
                        + "  <stage name='mingle'>"
                        + "    <jobs>"
                        + "      <job name='do-something'><tasks>"
                        + "        <task>"
                        + "          <pluginConfiguration id='plugin-id-1' version='1.0'/>"
                        + "          <configuration>"
                        + "            <property><key>url</key><value>http://fake-go-server</value></property>"
                        + "            <property><key>username</key><value>godev</value></property>"
                        + "            <property><key>password</key><value>password</value></property>"
                        + "          </configuration>"
                        + "        </task> </tasks>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline></pipelines>"
                        + "</cruise>";
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(configString).configForEdit;

        PipelineConfig pipelineConfig = cruiseConfig.getAllPipelineConfigs().get(0);
        JobConfig jobConfig = pipelineConfig.getFirstStageConfig().getJobs().get(0);
        Tasks tasks = jobConfig.getTasks();
        assertThat(tasks.size(), is(1));
        assertThat(tasks.get(0) instanceof PluggableTask, is(true));
        PluggableTask task = (PluggableTask) tasks.get(0);
        assertThat(task.getTaskType(), is("pluggable_task_plugin_id_1"));
        assertThat(task.getTypeForDisplay(), is("Pluggable Task"));
        final Configuration configuration = task.getConfiguration();
        assertThat(configuration.listOfConfigKeys().size(), is(3));
        assertThat(configuration.listOfConfigKeys(), is(asList("url", "username", "password")));
        Collection values = CollectionUtils.collect(configuration.listOfConfigKeys(), new Transformer() {
            @Override
            public Object transform(Object o) {
                ConfigurationProperty property = configuration.getProperty((String) o);
                return property.getConfigurationValue().getValue();
            }
        });
        assertThat(new ArrayList<String>(values), is(asList("http://fake-go-server", "godev", "password")));
    }

    @Test
    public void shouldBeAbleToResolveSecureConfigPropertiesForPluggableTasks() throws Exception {
        String encryptedValue = new GoCipher().encrypt("password");
        String configString =
                "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                        + " <pipelines>"
                        + "<pipeline name='pipeline1'>"
                        + "    <materials>"
                        + "      <svn url='svnurl' username='admin' password='%s'/>"
                        + "    </materials>"
                        + "  <stage name='mingle'>"
                        + "    <jobs>"
                        + "      <job name='do-something'><tasks>"
                        + "        <task>"
                        + "          <pluginConfiguration id='plugin-id-1' version='1.0'/>"
                        + "          <configuration>"
                        + "            <property><key>username</key><value>godev</value></property>"
                        + "            <property><key>password</key><value>password</value></property>"
                        + "          </configuration>"
                        + "        </task> </tasks>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline></pipelines>"
                        + "</cruise>";

        //meta data of package
        PluggableTaskConfigStore.store().setPreferenceFor("plugin-id-1", new TaskPreference(new com.thoughtworks.go.plugin.api.task.Task() {
            @Override
            public TaskConfig config() {
                TaskConfig taskConfig = new TaskConfig();
                taskConfig.addProperty("username").with(Property.SECURE, false);
                taskConfig.addProperty("password").with(Property.SECURE, true);
                return taskConfig;
            }

            @Override
            public TaskExecutor executor() {
                return null;
            }

            @Override
            public TaskView view() {
                return null;
            }

            @Override
            public ValidationResult validate(TaskConfig configuration) {
                return null;
            }
        }));

        GoConfigHolder goConfigHolder = xmlLoader.loadConfigHolder(configString);

        PipelineConfig pipelineConfig = goConfigHolder.config.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        PluggableTask task = (PluggableTask) pipelineConfig.getStage("mingle").getJobs().getJob(new CaseInsensitiveString("do-something")).getTasks().first();

        assertFalse(task.getConfiguration().getProperty("username").isSecure());
        assertTrue(task.getConfiguration().getProperty("password").isSecure());
    }

    @Test
    public void shouldSerializeJobElasticProfileId() throws Exception {
        String configWithJobElasticProfileId =
                "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                        + "<server>\n"
                        + "  <elastic jobStarvationTimeout=\"10\">\n"
                        + "    <profiles>\n"
                        + "      <profile id='unit-test' pluginId='aws'>\n"
                        + "        <property>\n"
                        + "          <key>instance-type</key>\n"
                        + "          <value>m1.small</value>\n"
                        + "        </property>\n"
                        + "      </profile>\n"
                        + "    </profiles>\n"
                        + "  </elastic>\n"
                        + "</server>\n"
                        + "<pipelines group=\"first\">\n"
                        + "<pipeline name=\"pipeline\">\n"
                        + "  <materials>\n"
                        + "    <hg url=\"/hgrepo\"/>\n"
                        + "  </materials>\n"
                        + "  <stage name=\"mingle\">\n"
                        + "    <jobs>\n"
                        + "      <job name=\"functional\" elasticProfileId=\"unit-test\">\n"
                        + "      </job>\n"
                        + "    </jobs>\n"
                        + "  </stage>\n"
                        + "</pipeline>\n"
                        + "</pipelines>\n"
                        + "</cruise>\n";

        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(configWithJobElasticProfileId).configForEdit;

        String elasticProfileId = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline")).getStage("mingle").jobConfigByConfigName("functional").getElasticProfileId();

        assertThat(elasticProfileId, is("unit-test"));
    }

    @Test
    public void shouldSerializeElasticAgentProfiles() throws Exception {
        String configWithElasticProfile =
                "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                        + "<server artifactsdir='artifacts'>\n"
                        + "  <elastic jobStarvationTimeout=\"2\">\n"
                        + "    <profiles>\n"
                        + "      <profile id=\"foo\" pluginId=\"docker\">\n"
                        + "          <property>\n"
                        + "           <key>USERNAME</key>\n"
                        + "           <value>bob</value>\n"
                        + "          </property>\n"
                        + "      </profile>\n"
                        + "    </profiles>\n"
                        + "  </elastic>\n"
                        + "</server>\n"
                        + "</cruise>\n";

        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(configWithElasticProfile).configForEdit;

        assertThat(cruiseConfig.server().getElasticConfig().getJobStarvationTimeout(), is(120000L));
        assertThat(cruiseConfig.server().getElasticConfig().getProfiles().size(), is(1));

        ElasticProfile elasticProfile = cruiseConfig.server().getElasticConfig().getProfiles().find("foo");
        assertThat(elasticProfile, is(notNullValue()));
        assertThat(elasticProfile.getPluginId(), is("docker"));
        assertThat(elasticProfile.size(), is(1));
        assertThat(elasticProfile.getProperty("USERNAME").getValue(), is("bob"));
    }

    @Test
    public void shouldNotAllowJobElasticProfileIdAndResourcesTogether() throws Exception {
        String configWithJobElasticProfile =
                "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                        + "<pipelines group=\"first\">\n"
                        + "<pipeline name=\"pipeline\">\n"
                        + "  <materials>\n"
                        + "    <hg url=\"/hgrepo\"/>\n"
                        + "  </materials>\n"
                        + "  <stage name=\"mingle\">\n"
                        + "    <jobs>\n"
                        + "      <job name=\"functional\" elasticProfileId=\"docker.unit-test\">\n"
                        + "        <resources>\n"
                        + "          <resource>foo</resource>\n"
                        + "        </resources>\n"
                        + "      </job>\n"
                        + "    </jobs>\n"
                        + "  </stage>\n"
                        + "</pipeline>\n"
                        + "</pipelines>\n"
                        + "</cruise>\n";
        try {
            CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(configWithJobElasticProfile).configForEdit;
            fail("expected exception!");
        } catch (Exception e) {
            assertThat(e.getCause().getCause(), instanceOf(GoConfigInvalidException.class));
            assertThat(e.getCause().getCause().getMessage(), is("Job cannot have both `resource` and `elasticProfileId`, No profile defined corresponding to profile_id 'docker.unit-test', Job cannot have both `resource` and `elasticProfileId`"));
        }

    }

    @Test
    public void shouldGetConfigRepoPreprocessor(){
        MagicalGoConfigXmlLoader loader = new MagicalGoConfigXmlLoader(null, null);
        assertThat(loader.getPreprocessorOfType(ConfigRepoPartialPreprocessor.class) instanceof ConfigRepoPartialPreprocessor, is(true));
        assertThat(loader.getPreprocessorOfType(ConfigParamPreprocessor.class) instanceof ConfigParamPreprocessor, is(true));
    }

    private StageConfig stageWithJobResource(String resourceName) {
        StageConfig stage = StageConfigMother.custom("stage", "job");
        JobConfigs configs = stage.allBuildPlans();
        Resource resource = new Resource();
        resource.setName(resourceName);
        configs.get(0).resources().add(resource);
        return stage;
    }
}
