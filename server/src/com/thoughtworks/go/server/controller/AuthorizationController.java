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

import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.plugin.access.authentication.AuthenticationPluginRegistry;
import com.thoughtworks.go.server.service.SecurityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

@Controller
public class AuthorizationController {
    private final Localizer localizer;
    private final SecurityService securityService;
    private final AuthenticationPluginRegistry authenticationPluginRegistry;

    @Autowired
    public AuthorizationController(Localizer localizer, SecurityService securityService, AuthenticationPluginRegistry authenticationPluginRegistry) {
        this.localizer = localizer;
        this.securityService = securityService;
        this.authenticationPluginRegistry = authenticationPluginRegistry;
    }

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public ModelAndView login(@RequestParam(value = "login_error", required = false) Boolean loginError,
                              HttpServletRequest request, HttpServletResponse response) throws IOException {
        HashMap model = new HashMap();
        model.put("login_error", loginError);
        model.put("l", localizer);
        model.put("authentication_plugin_registry", authenticationPluginRegistry);
        return new ModelAndView("auth/login", model);
    }


    @RequestMapping(value = "/security_check", method = RequestMethod.POST)
    public ModelAndView securityCheckHandlerWhenAuthenticationProcessingFilterIsOff(HttpServletRequest request,
                                                                                    HttpServletResponse response)
            throws IOException {
        response.sendRedirect("/go");
        return null;
    }
}
