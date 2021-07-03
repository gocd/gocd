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
package com.thoughtworks.go.server.service.result;

import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;

public class HttpLocalizedOperationResultTest {

    @Test
    public void shouldReturnSuccessfulIfNothingChanged() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        assertThat(result.isSuccessful(), is(true));
        assertThat(result.httpCode(), is(200));
    }

    @Test
    public void shouldNotReturnSuccessfulIfUnauthorized() {
        LocalizedOperationResult result = new HttpLocalizedOperationResult();
        result.forbidden("blah", HealthStateType.general(HealthStateScope.GLOBAL));
        assertThat(result.isSuccessful(), is(false));
    }

    @Test
    public void shouldNotReturnSuccessfulIfNotFound() {
        LocalizedOperationResult result = new HttpLocalizedOperationResult();
        result.notFound("blah", HealthStateType.general(HealthStateScope.GLOBAL));
        assertThat(result.isSuccessful(), is(false));
    }

    @Test
    public void shouldReturn404AndLocalizeWhenNotFound() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        result.notFound("foo", HealthStateType.general(HealthStateScope.GLOBAL));

        assertThat(result.httpCode(), is(404));
        assertThat(result.message(), is("foo"));
    }

    @Test
    public void shouldReturn403WhenUserDoesNotHavePermissionToAccessResource() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        result.forbidden("blah", HealthStateType.general(HealthStateScope.GLOBAL));

        assertThat(result.httpCode(), is(403));
    }

    @Test
    public void shouldReturnMessageAndStatus501WhenNotImplemented() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        result.notImplemented(null);

        assertThat(result.httpCode(), is(501));
    }

    @Test
    public void shouldReturnAccepted() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        result.accepted("foo");

        assertThat(result.httpCode(), is(202));
        assertThat(result.message(), is("foo"));
    }

    @Test
    public void shouldNotFailWhenNoMessageSet() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        assertThat(result.message(), is(nullValue()));
    }

    @Test
    public void shouldTellWhetherTheMessageIsPresentOrNot() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        assertFalse(result.hasMessage());
        result.setMessage("foo");
        assertTrue(result.hasMessage());
    }
}
