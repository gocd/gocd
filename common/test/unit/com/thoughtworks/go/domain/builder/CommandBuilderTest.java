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

package com.thoughtworks.go.domain.builder;

import java.io.File;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.command.CommandLine;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(JunitExtRunner.class)
public class CommandBuilderTest {

    private File tempWorkDir;

    @Before
    public void setUp(){
        tempWorkDir = TestFileUtil.createTempFolder("someDir");
    }

    @After
    public void tearDown(){
        FileUtils.deleteQuietly(tempWorkDir);
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {EnhancedOSChecker.WINDOWS})
    public void commandWithArgsList_shouldAddCmdBeforeAWindowsCommand() {
        String[] args = {"some thing"};
        CommandBuilderWithArgList commandBuilderWithArgList = new CommandBuilderWithArgList("echo", args, tempWorkDir, null, null, "some desc");
        CommandLine commandLine = commandBuilderWithArgList.buildCommandLine();
        assertThat(commandLine.toStringForDisplay(), is("cmd /c echo some thing"));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {EnhancedOSChecker.WINDOWS})
    public void commandWithArgs_shouldAddCmdBeforeAWindowsCommand() {
        CommandBuilder commandBuilder = new CommandBuilder("echo", "some thing", tempWorkDir, null, null, "some desc");
        CommandLine commandLine = commandBuilder.buildCommandLine();
        assertThat(commandLine.toStringForDisplay(), is("cmd /c echo some thing"));
    }
}
