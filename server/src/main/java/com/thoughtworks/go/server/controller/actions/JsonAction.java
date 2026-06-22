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
package com.thoughtworks.go.server.controller.actions;

import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.server.web.GoRequestContext;
import com.thoughtworks.go.server.web.JsonRenderer;
import com.thoughtworks.go.server.web.JsonView;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.stream.Stream;

import static com.thoughtworks.go.util.json.JsonAware.ERROR_KEY;
import static java.net.HttpURLConnection.*;

public class JsonAction implements RestfulAction {
    public static final String CONTENT_TYPE = "application/json; charset=utf-8";
    private static final String CACHE_CONTROL_CLEAR = "max-age=1, no-cache";

    private final int status;
    private final Object jsonSerializable;

    public static JsonAction jsonFound(Object jsonSerializable) {
        return new JsonAction(HTTP_OK, jsonSerializable);
    }

    public static JsonAction jsonNotAcceptable(String message) {
        return new JsonAction(HTTP_NOT_ACCEPTABLE, Map.of(ERROR_KEY, message));
    }

    public static JsonAction jsonForbidden(String message) {
        return new JsonAction(HTTP_FORBIDDEN, Map.of(ERROR_KEY, message));
    }

    public static JsonAction jsonBadRequest(String message) {
        return new JsonAction(HTTP_BAD_REQUEST, Map.of(ERROR_KEY, message));
    }

    public static JsonAction jsonNotFound(Object json) {
        return new JsonAction(HTTP_NOT_FOUND, json);
    }

    public static JsonAction jsonConflict(Object json) {
        return new JsonAction(HTTP_CONFLICT, json);
    }

    public static JsonAction jsonByValidity(Object json, GoConfigValidity.InvalidGoConfig configValidity) {
        return Stream.of(GoConfigValidity.VT_CONFLICT, GoConfigValidity.VT_MERGE_OPERATION_ERROR, GoConfigValidity.VT_MERGE_POST_VALIDATION_ERROR, GoConfigValidity.VT_MERGE_PRE_VALIDATION_ERROR).anyMatch(configValidity::isType) ? jsonConflict(json) : jsonNotFound(json);
    }

    @Override
    public ModelAndView respond(HttpServletResponse response) {
        return new JsonModelAndView(response, jsonSerializable, status);
    }

    private JsonAction(int status, Object jsonSerializable) {
        this.status = status;
        this.jsonSerializable = jsonSerializable;
    }

    public ModelAndView createView() {
        SimpleJsonView view = new SimpleJsonView(status, jsonSerializable);
        return new ModelAndView(view, JsonView.asMap(jsonSerializable));
    }

    private static class SimpleJsonView implements View {
        private final int status;
        private final Object jsonSerializable;

        private SimpleJsonView(int status, Object jsonSerializable) {
            this.status = status;
            this.jsonSerializable = jsonSerializable;
        }

        @Override
        public String getContentType() {
            return CONTENT_TYPE;
        }

        @Override
        public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws IOException {
            // In IE, there's a problem with caching. We want to cache if we can.
            // This will force the browser to clear the cache only for this page.
            // If any other pages need to clear the cache, we might want to move this
            // logic to an interceptor.
            GoRequestContext goRequestContext = new GoRequestContext(request);
            response.addHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_CLEAR);
            response.setStatus(status);
            response.setContentType(getContentType());
            try (PrintWriter writer = response.getWriter()) {
                JsonRenderer.render(jsonSerializable, goRequestContext, writer);
            }
        }
    }

    private static class JsonModelAndView extends ModelAndView {
        @Override
        public String getViewName() {
            return "jsonView";
        }

        public JsonModelAndView(HttpServletResponse response, Object json, int status) {
            super(new JsonView(), JsonView.asMap(json));
            // In IE, there's a problem with caching. We want to cache if we can.
            // This will force the browser to clear the cache only for this page.
            // If any other pages need to clear the cache, we might want to move this
            // logic to an interceptor.
            response.addHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_CLEAR);
            response.setStatus(status);
            response.setContentType(CONTENT_TYPE);
        }
    }
}
