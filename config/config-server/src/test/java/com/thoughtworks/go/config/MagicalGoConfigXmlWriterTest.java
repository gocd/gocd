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
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.XsdValidationException;
import com.thoughtworks.go.util.command.UrlArgument;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.jdom2.input.JDOMParseException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.xmlunit.assertj.XmlAssert;

import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static com.thoughtworks.go.helper.MaterialConfigsMother.git;
import static com.thoughtworks.go.helper.MaterialConfigsMother.tfs;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static com.thoughtworks.go.util.GoConstants.CONFIG_SCHEMA_VERSION;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class MagicalGoConfigXmlWriterTest {
    private ByteArrayOutputStream output;
    private MagicalGoConfigXmlWriter xmlWriter;
    public SystemEnvironment systemEnvironment;
    private MagicalGoConfigXmlLoader xmlLoader;
    private CruiseConfig cruiseConfig;

    @Rule
    public final ResetCipher resetCipher = new ResetCipher();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setup() {
        output = new ByteArrayOutputStream();
        ConfigCache configCache = new ConfigCache();
        xmlWriter = new MagicalGoConfigXmlWriter(configCache, ConfigElementImplementationRegistryMother.withNoPlugins());
        xmlLoader = new MagicalGoConfigXmlLoader(configCache, ConfigElementImplementationRegistryMother.withNoPlugins());
        cruiseConfig = new BasicCruiseConfig();
        cruiseConfig.initializeServer();
    }

    @Test
    public void shouldBeAbleToExplicitlyLockAPipeline() throws Exception {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1");
        config.setServerConfig(new ServerConfig("foo", new SecurityConfig()));
        config.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).lockExplicitly();
        xmlWriter.write(config, output, false);
        assertThat(output.toString(), containsString("lockBehavior=\"" + PipelineConfig.LOCK_VALUE_LOCK_ON_FAILURE));
    }

    @Test
    public void shouldBeAbleToExplicitlyUnlockAPipeline() throws Exception {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1");
        config.setServerConfig(new ServerConfig("foo", new SecurityConfig()));
        config.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).unlockExplicitly();
        xmlWriter.write(config, output, false);
        assertThat(output.toString(), containsString("lockBehavior=\"" + PipelineConfig.LOCK_VALUE_NONE));
    }

    @Test
    public void shouldWriteServerConfig() throws Exception {
        String xml = ConfigFileFixture.SERVER_WITH_ARTIFACTS_DIR_AND_PURGE_SETTINGS;
        CruiseConfig cruiseConfig = xmlLoader.loadConfigHolder(xml).config;
        xmlWriter.write(cruiseConfig, output, false);
        XmlAssert.assertThat(output.toString()).and(xml).normalizeWhitespace().areIdentical();
    }

    @Test
    public void shouldThrowInvalidConfigWhenAttemptedToSaveMergedConfig() throws Exception {
        String xml = ConfigFileFixture.TWO_PIPELINES;

        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(xml).config;
        PartialConfig remotePart = PartialConfigMother.withPipeline("some-pipe");
        remotePart.setOrigin(new RepoConfigOrigin());
        BasicCruiseConfig merged = new BasicCruiseConfig((BasicCruiseConfig) cruiseConfig, remotePart);
        try {
            xmlWriter.write(merged, output, true);
        } catch (GoConfigInvalidException ex) {
            // ok
            assertThat(ex.getMessage(), is("Attempted to save merged configuration with patials"));
            return;
        }
        fail("should have thrown when saving merged configuration");
    }

    @Test
    public void shouldWritePipelines() throws Exception {
        String xml = ConfigFileFixture.TWO_PIPELINES;

        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(xml).config;
        xmlWriter.write(cruiseConfig, output, false);
        XmlAssert.assertThat(output.toString()).and(xml).normalizeWhitespace().areIdentical();
    }

    @Test
    public void shouldNotWriteDuplicatedPipelines() throws Exception {
        String xml = ConfigFileFixture.TWO_PIPELINES;

        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(xml).config;
        cruiseConfig.addPipeline("someGroup", PipelineConfigMother.pipelineConfig("pipeline1"));
        try {
            xmlWriter.write(cruiseConfig, output, false);
            fail("Should not be able to save config when there are 2 pipelines with same name");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("You have defined multiple pipelines named 'pipeline1'. Pipeline names must be unique. Source(s): [cruise-config.xml]"));
        }
    }

    @Test
    public void shouldNotDeleteServerTag() throws Exception {
        CruiseConfig config = ConfigMigrator.load(ConfigFileFixture.SERVER_TAG_WITH_DEFAULTS_PLUS_LICENSE_TAG);

        xmlWriter.write(config, output, false);
        assertThat(output.toString(), containsString("<server"));
    }

    @Test
    public void shouldWriteConfigRepos() throws Exception {
        CruiseConfig config = GoConfigMother.configWithConfigRepo();
        config.initializeServer();
        xmlWriter.write(config, output, false);

        assertThat(output.toString(), containsString("<config-repo id=\"id2\" pluginId=\"myplugin\">"));
        assertThat(output.toString(), containsString("<git url=\"https://github.com/tomzo/gocd-indep-config-part.git\" />"));
    }

    @Test
    public void shouldNotWriteDependenciesIfEmptyDependencies() throws Exception {
        String xml = ConfigFileFixture.EMPTY_DEPENDENCIES;

        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(xml).config;
        xmlWriter.write(cruiseConfig, output, false);
        assertThat(output.toString().replaceAll(">\\s+<", ""), not(containsString("dependencies")));
    }

    @Test
    public void shouldNotWriteWhenEnvironmentNameIsNotSet() throws Exception {
        String xml = ConfigFileFixture.CONFIG_WITH_NANT_AND_EXEC_BUILDER;

        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(xml).config;
        cruiseConfig.addEnvironment(new BasicEnvironmentConfig());
        try {
            xmlWriter.write(cruiseConfig, output, false);
            fail("Should not be able to save config when the environment name is not set");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("\"Name\" is required for Environment"));
        }
    }

    @Test
    public void shouldValidateThatEnvironmentsAreSameEvenNamesAreOfDifferentCase() throws Exception {
        String xml = ConfigFileFixture.WITH_DUPLICATE_ENVIRONMENTS;
        try {

            ConfigMigrator.loadWithMigration(xml);

            fail("Should not be able to save config when 2 environments have the same name with different case");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Environment with name 'FOO' already exists."));
        }
    }

    @Test
    public void shouldWriteConfigWithTemplates() throws Exception {
        String content = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server>"
                + "     <artifacts>" +
                "           <artifactsDir>artifactsDir</artifactsDir> " +
                "       </artifacts>"
                + "</server>"
                + "<pipelines>\n"
                + "<pipeline name='pipeline1' template='abc'>\n"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "</pipeline>\n"
                + "<pipeline name='pipeline2'>\n"
                + "    <materials>\n"
                + "      <pipeline pipelineName='pipeline1' stageName='stage1'/>"
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
        CruiseConfig config = ConfigMigrator.loadWithMigration(content).configForEdit;
        xmlWriter.write(config, output, false);
        assertThat(output.toString().replaceAll("\\s+", " "), containsString(
                "<pipeline name=\"pipeline1\" template=\"abc\">"
                        + " <materials>"
                        + " <svn url=\"svnurl\" />"
                        + " </materials>"
                        + " </pipeline>"));
    }

    @Test
    public void shouldWriteObjectToXmlPartial() throws Exception {
        String xml = ConfigFileFixture.ONE_PIPELINE;
        CruiseConfig cruiseConfig = ConfigMigrator.load(xml);
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        StageConfig stageConfig = pipelineConfig.findBy(new CaseInsensitiveString("stage"));
        JobConfig build = stageConfig.jobConfigByInstanceName("functional", true);

        assertThat(xmlWriter.toXmlPartial(pipelineConfig), is(
                "<pipeline name=\"pipeline1\">\n"
                        + "  <materials>\n"
                        + "    <svn url=\"foobar\" checkexternals=\"true\" />\n"
                        + "  </materials>\n"
                        + "  <stage name=\"stage\">\n"
                        + "    <jobs>\n"
                        + "      <job name=\"functional\">\n"
                        + "        <artifacts>\n"
                        + "          <artifact type=\"build\" src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
                        + "        </artifacts>\n"
                        + "      </job>\n"
                        + "    </jobs>\n"
                        + "  </stage>\n"
                        + "</pipeline>"
        ));

        assertThat(xmlWriter.toXmlPartial(stageConfig), is(
                "<stage name=\"stage\">\n"
                        + "  <jobs>\n"
                        + "    <job name=\"functional\">\n"
                        + "      <artifacts>\n"
                        + "        <artifact type=\"build\" src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
                        + "      </artifacts>\n"
                        + "    </job>\n"
                        + "  </jobs>\n"
                        + "</stage>"
        ));

        assertThat(xmlWriter.toXmlPartial(build), is(
                "<job name=\"functional\">\n"
                        + "  <artifacts>\n"
                        + "    <artifact type=\"build\" src=\"artifact1.xml\" dest=\"cruise-output\" />\n"
                        + "  </artifacts>\n"
                        + "</job>"
        ));

    }

    @Test
    public void shouldWriteEmptyOnCancelTaskWhenDefined() throws Exception {
        String partial = "<job name=\"functional\">\n"
                + "  <tasks>\n"
                + "    <exec command=\"echo\">\n"
                + "      <oncancel />\n"
                + "    </exec>\n"
                + "  </tasks>\n"
                + "</job>";
        JobConfig jobConfig = xmlLoader.fromXmlPartial(partial, JobConfig.class);
        assertThat(xmlWriter.toXmlPartial(jobConfig), is(partial));
    }

    @Test
    public void shouldBeAValidXSD() throws Exception {
        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        factory.newSchema(new StreamSource(getClass().getResourceAsStream("/cruise-config.xsd")));
    }

    @Test
    public void shouldFailWhenWritingObjectToXmlPartialWithNoConfigTag() throws Exception {
        Object badObject = "foo";
        try {
            xmlWriter.toXmlPartial(badObject);
            fail("Should not be able to write a non ConfigTag enabled object");
        } catch (RuntimeException expected) {
            assertThat(expected.getMessage(), is("Object " + badObject + " does not have a ConfigTag"));
        }
    }

    @Test
    public void shouldNotSaveUserNameAndPasswordWhenBothAreEmpty() throws Exception {
        MailHost mailHost = new MailHost("hostname", 24, "", "", null, true, false, "from@te.com", "to@te.com", new GoCipher());
        mailHost.ensureEncrypted();
        String s = xmlWriter.toXmlPartial(mailHost);
        assertThat(s, is(
                "<mailhost hostname=\"hostname\" port=\"24\" "
                        + "from=\"from@te.com\" admin=\"to@te.com\" />"));
    }

    @Test
    public void shouldEncryptPasswordBeforeWriting() throws Exception {
        resetCipher.setupDESCipherFile();
        String content = "<cruise schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server>"
                + "     <artifacts>" +
                "           <artifactsDir>artifactsDir</artifactsDir> " +
                "       </artifacts>"
                + "<mailhost hostname=\"10.18.3.171\" port=\"25\" username=\"cruise2\" password=\"password\" tls=\"false\" from=\"cruise2@cruise.com\" admin=\"ps@somewhere.com\" />"
                + "</server>"
                + "<pipelines>\n"
                + "<pipeline name='pipeline1' template='abc'>\n"
                + "    <materials>\n"
                + "      <svn url ='svnurl' username='foo' password='password'/>"
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
        CruiseConfig config = ConfigMigrator.loadWithMigration(content).configForEdit;
        xmlWriter.write(config, output, false);
        assertThat(output.toString().replaceAll("\\s+", " "), containsString(
                "<svn url=\"svnurl\" username=\"foo\" encryptedPassword=\"" + new GoCipher().encrypt("password") + "\" />"));
        assertThat(output.toString().replaceAll("\\s+", " "), containsString(
                "<mailhost hostname=\"10.18.3.171\" port=\"25\" username=\"cruise2\" encryptedPassword=\"" + new GoCipher().encrypt("password") + "\" from=\"cruise2@cruise.com\" admin=\"ps@somewhere.com\" />"));
    }

    @Test
    public void shouldWriteP4MaterialToXmlPartial() throws Exception {
        String encryptedPassword = new GoCipher().encrypt("password");
        P4MaterialConfig p4MaterialConfig = com.thoughtworks.go.helper.MaterialConfigsMother.p4MaterialConfig();
        p4MaterialConfig.setPassword("password");
        p4MaterialConfig.setConfigAttributes(m(
                P4MaterialConfig.SERVER_AND_PORT, "localhost:1666",
                P4MaterialConfig.USERNAME, "cruise",
                P4MaterialConfig.VIEW, "//depot/dir1/... //lumberjack/...",
                P4MaterialConfig.AUTO_UPDATE, "true"));
        assertThat(xmlWriter.toXmlPartial(p4MaterialConfig), is(
                "<p4 port=\"localhost:1666\" username=\"cruise\" encryptedPassword=\"" + encryptedPassword + "\">\n"
                        + "  <view><![CDATA["
                        + "//depot/dir1/... //lumberjack/..."
                        + "]]></view>\n"
                        + "</p4>"));
    }

    @Test
    public void shouldWriteSvnMaterialToXmlPartial() throws Exception {
        String encryptedPassword = new GoCipher().encrypt("password");
        SvnMaterialConfig material = com.thoughtworks.go.helper.MaterialConfigsMother.svnMaterialConfig("http://user:pass@svn", null, "cruise", "password", false, null);
        assertThat(xmlWriter.toXmlPartial(material), is(
                "<svn url=\"http://user:pass@svn\" username=\"cruise\" encryptedPassword=\"" + encryptedPassword + "\" materialName=\"http___user_pass@svn\" />"));
    }

    @Test
    public void shouldWriteHgMaterialToXmlPartial() throws Exception {
        HgMaterialConfig material = com.thoughtworks.go.helper.MaterialConfigsMother.hgMaterialConfig();
        material.setConfigAttributes(m(HgMaterialConfig.URL, "http://user:pass@hg", HgMaterialConfig.AUTO_UPDATE, "true"));
        assertThat(xmlWriter.toXmlPartial(material), is("<hg url=\"http://user:pass@hg\" />"));
    }

    @Test
    public void shouldWriteGitMaterialToXmlPartial() throws Exception {
        GitMaterialConfig gitMaterial = git("gitUrl");
        assertThat(xmlWriter.toXmlPartial(gitMaterial), is("<git url=\"gitUrl\" />"));
    }

    @Test
    public void shouldWritePipelineGroupAdmins() throws Exception {
        String content = ConfigFileFixture.configWithPipelines("<pipelines group=\"first\">\n"
                + "<authorization>"
                + "     <admins>\n"
                + "         <user>foo</user>\n"
                + "      </admins>"
                + "</authorization>"
                + "<pipeline name='pipeline1'>\n"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n<stage name='stage'><jobs><job name='job'></job></jobs></stage>"
                + "</pipeline>\n"
                + "</pipelines>\n", CONFIG_SCHEMA_VERSION);
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        xmlWriter.write(cruiseConfig, out, false);
        assertThat(out.toString(), containsString("<admins>"));
        assertThat(out.toString(), containsString("<user>foo</user>"));
    }

    @Test
    public void shouldAllowParamsInsidePipeline() throws Exception {
        String content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "     xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server>"
                + "     <artifacts>" +
                "           <artifactsDir>artifactsDir</artifactsDir> " +
                "       </artifacts>"
                + "</server>"
                + "<pipelines>\n"
                + "<pipeline name='framework'>\n"
                + "    <params>\n"
                + "      <param name='first'>foo</param>\n"
                + "      <param name='second'>bar</param>\n"
                + "    </params>"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "  <stage name='dist' fetchMaterials='true'>\n"
                + "    <jobs>\n"
                + "      <job name='package' />\n"
                + "    </jobs>\n"
                + "  </stage>\n"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "</cruise>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("framework"));
        ParamsConfig params = pipelineConfig.getParams();
        assertThat(params.getParamNamed("first"), is(new ParamConfig("first", "foo")));
        assertThat(params.getParamNamed("second"), is(new ParamConfig("second", "bar")));
        assertThat(params.getParamNamed("third"), is(nullValue()));

        params.remove(0);

        xmlWriter.write(cruiseConfig, out, false);
        assertThat(out.toString(), not(containsString("<param name=\"first\">foo</param>")));
        assertThat(out.toString(), containsString("<param name=\"second\">bar</param>"));
    }

    @Test
    public void shouldWriteFetchMaterialsFlagToStage() throws Exception {
        String content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "     xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server>"
                + "     <artifacts>" +
                "           <artifactsDir>artifactsDir</artifactsDir> " +
                "       </artifacts>"
                + "</server>"
                + "<pipelines>\n"
                + "<pipeline name='framework'>\n"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "  <stage name='dist' fetchMaterials='true'>\n"
                + "    <jobs>\n"
                + "      <job name='package' />\n"
                + "    </jobs>\n"
                + "  </stage>\n"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "</cruise>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        StageConfig stageConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("framework")).get(0);

        assertThat(stageConfig.isFetchMaterials(), is(true));
        stageConfig.setFetchMaterials(false);
        xmlWriter.write(cruiseConfig, out, false);
        assertThat(out.toString(), containsString("fetchMaterials=\"false\""));
    }

    @Test
    public void shouldWriteCleanWorkingDirFlagToStage() throws Exception {
        String content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
                + "     xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server>"
                + "     <artifacts>" +
                "           <artifactsDir>artifactsDir</artifactsDir> " +
                "       </artifacts>"
                + "</server>"
                + "<pipelines>\n"
                + "<pipeline name='framework'>\n"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "  <stage name='dist' cleanWorkingDir='false'>\n"
                + "    <jobs>\n"
                + "      <job name='package' />\n"
                + "    </jobs>\n"
                + "  </stage>\n"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "</cruise>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        xmlWriter.write(cruiseConfig, out, false);
        assertThat(out.toString(), not(containsString("cleanWorkingDir")));

        StageConfig stageConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("framework")).get(0);
        stageConfig.setCleanWorkingDir(true);
        xmlWriter.write(cruiseConfig, out, false);
        assertThat(out.toString(), containsString("cleanWorkingDir=\"true\""));
    }

    @Test
    public void shouldWriteArtifactPurgeSettings() throws Exception {
        String content = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<cruise xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"cruise-config.xsd\" schemaVersion='" + CONFIG_SCHEMA_VERSION + "'>\n"
                + "<server>"
                + "     <artifacts>" +
                "           <artifactsDir>other-artifacts</artifactsDir> " +
                "       </artifacts>"
                + "</server>"
                + "<pipelines>\n"
                + "<pipeline name='framework'>\n"
                + "    <materials>\n"
                + "      <svn url =\"svnurl\"/>"
                + "    </materials>\n"
                + "  <stage name='dist'>\n"
                + "    <jobs>\n"
                + "      <job name='package' />\n"
                + "    </jobs>\n"
                + "  </stage>\n"
                + "</pipeline>\n"
                + "</pipelines>\n"
                + "</cruise>";
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        CruiseConfig cruiseConfig = ConfigMigrator.loadWithMigration(content).config;
        StageConfig stageConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("framework")).get(0);
        ReflectionUtil.setField(stageConfig, "artifactCleanupProhibited", false);
        xmlWriter.write(cruiseConfig, out, false);
        assertThat(out.toString(), not(containsString("artifactCleanupProhibited=\"true\"")));
        ReflectionUtil.setField(stageConfig, "artifactCleanupProhibited", true);
        xmlWriter.write(cruiseConfig, out, false);
        assertThat(out.toString(), containsString("artifactCleanupProhibited=\"true\""));

        assertThat(out.toString(), not(containsString("purgeStart=\"10.0\"")));
        assertThat(out.toString(), not(containsString("purgeUpto=\"20.0\"")));

        cruiseConfig.server().setPurgeLimits(10.0, 20.0);

        xmlWriter.write(cruiseConfig, out, false);

        assertThat(out.toString(), containsString("<purgeStartDiskSpace>10.0</purgeStartDiskSpace>"));
        assertThat(out.toString(), containsString("<purgeUptoDiskSpace>20.0</purgeUptoDiskSpace>"));
    }

    @Test
    public void shouldRemoveDuplicatedIgnoreTag() throws Exception {
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.TWO_DUPLICATED_FILTER);

        int size = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).materialConfigs().first().filter().size();
        assertThat(size, is(1));
    }

    @Test
    public void shouldNotAllowEmptyAuthInApproval() throws Exception {
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.ONE_PIPELINE);
        StageConfig stageConfig = com.thoughtworks.go.helper.StageConfigMother.custom("newStage", new AuthConfig());
        cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1")).add(stageConfig);

        try {
            xmlWriter.write(cruiseConfig, output, false);
            assertThat("Should not allow approval with empty auth", output.toString().contains("<auth"), is(false));
        } catch (JDOMParseException expected) {
            assertThat(expected.getMessage(), containsString("The content of element 'auth' is not complete"));
        }
    }

    @Test
    public void shouldNotWriteTrackingToolIfItIsNotDefined() throws Exception {
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.ONE_PIPELINE);
        xmlWriter.write(cruiseConfig, output, false);

        assertThat("should not write empty trackingtool to config when it is not defined",
                output.toString(), not(containsString("<trackingtool")));
    }

    @Test
    public void shouldNotDefineATrackingToolWithoutALink() throws Exception {
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.ONE_PIPELINE);
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        pipelineConfig.setTrackingTool(new TrackingTool("", "regex"));
        try {
            xmlWriter.write(cruiseConfig, output, false);
            fail("should not save a trackingtool without a link");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Link should be populated"));
        }
    }

    @Test
    public void shouldSkipValidationIfExplicitlyToldWhileWritingConfig() throws Exception {
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.ONE_PIPELINE);
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        pipelineConfig.addEnvironmentVariable("name1", "value1");
        pipelineConfig.addEnvironmentVariable("name1", "value1");
        xmlWriter.write(cruiseConfig, output, true);
        assertThat(cruiseConfig.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldNotDefineATrackingToolWithoutARegex() throws Exception {
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.ONE_PIPELINE);
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("pipeline1"));
        pipelineConfig.setTrackingTool(new TrackingTool("link", ""));
        try {
            xmlWriter.write(cruiseConfig, output, false);
            fail("should not save a trackingtool without a regex");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString("Regex should be populated"));
        }
    }

    @Test
    public void shouldWriteConfigWithMultipleGroups() throws Exception {
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.PIPELINE_GROUPS);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        xmlWriter.write(cruiseConfig, buffer, false);

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer.toByteArray());
        CruiseConfig config = xmlLoader.loadConfigHolder(IOUtils.toString(inputStream, UTF_8)).config;
        assertThat(config.getGroups().size(), is(2));
        assertThat(config.getGroups().first().getGroup(), is("studios"));
    }

    @Test
    public void shouldWriteConfigWithTaskExecutionConditions() throws Exception {
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.TASKS_WITH_CONDITION);

        xmlWriter.write(cruiseConfig, output, false);

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());
        CruiseConfig config = xmlLoader.loadConfigHolder(IOUtils.toString(inputStream, UTF_8)).config;
        JobConfig job = config.jobConfigByName("pipeline1", "mingle", "cardlist", true);

        assertThat(job.tasks().size(), is(2));
        assertThat(job.tasks().findFirstByType(AntTask.class).getConditions().get(0), is(new RunIfConfig("failed")));

        RunIfConfigs conditions = job.tasks().findFirstByType(NantTask.class).getConditions();
        assertThat(conditions.get(0), is(new RunIfConfig("failed")));
        assertThat(conditions.get(1), is(new RunIfConfig("any")));
        assertThat(conditions.get(2), is(new RunIfConfig("passed")));
    }

    @Test
    // #3098
    public void shouldAlwaysWriteArtifactsDir() throws Exception {
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.DEFAULT_XML_WITH_2_AGENTS);

        xmlWriter.write(cruiseConfig, output, false);

        assertThat(xmlWriter.toXmlPartial(cruiseConfig.server()), containsString("<artifactsDir>artifactsDir</artifactsDir>"));
    }

    @Test
    public void shouldThrowExceptionWhenPersisInvalidDom() throws Exception {
        //simulate the xml partial saving logic
        CruiseConfig cruiseConfig = ConfigMigrator.load(ConfigFileFixture.CONTAINS_MULTI_DIFFERENT_STATUS_RUN_IF);
        StageConfig stage = xmlLoader.fromXmlPartial(ConfigFileFixture.SAME_STATUS_RUN_IF_PARTIAL, StageConfig.class);
        PipelineConfig pipelineConfig = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("test"));
        pipelineConfig.set(0, stage);

        try {
            xmlWriter.write(cruiseConfig, output, false);
            fail();
        } catch (Exception e) {
            assertThat(e.getMessage(), anyOf(
                    is("Duplicate unique value [passed] declared for identity constraint of element \"exec\"."),
                    is("Duplicate unique value [passed] declared for identity constraint \"uniqueRunIfTypeForExec\" of element \"exec\".")
            ));
        }
    }

    @Test
    public void shouldNotThrowUpWhenTfsWorkspaceIsNotSpecified() {
        CruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("tfs_pipeline");
        cruiseConfig.initializeServer();
        PipelineConfig tfs_pipeline = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("tfs_pipeline"));
        tfs_pipeline.materialConfigs().clear();
        tfs_pipeline.addMaterialConfig(tfs(new GoCipher(), new UrlArgument("http://tfs.com"), "username", "CORPORATE", "password", "$/project_path"));
        try {
            xmlWriter.write(cruiseConfig, output, false);
        } catch (Exception e) {
            fail("should not fail as workspace name is not mandatory anymore " + e);
        }
    }

    @Test
    public void shouldSerialize_CaseInsensitiveString_whenUsedInConfigAttributeValue() {//for instance FetchTask uses PathFromAncestor which has CaseInsensitiveString
        CruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("uppest", "upper", "downer", "downest");
        cruiseConfig.initializeServer();
        setDepedencyOn(cruiseConfig, "upper", "uppest", "stage");
        setDepedencyOn(cruiseConfig, "downer", "upper", "stage");
        setDepedencyOn(cruiseConfig, "downest", "downer", "stage");
        PipelineConfig downest = cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("downest"));
        FetchTask fetchTask = new FetchTask(new CaseInsensitiveString("uppest/upper/downer"), new CaseInsensitiveString("stage"), new CaseInsensitiveString("job"), "src", "dest");
        downest.add(com.thoughtworks.go.helper.StageConfigMother.stageConfig("stage-2", new JobConfigs(new JobConfig(new CaseInsensitiveString("downloader"), new ResourceConfigs(), new ArtifactTypeConfigs(), new Tasks(fetchTask)))));

        try {
            xmlWriter.write(cruiseConfig, output, false);
        } catch (Exception e) {
            fail("should not fail as workspace name is not mandatory anymore " + e);
        }

        assertThat(new String(output.toByteArray()), containsString("<fetchartifact artifactOrigin=\"gocd\" srcfile=\"src\" dest=\"dest\" pipeline=\"uppest/upper/downer\" stage=\"stage\" job=\"job\" />"));
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
        assertThat(packageRepositories, is(cruiseConfig.getPackageRepositories()));
        assertThat(packageRepositories.get(0).getConfiguration().first().getConfigurationValue().getValue(), is("http://go"));
        assertThat(packageRepositories.get(0).getConfiguration().first().getEncryptedConfigurationValue(), is(nullValue()));
        assertThat(packageRepositories.get(0).getConfiguration().last().getEncryptedValue(), is(new GoCipher().encrypt("secure")));
        assertThat(packageRepositories.get(0).getConfiguration().last().getConfigurationValue(), is(nullValue()));
        assertThat(packageRepositories.get(0).getPackages().get(0), is(packageDefinition));
        assertThat(packageRepositories.get(0).getPackages().get(0).getConfiguration().first().getConfigurationValue().getValue(), is("go-agent"));
        assertThat(packageRepositories.get(0).getPackages().get(0).getConfiguration().first().getEncryptedConfigurationValue(), is(nullValue()));
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
        assertThat(packageRepositories.size(), is(cruiseConfig.getPackageRepositories().size()));
        assertThat(packageRepositories.get(0).getId(), is(notNullValue()));
        assertThat(packageRepositories.get(0).getPackages().size(), is(1));
        assertThat(packageRepositories.get(0).getPackages().get(0).getId(), is(notNullValue()));
    }

    @Test
    public void shouldNotAllowMultipleRepositoriesWithSameId() throws Exception {
        Configuration packageConfiguration = new Configuration(getConfigurationProperty("name", false, "go-agent"));
        Configuration repositoryConfiguration = new Configuration(getConfigurationProperty("url", false, "http://go"));

        PackageRepository packageRepository = createPackageRepository("plugin-id-1", "version", "id", "name1", repositoryConfiguration,
                new Packages(new PackageDefinition("id", "name", packageConfiguration)));

        PackageRepository anotherPackageRepository = createPackageRepository("plugin-id-2", "version", "id", "name2", repositoryConfiguration,
                new Packages(new PackageDefinition("id", "name", packageConfiguration)));

        cruiseConfig.setPackageRepositories(new PackageRepositories(packageRepository, anotherPackageRepository));
        try {
            xmlWriter.write(cruiseConfig, output, false);
            fail("should not have allowed two repositories with same id");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), anyOf(
                    is("Duplicate unique value [id] declared for identity constraint of element \"repositories\"."),
                    is("Duplicate unique value [id] declared for identity constraint \"uniqueRepositoryId\" of element \"repositories\".")
            ));
        }
    }

    @Test
    public void shouldNotAllowMultiplePackagesWithSameId() throws Exception {
        Configuration packageConfiguration = new Configuration(getConfigurationProperty("name", false, "go-agent"));
        Configuration repositoryConfiguration = new Configuration(getConfigurationProperty("url", false, "http://go"));

        PackageRepository packageRepository = createPackageRepository("plugin-id-1", "version", "id1", "name1", repositoryConfiguration,
                new Packages(new PackageDefinition("id", "name", packageConfiguration)));

        PackageRepository anotherPackageRepository = createPackageRepository("plugin-id-2", "version", "id2", "name2", repositoryConfiguration,
                new Packages(new PackageDefinition("id", "name", packageConfiguration)));

        cruiseConfig.setPackageRepositories(new PackageRepositories(packageRepository, anotherPackageRepository));
        try {
            xmlWriter.write(cruiseConfig, output, false);
            fail("should not have allowed two package repositories with same id");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), anyOf(
                    is("Duplicate unique value [id] declared for identity constraint of element \"cruise\"."),
                    is("Duplicate unique value [id] declared for identity constraint \"uniquePackageId\" of element \"cruise\".")
            ));
        }
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

        cruiseConfig.addPipeline("default", com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig("test", new MaterialConfigs(packageMaterialConfig), new JobConfigs(new JobConfig("ls"))));
        xmlWriter.write(cruiseConfig, output, false);
        GoConfigHolder goConfigHolder = xmlLoader.loadConfigHolder(output.toString());
        PipelineConfig pipelineConfig = goConfigHolder.config.pipelineConfigByName(new CaseInsensitiveString("test"));
        assertThat(pipelineConfig.materialConfigs().get(0) instanceof PackageMaterialConfig, is(true));
        assertThat(((PackageMaterialConfig) pipelineConfig.materialConfigs().get(0)).getPackageId(), is(packageId));
        PackageDefinition packageDefinition = goConfigHolder.config.getPackageRepositories().first().getPackages().first();
        assertThat(((PackageMaterialConfig) pipelineConfig.materialConfigs().get(0)).getPackageDefinition(), is(packageDefinition));
    }

    @Test
    public void shouldFailValidationIfPackageTypeMaterialForPipelineHasARefToNonExistantPackage() throws Exception {
        String packageId = "does-not-exist";
        PackageMaterialConfig packageMaterialConfig = new PackageMaterialConfig(packageId);
        PackageRepository repository = com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother.create("repo-id", "repo-name", "pluginid", "version", new Configuration(com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create("k1", false, "v1")));
        packageMaterialConfig.setPackageDefinition(
                com.thoughtworks.go.domain.packagerepository.PackageDefinitionMother.create("does-not-exist", "package-name", new Configuration(com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create("k2", false, "v2")), repository));

        cruiseConfig.addPipeline("default", com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig("test", new MaterialConfigs(packageMaterialConfig), new JobConfigs(new JobConfig("ls"))));
        try {
            xmlWriter.write(cruiseConfig, output, false);
            fail("should not allow this");
        } catch (XsdValidationException exception) {
            assertThat(exception.getMessage(), is("Key 'packageIdReferredByMaterial' with value 'does-not-exist' not found for identity constraint of element 'cruise'."));
        }
    }

    @Test
    public void shouldNotAllowMultipleRepositoriesWithSameName() throws Exception {
        Configuration packageConfiguration = new Configuration(getConfigurationProperty("name", false, "go-agent"));
        Configuration repositoryConfiguration = new Configuration(getConfigurationProperty("url", false, "http://go"));

        PackageRepository packageRepository = createPackageRepository("plugin-id", "version", "id1", "name", repositoryConfiguration,
                new Packages(new PackageDefinition("id1", "name1", packageConfiguration)));

        PackageRepository anotherPackageRepository = createPackageRepository("plugin-id", "version", "id2", "name", repositoryConfiguration,
                new Packages(new PackageDefinition("id2", "name2", packageConfiguration)));

        cruiseConfig.setPackageRepositories(new PackageRepositories(packageRepository, anotherPackageRepository));
        try {
            xmlWriter.write(cruiseConfig, output, false);
            fail("should not have allowed two repositories with same id");
        } catch (GoConfigInvalidException e) {
            assertThat(e.getMessage(), is("You have defined multiple repositories called 'name'. Repository names are case-insensitive and must be unique."));
        }
    }

    @Test
    public void shouldNotAllowMultiplePackagesWithSameNameWithinARepo() throws Exception {
        Configuration packageConfiguration1 = new Configuration(getConfigurationProperty("name", false, "go-agent"));
        Configuration packageConfiguration2 = new Configuration(getConfigurationProperty("name2", false, "go-server"));
        Configuration repositoryConfiguration = new Configuration(getConfigurationProperty("url", false, "http://go"));

        PackageRepository packageRepository = createPackageRepository("plugin-id", "version", "id", "name", repositoryConfiguration,
                new Packages(new PackageDefinition("id1", "name", packageConfiguration1), new PackageDefinition("id2", "name", packageConfiguration2)));

        cruiseConfig.setPackageRepositories(new PackageRepositories(packageRepository));
        try {
            xmlWriter.write(cruiseConfig, output, false);
            fail("should not have allowed two repositories with same id");
        } catch (GoConfigInvalidException e) {
            assertThat(e.getMessage(), is("You have defined multiple packages called 'name'. Package names are case-insensitive and must be unique within a repository."));
        }
    }

    @Test
    public void shouldNotAllowPackagesRepositoryWithInvalidId() throws Exception {
        Configuration packageConfiguration = new Configuration(getConfigurationProperty("name", false, "go-agent"));
        Configuration repositoryConfiguration = new Configuration(getConfigurationProperty("url", false, "http://go"));

        PackageRepository packageRepository = createPackageRepository("plugin-id", "version", "id wth space", "name", repositoryConfiguration,
                new Packages(new PackageDefinition("id", "name", packageConfiguration)));

        cruiseConfig.setPackageRepositories(new PackageRepositories(packageRepository));
        try {
            xmlWriter.write(cruiseConfig, output, false);
            fail("should not have allowed two repositories with same id");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), is("Repo id is invalid. \"id wth space\" should conform to the pattern - [a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*"));
        }
    }

    @Test
    public void shouldNotAllowPackagesRepositoryWithInvalidName() throws Exception {
        Configuration packageConfiguration = new Configuration(getConfigurationProperty("name", false, "go-agent"));
        Configuration repositoryConfiguration = new Configuration(getConfigurationProperty("url", false, "http://go"));

        PackageRepository packageRepository = createPackageRepository("plugin-id", "version", "id", "name with space", repositoryConfiguration,
                new Packages(new PackageDefinition("id", "name", packageConfiguration)));

        cruiseConfig.setPackageRepositories(new PackageRepositories(packageRepository));
        try {
            xmlWriter.write(cruiseConfig, output, false);
            fail("should not have allowed two repositories with same id");
        } catch (GoConfigInvalidException e) {
            assertThat(e.getMessage(),
                    is("Invalid PackageRepository name 'name with space'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
        }
    }

    @Test
    public void shouldNotAllowPackagesWithInvalidId() throws Exception {
        Configuration packageConfiguration = new Configuration(getConfigurationProperty("name", false, "go-agent"));
        Configuration repositoryConfiguration = new Configuration(getConfigurationProperty("url", false, "http://go"));

        PackageRepository packageRepository = createPackageRepository("plugin-id", "version", "id", "name", repositoryConfiguration,
                new Packages(new PackageDefinition("id with space", "name", packageConfiguration)));

        cruiseConfig.setPackageRepositories(new PackageRepositories(packageRepository));
        try {
            xmlWriter.write(cruiseConfig, output, false);
            fail("should not have allowed two repositories with same id");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), is("Package id is invalid. \"id with space\" should conform to the pattern - [a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*"));
        }
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

        assertThat(output.toString().contains("autoUpdate=\"true\""), is(false));
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

        assertThat(output.toString().contains("autoUpdate=\"false\""), is(true));
    }

    @Test
    public void shouldNotAllowPackagesWithInvalidName() throws Exception {
        Configuration packageConfiguration = new Configuration(getConfigurationProperty("name", false, "go-agent"));
        Configuration repositoryConfiguration = new Configuration(getConfigurationProperty("url", false, "http://go"));

        PackageRepository packageRepository = createPackageRepository("plugin-id", "version", "id", "name", repositoryConfiguration,
                new Packages(new PackageDefinition("id", "name with space", packageConfiguration)));

        cruiseConfig.setPackageRepositories(new PackageRepositories(packageRepository));
        try {
            xmlWriter.write(cruiseConfig, output, false);
            fail("should not have allowed two repositories with same id");
        } catch (GoConfigInvalidException e) {
            assertThat(e.getMessage(),
                    is("Invalid Package name 'name with space'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
        }
    }

    @Test
    public void shouldNotWriteEmptyAuthorizationUnderEachTemplateTagOntoConfigFile() throws Exception {
        PipelineTemplateConfig template = com.thoughtworks.go.helper.PipelineTemplateConfigMother.createTemplate("template-name", new Authorization(new AdminsConfig()), com.thoughtworks.go.helper.StageConfigMother.manualStage("stage-name"));
        cruiseConfig.addTemplate(template);

        xmlWriter.write(cruiseConfig, output, false);

        String writtenConfigXml = this.output.toString();
        assertThat(writtenConfigXml, not(containsString("<authorization>")));
    }

    @Test(timeout = 1000)
    public void shouldValidateLeadingAndTrailingSpacesOnExecCommandInReasonableTime() throws Exception {
        // See https://github.com/gocd/gocd/issues/3551
        // This is only reproducible on longish strings, so don't try shortening the exec task length...
        String longPath = StringUtils.repeat("f", 100);
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1");
        config.initializeServer();
        config.findJob("pipeline1", "stage", "job").addTask(new ExecTask(longPath + " ", "arg1", (String) null));

        output = new ByteArrayOutputStream();
        try {
            xmlWriter.write(config, output, false);
            fail("expected to blow up");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), containsString("should conform to the pattern - \\S(.*\\S)?"));
        }
    }

    @Test
    public void shouldDisplayTheFlagInXmlIfTemplateAuthorizationDoesNotAllowGroupAdmins() throws Exception {
        PipelineTemplateConfig template = com.thoughtworks.go.helper.PipelineTemplateConfigMother.createTemplate("template-name", new Authorization(new AdminsConfig()), com.thoughtworks.go.helper.StageConfigMother.manualStage("stage-name"));
        template.getAuthorization().setAllowGroupAdmins(false);
        cruiseConfig.addTemplate(template);

        xmlWriter.write(cruiseConfig, output, false);

        String writtenConfigXml = this.output.toString();
        assertThat(writtenConfigXml, containsString("allGroupAdminsAreViewers"));
    }

    @Test
    public void shouldNotDisplayTheOptionIfTemplateAllowsGroupAdminsToBeViewers() throws Exception {
        PipelineTemplateConfig template = com.thoughtworks.go.helper.PipelineTemplateConfigMother.createTemplate("template-name", new Authorization(new AdminsConfig()), com.thoughtworks.go.helper.StageConfigMother.manualStage("stage-name"));
        cruiseConfig.addTemplate(template);

        xmlWriter.write(cruiseConfig, output, false);

        String writtenConfigXml = this.output.toString();
        assertThat(writtenConfigXml, not(containsString("allGroupAdminsAreViewers")));
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

        assertThat(actualXML, containsString("<artifact type=\"build\" src=\"build/libs/*.jar\" dest=\"dist\" />"));
        assertThat(actualXML, containsString("<artifact type=\"test\" src=\"test-result/*\" dest=\"reports\" />"));
        assertThat(actualXML, containsString("<artifact type=\"external\" id=\"installers\" storeId=\"s3\" />"));
    }

    private ConfigurationProperty getConfigurationProperty(String key, boolean isSecure, String value) {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey(key), new ConfigurationValue(value), null, new GoCipher());
        property.handleSecureValueConfiguration(isSecure);
        return property;
    }

    private void setDepedencyOn(CruiseConfig cruiseConfig, String toPipeline, String upstreamPipeline, String upstreamStage) {
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
