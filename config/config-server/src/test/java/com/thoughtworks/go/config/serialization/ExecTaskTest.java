/*
 * Copyright 2024 Thoughtworks, Inc.
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
package com.thoughtworks.go.config.serialization;

import com.thoughtworks.go.config.Argument;
import com.thoughtworks.go.config.ConfigCache;
import com.thoughtworks.go.config.ExecTask;
import com.thoughtworks.go.config.MagicalGoConfigXmlLoader;
import com.thoughtworks.go.config.registry.ConfigElementImplementationRegistry;
import com.thoughtworks.go.domain.config.Arguments;
import com.thoughtworks.go.helper.ConfigFileFixture;
import com.thoughtworks.go.util.ConfigElementImplementationRegistryMother;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.fail;

public class ExecTaskTest {
    @Test
    public void shouldSupportMultipleArgs() throws Exception {
        String xml = """
                <exec command='ls'>
                  <arg>arg1</arg>
                  <arg>arg2</arg>
                </exec>""";
        ExecTask execTask = new MagicalGoConfigXmlLoader(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins()).fromXmlPartial(xml, ExecTask.class);
        assertThat(execTask.getArgList(), is(new Arguments(new Argument("arg1"), new Argument("arg2"))));
    }

    @Test
    public void shouldNotSupportArgsAttributeWithArgSubElement() throws Exception {
        String jobXml = """
                <job name='dev'>
                  <tasks>
                    <exec command='ls' args='arg1 arg2'>
                      <arg>arg1</arg>
                      <arg>arg2</arg>
                    </exec>
                  </tasks></job>""";
        String configXml = ConfigFileFixture.withJob(jobXml);
        try {
            ConfigElementImplementationRegistry registry = ConfigElementImplementationRegistryMother.withNoPlugins();

            new MagicalGoConfigXmlLoader(new ConfigCache(), registry).loadConfigHolder(configXml);
            fail("should throw exception if both 'args' attribute and 'arg' sub element are configured");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(ExecTask.EXEC_CONFIG_ERROR));
        }
    }

    @Test
    public void validateTask_shouldValidateThatCommandIsRequired() {
        ExecTask execTask = new ExecTask();

        execTask.validateTask(null);

        assertThat(execTask.errors().isEmpty(), is(false));
        assertThat(execTask.errors().on(ExecTask.COMMAND), is("Command cannot be empty"));
    }

    @Test
    public void shouldUseConfiguredWorkingDirectory() throws Exception {
        File absoluteFile = new File("test").getAbsoluteFile();
        ExecTask task = new ExecTask("command", "arguments", absoluteFile.getAbsolutePath());

        assertThat(task.workingDirectory(), Matchers.is(absoluteFile.getPath()));
    }
}
