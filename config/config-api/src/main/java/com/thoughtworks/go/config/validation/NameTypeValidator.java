/*
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.go.config.validation;

import java.util.regex.Pattern;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.util.XmlUtils;

public class NameTypeValidator {
    public static final int MAX_LENGTH = 255;
    static final String NAME_TYPE_PATTERN = "[a-zA-Z0-9_\\-]{1}[a-zA-Z0-9_\\-.]*";
    static final Pattern NAME_TYPE_PATTERN_REGEX = Pattern.compile(String.format("^(%s)$", NAME_TYPE_PATTERN));
    public static final String ERROR_MESSAGE =
            String.format("This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is %d characters.", MAX_LENGTH);

    public boolean isNameValid(String name) {
        return name != null && name.length() <= MAX_LENGTH && XmlUtils.matchUsingRegex(NAME_TYPE_PATTERN_REGEX, name);
    }

    public boolean isNameInvalid(String name) {
        return !isNameValid(name);
    }

    public boolean isNameValid(CaseInsensitiveString name) {
        return isNameValid(CaseInsensitiveString.str(name));
    }

    public static String errorMessage(String type, Object name) {
        return String.format("Invalid %s name '%s'. %s", type, name, NameTypeValidator.ERROR_MESSAGE);
    }
}
