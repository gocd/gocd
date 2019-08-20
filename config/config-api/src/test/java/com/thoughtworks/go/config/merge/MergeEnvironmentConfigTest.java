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
package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static com.thoughtworks.go.util.command.EnvironmentVariableContext.GO_ENVIRONMENT_NAME;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.*;

class MergeEnvironmentConfigTest extends EnvironmentConfigTestBase {
    private MergeEnvironmentConfig singleEnvironmentConfig;
    private MergeEnvironmentConfig pairEnvironmentConfig;
    private EnvironmentConfig localUatEnv1;
    private EnvironmentConfig uatLocalPart2;
    private BasicEnvironmentConfig uatRemotePart;

    @BeforeEach
    void setUp() {
        localUatEnv1 = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        localUatEnv1.setOrigins(new FileConfigOrigin());

        uatLocalPart2 = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatLocalPart2.setOrigins(new FileConfigOrigin());
        uatRemotePart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatRemotePart.setOrigins(new RepoConfigOrigin());
        pairEnvironmentConfig = new MergeEnvironmentConfig(uatLocalPart2, uatRemotePart);
        singleEnvironmentConfig = new MergeEnvironmentConfig(localUatEnv1);

        super.environmentConfig = pairEnvironmentConfig;
    }

    @Test
    void shouldNotAllowPartsWithDifferentNames() {
        assertThatCode(() -> {
            BasicEnvironmentConfig part1WithEnvName1 = new BasicEnvironmentConfig(new CaseInsensitiveString("envName1"));
            BasicEnvironmentConfig part2WithEnvName2 = new BasicEnvironmentConfig(new CaseInsensitiveString("envName2"));
            new MergeEnvironmentConfig(part1WithEnvName1, part2WithEnvName2);
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ShouldContainSameNameAsOfPartialEnvironments() {
        BasicEnvironmentConfig local = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        BasicEnvironmentConfig remote = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        MergeEnvironmentConfig mergeEnv = new MergeEnvironmentConfig(local, remote);

        assertThat(mergeEnv.name()).isEqualTo(local.name());
    }

    @Nested
    class GetRemotePipelines {
        @Test
        void shouldReturnEmptyWhenOnlyLocalPartHasPipelines() {
            uatLocalPart2.addPipeline(new CaseInsensitiveString("pipe"));
            assertThat(pairEnvironmentConfig.getRemotePipelines().isEmpty()).isTrue();
        }

        @Test
        void shouldReturnPipelinesFromRemotePartWhenRemoteHasPipesAssigned() {
            uatRemotePart.addPipeline(new CaseInsensitiveString("pipe"));
            assertThat(environmentConfig.getRemotePipelines().isEmpty()).isFalse();
        }
    }

    @Test
    void shouldReturnFalseThatLocal() {
        assertThat(environmentConfig.isLocal()).isFalse();
    }

    @Test
    void shouldGetLocalPartWhenOriginFile() {
        assertThat(environmentConfig.getLocal()).isEqualTo(uatLocalPart2);
    }

    @Nested
    class hasSamePipelinesAs {
        @Test
        void shouldReturnTrueWhenAnyPipelineNameIsEqualToOther() {
            pairEnvironmentConfig.get(0).addPipeline(new CaseInsensitiveString("pipe1"));
            pairEnvironmentConfig.get(1).addPipeline(new CaseInsensitiveString("pipe2"));
            BasicEnvironmentConfig config = new BasicEnvironmentConfig();
            config.addPipeline(new CaseInsensitiveString("pipe2"));
            assertThat(pairEnvironmentConfig.hasSamePipelinesAs(config)).isTrue();
        }

        @Test
        void shouldReturnFalseWhenNoneOfOtherPipelinesIsEqualToOther() {
            pairEnvironmentConfig.get(0).addPipeline(new CaseInsensitiveString("pipe1"));
            pairEnvironmentConfig.get(1).addPipeline(new CaseInsensitiveString("pipe2"));
            BasicEnvironmentConfig config = new BasicEnvironmentConfig();
            config.addPipeline(new CaseInsensitiveString("pipe3"));
            assertThat(pairEnvironmentConfig.hasSamePipelinesAs(config)).isFalse();
        }
    }

    @Nested
    class getPipelineNames {
        @Test
        void shouldReturnPipelineNamesFrom2Parts() {
            pairEnvironmentConfig.get(0).addPipeline(new CaseInsensitiveString("deployment"));
            pairEnvironmentConfig.get(1).addPipeline(new CaseInsensitiveString("testing"));

            List<CaseInsensitiveString> pipelineNames = pairEnvironmentConfig.getPipelineNames();

            assertThat(pipelineNames).hasSize(2)
                    .contains(new CaseInsensitiveString("deployment"),
                            new CaseInsensitiveString("testing"));
        }

        @Test
        void shouldNotRepeatPipelineNamesFrom2Parts() {
            pairEnvironmentConfig.get(0).addPipeline(new CaseInsensitiveString("deployment"));
            pairEnvironmentConfig.get(1).addPipeline(new CaseInsensitiveString("deployment"));

            List<CaseInsensitiveString> pipelineNames = pairEnvironmentConfig.getPipelineNames();

            assertThat(pipelineNames).contains(new CaseInsensitiveString("deployment"));
        }

        @Test
        void shouldDeduplicateRepeatedPipelinesFrom2Parts() {
            pairEnvironmentConfig.get(0).addPipeline(new CaseInsensitiveString("deployment"));
            pairEnvironmentConfig.get(1).addPipeline(new CaseInsensitiveString("deployment"));

            List<CaseInsensitiveString> pipelineNames = pairEnvironmentConfig.getPipelineNames();

            assertThat(pipelineNames).hasSize(1);
            assertThat(pairEnvironmentConfig.containsPipeline(new CaseInsensitiveString("deployment"))).isTrue();
        }
    }

    @Nested
    class getAgents {
        @Test
        void shouldHaveAgentsFrom2Parts() {
            pairEnvironmentConfig.get(0).addAgent("123");
            pairEnvironmentConfig.get(1).addAgent("345");

            assertThat(pairEnvironmentConfig.hasAgent("123")).isTrue();
            assertThat(pairEnvironmentConfig.hasAgent("345")).isTrue();
        }

        @Test
        void shouldReturnAgentsUuidsFrom2Parts() {
            pairEnvironmentConfig.get(0).addAgent("123");
            pairEnvironmentConfig.get(1).addAgent("345");

            EnvironmentAgentsConfig agents = pairEnvironmentConfig.getAgents();

            assertThat(agents).hasSize(2);
            assertThat(agents.getUuids()).contains("123", "345");
        }

        @Test
        void shouldDeduplicateRepeatedAgentsFrom2Parts() {
            pairEnvironmentConfig.get(0).addAgent("123");
            pairEnvironmentConfig.get(1).addAgent("123");
            EnvironmentAgentsConfig agents = pairEnvironmentConfig.getAgents();
            assertThat(agents).hasSize(1);
            assertThat(agents.getUuids()).contains("123");
        }
    }

    @Test
    void shouldHaveVariablesFrom2Parts() {
        pairEnvironmentConfig.get(0).addEnvironmentVariable("variable-name1", "variable-value1");
        pairEnvironmentConfig.get(1).addEnvironmentVariable("variable-name2", "variable-value2");

        assertThat(pairEnvironmentConfig.hasVariable("variable-name1")).isTrue();
        assertThat(pairEnvironmentConfig.hasVariable("variable-name2")).isTrue();
    }

    @Test
    void shouldAddEnvironmentVariablesToEnvironmentVariableContextFrom2Parts() {
        pairEnvironmentConfig.get(0).addEnvironmentVariable("variable-name1", "variable-value1");
        pairEnvironmentConfig.get(1).addEnvironmentVariable("variable-name2", "variable-value2");

        EnvironmentVariableContext context = pairEnvironmentConfig.createEnvironmentContext();
        assertThat(context.getProperty("variable-name1")).isEqualTo("variable-value1");
        assertThat(context.getProperty("variable-name2")).isEqualTo("variable-value2");
    }

    @Test
    void shouldAddDeduplicatedEnvironmentVariablesToEnvironmentVariableContextFrom2Parts() {
        pairEnvironmentConfig.get(0).addEnvironmentVariable("variable-name1", "variable-value1");
        pairEnvironmentConfig.get(1).addEnvironmentVariable("variable-name1", "variable-value1");

        assertThat(pairEnvironmentConfig.getVariables().size()).isEqualTo(1);

        EnvironmentVariableContext context = pairEnvironmentConfig.createEnvironmentContext();
        assertThat(context.getProperty("variable-name1")).isEqualTo("variable-value1");
    }

    @Test
    void shouldCreateErrorsForInconsistentEnvironmentVariables() {
        pairEnvironmentConfig.get(0).addEnvironmentVariable("variable-name1", "variable-value1");
        pairEnvironmentConfig.get(1).addEnvironmentVariable("variable-name1", "variable-value2");
        pairEnvironmentConfig.validate(ConfigSaveValidationContext.forChain(pairEnvironmentConfig));
        assertThat(pairEnvironmentConfig.errors().isEmpty()).isFalse();
        assertThat(pairEnvironmentConfig.errors().on(MergeEnvironmentConfig.CONSISTENT_KV)).isEqualTo("Environment variable 'variable-name1' is defined more than once with different values");
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
        assertTrue(pairEnvironmentConfig.errors().isEmpty());

        pairEnvironmentConfig.addError("field-name", "some error message.");

        assertThat(pairEnvironmentConfig.errors().size()).isEqualTo(1);
        assertThat(pairEnvironmentConfig.errors().on("field-name")).isEqualTo("some error message.");
    }

    @Nested
    class validate {
        @Test
        void shouldValidateDuplicatePipelines() {
            pairEnvironmentConfig.get(0).addPipeline(new CaseInsensitiveString("up42"));
            pairEnvironmentConfig.get(1).addPipeline(new CaseInsensitiveString("up42"));

            pairEnvironmentConfig.validate(ConfigSaveValidationContext.forChain(pairEnvironmentConfig));

            assertThat(pairEnvironmentConfig.errors().isEmpty()).isFalse();

            assertThat(pairEnvironmentConfig.errors().firstError()).isEqualTo("Environment pipeline 'up42' is defined more than once.");
        }

        @Test
        void shouldValidateDuplicateAgents() {
            pairEnvironmentConfig.get(0).addAgent("random-uuid");
            pairEnvironmentConfig.get(1).addAgent("random-uuid");

            pairEnvironmentConfig.validate(ConfigSaveValidationContext.forChain(pairEnvironmentConfig));

            assertThat(pairEnvironmentConfig.errors().isEmpty()).isFalse();

            assertThat(pairEnvironmentConfig.errors().firstError()).isEqualTo("Environment agent 'random-uuid' is defined more than once.");
        }
    }

    @Test
    void shouldReturnTrueWhenOnlyPartIsLocal() {
        BasicEnvironmentConfig uatLocalPart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatLocalPart.setOrigins(new FileConfigOrigin());
        environmentConfig = new MergeEnvironmentConfig(uatLocalPart);
        assertThat(environmentConfig.isLocal()).isTrue();
    }

    @Test
    void shouldReturnFalseWhenPartIsRemote() {
        BasicEnvironmentConfig uatLocalPart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatLocalPart.setOrigins(new FileConfigOrigin());
        BasicEnvironmentConfig uatRemotePart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatRemotePart.setOrigins(new RepoConfigOrigin());
        environmentConfig = new MergeEnvironmentConfig(uatLocalPart, uatRemotePart);
        assertThat(environmentConfig.isLocal()).isFalse();
    }

    @Test
    void shouldUpdateEnvironmentVariablesWhenSourceIsEditable() {
        BasicEnvironmentConfig uatLocalPart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatLocalPart.setOrigins(new FileConfigOrigin());
        BasicEnvironmentConfig uatRemotePart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatRemotePart.setOrigins(new RepoConfigOrigin());

        uatLocalPart.addEnvironmentVariable("hello", "world");
        environmentConfig = new MergeEnvironmentConfig(uatLocalPart, uatRemotePart);
        environmentConfig.setConfigAttributes(Collections.singletonMap(BasicEnvironmentConfig.VARIABLES_FIELD,
                asList(envVar("foo", "bar"), envVar("baz", "quux"), envVar("hello", "you"))));

        assertThat(environmentConfig.getVariables()).contains(new EnvironmentVariableConfig("hello", "you"));
        assertThat(environmentConfig.getVariables()).contains(new EnvironmentVariableConfig("foo", "bar"));
        assertThat(environmentConfig.getVariables()).contains(new EnvironmentVariableConfig("baz", "quux"));
        assertThat(environmentConfig.getVariables().size()).isEqualTo(3);

        assertThat(uatLocalPart.getVariables()).as("ChangesShouldBeInLocalConfig").contains(new EnvironmentVariableConfig("hello", "you"));
        assertThat(uatLocalPart.getVariables()).as("ChangesShouldBeInLocalConfig").contains(new EnvironmentVariableConfig("foo", "bar"));
        assertThat(uatLocalPart.getVariables()).as("ChangesShouldBeInLocalConfig").contains(new EnvironmentVariableConfig("baz", "quux"));
        assertThat(uatLocalPart.getVariables().size()).as("ChangesShouldBeInLocalConfig").isEqualTo(3);
    }

    @Test
    void shouldReturnCorrectOriginOfDefinedPipeline() {
        BasicEnvironmentConfig uatLocalPart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatLocalPart.setOrigins(new FileConfigOrigin());
        String localPipeline = "local-pipeline";
        uatLocalPart.addPipeline(new CaseInsensitiveString(localPipeline));
        BasicEnvironmentConfig uatRemotePart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatRemotePart.setOrigins(new RepoConfigOrigin());
        String remotePipeline = "remote-pipeline";
        uatRemotePart.addPipeline(new CaseInsensitiveString(remotePipeline));
        MergeEnvironmentConfig environmentConfig = new MergeEnvironmentConfig(uatLocalPart, uatRemotePart);

        assertThat(environmentConfig.getOriginForPipeline(new CaseInsensitiveString(localPipeline))).isEqualTo(new FileConfigOrigin());
        assertThat(environmentConfig.getOriginForPipeline(new CaseInsensitiveString(remotePipeline))).isEqualTo(new RepoConfigOrigin());
    }

    @Test
    void shouldReturnCorrectOriginOfDefinedAgent() {
        BasicEnvironmentConfig uatLocalPart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatLocalPart.setOrigins(new FileConfigOrigin());
        String localAgent = "local-agent";
        uatLocalPart.addAgent(localAgent);
        BasicEnvironmentConfig uatRemotePart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatRemotePart.setOrigins(new RepoConfigOrigin());
        String remoteAgent = "remote-agent";
        uatRemotePart.addAgent(remoteAgent);
        MergeEnvironmentConfig environmentConfig = new MergeEnvironmentConfig(uatLocalPart, uatRemotePart);

        assertThat(environmentConfig.getOriginForAgent(localAgent)).isEqualTo(new FileConfigOrigin());
        assertThat(environmentConfig.getOriginForAgent(remoteAgent)).isEqualTo(new RepoConfigOrigin());
    }

    @Nested
    class GetFirstEditablePart {
        @Test
        void shouldReturnFirstEditablePart() {
            assertNotNull(singleEnvironmentConfig.getFirstEditablePartOrNull());
        }

        @Test
        void shouldReturnNullAsFirstEditablePart() {
            BasicEnvironmentConfig basicEnvironmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("repo"));
            basicEnvironmentConfig.setOrigins(new RepoConfigOrigin());

            assertNull(new MergeEnvironmentConfig(basicEnvironmentConfig).getFirstEditablePartOrNull());
        }

        @Test
        void shouldThrowExceptionIfNoEditablePartExists() {
            BasicEnvironmentConfig basicEnvironmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("repo"));
            basicEnvironmentConfig.setOrigins(new RepoConfigOrigin());

            MergeEnvironmentConfig mergeEnvironmentConfig = new MergeEnvironmentConfig(basicEnvironmentConfig);
            assertThatCode(mergeEnvironmentConfig::getFirstEditablePart)
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("No editable configuration part");
        }

        @Test
        void shouldNotThrowExceptionIfEditablePartExists() {
            assertThatCode(singleEnvironmentConfig::getFirstEditablePart).doesNotThrowAnyException();
        }

        @Test
        void shouldNotThrowExceptionIfNoEditablePartExistsAndShouldReturnNull() {
            BasicEnvironmentConfig basicEnvironmentConfig = new BasicEnvironmentConfig(new CaseInsensitiveString("repo"));
            basicEnvironmentConfig.setOrigins(new RepoConfigOrigin());

            MergeEnvironmentConfig mergeEnvironmentConfig = new MergeEnvironmentConfig(basicEnvironmentConfig);

            assertThatCode(() -> {
                EnvironmentConfig environmentConfig = mergeEnvironmentConfig.getFirstEditablePartWithoutThrowingException();
                assertNull(environmentConfig);
            }).doesNotThrowAnyException();
        }
    }

    @Test
    void shouldReturnMatchersWithTheProperties() {
        singleEnvironmentConfig.addPipeline(new CaseInsensitiveString("pipeline-1"));
        singleEnvironmentConfig.addAgent("agent-1");

        EnvironmentPipelineMatcher matcher = singleEnvironmentConfig.createMatcher();

        assertNotNull(matcher);
        assertThat(matcher.name()).isEqualTo(singleEnvironmentConfig.name());
        assertTrue(matcher.hasPipeline("pipeline-1"));
        assertTrue(matcher.match("pipeline-1", "agent-1"));

        assertFalse(matcher.hasPipeline("non-existent-pipeline"));
    }

    @Test
    void shouldNotThrowExceptionIfAllThePipelinesArePresent() {
        CaseInsensitiveString p1 = new CaseInsensitiveString("pipeline-1");
        CaseInsensitiveString p2 = new CaseInsensitiveString("pipeline-2");

        singleEnvironmentConfig.addPipeline(p1);
        singleEnvironmentConfig.addPipeline(p2);

        assertThatCode(() -> singleEnvironmentConfig.validateContainsOnlyPipelines(asList(p1, p2)))
                .doesNotThrowAnyException();
    }

    @Test
    void shouldThrowExceptionIfOneOfThePipelinesAreNotPassed() {
        CaseInsensitiveString p1 = new CaseInsensitiveString("pipeline-1");
        CaseInsensitiveString p2 = new CaseInsensitiveString("pipeline-2");
        CaseInsensitiveString p3 = new CaseInsensitiveString("pipeline-3");

        singleEnvironmentConfig.addPipeline(p1);
        singleEnvironmentConfig.addPipeline(p2);

        assertThatCode(() -> singleEnvironmentConfig.validateContainsOnlyPipelines(asList(p1, p3)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Environment 'UAT' refers to an unknown pipeline 'pipeline-2'.");
    }

    @Test
    void shouldReturnTrueIsChildConfigContainsNoPipelineAgentsAndVariables() {
        assertTrue(singleEnvironmentConfig.isEnvironmentEmpty());
    }

    @Test
    void shouldReturnFalseIfNotEmpty() {
        singleEnvironmentConfig.addPipeline(new CaseInsensitiveString("pipeline1"));
        assertFalse(singleEnvironmentConfig.isEnvironmentEmpty());
    }
}
