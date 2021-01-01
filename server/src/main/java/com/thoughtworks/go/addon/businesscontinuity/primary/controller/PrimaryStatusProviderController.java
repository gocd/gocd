/*
 * Copyright 2021 ThoughtWorks, Inc.
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

package com.thoughtworks.go.addon.businesscontinuity.primary.controller;

import com.google.gson.Gson;
import com.thoughtworks.go.addon.businesscontinuity.ConfigFileType;
import com.thoughtworks.go.addon.businesscontinuity.DatabaseStatusProvider;
import com.thoughtworks.go.addon.businesscontinuity.FileDetails;
import com.thoughtworks.go.addon.businesscontinuity.PluginsList;
import com.thoughtworks.go.addon.businesscontinuity.primary.ServerStatusResponse;
import com.thoughtworks.go.addon.businesscontinuity.primary.service.GoFilesStatusProvider;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Controller
@SuppressWarnings("WeakerAccess")
@RequestMapping(value = "/add-on/business-continuity/api")
public class PrimaryStatusProviderController {

    private GoFilesStatusProvider goFilesStatusProvider;
    private DatabaseStatusProvider databaseStatusProvider;
    private final SystemEnvironment systemEnvironment;
    private PluginsList pluginsList;

    @Autowired
    public PrimaryStatusProviderController(GoFilesStatusProvider goFilesStatusProvider, DatabaseStatusProvider databaseStatusProvider, PluginsList pluginsList, SystemEnvironment systemEnvironment) {
        this.goFilesStatusProvider = goFilesStatusProvider;
        this.databaseStatusProvider = databaseStatusProvider;
        this.pluginsList = pluginsList;
        this.systemEnvironment = systemEnvironment;
    }

    @RequestMapping(value = "/health-check", method = RequestMethod.GET)
    @ResponseBody
    public String healthCheck() {
        return "OK!";
    }

    @RequestMapping(value = "/latest_database_wal_location", method = RequestMethod.GET)
    @ResponseBody
    public String latestDatabaseWalLocation() {
        return databaseStatusProvider.latestWalLocation();
    }

    @RequestMapping(value = "/config_files_status", method = RequestMethod.GET)
    public void latestStatus(HttpServletResponse response) throws IOException {
        Map<ConfigFileType, String> latestFileStatusMap = goFilesStatusProvider.getLatestStatusMap();

        Map<ConfigFileType, FileDetails> fileDetailsMap = new HashMap<>();
        for (ConfigFileType configFileType : latestFileStatusMap.keySet()) {
            if (!isEmpty(latestFileStatusMap.get(configFileType))) {
                fileDetailsMap.put(configFileType, new FileDetails(latestFileStatusMap.get(configFileType)));
            }
        }

        ServerStatusResponse serverStatusResponse = new ServerStatusResponse(goFilesStatusProvider.updateInterval(), goFilesStatusProvider.getLastUpdateTime(), fileDetailsMap);
        String responseBody = new Gson().toJson(serverStatusResponse);
        response.setContentType("application/json");
        response.getOutputStream().print(responseBody);
    }

    @RequestMapping(value = "/cruise_config", method = RequestMethod.GET)
    public void getLatestCruiseConfigXML(HttpServletResponse response) {
        serveFile(ConfigFileType.CRUISE_CONFIG_XML.load(systemEnvironment), response, "text/xml");
    }

    @RequestMapping(value = "/user_feature_toggle", method = RequestMethod.GET)
    public void geUserFeatureToggleFile(HttpServletResponse response) {
        serveFile(ConfigFileType.USER_FEATURE_TOGGLE.load(systemEnvironment), response, "text/json");
    }

    @RequestMapping(value = "/cipher.aes", method = RequestMethod.GET)
    public void getLatestAESCipher(HttpServletResponse response) {
        serveFile(ConfigFileType.AES_CIPHER.load(systemEnvironment), response, "text/plain");
    }

    @RequestMapping(value = "/jetty_config", method = RequestMethod.GET)
    public void getLatestJettyXML(HttpServletResponse response) {
        serveFile(ConfigFileType.JETTY_XML.load(systemEnvironment), response, "text/xml");
    }

    @RequestMapping(value = "/plugin_files_status", method = RequestMethod.GET)
    public void latest(HttpServletResponse response) throws IOException {
        String pluginsJSON = pluginsList.getPluginsJSON();
        response.setContentType("application/json");
        response.getOutputStream().print(pluginsJSON);
    }

    @RequestMapping(value = "/plugin", method = RequestMethod.GET)
    public void getPluginFile(
            @RequestParam("folderName") String folderName,
            @RequestParam("pluginName") String pluginName,
            HttpServletResponse response) {
        String pluginFolderPath = isBlank(folderName) || folderName.equalsIgnoreCase("bundled") ? systemEnvironment.getBundledPluginAbsolutePath() : systemEnvironment.getExternalPluginAbsolutePath();
        File pluginFile = new File(pluginFolderPath, pluginName);
        serveFile(pluginFile, response, "application/octet-stream");
    }

    private void serveFile(File file, HttpServletResponse response, String contentType) {
        try (FileInputStream inputStream = new FileInputStream(file)) {
            response.setStatus(HttpServletResponse.SC_OK);
            response.setCharacterEncoding("UTF-8");
            response.setContentType(contentType);
            copy(inputStream, response.getOutputStream());
            response.flushBuffer();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
