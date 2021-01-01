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
package com.thoughtworks.go.plugin.access.artifact;

import java.util.Arrays;
import java.util.List;

public interface ArtifactExtensionConstants {
    String V1 = "1.0";
    String V2 = "2.0";
    List<String> SUPPORTED_VERSIONS = Arrays.asList(V1, V2);

    String REQUEST_PREFIX = "cd.go.artifact";
    String REQUEST_GET_CAPABILITIES = REQUEST_PREFIX + ".get-capabilities";
    String REQUEST_STORE_CONFIG_METADATA = String.join(".", REQUEST_PREFIX, "store", "get-metadata");
    String REQUEST_STORE_CONFIG_VIEW = String.join(".", REQUEST_PREFIX, "store", "get-view");
    String REQUEST_STORE_CONFIG_VALIDATE = String.join(".", REQUEST_PREFIX, "store", "validate");

    String REQUEST_PUBLISH_ARTIFACT_METADATA = String.join(".", REQUEST_PREFIX, "publish", "get-metadata");
    String REQUEST_PUBLISH_ARTIFACT_VIEW = String.join(".", REQUEST_PREFIX, "publish", "get-view");
    String REQUEST_PUBLISH_ARTIFACT_VALIDATE = String.join(".", REQUEST_PREFIX, "publish", "validate");

    String REQUEST_FETCH_ARTIFACT_METADATA = String.join(".", REQUEST_PREFIX, "fetch", "get-metadata");
    String REQUEST_FETCH_ARTIFACT_VIEW = String.join(".", REQUEST_PREFIX, "fetch", "get-view");
    String REQUEST_FETCH_ARTIFACT_VALIDATE = String.join(".", REQUEST_PREFIX, "fetch", "validate");

    String REQUEST_PUBLISH_ARTIFACT = String.join(".", REQUEST_PREFIX, "publish-artifact");
    String REQUEST_FETCH_ARTIFACT = String.join(".", REQUEST_PREFIX, "fetch-artifact");
    String REQUEST_GET_PLUGIN_ICON = String.join(".", REQUEST_PREFIX, "get-icon");
}
