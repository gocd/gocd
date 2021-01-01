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
package com.thoughtworks.go.server.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AccessTokenFilterTest {

    @Test
    void shouldParseKnownEnumValues() {
        assertThat(AccessTokenFilter.fromString("all")).isEqualTo(AccessTokenFilter.all);
        assertThat(AccessTokenFilter.fromString("revoked")).isEqualTo(AccessTokenFilter.revoked);
        assertThat(AccessTokenFilter.fromString("active")).isEqualTo(AccessTokenFilter.active);
    }

    @Test
    void shouldReturnDefaultEnumWhenNoValueIsSpecified() {
        assertThat(AccessTokenFilter.fromString("  ")).isEqualTo(AccessTokenFilter.active);
        assertThat(AccessTokenFilter.fromString("\n\t  \r")).isEqualTo(AccessTokenFilter.active);
        assertThat(AccessTokenFilter.fromString(null)).isEqualTo(AccessTokenFilter.active);
    }

}
