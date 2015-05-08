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

package com.thoughtworks.go.server.service.plugins.processor;

import com.google.gson.Gson;
import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.DefaultGoApplicationAccessor;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpSession;
import java.util.Map;

@Component
public class SessionDAORequestProcessor implements GoPluginApiRequestProcessor {
    private static final String STORE_IN_SESSION = "store-in-session";
    private static final String GET_FROM_SESSION = "get-from-session";
    private static final String REMOVE_FROM_SESSION = "remove-from-session";

    @Autowired
    public SessionDAORequestProcessor(DefaultGoApplicationAccessor goApplicationAccessor) {
        goApplicationAccessor.registerProcessorFor(STORE_IN_SESSION, this);
        goApplicationAccessor.registerProcessorFor(GET_FROM_SESSION, this);
        goApplicationAccessor.registerProcessorFor(REMOVE_FROM_SESSION, this);
    }

    @Override
    public GoApiResponse process(GoApiRequest goPluginApiRequest) {
        if (goPluginApiRequest.api().equals(STORE_IN_SESSION)) {
            Map<String, Object> requestDataMap = getRequestBodyMap(goPluginApiRequest);
            String pluginId = (String) requestDataMap.get("plugin-id");
            Map<String, Object> sessionData = (Map<String, Object>) requestDataMap.get("session-data");
            HttpSession session = getUserSession();
            if (session != null) {
                session.setAttribute(pluginId, sessionData);
                return new DefaultGoApiResponse(200);
            } else {
                return new DefaultGoApiResponse(401);
            }
        }
        if (goPluginApiRequest.api().equals(GET_FROM_SESSION)) {
            Map<String, Object> requestDataMap = getRequestBodyMap(goPluginApiRequest);
            String pluginId = (String) requestDataMap.get("plugin-id");
            HttpSession session = getUserSession();
            if (session != null) {
                Map<String, Object> sessionData = (Map<String, Object>) session.getAttribute(pluginId);
                DefaultGoApiResponse response = new DefaultGoApiResponse(200);
                response.setResponseBody(getJSONFor(sessionData));
                return response;
            } else {
                return new DefaultGoApiResponse(401);
            }
        }
        if (goPluginApiRequest.api().equals(REMOVE_FROM_SESSION)) {
            Map<String, Object> requestDataMap = getRequestBodyMap(goPluginApiRequest);
            String pluginId = (String) requestDataMap.get("plugin-id");
            HttpSession session = getUserSession();
            if (session != null) {
                session.removeAttribute(pluginId);
            }
            return new DefaultGoApiResponse(200);
        }
        return new DefaultGoApiResponse(401);
    }

    private Map getRequestBodyMap(GoApiRequest goPluginApiRequest) {
        return new Gson().fromJson(goPluginApiRequest.requestBody(), Map.class);
    }

    private String getJSONFor(Map<String, Object> sessionData) {
        return new Gson().toJson(sessionData);
    }

    private HttpSession getUserSession() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession(false);
    }
}
