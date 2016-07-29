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

package com.thoughtworks.go.util.validators;

import com.thoughtworks.go.util.ClassMockery;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.TestFileUtil;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JMock.class)
public class LoggingValidatorTest {
    private final Mockery context = new ClassMockery();
    private File log4jPropertiesFile;
    private Validator log4jFileValidator;
    private LoggingValidator.Log4jConfigReloader log4jConfigReloader;
    private SystemEnvironment env;

    @Before
    public void setUp() throws Exception {
        log4jPropertiesFile = new File("log4j.properties");
        log4jFileValidator = context.mock(Validator.class);
        log4jConfigReloader = context.mock(LoggingValidator.Log4jConfigReloader.class);
        env = context.mock(SystemEnvironment.class);
    }

    @Test
    public void shouldValidateLog4jExistsAndUpdateLogFolder() throws Exception {
        final Validation validation = new Validation();
        context.checking(new Expectations() { {
            one(log4jFileValidator).validate(validation);
            will(returnValue(validation));

            will(returnValue(validation));

            one(log4jConfigReloader).reload(log4jPropertiesFile);
        } });

        new LoggingValidator(log4jPropertiesFile, log4jFileValidator, log4jConfigReloader)
                .validate(validation);
        assertThat(validation.isSuccessful(), is(true));
    }

    @Test
    public void shouldCreateObjectCorrectly() throws Exception {
        File tempFolder = TestFileUtil.createUniqueTempFolder("foo");
        final String path = tempFolder.getAbsolutePath();

        context.checking(new Expectations() { {
            atLeast(1).of(env).getConfigDir();
            will(returnValue(path));
        } });
        LoggingValidator validator = new LoggingValidator(env);
        assertThat(validator.getLog4jFile(), is(new File(tempFolder, "log4j.properties")));
        assertThat((FileValidator) validator.getLog4jPropertiesValidator(), is(FileValidator.configFile("log4j.properties", env)));
    }
}
