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
package com.thoughtworks.go.config.policy;

import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.StageConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class DenyTest extends AbstractDirectiveTest {
    //runs the test from AbstractDirectiveTest for this Directive
    @Override
    Directive getDirective(String action, String type, String resource) {
        return new Deny(action, type, resource);
    }

    @Nested
    class apply {
        @Nested
        class shouldSkip {
            @Test
            void ifActionDoesNotMatch() {
                final Deny deny = new Deny("view", null, null);

                assertThat(deny.apply("unknown-action", null, null, null))
                        .isEqualTo(Result.SKIP);
            }

            @Test
            void ifEntityTypeDoesNotMatch() {
                final Deny deny = new Deny("view", "environment", null);

                assertThat(deny.apply("view", StageConfig.class, null, null))
                        .isEqualTo(Result.SKIP);
            }

            @Test
            void denyToViewConfigIfEntityActionIsAdminister() {
                final Deny deny = new Deny("administer", "environment", null);

                assertThat(deny.apply("view", StageConfig.class, null, null))
                        .isEqualTo(Result.SKIP);
            }

            @Test
            void ifResourceDoesNotMatch() {
                final Deny deny = new Deny("view", "environment", "group1");

                assertThat(deny.apply("view", EnvironmentConfig.class, "group2", null))
                        .isEqualTo(Result.SKIP);
            }

            @Test
            void ifProvidedResourceDoesNotMatchTheWildcardInDirective() {
                final Deny deny = new Deny("view", "environment", "group_*");

                assertThat(deny.apply("view", EnvironmentConfig.class, "groupA", null))
                        .isEqualTo(Result.SKIP);
            }

            @ParameterizedTest
            @ValueSource(strings = {"my_group", "grpoup"})
            void ifResourceDoesNotMatchTheWildCardPatterAndActionAndTypeMatch(String resource) {
                final Deny deny = new Deny("view", "environment", "gro*up*");

                assertThat(deny.apply("view", EnvironmentConfig.class, resource, null))
                        .isEqualTo(Result.SKIP);
            }
        }

        @Nested
        class shouldDeny {
            @Test
            void ifActionTypeAndResourceMatches() {
                final Deny deny = new Deny("view", "environment", "env_1");

                assertThat(deny.apply("view", EnvironmentConfig.class, "env_1", null))
                        .isEqualTo(Result.DENY);
            }

            @Test
            void ifResourceAndActionMatchesWhenAllTypesAreAllowed() {
                final Deny deny = new Deny("view", "*", "env_1");

                assertThat(deny.apply("view", EnvironmentConfig.class, "env_1", null))
                        .isEqualTo(Result.DENY);
            }

            @ParameterizedTest
            @ValueSource(strings = {"group", "my_group", "group_A", "gro---up", "gro...up"})
            void ifResourceMatchesTheWildCardAndActionAndTypeMatch(String resource) {
                final Deny deny = new Deny("view", "environment", "*gro*up*");

                assertThat(deny.apply("view", EnvironmentConfig.class, resource, null))
                        .isEqualTo(Result.DENY);
            }
        }
    }
}
