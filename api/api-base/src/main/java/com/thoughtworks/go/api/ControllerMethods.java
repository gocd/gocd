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

package com.thoughtworks.go.api;


import com.thoughtworks.go.api.util.MessageJson;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.springframework.http.HttpStatus;
import spark.Request;
import spark.Response;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import static com.thoughtworks.go.api.util.HaltApiMessages.notFoundMessage;

public interface ControllerMethods {

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

    default <ALWAYS_NULL> ALWAYS_NULL notModified(Response res) {
        res.status(304);
        return null;
    }

    default void notFound(Exception ex, Request req, Response res) {
        res.status(HttpStatus.NOT_FOUND.value());
        res.body(MessageJson.create(notFoundMessage()));
    }

    default void setEtagHeader(Response res, String value) {
        if (value == null) {
            return;
        }
        res.header("ETag", '"' + value + '"');
    }

    default Map<String, Object> renderHTTPOperationResult(HttpLocalizedOperationResult result, Response response, Localizer localizer) {
        response.status(result.httpCode());
        return Collections.singletonMap("message", result.message(localizer));
    }

}
