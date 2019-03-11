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

package com.thoughtworks.go.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecretParamsTest {

    @Nested
    class parse {
        @Test
        void shouldParseSecretParamStringToObject() {
            String materialUrl = "http://#{SECRET[secret_config_id][lookup_username]}:#{SECRET[secret_config_id][lookup_password]}@foo.com/foo.git";
            SecretParams secretParams = SecretParams.parse(materialUrl);

            assertThat(secretParams)
                    .hasSize(2)
                    .contains(new SecretParam("secret_config_id", "lookup_username"))
                    .contains(new SecretParam("secret_config_id", "lookup_password"));
        }

        @Test
        void shouldReturnAnEmptyListInAbsenceOfSecretParamsInString() {
            String materialUrl = "http://username:#{SECRET[secret_config_id]}@foo.com/foo.git";
            SecretParams secretParams = SecretParams.parse(materialUrl);

            assertThat(secretParams).isEmpty();
        }

        @Test
        void shouldReturnAnEmptyListForNull() {
            assertThat(SecretParams.parse(null)).isEmpty();
        }
    }

    @Nested
    class Union {
        @Test
        void shouldMergeToSecretParamsListAndReturnAsNewList() {
            final SecretParams list1 = new SecretParams(new SecretParam("secret_config_id", "lookup_username"));
            final SecretParams list2 = new SecretParams(new SecretParam("secret_config_id", "lookup_password"));

            final SecretParams merged = SecretParams.union(list1, list2);

            assertThat(list1).hasSize(1);
            assertThat(list1).hasSize(1);
            assertThat(merged).hasSize(2)
                    .contains(new SecretParam("secret_config_id", "lookup_username"),
                            new SecretParam("secret_config_id", "lookup_password"));
        }
    }

    @Nested
    class GroupBySecretConfigId {
        @Test
        void shouldGroupSecretParams() {
            final SecretParams allSecretParams = new SecretParams(
                    new SecretParam("secret_config_id_1", "lookup_username"),
                    new SecretParam("secret_config_id_1", "lookup_password"),
                    new SecretParam("secret_config_id_2", "lookup_bucket_name"),
                    new SecretParam("secret_config_id_2", "lookup_access_key"),
                    new SecretParam("secret_config_id_2", "lookup_secret_key"),
                    new SecretParam("secret_config_id_foo", "lookup_foo")
            );

            final Map<String, SecretParams> groupedBySecretConfigId = allSecretParams.groupBySecretConfigId();

            assertThat(groupedBySecretConfigId).hasSize(3)
                    .containsEntry("secret_config_id_1", new SecretParams(
                            new SecretParam("secret_config_id_1", "lookup_username"),
                            new SecretParam("secret_config_id_1", "lookup_password")
                    ))
                    .containsEntry("secret_config_id_2", new SecretParams(
                            new SecretParam("secret_config_id_2", "lookup_bucket_name"),
                            new SecretParam("secret_config_id_2", "lookup_access_key"),
                            new SecretParam("secret_config_id_2", "lookup_secret_key")
                    ))
                    .containsEntry("secret_config_id_foo", new SecretParams(
                            new SecretParam("secret_config_id_foo", "lookup_foo")
                    ));


        }
    }

    @Nested
    class Merge {
        @Test
        void shouldMergeGivenSecretParamsToTheCurrentList() {
            final SecretParams mergeInThis = new SecretParams(new SecretParam("secret_config_id", "lookup_username"));
            final SecretParams paramsToMerge = new SecretParams(new SecretParam("secret_config_id", "lookup_password"));

            mergeInThis.merge(paramsToMerge);

            assertThat(paramsToMerge).hasSize(1);
            assertThat(mergeInThis).hasSize(2)
                    .contains(new SecretParam("secret_config_id", "lookup_username"),
                            new SecretParam("secret_config_id", "lookup_password"));
        }
    }

}