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
package com.thoughtworks.go.domain;

public enum StageResult {

    Passed {
        @Override
        public StageEvent describeChangeEvent(StageResult previousResult) {
            if (previousResult == StageResult.Failed) {
                return StageEvent.Fixed;
            }
            return StageEvent.Passes;
        }
    },

    Failed {
        @Override
        public StageEvent describeChangeEvent(StageResult previousResult) {
            if (previousResult == StageResult.Passed) {
                return StageEvent.Breaks;
            }
            return StageEvent.Fails;
        }
    },

    Cancelled {
        @Override
        public StageEvent describeChangeEvent(StageResult previousResult) {
            return StageEvent.Cancelled;
        }
    },

    Unknown {
        @Override
        public StageEvent describeChangeEvent(StageResult previousResult) {
            throw new IllegalStateException("Current result can not be Unknown");
        }
    };

    public abstract StageEvent describeChangeEvent(StageResult previousResult);
}
