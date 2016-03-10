/*************************** GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/
package com.thoughtworks.go.domain;

import com.google.gson.annotations.Expose;

public class BuildSettings {
    @Expose
    private String buildId;
    @Expose
    private String consoleUrl;
    @Expose
    private String buildLocatorForDisplay;
    @Expose
    private String buildLocator;
    @Expose
    private String artifactUploadBaseUrl;
    @Expose
    private String propertyBaseUrl;
    @Expose
    private BuildCommand buildCommand;

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public String getConsoleUrl() {
        return consoleUrl;
    }

    public void setConsoleUrl(String consoleUrl) {
        this.consoleUrl = consoleUrl;
    }

    public String getBuildLocatorForDisplay() {
        return buildLocatorForDisplay;
    }

    public void setBuildLocatorForDisplay(String buildLocatorForDisplay) {
        this.buildLocatorForDisplay = buildLocatorForDisplay;
    }

    public String getArtifactUploadBaseUrl() {
        return artifactUploadBaseUrl;
    }

    public void setArtifactUploadBaseUrl(String artifactUploadBaseUrl) {
        this.artifactUploadBaseUrl = artifactUploadBaseUrl;
    }

    public String getPropertyBaseUrl() {
        return propertyBaseUrl;
    }

    public void setPropertyBaseUrl(String propertyBaseUrl) {
        this.propertyBaseUrl = propertyBaseUrl;
    }

    public BuildCommand getBuildCommand() {
        return buildCommand;
    }

    public void setBuildCommand(BuildCommand buildCommand) {
        this.buildCommand = buildCommand;
    }

    public String getBuildLocator() {
        return buildLocator;
    }

    public void setBuildLocator(String buildLocator) {
        this.buildLocator = buildLocator;
    }
}
