/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain.materials.git;

import de.skuzzle.semantic.Version;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

public class GitVersion {

    private static final Pattern GIT_VERSION_PATTERN = Pattern.compile(
            "git version (\\d+)\\.(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?(.*)", Pattern.CASE_INSENSITIVE);

    private final Version version;
    private final static Version MINIMUM_SUPPORTED_VERSION = Version.create(1, 9, 0);
    private final static Version SUBMODULE_DEPTH_SUPPORT = Version.create(2, 10, 0);
    private final static Version SUBMODULE_FOREACH_RECURSIVE_BREAK = Version.create(2, 22, 0);

    private GitVersion(Version parsedVersion) {
        this.version = parsedVersion;
    }

    public static GitVersion parse(String versionFromConsoleOutput) {
        String[] lines = versionFromConsoleOutput.split("\n");
        String firstLine = lines[0];
        Matcher m = GIT_VERSION_PATTERN.matcher(firstLine);
        if (m.find()) {
            try {
                return new GitVersion(parseVersion(m));
            } catch (Exception e) {
                throw bomb("cannot parse git version : " + versionFromConsoleOutput);
            }
        }
        throw bomb("cannot parse git version : " + versionFromConsoleOutput);
    }

    public boolean isMinimumSupportedVersionOrHigher() {
        return this.version.compareTo(MINIMUM_SUPPORTED_VERSION) >= 0;
    }

    public boolean supportsSubmoduleDepth() {
        return version.compareTo(SUBMODULE_DEPTH_SUPPORT) >= 0;
    }

    public boolean requiresSubmoduleCommandFix() {
        return version.compareTo(SUBMODULE_FOREACH_RECURSIVE_BREAK) >= 0;
    }

    public Version getVersion() {
        return this.version;
    }

    private static Version parseVersion(Matcher m) {
        int major = asInt(m, 1);
        int minor = asInt(m, 2);
        int rev = asInt(m, 3);
        return Version.create(major, minor, rev);
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
