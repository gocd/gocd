/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.domain.EnvironmentPipelineMatcher;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.apache.commons.collections.map.SingletonMap;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public abstract class EnvironmentConfigTestBase {
    public EnvironmentConfig environmentConfig;
    private static final String AGENT_UUID = "uuid";

    @Test
    public void shouldCreateMatcherWhenNoPipelines() throws Exception {
        EnvironmentPipelineMatcher pipelineMatcher = environmentConfig.createMatcher();
        assertThat(pipelineMatcher.match("pipeline", AGENT_UUID), is(false));
    }

    @Test
    public void shouldCreateMatcherWhenPipelinesGiven() throws Exception {
        environmentConfig.addPipeline(new CaseInsensitiveString("pipeline"));
        environmentConfig.addAgent(AGENT_UUID);
        EnvironmentPipelineMatcher pipelineMatcher = environmentConfig.createMatcher();
        assertThat(pipelineMatcher.match("pipeline", AGENT_UUID), is(true));
    }

    @Test
    public void shouldRemoveAgentFromEnvironment() throws Exception {
        environmentConfig.addAgent("uuid1");
        environmentConfig.addAgent("uuid2");
        assertThat(environmentConfig.getAgents().size(), is(2));
        assertThat(environmentConfig.hasAgent("uuid1"), is(true));
        assertThat(environmentConfig.hasAgent("uuid2"), is(true));
        environmentConfig.removeAgent("uuid1");
        assertThat(environmentConfig.getAgents().size(), is(1));
        assertThat(environmentConfig.hasAgent("uuid1"), is(false));
        assertThat(environmentConfig.hasAgent("uuid2"), is(true));
    }

    @Test
    public void shouldAddAgentToEnvironmentIfNotPresent() throws Exception {
        environmentConfig.addAgent("uuid");
        environmentConfig.addAgentIfNew("uuid");
        environmentConfig.addAgentIfNew("uuid1");
        assertThat(environmentConfig.getAgents().size(), is(2));
        assertThat(environmentConfig.hasAgent("uuid"), is(true));
        assertThat(environmentConfig.hasAgent("uuid1"), is(true));
    }

    @Test
    public void twoEnvironmentConfigsShouldBeEqualIfNameIsEqual() throws Exception {
        EnvironmentConfig another = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        assertThat(another, Matchers.is(environmentConfig));
    }

    @Test
    public void twoEnvironmentConfigsShouldNotBeEqualIfnameNotEqual() throws Exception {
        EnvironmentConfig another = new BasicEnvironmentConfig(new CaseInsensitiveString("other"));
        assertThat(another, Matchers.is(not(environmentConfig)));
    }

    @Test
    public void shouldAddEnvironmentVariablesToEnvironmentVariableContext() throws Exception {
        EnvironmentConfig another = new BasicEnvironmentConfig(new CaseInsensitiveString("other"));
        another.addEnvironmentVariable("variable-name", "variable-value");
        EnvironmentVariableContext context = another.createEnvironmentContext();
        assertThat(context.getProperty("variable-name"), is("variable-value"));
    }

    @Test
    public void shouldAddEnvironmentNameToEnvironmentVariableContext() throws Exception {
        EnvironmentConfig another = new BasicEnvironmentConfig(new CaseInsensitiveString("other"));
        EnvironmentVariableContext context = another.createEnvironmentContext();
        assertThat(context.getProperty(EnvironmentVariableContext.GO_ENVIRONMENT_NAME), is("other"));
    }

    @Test
    public void shouldReturnPipelineNamesContainedInIt() throws Exception {
        environmentConfig.addPipeline(new CaseInsensitiveString("deployment"));
        environmentConfig.addPipeline(new CaseInsensitiveString("testing"));
        List<CaseInsensitiveString> pipelineNames = environmentConfig.getPipelineNames();
        assertThat(pipelineNames.size(), is(2));
        assertThat(pipelineNames, hasItem(new CaseInsensitiveString("deployment")));
        assertThat(pipelineNames, hasItem(new CaseInsensitiveString("testing")));
    }

    @Test
    public void shouldUpdatePipelines() {
        environmentConfig.addPipeline(new CaseInsensitiveString("baz"));
        environmentConfig.setConfigAttributes(new SingletonMap(BasicEnvironmentConfig.PIPELINES_FIELD, Arrays.asList(new SingletonMap("name", "foo"), new SingletonMap("name", "bar"))));
        assertThat(environmentConfig.getPipelineNames(), is(Arrays.asList(new CaseInsensitiveString("foo"), new CaseInsensitiveString("bar"))));
    }

    @Test
    public void shouldUpdateAgents() {
        environmentConfig.addAgent("uuid-1");
        environmentConfig.setConfigAttributes(new SingletonMap(BasicEnvironmentConfig.AGENTS_FIELD, Arrays.asList(new SingletonMap("uuid", "uuid-2"), new SingletonMap("uuid", "uuid-3"))));
        EnvironmentAgentsConfig expectedAgents = new EnvironmentAgentsConfig();
        expectedAgents.add(new EnvironmentAgentConfig("uuid-2"));
        expectedAgents.add(new EnvironmentAgentConfig("uuid-3"));
        assertThat(environmentConfig.getAgents(), is(expectedAgents));
    }

    @Test
    public void shouldUpdateEnvironmentVariables() {
        environmentConfig.addEnvironmentVariable("hello", "world");
        environmentConfig.setConfigAttributes(new SingletonMap(BasicEnvironmentConfig.VARIABLES_FIELD, Arrays.asList(envVar("foo", "bar"), envVar("baz", "quux"))));
        assertThat(environmentConfig.getVariables(), hasItem(new EnvironmentVariableConfig("foo", "bar")));
        assertThat(environmentConfig.getVariables(), hasItem(new EnvironmentVariableConfig("baz", "quux")));
        assertThat(environmentConfig.getVariables().size(), is(2));
    }

    @Test
    public void shouldNotSetEnvironmentVariableFromConfigAttributesIfNameAndValueIsEmpty() {
        environmentConfig.setConfigAttributes(new SingletonMap(BasicEnvironmentConfig.VARIABLES_FIELD, Arrays.asList(envVar("", "anything"), envVar("", ""))));
        assertThat(environmentConfig.errors().isEmpty(), is(true));
        assertThat(environmentConfig.getVariables(), hasItem(new EnvironmentVariableConfig("", "anything")));
        assertThat(environmentConfig.getVariables().size(), is(1));
    }

    @Test
    public void shouldNotUpdateAnythingForNullAttributes() {
        EnvironmentConfig beforeUpdate = new Cloner().deepClone(environmentConfig);
        environmentConfig.setConfigAttributes(null);
        assertThat(environmentConfig, is(beforeUpdate));
    }

    protected static Map<String, String> envVar(String name, String value) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(EnvironmentVariableConfig.NAME, name);
        map.put(EnvironmentVariableConfig.VALUE, value);
        return map;
    }
}