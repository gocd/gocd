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

import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static com.thoughtworks.go.utils.SerializationTester.serializeAndDeserialize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class DefaultJobPlanTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File workingFolder;

    @Before
    public void setUp() throws IOException {
        workingFolder = temporaryFolder.newFolder("workingFolder");
        File file = new File(workingFolder, "cruise-output/log.xml");
        file.getParentFile().mkdirs();
        file.createNewFile();
    }

    @Test
    public void shouldApplyEnvironmentVariablesWhenRunningTheJob() {
        EnvironmentVariables variables = new EnvironmentVariables();
        variables.add("VARIABLE_NAME", "variable value");
        DefaultJobPlan plan = new DefaultJobPlan(new Resources(), new ArrayList<>(), new ArrayList<>(), -1, null, null,
                variables, new EnvironmentVariables(), null, null);

        EnvironmentVariableContext variableContext = new EnvironmentVariableContext();
        plan.applyTo(variableContext);
        assertThat(variableContext.getProperty("VARIABLE_NAME"), is("variable value"));
    }

    @Test
    public void shouldRespectTriggerVariablesOverConfigVariables() {
        final EnvironmentVariables environmentVariables = new EnvironmentVariables(Arrays.asList(
                new EnvironmentVariable("blah", "value"), new EnvironmentVariable("foo", "bar")));
        final EnvironmentVariables triggerEnvironmentVariables = new EnvironmentVariables(Arrays.asList(
                new EnvironmentVariable("blah", "override"), new EnvironmentVariable("another", "anotherValue")));

        DefaultJobPlan original = new DefaultJobPlan(new Resources(), new ArrayList<>(),
                new ArrayList<>(), 0, new JobIdentifier(), "uuid", environmentVariables, triggerEnvironmentVariables, null, null);
        EnvironmentVariableContext variableContext = new EnvironmentVariableContext();
        original.applyTo(variableContext);
        assertThat(variableContext.getProperty("blah"), is("override"));
        assertThat(variableContext.getProperty("foo"), is("bar"));
        //becuase its a security issue to let operator set values for unconfigured variables
        assertThat(variableContext.getProperty("another"), is(nullValue()));
    }

    @Test
    public void shouldBeAbleToSerializeAndDeserialize() throws ClassNotFoundException, IOException {
        DefaultJobPlan original = new DefaultJobPlan(new Resources(), new ArrayList<>(),
                new ArrayList<>(), 0, new JobIdentifier(), "uuid", new EnvironmentVariables(), new EnvironmentVariables(), null, null);
        DefaultJobPlan clone = (DefaultJobPlan) serializeAndDeserialize(original);
        assertThat(clone, is(original));
    }
}
