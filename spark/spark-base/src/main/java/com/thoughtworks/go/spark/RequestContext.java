/*
 * Copyright Thoughtworks, Inc.
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

import java.net.URI;
import java.net.URISyntaxException;

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

    public Link build(String name, String encodedPathAfterContext) {
        return new Link(name, urlFor(encodedPathAfterContext));
    }

    public String pathFor(String encodedPathAfterContext) {
        return contextPath + encodedPathAfterContext;
    }

    public String urlFor(String encodedPathAfterContext) {
        try {
            // Append path separately; otherwise it will be double-encoded
            URI rootUri = new URI(protocol, null, host, port, null, null, null);
            return rootUri + pathFor(encodedPathAfterContext);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
