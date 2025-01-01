/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.util.validators;

import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class JettyWorkDirValidatorTest {

    private JettyWorkDirValidator jettyWorkDirValidator;

    @Mock
    public SystemEnvironment systemEnvironment;

    @TempDir
    public File homeDir;

    @BeforeEach
    public void setUp() throws Exception {
        jettyWorkDirValidator = new JettyWorkDirValidator(systemEnvironment);
    }

    @Test
    public void shouldSetJettyHomeAndBasePropertyIfItsNotSet() {
        when(systemEnvironment.getPropertyImpl("jetty.home")).thenReturn("");
        when(systemEnvironment.getPropertyImpl("user.dir")).thenReturn("junk");
        Validation val = new Validation();
        jettyWorkDirValidator.validate(val);
        assertThat(val.isSuccessful()).isTrue();

        verify(systemEnvironment).getPropertyImpl("user.dir");
        verify(systemEnvironment).setProperty("jetty.home", "junk");
    }

    @Test
    public void shouldSetJettyBaseToValueOfJettyHome() {
        when(systemEnvironment.getPropertyImpl("jetty.home")).thenReturn("foo");
        Validation val = new Validation();
        jettyWorkDirValidator.validate(val);
        assertThat(val.isSuccessful()).isTrue();
        verify(systemEnvironment).setProperty("jetty.base", "foo");
    }

    @Test
    public void shouldCreateWorkDirIfItDoesNotExist() {
        when(systemEnvironment.getPropertyImpl("jetty.home")).thenReturn(homeDir.getAbsolutePath());
        Validation val = new Validation();
        jettyWorkDirValidator.validate(val);
        assertThat(val.isSuccessful()).isTrue();
        File work = new File(homeDir, "work");
        assertThat(work.exists()).isTrue();
    }

    @Test
    public void shouldNotCreateTheJettyHomeDirIfItDoesNotExist() {
        String jettyHome = "home_dir";
        when(systemEnvironment.getPropertyImpl("jetty.home")).thenReturn(jettyHome);
        Validation val = new Validation();
        jettyWorkDirValidator.validate(val);
        assertThat(val.isSuccessful()).isTrue();
        assertThat(new File(jettyHome).exists()).isFalse();
    }

    @Test
    public void shouldRecreateWorkDirIfItExists() throws IOException {
        File oldWorkDir = new File(homeDir, "work");
        oldWorkDir.mkdir();
        new File(oldWorkDir, "junk.txt").createNewFile();
        when(systemEnvironment.getPropertyImpl("jetty.home")).thenReturn(homeDir.getAbsolutePath());
        Validation val = new Validation();
        jettyWorkDirValidator.validate(val);
        assertThat(val.isSuccessful()).isTrue();
        File recreatedWorkDir = new File(homeDir, "work");
        assertThat(recreatedWorkDir.exists()).isTrue();
        assertThat(recreatedWorkDir.listFiles().length).isEqualTo(0);
    }
}
