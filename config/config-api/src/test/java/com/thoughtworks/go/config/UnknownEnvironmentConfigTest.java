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

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.remote.ConfigOrigin;
import com.thoughtworks.go.config.remote.UnknownOrigin;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;

import static com.thoughtworks.go.config.ConfigSaveValidationContext.forChain;
import static com.thoughtworks.go.util.command.EnvironmentVariableContext.GO_ENVIRONMENT_NAME;
import static java.util.Collections.emptySet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.*;

class UnknownEnvironmentConfigTest {
    private static final String AGENT_UUID = "uuid";
    private EnvironmentConfig environmentConfig;
    private CaseInsensitiveString envName;

    @BeforeEach
    void setUp() {
        envName = new CaseInsensitiveString("non-existent-env");
        environmentConfig = new UnknownEnvironmentConfig(envName);
    }

    @Test
    void shouldReturnTrueWhenIsEmpty() {
        assertTrue(environmentConfig.isEnvironmentEmpty());
    }

    @Test
    void shouldReturnFalseThatNotEmptyWhenHasAgent() {
        environmentConfig.addAgent("agent");
        assertThat(environmentConfig.isEnvironmentEmpty()).isFalse();
    }

    @Test
    void shouldReturnNullAsMatcher() {
        EnvironmentPipelineMatcher pipelineMatcher = environmentConfig.createMatcher();
        assertThat(pipelineMatcher).isNull();
    }

    @Test
    void shouldRemoveAgentFromEnvironment() {
        environmentConfig.addAgent("uuid1");
        environmentConfig.addAgent("uuid2");
        assertThat(environmentConfig.getAgents().size()).isEqualTo(2);
        assertThat(environmentConfig.hasAgent("uuid1")).isTrue();
        assertThat(environmentConfig.hasAgent("uuid2")).isTrue();
        environmentConfig.removeAgent("uuid1");
        assertThat(environmentConfig.getAgents().size()).isEqualTo(1);
        assertThat(environmentConfig.hasAgent("uuid1")).isFalse();
        assertThat(environmentConfig.hasAgent("uuid2")).isTrue();
    }

    @Test
    void twoEnvironmentConfigsShouldBeEqualIfNameIsEqual() {
        EnvironmentConfig another = new UnknownEnvironmentConfig(envName);
        assertThat(another).isEqualTo(environmentConfig);
    }

    @Test
    void twoEnvironmentConfigsShouldNotBeEqualIfNameNotEqual() {
        EnvironmentConfig another = new UnknownEnvironmentConfig(new CaseInsensitiveString("other"));
        assertThat(another).isNotEqualTo(environmentConfig);
    }

    @Test
    void shouldNotUpdateAnythingForNullAttributes() {
        EnvironmentConfig beforeUpdate = new Cloner().deepClone(environmentConfig);
        environmentConfig.setConfigAttributes(null);
        assertThat(environmentConfig).isEqualTo(beforeUpdate);
    }

    @Test
    void shouldReturnFalseOnHasSecretParams() {
        assertFalse(environmentConfig.hasSecretParams());
    }

    @Test
    void shouldReturnEmptyConfigErrors() {
        assertThat(environmentConfig.errors()).isEmpty();
    }

    @Test
    void shouldDoNothingOnAddError() {
        assertThat(environmentConfig.errors()).isEmpty();
        environmentConfig.addError("field", "value");
        assertThat(environmentConfig.errors()).isEmpty();
    }

    @Nested
    class HasAgent {
        @Test
        void shouldReturnTrueIfContainsAgent() {
            environmentConfig.addAgent(AGENT_UUID);
            assertTrue(environmentConfig.hasAgent(AGENT_UUID));
        }

        @Test
        void shouldReturnFalseIfDoesNotContainTheAgent() {
            assertFalse(environmentConfig.hasAgent(AGENT_UUID));
        }
    }

    @Nested
    class ValidateContainsOnlyUuids {

        @Test
        void shouldValidateOnEmpty() {
            assertTrue(environmentConfig.validateContainsAgentUUIDsFrom(emptySet()));
        }

        @Test
        void shouldReturnTrueIfTheInputsArePresent() {
            environmentConfig.addAgent(AGENT_UUID);

            HashSet<String> input = new HashSet<>();
            input.add(AGENT_UUID);

            assertTrue(environmentConfig.validateContainsAgentUUIDsFrom(input));
        }

        @Test
        void shouldReturnTrueEvenIfTheInputContainsExtraAgents() {
            HashSet<String> input = new HashSet<>();
            input.add(AGENT_UUID);

            assertTrue(environmentConfig.validateContainsAgentUUIDsFrom(input));
        }

        @Test
        void shouldReturnFalseIfTheConfigContainsExtraAgents() {
            environmentConfig.addAgent(AGENT_UUID);
            environmentConfig.addAgent("uuid1");

            HashSet<String> input = new HashSet<>();
            input.add(AGENT_UUID);

            assertFalse(environmentConfig.validateContainsAgentUUIDsFrom(input));
        }
    }

    @Nested
    class Contains {
        @Test
        void shouldReturnFalseOnContainsPipeline() {
            assertFalse(environmentConfig.contains("any-pipeline-name"));
            assertFalse(environmentConfig.containsPipeline(new CaseInsensitiveString("any-pipeline-name")));
            assertFalse(environmentConfig.containsPipelineRemotely(new CaseInsensitiveString("any-pipeline-name")));
        }

