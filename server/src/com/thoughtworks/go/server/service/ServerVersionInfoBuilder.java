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
import com.thoughtworks.go.domain.exception.VersionFormatException;
import com.thoughtworks.go.server.dao.VersionInfoDao;
import com.thoughtworks.go.server.util.ServerVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServerVersionInfoBuilder {
    private static final String GO_SERVER = "go_server";
    private VersionInfoDao versionInfoDao;
    private ServerVersion serverVersion;
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerVersionInfoBuilder.class.getName());

    @Autowired
    public ServerVersionInfoBuilder(VersionInfoDao versionInfoDao, ServerVersion serverVersion) {
        this.versionInfoDao = versionInfoDao;
        this.serverVersion = serverVersion;
    }

    public VersionInfo getServerVersionInfo() {
        return updateOrCreate();
    }

    private VersionInfo updateOrCreate() {
        VersionInfo versionInfo = findAndUpdate();

        if (versionInfo == null)
            versionInfo = createVersionInfo();

        return versionInfo;
    }

    private VersionInfo findAndUpdate() {
        VersionInfo versionInfo = find();

        if (versionInfo == null) return versionInfo;

        return update(versionInfo);
    }

    private VersionInfo find() {
        return versionInfoDao.findByComponentName(GO_SERVER);
    }

    private VersionInfo update(VersionInfo versionInfo) {
        GoVersion currentGoVersion = installedVersion();

        if (currentGoVersion == null) return versionInfo;

        if (!isServerVersionInfoUpToDate(versionInfo, currentGoVersion)) {
            versionInfo.setInstalledVersion(currentGoVersion);
            versionInfoDao.saveOrUpdate(versionInfo);
        }
        return versionInfo;
    }

    private boolean isServerVersionInfoUpToDate(VersionInfo versionInfo, GoVersion currentGoVersion) {
        return currentGoVersion.equals(versionInfo.getInstalledVersion());
    }

    private VersionInfo createVersionInfo() {
        VersionInfo versionInfo = null;
        GoVersion installedVersion = installedVersion();

        if (installedVersion == null) return versionInfo;

        versionInfo = new VersionInfo(GO_SERVER, installedVersion);
        versionInfoDao.saveOrUpdate(versionInfo);

        return versionInfo;
    }

    private GoVersion installedVersion() {
        GoVersion version = null;
        String installedVersion = serverVersion.version();
        try {
            version = new GoVersion(installedVersion);
        } catch (VersionFormatException e) {
           LOGGER.error("[Go Update Check] Server Version: {} format is Invalid.", installedVersion);
        }
        return version;
    }
}
