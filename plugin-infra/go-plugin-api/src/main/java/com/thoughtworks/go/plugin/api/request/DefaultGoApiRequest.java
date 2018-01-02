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

import com.thoughtworks.go.plugin.api.GoPluginIdentifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of GoApiRequest
 */
public class DefaultGoApiRequest extends GoApiRequest {

    private String api;

    private String apiVersion;

    private GoPluginIdentifier pluginIdentifier;

    private Map<String, String> requestParameters = new HashMap<>();

    private Map<String, String> requestHeaders = new HashMap<>();

    private String requestBody;

    /**
     * Constructs DefaultGoApiRequest with api name, api version and plugin identifier
     *
     * @param api Name of api
     * @param apiVersion version of api
     * @param pluginIdentifier An instance of GoPluginIdentifier
     */
    public DefaultGoApiRequest(String api, String apiVersion, GoPluginIdentifier pluginIdentifier) {
        this.api = api;
        this.apiVersion = apiVersion;
        this.pluginIdentifier = pluginIdentifier;
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
     * Api name for the request
     *
     * @return api name
     */
    @Override
    public String api() {
        return api;
    }

    /**
     * Api version of the request
     *
     * @return api version
     */
    @Override
    public String apiVersion() {
        return apiVersion;
    }

    /**
     * Provides an instance of GoPluginIdentifier for the request
     *
     * @return an instance of GoPluginIdentifier
     */
    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return pluginIdentifier;
    }

    /**
     * Provides request parameters as key value pair for the request
     *
     * @return request parameters as a Map
     */
    @Override
    public Map<String, String> requestParameters() {
        return requestParameters;
    }

    /**
     * Provides request headers as key value pair for the request. Request headers can be used to send any meta information related to request
     *
     * @return request headers as a Map
     */
    @Override
    public Map<String, String> requestHeaders() {
        return requestHeaders;
    }

    /**
     * Provides json formatted request body
     *
     * @return request body
     */
    @Override
    public String requestBody() {
        return requestBody;
    }
}
