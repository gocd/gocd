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

package com.thoughtworks.go.plugin.access.authorization;

import com.thoughtworks.go.plugin.access.elastic.ElasticAgentExtensionConverterV1;

import java.util.Arrays;
import java.util.List;

public interface AuthorizationPluginConstants {
    List<String> SUPPORTED_VERSIONS = Arrays.asList(ElasticAgentExtensionConverterV1.VERSION);

    String EXTENSION_NAME = "authorization";

    String REQUEST_PREFIX = "go.cd.authorization";
    String REQUEST_GET_CAPABILITIES = REQUEST_PREFIX + ".get-capabilities";
    String REQUEST_GET_PLUGIN_ICON = REQUEST_PREFIX + ".get-icon";

    String _PLUGIN_CONFIG_METADATA = "plugin-config";
    String _ROLE_CONFIG_METADATA = "role-config";

    String REQUEST_GET_PLUGIN_CONFIG_METADATA = String.join(".", REQUEST_PREFIX, _PLUGIN_CONFIG_METADATA, "get-metadata");
    String REQUEST_PLUGIN_CONFIG_VIEW = String.join(".", REQUEST_PREFIX, _PLUGIN_CONFIG_METADATA, "get-view");
    String REQUEST_VALIDATE_PLUGIN_CONFIG = String.join(".", REQUEST_PREFIX, _PLUGIN_CONFIG_METADATA, "validate");
    String REQUEST_VERIFY_CONNECTION = String.join(".", REQUEST_PREFIX, _PLUGIN_CONFIG_METADATA, "verify-connection");

    String REQUEST_GET_ROLE_CONFIG_METADATA = String.join(".", REQUEST_PREFIX, _ROLE_CONFIG_METADATA, "get-metadata");
    String REQUEST_ROLE_CONFIG_VIEW = String.join(".", REQUEST_PREFIX, _ROLE_CONFIG_METADATA, "get-view");
    String REQUEST_VALIDATE_ROLE_CONFIG = String.join(".", REQUEST_PREFIX, _ROLE_CONFIG_METADATA, "validate");

    String REQUEST_AUTHENTICATE_USER = REQUEST_PREFIX + ".authenticate-user";
    String REQUEST_SEARCH_USERS = REQUEST_PREFIX + ".search-users";
}
