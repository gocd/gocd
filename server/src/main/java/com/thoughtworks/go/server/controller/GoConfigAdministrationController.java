/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import com.thoughtworks.go.config.TemplatesConfig;
import com.thoughtworks.go.config.exceptions.ConfigFileHasChangedException;
import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.domain.GoConfigRevision;
import com.thoughtworks.go.server.controller.actions.JsonAction;
import com.thoughtworks.go.server.controller.actions.RestfulAction;
import com.thoughtworks.go.server.controller.actions.XmlAction;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.newsecurity.utils.SessionUtils;
import com.thoughtworks.go.server.security.HeaderConstraint;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.SecurityService;
import com.thoughtworks.go.server.web.JsonView;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.thoughtworks.go.server.controller.actions.JsonAction.jsonByValidity;
import static com.thoughtworks.go.server.controller.actions.XmlAction.X_CRUISE_CONFIG_MD5;

@Controller
public class GoConfigAdministrationController {
    private HeaderConstraint headerConstraint;
    private GoConfigService goConfigService;
    private SecurityService securityService;

    public GoConfigAdministrationController() {
    }

    @Autowired
    GoConfigAdministrationController(GoConfigService goConfigService, SecurityService securityService, SystemEnvironment systemEnvironment) {
        this.goConfigService = goConfigService;
        this.securityService = securityService;
        this.headerConstraint = new HeaderConstraint(systemEnvironment);
    }

    @RequestMapping(value = "/admin/restful/configuration/file/GET/xml", method = RequestMethod.GET)
    public void getCurrentConfigXml(@RequestParam(value = "md5", required = false) String md5, HttpServletResponse response) throws Exception {
        getXmlPartial(null, md5, goConfigService.fileSaver(false)).respond(response);
    }

    @RequestMapping(value = "/admin/restful/configuration/file/GET/historical-xml", method = RequestMethod.GET)
    public void getConfigRevision(@RequestParam(value = "version", required = true) String version, HttpServletResponse response) throws Exception {
        GoConfigRevision configRevision = goConfigService.getConfigAtVersion(version);
        String md5 = configRevision.getMd5();
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("text/xml");
        response.setCharacterEncoding("utf-8");
        response.setHeader(X_CRUISE_CONFIG_MD5, md5);
        if (configRevision.isByteArrayBacked()) {
            IOUtils.write(configRevision.getConfigXmlBytes(), response.getOutputStream());
        } else {
            response.getWriter().write(configRevision.getContent());
        }
    }

    private RestfulAction getXmlPartial(String groupName, String oldMd5, GoConfigService.XmlPartialSaver xmlPartialSaver) {
        if (!isTemplate(groupName) && !isCurrentUserAdminOfGroup(groupName)) {
            return XmlAction.xmlForbidden(errorMessageForGroup(groupName));
        }
        if (isTemplate(groupName) && !isCurrentUserAdmin()) {
            return XmlAction.xmlForbidden(errorMessageForTemplates());
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

    @RequestMapping(value = "/admin/restful/configuration/file/POST/xml", method = RequestMethod.POST)
    public ModelAndView postFileAsXml(@RequestParam("xmlFile") String xmlFile,
                                      @RequestParam("md5") String md5,
                                      HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (!headerConstraint.isSatisfied(request)) {
            return JsonAction.jsonBadRequest(Collections.singletonMap("message", "Missing required header `Confirm`")).respond(response);
        }

        if (!isCurrentUserAdmin()) {
            return JsonAction.jsonForbidden().respond(response);
        }
        return postXmlPartial(null, goConfigService.fileSaver(false), xmlFile, "File changed successfully.", md5).respond(response);
    }

    private RestfulAction postXmlPartial(String groupName, GoConfigService.XmlPartialSaver xmlPartialSaver, String xmlPartial, String successMessage, String expectedMd5) {
        if (!isTemplate(groupName) && !isCurrentUserAdminOfGroup(groupName)) {
            return JsonAction.jsonForbidden(errorMessageForGroup(groupName));
        }
        if (isTemplate(groupName) && !isCurrentUserAdmin()) {
            return JsonAction.jsonForbidden();
        }
        GoConfigValidity configValidity = xmlPartialSaver.saveXml(xmlPartial, expectedMd5);
        if (configValidity.isValid()) {
            return JsonAction.jsonFound(JsonView.getSimpleAjaxResult("result", successMessage));
        } else {
            GoConfigValidity.InvalidGoConfig invalidGoConfig = (GoConfigValidity.InvalidGoConfig) configValidity;
            Map<String, Object> jsonMap = new LinkedHashMap<>();
            jsonMap.put("result", invalidGoConfig.errorMessage());
            jsonMap.put("originalContent", xmlPartial);
            return jsonByValidity(jsonMap, invalidGoConfig);
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
        return SessionUtils.currentUsername();
    }

}
