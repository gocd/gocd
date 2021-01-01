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

package com.thoughtworks.go.addon.businesscontinuity.standby.controller;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.CurrentGoCDVersion;
import com.thoughtworks.go.addon.businesscontinuity.*;
import com.thoughtworks.go.addon.businesscontinuity.primary.ServerStatusResponse;
import com.thoughtworks.go.addon.businesscontinuity.standby.service.PrimaryServerCommunicationService;
import com.thoughtworks.go.addon.businesscontinuity.standby.service.StandbyFileSyncService;
import com.thoughtworks.go.server.newsecurity.models.UsernamePassword;
import com.thoughtworks.go.server.service.RailsAssetsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import static com.thoughtworks.go.server.newsecurity.utils.BasicAuthHeaderExtractor.extractBasicAuthenticationCredentials;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.join;

@Controller
public class DashBoardController {

    private final AuthToken authToken;
    private final BiFunction<HttpServletRequest, HttpServletResponse, String> dashboardHTML;
    private final BiFunction<HttpServletRequest, HttpServletResponse, String> dashboardJSON;
    private AddOnConfiguration addOnConfiguration;
    private PrimaryServerCommunicationService primaryServerCommunicationService;
    private StandbyFileSyncService standbyFileSyncService;
    private ViewResolver viewResolver;
    private DatabaseStatusProvider databaseStatusProvider;
    private RailsAssetsService railsAssetsService;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().setDateFormat("MMM d, YYYY HH:mm:ss").create();

    private static final String CREDENTIALS_KEY = DashBoardController.class.getName() + ".existing-credentials";

    @Autowired
    public DashBoardController(AddOnConfiguration addOnConfiguration, PrimaryServerCommunicationService primaryServerCommunicationService,
                               StandbyFileSyncService standbyFileSyncService,
                               ViewResolver viewResolver, DatabaseStatusProvider databaseStatusProvider, RailsAssetsService railsAssetsService, AuthToken authToken) {
        this.addOnConfiguration = addOnConfiguration;
        this.primaryServerCommunicationService = primaryServerCommunicationService;
        this.standbyFileSyncService = standbyFileSyncService;
        this.viewResolver = viewResolver;
        this.databaseStatusProvider = databaseStatusProvider;
        this.railsAssetsService = railsAssetsService;
        this.authToken = authToken;
        this.dashboardHTML = this::showStatusPage;
        this.dashboardJSON = this::showStatusJSON;
    }

    @RequestMapping(value = "/add-on/business-continuity/admin/dashboard", method = RequestMethod.GET)
    @ResponseBody
    public String dashboard(HttpServletRequest request, HttpServletResponse response) {
        return renderAfterAuthentication(request, response, dashboardHTML);
    }

