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
package com.thoughtworks.go;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CurrentGoCDVersion {

    private final String goVersion;
    private final String distVersion;
    private final String gitRevision;
    private final String formatted;
    private final String fullVersion;
    private final String copyrightYear;
    private final String baseDocsUrl;
    private final String baseApiDocsUrl;
    private final String gocdDistVersion;

    private CurrentGoCDVersion() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("gocd-version.properties")) {
            Properties properties = new Properties();
            properties.load(in);

            this.goVersion = properties.getProperty("goVersion", "unknown");
            this.distVersion = properties.getProperty("distVersion", "unknown");
            this.gitRevision = properties.getProperty("gitRevision", "unknown");
            this.fullVersion = properties.getProperty("fullVersion", "unknown");
            this.copyrightYear = properties.getProperty("copyrightYear", "unknown");
            this.formatted = String.format("%s (%s-%s)", goVersion, distVersion, gitRevision);
            this.gocdDistVersion = String.format("%s-%s", goVersion, distVersion);
            this.baseDocsUrl = "https://docs.gocd.org/" + this.goVersion;
            this.baseApiDocsUrl = "https://api.gocd.org/" + this.goVersion;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String copyrightYear() {
        return copyrightYear;
    }

    public String goVersion() {
        return goVersion;
    }

    public String distVersion() {
        return distVersion;
    }

    public String gitRevision() {
        return gitRevision;
    }

    public String formatted() {
        return formatted;
    }

    public String fullVersion() {
        return fullVersion;
    }

    public String baseDocsUrl() {
        return baseDocsUrl;
    }

    public String getGocdDistVersion() {
        return gocdDistVersion;
    }

    public String baseApiDocsUrl() {
        return baseApiDocsUrl;
    }

    private static class SingletonHolder {
        private static final CurrentGoCDVersion INSTANCE = new CurrentGoCDVersion();
    }

    public static String docsUrl(String suffix) {
        return SingletonHolder.INSTANCE.baseDocsUrl() + "/" + stripLeadingPrefix(suffix, "/");
    }

    public static String apiDocsUrl(String hashFragment) {
        return SingletonHolder.INSTANCE.baseApiDocsUrl() + "/#" + stripLeadingPrefix(hashFragment, "#");
    }

    private static String stripLeadingPrefix(String suffix, String prefix) {
        if (suffix.startsWith(prefix)) {
            suffix = suffix.substring(1);
        }
        return suffix;
    }

    public static CurrentGoCDVersion getInstance() {
        return SingletonHolder.INSTANCE;
    }

}
