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

package com.thoughtworks.go.config.update;

import com.thoughtworks.go.config.ErrorCollector;
import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.config.rules.Allow;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.plugin.access.configrepo.InvalidPartialConfigException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static com.thoughtworks.go.helper.MaterialConfigsMother.dependencyMaterialConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.MockitoAnnotations.initMocks;

class PartialConfigUpdateCommandTest {

    private BasicCruiseConfig cruiseConfig;
    private ConfigRepoConfig configRepoConfig;
    private PartialConfig partial;
    private String fingerprint;
    @Mock
    private PartialConfigResolver resolver;

    @BeforeEach
    void setUp() {
        initMocks(this);
        cruiseConfig = new BasicCruiseConfig();
        configRepoConfig = new ConfigRepoConfig();
        partial = new PartialConfig();
        fingerprint = "some-fingerprint";
    }

    @Test
    void shouldUpdateSuccessfully() {
        configRepoConfig.getRules().add(new Allow("refer", "*", "*"));

        BasicPipelineConfigs group = new BasicPipelineConfigs("first", new Authorization());
        group.setOrigin(new RepoConfigOrigin());
        group.add(PipelineConfigMother.pipelineConfig("up42"));
        partial.setPipelines(new PipelineGroups(group));
        PartialConfigUpdateCommand command = new PartialConfigUpdateCommand(partial, fingerprint, resolver, configRepoConfig);
        CruiseConfig updated = command.update(cruiseConfig);

        assertThat(ErrorCollector.getAllErrors(this.partial)).isEmpty();
        assertThat(updated.hasPipelineGroup("first")).isTrue();
        assertThat(updated.getPartials()).hasSize(1);
    }

    @Test
    void shouldFailToUpdateWhenNoRulesAreConfigured() {
        BasicPipelineConfigs group = new BasicPipelineConfigs("first", new Authorization());
        group.setOrigin(new RepoConfigOrigin());
        group.add(PipelineConfigMother.pipelineConfig("up42"));
        partial.setPipelines(new PipelineGroups(group));
        PartialConfigUpdateCommand command = new PartialConfigUpdateCommand(partial, fingerprint, resolver, configRepoConfig);

        assertThatCode(() -> command.update(cruiseConfig))
                .isInstanceOf(InvalidPartialConfigException.class)
                .hasMessage("Configurations can not be merged as no rules are defined.");
    }

    @Test
    void shouldFailToDefinePipelineGroupWhenNoRuleMatches() {
        configRepoConfig.getRules().add(new Allow("refer", "pipeline_group", "team1"));

        BasicPipelineConfigs team1 = new BasicPipelineConfigs("team1", new Authorization());
        BasicPipelineConfigs first = new BasicPipelineConfigs("first", new Authorization());
        first.setOrigin(new RepoConfigOrigin());
        first.add(PipelineConfigMother.pipelineConfig("up42"));
        partial.setPipelines(new PipelineGroups(first, team1));
        PartialConfigUpdateCommand command = new PartialConfigUpdateCommand(partial, fingerprint, resolver, configRepoConfig);
        CruiseConfig updated = command.update(cruiseConfig);

        List<ConfigErrors> allErrors = ErrorCollector.getAllErrors(cruiseConfig.getPartials().get(0));
        assertThat(allErrors).hasSize(1);
        assertThat(allErrors.get(0).on("pipeline_group")).isEqualTo("Not allowed to refer pipeline group 'first' from the config repository.");
        assertThat(updated.hasPipelineGroup("first")).isFalse();
    }

