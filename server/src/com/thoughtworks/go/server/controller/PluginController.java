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

package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.plugin.api.request.DefaultGoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.go.plugin.infra.PluginManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.View;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PluginController {
    private PluginManager pluginManager;

    @Autowired
    public PluginController(PluginManager defaultPluginManager) {
        this.pluginManager = defaultPluginManager;
    }

    @RequestMapping(value = "/{pluginId}/{requestName}", method = RequestMethod.GET)
    public ModelAndView handleAboutRequest(
            @PathVariable String pluginId,
            @PathVariable String requestName,
            HttpServletRequest request) {
        DefaultGoPluginApiRequest apiRequest = new DefaultGoPluginApiRequest(null, null, requestName);
        apiRequest.setRequestParams(getParameterMap(request));
        GoPluginApiResponse response = pluginManager.submitTo(pluginId, apiRequest);
        if (DefaultGoApiResponse.SUCCESS_RESPONSE_CODE == response.responseCode()) {
            return renderPluginResponse(response);
        }
        if (302 == response.responseCode()) {
            return new ModelAndView("redirect:" + response.responseHeaders().get("Location"));
        }
        throw new RuntimeException("render error page");
    }

    private Map<String, String> getParameterMap(HttpServletRequest request) {
        Map<String, String[]> springParameterMap = request.getParameterMap();
        Map<String, String> pluginParameterMap = new HashMap<String, String>();
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

    private ModelAndView renderPluginResponse(final GoPluginApiResponse response) {
        return new ModelAndView(new View() {
            @Override
            public String getContentType() {
                return "text/html; charset=UTF-8";
            }

            @Override
            public void render(Map<String, ?> map, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws Exception {
                httpServletResponse.getWriter().write(response.responseBody());
            }
        });
    }
}
