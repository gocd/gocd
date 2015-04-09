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

package com.thoughtworks.go.util.validators;

import java.io.File;
import java.io.IOException;

import com.thoughtworks.go.util.SystemEnvironment;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class JettyWorkDirValidatorTest {
    @Rule
    public TemporaryFolder temporaryFolder =  new TemporaryFolder();
    private File homeDir;
    private SystemEnvironment systemEnvironment;

    @Before
    public void setUp() throws Exception {
        homeDir = temporaryFolder.newFolder();
        systemEnvironment = new SystemEnvironment();
    }

    @Test
    public void shouldSetJettyHomeAndBasePropertyIfItsNotSet() {
        systemEnvironment.clearProperty("jetty.home");
        systemEnvironment.clearProperty("jetty.base");
        systemEnvironment.setProperty("user.dir", "junk");
        JettyWorkDirValidator jettyWorkDirValidator = new JettyWorkDirValidator();
        Validation val = new Validation();
        jettyWorkDirValidator.validate(val);
        assertThat(val.isSuccessful(), is(true));
        assertThat(SystemEnvironment.getProperty("jetty.home"), is("junk"));
        assertThat(SystemEnvironment.getProperty("jetty.base"), is("junk"));
    }

    @Test
    public void shouldSetJettyBaseToValueOfJettyHome() {
        systemEnvironment.setProperty("jetty.home", "foo");
        systemEnvironment.clearProperty("jetty.base");
        JettyWorkDirValidator jettyWorkDirValidator = new JettyWorkDirValidator();
        Validation val = new Validation();
        jettyWorkDirValidator.validate(val);
        assertThat(val.isSuccessful(), is(true));
        assertThat(systemEnvironment.getPropertyImpl("jetty.base"), is("foo"));
    }

    @Test
    public void shouldCreateWorkDirIfItDoesNotExist() {
        systemEnvironment.setProperty("jetty.home", homeDir.getAbsolutePath());
        JettyWorkDirValidator jettyWorkDirValidator = new JettyWorkDirValidator();
        Validation val = new Validation();
        jettyWorkDirValidator.validate(val);
        assertThat(val.isSuccessful(), is(true));
        File work = new File(homeDir, "work");
        assertThat(work.exists(), is(true));
    }

    @Test
    public void shouldNotCreateTheJettyHomeDirIfItDoesNotExist() {
        String jettyHome = "home_dir";
        systemEnvironment.setProperty("jetty.home", jettyHome);
        JettyWorkDirValidator jettyWorkDirValidator = new JettyWorkDirValidator();
        Validation val = new Validation();
        jettyWorkDirValidator.validate(val);
        assertThat(val.isSuccessful(), is(true));
        assertThat(systemEnvironment.getPropertyImpl("jetty.home"), is(jettyHome));
        assertThat(systemEnvironment.getPropertyImpl("jetty.base"), is(jettyHome));
        assertThat(new File(jettyHome).exists(), is(false));
    }

    @Test
    public void shouldRecreateWorkDirIfItExists() throws IOException {
        File oldWorkDir = new File(homeDir, "work");
        oldWorkDir.mkdir();
        new File(oldWorkDir, "junk.txt").createNewFile();
        systemEnvironment.setProperty("jetty.home", homeDir.getAbsolutePath());
        JettyWorkDirValidator jettyWorkDirValidator = new JettyWorkDirValidator();
        Validation val = new Validation();
        jettyWorkDirValidator.validate(val);
        assertThat(val.isSuccessful(), is(true));
        File recreatedWorkDir = new File(homeDir, "work");
        assertThat(recreatedWorkDir.exists(), is(true));
        assertThat(recreatedWorkDir.listFiles().length, is(0));
    }
}
