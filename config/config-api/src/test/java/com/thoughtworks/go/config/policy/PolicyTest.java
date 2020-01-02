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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PolicyTest {
    @Nested
    class validateTree {
        @Test
        void shouldCallValidateOfEachRule() {
            ValidationContext validationContext = mock(ValidationContext.class);
            Allow allow = mock(Allow.class);
            Deny deny = mock(Deny.class);
            Policy policy = new Policy(allow, deny);

            policy.validateTree(validationContext);

            verify(allow).validate(validationContext);
            verify(deny).validate(validationContext);
        }

        @Test
        void shouldHaveErrorsIfOneOfTheDirectiveHasError() {
            final Allow invalidDirective = new Allow(null, "environment", null);
            final Policy policy = new Policy(invalidDirective);

            policy.validateTree(rulesValidationContext(singletonList("view"), singletonList("environment")));

            assertThat(policy.hasErrors()).isTrue();
        }

        @Test
        void shouldNotHaveErrorsIfNoneOfTheDirectiveHasErrors() {
            final Policy policy = new Policy(
                    new Allow("view", "environment", "env1"),
                    new Deny("view", "environment", "env2"));

            policy.validateTree(rulesValidationContext(singletonList("view"), singletonList("environment")));

            assertThat(policy.hasErrors()).isFalse();
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
}