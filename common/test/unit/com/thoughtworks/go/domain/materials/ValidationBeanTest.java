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

package com.thoughtworks.go.domain.materials;

import org.hamcrest.core.Is;
import org.json.JSONException;
import org.junit.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static com.thoughtworks.go.domain.materials.ValidationBean.valid;
import static com.thoughtworks.go.server.web.JsonRenderer.render;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ValidationBeanTest {

    @Test
    public void shouldBeInvalidIfMessageIsEmptyOrNull() {
        assertThat(ValidationBean.notValid("").isValid(), is(false));
        assertThat(ValidationBean.notValid((String) null).isValid(), is(false));
        assertThat(ValidationBean.notValid(new Exception()).isValid(), is(false));
    }

    @Test
    public void shouldBeInvalidIfMessageIsNotEmpty() {
        assertThat(ValidationBean.notValid("With a message").isValid(), is(false));
    }

    @Test
    public void shouldHaveBasicErrorIfMessageIsEmpty() {
        assertThat(ValidationBean.notValid("").getError(), is(""));
        assertThat(ValidationBean.notValid((String) null).isValid(), is(false));
        assertThat(ValidationBean.notValid(new Exception()).isValid(), is(false));
    }

    /**
     * This is a straight test of what is already there.
     * We are NOT sure that this is the correct behaviour.
     * We think it was there for SVNKIT and can now be removed.
     */
    @Test
    public void shouldStripOutExceptionText() {
        assertThat(
                ValidationBean.notValid(new Exception("SVNKITException:   The actual message")).getError(),
                is("The actual message")
        );
    }

    @Test
    public void shouldSeeOriginalExceptionMessage() {
        String message = "A message.";
        assertThat(ValidationBean.notValid(new Exception(message)).getError(), is(message));
        assertThat(ValidationBean.notValid(message).getError(), is(message));
    }

    @Test
    public void shouldBeValid() {
        assertThat(valid().isValid(), is(true));
        assertThat(valid().getError(), is(""));
        ValidationBean bean = valid();
        assertThat(bean.isValid(), is(true));
        assertThat(bean.toJson().get("isValid"), Is.<Object>is("true"));
    }

    @Test
    public void shouldBeAbleToSerializeToJson() throws JSONException {
        ValidationBean bean = ValidationBean.notValid("ErrorMessage");
        String output = render(bean);
        JSONAssert.assertEquals(output, "{ \"isValid\": \"false\",\"error\": \"ErrorMessage\" }", true);
    }
}