    @Test
    void shouldFailToDefineEnvironmentWhenNoRuleMatches() {
        configRepoConfig.getRules().add(new Allow("refer", "environment", "prod"));

        EnvironmentsConfig allEnvs = new EnvironmentsConfig();
        CaseInsensitiveString prodEnv = new CaseInsensitiveString("prod");
        CaseInsensitiveString uatEnv = new CaseInsensitiveString("uat");
        allEnvs.add(new BasicEnvironmentConfig(uatEnv));
        allEnvs.add(new BasicEnvironmentConfig(prodEnv));
        partial.setEnvironments(allEnvs);
        PartialConfigUpdateCommand command = new PartialConfigUpdateCommand(partial, fingerprint, resolver, configRepoConfig);
        CruiseConfig updated = command.update(cruiseConfig);

        List<ConfigErrors> allErrors = ErrorCollector.getAllErrors(cruiseConfig.getPartials().get(0));
        assertThat(allErrors).hasSize(1);
        assertThat(allErrors.get(0).on("environment")).isEqualTo("Not allowed to refer environment 'uat' from the config repository.");
        assertThat(updated.getEnvironments().hasEnvironmentNamed(uatEnv)).isFalse();
    }

    @Test
    void shouldFailToDefineUpstreamDependencyWhenNoRuleMatches() {
        configRepoConfig.getRules().add(new Allow("refer", "pipeline_group", "*"));
        configRepoConfig.getRules().add(new Allow("refer", "pipeline", "build"));

        BasicPipelineConfigs group = new BasicPipelineConfigs("first", new Authorization());
        group.setOrigin(new RepoConfigOrigin());
        group.add(PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs(dependencyMaterialConfig("build", "stage"), dependencyMaterialConfig("deploy", "stage"))));
        partial.setPipelines(new PipelineGroups(group));
        PartialConfigUpdateCommand command = new PartialConfigUpdateCommand(partial, fingerprint, resolver, configRepoConfig);
        CruiseConfig updated = command.update(cruiseConfig);

        List<ConfigErrors> allErrors = ErrorCollector.getAllErrors(cruiseConfig.getPartials().get(0));
        assertThat(allErrors).hasSize(1);
        assertThat(allErrors.get(0).on("pipeline")).isEqualTo("Not allowed to refer pipeline 'deploy' from the config repository.");
        assertThat(updated.hasPipelineGroup("first")).isFalse();
    }

    @Test
    void shouldAllowToDefineUpstreamDependencyFromTheSameConfigRepositoryRegardlessOfRules() {
        configRepoConfig.getRules().add(new Allow("refer", "pipeline_group", "*"));
        configRepoConfig.getRules().add(new Allow("refer", "pipeline", "build"));

        BasicPipelineConfigs group = new BasicPipelineConfigs("first", new Authorization());
        group.setOrigin(new RepoConfigOrigin());
        group.add(PipelineConfigMother.pipelineConfig("upstream", new MaterialConfigs(dependencyMaterialConfig("build", "stage"))));
        group.add(PipelineConfigMother.pipelineConfig("downstream", new MaterialConfigs(dependencyMaterialConfig("upstream", "stage"))));
        partial.setPipelines(new PipelineGroups(group));
        PartialConfigUpdateCommand command = new PartialConfigUpdateCommand(partial, fingerprint, resolver, configRepoConfig);
        CruiseConfig updated = command.update(cruiseConfig);

        assertThat(ErrorCollector.getAllErrors(this.partial)).isEmpty();
        assertThat(updated.hasPipelineGroup("first")).isTrue();
        assertThat(updated.getPartials()).hasSize(1);
    }

    @Test
    void shouldNotApplyRulesForPreflightCheck_WhenNoConfigRepositoryIsSpecified() {
        BasicPipelineConfigs group = new BasicPipelineConfigs("first", new Authorization());
        group.setOrigin(new RepoConfigOrigin());
        group.add(PipelineConfigMother.pipelineConfig("up42", new MaterialConfigs(dependencyMaterialConfig("build", "stage"), dependencyMaterialConfig("deploy", "stage"))));
        partial.setPipelines(new PipelineGroups(group));
        PartialConfigUpdateCommand command = new PartialConfigUpdateCommand(partial, fingerprint, resolver, null);
        CruiseConfig updated = command.update(cruiseConfig);

        assertThat(ErrorCollector.getAllErrors(this.partial)).isEmpty();
        assertThat(updated.hasPipelineGroup("first")).isTrue();
        assertThat(updated.getPartials()).hasSize(1);
    }
}
