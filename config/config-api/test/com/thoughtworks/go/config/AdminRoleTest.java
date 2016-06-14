/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.helper.StageConfigMother;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AdminRoleTest {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void shouldThrowExceptionIfRoleNameInStageAuthorizationDoesNotExist() throws Exception {
        AdminRole role = new AdminRole(new CaseInsensitiveString("role2"));
        StageConfig stage = StageConfigMother.custom("ft", new AuthConfig(role));
        CruiseConfig config = new BasicCruiseConfig(new BasicPipelineConfigs(new PipelineConfig(new CaseInsensitiveString("pipeline"), new MaterialConfigs(), stage)));
        role.validate(ConfigSaveValidationContext.forChain(config));
        ConfigErrors configErrors = role.errors();
        assertThat(configErrors.isEmpty(), is(false));
        assertThat(configErrors.on(AdminRole.NAME), is("Role \"role2\" does not exist."));
    }

    @Test
    public void shouldThrowExceptionIfRoleNameInPipelinesAuthorizationDoesNotExist_ConfigSaveValidationContext() throws Exception {
        AdminRole role = new AdminRole(new CaseInsensitiveString("role2"));
        PipelineConfigs pipelinesConfig = new BasicPipelineConfigs(new Authorization(new ViewConfig(role)));
        CruiseConfig config = new BasicCruiseConfig(pipelinesConfig);
        role.validate(ConfigSaveValidationContext.forChain(config));
        ConfigErrors errors = role.errors();
        assertThat(errors.isEmpty(), is(false));
        assertThat(errors.on(AdminRole.NAME), is("Role \"role2\" does not exist."));
    }

    @Test
    public void shouldAddValidationErrorIfRoleNameInPipelinesAuthorizationDoesNotExist_PipelineConfigSaveValidationContext() throws Exception {
        AdminRole role = new AdminRole(new CaseInsensitiveString("role2"));
        PipelineConfig pipelineConfig = new PipelineConfig();
        PipelineConfigs pipelinesConfig = new BasicPipelineConfigs(new Authorization(new ViewConfig(role)), pipelineConfig);
        CruiseConfig config = new BasicCruiseConfig(pipelinesConfig);
        role.validate(PipelineConfigSaveValidationContext.forChain(true, "group",config, pipelineConfig));
        ConfigErrors errors = role.errors();
        assertThat(errors.isEmpty(), is(false));
        assertThat(errors.on(AdminRole.NAME), is("Role \"role2\" does not exist."));
    }

    @Test
    public void shouldNotThrowExceptionIfRoleNameExistInPipelinesAuthorization() throws Exception {
        AdminRole role = new AdminRole(new CaseInsensitiveString("role2"));
        PipelineConfigs pipelinesConfig = new BasicPipelineConfigs(new Authorization(new ViewConfig(role)));
        CruiseConfig config = new BasicCruiseConfig(pipelinesConfig);
        config.server().security().addRole(new Role(new CaseInsensitiveString("role2")));
        role.validate(ConfigSaveValidationContext.forChain(config));
        assertThat(role.errors().isEmpty(), is(true));

    }

    @Test
    public void shouldNotThrowExceptionIfRoleNameExist() throws Exception {
        AdminRole role = new AdminRole(new CaseInsensitiveString("role1"));
        StageConfig stage = StageConfigMother.custom("ft", new AuthConfig(role));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs(new PipelineConfig(new CaseInsensitiveString("pipeline"), new MaterialConfigs(), stage));
        CruiseConfig config = new BasicCruiseConfig(pipelineConfigs);
        config.server().security().addRole(new Role(new CaseInsensitiveString("role1")));
        role.validate(ConfigSaveValidationContext.forChain(config));
        assertThat(role.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldNotThrowExceptionIfNoRoleUsed() throws Exception {
        StageConfig stage = new StageConfig(new CaseInsensitiveString("stage-foo"), new JobConfigs(new JobConfig(new CaseInsensitiveString("build-1"), new Resources(), new ArtifactPlans(), new Tasks(new ExecTask("ls", "-la", "work"))
        ))
        );
        PipelineConfigs pipelinesConfig = new BasicPipelineConfigs("group", new Authorization(), new PipelineConfig(new CaseInsensitiveString("pipeline"), new MaterialConfigs(), stage));
        CruiseConfig config = new BasicCruiseConfig(pipelinesConfig);
        config.server().security().addRole(new Role(new CaseInsensitiveString("role1")));
        pipelinesConfig.validate(ConfigSaveValidationContext.forChain(config));
        assertThat(pipelinesConfig.errors().isEmpty(), is(true));
    }

    @Test
    public void shouldThrowExceptionIfRoleNameInPipelinesAuthorizationAdminSectionDoesNotExist() throws Exception {
        AdminRole role = new AdminRole(new CaseInsensitiveString("shilpaIsNotHere"));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs(new Authorization(new AdminsConfig(role)));
        CruiseConfig config = new BasicCruiseConfig(pipelineConfigs);
        role.validate(ConfigSaveValidationContext.forChain(config));
        ConfigErrors errors = role.errors();
        assertThat(errors.isEmpty(), is(false));
        assertThat(errors.on(AdminRole.NAME), is("Role \"shilpaIsNotHere\" does not exist."));
    }

    @Test
    public void shouldNotThrowExceptionIfRoleNameInPipelinesAuthorizationAdminSectionExists() throws Exception {
        AdminRole role = new AdminRole(new CaseInsensitiveString("shilpaIsHere"));
        PipelineConfigs pipelineConfigs = new BasicPipelineConfigs(new Authorization(new AdminsConfig(role)));
        CruiseConfig config = new BasicCruiseConfig(pipelineConfigs);
        config.server().security().addRole(new Role(new CaseInsensitiveString("shilpaIsHere")));
        role.validate(ConfigSaveValidationContext.forChain(config));
        assertThat(role.errors().isEmpty(), is(true));
    }
}
