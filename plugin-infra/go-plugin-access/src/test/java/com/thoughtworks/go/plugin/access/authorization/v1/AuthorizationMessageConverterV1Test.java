/*
 * Copyright 2023 Thoughtworks, Inc.
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
package com.thoughtworks.go.plugin.access.authorization.v1;

import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.domain.config.ConfigurationProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

public class AuthorizationMessageConverterV1Test {

    private AuthorizationMessageConverterV1 testSubject;

    @BeforeEach
    void beforeEach() {
        this.testSubject = new AuthorizationMessageConverterV1();
    }

    @Test
    void getProcessRoleConfigsResponseBody() {
        String json = testSubject.getProcessRoleConfigsResponseBody(List.of(new PluginRoleConfig("blackbird", "ldap", create("foo", false, "bar"))));

        assertThatJson(json).isEqualTo("[{\"name\":\"blackbird\",\"configuration\":{\"foo\":\"bar\"}}]");
    }

    @Test
    void grantAccessRequestBodyTests() {
        String json = testSubject.grantAccessRequestBody(List.of(new SecurityAuthConfig("github", "cd.go.github", create("url", false, "some-url"))), Map.of("foo", "bar"));

        String expectedRequestBody = "{\n" +
            "  \"auth_configs\": [\n" +
            "    {\n" +
            "      \"id\": \"github\",\n" +
            "      \"configuration\": {\n" +
            "        \"url\": \"some-url\"\n" +
            "      }\n" +
            "    }\n" +
            "  ],\n" +
            "  \"auth_session\": {\n" +
            "       \"foo\": \"bar\"\n" +
            "  }\n" +
            "}";
        assertThatJson(json).isEqualTo(expectedRequestBody);
    }
}
