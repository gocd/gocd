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

import java.io.IOException;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.PipelineConfigs;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.util.UserHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class TabController {
    private GoConfigService goConfigService;
    private final Localizer localizer;
    public static final String TAB_PLACEHOLDER = "";

    @Autowired
    public TabController(GoConfigService goConfigService, Localizer localizer) {
        this.goConfigService = goConfigService;
        this.localizer = localizer;
    }

    @RequestMapping(value = "/old_current_activities_page/**", method = RequestMethod.GET)
    public ModelAndView handleOldPipelineTab(HttpServletRequest request,
                                          HttpServletResponse response) throws IOException {
        if (goConfigService.isPipelineEmpty() && goConfigService.isAdministrator(CaseInsensitiveString.str(UserHelper.getUserName().getUsername()))) {
            response.sendRedirect(String.format("admin/pipeline/new?group=%s", PipelineConfigs.DEFAULT_GROUP));
            return null;
        }
        return new ModelAndView(TAB_PLACEHOLDER,  new HashMap() { { put("l", localizer); } });
    }

    @RequestMapping(value = "/admin/**", method = RequestMethod.GET)
    public ModelAndView handleAdminTab(HttpServletRequest request, HttpServletResponse response) {
        return new ModelAndView(TAB_PLACEHOLDER,  new HashMap() { { put("l", localizer); } });
    }

    @RequestMapping(value = "/pipeline/**", method = RequestMethod.GET)
    public ModelAndView handlePipelineTab(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.sendRedirect("/go/home");
        return null;
    }
}