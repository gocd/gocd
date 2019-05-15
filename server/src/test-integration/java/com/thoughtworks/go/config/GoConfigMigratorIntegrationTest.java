/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.elastic.ClusterProfiles;
import com.thoughtworks.go.config.elastic.ElasticProfile;
import com.thoughtworks.go.config.elastic.ElasticProfiles;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.pluggabletask.PluggableTask;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.domain.Task;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
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
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xmlunit.assertj.SingleNodeAssert;
import org.xmlunit.assertj.XmlAssert;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.go.domain.config.CaseInsensitiveStringMother.str;
import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import com.thoughtworks.go.util.GoConfigFileHelper;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml"
})
public class GoConfigMigratorIntegrationTest {
    private File configFile;
    ConfigRepository configRepository;
    @Autowired
    private SystemEnvironment systemEnvironment;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private ServerHealthService serverHealthService;
    private GoConfigMigrator goConfigMigrator;
    @Autowired
    private GoConfigMigration goConfigMigration;
    @Autowired
    private FullConfigSaveNormalFlow fullConfigSaveNormalFlow;
    @Autowired
    private ConfigCache configCache;
    @Autowired
    private ConfigElementImplementationRegistry registry;
    @Autowired
    private GoFileConfigDataSource goFileConfigDataSource;
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public ResetCipher resetCipher = new ResetCipher();
    @Rule
    public ExpectedException thrown = ExpectedException.none();
    private ArrayList<Exception> exceptions;

    @Before
    public void setUp() throws Exception {
        File file = temporaryFolder.newFolder();
        configFile = new File(file, "cruise-config.xml");
        new SystemEnvironment().setProperty(SystemEnvironment.CONFIG_FILE_PROPERTY, configFile.getAbsolutePath());
        GoConfigFileHelper.clearConfigVersions();
        configRepository = new ConfigRepository(systemEnvironment);
        configRepository.initialize();
        serverHealthService.removeAllLogs();
        resetCipher.setupDESCipherFile();
        resetCipher.setupAESCipherFile();
        exceptions = new ArrayList<>();
        MagicalGoConfigXmlLoader xmlLoader = new MagicalGoConfigXmlLoader(configCache, registry);
        goConfigMigrator = new GoConfigMigrator(goConfigMigration, systemEnvironment, fullConfigSaveNormalFlow, xmlLoader, new GoConfigFileReader(systemEnvironment), configRepository, serverHealthService, new GoConfigMigrator.UpgradeFailedHandler() {
            @Override
            public void handle(Exception e) {
                exceptions.add(e);
            }
        });

    }

    @After
    public void tearDown() throws Exception {
        GoConfigFileHelper.clearConfigVersions();
        configFile.delete();
        serverHealthService.removeAllLogs();
    }

    @Test
    public void shouldNotUpgradeCruiseConfigFileUponServerStartupIfSchemaVersionMatches() throws Exception {

        String config = ConfigFileFixture.SERVER_WITH_ARTIFACTS_DIR;
        FileUtils.writeStringToFile(configFile, config, UTF_8);
        // To create a version of this config in config.git since there wouldn't be any commit
        // in config.git at this point
        goFileConfigDataSource.forceLoad(configFile);

        CruiseConfig cruiseConfig = loadConfigFileWithContent(config);
        assertThat(cruiseConfig.schemaVersion()).isEqualTo(GoConstants.CONFIG_SCHEMA_VERSION);
        assertThat(configRepository.getRevision(ConfigRepository.CURRENT).getUsername()).isNotEqualTo("Upgrade");
    }

