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
package com.thoughtworks.go.config.rules;

import com.thoughtworks.go.config.PipelineConfigs;
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
                final Allow allow = new Allow("refer", null, null);

                assertThat(allow.apply("unknown-action", null, null))
                        .isEqualTo(Result.SKIP);
            }

            @Test
            void ifEntityTypeDoesNotMatch() {
                final Allow allow = new Allow("refer", "pipeline_group", null);

                assertThat(allow.apply("refer", StageConfig.class, null))
                        .isEqualTo(Result.SKIP);
            }

            @Test
            void ifResourceDoesNotMatch() {
                final Allow allow = new Allow("refer", "pipeline_group", "group1");

                assertThat(allow.apply("refer", PipelineConfigs.class, "group2"))
                        .isEqualTo(Result.SKIP);
            }

            @Test
            void ifProvidedResourceDoesNotMatchTheWildcardInDirective() {
                final Allow allow = new Allow("refer", "pipeline_group", "group_*");

                assertThat(allow.apply("refer", PipelineConfigs.class, "groupA"))
                        .isEqualTo(Result.SKIP);
            }

            @ParameterizedTest
            @ValueSource(strings = {"my_group", "grpoup"})
            void ifResourceDoesNotMatchTheWildCardPatterAndActionAndTypeMatch(String resource) {
                final Allow allow = new Allow("refer", "pipeline_group", "gro*up*");

                assertThat(allow.apply("refer", PipelineConfigs.class, resource))
                        .isEqualTo(Result.SKIP);
            }
        }

        @Nested
        class shouldAllow {
            @Test
            void ifActionTypeAndResourceMatches() {
                final Allow allow = new Allow("refer", "pipeline_group", "group_1");

                assertThat(allow.apply("refer", PipelineConfigs.class, "group_1"))
                        .isEqualTo(Result.ALLOW);
            }

            @Test
            void ifResourceAndTypeMatchesWhenAllActionsAreAllowed() {
                final Allow allow = new Allow("*", "pipeline_group", "group_1");

                assertThat(allow.apply("any-action-is-allowed", PipelineConfigs.class, "group_1"))
                        .isEqualTo(Result.ALLOW);
            }

            @Test
            void ifResourceAndActionMatchesWhenAllTypesAreAllowed() {
                final Allow allow = new Allow("refer", "*", "group_1");

                assertThat(allow.apply("refer", PipelineConfigs.class, "group_1"))
                        .isEqualTo(Result.ALLOW);
            }

            @ParameterizedTest
            @ValueSource(strings = {"group", "my_group", "group_A", "gro---up", "gro...up"})
            void ifResourceMatchesTheWildCardAndActionAndTypeMatch(String resource) {
                final Allow allow = new Allow("refer", "pipeline_group", "*gro*up*");

                assertThat(allow.apply("refer", PipelineConfigs.class, resource))
                        .isEqualTo(Result.ALLOW);
            }
        }
    }
}
