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
package com.thoughtworks.go.config.domain;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EnvironmentVariablesConfigTest {

    private EnvironmentVariablesConfig environmentVariablesConfig;
    private ValidationContext context = mock(ValidationContext.class);
    private GoCipher goCipher = mock(GoCipher.class);

    @BeforeEach
    void setUp() {
        when(context.getParent()).thenReturn(PipelineConfigMother.createPipelineConfig("some-pipeline", "stage-name", "job-name"));
        when(context.getParentDisplayName()).thenReturn("pipeline");
    }

    @Test
    void shouldPopulateErrorWhenDuplicateEnvironmentVariableNameIsPresent() {
        environmentVariablesConfig = new EnvironmentVariablesConfig();
        EnvironmentVariableConfig one = new EnvironmentVariableConfig("FOO", "BAR");
        EnvironmentVariableConfig two = new EnvironmentVariableConfig("FOO", "bAZ");
        environmentVariablesConfig.add(one);
        environmentVariablesConfig.add(two);

        environmentVariablesConfig.validate(context);

        assertThat(one.errors().isEmpty()).isFalse();
        assertThat(one.errors().firstError()).contains("Environment Variable name 'FOO' is not unique for pipeline 'some-pipeline'");
        assertThat(two.errors().isEmpty()).isFalse();
        assertThat(two.errors().firstError()).contains("Environment Variable name 'FOO' is not unique for pipeline 'some-pipeline'");
    }

    @Test
    void shouldValidateTree() {
        environmentVariablesConfig = new EnvironmentVariablesConfig();
        EnvironmentVariableConfig one = new EnvironmentVariableConfig("FOO", "BAR");
        EnvironmentVariableConfig two = new EnvironmentVariableConfig("FOO", "bAZ");
        EnvironmentVariableConfig three = new EnvironmentVariableConfig("", "bAZ");
        environmentVariablesConfig.add(one);
        environmentVariablesConfig.add(two);
        environmentVariablesConfig.add(three);

        environmentVariablesConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig(new CaseInsensitiveString("p1"), null)));

        assertThat(one.errors().isEmpty()).isFalse();
        assertThat(one.errors().firstError()).contains("Environment Variable name 'FOO' is not unique for pipeline 'p1'");
        assertThat(two.errors().isEmpty()).isFalse();
        assertThat(two.errors().firstError()).contains("Environment Variable name 'FOO' is not unique for pipeline 'p1'");
        assertThat(three.errors().isEmpty()).isFalse();
        assertThat(three.errors().firstError()).contains("Environment Variable cannot have an empty name for pipeline 'p1'.");
    }

    @Test
    void shouldPopulateErrorWhenVariableNameIsEmpty() {
        environmentVariablesConfig = new EnvironmentVariablesConfig();
        EnvironmentVariableConfig one = new EnvironmentVariableConfig("", "BAR");
        environmentVariablesConfig.add(one);

        environmentVariablesConfig.validate(context);

        assertThat(one.errors().isEmpty()).isFalse();
        assertThat(one.errors().on(EnvironmentVariableConfig.NAME)).contains("Environment Variable cannot have an empty name for pipeline 'some-pipeline'.");
    }

    @Test
    void shouldPopulateErrorWhenVariableNameStartsWithSpace() {
        environmentVariablesConfig = new EnvironmentVariablesConfig();
        EnvironmentVariableConfig one = new EnvironmentVariableConfig(" foo", "BAR");
        environmentVariablesConfig.add(one);

        environmentVariablesConfig.validate(context);

        assertThat(one.errors().isEmpty()).isFalse();
        assertThat(one.errors().on(EnvironmentVariableConfig.NAME)).contains("Environment Variable cannot start or end with spaces for pipeline 'some-pipeline'.");
    }

    @Test
    void shouldPopulateErrorWhenVariableNameEndsWithSpace() {
        environmentVariablesConfig = new EnvironmentVariablesConfig();
        EnvironmentVariableConfig one = new EnvironmentVariableConfig("FOO ", "BAR");
        environmentVariablesConfig.add(one);

        environmentVariablesConfig.validate(context);

        assertThat(one.errors().isEmpty()).isFalse();
        assertThat(one.errors().on(EnvironmentVariableConfig.NAME)).contains("Environment Variable cannot start or end with spaces for pipeline 'some-pipeline'.");
    }

    @Test
    void shouldPopulateErrorWhenVariableNameContainsLeadingAndTrailingSpaces() {
        environmentVariablesConfig = new EnvironmentVariablesConfig();
        EnvironmentVariableConfig one = new EnvironmentVariableConfig("     FOO   ", "BAR");
        environmentVariablesConfig.add(one);

        environmentVariablesConfig.validate(context);

        assertThat(one.errors().isEmpty()).isFalse();
        assertThat(one.errors().on(EnvironmentVariableConfig.NAME)).contains("Environment Variable cannot start or end with spaces for pipeline 'some-pipeline'.");
    }

    @Test
    void shouldClearEnvironmentVariablesWhenTheMapIsNull() {
        environmentVariablesConfig = new EnvironmentVariablesConfig();
        EnvironmentVariableConfig one = new EnvironmentVariableConfig("FOO", "BAR");
        environmentVariablesConfig.add(one);

        environmentVariablesConfig.setConfigAttributes(null);

        assertThat(environmentVariablesConfig.size()).isEqualTo(0);
    }

    @Test
    void shouldSetConfigAttributesSecurely() {
        environmentVariablesConfig = new EnvironmentVariablesConfig();
        ArrayList<Map<String, String>> attribs = new ArrayList<>();
        Map<String, String> var1 = new HashMap<>();
        var1.put(EnvironmentVariableConfig.NAME, "name-var1");
        var1.put(EnvironmentVariableConfig.VALUE, "val-var1");
        attribs.add(var1);
        Map<String, String> var2 = new HashMap<>();
        var2.put(EnvironmentVariableConfig.NAME, "name-var2");
        var2.put(EnvironmentVariableConfig.VALUE, "val-var2");
        var2.put(EnvironmentVariableConfig.SECURE, "true");
        var2.put(EnvironmentVariableConfig.ISCHANGED, "true");
        attribs.add(var2);
        assertThat(environmentVariablesConfig.size()).isEqualTo(0);
        environmentVariablesConfig.setConfigAttributes(attribs);
        assertThat(environmentVariablesConfig.size()).isEqualTo(2);
        assertThat(environmentVariablesConfig).contains(new EnvironmentVariableConfig(null, "name-var1", "val-var1", false));
        assertThat(environmentVariablesConfig).contains(new EnvironmentVariableConfig(new GoCipher(), "name-var2", "val-var2", true));
    }

    @Test
    void shouldGetSecureVariables() throws CryptoException {
        EnvironmentVariablesConfig environmentVariablesConfig = new EnvironmentVariablesConfig();
        GoCipher mockGoCipher = mock(GoCipher.class);
        EnvironmentVariableConfig plainVar1 = new EnvironmentVariableConfig("var1", "var1_value");
        EnvironmentVariableConfig var1 = secureVariable(mockGoCipher, "foo1", "bar1", "encryptedBar1");
        EnvironmentVariableConfig var2 = secureVariable(mockGoCipher, "foo2", "bar2", "encryptedBar2");
        EnvironmentVariableConfig var3 = secureVariable(mockGoCipher, "foo3", "bar3", "encryptedBar3");
        environmentVariablesConfig.addAll(Arrays.asList(var1, var2, var3, plainVar1));

        List<EnvironmentVariableConfig> variables = environmentVariablesConfig.getSecureVariables();

        assertThat(variables.size()).isEqualTo(3);
        assertThat(variables).contains(var1, var2, var3);
    }

    @Test
    void shouldGetOnlyPlainTextVariables() throws CryptoException {
        EnvironmentVariablesConfig environmentVariablesConfig = new EnvironmentVariablesConfig();
        EnvironmentVariableConfig plainVar1 = new EnvironmentVariableConfig("var1", "var1_value");
        EnvironmentVariableConfig plainVar2 = new EnvironmentVariableConfig("var2", "var2_value");
        EnvironmentVariableConfig var1 = secureVariable(goCipher, "foo1", "bar1", "encryptedBar1");
        EnvironmentVariableConfig var2 = secureVariable(goCipher, "foo2", "bar2", "encryptedBar2");
        environmentVariablesConfig.addAll(Arrays.asList(var1, var2, plainVar1, plainVar2));

        List<EnvironmentVariableConfig> variables = environmentVariablesConfig.getPlainTextVariables();

        assertThat(variables.size()).isEqualTo(2);
        assertThat(variables).contains(plainVar1, plainVar2);
    }

    @Nested
    class HasSecretParams {

        @Test
        void shouldBeFalseWhenNoneOfTheEnvironmentVariablesIsDefinedAsSecretParam() {
            EnvironmentVariablesConfig environmentVariablesConfig = new EnvironmentVariablesConfig();
            environmentVariablesConfig.add(new EnvironmentVariableConfig("var1", "var_value1"));
            environmentVariablesConfig.add(new EnvironmentVariableConfig("var2", "var_value2"));

            boolean result = environmentVariablesConfig.hasSecretParams();

            assertThat(result).isFalse();
        }

        @Test
        void shouldBeTrueWhenOneOfTheEnvironmentVariableIsDefinedAsSecretParam() {
            EnvironmentVariablesConfig environmentVariablesConfig = new EnvironmentVariablesConfig();
            environmentVariablesConfig.add(new EnvironmentVariableConfig("var1", "var_value1"));
            environmentVariablesConfig.add(new EnvironmentVariableConfig("Token", "{{SECRET:[secret_config_id][token]}}"));

            boolean result = environmentVariablesConfig.hasSecretParams();

            assertThat(result).isTrue();
        }
    }

    @Nested
    class getSecretParams {
        @Test
        void shouldReturnEmptyIfNoneOfTheEnvironmentVariablesIsDefinedAsSecretParam() {
            EnvironmentVariablesConfig environmentVariablesConfig = new EnvironmentVariablesConfig();
            environmentVariablesConfig.add(new EnvironmentVariableConfig("var1", "var_value1"));
            environmentVariablesConfig.add(new EnvironmentVariableConfig("var2", "var_value2"));

            SecretParams secretParams = environmentVariablesConfig.getSecretParams();

            assertThat(secretParams).isEmpty();
        }

        @Test
        void shouldReturnSecretParamsIfTheEnvironmentVariablesIsDefinedAsSecretParam() {
            EnvironmentVariablesConfig environmentVariablesConfig = new EnvironmentVariablesConfig();
            environmentVariablesConfig.add(new EnvironmentVariableConfig("var1", "var_value1"));
            environmentVariablesConfig.add(new EnvironmentVariableConfig("var2", "{{SECRET:[secret_config_id][token]}}"));

            SecretParams secretParams = environmentVariablesConfig.getSecretParams();

            assertThat(secretParams)
                    .hasSize(1)
                    .contains(new SecretParam("secret_config_id", "token"));
        }
    }

    private EnvironmentVariableConfig secureVariable(final GoCipher goCipher, String key, String plainText, String cipherText) throws CryptoException {
        when(goCipher.encrypt(plainText)).thenReturn(cipherText);
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, key, plainText, true);
        return environmentVariableConfig;
    }
}
