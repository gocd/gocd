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
package com.thoughtworks.go.api;


import com.thoughtworks.go.api.base.JsonOutputWriter;
import com.thoughtworks.go.api.base.OutputListWriter;
import com.thoughtworks.go.api.base.OutputWriter;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.spark.RequestContext;
import org.apache.commons.codec.digest.DigestUtils;
import spark.Request;
import spark.Response;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Objects;
import java.util.function.Consumer;

public interface ControllerMethods {

    String NOTHING = "";

    default boolean fresh(Request req, String etagFromServer) {
        String etagFromClient = getIfNoneMatch(req);
        if (etagFromClient == null) {
            return false;
        }
        return Objects.equals(etagFromClient, etagFromServer);
    }

    default String getIfNoneMatch(Request request) {
        return preconditionHeader(request, "If-None-Match");
    }

    default String getIfMatch(Request request) {
        return preconditionHeader(request, "If-Match");
    }

    default String preconditionHeader(Request request, String header) {
        //possibly move to a filter/middleware?
        String etag = request.headers(header);
        if (etag == null) {
            return null;
        }
        // workaround for how jetty's gzip handler modifies the etag
        return etag.replaceAll("^\"(.*)\"$", "$1").replaceAll("(.*)(--(gzip|deflate))", "$1");
    }

    default String notModified(Response res) {
        res.status(304);
        return NOTHING;
    }

    default void setEtagHeader(Response res, String value) {
        if (value == null) {
            return;
        }
        res.header("ETag", '"' + value + '"');
    }

    default String etagFor(String str) {
        return DigestUtils.md5Hex(str);
    }

    default String renderHTTPOperationResult(HttpLocalizedOperationResult result, Request request, Response response) throws IOException {
        response.status(result.httpCode());
        return writerForTopLevelObject(request, response, writer -> writer.add("message", result.message()));
    }

    default String renderHTTPOperationResult(HttpOperationResult result, Request request, Response response) throws IOException {
        response.status(result.httpCode());
        return writerForTopLevelObject(request, response, writer -> writer.add("message", result.fullMessage()));
    }

    default String writerForTopLevelObject(Request request, Response response, Consumer<OutputWriter> consumer) throws IOException {
        new JsonOutputWriter(response.raw().getWriter(), RequestContext.requestContext(request)).forTopLevelObject(consumer);
        return NOTHING;
    }

    default String writerForTopLevelArray(Request request, Response response, Consumer<OutputListWriter> consumer) throws IOException {
        new JsonOutputWriter(response.raw().getWriter(), RequestContext.requestContext(request)).forTopLevelArray(consumer);
        return NOTHING;
    }

    default String jsonizeAsTopLevelObject(Request request, Consumer<OutputWriter> consumer) {
        StringWriter writer = new StringWriter(1024);
        new JsonOutputWriter(writer, RequestContext.requestContext(request)).forTopLevelObject(consumer);
        return writer.toString();
    }

    default String jsonizeAsTopLevelArray(Request request, Consumer<OutputListWriter> consumer) {
        StringWriter writer = new StringWriter(1024);
        new JsonOutputWriter(writer, RequestContext.requestContext(request)).forTopLevelArray(consumer);
        return writer.toString();
    }

}
