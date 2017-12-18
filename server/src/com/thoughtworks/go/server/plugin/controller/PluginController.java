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

package com.thoughtworks.go.server.plugin.controller;

import com.google.common.collect.Sets;
import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.AUTHENTICATION_EXTENSION;
import static javax.servlet.http.HttpServletResponse.SC_FORBIDDEN;
import static org.apache.commons.lang.StringUtils.isNotBlank;

@Controller
public class PluginController {
    public static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";

    private PluginManager pluginManager;
    private static final Set<String> BLACK_LISTED_REQUESTS = Sets.newHashSet("go.plugin-settings.get-configuration",
            "go.plugin-settings.get-view",
            "go.plugin-settings.validate-configuration",
            "go.authentication.plugin-configuration",
            "go.authentication.authenticate-user",
            "go.authentication.search-user");

    @Autowired
    public PluginController(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    @RequestMapping(value = "/plugin/interact/{pluginId}/{requestName}", method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public void handlePluginInteractRequest(
            @PathVariable String pluginId,
            @PathVariable String requestName,
            HttpServletRequest request, HttpServletResponse response) throws IOException {

        if (!isAuthPlugin(pluginId)) {
            response.setStatus(SC_FORBIDDEN);
            response.getWriter().println("Plugin interact endpoint is enabled only for Authentication Plugins");
            return;
        }

        if (isRestrictedRequestName(requestName)) {
            response.setStatus(SC_FORBIDDEN);
            response.getWriter().println(String.format("Plugin interact for '%s' requestName is disallowed.", requestName));
            return;
        }

        DefaultGoPluginApiRequest apiRequest = new DefaultGoPluginApiRequest(null, null, requestName);
        apiRequest.setRequestParams(getParameterMap(request));
        addRequestHeaders(request, apiRequest);

        try {
            GoPluginApiResponse pluginApiResponse = pluginManager.submitTo(pluginId, apiRequest);

            if (DefaultGoApiResponse.SUCCESS_RESPONSE_CODE == pluginApiResponse.responseCode()) {
                renderPluginResponse(pluginApiResponse, response);
                return;
            }
            if (DefaultGoApiResponse.REDIRECT_RESPONSE_CODE == pluginApiResponse.responseCode()) {
                String location = "";
                if (hasValueFor(pluginApiResponse, "Location")) {
                    location = pluginApiResponse.responseHeaders().get("Location");
                }
                response.sendRedirect(location);
                return;
            }
        } catch (Exception e) {
            // handle
        }
        throw new RuntimeException("render error page");
    }

    private boolean isRestrictedRequestName(String requestName) {
        return BLACK_LISTED_REQUESTS.contains(requestName);
    }

    private boolean isAuthPlugin(String pluginId) {
        return pluginManager.isPluginOfType(AUTHENTICATION_EXTENSION, pluginId);
    }

    private Map<String, String> getParameterMap(HttpServletRequest request) {
        Map<String, String[]> springParameterMap = request.getParameterMap();
        Map<String, String> pluginParameterMap = new HashMap<>();
        for (String parameterName : springParameterMap.keySet()) {
            String[] values = springParameterMap.get(parameterName);
            if (values != null && values.length > 0) {
                pluginParameterMap.put(parameterName, values[0]);
            } else {
                pluginParameterMap.put(parameterName, null);
            }
        }
        return pluginParameterMap;
    }

    private void addRequestHeaders(HttpServletRequest request, DefaultGoPluginApiRequest apiRequest) {
        Enumeration headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String header = (String) headerNames.nextElement();
            String value = request.getHeader(header);
            apiRequest.addRequestHeader(header, value);
        }
    }

    private void renderPluginResponse(final GoPluginApiResponse response, HttpServletResponse httpServletResponse) throws IOException {
        String contentType = CONTENT_TYPE_HTML;
        if (hasValueFor(response, "Content-Type")) {
            contentType = response.responseHeaders().get("Content-Type");
        }

        httpServletResponse.setHeader("Content-Type", contentType);
        httpServletResponse.getWriter().write(response.responseBody());
    }

    private boolean hasValueFor(GoPluginApiResponse response, String header) {
        return response.responseHeaders() != null && isNotBlank(response.responseHeaders().get(header));
    }
}
