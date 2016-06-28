/*
 * Copyright 2015 ThoughtWorks, Inc.
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

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.exceptions.ConfigFileHasChangedException;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.server.controller.actions.JsonAction;
import com.thoughtworks.go.server.controller.actions.RestfulAction;
import com.thoughtworks.go.server.controller.actions.XmlAction;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.util.UserHelper;
import com.thoughtworks.go.server.web.JsonView;

import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletResponse;

import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.server.controller.actions.JsonAction.jsonByValidity;

@Controller
public class GoConfigAdministrationController {
    private GoConfigService goConfigService;
    private SecurityService securityService;

    private static final org.apache.commons.logging.Log LOGGER = LogFactory.getLog(GoConfigAdministrationController.class);

    public GoConfigAdministrationController() {
    }

    @Autowired
    GoConfigAdministrationController(GoConfigService goConfigService, SecurityService securityService) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
    }

    @RequestMapping("/admin/restful/configuration/file/GET/xml")
    public void getCurrentConfigXml(@RequestParam(value = "md5", required = false) String md5, HttpServletResponse response) throws Exception {
        getXmlPartial(null, md5, goConfigService.fileSaver(false)).respond(response);
    }

    @RequestMapping("/admin/restful/configuration/file/GET/historical-xml")
    public void getConfigRevision(@RequestParam(value = "version", required = true) String version, HttpServletResponse response) throws Exception {
        GoConfigRevision configRevision = goConfigService.getConfigAtVersion(version);
        String md5 = configRevision.getMd5();
        XmlAction.xmlFound(configRevision.getContent(), md5).respond(response);
    }

    private RestfulAction getXmlPartial(String groupName, String oldMd5, GoConfigService.XmlPartialSaver xmlPartialSaver) {
        if (!isTemplate(groupName) && !isCurrentUserAdminOfGroup(groupName)) {
            return XmlAction.xmlUnAuthorized(errorMessageForGroup(groupName));
        }
        if (isTemplate(groupName) && !isCurrentUserAdmin()) {
            return XmlAction.xmlUnAuthorized(errorMessageForTemplates());
        }
        String xml;
        try {
            xml = xmlPartialSaver.asXml();
        } catch (Exception e) {
            return XmlAction.xmlNotFound(e.getMessage());
        }
        String newMd5 = xmlPartialSaver.getMd5();

        if (oldMd5 != null && !oldMd5.equals(newMd5)) {
            return XmlAction.xmlMd5Conflict(ConfigFileHasChangedException.CONFIG_CHANGED_PLEASE_REFRESH, newMd5);
        }

        return XmlAction.xmlFound(xml, newMd5);
    }

    private boolean isTemplate(String groupName) {
        return TemplatesConfig.PIPELINE_TEMPLATES_FAKE_GROUP_NAME.equals(groupName);
    }

    private String errorMessageForTemplates() {
        return String.format("User '%s' does not have permission to administer pipeline templates", getCurrentUsername());
    }

    private String errorMessageForGroup(String groupName) {
        return String.format("User '%s' does not have permissions to administer pipeline group '%s'", getCurrentUsername(), groupName);
    }

    @RequestMapping("/admin/restful/configuration/file/POST/xml")
    public ModelAndView postFileAsXml(@RequestParam("xmlFile")String xmlFile,
                                      @RequestParam("md5")String md5,
                                      HttpServletResponse response) throws Exception {
        if (!isCurrentUserAdmin()) {
            return JsonAction.jsonUnauthorized().respond(response);
        }
        return postXmlPartial(null, goConfigService.fileSaver(false), xmlFile, "File changed successfully.", md5).respond(response);
    }

    private RestfulAction postXmlPartial(String groupName, GoConfigService.XmlPartialSaver xmlPartialSaver, String xmlPartial, String successMessage, String expectedMd5) {
        if (!isTemplate(groupName) && !isCurrentUserAdminOfGroup(groupName)) {
            return JsonAction.jsonUnauthorized(errorMessageForGroup(groupName));
        }
        if (isTemplate(groupName) && !isCurrentUserAdmin()) {
            return JsonAction.jsonUnauthorized();
        }
        GoConfigValidity configValidity = xmlPartialSaver.saveXml(xmlPartial, expectedMd5);
        if (configValidity.isValid()) {
            return JsonAction.jsonFound(JsonView.getSimpleAjaxResult("result", successMessage));
        } else {
            Map<String, Object> jsonMap = new LinkedHashMap<>();
            jsonMap.put("result", configValidity.errorMessage());
            jsonMap.put("originalContent", xmlPartial);
            return jsonByValidity(jsonMap, configValidity);
        }
    }

    private boolean isCurrentUserAdmin() {
        return securityService.isUserAdmin(getCurrentUser());
    }

    private boolean isCurrentUserAdminOfGroup(String groupName) {
        return securityService.isUserAdminOfGroup(getCurrentUsername(), groupName);
    }

    private CaseInsensitiveString getCurrentUsername() {
        return getCurrentUser().getUsername();
    }

    private Username getCurrentUser() {
        return UserHelper.getUserName();
    }

}
