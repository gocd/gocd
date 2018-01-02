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

package com.thoughtworks.go.server.service.result;

import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.serverhealth.HealthStateScope;
import com.thoughtworks.go.serverhealth.HealthStateType;
import org.junit.Test;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        result.unauthorized(LocalizedMessage.cannotViewPipeline("whateva"), HealthStateType.general(HealthStateScope.GLOBAL));
        assertThat(result.isSuccessful(), is(false));
    }

    @Test
    public void shouldNotReturnSuccessfulIfNotFound() {
        LocalizedOperationResult result = new HttpLocalizedOperationResult();
        result.notFound(LocalizedMessage.cannotViewPipeline("whateva"), HealthStateType.general(HealthStateScope.GLOBAL));
        assertThat(result.isSuccessful(), is(false));
    }

    @Test
    public void shouldReturn404AndLocalizeWhenNotFound() {
        Localizable message = mock(Localizable.class);
        Localizer localizer = mock(Localizer.class);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(message.localize(localizer)).thenReturn("Seriously, whateva");
        result.notFound(message, HealthStateType.general(HealthStateScope.GLOBAL));

        assertThat(result.httpCode(), is(404));
        assertThat(result.message(localizer), is("Seriously, whateva"));
    }

    @Test
    public void shouldReturn401WhenNotAuthorized() {
        Localizable message = LocalizedMessage.cannotViewPipeline("whateva");
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        result.unauthorized(message, HealthStateType.general(HealthStateScope.GLOBAL));

        assertThat(result.httpCode(), is(401));
    }

    @Test
    public void shouldReturnMessageAndStatus501WhenNotImplemented() {
        Localizable message = mock(Localizable.class);
        Localizer localizer = mock(Localizer.class);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        when(message.localize(localizer)).thenReturn("Seriously, whateva");
        result.notImplemented(message);

        assertThat(result.httpCode(), is(501));
    }

    @Test
    public void shouldReturnAccepted() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Localizable localizable = LocalizedMessage.string("KEY");
        result.accepted(localizable);

        assertThat(result.httpCode(), is(202));
        assertThat(result.localizable(), is(localizable));
    }

    @Test
    public void shouldNotFailWhenNoMessageSet() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        Localizer localizer = mock(Localizer.class);
        assertThat(result.message(localizer), is(nullValue()));
    }

    @Test
    public void shouldTestEquality() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        result.unauthorized(LocalizedMessage.string("KEY"), HealthStateType.general(HealthStateScope.GLOBAL));

        HttpLocalizedOperationResult equalResult = new HttpLocalizedOperationResult();
        equalResult.unauthorized(LocalizedMessage.string("KEY"), HealthStateType.general(HealthStateScope.GLOBAL));

        assertThat(result, is(equalResult));
    }

    @Test
    public void shouldTestInEquality() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        result.unauthorized(LocalizedMessage.string("KEY"), HealthStateType.general(HealthStateScope.GLOBAL));

        HttpLocalizedOperationResult equalResult = new HttpLocalizedOperationResult();
        equalResult.unauthorized(LocalizedMessage.string("ANOTHERKEY"), HealthStateType.general(HealthStateScope.GLOBAL));

        assertThat(result, is(not(equalResult)));
    }

    @Test
    public void shouldTellWhetherTheMessageIsPresentOrNot() throws Exception {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        assertFalse(result.hasMessage());
        result.setMessage(LocalizedMessage.string("Some Message"));
        assertTrue(result.hasMessage());
    }
}
