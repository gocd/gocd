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
package com.thoughtworks.go.plugin.api.request;

import java.util.Map;

/**
 * Provides an abstraction for api request sent from Go to plugin
 */
public abstract class GoPluginApiRequest {

    /**
     * Extension name of the request
     *
     * @return extension name
     */
    public abstract String extension();

    /**
     * Extension version of the request
     *
     * @return extension version
     */
    public abstract String extensionVersion();

    /**
     * Name or operation supported for an extension
     *
     * @return name of the request
     */
    public abstract String requestName();

    /**
     * Provides request parameters as key value pair for the request
     *
     * @return map of request parameters
     */
    public abstract Map<String, String> requestParameters();

    /**
     * Provides request headers as key value pair for the request. Request headers can be used to send any meta information related to request
     *
     * @return map of request headers
     */
    public abstract Map<String, String> requestHeaders();

    /**
     * Provides json formatted request body
     *
     * @return request body
     */
    public abstract String requestBody();
}
