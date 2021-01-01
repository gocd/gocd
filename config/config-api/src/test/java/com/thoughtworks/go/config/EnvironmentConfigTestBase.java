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
package com.thoughtworks.go.config;

import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.helper.GoConfigMother;
import com.thoughtworks.go.util.ClonerFactory;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public abstract class EnvironmentConfigTestBase {
    public EnvironmentConfig environmentConfig;
    private static final String AGENT_UUID = "uuid";

    @Test
    void shouldReturnTrueWhenIsEmpty() {
        assertThat(environmentConfig.isEnvironmentEmpty()).isTrue();
    }

    @Test
    void shouldReturnFalseThatNotEmptyWhenHasPipeline() {
        environmentConfig.addPipeline(new CaseInsensitiveString("pipe"));
        assertThat(environmentConfig.isEnvironmentEmpty()).isFalse();
    }

    @Test
    void shouldReturnFalseThatNotEmptyWhenHasAgent() {
        environmentConfig.addAgent("agent");
        assertThat(environmentConfig.isEnvironmentEmpty()).isFalse();
    }

    @Test
    void shouldReturnFalseThatNotEmptyWhenHasVariable() {
        environmentConfig.addEnvironmentVariable("k", "v");
        assertThat(environmentConfig.isEnvironmentEmpty()).isFalse();
    }

    @Test
    void shouldCreateMatcherWhenNoPipelines() {
        EnvironmentPipelineMatcher pipelineMatcher = environmentConfig.createMatcher();
        assertThat(pipelineMatcher.match("pipeline", AGENT_UUID)).isFalse();
    }

    @Test
    void shouldCreateMatcherWhenPipelinesGiven() {
        environmentConfig.addPipeline(new CaseInsensitiveString("pipeline"));
        environmentConfig.addAgent(AGENT_UUID);
        EnvironmentPipelineMatcher pipelineMatcher = environmentConfig.createMatcher();
        assertThat(pipelineMatcher.match("pipeline", AGENT_UUID)).isTrue();
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
    void shouldAddAgentToEnvironmentIfNotPresent() {
        environmentConfig.addAgent("uuid");
        environmentConfig.addAgentIfNew("uuid");
        environmentConfig.addAgentIfNew("uuid1");
        assertThat(environmentConfig.getAgents().size()).isEqualTo(2);
        assertThat(environmentConfig.hasAgent("uuid")).isTrue();
        assertThat(environmentConfig.hasAgent("uuid1")).isTrue();
    }

    @Test
    void twoEnvironmentConfigsShouldBeEqualIfNameIsEqual() {
        EnvironmentConfig another = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        assertThat(another).isEqualTo(environmentConfig);
    }

    @Test
    void twoEnvironmentConfigsShouldNotBeEqualIfnameNotEqual() {
        EnvironmentConfig another = new BasicEnvironmentConfig(new CaseInsensitiveString("other"));
        assertThat(another).isNotEqualTo(environmentConfig);
    }

    @Test
    void shouldAddEnvironmentVariablesToEnvironmentVariableContext() {
        EnvironmentConfig another = new BasicEnvironmentConfig(new CaseInsensitiveString("other"));
        another.addEnvironmentVariable("variable-name", "variable-value");
        EnvironmentVariableContext context = another.createEnvironmentContext();
        assertThat(context.getProperty("variable-name")).isEqualTo("variable-value");
    }

    @Test
    void shouldAddEnvironmentNameToEnvironmentVariableContext() {
        EnvironmentConfig another = new BasicEnvironmentConfig(new CaseInsensitiveString("other"));
        EnvironmentVariableContext context = another.createEnvironmentContext();
        assertThat(context.getProperty(EnvironmentVariableContext.GO_ENVIRONMENT_NAME)).isEqualTo("other");
    }

    @Test
    void shouldReturnPipelineNamesContainedInIt() {
        environmentConfig.addPipeline(new CaseInsensitiveString("deployment"));
        environmentConfig.addPipeline(new CaseInsensitiveString("testing"));
        List<CaseInsensitiveString> pipelineNames = environmentConfig.getPipelineNames();
        assertThat(pipelineNames.size()).isEqualTo(2);
        assertThat(pipelineNames).contains(new CaseInsensitiveString("deployment"));
        assertThat(pipelineNames).contains(new CaseInsensitiveString("testing"));
    }

    @Test
    void shouldUpdatePipelines() {
        environmentConfig.addPipeline(new CaseInsensitiveString("baz"));
        environmentConfig.setConfigAttributes(Collections.singletonMap(BasicEnvironmentConfig.PIPELINES_FIELD, Arrays.asList(Collections.singletonMap("name", "foo"), Collections.singletonMap("name", "bar"))));
        assertThat(environmentConfig.getPipelineNames()).isEqualTo(Arrays.asList(new CaseInsensitiveString("foo"), new CaseInsensitiveString("bar")));
    }

    @Test
    void shouldUpdateAgents() {
        environmentConfig.addAgent("uuid-1");
        environmentConfig.setConfigAttributes(Collections.singletonMap(BasicEnvironmentConfig.AGENTS_FIELD, Arrays.asList(Collections.singletonMap("uuid", "uuid-2"), Collections.singletonMap("uuid", "uuid-3"))));
        EnvironmentAgentsConfig expectedAgents = new EnvironmentAgentsConfig();
        expectedAgents.add(new EnvironmentAgentConfig("uuid-2"));
        expectedAgents.add(new EnvironmentAgentConfig("uuid-3"));
        assertThat(environmentConfig.getAgents()).isEqualTo(expectedAgents);
    }

    @Test
    void shouldUpdateEnvironmentVariables() {
        environmentConfig.addEnvironmentVariable("hello", "world");
        environmentConfig.setConfigAttributes(Collections.singletonMap(BasicEnvironmentConfig.VARIABLES_FIELD, Arrays.asList(envVar("foo", "bar"), envVar("baz", "quux"))));
        assertThat(environmentConfig.getVariables()).contains(new EnvironmentVariableConfig("foo", "bar"));
        assertThat(environmentConfig.getVariables()).contains(new EnvironmentVariableConfig("baz", "quux"));
        assertThat(environmentConfig.getVariables().size()).isEqualTo(2);
    }

    @Test
    void shouldNotSetEnvironmentVariableFromConfigAttributesIfNameAndValueIsEmpty() {
        environmentConfig.setConfigAttributes(Collections.singletonMap(BasicEnvironmentConfig.VARIABLES_FIELD, Arrays.asList(envVar("", "anything"), envVar("", ""))));
        assertThat(environmentConfig.errors().isEmpty()).isTrue();
        assertThat(environmentConfig.getVariables()).contains(new EnvironmentVariableConfig("", "anything"));
        assertThat(environmentConfig.getVariables().size()).isEqualTo(1);
    }

    @Test
    void shouldNotUpdateAnythingForNullAttributes() {
        EnvironmentConfig beforeUpdate = ClonerFactory.instance().deepClone(environmentConfig);
        environmentConfig.setConfigAttributes(null);
        assertThat(environmentConfig).isEqualTo(beforeUpdate);
    }

    @Nested
    class validateTree {
        @Test
        void shouldCallValidate() {
            EnvironmentConfig spy = spy(environmentConfig);
            ConfigSaveValidationContext validationContext = mock(ConfigSaveValidationContext.class);
            CruiseConfig cruiseConfig = GoConfigMother.defaultCruiseConfig();
            when(validationContext.getCruiseConfig()).thenReturn(cruiseConfig);

            spy.validateTree(validationContext, cruiseConfig);

            verify(spy).validate(validationContext);
        }
    }


    @Nested
    class HasSecretParams {
        @Test
        void shouldReturnTrueIfEnvironmentConfigHasEnvironmentVariablesDefinedUsingSecretParams() {
            environmentConfig.addEnvironmentVariable("var1", "var_value1");
            environmentConfig.addEnvironmentVariable("var2", "{{SECRET:[secret_config_id][token]}}");

            boolean result = environmentConfig.hasSecretParams();

            assertThat(result).isTrue();
        }

        @Test
        void shouldReturnFalseIfEnvironmentConfigHasNoneOfTheEnvironmentVariablesDefinedUsingSecretParams() {
            environmentConfig.addEnvironmentVariable("var1", "var_value1");
            environmentConfig.addEnvironmentVariable("var2", "var_value2");

            boolean result = environmentConfig.hasSecretParams();

            assertThat(result).isFalse();
        }
    }

    @Nested
    class getSecretParams {
        @Test
        void shouldReturnEmptyIfEnvironmentConfigHasNoneOfTheEnvironmentVariablesDefinedUsingSecretParams() {
            environmentConfig.addEnvironmentVariable("var1", "var_value1");
            environmentConfig.addEnvironmentVariable("var2", "var_value2");

            SecretParams secretParams = environmentConfig.getSecretParams();

            assertThat(secretParams).isEmpty();
        }

        @Test
        void shouldReturnSecretParamsIfEnvironmentConfigHasOneOfTheEnvironmentVariablesDefinedUsingSecretParams() {
            environmentConfig.addEnvironmentVariable("var1", "var_value1");
            environmentConfig.addEnvironmentVariable("var2", "{{SECRET:[secret_config_id][token]}}");

            SecretParams secretParams = environmentConfig.getSecretParams();

            assertThat(secretParams)
                    .hasSize(1)
                    .contains(new SecretParam("secret_config_id", "token"));
        }
    }

    protected static Map<String, String> envVar(String name, String value) {
        Map<String, String> map = new HashMap<>();
        map.put(EnvironmentVariableConfig.NAME, name);
        map.put(EnvironmentVariableConfig.VALUE, value);
        return map;
    }
}
