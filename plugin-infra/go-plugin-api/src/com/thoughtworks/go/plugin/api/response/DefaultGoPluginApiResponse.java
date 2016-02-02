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

package com.thoughtworks.go.plugin.api.response;

import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of GoPluginApiResponse
 */
public class DefaultGoPluginApiResponse extends GoPluginApiResponse {

    public static final int SUCCESS_RESPONSE_CODE = 200;
    public static final int VALIDATION_FAILED = 412;
    public static final int BAD_REQUEST = 400;
    public static final int INTERNAL_ERROR = 500;

    private int responseCode;

    private Map<String, String> responseHeaders;

    private String responseBody;

    /**
     * Constructs DefaultGoPluginApiResponse with response code
     *
     * @param responseCode Response code for the response
     */
    public DefaultGoPluginApiResponse(int responseCode) {
        this(responseCode, null, new HashMap<String, String>());
    }

    /**
     * Constructs DefaultGoPluginApiResponse
     *
     * @param responseCode Response code for the response
     * @param responseBody Body of the response
     */
    public DefaultGoPluginApiResponse(int responseCode, String responseBody) {
        this(responseCode, responseBody, new HashMap<String, String>());
    }

    /**
     * Constructs DefaultGoPluginApiResponse
     *
     * @param responseCode    Response code for the response
     * @param responseBody    Body of the response
     * @param responseHeaders The headers of the response
     */
    public DefaultGoPluginApiResponse(int responseCode, String responseBody, Map<String, String> responseHeaders) {
        this.responseCode = responseCode;
        this.responseBody = responseBody;
        this.responseHeaders = responseHeaders;
    }

    /**
     * Creates an instance DefaultGoPluginApiResponse which represents incomplete request with response code 412
     *
     * @param responseBody Response body
     * @return an instance of DefaultGoPluginApiResponse
     */
    public static DefaultGoPluginApiResponse incompleteRequest(String responseBody) {
        return new DefaultGoPluginApiResponse(VALIDATION_FAILED, responseBody);
    }

    /**
     * Creates an instance DefaultGoPluginApiResponse which represents bad request with response code 400
     *
     * @param responseBody Response body
     * @return an instance of DefaultGoPluginApiResponse
     */
    public static DefaultGoPluginApiResponse badRequest(String responseBody) {
        return new DefaultGoPluginApiResponse(BAD_REQUEST, responseBody);
    }

    /**
     * Creates an instance DefaultGoPluginApiResponse which represents error request with response code 500
     *
     * @param responseBody Response body
     * @return an instance of DefaultGoPluginApiResponse
     */
    public static DefaultGoPluginApiResponse error(String responseBody) {
        return new DefaultGoPluginApiResponse(INTERNAL_ERROR, responseBody);
    }

    /**
     * Creates an instance DefaultGoPluginApiResponse which represents success request with response code 200
     *
     * @param responseBody Json formatted response body
     * @return an instance of DefaultGoPluginApiResponse
     */
    public static DefaultGoPluginApiResponse success(String responseBody) {
        return new DefaultGoPluginApiResponse(SUCCESS_RESPONSE_CODE, responseBody);
    }


    /**
     * Adds new response header. Replace existing header with same name
     *
     * @param name  Name of the header
     * @param value Value of the header
     */
    public void addResponseHeader(String name, String value) {
        responseHeaders.put(name, value);
    }

    /**
     * Sets response body
     *
     * @param responseBody Json formatted response body represented as string
     */
    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }

    /**
     * Provides response code for the request sent
     *
     * @return
     */
    @Override
    public int responseCode() {
        return responseCode;
    }

    /**
     * Provides response headers as key value pair for the response. Response headers can be used to send any meta information related to response
     *
     * @return request headers as a Map
     */
    @Override
    public Map<String, String> responseHeaders() {
        return responseHeaders;
    }

    /**
     * Provides json formatted response body
     *
     * @return response body
     */
    @Override
    public String responseBody() {
        return responseBody;
    }
}
