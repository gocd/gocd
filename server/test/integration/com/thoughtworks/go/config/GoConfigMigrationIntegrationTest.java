/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.server.security.ldap.BasesConfig;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.config.Configuration;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import com.thoughtworks.go.domain.config.PluginConfiguration;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.util.ServerVersion;
import com.thoughtworks.go.serverhealth.HealthStateLevel;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.serverhealth.ServerHealthStates;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.*;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.jdom.Document;
import org.jdom.filter.ElementFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static com.thoughtworks.go.util.FileUtil.readToEnd;
import static java.util.Arrays.asList;
import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class GoConfigMigrationIntegrationTest {
    private File configFile;
    ConfigRepository configRepository;
    @Autowired private SystemEnvironment systemEnvironment;
    @Autowired private ConfigCache configCache;
    @Autowired private ServerVersion serverVersion;
    @Autowired private ConfigElementImplementationRegistry registry;
    @Autowired private GoConfigService goConfigService;
    @Autowired private MetricsProbeService metricsProbeService;
    @Autowired private ServerHealthService serverHealthService;

    private String currentGoServerVersion;
    private MagicalGoConfigXmlLoader loader;

    @Before
    public void setUp() throws Exception {
        configFile = TestFileUtil.createTempFileInSubfolder("cruise-config.xml");
        new SystemEnvironment().setProperty(SystemEnvironment.CONFIG_FILE_PROPERTY, configFile.getAbsolutePath());
        GoConfigFileHelper.clearConfigVersions();
        configRepository = new ConfigRepository(systemEnvironment);
        configRepository.initialize();
        serverHealthService.removeAllLogs();
        currentGoServerVersion = serverVersion.version();
        loader = new MagicalGoConfigXmlLoader(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);
    }

    @After
    public void tearDown() throws Exception {
        GoConfigFileHelper.clearConfigVersions();
        configFile.delete();
        serverHealthService.removeAllLogs();
    }

    @Test
    public void shouldNotUpgradeCruiseConfigFileIfVersionMatches() throws Exception {
        CruiseConfig cruiseConfig = loadConfigFileWithContent(ConfigFileFixture.MINIMAL);
        assertThat(cruiseConfig.schemaVersion(), is(GoConstants.CONFIG_SCHEMA_VERSION));
        assertThat(configRepository.getRevision(ConfigRepository.CURRENT).getUsername(), is(not("Upgrade")));
    }

    @Test
    public void shouldValidateCruiseConfigFileIrrespectiveOfUpgrade() throws Exception {
        String configString = ConfigFileFixture.configWithEnvironments("<environments>"
                + "  <environment name='foo'>"
                + "<pipelines>"
                + " <pipeline name='does_not_exist'/>"
                + "</pipelines>"
                + "</environment>"
                + "</environments>");
        try {
            loadConfigFileWithContent(configString);
            fail("Should not upgrade invalid config file");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Environment 'foo' refers to an unknown pipeline 'does_not_exist'"));
        }
    }

    @Test
    public void shouldUpgradeCruiseConfigFileIfVersionDoesNotMatch() throws Exception {
        CruiseConfig cruiseConfig = loadConfigFileWithContent(ConfigFileFixture.OLD);
        assertThat(cruiseConfig.schemaVersion(), is(GoConstants.CONFIG_SCHEMA_VERSION));
    }

    @Test
    public void shouldMigrateConfigContentAsAString() throws Exception {
        String newContent = new GoConfigMigration(configRepository, new TimeProvider(), configCache, ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService)
                .upgradeIfNecessary(ConfigFileFixture.VERSION_0);
        assertThat(newContent, containsString("schemaVersion=\"" + GoConfigSchema.currentSchemaVersion() + "\""));
    }

    @Test
    public void shouldNotMigrateConfigContentAsAStringWhenAlreadyUpToDate() throws Exception {
        GoConfigMigration configMigration = new GoConfigMigration(configRepository, new TimeProvider(), configCache,
                ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);
        String newContent = configMigration.upgradeIfNecessary(ConfigFileFixture.VERSION_0);
        assertThat(newContent, is(configMigration.upgradeIfNecessary(newContent)));
    }

    @Test
    public void shouldNotUpgradeInvalidConfigFileWhenThereIsNoValidConfigVersioned() throws Exception {
        try {
            assertThat(configRepository.getRevision(ConfigRepository.CURRENT), is(nullValue()));
            loadConfigFileWithContent("<cruise></cruise>");
            fail("Should not upgrade invalid config file");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Cruise config file with version 0 is invalid. Unable to upgrade."));
        }
    }

    @Test
    public void shouldRevertConfigToTheLatestValidConfigVersionFromGitIfCurrentConfigIsInvalid() throws IOException {
        try {
            loadConfigFileWithContent(ConfigFileFixture.MINIMAL);
            loadConfigFileWithContent("<cruise></cruise>");
            ServerHealthStates states = serverHealthService.getAllLogs();
            assertThat(states.size(), is(1));
            assertThat(states.get(0).getDescription(), containsString("Go encountered an invalid configuration file while starting up. The invalid configuration file has been renamed to ‘"));
            assertThat(states.get(0).getDescription(), containsString("’ and a new configuration file has been automatically created using the last good configuration."));
            assertThat(states.get(0).getMessage(), containsString("Invalid Configuration"));
            assertThat(states.get(0).getType(), is(HealthStateType.general(HealthStateScope.forInvalidConfig())));
            assertThat(states.get(0).getLogLevel(), is(HealthStateLevel.WARNING));
        } catch (Exception e) {
            fail("Should not Throw an exception, should revert to the last valid file versioned in config.git");
        }
    }

    @Test
    public void shouldTryToRevertConfigToTheLatestValidConfigVersionOnlyOnce() throws IOException {
        try {
            configRepository.checkin(new GoConfigRevision("<cruise></cruise>", "md5", "ps", "123", new TimeProvider()));
            loadConfigFileWithContent("<cruise></cruise>");
            ServerHealthStates states = serverHealthService.getAllLogs();
            assertThat(states.size(), is(1));
            assertThat(states.get(0).getDescription(), containsString("Go encountered an invalid configuration file while starting up. The invalid configuration file has been renamed to ‘"));
            assertThat(states.get(0).getDescription(), containsString("’ and a new configuration file has been automatically created using the last good configuration."));
            assertThat(states.get(0).getMessage(), containsString("Invalid Configuration"));
            assertThat(states.get(0).getType(), is(HealthStateType.general(HealthStateScope.forInvalidConfig())));
            assertThat(states.get(0).getLogLevel(), is(HealthStateLevel.WARNING));
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Cruise config file with version 0 is invalid. Unable to upgrade."));
        }
    }

    @Test
    public void shouldMoveApprovalFromAPreviousStageToTheBeginningOfASecondStage() throws Exception {
        CruiseConfig cruiseConfig = loadConfigFileWithContent(ConfigFileFixture.VERSION_0);

        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline"));
        StageConfig firstStage = pipelineConfig.get(0);
        StageConfig secondStage = pipelineConfig.get(1);
        assertThat(firstStage.requiresApproval(), is(Boolean.FALSE));
        assertThat(secondStage.requiresApproval(), is(Boolean.TRUE));
    }

    @Test
    public void shouldMigrateApprovalsCorrectlyBug2112() throws Exception {
        File bjcruise = new File("../common/test-resources/unit/data/bjcruise-cruise-config-1.0.xml");
        assertThat(bjcruise.exists(), is(true));
        String xml = readToEnd(bjcruise);

        CruiseConfig cruiseConfig = loadConfigFileWithContent(xml);

        PipelineConfig pipeline = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("evolve"));

        StageConfig dbStage = pipeline.findBy(new CaseInsensitiveString("db"));
        assertThat(dbStage.requiresApproval(), is(false));

        StageConfig installStage = pipeline.findBy(new CaseInsensitiveString("install"));
        assertThat(installStage.requiresApproval(), is(true));
    }

    @Test
    public void shouldBackupOldConfigFileBeforeUpgrade() throws Exception {
        assertThat(configFiles().length, is(0));
        loadConfigFileWithContent(ConfigFileFixture.OLD);
        assertThat(configFiles().length, is(1));
    }

    @Test
    public void shouldMigrateMaterialFolderAttributeToDest() throws Exception {
        CruiseConfig cruiseConfig = loadConfigFileWithContent(ConfigFileFixture.VERSION_2);
        MaterialConfig actual = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("multiple")).materialConfigs().first();
        assertThat(actual.getFolder(), is("part1"));
    }

    @Test
    public void shouldMigrateRevision5ToTheLatest() throws Exception {
        CruiseConfig cruiseConfig = loadConfigFileWithContent(ConfigFileFixture.VERSION_5);
        assertThat(cruiseConfig.schemaVersion(), is(GoConstants.CONFIG_SCHEMA_VERSION));
    }

    @Test
    public void shouldMigrateRevision7To8() throws Exception {
        CruiseConfig cruiseConfig = loadConfigFileWithContent(ConfigFileFixture.VERSION_7);
        HgMaterialConfig hgConfig = (HgMaterialConfig) cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("framework")).materialConfigs().first();
        assertThat(hgConfig.getFolder(), is(nullValue()));
        assertThat(hgConfig.filter(), is(notNullValue()));
    }

    @Test
    public void shouldMigrateToRevision17() throws Exception {
        CruiseConfig cruiseConfig = loadConfigFileWithContent(ConfigFileFixture.WITH_3_AGENT_CONFIG);
        assertThat(cruiseConfig.agents().size(), is(3));
        assertThat(cruiseConfig.agents().getAgentByUuid("2").isDisabled(), is(true));
        assertThat(cruiseConfig.agents().getAgentByUuid("1").isDisabled(), is(false));
    }

    @Test
    public void shouldMigrateDependsOnTagToBeADependencyMaterial() throws Exception {
        String content = FileUtils.readFileToString(
                new File("../common/test-resources/unit/data/config/version4/cruise-config-dependency-migration.xml"));
        CruiseConfig cruiseConfig = loadConfigFileWithContent(content);
        MaterialConfig actual = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("depends")).materialConfigs().first();
        assertThat(actual, instanceOf(DependencyMaterialConfig.class));
        DependencyMaterialConfig depends = (DependencyMaterialConfig) actual;
        assertThat(depends.getPipelineName(), is(new CaseInsensitiveString("multiple")));
        assertThat(depends.getStageName(), is(new CaseInsensitiveString("helloworld-part2")));
    }

    @Test
    public void shouldFailIfJobsWithSameNameButDifferentCasesExistInConfig() throws IOException {
        final List<Exception> exs = new ArrayList<Exception>();
        GoConfigMigration upgrader = new GoConfigMigration(
                new GoConfigMigration.UpgradeFailedHandler() {
                    public void handle(Exception e) {
                        exs.add(e);
                    }
                }, configRepository, new TimeProvider(), configCache, ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);
        FileUtils.writeStringToFile(configFile, ConfigFileFixture.JOBS_WITH_DIFFERNT_CASE);

        upgrader.upgradeIfNecessary(configFile, currentGoServerVersion);
        assertThat(exs.size(), is(1));
        assertThat(exs.get(0).getMessage(), containsString("You have defined multiple Jobs called 'Test'"));
    }

    @Test
    public void shouldVersionControlAnUpgradedConfigIfItIsValid() throws Exception {
        GoConfigMigration upgrader = new GoConfigMigration(
                new GoConfigMigration.UpgradeFailedHandler() {
                    public void handle(Exception e) {
                        throw new AssertionError("upgrade failed!!!!!");
                    }
                }, configRepository, new TimeProvider(), configCache, ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);
        FileUtils.writeStringToFile(configFile, ConfigFileFixture.DEFAULT_XML_WITH_2_AGENTS);
        configRepository.checkin(new GoConfigRevision("dummy-content", "some-md5", "loser", "100.3.1", new TimeProvider()));

        upgrader.upgradeIfNecessary(configFile, currentGoServerVersion);

        GoConfigRevision latest = configRepository.getRevision(ConfigRepository.CURRENT);

        assertThat(latest.getUsername(), is("Upgrade"));

        String contents = FileUtils.readFileToString(configFile);
        assertThat(latest.getContent(), is(contents));
        assertThat(latest.getMd5(), is(DigestUtils.md5Hex(contents)));
    }

    @Test
    public void shouldMigrateToRevision22() throws Exception {
        final String content = FileUtil.readToEnd(getClass().getResourceAsStream("cruise-config-escaping-migration-test-fixture.xml"));

        String migratedContent = migrateXmlString(content, 21, 22);

        String expected = content.replaceAll("(?<!do_not_sub_)#", "##").replace("<cruise schemaVersion=\"21\">", "<cruise schemaVersion=\"22\">");
        assertStringsIgnoringCarriageReturnAreEqual(expected, migratedContent);
    }

    @Test
    public void shouldMigrateToRevision28() throws Exception {
        final String content = FileUtil.readToEnd(getClass().getResourceAsStream("no-tracking-tool-group-holder-config.xml"));

        String migratedContent = migrateXmlString(content, 27);

        assertThat(migratedContent, containsString("\"http://foo.bar/baz/${ID}\""));
        assertThat(migratedContent, containsString("\"http://hello.world/${ID}/hello\""));
    }

    @Test
    public void shouldMigrateToRevision34() throws Exception {
        final String content = FileUtil.readToEnd(getClass().getResourceAsStream("svn-p4-with-parameterized-passwords.xml"));

        String migratedContent = migrateXmlString(content, 22);

        String expected = content.replaceAll("#\\{jez_passwd\\}", "badger")
                .replace("<cruise schemaVersion=\"22\">", "<cruise schemaVersion=\"" + GoConstants.CONFIG_SCHEMA_VERSION + "\">")
                .replaceAll("##", "#");
        assertStringsIgnoringCarriageReturnAreEqual(expected, migratedContent);
    }

    @Test
    public void shouldMigrateToRevision35_escapeHash() throws Exception {
        final String content = FileUtil.readToEnd(getClass().getResourceAsStream("escape_param_for_nant_p4.xml"));

        String migratedContent = migrateXmlString(content, 22);

        String expected = content.replace("<cruise schemaVersion=\"22\">", "<cruise schemaVersion=\"" + GoConstants.CONFIG_SCHEMA_VERSION + "\">")
                .replace("<view>##foo#</view>", "<view>####foo##</view>").replace("nantpath=\"#foo##\"", "nantpath=\"##foo####\"");
        assertStringsIgnoringCarriageReturnAreEqual(expected, migratedContent);
    }

    @Test
    public void shouldMigrateToRevision58_deleteVMMS() throws Exception {
        String migratedContent = migrateXmlString(ConfigFileFixture.WITH_VMMS_CONFIG, 50, 58);

        assertFalse(migratedContent.contains("vmms"));
    }

    @Test
    public void shouldMigrateToRevision59_convertLogTypeToArtifact() throws Exception {
        String migratedContent = migrateXmlString(ConfigFileFixture.WITH_LOG_ARTIFACT_CONFIG, 50, 59);

        CruiseConfig cruiseConfig = loadConfigFileWithContent(migratedContent);

        ArtifactPlans artifactPlans = cruiseConfig.getAllPipelineConfigs().get(0).getStage(new CaseInsensitiveString("mingle")).getJobs().getJob(
                new CaseInsensitiveString("bluemonkeybutt")).artifactPlans();

        assertEquals("from1", artifactPlans.get(0).getSrc());
        assertEquals(artifactPlans.get(0).getDest(), "");
        assertEquals("from2", artifactPlans.get(1).getSrc());
        assertEquals("to2", artifactPlans.get(1).getDest());
        assertEquals("from3", artifactPlans.get(2).getSrc());
        assertEquals(artifactPlans.get(2).getDest(), "");
        assertEquals("from4", artifactPlans.get(3).getSrc());
        assertEquals("to4", artifactPlans.get(3).getDest());
    }


    @Test
    public void shouldMigrateExecTaskArgValueToTextNode() throws Exception {
        String migratedContent = migrateXmlString(ConfigFileFixture.VALID_XML_3169, 14);
        assertThat(migratedContent, containsString("<arg>test</arg>"));
    }

    @Test
    public void shouldMigrateToRevision23_IsLockedIsFalseByDefault() throws InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        final String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cruise schemaVersion=\"22\">\n"
                + "    <server artifactsdir=\"artifacts\"/>\n"
                + "    <pipelines>"
                + "      <pipeline name=\"in_env\">"
                + "         <materials> "
                + "           <hg url=\"blah\"/>"
                + "         </materials>  "
                + "         <stage name=\"some_stage\">"
                + "             <jobs>"
                + "             <job name=\"some_job\">"
                + "                 <tasks>"
                + "                    <exec command=\"ls\"/>"
                + "                 </tasks>"
                + "             </job>"
                + "             </jobs>"
                + "         </stage>"
                + "      </pipeline>"
                + "      <pipeline name=\"not_in_env\">"
                + "         <materials> "
                + "           <hg url=\"blah\"/>"
                + "         </materials>  "
                + "         <stage name=\"some_stage\">"
                + "             <jobs>"
                + "             <job name=\"some_job\">"
                + "                 <tasks>"
                + "                    <exec command=\"ls\"/>"
                + "                 </tasks>"
                + "             </job>"
                + "             </jobs>"
                + "         </stage>"
                + "      </pipeline>"
                + "      <pipeline name=\"in_env_unLocked\" isLocked=\"false\">"
                + "         <materials> "
                + "           <hg url=\"blah\"/>"
                + "         </materials>  "
                + "         <stage name=\"some_stage\">"
                + "             <jobs>"
                + "             <job name=\"some_job\">"
                + "                 <tasks>"
                + "                    <exec command=\"ls\"/>"
                + "                 </tasks>"
                + "             </job>"
                + "             </jobs>"
                + "         </stage>"
                + "      </pipeline>"
                + "    </pipelines>"
                + "    <environments>"
                + "    <environment name=\"some_env\">"
                + "        <pipelines>"
                + "            <pipeline name=\"in_env\"/>"
                + "            <pipeline name=\"in_env_unLocked\"/>"
                + "        </pipelines>"
                + "    </environment>"
                + "    </environments>"
                + " </cruise>";
        String migratedContent = migrateXmlString(content, 22);

        assertThat(migratedContent, containsString("<pipeline isLocked=\"true\" name=\"in_env\">"));
        assertThat(migratedContent, containsString("<pipeline isLocked=\"false\" name=\"in_env_unLocked\">"));
        assertThat(migratedContent, containsString("<pipeline name=\"not_in_env\">"));
    }

    @Test
    public void shouldEncryptPasswordsOnUpgradeIfNecessary() throws IOException {
        final List<Exception> exs = new ArrayList<Exception>();
        GoConfigMigration upgrader = new GoConfigMigration(
                new GoConfigMigration.UpgradeFailedHandler() {
                    public void handle(Exception e) {
                        exs.add(e);
                    }
                }, configRepository, new TimeProvider(), configCache, ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);
        String configContent = ConfigFileFixture.configWithPipeline(String.format(
                "<pipeline name='pipeline1'>"
                        + "    <materials>"
                        + "      <svn url='svnurl' username='admin' password='%s'/>"
                        + "    </materials>"
                        + "  <stage name='mingle'>"
                        + "    <jobs>"
                        + "      <job name='do-something'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", "hello"), 32);
        FileUtils.writeStringToFile(configFile, configContent);

        upgrader.upgradeIfNecessary(configFile, currentGoServerVersion);

        assertThat(FileUtils.readFileToString(configFile), containsString("encryptedPassword="));
        assertThat(FileUtils.readFileToString(configFile), not(containsString("password=")));
    }

    @Test
    public void shouldMergeRolesWithMatchingCaseInsensitiveNames() throws Exception {
        final String configContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cruise schemaVersion=\"39\">\n"
                + "    <server artifactsdir=\"artifacts\">\n"
                + "        <security>\n"
                + "             <roles>\n"
                + "                 <role name=\"bAr\">\n"
                + "                     <user>quux</user>\n"
                + "                     <user>bang</user>\n"
                + "                     <user>LoSeR</user>\n"
                + "                 </role>\n"
                + "                 <role name=\"Foo\">\n"
                + "                     <user>foo</user>\n"
                + "                     <user>LoSeR</user>\n"
                + "                     <user>bar</user>\n"
                + "                     <user>LOsER</user>\n"
                + "                 </role>\n"
                + "                 <role name=\"BaR\">\n"
                + "                     <user>baz</user>\n"
                + "                     <user>bang</user>\n"
                + "                     <user>lOsEr</user>\n"
                + "                 </role>\n"
                + "             </roles>\n"
                + "        </security>"
                + "    </server>"
                + " </cruise>";

        File configFile = new File(systemEnvironment.getCruiseConfigFile());
        FileUtils.writeStringToFile(configFile, configContent);
        CruiseConfig cruiseConfig = loadWithMigration(configFile).config;

        RolesConfig roles = cruiseConfig.server().security().getRoles();
        assertThat(roles.size(), is(2));
        assertThat(roles.get(0), is(new Role(new CaseInsensitiveString("bAr"),
                new RoleUser(new CaseInsensitiveString("quux")),
                new RoleUser(new CaseInsensitiveString("bang")),
                new RoleUser(new CaseInsensitiveString("LoSeR")),
                new RoleUser(new CaseInsensitiveString("baz")))));

        assertThat(roles.get(1), is(new Role(new CaseInsensitiveString("Foo"),
                new RoleUser(new CaseInsensitiveString("foo")),
                new RoleUser(new CaseInsensitiveString("LoSeR")),
                new RoleUser(new CaseInsensitiveString("bar")))));
    }

    @Test
    public void shouldAllowParamsInP4ServerAndPortField() throws IOException {
        final List<Exception> exs = new ArrayList<Exception>();
        GoConfigMigration upgrader = new GoConfigMigration(
                new GoConfigMigration.UpgradeFailedHandler() {
                    public void handle(Exception e) {
                        exs.add(e);
                    }
                }, configRepository, new TimeProvider(), configCache, ConfigElementImplementationRegistryMother.withNoPlugins(),
                metricsProbeService);
        String configContent = ConfigFileFixture.configWithPipeline(String.format(
                "<pipeline name='pipeline1'>"
                        + "<params>"
                        + "        <param name='param_foo'>a:3</param>"
                        + "      </params>"
                        + "    <materials>"
                        + "<p4 port='#{param_foo}' username='' dest='blah' materialName='boo'>"
                        + "<view><![CDATA[blah]]></view>"
                        + "<filter>"
                        + "<ignore pattern='' />"
                        + "</filter>"
                        + "</p4>"
                        + "    </materials>"
                        + "  <stage name='mingle'>"
                        + "    <jobs>"
                        + "      <job name='do-something'>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline>", "hello"), 32);
        FileUtils.writeStringToFile(configFile, configContent);

        upgrader.upgradeIfNecessary(configFile, currentGoServerVersion);

        assertThat(FileUtils.readFileToString(configFile), containsString("port='#{param_foo}'"));
    }

    @Test
    public void shouldIntroduceAWrapperTagForUsersOfRole() throws Exception {

        final List<Exception> exs = new ArrayList<Exception>();
        GoConfigMigration upgrader = new GoConfigMigration(
                new GoConfigMigration.UpgradeFailedHandler() {
                    public void handle(Exception e) {
                        exs.add(e);
                    }
                }, configRepository, new TimeProvider(), configCache, ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);

        String content = "<cruise schemaVersion='" + 47 + "'>\n"
                + "<server artifactsdir=\"logs\" siteUrl=\"http://go-server-site-url:8153\" secureSiteUrl=\"https://go-server-site-url:8154\" jobTimeout=\"60\">\n"
                + "    <security>\n"
                + "      <roles>\n"
                + "        <role name=\"admins\">\n"
                + "            <user>admin_one</user>\n"
                + "            <user>admin_two</user>\n"
                + "        </role>\n"
                + "        <role name=\"devs\">\n"
                + "            <user>dev_one</user>\n"
                + "            <user>dev_two</user>\n"
                + "            <user>dev_three</user>\n"
                + "        </role>\n"
                + "      </roles>\n"
                + "      <admins>\n"
                + "        <role>admins</role>\n"
                + "      </admins>\n"
                + "    </security>\n"
                + "  </server>"
                + "</cruise>";

        FileUtils.writeStringToFile(configFile, content);

        upgrader.upgradeIfNecessary(configFile, currentGoServerVersion);

        String configXml = FileUtils.readFileToString(configFile);

        MagicalGoConfigXmlLoader loader = new MagicalGoConfigXmlLoader(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);
        GoConfigHolder configHolder = loader.loadConfigHolder(configXml);

        CruiseConfig config = configHolder.config;

        ServerConfig server = config.server();
        RolesConfig roles = server.security().getRoles();
        assertThat(roles,
                hasItem(new Role(new CaseInsensitiveString("admins"), new RoleUser(new CaseInsensitiveString("admin_one")), new RoleUser(new CaseInsensitiveString("admin_two")))));
        assertThat(roles, hasItem(new Role(new CaseInsensitiveString("devs"), new RoleUser(new CaseInsensitiveString("dev_one")), new RoleUser(new CaseInsensitiveString("dev_two")),
                new RoleUser(new CaseInsensitiveString("dev_three")))));
    }

    @Test
    public void shouldSetServerId_toARandomUUID_ifServerTagDoesntExist() {
        GoConfigService.XmlPartialSaver fileSaver = goConfigService.fileSaver(true);
        GoConfigValidity configValidity = fileSaver.saveXml("<cruise schemaVersion='" + 53 + "'>\n"
                + "</cruise>", goConfigService.configFileMd5());
        assertThat("Has error: " + configValidity.errorMessage(), configValidity.isValid(), is(true));

        CruiseConfig config = goConfigService.getCurrentConfig();
        ServerConfig server = config.server();

        assertThat(server.getServerId().matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"), is(true));
    }

    @Test
    public void shouldSetServerId_toARandomUUID_ifOneDoesntExist() {
        GoConfigService.XmlPartialSaver fileSaver = goConfigService.fileSaver(true);
        GoConfigValidity configValidity = fileSaver.saveXml("<cruise schemaVersion='" + 55 + "'>\n"
                + "<server artifactsdir=\"logs\" siteUrl=\"http://go-server-site-url:8153\" secureSiteUrl=\"https://go-server-site-url:8154\" jobTimeout=\"60\">\n"
                + "  </server>"
                + "</cruise>", goConfigService.configFileMd5());
        assertThat("Has error: " + configValidity.errorMessage(), configValidity.isValid(), is(true));

        CruiseConfig config = goConfigService.getCurrentConfig();
        ServerConfig server = config.server();

        assertThat(server.getServerId().matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"), is(true));
    }

    @Test
    public void shouldLoadServerId_ifOneExists() {
        GoConfigService.XmlPartialSaver fileSaver = goConfigService.fileSaver(true);
        GoConfigValidity configValidity = fileSaver.saveXml("<cruise schemaVersion='" + 55 + "'>\n"
                + "<server artifactsdir=\"logs\" siteUrl=\"http://go-server-site-url:8153\" secureSiteUrl=\"https://go-server-site-url:8154\" jobTimeout=\"60\" serverId=\"foo\">\n"
                + "  </server>"
                + "</cruise>", goConfigService.configFileMd5());
        assertThat("Has error: " + configValidity.errorMessage(), configValidity.isValid(), is(true));

        CruiseConfig config = goConfigService.getCurrentConfig();
        ServerConfig server = config.server();

        assertThat(server.getServerId(), is("foo"));
    }

    @Test
    public void shouldMigrateFrom61_MigrateSearchBaseIntoAnElement() throws Exception {
        final String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cruise schemaVersion=\"61\">\n"
                + "    <server artifactsdir=\"artifacts\">\n"
                + "      <security>"
                + "        <ldap uri='some_url' managerDn='some_manager_dn' managerPassword='foo' searchBase='ou=Enterprise,ou=Principal,dc=corporate,dc=thoughtworks,dc=com' searchFilter='(sAMAccountName={0})' />"
                + "      </security>"
                + "    </server>"
                + " </cruise>";
        CruiseConfig config = migrateConfigAndLoadTheNewConfig(content, 61);

        LdapConfig ldapConfig = config.server().security().ldapConfig();
        BasesConfig basesConfig = ldapConfig.getBasesConfig();
        assertThat(basesConfig.size(), is(1));
        assertThat(basesConfig.first().getValue(), is("ou=Enterprise,ou=Principal,dc=corporate,dc=thoughtworks,dc=com"));
        assertThat(ldapConfig.searchFilter(), is("(sAMAccountName={0})"));
        assertThat(ldapConfig.uri(), is("some_url"));
        assertThat(ldapConfig.managerDn(), is("some_manager_dn"));
        assertThat(ldapConfig.managerPassword(), is("foo"));

    }

    @Test
    public void shouldMigrateFrom61_MigrateSearchBaseIntoAnElementAndOnlyOtherNecessaryFields() throws Exception {
        final String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cruise schemaVersion=\"61\">\n"
                + "    <server artifactsdir=\"artifacts\">\n"
                + "      <security>"
                + "        <ldap uri='some_url' searchBase='ou=Enterprise,ou=Principal,dc=corporate,dc=thoughtworks,dc=com'/>"
                + "      </security>"
                + "    </server>"
                + " </cruise>";
        CruiseConfig config = migrateConfigAndLoadTheNewConfig(content, 61);

        LdapConfig ldapConfig = config.server().security().ldapConfig();
        BasesConfig basesConfig = ldapConfig.getBasesConfig();
        assertThat(basesConfig.size(), is(1));
        assertThat(basesConfig.first().getValue(), is("ou=Enterprise,ou=Principal,dc=corporate,dc=thoughtworks,dc=com"));
        assertThat(ldapConfig.uri(), is("some_url"));
        assertThat(ldapConfig.searchFilter(), isEmptyString());
        assertThat(ldapConfig.managerDn(), isEmptyString());
        assertThat(ldapConfig.managerPassword(), isEmptyString());
        assertThat(ldapConfig.getEncryptedManagerPassword(), is(nullValue()));
    }

    @Test
    public void shouldMigrateFrom62_ToAddOnChangesAttributeToTimerWithDefaultValueOff() throws Exception {
        final String oldContent = ConfigFileFixture.configWithPipeline("<pipeline name='old-timer'>"
                + "  <timer>0 0 1 * * ?</timer>"
                + "  <materials>"
                + "    <git url='/tmp/git' />"
                + "  </materials>"
                + "  <stage name='dist'>"
                + "    <jobs>"
                + "      <job name='test' />"
                + "    </jobs>"
                + "  </stage>"
                + "</pipeline>", 62);
        CruiseConfig configAfterMigration = migrateConfigAndLoadTheNewConfig(oldContent, 62);
        String currentContent = FileUtils.readFileToString(new File(goConfigService.fileLocation()));

        PipelineConfig pipelineConfig = configAfterMigration.pipelineConfigByName(new CaseInsensitiveString("old-timer"));
        TimerConfig timer = pipelineConfig.getTimer();

        assertThat(configAfterMigration.schemaVersion(), is(greaterThan(62)));
        assertThat(timer.shouldTriggerOnlyOnChanges(), is(false));
        assertThat("Should not have added onChanges since its default value is false.", currentContent, not(containsString("onChanges")));
    }

    @Test
    public void forVersion63_shouldUseOnChangesWhileCreatingTimerConfigWhenItIsTrue() throws Exception {
        TimerConfig timer = createTimerConfigWithAttribute("onlyOnChanges='true'");

        assertThat(timer.shouldTriggerOnlyOnChanges(), is(true));
    }

    @Test
    public void forVersion63_shouldUseOnChangesWhileCreatingTimerConfigWhenItIsFalse() throws Exception {
        TimerConfig timer = createTimerConfigWithAttribute("onlyOnChanges='false'");

        assertThat(timer.shouldTriggerOnlyOnChanges(), is(false));
    }

    @Test
    public void forVersion63_shouldSetOnChangesToFalseWhileCreatingTimerConfigWhenTheWholeAttributeIsNotPresent() throws Exception {
        TimerConfig timer = createTimerConfigWithAttribute("");

        assertThat(timer.shouldTriggerOnlyOnChanges(), is(false));
    }

    @Test
    public void forVersion63_shouldFailWhenOnChangesValueIsEmpty() throws Exception {
        try {
            createTimerConfigWithAttribute("onlyOnChanges=''");
            fail("Didn't get the exception");
        } catch (Exception e) {
            assertThat(e.getCause().getCause().getMessage(), containsString("'' is not a valid value for 'boolean'"));
        }
    }

    @Test
    public void forVersion63_shouldFailWhenOnChangesValueIsNotAValidBooleanValue() throws Exception {
        try {
            createTimerConfigWithAttribute("onlyOnChanges='junk-non-boolean'");
            fail("Didn't get the exception");
        } catch (Exception e) {
            assertThat(e.getCause().getCause().getMessage(), containsString("'junk-non-boolean' is not a valid value for 'boolean'"));
        }
    }

    @Test
    public void shouldValidatePackageRepositoriesConfiguration() throws Exception {
        String configString =
                "<cruise schemaVersion='66'>"
                        + "<repositories>"
                        + "<repository id='go-repo' name='go-repo'>"
                        + "		<pluginConfiguration id='plugin-id' version='1.0'/>"
                        + "		<configuration>"
                        + "			<property><key>url</key><value>http://fake-yum-repo</value></property>"
                        + "			<property><key>username</key><value>godev</value></property>"
                        + "			<property><key>password</key><value>password</value></property>"
                        + "		</configuration>"
                        + "		<packages>"
                        + "			<package id='go-server' name='go-server'>"
                        + "				<configuration>"
                        + "					<property><key>name</key><value>go-server-13.2.0-1-i386</value></property>"
                        + "				</configuration>"
                        + "			</package>"
                        + "		</packages>"
                        + "</repository>"
                        + "</repositories>"
                        + "</cruise>";

        CruiseConfig cruiseConfig = migrateConfigAndLoadTheNewConfig(configString, 66);
        PackageRepositories packageRepositories = cruiseConfig.getPackageRepositories();
        assertThat(packageRepositories.size(), is(1));

        assertThat(packageRepositories.get(0).getId(), is("go-repo"));
        assertThat(packageRepositories.get(0).getName(), is("go-repo"));
        assertThat(packageRepositories.get(0).getPluginConfiguration().getId(), is("plugin-id"));
        assertThat(packageRepositories.get(0).getPluginConfiguration().getVersion(), is("1.0"));
        assertThat(packageRepositories.get(0).getConfiguration(), is(notNullValue()));
        assertThat(packageRepositories.get(0).getPackages().size(), is(1));

        assertConfiguration(packageRepositories.get(0).getConfiguration(),
                asList(new List[]{asList("url", Boolean.FALSE, "http://fake-yum-repo"), asList("username", Boolean.FALSE, "godev"), asList("password", Boolean.FALSE, "password")}));

        assertThat(packageRepositories.get(0).getPackages().get(0).getId(), is("go-server"));
        assertThat(packageRepositories.get(0).getPackages().get(0).getName(), is("go-server"));
        assertConfiguration(packageRepositories.get(0).getPackages().get(0).getConfiguration(),
                asList(new List[]{asList("name", Boolean.FALSE, "go-server-13.2.0-1-i386")}));

    }

    @Test
    public void shouldAllowOnlyRepositoryConfiguration() throws Exception {
        String configString =
                "<cruise schemaVersion='66'>"
                        + "<repositories>"
                        + "<repository id='go-repo' name='go-repo'>"
                        + "		<pluginConfiguration id='plugin-id' version='1.0'/>"
                        + "		<configuration>"
                        + "			<property><key>url</key><value>http://fake-yum-repo</value></property>"
                        + "			<property><key>username</key><value>godev</value></property>"
                        + "			<property><key>password</key><value>password</value></property>"
                        + "		</configuration>"
                        + "</repository>"
                        + "</repositories>"
                        + "</cruise>";
        CruiseConfig cruiseConfig = loadConfigFileWithContent(configString);
        PackageRepositories packageRepositories = cruiseConfig.getPackageRepositories();
        assertThat(packageRepositories.size(), is(1));

        assertThat(packageRepositories.get(0).getId(), is("go-repo"));
        assertThat(packageRepositories.get(0).getName(), is("go-repo"));
        assertThat(packageRepositories.get(0).getPluginConfiguration().getId(), is("plugin-id"));
        assertThat(packageRepositories.get(0).getPluginConfiguration().getVersion(), is("1.0"));
        assertThat(packageRepositories.get(0).getConfiguration(), is(notNullValue()));
        assertThat(packageRepositories.get(0).getPackages().size(), is(0));

        assertConfiguration(packageRepositories.get(0).getConfiguration(),
                asList(new List[]{asList("url", Boolean.FALSE, "http://fake-yum-repo"), asList("username", Boolean.FALSE, "godev"), asList("password", Boolean.FALSE, "password")}));

    }

    @Test
    public void shouldRemoveAllLuauConfigurationFromConfig() throws Exception {
        String configString =
                "<cruise schemaVersion='66'>"
                        + "<server siteUrl='https://hostname'>"
                        + "<security>"
                        + "      <luau url='https://luau.url.com' clientKey='0d010cf97ec505ee3788a9b5b8cf71d482c394ae88d32f0333' authState='authorized' />"
                        + "      <ldap uri='ldap' managerDn='managerDn' encryptedManagerPassword='+XhtUNvVAxJdHGF4qQGnWw==' searchFilter='(sAMAccountName={0})'>"
                        + "        <bases>"
                        + "          <base value='ou=Enterprise,ou=Principal,dc=corporate,dc=thoughtworks,dc=com' />"
                        + "        </bases>"
                        + "      </ldap>"
                        + "      <roles>"
                        + "         <role name='luau-role'><groups><luauGroup>luau-group</luauGroup></groups></role>"
                        + "         <role name='ldap-role'><users><user>some-user</user></users></role>"
                        + "</roles>"
                        + "</security>"
                        + "</server>"
                        + "</cruise>";

        String migratedContent = migrateXmlString(configString, 66);
        Document document = XmlUtils.buildXmlDocument(migratedContent);

        assertThat(document.getDescendants(new ElementFilter("luau")).hasNext(), is(false));
        assertThat(document.getDescendants(new ElementFilter("groups")).hasNext(), is(false));
    }

    @Test
    public void shouldAddAttributeAutoUpdateOnPackage_AsPartOfMigration68() throws Exception {
        String configString =
                "<cruise schemaVersion='67'>" +
                        "<repositories>" +
                        "	<repository id='2ef830d7-dd66-42d6-b393-64a84646e557' name='GoYumRepo'>" +
                        "		<pluginConfiguration id='yum' version='1' />" +
                        "       <configuration>" +
                        "           <property>" +
                        "               <key>REPO_URL</key>" +
                        "               <value>http://random-yum-repo/go/yum/no-arch</value>" +
                        "               </property>" +
                        "       </configuration>" +
                        "	    <packages>" +
                        "           <package id='88a3beca-cbe2-4c4d-9744-aa0cda3f371c' name='1'>" +
                        "               <configuration>" +
                        "                   <property>" +
                        "                       <key>REPO_URL</key>" +
                        "                       <value>http://random-yum-repo/go/yum/no-arch</value>" +
                        "                   </property>" +
                        "               </configuration>" +
                        "           </package>" +
                        "	     </packages>" +
                        "   </repository>" +
                        "</repositories>" +
                        "</cruise>";

        String migratedContent = migrateXmlString(configString, 67);
        GoConfigHolder holder = loader.loadConfigHolder(migratedContent);
        PackageRepository packageRepository = holder.config.getPackageRepositories().find("2ef830d7-dd66-42d6-b393-64a84646e557");
        PackageDefinition aPackage = packageRepository.findPackage("88a3beca-cbe2-4c4d-9744-aa0cda3f371c");
        assertThat(aPackage.isAutoUpdate(), is(true));
    }

    @Test
    public void shouldAllowAuthorizationUnderEachTemplate_asPartOfMigration69() throws Exception {
        String configString =
                "<cruise schemaVersion='69'>" +
                        "   <templates>" +
                        "       <pipeline name='template-name'>" +
                        "           <authorization>" +
                        "               <admins>" +
                        "                   <user>admin1</user>" +
                        "                   <user>admin2</user>" +
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

        String migratedContent = migrateXmlString(configString, 69);
        assertThat(migratedContent, containsString("<authorization>"));
        CruiseConfig configForEdit = loader.loadConfigHolder(migratedContent).configForEdit;
        PipelineTemplateConfig template = configForEdit.getTemplateByName(new CaseInsensitiveString("template-name"));
        Authorization authorization = template.getAuthorization();
        assertThat(authorization, is(not(nullValue())));
        assertThat(authorization.hasAdminsDefined(), is(true));
        assertThat(authorization.getAdminsConfig().getUsers(), hasItems(new AdminUser(new CaseInsensitiveString("admin1")), new AdminUser(new CaseInsensitiveString("admin2"))));
    }

    @Test
    public void shouldAllowPluggableTaskConfiguration_asPartOfMigration70() throws Exception {
        String configString =
                "<cruise schemaVersion='70'> <pipelines>"
                        + "<pipeline name='pipeline1'>"
                        + "    <materials>"
                        + "      <svn url='svnurl' username='admin' password='%s'/>"
                        + "    </materials>"
                        + "  <stage name='mingle'>"
                        + "    <jobs>"
                        + "      <job name='do-something'><tasks>"
                        + "        <task name='run-curl'>"
                        + "          <pluginConfiguration id='plugin-id' version='1.0' />"
                        + "          <configuration>"
                        + "            <property><key>url</key><value>http://fake-yum-repo</value></property>"
                        + "            <property><key>username</key><value>godev</value></property>"
                        + "            <property><key>password</key><value>password</value></property>"
                        + "          </configuration>"
                        + "        </task> </tasks>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline></pipelines>"
                        + "</cruise>";
        CruiseConfig cruiseConfig = loadConfigFileWithContent(configString);
        PipelineConfig pipelineConfig = cruiseConfig.getAllPipelineConfigs().get(0);
        JobConfig jobConfig = pipelineConfig.getFirstStageConfig().getJobs().get(0);
        Tasks tasks = jobConfig.getTasks();
        assertThat(tasks.size(),is(1));
        assertThat(tasks.get(0) instanceof PluggableTask, is(true));
    }

    @Test
    public void shouldRemoveNameFromPluggableTask_asPartOfMigration71() throws Exception {
        String oldConfigWithNameInTask =
                "<cruise schemaVersion='70'> <pipelines>"
                        + "<pipeline name='pipeline1'>"
                        + "    <materials>"
                        + "      <svn url='svnurl' username='admin' password='%s'/>"
                        + "    </materials>"
                        + "  <stage name='mingle'>"
                        + "    <jobs>"
                        + "      <job name='do-something'><tasks>"
                        + "        <task name='run-curl'>"
                        + "          <pluginConfiguration id='plugin-id' version='1.0' />"
                        + "          <configuration>"
                        + "            <property><key>url</key><value>http://fake-yum-repo</value></property>"
                        + "            <property><key>username</key><value>godev</value></property>"
                        + "            <property><key>password</key><value>password</value></property>"
                        + "          </configuration>"
                        + "      </task> </tasks>"
                        + "      </job>"
                        + "    </jobs>"
                        + "  </stage>"
                        + "</pipeline></pipelines>"
                        + "</cruise>";

        String newConfigWithoutNameInTask = migrateXmlString(oldConfigWithNameInTask, 70);
        assertThat(newConfigWithoutNameInTask, not(containsString("<task name")));
        assertThat(newConfigWithoutNameInTask, containsString("<task>"));

        CruiseConfig cruiseConfig = loadConfigFileWithContent(newConfigWithoutNameInTask);
        PipelineConfig pipelineConfig = cruiseConfig.getAllPipelineConfigs().get(0);
        JobConfig jobConfig = pipelineConfig.getFirstStageConfig().getJobs().get(0);

        Configuration configuration = new Configuration(
                create("url", false, "http://fake-yum-repo"),
                create("username", false, "godev"),
                create("password", false, "password"));

        Tasks tasks = jobConfig.getTasks();
        assertThat(tasks.size(),is(1));
        assertThat((PluggableTask) tasks.get(0), is(new PluggableTask(null, new PluginConfiguration("plugin-id", "1.0"), configuration)));
    }

    @Test
    public void shouldRemoveLicenseSection_asPartOfMigration72() throws Exception {
        String licenseUser = "Go UAT ThoughtWorks";
        String configWithLicenseSection =
                "<cruise schemaVersion='71'>"+
                    "<server artifactsdir=\"logs\" commandRepositoryLocation=\"default\" serverId=\"dev-id\">" +
                        "    <license user=\"" + licenseUser + "\">kTr+1ZBEr/5EiWlADIM6gUMtedtaLKPh6WRGp/2qISy1QczZpqJP5vmfydvx\n" +
                        "            Hq6o5X+nrb69sGOaBAvmjJ4cZBaIq+/4Yb+ufQCUM2DkacG/BjdEDpIoPHRA\n" +
                        "            fUnmjddxMnVKh2CW7gn7ZnmZUyasS9621UH2uNsfms3gfIK/1PRfbdrFuu5d\n" +
                        "            6xQEiEhjRVhKGFH4Uq2Cb0BVYCnQ+9eJ7WNwcV4pZCt1AoaMAxo4dox4NLpS\n" +
                        "            pKtgCp1Is/7ui+MGzKEyLCuO/LLMt0ChxWSN62vXiwdW3jl2HCEsLpb70FYR\n" +
                        "            Gj8eif3vuIB2rkOSvLkiAXqDFdEBEmb+GNV3nA4qOw==" +
                        "</license>\n" +
                    "  </server>" +
                "</cruise>";

        String migratedContent = migrateXmlString(configWithLicenseSection, 71);
        assertThat(migratedContent, not(containsString("license")));
        assertThat(migratedContent, not(containsString(licenseUser)));
    }

    @Test
    public void shouldPerformNOOPWhenNoLicenseIsPresent_asPartOfMigration72() throws Exception {
        String licenseUser = "Go UAT ThoughtWorks";
        String configWithLicenseSection =
                "<cruise schemaVersion='71'>"+
                    "<server artifactsdir=\"logs\" commandRepositoryLocation=\"default\" serverId=\"dev-id\">" +
                    "  </server>" +
                "</cruise>";

        String migratedContent = migrateXmlString(configWithLicenseSection, 71);
        assertThat(migratedContent, not(containsString("license")));
        assertThat(migratedContent, not(containsString(licenseUser)));
    }

    @Test
    public void shouldTrimLeadingAndTrailingWhitespaceFromCommands_asPartOfMigration73() throws Exception {
        String configXml =
            "<cruise schemaVersion='72'>" +
                "  <pipelines group='first'>" +
                "    <pipeline name='Test'>" +
                "      <materials>" +
                "        <hg url='../manual-testing/ant_hg/dummy' />" +
                "      </materials>" +
                "      <stage name='Functional'>" +
                "        <jobs>" +
                "          <job name='Functional'>" +
                "            <tasks>" +
                "              <exec command='  c:\\program files\\cmd.exe    ' args='arguments' />" +
                "            </tasks>" +
                "           </job>" +
                "        </jobs>" +
                "      </stage>" +
                "    </pipeline>" +
                "  </pipelines>" +
                "</cruise>";
        CruiseConfig migratedConfig = migrateConfigAndLoadTheNewConfig(configXml, 72);
        Task task = migratedConfig.tasksForJob("Test", "Functional", "Functional").get(0);
        assertThat(task, is(instanceOf(ExecTask.class)));
        assertThat((ExecTask) task, is(new ExecTask("c:\\program files\\cmd.exe", "arguments", (String) null)));
    }

    @Test
    public void shouldTrimLeadingAndTrailingWhitespaceFromCommandsInTemplates_asPartOfMigration73() throws Exception {
        String configXml =
            "<cruise schemaVersion='72'>" +
                "  <pipelines group='first'>" +
                "    <pipeline name='Test' template='test_template'>" +
                "      <materials>" +
                "        <hg url='../manual-testing/ant_hg/dummy' />" +
                "      </materials>" +
                "     </pipeline>" +
                "  </pipelines>" +
                "  <templates>" +
                "    <pipeline name='test_template'>" +
                "      <stage name='Functional'>" +
                "        <jobs>" +
                "          <job name='Functional'>" +
                "            <tasks>" +
                "              <exec command='  c:\\program files\\cmd.exe    ' args='arguments' />" +
                "            </tasks>" +
                "           </job>" +
                "        </jobs>" +
                "      </stage>" +
                "    </pipeline>" +
                "  </templates>" +
                "</cruise>";
        CruiseConfig migratedConfig = migrateConfigAndLoadTheNewConfig(configXml, 72);
        Task task = migratedConfig.tasksForJob("Test", "Functional", "Functional").get(0);
        assertThat(task, is(instanceOf(ExecTask.class)));
        assertThat((ExecTask) task, is(new ExecTask("c:\\program files\\cmd.exe", "arguments", (String) null)));
    }

    private void assertStringsIgnoringCarriageReturnAreEqual(String expected, String actual) {
        assertEquals(expected.replaceAll("\\r", ""), actual.replaceAll("\\r", ""));
    }

    private TimerConfig createTimerConfigWithAttribute(String valueForOnChangesInTimer) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final String content = ConfigFileFixture.configWithPipeline("<pipeline name='old-timer'>"
                + "  <timer " + valueForOnChangesInTimer + ">0 0 1 * * ?</timer>"
                + "  <materials>"
                + "    <git url='/tmp/git' />"
                + "  </materials>"
                + "  <stage name='dist'>"
                + "    <jobs>"
                + "      <job name='test' />"
                + "    </jobs>"
                + "  </stage>"
                + "</pipeline>", 63);
        CruiseConfig configAfterMigration = migrateConfigAndLoadTheNewConfig(content, 63);

        PipelineConfig pipelineConfig = configAfterMigration.pipelineConfigByName(new CaseInsensitiveString("old-timer"));
        return pipelineConfig.getTimer();
    }

    private File[] configFiles() {
        return configFile.getParentFile().listFiles(new FilenameFilter() {

            public boolean accept(File file, String name) {
                return name.startsWith("cruise-config.xml.");
            }
        }
        );
    }

    private CruiseConfig migrateConfigAndLoadTheNewConfig(String content, int fromVersion) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String migratedContent = migrateXmlString(content, fromVersion);
        GoConfigService.XmlPartialSaver fileSaver = goConfigService.fileSaver(true);
        GoConfigValidity configValidity = fileSaver.saveXml(migratedContent, goConfigService.configFileMd5());
        assertThat(configValidity.isValid(), is(true));
        return goConfigService.getCurrentConfig();
    }

    private String migrateXmlString(String content, int fromVersion) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return migrateXmlString(content, fromVersion, GoConfigSchema.currentSchemaVersion());
    }

    private String migrateXmlString(String content, int fromVersion, int toVersion) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final List<Exception> exs = new ArrayList<Exception>();
        GoConfigMigration upgrader = new GoConfigMigration(
                new GoConfigMigration.UpgradeFailedHandler() {
                    public void handle(Exception e) {
                        e.printStackTrace();
                        exs.add(e);
                    }
                }, configRepository, new TimeProvider(), configCache, ConfigElementImplementationRegistryMother.withNoPlugins(),
                metricsProbeService);
        Method upgrade = upgrader.getClass().getDeclaredMethod("upgrade", String.class, Integer.TYPE, Integer.TYPE);
        upgrade.setAccessible(true);
        return (String) upgrade.invoke(upgrader, content, fromVersion, toVersion);
    }

    private CruiseConfig loadConfigFileWithContent(String content) throws Exception {
        FileUtil.writeContentToFile(content, configFile);
        return loadWithMigration(configFile).config;
    }

    private GoConfigHolder loadWithMigration(final File configFile) throws Exception {
        GoConfigMigration migration = new GoConfigMigration(new GoConfigMigration.UpgradeFailedHandler() {
            public void handle(Exception e) {
                String content = "";
                try {
                    content = FileUtil.readContentFromFile(configFile);
                } catch (IOException e1) {
                }
                throw bomb(e.getMessage() + ": content=\n" + content, e);
            }
        }, configRepository, new TimeProvider(), configCache, registry,
                metricsProbeService);
        SystemEnvironment sysEnv = new SystemEnvironment();
        GoFileConfigDataSource configDataSource = new GoFileConfigDataSource(migration, configRepository, sysEnv, new TimeProvider(), configCache, serverVersion,
                registry, metricsProbeService, serverHealthService);
        configDataSource.upgradeIfNecessary();
        return configDataSource.forceLoad(configFile);
    }

    public void assertConfiguration(Configuration configuration, List<List> expectedKeyValuePair) {
        int position = 0;
        for (ConfigurationProperty configurationProperty : configuration) {
            assertThat(configurationProperty.getConfigurationKey().getName(), is(expectedKeyValuePair.get(position).get(0)));
            assertThat(configurationProperty.isSecure(), is(expectedKeyValuePair.get(position).get(1)));
            assertThat(configurationProperty.getConfigurationValue().getValue(), is(expectedKeyValuePair.get(position).get(2)));
            position++;
        }
    }
}
