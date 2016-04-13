/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent.common;

import com.beust.jcommander.ParameterException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ServerUrlValidatorTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void shouldValidateByParsingUrl() throws Exception {
        expectedEx.expect(ParameterException.class);
        expectedEx.expectMessage("is not a valid url");
        new ServerUrlValidator().validate("foo", "bad-url");
    }

    @Test
    public void shouldNotAllowUrlEndingWithoutGo() throws Exception {
        expectedEx.expect(ParameterException.class);
        expectedEx.expectMessage("must end with '/go' (https://localhost:8154/go)");
        new ServerUrlValidator().validate("foo", "https://example.com");
    }

    @Test
    public void shouldAllowSslUrlEndingWithGo() throws Exception {
        new ServerUrlValidator().validate("foo", "https://example.com/go");
    }

    @Test
    public void shouldNotAllowPlainTextUrlEndingWithGo() throws Exception {
        expectedEx.expect(ParameterException.class);
        expectedEx.expectMessage("must be an HTTPS url and must begin with https://");
        new ServerUrlValidator().validate("foo", "http://example.com/go");
    }
}
