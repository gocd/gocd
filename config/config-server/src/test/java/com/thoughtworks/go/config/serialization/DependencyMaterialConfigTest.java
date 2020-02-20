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
package com.thoughtworks.go.config.serialization;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterialConfig;
import com.thoughtworks.go.config.materials.perforce.P4MaterialConfig;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.materials.dependency.NewGoConfigMother;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.helper.MaterialConfigsMother.p4;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

class DependencyMaterialConfigTest {

    private MagicalGoConfigXmlWriter writer;
    private MagicalGoConfigXmlLoader loader;
    private CruiseConfig config;
    private PipelineConfig pipelineConfig;

    @BeforeEach
    void setUp() throws Exception {
        writer = new MagicalGoConfigXmlWriter(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins());
        loader = new MagicalGoConfigXmlLoader(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins());
        config = GoConfigMother.configWithPipelines("pipeline1", "pipeline2", "pipeline3", "go");
        pipelineConfig = config.getAllPipelineConfigs().get(0);
    }

    @Test
    void shouldBeAbleToLoadADependencyMaterialFromConfig() throws Exception {
        String xml = "<pipeline pipelineName=\"pipeline-name\" stageName=\"stage-name\" />";
        DependencyMaterialConfig material = loader.fromXmlPartial(xml, DependencyMaterialConfig.class);
        assertThat(material.getPipelineName()).isEqualTo(new CaseInsensitiveString("pipeline-name"));
        assertThat(material.getStageName()).isEqualTo(new CaseInsensitiveString("stage-name"));
        assertThat(writer.toXmlPartial(material)).isEqualTo(xml);
    }

    @Test
    void shouldBeAbleToSaveADependencyMaterialToConfig() throws Exception {
        DependencyMaterialConfig originalMaterial = new DependencyMaterialConfig(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name"));

        NewGoConfigMother mother = new NewGoConfigMother();
        mother.addPipeline("pipeline-name", "stage-name", "job-name");
        mother.addPipeline("dependent", "stage-name", "job-name").addMaterialConfig(originalMaterial);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        CruiseConfig configForEdit = mother.cruiseConfig();
        configForEdit.initializeServer();
        writer.write(configForEdit, buffer, false);

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer.toByteArray());
        CruiseConfig config = loader.loadConfigHolder(IOUtils.toString(inputStream, UTF_8)).config;

        DependencyMaterialConfig material = (DependencyMaterialConfig) config.pipelineConfigByName(new CaseInsensitiveString("dependent")).materialConfigs().get(1);
        assertThat(material).isEqualTo(originalMaterial);
        assertThat(material.getPipelineName()).isEqualTo(new CaseInsensitiveString("pipeline-name"));
        assertThat(material.getStageName()).isEqualTo(new CaseInsensitiveString("stage-name"));
    }

    @Test
    void shouldBeAbleToHaveADependencyAndOneOtherMaterial() throws Exception {
        NewGoConfigMother mother = new NewGoConfigMother();
        mother.addPipeline("pipeline-name", "stage-name", "job-name");
        PipelineConfig pipelineConfig = mother.addPipeline("dependent", "stage-name", "job-name",
                new DependencyMaterialConfig(new CaseInsensitiveString("pipeline-name"), new CaseInsensitiveString("stage-name")));
        pipelineConfig.addMaterialConfig(p4("localhost:1666", "foo"));

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        CruiseConfig cruiseConfig = mother.cruiseConfig();
        cruiseConfig.initializeServer();

        writer.write(cruiseConfig, buffer, false);

        final ByteArrayInputStream inputStream = new ByteArrayInputStream(buffer.toByteArray());
        CruiseConfig config = loader.loadConfigHolder(IOUtils.toString(inputStream, UTF_8)).config;

        MaterialConfigs materialConfigs = config.pipelineConfigByName(new CaseInsensitiveString("dependent")).materialConfigs();
        assertThat(materialConfigs.get(0)).isInstanceOf(DependencyMaterialConfig.class);
        assertThat(materialConfigs.get(1)).isInstanceOf(P4MaterialConfig.class);
    }

