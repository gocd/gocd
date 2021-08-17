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
package com.thoughtworks.go.validation;

import com.thoughtworks.go.domain.materials.ValidationBean;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

public class EmailValidatorTest {

    @Test
    public void shouldCheckValidationForEmailAddress() throws Exception {
        assertThat(validate("some@here.com"), Matchers.is(ValidationBean.valid()));
    }

    @Test
    public void shouldReturnInvalidForInvalidEmailAddress() throws Exception {
        assertThat(validate("invalid").isValid(), is(false));
    }

    @Test
    public void shouldExplainThatEmailAddressIsInvalid() throws Exception {
        assertThat(validate("invalid").getError(), containsString(EmailValidator.EMAIL_ERROR_MESSAGE));
    }

    @Test
    public void shouldThrowExceptionWhenEmailIsInvalid() throws Exception {
        try {
            Validator.EMAIL.assertValid("dklaf;jds;l");
            fail("Expected to throw exception");
        } catch (Exception e) {
            assertThat(e.getMessage(), containsString(EmailValidator.EMAIL_ERROR_MESSAGE));
        }
    }

    private ValidationBean validate(String address) {
        return Validator.EMAIL.validate(address);
    }

}
