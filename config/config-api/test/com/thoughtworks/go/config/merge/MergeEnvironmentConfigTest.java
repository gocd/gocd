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
package com.thoughtworks.go.config.merge;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.remote.FileConfigOrigin;
import com.thoughtworks.go.config.remote.RepoConfigOrigin;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.apache.commons.collections.map.SingletonMap;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MergeEnvironmentConfigTest extends EnvironmentConfigTestBase {
    public MergeEnvironmentConfig singleEnvironmentConfig;
    public MergeEnvironmentConfig pairEnvironmentConfig;
    private static final String AGENT_UUID = "uuid";

    @Before
    public void setUp() throws Exception {
        BasicEnvironmentConfig localUatEnv = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        localUatEnv.setOrigins(new FileConfigOrigin());

        singleEnvironmentConfig = new MergeEnvironmentConfig(localUatEnv);
        BasicEnvironmentConfig uatLocalPart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatLocalPart.setOrigins(new FileConfigOrigin());
        BasicEnvironmentConfig uatRemotePart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatRemotePart.setOrigins(new RepoConfigOrigin());
        pairEnvironmentConfig = new MergeEnvironmentConfig(
                uatLocalPart,
                uatRemotePart);

        super.environmentConfig = pairEnvironmentConfig;
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowPartsWithDifferentNames()
    {
        new MergeEnvironmentConfig(new BasicEnvironmentConfig(new CaseInsensitiveString("UAT")),
                new BasicEnvironmentConfig(new CaseInsensitiveString("Two")));
    }


    @Test
    public void shouldFailUpdateName() {
        try {
            environmentConfig.setConfigAttributes(new SingletonMap(EnvironmentConfig.NAME_FIELD, "PROD"));
        }
        catch (RuntimeException ex)
        {
            assertThat(ex.getMessage(),is("Cannot update name of environment defined in multiple sources"));
            return;
        }
        fail("should have thrown");
    }

    // merges

    @Test
    public void shouldReturnPipelineNamesFrom2Parts() throws Exception {
        pairEnvironmentConfig.get(0).addPipeline(new CaseInsensitiveString("deployment"));
        pairEnvironmentConfig.get(1).addPipeline(new CaseInsensitiveString("testing"));
        List<CaseInsensitiveString> pipelineNames = pairEnvironmentConfig.getPipelineNames();
        assertThat(pipelineNames.size(), is(2));
        assertThat(pipelineNames, hasItem(new CaseInsensitiveString("deployment")));
        assertThat(pipelineNames, hasItem(new CaseInsensitiveString("testing")));
    }

    @Test
    public void shouldNotRepeatPipelineNamesFrom2Parts() throws Exception {
        pairEnvironmentConfig.get(0).addPipeline(new CaseInsensitiveString("deployment"));
        pairEnvironmentConfig.get(1).addPipeline(new CaseInsensitiveString("deployment"));
        List<CaseInsensitiveString> pipelineNames = pairEnvironmentConfig.getPipelineNames();
        assertThat(pipelineNames, hasItem(new CaseInsensitiveString("deployment")));
    }

    @Test
    public void shouldDeduplicateRepeatedPipelinesFrom2Parts() throws Exception {
        pairEnvironmentConfig.get(0).addPipeline(new CaseInsensitiveString("deployment"));
        pairEnvironmentConfig.get(1).addPipeline(new CaseInsensitiveString("deployment"));
        List<CaseInsensitiveString> pipelineNames = pairEnvironmentConfig.getPipelineNames();
        assertThat(pipelineNames.size(), is(1));
        assertTrue(pairEnvironmentConfig.containsPipeline(new CaseInsensitiveString("deployment")));
    }

    @Test
    public void shouldHaveAgentsFrom2Parts() throws Exception {
        pairEnvironmentConfig.get(0).addAgent("123");
        pairEnvironmentConfig.get(1).addAgent("345");
        EnvironmentAgentsConfig agents = pairEnvironmentConfig.getAgents();

        assertTrue(pairEnvironmentConfig.hasAgent("123"));
        assertTrue(pairEnvironmentConfig.hasAgent("345"));
    }
    @Test
    public void shouldReturnAgentsUuidsFrom2Parts() throws Exception {
        pairEnvironmentConfig.get(0).addAgent("123");
        pairEnvironmentConfig.get(1).addAgent("345");
        EnvironmentAgentsConfig agents = pairEnvironmentConfig.getAgents();
        assertThat(agents.size(), is(2));
        assertThat(agents.getUuids(), hasItem("123"));
        assertThat(agents.getUuids(), hasItem("345"));
    }
    @Test
    public void shouldDeduplicateRepeatedAgentsFrom2Parts() throws Exception {
        pairEnvironmentConfig.get(0).addAgent("123");
        pairEnvironmentConfig.get(1).addAgent("123");
        EnvironmentAgentsConfig agents = pairEnvironmentConfig.getAgents();
        assertThat(agents.size(), is(1));
        assertThat(agents.getUuids(), hasItem("123"));
    }

    @Test
    public void shouldHaveVariablesFrom2Parts() throws Exception {
        pairEnvironmentConfig.get(0).addEnvironmentVariable("variable-name1", "variable-value1");
        pairEnvironmentConfig.get(1).addEnvironmentVariable("variable-name2", "variable-value2");

        assertTrue(pairEnvironmentConfig.hasVariable("variable-name1"));
        assertTrue(pairEnvironmentConfig.hasVariable("variable-name2"));
    }
    @Test
    public void shouldAddEnvironmentVariablesToEnvironmentVariableContextFrom2Parts() throws Exception {
        pairEnvironmentConfig.get(0).addEnvironmentVariable("variable-name1", "variable-value1");
        pairEnvironmentConfig.get(1).addEnvironmentVariable("variable-name2", "variable-value2");

        EnvironmentVariableContext context = pairEnvironmentConfig.createEnvironmentContext();
        assertThat(context.getProperty("variable-name1"), is("variable-value1"));
        assertThat(context.getProperty("variable-name2"), is("variable-value2"));
    }
    @Test
    public void shouldAddDeduplicatedEnvironmentVariablesToEnvironmentVariableContextFrom2Parts() throws Exception {
        pairEnvironmentConfig.get(0).addEnvironmentVariable("variable-name1", "variable-value1");
        pairEnvironmentConfig.get(1).addEnvironmentVariable("variable-name1", "variable-value1");

        assertThat(pairEnvironmentConfig.getVariables().size(), is(1));

        EnvironmentVariableContext context = pairEnvironmentConfig.createEnvironmentContext();
        assertThat(context.getProperty("variable-name1"), is("variable-value1"));
    }

    @Test
    public void shouldCreateErrorsForInconsistentEnvironmentVariables() throws Exception {
        pairEnvironmentConfig.get(0).addEnvironmentVariable("variable-name1", "variable-value1");
        pairEnvironmentConfig.get(1).addEnvironmentVariable("variable-name1", "variable-value2");
        pairEnvironmentConfig.validate(null);
        assertThat(pairEnvironmentConfig.errors().isEmpty(), is(false));
        assertThat(pairEnvironmentConfig.errors().on(MergeEnvironmentConfig.CONSISTENT_KV),
                Matchers.is("Environment variable 'variable-name1' is defined more than once with different values"));
    }

    @Test
    public void shouldFailUpdatePipelinesWhenRemovingFromNonEditableSource() {
        BasicEnvironmentConfig uatLocalPart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatLocalPart.setOrigins(new FileConfigOrigin());
        BasicEnvironmentConfig uatRemotePart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatRemotePart.setOrigins(new RepoConfigOrigin());
        // add untouchable pipeline
        uatRemotePart.addPipeline(new CaseInsensitiveString("baz"));
        pairEnvironmentConfig = new MergeEnvironmentConfig(uatLocalPart, uatRemotePart);

        try {
            pairEnvironmentConfig.setConfigAttributes(new SingletonMap(BasicEnvironmentConfig.PIPELINES_FIELD,
                    Arrays.asList(new SingletonMap("name", "foo"), new SingletonMap("name", "bar"))));
        }
        catch (Exception ex)
        {
            assertThat(ex.getMessage(), startsWith("Cannot remove pipeline baz from environment UAT because it is defined in non-editable source"));
        }

        assertThat(pairEnvironmentConfig.getPipelineNames(), is(Arrays.asList(new CaseInsensitiveString("baz"))));
    }

    @Test
    public void shouldFailToRemoveAgentFromEnvironment_WhenSourceIsNonEditable() throws Exception {
        BasicEnvironmentConfig uatLocalPart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatLocalPart.setOrigins(new FileConfigOrigin());
        BasicEnvironmentConfig uatRemotePart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatRemotePart.setOrigins(new RepoConfigOrigin());

        environmentConfig = new MergeEnvironmentConfig(uatLocalPart, uatRemotePart);
        uatRemotePart.addAgent("uuid1");
        uatRemotePart.addAgent("uuid2");
        assertThat(environmentConfig.getAgents().size(), is(2));
        assertThat(environmentConfig.hasAgent("uuid1"), is(true));
        assertThat(environmentConfig.hasAgent("uuid2"), is(true));

        try {
            environmentConfig.setConfigAttributes(new SingletonMap(BasicEnvironmentConfig.AGENTS_FIELD,
                    Arrays.asList(new SingletonMap("uuid", "uuid-2"), new SingletonMap("uuid", "uuid-3"))));
        }
        catch (Exception ex){
            assertThat(ex.getMessage(),startsWith("Cannot remove agent uuid1 from environment UAT because it is defined in non-editable source"));
        }

        assertThat(environmentConfig.getAgents().size(), is(2));
        assertThat(environmentConfig.hasAgent("uuid1"), is(true));
        assertThat(environmentConfig.hasAgent("uuid2"), is(true));
    }

    @Test
    public void shouldFailToUpdateEnvironmentVariables_WhenSourceIsNonEditable() {
        BasicEnvironmentConfig uatLocalPart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatLocalPart.setOrigins(new FileConfigOrigin());
        BasicEnvironmentConfig uatRemotePart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatRemotePart.setOrigins(new RepoConfigOrigin());

        uatRemotePart.addEnvironmentVariable("hello", "world");
        environmentConfig = new MergeEnvironmentConfig(uatLocalPart, uatRemotePart);
        try {
            environmentConfig.setConfigAttributes(new SingletonMap(BasicEnvironmentConfig.VARIABLES_FIELD,
                    Arrays.asList(envVar("foo", "bar"), envVar("baz", "quux"))));
        }
        catch (Exception ex)
        {
            assertThat(ex.getMessage(),startsWith("Cannot remove variable hello from environment UAT because it is defined in non-editable source"));
        }
        assertThat(environmentConfig.getVariables(), hasItem(new EnvironmentVariableConfig("hello", "world")));
        assertThat(environmentConfig.getVariables().size(), is(1));
    }


    @Test
    public void shouldUpdateEnvironmentVariables_WhenSourceIsNonEditable_ButChangesAreCompatible() {
        BasicEnvironmentConfig uatLocalPart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatLocalPart.setOrigins(new FileConfigOrigin());
        BasicEnvironmentConfig uatRemotePart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatRemotePart.setOrigins(new RepoConfigOrigin());

        uatRemotePart.addEnvironmentVariable("hello", "world");
        environmentConfig = new MergeEnvironmentConfig(uatLocalPart, uatRemotePart);
        environmentConfig.setConfigAttributes(new SingletonMap(BasicEnvironmentConfig.VARIABLES_FIELD,
                    Arrays.asList(envVar("foo", "bar"), envVar("baz", "quux"),envVar("hello", "world"))));

        assertThat(environmentConfig.getVariables(), hasItem(new EnvironmentVariableConfig("hello", "world")));
        assertThat(environmentConfig.getVariables(), hasItem(new EnvironmentVariableConfig("foo", "bar")));
        assertThat(environmentConfig.getVariables(), hasItem(new EnvironmentVariableConfig("baz", "quux")));
        assertThat(environmentConfig.getVariables().size(), is(3));

        assertThat("ChangesShouldBeInLocalConfig",uatLocalPart.getVariables(), hasItem(new EnvironmentVariableConfig("foo", "bar")));
        assertThat("ChangesShouldBeInLocalConfig",uatLocalPart.getVariables(), hasItem(new EnvironmentVariableConfig("baz", "quux")));
        assertThat("ChangesShouldBeInLocalConfig",uatLocalPart.getVariables().size(), is(2));
    }
    @Test
    public void shouldUpdateEnvironmentVariablesWhenSourceIsEditable() {
        BasicEnvironmentConfig uatLocalPart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatLocalPart.setOrigins(new FileConfigOrigin());
        BasicEnvironmentConfig uatRemotePart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatRemotePart.setOrigins(new RepoConfigOrigin());

        uatLocalPart.addEnvironmentVariable("hello", "world");
        environmentConfig = new MergeEnvironmentConfig(uatLocalPart, uatRemotePart);
        environmentConfig.setConfigAttributes(new SingletonMap(BasicEnvironmentConfig.VARIABLES_FIELD,
                Arrays.asList(envVar("foo", "bar"), envVar("baz", "quux"),envVar("hello", "you"))));

        assertThat(environmentConfig.getVariables(), hasItem(new EnvironmentVariableConfig("hello", "you")));
        assertThat(environmentConfig.getVariables(), hasItem(new EnvironmentVariableConfig("foo", "bar")));
        assertThat(environmentConfig.getVariables(), hasItem(new EnvironmentVariableConfig("baz", "quux")));
        assertThat(environmentConfig.getVariables().size(), is(3));

        assertThat("ChangesShouldBeInLocalConfig",uatLocalPart.getVariables(), hasItem(new EnvironmentVariableConfig("hello", "you")));
        assertThat("ChangesShouldBeInLocalConfig",uatLocalPart.getVariables(), hasItem(new EnvironmentVariableConfig("foo", "bar")));
        assertThat("ChangesShouldBeInLocalConfig",uatLocalPart.getVariables(), hasItem(new EnvironmentVariableConfig("baz", "quux")));
        assertThat("ChangesShouldBeInLocalConfig",uatLocalPart.getVariables().size(), is(3));
    }
}
