/*
 * Copyright 2019 ThoughtWorks, Inc.
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

package com.thoughtworks.go.apiv5.plugininfos.representers;

public enum ExtensionType {
    AUTHORIZATION("authorization"),
    SCM("scm"),
    CONFIGREPO("configrepo"),
    ELASTICAGENT("elastic-agent"),
    TASK("task"),
    PACKAGEREPOSITORY("package-repository"),
    NOTIFICATION("notification"),
    ANALYTICS("analytics"),
    ARTIFACT("artifact");

    private String extensionType;

    ExtensionType(String extensionType) {

        this.extensionType = extensionType;
    }

    public String getExtensionType() {
        return extensionType;
    }
}
