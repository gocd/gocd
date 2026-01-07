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

import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.PackageMaterialConfig;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.git.GitMaterialConfig;
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterialConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.packagerepository.PackageDefinition;
import com.thoughtworks.go.domain.packagerepository.PackageRepositories;
import com.thoughtworks.go.domain.packagerepository.PackageRepository;
import com.thoughtworks.go.domain.packagerepository.Packages;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PartialConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.security.ResetCipher;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.XsdValidationException;
import com.thoughtworks.go.util.command.UrlArgument;
import org.jdom2.JDOMException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.xmlunit.assertj.XmlAssert;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Map;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static com.thoughtworks.go.helper.MaterialConfigsMother.tfs;
import static com.thoughtworks.go.util.GoConstants.CONFIG_SCHEMA_VERSION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(ResetCipher.class)
public class MagicalGoConfigXmlWriterTest {
    private ByteArrayOutputStream output;
    private MagicalGoConfigXmlWriter xmlWriter;
    private MagicalGoConfigXmlLoader xmlLoader;
    private CruiseConfig cruiseConfig;

    @BeforeEach
    public void setup() {
        output = new ByteArrayOutputStream();
        xmlWriter = new MagicalGoConfigXmlWriter(ConfigElementImplementationRegistryMother.withNoPlugins());
        xmlLoader = new MagicalGoConfigXmlLoader(ConfigElementImplementationRegistryMother.withNoPlugins());
        cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.initializeServer();
    }

