/*
 * Copyright 2015 ThoughtWorks, Inc.
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

package com.thoughtworks.go.config.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.security.GoCipher;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.junit.Before;
import org.junit.Test;

import static com.thoughtworks.go.util.TestUtils.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EnvironmentVariablesConfigTest {

    private EnvironmentVariablesConfig environmentVariablesConfig;
    private ValidationContext context = mock(ValidationContext.class);
    private GoCipher goCipher = mock(GoCipher.class);

    @Before
    public void setUp() {
        when(context.getParent()).thenReturn(PipelineConfigMother.createPipelineConfig("some-pipeline", "stage-name", "job-name"));
        when(context.getParentDisplayName()).thenReturn("pipeline");
    }

    @Test
    public void shouldPopulateErrorWhenDuplicateEnvironmentVariableNameIsPresent() {
        environmentVariablesConfig = new EnvironmentVariablesConfig();
        EnvironmentVariableConfig one = new EnvironmentVariableConfig("FOO", "BAR");
        EnvironmentVariableConfig two = new EnvironmentVariableConfig("FOO", "bAZ");
        environmentVariablesConfig.add(one);
        environmentVariablesConfig.add(two);

        environmentVariablesConfig.validate(context);

        assertThat(one.errors().isEmpty(), is(false));
        assertThat(one.errors().firstError(), contains("Environment Variable name 'FOO' is not unique for pipeline 'some-pipeline'"));
        assertThat(two.errors().isEmpty(), is(false));
        assertThat(two.errors().firstError(), contains("Environment Variable name 'FOO' is not unique for pipeline 'some-pipeline'"));
    }

    @Test
    public void shouldValidateTree() {
        environmentVariablesConfig = new EnvironmentVariablesConfig();
        EnvironmentVariableConfig one = new EnvironmentVariableConfig("FOO", "BAR");
        EnvironmentVariableConfig two = new EnvironmentVariableConfig("FOO", "bAZ");
        EnvironmentVariableConfig three = new EnvironmentVariableConfig("", "bAZ");
        environmentVariablesConfig.add(one);
        environmentVariablesConfig.add(two);
        environmentVariablesConfig.add(three);

        environmentVariablesConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, "group", new PipelineConfig(new CaseInsensitiveString("p1"), null)));

        assertThat(one.errors().isEmpty(), is(false));
        assertThat(one.errors().firstError(), contains("Environment Variable name 'FOO' is not unique for pipeline 'p1'"));
        assertThat(two.errors().isEmpty(), is(false));
        assertThat(two.errors().firstError(), contains("Environment Variable name 'FOO' is not unique for pipeline 'p1'"));
        assertThat(three.errors().isEmpty(), is(false));
        assertThat(three.errors().firstError(), contains("Environment Variable cannot have an empty name for pipeline 'p1'."));
    }


    @Test
    public void shouldPopulateErrorWhenVariableNameIsEmpty() {
        environmentVariablesConfig = new EnvironmentVariablesConfig();
        EnvironmentVariableConfig one = new EnvironmentVariableConfig("", "BAR");
        environmentVariablesConfig.add(one);

        environmentVariablesConfig.validate(context);

        assertThat(one.errors().isEmpty(), is(false));
        assertThat(one.errors().on(EnvironmentVariableConfig.NAME), contains("Environment Variable cannot have an empty name for pipeline 'some-pipeline'."));
    }

    @Test
    public void shouldPopulateErrorWhenVariableNameStartsWithSpace() {
        environmentVariablesConfig = new EnvironmentVariablesConfig();
        EnvironmentVariableConfig one = new EnvironmentVariableConfig(" foo", "BAR");
        environmentVariablesConfig.add(one);

        environmentVariablesConfig.validate(context);

        assertThat(one.errors().isEmpty(), is(false));
        assertThat(one.errors().on(EnvironmentVariableConfig.NAME), contains("Environment Variable cannot start or end with spaces for pipeline 'some-pipeline'."));
    }

    @Test
    public void shouldPopulateErrorWhenVariableNameEndsWithSpace() {
        environmentVariablesConfig = new EnvironmentVariablesConfig();
        EnvironmentVariableConfig one = new EnvironmentVariableConfig("FOO ", "BAR");
        environmentVariablesConfig.add(one);

        environmentVariablesConfig.validate(context);

        assertThat(one.errors().isEmpty(), is(false));
        assertThat(one.errors().on(EnvironmentVariableConfig.NAME), contains("Environment Variable cannot start or end with spaces for pipeline 'some-pipeline'."));
    }

    @Test
    public void shouldPopulateErrorWhenVariableNameContainsLeadingAndTrailingSpaces() {
        environmentVariablesConfig = new EnvironmentVariablesConfig();
        EnvironmentVariableConfig one = new EnvironmentVariableConfig("     FOO   ", "BAR");
        environmentVariablesConfig.add(one);

        environmentVariablesConfig.validate(context);

        assertThat(one.errors().isEmpty(), is(false));
        assertThat(one.errors().on(EnvironmentVariableConfig.NAME), contains("Environment Variable cannot start or end with spaces for pipeline 'some-pipeline'."));
    }

    @Test
    public void shouldClearEnvironmentVariablesWhenTheMapIsNull() {
        environmentVariablesConfig = new EnvironmentVariablesConfig();
        EnvironmentVariableConfig one = new EnvironmentVariableConfig("FOO", "BAR");
        environmentVariablesConfig.add(one);

        environmentVariablesConfig.setConfigAttributes(null);

        assertThat(environmentVariablesConfig.size(), is(0));
    }

    @Test
    public void shouldSetConfigAttributesSecurely(){
        environmentVariablesConfig = new EnvironmentVariablesConfig();
        ArrayList<Map<String,String>> attribs = new ArrayList<Map<String, String>>();
        Map<String,String> var1 = new HashMap<String, String>();
        var1.put(EnvironmentVariableConfig.NAME, "name-var1");
        var1.put(EnvironmentVariableConfig.VALUE, "val-var1");
        attribs.add(var1);
        Map<String,String> var2 = new HashMap<String, String>();
        var2.put(EnvironmentVariableConfig.NAME, "name-var2");
        var2.put(EnvironmentVariableConfig.VALUE, "val-var2");
        var2.put(EnvironmentVariableConfig.SECURE, "true");
        var2.put(EnvironmentVariableConfig.ISCHANGED, "true");
        attribs.add(var2);
        assertThat(environmentVariablesConfig.size(), is(0));
        environmentVariablesConfig.setConfigAttributes(attribs);
        assertThat(environmentVariablesConfig.size(), is(2));
        assertThat(environmentVariablesConfig, hasItem(new EnvironmentVariableConfig(null, "name-var1", "val-var1", false)));
        assertThat(environmentVariablesConfig, hasItem(new EnvironmentVariableConfig(new GoCipher(), "name-var2", "val-var2", true)));
    }

    @Test
    public void shouldGetSecureVariables() throws InvalidCipherTextException {
        EnvironmentVariablesConfig environmentVariablesConfig = new EnvironmentVariablesConfig();
        GoCipher mockGoCipher = mock(GoCipher.class);
        EnvironmentVariableConfig plainVar1 = new EnvironmentVariableConfig("var1", "var1_value");
        EnvironmentVariableConfig var1 = secureVariable(mockGoCipher, "foo1", "bar1", "encryptedBar1");
        EnvironmentVariableConfig var2 = secureVariable(mockGoCipher, "foo2", "bar2", "encryptedBar2");
        EnvironmentVariableConfig var3 = secureVariable(mockGoCipher, "foo3", "bar3", "encryptedBar3");
        environmentVariablesConfig.addAll(Arrays.asList(var1, var2, var3, plainVar1));

        List<EnvironmentVariableConfig> variables = environmentVariablesConfig.getSecureVariables();

        assertThat(variables.size(), is(3));
        assertThat(variables, hasItems(var1, var2, var3));
    }

    @Test
    public void shouldGetOnlyPlainTextVariables() throws InvalidCipherTextException {
        EnvironmentVariablesConfig environmentVariablesConfig = new EnvironmentVariablesConfig();
        EnvironmentVariableConfig plainVar1 = new EnvironmentVariableConfig("var1", "var1_value");
        EnvironmentVariableConfig plainVar2 = new EnvironmentVariableConfig("var2", "var2_value");
        EnvironmentVariableConfig var1 = secureVariable(goCipher, "foo1", "bar1", "encryptedBar1");
        EnvironmentVariableConfig var2 = secureVariable(goCipher, "foo2", "bar2", "encryptedBar2");
        environmentVariablesConfig.addAll(Arrays.asList(var1, var2, plainVar1, plainVar2));

        List<EnvironmentVariableConfig> variables = environmentVariablesConfig.getPlainTextVariables();

        assertThat(variables.size(), is(2));
        assertThat(variables, hasItems(plainVar1, plainVar2));
    }


    private EnvironmentVariableConfig secureVariable(final GoCipher goCipher, String key, String plainText, String cipherText) throws InvalidCipherTextException {
        when(goCipher.encrypt(plainText)).thenReturn(cipherText);
        EnvironmentVariableConfig environmentVariableConfig = new EnvironmentVariableConfig(goCipher, key, plainText, true);
        return environmentVariableConfig;
    }
}