    @Test
    void shouldAddErrorForInvalidMaterialName() {
        DependencyMaterialConfig materialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("wrong name"), new CaseInsensitiveString("pipeline-foo"), new CaseInsensitiveString("stage-bar"));
        materialConfig.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig(), pipelineConfig));
        assertThat(materialConfig.errors().on(AbstractMaterialConfig.MATERIAL_NAME)).isEqualTo("Invalid material name 'wrong name'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
    }

    @Test
    void shouldAddErrorWhenInvalidPipelineNameStage() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig();
        Map<String, String> configMap = new HashMap<>();
        configMap.put(DependencyMaterialConfig.PIPELINE_STAGE_NAME, "invalid pipeline stage");

        dependencyMaterialConfig.setConfigAttributes(configMap);

        assertThat(dependencyMaterialConfig.getPipelineStageName()).isEqualTo("invalid pipeline stage");
        assertThat(dependencyMaterialConfig.errors().isEmpty()).isFalse();
        assertThat(dependencyMaterialConfig.errors().on(DependencyMaterialConfig.PIPELINE_STAGE_NAME)).isEqualTo("'invalid pipeline stage' should conform to the pattern 'pipeline [stage]'");
    }

    @Test
    void shouldNotBombValidationWhenMaterialNameIsNotSet() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("pipeline-foo"), new CaseInsensitiveString("stage-bar"));
        dependencyMaterialConfig.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig(), pipelineConfig));
        assertThat(dependencyMaterialConfig.errors().on(AbstractMaterialConfig.MATERIAL_NAME)).isNull();
    }

    @Test
    void shouldNOTBeValidIfThePipelineExistsButTheStageDoesNot() throws Exception {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("pipeline2"), new CaseInsensitiveString("stage-not-existing does not exist!"));
        dependencyMaterialConfig.validate(ConfigSaveValidationContext.forChain(config, pipelineConfig));
        ConfigErrors configErrors = dependencyMaterialConfig.errors();
        assertThat(configErrors.isEmpty()).isFalse();
        assertThat(configErrors.on(DependencyMaterialConfig.PIPELINE_STAGE_NAME)).contains("Stage with name 'stage-not-existing does not exist!' does not exist on pipeline 'pipeline2'");
    }

    @Test
    void shouldNOTBeValidIfTheReferencedPipelineDoesNotExist() throws Exception {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1", "pipeline2", "pipeline3", "go");

        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("pipeline-not-exist"), new CaseInsensitiveString("stage"));
        dependencyMaterialConfig.validate(ConfigSaveValidationContext.forChain(config, pipelineConfig));
        ConfigErrors configErrors = dependencyMaterialConfig.errors();
        assertThat(configErrors.isEmpty()).isFalse();
        assertThat(configErrors.on(DependencyMaterialConfig.PIPELINE_STAGE_NAME)).contains("Pipeline with name 'pipeline-not-exist' does not exist");
    }

    @Test
    void setConfigAttributes_shouldPopulateFromConfigAttributes() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString(""), new CaseInsensitiveString(""));
        assertThat(dependencyMaterialConfig.getPipelineStageName()).isNull();
        assertThat(dependencyMaterialConfig.ignoreForScheduling()).isFalse();
        HashMap<String, String> configMap = new HashMap<>();
        configMap.put(AbstractMaterialConfig.MATERIAL_NAME, "name1");
        configMap.put(DependencyMaterialConfig.PIPELINE_STAGE_NAME, "pipeline-1 [stage-1]");
        configMap.put(DependencyMaterialConfig.IGNORE_FOR_SCHEDULING, "true");

        dependencyMaterialConfig.setConfigAttributes(configMap);

        assertThat(dependencyMaterialConfig.getMaterialName()).isEqualTo(new CaseInsensitiveString("name1"));
        assertThat(dependencyMaterialConfig.getPipelineName()).isEqualTo(new CaseInsensitiveString("pipeline-1"));
        assertThat(dependencyMaterialConfig.getStageName()).isEqualTo(new CaseInsensitiveString("stage-1"));
        assertThat(dependencyMaterialConfig.getPipelineStageName()).isEqualTo("pipeline-1 [stage-1]");
        assertThat(dependencyMaterialConfig.ignoreForScheduling()).isTrue();
    }

    @Test
    void setConfigAttributes_shouldNotPopulateNameFromConfigAttributesIfNameIsEmptyOrNull() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("name2"), new CaseInsensitiveString("pipeline"), new CaseInsensitiveString("stage"));
        HashMap<String, String> configMap = new HashMap<>();
        configMap.put(AbstractMaterialConfig.MATERIAL_NAME, "");

        dependencyMaterialConfig.setConfigAttributes(configMap);

        assertThat(dependencyMaterialConfig.getMaterialName()).isNull();
    }

    @Test
    void shouldValidateTree() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("upstream_stage"), new CaseInsensitiveString("upstream_pipeline"), new CaseInsensitiveString("stage"));
        PipelineConfig pipeline = new PipelineConfig(new CaseInsensitiveString("p"), new MaterialConfigs());
        pipeline.setOrigin(new FileConfigOrigin());
        dependencyMaterialConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", config, pipeline));
        assertThat(dependencyMaterialConfig.errors().on(DependencyMaterialConfig.PIPELINE_STAGE_NAME)).isEqualTo("Pipeline with name 'upstream_pipeline' does not exist, it is defined as a dependency for pipeline 'p' (cruise-config.xml)");
    }

    @Test
    void shouldSetLongDescriptionAsCombinationOfPipelineAndStageName() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(new CaseInsensitiveString("upstream_stage"), new CaseInsensitiveString("upstream_pipeline"), new CaseInsensitiveString("stage"));

        assertThat(dependencyMaterialConfig.getLongDescription()).isEqualTo("upstream_pipeline [ stage ]");
    }
}
