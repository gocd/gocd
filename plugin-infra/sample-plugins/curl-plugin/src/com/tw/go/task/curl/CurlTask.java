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

package com.tw.go.task.curl;

import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.api.task.*;
import org.apache.commons.io.IOUtils;
import com.google.gson.GsonBuilder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@Extension
public class CurlTask implements GoPlugin {

    public static final String URL_PROPERTY = "Url";
    public static final String ADDITIONAL_OPTIONS = "AdditionalOptions";
    public static final String SECURE_CONNECTION = "yes";
    public static final String SECURE_CONNECTION_PROPERTY = "SecureConnection";
    public static final String REQUEST_TYPE = "-G";
    public static final String REQUEST_PROPERTY = "RequestType";
    private Logger logger = Logger.getLoggerFor(CurlTask.class);

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest request) throws UnhandledRequestTypeException {
        if ("configuration".equals(request.requestName())) {
            return handleGetConfigRequest();
        } else if ("validate".equals(request.requestName())) {
            return handleValidation(request);
        } else if ("execute".equals(request.requestName())) {
            return handleTaskExecution(request);
        } else if ("view".equals(request.requestName())) {
            return handleTaskView();
        }
        throw new UnhandledRequestTypeException(request.requestName());
    }

    private GoPluginApiResponse handleTaskView() {
        int responseCode = DefaultGoApiResponse.SUCCESS_RESPONSE_CODE;
        HashMap view = new HashMap();
        view.put("displayValue", "Curl");
        try {
            view.put("template", IOUtils.toString(getClass().getResourceAsStream("/views/task.template.html"), "UTF-8"));
        } catch (Exception e) {
            responseCode = DefaultGoApiResponse.INTERNAL_ERROR;
            String errorMessage = "Failed to find template: " + e.getMessage();
            view.put("exception", errorMessage);
            logger.error(errorMessage, e);
        }
        return createResponse(responseCode, view);
    }

    private GoPluginApiResponse handleTaskExecution(GoPluginApiRequest request) {
        CurlTaskExecutor executor = new CurlTaskExecutor();
        Map executionRequest = (Map) new GsonBuilder().create().fromJson(request.requestBody(), Object.class);
        Map config = (Map) executionRequest.get("config");
        Map context = (Map) executionRequest.get("context");

        Result result = executor.execute(new Config(config), new Context(context), JobConsoleLogger.getConsoleLogger());
        return createResponse(result.responseCode(), result.toMap());
    }

    private GoPluginApiResponse handleValidation(GoPluginApiRequest request) {
        HashMap validationResult = new HashMap();
        int responseCode = DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE;
        Map configMap = (Map) new GsonBuilder().create().fromJson(request.requestBody(), Object.class);
        HashMap errorMap = new HashMap();
        if (!configMap.containsKey(URL_PROPERTY) || ((Map) configMap.get(URL_PROPERTY)).get("value") == null || ((String) ((Map) configMap.get(URL_PROPERTY)).get("value")).trim().isEmpty()) {
            errorMap.put(URL_PROPERTY, "URL cannot be empty");
        }
        validationResult.put("errors", errorMap);
        return createResponse(responseCode, validationResult);
    }

    private GoPluginApiResponse handleGetConfigRequest() {
        HashMap config = new HashMap();
        HashMap url = new HashMap();
        url.put("display-order", "0");
        url.put("display-name", "Url");
        url.put("required", true);
        config.put(URL_PROPERTY, url);

        HashMap secure = new HashMap();
        secure.put("default-value", SECURE_CONNECTION);
        secure.put("display-order", "1");
        secure.put("display-name", "Secure Connection");
        secure.put("require", false);
        config.put(SECURE_CONNECTION_PROPERTY, secure);

        HashMap requestType = new HashMap();
        requestType.put("default-value", REQUEST_TYPE);
        requestType.put("display-order", "2");
        requestType.put("display-name", "Request Type");
        requestType.put("require", false);
        config.put(REQUEST_PROPERTY, requestType);

        HashMap additionalOptions = new HashMap();
        additionalOptions.put("display-order", "3");
        additionalOptions.put("display-name", "Additional Options");
        additionalOptions.put("require", false);
        config.put(ADDITIONAL_OPTIONS, additionalOptions);
        return createResponse(DefaultGoPluginApiResponse.SUCCESS_RESPONSE_CODE, config);
    }

    private GoPluginApiResponse createResponse(int responseCode, Map body) {
        final DefaultGoPluginApiResponse response = new DefaultGoPluginApiResponse(responseCode);
        response.setResponseBody(new GsonBuilder().serializeNulls().create().toJson(body));
        return response;
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier("task", Arrays.asList("1.0"));
    }
}
