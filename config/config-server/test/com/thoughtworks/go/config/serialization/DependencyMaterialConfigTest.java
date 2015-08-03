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

package com.thoughtworks.go.config.serialization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.MagicalGoConfigXmlLoader;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.materials.dependency.NewGoConfigMother;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.NoOpMetricsProbeService;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import com.thoughtworks.go.util.FileUtil;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class DependencyMaterialConfigTest {

    private MetricsProbeService metricsProbeService;
    private MagicalGoConfigXmlWriter writer;
    private MagicalGoConfigXmlLoader loader;

    @Before
    public void setUp() throws Exception {
        metricsProbeService = new NoOpMetricsProbeService();
        writer = new MagicalGoConfigXmlWriter(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);
        loader = new MagicalGoConfigXmlLoader(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins(), metricsProbeService);
    }

    @Test
    public void shouldBeAbleToLoadADependencyMaterialFromConfig() throws Exception {
        String xml = "<pipeline pipelineName=\"pipeline-name\" stageName=\"stage-name\" />";
        DependencyMaterialConfig material = loader.fromXmlPartial(xml, DependencyMaterialConfig.class);
        assertThat(material.getPipelineName(), is(new CaseInsensitiveString("pipeline-name")));
        assertThat(material.getStageName(), is(new CaseInsensitiveString("stage-name")));
        assertThat(writer.toXmlPartial(material), is(xml));
    }

    @Test
    public void shouldBeAbleToSaveADependencyMaterialToConfig() throws Exception {
        DependencyMaterialConfig originalMaterial = new DependencyMaterialConfig(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));

        NewGoConfigMother mother = new NewGoConfigMother();
        mother.addPipeline("pipeline-name", "stage-name", "job-name");
        mother.addPipeline("dependent", "stage-name", "job-name").addMaterialConfig(originalMaterial);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        writer.write(mother.cruiseConfig(), buffer, false);

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer.toByteArray());
        CruiseConfig config = loader.loadConfigHolder(FileUtil.readToEnd(inputStream)).config;

        DependencyMaterialConfig material = (DependencyMaterialConfig) config.pipelineConfigByName(new CaseInsensitiveString("dependent")).materialConfigs().get(1);
        assertThat(material, is(originalMaterial));
        assertThat(material.getPipelineName(), is(new CaseInsensitiveString("pipeline-name")));
        assertThat(material.getStageName(), is(new CaseInsensitiveString("stage-name")));
    }

    @Test
    public void shouldBeAbleToHaveADependencyAndOneOtherMaterial() throws Exception {
        NewGoConfigMother mother = new NewGoConfigMother();
        mother.addPipeline("pipeline-name", "stage-name", "job-name");
        PipelineConfig pipelineConfig = mother.addPipeline("dependent", "stage-name", "job-name",
                new DependencyMaterialConfig(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name")));
        pipelineConfig.addMaterialConfig(new P4MaterialConfig("localhost:1666", "foo"));

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        CruiseConfig cruiseConfig = mother.cruiseConfig();

        writer.write(cruiseConfig, buffer, false);

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer.toByteArray());
        CruiseConfig config = loader.loadConfigHolder(FileUtil.readToEnd(inputStream)).config;

        MaterialConfigs materialConfigs = config.pipelineConfigByName(new CaseInsensitiveString("dependent")).materialConfigs();
        assertThat(materialConfigs.get(0), is(instanceOf(DependencyMaterialConfig.class)));
        assertThat(materialConfigs.get(1), is(instanceOf(P4MaterialConfig.class)));
    }

    @Test
    public void shouldAddErrorForInvalidMaterialName() {
        DependencyMaterialConfig materialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("wrong name"), new CaseInsensitiveString("pipeline-foo"), new CaseInsensitiveString("stage-bar"));
        materialConfig.validate(ValidationContext.forChain(new BasicCruiseConfig()));
        assertThat(materialConfig.errors().on(AbstractMaterialConfig.MATERIAL_NAME), is("Invalid material name 'wrong name'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."));
    }

    @Test
    public void shouldAddErrorWhenInvalidPipelineNameStage() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig();
        Map<String, String> configMap = new HashMap<String, String>();
        configMap.put(DependencyMaterialConfig.PIPELINE_STAGE_NAME, "invalid pipeline stage");

        dependencyMaterialConfig.setConfigAttributes(configMap);

        assertThat(dependencyMaterialConfig.getPipelineStageName(), is("invalid pipeline stage"));
        assertThat(dependencyMaterialConfig.errors().isEmpty(), is(false));
        assertThat(dependencyMaterialConfig.errors().on(DependencyMaterialConfig.PIPELINE_STAGE_NAME), is("'invalid pipeline stage' should conform to the pattern 'pipeline [stage]'"));
    }

    @Test
    public void shouldNotBombValidationWhenMaterialNameIsNotSet() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("pipeline-foo"), new CaseInsensitiveString("stage-bar"));
        dependencyMaterialConfig.validate(ValidationContext.forChain(new BasicCruiseConfig()));
        assertThat(dependencyMaterialConfig.errors().on(AbstractMaterialConfig.MATERIAL_NAME), is(nullValue()));
    }

    @Test
    public void shouldNOTBeValidIfThePipelineExistsButTheStageDoesNot() throws Exception {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1", "pipeline2", "pipeline3", "go");
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("pipeline2"), new CaseInsensitiveString("stage-not-existing does not exist!"));
        dependencyMaterialConfig.validate(ValidationContext.forChain(config));
        ConfigErrors configErrors = dependencyMaterialConfig.errors();
        assertThat(configErrors.isEmpty(), is(false));
        assertThat(configErrors.on(DependencyMaterialConfig.PIPELINE_STAGE_NAME), containsString("Stage with name 'stage-not-existing does not exist!' does not exist on pipeline 'pipeline2'"));
    }

    @Test
    public void shouldNOTBeValidIfTheReferencedPipelineDoesNotExist() throws Exception {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1", "pipeline2", "pipeline3", "go");
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("pipeline-not-exist"), new CaseInsensitiveString("stage"));
        dependencyMaterialConfig.validate(ValidationContext.forChain(config));
        ConfigErrors configErrors = dependencyMaterialConfig.errors();
        assertThat(configErrors.isEmpty(), is(false));
        assertThat(configErrors.on(DependencyMaterialConfig.PIPELINE_STAGE_NAME), containsString("Pipeline with name 'pipeline-not-exist' does not exist"));
    }

    @Test
    public void setConfigAttributes_shouldPopulateFromConfigAttributes() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString(""), new CaseInsensitiveString(""));
        assertThat(dependencyMaterialConfig.getPipelineStageName(), is(nullValue()));
        HashMap<String, String> configMap = new HashMap<String, String>();
        configMap.put(AbstractMaterialConfig.MATERIAL_NAME, "name1");
        configMap.put(DependencyMaterialConfig.PIPELINE_STAGE_NAME, "pipeline-1 [stage-1]");

        dependencyMaterialConfig.setConfigAttributes(configMap);

        assertThat(dependencyMaterialConfig.getMaterialName(), is(new CaseInsensitiveString("name1")));
        assertThat(dependencyMaterialConfig.getPipelineName(), is(new CaseInsensitiveString("pipeline-1")));
        assertThat(dependencyMaterialConfig.getStageName(), is(new CaseInsensitiveString("stage-1")));
        assertThat(dependencyMaterialConfig.getPipelineStageName(), is("pipeline-1 [stage-1]"));
    }

    @Test
    public void setConfigAttributes_shouldNotPopulateNameFromConfigAttributesIfNameIsEmptyOrNull() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("name2"), new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"));
        HashMap<String, String> configMap = new HashMap<String, String>();
        configMap.put(AbstractMaterialConfig.MATERIAL_NAME, "");

        dependencyMaterialConfig.setConfigAttributes(configMap);

        assertThat(dependencyMaterialConfig.getMaterialName(), is(nullValue()));
    }

    @Test
    public void shouldGetAttributesAllFields() {
        DependencyMaterialConfig material = new DependencyMaterialConfig(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));

        Map<String, Object> attributesWithSecureFields = material.getAttributes(true);
        assertAttributes(attributesWithSecureFields);

        Map<String, Object> attributesWithoutSecureFields = material.getAttributes(false);
        assertAttributes(attributesWithoutSecureFields);
    }

    private void assertAttributes(Map<String, Object> attributes) {
        assertThat((String) attributes.get("type"), is("pipeline"));
        Map<String, Object> configuration = (Map<String, Object>) attributes.get("pipeline-configuration");
        assertThat((String) configuration.get("pipeline-name"), is("pipeline-name"));
        assertThat((String) configuration.get("stage-name"), is("stage-name"));
    }
}