    @RequestMapping(value = "/add-on/business-continuity/admin/dashboard.json", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String dashboardData(HttpServletRequest request, HttpServletResponse response) {
        return renderAfterAuthentication(request, response, dashboardJSON);
    }

    private String renderAfterAuthentication(HttpServletRequest request, HttpServletResponse response, BiFunction<HttpServletRequest, HttpServletResponse, String> renderer) {
        if (!authToken.isValid()) {
            response.setStatus(422);
            return "Please setup business continuity according to the documentation at https://extensions-docs.gocd.org/business-continuity/" + CurrentGoCDVersion.getInstance().goVersion();
        }
        HttpSession session = request.getSession();

        UsernamePassword usernamePassword = extractBasicAuthenticationCredentials(request.getHeader("Authorization"));
        UsernamePassword credentialsOnDisk = authToken.toUsernamePassword();
        UsernamePassword existingCredentialsInSession = (UsernamePassword) session.getAttribute(CREDENTIALS_KEY);

        HashSet<UsernamePassword> submittedPasswords = Sets.newHashSet(usernamePassword, existingCredentialsInSession);
        submittedPasswords.remove(null);

        if (submittedPasswords.isEmpty()) {
            return forceBasicAuth(request, response);
        }

        if (submittedPasswords.stream().allMatch(up -> up.equals(credentialsOnDisk))) {
            return renderer.apply(request, response);
        }

        if (credentialsOnDisk.equals(usernamePassword)) {
            session.invalidate();
            loginCurrentUser(request, request.getSession(true), usernamePassword);
            return renderer.apply(request, response);
        }

        return forceBasicAuth(request, response);
    }

    private void loginCurrentUser(HttpServletRequest request, HttpSession session, UsernamePassword usernamePassword) {
        session.invalidate();
        session = request.getSession(true);
        session.setAttribute(CREDENTIALS_KEY, usernamePassword);
    }

    private String showStatusPage(HttpServletRequest request, HttpServletResponse response) {
        Map<String, String> modelMap = new HashMap<>();
        modelMap.put("REPLACED_BY_GO:application.css", railsAssetsService.getAssetPath("application.css"));
        modelMap.put("REPLACED_BY_GO:patterns/application.css", railsAssetsService.getAssetPath("patterns/application.css"));
        modelMap.put("REPLACED_BY_GO:application.js", railsAssetsService.getAssetPath("application.js"));
        modelMap.put("REPLACED_BY_GO:cruise.ico", railsAssetsService.getAssetPath("cruise.ico"));
        if (addOnConfiguration.isServerInStandby()) {
            return viewResolver.resolveView("standby_dashboard", modelMap);
        } else {
            return viewResolver.resolveView("error", modelMap);
        }
    }

    private String forceBasicAuth(HttpServletRequest request, HttpServletResponse response) {
        response.addHeader("WWW-Authenticate", "Basic realm=\"GoCD Business Continuity\"");
        response.setStatus(401);
        return "bad credentials!";
    }

    private String showStatusJSON(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType("application/json");
        if (addOnConfiguration.isServerInStandby()) {

            Map<String, Object> dashboardContent = new HashMap<>();
            UsernamePassword usernamePassword = extractBasicAuthenticationCredentials(request.getHeader("Authorization"));
            dashboardContent.put("userName", usernamePassword.getUsername());
            if (!primaryServerCommunicationService.ableToConnect()) {
                dashboardContent.put("setupStatus", "incomplete");
                dashboardContent.put("syncErrors", Collections.singletonList("Unable to connect to primary, please check that the business-continuity-token file is identical on primary and secondary, and that this server can connect to the primary server."));
            } else {
                dashboardContent.put("setupStatus", "success");
                dashboardContent.put("primaryServerDetails", primaryServerDetails());
                dashboardContent.put("standbyServerDetails", standbyServerDetails());
                dashboardContent.put("syncErrors", standbyFileSyncService.syncErrors());
            }
            return GSON.toJson(dashboardContent);
        }
        throw new RuntimeException("This information only available for standby server");
    }

    Map<String, Object> standbyServerDetails() {
        Map<String, Object> details = new HashMap<>();
        details.put("primaryStatusCheckInterval", standbyFileSyncService.primaryStatusCheckInterval());
        details.put("lastUpdateTime", new Date(standbyFileSyncService.lastUpdateTime()));
        details.put("latestReceivedDatabaseWalLocation", databaseStatusProvider.latestReceivedWalLocation());
        Map<ConfigFileType, String> currentFileStatus = standbyFileSyncService.getCurrentFileStatus();
        for (ConfigFileType configFileType : currentFileStatus.keySet()) {
            details.put(configFileType.name(), currentFileStatus.get(configFileType));
        }
        details.put("pluginStatus", standbyPluginStatus());
        return details;
    }

    Map<String, Object> primaryServerDetails() {
        Map<String, Object> details = new HashMap<>();

        String primaryServerUrl = primaryServerCommunicationService.primaryServerUrl();
        details.put("url", primaryServerUrl);


        try {
            details.put("latestDatabaseWalLocation", primaryServerCommunicationService.latestDatabaseWalLocation());
            ServerStatusResponse latestFileStatus = primaryServerCommunicationService.getLatestFileStatus();
            details.put("configFilesUpdateInterval", latestFileStatus.getConfigFilesUpdateInterval());
            details.put("lastConfigUpdateTime", new Date(latestFileStatus.getLastConfigFilesUpdateTime()));
            Map<ConfigFileType, FileDetails> fileDetailsMap = latestFileStatus.getFileDetailsMap();
            for (ConfigFileType fileType : fileDetailsMap.keySet()) {
                details.put(fileType.name(), fileDetailsMap.get(fileType));
            }

            details.put("pluginStatus", primaryPluginStatus());
        } catch (Exception e) {
            details.put("error", format("Could not fetch latest file status from master, Reason, %s", e.getMessage()));
        }
        return details;
    }

    private String standbyPluginStatus() {
        final Map<String, String> currentExternalPluginsStatus = standbyFileSyncService.getCurrentExternalPluginsStatus();
        List<String> pluginsMd5 = currentExternalPluginsStatus.keySet().stream().map(pluginName -> format("%s=%s", pluginName, currentExternalPluginsStatus.get(pluginName))).sorted().collect(Collectors.toList());

        return join(pluginsMd5, ", ");
    }

    private String primaryPluginStatus() {
        List<Map> externalPlugins = (List<Map>) primaryServerCommunicationService.getLatestPluginsStatus().get("external");
        return externalPlugins.stream().map(map -> format("%s=%s", map.get("name"), map.get("md5"))).collect(Collectors.joining(", "));
    }

}
