/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static com.thoughtworks.go.helper.EnvironmentVariablesConfigMother.env;
import static com.thoughtworks.go.utils.SerializationTester.serializeAndDeserialize;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.*;

public class DefaultJobPlanTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File workingFolder;

    @Before
    public void setUp() throws IOException {
        workingFolder = temporaryFolder.newFolder("workingFolder");
        File file = new File(workingFolder, "cruise-output/log.xml");
        file.getParentFile().mkdirs();
        file.createNewFile();
    }

    @Test
    public void shouldMatchResourcesIfBuildPlanHasNoResources() {
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(), new ArrayList<>(), new ArtifactPropertiesGenerators(), 0, null, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
        Resources agentResources = new Resources(new Resource("Foo"));
        assertTrue(plan.match(agentResources));
    }

    @Test
    public void shouldMatchIfBuildPlanAndAgentHaveSameResources() {
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(new Resource("Foo")), new ArrayList<>(), new ArtifactPropertiesGenerators(), 0, null, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
        assertTrue(plan.match(new Resources(new Resource("Foo"))));
    }

    @Test
    public void shouldNotMatchIfAgentDonotContainBuildPlanResources() {
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(new Resource("Foo")), new ArrayList<>(), new ArtifactPropertiesGenerators(), 0, null, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
        assertFalse(plan.match(new Resources(new Resource("Bar"))));
    }

    @Test
    public void shouldMatchIfAgentAndBuildPlanResourcesIrrespectiveOfOrder() {
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(new Resource("Foo"), new Resource("Bar"), new Resource("car")), new ArrayList<>(), new ArtifactPropertiesGenerators(), 0, null, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
        assertTrue(plan.match(
                new Resources(new Resource("Bar"), new Resource("car"), new Resource("Foo"))));
    }

    @Test
    public void shouldMatchIfBothAgentAndBuildPlanHaveNotResources() {
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(), new ArrayList<>(), new ArtifactPropertiesGenerators(), 0, null, null, new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);

        assertTrue(plan.match(new Resources()));
    }

    @Test
    public void shouldApplyEnvironmentVariablesWhenRunningTheJob() {
        EnvironmentVariablesConfig variables = new EnvironmentVariablesConfig();
        variables.add("VARIABLE_NAME", "variable value");
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(), new ArrayList<>(), new ArtifactPropertiesGenerators(), -1, null, null,
                variables, new EnvironmentVariablesConfig(), null);

        EnvironmentVariableContext variableContext = new EnvironmentVariableContext();
        plan.applyTo(variableContext);
        assertThat(variableContext.getProperty("VARIABLE_NAME"), is("variable value"));
    }

    @Test
    public void shouldRespectTriggerVariablesOverConfigVariables() {
        DefaultJobPlan original = new DefaultJobPlan(new Resources(), new ArrayList<>(),
                new ArtifactPropertiesGenerators(), 0, new JobIdentifier(), "uuid", env(new String[]{"blah","foo"},new String[]{"value","bar"}), new EnvironmentVariablesConfig(), null);
        original.setTriggerVariables(env(new String[]{"blah","another"},new String[]{"override","anotherValue"}));
        EnvironmentVariableContext variableContext = new EnvironmentVariableContext();
        original.applyTo(variableContext);
        assertThat(variableContext.getProperty("blah"),is("override"));
        assertThat(variableContext.getProperty("foo"),is("bar"));
        //becuase its a security issue to let operator set values for unconfigured variables
        assertThat(variableContext.getProperty("another"),is(nullValue()));
    }

    @Test
    public void shouldBeAbleToSerializeAndDeserialize() throws ClassNotFoundException, IOException {
        DefaultJobPlan original = new DefaultJobPlan(new Resources(), new ArrayList<>(),
                new ArtifactPropertiesGenerators(), 0, new JobIdentifier(), "uuid", new EnvironmentVariablesConfig(), new EnvironmentVariablesConfig(), null);
        DefaultJobPlan clone = (DefaultJobPlan) serializeAndDeserialize(original);
        assertThat(clone, is(original));
    }
}
