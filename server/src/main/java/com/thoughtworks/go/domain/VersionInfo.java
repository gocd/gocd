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

import java.util.Date;

public class VersionInfo extends PersistentObject{
    private String componentName;
    private GoVersion installedVersion;
    private GoVersion latestVersion;
    private Date latestVersionUpdatedAt;

    public VersionInfo() {

    }

    public VersionInfo(String componentName, GoVersion currentVersion, GoVersion latestVersion,
                       Date latestVersionUpdatedAt) {
        this.componentName = componentName;
        this.installedVersion = currentVersion;
        this.latestVersion = latestVersion;
        this.latestVersionUpdatedAt = latestVersionUpdatedAt;
    }

    public VersionInfo(String componentName, GoVersion currentVersion) {
        this(componentName, currentVersion, null, null);
    }

    public String getComponentName() {
        return componentName;
    }

    public GoVersion getInstalledVersion() {
        return installedVersion;
    }

    public GoVersion getLatestVersion() {
        return latestVersion;
    }

    public Date getLatestVersionUpdatedAt() {
        return latestVersionUpdatedAt;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

    public void setInstalledVersion(GoVersion installedVersion) {
        this.installedVersion = installedVersion;
    }

    public void setLatestVersion(GoVersion latestVersion) {
        this.latestVersion = latestVersion;
    }

    public void setLatestVersionUpdatedAt(Date latestVersionUpdatedAt) {
        this.latestVersionUpdatedAt = latestVersionUpdatedAt;
    }
}
