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

import com.thoughtworks.go.config.validation.GoConfigValidity;
import com.thoughtworks.go.i18n.Localizable;
import com.thoughtworks.go.i18n.LocalizedMessage;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.metrics.domain.context.Context;
import com.thoughtworks.go.metrics.domain.probes.ProbeType;
import com.thoughtworks.go.metrics.service.MetricsProbeService;
import com.thoughtworks.go.server.service.AdminService;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.GoConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
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
import java.util.Map;

import static com.thoughtworks.go.server.controller.actions.JsonAction.jsonFound;

@Controller
public class AdminController {

    private final AdminService adminService;
    private final MetricsProbeService metricsProbeService;
    private final Localizer localizer;
    private final GoConfigService goConfigService;
    static final String UPDATE_SUCCESS_MESSAGE = "Configuration file has been updated successfully. You can see your"
            + " updates when the pipeline is scheduled to build.";
    private static final Logger LOGGER = Logger.getLogger(AdminController.class);


    @Autowired
    public AdminController(GoConfigService goConfigService, AdminService adminService, Localizer localizer, MetricsProbeService metricsProbeService) {
        this.goConfigService = goConfigService;
        this.adminService = adminService;
        this.localizer = localizer;
        this.metricsProbeService = metricsProbeService;
    }

    @RequestMapping(value = "/tab/admin", method = RequestMethod.GET)
    public ModelAndView handleTabRequest(HttpServletRequest request) {
        return new ModelAndView("", adminModel(request));
    }

    private Map adminModel(HttpServletRequest request) {
        Map model = new HashMap<String, String>();
        model.put("active", getActiveTab(request));
        model.put("current_tab", "");
        model.put("l", localizer);
        model.put("configuration_validity", goConfigService.checkConfigFileValid().isValid());
        return adminService.populateModel(model);
    }

    @RequestMapping(value = "/tab/admin", method = RequestMethod.POST)
    public ModelAndView handleEditConfiguration(@RequestParam("configFileContent") String configFileContent,
                                                @RequestParam("configMd5") String expectedMd5,
                                                @RequestParam(value = "shouldMigrate", required = false) Boolean shouldMigrate,
                                                HttpServletRequest request) throws IOException {
        LOGGER.debug("[Config Save] Configuration being saved");
        Context context = metricsProbeService.begin(ProbeType.SAVE_CONFIG_XML_THROUGH_API);
        Map<String, String> data;
        try {
            GoConfigValidity configValidity = saveConfigFile(configFileContent, expectedMd5, shouldMigrate);
            data = new HashMap<String, String>();
            if (configValidity.isValid()) {
                Localizable savedSuccessMessage = LocalizedMessage.string("SAVED_CONFIGURATION_SUCCESSFULLY");
                Localizable localizableMessage = configValidity.wasMerged() ? LocalizedMessage.composite(savedSuccessMessage,LocalizedMessage.string("CONFIG_MERGED")) : savedSuccessMessage;
                data.put(GoConstants.SUCCESS_MESSAGE, localizableMessage.localize(localizer));
            } else {
                data.put(GoConstants.ERROR_FOR_PAGE, configValidity.errorMessage());
                data.put("editing_md5", expectedMd5);
                data.put("editing_content", configFileContent);
            }
            data.putAll(adminModel(request));
            LOGGER.debug("[Config Save] Done saving configuration");
        } finally {
            metricsProbeService.end(ProbeType.SAVE_CONFIG_XML_THROUGH_API, context);
        }
        return new ModelAndView("", data);
    }

    private GoConfigValidity saveConfigFile(String configFileContent, String expectedMd5, Boolean shouldMigrate) {
        shouldMigrate = shouldMigrate == null ? false : shouldMigrate;
        GoConfigService.XmlPartialSaver fileSaver = goConfigService.fileSaver(shouldMigrate);
        GoConfigValidity configValidity = fileSaver.saveXml(configFileContent, expectedMd5);
        return configValidity;
    }

    @RequestMapping(value = "/tab/admin/configuration.json", method = RequestMethod.GET)
    public ModelAndView handleConfiguration(HttpServletResponse response) {
        Map<String, Object> json = adminService.configurationJsonForSourceXml();
        return jsonFound(json).respond(response);
    }

    private String getActiveTab(HttpServletRequest request) {
        return StringUtils.defaultString(request.getParameter("active"));
    }
}
