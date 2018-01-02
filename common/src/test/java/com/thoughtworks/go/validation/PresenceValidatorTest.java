/*
 * Copyright 2015 ThoughtWorks, Inc.
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
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class PresenceValidatorTest {
    @Test
    public void shouldNotRenderErrorWhenStringIsNotBlank() throws Exception {
        ValidationBean validation = new PresenceValidator("String must be non-blank").validate("foo");
        assertThat(validation.isValid(), is(true));
    }

    @Test
    public void shouldRenderErrorWhenStringIsBlank() throws Exception {
        ValidationBean validation = new PresenceValidator("String must be non-blank").validate("");
        assertThat(validation.isValid(), is(false));

        validation = new PresenceValidator("String must be non-blank").validate(null);
        assertThat(validation.isValid(), is(false));

        validation = new PresenceValidator("String must be non-blank").validate("   ");
        assertThat(validation.isValid(), is(false));

        validation = new PresenceValidator("String must be non-blank").validate(" \t\n  ");
        assertThat(validation.isValid(), is(false));
    }
}
