/*
 * Copyright 2016 ThoughtWorks, Inc.
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
import com.thoughtworks.go.util.FileUtil;
import org.apache.commons.io.IOUtils;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ExecTaskTest {
    @Test
    public void shouldSupportMultipleArgs() throws Exception {
        String xml = "<exec command='ls'>\n"
                + "  <arg>arg1</arg>\n"
                + "  <arg>arg2</arg>\n"
                + "</exec>";
        ExecTask execTask = new MagicalGoConfigXmlLoader(new ConfigCache(), ConfigElementImplementationRegistryMother.withNoPlugins()).fromXmlPartial(xml, ExecTask.class);
        assertThat(execTask.getArgList(), is(new Arguments(new Argument("arg1"), new Argument("arg2"))));
    }

    @Test
    public void shouldNotSupportArgsAttributeWithArgSubElement() throws Exception {
        String jobXml = "<job name='dev'>\n"
                + "  <tasks>\n"
                + "    <exec command='ls' args='arg1 arg2'>\n"
                + "      <arg>arg1</arg>\n"
                + "      <arg>arg2</arg>\n"
                + "    </exec>\n"
                + "  </tasks>"
                + "</job>";
        String configXml = ConfigFileFixture.withJob(jobXml);
        try {
            ConfigElementImplementationRegistry registry = ConfigElementImplementationRegistryMother.withNoPlugins();

            new MagicalGoConfigXmlLoader(new ConfigCache(), registry).loadConfigHolder(FileUtil.readToEnd(IOUtils.toInputStream(configXml)));
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
