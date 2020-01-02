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
package com.thoughtworks.go.config.validation;

import com.thoughtworks.go.config.ConfigSaveState;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.thoughtworks.go.config.validation.GoConfigValidity.*;
import static org.assertj.core.api.Assertions.assertThat;

class GoConfigValidityTest {

    @Nested
    class ValidGoConfig {
        @Test
        void shouldReturnValidConfigResult() {
            assertThat(valid())
                    .isNotNull()
                    .isInstanceOf(GoConfigValidity.class)
                    .isInstanceOf(GoConfigValidity.ValidGoConfig.class);

            assertThat(valid().isValid()).isTrue();
        }

        @Test
        void shouldReturnValidConfigResultWithConfigState() {
            final GoConfigValidity.ValidGoConfig validity = valid(ConfigSaveState.UPDATED);

            assertThat(validity.isValid()).isTrue();
            assertThat(validity.wasMerged()).isFalse();
        }

    }

    @Nested
    class Invalid {
        @Test
        void shouldReturnInvalidConfigResultForGivenErrorMessage() {
            final InvalidGoConfig invalidGoConfig = invalid("some error");

            assertThat(invalidGoConfig)
                    .isNotNull()
                    .isInstanceOf(GoConfigValidity.class)
                    .isInstanceOf(InvalidGoConfig.class);
            assertThat(invalidGoConfig.isValid()).isFalse();
            assertThat(invalidGoConfig.errorMessage()).isEqualTo("some error");

        }

        @Test
        void shouldReturnInvalidConfigResultForGivenException() {
            final InvalidGoConfig invalidGoConfig = invalid("some-error");

            assertThat(invalidGoConfig)
                    .isNotNull()
                    .isInstanceOf(GoConfigValidity.class)
                    .isInstanceOf(InvalidGoConfig.class);
            assertThat(invalidGoConfig.isValid()).isFalse();
            assertThat(invalidGoConfig.errorMessage()).isEqualTo("some-error");
        }

        @Test
        void shouldAllowToSetConflicts() {
            final InvalidGoConfig configValidity = fromConflict("error message");

            assertThat(configValidity.isType(VT_CONFLICT)).isTrue();
            assertThat(configValidity.isValid()).isFalse();
        }

        @Test
        void shouldAllowToSetMergeConflict() {
            final InvalidGoConfig configValidity = mergeConflict("error message");

            assertThat(configValidity.isMergeConflict()).isTrue();
            assertThat(configValidity.isValid()).isFalse();
        }

        @Test
        void shouldAllowToSetMergePreValidationError() {
            final InvalidGoConfig configValidity = mergePreValidationError("error message");

            assertThat(configValidity.isType(VT_MERGE_PRE_VALIDATION_ERROR)).isTrue();
        }

        @Test
        void shouldAllowToSetMergePostValidationError() {
            final InvalidGoConfig configValidity = mergePostValidationError("error message");

            assertThat(configValidity.isPostValidationError()).isTrue();
        }
    }
}
