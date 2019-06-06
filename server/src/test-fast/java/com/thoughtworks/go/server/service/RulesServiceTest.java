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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.rules.Allow;
import com.thoughtworks.go.config.rules.Rules;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.exceptions.RulesViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class RulesServiceTest {
    @Mock
    GoConfigService goConfigService;
    RulesService rulesService;

    @BeforeEach
    void setUp() {
        initMocks(this);

        rulesService = new RulesService(goConfigService);
    }

    @Nested
    class validateSecretConfigReferences_forMaterials {
        @Test
        void shouldErrorOutIfMaterialDoesNotHavePermissionToReferASecretConfig() {
            GitMaterial gitMaterial = new GitMaterial("http://example.com");
            gitMaterial.setPassword("{{SECRET:[secret_config_id][password]}}");

            Rules rules = new Rules(new Allow("refer", "pipeline_group", "default"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            PipelineConfig up42 = PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs(gitMaterial.config()));
            PipelineConfigs defaultGroup = PipelineConfigMother.createGroup("some_group", up42);
            CruiseConfig cruiseConfig = GoConfigMother.configWithSecretConfig(secretConfig);
            cruiseConfig.setGroup(new PipelineGroups(defaultGroup));

            when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
            when(goConfigService.pipelinesWithMaterial(gitMaterial.getFingerprint())).thenReturn(asList(new CaseInsensitiveString("up42")));
            when(goConfigService.findGroupByPipeline(any())).thenReturn(defaultGroup);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(gitMaterial))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Material with url: 'http://example.com' in Pipeline: 'up42' and " +
                            "Pipeline Group: 'some_group' does not have permission to refer to Secrets using SecretConfig: 'secret_config_id'");
        }

        @Test
        void forAMaterialReferredInMultiplePipelineGroups_shouldErrorOutEvenIfOneOfThePipelineDoesNotHavePermissionToReferASecretConfig() {
            GitMaterial gitMaterial = new GitMaterial("http://example.com");
            gitMaterial.setPassword("{{SECRET:[secret_config_id][password]}}");

            Rules rules = new Rules(new Allow("refer", "pipeline_group", "default"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            PipelineConfig up42 = PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs(gitMaterial.config()));
            PipelineConfig up43 = PipelineConfigMother.pipelineConfig("up43", new MaterialConfigs(gitMaterial.config()));
            PipelineConfigs defaultGroup = PipelineConfigMother.createGroup("default", up42);
            PipelineConfigs someGroup = PipelineConfigMother.createGroup("some_group", up43);
            CruiseConfig cruiseConfig = GoConfigMother.configWithSecretConfig(secretConfig);
            cruiseConfig.setGroup(new PipelineGroups(defaultGroup, someGroup));

            when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
            when(goConfigService.pipelinesWithMaterial(gitMaterial.getFingerprint())).thenReturn(asList(new CaseInsensitiveString("up42"), new CaseInsensitiveString("up43")));
            when(goConfigService.findGroupByPipeline(new CaseInsensitiveString("up42"))).thenReturn(defaultGroup);
            when(goConfigService.findGroupByPipeline(new CaseInsensitiveString("up43"))).thenReturn(someGroup);

            assertThatCode(() -> rulesService.validateSecretConfigReferences(gitMaterial))
                    .isInstanceOf(RulesViolationException.class)
                    .hasMessage("Material with url: 'http://example.com' in Pipeline: 'up43' and " +
                            "Pipeline Group: 'some_group' does not have permission to refer to Secrets using SecretConfig: 'secret_config_id'");
        }

        @Test
        void shouldValidateIfMaterialHasPermissionToReferASecretConfig() {
            GitMaterial gitMaterial = new GitMaterial("http://example.com");
            gitMaterial.setPassword("{{SECRET:[secret_config_id][password]}}");

            Rules rules = new Rules(new Allow("refer", "pipeline_group", "default"));
            SecretConfig secretConfig = new SecretConfig("secret_config_id", "cd.go.file", rules);

            PipelineConfig up42 = PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs(gitMaterial.config()));
            PipelineConfigs defaultGroup = PipelineConfigMother.createGroup("default", up42);
            CruiseConfig cruiseConfig = GoConfigMother.configWithSecretConfig(secretConfig);
            cruiseConfig.setGroup(new PipelineGroups(defaultGroup));

            when(goConfigService.cruiseConfig()).thenReturn(cruiseConfig);
            when(goConfigService.pipelinesWithMaterial(gitMaterial.getFingerprint())).thenReturn(asList(new CaseInsensitiveString("up42")));
            when(goConfigService.findGroupByPipeline(any())).thenReturn(defaultGroup);

            assertThat(rulesService.validateSecretConfigReferences(gitMaterial)).isEqualTo(true);

        }
    }
}
