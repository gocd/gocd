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

package com.thoughtworks.go.config.rules;

import com.thoughtworks.go.config.DelegatingValidationContext;
import com.thoughtworks.go.config.ValidationContext;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RulesTest {
    @Nested
    class validateTree {
        @Test
        void shouldCallValidateOfEachRule() {
            ValidationContext validationContext = mock(ValidationContext.class);
            Allow allow = mock(Allow.class);
            Deny deny = mock(Deny.class);
            Rules rules = new Rules(allow, deny);

            rules.validateTree(validationContext);

            verify(allow).validate(validationContext);
            verify(deny).validate(validationContext);
        }

        @Test
        void shouldHaveErrorsIfOneOfTheDirectiveHasError() {
            final Allow invalidDirective = new Allow(null, "pipeline_group", null);
            final Rules rules = new Rules(invalidDirective);

            rules.validateTree(rulesValidationContext(singletonList("refer"), singletonList("pipeline_group")));

            assertThat(rules.hasErrors()).isTrue();
        }

        @Test
        void shouldNotHaveErrorsIfNoneOfTheDirectiveHasErrors() {
            final Rules rules = new Rules(
                    new Allow("refer", "pipeline_group", "env1"),
                    new Deny("refer", "pipeline_group", "env2"));

            rules.validateTree(rulesValidationContext(singletonList("refer"), singletonList("pipeline_group")));

            assertThat(rules.hasErrors()).isFalse();
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
}