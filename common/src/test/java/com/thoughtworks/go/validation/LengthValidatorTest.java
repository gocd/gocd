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
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class LengthValidatorTest {

    @Test
    public void shouldReturnInvalidWhenLengthExceeds() {
        LengthValidator lengthValidator = new LengthValidator(2);
        ValidationBean validationBean = lengthValidator.validate("abc");
        assertThat(validationBean.isValid(), is(false));
    }

    @Test
    public void shouldReturnValidWhenLengthDoesNotExceeds() {
        LengthValidator lengthValidator = new LengthValidator(2);
        ValidationBean validationBean = lengthValidator.validate("ab");
        assertThat(validationBean.isValid(), is(true));
    }

    @Test
    public void shouldReturnValidWhenNoInput() {
        LengthValidator lengthValidator = new LengthValidator(2);
        ValidationBean validationBean = lengthValidator.validate(null);
        assertThat(validationBean.isValid(), is(true));
    }

    @Test
    public void shouldReturnValidWhenEmptyInput() {
        LengthValidator lengthValidator = new LengthValidator(2);
        ValidationBean validationBean = lengthValidator.validate("");
        assertThat(validationBean.isValid(), is(true));
    }

}
