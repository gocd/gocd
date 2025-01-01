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
package com.thoughtworks.go.domain.materials;

import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.domain.materials.ValidationBean.valid;
import static org.assertj.core.api.Assertions.assertThat;

public class ValidationBeanTest {

    @Test
    public void shouldBeInvalidIfMessageIsEmptyOrNull() {
        assertThat(ValidationBean.notValid("").isValid()).isFalse();
        assertThat(ValidationBean.notValid((String) null).isValid()).isFalse();
        assertThat(ValidationBean.notValid(new Exception()).isValid()).isFalse();
    }

    @Test
    public void shouldBeInvalidIfMessageIsNotEmpty() {
        assertThat(ValidationBean.notValid("With a message").isValid()).isFalse();
    }

    @Test
    public void shouldHaveBasicErrorIfMessageIsEmpty() {
        assertThat(ValidationBean.notValid("").getError()).isEqualTo("");
        assertThat(ValidationBean.notValid((String) null).isValid()).isFalse();
        assertThat(ValidationBean.notValid(new Exception()).isValid()).isFalse();
    }

    /**
     * This is a straight test of what is already there.
     * We are NOT sure that this is the correct behaviour.
     * We think it was there for SVNKIT and can now be removed.
     */
    @Test
    public void shouldStripOutExceptionText() {
        assertThat(ValidationBean.notValid(new Exception("SVNKITException:   The actual message")).getError())
            .isEqualTo("The actual message");
    }

    @Test
    public void shouldSeeOriginalExceptionMessage() {
        String message = "A message.";
        assertThat(ValidationBean.notValid(new Exception(message)).getError()).isEqualTo(message);
        assertThat(ValidationBean.notValid(message).getError()).isEqualTo(message);
    }

    @Test
    public void shouldBeValid() {
        assertThat(valid().isValid()).isTrue();
        assertThat(valid().getError()).isEqualTo("");
        ValidationBean bean = valid();
        assertThat(bean.isValid()).isTrue();
        assertThat(bean.toJson().get("isValid")).isEqualTo("true");
    }
}
