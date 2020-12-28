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

package com.thoughtworks.go.addon.businesscontinuity.standby.service;

import com.thoughtworks.go.addon.businesscontinuity.ConfigFileType;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.http.client.methods.HttpGet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;

@Component
public class PrimaryServerEndPoint {

    private final String DEFAULT_PRIMARY = "https://localhost:8154";
    private static Map<ConfigFileType, String> urlMap = new HashMap<>();
    private final SystemEnvironment systemEnvironment;

    @Autowired
    public PrimaryServerEndPoint(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
        urlMap.put(ConfigFileType.CRUISE_CONFIG_XML, createBusinessContinuityAddOnApiURLFor("/cruise_config"));
        urlMap.put(ConfigFileType.AES_CIPHER, createBusinessContinuityAddOnApiURLFor("/cipher.aes"));
        urlMap.put(ConfigFileType.JETTY_XML, createBusinessContinuityAddOnApiURLFor("/jetty_config"));
        urlMap.put(ConfigFileType.USER_FEATURE_TOGGLE, createBusinessContinuityAddOnApiURLFor("/user_feature_toggle"));
    }

    HttpGet configFileStatus(String token) {
        return createBasicAuthGetMethod(createBusinessContinuityAddOnApiURLFor("/config_files_status"), token);
    }

    HttpGet downloadConfigFile(ConfigFileType configFileType, String token) {
        return createBasicAuthGetMethod(urlMap.get(configFileType), token);
    }

    String primaryServerUrl() {
        return System.getProperty("bc.primary.url", DEFAULT_PRIMARY).replaceAll("/\\z", "");
    }

    private String createBusinessContinuityAddOnApiURLFor(String path) {
        return primaryServerUrl() + systemEnvironment.getWebappContextPath() + "/add-on/business-continuity/api" + path;
    }

    private HttpGet createBasicAuthGetMethod(String url, String usernamePassword) {
        HttpGet getMethod = new HttpGet(url);
        getMethod.addHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString(usernamePassword.getBytes(UTF_8)));
        return getMethod;
    }

    HttpGet latestDatabaseWalLocation(String token) {
        return createBasicAuthGetMethod(createBusinessContinuityAddOnApiURLFor("/latest_database_wal_location"), token);
    }

    HttpGet pluginsListing(String token) {
        return createBasicAuthGetMethod(createBusinessContinuityAddOnApiURLFor("/plugin_files_status"), token);
    }

    HttpGet downloadPlugin(String folderName, String pluginName, String token) {
        return createBasicAuthGetMethod(createBusinessContinuityAddOnApiURLFor(String.format("/plugin?folderName=%s&pluginName=%s", folderName, pluginName)), token);
    }

    HttpGet healthCheck(String token) {
        return createBasicAuthGetMethod(createBusinessContinuityAddOnApiURLFor("/health-check"), token);
    }
}
