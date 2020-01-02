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
package com.thoughtworks.go.plugin.api;

import java.util.List;


/**
 * Provides details about supported extension point and its version by a plugin
 */
public class GoPluginIdentifier {

    private String extension;

    private List<String> supportedExtensionVersions;

    /**
     * Constructs GoPluginIdentifier with extension and list of supported extension versions
     * @param extension Name of extension
     * @param supportedExtensionVersions List of supported extension versions
     */
    public GoPluginIdentifier(String extension, List<String> supportedExtensionVersions) {
        this.extension = extension;
        this.supportedExtensionVersions = supportedExtensionVersions;
    }

    /**
     * Gets extension name
     * @return Extension name
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Gets list of supported extension versions
     * @return List of supported extension versions
     */
    public List<String> getSupportedExtensionVersions() {
        return supportedExtensionVersions;
    }
}
