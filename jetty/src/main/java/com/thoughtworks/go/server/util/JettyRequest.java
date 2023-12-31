/*
 * Copyright 2024 Thoughtworks, Inc.
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

import org.eclipse.jetty.http.HttpURI;
import org.eclipse.jetty.server.Request;

import javax.servlet.ServletRequestWrapper;
import java.util.function.Function;

public class JettyRequest implements ServletRequest {
    private final Request request;

    public JettyRequest(javax.servlet.ServletRequest request) {
        while (request instanceof ServletRequestWrapper) {
            request = ((ServletRequestWrapper) request).getRequest();
        }

        this.request = (Request) request;
    }

    @Override
    public void modifyPath(Function<String, String> pathModifier) {
        request.setHttpURI(HttpURI.build(request.getHttpURI()).path(pathModifier.apply(request.getHttpURI().getPath())).asImmutable());
    }

    @Override
    public String getRootURL() {
        return request.getRootURL().toString();
    }
}
