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
package com.thoughtworks.go.config.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FilePathTypeValidatorTest {
    private FilePathTypeValidator filePathTypeValidator;

    @BeforeEach
    public void setUp() throws Exception {
        filePathTypeValidator = new FilePathTypeValidator();
    }

    @Test
    public void shouldConsiderNullAsValidPath() { //because attributes of filePathType in xsd are all optional
        assertThat(filePathTypeValidator.isPathValid(null)).isTrue();
    }

    @Test
    public void shouldEnsurePathIsRelative() {
        assertThat(filePathTypeValidator.isPathValid("..")).isFalse();
        assertThat(filePathTypeValidator.isPathValid("../a")).isFalse();
        assertThat(filePathTypeValidator.isPathValid(" ")).isFalse();
        assertThat(filePathTypeValidator.isPathValid("./a")).isTrue();
        assertThat(filePathTypeValidator.isPathValid(". ")).isFalse();
        assertThat(filePathTypeValidator.isPathValid(" .")).isFalse();
        assertThat(filePathTypeValidator.isPathValid("abc")).isTrue();
    }
}
