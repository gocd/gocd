/*
 * Copyright 2020 ThoughtWorks, Inc.
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

package com.thoughtworks.go.spark.spring;

import spark.route.HttpMethod;

import java.util.List;

import static org.apache.commons.lang3.StringUtils.startsWith;

public class RouteEntry {
    private final List<String> METHODS = List.of("get", "post", "put", "patch", "delete", "trace");
    private HttpMethod httpMethod;
    private String path;
    private String acceptedType;
    private Object target;

    public RouteEntry(HttpMethod httpMethod, String path, String acceptedType, Object target) {
        this.httpMethod = httpMethod;
        this.path = path;
        this.acceptedType = acceptedType;
        this.target = target;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public String getAcceptedType() {
        return acceptedType;
    }

    public Object getTarget() {
        return target;
    }

    public boolean isAPI() {
        return METHODS.contains(httpMethod.name().toLowerCase()) &&
            startsWith(acceptedType, "application/vnd.go.cd");
    }
}
