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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.config.remote.UIConfigOrigin;
import com.thoughtworks.go.domain.ConfigErrors;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.thoughtworks.go.config.ConfigSaveValidationContext.forChain;
import static com.thoughtworks.go.helper.EnvironmentConfigMother.environment;
import static com.thoughtworks.go.helper.EnvironmentConfigMother.remote;
import static com.thoughtworks.go.util.command.EnvironmentVariableContext.GO_ENVIRONMENT_NAME;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.*;

class BasicEnvironmentConfigTest extends EnvironmentConfigTestBase {
    @BeforeEach
    void setUp() throws Exception {
        environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
    }

    @Test
    void shouldReturnEmptyForRemotePipelinesWhenIsLocal() {
        environmentConfig.addPipeline(new CaseInsensitiveString("pipe"));
        assertThat(environmentConfig.getRemotePipelines().isEmpty()).isTrue();
    }

    @Test
    void shouldReturnAllPipelinesForRemotePipelinesWhenIsRemote() {
        environmentConfig.setOrigins(new RepoConfigOrigin());
        environmentConfig.addPipeline(new CaseInsensitiveString("pipe"));
        assertThat(environmentConfig.getRemotePipelines().isEmpty()).isFalse();
    }

    @Test
    void shouldReturnTrueThatLocalWhenOriginIsNotSet() {
        environmentConfig.setOrigins(null);
        assertThat(environmentConfig.isLocal()).isTrue();
    }

    @Test
    void shouldReturnTrueThatLocalWhenOriginIsFile() {
        environmentConfig.setOrigins(new FileConfigOrigin());
        assertThat(environmentConfig.isLocal()).isTrue();
    }

    @Test
    void shouldReturnFalseThatLocalWhenOriginIsConfigRepo() {
        environmentConfig.setOrigins(new RepoConfigOrigin());
        assertThat(environmentConfig.isLocal()).isFalse();
    }

    @Test
    void shouldReturnSelfAsLocalPartWhenOriginIsFile() {
        environmentConfig.setOrigins(new FileConfigOrigin());
        assertThat(environmentConfig.getLocal()).isSameAs(environmentConfig);
    }

    @Test
    void shouldReturnSelfAsLocalPartWhenOriginIsUI() {
        environmentConfig.setOrigins(new UIConfigOrigin());
        assertThat(environmentConfig.getLocal()).isSameAs(environmentConfig);

    }

    @Test
    void shouldReturnNullAsLocalPartWhenOriginIsConfigRepo() {
        environmentConfig.setOrigins(new RepoConfigOrigin());
        assertThat(environmentConfig.getLocal()).isNull();
    }

    @Test
    void shouldUpdateName() {
        environmentConfig.setConfigAttributes(Collections.singletonMap(BasicEnvironmentConfig.NAME_FIELD, "PROD"));
        assertThat(environmentConfig.name()).isEqualTo(new CaseInsensitiveString("PROD"));
    }

    @Nested
    class Validate {
        @Test
        void shouldNotAllowToReferencePipelineDefinedInConfigRepo_WhenEnvironmentDefinedInFile() {
            ConfigOrigin pipelineOrigin = new RepoConfigOrigin();
            ConfigOrigin envOrigin = new FileConfigOrigin();

            BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("pipe1");
            cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("pipe1")).setOrigin(pipelineOrigin);
            BasicEnvironmentConfig environmentConfig = (BasicEnvironmentConfig) BasicEnvironmentConfigTest.this.environmentConfig;
            environmentConfig.setOrigins(envOrigin);
            environmentConfig.addPipeline(new CaseInsensitiveString("pipe1"));
            cruiseConfig.addEnvironment(environmentConfig);

            environmentConfig.validate(forChain(cruiseConfig, environmentConfig));
            EnvironmentPipelineConfig reference = environmentConfig.getPipelines().first();

            assertThat(reference.errors()).isNotEmpty();
            assertThat(reference.errors().on(EnvironmentPipelineConfig.ORIGIN)).startsWith("Environment defined in");
        }

        @Test
        void shouldAllowToReferencePipelineDefinedInConfigRepo_WhenEnvironmentDefinedInConfigRepo() {
            ConfigOrigin pipelineOrigin = new RepoConfigOrigin();
            ConfigOrigin envOrigin = new RepoConfigOrigin();

            passReferenceValidationHelper(pipelineOrigin, envOrigin);
        }

