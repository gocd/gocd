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

import java.util.Map;

public class Config {
    private final String requestType;
    private final String secureConnection;
    private final String additionalOptions;
    private final String url;

    public Config(Map config) {
        requestType = getValue(config, CurlTask.REQUEST_PROPERTY);
        secureConnection = getValue(config, CurlTask.SECURE_CONNECTION_PROPERTY);
        additionalOptions = getValue(config, CurlTask.ADDITIONAL_OPTIONS);
        url = getValue(config, CurlTask.URL_PROPERTY);
    }

    private String getValue(Map config, String property) {
        return (String) ((Map) config.get(property)).get("value");
    }

    public String getRequestType() {
        return requestType;
    }

    public String getSecureConnection() {
        return secureConnection;
    }

    public String getAdditionalOptions() {
        return additionalOptions;
    }

    public String getUrl() {
        return url;
    }
}