        @Test
        void shouldReturnFalseOnContainsAgents() {
            environmentConfig.addAgent(AGENT_UUID);
            assertFalse(environmentConfig.containsAgentRemotely(AGENT_UUID));
        }

        @Test
        void shouldReturnFalseOnContainsEnvVar() {
            assertFalse(environmentConfig.containsEnvironmentVariableRemotely("var-name"));
        }
    }

    @Nested
    class Add {
        @Test
        void shouldAddAgentToEnvironmentIfNotPresent() {
            environmentConfig.addAgent("uuid");
            environmentConfig.addAgentIfNew("uuid");
            environmentConfig.addAgentIfNew("uuid1");
            assertThat(environmentConfig.getAgents().size()).isEqualTo(2);
            assertThat(environmentConfig.hasAgent("uuid")).isTrue();
            assertThat(environmentConfig.hasAgent("uuid1")).isTrue();
        }

        @Test
        void shouldThrowExceptionOnAddPipeline() {
            assertThatCode(() -> environmentConfig.addPipeline(new CaseInsensitiveString("pipeline-name")))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("Cannot add pipeline to an UnknownEnvironmentConfig!");
        }

        @Test
        void shouldThrowExceptionOnAddEnvVariable() {
            assertThatCode(() -> environmentConfig.addEnvironmentVariable("name", "value"))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("Cannot add an environment variable to an UnknownEnvironmentConfig!");

            assertThatCode(() -> environmentConfig.addEnvironmentVariable(new EnvironmentVariableConfig("name", "value")))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("Cannot add an environment variable to an UnknownEnvironmentConfig!");
        }
    }

    @Test
    void shouldMatchEnvName() {
        assertTrue(environmentConfig.hasName(envName));
    }

    @Test
    void shouldReturnDefaultEnvVarContext() {
        EnvironmentVariableContext envVarContext = environmentConfig.createEnvironmentContext();

        assertEquals(1, envVarContext.getProperties().size());
        assertEquals(environmentConfig.name().toString(), envVarContext.getProperty(GO_ENVIRONMENT_NAME));
    }

    @Test
    void shouldReturnEmptyListOnPipelineNames() {
        assertThat(environmentConfig.getPipelineNames()).isEmpty();
    }

    @Test
    void shouldReturnEmptyListOnGetPipelines() {
        assertThat(environmentConfig.getPipelines()).isEmpty();
    }

    @Test
    void shouldReturnEmptyListOnGetRemotePipelines() {
        assertThat(environmentConfig.getRemotePipelines()).isEmpty();
    }

    @Test
    void shouldReturnEmptyListOnGetEnvVars() {
        assertThat(environmentConfig.getVariables()).isEmpty();
    }

    @Test
    void shouldReturnEmptyListOnGetPlainTextVars() {
        assertThat(environmentConfig.getPlainTextVariables()).isEmpty();
    }

    @Test
    void shouldReturnEmptyListOnGetSecureEnvVars() {
        assertThat(environmentConfig.getSecureVariables()).isEmpty();
    }

    @Test
    void shouldReturnFalseOnHasVariable() {
        assertFalse(environmentConfig.hasVariable("any-var"));
    }

    @Test
    void shouldReturnNullAsLocal() {
        assertThat(environmentConfig.getLocal()).isNull();
    }

    @Test
    void shouldReturnFalseOnIsLocal() {
        assertFalse(environmentConfig.isLocal());
    }

    @Test
    void shouldReturnEmptyListOnGetLocalAgents() {
        environmentConfig.addAgent(AGENT_UUID);
        assertThat(environmentConfig.getLocalAgents()).isEmpty();
    }

    // it is an unknown env config - there should not be any validate call to it
    @Test
    void shouldThrowErrorOnValidate() {
        BasicCruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
        assertThatCode(() -> environmentConfig.validate(forChain(cruiseConfig)))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Cannot validate an UnknownEnvironmentConfig!");

        assertThatCode(() -> environmentConfig.validateTree(forChain(cruiseConfig), cruiseConfig))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Cannot validate an UnknownEnvironmentConfig!");
    }

    @Nested
    class OriginForAgent {
        @Test
        void shouldReturnOriginIfAgentIsPresent() {
            environmentConfig.addAgent(AGENT_UUID);
            Optional<ConfigOrigin> configOrigin = environmentConfig.originForAgent(AGENT_UUID);

            assertTrue(configOrigin.isPresent());
            assertThat(configOrigin.get()).isEqualTo(new UnknownOrigin());
        }

        @Test
        void shouldReturnEmptyOptionalIfAgentNotPresent() {
            assertFalse(environmentConfig.originForAgent(AGENT_UUID).isPresent());
        }
    }

    @Test
    void shouldReturnUnknownOrigin() {
        assertThat(environmentConfig.getOrigin()).isInstanceOf(UnknownOrigin.class);
    }

    @Test
    void shouldReturnTrueOnIsUnknown() {
        assertTrue(environmentConfig.isUnknown());
    }

    @Test
    void shouldThrowErrorOnSetOrigin() {
        assertThatCode(() -> environmentConfig.setOrigins(new UnknownOrigin()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("Cannot set origin on an UnknownEnvironmentConfig!");
    }
}