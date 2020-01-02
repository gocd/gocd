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
package com.thoughtworks.go.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SecretParamTest {

    @Nested
    class parse {
        @Test
        void shouldParseSecretParamStringToObject() {
            String materialUrl = "http://{{SECRET:[secret_config_id][lookup_username]}}:{{SECRET:[secret_config_id][lookup_password]}}@foo.com/foo.git";
            SecretParams secretParams = SecretParams.parse(materialUrl);

            assertThat(secretParams)
                    .hasSize(2)
                    .contains(new SecretParam("secret_config_id", "lookup_username"))
                    .contains(new SecretParam("secret_config_id", "lookup_password"));
        }

        @Test
        void shouldReturnAnEmptyListInAbsenceOfSecretParamsInString() {
            String materialUrl = "http://username:{{SECRET:[secret_config_id]}}@foo.com/foo.git";
            SecretParams secretParams = SecretParams.parse(materialUrl);

            assertThat(secretParams).isEmpty();
        }

        @Test
        void shouldReturnAnEmptyListForNull() {
            assertThat(SecretParams.parse(null)).isEmpty();
        }
    }
}