    @Test
    public void shouldBeAbleToExplicitlyLockAPipeline() throws Exception {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1");
        config.setServerConfig(new ServerConfig("foo", new SecurityConfig()));
        config.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).lockExplicitly();
        xmlWriter.write(config, output, false);
        assertThat(output.toString()).contains("lockBehavior=\"" + PipelineConfig.LOCK_VALUE_LOCK_ON_FAILURE);
    }

    @Test
    public void shouldBeAbleToExplicitlyUnlockAPipeline() throws Exception {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1");
        config.setServerConfig(new ServerConfig("foo", new SecurityConfig()));
        config.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).unlockExplicitly();
        xmlWriter.write(config, output, false);
        assertThat(output.toString()).contains("lockBehavior=\"" + PipelineConfig.LOCK_VALUE_NONE);
    }

    @Test
    public void shouldWriteServerConfig() throws Exception {
        String xml = ConfigFileFixture.SERVER_WITH_ARTIFACTS_DIR_AND_PURGE_SETTINGS;
        CruiseConfig cruiseConfig = xmlLoader.loadConfigHolder(xml).config;
        xmlWriter.write(cruiseConfig, output, false);
        XmlAssert.assertThat(output.toString()).and(xml).normalizeWhitespace().areIdentical();
    }

    @Test
    public void shouldThrowInvalidConfigWhenAttemptedToSaveMergedConfig() {
        String xml = ConfigFileFixture.TWO_PIPELINES;

        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(xml).config;
        PartialConfig remotePart = PartialConfigMother.withPipeline("some-pipe");
        remotePart.setOrigin(new RepoConfigOrigin());
        BasicCruiseConfig merged = new BasicCruiseConfig((BasicCruiseConfig) cruiseConfig, remotePart);
        assertThatThrownBy(() -> xmlWriter.write(merged, output, true))
            .isInstanceOf(GoConfigInvalidException.class)
            .hasMessage("Attempted to save merged configuration with partials");
    }

    @Test
    public void shouldWritePipelines() throws Exception {
        String xml = ConfigFileFixture.TWO_PIPELINES;

        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(xml).config;
        xmlWriter.write(cruiseConfig, output, false);
        XmlAssert.assertThat(output.toString()).and(xml).normalizeWhitespace().areIdentical();
    }

    @Test
    public void shouldNotWriteDuplicatedPipelines() {
        String xml = ConfigFileFixture.TWO_PIPELINES;

        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(xml).config;
        cruiseConfig.addPipeline("someGroup", PipelineConfigMother.pipelineConfig("pipeline1"));
        assertThatThrownBy(() -> xmlWriter.write(cruiseConfig, output, false))
            .hasMessageContaining("You have defined multiple pipelines named 'pipeline1'. Pipeline names must be unique. Source(s): [cruise-config.xml]");
    }

    @Test
    public void shouldNotDeleteServerTag() throws Exception {
        CruiseConfig config = ConfigMigrator.load(ConfigFileFixture.SERVER_TAG_WITH_DEFAULTS_PLUS_LICENSE_TAG);

        xmlWriter.write(config, output, false);
        assertThat(output.toString()).contains("<server");
    }

    @Test
    public void shouldWriteConfigRepos() throws Exception {
        CruiseConfig config = GoConfigMother.configWithConfigRepo();
        config.initializeServer();
        xmlWriter.write(config, output, false);

        assertThat(output.toString()).contains("<config-repo id=\"id2\" pluginId=\"myplugin\">");
        assertThat(output.toString()).contains("<git url=\"https://github.com/tomzo/gocd-indep-config-part.git\" />");
    }

    @Test
    public void shouldNotWriteDependenciesIfEmptyDependencies() throws Exception {
        String xml = ConfigFileFixture.EMPTY_DEPENDENCIES;

        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(xml).config;
        xmlWriter.write(cruiseConfig, output, false);
        assertThat(output.toString().replaceAll(">\\s+<", "")).doesNotContain("dependencies");
    }

    @Test
    public void shouldNotWriteWhenEnvironmentNameIsNotSet() {
        String xml = ConfigFileFixture.CONFIG_WITH_NANT_AND_EXEC_BUILDER;

        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(xml).config;
        cruiseConfig.addEnvironment(new BasicEnvironmentConfig());
        assertThatThrownBy(() -> xmlWriter.write(cruiseConfig, output, false))
            .hasMessageContaining("\"Name\" is required for Environment");
    }

    @Test
    public void shouldValidateThatEnvironmentsAreSameEvenNamesAreOfDifferentCase() {
        String xml = ConfigFileFixture.WITH_DUPLICATE_ENVIRONMENTS;
        assertThatThrownBy(() -> ConfigMigrator.loadWithMigration(xml))
            .hasMessageContaining("Environment with name 'FOO' already exists.");
    }

    @Test
    public void shouldWriteConfigWithTemplates() throws Exception {
        String content = ("""
                <cruise schemaVersion='%d'>
                <server>
                     <artifacts>
                           <artifactsDir>artifactsDir</artifactsDir>
                     </artifacts>
                </server>
                <pipelines>
                <pipeline name='pipeline1' template='abc'>
                    <materials>
                      <svn url ="svnurl"/>
                    </materials>
                </pipeline>
                <pipeline name='pipeline2'>
                    <materials>
                      <pipeline pipelineName='pipeline1' stageName='stage1'/>
                    </materials>
                    <stage name='badstage'>
                      <jobs>
                        <job name='job1'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                      </jobs>
                    </stage>
                </pipeline>
                </pipelines>
                <templates>
                  <pipeline name='abc'>
                    <stage name='stage1'>
                      <jobs>
                        <job name='job1'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                      </jobs>
                    </stage>
                  </pipeline>
                </templates>
                </cruise>""").formatted(CONFIG_SCHEMA_VERSION);
        CruiseConfig config = ConfigMigrator.loadWithMigration(content).configForEdit;
        xmlWriter.write(config, output, false);
        assertThat(output.toString().replaceAll("\\s+", " ")).contains(
            """
                <pipeline name="pipeline1" template="abc"> <materials> <svn url="svnurl" /> </materials> </pipeline>""");
    }

    @Test
    public void shouldWriteObjectToXmlPartial() {
        String xml = ConfigFileFixture.ONE_PIPELINE;
        CruiseConfig cruiseConfig = ConfigMigrator.load(xml);
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        StageConfig stageConfig = pipelineConfig.findBy(new CaseInsensitiveString("stage"));
        JobConfig build = stageConfig.jobConfigByInstanceName("functional", true);

        assertThat(xmlWriter.toXmlPartial(pipelineConfig)).isEqualTo(
                """
                        <pipeline name="pipeline1">
                          <materials>
                            <svn url="foobar" checkexternals="true" />
                          </materials>
                          <stage name="stage">
                            <jobs>
                              <job name="functional">
                                <tasks>
                                  <ant />
                                </tasks>
                                <artifacts>
                                  <artifact type="build" src="artifact1.xml" dest="cruise-output" />
                                </artifacts>
                              </job>
                            </jobs>
                          </stage>
                        </pipeline>"""
        );

        assertThat(xmlWriter.toXmlPartial(stageConfig)).isEqualTo(
                """
                        <stage name="stage">
                          <jobs>
                            <job name="functional">
                              <tasks>
                                <ant />
                              </tasks>
                              <artifacts>
                                <artifact type="build" src="artifact1.xml" dest="cruise-output" />
                              </artifacts>
                            </job>
                          </jobs>
                        </stage>"""
        );

        assertThat(xmlWriter.toXmlPartial(build)).isEqualTo(
                """
                        <job name="functional">
                          <tasks>
                            <ant />
                          </tasks>
                          <artifacts>
                            <artifact type="build" src="artifact1.xml" dest="cruise-output" />
                          </artifacts>
                        </job>"""
        );
    }

    @Test
    public void shouldWriteEmptyOnCancelTaskWhenDefined() throws Exception {
        String partial = """
                <job name="functional">
                  <tasks>
                    <exec command="echo">
                      <oncancel />
                    </exec>
                  </tasks>
                </job>""";
        JobConfig jobConfig = xmlLoader.fromXmlPartial(partial, JobConfig.class);
        assertThat(xmlWriter.toXmlPartial(jobConfig)).isEqualTo(partial);
    }

    @Test
    public void shouldBeAValidXSD() throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        try (InputStream xsdStream = getClass().getResourceAsStream("/cruise-config.xsd")) {
            factory.newSchema(new StreamSource(xsdStream));
        }
    }

    @Test
    public void shouldFailWhenWritingObjectToXmlPartialWithNoConfigTag() {
        Object badObject = "foo";
        assertThatThrownBy(() -> xmlWriter.toXmlPartial(badObject))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Object " + badObject + " does not have a ConfigTag");
    }

    @Test
    public void shouldNotSaveUserNameAndPasswordWhenBothAreEmpty() {
        MailHost mailHost = new MailHost("hostname", 24, "", "", null, true, false, "from@te.com", "to@te.com", new GoCipher());
        mailHost.ensureEncrypted();
        String s = xmlWriter.toXmlPartial(mailHost);
        assertThat(s).isEqualTo(
                "<mailhost hostname=\"hostname\" port=\"24\" "
                        + "from=\"from@te.com\" admin=\"to@te.com\" />");
    }

    @Test
    public void shouldEncryptPasswordBeforeWriting(ResetCipher resetCipher) throws Exception {
        resetCipher.setupDESCipherFile();
        String content = ("""
                <cruise schemaVersion='%d'>
                <server>
                    <artifacts>
                        <artifactsDir>artifactsDir</artifactsDir>
                    </artifacts>
                    <mailhost hostname="10.18.3.171" port="25" username="cruise2" password="password" tls="false" from="cruise2@cruise.com" admin="ps@somewhere.com" />
                </server>
                <pipelines>
                <pipeline name='pipeline1' template='abc'>
                    <materials>
                      <svn url ='svnurl' username='foo' password='password'/>
                    </materials>
                </pipeline>
                </pipelines>
                <templates>
                  <pipeline name='abc'>
                    <stage name='stage1'>
                      <jobs>
                        <job name='job1'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                      </jobs>
                    </stage>
                  </pipeline>
                </templates>
                </cruise>""").formatted(CONFIG_SCHEMA_VERSION);
        CruiseConfig config = ConfigMigrator.loadWithMigration(content).configForEdit;
        xmlWriter.write(config, output, false);
        assertThat(output.toString().replaceAll("\\s+", " ")).contains(
                "<svn url=\"svnurl\" username=\"foo\" encryptedPassword=\"" + new GoCipher().encrypt("password") + "\" />");
        assertThat(output.toString().replaceAll("\\s+", " ")).contains(
                "<mailhost hostname=\"10.18.3.171\" port=\"25\" username=\"cruise2\" encryptedPassword=\"" + new GoCipher().encrypt("password") + "\" from=\"cruise2@cruise.com\" admin=\"ps@somewhere.com\" />");
    }

    @Test
    public void shouldWriteP4MaterialToXmlPartial() throws Exception {
        String encryptedPassword = new GoCipher().encrypt("password");
        P4MaterialConfig p4MaterialConfig = com.thoughtworks.go.helper.MaterialConfigsMother.p4MaterialConfig();
        p4MaterialConfig.setPassword("password");
        p4MaterialConfig.setConfigAttributes(Map.of(
                P4MaterialConfig.SERVER_AND_PORT, "localhost:1666",
                P4MaterialConfig.USERNAME, "cruise",
                P4MaterialConfig.VIEW, "//depot/dir1/... //lumberjack/...",
                P4MaterialConfig.AUTO_UPDATE, "true"));
        assertThat(xmlWriter.toXmlPartial(p4MaterialConfig)).isEqualTo(
                ("""
                        <p4 port="localhost:1666" username="cruise" encryptedPassword="%s">
                          <view><![CDATA[//depot/dir1/... //lumberjack/...]]></view>
                        </p4>""").formatted(encryptedPassword));
    }

    @Test
    public void shouldWriteSvnMaterialToXmlPartial() throws Exception {
        String encryptedPassword = new GoCipher().encrypt("password");
        SvnMaterialConfig material = com.thoughtworks.go.helper.MaterialConfigsMother.svnMaterialConfig("http://user:pass@svn", null, "cruise", "password", false, null);
        assertThat(xmlWriter.toXmlPartial(material)).isEqualTo(
                "<svn url=\"http://user:pass@svn\" username=\"cruise\" encryptedPassword=\"" + encryptedPassword + "\" materialName=\"http___user_pass@svn\" />");
    }

    @Test
    public void shouldWriteHgMaterialToXmlPartial() {
        HgMaterialConfig material = com.thoughtworks.go.helper.MaterialConfigsMother.hgMaterialConfig();
        material.setConfigAttributes(Map.of(HgMaterialConfig.URL, "http://user:pass@hg", HgMaterialConfig.AUTO_UPDATE, "true"));
        assertThat(xmlWriter.toXmlPartial(material)).isEqualTo("<hg url=\"http://user:pass@hg\" />");
    }

    @Test
    public void shouldWriteGitMaterialToXmlPartial() {
        GitMaterialConfig gitMaterial = git("gitUrl");
        assertThat(xmlWriter.toXmlPartial(gitMaterial)).isEqualTo("<git url=\"gitUrl\" />");
    }

    @Test
    public void shouldWritePipelineGroupAdmins() throws Exception {
        String content = ConfigFileFixture.configWithPipelines("""
                <pipelines group="first">
                <authorization>
                     <admins>
                         <user>foo</user>
                      </admins>
                </authorization>
                <pipeline name='pipeline1'>
                    <materials>
                      <svn url ="svnurl"/>
                    </materials>
                <stage name='stage'><jobs><job name='job'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job></jobs></stage>
                </pipeline>
                </pipelines>
                """, CONFIG_SCHEMA_VERSION);
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        xmlWriter.write(cruiseConfig, out, false);
        assertThat(out.toString()).contains("<admins>");
        assertThat(out.toString()).contains("<user>foo</user>");
    }

    @Test
    public void shouldAllowParamsInsidePipeline() throws Exception {
        String content = ("""
                <?xml version="1.0" encoding="utf-8"?>
                <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"      xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion='%d'>
                <server>
                     <artifacts>
                           <artifactsDir>artifactsDir</artifactsDir>
                      </artifacts>
                </server>
                <pipelines>
                <pipeline name='framework'>
                    <params>
                      <param name='first'>foo</param>
                      <param name='second'>bar</param>
                    </params>
                    <materials>
                      <svn url ="svnurl"/>
                    </materials>
                  <stage name='dist' fetchMaterials='true'>
                    <jobs>
                      <job name='package'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                    </jobs>
                  </stage>
                </pipeline>
                </pipelines>
                </cruise>""").formatted(CONFIG_SCHEMA_VERSION);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("framework"));
        ParamsConfig params = pipelineConfig.getParams();
        assertThat(params.getParamNamed("first")).isEqualTo(new ParamConfig("first", "foo"));
        assertThat(params.getParamNamed("second")).isEqualTo(new ParamConfig("second", "bar"));
        assertThat(params.getParamNamed("third")).isNull();

        params.remove(0);

        xmlWriter.write(cruiseConfig, out, false);
        assertThat(out.toString()).doesNotContain("<param name=\"first\">foo</param>");
        assertThat(out.toString()).contains("<param name=\"second\">bar</param>");
    }

    @Test
    public void shouldWriteFetchMaterialsFlagToStage() throws Exception {
        String content = ("""
                <?xml version="1.0" encoding="utf-8"?>
                <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"      xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion='%d'>
                <server>
                     <artifacts>
                           <artifactsDir>artifactsDir</artifactsDir>
                     </artifacts>
                </server>
                <pipelines>
                <pipeline name='framework'>
                    <materials>
                      <svn url ="svnurl"/>
                    </materials>
                  <stage name='dist' fetchMaterials='true'>
                    <jobs>
                      <job name='package'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                    </jobs>
                  </stage>
                </pipeline>
                </pipelines>
                </cruise>""").formatted(CONFIG_SCHEMA_VERSION);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        StageConfig stageConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("framework")).get(0);

        assertThat(stageConfig.isFetchMaterials()).isTrue();
        stageConfig.setFetchMaterials(false);
        xmlWriter.write(cruiseConfig, out, false);
        assertThat(out.toString()).contains("fetchMaterials=\"false\"");
    }

    @Test
    public void shouldWriteCleanWorkingDirFlagToStage() throws Exception {
        String content = ("""
                <?xml version="1.0" encoding="utf-8"?>
                <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"      xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion='%d'>
                <server>
                     <artifacts>
                           <artifactsDir>artifactsDir</artifactsDir>
                     </artifacts>
                </server>
                <pipelines>
                <pipeline name='framework'>
                    <materials>
                      <svn url ="svnurl"/>
                    </materials>
                  <stage name='dist' cleanWorkingDir='false'>
                    <jobs>
                      <job name='package'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                    </jobs>
                  </stage>
                </pipeline>
                </pipelines>
                </cruise>""").formatted(CONFIG_SCHEMA_VERSION);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        xmlWriter.write(cruiseConfig, out, false);
        assertThat(out.toString()).doesNotContain("cleanWorkingDir");

        StageConfig stageConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("framework")).get(0);
        stageConfig.setCleanWorkingDir(true);
        xmlWriter.write(cruiseConfig, out, false);
        assertThat(out.toString()).contains("cleanWorkingDir=\"true\"");
    }

    @Test
    public void shouldWriteArtifactPurgeSettings() throws Exception {
        String content = ("""
                <?xml version="1.0" encoding="utf-8"?>
                <cruise xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:noNamespaceSchemaLocation="cruise-config.xsd" schemaVersion='%d'>
                <server>
                     <artifacts>
                           <artifactsDir>other-artifacts</artifactsDir>
                     </artifacts>
                </server>
                <pipelines>
                <pipeline name='framework'>
                    <materials>
                      <svn url ="svnurl"/>
                    </materials>
                  <stage name='dist'>
                    <jobs>
                      <job name='package'><tasks><exec command='echo'><runif status='passed' /></exec></tasks></job>
                    </jobs>
                  </stage>
                </pipeline>
                </pipelines>
                </cruise>""").formatted(CONFIG_SCHEMA_VERSION);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        StageConfig stageConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("framework")).get(0);
        ReflectionUtil.setField(stageConfig, "artifactCleanupProhibited", false);
        xmlWriter.write(cruiseConfig, out, false);
        assertThat(out.toString()).doesNotContain("artifactCleanupProhibited=\"true\"");
        ReflectionUtil.setField(stageConfig, "artifactCleanupProhibited", true);
        xmlWriter.write(cruiseConfig, out, false);
        assertThat(out.toString()).contains("artifactCleanupProhibited=\"true\"");

        assertThat(out.toString()).doesNotContain("purgeStart=\"10.0\"");
        assertThat(out.toString()).doesNotContain("purgeUpto=\"20.0\"");

        cruiseConfig.server().setPurgeLimits(10.0, 20.0);

        xmlWriter.write(cruiseConfig, out, false);

        assertThat(out.toString()).contains("<purgeStartDiskSpace>10.0</purgeStartDiskSpace>");
        assertThat(out.toString()).contains("<purgeUptoDiskSpace>20.0</purgeUptoDiskSpace>");
    }

    @Test
    public void shouldRemoveDuplicatedIgnoreTag() {
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.TWO_DUPLICATED_FILTER);

        int size = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).materialConfigs().getFirstOrNull().filter().size();
        assertThat(size).isEqualTo(1);
    }

    @Test
    public void shouldNotAllowEmptyAuthInApproval() throws Exception {
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.ONE_PIPELINE);
        StageConfig stageConfig = com.thoughtworks.go.helper.StageConfigMother.custom("newStage", new AuthConfig());
        cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).add(stageConfig);

        try {
            xmlWriter.write(cruiseConfig, output, false);
            assertThat(output.toString().contains("<auth")).isFalse();
        } catch (JDOMException expected) {
            assertThat(expected.getMessage()).contains("The content of element 'auth' is not complete");
        }
    }

    @Test
    public void shouldNotWriteTrackingToolIfItIsNotDefined() throws Exception {
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.ONE_PIPELINE);
        xmlWriter.write(cruiseConfig, output, false);

        assertThat(output.toString()).doesNotContain("<trackingtool");
    }

    @Test
    public void shouldNotDefineATrackingToolWithoutALink() {
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.ONE_PIPELINE);
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        pipelineConfig.setTrackingTool(new TrackingTool("", "regex"));
        assertThatThrownBy(() -> xmlWriter.write(cruiseConfig, output, false))
            .hasMessageContaining("Link should be populated");
    }

    @Test
    public void shouldSkipValidationIfExplicitlyToldWhileWritingConfig() throws Exception {
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.ONE_PIPELINE);
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        pipelineConfig.addEnvironmentVariable("name1", "value1");
        pipelineConfig.addEnvironmentVariable("name1", "value1");
        xmlWriter.write(cruiseConfig, output, true);
        assertThat(cruiseConfig.errors().isEmpty()).isTrue();
    }

    @Test
    public void shouldNotDefineATrackingToolWithoutARegex() {
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.ONE_PIPELINE);
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        pipelineConfig.setTrackingTool(new TrackingTool("link", ""));
        assertThatThrownBy(() -> xmlWriter.write(cruiseConfig, output, false))
            .hasMessageContaining("Regex should be populated");
    }

    @Test
    public void shouldWriteConfigWithMultipleGroups() throws Exception {
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.PIPELINE_GROUPS);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        xmlWriter.write(cruiseConfig, buffer, false);

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer.toByteArray());
        CruiseConfig config = xmlLoader.loadConfigHolder(new String(inputStream.readAllBytes(), UTF_8)).config;
        assertThat(config.getGroups().size()).isEqualTo(2);
        assertThat(config.getGroups().getFirstOrNull().getGroup()).isEqualTo("studios");
    }

    @Test
    public void shouldWriteConfigWithTaskExecutionConditions() throws Exception {
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.TASKS_WITH_CONDITION);

        xmlWriter.write(cruiseConfig, output, false);

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());
        CruiseConfig config = xmlLoader.loadConfigHolder(new String(inputStream.readAllBytes(), UTF_8)).config;
        JobConfig job = config.jobConfigByName("pipeline1", "mingle", "cardlist", true);

        assertThat(job.tasks().size()).isEqualTo(2);
        assertThat(job.tasks().findFirstByType(AntTask.class).getConditions().get(0)).isEqualTo(new RunIfConfig("failed"));

        RunIfConfigs conditions = job.tasks().findFirstByType(NantTask.class).getConditions();
        assertThat(conditions.get(0)).isEqualTo(new RunIfConfig("failed"));
        assertThat(conditions.get(1)).isEqualTo(new RunIfConfig("any"));
        assertThat(conditions.get(2)).isEqualTo(new RunIfConfig("passed"));
    }

    @Test
    // #3098
    public void shouldAlwaysWriteArtifactsDir() throws Exception {
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.DEFAULT_XML_WITH_2_AGENTS);

        xmlWriter.write(cruiseConfig, output, false);

        assertThat(xmlWriter.toXmlPartial(cruiseConfig.server())).contains("<artifactsDir>artifactsDir</artifactsDir>");
    }

    @Test
    public void shouldThrowExceptionWhenPersisInvalidDom() throws Exception {
        //simulate the xml partial saving logic
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.CONTAINS_MULTI_DIFFERENT_STATUS_RUN_IF);
        StageConfig stage = xmlLoader.fromXmlPartial(ConfigFileFixture.SAME_STATUS_RUN_IF_PARTIAL, StageConfig.class);
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("test"));
        pipelineConfig.set(0, stage);
        assertThatThrownBy(() -> xmlWriter.write(cruiseConfig, output, false))
            .hasMessageContaining("Duplicate unique value [passed] declared for identity constraint");
    }

    @Test
    public void shouldNotThrowUpWhenTfsWorkspaceIsNotSpecified() throws Exception {
        CruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("tfs_pipeline");
        cruiseConfig.initializeServer();
        PipelineConfig tfs_pipeline = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("tfs_pipeline"));
        tfs_pipeline.materialConfigs().clear();
        tfs_pipeline.addMaterialConfig(tfs(new GoCipher(), new UrlArgument("http://tfs.com"), "username", "CORPORATE", "password", "$/project_path"));
        xmlWriter.write(cruiseConfig, output, false);
    }

    @Test
    public void shouldSerialize_CaseInsensitiveString_whenUsedInConfigAttributeValue() throws Exception {//for instance FetchTask uses PathFromAncestor which has CaseInsensitiveString
        CruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("uppest", "upper", "downer", "downest");
        cruiseConfig.initializeServer();
        setDependencyOn(cruiseConfig, "upper", "uppest", "stage");
        setDependencyOn(cruiseConfig, "downer", "upper", "stage");
        setDependencyOn(cruiseConfig, "downest", "downer", "stage");
        PipelineConfig downest = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("downest"));
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("uppest/upper/downer"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "src", "dest");
        downest.add(com.thoughtworks.go.helper.StageConfigMother.stageConfig("stage-2", new JobConfigs(new JobConfig(new CaseInsensitiveString("downloader"), new ResourceConfigs(), new ArtifactTypeConfigs(), new Tasks(fetchTask)))));

        xmlWriter.write(cruiseConfig, output, false);

        assertThat(output.toString()).contains("<fetchartifact artifactOrigin=\"gocd\" srcfile=\"src\" dest=\"dest\" pipeline=\"uppest/upper/downer\" stage=\"stage\" job=\"job\" />");
    }

    @Test
    public void shouldWriteRepositoryConfigurationWithPackages() throws Exception {
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.setId("id");
        packageRepository.setName("name");
        packageRepository.setPluginConfiguration(new PluginConfiguration("plugin-id", "version"));
        packageRepository.setConfiguration(new Configuration(getConfigurationProperty("url", false, "http://go"), getConfigurationProperty("secure", true, "secure")));
        PackageDefinition packageDefinition = new PackageDefinition("id", "name", new Configuration(getConfigurationProperty("name", false, "go-agent")));
        packageDefinition.setRepository(packageRepository);
        packageRepository.getPackages().add(packageDefinition);

        cruiseConfig.getPackageRepositories().add(packageRepository);

        xmlWriter.write(cruiseConfig, output, false);

        GoConfigHolder goConfigHolder = xmlLoader.loadConfigHolder(output.toString());

        PackageRepositories packageRepositories = goConfigHolder.config.getPackageRepositories();
        assertThat(packageRepositories).isEqualTo(cruiseConfig.getPackageRepositories());
        assertThat(packageRepositories.get(0).getConfiguration().getFirstOrNull().getConfigurationValue().getValue()).isEqualTo("http://go");
        assertThat(packageRepositories.get(0).getConfiguration().getFirstOrNull().getEncryptedConfigurationValue()).isNull();
        assertThat(packageRepositories.get(0).getConfiguration().getLastOrNull().getEncryptedValue()).isEqualTo(new GoCipher().encrypt("secure"));
        assertThat(packageRepositories.get(0).getConfiguration().getLastOrNull().getConfigurationValue()).isNull();
        assertThat(packageRepositories.get(0).getPackages().get(0)).isEqualTo(packageDefinition);
        assertThat(packageRepositories.get(0).getPackages().get(0).getConfiguration().getFirstOrNull().getConfigurationValue().getValue()).isEqualTo("go-agent");
        assertThat(packageRepositories.get(0).getPackages().get(0).getConfiguration().getFirstOrNull().getEncryptedConfigurationValue()).isNull();
    }

    @Test
    public void shouldWriteRepositoryConfigurationWithPackagesWhenNoRepoAndPkgIdIsProvided() throws Exception {
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.setName("name");
        packageRepository.setPluginConfiguration(new PluginConfiguration("plugin-id", "version"));
        packageRepository.setConfiguration(new Configuration(getConfigurationProperty("url", false, "http://go"), getConfigurationProperty("secure", true, "secure")));
        PackageDefinition packageDefinition = new PackageDefinition(null, "name", new Configuration(getConfigurationProperty("name", false, "go-agent")));
        packageDefinition.setRepository(packageRepository);
        packageRepository.getPackages().add(packageDefinition);

        cruiseConfig.getPackageRepositories().add(packageRepository);

        xmlWriter.write(cruiseConfig, output, false);

        GoConfigHolder goConfigHolder = xmlLoader.loadConfigHolder(output.toString());

        PackageRepositories packageRepositories = goConfigHolder.config.getPackageRepositories();
        assertThat(packageRepositories.size()).isEqualTo(cruiseConfig.getPackageRepositories().size());
        assertThat(packageRepositories.get(0).getId()).isNotNull();
        assertThat(packageRepositories.get(0).getPackages().size()).isEqualTo(1);
        assertThat(packageRepositories.get(0).getPackages().get(0).getId()).isNotNull();
    }

    @Test
    public void shouldNotAllowMultipleRepositoriesWithSameId() {
        Configuration packageConfiguration = new Configuration(getConfigurationProperty("name", false, "go-agent"));
        Configuration repositoryConfiguration = new Configuration(getConfigurationProperty("url", false, "http://go"));

        PackageRepository packageRepository = createPackageRepository("plugin-id-1", "version", "id", "name1", repositoryConfiguration,
                new Packages(new PackageDefinition("id", "name", packageConfiguration)));

        PackageRepository anotherPackageRepository = createPackageRepository("plugin-id-2", "version", "id", "name2", repositoryConfiguration,
                new Packages(new PackageDefinition("id", "name", packageConfiguration)));

        cruiseConfig.setPackageRepositories(new PackageRepositories(packageRepository, anotherPackageRepository));
        assertThatThrownBy(() -> xmlWriter.write(cruiseConfig, output, false))
            .isInstanceOf(XsdValidationException.class)
            .hasMessageContaining("Duplicate unique value [id] declared for identity constraint");
    }

    @Test
    public void shouldNotAllowMultiplePackagesWithSameId() {
        Configuration packageConfiguration = new Configuration(getConfigurationProperty("name", false, "go-agent"));
        Configuration repositoryConfiguration = new Configuration(getConfigurationProperty("url", false, "http://go"));

        PackageRepository packageRepository = createPackageRepository("plugin-id-1", "version", "id1", "name1", repositoryConfiguration,
                new Packages(new PackageDefinition("id", "name", packageConfiguration)));

        PackageRepository anotherPackageRepository = createPackageRepository("plugin-id-2", "version", "id2", "name2", repositoryConfiguration,
                new Packages(new PackageDefinition("id", "name", packageConfiguration)));

        cruiseConfig.setPackageRepositories(new PackageRepositories(packageRepository, anotherPackageRepository));
        assertThatThrownBy(() -> xmlWriter.write(cruiseConfig, output, false))
            .isInstanceOf(XsdValidationException.class)
            .hasMessageContaining("Duplicate unique value [id] declared for identity constraint");
    }

    @Test
    public void shouldAllowPackageTypeMaterialForPipeline() throws Exception {
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.setId("id");
        packageRepository.setName("name");
        packageRepository.setPluginConfiguration(new PluginConfiguration("plugin-id", "version"));
        packageRepository.setConfiguration(new Configuration(getConfigurationProperty("url", false, "http://go")));
        String packageId = "id";
        PackageDefinition expectedPackageDefinition = new PackageDefinition(packageId, "name", new Configuration(getConfigurationProperty("name", false, "go-agent")));
        expectedPackageDefinition.setRepository(packageRepository);
        packageRepository.getPackages().add(expectedPackageDefinition);


        cruiseConfig.getPackageRepositories().add(packageRepository);
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig(packageId);
        packageMaterialConfig.setPackageDefinition(expectedPackageDefinition);

        JobConfig jobConfig = new JobConfig("ls");
        jobConfig.addTask(new AntTask());
        cruiseConfig.addPipeline("default", com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig("test", new MaterialConfigs(packageMaterialConfig), new JobConfigs(jobConfig)));
        xmlWriter.write(cruiseConfig, output, false);
        GoConfigHolder goConfigHolder = xmlLoader.loadConfigHolder(output.toString());
        PipelineConfig pipelineConfig = goConfigHolder.config.pipelineConfigByName(new CaseInsensitiveString("test"));
        assertThat(pipelineConfig.materialConfigs().get(0) instanceof PackageMaterialConfig).isTrue();
        assertThat(((PackageMaterialConfig) pipelineConfig.materialConfigs().get(0)).getPackageId()).isEqualTo(packageId);
        PackageDefinition packageDefinition = goConfigHolder.config.getPackageRepositories().getFirstOrNull().getPackages().getFirstOrNull();
        assertThat(((PackageMaterialConfig) pipelineConfig.materialConfigs().get(0)).getPackageDefinition()).isEqualTo(packageDefinition);
    }

    @Test
    public void shouldFailValidationIfPackageTypeMaterialForPipelineHasARefToNonExistantPackage() {
        String packageId = "does-not-exist";
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig(packageId);
        PackageRepository repository = com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", new Configuration(com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create("k1", false, "v1")));
        packageMaterialConfig.setPackageDefinition(
                com.thoughtworks.go.domain.packagerepository.PackageDefinitionMother.create("does-not-exist", "package-name", new Configuration(com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create("k2", false, "v2")), repository));

        JobConfig jobConfig = new JobConfig("ls");
        jobConfig.addTask(new AntTask());
        cruiseConfig.addPipeline("default", com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig("test", new MaterialConfigs(packageMaterialConfig), new JobConfigs(jobConfig)));
        assertThatThrownBy(() -> xmlWriter.write(cruiseConfig, output, false))
            .isInstanceOf(XsdValidationException.class)
            .hasMessage("Key 'packageIdReferredByMaterial' with value 'does-not-exist' not found for identity constraint of element 'cruise'.");
    }

    @Test
    public void shouldNotAllowMultipleRepositoriesWithSameName() {
        Configuration packageConfiguration = new Configuration(getConfigurationProperty("name", false, "go-agent"));
        Configuration repositoryConfiguration = new Configuration(getConfigurationProperty("url", false, "http://go"));

        PackageRepository packageRepository = createPackageRepository("plugin-id", "version", "id1", "name", repositoryConfiguration,
                new Packages(new PackageDefinition("id1", "name1", packageConfiguration)));

        PackageRepository anotherPackageRepository = createPackageRepository("plugin-id", "version", "id2", "name", repositoryConfiguration,
                new Packages(new PackageDefinition("id2", "name2", packageConfiguration)));

        cruiseConfig.setPackageRepositories(new PackageRepositories(packageRepository, anotherPackageRepository));
        assertThatThrownBy(() -> xmlWriter.write(cruiseConfig, output, false))
            .isInstanceOf(GoConfigInvalidException.class)
            .hasMessage("You have defined multiple repositories called 'name'. Repository names are case-insensitive and must be unique.");
    }

    @Test
    public void shouldNotAllowMultiplePackagesWithSameNameWithinARepo() {
        Configuration packageConfiguration1 = new Configuration(getConfigurationProperty("name", false, "go-agent"));
        Configuration packageConfiguration2 = new Configuration(getConfigurationProperty("name2", false, "go-server"));
        Configuration repositoryConfiguration = new Configuration(getConfigurationProperty("url", false, "http://go"));

        PackageRepository packageRepository = createPackageRepository("plugin-id", "version", "id", "name", repositoryConfiguration,
                new Packages(new PackageDefinition("id1", "name", packageConfiguration1), new PackageDefinition("id2", "name", packageConfiguration2)));

        cruiseConfig.setPackageRepositories(new PackageRepositories(packageRepository));
        assertThatThrownBy(() -> xmlWriter.write(cruiseConfig, output, false))
            .isInstanceOf(GoConfigInvalidException.class)
            .hasMessage("You have defined multiple packages called 'name'. Package names are case-insensitive and must be unique within a repository.");
    }

    @Test
    public void shouldNotAllowPackagesRepositoryWithInvalidId() {
        Configuration packageConfiguration = new Configuration(getConfigurationProperty("name", false, "go-agent"));
        Configuration repositoryConfiguration = new Configuration(getConfigurationProperty("url", false, "http://go"));

        PackageRepository packageRepository = createPackageRepository("plugin-id", "version", "id wth space", "name", repositoryConfiguration,
                new Packages(new PackageDefinition("id", "name", packageConfiguration)));

        cruiseConfig.setPackageRepositories(new PackageRepositories(packageRepository));
        assertThatThrownBy(() -> xmlWriter.write(cruiseConfig, output, false))
            .isInstanceOf(XsdValidationException.class)
            .hasMessage("Repo id is invalid. \"id wth space\" should conform to the pattern - [a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*");
    }

    @Test
    public void shouldNotAllowPackagesRepositoryWithInvalidName() {
        Configuration packageConfiguration = new Configuration(getConfigurationProperty("name", false, "go-agent"));
        Configuration repositoryConfiguration = new Configuration(getConfigurationProperty("url", false, "http://go"));

        PackageRepository packageRepository = createPackageRepository("plugin-id", "version", "id", "name with space", repositoryConfiguration,
                new Packages(new PackageDefinition("id", "name", packageConfiguration)));

        cruiseConfig.setPackageRepositories(new PackageRepositories(packageRepository));
        assertThatThrownBy(() -> xmlWriter.write(cruiseConfig, output, false))
            .isInstanceOf(GoConfigInvalidException.class)
            .hasMessage("Invalid PackageRepository name 'name with space'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
    }

    @Test
    public void shouldNotAllowPackagesWithInvalidId() {
        Configuration packageConfiguration = new Configuration(getConfigurationProperty("name", false, "go-agent"));
        Configuration repositoryConfiguration = new Configuration(getConfigurationProperty("url", false, "http://go"));

        PackageRepository packageRepository = createPackageRepository("plugin-id", "version", "id", "name", repositoryConfiguration,
                new Packages(new PackageDefinition("id with space", "name", packageConfiguration)));

        cruiseConfig.setPackageRepositories(new PackageRepositories(packageRepository));
        assertThatThrownBy(() -> xmlWriter.write(cruiseConfig, output, false))
            .isInstanceOf(XsdValidationException.class)
            .hasMessage("Package id is invalid. \"id with space\" should conform to the pattern - [a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*");
    }

    @Test
    public void shouldNotWriteToFileWithDefaultValueOfTrueForPackageDefinitionAutoUpdateWhenTrue() throws Exception {
        Configuration configuration = new Configuration(getConfigurationProperty("url", false, "http://go"));

        Packages packages = new Packages();
        PackageRepository repository = createPackageRepository("plugin-id", "version", "id", "name", configuration, packages);
        PackageDefinition aPackage = new PackageDefinition("package-id", "package-name", configuration);
        aPackage.setAutoUpdate(true);
        packages.add(aPackage);
        aPackage.setRepository(repository);
        cruiseConfig.setPackageRepositories(new PackageRepositories(repository));

        xmlWriter.write(cruiseConfig, output, false);

        assertThat(output.toString().contains("autoUpdate=\"true\"")).isFalse();
    }

    @Test
    public void shouldWriteToFileWithValueOfFalseForPackageDefinitionAutoUpdateWhenFalse() throws Exception {
        Configuration configuration = new Configuration(getConfigurationProperty("url", false, "http://go"));

        Packages packages = new Packages();
        PackageDefinition aPackage = new PackageDefinition("package-id", "package-name", configuration);
        aPackage.setAutoUpdate(false);
        packages.add(aPackage);
        PackageRepository repository = createPackageRepository("plugin-id", "version", "id", "name", configuration, packages);
        cruiseConfig.setPackageRepositories(new PackageRepositories(repository));

        xmlWriter.write(cruiseConfig, output, false);

        assertThat(output.toString().contains("autoUpdate=\"false\"")).isTrue();
    }

    @Test
    public void shouldNotAllowPackagesWithInvalidName() {
        Configuration packageConfiguration = new Configuration(getConfigurationProperty("name", false, "go-agent"));
        Configuration repositoryConfiguration = new Configuration(getConfigurationProperty("url", false, "http://go"));

        PackageRepository packageRepository = createPackageRepository("plugin-id", "version", "id", "name", repositoryConfiguration,
                new Packages(new PackageDefinition("id", "name with space", packageConfiguration)));

        cruiseConfig.setPackageRepositories(new PackageRepositories(packageRepository));
        assertThatThrownBy(() -> xmlWriter.write(cruiseConfig, output, false))
            .isInstanceOf(GoConfigInvalidException.class)
            .hasMessage("Invalid Package name 'name with space'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
    }

    @Test
    public void shouldNotWriteEmptyAuthorizationUnderEachTemplateTagOntoConfigFile() throws Exception {
        PipelineTemplateConfig template = com.thoughtworks.go.helper.PipelineTemplateConfigMother.createTemplate("template-name", new Authorization(new AdminsConfig()), com.thoughtworks.go.helper.StageConfigMother.manualStage("stage-name"));
        cruiseConfig.addTemplate(template);

        xmlWriter.write(cruiseConfig, output, false);

        String writtenConfigXml = this.output.toString();
        assertThat(writtenConfigXml).doesNotContain("<authorization>");
    }

    @Test
    @Timeout(2)
    public void shouldValidateLeadingAndTrailingSpacesOnExecCommandInReasonableTime() {
        // See https://github.com/gocd/gocd/issues/3551
        // This is only reproducible on longish strings, so don't try shortening the exec task length...
        String longPath = "f".repeat(100);
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1");
        config.initializeServer();
        config.findJob("pipeline1", "stage", "job").addTask(new ExecTask(longPath + " ", "arg1", (String) null));

        output = new ByteArrayOutputStream();
        assertThatThrownBy(() -> xmlWriter.write(config, output, false))
            .isInstanceOf(XsdValidationException.class)
            .hasMessageContaining("should conform to the pattern - \\S(.*\\S)?");
    }

    @Test
    public void shouldDisplayTheFlagInXmlIfTemplateAuthorizationDoesNotAllowGroupAdmins() throws Exception {
        PipelineTemplateConfig template = com.thoughtworks.go.helper.PipelineTemplateConfigMother.createTemplate("template-name", new Authorization(new AdminsConfig()), com.thoughtworks.go.helper.StageConfigMother.manualStage("stage-name"));
        template.getAuthorization().setAllowGroupAdmins(false);
        cruiseConfig.addTemplate(template);

        xmlWriter.write(cruiseConfig, output, false);

        String writtenConfigXml = this.output.toString();
        assertThat(writtenConfigXml).contains("allGroupAdminsAreViewers");
    }

    @Test
    public void shouldNotDisplayTheOptionIfTemplateAllowsGroupAdminsToBeViewers() throws Exception {
        PipelineTemplateConfig template = com.thoughtworks.go.helper.PipelineTemplateConfigMother.createTemplate("template-name", new Authorization(new AdminsConfig()), com.thoughtworks.go.helper.StageConfigMother.manualStage("stage-name"));
        cruiseConfig.addTemplate(template);

        xmlWriter.write(cruiseConfig, output, false);

        String writtenConfigXml = this.output.toString();
        assertThat(writtenConfigXml).doesNotContain("allGroupAdminsAreViewers");
    }

    @Test
    public void shouldWriteArtifactsConfigXMLWithType() throws Exception {
        cruiseConfig.getArtifactStores().add(new ArtifactStore("s3", "cd.go.artifact.docker"));
        final PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfigWithStage("Test", "test-stage");
        final JobConfig jobConfig = pipelineConfig.getStage("test-stage").jobConfigByConfigName("dev");
        jobConfig.artifactTypeConfigs().add(new BuildArtifactConfig("build/libs/*.jar", "dist"));
        jobConfig.artifactTypeConfigs().add(new TestArtifactConfig("test-result/*", "reports"));
        jobConfig.artifactTypeConfigs().add(new PluggableArtifactConfig("installers", "s3"));
        cruiseConfig.addPipeline("TestGroup", pipelineConfig);

        xmlWriter.write(cruiseConfig, output, false);

        String actualXML = this.output.toString();

        assertThat(actualXML).contains("<artifact type=\"build\" src=\"build/libs/*.jar\" dest=\"dist\" />");
        assertThat(actualXML).contains("<artifact type=\"test\" src=\"test-result/*\" dest=\"reports\" />");
        assertThat(actualXML).contains("<artifact type=\"external\" id=\"installers\" storeId=\"s3\" />");
    }

    private ConfigurationProperty getConfigurationProperty(String key, boolean isSecure, String value) {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey(key), new ConfigurationValue(value), null, new GoCipher());
        property.handleSecureValueConfiguration(isSecure);
        return property;
    }

    private void setDependencyOn(CruiseConfig cruiseConfig, String toPipeline, String upstreamPipeline, String upstreamStage) {
        PipelineConfig targetPipeline = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString(toPipeline));
        targetPipeline.materialConfigs().clear();
        targetPipeline.addMaterialConfig(new DependencyMaterialConfig(new CaseInsensitiveString(upstreamPipeline), new CaseInsensitiveString(upstreamStage)));
    }

    private PackageRepository createPackageRepository(String pluginId, String version, String id, String name, Configuration configuration, Packages packages) {
        PackageRepository packageRepository = new PackageRepository();
        packageRepository.setConfiguration(configuration);
        packageRepository.setPackages(packages);
        packageRepository.setPluginConfiguration(new PluginConfiguration(pluginId, version));
        packageRepository.setId(id);
        packageRepository.setName(name);
        for (PackageDefinition aPackage : packages) {
            aPackage.setRepository(packageRepository);
        }
        return packageRepository;
    }

}
