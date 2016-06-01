/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.api.request;

import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

/**
 * Default implementation of GoPluginApiRequest
 */
public class DefaultGoPluginApiRequest extends GoPluginApiRequest {

    private String extension;

    private String extensionVersion;

    private String requestName;

    private Map<String, String> requestParameters = new HashMap<>();

    private Map<String, String> requestHeaders = new HashMap<>();

    private String requestBody;

    /**
     * Constructs DefaultGoPluginApiRequest with extension name, extension version and request name
     *
     * @param extension        Name of the extension
     * @param extensionVersion Version of the extension
     * @param requestName      Name of request or operation supported under extension
     */
    public DefaultGoPluginApiRequest(String extension, String extensionVersion, String requestName) {
        this.extension = extension;
        this.extensionVersion = extensionVersion;
        this.requestName = requestName;
    }

    /**
     * Sets request body
     *
     * @param requestBody Json formatted request body represented as string
     */
    public void setRequestBody(String requestBody) {
        this.requestBody = requestBody;
    }

    /**
     * Adds new request parameter. Replace existing parameter with same name
     *
     * @param name  Name of the parameter
     * @param value Value of the parameter
     */
    public void addRequestParameter(String name, String value) {
        requestParameters.put(name, value);
    }

    /**
     * Adds new request header. Replace existing header with same name
     *
     * @param name  Name of the header
     * @param value Value of the header
     */
    public void addRequestHeader(String name, String value) {
        requestHeaders.put(name, value);
    }

    /**
     * Extension name of the request
     *
     * @return extension name
     */
    @Override
    public String extension() {
        return extension;
    }

    /**
     * Extension version of the request
     *
     * @return extension version
     */
    @Override
    public String extensionVersion() {
        return extensionVersion;
    }

    /**
     * Name or operation supported for an extension
     *
     * @return name of the request
     */
    @Override
    public String requestName() {
        return requestName;
    }

    /**
     * Provides request parameters as key value pair for the request
     *
     * @return map of request parameters
     */
    @Override
    public Map<String, String> requestParameters() {
        return unmodifiableMap(requestParameters);
    }

    /**
     * Provides request headers as key value pair for the request. Request headers can be used to send any meta information related to request
     *
     * @return map of request headers
     */
    @Override
    public Map<String, String> requestHeaders() {
        return unmodifiableMap(requestHeaders);
    }

    /**
     * Provides json formatted request body of request
     *
     * @return Request body
     */
    @Override
    public String requestBody() {
        return requestBody;
    }

    public void setRequestParams(Map<String, String> params) {
        if (params != null) {
            this.requestParameters = params;
        }
    }
}
