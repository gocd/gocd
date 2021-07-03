/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileValidatorTest {
    private String realConfigDir;

    @BeforeEach
    public void setUp() {
        realConfigDir = new SystemEnvironment().getPropertyImpl("cruise.config.dir");
        new SystemEnvironment().setProperty("cruise.config.dir", new SystemEnvironment().getPropertyImpl("java.io.tmpdir"));
    }

    @AfterEach
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
        new File(new SystemEnvironment().getPropertyImpl("java.io.tmpdir"), "does.not.exist").deleteOnExit();
        assertThat(val.isSuccessful(), is(false));
    }
}
