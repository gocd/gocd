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

package com.thoughtworks.go.api;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiVersionTest {

    @Test
    public void shouldCheckValidity() {
        assertThat(ApiVersion.isValid("application/vnd.go.cd+json")).isEqualTo(true);
        assertThat(ApiVersion.isValid("application/vnd.go.cd.v1+json")).isEqualTo(true);
        assertThat(ApiVersion.isValid("application/vnd.go.cd.v0+json")).isEqualTo(false);
        assertThat(ApiVersion.isValid("something/else")).isEqualTo(false);
    }

    @Test
    public void shouldParseFromMimeType() {
        assertThat(ApiVersion.parse("application/vnd.go.cd.v1+json")).isEqualTo(ApiVersion.v1);
        assertThat(ApiVersion.parse("application/vnd.go.cd.v10+json")).isEqualTo(ApiVersion.v10);
        assertThat(ApiVersion.parse("something/else")).isNull();
    }

    @Test
    public void shouldGenerateMimeType() {
        assertThat(ApiVersion.v1.mimeType()).isEqualTo("application/vnd.go.cd.v1+json");
        assertThat(ApiVersion.v10.mimeType()).isEqualTo("application/vnd.go.cd.v10+json");
    }
}