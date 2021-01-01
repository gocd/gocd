/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.ConfigRepoConfig;
import com.thoughtworks.go.config.remote.PartialConfig;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.config.rules.Allow;
import com.thoughtworks.go.domain.PipelineGroups;
import com.thoughtworks.go.helper.PipelineConfigMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
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
    void update() {
        configRepoConfig.getRules().add(new Allow("refer", "*", "*"));

        BasicPipelineConfigs group = new BasicPipelineConfigs("first", new Authorization());
        group.setOrigin(new RepoConfigOrigin());
        group.add(PipelineConfigMother.pipelineConfig("up42"));
        partial.setPipelines(new PipelineGroups(group));
        partial.setOrigins(new RepoConfigOrigin(configRepoConfig, "123"));

        partial.validatePermissionsOnSubtree();
        assertFalse(partial.hasErrors());

        PartialConfigUpdateCommand command = new PartialConfigUpdateCommand(partial, fingerprint, resolver);
        CruiseConfig updated = command.update(cruiseConfig);

        assertTrue(updated.hasPipelineGroup("first"));
        assertEquals(1, updated.getPartials().size());
    }

    @Test
    void updateWillNotCreateGroupsOrEnvironmentsIfRuleViolationsExist() {
        BasicPipelineConfigs group = new BasicPipelineConfigs("first", new Authorization());
        group.setOrigin(new RepoConfigOrigin());
        group.add(PipelineConfigMother.pipelineConfig("up42"));
        partial.setPipelines(new PipelineGroups(group));
        EnvironmentsConfig allEnvs = new EnvironmentsConfig();
        CaseInsensitiveString prodEnv = new CaseInsensitiveString("prod");
        allEnvs.add(new BasicEnvironmentConfig(prodEnv));
        partial.setEnvironments(allEnvs);
        partial.setOrigins(new RepoConfigOrigin(configRepoConfig, "123"));

        partial.validatePermissionsOnSubtree();
        assertTrue(partial.hasErrors());

        PartialConfigUpdateCommand command = new PartialConfigUpdateCommand(partial, fingerprint, resolver);
        CruiseConfig updated = command.update(cruiseConfig);

        assertEquals(1, updated.getPartials().size());
        assertFalse(updated.hasPipelineGroup("first"));
        assertFalse(updated.getEnvironments().hasEnvironmentNamed(prodEnv));
    }
}
