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

package com.thoughtworks.go.config.crud;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException;
import com.thoughtworks.go.config.materials.*;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.domain.config.*;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import com.thoughtworks.go.domain.scm.SCM;
import com.thoughtworks.go.domain.scm.SCMMother;
import com.thoughtworks.go.domain.scm.SCMs;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.util.*;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class SCMConfigXmlWriterTest extends BaseConfigXmlWriterTest {
    @Test
    public void shouldWriteSCMConfiguration() throws Exception {
        CruiseConfig configToSave = new BasicCruiseConfig();

        SCM scm = new SCM();
        scm.setId("id");
        scm.setName("name");
        scm.setPluginConfiguration(new PluginConfiguration("plugin-id", "1.0"));
        scm.setConfiguration(new Configuration(getConfigurationProperty("url", false, "http://go"), getConfigurationProperty("secure", true, "secure")));

        configToSave.getSCMs().add(scm);

        xmlWriter.write(configToSave, output, false);

        GoConfigHolder goConfigHolder = xmlLoader.loadConfigHolder(output.toString());

        SCMs scms = goConfigHolder.config.getSCMs();
        assertThat(scms, is(configToSave.getSCMs()));
        assertThat(scms.get(0).getConfiguration().first().getConfigurationValue().getValue(), is("http://go"));
        assertThat(scms.get(0).getConfiguration().first().getEncryptedConfigurationValue(), is(nullValue()));
        assertThat(scms.get(0).getConfiguration().last().getEncryptedValue(), is(new GoCipher().encrypt("secure")));
        assertThat(scms.get(0).getConfiguration().last().getConfigurationValue(), is(nullValue()));
    }

    @Test
    public void shouldWriteSCMConfigurationWhenNoSCMIdIsProvided() throws Exception {
        CruiseConfig configToSave = new BasicCruiseConfig();

        SCM scm = new SCM();
        scm.setName("name");
        scm.setPluginConfiguration(new PluginConfiguration("plugin-id", "1.0"));
        scm.setConfiguration(new Configuration(getConfigurationProperty("url", false, "http://go"), getConfigurationProperty("secure", true, "secure")));

        configToSave.getSCMs().add(scm);

        xmlWriter.write(configToSave, output, false);

        GoConfigHolder goConfigHolder = xmlLoader.loadConfigHolder(output.toString());

        SCMs scms = goConfigHolder.config.getSCMs();
        assertThat(scms.size(), is(configToSave.getSCMs().size()));
        assertThat(scms.get(0).getId(), is(notNullValue()));
    }

    @Test
    public void shouldNotAllowMultipleSCMsWithSameId() throws Exception {
        Configuration configuration = new Configuration(getConfigurationProperty("url", false, "http://go"));
        CruiseConfig configToSave = new BasicCruiseConfig();

        SCM scm1 = createSCM("id", "name1", "plugin-id-1", "1.0", configuration);

        SCM scm2 = createSCM("id", "name2", "plugin-id-2", "1.0", configuration);

        configToSave.setSCMs(new SCMs(scm1, scm2));
        try {
            xmlWriter.write(configToSave, output, false);
            fail("should not have allowed two SCMs with same id");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), is("Duplicate unique value [id] declared for identity constraint \"uniqueSCMId\" of element \"cruise\"."));
        }
    }

    @Test
    public void shouldNotAllowSCMWithInvalidId() throws Exception {
        Configuration configuration = new Configuration(getConfigurationProperty("url", false, "http://go"));
        CruiseConfig configToSave = new BasicCruiseConfig();

        SCM scm = createSCM("id wth space", "name", "plugin-id", "1.0", configuration);

        configToSave.setSCMs(new SCMs(scm));
        try {
            xmlWriter.write(configToSave, output, false);
            fail("should not have allowed two SCMs with same id");
        } catch (XsdValidationException e) {
            assertThat(e.getMessage(), is("Scm id is invalid. \"id wth space\" should conform to the pattern - [a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*"));
        }
    }

    @Test
    public void shouldNotAllowMultipleSCMsWithSameName() throws Exception {
        Configuration scmConfiguration = new Configuration(getConfigurationProperty("url", false, "http://go"));
        CruiseConfig configToSave = new BasicCruiseConfig();

        SCM scm1 = createSCM("id1", "scm-name-1", "plugin-id", "1.0", scmConfiguration);

        SCM scm2 = createSCM("id2", "scm-name-2", "plugin-id", "1.0", scmConfiguration);

        configToSave.setSCMs(new SCMs(scm1, scm2));
        try {
            xmlWriter.write(configToSave, output, false);
            fail("should not have allowed two SCMs with same id");
        } catch (GoConfigInvalidException e) {
            assertThat(e.getMessage(), is("Cannot save SCM, found duplicate SCMs. scm-name-1, scm-name-2"));
        }
    }

    @Test
    public void shouldNotAllowSCMWithInvalidName() throws Exception {
        Configuration configuration = new Configuration(getConfigurationProperty("url", false, "http://go"));
        CruiseConfig configToSave = new BasicCruiseConfig();

        SCM scm = createSCM("id", "name with space", "plugin-id", "1.0", configuration);

        configToSave.setSCMs(new SCMs(scm));
        try {
            xmlWriter.write(configToSave, output, false);
            fail("should not have allowed two SCMs with same id");
        } catch (GoConfigInvalidException e) {
            assertThat(e.getMessage(),
                    is("Invalid SCM name 'name with space'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
        }
    }

    @Test
    public void shouldAllowSCMTypeMaterialForPipeline() throws Exception {
        CruiseConfig configToSave = new BasicCruiseConfig();

        SCM scm = new SCM();
        String scmId = "scm-id";
        scm.setId(scmId);
        scm.setName("name");
        scm.setPluginConfiguration(new PluginConfiguration("plugin-id", "1.0"));
        scm.setConfiguration(new Configuration(getConfigurationProperty("url", false, "http://go")));

        configToSave.getSCMs().add(scm);

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(scmId);
        pluggableSCMMaterialConfig.setSCMConfig(scm);

        configToSave.addPipeline("default", PipelineConfigMother.pipelineConfig("test", new MaterialConfigs(pluggableSCMMaterialConfig), new JobConfigs(new JobConfig("ls"))));

        xmlWriter.write(configToSave, output, false);

        GoConfigHolder goConfigHolder = xmlLoader.loadConfigHolder(output.toString());
        PipelineConfig pipelineConfig = goConfigHolder.config.pipelineConfigByName(new CaseInsensitiveString("test"));
        MaterialConfig materialConfig = pipelineConfig.materialConfigs().get(0);
        assertThat(materialConfig instanceof PluggableSCMMaterialConfig, is(true));
        assertThat(((PluggableSCMMaterialConfig) materialConfig).getScmId(), is(scmId));
        assertThat(((PluggableSCMMaterialConfig) materialConfig).getSCMConfig(), is(scm));
        assertThat(materialConfig.getFolder(), is(nullValue()));
        assertThat(materialConfig.filter(), is(new Filter()));
    }

    @Test
    public void shouldAllowFolderAndFilterForPluggableSCMMaterialForPipeline() throws Exception {
        CruiseConfig configToSave = new BasicCruiseConfig();

        SCM scm = new SCM();
        String scmId = "scm-id";
        scm.setId(scmId);
        scm.setName("name");
        scm.setPluginConfiguration(new PluginConfiguration("plugin-id", "1.0"));
        scm.setConfiguration(new Configuration(getConfigurationProperty("url", false, "http://go")));

        configToSave.getSCMs().add(scm);

        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(scmId);
        pluggableSCMMaterialConfig.setSCMConfig(scm);
        pluggableSCMMaterialConfig.setFolder("dest");
        pluggableSCMMaterialConfig.setFilter(new Filter(new IgnoredFiles("x"), new IgnoredFiles("y")));

        configToSave.addPipeline("default", PipelineConfigMother.pipelineConfig("test", new MaterialConfigs(pluggableSCMMaterialConfig), new JobConfigs(new JobConfig("ls"))));

        xmlWriter.write(configToSave, output, false);

        GoConfigHolder goConfigHolder = xmlLoader.loadConfigHolder(output.toString());
        PipelineConfig pipelineConfig = goConfigHolder.config.pipelineConfigByName(new CaseInsensitiveString("test"));
        MaterialConfig materialConfig = pipelineConfig.materialConfigs().get(0);
        assertThat(materialConfig instanceof PluggableSCMMaterialConfig, is(true));
        assertThat(((PluggableSCMMaterialConfig) materialConfig).getScmId(), is(scmId));
        assertThat(((PluggableSCMMaterialConfig) materialConfig).getSCMConfig(), is(scm));
        assertThat(materialConfig.getFolder(), is("dest"));
        assertThat(materialConfig.filter(), is(new Filter(new IgnoredFiles("x"), new IgnoredFiles("y"))));
    }

    @Test
    public void shouldFailValidationIfSCMTypeMaterialForPipelineHasARefToNonExistentSCM() throws Exception {
        CruiseConfig configToSave = new BasicCruiseConfig();
        String scmId = "does-not-exist";
        PluggableSCMMaterialConfig pluggableSCMMaterialConfig = new PluggableSCMMaterialConfig(scmId);
        SCM scm = SCMMother.create("scm-id", "scm-name", "pluginid", "1.0", new Configuration(ConfigurationPropertyMother.create("k1", false, "v1")));

        configToSave.getSCMs().add(scm);

        configToSave.addPipeline("default", PipelineConfigMother.pipelineConfig("test", new MaterialConfigs(pluggableSCMMaterialConfig), new JobConfigs(new JobConfig("ls"))));
        try {
            xmlWriter.write(configToSave, output, false);
            fail("should not allow this");
        } catch (XsdValidationException exception) {
            assertThat(exception.getMessage(), is("Key 'scmIdReferredByMaterial' with value 'does-not-exist' not found for identity constraint of element 'cruise'."));
        }
    }

    @Test
    public void shouldNotWriteToFileWithDefaultValueOfTrueForSCMAutoUpdateWhenTrue() throws Exception {
        Configuration configuration = new Configuration(getConfigurationProperty("url", false, "http://go"));
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        SCM scm = createSCM("id", "name", "plugin-id", "1.0", configuration);
        scm.setAutoUpdate(true);
        cruiseConfig.setSCMs(new SCMs(scm));

        xmlWriter.write(cruiseConfig, output, false);

        assertThat(output.toString().contains("autoUpdate=\"true\""), is(false));
    }

    @Test
    public void shouldWriteToFileWithValueOfFalseForSCMAutoUpdateWhenFalse() throws Exception {
        Configuration configuration = new Configuration(getConfigurationProperty("url", false, "http://go"));
        CruiseConfig cruiseConfig = new BasicCruiseConfig();
        SCM scm = createSCM("id", "name", "plugin-id", "1.0", configuration);
        scm.setAutoUpdate(false);
        cruiseConfig.setSCMs(new SCMs(scm));

        xmlWriter.write(cruiseConfig, output, false);

        assertThat(output.toString().contains("autoUpdate=\"false\""), is(true));
    }

    private ConfigurationProperty getConfigurationProperty(String key, boolean isSecure, String value) {
        ConfigurationProperty property = new ConfigurationProperty(new ConfigurationKey(key), new ConfigurationValue(value), null, new GoCipher());
        property.handleSecureValueConfiguration(isSecure);
        return property;
    }

    private SCM createSCM(String id, String name, String pluginId, String version, Configuration configuration) {
        SCM scm = new SCM();
        scm.setId(id);
        scm.setName(name);
        scm.setPluginConfiguration(new PluginConfiguration(pluginId, version));
        scm.setConfiguration(configuration);
        return scm;
    }
}
