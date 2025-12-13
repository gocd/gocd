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
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.apache.http.HttpHeaders;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

import static com.thoughtworks.go.util.GoConstants.ERROR_FOR_JSON;
import static com.thoughtworks.go.util.GoConstants.RESPONSE_CHARSET_JSON;
import static java.net.HttpURLConnection.*;

public class JsonAction implements RestfulAction {
    private final int status;
    private final Object json;

    public static JsonAction from(ServerHealthState serverHealthState) {
        if (serverHealthState.isSuccess()) {
            return jsonCreated(new LinkedHashMap<>());
        }

        Map<String, Object> jsonLog = new LinkedHashMap<>();
        jsonLog.put(ERROR_FOR_JSON, serverHealthState.getDescription());
        return new JsonAction(serverHealthState.getType().getHttpCode(), jsonLog);
    }

    public static JsonAction jsonCreated(Object json) {
        return new JsonAction(HTTP_CREATED, json);
    }

    public static JsonAction jsonFound(Object json) {
        return new JsonAction(HTTP_OK, json);
    }

    public static JsonAction jsonNotAcceptable(Object json) {
        return new JsonAction(HTTP_NOT_ACCEPTABLE, json);
    }

    public static JsonAction jsonForbidden() {
        return new JsonAction(HTTP_FORBIDDEN, new LinkedHashMap<>());
    }

    public static JsonAction jsonForbidden(String message) {
        return new JsonAction(HTTP_FORBIDDEN, JsonView.getSimpleAjaxResult(ERROR_FOR_JSON, message));
    }

    public static JsonAction jsonBadRequest(Object json) {
        return new JsonAction(HTTP_BAD_REQUEST, json);
    }

    public static JsonAction jsonNotFound(Object json) {
        return new JsonAction(HTTP_NOT_FOUND, json);
    }

    public static JsonAction jsonConflict(Object json) {
        return new JsonAction(HTTP_CONFLICT, json);
    }

    public static JsonAction jsonByValidity(Object json, GoConfigValidity.InvalidGoConfig configValidity) {
        return (Stream.of(GoConfigValidity.VT_CONFLICT, GoConfigValidity.VT_MERGE_OPERATION_ERROR, GoConfigValidity.VT_MERGE_POST_VALIDATION_ERROR, GoConfigValidity.VT_MERGE_PRE_VALIDATION_ERROR).anyMatch(configValidity::isType)) ? jsonConflict(json) : jsonNotFound(json);
    }

    @Override
    public ModelAndView respond(HttpServletResponse response) {
        return new JsonModelAndView(response, json, status);
    }

    private JsonAction(int status, Object json) {
        this.status = status;
        this.json = json;
    }

    public ModelAndView createView() {
        SimpleJsonView view = new SimpleJsonView(status, json);
        return new ModelAndView(view, JsonView.asMap(json));
    }

    private static final String CLEAR_CACHE = "max-age=1, no-cache";

    public static class SimpleJsonView implements View {
        private final int status;
        private final Object json;

        public SimpleJsonView(int status, Object json) {
            this.status = status;
            this.json = json;
        }

        @Override
        public String getContentType() {
            return RESPONSE_CHARSET_JSON;
        }

        @Override
        public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws IOException {
            // In IE, there's a problem with caching. We want to cache if we can.
            // This will force the browser to clear the cache only for this page.
            // If any other pages need to clear the cache, we might want to move this
            // logic to an interceptor.
            GoRequestContext goRequestContext = new GoRequestContext(request);
            response.addHeader(HttpHeaders.CACHE_CONTROL, CLEAR_CACHE);
            response.setStatus(status);
            response.setContentType(getContentType());
            try (PrintWriter writer = response.getWriter()) {
                JsonRenderer.render(json, goRequestContext, writer);
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
            response.addHeader(HttpHeaders.CACHE_CONTROL, CLEAR_CACHE);
            response.setStatus(status);
            response.setContentType(RESPONSE_CHARSET_JSON);
        }
    }
}
