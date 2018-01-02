/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config.validation;

import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class NameTypeValidatorTest {

    @Test
    public void shouldValidateNameBasedOnLength() {
        assertThat(new NameTypeValidator().isNameValid("name"), is(true));
        assertThat(new NameTypeValidator().isNameValid(nameOfLength(255)), is(true));
        assertThat(new NameTypeValidator().isNameValid(nameOfLength(256)), is(false));
    }

    @Test
    public void shouldValidateNameBasedOnCharacterType() {
        //[a-zA-Z0-9_\-]{1}[a-zA-Z0-9_\-.]*
        assertThat(new NameTypeValidator().isNameValid(""), is(false));
        assertThat(new NameTypeValidator().isNameValid("name"), is(true));

        assertThat(new NameTypeValidator().isNameValid("!"), is(false));
        assertThat(new NameTypeValidator().isNameValid("name!"), is(false));

        assertThat(new NameTypeValidator().isNameValid("name_123"), is(true));
        assertThat(new NameTypeValidator().isNameValid("1"), is(true));

        assertThat(new NameTypeValidator().isNameValid("."), is(false));
        assertThat(new NameTypeValidator().isNameValid("1."), is(true));
    }

    private String nameOfLength(final int length) {
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < length; i++) {
            name.append("a");
        }
        return name.toString();
    }
}
