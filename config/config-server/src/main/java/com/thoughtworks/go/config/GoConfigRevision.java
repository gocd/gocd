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
package com.thoughtworks.go.config;

import com.thoughtworks.go.util.TimeProvider;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GoConfigRevision {
    private static final String DELIMITER_CHAR = "|";
    private static final String DELIMITER_CHAR_REGEX = "\\" + DELIMITER_CHAR;
    private static final String VALUE_REGEX_PART = "(([^" + DELIMITER_CHAR + "]|" + DELIMITER_CHAR_REGEX + DELIMITER_CHAR_REGEX + ")+)";

    private static final Pattern PATTERN = Pattern.compile("^" + Fragment.string(VALUE_REGEX_PART, DELIMITER_CHAR_REGEX) + "$");

    public enum Fragment {
        user, timestamp, schema_version, go_edition, go_version, md5;

        private static final int GROUPS_IN_VALUE_MATCHER = 2; //the VALUE matcher has one subgroup per group 1 + 1 = 2

        public String represent(String value) {
            return this + ":" + value;
        }

        private int offset() {
            return ArrayUtils.indexOf(values(), this);
        }

        private static String string(String value, String regexDelimiter) {
            return Arrays.stream(values())
                .map(f -> f.represent(value))
                .collect(Collectors.joining(regexDelimiter));
        }

        private String unesc(String escapedValue) {
            return escapedValue.replace(DELIMITER_CHAR + DELIMITER_CHAR, DELIMITER_CHAR);
        }

        private String getMatch(Matcher matcher) {
            return unesc(matcher.group(offset() * GROUPS_IN_VALUE_MATCHER + 1));
        }
    }

    private final String md5;
    private final String username;
    private final String goVersion;
    private final String goEdition;
    private final String xml;
    private final Date time;
    private final int schemaVersion;

    public GoConfigRevision(String configXml, String md5, String username, String goVersion, TimeProvider provider) {
        this.xml = configXml;
        this.md5 = md5;
        this.username = username;
        this.goVersion = goVersion;
        this.goEdition = "OpenSource";
        this.time = provider.currentUtilDate();
        this.schemaVersion = GoConfigSchema.VERSION;
    }

    public GoConfigRevision(String configXml, String comment) {
        this.xml = configXml;
        Matcher matcher = PATTERN.matcher(comment);
        if (matcher.matches()) {
            username = Fragment.user.getMatch(matcher);
            time = new Date(Long.parseLong(Fragment.timestamp.getMatch(matcher)));
            schemaVersion = Integer.parseInt(Fragment.schema_version.getMatch(matcher));
            goEdition = Fragment.go_edition.getMatch(matcher);
            goVersion = Fragment.go_version.getMatch(matcher);
            md5 = Fragment.md5.getMatch(matcher);
        } else {
            throw new IllegalArgumentException(String.format("failed to parse comment [%s]", comment));
        }
    }

    public String getMd5() {
        return md5;
    }

    public String getContent() {
        return xml;
    }

    public String getComment() {
        return String.format(Fragment.string("%s", DELIMITER_CHAR), esc(username), esc(time.getTime()), esc(schemaVersion), esc(goEdition), esc(goVersion), esc(md5));
    }

    public static String esc(Object content) {
        return content.toString().replace(DELIMITER_CHAR, DELIMITER_CHAR + DELIMITER_CHAR);
    }

    public String getGoVersion() {
        return goVersion;
    }

    public String getGoEdition() {
        return goEdition;
    }

    public String getUsername() {
        return username;
    }

    public Date getTime() {
        return time;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GoConfigRevision that = (GoConfigRevision) o;

        return Objects.equals(md5, that.md5);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(md5);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
            .append("time", time)
            .append("md5", md5)
            .append("username", username)
            .append("goVersion", goVersion)
            .append("goEdition", goEdition)
            .append("xml(length)", xml == null ? 0 : xml.length())
            .append("schemaVersion", schemaVersion)
            .toString();
    }
}
