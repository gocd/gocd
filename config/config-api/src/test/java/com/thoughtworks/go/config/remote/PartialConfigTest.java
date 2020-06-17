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

package com.thoughtworks.go.config.remote;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.rules.Allow;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.helper.MaterialConfigsMother.dependencyMaterialConfig;
import static org.junit.jupiter.api.Assertions.*;

class PartialConfigTest {
    private PartialConfig partial;
    private ConfigRepoConfig configRepo;

    @BeforeEach
    void setUp() {
        partial = new PartialConfig();
        configRepo = new ConfigRepoConfig();
        partial.setOrigin(new RepoConfigOrigin(configRepo, "123"));
    }

    @Test
    void validatePermissionsOnSubtreeFailsWhenNoRulesPresent() {
        BasicPipelineConfigs group = new BasicPipelineConfigs("first", new Authorization());
        group.add(PipelineConfigMother.pipelineConfig("up42"));
        partial.setPipelines(new PipelineGroups(group));
        partial.setOrigins(partial.getOrigin());

        partial.validatePermissionsOnSubtree();

        assertTrue(partial.hasErrors());
        assertEquals(1, partial.errors().size());
        assertEquals("Not allowed to refer to pipeline group 'first'. Check the 'Rules' of this config repository.", partial.errors().on("pipeline_group"));
    }

    @Test
    void validatePermissionsOnSubtreeFailsWhenNoRuleMatchesGroups() {
        configRepo.getRules().add(new Allow("refer", "pipeline_group", "team1"));

        BasicPipelineConfigs team1 = new BasicPipelineConfigs("team1", new Authorization());
        BasicPipelineConfigs first = new BasicPipelineConfigs("first", new Authorization());
        first.add(PipelineConfigMother.pipelineConfig("up42"));
        partial.setPipelines(new PipelineGroups(first, team1));
        partial.setOrigins(partial.getOrigin());

        partial.validatePermissionsOnSubtree();

        assertTrue(partial.hasErrors());
        assertEquals(1, partial.errors().size());
        assertEquals("Not allowed to refer to pipeline group 'first'. Check the 'Rules' of this config repository.", partial.errors().on("pipeline_group"));
    }

    @Test
    void validatePermissionsOnSubtreeFailsWhenNoRuleMatchesEnvironments() {
        configRepo.getRules().add(new Allow("refer", "environment", "prod"));

        EnvironmentsConfig allEnvs = new EnvironmentsConfig();
        CaseInsensitiveString prodEnv = new CaseInsensitiveString("prod");
        CaseInsensitiveString uatEnv = new CaseInsensitiveString("uat");
        allEnvs.add(new BasicEnvironmentConfig(uatEnv));
        allEnvs.add(new BasicEnvironmentConfig(prodEnv));
        partial.setEnvironments(allEnvs);
        partial.setOrigins(partial.getOrigin());

        partial.validatePermissionsOnSubtree();

        assertTrue(partial.hasErrors());
        assertEquals(1, partial.errors().size());

        assertEquals("Not allowed to refer to environment 'uat'. Check the 'Rules' of this config repository.", partial.errors().on("environment"));
    }

    @Test
    void validatePermissionsOnSubtreeFailsWhenNoRuleMatchesUpstreamDependency() {
        configRepo.getRules().add(new Allow("refer", "pipeline_group", "*"));
        configRepo.getRules().add(new Allow("refer", "pipeline", "build"));

        BasicPipelineConfigs group = new BasicPipelineConfigs("first", new Authorization());
        group.add(PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs(dependencyMaterialConfig("build", "stage"), dependencyMaterialConfig("deploy", "stage"))));
        partial.setPipelines(new PipelineGroups(group));
        partial.setOrigins(partial.getOrigin());

        partial.validatePermissionsOnSubtree();

        assertTrue(partial.hasErrors());
        assertEquals(1, partial.errors().size());
        assertEquals("Not allowed to refer to pipeline 'deploy'. Check the 'Rules' of this config repository.", partial.errors().on("pipeline"));
    }

    @Test
    @DisplayName("validatePermissionsOnSubtree() succeeds when preflighting ConfigRepos that do not yet exist")
    void validatePermissionsOnSubtreeSucceedsWhenNoConfigRepositoryIsSpecified() {
        BasicPipelineConfigs group = new BasicPipelineConfigs("first", new Authorization());
        group.add(PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs(dependencyMaterialConfig("build", "stage"), dependencyMaterialConfig("deploy", "stage"))));
        partial.setPipelines(new PipelineGroups(group));
        partial.setOrigins(null);

        partial.validatePermissionsOnSubtree();

        assertFalse(partial.hasErrors());
    }

    @Test
    void shouldAllowToDefineUpstreamDependencyFromTheSameConfigRepositoryRegardlessOfRules() {
        configRepo.getRules().add(new Allow("refer", "pipeline_group", "*"));
        configRepo.getRules().add(new Allow("refer", "pipeline", "build"));

        BasicPipelineConfigs group = new BasicPipelineConfigs("first", new Authorization());
        group.add(PipelineConfigMother.pipelineConfig("upstream", new MaterialConfigs(dependencyMaterialConfig("build", "stage"))));
        group.add(PipelineConfigMother.pipelineConfig("downstream", new MaterialConfigs(dependencyMaterialConfig("upstream", "stage"))));
        partial.setPipelines(new PipelineGroups(group));
        partial.setOrigins(partial.getOrigin());

        partial.validatePermissionsOnSubtree();

        assertFalse(partial.hasErrors());
    }
}