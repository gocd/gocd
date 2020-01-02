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

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class StringUtil {
    private static final String DELIMITER = "_";
    private static final String DELIMITER_MATCHER = DELIMITER;
    private static final String DELIMITER_ESCAPE_SEQ = DELIMITER + DELIMITER;

    private StringUtil() {
    }

    public static String quoteJavascriptString(String s) {
        return "\"" + StringEscapeUtils.escapeJava(s) + "\"";
    }

    public static String joinSentences(String... strings) {
        return Arrays.stream(strings).map(String::trim).map(s -> s.endsWith(".") ? s : s + ".").collect(Collectors.joining(" "));
    }

    public static String matchPattern(String regEx, String s) {
        Pattern pattern = Pattern.compile(regEx);
        Matcher matcher = pattern.matcher(s);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    public static String removeTrailingSlash(String s) {
        if (s.endsWith("/")) {
            return s.substring(0, s.length() - 1);
        }
        return s;
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static String humanize(String s) {
        String[] strings = StringUtils.splitByCharacterTypeCamelCase(s);
        for (int i = 0; i < strings.length; i++) {
            String string = strings[i];
            strings[i] = string.toLowerCase();
        }
        return StringUtils.join(strings, " ");
    }

    public static String escapeAndJoinStrings(Object... items) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.length; i++) {
            Object item = items[i];
            if (item != null) {
                builder.append(item.toString().replaceAll(DELIMITER_MATCHER, DELIMITER_ESCAPE_SEQ));
            }
            if (i + 1 < items.length) {
                builder.append(DELIMITER);
            }
        }
        return builder.toString();
    }

    public static String joinForDisplay(final Collection<?> objects) {
        return StringUtils.join(objects, " | ").trim();
    }

    public static String stripTillLastOccurrenceOf(String input, String pattern) {
        if (!StringUtils.isBlank(input) && !StringUtils.isBlank(pattern)) {
            int index = input.lastIndexOf(pattern);
            if (index > 0) {
                input = input.substring(index + pattern.length());
            }
        }
        return input;
    }

    public static String unQuote(String string) {
        return string == null ? null : string.replaceAll("^\"|\"$", "");
    }
}
