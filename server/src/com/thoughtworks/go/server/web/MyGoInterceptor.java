/*************************GO-LICENSE-START*********************************
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.server.controller.MyGoController;
import com.thoughtworks.go.server.service.GoConfigService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.beans.factory.annotation.Autowired;


@Controller
public class MyGoInterceptor implements HandlerInterceptor {
    private final GoConfigService goConfigService;
    public static final String SECURITY_IS_ENABLED = "securityIsEnabled";
    public static final String SMTP_IS_ENABLED = "smtpIsEnabled";

    @Autowired
    public MyGoInterceptor(GoConfigService goConfigService) {
        this.goConfigService = goConfigService;
    }

    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ((!goConfigService.isSecurityEnabled()) && (handler instanceof MyGoController)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return false;
        }
        return true;
    }

    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
                           ModelAndView modelAndView) throws Exception {
        if (modelAndView == null) {
            return;
        }
        ModelMap data = modelAndView.getModelMap();
        data.put(SECURITY_IS_ENABLED, goConfigService.isSecurityEnabled());
        data.put(SMTP_IS_ENABLED, goConfigService.isSmtpEnabled());
    }

    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex)
            throws Exception {
    }
}
