/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.elastic.ElasticProfiles;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.security.ResetCipher;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.serverhealth.*;
import com.thoughtworks.go.service.ConfigRepository;
import com.thoughtworks.go.util.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.Document;
import org.jdom2.filter.ElementFilter;
import org.jdom2.input.SAXBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.thoughtworks.go.config.PipelineConfig.LOCK_VALUE_LOCK_ON_FAILURE;
import static com.thoughtworks.go.config.PipelineConfig.LOCK_VALUE_NONE;
import static com.thoughtworks.go.domain.config.CaseInsensitiveStringMother.str;
import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static com.thoughtworks.go.helper.ConfigFileFixture.pipelineWithAttributes;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml",
        "classpath:WEB-INF/spring-all-servlet.xml",
})
public class GoConfigMigrationIntegrationTest {
    private File configFile;
    ConfigRepository configRepository;
    @Autowired
    private SystemEnvironment systemEnvironment;
    @Autowired
    private ConfigCache configCache;
    @Autowired
    private ConfigElementImplementationRegistry registry;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private ServerHealthService serverHealthService;
    @Autowired
    private CachedGoPartials cachedGoPartials;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public ResetCipher resetCipher = new ResetCipher();

    private String currentGoServerVersion;
    private MagicalGoConfigXmlLoader loader;
    private String password;
    private String encryptedPassword;

