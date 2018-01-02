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

package com.thoughtworks.go.plugin.api.response;


import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.unmodifiableMap;

/**
 * Default implementation of GoApiResponse
 */
public class DefaultGoApiResponse extends GoApiResponse {

    public static final int SUCCESS_RESPONSE_CODE = 200;
    public static final int REDIRECT_RESPONSE_CODE = 302;
    public static final int INTERNAL_ERROR = 500;
    public static final int VALIDATION_ERROR = 412;

    private int responseCode;
    private Map<String, String> responseHeaders = new HashMap<>();
    private String responseBody;

    /**
     * Constructs DefaultGoApiResponse with response code
     *
     * @param responseCode Response code for the response
     */
    public DefaultGoApiResponse(int responseCode) {
        this.responseCode = responseCode;
    }

    /**
     * Creates an instance DefaultGoApiResponse which represents incomplete request with response code 412
     *
     * @param responseBody Response body
     * @return an instance of DefaultGoApiResponse
     */
    public static DefaultGoApiResponse incompleteRequest(String responseBody) {
        DefaultGoApiResponse defaultGoApiResponse = new DefaultGoApiResponse(412);
        defaultGoApiResponse.setResponseBody(responseBody);
        return defaultGoApiResponse;
    }

    /**
     * Creates an instance DefaultGoApiResponse which represents bad request with response code 400
     *
     * @param responseBody Response body
     * @return an instance of DefaultGoApiResponse
     */
    public static DefaultGoApiResponse badRequest(String responseBody) {
        DefaultGoApiResponse defaultGoApiResponse = new DefaultGoApiResponse(400);
        defaultGoApiResponse.setResponseBody(responseBody);
        return defaultGoApiResponse;
    }

    /**
     * Creates an instance DefaultGoApiResponse which represents error request with response code 500
     *
     * @param responseBody Response body
     * @return an instance of DefaultGoApiResponse
     */
    public static DefaultGoApiResponse error(String responseBody) {
        DefaultGoApiResponse defaultGoApiResponse = new DefaultGoApiResponse(500);
        defaultGoApiResponse.setResponseBody(responseBody);
        return defaultGoApiResponse;
    }

    /**
     * Creates an instance DefaultGoApiResponse which represents success request with response code 200
     *
     * @param responseBody Json formatted response body
     * @return an instance of DefaultGoApiResponse
     */
    public static DefaultGoApiResponse success(String responseBody) {
        DefaultGoApiResponse defaultGoApiResponse = new DefaultGoApiResponse(SUCCESS_RESPONSE_CODE);
        defaultGoApiResponse.setResponseBody(responseBody);
        return defaultGoApiResponse;
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
        return unmodifiableMap(responseHeaders);
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
