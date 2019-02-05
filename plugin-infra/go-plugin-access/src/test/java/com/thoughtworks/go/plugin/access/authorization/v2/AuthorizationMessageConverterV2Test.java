/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.PluginRoleConfig;
import com.thoughtworks.go.config.SecurityAuthConfig;
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;

public class AuthorizationMessageConverterV2Test {
    private AuthorizationMessageConverterV2 converter;

    @BeforeEach
    public void setup() {
        converter = new AuthorizationMessageConverterV2();
    }

    @Test
    public void getProcessRoleConfigsResponseBody() throws Exception {
        String json = converter.getProcessRoleConfigsResponseBody(singletonList(new PluginRoleConfig("blackbird", "ldap", create("foo", false, "bar"))));
        assertThatJson("[{\"name\":\"blackbird\",\"configuration\":{\"foo\":\"bar\"}}]").isEqualTo(json);
    }

    @Test
    public void shouldReturnRequestBodyForGetUserRolesRequest() {
        List<SecurityAuthConfig> authConfigs = asList(new SecurityAuthConfig("p1", "ldap", ConfigurationPropertyMother.create("key1", false, "value2")));
        List<PluginRoleConfig> roleConfigs = asList(new PluginRoleConfig("role1", "p1", ConfigurationPropertyMother.create("key2", false, "value2")));


        String requestBody = converter.authenticateUserRequestBody("foo", authConfigs, roleConfigs);

        assertThatJson(requestBody).isEqualTo("{\n" +
                "  \"auth_configs\": [\n" +
                "    {\n" +
                "      \"configuration\": {\n" +
                "        \"key1\": \"value2\"\n" +
                "      },\n" +
                "      \"id\": \"p1\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"username\": \"foo\",\n" +
                "  \"role_configs\": [\n" +
                "    {\n" +
                "      \"auth_config_id\": \"p1\",\n" +
                "      \"configuration\": {\n" +
                "        \"key2\": \"value2\"\n" +
                "      },\n" +
                "      \"name\": \"role1\"\n" +
                "    }\n" +
                "  ]\n" +
                "}");
    }
}
