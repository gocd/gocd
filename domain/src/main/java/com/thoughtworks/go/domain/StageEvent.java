/*
 * Copyright Thoughtworks, Inc.
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


import java.util.Arrays;

import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public enum StageEvent {
    Fails {
        @Override
        public String describe() {
            return " failed";
        }
    },
    Passes {
        @Override
        public String describe() {
            return " passed";
        }
    },
    Breaks {
        @Override
        public String describe() {
            return " is broken";
        }
    },
    Fixed {
        @Override
        public String describe() {
            return " is fixed";
        }
    },
    Cancelled {
        @Override
        public String describe() {
            return " is cancelled";
        }
    },
    All {
        @Override
        public String describe() {
            return " [ALL]";
        }
    };

    public abstract String describe();

    public boolean include(StageEvent other) {
        return this == All || this == other;
    }

    public static StageEvent from(String event) {
        return Arrays.stream(values())
                .filter(e -> e.name().equalsIgnoreCase(trimToEmpty(event)))
                .findFirst()
                .orElse(null);
    }
}
