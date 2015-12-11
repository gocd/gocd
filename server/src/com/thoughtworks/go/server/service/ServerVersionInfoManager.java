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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.GoVersion;
import com.thoughtworks.go.domain.VersionInfo;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.VersionInfoDao;
import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.SystemEnvironment;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Calendar;
import java.util.Date;

@Component
public class ServerVersionInfoManager {
    private VersionInfo serverVersionInfo;
    private ServerVersionInfoBuilder builder;
    private VersionInfoDao versionInfoDao;
    private Clock clock;
    private GoCache goCache;
    private SystemEnvironment systemEnvironment;
    private DateTime versionInfoUpdatingFrom;
    private static String GO_UPDATE = "GOUpdate";
    private static final Object VERSION_INFO_MUTEX = new Object();
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerVersionInfoManager.class.getName());

    @Autowired
    public ServerVersionInfoManager(ServerVersionInfoBuilder builder, VersionInfoDao versionInfoDao, Clock clock, GoCache goCache, SystemEnvironment systemEnvironment) {
        this.builder = builder;
        this.versionInfoDao = versionInfoDao;
        this.clock = clock;
        this.goCache = goCache;
        this.systemEnvironment = systemEnvironment;
    }

    public void initialize() {
        this.serverVersionInfo = builder.getServerVersionInfo();

        if (!systemEnvironment.isGOUpdateCheckEnabled()) {
            LOGGER.info("[Go Update Check] Update check disabled.");
        }

        addGoUpdateToCacheIfAvailable();
    }

    public VersionInfo versionInfoForUpdate() {
        synchronized (VERSION_INFO_MUTEX) {
            if (isDevelopmentServer() || isVersionInfoUpdatedToday() || isUpdateInProgress()) return null;

            versionInfoUpdatingFrom = clock.currentDateTime();
            LOGGER.info("[Go Update Check] Starting update check at: {}", new Date());

            return this.serverVersionInfo;
        }
    }

    public VersionInfo updateLatestVersion(String latestVersion) {
        synchronized (VERSION_INFO_MUTEX) {
            serverVersionInfo.setLatestVersion(new GoVersion(latestVersion));
            serverVersionInfo.setLatestVersionUpdatedAt(clock.currentTime());
            versionInfoDao.saveOrUpdate(serverVersionInfo);

            versionInfoUpdatingFrom = null;
            addGoUpdateToCacheIfAvailable();

            LOGGER.info("[Go Update Check] Update check done at: {}, latest available version: {}", new Date(), latestVersion);
            return serverVersionInfo;
        }
    }

    public String getGoUpdate(){
        return (String) goCache.get(GO_UPDATE);
    }

    public boolean isUpdateCheckEnabled(){
        return !isDevelopmentServer() && systemEnvironment.isGOUpdateCheckEnabled();
    }

    private boolean isDevelopmentServer() {
        return serverVersionInfo == null;
    }

    private boolean isVersionInfoUpdatedToday() {
        Date latestVersionUpdatedAt = serverVersionInfo.getLatestVersionUpdatedAt();

        if (latestVersionUpdatedAt == null) return false;

        return isToday(latestVersionUpdatedAt);
    }

    private boolean isToday(Date date) {
        Calendar today = Calendar.getInstance();
        Calendar otherDay = Calendar.getInstance();
        otherDay.setTime(date);

        return (today.get(Calendar.YEAR) == otherDay.get(Calendar.YEAR) &&
                today.get(Calendar.DAY_OF_YEAR) == otherDay.get(Calendar.DAY_OF_YEAR));
    }

    private boolean isUpdateInProgress() {
        if (versionInfoUpdatingFrom == null) return false;

        DateTime halfHourAgo = new DateTime(System.currentTimeMillis() - 30 * 60 * 1000);
        return versionInfoUpdatingFrom.isAfter(halfHourAgo);
    }

    private void addGoUpdateToCacheIfAvailable() {
        if (this.serverVersionInfo == null) return;

        GoVersion latestVersion = serverVersionInfo.getLatestVersion();
        if (latestVersion == null) return;

        if (latestVersion.isGreaterThan(serverVersionInfo.getInstalledVersion())) {
            goCache.put(GO_UPDATE, latestVersion.toString());
        }
    }
}