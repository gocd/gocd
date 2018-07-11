/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.domain.user;

import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.regex.Pattern;

class FilterValidator {

    private static final Pattern LEAD_TRAIL_WHITESPACE = Pattern.compile("^\\s+.*|.*\\s+$");

    // translation: letters, numbers, spaces, and punctuation (i.e. the ASCII printable
    private static final Pattern NAME_FORMAT = Pattern.compile("^(?! )[\\x20-\\x7E]+(?<! )$");
    // down that road as this is liberal enough.

    // chars); must not start or end with spaces. sorry, no unicode chars -- not going
    private static final int MAX_NAME_LENGTH = 64;

    static final String MSG_NO_LEADING_TRAILING_SPACES = "Filter name must not have leading or trailing whitespaces";
    static final String MSG_MAX_LENGTH = "Filter name cannot be more than " + MAX_NAME_LENGTH + " characters";
    static final String MSG_NAME_FORMAT = "Filter name is only allowed to contain letters, numbers, spaces, and punctuation marks";
    static final String MSG_MISSING_NAME = "Missing filter name";

    static void validateFilter(Map<String, DashboardFilter> current, DashboardFilter filter) {
        final String name = filter.name();
        validateNamePresent(name);
        validateNameFormat(name);
        validateNameIsUnique(current, name);
    }

    static void validateNameFormat(String name) {
        if (LEAD_TRAIL_WHITESPACE.matcher(name).matches())
            throw new FilterValidationException(MSG_NO_LEADING_TRAILING_SPACES);
        if (!NAME_FORMAT.matcher(name).matches())
            throw new FilterValidationException(MSG_NAME_FORMAT);
        if (MAX_NAME_LENGTH < name.length())
            throw new FilterValidationException(MSG_MAX_LENGTH);
    }

    static void validateNameIsUnique(Map<String, DashboardFilter> current, String name) {
        if (current.containsKey(name.toLowerCase()))
            throw new FilterValidationException("Duplicate filter name: " + name);
    }

    static void validateNamePresent(String name) {
        if (StringUtils.isBlank(name)) throw new FilterValidationException(MSG_MISSING_NAME);
    }

    static class FilterValidationException extends RuntimeException {
        FilterValidationException(String message) {
            super(message);
        }
    }
}
