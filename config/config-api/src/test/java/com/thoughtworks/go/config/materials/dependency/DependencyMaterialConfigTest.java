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

package com.thoughtworks.go.config.materials.dependency;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.AbstractMaterialConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.helper.GoConfigMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.go.config.CaseInsensitiveString.cis;
import static org.assertj.core.api.Assertions.assertThat;

class DependencyMaterialConfigTest {

    private CruiseConfig config;
    private PipelineConfig pipelineConfig;

    @BeforeEach
    void setUp() {
        config = GoConfigMother.configWithPipelines("pipeline1", "pipeline2", "pipeline3", "go");
        pipelineConfig = config.getAllPipelineConfigs().getFirst();
    }

    @Test
    void shouldAddErrorForInvalidMaterialName() {
        DependencyMaterialConfig materialConfig = new DependencyMaterialConfig(cis("wrong name"), cis("pipeline-foo"), cis("stage-bar"));
        materialConfig.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig(), pipelineConfig));
        assertThat(materialConfig.errors().firstErrorOn(AbstractMaterialConfig.MATERIAL_NAME)).isEqualTo("Invalid material name 'wrong name'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.");
    }

    @Test
    void shouldAddErrorWhenInvalidPipelineNameStage() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig();
        Map<String, String> configMap = new HashMap<>();
        configMap.put(DependencyMaterialConfig.PIPELINE_STAGE_NAME, "invalid pipeline stage");

        dependencyMaterialConfig.setConfigAttributes(configMap);

        assertThat(dependencyMaterialConfig.getPipelineStageName()).isEqualTo("invalid pipeline stage");
        assertThat(dependencyMaterialConfig.errors().isEmpty()).isFalse();
        assertThat(dependencyMaterialConfig.errors().firstErrorOn(DependencyMaterialConfig.PIPELINE_STAGE_NAME)).isEqualTo("'invalid pipeline stage' should conform to the pattern 'pipeline [stage]'");
    }

    @Test
    void shouldNotBombValidationWhenMaterialNameIsNotSet() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(cis("pipeline-foo"), cis("stage-bar"));
        dependencyMaterialConfig.validate(ConfigSaveValidationContext.forChain(new BasicCruiseConfig(), pipelineConfig));
        assertThat(dependencyMaterialConfig.errors().firstErrorOn(AbstractMaterialConfig.MATERIAL_NAME)).isNull();
    }

    @Test
    void shouldNOTBeValidIfThePipelineExistsButTheStageDoesNot() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(cis("pipeline2"), cis("stage-not-existing does not exist!"));
        dependencyMaterialConfig.validate(ConfigSaveValidationContext.forChain(config, pipelineConfig));
        ConfigErrors configErrors = dependencyMaterialConfig.errors();
        assertThat(configErrors.isEmpty()).isFalse();
        assertThat(configErrors.firstErrorOn(DependencyMaterialConfig.PIPELINE_STAGE_NAME)).contains("Stage with name 'stage-not-existing does not exist!' does not exist on pipeline 'pipeline2'");
    }

    @Test
    void shouldNOTBeValidIfTheReferencedPipelineDoesNotExist() {
        CruiseConfig config = GoConfigMother.configWithPipelines("pipeline1", "pipeline2", "pipeline3", "go");

        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(cis("pipeline-not-exist"), cis("stage"));
        dependencyMaterialConfig.validate(ConfigSaveValidationContext.forChain(config, pipelineConfig));
        ConfigErrors configErrors = dependencyMaterialConfig.errors();
        assertThat(configErrors.isEmpty()).isFalse();
        assertThat(configErrors.firstErrorOn(DependencyMaterialConfig.PIPELINE_STAGE_NAME)).contains("Pipeline with name 'pipeline-not-exist' does not exist");
    }

    @Test
    void setConfigAttributes_shouldPopulateFromConfigAttributes() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(cis(""), cis(""));
        assertThat(dependencyMaterialConfig.getPipelineStageName()).isNull();
        assertThat(dependencyMaterialConfig.ignoreForScheduling()).isFalse();
        Map<String, String> configMap = new HashMap<>();
        configMap.put(AbstractMaterialConfig.MATERIAL_NAME, "name1");
        configMap.put(DependencyMaterialConfig.PIPELINE_STAGE_NAME, "pipeline-1 [stage-1]");
        configMap.put(DependencyMaterialConfig.IGNORE_FOR_SCHEDULING, "true");

        dependencyMaterialConfig.setConfigAttributes(configMap);

        assertThat(dependencyMaterialConfig.getMaterialName()).isEqualTo(cis("name1"));
        assertThat(dependencyMaterialConfig.getPipelineName()).isEqualTo(cis("pipeline-1"));
        assertThat(dependencyMaterialConfig.getStageName()).isEqualTo(cis("stage-1"));
        assertThat(dependencyMaterialConfig.getPipelineStageName()).isEqualTo("pipeline-1 [stage-1]");
        assertThat(dependencyMaterialConfig.ignoreForScheduling()).isTrue();
    }

    @Test
    void setConfigAttributes_shouldNotPopulateNameFromConfigAttributesIfNameIsEmptyOrNull() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(cis("name2"), cis("pipeline"), cis("stage"));
        Map<String, String> configMap = new HashMap<>();
        configMap.put(AbstractMaterialConfig.MATERIAL_NAME, "");

        dependencyMaterialConfig.setConfigAttributes(configMap);

        assertThat(dependencyMaterialConfig.getMaterialName()).isNull();
    }

    @Test
    void shouldValidateTree() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(cis("upstream_stage"), cis("upstream_pipeline"), cis("stage"));
        PipelineConfig pipeline = new PipelineConfig(cis("p"), new MaterialConfigs());
        pipeline.setOrigin(new FileConfigOrigin());
        dependencyMaterialConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", config, pipeline));
        assertThat(dependencyMaterialConfig.errors().firstErrorOn(DependencyMaterialConfig.PIPELINE_STAGE_NAME)).isEqualTo("Pipeline with name 'upstream_pipeline' does not exist, it is defined as a dependency for pipeline 'p' (cruise-config.xml)");
    }

    @Test
    void shouldSetLongDescriptionAsCombinationOfPipelineAndStageName() {
        DependencyMaterialConfig dependencyMaterialConfig = new DependencyMaterialConfig(cis("upstream_stage"), cis("upstream_pipeline"), cis("stage"));

        assertThat(dependencyMaterialConfig.getLongDescription()).isEqualTo("upstream_pipeline [ stage ]");
    }

    @Test
    void shouldNotDefaultToPipelineNameSinceItsUsedToSerializeConfigToJSON() {
        DependencyMaterialConfig config = new DependencyMaterialConfig(
            cis("pipeline_name"), cis("stage_name"));

        assertThat(config.getNameWithoutDefaults()).isNull();

        config = new DependencyMaterialConfig(cis("material_name"),
            cis("pipeline_name"), cis("stage_name"));

        assertThat(config.getNameWithoutDefaults()).isEqualTo(cis("material_name"));
    }
}