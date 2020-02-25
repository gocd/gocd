/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class ServerUrlValidatorTest {

    @Test
    void shouldValidateByParsingUrl() {
        assertThatCode(() -> {
            new ServerUrlValidator().validate("foo", "bad-url");
        })
                .isOfAnyClassIn(ParameterException.class)
                .hasMessageContaining("is not a valid url");
    }

    @Test
    void shouldNotAllowUrlEndingWithoutGo() {
        assertThatCode(() -> {
            new ServerUrlValidator().validate("foo", "https://example.com");
        })
                .isOfAnyClassIn(ParameterException.class)
                .hasMessageContaining("must end with '/go' (http://localhost:8153/go)");
    }

    @Test
    void shouldAllowHttpOrHttpsUrlEndingWithGo() {
        new ServerUrlValidator().validate("foo", "https://example.com/go");
        new ServerUrlValidator().validate("foo", "http://example.com/go");
    }

    @Test
    void shouldNotAllowNonHttpUrlEndingWithGo() {
        assertThatCode(() -> {
            new ServerUrlValidator().validate("foo", "file://example.com/go");
        })
                .isOfAnyClassIn(ParameterException.class)
                .hasMessageContaining("foo must use http or https protocol");
    }
}
