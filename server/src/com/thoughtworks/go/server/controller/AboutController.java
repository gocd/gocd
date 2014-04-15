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
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.thoughtworks.go.licensing.LicenseValidityHandler;
import com.thoughtworks.go.server.service.AboutService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.GoConstants;
import static com.thoughtworks.go.util.GoConstants.ERROR_FOR_PAGE;
import static com.thoughtworks.go.util.ExceptionUtils.bomb;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
public class AboutController {
    private final GoConfigService goConfigService;
    private final AboutService aboutService;

    @Autowired
    public AboutController(GoConfigService goConfigService, AboutService aboutService) {
        this.goConfigService = goConfigService;
        this.aboutService = aboutService;
    }

    @RequestMapping(value = "/about", method = RequestMethod.GET)
    public ModelAndView handleAboutRequest(
            @RequestParam(value = "updatedSuccessfully", required = false)Boolean updatedSuccessfully,
            @RequestParam(value = "msg", required = false)String msg,
            HttpServletRequest request) {
        return respond(request, updatedSuccessfully != null, updatedSuccessfully, msg);
    }


    private ModelAndView respond(HttpServletRequest request, boolean isPostRedirect, Boolean postSuccessful,
                                 String msg) {
        Map<String, Object> model = aboutService.populateModel(getActiveTab(request));
        if (isPostRedirect) {
            populatePostStatus(postSuccessful, model, msg);
        }
        return new ModelAndView("admin/about", model);
    }

    private void populatePostStatus(Boolean postSuccessful, Map<String, Object> model, String msg) {
        if (!postSuccessful) {
            model.put(ERROR_FOR_PAGE, msg);
        } else if (!model.containsKey(ERROR_FOR_PAGE)) {
            model.put(GoConstants.SUCCESS_MESSAGE, GoConstants.SUCCESSFULLY_CHANGED_LICENSE);
        }
    }

    private String getActiveTab(HttpServletRequest request) {
        return StringUtils.defaultString(request.getParameter("active"));
    }
}
