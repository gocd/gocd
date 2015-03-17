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

import org.junit.Test;

public class JettyWorkDirValidatorTest {
    @Test
    public void shouldSetJettyHomePropertyIfItsNotSet() {
        new SystemEnvironment().clearProperty("jetty.home");
        JettyWorkDirValidator jettyWorkDirValidator = new JettyWorkDirValidator();
        Validation val = new Validation();
        jettyWorkDirValidator.validate(val);
        assertThat(val.isSuccessful(), is(true));
        assertThat(SystemEnvironment.getProperty("jetty.home"), is(SystemEnvironment.getProperty("user.dir")));
    }

    @Test
    public void shouldCreateWorkDirIfItDoesNotExist() {
        String testHomeDir = SystemEnvironment.getProperty("java.io.tmpdir") + "/test.jetty.home.dir";
        File testHome = new File(testHomeDir);
        testHome.mkdir();
        testHome.deleteOnExit();
        new SystemEnvironment().setProperty("jetty.home", testHomeDir);
        JettyWorkDirValidator jettyWorkDirValidator = new JettyWorkDirValidator();
        Validation val = new Validation();
        jettyWorkDirValidator.validate(val);
        assertThat(val.isSuccessful(), is(true));
        assertThat(SystemEnvironment.getProperty("jetty.home"), is(testHomeDir));
        File work = new File(testHomeDir, "work");
        assertThat(work.exists(), is(true));
        work.delete();
    }

    @Test
    public void shouldNotCreateTheJettyHomeDirIfItDoesNotExist() {
        String testHomeDir = SystemEnvironment.getProperty("java.io.tmpdir") + "/test.jetty.home.dir.should.not.exist";
        File testHome = new File(testHomeDir);
        testHome.delete();
        testHome.deleteOnExit();
        new SystemEnvironment().setProperty("jetty.home", testHomeDir);
        JettyWorkDirValidator jettyWorkDirValidator = new JettyWorkDirValidator();
        Validation val = new Validation();
        jettyWorkDirValidator.validate(val);
        assertThat(val.isSuccessful(), is(true));
        assertThat(SystemEnvironment.getProperty("jetty.home"), is(testHomeDir));
        assertThat(testHome.exists(), is(false));
    }

    @Test
    public void shouldRecreateWorkDirIfItExists() throws IOException {
        String testHomeDir = SystemEnvironment.getProperty("java.io.tmpdir") + "/test.jetty.home.dir";
        File testHome = new File(testHomeDir);
        testHome.mkdir();
        testHome.deleteOnExit();
        File work = new File(testHome, "work");
        work.mkdir();
        File oldFile = new File(work, "oldfile");
        work.deleteOnExit();
        oldFile.createNewFile();
        oldFile.deleteOnExit();

        new SystemEnvironment().setProperty("jetty.home", testHomeDir);
        JettyWorkDirValidator jettyWorkDirValidator = new JettyWorkDirValidator();
        Validation val = new Validation();
        jettyWorkDirValidator.validate(val);
        assertThat(val.isSuccessful(), is(true));

        File recreatedWorkDir = new File(testHomeDir, "work");
        assertThat(recreatedWorkDir.exists(), is(true));
        assertThat(recreatedWorkDir.listFiles().length, is(0));
    }
}
