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
package com.thoughtworks.go.domain.materials.mercurial;

import de.skuzzle.semantic.Version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class HgVersion {
    private static final Pattern HG_VERSION_PATTERN =
            Pattern.compile(".+\\(\\s*\\S+\\s+(\\d+)\\.(\\d+)[.]?(\\d+)?.*\\s*\\)\\s*.*\\s*", Pattern.CASE_INSENSITIVE);
    private final Version version;

    public HgVersion(Version version) {
        this.version = version;
    }

    public static HgVersion parse(String hgOut) {
        String[] lines = hgOut.split("\n");
        String firstLine = lines[0];
        Matcher m = HG_VERSION_PATTERN.matcher(firstLine);
        if (m.matches()) {
            try {
                return new HgVersion(Version.create(
                        asInt(m, 1),
                        asInt(m, 2),
                        asInt(m, 3)));
            } catch (Exception e) {
                throw bomb("cannot parse Hg version : " + hgOut);
            }
        }
        throw bomb("cannot parse Hg version : " + hgOut);
    }

    public boolean isOlderThanOneDotZero() {
        return this.version.isLowerThan(Version.create(1, 0, 0));
    }

    public Version getVersion() {
        return this.version;
    }

    private static int asInt(Matcher m, int group) {
        if (group > m.groupCount() + 1) {
            return 0;
        }
        final String match = m.group(group);
        if (match == null) {
            return 0;
        }
        return Integer.parseInt(match);
    }
}
