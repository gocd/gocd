/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.server.service.plugins.processor.session;

import com.thoughtworks.go.plugin.api.request.GoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoApiResponse;
import com.thoughtworks.go.plugin.infra.PluginRequestProcessorRegistry;
import com.thoughtworks.go.plugin.infra.GoPluginApiRequestProcessor;
import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;
import com.thoughtworks.go.util.json.JsonHelper;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

@Component
public class SessionRequestProcessor implements GoPluginApiRequestProcessor {
    private static final Logger LOGGER = Logger.getLogger(SessionRequestProcessor.class);

    public static final String PUT_INTO_SESSION = "go.processor.session.put";
    public static final String GET_FROM_SESSION = "go.processor.session.get";
    public static final String REMOVE_FROM_SESSION = "go.processor.session.remove";
    private static final List<String> goSupportedVersions = asList("1.0");

    private Map<String, JsonMessageHandler> messageHandlerMap = new HashMap<>();

    @Autowired
    public SessionRequestProcessor(PluginRequestProcessorRegistry registry) {
        registry.registerProcessorFor(PUT_INTO_SESSION, this);
        registry.registerProcessorFor(GET_FROM_SESSION, this);
        registry.registerProcessorFor(REMOVE_FROM_SESSION, this);
        this.messageHandlerMap.put("1.0", new JsonMessageHandler1_0());
    }

    @Override
    public GoApiResponse process(GoPluginDescriptor pluginDescriptor, GoApiRequest goPluginApiRequest) {
        try {
            String version = goPluginApiRequest.apiVersion();
            if (!goSupportedVersions.contains(version)) {
                throw new RuntimeException(String.format("Unsupported '%s' API version: %s. Supported versions: %s", goPluginApiRequest.api(), version, goSupportedVersions));
            }

            if (goPluginApiRequest.api().equals(PUT_INTO_SESSION)) {
                return handleSessionPutRequest(goPluginApiRequest);
            }
            if (goPluginApiRequest.api().equals(GET_FROM_SESSION)) {
                return handleSessionGetRequest(goPluginApiRequest);
            }
            if (goPluginApiRequest.api().equals(REMOVE_FROM_SESSION)) {
                return handleSessionRemoveRequest(goPluginApiRequest);
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred while authenticating user", e);
        }
        return new DefaultGoApiResponse(500);
    }

    private GoApiResponse handleSessionPutRequest(GoApiRequest goPluginApiRequest) {
        SessionData sessionData = messageHandlerMap.get(goPluginApiRequest.apiVersion()).requestMessageSessionPut(goPluginApiRequest.requestBody());
        HttpSession session = getUserSession();
        if (session == null) {
            throw new RuntimeException("session not found");
        }
        session.setAttribute(sessionData.getPluginId(), sessionData.getSessionData());
        return new DefaultGoApiResponse(200);
    }

    private GoApiResponse handleSessionGetRequest(GoApiRequest goPluginApiRequest) {
        String pluginId = messageHandlerMap.get(goPluginApiRequest.apiVersion()).requestMessageSessionGetAndRemove(goPluginApiRequest.requestBody());
        HttpSession session = getUserSession();
        if (session == null) {
            throw new RuntimeException("session not found");
        }
        Map<String, String> sessionDataMap = (Map<String, String>) session.getAttribute(pluginId);
        DefaultGoApiResponse response = new DefaultGoApiResponse(200);
        response.setResponseBody(JsonHelper.toJsonString(sessionDataMap));
        return response;
    }

    private GoApiResponse handleSessionRemoveRequest(GoApiRequest goPluginApiRequest) {
        String pluginId = messageHandlerMap.get(goPluginApiRequest.apiVersion()).requestMessageSessionGetAndRemove(goPluginApiRequest.requestBody());
        HttpSession session = getUserSession();
        if (session == null) {
            throw new RuntimeException("session not found");
        }
        session.removeAttribute(pluginId);
        return new DefaultGoApiResponse(200);
    }

    HttpSession getUserSession() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession(false);
    }

    Map<String, JsonMessageHandler> getMessageHandlerMap() {
        return messageHandlerMap;
    }
}
