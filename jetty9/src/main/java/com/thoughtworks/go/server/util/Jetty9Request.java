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
package com.thoughtworks.go.server.util;

import org.eclipse.jetty.server.Request;

import javax.servlet.ServletRequestWrapper;

public class Jetty9Request implements ServletRequest {
    private Request request;

    public Jetty9Request(javax.servlet.ServletRequest request) {
        while (request instanceof ServletRequestWrapper) {
            request = ((ServletRequestWrapper) request).getRequest();
        }

        this.request = (Request) request;
    }

    @Override
    public String getUrl() {
        return request.getRootURL().append(getUriAsString()).toString();
    }

    @Override
    public String getUriPath() {
        return request.getHttpURI().getPath();
    }

    @Override
    public String getUriAsString() {
        return request.getHttpURI().getPathQuery();
    }

    @Override
    public void setRequestURI(String uri) {
        request.getHttpURI().setPath(uri);
    }

    @Override
    public String getRootURL() {
        return request.getRootURL().toString();
    }
}
