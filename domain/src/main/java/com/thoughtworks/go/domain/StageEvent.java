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
package com.thoughtworks.go.domain;

public enum StageEvent {
    Fails {
        public String describe() {
            return " failed";
        }
    },
    Passes {
        public String describe() {
            return " passed";
        }
    },
    Breaks {
        public String describe() {
            return " is broken";
        }
    },
    Fixed {
        public String describe() {
            return " is fixed";
        }
    },
    Cancelled {
        public String describe() {
            return " is cancelled";
        }
    },
    All {
        public String describe() {
            return " [ALL]";
        }
    };

    public abstract String describe();

    public boolean include(StageEvent other) {
        return this == All || this == other;
    }
}
