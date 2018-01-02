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

package com.thoughtworks.go.config.validation;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class FilePathTypeValidatorTest {
    private FilePathTypeValidator filePathTypeValidator;

    @Before public void setUp() throws Exception {
        filePathTypeValidator = new FilePathTypeValidator();
    }

    @Test
    public void shouldConsiderNullAsValidPath() { //because attributes of filePathType in xsd are all optional
        assertThat(filePathTypeValidator.isPathValid(null), is(true));
    }

    @Test
    public void shouldEnsurePathIsRelative() {
        assertThat(filePathTypeValidator.isPathValid(".."), is(false));
        assertThat(filePathTypeValidator.isPathValid("../a"), is(false));
        assertThat(filePathTypeValidator.isPathValid(" "), is(false));
        assertThat(filePathTypeValidator.isPathValid("./a"), is(true));
        assertThat(filePathTypeValidator.isPathValid(". "), is(false));
        assertThat(filePathTypeValidator.isPathValid(" ."), is(false));
        assertThat(filePathTypeValidator.isPathValid("abc"), is(true));
    }
}
