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
package com.thoughtworks.go.config.policy;

import com.thoughtworks.go.config.EnvironmentConfig;
import com.thoughtworks.go.config.StageConfig;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class AllowTest extends AbstractDirectiveTest {
    //runs the test from AbstractDirectiveTest for this Directive
    @Override
    Directive getDirective(String action, String type, String resource) {
        return new Allow(action, type, resource);
    }

    @Nested
    class apply {
        @Nested
        class shouldSkip {
            @Test
            void ifActionDoesNotMatch() {
                final Allow allow = new Allow("view", null, null);

                assertThat(allow.apply("unknown-action", null, null, null))
                        .isEqualTo(Result.SKIP);
            }

            @Test
            void ifEntityTypeDoesNotMatch() {
                final Allow allow = new Allow("view", "environment", null);

                assertThat(allow.apply("view", StageConfig.class, null, null))
                        .isEqualTo(Result.SKIP);
            }

            @Test
            void ifResourceDoesNotMatch() {
                final Allow allow = new Allow("view", "environment", "group1");

                assertThat(allow.apply("view", EnvironmentConfig.class, "group2", null))
                        .isEqualTo(Result.SKIP);
            }

            @Test
            void ifProvidedResourceDoesNotMatchTheWildcardInDirective() {
                final Allow allow = new Allow("view", "environment", "group_*");

                assertThat(allow.apply("view", EnvironmentConfig.class, "groupA", null))
                        .isEqualTo(Result.SKIP);
            }

            @ParameterizedTest
            @ValueSource(strings = {"my_group", "grpoup"})
            void ifResourceDoesNotMatchTheWildCardPatterAndActionAndTypeMatch(String resource) {
                final Allow allow = new Allow("view", "environment", "gro*up*");

                assertThat(allow.apply("view", EnvironmentConfig.class, resource, null))
                        .isEqualTo(Result.SKIP);
            }
        }

        @Nested
        class shouldAllow {
            @Test
            void ifActionTypeAndResourceMatches() {
                final Allow allow = new Allow("view", "environment", "env_1");

                assertThat(allow.apply("view", EnvironmentConfig.class, "env_1", null))
                        .isEqualTo(Result.ALLOW);
            }

            @Test
            void toViewConfigIfActionIsAdminister() {
                final Allow allow = new Allow("administer", "environment", "env_1");

                assertThat(allow.apply("view", EnvironmentConfig.class, "env_1", null))
                        .isEqualTo(Result.ALLOW);
            }

            @Test
            void ifResourceAndActionMatchesWhenAllTypesAreAllowed() {
                final Allow allow = new Allow("view", "*", "env_1");

                assertThat(allow.apply("view", EnvironmentConfig.class, "env_1", null))
                        .isEqualTo(Result.ALLOW);
            }

            @ParameterizedTest
            @ValueSource(strings = {"group", "my_group", "group_A", "gro---up", "gro...up"})
            void ifResourceMatchesTheWildCardAndActionAndTypeMatch(String resource) {
                final Allow allow = new Allow("view", "environment", "*gro*up*");

                assertThat(allow.apply("view", EnvironmentConfig.class, resource, null))
                        .isEqualTo(Result.ALLOW);
            }
        }
    }
}