    @Before
    public void setUp() throws Exception {
        File file = temporaryFolder.newFolder();
        configFile = new File(file, "cruise-config.xml");
        new SystemEnvironment().setProperty(SystemEnvironment.CONFIG_FILE_PROPERTY, configFile.getAbsolutePath());
        GoConfigFileHelper.clearConfigVersions();
        configRepository = new ConfigRepository(systemEnvironment);
        configRepository.initialize();
        serverHealthService.removeAllLogs();
        currentGoServerVersion = CurrentGoCDVersion.getInstance().formatted();
        loader = new MagicalGoConfigXmlLoader(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins());
        resetCipher.setupDESCipherFile();
        resetCipher.setupAESCipherFile();
        password = UUID.randomUUID().toString();
        encryptedPassword = new GoCipher().encrypt(password);
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
        assertThat(cruiseConfig.schemaVersion()).isEqualTo(GoConstants.CONFIG_SCHEMA_VERSION);
        assertThat(configRepository.getRevision(ConfigRepository.CURRENT).getUsername()).isNotEqualTo("Upgrade");
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
            assertThat(e).hasMessageContaining("Environment 'foo' refers to an unknown pipeline 'does_not_exist'");
        }
    }

    @Test
    public void shouldUpgradeCruiseConfigFileIfVersionDoesNotMatch() throws Exception {
        CruiseConfig cruiseConfig = loadConfigFileWithContent(ConfigFileFixture.OLD);
        assertThat(cruiseConfig.schemaVersion()).isEqualTo(GoConstants.CONFIG_SCHEMA_VERSION);
    }

    @Test
    public void shouldMigrateConfigContentAsAString() throws Exception {
        String newContent = new GoConfigMigration(configRepository, new TimeProvider(), configCache, ConfigElementImplementationRegistryMother.withNoPlugins())
                .upgradeIfNecessary(ConfigFileFixture.VERSION_0);
        assertThat(newContent).contains("schemaVersion=\"" + GoConfigSchema.currentSchemaVersion() + "\"");
    }

    @Test
    public void shouldNotMigrateConfigContentAsAStringWhenAlreadyUpToDate() throws Exception {
        GoConfigMigration configMigration = new GoConfigMigration(configRepository, new TimeProvider(), configCache,
                ConfigElementImplementationRegistryMother.withNoPlugins());
        String newContent = configMigration.upgradeIfNecessary(ConfigFileFixture.VERSION_0);
        assertThat(newContent).isEqualTo(configMigration.upgradeIfNecessary(newContent));
    }

    @Test
    public void shouldNotUpgradeInvalidConfigFileWhenThereIsNoValidConfigVersioned() throws Exception {
        try {
            assertThat(configRepository.getRevision(ConfigRepository.CURRENT)).isNull();
            loadConfigFileWithContent("<cruise></cruise>");
            fail("Should not upgrade invalid config file");
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("Cruise config file with version 0 is invalid. Unable to upgrade.");
        }
    }

    @Test
    public void shouldRevertConfigToTheLatestValidConfigVersionFromGitIfCurrentConfigIsInvalid() throws IOException {
        try {
            loadConfigFileWithContent(ConfigFileFixture.MINIMAL);
            loadConfigFileWithContent("<cruise></cruise>");
            ServerHealthStates states = serverHealthService.logs();
            assertThat(states.size()).isEqualTo(1);
            assertThat(states.get(0).getDescription()).contains("Go encountered an invalid configuration file while starting up. The invalid configuration file has been renamed to &lsquo;");
            assertThat(states.get(0).getDescription()).contains("&rsquo; and a new configuration file has been automatically created using the last good configuration.");
            assertThat(states.get(0).getMessage()).contains("Invalid Configuration");
            assertThat(states.get(0).getType()).isEqualTo(HealthStateType.general(HealthStateScope.forInvalidConfig()));
            assertThat(states.get(0).getLogLevel()).isEqualTo(HealthStateLevel.WARNING);
        } catch (Exception e) {
            fail("Should not Throw an exception, should revert to the last valid file versioned in config.git");
        }
    }

    @Test
    public void shouldTryToRevertConfigToTheLatestValidConfigVersionOnlyOnce() throws IOException {
        try {
            configRepository.checkin(new GoConfigRevision("<cruise></cruise>", "md5", "ps", "123", new TimeProvider()));
            loadConfigFileWithContent("<cruise></cruise>");
            ServerHealthStates states = serverHealthService.logs();
            assertThat(states.size()).isEqualTo(1);
            assertThat(states.get(0).getDescription()).contains("Go encountered an invalid configuration file while starting up. The invalid configuration file has been renamed to &lsquo;");
            assertThat(states.get(0).getDescription()).contains("&rsquo; and a new configuration file has been automatically created using the last good configuration.");
            assertThat(states.get(0).getMessage()).contains("Invalid Configuration");
            assertThat(states.get(0).getType()).isEqualTo(HealthStateType.general(HealthStateScope.forInvalidConfig()));
            assertThat(states.get(0).getLogLevel()).isEqualTo(HealthStateLevel.WARNING);
        } catch (Exception e) {
            assertThat(e).hasMessageContaining("Cruise config file with version 0 is invalid. Unable to upgrade.");
        }
    }

    @Test
    public void shouldMoveApprovalFromAPreviousStageToTheBeginningOfASecondStage() throws Exception {
        CruiseConfig cruiseConfig = loadConfigFileWithContent(ConfigFileFixture.VERSION_0);

        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline"));
        StageConfig firstStage = pipelineConfig.get(0);
        StageConfig secondStage = pipelineConfig.get(1);
        assertThat(firstStage.requiresApproval()).isEqualTo(Boolean.FALSE);
        assertThat(secondStage.requiresApproval()).isEqualTo(Boolean.TRUE);
    }

    @Test
    public void shouldMigrateApprovalsCorrectlyBug2112() throws Exception {
        File bjcruise = new File("../common/src/test/resources/data/bjcruise-cruise-config-1.0.xml");
        assertThat(bjcruise.exists()).isTrue();
        String xml = FileUtils.readFileToString(bjcruise, StandardCharsets.UTF_8);

        CruiseConfig cruiseConfig = loadConfigFileWithContent(xml);

        PipelineConfig pipeline = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("evolve"));

        StageConfig dbStage = pipeline.findBy(new CaseInsensitiveString("db"));
        assertThat(dbStage.requiresApproval()).isFalse();

        StageConfig installStage = pipeline.findBy(new CaseInsensitiveString("install"));
        assertThat(installStage.requiresApproval()).isTrue();
    }

    @Test
    public void shouldBackupOldConfigFileBeforeUpgrade() throws Exception {
        assertThat(configFiles().length).isEqualTo(0);
        loadConfigFileWithContent(ConfigFileFixture.OLD);
        assertThat(configFiles().length).isEqualTo(1);
    }

    @Test
    public void shouldMigrateMaterialFolderAttributeToDest() throws Exception {
        CruiseConfig cruiseConfig = loadConfigFileWithContent(ConfigFileFixture.VERSION_2);
        MaterialConfig actual = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("multiple")).materialConfigs().first();
        assertThat(actual.getFolder()).isEqualTo("part1");
    }

    @Test
    public void shouldMigrateRevision5ToTheLatest() throws Exception {
        CruiseConfig cruiseConfig = loadConfigFileWithContent(ConfigFileFixture.VERSION_5);
        assertThat(cruiseConfig.schemaVersion()).isEqualTo(GoConstants.CONFIG_SCHEMA_VERSION);
    }

    @Test
    public void shouldMigrateRevision7To8() throws Exception {
        CruiseConfig cruiseConfig = loadConfigFileWithContent(ConfigFileFixture.VERSION_7);
        HgMaterialConfig hgConfig = (HgMaterialConfig) cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("framework")).materialConfigs().first();
        assertThat(hgConfig.getFolder()).isNull();
        assertThat(hgConfig.filter()).isNotNull();
    }

    @Test
    public void shouldMigrateToRevision17() throws Exception {
        CruiseConfig cruiseConfig = loadConfigFileWithContent(ConfigFileFixture.WITH_3_AGENT_CONFIG);
        assertThat(cruiseConfig.agents().size()).isEqualTo(3);
        assertThat(cruiseConfig.agents().getAgentByUuid("2").isDisabled()).isTrue();
        assertThat(cruiseConfig.agents().getAgentByUuid("1").isDisabled()).isFalse();
    }

    @Test
    public void shouldMigrateDependsOnTagToBeADependencyMaterial() throws Exception {
        String content = FileUtils.readFileToString(
                new File("../common/src/test/resources/data/config/version4/cruise-config-dependency-migration.xml"), UTF_8);
        CruiseConfig cruiseConfig = loadConfigFileWithContent(content);
        MaterialConfig actual = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("depends")).materialConfigs().first();
        assertThat(actual).isInstanceOf(DependencyMaterialConfig.class);
        DependencyMaterialConfig depends = (DependencyMaterialConfig) actual;
        assertThat(depends.getPipelineName()).isEqualTo(new CaseInsensitiveString("multiple"));
        assertThat(depends.getStageName()).isEqualTo(new CaseInsensitiveString("helloworld-part2"));
    }

    @Test
    public void shouldFailIfJobsWithSameNameButDifferentCasesExistInConfig() throws IOException {
        final List<Exception> exs = new ArrayList<>();
        GoConfigMigration upgrader = new GoConfigMigration(
                new GoConfigMigration.UpgradeFailedHandler() {
                    public void handle(Exception e) {
                        exs.add(e);
                    }
                }, configRepository, new TimeProvider(), configCache, ConfigElementImplementationRegistryMother.withNoPlugins());
        FileUtils.writeStringToFile(configFile, ConfigFileFixture.JOBS_WITH_DIFFERNT_CASE, UTF_8);

        upgrader.upgradeIfNecessary(configFile, currentGoServerVersion);
        assertThat(exs.size()).isEqualTo(1);
        assertThat(exs.get(0).getMessage()).contains("You have defined multiple Jobs called 'Test'");
    }

    @Test
    public void shouldVersionControlAnUpgradedConfigIfItIsValid() throws Exception {
        GoConfigMigration upgrader = new GoConfigMigration(
                new GoConfigMigration.UpgradeFailedHandler() {
                    public void handle(Exception e) {
                        throw new AssertionError("upgrade failed!!!!!");
                    }
                }, configRepository, new TimeProvider(), configCache, ConfigElementImplementationRegistryMother.withNoPlugins());
        FileUtils.writeStringToFile(configFile, ConfigFileFixture.DEFAULT_XML_WITH_2_AGENTS, UTF_8);
        configRepository.checkin(new GoConfigRevision("dummy-content", "some-md5", "loser", "100.3.1", new TimeProvider()));

        upgrader.upgradeIfNecessary(configFile, currentGoServerVersion);

        GoConfigRevision latest = configRepository.getRevision(ConfigRepository.CURRENT);

        assertThat(latest.getUsername()).isEqualTo("Upgrade");

        String contents = FileUtils.readFileToString(configFile, UTF_8);
        assertThat(latest.getContent()).isEqualTo(contents);
        assertThat(latest.getMd5()).isEqualTo(DigestUtils.md5Hex(contents));
    }

    @Test
    public void shouldMigrateToRevision22() throws Exception {
        final String content = IOUtils.toString(getClass().getResourceAsStream("cruise-config-escaping-migration-test-fixture.xml"), UTF_8);

        String migratedContent = migrateXmlString(content, 21, 22);

        String expected = content.replaceAll("(?<!do_not_sub_)#", "##").replace("<cruise schemaVersion=\"21\">", "<cruise schemaVersion=\"22\">");
        assertStringsIgnoringCarriageReturnAreEqual(expected, migratedContent);
    }

    @Test
    public void shouldMigrateToRevision28() throws Exception {
        final String content = IOUtils.toString(getClass().getResourceAsStream("no-tracking-tool-group-holder-config.xml"), UTF_8);

        String migratedContent = migrateXmlString(content, 27);

        assertThat(migratedContent).contains("\"http://foo.bar/baz/${ID}\"");
        assertThat(migratedContent).contains("\"http://hello.world/${ID}/hello\"");
    }

    @Test
    public void shouldMigrateToRevision34() throws Exception {
        final String content = IOUtils.toString(getClass().getResourceAsStream("svn-p4-with-parameterized-passwords.xml"), UTF_8);

        String migratedContent = migrateXmlString(content, 22, 34);

        String expected = content.replaceAll("#\\{jez_passwd\\}", "badger")
                .replace("<cruise schemaVersion=\"22\">", "<cruise schemaVersion=\"34\">")
                .replaceAll("##", "#");
        assertStringsIgnoringCarriageReturnAreEqual(expected, migratedContent);
    }

    @Test
    public void shouldMigrateToRevision35_escapeHash() throws Exception {
        final String content = IOUtils.toString(getClass().getResourceAsStream("escape_param_for_nant_p4.xml"), UTF_8).trim();

        String migratedContent = migrateXmlString(content, 22, 35);

        String expected = content.replace("<cruise schemaVersion=\"22\">", "<cruise schemaVersion=\"35\">")
                .replace("<view>##foo#</view>", "<view>####foo##</view>").replace("nantpath=\"#foo##\"", "nantpath=\"##foo####\"");
        assertStringsIgnoringCarriageReturnAreEqual(expected, migratedContent);
    }

    @Test
    public void shouldMigrateToRevision58_deleteVMMS() throws Exception {
        String migratedContent = migrateXmlString(ConfigFileFixture.WITH_VMMS_CONFIG, 50, 58);

        assertThat(migratedContent.contains("vmms")).isFalse();
    }

    @Test
    public void shouldMigrateToRevision59_convertLogTypeToArtifact() throws Exception {
        String migratedContent = migrateXmlString(ConfigFileFixture.WITH_LOG_ARTIFACT_CONFIG, 50, 59);

        CruiseConfig cruiseConfig = loadConfigFileWithContent(migratedContent);

        ArtifactConfigs artifactConfigs = cruiseConfig.getAllPipelineConfigs().get(0).getStage(new CaseInsensitiveString("mingle")).getJobs().getJob(
                new CaseInsensitiveString("bluemonkeybutt")).artifactConfigs();

        assertThat(artifactConfigs.getBuiltInArtifactConfigs().get(0).getSource()).isEqualTo("from1");
        assertThat(artifactConfigs.getBuiltInArtifactConfigs().get(0).getDestination()).isEmpty();
        assertThat(artifactConfigs.getBuiltInArtifactConfigs().get(1).getSource()).isEqualTo("from2");
        assertThat(artifactConfigs.getBuiltInArtifactConfigs().get(1).getDestination()).isEqualTo("to2");
        assertThat(artifactConfigs.getBuiltInArtifactConfigs().get(2).getSource()).isEqualTo("from3");
        assertThat(artifactConfigs.getBuiltInArtifactConfigs().get(2).getDestination()).isEmpty();
        assertThat(artifactConfigs.getBuiltInArtifactConfigs().get(3).getSource()).isEqualTo("from4");
        assertThat(artifactConfigs.getBuiltInArtifactConfigs().get(3).getDestination()).isEqualTo("to4");
    }


    @Test
    public void shouldMigrateExecTaskArgValueToTextNode() throws Exception {
        String migratedContent = migrateXmlString(ConfigFileFixture.VALID_XML_3169, 14);
        assertThat(migratedContent).contains("<arg>test</arg>");
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
        String migratedContent = migrateXmlString(content, 22, 23);

        assertThat(migratedContent).contains("<pipeline isLocked=\"true\" name=\"in_env\">");
        assertThat(migratedContent).contains("<pipeline isLocked=\"false\" name=\"in_env_unLocked\">");
        assertThat(migratedContent).contains("<pipeline name=\"not_in_env\">");
    }

    @Test
    public void shouldEncryptPasswordsOnUpgradeIfNecessary() throws IOException {
        final List<Exception> exs = new ArrayList<>();
        GoConfigMigration upgrader = new GoConfigMigration(
                new GoConfigMigration.UpgradeFailedHandler() {
                    public void handle(Exception e) {
                        exs.add(e);
                    }
                }, configRepository, new TimeProvider(), configCache, ConfigElementImplementationRegistryMother.withNoPlugins());
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
        FileUtils.writeStringToFile(configFile, configContent, UTF_8);

        upgrader.upgradeIfNecessary(configFile, currentGoServerVersion);

        assertThat(FileUtils.readFileToString(configFile, UTF_8)).contains("encryptedPassword=");
        assertThat(FileUtils.readFileToString(configFile, UTF_8)).doesNotContain("password=");
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
        FileUtils.writeStringToFile(configFile, configContent, UTF_8);
        CruiseConfig cruiseConfig = loadWithMigration(configFile).config;

        RolesConfig roles = cruiseConfig.server().security().getRoles();
        assertThat(roles.size()).isEqualTo(2);
        assertThat(roles.get(0)).isEqualTo(new RoleConfig(new CaseInsensitiveString("bAr"),
                new RoleUser(new CaseInsensitiveString("quux")),
                new RoleUser(new CaseInsensitiveString("bang")),
                new RoleUser(new CaseInsensitiveString("LoSeR")),
                new RoleUser(new CaseInsensitiveString("baz"))));

        assertThat(roles.get(1)).isEqualTo(new RoleConfig(new CaseInsensitiveString("Foo"),
                new RoleUser(new CaseInsensitiveString("foo")),
                new RoleUser(new CaseInsensitiveString("LoSeR")),
                new RoleUser(new CaseInsensitiveString("bar"))));
    }

    @Test
    public void shouldAllowParamsInP4ServerAndPortField() throws IOException {
        final List<Exception> exs = new ArrayList<>();
        GoConfigMigration upgrader = new GoConfigMigration(
                new GoConfigMigration.UpgradeFailedHandler() {
                    public void handle(Exception e) {
                        exs.add(e);
                    }
                }, configRepository, new TimeProvider(), configCache, ConfigElementImplementationRegistryMother.withNoPlugins()
        );
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
        FileUtils.writeStringToFile(configFile, configContent, UTF_8);

        upgrader.upgradeIfNecessary(configFile, currentGoServerVersion);

        assertThat(FileUtils.readFileToString(configFile, UTF_8)).contains("port='#{param_foo}'");
    }

    @Test
    public void shouldIntroduceAWrapperTagForUsersOfRole() throws Exception {

        final List<Exception> exs = new ArrayList<>();
        GoConfigMigration upgrader = new GoConfigMigration(
                new GoConfigMigration.UpgradeFailedHandler() {
                    public void handle(Exception e) {
                        exs.add(e);
                    }
                }, configRepository, new TimeProvider(), configCache, ConfigElementImplementationRegistryMother.withNoPlugins());

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

        FileUtils.writeStringToFile(configFile, content, UTF_8);

        upgrader.upgradeIfNecessary(configFile, currentGoServerVersion);

        String configXml = FileUtils.readFileToString(configFile, UTF_8);

        MagicalGoConfigXmlLoader loader = new MagicalGoConfigXmlLoader(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins());
        GoConfigHolder configHolder = loader.loadConfigHolder(configXml);

        CruiseConfig config = configHolder.config;

        ServerConfig server = config.server();
        RolesConfig roles = server.security().getRoles();
        assertThat(roles).contains(new RoleConfig(new CaseInsensitiveString("admins"), new RoleUser(new CaseInsensitiveString("admin_one")), new RoleUser(new CaseInsensitiveString("admin_two"))));
        assertThat(roles).contains(new RoleConfig(new CaseInsensitiveString("devs"), new RoleUser(new CaseInsensitiveString("dev_one")), new RoleUser(new CaseInsensitiveString("dev_two")),
                new RoleUser(new CaseInsensitiveString("dev_three"))));
    }

    @Test
    public void shouldSetServerId_toARandomUUID_ifServerTagDoesntExist() {
        GoConfigService.XmlPartialSaver fileSaver = goConfigService.fileSaver(true);
        GoConfigValidity configValidity = fileSaver.saveXml("<cruise schemaVersion='" + 53 + "'>\n"
                + "</cruise>", goConfigService.configFileMd5());
        assertThat(configValidity.isValid()).as("Has error: " + configValidity.errorMessage()).isTrue();

        CruiseConfig config = goConfigService.getCurrentConfig();
        ServerConfig server = config.server();

        assertThat(server.getServerId().matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")).isTrue();
    }

    @Test
    public void shouldSetServerId_toARandomUUID_ifOneDoesntExist() {
        GoConfigService.XmlPartialSaver fileSaver = goConfigService.fileSaver(true);
        GoConfigValidity configValidity = fileSaver.saveXml("<cruise schemaVersion='" + 55 + "'>\n"
                + "<server artifactsdir=\"logs\" siteUrl=\"http://go-server-site-url:8153\" secureSiteUrl=\"https://go-server-site-url:8154\" jobTimeout=\"60\">\n"
                + "  </server>"
                + "</cruise>", goConfigService.configFileMd5());
        assertThat(configValidity.isValid()).as("Has error: " + configValidity.errorMessage()).isTrue();

        CruiseConfig config = goConfigService.getCurrentConfig();
        ServerConfig server = config.server();

        assertThat(server.getServerId().matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")).isTrue();
    }

    @Test
    public void shouldLoadServerId_ifOneExists() {
        GoConfigService.XmlPartialSaver fileSaver = goConfigService.fileSaver(true);
        GoConfigValidity configValidity = fileSaver.saveXml("<cruise schemaVersion='" + 55 + "'>\n"
                + "<server artifactsdir=\"logs\" siteUrl=\"http://go-server-site-url:8153\" secureSiteUrl=\"https://go-server-site-url:8154\" jobTimeout=\"60\" serverId=\"foo\">\n"
                + "  </server>"
                + "</cruise>", goConfigService.configFileMd5());
        assertThat(configValidity.isValid()).as("Has error: " + configValidity.errorMessage()).isTrue();

        CruiseConfig config = goConfigService.getCurrentConfig();
        ServerConfig server = config.server();

        assertThat(server.getServerId()).isEqualTo("foo");
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
        String currentContent = FileUtils.readFileToString(new File(goConfigService.fileLocation()), UTF_8);

        PipelineConfig pipelineConfig = configAfterMigration.pipelineConfigByName(new CaseInsensitiveString("old-timer"));
        TimerConfig timer = pipelineConfig.getTimer();

        assertThat(configAfterMigration.schemaVersion()).isGreaterThan(62);
        assertThat(timer.shouldTriggerOnlyOnChanges()).isFalse();
        assertThat(currentContent).as("Should not have added onChanges since its default value is false.").doesNotContain("onChanges");
    }

    @Test
    public void forVersion63_shouldUseOnChangesWhileCreatingTimerConfigWhenItIsTrue() throws Exception {
        TimerConfig timer = createTimerConfigWithAttribute("onlyOnChanges='true'");

        assertThat(timer.shouldTriggerOnlyOnChanges()).isTrue();
    }

    @Test
    public void forVersion63_shouldUseOnChangesWhileCreatingTimerConfigWhenItIsFalse() throws Exception {
        TimerConfig timer = createTimerConfigWithAttribute("onlyOnChanges='false'");

        assertThat(timer.shouldTriggerOnlyOnChanges()).isFalse();
    }

    @Test
    public void forVersion63_shouldSetOnChangesToFalseWhileCreatingTimerConfigWhenTheWholeAttributeIsNotPresent() throws Exception {
        TimerConfig timer = createTimerConfigWithAttribute("");

        assertThat(timer.shouldTriggerOnlyOnChanges()).isFalse();
    }

    @Test
    public void forVersion63_shouldFailWhenOnChangesValueIsEmpty() throws Exception {
        try {
            createTimerConfigWithAttribute("onlyOnChanges=''");
            fail("Didn't get the exception");
        } catch (Exception e) {
            assertThat(e.getCause().getCause()).hasMessageContaining("'' is not a valid value for 'boolean'");
        }
    }

    @Test
    public void forVersion63_shouldFailWhenOnChangesValueIsNotAValidBooleanValue() throws Exception {
        try {
            createTimerConfigWithAttribute("onlyOnChanges='junk-non-boolean'");
            fail("Didn't get the exception");
        } catch (Exception e) {
            assertThat(e.getCause().getCause()).hasMessageContaining("'junk-non-boolean' is not a valid value for 'boolean'");
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
        assertThat(packageRepositories.size()).isEqualTo(1);

        assertThat(packageRepositories.get(0).getId()).isEqualTo("go-repo");
        assertThat(packageRepositories.get(0).getName()).isEqualTo("go-repo");
        assertThat(packageRepositories.get(0).getPluginConfiguration().getId()).isEqualTo("plugin-id");
        assertThat(packageRepositories.get(0).getPluginConfiguration().getVersion()).isEqualTo("1.0");
        assertThat(packageRepositories.get(0).getConfiguration()).isNotNull();
        assertThat(packageRepositories.get(0).getPackages().size()).isEqualTo(1);

        assertConfiguration(packageRepositories.get(0).getConfiguration(),
                asList(new List[]{asList("url", Boolean.FALSE, "http://fake-yum-repo"), asList("username", Boolean.FALSE, "godev"), asList("password", Boolean.FALSE, "password")}));

        assertThat(packageRepositories.get(0).getPackages().get(0).getId()).isEqualTo("go-server");
        assertThat(packageRepositories.get(0).getPackages().get(0).getName()).isEqualTo("go-server");
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
        assertThat(packageRepositories.size()).isEqualTo(1);

        assertThat(packageRepositories.get(0).getId()).isEqualTo("go-repo");
        assertThat(packageRepositories.get(0).getName()).isEqualTo("go-repo");
        assertThat(packageRepositories.get(0).getPluginConfiguration().getId()).isEqualTo("plugin-id");
        assertThat(packageRepositories.get(0).getPluginConfiguration().getVersion()).isEqualTo("1.0");
        assertThat(packageRepositories.get(0).getConfiguration()).isNotNull();
        assertThat(packageRepositories.get(0).getPackages().size()).isEqualTo(0);

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
        Document document = new SAXBuilder().build(new StringReader(migratedContent));

        assertThat(document.getDescendants(new ElementFilter("luau")).hasNext()).isFalse();
        assertThat(document.getDescendants(new ElementFilter("groups")).hasNext()).isFalse();
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
        assertThat(aPackage.isAutoUpdate()).isTrue();
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
        assertThat(migratedContent).contains("<authorization>");
        CruiseConfig configForEdit = loader.loadConfigHolder(migratedContent).configForEdit;
        PipelineTemplateConfig template = configForEdit.getTemplateByName(new CaseInsensitiveString("template-name"));
        Authorization authorization = template.getAuthorization();
        assertThat(authorization).isNotNull();
        assertThat(authorization.hasAdminsDefined()).isTrue();
        assertThat(authorization.getAdminsConfig().getUsers()).contains(new AdminUser(new CaseInsensitiveString("admin1")), new AdminUser(new CaseInsensitiveString("admin2")));
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
        assertThat(tasks.size()).isEqualTo(1);
        assertThat(tasks.get(0) instanceof PluggableTask).isTrue();
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
        assertThat(newConfigWithoutNameInTask).doesNotContain("<task name");
        assertThat(newConfigWithoutNameInTask).contains("<task>");

        CruiseConfig cruiseConfig = loadConfigFileWithContent(newConfigWithoutNameInTask);
        PipelineConfig pipelineConfig = cruiseConfig.getAllPipelineConfigs().get(0);
        JobConfig jobConfig = pipelineConfig.getFirstStageConfig().getJobs().get(0);

        Configuration configuration = new Configuration(
                create("url", false, "http://fake-yum-repo"),
                create("username", false, "godev"),
                create("password", false, "password"));

        Tasks tasks = jobConfig.getTasks();
        assertThat(tasks.size()).isEqualTo(1);
        assertThat(tasks.get(0)).isEqualTo(new PluggableTask(new PluginConfiguration("plugin-id", "1.0"), configuration));
    }

    @Test
    public void shouldRemoveLicenseSection_asPartOfMigration72() throws Exception {
        String licenseUser = "Go UAT ThoughtWorks";
        String configWithLicenseSection =
                "<cruise schemaVersion='71'>" +
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
        assertThat(migratedContent).doesNotContain("license");
        assertThat(migratedContent).doesNotContain(licenseUser);
    }

    @Test
    public void shouldPerformNOOPWhenNoLicenseIsPresent_asPartOfMigration72() throws Exception {
        String licenseUser = "Go UAT ThoughtWorks";
        String configWithLicenseSection =
                "<cruise schemaVersion='71'>" +
                        "<server artifactsdir=\"logs\" commandRepositoryLocation=\"default\" serverId=\"dev-id\">" +
                        "  </server>" +
                        "</cruise>";

        String migratedContent = migrateXmlString(configWithLicenseSection, 71);
        assertThat(migratedContent).doesNotContain("license");
        assertThat(migratedContent).doesNotContain(licenseUser);
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
        assertThat(task).isInstanceOf(ExecTask.class);
        assertThat(task).isEqualTo(new ExecTask("c:\\program files\\cmd.exe", "arguments", (String) null));
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
        assertThat(task).isInstanceOf(ExecTask.class);
        assertThat(task).isEqualTo(new ExecTask("c:\\program files\\cmd.exe", "arguments", (String) null));
    }

    @Test
    public void shouldNotRemoveNonEmptyUserTags_asPartOfMigration78() throws Exception {
        String configXml =
                "<cruise schemaVersion='77'>"
                        + "  <pipelines group='first'>"
                        + "    <authorization>"
                        + "       <view>"
                        + "         <user>abc</user>"
                        + "       </view>"
                        + "    </authorization>"
                        + "    <pipeline name='Test' template='test_template'>"
                        + "      <materials>"
                        + "        <hg url='../manual-testing/ant_hg/dummy' />"
                        + "      </materials>"
                        + "     </pipeline>"
                        + "  </pipelines>"
                        + "</cruise>";
        String migratedXml = migrateXmlString(configXml, 77);
        assertThat(migratedXml).contains("<user>");
    }

    @Test
    public void shouldRemoveEmptyTags_asPartOfMigration78() throws Exception {
        String configXml =
                "<cruise schemaVersion='77'>"
                        + "  <pipelines group='first'>"
                        + "    <authorization>"
                        + "       <view>"
                        + "         <user>foo</user>"
                        + "         <user />"
                        + "         <user>        </user>"
                        + "       </view>"
                        + "       <operate>"
                        + "          <user></user>"
                        + "       </operate>"
                        + "    </authorization>"
                        + "    <pipeline name='Test' template='test_template'>"
                        + "      <materials>"
                        + "        <hg url='../manual-testing/ant_hg/dummy' />"
                        + "      </materials>"
                        + "     </pipeline>"
                        + "  </pipelines>"
                        + "</cruise>";
        String migratedXml = migrateXmlString(configXml, 77);
        assertThat(StringUtils.countMatches(migratedXml, "<user>")).isEqualTo(1);
    }

    @Test
    public void shouldRemoveEmptyTagsRecursively_asPartOfMigration78() throws Exception {
        String configXml =
                "<cruise schemaVersion='77'>"
                        + "  <pipelines group='first'>"
                        + "    <authorization>"
                        + "       <view>"
                        + "         <user></user>"
                        + "       </view>"
                        + "    </authorization>"
                        + "    <pipeline name='Test' template='test_template'>"
                        + "      <materials>"
                        + "        <hg url='../manual-testing/ant_hg/dummy' />"
                        + "      </materials>"
                        + "     </pipeline>"
                        + "  </pipelines>"
                        + "</cruise>";
        String migratedXml = migrateXmlString(configXml, 77);
        assertThat(migratedXml).doesNotContain("<user>");
        assertThat(migratedXml).doesNotContain("<view>");
        assertThat(migratedXml).doesNotContain("<authorization>");
    }

    @Test
    public void ShouldTrimEnvironmentVariables_asPartOfMigration85() throws Exception {
        String configXml = "<cruise schemaVersion='84'>"
                + "  <pipelines group='first'>"
                + "    <pipeline name='up42'>"
                + "      <environmentvariables>"
                + "        <variable name=\" test  \">"
                + "          <value>foobar</value>"
                + "        </variable>"
                + "        <variable name=\"   PATH \" secure=\"true\">\n" +
                "          <encryptedValue>trMHp15AjUE=</encryptedValue>\n" +
                "        </variable>"
                + "      </environmentvariables>"
                + "      <materials>"
                + "        <hg url='../manual-testing/ant_hg/dummy' />"
                + "      </materials>"
                + "  <stage name='dist'>"
                + "    <jobs>"
                + "      <job name='test' />"
                + "    </jobs>"
                + "  </stage>"
                + "     </pipeline>"
                + "  </pipelines>"
                + "</cruise>";
        CruiseConfig migratedConfig = migrateConfigAndLoadTheNewConfig(configXml, 84);
        PipelineConfig pipelineConfig = migratedConfig.pipelineConfigByName(new CaseInsensitiveString("up42"));
        EnvironmentVariablesConfig variables = pipelineConfig.getVariables();
        assertThat(variables.getPlainTextVariables().first().getName()).isEqualTo("test");
        assertThat(variables.getPlainTextVariables().first().getValue()).isEqualTo("foobar");
        assertThat(variables.getSecureVariables().first().getName()).isEqualTo("PATH");
        // encrypted value for "abcd" is "trMHp15AjUE=" for the cipher "269298bc31c44620"
        assertThat(variables.getSecureVariables().first().getValue()).isEqualTo("abcd");
    }

    @Test
    public void shouldCreateProfilesFromAgentConfig_asPartOfMigration86And87() throws Exception {
        String configXml = "<cruise schemaVersion='85'>"
                + "  <server serverId='dev-id'>"
                + "  </server>"
                + "  <pipelines group='first'>"
                + "    <pipeline name='up42'>"
                + "      <materials>"
                + "        <hg url='../manual-testing/ant_hg/dummy' />"
                + "      </materials>"
                + "  <stage name='dist'>"
                + "    <jobs>"
                + "      <job name='test'>"
                + "       <agentConfig pluginId='docker'>"
                + "         <property>"
                + "           <key>instance-type</key>"
                + "           <value>m1.small</value>"
                + "         </property>"
                + "       </agentConfig>"
                + "      </job>"
                + "    </jobs>"
                + "  </stage>"
                + "   </pipeline>"
                + "  </pipelines>"
                + "</cruise>";

        CruiseConfig migratedConfig = migrateConfigAndLoadTheNewConfig(configXml, 85);
        PipelineConfig pipelineConfig = migratedConfig.pipelineConfigByName(new CaseInsensitiveString("up42"));
        JobConfig jobConfig = pipelineConfig.getStages().get(0).getJobs().get(0);

        assertThat(migratedConfig.schemaVersion()).isGreaterThan(86);

        ElasticProfiles profiles = migratedConfig.getElasticConfig().getProfiles();
        assertThat(profiles.size()).isEqualTo(1);

        ElasticProfile expectedProfile = new ElasticProfile(jobConfig.getElasticProfileId(), "docker",
                new ConfigurationProperty(new ConfigurationKey("instance-type"), new ConfigurationValue("m1.small")));

        ElasticProfile elasticProfile = profiles.get(0);
        assertThat(elasticProfile).isEqualTo(expectedProfile);
    }

    @Test
    public void shouldCreateProfilesFromMultipleAgentConfigs_asPartOfMigration86And87() throws Exception {
        String configXml = "<cruise schemaVersion='85'>"
                + "  <server serverId='dev-id'>"
                + "  </server>"
                + "  <pipelines group='first'>"
                + "    <pipeline name='up42'>"
                + "      <materials>"
                + "        <hg url='../manual-testing/ant_hg/dummy' />"
                + "      </materials>"
                + "  <stage name='dist'>"
                + "    <jobs>"
                + "      <job name='test1'>"
                + "       <agentConfig pluginId='docker'>"
                + "         <property>"
                + "           <key>instance-type</key>"
                + "           <value>m1.small</value>"
                + "         </property>"
                + "       </agentConfig>"
                + "      </job>"
                + "      <job name='test2'>"
                + "       <agentConfig pluginId='aws'>"
                + "         <property>"
                + "           <key>ami</key>"
                + "           <value>some.ami</value>"
                + "         </property>"
                + "         <property>"
                + "           <key>ram</key>"
                + "           <value>1024</value>"
                + "         </property>"
                + "         <property>"
                + "           <key>diskSpace</key>"
                + "           <value>10G</value>"
                + "         </property>"
                + "       </agentConfig>"
                + "      </job>"
                + "    </jobs>"
                + "  </stage>"
                + "   </pipeline>"
                + "  </pipelines>"
                + "</cruise>";

        CruiseConfig migratedConfig = migrateConfigAndLoadTheNewConfig(configXml, 85);
        PipelineConfig pipelineConfig = migratedConfig.pipelineConfigByName(new CaseInsensitiveString("up42"));
        JobConfigs jobs = pipelineConfig.getStages().get(0).getJobs();

        ElasticProfiles profiles = migratedConfig.getElasticConfig().getProfiles();
        assertThat(profiles.size()).isEqualTo(2);

        ElasticProfile expectedDockerProfile = new ElasticProfile(jobs.get(0).getElasticProfileId(), "docker",
                new ConfigurationProperty(new ConfigurationKey("instance-type"), new ConfigurationValue("m1.small")));
        assertThat(profiles.get(0)).isEqualTo(expectedDockerProfile);

        ElasticProfile expectedAWSProfile = new ElasticProfile(jobs.get(1).getElasticProfileId(), "aws",
                new ConfigurationProperty(new ConfigurationKey("ami"), new ConfigurationValue("some.ami")),
                new ConfigurationProperty(new ConfigurationKey("ram"), new ConfigurationValue("1024")),
                new ConfigurationProperty(new ConfigurationKey("diskSpace"), new ConfigurationValue("10G")));
        assertThat(profiles.get(1)).isEqualTo(expectedAWSProfile);
    }

    @Test
    public void shouldCreateProfilesFromMultipleAgentConfigsAcrossStages_asPartOfMigration86And87() throws Exception {
        String configXml = "<cruise schemaVersion='85'>"
                + " <server serverId='dev-id'>"
                + " </server>"
                + " <pipelines group='first'>"
                + "   <pipeline name='up42'>"
                + "     <materials>"
                + "       <hg url='../manual-testing/ant_hg/dummy' />"
                + "     </materials>"
                + "  <stage name='build'>"
                + "    <jobs>"
                + "      <job name='test1'>"
                + "       <agentConfig pluginId='docker'>"
                + "         <property>"
                + "           <key>instance-type</key>"
                + "           <value>m1.small</value>"
                + "         </property>"
                + "       </agentConfig>"
                + "      </job>"
                + "      <job name='test2'>"
                + "       <agentConfig pluginId='aws'>"
                + "         <property>"
                + "           <key>ami</key>"
                + "           <value>some.ami</value>"
                + "         </property>"
                + "         <property>"
                + "           <key>ram</key>"
                + "           <value>1024</value>"
                + "         </property>"
                + "         <property>"
                + "           <key>diskSpace</key>"
                + "           <value>10G</value>"
                + "         </property>"
                + "       </agentConfig>"
                + "      </job>"
                + "    </jobs>"
                + "  </stage>"
                + "  <stage name='dist'>"
                + "    <jobs>"
                + "      <job name='package'>"
                + "       <agentConfig pluginId='docker'>"
                + "         <property>"
                + "           <key>instance-type</key>"
                + "           <value>m1.small</value>"
                + "         </property>"
                + "       </agentConfig>"
                + "      </job>"
                + "    </jobs>"
                + "  </stage>"
                + "   </pipeline>"
                + "  </pipelines>"
                + "</cruise>";

        CruiseConfig migratedConfig = migrateConfigAndLoadTheNewConfig(configXml, 85);
        PipelineConfig pipelineConfig = migratedConfig.pipelineConfigByName(new CaseInsensitiveString("up42"));
        JobConfigs buildJobs = pipelineConfig.getStages().get(0).getJobs();
        JobConfigs distJobs = pipelineConfig.getStages().get(1).getJobs();

        ElasticProfiles profiles = migratedConfig.getElasticConfig().getProfiles();
        assertThat(profiles.size()).isEqualTo(3);

        ElasticProfile expectedDockerProfile = new ElasticProfile(buildJobs.get(0).getElasticProfileId(), "docker",
                new ConfigurationProperty(new ConfigurationKey("instance-type"), new ConfigurationValue("m1.small")));
        assertThat(profiles.get(0)).isEqualTo(expectedDockerProfile);

        ElasticProfile expectedAWSProfile = new ElasticProfile(buildJobs.get(1).getElasticProfileId(), "aws",
                new ConfigurationProperty(new ConfigurationKey("ami"), new ConfigurationValue("some.ami")),
                new ConfigurationProperty(new ConfigurationKey("ram"), new ConfigurationValue("1024")),
                new ConfigurationProperty(new ConfigurationKey("diskSpace"), new ConfigurationValue("10G")));
        assertThat(profiles.get(1)).isEqualTo(expectedAWSProfile);

        ElasticProfile expectedSecondDockerProfile = new ElasticProfile(distJobs.get(0).getElasticProfileId(), "docker",
                new ConfigurationProperty(new ConfigurationKey("instance-type"), new ConfigurationValue("m1.small")));
        assertThat(profiles.get(2)).isEqualTo(expectedSecondDockerProfile);
    }

    @Test
    public void shouldCreateProfilesFromMultipleAgentConfigsAcrossPipelines_asPartOfMigration86And87() throws Exception {
        String configXml = "<cruise schemaVersion='85'>"
                + " <server serverId='dev-id'>"
                + " </server>"
                + " <pipelines group='first'>"
                + "   <pipeline name='up42'>"
                + "     <materials>"
                + "       <hg url='../manual-testing/ant_hg/dummy' />"
                + "     </materials>"
                + "  <stage name='build'>"
                + "    <jobs>"
                + "      <job name='test1'>"
                + "       <agentConfig pluginId='docker'>"
                + "         <property>"
                + "           <key>instance-type</key>"
                + "           <value>m1.small</value>"
                + "         </property>"
                + "       </agentConfig>"
                + "      </job>"
                + "    </jobs>"
                + "  </stage>"
                + "   </pipeline>"
                + "   <pipeline name='up43'>"
                + "     <materials>"
                + "       <hg url='../manual-testing/ant_hg/dummy' />"
                + "     </materials>"
                + "  <stage name='build'>"
                + "    <jobs>"
                + "      <job name='test2'>"
                + "       <agentConfig pluginId='aws'>"
                + "         <property>"
                + "           <key>ami</key>"
                + "           <value>some.ami</value>"
                + "         </property>"
                + "         <property>"
                + "           <key>ram</key>"
                + "           <value>1024</value>"
                + "         </property>"
                + "         <property>"
                + "           <key>diskSpace</key>"
                + "           <value>10G</value>"
                + "         </property>"
                + "       </agentConfig>"
                + "      </job>"
                + "    </jobs>"
                + "  </stage>"
                + "   </pipeline>"
                + "  </pipelines>"
                + "</cruise>";

        CruiseConfig migratedConfig = migrateConfigAndLoadTheNewConfig(configXml, 85);
        PipelineConfig up42 = migratedConfig.pipelineConfigByName(new CaseInsensitiveString("up42"));
        PipelineConfig up43 = migratedConfig.pipelineConfigByName(new CaseInsensitiveString("up43"));
        JobConfigs up42Jobs = up42.getStages().get(0).getJobs();
        JobConfigs up43Jobs = up43.getStages().get(0).getJobs();

        ElasticProfiles profiles = migratedConfig.getElasticConfig().getProfiles();
        assertThat(profiles.size()).isEqualTo(2);

        ElasticProfile expectedDockerProfile = new ElasticProfile(up42Jobs.get(0).getElasticProfileId(), "docker",
                new ConfigurationProperty(new ConfigurationKey("instance-type"), new ConfigurationValue("m1.small")));
        assertThat(profiles.get(0)).isEqualTo(expectedDockerProfile);

        ElasticProfile expectedAWSProfile = new ElasticProfile(up43Jobs.get(0).getElasticProfileId(), "aws",
                new ConfigurationProperty(new ConfigurationKey("ami"), new ConfigurationValue("some.ami")),
                new ConfigurationProperty(new ConfigurationKey("ram"), new ConfigurationValue("1024")),
                new ConfigurationProperty(new ConfigurationKey("diskSpace"), new ConfigurationValue("10G")));
        assertThat(profiles.get(1)).isEqualTo(expectedAWSProfile);
    }

    @Test
    public void shouldAddIdOnConfigRepoAsPartOfMigration94() throws Exception {
        String configXml = "<cruise schemaVersion='93'>" +
                "<config-repos>\n" +
                "   <config-repo plugin=\"json.config.plugin\">\n" +
                "     <git url=\"https://github.com/tomzo/gocd-json-config-example.git\" />\n" +
                "   </config-repo>\n" +
                "</config-repos>" +
                "</cruise>";

        String migratedContent = migrateXmlString(configXml, 93);
        assertThat(migratedContent).contains("id=");
    }

    @Test
    public void shouldConvertPluginToPluginIdOnConfigRepoAsPartOfMigration95() throws Exception {
        String configXml = "<cruise schemaVersion='94'>" +
                "<config-repos>\n" +
                "   <config-repo plugin=\"json.config.plugin\" id=\"config-repo-1\">\n" +
                "     <git url=\"https://github.com/tomzo/gocd-json-config-example.git\" />\n" +
                "   </config-repo>\n" +
                "</config-repos>" +
                "</cruise>";

        assertThat(configXml).doesNotContain("pluginId=\"json.config.plugin\"");
        String migratedContent = migrateXmlString(configXml, 94);
        assertThat(migratedContent).contains("pluginId=\"json.config.plugin\"");
    }

    @Test
    public void shouldConvertIsLockedAttributeToATristateNamedLockBehavior() throws Exception {
        String defaultPipeline = pipelineWithAttributes("name=\"default1\"", 97);
        String lockedPipeline = pipelineWithAttributes("name=\"locked1\" isLocked=\"true\"", 97);
        String unLockedPipeline = pipelineWithAttributes("name=\"unlocked1\" isLocked=\"false\"", 97);

        String defaultPipelineAfterMigration = pipelineWithAttributes("name=\"default1\"", 98);
        String lockedPipelineAfterMigration = pipelineWithAttributes("name=\"locked1\" lockBehavior=\"" + LOCK_VALUE_LOCK_ON_FAILURE + "\"", 98);
        String unLockedPipelineAfterMigration = pipelineWithAttributes("name=\"unlocked1\" lockBehavior=\"" + LOCK_VALUE_NONE + "\"", 98);

        assertStringsIgnoringCarriageReturnAreEqual(defaultPipelineAfterMigration, migrateXmlString(defaultPipeline, 97, 98));
        assertStringsIgnoringCarriageReturnAreEqual(lockedPipelineAfterMigration, migrateXmlString(lockedPipeline, 97, 98));
        assertStringsIgnoringCarriageReturnAreEqual(unLockedPipelineAfterMigration, migrateXmlString(unLockedPipeline, 97, 98));
    }

    @Test
    public void shouldNotSupportedUncesseryMaterialFieldsAsPartOfMigration99() throws Exception {
        String configXml = "<cruise schemaVersion='99'>" +
                "<config-repos>\n" +
                "   <config-repo pluginId=\"json.config.plugin\" id=\"config-repo-1\">\n" +
                "     <git url=\"https://github.com/tomzo/gocd-json-config-example.git\" dest=\"dest\"/>\n" +
                "   </config-repo>\n" +
                "</config-repos>" +
                "</cruise>";

        String message = "Attribute 'dest' is not allowed to appear in element 'git'.";

        try {
            migrateXmlString(configXml, 99);
            fail(String.format("Expected a failure. Reason: Cruise config file with version 98 is invalid. Unable to upgrade. Message:%s", message));
        } catch (InvocationTargetException e) {
            assertThat(e.getTargetException().getCause().getMessage()).isEqualTo(message);
        }
    }

    @Test
    public void migration99_shouldMigrateGitMaterialsUnderConfigRepoAndRetainOnlyTheMinimalRequiredAttributes() throws Exception {
        String configXml = "<cruise schemaVersion='98'>" +
                "<config-repos>\n" +
                "   <config-repo pluginId=\"json.config.plugin\" id=\"config-repo-1\">\n" +
                "      <git url=\"test-repo\" dest='dest' shallowClone='true' autoUpdate='true' invertFilter='true' materialName=\"foo\">\n" +
                "        <filter>\n" +
                "          <ignore pattern=\"asdsd\" />\n" +
                "        </filter>\n" +
                "      </git>" +
                "   </config-repo>\n" +
                "</config-repos>" +
                "</cruise>";

        assertThat(configXml).contains("<filter>");
        assertThat(configXml).contains("dest='dest'");
        assertThat(configXml).contains("autoUpdate='true'");
        assertThat(configXml).contains("invertFilter='true'");
        assertThat(configXml).contains("shallowClone='true'");

        String migratedContent = migrateXmlString(configXml, 98);
        CruiseConfig cruiseConfig = loader.deserializeConfig(migratedContent);
        GitMaterialConfig materialConfig = (GitMaterialConfig) cruiseConfig.getConfigRepos().getConfigRepo("config-repo-1").getMaterialConfig();

        assertThat(migratedContent).doesNotContain("<filter>");
        assertThat(migratedContent).doesNotContain("dest='dest'");
        assertThat(migratedContent).doesNotContain("invertFilter='true'");
        assertThat(migratedContent).doesNotContain("shallowClone='true'");

        assertThat(materialConfig.getFolder()).isNull();
        assertThat(materialConfig.filter().size()).isEqualTo(0);
        assertThat(materialConfig.isAutoUpdate()).isTrue();
        assertThat(materialConfig.isInvertFilter()).isFalse();
        assertThat(materialConfig.isShallowClone()).isFalse();
    }

    @Test
    public void migration99_shouldMigrateSvnMaterialsUnderConfigRepoAndRetainOnlyTheMinimalRequiredAttributes() throws Exception {
        String configXml = "<cruise schemaVersion='98'>" +
                "<config-repos>\n" +
                "   <config-repo pluginId=\"json.config.plugin\" id=\"config-repo-1\">\n" +
                "      <svn url=\"test-repo\" dest='dest' autoUpdate='true' checkexternals='false' invertFilter='true' materialName=\"foo\">\n" +
                "        <filter>\n" +
                "          <ignore pattern=\"asdsd\" />\n" +
                "        </filter>\n" +
                "      </svn>" +
                "   </config-repo>\n" +
                "</config-repos>" +
                "</cruise>";

        assertThat(configXml).contains("<filter>");
        assertThat(configXml).contains("dest='dest'");
        assertThat(configXml).contains("autoUpdate='true'");
        assertThat(configXml).contains("invertFilter='true'");

        String migratedContent = migrateXmlString(configXml, 98);
        CruiseConfig cruiseConfig = loader.deserializeConfig(migratedContent);
        MaterialConfig materialConfig = cruiseConfig.getConfigRepos().getConfigRepo("config-repo-1").getMaterialConfig();

        assertThat(migratedContent).doesNotContain("<filter>");
        assertThat(migratedContent).doesNotContain("dest='dest'");
        assertThat(migratedContent).doesNotContain("invertFilter='true'");

        assertThat(materialConfig.getFolder()).isNull();
        assertThat(materialConfig.filter().size()).isEqualTo(0);
        assertThat(materialConfig.isAutoUpdate()).isTrue();
        assertThat(materialConfig.isInvertFilter()).isFalse();
    }

    @Test
    public void migration99_shouldMigrateP4MaterialsUnderConfigRepoAndRetainOnlyTheMinimalRequiredAttributes() throws Exception {
        String configXml = "<cruise schemaVersion='98'>" +
                "<config-repos>\n" +
                "   <config-repo pluginId=\"json.config.plugin\" id=\"config-repo-1\">\n" +
                "      <p4 port=\"10.18.3.241:9999\" username=\"cruise\" password=\"password\" autoUpdate='true' invertFilter='true' dest=\"dest\">\n" +
                "          <view><![CDATA[//depot/dev/... //lumberjack/...]]></view>\n" +
                "        <filter>\n" +
                "          <ignore pattern=\"asdsd\" />\n" +
                "        </filter>\n" +
                "      </p4>" +
                "   </config-repo>\n" +
                "</config-repos>" +
                "</cruise>";

        assertThat(configXml).contains("dest=\"dest\"");
        assertThat(configXml).contains("<filter>");
        assertThat(configXml).contains("autoUpdate='true'");
        assertThat(configXml).contains("invertFilter='true'");

        String migratedContent = migrateXmlString(configXml, 98);
        CruiseConfig cruiseConfig = loader.deserializeConfig(migratedContent);
        MaterialConfig materialConfig = cruiseConfig.getConfigRepos().getConfigRepo("config-repo-1").getMaterialConfig();

        assertThat(migratedContent).doesNotContain("dest=\"dest\"");
        assertThat(migratedContent).doesNotContain("<filter>");
        assertThat(migratedContent).doesNotContain("invertFilter='true'");

        assertThat(materialConfig.getFolder()).isNull();
        assertThat(materialConfig.filter().size()).isEqualTo(0);
        assertThat(materialConfig.isAutoUpdate()).isTrue();
        assertThat(materialConfig.isInvertFilter()).isFalse();
    }

    @Test
    public void migration99_shouldMigrateHgMaterialsUnderConfigRepoAndRetainOnlyTheMinimalRequiredAttributes() throws Exception {
        String configXml = "<cruise schemaVersion='98'>" +
                "<config-repos>\n" +
                "   <config-repo pluginId=\"json.config.plugin\" id=\"config-repo-1\">\n" +
                "      <hg url=\"test-repo\" dest='dest' autoUpdate='true' invertFilter='true' materialName=\"foo\">\n" +
                "        <filter>\n" +
                "          <ignore pattern=\"asdsd\" />\n" +
                "        </filter>\n" +
                "      </hg>" +
                "   </config-repo>\n" +
                "</config-repos>" +
                "</cruise>";

        assertThat(configXml).contains("<filter>");
        assertThat(configXml).contains("dest='dest'");
        assertThat(configXml).contains("autoUpdate='true'");
        assertThat(configXml).contains("invertFilter='true'");

        String migratedContent = migrateXmlString(configXml, 98);
        CruiseConfig cruiseConfig = loader.deserializeConfig(migratedContent);
        MaterialConfig materialConfig = cruiseConfig.getConfigRepos().getConfigRepo("config-repo-1").getMaterialConfig();

        assertThat(migratedContent).doesNotContain("<filter>");
        assertThat(migratedContent).doesNotContain("dest='dest'");
        assertThat(migratedContent).doesNotContain("invertFilter='true'");

        assertThat(materialConfig.getFolder()).isNull();
        assertThat(materialConfig.filter().size()).isEqualTo(0);
        assertThat(materialConfig.isAutoUpdate()).isTrue();
        assertThat(materialConfig.isInvertFilter()).isFalse();
    }

    @Test
    public void migration99_shouldMigrateTfsMaterialsUnderConfigRepoAndRetainOnlyTheMinimalRequiredAttributes() throws Exception {
        String configXml = "<cruise schemaVersion='98'>" +
                "<config-repos>\n" +
                "   <config-repo pluginId=\"json.config.plugin\" id=\"config-repo-1\">\n" +
                "      <tfs url='tfsurl' dest='dest' autoUpdate='true' invertFilter='true' username='foo' password='bar' projectPath='project-path'>\n" +
                "        <filter>\n" +
                "          <ignore pattern=\"asdsd\" />\n" +
                "        </filter>\n" +
                "      </tfs>" +
                "   </config-repo>\n" +
                "</config-repos>" +
                "</cruise>";

        assertThat(configXml).contains("<filter>");
        assertThat(configXml).contains("dest='dest'");
        assertThat(configXml).contains("autoUpdate='true'");
        assertThat(configXml).contains("invertFilter='true'");

        String migratedContent = migrateXmlString(configXml, 98);
        CruiseConfig cruiseConfig = loader.deserializeConfig(migratedContent);
        MaterialConfig materialConfig = cruiseConfig.getConfigRepos().getConfigRepo("config-repo-1").getMaterialConfig();

        assertThat(migratedContent).doesNotContain("<filter>");
        assertThat(migratedContent).doesNotContain("dest='dest'");
        assertThat(migratedContent).doesNotContain("invertFilter='true'");

        assertThat(materialConfig.getFolder()).isNull();
        assertThat(materialConfig.filter().size()).isEqualTo(0);
        assertThat(materialConfig.isAutoUpdate()).isTrue();
        assertThat(materialConfig.isInvertFilter()).isFalse();
    }

    @Test
    public void migration99_shouldMigrateScmMaterialsUnderConfigRepoAndRetainOnlyTheMinimalRequiredAttributes() throws Exception {
        String configXml = "<cruise schemaVersion='98'>" +
                "<config-repos>\n" +
                "   <config-repo pluginId=\"json.config.plugin\" id=\"config-repo-1\">\n" +
                "      <scm ref='some-ref' dest='dest'>\n" +
                "        <filter>\n" +
                "          <ignore pattern=\"asdsd\" />\n" +
                "        </filter>\n" +
                "      </scm>" +
                "   </config-repo>\n" +
                "</config-repos>" +
                "</cruise>";

        assertThat(configXml).contains("<filter>");
        assertThat(configXml).contains("dest='dest'");

        String migratedContent = migrateXmlString(configXml, 98);
        CruiseConfig cruiseConfig = loader.deserializeConfig(migratedContent);
        MaterialConfig materialConfig = cruiseConfig.getConfigRepos().getConfigRepo("config-repo-1").getMaterialConfig();

        assertThat(migratedContent).doesNotContain("<filter>");
        assertThat(migratedContent).doesNotContain("dest='dest'");

        assertThat(materialConfig.getFolder()).isNull();
        assertThat(materialConfig.filter().size()).isEqualTo(0);

    }

    @Test
    public void shouldAddTokenGenerationKeyAttributeOnServerAsPartOf99To100Migration() throws Exception {
        String configXml = "<cruise schemaVersion='99'><server artifactsdir=\"artifacts\" agentAutoRegisterKey=\"041b5c7e-dab2-11e5-a908-13f95f3c6ef6\" webhookSecret=\"5f8b5eac-1148-4145-aa01-7b2934b6e1ab\" commandRepositoryLocation=\"default\" serverId=\"dev-id\">\n" +
                "    <security>\n" +
                "      <authConfigs>\n" +
                "        <authConfig id=\"9cad79b0-4d9e-4a62-829c-eb4d9488062f\" pluginId=\"cd.go.authentication.passwordfile\">\n" +
                "          <property>\n" +
                "            <key>PasswordFilePath</key>\n" +
                "            <value>../manual-testing/ant_hg/password.properties</value>\n" +
                "          </property>\n" +
                "        </authConfig>\n" +
                "      </authConfigs>\n" +
                "      <roles>\n" +
                "        <role name=\"xyz\" />\n" +
                "      </roles>\n" +
                "      <admins>\n" +
                "        <user>admin</user>\n" +
                "      </admins>\n" +
                "    </security>\n" +
                "  </server>" +
                "</cruise>";

        final CruiseConfig cruiseConfig = migrateConfigAndLoadTheNewConfig(configXml, 99);
        assertThat(StringUtils.isNotBlank(cruiseConfig.server().getTokenGenerationKey())).isTrue();
    }

    @Test
    public void shouldMigrateElasticProfilesOutOfServerConfig_asPartOf100To101Migration() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String configXml = "<cruise schemaVersion='100'>" +
                "<server artifactsdir=\"artifactsDir\" agentAutoRegisterKey=\"041b5c7e-dab2-11e5-a908-13f95f3c6ef6\" webhookSecret=\"5f8b5eac-1148-4145-aa01-7b2934b6e1ab\" commandRepositoryLocation=\"default\" serverId=\"dev-id\">\n" +
                "<elastic jobStarvationTimeout=\"3\">\n" +
                "      <profiles>\n" +
                "        <profile id=\"dev-build\" pluginId=\"cd.go.contrib.elastic-agent.docker-swarm\">\n" +
                "          <property>\n" +
                "            <key>Image</key>\n" +
                "            <value>bar</value>\n" +
                "          </property>\n" +
                "          <property>\n" +
                "            <key>ReservedMemory</key>\n" +
                "            <value>3GB</value>\n" +
                "          </property>\n" +
                "          <property>\n" +
                "            <key>MaxMemory</key>\n" +
                "            <value>3GB</value>\n" +
                "          </property>\n" +
                "        </profile>\n" +
                "      </profiles>\n" +
                "    </elastic>\n" +
                "    <security allowOnlyKnownUsersToLogin=\"true\"></security>\n" +
                "  </server>\n" +
                "  <scms>\n" +
                "    <scm id=\"c0758880-10f7-4f38-a0b0-f3dc31e5d907\" name=\"gocd\">\n" +
                "      <pluginConfiguration id=\"github.pr\" version=\"1\"/>\n" +
                "      <configuration>\n" +
                "        <property>\n" +
                "          <key>url</key>\n" +
                "          <value>https://foo/bar</value>\n" +
                "        </property>\n" +
                "      </configuration>\n" +
                "    </scm>\n" +
                "  </scms>" +
                "</cruise>";

        final CruiseConfig cruiseConfig = migrateConfigAndLoadTheNewConfig(configXml, 100);
        assertThat(cruiseConfig.getElasticConfig()).isNotNull();
        assertThat(cruiseConfig.server().getAgentAutoRegisterKey()).isEqualTo("041b5c7e-dab2-11e5-a908-13f95f3c6ef6");
        assertThat(cruiseConfig.server().getWebhookSecret()).isEqualTo("5f8b5eac-1148-4145-aa01-7b2934b6e1ab");
        assertThat(cruiseConfig.server().getCommandRepositoryLocation()).isEqualTo("default");
        assertThat(cruiseConfig.server().artifactsDir()).isEqualTo("artifactsDir");
        assertThat(cruiseConfig.getElasticConfig().getProfiles()).hasSize(1);
        assertThat(cruiseConfig.getElasticConfig().getProfiles().get(0)).isEqualTo(new ElasticProfile("dev-build", "cd.go.contrib.elastic-agent.docker-swarm",
                ConfigurationPropertyMother.create("Image", false, "bar"),
                ConfigurationPropertyMother.create("ReservedMemory", false, "3GB"),
                ConfigurationPropertyMother.create("MaxMemory", false, "3GB")
        ));
    }

    @Test
    public void shouldRetainAllOtherServerConfigElements_asPartOf100To101Migration() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String configXml = "<cruise schemaVersion='100'>" +
                "<server artifactsdir=\"artifactsDir\" agentAutoRegisterKey=\"041b5c7e-dab2-11e5-a908-13f95f3c6ef6\" webhookSecret=\"5f8b5eac-1148-4145-aa01-7b2934b6e1ab\" commandRepositoryLocation=\"default\" serverId=\"dev-id\">\n" +
                "<elastic jobStarvationTimeout=\"3\">\n" +
                "      <profiles>\n" +
                "        <profile id=\"dev-build\" pluginId=\"cd.go.contrib.elastic-agent.docker-swarm\">\n" +
                "          <property>\n" +
                "            <key>Image</key>\n" +
                "            <value>bar</value>\n" +
                "          </property>\n" +
                "          <property>\n" +
                "            <key>ReservedMemory</key>\n" +
                "            <value>3GB</value>\n" +
                "          </property>\n" +
                "          <property>\n" +
                "            <key>MaxMemory</key>\n" +
                "            <value>3GB</value>\n" +
                "          </property>\n" +
                "        </profile>\n" +
                "      </profiles>\n" +
                "    </elastic>\n" +
                "    <security allowOnlyKnownUsersToLogin=\"true\"></security>\n" +
                "  </server>\n" +
                "  <scms>\n" +
                "    <scm id=\"c0758880-10f7-4f38-a0b0-f3dc31e5d907\" name=\"gocd\">\n" +
                "      <pluginConfiguration id=\"github.pr\" version=\"1\"/>\n" +
                "      <configuration>\n" +
                "        <property>\n" +
                "          <key>url</key>\n" +
                "          <value>https://foo/bar</value>\n" +
                "        </property>\n" +
                "      </configuration>\n" +
                "    </scm>\n" +
                "  </scms>" +
                "</cruise>";

        final CruiseConfig cruiseConfig = migrateConfigAndLoadTheNewConfig(configXml, 100);
        assertThat(cruiseConfig.server().getAgentAutoRegisterKey()).isEqualTo("041b5c7e-dab2-11e5-a908-13f95f3c6ef6");
        assertThat(cruiseConfig.server().getWebhookSecret()).isEqualTo("5f8b5eac-1148-4145-aa01-7b2934b6e1ab");
        assertThat(cruiseConfig.server().getCommandRepositoryLocation()).isEqualTo("default");
        assertThat(cruiseConfig.server().artifactsDir()).isEqualTo("artifactsDir");
        assertThat(cruiseConfig.server().security()).isEqualTo(new SecurityConfig(true));
        assertThat(cruiseConfig.getSCMs()).hasSize(1);
    }

    @Test
    public void shouldSkipParamResoulutionForElasticConfig_asPartOf100To101Migration() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String configXml = "<cruise schemaVersion='100'>" +
                "<server artifactsdir=\"artifactsDir\" agentAutoRegisterKey=\"041b5c7e-dab2-11e5-a908-13f95f3c6ef6\" webhookSecret=\"5f8b5eac-1148-4145-aa01-7b2934b6e1ab\" commandRepositoryLocation=\"default\" serverId=\"dev-id\">\n" +
                "<elastic jobStarvationTimeout=\"3\">\n" +
                "      <profiles>\n" +
                "        <profile id=\"dev-build\" pluginId=\"cd.go.contrib.elastic-agent.docker-swarm\">\n" +
                "          <property>\n" +
                "            <key>Image</key>\n" +
                "            <value>#bar</value>\n" +
                "          </property>\n" +
                "        </profile>\n" +
                "      </profiles>\n" +
                "    </elastic>\n" +
                "  </server>\n" +
                "</cruise>";

        final CruiseConfig cruiseConfig = migrateConfigAndLoadTheNewConfig(configXml, 100);
        assertThat(cruiseConfig.getElasticConfig().getProfiles().get(0)).isEqualTo(new ElasticProfile("dev-build", "cd.go.contrib.elastic-agent.docker-swarm",
                ConfigurationPropertyMother.create("Image", false, "#bar")
        ));
    }


    @Test
    public void shouldRemoveAgentWithDuplicateElasticAgentId_asPartOf102To103Migration() throws Exception {
        String configXml = "<cruise schemaVersion='102'>" +
                "<agents>\n" +
                "    <agent hostname=\"hostname\" ipaddress=\"127.0.0.1\" uuid=\"c46a08a7-921c-4e77-b748-6128975a3e7d\" elasticAgentId=\"16649813-4cb3-4682-8702-8e202824dd73\" elasticPluginId=\"elastic-plugin-id\" />\n" +
                "    <agent hostname=\"hostname\" ipaddress=\"127.0.0.1\" uuid=\"c46a08a7-921c-4e77-b748-6128975a3e7e\" elasticAgentId=\"16649813-4cb3-4682-8702-8e202824dd73\" elasticPluginId=\"elastic-plugin-id\" />\n" +
                "    <agent hostname=\"hostname\" ipaddress=\"127.0.0.1\" uuid=\"537d36f9-bf4b-48b2-8d09-5d20357d4f16\" elasticAgentId=\"a38d2559-0703-4e69-a30d-a21245d740af\" elasticPluginId=\"elastic-plugin-id\" />\n" +
                "    <agent hostname=\"hostname\" ipaddress=\"127.0.0.1\" uuid=\"c46a08a7-921c-4e77-b748-6128975a3e7f\" elasticAgentId=\"16649813-4cb3-4682-8702-8e202824dd73\" elasticPluginId=\"elastic-plugin-id\" />\n" +
                "    <agent hostname=\"hostname\" ipaddress=\"127.0.0.1\" uuid=\"537d36f9-bf4b-48b2-8d09-5d20357d4f17\" elasticAgentId=\"a38d2559-0703-4e69-a30d-a21245d740af\" elasticPluginId=\"elastic-plugin-id\" />\n" +
                "  </agents>" +
                "</cruise>";

        //before migration should contain 5 elastic agents 3 duplicates
        assertThat(configXml).contains("c46a08a7-921c-4e77-b748-6128975a3e7d");
        assertThat(configXml).contains("537d36f9-bf4b-48b2-8d09-5d20357d4f16");
        assertThat(configXml).contains("c46a08a7-921c-4e77-b748-6128975a3e7e");
        assertThat(configXml).contains("c46a08a7-921c-4e77-b748-6128975a3e7f");
        assertThat(configXml).contains("537d36f9-bf4b-48b2-8d09-5d20357d4f17");

        String migratedContent = migrateXmlString(configXml, 102);

        //after migration should contain 2 unique elastic agents
        assertThat(configXml).contains("c46a08a7-921c-4e77-b748-6128975a3e7d");
        assertThat(configXml).contains("537d36f9-bf4b-48b2-8d09-5d20357d4f16");

        //after migration should remove 3 duplicate elastic agents
        assertThat(migratedContent).doesNotContain("c46a08a7-921c-4e77-b748-6128975a3e7e");
        assertThat(migratedContent).doesNotContain("c46a08a7-921c-4e77-b748-6128975a3e7f");
        assertThat(migratedContent).doesNotContain("537d36f9-bf4b-48b2-8d09-5d20357d4f17");
    }

    @Test
    public void shouldAddHttpPrefixToTrackingToolUrlsIfProtocolNotPresent() throws Exception {
        String configXml = "<cruise schemaVersion='104'>\n" +
                "<pipelines group='first'>" +
                "    <pipeline name='up42'>\n" +
                "      <trackingtool link='github.com/gocd/gocd/issues/${ID}' regex='##(\\d+)'/>" +
                "      <materials>\n" +
                "        <git url='test-repo' />\n" +
                "      </materials>\n" +
                "      <stage name='up42_stage'>\n" +
                "        <jobs>\n" +
                "          <job name='up42_job'>\n" +
                "            <tasks>\n" +
                "              <exec command='ls' />\n" +
                "            </tasks>\n" +
                "          </job>\n" +
                "        </jobs>\n" +
                "      </stage>\n" +
                "    </pipeline>\n" +
                "    <pipeline name='up43'>\n" +
                "      <trackingtool link='https://github.com/gocd/gocd/issues/${ID}' regex='##(\\d+)'/>" +
                "      <materials>\n" +
                "        <git url='test-repo' />\n" +
                "      </materials>\n" +
                "      <stage name='up43_stage'>\n" +
                "        <jobs>\n" +
                "          <job name='up43_job'>\n" +
                "            <tasks>\n" +
                "              <exec command='ls' />\n" +
                "            </tasks>\n" +
                "          </job>\n" +
                "        </jobs>\n" +
                "      </stage>\n" +
                "    </pipeline>\n" +
                "  </pipelines>" +
                "<pipelines group='second'>" +
                "    <pipeline name='up12'>\n" +
                "      <trackingtool link='http://github.com/gocd/gocd/issues/${ID}' regex='##(\\d+)'/>" +
                "      <materials>\n" +
                "        <git url='test-repo' />\n" +
                "      </materials>\n" +
                "      <stage name='up42_stage'>\n" +
                "        <jobs>\n" +
                "          <job name='up42_job'>\n" +
                "            <tasks>\n" +
                "              <exec command='ls' />\n" +
                "            </tasks>\n" +
                "          </job>\n" +
                "        </jobs>\n" +
                "      </stage>\n" +
                "    </pipeline>\n" +
                "    <pipeline name='up13'>\n" +
                "      <trackingtool link='github.com/gocd/gocd/issues/${ID}' regex='##(\\d+)'/>" +
                "      <materials>\n" +
                "        <git url='test-repo' />\n" +
                "      </materials>\n" +
                "      <stage name='up43_stage'>\n" +
                "        <jobs>\n" +
                "          <job name='up43_job'>\n" +
                "            <tasks>\n" +
                "              <exec command='ls' />\n" +
                "            </tasks>\n" +
                "          </job>\n" +
                "        </jobs>\n" +
                "      </stage>\n" +
                "    </pipeline>\n" +
                "  </pipelines>" +
                "</cruise>\n";

        final CruiseConfig cruiseConfig = migrateConfigAndLoadTheNewConfig(configXml, 104);

        assertThat(cruiseConfig.pipelines("first").findBy(str("up42")).getTrackingTool().getLink()).isEqualTo("http://github.com/gocd/gocd/issues/${ID}");
        assertThat(cruiseConfig.pipelines("first").findBy(str("up43")).getTrackingTool().getLink()).isEqualTo("https://github.com/gocd/gocd/issues/${ID}");
        assertThat(cruiseConfig.pipelines("second").findBy(str("up12")).getTrackingTool().getLink()).isEqualTo("http://github.com/gocd/gocd/issues/${ID}");
        assertThat(cruiseConfig.pipelines("second").findBy(str("up13")).getTrackingTool().getLink()).isEqualTo("http://github.com/gocd/gocd/issues/${ID}");
    }

    @Test
    public void shouldIntroduceTypeOnBuildArtifacts_asPartOf106Migration() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cruise schemaVersion=\"105\">\n"
                + "    <server artifactsdir=\"artifacts\"/>\n"
                + "    <pipelines>"
                + "      <pipeline name=\"foo\">"
                + "         <materials> "
                + "           <hg url=\"blah\"/>"
                + "         </materials>  "
                + "         <stage name=\"some_stage\">"
                + "             <jobs>"
                + "             <job name=\"some_job\">"
                + "                 <tasks>"
                + "                    <exec command=\"ls\"/>"
                + "                 </tasks>"
                + "                 <artifacts>"
                + "                     <artifact src='foo.txt' dest='cruise-output' />"
                + "                     <artifact src='dir/**' dest='dir' />"
                + "                     <artifact src='build' />"
                + "                 </artifacts>"
                + "             </job>"
                + "             </jobs>"
                + "         </stage>"
                + "      </pipeline>"
                + "    </pipelines>"
                + "</cruise>";

        String migratedContent = migrateXmlString(configXml, 105);

        assertThat(migratedContent).contains("<artifact type=\"build\" src=\"foo.txt\" dest=\"cruise-output\"/>");
        assertThat(migratedContent).contains("<artifact type=\"build\" src=\"dir/**\" dest=\"dir\"/>");
        assertThat(migratedContent).contains("<artifact type=\"build\" src=\"build\"/>");
    }

    @Test
    public void shouldConvertTestTagToArtifactWithTypeOnTestArtifacts_asPartOf106Migration() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cruise schemaVersion=\"105\">\n"
                + "    <server artifactsdir=\"artifacts\"/>\n"
                + "    <pipelines>"
                + "      <pipeline name=\"foo\">"
                + "         <materials> "
                + "           <hg url=\"blah\"/>"
                + "         </materials>  "
                + "         <stage name=\"some_stage\">"
                + "             <jobs>"
                + "             <job name=\"some_job\">"
                + "                 <tasks>"
                + "                    <exec command=\"ls\"/>"
                + "                 </tasks>"
                + "                 <artifacts>"
                + "                     <test src='foo.txt' dest='cruise-output' />"
                + "                     <test src='dir/**' dest='dir' />"
                + "                     <test src='build' />"
                + "                 </artifacts>"
                + "             </job>"
                + "             </jobs>"
                + "         </stage>"
                + "      </pipeline>"
                + "    </pipelines>"
                + "</cruise>";

        String migratedContent = migrateXmlString(configXml, 105);

        assertThat(migratedContent).contains("<artifact type=\"test\" src=\"foo.txt\" dest=\"cruise-output\"/>");
        assertThat(migratedContent).contains("<artifact type=\"test\" src=\"dir/**\" dest=\"dir\"/>");
        assertThat(migratedContent).contains("<artifact type=\"test\" src=\"build\"/>");
    }

    @Test
    public void shouldConvertPluggableArtifactTagToArtifactWithTypeOnPluggableArtifacts_asPartOf106Migration() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cruise schemaVersion=\"105\">\n"
                + "    <server artifactsdir=\"artifacts\"/>\n"
                + "    <artifactStores>\n"
                + "      <artifactStore id=\"foo\" pluginId=\"cd.go.artifact.docker.registry\">\n"
                + "        <property>\n"
                + "          <key>RegistryURL</key>\n"
                + "          <value>http://foo</value>\n"
                + "        </property>\n"
                + "      </artifactStore>\n"
                + "    </artifactStores>"
                + "    <pipelines>"
                + "      <pipeline name=\"foo\">"
                + "         <materials> "
                + "           <hg url=\"blah\"/>"
                + "         </materials>  "
                + "         <stage name=\"some_stage\">"
                + "             <jobs>"
                + "             <job name=\"some_job\">"
                + "                 <tasks>"
                + "                    <exec command=\"ls\"/>"
                + "                 </tasks>"
                + "                 <artifacts>"
                + "                     <pluggableArtifact id='artifactId1' storeId='foo' />"
                + "                     <pluggableArtifact id='artifactId2' storeId='foo'>"
                + "                         <property>"
                + "                             <key>BuildFile</key>"
                + "                             <value>foo.json</value>"
                + "                         </property>"
                + "                     </pluggableArtifact>"
                + "                     <pluggableArtifact id='artifactId3' storeId='foo'>"
                + "                         <property>"
                + "                             <key>SecureProperty</key>"
                + "                             <encryptedValue>trMHp15AjUE=</encryptedValue>"
                + "                         </property>"
                + "                     </pluggableArtifact>"
                + "                 </artifacts>"
                + "             </job>"
                + "             </jobs>"
                + "         </stage>"
                + "      </pipeline>"
                + "    </pipelines>"
                + "</cruise>";

        String migratedContent = migrateXmlString(configXml, 105, 106);
        String artifactId2 = "<artifact type=\"external\" id=\"artifactId2\" storeId=\"foo\">"
                +"                         <property>"
                +"                             <key>BuildFile</key>"
                +"                             <value>foo.json</value>"
                +"                         </property>"
                +"                     </artifact>";

        String artifactId3 = "<artifact type=\"external\" id=\"artifactId3\" storeId=\"foo\">"
                +"                         <property>"
                +"                             <key>SecureProperty</key>"
                +"                             <encryptedValue>trMHp15AjUE=</encryptedValue>"
                +"                         </property>"
                +"                     </artifact>";
        assertThat(migratedContent).contains("<artifact type=\"external\" id=\"artifactId1\" storeId=\"foo\"/>");
        assertThat(migratedContent).contains(artifactId2);
        assertThat(migratedContent).contains(artifactId3);
    }

    @Test
    public void shouldAddTypeAttributeOnFetchArtifactTag_asPartOf107Migration() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cruise schemaVersion=\"106\">\n"
                + "    <server artifactsdir=\"artifacts\"/>\n"
                + "    <pipelines>"
                + "      <pipeline name=\"foo\">"
                + "         <materials> "
                + "           <hg url=\"blah\"/>"
                + "         </materials>  "
                + "         <stage name=\"stage1\">"
                + "             <jobs>"
                + "             <job name=\"job1\">"
                + "                 <tasks>"
                + "                    <exec command=\"ls\"/>"
                + "                 </tasks>"
                + "                 <artifacts>"
                + "                     <artifact type='build' src='foo/**' dest='cruise-output' />"
                + "                 </artifacts>"
                + "             </job>"
                + "             </jobs>"
                + "         </stage>"
                + "         <stage name=\"stage2\">"
                + "             <jobs>"
                + "             <job name=\"job2\">"
                + "                 <tasks>"
                + "                    <exec command=\"ls\"/>"
                + "                     <fetchartifact pipeline='foo' stage='stage1' job='job1' srcfile='foo/foo.txt'/>"
                + "                     <fetchartifact pipeline='foo' stage='stage1' job='job1' srcdir='foo'/>"
                + "                     <fetchartifact stage='stage1' job='job1' srcdir='foo' dest='dest_on_agent'/>"
                + "                 </tasks>"
                + "             </job>"
                + "             </jobs>"
                + "         </stage>"
                + "      </pipeline>"
                + "    </pipelines>"
                + "</cruise>";

        String migratedContent = migrateXmlString(configXml, 106);

        assertThat(migratedContent).contains("<fetchartifact artifactOrigin=\"gocd\" pipeline=\"foo\" stage=\"stage1\" job=\"job1\" srcfile=\"foo/foo.txt\"");
        assertThat(migratedContent).contains("<fetchartifact artifactOrigin=\"gocd\" pipeline=\"foo\" stage=\"stage1\" job=\"job1\" srcdir=\"foo\"");
        assertThat(migratedContent).contains("<fetchartifact artifactOrigin=\"gocd\" stage=\"stage1\" job=\"job1\" srcdir=\"foo\" dest=\"dest_on_agent\"");
    }

    @Test
    public void shouldConvertFetchPluggableArtifactToFetchArtifactTagWithType_asPartOf107Migration() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, CryptoException {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cruise schemaVersion=\"106\">\n"
                + "    <server artifactsdir=\"artifacts\"/>\n"
                + "    <artifactStores>\n"
                + "      <artifactStore id=\"foobar\" pluginId=\"cd.go.artifact.docker.registry\">\n"
                + "        <property>\n"
                + "          <key>RegistryURL</key>\n"
                + "          <value>http://foo</value>\n"
                + "        </property>\n"
                + "      </artifactStore>\n"
                + "    </artifactStores>"
                + "    <pipelines>"
                + "      <pipeline name=\"foo\">"
                + "         <materials> "
                + "           <hg url=\"blah\"/>"
                + "         </materials>  "
                + "         <stage name=\"stage1\">"
                + "             <jobs>"
                + "             <job name=\"job1\">"
                + "                 <tasks>"
                + "                    <exec command=\"ls\"/>"
                + "                 </tasks>"
                + "                 <artifacts>"
                + "                     <artifact type='external' id='artifactId1' storeId='foobar' />"
                + "                     <artifact type='external' id='artifactId2' storeId='foobar' />"
                + "                     <artifact type='external' id='artifactId3' storeId='foobar' />"
                + "                 </artifacts>"
                + "             </job>"
                + "             </jobs>"
                + "         </stage>"
                + "         <stage name=\"stage2\">"
                + "             <jobs>"
                + "             <job name=\"job2\">"
                + "                 <tasks>"
                + "                    <exec command=\"ls\"/>"
                + "                     <fetchPluggableArtifact pipeline='foo' stage='stage1' job='job1' artifactId='artifactId1'/>"
                + "                     <fetchPluggableArtifact pipeline='foo' stage='stage1' job='job1' artifactId='artifactId2'>"
                + "                         <configuration>"
                + "                             <property>"
                + "                                 <key>dest</key>"
                + "                                 <value>destination</value>"
                + "                             </property>"
                + "                         </configuration>"
                + "                     </fetchPluggableArtifact>"
                + "                     <fetchPluggableArtifact pipeline='foo' stage='stage1' job='job1' artifactId='artifactId3'>"
                + "                         <configuration>"
                + "                             <property>"
                + "                                 <key>SomeSecureProperty</key>"
                + "                                 <encryptedValue>trMHp15AjUE=</encryptedValue>"
                + "                             </property>"
                + "                         </configuration>"
                + "                     </fetchPluggableArtifact>"
                + "                 </tasks>"
                + "             </job>"
                + "             </jobs>"
                + "         </stage>"
                + "      </pipeline>"
                + "    </pipelines>"
                + "</cruise>";

        String migratedContent = migrateXmlString(configXml, 106);

        String artifactId2 = "<fetchartifact artifactOrigin=\"external\" pipeline=\"foo\" stage=\"stage1\" job=\"job1\" artifactId=\"artifactId2\">"
                +"                         <configuration>"
                +"                             <property>"
                +"                                 <key>dest</key>"
                +"                                 <value>destination</value>"
                +"                             </property>"
                +"                         </configuration>"
                +"                     </fetchartifact>";

        String artifactId3 = "<fetchartifact artifactOrigin=\"external\" pipeline=\"foo\" stage=\"stage1\" job=\"job1\" artifactId=\"artifactId3\">"
                +"                         <configuration>"
                +"                             <property>"
                +"                                 <key>SomeSecureProperty</key>"
                +"                                 <encryptedValue>" + new GoCipher().encrypt("abcd") + "</encryptedValue>"
                +"                             </property>"
                +"                         </configuration>"
                +"                     </fetchartifact>";

        assertThat(migratedContent).contains("<fetchartifact artifactOrigin=\"external\" pipeline=\"foo\" stage=\"stage1\" job=\"job1\" artifactId=\"artifactId1\"");
        assertThat(migratedContent).contains(artifactId2);
        assertThat(migratedContent).contains(artifactId3);
    }

    @Test
    public void shouldAddTheConfigurationSubTagOnExternalArtifacts_asPartOf108Migration() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cruise schemaVersion=\"107\">\n"
                + "    <server artifactsdir=\"artifacts\"/>\n"
                + "    <artifactStores>\n"
                + "      <artifactStore id=\"foobar\" pluginId=\"cd.go.artifact.docker.registry\">\n"
                + "        <property>\n"
                + "          <key>RegistryURL</key>\n"
                + "          <value>http://foo</value>\n"
                + "        </property>\n"
                + "      </artifactStore>\n"
                + "    </artifactStores>"
                + "    <pipelines>"
                + "      <pipeline name=\"p1\">"
                + "         <materials> "
                + "           <hg url=\"blah\"/>"
                + "         </materials>  "
                + "         <stage name=\"s1\">"
                + "             <jobs>"
                + "             <job name=\"j1\">"
                + "                 <tasks>"
                + "                    <exec command=\"ls\"/>"
                + "                 </tasks>"
                + "                 <artifacts>"
                + "                     <artifact type=\"external\" id=\"artifactId1\" storeId=\"foobar\" />"
                + "                     <artifact type=\"external\" id=\"artifactId2\" storeId=\"foobar\">"
                + "                         <property>"
                + "                             <key>BuildFile</key>"
                + "                             <value>foo.json</value>"
                + "                         </property>"
                + "                     </artifact>"
                + "                 </artifacts>"
                + "             </job>"
                + "             </jobs>"
                + "         </stage>"
                + "      </pipeline>"
                + "    </pipelines>"
                + "</cruise>";

        String migratedContent = migrateXmlString(configXml, 107);
        String migratedArtifact1 = "<artifact type=\"external\" id=\"artifactId1\" storeId=\"foobar\"/>";
        String migratedArtifact2 = "<artifact type=\"external\" id=\"artifactId2\" storeId=\"foobar\"><configuration>"
                + "                         <property>"
                + "                             <key>BuildFile</key>"
                + "                             <value>foo.json</value>"
                + "                         </property>"
                + "                     </configuration></artifact>";

        assertThat(migratedContent).contains(migratedArtifact1);
        assertThat(migratedContent).contains(migratedArtifact2);

    }

    @Test
    public void shouldOnlyUpdateSchemaVersionForMigration114() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String configContent =  "<pipelines>"
                + "      <pipeline name=\"p1\">"
                + "         <materials> "
                + "           <hg url=\"blah\"/>"
                + "         </materials>  "
                + "         <stage name=\"s1\">"
                + "             <jobs>"
                + "             <job name=\"j1\">"
                + "                 <tasks>"
                + "                    <exec command=\"ls\"/>"
                + "                 </tasks>"
                + "             </job>"
                + "             </jobs>"
                + "         </stage>"
                + "      </pipeline>"
                + "    </pipelines>";

        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cruise schemaVersion=\"113\">\n"
                + configContent
                + "</cruise>";

        String migratedContent = migrateXmlString(configXml, 113, 114);

        assertThat(migratedContent).contains("<cruise schemaVersion=\"114\"");
        assertThat(migratedContent).contains(configContent);
    }

    @Test
    public void shouldRenameOriginAttributeOnFetchArtifactToArtifactOrigin_AsPartOf110To111Migration() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cruise schemaVersion=\"110\">\n"
                + "    <server artifactsdir=\"artifacts\"/>\n"
                + "    <artifactStores>\n"
                + "      <artifactStore id=\"foobar\" pluginId=\"cd.go.artifact.docker.registry\">\n"
                + "        <property>\n"
                + "          <key>RegistryURL</key>\n"
                + "          <value>http://foo</value>\n"
                + "        </property>\n"
                + "      </artifactStore>\n"
                + "    </artifactStores>"
                + "    <pipelines>"
                + "      <pipeline name=\"foo\">"
                + "         <materials> "
                + "           <hg url=\"blah\"/>"
                + "         </materials>  "
                + "         <stage name=\"stage1\">"
                + "             <jobs>"
                + "             <job name=\"job1\">"
                + "                 <tasks>"
                + "                    <exec command=\"ls\"/>"
                + "                 </tasks>"
                + "                 <artifacts>"
                + "                     <artifact type='build' src='foo' dest='bar'/>"
                + "                     <artifact type='external' id='artifactId1' storeId='foobar' />"
                + "                 </artifacts>"
                + "             </job>"
                + "             </jobs>"
                + "         </stage>"
                + "         <stage name=\"stage2\">"
                + "             <jobs>"
                + "             <job name=\"job2\">"
                + "                 <tasks>"
                + "                    <exec command=\"ls\"/>"
                + "                     <fetchartifact origin='gocd' pipeline='foo' stage='stage1' job='job1' srcdir='dist/zip' dest='target'/>"
                + "                     <fetchartifact origin='external' pipeline='foo' stage='stage1' job='job1' artifactId='artifactId1'>"
                + "                         <configuration>"
                + "                             <property>"
                + "                                 <key>dest</key>"
                + "                                 <value>destination</value>"
                + "                             </property>"
                + "                         </configuration>"
                + "                     </fetchartifact>"
                + "                 </tasks>"
                + "             </job>"
                + "             </jobs>"
                + "         </stage>"
                + "      </pipeline>"
                + "    </pipelines>"
                + "</cruise>";

        String migratedContent = migrateXmlString(configXml, 110);

        assertThat(migratedContent).contains("<fetchartifact artifactOrigin=\"gocd\" pipeline=\"foo\" stage=\"stage1\" job=\"job1\" srcdir=\"dist/zip\" dest=\"target\"/> ");
        assertThat(migratedContent).contains("<fetchartifact artifactOrigin=\"external\" pipeline=\"foo\" stage=\"stage1\" job=\"job1\" artifactId=\"artifactId1\">");
    }

    @Test
    public void shouldRemoveMaterialNameFromConfigRepos_AsPartOf114To115Migration() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                "<cruise schemaVersion='114'>" +
                "<config-repos>\n" +
                "    <config-repo pluginId=\"yaml.config.plugin\" id=\"test\">\n" +
                "      <git url=\"test\" branch=\"test\" materialName=\"test\" />\n" +
                "    </config-repo>\n" +
                "    <config-repo pluginId=\"yaml.config.plugin\" id=\"test1\">\n" +
                "      <svn url=\"test\" username=\"\" materialName=\"test\" />\n" +
                "    </config-repo>\n" +
                "    <config-repo pluginId=\"yaml.config.plugin\" id=\"test2\">\n" +
                "      <hg url=\"test\" materialName=\"test\" />\n" +
                "    </config-repo>\n" +
                "    <config-repo pluginId=\"yaml.config.plugin\" id=\"asd\">\n" +
                "      <tfs url=\"test\" username=\"admin\" domain=\"test\" encryptedPassword=\"AES:09M8nDpEgOgRGVVWAnEiMQ==:7lAsVu5nZ6iYhoZ4Alwc5g==\" projectPath=\"test\" materialName=\"test\" />\n" +
                "    </config-repo>\n" +
                "    <config-repo pluginId=\"yaml.config.plugin\" id=\"asdasd\">\n" +
                "      <p4 port=\"test\" username=\"admin\" encryptedPassword=\"AES:A7h8pqjGyz372Kogx5xX/w==:tG1WNNd680UyqOUM1BVrfQ==\" materialName=\"test\">\n" +
                "        <view><![CDATA[<h1>test</h1>]]></view>\n" +
                "      </p4>\n" +
                "    </config-repo>\n" +
                "  </config-repos>\n" +
                "</cruise>";
        String migratedContent = migrateXmlString(configXml, 114);

        assertStringContainsIgnoringCarriageReturn(migratedContent,
                "<config-repos>\n" +
                "    <config-repo pluginId=\"yaml.config.plugin\" id=\"test\">\n" +
                "      <git url=\"test\" branch=\"test\"/>\n" +
                "    </config-repo>\n" +
                "    <config-repo pluginId=\"yaml.config.plugin\" id=\"test1\">\n" +
                "      <svn url=\"test\" username=\"\"/>\n" +
                "    </config-repo>\n" +
                "    <config-repo pluginId=\"yaml.config.plugin\" id=\"test2\">\n" +
                "      <hg url=\"test\"/>\n" +
                "    </config-repo>\n" +
                "    <config-repo pluginId=\"yaml.config.plugin\" id=\"asd\">\n" +
                "      <tfs url=\"test\" username=\"admin\" domain=\"test\" encryptedPassword=\"AES:09M8nDpEgOgRGVVWAnEiMQ==:7lAsVu5nZ6iYhoZ4Alwc5g==\" projectPath=\"test\"/>\n" +
                "    </config-repo>\n" +
                "    <config-repo pluginId=\"yaml.config.plugin\" id=\"asdasd\">\n" +
                "      <p4 port=\"test\" username=\"admin\" encryptedPassword=\"AES:A7h8pqjGyz372Kogx5xX/w==:tG1WNNd680UyqOUM1BVrfQ==\">\n" +
                "        <view>&lt;h1&gt;test&lt;/h1&gt;</view>\n" +
                "      </p4>\n" +
                "    </config-repo>\n" +
                "  </config-repos>");
    }

    @Test
    public void shouldOnlyUpdateSchemaVersionForMigration116() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String configContent =  "<pipelines>"
                + "      <pipeline name=\"p1\">"
                + "         <materials> "
                + "           <hg url=\"blah\"/>"
                + "         </materials>  "
                + "         <stage name=\"s1\">"
                + "             <jobs>"
                + "             <job name=\"j1\">"
                + "                 <tasks>"
                + "                    <exec command=\"ls\"/>"
                + "                 </tasks>"
                + "             </job>"
                + "             </jobs>"
                + "         </stage>"
                + "      </pipeline>"
                + "    </pipelines>";

        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cruise schemaVersion=\"115\">\n"
                + configContent
                + "</cruise>";

        String migratedContent = migrateXmlString(configXml, 115, 116);

        assertThat(migratedContent).contains("<cruise schemaVersion=\"116\"");
        assertThat(migratedContent).contains(configContent);
    }

    @Test
    public void shouldOnlyUpdateSchemaVersionForMigration118() throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        String configContent =  " <elastic>\n" +
                "    <profiles>\n" +
                "      <profile id=\"asdf\" pluginId=\"cd.go.contrib.elastic-agent.docker\">\n" +
                "        <property>\n" +
                "          <key>Image</key>\n" +
                "          <value>asdf</value>\n" +
                "        </property>\n" +
                "      </profile>" +
                "    </profiles>" +
                "  </elastic>";

        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cruise schemaVersion=\"117\">\n"
                + configContent
                + "</cruise>";

        String migratedContent = migrateXmlString(configXml, 117, 118);

        assertThat(migratedContent).contains("<cruise schemaVersion=\"118\"");
        assertThat(migratedContent).contains(configContent);
    }

    @Test
    public void shouldAllowSpecifyingClusterProfileIdAttributeOnProfilesAsPartMigration118() throws Exception {
        String configContent =  " <elastic>\n" +
        "    <profiles>\n" +
                "      <profile clusterProfileId=\"foo\" id=\"profile1\" pluginId=\"cd.go.contrib.elastic-agent.docker\">\n" +
                "        <property>\n" +
                "          <key>Image</key>\n" +
                "          <value>asdf</value>\n" +
                "        </property>\n" +
                "      </profile>" +
                "    </profiles>" +
                "  </elastic>";

        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cruise schemaVersion=\"118\">\n"
                + configContent
                + "</cruise>";

        CruiseConfig config = loadConfigFileWithContent(configXml);
        ElasticProfile elasticProfile = config.getElasticConfig().getProfiles().find("profile1");
        assertThat(elasticProfile.getClusterProfileId()).isEqualTo("foo");
    }

    private void assertStringsIgnoringCarriageReturnAreEqual(String expected, String actual) {
        assertThat(actual.replaceAll("\\r", "")).isEqualTo(expected.replaceAll("\\r", ""));
    }

    private void assertStringContainsIgnoringCarriageReturn(String actual, String substring) {
        assertThat(actual.replaceAll("\\r", "")).contains(substring.replaceAll("\\r", ""));
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
        assertThat(configValidity.isValid()).as(configValidity.errorMessage()).isTrue();
        return goConfigService.getCurrentConfig();
    }

    private String migrateXmlString(String content, int fromVersion) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        return migrateXmlString(content, fromVersion, GoConfigSchema.currentSchemaVersion());
    }

    private String migrateXmlString(String content, int fromVersion, int toVersion) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        final List<Exception> exs = new ArrayList<>();
        GoConfigMigration upgrader = new GoConfigMigration(
                new GoConfigMigration.UpgradeFailedHandler() {
                    public void handle(Exception e) {
                        e.printStackTrace();
                        exs.add(e);
                    }
                }, configRepository, new TimeProvider(), configCache, ConfigElementImplementationRegistryMother.withNoPlugins()
        );
        Method upgrade = upgrader.getClass().getDeclaredMethod("upgrade", String.class, Integer.TYPE, Integer.TYPE);
        upgrade.setAccessible(true);
        return (String) upgrade.invoke(upgrader, content, fromVersion, toVersion);
    }

    private CruiseConfig loadConfigFileWithContent(String content) throws Exception {
        FileUtils.writeStringToFile(configFile, content, UTF_8);
        return loadWithMigration(configFile).config;
    }

    private GoConfigHolder loadWithMigration(final File configFile) throws Exception {
        GoConfigMigration migration = new GoConfigMigration(new GoConfigMigration.UpgradeFailedHandler() {
            public void handle(Exception e) {
                String content = "";
                try {
                    content = FileUtils.readFileToString(configFile, UTF_8);
                } catch (IOException e1) {
                }
                throw bomb(e.getMessage() + ": content=\n" + content, e);
            }
        }, configRepository, new TimeProvider(), configCache, registry
        );
        SystemEnvironment sysEnv = new SystemEnvironment();
        FullConfigSaveNormalFlow normalFlow = new FullConfigSaveNormalFlow(configCache, registry, sysEnv, new TimeProvider(), configRepository, cachedGoPartials);
        GoFileConfigDataSource configDataSource = new GoFileConfigDataSource(migration, configRepository, sysEnv, new TimeProvider(), configCache,
                registry, serverHealthService, cachedGoPartials, null, normalFlow);
        configDataSource.upgradeIfNecessary();
        return configDataSource.forceLoad(configFile);
    }

    public void assertConfiguration(Configuration configuration, List<List> expectedKeyValuePair) {
        int position = 0;
        for (ConfigurationProperty configurationProperty : configuration) {
            assertThat(configurationProperty.getConfigurationKey().getName()).isEqualTo(expectedKeyValuePair.get(position).get(0));
            assertThat(configurationProperty.isSecure()).isEqualTo(expectedKeyValuePair.get(position).get(1));
            assertThat(configurationProperty.getConfigurationValue().getValue()).isEqualTo(expectedKeyValuePair.get(position).get(2));
            position++;
        }
    }
}
