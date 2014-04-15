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

import com.thoughtworks.go.util.SystemEnvironment;
import static org.hamcrest.core.Is.is;

import com.thoughtworks.go.util.validators.FileValidator;
import com.thoughtworks.go.util.validators.Validation;
import org.junit.After;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;

public class FileValidatorTest {
    private String realConfigDir;

    @Before
    public void setUp() {
        realConfigDir = SystemEnvironment.getProperty("cruise.config.dir");
        new SystemEnvironment().setProperty("cruise.config.dir", SystemEnvironment.getProperty("java.io.tmpdir"));
    }

    @After
    public void tearDown() {
        if (realConfigDir != null) {
            new SystemEnvironment().setProperty("cruise.config.dir", realConfigDir);
        } else {
            new SystemEnvironment().clearProperty("cruise.config.dir");
        }
    }

    @Test
    public void shouldSetValidationToFalseIfFileDoesNotExistInClasspath() {
        FileValidator fv = FileValidator.configFileAlwaysOverwrite("does.not.exist", new SystemEnvironment());
        Validation val = new Validation();
        fv.validate(val);
        new File(SystemEnvironment.getProperty("java.io.tmpdir"), "does.not.exist").deleteOnExit();
        assertThat(val.isSuccessful(), is(false));
    }
}
