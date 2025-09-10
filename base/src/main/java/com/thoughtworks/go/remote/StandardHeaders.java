/*
 * Copyright Thoughtworks, Inc.
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

package com.thoughtworks.go.remote;

public interface StandardHeaders {
    String REQUEST_ARTIFACT_PAYLOAD_SIZE = "X-Go-Artifact-Size";
    String REQUEST_CONFIRM_MODIFICATION_DEPRECATED = "Confirm";
    String REQUEST_CONFIRM_MODIFICATION = "X-GoCD-Confirm";

    String REQUEST_UUID = "X-Agent-GUID";
    String REQUEST_AUTH = "Authorization";

    String RESPONSE_CONTENT_MD5 = "Content-MD5";

    String RESPONSE_AGENT_CONTENT_MD5 = "Agent-Content-MD5";
    String RESPONSE_AGENT_LAUNCHER_CONTENT_MD5 = "Agent-Launcher-Content-MD5";
    String RESPONSE_AGENT_PLUGINS_ZIP_MD5 = "Agent-Plugins-Content-MD5";
    String RESPONSE_AGENT_TFS_SDK_MD5 = "TFS-SDK-Content-MD5";
    String RESPONSE_AGENT_EXTRA_PROPERTIES = "GoCD-Agent-Extra-Properties";
}
