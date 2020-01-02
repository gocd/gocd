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

import com.thoughtworks.go.config.DelegatingValidationContext;
import com.thoughtworks.go.config.ValidationContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class AbstractDirectiveTest {
    @Nested
    class Validate {
        @Test
        void shouldAddErrorIfActionIsNotSet() {
            Directive directive = getDirective(null, "environment", "test-resource");

            directive.validate(rulesValidationContext(singletonList("view"), singletonList("environment")));

            assertThat(directive.errors()).hasSize(1);
            assertThat(directive.errors().get("action"))
                    .hasSize(1)
                    .contains("Invalid action, must be one of [view].");
        }

        @Test
        void shouldAddErrorIfActionIsSetToOtherThanAllowedActions() {
            Directive directive = getDirective("invalid", "environment", "test-resource");

            directive.validate(rulesValidationContext(singletonList("view"), singletonList("environment")));

            assertThat(directive.errors()).hasSize(1);

            assertThat(directive.errors().get("action"))
                    .hasSize(1)
                    .contains("Invalid action, must be one of [view].");
        }

        @Test
        void shouldNotAllowActionToHaveWildcard() {
            Directive directive = getDirective("*", "environment", "test-resource");

            directive.validate(rulesValidationContext(singletonList("view"), singletonList("environment")));

            assertThat(directive.errors()).hasSize(1);
            assertThat(directive.errors().on("action")).isEqualTo("Invalid action, must be one of [view].");
        }

        @Test
        void shouldAddErrorIfTypeIsNotSet() {
            Directive directive = getDirective("view", null, "test-resource");

            directive.validate(rulesValidationContext(singletonList("view"), singletonList("environment")));

            assertThat(directive.errors()).hasSize(1);
            assertThat(directive.errors().get("type"))
                    .hasSize(1)
                    .contains("Invalid type, must be one of [environment].");
        }

        @Test
        void shouldAddErrorIfTypeIsSetToOtherThanAllowedActions() {
            Directive directive = getDirective("view", "invalid", "test-resource");

            directive.validate(rulesValidationContext(singletonList("view"), singletonList("environment")));

            assertThat(directive.errors()).hasSize(1);

            assertThat(directive.errors().get("type"))
                    .hasSize(1)
                    .contains("Invalid type, must be one of [environment].");
        }

        @Test
        void shouldAllowTypeToHaveWildcard() {
            Directive directive = getDirective("view", "*", "test-resource");

            directive.validate(rulesValidationContext(singletonList("view"), singletonList("environment")));

            assertThat(directive.errors()).hasSize(0);
        }
    }

    private ValidationContext rulesValidationContext(List<String> allowedAction, List<String> allowedType) {
        return new DelegatingValidationContext(null) {
            @Override
            public PolicyValidationContext getPolicyValidationContext() {
                return new PolicyValidationContext(allowedAction, allowedType);
            }
        };
    }

    Directive getDirective(String action, String type, String resource) {
        return new Allow(action, type, resource);
    }
}
