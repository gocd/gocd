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

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RulesTest {
    @Nested
    class validate {
        @Test
        void shouldCallValidateOfEachRule() {
            RulesValidationContext validationContext = mock(RulesValidationContext.class);
            Allow allow = mock(Allow.class);
            Deny deny = mock(Deny.class);
            Rules rules = new Rules(allow, deny);

            rules.validate(validationContext);

            verify(allow).validate(validationContext);
            verify(deny).validate(validationContext);
        }

        @Test
        void shouldHaveErrorsIfOneOfTheDirectiveHasError() {
            final Allow invalidDirective = new Allow(null, "pipeline_group", null);
            final Rules rules = new Rules(invalidDirective);

            rules.validate(new RulesValidationContext(null, singletonList("refer"), singletonList("pipeline_group")));

            assertThat(rules.hasErrors()).isTrue();
        }

        @Test
        void shouldNotHaveErrorsIfNoneOfTheDirectiveHasErrors() {
            final Rules rules = new Rules(
                    new Allow("refer", "pipeline_group", "env1"),
                    new Deny("refer", "pipeline_group", "env2"));

            rules.validate(new RulesValidationContext(null, singletonList("refer"), singletonList("pipeline_group")));

            assertThat(rules.hasErrors()).isFalse();
        }
    }

    @Nested
    class errors {
        @Test
        void shouldErrorOutSinceRulesCannotHaveErrors() {
            assertThatCode(() -> new Rules().errors())
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("Rules cannot have errors.");
        }
    }

    @Nested
    class addError {
        @Test
        void shouldErrorOutSinceRulesCannotHaveErrors() {
            assertThatCode(() -> new Rules().addError("key", "message"))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessage("Rules cannot have errors. Errors can be added to the directives.");
        }
    }
}