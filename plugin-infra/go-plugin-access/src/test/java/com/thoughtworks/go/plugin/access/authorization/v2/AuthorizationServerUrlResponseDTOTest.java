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

package com.thoughtworks.go.plugin.access.authorization.v2;

import com.thoughtworks.go.plugin.domain.authorization.AuthorizationServerUrlResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class AuthorizationServerUrlResponseDTOTest {

    @Test
    public void shouldDeserializeFromJSON() {
        String json = "{" +
            "  \"authorization_server_url\": \"https://example.tld/auth\"," +
            "  \"auth_session\": {" +
            "    \"foo\": \"bar\"," +
            "    \"apple\": \"banana\"" +
            "  }" +
            "}";

        AuthorizationServerUrlResponseDTO dto = AuthorizationServerUrlResponseDTO.fromJSON(json);

        assertThat(dto.getAuthorizationServerUrl(), is("https://example.tld/auth"));
        assertThat(dto.getAuthSession(), is(Map.of(
            "foo", "bar",
            "apple", "banana"
        )));
    }

    @Nested
    class ToDomainModelTests {

        @Test
        public void shouldConvertToDomainModel() {
            AuthorizationServerUrlResponseDTO dto = new AuthorizationServerUrlResponseDTO("https://example.tld/auth", Map.of(
                "foo", "bar",
                "apple", "banana"
            ));

            AuthorizationServerUrlResponse domainModel = dto.toDomainModel();

            assertThat(domainModel.getAuthorizationServerUrl(), is("https://example.tld/auth"));
            assertThat(domainModel.getAuthSession(), is(Map.of(
                "foo", "bar",
                "apple", "banana"
            )));
        }

        @Test
        public void shouldConvertToDomainModel_DefaultingAuthSessionToEmptyMap_WhenNullOnDTO() {
            AuthorizationServerUrlResponseDTO dto = new AuthorizationServerUrlResponseDTO("https://example.tld/auth", null);

            AuthorizationServerUrlResponse domainModel = dto.toDomainModel();

            assertThat(domainModel.getAuthorizationServerUrl(), is("https://example.tld/auth"));
            assertThat(domainModel.getAuthSession(), is(Map.of()));
        }
    }

}