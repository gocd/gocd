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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mock;

public class JettyWorkDirValidatorTest {

    private JettyWorkDirValidator jettyWorkDirValidator;

    @Mock
    public SystemEnvironment systemEnvironment;

    @Rule
    public TemporaryFolder temporaryFolder =  new TemporaryFolder();
    private File homeDir;

    @Before
    public void setUp() throws Exception {
        initMocks(this);
        homeDir = temporaryFolder.newFolder();
        jettyWorkDirValidator = new JettyWorkDirValidator(systemEnvironment);

    }

    @Test
    public void shouldSetJettyHomeAndBasePropertyIfItsNotSet() {
        when(systemEnvironment.getPropertyImpl("jetty.home")).thenReturn("");
        when(systemEnvironment.getPropertyImpl("user.dir")).thenReturn("junk");
        Validation val = new Validation();
        jettyWorkDirValidator.validate(val);
        assertThat(val.isSuccessful(), is(true));

        verify(systemEnvironment).getPropertyImpl("user.dir");
        verify(systemEnvironment).setProperty("jetty.home", "junk");
    }

    @Test
    public void shouldSetJettyBaseToValueOfJettyHome() {
        when(systemEnvironment.getPropertyImpl("jetty.home")).thenReturn("foo");
        Validation val = new Validation();
        jettyWorkDirValidator.validate(val);
        assertThat(val.isSuccessful(), is(true));
        verify(systemEnvironment).setProperty("jetty.base", "foo");
    }

    @Test
    public void shouldCreateWorkDirIfItDoesNotExist() {
        when(systemEnvironment.getPropertyImpl("jetty.home")).thenReturn(homeDir.getAbsolutePath());
        Validation val = new Validation();
        jettyWorkDirValidator.validate(val);
        assertThat(val.isSuccessful(), is(true));
        File work = new File(homeDir, "work");
        assertThat(work.exists(), is(true));
    }

    @Test
    public void shouldNotCreateTheJettyHomeDirIfItDoesNotExist() {
        String jettyHome = "home_dir";
        when(systemEnvironment.getPropertyImpl("jetty.home")).thenReturn(jettyHome);
        Validation val = new Validation();
        jettyWorkDirValidator.validate(val);
        assertThat(val.isSuccessful(), is(true));
        assertThat(new File(jettyHome).exists(), is(false));
    }

    @Test
    public void shouldRecreateWorkDirIfItExists() throws IOException {
        File oldWorkDir = new File(homeDir, "work");
        oldWorkDir.mkdir();
        new File(oldWorkDir, "junk.txt").createNewFile();
        when(systemEnvironment.getPropertyImpl("jetty.home")).thenReturn(homeDir.getAbsolutePath());
        Validation val = new Validation();
        jettyWorkDirValidator.validate(val);
        assertThat(val.isSuccessful(), is(true));
        File recreatedWorkDir = new File(homeDir, "work");
        assertThat(recreatedWorkDir.exists(), is(true));
        assertThat(recreatedWorkDir.listFiles().length, is(0));
    }
}
