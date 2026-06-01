/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.server.controller;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.remote.StandardHeaders;
import com.thoughtworks.go.server.controller.actions.JsonAction;
import com.thoughtworks.go.server.controller.actions.RestfulAction;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.ConfirmationConstraint;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.server.controller.actions.JsonAction.jsonByValidity;
import static com.thoughtworks.go.server.controller.actions.TextAction.forbidden;
import static com.thoughtworks.go.server.controller.actions.TextAction.notFound;
import static com.thoughtworks.go.server.controller.actions.XmlAction.xmlFound;

@Controller
public class GoConfigAdministrationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoConfigAdministrationController.class);

    private final ConfirmationConstraint confirmationConstraint = new ConfirmationConstraint();
    private final GoConfigService goConfigService;
    private final SecurityService securityService;

    @Autowired
    GoConfigAdministrationController(GoConfigService goConfigService, SecurityService securityService) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
    }

    @RequestMapping(value = "/spring-internal/admin/configuration/file/GET/xml", method = RequestMethod.GET)
    public void getCurrentConfigXml(HttpServletResponse response) throws IOException {
        (!isCurrentUserAdmin() ? forbidden(forbiddenMessage()) : configXmlFromMemory())
            .respond(response);
    }

    @RequestMapping(value = "/spring-internal/admin/configuration/file/POST/xml", method = RequestMethod.POST)
    public ModelAndView postFileAsXml(@RequestParam("xmlFile") String xmlFile,
                                      @RequestParam("md5") String md5,
                                      HttpServletRequest request, HttpServletResponse response) throws IOException {

        if (!confirmationConstraint.isSatisfied(request)) {
            return JsonAction.jsonBadRequest(String.format("Missing required header `%s`", StandardHeaders.REQUEST_CONFIRM_MODIFICATION)).respond(response);
        } else if (!isCurrentUserAdmin()) {
            return JsonAction.jsonForbidden(forbiddenMessage()).respond(response);
        }
        return postXmlPartial(goConfigService.fileSaver(false), xmlFile, md5).respond(response);
    }

    private RestfulAction configXmlFromMemory() {
        try {
            GoConfigService.XmlPartialSaver<?> xmlPartialSaver = goConfigService.fileSaver(false);
            return xmlFound(xmlPartialSaver.asXml(), xmlPartialSaver.getMd5());
        } catch (Exception e) {
            LOGGER.warn("Unable to serialize config XML from memory for return: {}", e.toString());
            return notFound("Unable to retrieve config XML for return");
        }
    }

    private RestfulAction postXmlPartial(GoConfigService.XmlPartialSaver<?> xmlPartialSaver, String xmlPartial, String expectedMd5) {
        GoConfigValidity configValidity = xmlPartialSaver.saveXml(xmlPartial, expectedMd5);
        if (configValidity.isValid()) {
            return JsonAction.jsonFound(Map.<String, Object>of("result", "File changed successfully."));
        } else {
            GoConfigValidity.InvalidGoConfig invalidGoConfig = (GoConfigValidity.InvalidGoConfig) configValidity;
            Map<String, Object> jsonMap = new LinkedHashMap<>();
            jsonMap.put("result", invalidGoConfig.errorMessage());
            jsonMap.put("originalContent", xmlPartial);
            return jsonByValidity(jsonMap, invalidGoConfig);
        }
    }

    private String forbiddenMessage() {
        return String.format("User '%s' does not have permissions to administer", getCurrentUsername());
    }

    private boolean isCurrentUserAdmin() {
        return securityService.isUserAdmin(getCurrentUser());
    }

    private CaseInsensitiveString getCurrentUsername() {
        return getCurrentUser().getUsername();
    }

    private Username getCurrentUser() {
        return SessionUtils.currentUsername();
    }

}
