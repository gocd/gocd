/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.exception.VersionFormatException;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Integer.compare;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;

public class GoVersion implements Comparable<GoVersion> {
    private int major;
    private int minor;
    private int patches;
    private int modifications;

    public GoVersion() {
    }

    public GoVersion(String version) {
        parse(version);
    }

    public void setVersion(String version) {
        parse(version);
    }

    public String getVersion() {
        return toString();
    }

    private void parse(String version) {
        Matcher matcher = matcherFor(version);

        if (matcher == null) throw new VersionFormatException(format("Invalid version format [%s]", version));

        this.major = parseInt(matcher.group(1));
        this.minor = parseInt(matcher.group(2));
        this.patches = parseInt(matcher.group(3));
        this.modifications = parseInt(matcher.group(4));
    }

    private Matcher matcherFor(String version) {
        Pattern updateServerPattern = Pattern.compile("^(?:(\\d+)\\.)?(?:(\\d+)\\.)?(?:(\\d+)\\-)?(?:(\\d+))$");
        Pattern serverVersionPattern = Pattern.compile("^(?:(\\d+)\\.)?(?:(\\d+)\\.)?(?:(\\d+)\\s*\\()?(?:(\\d+)\\-)?(?:(\\w+)\\))$");
        Matcher matcher = null;

        matcher = updateServerPattern.matcher(version);
        if (matcher.matches()) return matcher;

        matcher = serverVersionPattern.matcher(version);
        if (matcher.matches()) return matcher;

        return null;
    }

    @Override
    public int compareTo(GoVersion o) {
        if (this.major != o.major) {
            return compare(this.major, o.major);
        }
        if (this.minor != o.minor) {
            return compare(this.minor, o.minor);
        }
        if (this.patches != o.patches) {
            return compare(this.patches, o.patches);
        }
        if (this.modifications != o.modifications) {
            return compare(this.modifications, o.modifications);
        }

        return 0;
    }

    public boolean isGreaterThan(GoVersion o) {
        return compareTo(o) == 1;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatches() {
        return patches;
    }

    public int getModifications() {
        return modifications;
    }

    @Override
    public String toString() {
        return format("%s.%s.%s-%s", major, minor, patches, modifications);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GoVersion that = (GoVersion) o;

        return compareTo(that) == 0;
    }

    @Override
    public int hashCode() {
        int result = major;
        result = 31 * result + minor;
        result = 31 * result + patches;
        result = 31 * result + modifications;
        return result;
    }
}
