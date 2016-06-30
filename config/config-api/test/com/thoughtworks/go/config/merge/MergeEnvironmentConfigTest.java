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
import com.thoughtworks.go.config.remote.UIConfigOrigin;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.apache.commons.collections.map.SingletonMap;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertNull;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class MergeEnvironmentConfigTest extends EnvironmentConfigTestBase {
    public MergeEnvironmentConfig singleEnvironmentConfig;
    public MergeEnvironmentConfig pairEnvironmentConfig;
    private static final String AGENT_UUID = "uuid";
    private EnvironmentConfig localUatEnv1;
    private EnvironmentConfig uatLocalPart2;
    private BasicEnvironmentConfig uatRemotePart;

    @Before
    public void setUp() throws Exception {
        localUatEnv1 = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        localUatEnv1.setOrigins(new FileConfigOrigin());

        singleEnvironmentConfig = new MergeEnvironmentConfig(localUatEnv1);
        uatLocalPart2 = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatLocalPart2.setOrigins(new FileConfigOrigin());
        uatRemotePart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatRemotePart.setOrigins(new RepoConfigOrigin());
        pairEnvironmentConfig = new MergeEnvironmentConfig(
                uatLocalPart2,
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
    public void getRemotePipelines_shouldReturnEmptyWhenOnlyLocalPartHasPipelines()
    {
        uatLocalPart2.addPipeline(new CaseInsensitiveString("pipe"));
        assertThat(pairEnvironmentConfig.getRemotePipelines().isEmpty(), is(true));
    }

    @Test
    public void getRemotePipelines_shouldReturnPipelinesFromRemotePartWhenRemoteHasPipesAssigned() {
        uatRemotePart.addPipeline(new CaseInsensitiveString("pipe"));
        assertThat(environmentConfig.getRemotePipelines().isEmpty(), is(false));
    }

    @Test
    public void shouldReturnFalseThatLocal()
    {
        assertThat(environmentConfig.isLocal(),is(false));
    }
    @Test
    public void shouldGetLocalPartWhenOriginFile()
    {
        assertThat(environmentConfig.getLocal(),is(uatLocalPart2));
    }

    @Test
    public void hasSamePipelinesAs_shouldReturnTrueWhenAnyPipelineNameIsEqualToOther(){
        pairEnvironmentConfig.get(0).addPipeline(new CaseInsensitiveString("pipe1"));
        pairEnvironmentConfig.get(1).addPipeline(new CaseInsensitiveString("pipe2"));
        BasicEnvironmentConfig config = new BasicEnvironmentConfig();
        config.addPipeline(new CaseInsensitiveString("pipe2"));
        assertThat(pairEnvironmentConfig.hasSamePipelinesAs(config),is(true));
    }

    @Test
    public void hasSamePipelinesAs_shouldReturnFalseWhenNoneOfOtherPipelinesIsEqualToOther(){
        pairEnvironmentConfig.get(0).addPipeline(new CaseInsensitiveString("pipe1"));
        pairEnvironmentConfig.get(1).addPipeline(new CaseInsensitiveString("pipe2"));
        BasicEnvironmentConfig config = new BasicEnvironmentConfig();
        config.addPipeline(new CaseInsensitiveString("pipe3"));
        assertThat(pairEnvironmentConfig.hasSamePipelinesAs(config),is(false));
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
    public void shouldReturnTrueWhenOnlyPartIsLocal() {
        BasicEnvironmentConfig uatLocalPart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatLocalPart.setOrigins(new FileConfigOrigin());
        environmentConfig = new MergeEnvironmentConfig(uatLocalPart);
        assertThat(environmentConfig.isLocal(),is(true));
    }

    @Test
    public void shouldReturnFalseWhenPartIsRemote() {
        BasicEnvironmentConfig uatLocalPart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatLocalPart.setOrigins(new FileConfigOrigin());
        BasicEnvironmentConfig uatRemotePart = new BasicEnvironmentConfig(new CaseInsensitiveString("UAT"));
        uatRemotePart.setOrigins(new RepoConfigOrigin());
        environmentConfig = new MergeEnvironmentConfig(uatLocalPart, uatRemotePart);
        assertThat(environmentConfig.isLocal(),is(false));
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
