/*
 * Copyright 2024 Thoughtworks, Inc.
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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class EmailValidatorTest {

    @Test
    public void shouldCheckValidationForEmailAddress() {
        assertThat(validate("some@here.com")).isEqualTo((ValidationBean.valid()));
    }

    @Test
    public void shouldReturnInvalidForInvalidEmailAddress() {
        assertThat(validate("invalid").isValid()).isFalse();
    }

    @Test
    public void shouldExplainThatEmailAddressIsInvalid() {
        assertThat(validate("invalid").getError()).contains(EmailValidator.EMAIL_ERROR_MESSAGE);
    }

    @Test
    public void shouldThrowExceptionWhenEmailIsInvalid() {
        assertThatThrownBy(() -> new EmailValidator().assertValid("dklaf;jds;l"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining(EmailValidator.EMAIL_ERROR_MESSAGE);
    }

    private ValidationBean validate(String address) {
        return new EmailValidator().validate(address);
    }

}
