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

import com.thoughtworks.go.addon.businesscontinuity.AddOnConfiguration;
import com.thoughtworks.go.addon.businesscontinuity.ConfigFileType;
import com.thoughtworks.go.addon.businesscontinuity.FileDetails;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

@Component
public class StandbyFileSyncService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StandbyFileSyncService.class);
    private final int INTERVAL = Integer.parseInt(System.getProperty("bc.primary.status.check.interval", Integer.toString(60 * 1000)));
    private final SystemEnvironment systemEnvironment;
    private final PrimaryServerCommunicationService primaryServerCommunicationService;
    private AddOnConfiguration addOnConfiguration;
    private Map<ConfigFileType, String> currentFileStatus = new ConcurrentHashMap<>();
    private CircularFifoBuffer errorQueue = new CircularFifoBuffer(5);
    private long lastUpdateTime;
    Map<String, String> currentExternalPluginsStatus = new ConcurrentHashMap<>();

    @Autowired
    public StandbyFileSyncService(SystemEnvironment systemEnvironment, PrimaryServerCommunicationService primaryServerCommunicationService, ScheduledExecutorService scheduledExecutorService, AddOnConfiguration addOnConfiguration) {
        this.systemEnvironment = systemEnvironment;
        this.primaryServerCommunicationService = primaryServerCommunicationService;
        this.addOnConfiguration = addOnConfiguration;
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                if (StandbyFileSyncService.this.addOnConfiguration.isServerInStandby() && primaryServerCommunicationService.ableToConnect()) {
                    syncFiles();
                    lastUpdateTime = System.currentTimeMillis();
                    errorQueue.clear();
                }
            } catch (Exception e) {
                Date currentTime = new Date(System.currentTimeMillis());
                errorQueue.add(String.format("[%s] Error while syncing files. Reason, %s", currentTime, e.getMessage()));
                LOGGER.error("Error while syncing files", e);
            }
        }, 1, INTERVAL, TimeUnit.MILLISECONDS);
    }

    private void syncFiles() throws IOException {
        syncConfigFiles();
        syncPlugins();
    }

    private void syncConfigFiles() throws IOException {
        Map<ConfigFileType, FileDetails> primaryConfigFileDetails = StandbyFileSyncService.this.primaryServerCommunicationService.getLatestFileStatus().getFileDetailsMap();
        for (ConfigFileType fileType : primaryConfigFileDetails.keySet()) {
            File fileOnStandby = fileType.load(systemEnvironment);
            if (!fileOnStandby.exists()) {
                fileOnStandby.createNewFile();
                currentFileStatus.put(fileType, "");
            }
            String md5AtStandby = currentMd5(fileType);
            String md5AtPrimary = primaryConfigFileDetails.get(fileType).getMd5();
            if (!md5AtStandby.equals(md5AtPrimary)) {
                primaryServerCommunicationService.downloadConfigFile(fileType, fileOnStandby);
                currentFileStatus.put(fileType, md5AtPrimary);
            }
        }
    }

    private String currentMd5(ConfigFileType fileType) {
        return currentFileStatus.getOrDefault(fileType, StringUtils.EMPTY);
    }

    public List<String> syncErrors() {
        return asList((String[]) errorQueue.toArray(new String[0]));
    }

    public Map<ConfigFileType, String> getCurrentFileStatus() {
        return currentFileStatus;
    }

    public int primaryStatusCheckInterval() {
        return INTERVAL;
    }

    public long lastUpdateTime() {
        return lastUpdateTime;
    }

    void syncPlugins() {
        Map latestPluginsStatus = StandbyFileSyncService.this.primaryServerCommunicationService.getLatestPluginsStatus();
        String folderName = "external";
        List<Map> externalPluginsStatus = (List<Map>) latestPluginsStatus.get(folderName);
        Set<String> pluginsAtPrimary = fetchPluginsUpdatedInPrimary(folderName, externalPluginsStatus);
        removePluginsNotPresentInPrimary(pluginsAtPrimary);
    }

    private Set<String> fetchPluginsUpdatedInPrimary(String folderName, List<Map> externalPluginsStatus) {
        Set<String> pluginsAtPrimary = new HashSet<>();
        for (Map externalPluginStatus : externalPluginsStatus) {
            String pluginName = (String) externalPluginStatus.get("name");
            pluginsAtPrimary.add(pluginName);

            String md5AtStandby = currentPluginMd5(pluginName);
            String md5AtPrimary = (String) externalPluginStatus.get("md5");
            if (!md5AtStandby.equals(md5AtPrimary)) {
                primaryServerCommunicationService.downloadPlugin(folderName, pluginName, new File(systemEnvironment.getExternalPluginAbsolutePath(), pluginName));
                currentExternalPluginsStatus.put(pluginName, md5AtPrimary);
            }
        }
        return pluginsAtPrimary;
    }

    private void removePluginsNotPresentInPrimary(Set<String> pluginsAtPrimary) {
        Set<String> pluginsAtStandby = new HashSet<>(currentExternalPluginsStatus.keySet());
        pluginsAtStandby.removeAll(pluginsAtPrimary);
        for (String pluginToDelete : pluginsAtStandby) {
            FileUtils.deleteQuietly(new File(systemEnvironment.getExternalPluginAbsolutePath(), pluginToDelete));
            currentExternalPluginsStatus.remove(pluginToDelete);
        }
    }

    private String currentPluginMd5(String pluginName) {
        return currentExternalPluginsStatus.getOrDefault(pluginName, StringUtils.EMPTY);
    }

    public Map<String, String> getCurrentExternalPluginsStatus() {
        return currentExternalPluginsStatus;
    }
}
