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

package com.thoughtworks.go.server.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.util.json.Json;
import com.thoughtworks.go.util.json.JsonpString;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class JsonpInterceptor implements HandlerInterceptor {
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        return true;
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
        String callback = request.getParameter("callback");
        if(!StringUtils.isEmpty(callback) && modelAndView.getModel().containsKey("json")) {
            Json json = (Json)modelAndView.getModel().get("json");
            modelAndView.getModel().put("json", new JsonpString(json, callback));
        }
    }

    private String originalJsonAsString(HttpServletRequest request, ModelAndView modelAndView) {
        Json original = (Json) modelAndView.getModel().get("json");
        JsonStringRenderer render = new JsonStringRenderer(new GoRequestContext(request));
        original.renderTo(render);
        String originalJsonString = render.toString();
        return originalJsonString;
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
    }
}
