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
package com.thoughtworks.go.spark;

import spark.Request;

import java.net.MalformedURLException;
import java.net.URL;

public class RequestContext {

    private final String host;
    private final int port;
    private final String protocol;
    private final String contextPath;

    public RequestContext(String protocol, String host, int port, String contextPath) {
        this.host = host;
        this.port = protocol.equalsIgnoreCase("https") && port == 443 || protocol.equals("http") && port == 80 ? -1 : port;
        this.protocol = protocol;
        this.contextPath = contextPath;
    }

    public static RequestContext requestContext(Request req) {
        return new RequestContext(req.scheme(), req.raw().getServerName(), req.port(), req.contextPath());
    }

    public Link build(String name, String pathAfterContext) {
        return new Link(name, urlFor(pathAfterContext));
    }

    public String pathWithContext(String pathAfterContext) {
        try {
            return new URL(protocol, host, port, contextPath + pathAfterContext).getPath();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public String urlFor(String pathAfterContext) {
        try {
            return new URL(protocol, host, port, contextPath + pathAfterContext).toExternalForm();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
