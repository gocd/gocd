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
package com.thoughtworks.go.config;

import com.thoughtworks.go.config.exceptions.UnresolvedSecretParamException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class SecretParamsTest {

    @Nested
    class parse {
        @Test
        void shouldParseSecretParamStringToObject() {
            String strWithSecretParams = "{{SECRET:[secret_config_id][lookup_username]}}:{{SECRET:[secret_config_id][lookup_password]}}/foo-bar";
            SecretParams secretParams = SecretParams.parse(strWithSecretParams);

            assertThat(secretParams)
                    .hasSize(2)
                    .contains(newParam("lookup_username"), newParam("lookup_password"));
        }

        @Test
        void shouldReturnAnEmptyListInAbsenceOfSecretParamsInString() {
            String strWithSecretParams = "username-{{SECRET:[secret_config_id]}}-foo.com/f}oo.git";
            SecretParams secretParams = SecretParams.parse(strWithSecretParams);

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
            final SecretParams list1 = new SecretParams(newParam("lookup_username"));
            final SecretParams list2 = new SecretParams(newParam("lookup_password"));

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
                    newParam("secret_config_id_1", "lookup_username"),
                    newParam("secret_config_id_1", "lookup_password"),
                    newParam("secret_config_id_2", "lookup_bucket_name"),
                    newParam("secret_config_id_2", "lookup_access_key"),
                    newParam("secret_config_id_2", "lookup_secret_key"),
                    newParam("secret_config_id_foo", "lookup_foo")
            );

            final Map<String, SecretParams> groupedBySecretConfigId = allSecretParams.groupBySecretConfigId();

            assertThat(groupedBySecretConfigId).hasSize(3)
                    .containsEntry("secret_config_id_1", new SecretParams(
                            newParam("secret_config_id_1", "lookup_username"),
                            newParam("secret_config_id_1", "lookup_password")
                    ))
                    .containsEntry("secret_config_id_2", new SecretParams(
                            newParam("secret_config_id_2", "lookup_bucket_name"),
                            newParam("secret_config_id_2", "lookup_access_key"),
                            newParam("secret_config_id_2", "lookup_secret_key")
                    ))
                    .containsEntry("secret_config_id_foo", new SecretParams(
                            newParam("secret_config_id_foo", "lookup_foo")
                    ));


        }
    }

    @Nested
    class Merge {
        @Test
        void shouldMergeGivenSecretParamsToTheCurrentList() {
            final SecretParams mergeInThis = new SecretParams(newParam("lookup_username"));
            final SecretParams paramsToMerge = new SecretParams(newParam("lookup_password"));

            mergeInThis.merge(paramsToMerge);

            assertThat(paramsToMerge).hasSize(1);
            assertThat(mergeInThis).hasSize(2)
                    .contains(
                            newParam("lookup_username"),
                            newParam("lookup_password")
                    );
        }
    }

    @Nested
    class toFlatSecretParams {
        @Test
        void shouldCollectMultipleSecretParamsToFlatList() {
            final SecretParams listOne = new SecretParams(newParam("lookup_username"));
            final SecretParams listTwo = new SecretParams(newParam("lookup_password"));

            final SecretParams flattenList = Stream.of(listOne, listTwo).collect(SecretParams.toFlatSecretParams());

            assertThat(flattenList).hasSize(2)
                    .contains(
                            newParam("lookup_username"),
                            newParam("lookup_password")
                    );
        }
    }

    @Nested
    class substitute {
        @Test
        void shouldSubstituteSecretParamsValueInGiveString() {
            final SecretParams allSecretParams = new SecretParams(
                    newResolvedParam("username", "some-username"),
                    newResolvedParam("password", "some-password")
            );

            assertThat(allSecretParams.substitute("{{SECRET:[secret_config_id][username]}}"))
                    .isEqualTo("some-username");

            assertThat(allSecretParams.substitute("{{SECRET:[secret_config_id][username]}}@{{SECRET:[secret_config_id][password]}}"))
                    .isEqualTo("some-username@some-password");

        }

        @Test
        void shouldThrowWhenSecretParamIsUsedBeforeItIsResolved() {
            final SecretParams secretParams = new SecretParams(newParam("username"));

            assertThatCode(() -> secretParams.substitute("{{SECRET:[secret_config_id][username]}}"))
                    .isInstanceOf(UnresolvedSecretParamException.class)
                    .hasMessage("SecretParam 'username' is used before it is resolved.");

        }
    }

    @Nested
    class Mask {
        @Test
        void shouldMaskSecretParamsInGivenString() {
            final String originalValue = "bob:{{SECRET:[secret_config_id][username]}}";
            final SecretParams secretParams = new SecretParams(newParam("username"));

            final String maskedValue = secretParams.mask(originalValue);

            assertThat(maskedValue).isEqualTo("bob:******");
        }
    }

    private SecretParam newResolvedParam(String key, String value) {
        final SecretParam secretParam = newParam("secret_config_id", key);
        secretParam.setValue(value);
        return secretParam;
    }


    private SecretParam newParam(String secretConfigId, String key) {
        return new SecretParam(secretConfigId, key);
    }

    private SecretParam newParam(String key) {
        return newParam("secret_config_id", key);
    }
}