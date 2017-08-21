/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.elastic;

import com.thoughtworks.go.plugin.access.elastic.v1.ElasticAgentExtensionConverterV1;
import com.thoughtworks.go.plugin.access.elastic.v2.ElasticAgentExtensionConverterV2;

import java.util.Arrays;
import java.util.List;

public interface ElasticAgentPluginConstants {
    List<String> SUPPORTED_VERSIONS = Arrays.asList(ElasticAgentExtensionConverterV1.VERSION, ElasticAgentExtensionConverterV2.VERSION);

    String EXTENSION_NAME = "elastic-agent";

    String REQUEST_PREFIX = "go.cd.elastic-agent";
    String PROCESSOR_PREFIX = "go.processor.elastic-agents";

    String REQUEST_CREATE_AGENT = REQUEST_PREFIX + ".create-agent";
    String REQUEST_SERVER_PING = REQUEST_PREFIX + ".server-ping";
    String REQUEST_SHOULD_ASSIGN_WORK = REQUEST_PREFIX + ".should-assign-work";

    String REQUEST_GET_PROFILE_METADATA = REQUEST_PREFIX + ".get-profile-metadata";
    String REQUEST_GET_PROFILE_VIEW = REQUEST_PREFIX + ".get-profile-view";
    String REQUEST_VALIDATE_PROFILE = REQUEST_PREFIX + ".validate-profile";
    String REQUEST_GET_PLUGIN_SETTINGS_ICON = REQUEST_PREFIX + ".get-icon";

    String PROCESS_DISABLE_AGENTS = PROCESSOR_PREFIX + ".disable-agents";
    String PROCESS_DELETE_AGENTS = PROCESSOR_PREFIX + ".delete-agents";
    String REQUEST_SERVER_LIST_AGENTS = PROCESSOR_PREFIX + ".list-agents";
    String REQUEST_STATUS_REPORT = REQUEST_PREFIX + ".status-report";
    String REQUEST_CAPABILTIES = REQUEST_PREFIX + ".get-capabilities";
}
