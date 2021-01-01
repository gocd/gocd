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
package com.thoughtworks.go.plugin.api;

import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;

/**
 * GoPlugin interface represents Go plugin. It is necessary to implement this interface for any plugin implementation to be recognized as a Go plugin
 */
@GoPluginApiMarker
public interface GoPlugin {

    /**
     * Initializes an instance of GoApplicationAccessor. This method would be invoked before Go interacts with plugin to handle any GoPluginApiRequest.
     * Instance of GoApplicationAccessor will allow plugin to communicate with Go.
     *
     * @param goApplicationAccessor An instance of GoApplicationAccessor
     */
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor);

    /**
     * Handles GoPluginApiRequest request submitted from Go to plugin implementation and returns result as GoPluginApiResponse
     *
     * @param requestMessage An instance of GoPluginApiRequest
     * @return an instance of GoPluginApiResponse
     */
    public GoPluginApiResponse handle(GoPluginApiRequest requestMessage) throws UnhandledRequestTypeException;

    /**
     * Provides an instance of GoPluginIdentifier, providing details about supported extension point and its versions
     *
     * @return an instance of GoPluginIdentifier
     */
    public GoPluginIdentifier pluginIdentifier();

}



