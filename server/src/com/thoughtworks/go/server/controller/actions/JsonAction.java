/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.controller.actions;

import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.json.JsonAware;
import com.thoughtworks.go.util.json.JsonMap;
import com.thoughtworks.go.server.web.JsonView;
import com.thoughtworks.go.server.web.SimpleJsonView;
import com.thoughtworks.go.serverhealth.ServerHealthState;
import org.springframework.web.servlet.ModelAndView;

import static com.thoughtworks.go.util.GoConstants.ERROR_FOR_JSON;
import static com.thoughtworks.go.util.GoConstants.RESPONSE_CHARSET;
import static com.thoughtworks.go.util.GoConstants.RESPONSE_CHARSET_JSON;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_CONFLICT;
import static javax.servlet.http.HttpServletResponse.SC_CREATED;
import static javax.servlet.http.HttpServletResponse.SC_NOT_ACCEPTABLE;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static javax.servlet.http.HttpServletResponse.SC_PAYMENT_REQUIRED;
import static org.apache.commons.httpclient.HttpStatus.SC_UNAUTHORIZED;

public class JsonAction implements RestfulAction {
    private final int status;
    private final JsonAware json;

    public static JsonAction from(ServerHealthState serverHealthState) {
        if (serverHealthState.isSuccess()) {
            return JsonAction.jsonCreated(new JsonMap());
        }

        JsonMap jsonLog = new JsonMap();
        jsonLog.put(ERROR_FOR_JSON, serverHealthState.getDescription());
        return new JsonAction(serverHealthState.getType().getHttpCode(), jsonLog);
    }

    public static JsonAction jsonCreated(JsonAware json) {
        return new JsonAction(SC_CREATED, json);
    }

    public static JsonAction jsonFound(JsonAware json) {
        return new JsonAction(SC_OK, json);
    }

    public static JsonAction jsonOK() {
        return jsonOK(new JsonMap());
    }

    public static JsonAction jsonPaymentRequired(JsonAware json) {
        return new JsonAction(SC_PAYMENT_REQUIRED, json);
    }

    public static JsonAction jsonNotAcceptable(JsonAware json) {
        return new JsonAction(SC_NOT_ACCEPTABLE, json);
    }

    public static JsonAction jsonUnauthorized() {
        return new JsonAction(SC_UNAUTHORIZED, new JsonMap());
    }

    public static JsonAction jsonUnauthorized(String message) {
        JsonMap map = new JsonMap();
        map.put(ERROR_FOR_JSON, message);
        return new JsonAction(SC_UNAUTHORIZED, map);
    }

    public static JsonAction jsonUnauthorized(Exception e) {
        return jsonUnauthorized(e.getMessage());
    }

    public static JsonAction jsonBadRequest(JsonAware json) {
        return new JsonAction(SC_BAD_REQUEST, json);
    }

    public static JsonAction jsonNotFound(JsonAware json) {
        return new JsonAction(SC_NOT_FOUND, json);
    }

    public static JsonAction jsonConflict(JsonAware json) {
        return new JsonAction(SC_CONFLICT, json);
    }

    public static JsonAction jsonByValidity(JsonAware json, GoConfigValidity configValidity) {
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

    private JsonAction(int status, JsonAware json) {
        this.status = status;
        this.json = json;
    }

    public ModelAndView createView() {
        SimpleJsonView view = new SimpleJsonView(status, json);
        return new ModelAndView(view, JsonView.asMap(json.toJson()));
    }

    public static JsonAction jsonOK(JsonMap jsonMap) {
        return new JsonAction(SC_OK, jsonMap);
    }

    private class JsonModelAndView extends ModelAndView {

        public String getViewName() {
            return "jsonView";
        }

        public JsonModelAndView(HttpServletResponse response, JsonAware json, int status) {
            super(new JsonView(), JsonView.asMap(json.toJson()));
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
