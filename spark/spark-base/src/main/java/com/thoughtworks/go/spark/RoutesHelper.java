/*
 * Copyright 2019 ThoughtWorks, Inc.
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


import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.thoughtworks.go.config.exceptions.HttpException;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import spark.Request;
import spark.Response;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static spark.Spark.*;

public class RoutesHelper {
    private static final String TIMER_START = RuntimeHeaderEmitter.class.getName();

    private List<SparkSpringController> controllers;
    private List<SparkController> sparkControllers;

    public RoutesHelper(SparkSpringController... controllers) {
        this(controllers, null);
    }

    public RoutesHelper(SparkController... sparkControllers) {
        this(null, sparkControllers);
    }

    private RoutesHelper(SparkSpringController[] controllers, SparkController[] apiControllers) {
        this.controllers = controllers == null ? Collections.emptyList() : Arrays.asList(controllers);
        this.sparkControllers = apiControllers == null ? Collections.emptyList() : Arrays.asList(apiControllers);
    }

    public void init() {
        before("/*", (request, response) -> request.attribute(TIMER_START, new RuntimeHeaderEmitter(request, response)));
        before("/*", (request, response) -> response.header("Cache-Control", "max-age=0, private, must-revalidate"));

        controllers.forEach(SparkSpringController::setupRoutes);
        sparkControllers.forEach(SparkController::setupRoutes);

        exception(HttpException.class, this::httpException);

        exception(JsonParseException.class, this::invalidJsonPayload);
        exception(UnprocessableEntityException.class, this::unprocessableEntity);

        afterAfter("/*", (request, response) -> request.<RuntimeHeaderEmitter>attribute(TIMER_START).render());
    }

    private void unprocessableEntity(UnprocessableEntityException exception, Request request, Response response) {
        response.status(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        response.body(new Gson().toJson(Collections.singletonMap("message", "Your request could not be processed. " + exception.getMessage())));
    }

    private void httpException(HttpException ex, Request req, Response res) {
        res.status(ex.getStatus().value());
        List<String> acceptedTypes = getAcceptedTypesFromRequest(req);

        if (acceptedTypes.contains("text/html") || acceptedTypes.contains("application/xhtml+xml")) {
            res.body(HtmlErrorPage.errorPage(ex.getStatus().value(), ex.getMessage()));
        } else {
            res.body(new Gson().toJson(Collections.singletonMap("message", ex.getMessage())));
        }
    }

    private List<String> getAcceptedTypesFromRequest(Request request) {
        String acceptHeader = request.headers("Accept");
        if (StringUtils.isBlank(acceptHeader)) {
            return Collections.emptyList();
        }

        return Arrays.asList(acceptHeader.toLowerCase().split(","));
    }

    private void invalidJsonPayload(JsonParseException ex, Request req, Response res) {
        res.status(HttpStatus.SC_BAD_REQUEST);
        res.body(new Gson().toJson(Collections.singletonMap("error", "Payload data is not valid JSON: " + ex.getMessage())));
    }

    private class RuntimeHeaderEmitter {
        private final Request request;
        private final Response response;
        private final long timerStart;

        public RuntimeHeaderEmitter(Request request, Response response) {
            this.timerStart = System.currentTimeMillis();
            this.request = request;
            this.response = response;
        }

        public void render() {
            if (!response.raw().isCommitted()) {
                response.header("X-Runtime", String.valueOf(System.currentTimeMillis() - timerStart));
            }
        }
    }
}
