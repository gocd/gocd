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
package com.thoughtworks.go.plugin.access.secrets.v1;

import net.javacrumbs.jsonunit.fluent.JsonFluentAssert;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

class SecretsMessageConverterV1Test {

    @Nested
    class lookupSecretsRequestBody {
        @Test
        void shouldSendLookupKeysWithSecretConfiguration() {
            final String requestBody = new SecretsMessageConverterV1().lookupSecretsRequestBody(new LinkedHashSet<>(asList("username", "password")), singletonMap("FilePath", "/var/lib/secret.config"));

            JsonFluentAssert.assertThatJson(requestBody)
                    .isEqualTo("{\n" +
                            "  \"configuration\": {\n" +
                            "    \"FilePath\": \"/var/lib/secret.config\"\n" +
                            "  },\n" +
                            "  \"keys\": [\n" +
                            "    \"username\",\n" +
                            "    \"password\"\n" +
                            "  ]\n" +
                            "}");
        }
    }
}