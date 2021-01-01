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
package com.thoughtworks.go.plugin.access.secrets;

import static java.lang.String.join;

public interface SecretsPluginConstants {
    String REQUEST_PREFIX = "go.cd.secrets";
    String _SECRETS_CONFIG_METADATA = "secrets-config";

    String REQUEST_GET_PLUGIN_ICON = REQUEST_PREFIX + ".get-icon";
    String REQUEST_GET_SECRETS_CONFIG_METADATA = join(".", REQUEST_PREFIX, _SECRETS_CONFIG_METADATA, "get-metadata");
    String REQUEST_GET_SECRETS_CONFIG_VIEW = join(".", REQUEST_PREFIX, _SECRETS_CONFIG_METADATA, "get-view");
    String REQUEST_VALIDATE_SECRETS_CONFIG = join(".", REQUEST_PREFIX, _SECRETS_CONFIG_METADATA, "validate");
    String REQUEST_VERIFY_CONNECTION = join(".", REQUEST_PREFIX, _SECRETS_CONFIG_METADATA, "verify-connection");
    String REQUEST_LOOKUP_SECRETS = join(".", REQUEST_PREFIX, "secrets-lookup");
}
