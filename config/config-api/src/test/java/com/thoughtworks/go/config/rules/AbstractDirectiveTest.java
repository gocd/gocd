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

import com.thoughtworks.go.config.DelegatingValidationContext;
import com.thoughtworks.go.config.ValidationContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

abstract class AbstractDirectiveTest {
    @Nested
    class validate {
        @Test
        void shouldAddErrorIfActionIsNotSet() {
            Directive directive = getDirective(null, "pipeline_group", "test-resource");

            directive.validate(rulesValidationContext(singletonList("refer"), singletonList("pipeline_group")));

            assertThat(directive.errors()).hasSize(1);
            assertThat(directive.errors().get("action"))
                    .hasSize(1)
                    .contains("Invalid action, must be one of [refer].");
        }

        @Test
        void shouldAddErrorIfActionIsSetToOtherThanAllowedActions() {
            Directive directive = getDirective("invalid", "pipeline_group", "test-resource");

            directive.validate(rulesValidationContext(singletonList("refer"), singletonList("pipeline_group")));

            assertThat(directive.errors()).hasSize(1);

            assertThat(directive.errors().get("action"))
                    .hasSize(1)
                    .contains("Invalid action, must be one of [refer].");
        }

        @Test
        void shouldAllowActionToHaveWildcard() {
            Directive directive = getDirective("*", "pipeline_group", "test-resource");

            directive.validate(rulesValidationContext(singletonList("refer"), singletonList("pipeline_group")));

            assertThat(directive.errors()).hasSize(0);
        }

        @Test
        void shouldAddErrorIfTypeIsNotSet() {
            Directive directive = getDirective("refer", null, "test-resource");

            directive.validate(rulesValidationContext(singletonList("refer"), singletonList("pipeline_group")));

            assertThat(directive.errors()).hasSize(1);
            assertThat(directive.errors().get("type"))
                    .hasSize(1)
                    .contains("Invalid type, must be one of [pipeline_group].");
        }

        @Test
        void shouldAddErrorIfTypeIsSetToOtherThanAllowedActions() {
            Directive directive = getDirective("refer", "invalid", "test-resource");

            directive.validate(rulesValidationContext(singletonList("refer"), singletonList("pipeline_group")));

            assertThat(directive.errors()).hasSize(1);

            assertThat(directive.errors().get("type"))
                    .hasSize(1)
                    .contains("Invalid type, must be one of [pipeline_group].");
        }

        @Test
        void shouldAllowTypeToHaveWildcard() {
            Directive directive = getDirective("refer", "*", "test-resource");

            directive.validate(rulesValidationContext(singletonList("refer"), singletonList("pipeline_group")));

            assertThat(directive.errors()).hasSize(0);
        }

        @Test
        void shouldAddErrorIfResourceIsBlank() {
            Directive directive = getDirective("refer", "*", "");

            directive.validate(rulesValidationContext(singletonList("refer"), singletonList("pipeline_group")));

            assertThat(directive.errors().get("resource"))
                    .hasSize(1)
                    .contains("Resource cannot be blank.");
        }

    }

    private ValidationContext rulesValidationContext(List<String> allowedAction, List<String> allowedType) {
        return new DelegatingValidationContext(null) {
            @Override
            public RulesValidationContext getRulesValidationContext() {
                return new RulesValidationContext(allowedAction, allowedType);
            }
        };
    }


    abstract Directive getDirective(String action, String type, String resource);
}