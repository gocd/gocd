/*
 * Copyright 2015 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.web.JsonView;
import com.thoughtworks.go.server.web.SimpleJsonView;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import com.thoughtworks.go.util.GoConstants;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.util.GoConstants.ERROR_FOR_JSON;
import static com.thoughtworks.go.util.GoConstants.RESPONSE_CHARSET_JSON;
import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.commons.httpclient.HttpStatus.SC_UNAUTHORIZED;

public class JsonAction implements RestfulAction {
    private final int status;
    private final Object json;

    public static JsonAction from(ServerHealthState serverHealthState) {
        if (serverHealthState.isSuccess()) {
            return jsonCreated(new LinkedHashMap());
        }

        Map<String, Object> jsonLog = new LinkedHashMap<>();
        jsonLog.put(ERROR_FOR_JSON, serverHealthState.getDescription());
        return new JsonAction(serverHealthState.getType().getHttpCode(), jsonLog);
    }

    public static JsonAction jsonCreated(Object json) {
        return new JsonAction(SC_CREATED, json);
    }

    public static JsonAction jsonFound(Object json) {
        return new JsonAction(SC_OK, json);
    }

    public static JsonAction jsonOK() {
        return jsonOK(new LinkedHashMap());
    }

    public static JsonAction jsonPaymentRequired(Object json) {
        return new JsonAction(SC_PAYMENT_REQUIRED, json);
    }

    public static JsonAction jsonNotAcceptable(Object json) {
        return new JsonAction(SC_NOT_ACCEPTABLE, json);
    }

    public static JsonAction jsonUnauthorized() {
        return new JsonAction(SC_UNAUTHORIZED, new LinkedHashMap());
    }

    public static JsonAction jsonUnauthorized(String message) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put(ERROR_FOR_JSON, message);
        return new JsonAction(SC_UNAUTHORIZED, map);
    }

    public static JsonAction jsonUnauthorized(Exception e) {
        return jsonUnauthorized(e.getMessage());
    }

    public static JsonAction jsonBadRequest(Object json) {
        return new JsonAction(SC_BAD_REQUEST, json);
    }

    public static JsonAction jsonNotFound(Object json) {
        return new JsonAction(SC_NOT_FOUND, json);
    }

    public static JsonAction jsonConflict(Object json) {
        return new JsonAction(SC_CONFLICT, json);
    }

    public static JsonAction jsonByValidity(Object json, GoConfigValidity configValidity) {
        return (configValidity.isType(GoConfigValidity.VT_CONFLICT) ||
                configValidity.isType(GoConfigValidity.VT_MERGE_OPERATION_ERROR) ||
                configValidity.isType(GoConfigValidity.VT_MERGE_POST_VALIDATION_ERROR) ||
                configValidity.isType(GoConfigValidity.VT_MERGE_PRE_VALIDATION_ERROR)) ? jsonConflict(json) : jsonNotFound(json);
    }

    /**
     * @deprecated replace with createView
     */
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

    public static JsonAction jsonOK(Map jsonMap) {
        return new JsonAction(SC_OK, jsonMap);
    }

    private class JsonModelAndView extends ModelAndView {

        public String getViewName() {
            return "jsonView";
        }

        public JsonModelAndView(HttpServletResponse response, Object json, int status) {
            super(new JsonView(), JsonView.asMap(json));
            // In IE, there's a problem with caching. We want to cache if we can.
            // This will force the browser to clear the cache only for this page.
            // If any other pages need to clear the cache, we might want to move this
            // logic to an intercepter.
            response.addHeader("Cache-Control", GoConstants.CACHE_CONTROL);
            response.setStatus(status);
            response.setContentType(RESPONSE_CHARSET_JSON);
        }
    }

}