        @Test
        void shouldAllowToReferencePipelineDefinedInFile_WhenEnvironmentDefinedInFile() {
            ConfigOrigin pipelineOrigin = new FileConfigOrigin();
            ConfigOrigin envOrigin = new FileConfigOrigin();

            passReferenceValidationHelper(pipelineOrigin, envOrigin);
        }

        @Test
        void shouldAllowToReferencePipelineDefinedInFile_WhenEnvironmentDefinedInConfigRepo() {
            ConfigOrigin pipelineOrigin = new FileConfigOrigin();
            ConfigOrigin envOrigin = new RepoConfigOrigin();

            passReferenceValidationHelper(pipelineOrigin, envOrigin);
        }
    }

    @Test
    void shouldValidateWhenPipelineNotFound() {
        ConfigOrigin pipelineOrigin = new RepoConfigOrigin();
        ConfigOrigin envOrigin = new FileConfigOrigin();

        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("pipe1");
        cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("pipe1")).setOrigin(pipelineOrigin);
        BasicEnvironmentConfig environmentConfig = (BasicEnvironmentConfig) this.environmentConfig;
        environmentConfig.setOrigins(envOrigin);
        environmentConfig.addPipeline(new CaseInsensitiveString("unknown"));
        cruiseConfig.addEnvironment(environmentConfig);

        environmentConfig.validate(forChain(cruiseConfig, environmentConfig));
        EnvironmentPipelineConfig reference = environmentConfig.getPipelines().first();

        assertThat(reference.errors().isEmpty()).isTrue();
    }

    @Nested
    class ValidateTree {
        @Test
        void shouldNotAllowToReferencePipelineDefinedInConfigRepo_WhenEnvironmentDefinedInFile() {
            ConfigOrigin pipelineOrigin = new RepoConfigOrigin();
            ConfigOrigin envOrigin = new FileConfigOrigin();

            BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("pipe1");
            cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("pipe1")).setOrigin(pipelineOrigin);
            BasicEnvironmentConfig environmentConfig = (BasicEnvironmentConfig) BasicEnvironmentConfigTest.this.environmentConfig;
            environmentConfig.setOrigins(envOrigin);
            environmentConfig.addPipeline(new CaseInsensitiveString("pipe1"));
            cruiseConfig.addEnvironment(environmentConfig);

            environmentConfig.validateTree(forChain(cruiseConfig, environmentConfig), cruiseConfig);
            EnvironmentPipelineConfig reference = environmentConfig.getPipelines().first();

            assertThat(reference.errors()).isNotEmpty();
            assertThat(reference.errors().on(EnvironmentPipelineConfig.ORIGIN)).startsWith("Environment defined in");
        }

        @Test
        void shouldAllowToReferencePipelineDefinedInConfigRepo_WhenEnvironmentDefinedInConfigRepo() {
            ConfigOrigin pipelineOrigin = new RepoConfigOrigin();
            ConfigOrigin envOrigin = new RepoConfigOrigin();

            passReferenceValidationHelperForValidateTree(pipelineOrigin, envOrigin);
        }

        @Test
        void shouldAllowToReferencePipelineDefinedInFile_WhenEnvironmentDefinedInFile() {
            ConfigOrigin pipelineOrigin = new FileConfigOrigin();
            ConfigOrigin envOrigin = new FileConfigOrigin();

            passReferenceValidationHelperForValidateTree(pipelineOrigin, envOrigin);
        }

        @Test
        void shouldAllowToReferencePipelineDefinedInFile_WhenEnvironmentDefinedInConfigRepo() {
            ConfigOrigin pipelineOrigin = new FileConfigOrigin();
            ConfigOrigin envOrigin = new RepoConfigOrigin();

            passReferenceValidationHelperForValidateTree(pipelineOrigin, envOrigin);
        }

        @Test
        void shouldValidateEnvVariables() {
            ConfigOrigin repoConfigOrigin = new RepoConfigOrigin();

            BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("pipe1");
            cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("pipe1")).setOrigin(repoConfigOrigin);
            BasicEnvironmentConfig environmentConfig = (BasicEnvironmentConfig) BasicEnvironmentConfigTest.this.environmentConfig;
            environmentConfig.setOrigins(repoConfigOrigin);
            environmentConfig.addPipeline(new CaseInsensitiveString("pipe1"));
            environmentConfig.addEnvironmentVariable("  ", "bar");
            cruiseConfig.addEnvironment(environmentConfig);

            boolean validate = environmentConfig.validateTree(forChain(cruiseConfig, environmentConfig), cruiseConfig);
            List<ConfigErrors> configErrors = environmentConfig.getAllErrors();
            assertThat(validate).isFalse();
            assertThat(configErrors).isNotEmpty();
            assertThat(configErrors.get(0).on("name")).isEqualTo("Environment Variable cannot have an empty name for environment 'UAT'.");
        }

        @Test
        void shouldValidateViaValidateTreeWhenPipelineNotFound() {
            ConfigOrigin pipelineOrigin = new RepoConfigOrigin();
            ConfigOrigin envOrigin = new FileConfigOrigin();

            BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("pipe1");
            cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("pipe1")).setOrigin(pipelineOrigin);
            BasicEnvironmentConfig environmentConfig = (BasicEnvironmentConfig) BasicEnvironmentConfigTest.this.environmentConfig;
            environmentConfig.setOrigins(envOrigin);
            environmentConfig.addPipeline(new CaseInsensitiveString("unknown"));
            cruiseConfig.addEnvironment(environmentConfig);

            environmentConfig.validate(forChain(cruiseConfig, environmentConfig));
            EnvironmentPipelineConfig reference = environmentConfig.getPipelines().first();

            assertThat(reference.errors().isEmpty()).isTrue();
        }
    }

    @Test
    void shouldReturnEnvironmentContextWithGO_ENVIRONMENT_NAMEVariableWhenNoEnvironmentVariablesAreDefined() {
        EnvironmentVariableContext environmentContext = environmentConfig.createEnvironmentContext();

        assertThat(environmentContext.getProperties()).hasSize(1);
        assertThat(environmentContext.getProperty(GO_ENVIRONMENT_NAME)).isEqualTo(environmentConfig.name().toString());
    }

    @Test
    void shouldReturnEnvironmentContextWithGO_ENVIRONMENT_NAMEVariableWhenEnvironmentVariablesAreDefined() {
        environmentConfig.addEnvironmentVariable("foo", "bar");
        EnvironmentVariableContext environmentContext = environmentConfig.createEnvironmentContext();

        assertThat(environmentContext.getProperties()).hasSize(2);
        assertThat(environmentContext.getProperty(GO_ENVIRONMENT_NAME)).isEqualTo(environmentConfig.name().toString());
        assertThat(environmentContext.getProperty("foo")).isEqualTo("bar");
    }

    @Test
    void shouldAddErrorToTheConfig() {
        assertTrue(environmentConfig.errors().isEmpty());

        environmentConfig.addError("field-name", "some error message.");

        assertThat(environmentConfig.errors().size()).isEqualTo(1);
        assertThat(environmentConfig.errors().on("field-name")).isEqualTo("some error message.");
    }

    @Test
    void shouldReturnMatchersWithTheProperties() {
        environmentConfig.addPipeline(new CaseInsensitiveString("pipeline-1"));
        environmentConfig.addAgent("agent-1");

        EnvironmentPipelineMatcher matcher = environmentConfig.createMatcher();

        assertNotNull(matcher);
        assertThat(matcher.name()).isEqualTo(environmentConfig.name());
        assertTrue(matcher.hasPipeline("pipeline-1"));
        assertTrue(matcher.match("pipeline-1", "agent-1"));

        assertFalse(matcher.hasPipeline("non-existent-pipeline"));
    }

    @Test
    void shouldNotThrowExceptionIfAllThePipelinesArePresent() {
        CaseInsensitiveString p1 = new CaseInsensitiveString("pipeline-1");
        CaseInsensitiveString p2 = new CaseInsensitiveString("pipeline-2");

        environmentConfig.addPipeline(p1);
        environmentConfig.addPipeline(p2);

        assertThatCode(() -> environmentConfig.validateContainsOnlyPipelines(asList(p1, p2)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrowExceptionIfOneOfThePipelinesAreNotPassed() {
        CaseInsensitiveString p1 = new CaseInsensitiveString("pipeline-1");
        CaseInsensitiveString p2 = new CaseInsensitiveString("pipeline-2");
        CaseInsensitiveString p3 = new CaseInsensitiveString("pipeline-3");

        environmentConfig.addPipeline(p1);
        environmentConfig.addPipeline(p2);

        assertThatCode(() -> environmentConfig.validateContainsOnlyPipelines(asList(p1, p3)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Environment 'UAT' refers to an unknown pipeline 'pipeline-2'.");
    }

    @Test
    void shouldReturnTrueIsChildConfigContainsNoPipelineAgentsAndVariables() {
        assertTrue(environmentConfig.isEnvironmentEmpty());
    }

    @Test
    void shouldReturnFalseIfNotEmpty() {
        environmentConfig.addPipeline(new CaseInsensitiveString("pipeline1"));
        assertFalse(environmentConfig.isEnvironmentEmpty());
    }

    @Nested
    class ContainsAgentRemotely {

        @Test
        void shouldReturnTrueIfTheEnvAgentAssociationIsFromConfigRepo() {
            BasicEnvironmentConfig environmentConfig = remote("env1");
            String uuid = "uuid";
            environmentConfig.addAgent(uuid);

            assertThat(environmentConfig.containsAgentRemotely(uuid)).isTrue();
        }

        @Test
        void shouldReturnFalseIfTheAgentIsNotPresentInTheEnvFromConfigRepo() {
            BasicEnvironmentConfig environmentConfig = remote("env1");
            String uuid = "uuid";

            assertThat(environmentConfig.containsAgentRemotely(uuid)).isFalse();
        }

        @Test
        void shouldReturnFalseIfTheEnvAgentAssociationIsFromConfigXml() {
            BasicEnvironmentConfig environmentConfig = environment("env1");
            String uuid = "uuid";
            environmentConfig.addAgent(uuid);

            assertThat(environmentConfig.containsAgentRemotely(uuid)).isFalse();
        }

        @Test
        void shouldReturnFalseIfTheOriginIsNull() {
            BasicEnvironmentConfig environmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("env1"));
            String uuid = "uuid";
            environmentConfig.addAgent(uuid);

            assertThat(environmentConfig.getOrigin()).isNull();
            assertThat(environmentConfig.containsAgentRemotely(uuid)).isFalse();
        }
    }

    @Test
    void shouldReturnEmptyOptionalIfEnvDoesNotContainTheAgent() {
        Optional<ConfigOrigin> originForAgent = this.environmentConfig.originForAgent("uuid");

        assertThat(originForAgent.isPresent()).isFalse();
    }

    @Test
    void shouldReturnOriginIfEnvContainsTheAgent() {
        String uuid = "uuid";
        this.environmentConfig.addAgent(uuid);
        this.environmentConfig.setOrigins(new FileConfigOrigin());
        Optional<ConfigOrigin> originForAgent = this.environmentConfig.originForAgent(uuid);

        assertThat(originForAgent.isPresent()).isTrue();
        assertThat(originForAgent.get().displayName()).isEqualTo("cruise-config.xml");
    }

    @Test
    void shouldReturnFalseIfEnvDoesNotContainTheSpecifiedAgentUuid() {
        assertThat(this.environmentConfig.hasAgent("uuid")).isFalse();
    }

    @Test
    void shouldReturnTrueIfEnvContainsTheSpecifiedAgentUuid() {
        this.environmentConfig.addAgent("uuid");
        assertThat(this.environmentConfig.hasAgent("uuid")).isTrue();
    }

    private void passReferenceValidationHelper(ConfigOrigin pipelineOrigin, ConfigOrigin envOrigin) {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("pipe1");
        cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("pipe1")).setOrigin(pipelineOrigin);
        BasicEnvironmentConfig environmentConfig = (BasicEnvironmentConfig) this.environmentConfig;
        environmentConfig.setOrigins(envOrigin);
        environmentConfig.addPipeline(new CaseInsensitiveString("pipe1"));
        cruiseConfig.addEnvironment(environmentConfig);

        environmentConfig.validate(forChain(cruiseConfig, environmentConfig));
        EnvironmentPipelineConfig reference = environmentConfig.getPipelines().first();

        assertThat(reference.errors().isEmpty()).isTrue();
    }

    private void passReferenceValidationHelperForValidateTree(ConfigOrigin pipelineOrigin, ConfigOrigin envOrigin) {
        BasicCruiseConfig cruiseConfig = GoConfigMother.configWithPipelines("pipe1");
        cruiseConfig.getPipelineConfigByName(new CaseInsensitiveString("pipe1")).setOrigin(pipelineOrigin);
        BasicEnvironmentConfig environmentConfig = (BasicEnvironmentConfig) this.environmentConfig;
        environmentConfig.setOrigins(envOrigin);
        environmentConfig.addPipeline(new CaseInsensitiveString("pipe1"));
        cruiseConfig.addEnvironment(environmentConfig);

        environmentConfig.validateTree(forChain(cruiseConfig, environmentConfig), cruiseConfig);
        EnvironmentPipelineConfig reference = environmentConfig.getPipelines().first();

        assertThat(reference.errors().isEmpty()).isTrue();
    }
}