    @Test
    public void shouldValidateCruiseConfigFileIrrespectiveOfUpgrade() {
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
            assertThat(e.getMessage()).contains("Environment 'foo' refers to an unknown pipeline 'does_not_exist'");
        }
    }

    @Test
    public void shouldUpgradeCruiseConfigFileIfVersionDoesNotMatch() throws Exception {
        CruiseConfig cruiseConfig = loadConfigFileWithContent(ConfigFileFixture.OLD);
        assertThat(cruiseConfig.schemaVersion()).isEqualTo(GoConstants.CONFIG_SCHEMA_VERSION);
    }

    @Test
    public void shouldNotUpgradeInvalidConfigFileWhenThereIsNoValidConfigVersioned() throws GitAPIException, IOException {
        Assertions.assertThat(configRepository.getRevision(ConfigRepository.CURRENT)).isNull();
        FileUtils.writeStringToFile(configFile, "<cruise></cruise>", UTF_8);
        goConfigMigrator.migrate();
        assertThat(exceptions.size()).isEqualTo(1);
        assertThat(exceptions.get(0).getMessage()).contains("Cruise config file with version 0 is invalid. Unable to upgrade.");
    }

    @Test
    public void shouldRevertConfigToTheLatestValidConfigVersionFromGitIfCurrentConfigIsInvalid() {
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
    public void shouldTryToRevertConfigToTheLatestValidConfigVersionOnlyOnce() throws Exception {
        configRepository.checkin(new GoConfigRevision("<cruise></cruise>", "md5", "ps", "123", new TimeProvider()));
        FileUtils.writeStringToFile(configFile, "<cruise></cruise>", UTF_8);
        goConfigMigrator.migrate();
        assertThat(exceptions.get(0).getMessage()).contains("Cruise config file with version 0 is invalid. Unable to upgrade.");
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
        Assertions.assertThat(actual).isInstanceOf(DependencyMaterialConfig.class);
        DependencyMaterialConfig depends = (DependencyMaterialConfig) actual;
        assertThat(depends.getPipelineName()).isEqualTo(new CaseInsensitiveString("multiple"));
        assertThat(depends.getStageName()).isEqualTo(new CaseInsensitiveString("helloworld-part2"));
    }

    @Test
    public void shouldFailIfJobsWithSameNameButDifferentCasesExistInConfig() throws Exception {
        FileUtils.writeStringToFile(configFile, ConfigFileFixture.JOBS_WITH_DIFFERNT_CASE, UTF_8);
        GoConfigHolder configHolder = goConfigMigrator.migrate();
        Assertions.assertThat(configHolder).isNull();
        PipelineConfig frameworkPipeline = goConfigService.getCurrentConfig().getPipelineConfigByName(new CaseInsensitiveString("framework"));
        assertThat(frameworkPipeline).isNull();

        assertThat(exceptions.size()).isEqualTo(1);
        assertThat(exceptions.get(0).getMessage()).contains("You have defined multiple Jobs called 'Test'");
    }

    @Test
    public void shouldVersionControlAnUpgradedConfigIfItIsValid() throws Exception {
        FileUtils.writeStringToFile(configFile, ConfigFileFixture.DEFAULT_XML_WITH_2_AGENTS, UTF_8);
        configRepository.checkin(new GoConfigRevision("dummy-content", "some-md5", "loser", "100.3.1", new TimeProvider()));

        GoConfigHolder goConfigHolder = goConfigMigrator.migrate();
        Assertions.assertThat(goConfigHolder.config).isNotNull();
        Assertions.assertThat(goConfigHolder.configForEdit).isNotNull();

        GoConfigRevision latest = configRepository.getRevision(ConfigRepository.CURRENT);

        assertThat(latest.getUsername()).isEqualTo("Upgrade");

        String contents = FileUtils.readFileToString(configFile, UTF_8);
        assertThat(latest.getContent()).isEqualTo(contents);
        assertThat(latest.getMd5()).isEqualTo(DigestUtils.md5Hex(contents));
    }

    @Test
    public void shouldEncryptPasswordsOnMigration() throws Exception {
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

        goConfigMigrator.migrate();

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
        CruiseConfig cruiseConfig = goConfigMigrator.migrate().config;

        RolesConfig roles = cruiseConfig.server().security().getRoles();
        assertThat(roles.size()).isEqualTo(2);
        Assertions.assertThat(roles.get(0)).isEqualTo(new RoleConfig(new CaseInsensitiveString("bAr"),
                new RoleUser(new CaseInsensitiveString("quux")),
                new RoleUser(new CaseInsensitiveString("bang")),
                new RoleUser(new CaseInsensitiveString("LoSeR")),
                new RoleUser(new CaseInsensitiveString("baz"))));

        Assertions.assertThat(roles.get(1)).isEqualTo(new RoleConfig(new CaseInsensitiveString("Foo"),
                new RoleUser(new CaseInsensitiveString("foo")),
                new RoleUser(new CaseInsensitiveString("LoSeR")),
                new RoleUser(new CaseInsensitiveString("bar"))));
    }

    @Test
    public void shouldAllowParamsInP4ServerAndPortField() throws Exception {
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
                        + "</pipeline>", "hello"), 34);
        FileUtils.writeStringToFile(configFile, configContent, UTF_8);

        goConfigMigrator.migrate();

        assertThat(FileUtils.readFileToString(configFile, UTF_8)).contains("port=\"#{param_foo}\"");
    }

    @Test
    public void shouldIntroduceAWrapperTagForUsersOfRole() throws Exception {
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

        goConfigMigrator.migrate();

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
        assertThat(configValidity.isValid()).as("Has no error").isTrue();

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
        assertThat(configValidity.isValid()).as("Has no error").isTrue();

        CruiseConfig config = goConfigService.getCurrentConfig();
        ServerConfig server = config.server();

        assertThat(server.getServerId().matches("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")).isTrue();
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
        CruiseConfig configAfterMigration = migrateConfigAndLoadTheNewConfig(oldContent);
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
    public void forVersion63_shouldFailWhenOnChangesValueIsEmpty() throws IOException {
        String config = configWithTimerBasedPipeline("onlyOnChanges=''");
        FileUtils.writeStringToFile(configFile, config, UTF_8);
        goConfigMigrator.migrate();
        assertThat(exceptions.size()).isEqualTo(1);
        assertThat(exceptions.get(0).getCause().getMessage()).contains("'' is not a valid value for 'boolean'");
    }

    @Test
    public void forVersion63_shouldFailWhenOnChangesValueIsNotAValidBooleanValue() throws IOException {
        String config = configWithTimerBasedPipeline("onlyOnChanges='junk-non-boolean'");
        FileUtils.writeStringToFile(configFile, config, UTF_8);
        goConfigMigrator.migrate();
        assertThat(exceptions.size()).isEqualTo(1);
        assertThat(exceptions.get(0).getCause().getMessage()).contains("'junk-non-boolean' is not a valid value for 'boolean'");
    }

    @Test
    public void shouldValidatePackageRepositoriesConfiguration() throws Exception {
        String configString =
                "<cruise schemaVersion='66'>"
                        + "<repositories>"
                        + "<repository id='go-repo' name='go-repo'>"
                        + "     <pluginConfiguration id='plugin-id' version='1.0'/>"
                        + "     <configuration>"
                        + "         <property><key>url</key><value>http://fake-yum-repo</value></property>"
                        + "         <property><key>username</key><value>godev</value></property>"
                        + "         <property><key>password</key><value>password</value></property>"
                        + "     </configuration>"
                        + "     <packages>"
                        + "         <package id='go-server' name='go-server'>"
                        + "             <configuration>"
                        + "                 <property><key>name</key><value>go-server-13.2.0-1-i386</value></property>"
                        + "             </configuration>"
                        + "         </package>"
                        + "     </packages>"
                        + "</repository>"
                        + "</repositories>"
                        + "</cruise>";

        CruiseConfig cruiseConfig = migrateConfigAndLoadTheNewConfig(configString);
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
                        + "     <pluginConfiguration id='plugin-id' version='1.0'/>"
                        + "     <configuration>"
                        + "         <property><key>url</key><value>http://fake-yum-repo</value></property>"
                        + "         <property><key>username</key><value>godev</value></property>"
                        + "         <property><key>password</key><value>password</value></property>"
                        + "     </configuration>"
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
        CruiseConfig migratedConfig = migrateConfigAndLoadTheNewConfig(configXml);
        Task task = migratedConfig.tasksForJob("Test", "Functional", "Functional").get(0);
        Assertions.assertThat(task).isInstanceOf(ExecTask.class);
        Assertions.assertThat(task).isEqualTo(new ExecTask("c:\\program files\\cmd.exe", "arguments", (String) null));
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
        CruiseConfig migratedConfig = migrateConfigAndLoadTheNewConfig(configXml);
        Task task = migratedConfig.tasksForJob("Test", "Functional", "Functional").get(0);
        Assertions.assertThat(task).isInstanceOf(ExecTask.class);
        Assertions.assertThat(task).isEqualTo(new ExecTask("c:\\program files\\cmd.exe", "arguments", (String) null));
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
        CruiseConfig migratedConfig = migrateConfigAndLoadTheNewConfig(configXml);
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

        CruiseConfig migratedConfig = migrateConfigAndLoadTheNewConfig(configXml);
        PipelineConfig pipelineConfig = migratedConfig.pipelineConfigByName(new CaseInsensitiveString("up42"));
        JobConfig jobConfig = pipelineConfig.getStages().get(0).getJobs().get(0);

        assertThat(migratedConfig.schemaVersion()).isGreaterThan(86);

        ElasticProfiles profiles = migratedConfig.getElasticConfig().getProfiles();
        assertThat(profiles.size()).isEqualTo(1);

        ElasticProfile expectedProfile = new ElasticProfile(jobConfig.getElasticProfileId(), "no-op-cluster-for-docker",
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

        CruiseConfig migratedConfig = migrateConfigAndLoadTheNewConfig(configXml);
        PipelineConfig pipelineConfig = migratedConfig.pipelineConfigByName(new CaseInsensitiveString("up42"));
        JobConfigs jobs = pipelineConfig.getStages().get(0).getJobs();

        ElasticProfiles profiles = migratedConfig.getElasticConfig().getProfiles();
        assertThat(profiles.size()).isEqualTo(2);

        ElasticProfile expectedDockerProfile = new ElasticProfile(jobs.get(0).getElasticProfileId(), "no-op-cluster-for-docker",
                new ConfigurationProperty(new ConfigurationKey("instance-type"), new ConfigurationValue("m1.small")));
        assertThat(profiles.get(0)).isEqualTo(expectedDockerProfile);

        ElasticProfile expectedAWSProfile = new ElasticProfile(jobs.get(1).getElasticProfileId(), "no-op-cluster-for-aws",
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

        CruiseConfig migratedConfig = migrateConfigAndLoadTheNewConfig(configXml);
        PipelineConfig pipelineConfig = migratedConfig.pipelineConfigByName(new CaseInsensitiveString("up42"));
        JobConfigs buildJobs = pipelineConfig.getStages().get(0).getJobs();
        JobConfigs distJobs = pipelineConfig.getStages().get(1).getJobs();

        ElasticProfiles profiles = migratedConfig.getElasticConfig().getProfiles();
        assertThat(profiles.size()).isEqualTo(3);

        ElasticProfile expectedDockerProfile = new ElasticProfile(buildJobs.get(0).getElasticProfileId(), "no-op-cluster-for-docker",
                new ConfigurationProperty(new ConfigurationKey("instance-type"), new ConfigurationValue("m1.small")));
        assertThat(profiles.get(0)).isEqualTo(expectedDockerProfile);

        ElasticProfile expectedAWSProfile = new ElasticProfile(buildJobs.get(1).getElasticProfileId(), "no-op-cluster-for-aws",
                new ConfigurationProperty(new ConfigurationKey("ami"), new ConfigurationValue("some.ami")),
                new ConfigurationProperty(new ConfigurationKey("ram"), new ConfigurationValue("1024")),
                new ConfigurationProperty(new ConfigurationKey("diskSpace"), new ConfigurationValue("10G")));
        assertThat(profiles.get(1)).isEqualTo(expectedAWSProfile);

        ElasticProfile expectedSecondDockerProfile = new ElasticProfile(distJobs.get(0).getElasticProfileId(), "no-op-cluster-for-docker",
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

        CruiseConfig migratedConfig = migrateConfigAndLoadTheNewConfig(configXml);
        PipelineConfig up42 = migratedConfig.pipelineConfigByName(new CaseInsensitiveString("up42"));
        PipelineConfig up43 = migratedConfig.pipelineConfigByName(new CaseInsensitiveString("up43"));
        JobConfigs up42Jobs = up42.getStages().get(0).getJobs();
        JobConfigs up43Jobs = up43.getStages().get(0).getJobs();

        ElasticProfiles profiles = migratedConfig.getElasticConfig().getProfiles();
        assertThat(profiles.size()).isEqualTo(2);

        ElasticProfile expectedDockerProfile = new ElasticProfile(up42Jobs.get(0).getElasticProfileId(), "no-op-cluster-for-docker",
                new ConfigurationProperty(new ConfigurationKey("instance-type"), new ConfigurationValue("m1.small")));
        assertThat(profiles.get(0)).isEqualTo(expectedDockerProfile);

        ElasticProfile expectedAWSProfile = new ElasticProfile(up43Jobs.get(0).getElasticProfileId(), "no-op-cluster-for-aws",
                new ConfigurationProperty(new ConfigurationKey("ami"), new ConfigurationValue("some.ami")),
                new ConfigurationProperty(new ConfigurationKey("ram"), new ConfigurationValue("1024")),
                new ConfigurationProperty(new ConfigurationKey("diskSpace"), new ConfigurationValue("10G")));
        assertThat(profiles.get(1)).isEqualTo(expectedAWSProfile);
    }

    @Test
    public void shouldAddTokenGenerationKeyAttributeOnServerAsPartOf99To100Migration() throws Exception {
        try {
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

            final CruiseConfig cruiseConfig = migrateConfigAndLoadTheNewConfig(configXml);
            assertThat(StringUtils.isNotBlank(cruiseConfig.server().getTokenGenerationKey())).isTrue();
        } catch (Exception e) {
            System.err.println("jyoti singh: " + e.getMessage());
        }
    }

    @Test
    public void shouldMigrateElasticProfilesOutOfServerConfig_asPartOf100To101Migration() throws Exception {
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

        final CruiseConfig cruiseConfig = migrateConfigAndLoadTheNewConfig(configXml);
        Assertions.assertThat(cruiseConfig.getElasticConfig()).isNotNull();
        assertThat(cruiseConfig.server().getAgentAutoRegisterKey()).isEqualTo("041b5c7e-dab2-11e5-a908-13f95f3c6ef6");
        assertThat(cruiseConfig.server().getWebhookSecret()).isEqualTo("5f8b5eac-1148-4145-aa01-7b2934b6e1ab");
        assertThat(cruiseConfig.server().getCommandRepositoryLocation()).isEqualTo("default");
        assertThat(cruiseConfig.server().artifactsDir()).isEqualTo("artifactsDir");
        assertThat(cruiseConfig.getElasticConfig().getProfiles()).hasSize(1);
        assertThat(cruiseConfig.getElasticConfig().getProfiles().get(0)).isEqualTo(new ElasticProfile("dev-build", "no-op-cluster-for-cd.go.contrib.elastic-agent.docker-swarm",
                ConfigurationPropertyMother.create("Image", false, "bar"),
                ConfigurationPropertyMother.create("ReservedMemory", false, "3GB"),
                ConfigurationPropertyMother.create("MaxMemory", false, "3GB")
        ));
    }

    @Test
    public void shouldRetainAllOtherServerConfigElements_asPartOf100To101Migration() throws Exception {
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

        final CruiseConfig cruiseConfig = migrateConfigAndLoadTheNewConfig(configXml);
        assertThat(cruiseConfig.server().getAgentAutoRegisterKey()).isEqualTo("041b5c7e-dab2-11e5-a908-13f95f3c6ef6");
        assertThat(cruiseConfig.server().getWebhookSecret()).isEqualTo("5f8b5eac-1148-4145-aa01-7b2934b6e1ab");
        assertThat(cruiseConfig.server().getCommandRepositoryLocation()).isEqualTo("default");
        assertThat(cruiseConfig.server().artifactsDir()).isEqualTo("artifactsDir");
        Assertions.assertThat(cruiseConfig.server().security()).isEqualTo(new SecurityConfig(true));
        assertThat(cruiseConfig.getSCMs()).hasSize(1);
    }

    @Test
    public void shouldSkipParamResoulutionForElasticConfig_asPartOf100To101Migration() throws Exception {
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

        final CruiseConfig cruiseConfig = migrateConfigAndLoadTheNewConfig(configXml);
        assertThat(cruiseConfig.getElasticConfig().getProfiles().get(0)).isEqualTo(new ElasticProfile("dev-build", "no-op-cluster-for-cd.go.contrib.elastic-agent.docker-swarm",
                ConfigurationPropertyMother.create("Image", false, "#bar")
        ));
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

        final CruiseConfig cruiseConfig = migrateConfigAndLoadTheNewConfig(configXml);

        assertThat(cruiseConfig.pipelines("first").findBy(str("up42")).getTrackingTool().getLink()).isEqualTo("http://github.com/gocd/gocd/issues/${ID}");
        assertThat(cruiseConfig.pipelines("first").findBy(str("up43")).getTrackingTool().getLink()).isEqualTo("https://github.com/gocd/gocd/issues/${ID}");
        assertThat(cruiseConfig.pipelines("second").findBy(str("up12")).getTrackingTool().getLink()).isEqualTo("http://github.com/gocd/gocd/issues/${ID}");
        assertThat(cruiseConfig.pipelines("second").findBy(str("up13")).getTrackingTool().getLink()).isEqualTo("http://github.com/gocd/gocd/issues/${ID}");
    }

    @Test
    public void shouldRunMigration59_convertLogTypeToArtifact() throws Exception {
        final CruiseConfig cruiseConfig = migrateConfigAndLoadTheNewConfig(ConfigFileFixture.WITH_LOG_ARTIFACT_CONFIG);

        ArtifactConfigs artifactConfigs = cruiseConfig.getAllPipelineConfigs().get(0).getStage(new CaseInsensitiveString("mingle")).getJobs().getJob(
                new CaseInsensitiveString("bluemonkeybutt")).artifactConfigs();

        assertThat(artifactConfigs.getBuiltInArtifactConfigs().get(0).getSource()).isEqualTo("from1");
        assertThat("").isEqualTo(artifactConfigs.getBuiltInArtifactConfigs().get(0).getDestination());
        assertThat(artifactConfigs.getBuiltInArtifactConfigs().get(1).getSource()).isEqualTo("from2");
        assertThat(artifactConfigs.getBuiltInArtifactConfigs().get(1).getDestination()).isEqualTo("to2");
        assertThat(artifactConfigs.getBuiltInArtifactConfigs().get(2).getSource()).isEqualTo("from3");
        assertThat("").isEqualTo(artifactConfigs.getBuiltInArtifactConfigs().get(2).getDestination());
        assertThat(artifactConfigs.getBuiltInArtifactConfigs().get(3).getSource()).isEqualTo("from4");
        assertThat(artifactConfigs.getBuiltInArtifactConfigs().get(3).getDestination()).isEqualTo("to4");
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

        CruiseConfig cruiseConfig = migrateConfigAndLoadTheNewConfig(oldConfigWithNameInTask);
        String newConfigWithoutNameInTask = FileUtils.readFileToString(configFile, UTF_8);

        XmlAssert.assertThat(newConfigWithoutNameInTask).hasXPath("//cruise/pipelines/pipeline/stage/jobs/job/tasks/task");
        XmlAssert.assertThat(newConfigWithoutNameInTask).doesNotHaveXPath("//cruise/pipelines/pipeline/stage/jobs/job/tasks/task[@name]");
        PipelineConfig pipelineConfig = cruiseConfig.getAllPipelineConfigs().get(0);
        JobConfig jobConfig = pipelineConfig.getFirstStageConfig().getJobs().get(0);

        Configuration configuration = new Configuration(
                create("url", false, "http://fake-yum-repo"),
                create("username", false, "godev"),
                create("password", false, "password"));

        Tasks tasks = jobConfig.getTasks();
        assertThat(tasks.size()).isEqualTo(1);
        Assertions.assertThat(tasks.get(0)).isEqualTo(new PluggableTask(new PluginConfiguration("plugin-id", "1.0"), configuration));
    }

    @Test
    public void shouldDefineNoOpClustersAsPartOfMigration119() throws Exception {
        String configContent = " <elastic>\n" +
                "    <profiles>\n" +
                "      <profile id=\"profile1\" pluginId=\"cd.go.contrib.elastic-agent.docker\">\n" +
                "        <property>\n" +
                "          <key>Image</key>\n" +
                "          <value>alpine:latest</value>\n" +
                "        </property>\n" +
                "      </profile>" +
                "      <profile id=\"profile2\" pluginId=\"cd.go.contrib.elasticagent.kubernetes\">\n" +
                "        <property>\n" +
                "          <key>Image</key>\n" +
                "          <value>alpine:latest</value>\n" +
                "        </property>\n" +
                "      </profile>" +
                "      <profile id=\"profile3\" pluginId=\"cd.go.contrib.elastic-agent.docker\">\n" +
                "        <property>\n" +
                "          <key>Image</key>\n" +
                "          <value>alpine:latest</value>\n" +
                "        </property>\n" +
                "      </profile>" +
                "      <profile id=\"profile4\" pluginId=\"com.thoughtworks.gocd.elastic-agent.azure\">\n" +
                "        <property>\n" +
                "          <key>Image</key>\n" +
                "          <value>alpine:latest</value>\n" +
                "        </property>\n" +
                "      </profile>" +
                "      <profile id=\"profile5\" pluginId=\"com.thoughtworks.gocd.elastic-agent.azure\">\n" +
                "        <property>\n" +
                "          <key>Image</key>\n" +
                "          <value>alpine:latest</value>\n" +
                "        </property>\n" +
                "      </profile>" +
                "    </profiles>" +
                "  </elastic>";

        String configXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<cruise schemaVersion=\"118\">\n"
                + configContent
                + "</cruise>";

        ClusterProfile azureProfile = new ClusterProfile("no-op-cluster-for-com.thoughtworks.gocd.elastic-agent.azure", "com.thoughtworks.gocd.elastic-agent.azure");
        ClusterProfile dockerProfile = new ClusterProfile("no-op-cluster-for-cd.go.contrib.elastic-agent.docker", "cd.go.contrib.elastic-agent.docker");
        ClusterProfile kubernetesProfile = new ClusterProfile("no-op-cluster-for-cd.go.contrib.elasticagent.kubernetes", "cd.go.contrib.elasticagent.kubernetes");

        CruiseConfig migratedConfig = migrateConfigAndLoadTheNewConfig(configXml);
        ClusterProfiles newlyDefinedClusters = migratedConfig.getElasticConfig().getClusterProfiles();
        ElasticProfiles migratedElasticAgentProfiles = migratedConfig.getElasticConfig().getProfiles();

        assertThat(newlyDefinedClusters).hasSize(3);
        assertThat(newlyDefinedClusters.find("no-op-cluster-for-com.thoughtworks.gocd.elastic-agent.azure")).isEqualTo(azureProfile);
        assertThat(newlyDefinedClusters.find("no-op-cluster-for-cd.go.contrib.elastic-agent.docker")).isEqualTo(dockerProfile);
        assertThat(newlyDefinedClusters.find("no-op-cluster-for-cd.go.contrib.elasticagent.kubernetes")).isEqualTo(kubernetesProfile);


        ElasticProfile profile1 = new ElasticProfile("profile1", "no-op-cluster-for-cd.go.contrib.elastic-agent.docker", new ConfigurationProperty(new ConfigurationKey("Image"), new ConfigurationValue("alpine:latest")));
        ElasticProfile profile2 = new ElasticProfile("profile2", "no-op-cluster-for-cd.go.contrib.elasticagent.kubernetes", new ConfigurationProperty(new ConfigurationKey("Image"), new ConfigurationValue("alpine:latest")));
        ElasticProfile profile3 = new ElasticProfile("profile3", "no-op-cluster-for-cd.go.contrib.elastic-agent.docker", new ConfigurationProperty(new ConfigurationKey("Image"), new ConfigurationValue("alpine:latest")));
        ElasticProfile profile4 = new ElasticProfile("profile4", "no-op-cluster-for-com.thoughtworks.gocd.elastic-agent.azure", new ConfigurationProperty(new ConfigurationKey("Image"), new ConfigurationValue("alpine:latest")));
        ElasticProfile profile5 = new ElasticProfile("profile5", "no-op-cluster-for-com.thoughtworks.gocd.elastic-agent.azure", new ConfigurationProperty(new ConfigurationKey("Image"), new ConfigurationValue("alpine:latest")));

        assertThat(migratedElasticAgentProfiles.find("profile1")).isEqualTo(profile1);
        assertThat(migratedElasticAgentProfiles.find("profile2")).isEqualTo(profile2);
        assertThat(migratedElasticAgentProfiles.find("profile3")).isEqualTo(profile3);
        assertThat(migratedElasticAgentProfiles.find("profile4")).isEqualTo(profile4);
        assertThat(migratedElasticAgentProfiles.find("profile5")).isEqualTo(profile5);
    }

    private TimerConfig createTimerConfigWithAttribute(String valueForOnChangesInTimer) throws Exception {
        final String content = configWithTimerBasedPipeline(valueForOnChangesInTimer);
        CruiseConfig configAfterMigration = migrateConfigAndLoadTheNewConfig(content);

        PipelineConfig pipelineConfig = configAfterMigration.pipelineConfigByName(new CaseInsensitiveString("old-timer"));
        return pipelineConfig.getTimer();
    }

    private String configWithTimerBasedPipeline(String valueForOnChangesInTimer) {
        return ConfigFileFixture.configWithPipeline("<pipeline name='old-timer'>"
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
    }

    private CruiseConfig migrateConfigAndLoadTheNewConfig(String content) throws Exception {
        FileUtils.writeStringToFile(configFile, content, UTF_8);
        GoConfigHolder configHolder = goConfigMigrator.migrate();
        assert configHolder != null;
        return configHolder.config;
    }

    private CruiseConfig loadConfigFileWithContent(String content) throws Exception {
        FileUtils.writeStringToFile(configFile, content, UTF_8);
        goConfigMigrator.migrate();
        return goFileConfigDataSource.forceLoad(configFile).config;
    }

    private void assertConfiguration(Configuration configuration, List<List> expectedKeyValuePair) {
        int position = 0;
        for (ConfigurationProperty configurationProperty : configuration) {
            assertThat(configurationProperty.getConfigurationKey().getName()).isEqualTo(expectedKeyValuePair.get(position).get(0));
            assertThat(configurationProperty.isSecure()).isEqualTo(expectedKeyValuePair.get(position).get(1));
            assertThat(configurationProperty.getConfigurationValue().getValue()).isEqualTo(expectedKeyValuePair.get(position).get(2));
            position++;
        }
    }
}
