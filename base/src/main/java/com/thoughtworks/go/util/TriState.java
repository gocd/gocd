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
package com.thoughtworks.go.util;

import org.apache.commons.lang3.StringUtils;

// an object that represents a ruby like truthy
public abstract class TriState {
    public static TriState UNSET = new TriState() {
        @Override
        public boolean isFalse() {
            return false;
        }

        @Override
        public boolean isTrue() {
            return false;
        }
    };

    public static TriState TRUE = new TriState() {
        @Override
        public boolean isFalse() {
            return false;
        }

        @Override
        public boolean isTruthy() {
            return true;
        }
    };

    public static TriState FALSE = new TriState() {

    };

    private TriState() {

    }

    public boolean isTruthy() {
        return false;
    }

    public boolean isFalsy() {
        return !isTruthy();
    }


    public boolean isFalse() {
        return true;
    }

    public boolean isTrue() {
        return !isFalse();
    }

    public static TriState from(String booleanLike) {
        if (StringUtils.isBlank(booleanLike)) {
            return UNSET;
        }
        if (booleanLike.equalsIgnoreCase("false")) {
            return FALSE;
        }
        if (booleanLike.equalsIgnoreCase("true")) {
            return TRUE;
        }
        throw new IllegalArgumentException(String.format("The string '%s' does not look like a boolean.", booleanLike));
    }

    public static TriState from(boolean booleanLike) {
        if (booleanLike) {
            return TRUE;
        } else {
            return FALSE;
        }
    }
}
