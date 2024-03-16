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
package com.thoughtworks.go.spark;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.thoughtworks.go.api.ApiVersion;
import com.thoughtworks.go.config.exceptions.HttpException;
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException;
import com.thoughtworks.go.spark.spring.SparkSpringController;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.MediaType.*;
import static spark.Spark.*;

public class RoutesHelper {
    private static final Logger LOG = LoggerFactory.getLogger(RoutesHelper.class);
    private static final Gson GSON = new Gson();
    private static final String TIMER_START = RuntimeHeaderEmitter.class.getName();

    private final List<SparkSpringController> controllers;
    private final List<SparkController> sparkControllers;

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
        before("/*", (request, response) -> request.attribute(TIMER_START, new RuntimeHeaderEmitter(response)));
        before("/*", (request, response) -> response.header("Cache-Control", "max-age=0, private, must-revalidate"));

        controllers.forEach(this::addDeprecationHeaders);

        controllers.forEach(SparkSpringController::setupRoutes);
        sparkControllers.forEach(SparkController::setupRoutes);

        exception(HttpException.class, this::httpException);

        exception(JsonParseException.class, this::invalidJsonPayload);
        exception(UnprocessableEntityException.class, this::unprocessableEntity);
        exception(Exception.class, this::unhandledException);

        afterAfter("/*", (request, response) -> request.<RuntimeHeaderEmitter>attribute(TIMER_START).render());
    }

    private void addDeprecationHeaders(SparkSpringController controller) {
        boolean isDeprecated = controller.getClass().isAnnotationPresent(DeprecatedAPI.class);
        if (!isDeprecated) {
            return;
        }

        DeprecatedAPI deprecated = controller.getClass().getAnnotation(DeprecatedAPI.class);
        String controllerBasePath, mimeType;

        try {
            controllerBasePath = (String) controller.getClass().getMethod("controllerBasePath").invoke(controller);
            Field field = controller.getClass().getSuperclass().getDeclaredField("mimeType");
            field.setAccessible(true);
            mimeType = (String) field.get(controller);
        } catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }

        path(controllerBasePath, () -> {
            before("", mimeType, (req, res) -> setDeprecationHeaders(req, res, deprecated));
            before("/*", mimeType, (req, res) -> setDeprecationHeaders(req, res, deprecated));
        });
    }

    void httpException(HttpException ex, Request req, Response res) {
        res.status(ex.getStatus().value());
        List<String> acceptedTypes = getAcceptedTypesFromRequest(req);

        if (containsAny(acceptedTypes, TEXT_HTML_VALUE, APPLICATION_XHTML_XML_VALUE)) {
            res.body(HtmlErrorPage.errorPage(ex.getStatus().value(), ex.getMessage()));
        } else if (containsAny(acceptedTypes, APPLICATION_XML_VALUE, TEXT_XML_VALUE, APPLICATION_RSS_XML_VALUE, APPLICATION_ATOM_XML_VALUE)) {
            res.body(ex.asXML());
        } else {
            res.body(GSON.toJson(Map.of("message", ex.getMessage())));
        }
    }

    private void unprocessableEntity(UnprocessableEntityException ex, Request request, Response response) {
        response.status(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        response.body(GSON.toJson(Map.of("message", "Your request could not be processed. " + ex.getMessage())));
    }

    void unhandledException(Exception ex, Request req, Response res) {
        final String query = req.queryString();
        final String uri = req.requestMethod() + " " + (isNotBlank(query) ? req.pathInfo() + "?" + query : req.pathInfo());
        LOG.error(format("Unhandled exception on [%s]: %s", uri, ex.getMessage()), ex);

        res.status(HttpURLConnection.HTTP_INTERNAL_ERROR);
        res.body(GSON.toJson(Map.of("error", ex.getMessage() == null ? "Internal server error" : ex.getMessage())));
    }

    private boolean containsAny(List<String> list, String... strs) {
        return Arrays.stream(strs).anyMatch(list::contains);
    }

    private List<String> getAcceptedTypesFromRequest(Request request) {
        String acceptHeader = request.headers("Accept");
        if (StringUtils.isBlank(acceptHeader)) {
            return Collections.emptyList();
        }

        return List.of(acceptHeader.trim().toLowerCase().split("\\s*,\\s*"));
    }

    private void invalidJsonPayload(JsonParseException ex, Request req, Response res) {
        res.status(HttpStatus.SC_BAD_REQUEST);
        res.body(GSON.toJson(Map.of("error", "Payload data is not valid JSON: " + ex.getMessage())));
    }

    private static class RuntimeHeaderEmitter {
        private final Response response;
        private final long timerStart;

        public RuntimeHeaderEmitter(Response response) {
            this.timerStart = System.currentTimeMillis();
            this.response = response;
        }

        public void render() {
            if (!response.raw().isCommitted()) {
                response.header("X-Runtime", String.valueOf(System.currentTimeMillis() - timerStart));
            }
        }
    }

    void setDeprecationHeaders(Request request, Response response, DeprecatedAPI controller) {
        String deprecatedRelease = controller.deprecatedIn();
        String removalRelease = controller.removalIn();
        String entityName = controller.entityName();
        ApiVersion deprecatedApiVersion = controller.deprecatedApiVersion();
        ApiVersion successorApiVersion = controller.successorApiVersion();

        String changelogUrl = format("https://api.gocd.org/%s/#api-changelog", deprecatedRelease);
        String link = format("<%s>; Accept=\"%s\"; rel=\"successor-version\"", request.url(), successorApiVersion.mimeType());
        String warning = format("299 GoCD/v%s \"The %s API version %s has been deprecated in GoCD Release v%s. This version will be removed in GoCD Release v%s. Version %s of the API is available, and users are encouraged to use it\"", deprecatedRelease, entityName, deprecatedApiVersion, deprecatedRelease, removalRelease, successorApiVersion);

        response.header("X-GoCD-API-Deprecated-In", format("v%s", deprecatedRelease));
        response.header("X-GoCD-API-Removal-In", format("v%s", removalRelease));
        response.header("X-GoCD-API-Deprecation-Info", changelogUrl);
        response.header("Link", link);
        response.header("Warning", warning);
    }
}
