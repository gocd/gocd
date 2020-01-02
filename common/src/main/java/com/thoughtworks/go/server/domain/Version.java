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
package com.thoughtworks.go.server.domain;

public class Version {
    private int version;

    public static Version belowAllVersions() {
        return new Version(-9999);
    }

    public Version(String versionFileContents) {
        this.version = getVersion(versionFileContents);
    }

    public Version(int version) {
        this.version = version;
    }

    public boolean isAtHigherVersionComparedTo(Version existingVersion) {
        return this.version > existingVersion.version;
    }

    private int getVersion(String content) {
        try {
            return Integer.parseInt(content.split("=")[1].trim());
        } catch (Exception e) {
            throw new RuntimeException("Could not parse contents of version file: " + content);
        }
    }

}
