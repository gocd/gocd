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

package com.thoughtworks.go.domain.builder;

import java.io.File;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.domain.BuildLogElement;
import com.thoughtworks.go.domain.RunIfConfigs;
import com.thoughtworks.go.domain.StubGoPublisher;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.plugin.access.pluggabletask.TaskExtension;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TempFiles;
import com.thoughtworks.go.util.command.CruiseControlException;
import com.thoughtworks.go.util.command.EnvironmentVariableContext;
import com.thoughtworks.go.work.DefaultGoPublisher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.thoughtworks.go.config.RunIfConfig.FAILED;
import static com.thoughtworks.go.config.RunIfConfig.PASSED;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.WINDOWS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;

@RunWith(JunitExtRunner.class)
public class BuilderTest {
    private StubGoPublisher goPublisher = new StubGoPublisher();

    private EnvironmentVariableContext environmentVariableContext;
    private SystemEnvironment systemEnvironment;

    private File workingDir;
    private File resolvedWorkingDir;

    @Before
    public void setUp() throws Exception {
        environmentVariableContext = new EnvironmentVariableContext();
        systemEnvironment = new SystemEnvironment();

        workingDir = new File(".");
        resolvedWorkingDir = systemEnvironment.resolveAgentWorkingDirectory(workingDir);

        resolvedWorkingDir.mkdirs();
    }

    @After
    public void tearDown() throws Exception {
        FileUtil.deleteFolder(resolvedWorkingDir);
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldReportErrorWhenCancelCommandDoesNotExists() throws Exception {

        StubBuilder stubBuilder = new StubBuilder();

        CommandBuilder cancelBuilder = new CommandBuilder("echo2", "cancel task", workingDir,
                new RunIfConfigs(FAILED), stubBuilder,
                "");

        CommandBuilder builder = new CommandBuilder("echo", "normal task", workingDir, new RunIfConfigs(FAILED),
                cancelBuilder,
                "");
        builder.cancel(goPublisher, new EnvironmentVariableContext(), systemEnvironment, null);

        assertThat(goPublisher.getMessage(),
                containsString("Error happened while attempting to execute 'echo2 cancel task'"));
    }

    @Test
    public void shouldRunCancelBuilderWhenCancelled() throws Exception {
        StubBuilder stubBuilder = new StubBuilder();
        CommandBuilder builder = new CommandBuilder("echo", "", workingDir, new RunIfConfigs(FAILED), stubBuilder,
                "");
        builder.cancel(goPublisher, environmentVariableContext, systemEnvironment, null);
        assertThat(stubBuilder.wasCalled, is(true));
    }

    @Test
    public void shouldLogToConsoleOutWhenCancelling() {
        StubBuilder stubBuilder = new StubBuilder();
        CommandBuilder builder = new CommandBuilder("echo", "", workingDir, new RunIfConfigs(FAILED), stubBuilder,
                "");
        builder.cancel(goPublisher, environmentVariableContext, systemEnvironment, null);

        assertThat(goPublisher.getMessage(), containsString("Start to execute cancel task"));
        assertThat(goPublisher.getMessage(), containsString("Task is cancelled"));
    }

    @Test
    public void shouldNotBuildIfTheJobIsCancelled() throws Exception {
        CommandBuilder builder = new CommandBuilder("echo", "", workingDir, new RunIfConfigs(FAILED),
                new StubBuilder(),
                "");

        builder.build(new BuildLogElement(), PASSED, goPublisher, environmentVariableContext, systemEnvironment, null);

        assertThat(goPublisher.getMessage(), is(""));
    }

    private class StubBuilder extends Builder {
        boolean wasCalled;

        public StubBuilder() {
            super(null, null, "");
        }

        public void build(BuildLogElement buildElement, DefaultGoPublisher publisher,
                          EnvironmentVariableContext environmentVariableContext, SystemEnvironment systemEnvironment, TaskExtension taskExtension) throws CruiseControlException {
            wasCalled = true;
        }
    }

}
