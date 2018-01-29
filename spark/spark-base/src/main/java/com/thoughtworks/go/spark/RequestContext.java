/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import com.thoughtworks.go.util.SystemEnvironment;
import spark.Request;

import java.net.MalformedURLException;
import java.net.URL;

public class RequestContext {

    private final String host;
    private final int port;
    private final String protocol;
    private final SystemEnvironment systemEnvironment;

    public RequestContext(RequestContext requestContext) {
        this.host = requestContext.host;
        this.port = requestContext.port;
        this.protocol = requestContext.protocol;
        this.systemEnvironment = requestContext.systemEnvironment;
    }

    public RequestContext(String protocol, String host, int port) {
        this.host = host;
        this.port = protocol.equalsIgnoreCase("https") && port == 443 || protocol.equals("http") && port == 80 ? -1 : port;
        this.protocol = protocol;
        this.systemEnvironment = new SystemEnvironment();
    }

    public static RequestContext requestContext(Request req) {
        return new RequestContext(req.scheme(), req.raw().getServerName(), req.port());
    }

    public Link build(String name, String template, Object... args) {
        try {
            String href = String.format(template, args);
            return getLink(name, href);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private Link getLink(String name, String href) throws MalformedURLException {
        return new Link(name, new URL(protocol, host, port, systemEnvironment.getWebappContextPath() +  href).toExternalForm());
    }
}
