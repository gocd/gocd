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

package com.thoughtworks.go.addon.businesscontinuity.primary.service;

import com.thoughtworks.go.addon.businesscontinuity.AddOnConfiguration;
import com.thoughtworks.go.addon.businesscontinuity.ConfigFileType;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.SystemEnvironment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.apache.commons.lang3.StringUtils.EMPTY;

@Component
public class GoFilesStatusProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(GoFilesStatusProvider.class);

    private final int INTERVAL = Integer.parseInt(System.getProperty("bc.file.status.check.interval", Integer.toString(60 * 1000)));
    private GoConfigService goConfigService;
    private SystemEnvironment systemEnvironment;
    private AddOnConfiguration addOnConfiguration;
    private Map<ConfigFileType, String> fileStatus = new ConcurrentHashMap<>();
    private long lastUpdateTime;

    @Autowired
    public GoFilesStatusProvider(GoConfigService goConfigService, SystemEnvironment systemEnvironment, ScheduledExecutorService scheduledExecutorService, AddOnConfiguration addOnConfiguration) {
        this.goConfigService = goConfigService;
        this.systemEnvironment = systemEnvironment;
        this.addOnConfiguration = addOnConfiguration;
        initializeFileStatus();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                updateStatus();
            } catch (Exception e) {
                LOGGER.error("Error while updating config files md5", e);
            }
        }, 1, INTERVAL, TimeUnit.MILLISECONDS);
    }

    void initializeFileStatus() {
        setFileStatus(ConfigFileType.CRUISE_CONFIG_XML, EMPTY);
        setFileStatus(ConfigFileType.DES_CIPHER, EMPTY);
        setFileStatus(ConfigFileType.AES_CIPHER, EMPTY);
        setFileStatus(ConfigFileType.JETTY_XML, EMPTY);
        setFileStatus(ConfigFileType.USER_FEATURE_TOGGLE, EMPTY);
    }

    private void updateStatus() throws IOException {
        if (addOnConfiguration.isServerInStandby()) return;
        for (ConfigFileType configFileType : fileStatus.keySet()) {
            File file = configFileType.load(systemEnvironment);
            if (!file.exists()) {
                LOGGER.warn(String.format("Could not find file %s", file.getAbsolutePath()));
                continue;
            }
            // TODO: in standby mode goConfigService.getCurrentConfig().getMd5() could always be null. handle the case.
            if (configFileType.equals(ConfigFileType.CRUISE_CONFIG_XML) && goConfigService.getCurrentConfig().getMd5() != null) {
                setFileStatus(ConfigFileType.CRUISE_CONFIG_XML, goConfigService.getCurrentConfig().getMd5());
            } else {
                try (FileInputStream fileInputStream = new FileInputStream(file)) {
                    String md5 = CachedDigestUtils.md5Hex(fileInputStream);
                    setFileStatus(configFileType, md5);
                }
            }
        }
        lastUpdateTime = System.currentTimeMillis();
    }

    public Map<ConfigFileType, String> getLatestStatusMap() {
        return new HashMap<>(fileStatus);
    }

    public long updateInterval() {
        return INTERVAL;
    }

    public long getLastUpdateTime() {
        return lastUpdateTime;
    }

    private void setFileStatus(ConfigFileType fileName, String value) {
        fileStatus.put(fileName, value);
    }
}
